package com.myhitbodedut

import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.ColorUtils

/**
 * Les paliers d'objectifs (11/10) : une carte par piste, la valeur, la barre
 * jusqu'au prochain palier, ce qu'il reste, et les paliers déjà atteints ;
 * puis les records. Le calcul est dans Paliers.
 */
class PaliersActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_paliers)
    }

    override fun onResume() {
        super.onResume()
        afficher()
    }

    private fun afficher() {
        val contenu = findViewById<LinearLayout>(R.id.paliers_contenu)
        contenu.removeAllViews()
        val bilan = Store.bilan(this)
        val etats = Paliers.etats(bilan)
        contenu.addView(trophees())
        contenu.addView(VueJour.texte(this, getString(R.string.paliers_title), 20f, R.color.ink, gras = true, haut = 22).apply {
            gravity = Gravity.CENTER
        })
        contenu.addView(VueJour.texte(this, getString(R.string.paliers_sous_titre), 15f, R.color.muted, haut = 4).apply {
            gravity = Gravity.CENTER
        })
        etats.forEach { contenu.addView(carte(it, bilan)) }

        val moisDe = { d: java.time.LocalDate -> CalendrierJuif.jour(d, false).let { "${it.anneeHebraique}-${it.moisHebreu}" } }
        val r = Paliers.records(bilan.minutes, moisDe)
        contenu.addView(VueJour.carte(this).apply {
            addView(VueJour.texte(context, getString(R.string.paliers_records), 17f, R.color.ink, gras = true))
            listOf(
                getString(R.string.record_serie, PaliersVue.valeur(context, Paliers.Piste.SERIE, Store.meilleureSerie(context))),
                getString(R.string.record_jour, Store.formatDuration(context, r.jour)),
                getString(R.string.record_semaine, Store.formatDuration(context, r.semaine)),
                getString(R.string.record_mois, Store.formatDuration(context, r.mois)),
            ).forEach { addView(VueJour.texte(context, it, 15f, R.color.ink, haut = 6)) }
        })
        // Ce qui est vu ici est acquis : l'accueil ne le refête pas.
        Store.setPaliersVus(this, Paliers.marques(etats, bilan))
        Store.setTropheesVus(this, Trophees.gagnes(Store.pratique(this)).map { it.cle }.toSet())
    }

    /** La vitrine : les trophées gagnés en couleur, les autres en attente, avec ce qu'il faut pour les gagner. */
    private fun trophees() = VueJour.carte(this).apply {
        val gagnes = Trophees.gagnes(Store.pratique(context))
        val tous = Trophees.Trophee.values()
        addView(LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(VueJour.texte(context, getString(R.string.trophees_titre), 18f, R.color.ink, gras = true).apply {
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            })
            addView(VueJour.texte(context, getString(R.string.trophees_compte, gagnes.size, tous.size), 14f, R.color.brand, gras = true).apply {
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            })
        })
        val grille = android.widget.GridLayout(context).apply {
            columnCount = 3
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(10) }
        }
        // Les gagnés d'abord, dans l'ordre de la liste.
        tous.sortedBy { if (it in gagnes) 0 else 1 }.forEach { t -> grille.addView(trophee(t, t in gagnes)) }
        addView(grille)
    }

    private fun trophee(t: Trophees.Trophee, gagne: Boolean) = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER_HORIZONTAL
        setPadding(dp(4), dp(8), dp(4), dp(8))
        alpha = if (gagne) 1f else 0.38f
        layoutParams = android.widget.GridLayout.LayoutParams(
            android.widget.GridLayout.spec(android.widget.GridLayout.UNDEFINED, 1f),
            android.widget.GridLayout.spec(android.widget.GridLayout.UNDEFINED, 1f)
        ).apply { width = 0 }
        addView(TextView(context).apply {
            text = t.icone
            textSize = 30f
            gravity = Gravity.CENTER
            val brand = Apparence.couleur(context, R.color.brand)
            setTextColor(brand)
            val d = dp(58)
            layoutParams = LinearLayout.LayoutParams(d, d)
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                if (gagne) setColor(ColorUtils.setAlphaComponent(brand, 0x26)) else setStroke(dp(1), brand)
            }
        })
        addView(VueJour.texte(context, TropheesVue.nom(context, t), 13f, R.color.ink, gras = true, haut = 6).apply { gravity = Gravity.CENTER })
        addView(VueJour.texte(context, TropheesVue.description(context, t), 11f, R.color.muted, haut = 2).apply { gravity = Gravity.CENTER })
        contentDescription = TropheesVue.nom(context, t) + ", " + TropheesVue.description(context, t)
    }

    private fun carte(e: Paliers.Etat, bilan: Paliers.Bilan) = VueJour.carte(this).apply {
        val brand = Apparence.couleur(context, R.color.brand)
        addView(VueJour.texte(context, PaliersVue.libelle(context, e.piste, bilan), 14f, R.color.muted, gras = true))
        addView(VueJour.texte(context, PaliersVue.valeur(context, e.piste, e.valeur), 26f, R.color.ink, gras = true, haut = 2))
        addView(ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 1000
            progress = (e.avancee * 1000).toInt()
            progressTintList = android.content.res.ColorStateList.valueOf(brand)
            progressBackgroundTintList = android.content.res.ColorStateList.valueOf(ColorUtils.setAlphaComponent(brand, 0x30))
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(10)).apply { topMargin = dp(8) }
        })
        addView(VueJour.texte(context, getString(R.string.palier_prochain,
            PaliersVue.valeur(context, e.piste, e.prochain), PaliersVue.valeur(context, e.piste, e.reste)), 15f, R.color.brand, gras = true, haut = 8))
        if (e.rang > 0) {
            addView(VueJour.texte(context, getString(R.string.palier_atteints, e.rang), 13f, R.color.muted, haut = 10))
            // Les paliers atteints, du plus récent au plus ancien, et le prochain en pointillé.
            val rangee = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL }
            val echelle = Paliers.echelle(e.piste, e.valeur, bilan.objectifJour)
            (echelle.filter { it <= e.valeur }.reversed().take(8)).forEach { rangee.addView(pastille(PaliersVue.court(context, e.piste, it), true)) }
            addView(HorizontalScrollView(context).apply {
                isHorizontalScrollBarEnabled = false
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(6) }
                addView(rangee)
            })
        }
    }

    private fun pastille(texte: String, atteinte: Boolean) = TextView(this).apply {
        text = texte
        textSize = 13f
        setTypeface(typeface, Typeface.BOLD)
        val brand = Apparence.couleur(context, R.color.brand)
        setTextColor(if (atteinte) Apparence.couleur(context, R.color.on_brand) else brand)
        setPadding(dp(12), dp(5), dp(12), dp(5))
        background = GradientDrawable().apply {
            cornerRadius = dp(999).toFloat()
            if (atteinte) setColor(brand) else setStroke(dp(1), brand)
        }
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { marginEnd = dp(6) }
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
}

