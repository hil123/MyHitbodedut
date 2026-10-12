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

    /**
     * La vitrine (refaite le 12/10, d'après les planches envoyées) : la
     * scène avec l'avatar, puis des étagères de verre où chaque famille pose
     * son objet (coupe, écusson, étoile, rosette, médaille, pièces, cristal,
     * gemme) sur un socle à plaque, une bulle au-dessus (métal et niveau) ;
     * dessous, ce qu'a demandé le niveau et la barre vers le suivant.
     */
    private fun trophees() = VueJour.carte(this).apply {
        val pratique = Store.pratique(context)
        val familles = Trophees.Famille.entries
        val atteints = familles.sumOf { Trophees.rang(pratique, it) }
        val total = familles.sumOf { it.seuils.size }
        addView(LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(VueJour.texte(context, getString(R.string.trophees_titre), 18f, R.color.ink, gras = true).apply {
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            })
            addView(VueJour.texte(context, getString(R.string.trophees_compte, atteints, total), 14f, R.color.brand, gras = true).apply {
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            })
        })
        // Les plus hauts métaux d'abord ; à égalité, les plus proches du niveau suivant.
        val tries = familles.sortedWith(compareByDescending<Trophees.Famille> { Trophees.rang(pratique, it).let { r -> if (r == 0) -1 else Trophees.metal(it, r).ordinal } }
            .thenByDescending { Trophees.progres(pratique, it).fraction })
        addView(scene(pratique, tries.firstOrNull()?.takeIf { Trophees.rang(pratique, it) > 0 }))
        addView(barre(atteints.toFloat() / total, dp(8)).apply {
            (layoutParams as LinearLayout.LayoutParams).topMargin = dp(12)
        })
        tries.chunked(3).forEach { rangee ->
            addView(rangeeLigne(dp(18)).apply { rangee.forEach { addView(objetPose(it, Trophees.rang(pratique, it))) } })
            addView(planche())
            addView(rangeeLigne(dp(8)).apply { rangee.forEach { addView(details(it, Trophees.rang(pratique, it), Trophees.progres(pratique, it))) } })
        }
    }

    private fun rangeeLigne(haut: Int) = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.BOTTOM
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = haut }
    }

    private fun barre(fraction: Float, hauteur: Int) = android.widget.ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
        max = 1000
        progress = (fraction.coerceIn(0f, 1f) * 1000).toInt()
        progressTintList = android.content.res.ColorStateList.valueOf(Apparence.couleur(context, R.color.brand))
        progressBackgroundTintList = android.content.res.ColorStateList.valueOf(Apparence.couleur(context, R.color.muted))
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, hauteur)
        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
    }

    /** La bulle au-dessus de l'objet : le métal (et le niveau), sur une pastille de sa couleur. */
    private fun bulle(metal: Trophees.Metal?, texte: String) = TextView(this).apply {
        text = texte
        textSize = 11f
        setTypeface(typeface, Typeface.BOLD)
        maxLines = 1
        ellipsize = android.text.TextUtils.TruncateAt.END
        gravity = Gravity.CENTER
        setPadding(dp(10), dp(3), dp(10), dp(3))
        val (clair, fonce, sombre) = TropheeDessin.teintes(metal)
        setTextColor(if (metal == null) Apparence.couleur(context, R.color.muted) else ColorUtils.blendARGB(sombre, Color.BLACK, 0.35f))
        background = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(ColorUtils.blendARGB(clair, Color.WHITE, 0.5f), ColorUtils.blendARGB(clair, fonce, 0.35f))).apply { cornerRadius = dp(99).toFloat() }
        elevation = dp(2).toFloat()
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { bottomMargin = dp(6) }
    }

    /** Le socle : bois sombre (ou métal pour l'argent et le platine), et sa plaque gravée du nom. */
    private fun socle(metal: Trophees.Metal?, nom: String, largeur: Int) = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(5), dp(6), dp(5), dp(7))
        layoutParams = LinearLayout.LayoutParams(largeur, ViewGroup.LayoutParams.WRAP_CONTENT)
        val (bas, haut) = when (metal) {
            Trophees.Metal.ARGENT, Trophees.Metal.PLATINE -> Color.rgb(0x2E, 0x34, 0x3A) to Color.rgb(0x59, 0x62, 0x6C)
            null -> Color.rgb(0x80, 0x85, 0x8A) to Color.rgb(0xA9, 0xAD, 0xB1)
            else -> Color.rgb(0x3E, 0x29, 0x15) to Color.rgb(0x6B, 0x4A, 0x2E)
        }
        background = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(haut, bas)).apply {
            cornerRadii = floatArrayOf(dp(6).toFloat(), dp(6).toFloat(), dp(6).toFloat(), dp(6).toFloat(), dp(3).toFloat(), dp(3).toFloat(), dp(3).toFloat(), dp(3).toFloat())
        }
        elevation = dp(3).toFloat()
        addView(TextView(context).apply {
            text = nom
            textSize = 10f
            setTypeface(typeface, Typeface.BOLD)
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
            gravity = Gravity.CENTER
            setPadding(dp(3), dp(2), dp(3), dp(2))
            val argent = metal == Trophees.Metal.ARGENT || metal == Trophees.Metal.PLATINE
            setTextColor(if (argent) Color.rgb(0x23, 0x2A, 0x31) else if (metal == null) Color.rgb(0x4E, 0x53, 0x57) else Color.rgb(0x3A, 0x2A, 0x08))
            background = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, when {
                argent -> intArrayOf(Color.rgb(0xF4, 0xF6, 0xF8), Color.rgb(0xB9, 0xC1, 0xC9))
                metal == null -> intArrayOf(Color.rgb(0xE6, 0xE8, 0xEA), Color.rgb(0xC4, 0xC8, 0xCB))
                else -> intArrayOf(Color.rgb(0xFF, 0xE9, 0xA3), Color.rgb(0xD9, 0xAE, 0x3A))
            }).apply { cornerRadius = dp(3).toFloat() }
        })
    }

    /** La planche de verre sous une rangée. */
    private fun planche() = View(this).apply {
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(12))
        background = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(Color.argb(0xF2, 0xFF, 0xFF, 0xFF), Color.argb(0xBF, 0xDC, 0xE6, 0xF0))).apply {
            cornerRadius = dp(6).toFloat()
            setStroke(1, Color.argb(0x59, 0x96, 0xAA, 0xBE))
        }
        elevation = dp(4).toFloat()
    }

    /** L'objet posé : bulle, objet dessiné, socle. */
    private fun objetPose(f: Trophees.Famille, rang: Int) = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER_HORIZONTAL or Gravity.BOTTOM
        layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        val metal = if (rang > 0) Trophees.metal(f, rang) else null
        addView(bulle(metal, if (metal != null) "${TropheesVue.metal(context, metal)} · $rang/${f.seuils.size}" else getString(R.string.trophee_a_venir)))
        addView(View(context).apply {
            layoutParams = LinearLayout.LayoutParams(dp(78), dp(78))
            background = TropheeDessin(context, f.forme, metal, f.icone)
            if (metal == null) alpha = 0.55f
        })
        addView(socle(metal, TropheesVue.nom(context, f), ViewGroup.LayoutParams.MATCH_PARENT).apply {
            (layoutParams as LinearLayout.LayoutParams).apply { marginStart = dp(6); marginEnd = dp(6) }
        })
        contentDescription = listOfNotNull(TropheesVue.nom(context, f),
            metal?.let { getString(R.string.trophee_niveau, rang, f.seuils.size, TropheesVue.metal(context, it)) } ?: getString(R.string.trophee_a_venir),
        ).joinToString(", ")
    }

    /** Sous la planche : ce qu'a demandé le niveau, la barre et le compte vers le suivant (ou le sommet). */
    private fun details(f: Trophees.Famille, rang: Int, pr: Trophees.Progres) = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER_HORIZONTAL
        layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        addView(VueJour.texte(context, TropheesVue.description(context, f, f.seuils[(rang - 1).coerceAtLeast(0)]), 11f, R.color.muted).apply { gravity = Gravity.CENTER })
        if (rang >= f.seuils.size) {
            addView(VueJour.texte(context, getString(R.string.trophee_sommet), 11f, R.color.brand, gras = true, haut = 2).apply { gravity = Gravity.CENTER })
        } else {
            addView(barre(pr.fraction, dp(4)).apply {
                (layoutParams as LinearLayout.LayoutParams).apply { topMargin = dp(6); marginStart = dp(10); marginEnd = dp(10) }
            })
            addView(VueJour.texte(context, getString(R.string.trophee_progres, pr.valeur.coerceAtMost(pr.cible), pr.cible), 11f, R.color.muted, haut = 2).apply { gravity = Gravity.CENTER })
        }
    }

    /**
     * La scène : l'avatar, de plus en plus joyeux à mesure que le métal
     * monte, et le plus beau trophée sur son socle, qui flotte doucement
     * (rien quand les animations sont coupées).
     */
    private fun scene(p: Trophees.Pratique, meilleure: Trophees.Famille?) = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.BOTTOM
        setPadding(dp(10), dp(14), dp(10), dp(12))
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(12) }
        val metal = meilleure?.let { Trophees.metal(it, Trophees.rang(p, it)) }
        val lumiere = when (metal) {
            Trophees.Metal.OR -> Color.rgb(0xF6, 0xD8, 0x6B)
            Trophees.Metal.PLATINE -> Color.rgb(0xAA, 0xC8, 0xFF)
            else -> Apparence.couleur(context, R.color.brand)
        }
        background = GradientDrawable().apply {
            gradientType = GradientDrawable.RADIAL_GRADIENT
            gradientRadius = dp(220).toFloat()
            setGradientCenter(0.65f, 0.15f)
            colors = intArrayOf(ColorUtils.setAlphaComponent(lumiere, 0x5A), ColorUtils.setAlphaComponent(lumiere, 0x0C))
            cornerRadius = dp(18).toFloat()
        }
        addView(android.widget.ImageView(context).apply {
            setImageResource(when (metal) {
                Trophees.Metal.BRONZE -> R.drawable.char_thumbs_up
                Trophees.Metal.ARGENT -> R.drawable.char_open_arms
                Trophees.Metal.OR -> R.drawable.char_arms_raised
                Trophees.Metal.PLATINE -> R.drawable.char_dancing_joy
                null -> R.drawable.char_thinking
            })
            adjustViewBounds = true
            scaleType = android.widget.ImageView.ScaleType.FIT_END
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            layoutParams = LinearLayout.LayoutParams(0, dp(180), 1f)
        })
        addView(LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.2f)
            addView(bulle(metal, metal?.let { TropheesVue.metal(context, it) } ?: getString(R.string.trophee_a_venir)))
            addView(View(context).apply {
                layoutParams = LinearLayout.LayoutParams(dp(116), dp(116))
                background = TropheeDessin(context, meilleure?.forme ?: Trophees.Forme.COUPE, metal, meilleure?.icone ?: "🏆")
                if (metal == null) alpha = 0.55f
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                if (metal != null && android.animation.ValueAnimator.areAnimatorsEnabled()) {
                    android.animation.ObjectAnimator.ofFloat(this, View.TRANSLATION_Y, 0f, -dp(5).toFloat()).apply {
                        duration = 3000
                        repeatCount = android.animation.ValueAnimator.INFINITE
                        repeatMode = android.animation.ValueAnimator.REVERSE
                        interpolator = android.view.animation.AccelerateDecelerateInterpolator()
                        start()
                    }
                }
            })
            addView(socle(metal, meilleure?.let { TropheesVue.nom(context, it) } ?: getString(R.string.trophees_titre), dp(124)))
            addView(VueJour.texte(context, getString(if (meilleure != null) R.string.trophee_vedette else R.string.trophee_vedette_aucune), 12f, R.color.muted, haut = 8).apply { gravity = Gravity.CENTER })
            contentDescription = if (meilleure != null && metal != null)
                getString(R.string.trophee_vedette) + ", " + TropheesVue.nom(context, meilleure) + ", " + TropheesVue.metal(context, metal)
            else getString(R.string.trophee_vedette_aucune)
        })
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

