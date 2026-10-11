package com.myhitbodedut

import android.content.Context
import android.content.Intent
import android.net.Uri
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

    private fun dp(c: Context, v: Int) = (v * c.resources.displayMetrics.density).toInt()
    private fun couleur(c: Context, id: Int) = Apparence.couleur(c, id)

    /** Une carte, comme le style Card. */
    fun carte(c: Context): LinearLayout = LinearLayout(c).apply {
        orientation = LinearLayout.VERTICAL
        setBackgroundResource(R.drawable.bg_card)
        setPadding(dp(c, 18), dp(c, 16), dp(c, 18), dp(c, 16))
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            .apply { setMargins(dp(c, 16), dp(c, 14), dp(c, 16), 0) }
    }

    fun texte(c: Context, valeur: CharSequence, taille: Float, couleurId: Int, gras: Boolean = false, haut: Int = 0) =
        TextView(c).apply {
            text = valeur
            textSize = taille
            setTextColor(couleur(c, couleurId))
            if (gras) setTypeface(typeface, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                .apply { topMargin = dp(c, haut) }
        }

    private fun locale(langue: String): Locale = Locale.forLanguageTag(langue)

    /** La carte d'un jour : la date hébraïque, la date civile, les fêtes, ce qu'il change aux prières. */
    fun carteJour(c: Context, date: LocalDate, enIsrael: Boolean): View {
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
                    .apply { gravity = Gravity.CENTER_HORIZONTAL; bottomMargin = dp(c, 10) }
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
        val ajouts = JourAffiche.ajouts(j.conditions, Store.siddurNusah(c).cle, enIsrael, Store.sansTahanounFinTichri(c)).mapNotNull { Libelles.res(it)?.let(c::getString) }
        carte.addView(texte(c, ajouts.joinToString(" · ").ifBlank { c.getString(R.string.zmanim_aucun_ajout) }, 15f, R.color.ink, haut = 2))
        // La hilloula du jour : trois noms au plus, et la page du jour de החכם היומי.
        val hilloulot = Hilloulot.pour(Hilloulot.table(c), date, enIsrael)
        if (hilloulot.isNotEmpty()) {
            carte.addView(texte(c, c.getString(R.string.hilloula_titre), 12f, R.color.muted, gras = true, haut = 14))
            val noms = hilloulot.take(3).joinToString(" · ") { h -> if (h.annee.isNotBlank()) "${h.he} (${h.annee})" else h.he }
            val reste = hilloulot.size - 3
            carte.addView(texte(c, if (reste > 0) "$noms · " + c.resources.getQuantityString(R.plurals.hilloula_autres, reste, reste) else noms, 15f, R.color.ink, haut = 2).apply {
                PoliceHebraique.appliquer(this)
            })
            carte.addView(lien(c, c.getString(R.string.hilloula_lien), Hilloulot.lienHyomi(date)))
        }
        // La halakha du jour : la page du site, seulement aujourd'hui (le site n'a pas d'archive par date).
        if (date == LocalDate.now()) {
            carte.addView(texte(c, c.getString(R.string.halakha_yomit_titre), 12f, R.color.muted, gras = true, haut = 14))
            carte.addView(lien(c, c.getString(R.string.halakha_yomit_lien), Hilloulot.lienHalakha(langue)))
        }
        return carte
    }

    private fun lien(c: Context, libelle: String, url: String) =
        texte(c, "$libelle ↗", 15f, R.color.brand, gras = true, haut = 4).apply {
            setPadding(0, dp(c, 6), 0, dp(c, 6))
            foreground = c.getDrawable(android.R.drawable.list_selector_background)
            setOnClickListener {
                runCatching { c.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
            }
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
        setPadding(dp(c, 20), dp(c, 18), dp(c, 20), 0)
        addView(texte(c, c.getString(R.string.zmanim_a_valider), 12f, R.color.muted))
        addView(texte(c, c.getString(R.string.zmanim_source_villes), 12f, R.color.muted, haut = 6))
    }
}