/** Les libellés et les valeurs des paliers, partagés par l'écran et l'accueil. */
object PaliersVue {

    fun libelle(c: Context, piste: Paliers.Piste, bilan: Paliers.Bilan): String = when (piste) {
        Paliers.Piste.SERIE -> c.getString(R.string.palier_serie)
        Paliers.Piste.JOUR -> c.getString(R.string.palier_jour)
        Paliers.Piste.SEMAINE -> c.getString(R.string.palier_semaine)
        Paliers.Piste.MOIS -> // Isolé (FSI … PDI) : le nom hébreu ne renverse pas la phrase.
            c.getString(R.string.palier_mois, "\u2068" + CalendrierJuif.jour(bilan.aujourdhui, false).moisEtAnnee + "\u2069")
        Paliers.Piste.CUMUL -> c.getString(R.string.palier_cumul)
        Paliers.Piste.JOURS -> c.getString(R.string.palier_jours)
    }

    /** Des jours pour la série et les jours de pratique, une durée pour le reste. */
    fun valeur(c: Context, piste: Paliers.Piste, v: Int): String = when (piste) {
        Paliers.Piste.SERIE, Paliers.Piste.JOURS -> c.resources.getQuantityString(R.plurals.palier_n_jours, v, v)
        else -> Store.formatDuration(c, v)
    }

