package com.myhitbodedut

import android.content.Context
import android.content.Intent
import android.widget.ImageView
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Les vues qu'ont en commun l'onglet Zmanim et le calendrier (10/10) : la
 * carte d'un jour (date hébraïque, fêtes, ajouts) et la liste de ses
 * horaires, chacun avec sa méthode. Ce qui est montré vient de JourAffiche,
 * testé sur la JVM ; ici, seulement la mise en forme.
 */
object VueJour {

    fun dp(c: Context, v: Int) = (v * c.resources.displayMetrics.density).toInt()
    private fun couleur(c: Context, id: Int) = Apparence.couleur(c, id)

    /** Une carte, comme le style Card. */
    fun carte(c: Context): LinearLayout = LinearLayout(c).apply {
        orientation = LinearLayout.VERTICAL
        setBackgroundResource(R.drawable.bg_card)
        setPadding(dp(c, 16), dp(c, 16), dp(c, 16), dp(c, 16))
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            .apply { setMargins(dp(c, 16), dp(c, 12), dp(c, 16), 0) }
    }

    /**
     * Un texte en plusieurs paragraphes (19/10) : entre deux paragraphes, une demi-ligne vide,
     * pour que chacun se détache sans écarter les lignes d'un même paragraphe.
     */
    fun paragraphes(texte: String): CharSequence {
        val parties = texte.split(Regex("\\n+")).map { it.trim() }.filter { it.isNotEmpty() }
        if (parties.size < 2) return texte.trim()
        val s = android.text.SpannableStringBuilder()
        parties.forEachIndexed { i, x ->
            if (i > 0) {
                s.append("\n")
                val vide = s.length
                s.append("\n")
                s.setSpan(android.text.style.RelativeSizeSpan(0.5f), vide, s.length, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
            s.append(x)
        }
        return s
    }

    fun texte(c: Context, valeur: CharSequence, taille: Float, couleurId: Int, gras: Boolean = false, haut: Int = 0) =
        TextView(c).apply {
            text = valeur
            textSize = taille
            setTextColor(couleur(c, couleurId))
            if (gras) setTypeface(typeface, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                .apply { topMargin = dp(c, espace(haut)) }
        }

    /** L'échelle des espacements (16/10, validée) : 0, 4, 8, 12, 16, 24, 32, 40 ; la valeur la plus proche, un petit écart non nul restant de 4. */
    fun espace(v: Int): Int = if (v in 1..3) 4 else ECHELLE.minByOrNull { kotlin.math.abs(it - v) * 100 + ECHELLE.indexOf(it) }!!
    private val ECHELLE = intArrayOf(0, 4, 8, 12, 16, 24, 32, 40)

    private fun locale(langue: String): Locale = Locale.forLanguageTag(langue)

    /** L'usage de Birkat haLevana : celui choisi, sinon celui du noussa'h (14/10). */
    fun usageLevana(c: Context): Levana.Usage = Store.levanaUsage(c) ?: Levana.Usage.duNusah(Store.siddurNusah(c).cle)

    /**
     * Birkat haLevana (13/10 ; plus réaliste le 14/10) : la lune de ce soir dessinée,
     * ce soir ou dans combien de jours, la fenêtre à l'heure près, le motsaé Chabbat
     * conseillé, Kippour et Tich'a beAv, la jauge du mois lunaire, l'usage suivi.
     */
    private fun levana(c: Context, date: LocalDate, langue: String, haut: Int = 14): View = LinearLayout(c).apply {
        orientation = LinearLayout.VERTICAL
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        val lieu = Store.lieuZmanim(c)
        val zone = lieu?.fuseau?.let { runCatching { java.time.ZoneId.of(it) }.getOrNull() } ?: java.time.ZoneId.systemDefault()
        val usage = usageLevana(c)
        val f = Levana.fenetre(date, zone, usage)
        val lune = Levana.phase(Levana.soir(date, zone))
        val etat = Levana.etat(f, date, zone)
        val jour = DateTimeFormatter.ofPattern("EEE d MMM", locale(langue))
        val heure = DateTimeFormatter.ofPattern("EEE d MMM, HH:mm", locale(langue))
        addView(LinearLayout(c).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                .apply { topMargin = dp(c, haut) }
            addView(Levana.Dessin(c, lune, sud = (lieu?.latitude ?: 1.0) < 0).apply {
                contentDescription = null
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                    .apply { marginEnd = dp(c, 12) }
            })
            addView(LinearLayout(c).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                addView(texte(c, c.getString(R.string.levana_titre), 12f, R.color.muted, gras = true))
                addView(texte(c, when (etat) {
                    is Levana.Etat.Dans -> c.resources.getQuantityString(R.plurals.levana_dans, etat.jours, etat.jours)
                    Levana.Etat.CeSoir -> c.getString(R.string.levana_ce_soir)
                    Levana.Etat.DernierSoir -> c.getString(R.string.levana_dernier_soir)
                }, 17f, if (etat is Levana.Etat.Dans) R.color.ink else R.color.brand, gras = true, haut = 2))
                val pct = "${Math.round(lune.eclairage * 100)} %"
                addView(texte(c, c.getString(R.string.levana_lune, pct,
                    c.getString(if (lune.croissante) R.string.levana_croissante else R.string.levana_decroissante)), 13f, R.color.muted, haut = 2))
            })
        })
        addView(texte(c, c.getString(R.string.levana_fenetre, f.debut.atZone(zone).format(heure), f.fin.atZone(zone).format(heure)), 14f, R.color.ink, haut = 8))
        Levana.motsaeChabbat(f, date, zone)?.let { addView(texte(c, c.getString(R.string.levana_motsae, it.format(jour)), 13f, R.color.brand, haut = 4)) }
        f.restriction?.let {
            addView(texte(c, c.getString(if (it == Levana.Restriction.KIPPOUR) R.string.levana_kippour else R.string.levana_tisha), 13f, R.color.brand, haut = 4))
        }
        addView(Levana.Jauge(c, Levana.part(f, f.debut), Levana.part(f, f.fin), Levana.part(f, Levana.soir(date, zone))).apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                .apply { topMargin = dp(c, 8) }
            contentDescription = null
        })
        addView(texte(c, c.getString(R.string.levana_source, c.getString(usage.libelle)), 11f, R.color.muted, haut = 4))
        // Le texte en un toucher (14/10), dans le noussa'h choisi.
        addView(lien(c, c.getString(R.string.levana_lire)) { SiddurLectureActivity.ouvrir(c, "birkat_halevana") })
    }

