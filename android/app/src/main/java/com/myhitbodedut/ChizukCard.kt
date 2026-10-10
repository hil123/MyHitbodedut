package com.myhitbodedut

import android.content.Context
import android.text.StaticLayout
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import java.util.Locale

/**
 * Le remplissage de view_chizuk.xml, et le tirage qui l'alimente.
 *
 * Une seule entrée, [show] : elle tire un texte pour le moment demandé,
 * remplit la carte et la rend visible, ou la masque s'il n'y a rien à
 * montrer. C'est le cas d'une release tant qu'aucun texte n'est relu :
 * mieux vaut pas de carte qu'une carte vide.
 */
object ChizukCard {

    /**
     * Tire et affiche. Rend le texte retenu, ou null si la carte est masquée.
     *
     * @param card la vue issue de view_chizuk.xml.
     */
    fun show(card: View, moment: Moment): ChizukItem? {
        val context = card.context
        val chizuk = Data.chizuk(context)
        val item = ChizukBag.draw(context, moment, chizuk, BuildConfig.DEBUG)
        bind(card, chizuk, item, moment)
        return item
    }

    /** Remplit la carte avec un texte déjà choisi, sans rien tirer. */
    fun bind(card: View, chizuk: Chizuk, item: ChizukItem?, moment: Moment) {
        if (item == null) {
            card.visibility = View.GONE
            return
        }
        card.visibility = View.VISIBLE

        // L'illustration et le nom du moment : avant même de lire, on voit
        // de quoi il s'agit. Ils tiennent du moment, pas du texte tiré, donc
        // ils ne changent pas d'un tirage à l'autre.
        card.findViewById<ImageView>(R.id.chizuk_art).setImageResource(Character.ofMoment(moment))
        card.findViewById<TextView>(R.id.chizuk_moment).setText(moment.labelRes)

        val hebreu = card.findViewById<TextView>(R.id.chizuk_he)
        hebreu.text = ChizukRules.hebrew(chizuk, item)
        ajusterHauteur(hebreu)

        val translation = card.findViewById<TextView>(R.id.chizuk_translation)
        val text = translationOf(item, language(card.context))
        translation.text = text
        ajusterHauteur(translation)
        // En hébreu, la traduction répéterait le texte du dessus.
        translation.visibility = if (text.isBlank()) View.GONE else View.VISIBLE

        card.findViewById<TextView>(R.id.chizuk_ref).text = Reference.localise(card.context, item.ref)
        card.findViewById<TextView>(R.id.chizuk_badge).visibility =
            if (ChizukRules.needsProofreading(item)) View.VISIBLE else View.GONE
    }

    /**
     * La hauteur fixe de la carte, sauf pour un texte qui n'y tient pas.
     *
     * Le corps automatique réduit le texte pour qu'il tienne dans la hauteur
     * prévue. Il descendait jusqu'à 8 points, illisible en hébreu voyellé, et
     * annulait la taille de texte choisie dans le téléphone (revue du
     * 29/09). Le minimum est désormais de 12 points ; un texte trop long pour
     * tenir à cette taille ne rétrécit plus : le bloc s'allonge, sans défiler.
     */
    private fun ajusterHauteur(vue: TextView) {
        val params = vue.layoutParams ?: return
        val fixe = (vue.getTag(R.id.chizuk_hauteur_fixe) as? Int)
            ?: params.height.also { vue.setTag(R.id.chizuk_hauteur_fixe, it) }
        val min = (vue.getTag(R.id.chizuk_corps_min) as? Int)
            ?: vue.autoSizeMinTextSize.also { vue.setTag(R.id.chizuk_corps_min, it) }
        val max = (vue.getTag(R.id.chizuk_corps_max) as? Int)
            ?: vue.autoSizeMaxTextSize.also { vue.setTag(R.id.chizuk_corps_max, it) }
        if (fixe <= 0 || min <= 0 || max <= 0) return

        // Retour à la mise en page prévue, puis mesure une fois la largeur connue.
        if (params.height != fixe) {
            params.height = fixe
            vue.layoutParams = params
        }
        vue.setAutoSizeTextTypeUniformWithConfiguration(min, max, 1, TypedValue.COMPLEX_UNIT_PX)
        vue.post {
            val largeur = vue.width - vue.paddingLeft - vue.paddingRight
            if (largeur <= 0) return@post
            val paint = android.text.TextPaint(vue.paint).apply { textSize = min.toFloat() }
            val mesure = StaticLayout.Builder
                .obtain(vue.text, 0, vue.text.length, paint, largeur)
                .setLineSpacing(vue.lineSpacingExtra, vue.lineSpacingMultiplier)
                .setIncludePad(vue.includeFontPadding)
                .build()
            val disponible = fixe - vue.paddingTop - vue.paddingBottom
            if (mesure.height > disponible) {
                vue.setAutoSizeTextTypeWithDefaults(TextView.AUTO_SIZE_TEXT_TYPE_NONE)
                vue.setTextSize(TypedValue.COMPLEX_UNIT_PX, min.toFloat())
                val p = vue.layoutParams
                p.height = ViewGroup.LayoutParams.WRAP_CONTENT
                vue.layoutParams = p
            }
        }
    }

