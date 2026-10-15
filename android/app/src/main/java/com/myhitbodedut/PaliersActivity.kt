package com.myhitbodedut

import android.content.Context
import android.graphics.Typeface
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.ColorUtils

/**
 * Les trophées (refaits le 15/10, « toujours trop compliqué, fais plus simple ») :
 * une seule liste de petits objectifs, du plus facile au plus dur (Trophees.chemin).
 * En haut, le prochain trophée et sa barre ; puis deux onglets, « Gagnés » (les
 * anciens) et « À venir » (les futurs, dans l'ordre) ; en bas, les records.
 * Plus de métaux, de niveaux « 4/16 » ni de cartes de paliers sur cet écran.
 */
class PaliersActivity : AppCompatActivity() {

    /** L'onglet montré : les trophées à venir d'abord, puis les gagnés au toucher. */
    private var gagnes = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_paliers)
        gagnes = savedInstanceState?.getBoolean(ETAT_ONGLET) ?: false
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(ETAT_ONGLET, gagnes)
    }

    override fun onResume() {
        super.onResume()
        afficher()
    }

    private fun afficher() {
        val contenu = findViewById<LinearLayout>(R.id.paliers_contenu)
        contenu.removeAllViews()
        val bilan = Store.bilan(this)
        val pratique = Store.pratique(this)
        val faits = Trophees.chemin.filter { Trophees.gagne(pratique, it) }
        val futurs = Trophees.chemin.filterNot { Trophees.gagne(pratique, it) }

        // Le prochain trophée, en grand.
        contenu.addView(VueJour.carte(this).apply {
            addView(VueJour.texte(context, getString(R.string.trophees_prochain), 14f, R.color.muted, gras = true))
            val n = Trophees.prochain(pratique)
            if (n == null) {
                addView(VueJour.texte(context, getString(R.string.trophees_tous), 20f, R.color.ink, gras = true, haut = 6))
            } else {
                addView(LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                        .apply { topMargin = dp(8) }
                    addView(objet(n, null, taille = 72))
                    addView(LinearLayout(context).apply {
                        orientation = LinearLayout.VERTICAL
                        layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = dp(14) }
                        addView(VueJour.texte(context, TropheesVue.description(context, n.famille, n.seuil), 20f, R.color.ink, gras = true))
                        addView(barre(Trophees.fraction(pratique, n), dp(10)).apply { (layoutParams as LinearLayout.LayoutParams).topMargin = dp(10) })
                        addView(VueJour.texte(context, getString(R.string.trophee_progres,
                            Trophees.valeur(pratique, n.famille).coerceAtMost(n.seuil), n.seuil), 14f, R.color.brand, gras = true, haut = 4))
                    })
                })
            }
        })

        // Les deux onglets : à venir, gagnés.
        contenu.addView(LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                .apply { setMargins(dp(16), dp(16), dp(16), 0) }
            addView(onglet(getString(R.string.trophees_a_venir, futurs.size), !gagnes) { gagnes = false; afficher() })
            addView(onglet(getString(R.string.trophees_gagnes, faits.size), gagnes) { gagnes = true; afficher() }.apply {
                (layoutParams as LinearLayout.LayoutParams).marginStart = dp(8)
            })
        })
        contenu.addView(VueJour.carte(this).apply {
            val liste = if (gagnes) faits.reversed() else futurs
            if (liste.isEmpty()) addView(VueJour.texte(context, getString(if (gagnes) R.string.trophees_aucun_gagne else R.string.trophees_tous), 15f, R.color.muted))
            // Le matériau d'un trophée gagné : le grade du moment où il l'a été (Trophees.grade).
            val rangs = faits.withIndex().associate { (i, n) -> n to i }
            liste.forEachIndexed { k, n -> addView(ligne(n, pratique, gagnes, premiere = k == 0, metal = rangs[n]?.let { Trophees.grade(it) })) }
        })

        // Les records, comme avant.
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
        Store.setPaliersVus(this, Paliers.marques(Paliers.etats(bilan), bilan))
        Store.setTropheesVus(this, Trophees.gagnes(pratique).map { it.cle }.toSet())
    }

    /** Un onglet : plein quand il est choisi. */
    private fun onglet(texte: String, choisi: Boolean, action: () -> Unit) = TextView(this).apply {
        text = texte
        textSize = 15f
        gravity = Gravity.CENTER
        setTypeface(typeface, Typeface.BOLD)
        val brand = Apparence.couleur(context, R.color.brand)
        setTextColor(if (choisi) Apparence.couleur(context, R.color.on_brand) else brand)
        setPadding(dp(12), dp(10), dp(12), dp(10))
        background = GradientDrawable().apply {
            cornerRadius = dp(999).toFloat()
            if (choisi) setColor(brand) else setStroke(dp(1), brand)
        }
        layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        isSelected = choisi
        setOnClickListener { action() }
    }

    /** Une ligne : l'objet, ce qu'il demande ; à venir, la barre et le compte ; gagné, une coche. */
    private fun ligne(n: Trophees.Niveau, p: Trophees.Pratique, gagne: Boolean, premiere: Boolean, metal: Trophees.Metal?) = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            .apply { topMargin = dp(if (premiere) 0 else 12) }
        addView(objet(n, if (gagne) metal else null, taille = 44))
        addView(LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = dp(12) }
            addView(VueJour.texte(context, TropheesVue.description(context, n.famille, n.seuil), 15f, if (gagne) R.color.ink else R.color.ink, gras = gagne))
            if (!gagne) {
                val f = Trophees.fraction(p, n)
                if (f > 0f) {
                    addView(barre(f, dp(5)).apply { (layoutParams as LinearLayout.LayoutParams).topMargin = dp(5) })
                    addView(VueJour.texte(context, getString(R.string.trophee_progres,
                        Trophees.valeur(p, n.famille).coerceAtMost(n.seuil), n.seuil), 12f, R.color.muted, haut = 2))
                }
            }
        })
        if (gagne) addView(VueJour.texte(context, "✓", 20f, R.color.brand, gras = true))
        contentDescription = TropheesVue.description(context, n.famille, n.seuil)
    }

    /** L'objet dessiné : du matériau du grade s'il est gagné, gris et pâle sinon. */
    private fun objet(n: Trophees.Niveau, metal: Trophees.Metal?, taille: Int) = View(this).apply {
        layoutParams = LinearLayout.LayoutParams(dp(taille), dp(taille))
        background = TropheeDessin(context, n.famille.forme, metal, n.famille.icone)
        if (metal == null) alpha = 0.45f
        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
    }

    private fun barre(fraction: Float, hauteur: Int) = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
        max = 1000
        progress = (fraction.coerceIn(0f, 1f) * 1000).toInt()
        val brand = Apparence.couleur(context, R.color.brand)
        progressTintList = android.content.res.ColorStateList.valueOf(brand)
        progressBackgroundTintList = android.content.res.ColorStateList.valueOf(ColorUtils.setAlphaComponent(brand, 0x30))
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, hauteur)
        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private companion object {
        const val ETAT_ONGLET = "trophees_gagnes"
    }
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
    fun nom(c: Context, f: Trophees.Famille): String = c.getString(when (f) {
        Trophees.Famille.PREMIER_PAS -> R.string.trophee_premier_pas
        Trophees.Famille.SEANCE_LONGUE -> R.string.trophee_seance_longue
        Trophees.Famille.JOURS -> R.string.trophee_jours
        Trophees.Famille.SERIE -> R.string.trophee_serie
        Trophees.Famille.HEURES -> R.string.trophee_heures
        Trophees.Famille.SEMAINES -> R.string.trophee_semaines
        Trophees.Famille.EREV_CHABBAT -> R.string.trophee_erev_chabbat
        Trophees.Famille.RETOUR -> R.string.trophee_retour
        Trophees.Famille.PLUME -> R.string.trophee_plume
    })

    /** Ce que demande un seuil de la famille (« 40 jours de pratique »). */
    fun description(c: Context, f: Trophees.Famille, seuil: Int): String = c.resources.getQuantityString(when (f) {
        Trophees.Famille.PREMIER_PAS -> R.plurals.trophee_premier_pas_desc
        Trophees.Famille.SEANCE_LONGUE -> R.plurals.trophee_seance_longue_desc
        Trophees.Famille.JOURS -> R.plurals.trophee_jours_desc
        Trophees.Famille.SERIE -> R.plurals.trophee_serie_desc
        Trophees.Famille.HEURES -> R.plurals.trophee_heures_desc
        Trophees.Famille.SEMAINES -> R.plurals.trophee_semaines_desc
        Trophees.Famille.EREV_CHABBAT -> R.plurals.trophee_erev_chabbat_desc
        Trophees.Famille.RETOUR -> R.plurals.trophee_retour_desc
        Trophees.Famille.PLUME -> R.plurals.trophee_plume_desc
    }, seuil, seuil)

    fun metal(c: Context, m: Trophees.Metal): String = c.getString(when (m) {
        Trophees.Metal.BRONZE -> R.string.metal_bronze
        Trophees.Metal.ARGENT -> R.string.metal_argent
        Trophees.Metal.OR -> R.string.metal_or
        Trophees.Metal.PLATINE -> R.string.metal_platine
    })
}