    /** La carte d'un jour : la date hébraïque, la date civile, les fêtes, ce qu'il change aux prières. */
    fun carteJour(c: Context, date: LocalDate, enIsrael: Boolean, complet: Boolean = true): View {
        val j = CalendrierJuif.jour(date, enIsrael, Store.mukafChoma(c))
        // L'omer se compte le soir, pour le jour hébreu qui commence : celui de demain.
        val omerCeSoir = CalendrierJuif.jour(date.plusDays(1), enIsrael).omer
        val langue = ChizukCard.language(c)
        val carte = carte(c)
        // Fête ou Chabbat : l'illustration du jour (11/10).
        JourAffiche.illustration(j.fetes, date)?.let { nom ->
            carte.addView(ImageView(c).apply {
                setImageResource(illustration(nom))
                adjustViewBounds = true
                contentDescription = null
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(c, 150))
                    .apply { gravity = Gravity.CENTER_HORIZONTAL; bottomMargin = dp(c, 8) }
            })
        }
        carte.addView(texte(c, j.dateHebraique, 26f, R.color.brand, gras = true).apply {
            textDirection = View.TEXT_DIRECTION_RTL
            PoliceHebraique.appliquer(this)
            setTypeface(typeface, Typeface.BOLD)
        })
        val civil = date.format(DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", locale(langue)))
        carte.addView(texte(c, civil, 14f, R.color.muted, haut = 2))
        val marques = j.fetes.mapNotNull { Libelles.res("fete_$it")?.let(c::getString) } +
            listOfNotNull(
                j.paracha?.let { c.getString(R.string.zmanim_paracha, it) },
                omerCeSoir.takeIf { it > 0 }?.let { c.getString(R.string.zmanim_omer_ce_soir, it) },
            )
        if (marques.isNotEmpty()) carte.addView(texte(c, marques.joinToString("  ·  "), 14f, R.color.brand, gras = true, haut = 10))
        carte.addView(texte(c, c.getString(R.string.zmanim_ajouts_titre), 12f, R.color.muted, gras = true, haut = 14))
        val ajouts = JourAffiche.ajouts(j.conditions, Store.siddurNusah(c).cle, enIsrael, Store.sansTahanounFinTichri(c), Store.hemisphereSud(c)).mapNotNull { Libelles.res(it)?.let(c::getString) }
        carte.addView(texte(c, ajouts.joinToString(" · ").ifBlank { c.getString(R.string.zmanim_aucun_ajout) }, 15f, R.color.ink, haut = 2))
        // Le calendrier montre tout dans la carte ; l'écran Zmanim (14/10) n'y garde que le jour,
        // et pose Levana, hilloula et halakha sous les horaires (VueJour.sousHoraires).
        if (complet) {
            if (Store.levanaAffichee(c)) carte.addView(levana(c, date, langue))
            blocHilloula(c, carte, date, enIsrael)
            blocHalakha(c, carte, date, enIsrael)
        }
        // Partager le jour en image (14/10).
        carte.addView(lien(c, c.getString(R.string.partage_jour) + " ↗") { runCatching { PartageJour.partager(c, date) } })
        return carte
    }

