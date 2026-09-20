package com.myhitbodedut

import android.content.Context
import android.view.View
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

        card.findViewById<TextView>(R.id.chizuk_he).text = ChizukRules.hebrew(chizuk, item)

        val translation = card.findViewById<TextView>(R.id.chizuk_translation)
        val text = translationOf(item, language(card.context))
        translation.text = text
        // En hébreu, la traduction répéterait le texte du dessus.
        translation.visibility = if (text.isBlank()) View.GONE else View.VISIBLE

        card.findViewById<TextView>(R.id.chizuk_ref).text = item.ref
        card.findViewById<TextView>(R.id.chizuk_badge).visibility =
            if (ChizukRules.needsProofreading(item)) View.VISIBLE else View.GONE
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

    fun chizuk(context: Context): Chizuk =
        chizuk ?: synchronized(this) {
            chizuk ?: ChizukLoader.load(context.applicationContext).also { chizuk = it }
        }

    fun library(context: Context): Library =
        library ?: synchronized(this) {
            library ?: LibraryLoader.load(context.applicationContext).also { library = it }
        }
}
