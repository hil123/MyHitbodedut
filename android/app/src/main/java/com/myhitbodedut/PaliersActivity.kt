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
        contenu.addView(VueJour.texte(this, getString(R.string.paliers_sous_titre), 15f, R.color.muted, haut = 14).apply {
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