    /**
     * Sous les horaires de l'écran Zmanim (14/10, « trop de choses avant les
     * זמנים ») : Birkat haLevana, la hilloula, la halakha, chacune dans sa
     * carte et chacune masquable dans les réglages.
     */
    fun sousHoraires(c: Context, date: LocalDate, enIsrael: Boolean): List<View> = listOfNotNull(
        if (Store.levanaAffichee(c)) carte(c).apply { addView(levana(c, date, ChizukCard.language(c), haut = 0)) } else null,
        carte(c).takeIf { blocHilloula(c, it, date, enIsrael, haut = 0) },
        carte(c).takeIf { blocHalakha(c, it, date, enIsrael, haut = 0) },
    )

    /** La hilloula du jour (comme CalJ) : les noms, cinq au plus, puis l'écran des hilloulot. Faux s'il n'y a rien. */
    private fun blocHilloula(c: Context, carte: LinearLayout, date: LocalDate, enIsrael: Boolean, haut: Int = 14): Boolean {
        val hilloulot = if (Store.hilloulotAffichees(c)) Hilloulot.pour(Hilloulot.table(c), date, enIsrael, Store.siddurNusah(c).cle == "ashkenaz") else emptyList()
        if (hilloulot.isEmpty()) return false
        carte.addView(texte(c, c.getString(R.string.hilloula_titre), 12f, R.color.muted, gras = true, haut = haut))
        val reste = hilloulot.size - 5
        val ouvrirHilloulot = View.OnClickListener {
            c.startActivity(Intent(c, HilloulotActivity::class.java).putExtra(HilloulotActivity.EXTRA_DATE, date.toString()))
        }
        // Plus petit que le reste de la carte (14/10) : les noms sont souvent longs.
        carte.addView(texte(c, hilloulot.take(5).joinToString("\n") { it.he }, 14f, R.color.ink, haut = 2).apply {
            textDirection = View.TEXT_DIRECTION_RTL
            PoliceHebraique.appliquer(this)
            setLineSpacing(0f, 1.25f)
        })
        // « ועוד אחד › » : plus petit, plus discret, et la flèche mène aux autres.
        if (reste > 0) carte.addView(texte(c, c.resources.getQuantityString(R.plurals.hilloula_autres, reste, reste) + " ›", 12f, R.color.muted, haut = 2).apply {
            textDirection = View.TEXT_DIRECTION_RTL
            PoliceHebraique.appliquer(this)
            setOnClickListener(ouvrirHilloulot)
            touchable(this)
        })
        carte.addView(lien(c, c.getString(R.string.hilloulot_tout), ouvrirHilloulot))
        return true
    }

    /**
     * Le début de la halakha du jour, dans le livre choisi (14/10) : le Yalkut
     * Yosef, le Ben Ish 'Haï, ou les deux ; null s'il n'y en a pas.
     */
    fun apercuHalakha(c: Context, date: LocalDate, enIsrael: Boolean): Pair<String, String>? {
        val source = Store.sourceHalakha(c)
        if (source.yalkut) YalkutYosef.pour(YalkutYosef.livre(c), date, enIsrael).firstOrNull()?.let {
            return c.getString(R.string.halakha_livre) to it.texte
        }
        if (source.benIshHai) Halakha.pour(Halakha.livre(c), date, enIsrael).firstOrNull()?.let {
            return c.getString(R.string.halakha_ben_ish_hai) to it.texte
        }
        return null
    }

    /** La halakha du jour, hors ligne : son début, puis l'écran de la halakha. Faux s'il n'y a rien. */
    private fun blocHalakha(c: Context, carte: LinearLayout, date: LocalDate, enIsrael: Boolean, haut: Int = 14): Boolean {
        val (livre, debut) = (if (Store.halakhaAffichee(c)) apercuHalakha(c, date, enIsrael) else null) ?: return false
        carte.addView(texte(c, c.getString(R.string.halakha_yomit_titre) + " · " + livre, 12f, R.color.muted, gras = true, haut = haut))
        carte.addView(texte(c, debut, 16f, R.color.ink, haut = 2).apply {
            textDirection = View.TEXT_DIRECTION_RTL
            PoliceHebraique.appliquer(this)
            maxLines = 4
            ellipsize = android.text.TextUtils.TruncateAt.END
        })
        carte.addView(lien(c, c.getString(R.string.halakha_tout)) { ouvrirHalakha(c, date) })
        return true
    }