    fun court(c: Context, piste: Paliers.Piste, v: Int): String = valeur(c, piste, v)

    /** La phrase de l'accueil : ce qu'il reste pour le palier le plus proche. */
    fun aLaUne(c: Context, e: Paliers.Etat): String {
        val id = when (e.piste) {
            Paliers.Piste.SERIE -> R.string.palier_une_serie
            Paliers.Piste.JOUR -> R.string.palier_une_jour
            Paliers.Piste.SEMAINE -> R.string.palier_une_semaine
            else -> R.string.palier_une_mois
        }
        return c.getString(id, valeur(c, e.piste, e.reste), valeur(c, e.piste, e.prochain))
    }
}

/** Les noms et les conditions des trophées (écrits ici, pas par getIdentifier : la réduction des ressources les retirerait). */
object TropheesVue {
    fun nom(c: Context, t: Trophees.Trophee): String = c.getString(when (t) {
        Trophees.Trophee.PREMIER_PAS -> R.string.trophee_premier_pas
        Trophees.Trophee.UNE_HEURE -> R.string.trophee_une_heure
        Trophees.Trophee.DEUX_HEURES -> R.string.trophee_deux_heures
        Trophees.Trophee.SEMAINE_PLEINE -> R.string.trophee_semaine_pleine
        Trophees.Trophee.EREV_CHABBAT -> R.string.trophee_erev_chabbat
        Trophees.Trophee.RETOUR -> R.string.trophee_retour
        Trophees.Trophee.CHAI -> R.string.trophee_chai
        Trophees.Trophee.QUARANTE -> R.string.trophee_quarante
        Trophees.Trophee.DIX_HEURES -> R.string.trophee_dix_heures
        Trophees.Trophee.CENT_HEURES -> R.string.trophee_cent_heures
        Trophees.Trophee.PLUME -> R.string.trophee_plume
        Trophees.Trophee.PLUME_FIDELE -> R.string.trophee_plume_fidele
        Trophees.Trophee.TARYAG -> R.string.trophee_taryag
    })

    fun description(c: Context, t: Trophees.Trophee): String = c.getString(when (t) {
        Trophees.Trophee.PREMIER_PAS -> R.string.trophee_premier_pas_desc
        Trophees.Trophee.UNE_HEURE -> R.string.trophee_une_heure_desc
        Trophees.Trophee.DEUX_HEURES -> R.string.trophee_deux_heures_desc
        Trophees.Trophee.SEMAINE_PLEINE -> R.string.trophee_semaine_pleine_desc
        Trophees.Trophee.EREV_CHABBAT -> R.string.trophee_erev_chabbat_desc
        Trophees.Trophee.RETOUR -> R.string.trophee_retour_desc
        Trophees.Trophee.CHAI -> R.string.trophee_chai_desc
        Trophees.Trophee.QUARANTE -> R.string.trophee_quarante_desc
        Trophees.Trophee.DIX_HEURES -> R.string.trophee_dix_heures_desc
        Trophees.Trophee.CENT_HEURES -> R.string.trophee_cent_heures_desc
        Trophees.Trophee.PLUME -> R.string.trophee_plume_desc
        Trophees.Trophee.PLUME_FIDELE -> R.string.trophee_plume_fidele_desc
        Trophees.Trophee.TARYAG -> R.string.trophee_taryag_desc
    })
}