    /**
     * La traduction dans la langue de l'interface. En hébreu il n'y en a pas
     * à donner : l'hébreu affiché au-dessus est le texte lui-même, et la
     * répéter mangerait la moitié de la carte.
     *
     * La langue est passée plutôt que lue, pour que la règle se vérifie sans
     * appareil.
     */
    fun translationOf(item: ChizukItem, language: String): String = when (language) {
        "he", "iw" -> ""
        "en" -> item.en
        // Le francais et toute langue que l'application ne parle pas : c'est
        // la langue de values/, donc celle de l'interface qui entoure la carte.
        else -> item.fr
    }

    /** « iw » est l'ancien code d'Android pour l'hébreu, encore rendu par Locale. */
    fun language(context: Context): String {
        val tag = context.resources.configuration.locales[0].language.lowercase(Locale.US)
        return if (tag == "iw") "he" else tag
    }
}

/**
 * Les deux fichiers de données, analysés une fois par processus.
 *
 * chizuk.json fait 68 ko et bibliotheque.json 60 ko : les relire à chaque
 * ouverture d'écran se verrait sur un téléphone ancien.
 */
object Data {

    @Volatile
    private var chizuk: Chizuk? = null

    @Volatile
    private var library: Library? = null

    @Volatile
    private var tikkun: Tikkun? = null

    @Volatile
    private var tehilim: Tehilim? = null

    private val siddurs = java.util.concurrent.ConcurrentHashMap<Nusah, Siddur>()

    fun chizuk(context: Context): Chizuk =
        chizuk ?: synchronized(this) {
            chizuk ?: ChizukLoader.load(context.applicationContext).also { chizuk = it }
        }

    fun library(context: Context): Library =
        library ?: synchronized(this) {
            library ?: LibraryLoader.load(context.applicationContext).also { library = it }
        }

    fun tikkun(context: Context): Tikkun =
        tikkun ?: synchronized(this) {
            tikkun ?: TikkunLoader.load(context.applicationContext).also { tikkun = it }
        }

    /** Le livre de Tehilim : 327 ko, lus une fois par processus comme les autres. */
    fun tehilim(context: Context): Tehilim =
        tehilim ?: synchronized(this) {
            tehilim ?: TehilimLoader.load(context.applicationContext).also { tehilim = it }
        }

    @Volatile
    private var annotations: String? = null

    @Volatile
    private var villes: List<Ville>? = null

    /** Les ajouts du jour du siddour (SiddurJour, 10/10) : le fichier est lu une fois. */
    fun ajouts(context: Context, nusah: Nusah = Store.siddurNusah(context)): Map<String, List<SiddurJour.Annotation>> {
        val json = annotations ?: Json.readRaw(context.applicationContext, R.raw.siddur_annotations).also { annotations = it }
        return SiddurJour.parse(json, nusah.cle)
    }

    /** Les villes des zmanim (GeoNames), lues une fois. */
    fun villes(context: Context): List<Ville> =
        villes ?: Villes.parse(Json.readRaw(context.applicationContext, R.raw.villes)).also { villes = it }

    /** Le siddour du nusah choisi : environ 550 ko par nusah, lus une fois par processus. */
    fun siddur(context: Context, nusah: Nusah = Store.siddurNusah(context)): Siddur =
        siddurs.getOrPut(nusah) { SiddurLoader.load(context.applicationContext, nusah) }
}