    fun ouvrirHalakha(c: Context, date: LocalDate) {
        c.startActivity(Intent(c, HalakhaActivity::class.java).putExtra(HalakhaActivity.EXTRA_DATE, date.toString()))
    }

    /** Un texte touchable : au moins 48 dp de haut, centré verticalement (audit du 16/10). */
    fun touchable(v: TextView): TextView = v.apply {
        minHeight = dp(context, 48)
        gravity = (gravity and android.view.Gravity.RELATIVE_HORIZONTAL_GRAVITY_MASK) or android.view.Gravity.CENTER_VERTICAL
    }

    private fun lien(c: Context, libelle: String, action: View.OnClickListener) = texte(c, libelle, 15f, R.color.brand, gras = true, haut = 4).apply {
        setPadding(0, dp(c, 4), 0, dp(c, 4))
        minHeight = dp(c, 48)
        gravity = android.view.Gravity.CENTER_VERTICAL or android.view.Gravity.START
        foreground = c.getDrawable(android.R.drawable.list_selector_background)
        setOnClickListener(action)
    }

    private fun illustration(nom: String): Int = when (nom) {
        "scene_sukkot" -> R.drawable.scene_sukkot
        "char_matsa" -> R.drawable.char_matsa
        "scene_hanukkah" -> R.drawable.scene_hanukkah
        "char_purim" -> R.drawable.char_purim
        else -> R.drawable.char_shabbat
    }

    /** Les horaires d'un jour ; sans lieu, l'invitation à en choisir un. */
    fun horaires(c: Context, date: LocalDate, enIsrael: Boolean, choisirLieu: () -> Unit): View {
        val carte = carte(c)
        val lieu = Store.lieuZmanim(c)
        if (lieu == null) {
            carte.addView(texte(c, c.getString(R.string.zmanim_sans_lieu), 15f, R.color.ink))
            carte.addView(texte(c, c.getString(R.string.zmanim_choisir_lieu), 16f, R.color.brand, gras = true, haut = 12).apply {
                setPadding(0, dp(c, 8), 0, dp(c, 8))
                foreground = c.getDrawable(android.R.drawable.list_selector_background)
                setOnClickListener { choisirLieu() }
            })
            return carte
        }
        val heure = DateTimeFormatter.ofPattern("HH:mm")
        val langue = ChizukCard.language(c)
        JourAffiche.horaires(lieu, date, enIsrael, Store.allumage(c)).forEachIndexed { i, z ->
            val chabbat = z.cle == "allumage" || z.cle == "sortie"
            val ligne = LinearLayout(c).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                    .apply { topMargin = dp(c, if (i == 0) 0 else 12) }
            }
            ligne.addView(texte(c, Libelles.res("zman_${z.cle}")?.let(c::getString) ?: z.cle, 15f,
                if (chabbat) R.color.brand else R.color.ink, gras = chabbat).apply {
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            })
            ligne.addView(texte(c, if (z.statut == Zmanim.Statut.OK) z.local!!.format(heure) else "—", 17f,
                if (chabbat) R.color.brand else R.color.ink, gras = true).apply {
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                textDirection = View.TEXT_DIRECTION_LTR
                fontFeatureSettings = "tnum"
            })
            carte.addView(ligne)
            val methode = when (z.statut) {
                Zmanim.Statut.OK -> Libelles.res("zman_methode_${z.methode.cle}")
                    ?.let { c.getString(it, parametre(z.methode.parametres, langue)) }.orEmpty()
                Zmanim.Statut.ABSENT -> c.getString(R.string.zmanim_absent)
                Zmanim.Statut.ERREUR -> c.getString(R.string.zmanim_erreur)
            }
            carte.addView(texte(c, methode, 12f, R.color.muted, haut = 1))
        }
        return carte
    }

    /** « 16.1 » → « 16,1 » en français. */
    private fun parametre(p: String, langue: String): String =
        p.toDoubleOrNull()?.let { java.text.NumberFormat.getInstance(locale(langue)).format(it) } ?: p

    /** Le pied : la méthode est à valider ; la source des villes, comme CC-BY le demande. */
    fun pied(c: Context): View = LinearLayout(c).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(c, 16), dp(c, 16), dp(c, 16), 0)
        addView(texte(c, c.getString(R.string.zmanim_a_valider), 12f, R.color.muted))
        addView(texte(c, c.getString(R.string.zmanim_source_villes), 12f, R.color.muted, haut = 6))
    }
}
