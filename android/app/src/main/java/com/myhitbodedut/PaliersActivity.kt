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
     * La vitrine (refaite le 12/10) : une médaille par famille, au métal du
     * niveau atteint (bronze, argent, or, platine), en relief ; le niveau,
     * ce qu'il a fallu, et la barre vers le niveau suivant.
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
        addView(scene(pratique))
        addView(barre(atteints.toFloat() / total, dp(8)).apply {
            (layoutParams as LinearLayout.LayoutParams).topMargin = dp(12)
        })
        val grille = android.widget.GridLayout(context).apply {
            columnCount = 3
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(12) }
        }
        // Les plus hauts métaux d'abord ; à égalité, les plus proches du niveau suivant.
        familles.sortedWith(compareByDescending<Trophees.Famille> { Trophees.rang(pratique, it).let { r -> if (r == 0) -1 else Trophees.metal(it, r).ordinal } }
            .thenByDescending { Trophees.progres(pratique, it).fraction })
            .forEach { f -> grille.addView(trophee(f, Trophees.rang(pratique, f), Trophees.progres(pratique, f))) }
        addView(grille)
    }

    /** Les deux tons d'un métal : le clair au centre, le foncé au bord. */
    private fun tons(m: Trophees.Metal): Pair<Int, Int> = when (m) {
        Trophees.Metal.PLATINE -> Color.rgb(0xF4, 0xF7, 0xFF) to Color.rgb(0x6F, 0x86, 0xA8)
        Trophees.Metal.OR -> Color.rgb(0xF6, 0xD8, 0x6B) to Color.rgb(0xB8, 0x86, 0x0B)
        Trophees.Metal.ARGENT -> Color.rgb(0xEE, 0xF1, 0xF4) to Color.rgb(0x8C, 0x96, 0xA0)
        Trophees.Metal.BRONZE -> Color.rgb(0xF0, 0xB9, 0x86) to Color.rgb(0x9C, 0x5A, 0x2B)
    }

    private fun barre(fraction: Float, hauteur: Int) = android.widget.ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
        max = 1000
        progress = (fraction.coerceIn(0f, 1f) * 1000).toInt()
        progressTintList = android.content.res.ColorStateList.valueOf(Apparence.couleur(context, R.color.brand))
        progressBackgroundTintList = android.content.res.ColorStateList.valueOf(Apparence.couleur(context, R.color.muted))
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, hauteur)
        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
    }

    /**
     * La médaille en relief : une tranche (anneau foncé puis clair), une face
     * bombée (dégradé radial éclairé en haut à gauche), un reflet, et
     * l'ombre portée de l'élévation. À gagner : la même, en gris.
     */
    private fun medaille(metal: Trophees.Metal?, taille: Int, forme: Trophees.Forme = Trophees.Forme.MEDAILLE): android.graphics.drawable.Drawable {
        val gris = Apparence.couleur(this, R.color.muted)
        val (clair, fonce) = metal?.let { tons(it) } ?: (ColorUtils.blendARGB(gris, Color.WHITE, 0.75f) to ColorUtils.blendARGB(gris, Color.WHITE, 0.25f))
        if (forme != Trophees.Forme.MEDAILLE) return FormeTrophee(forme, clair, fonce, metal != null)
        fun disque(couleurs: IntArray, radial: Boolean = false) = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            if (radial) {
                gradientType = GradientDrawable.RADIAL_GRADIENT
                gradientRadius = taille * 0.62f
                setGradientCenter(0.35f, 0.3f)
            } else orientation = GradientDrawable.Orientation.TL_BR
            colors = couleurs
        }
        val couches = android.graphics.drawable.LayerDrawable(arrayOf(
            disque(intArrayOf(ColorUtils.blendARGB(fonce, Color.BLACK, 0.3f), ColorUtils.blendARGB(fonce, Color.BLACK, 0.45f))), // l'épaisseur de la pièce
            disque(intArrayOf(clair, fonce)),                                  // la tranche, éclairée en haut à gauche
            disque(intArrayOf(fonce, clair)),                                  // le biseau, inversé
            disque(intArrayOf(Color.WHITE, clair, fonce), radial = true),      // la face bombée
            disque(intArrayOf(ColorUtils.setAlphaComponent(Color.WHITE, 0x8C), Color.TRANSPARENT)), // le reflet
        ))
        val e1 = taille / 22; val e2 = taille / 11; val ep = taille / 18
        couches.setLayerInset(0, 0, ep, 0, 0)
        couches.setLayerInset(1, 0, 0, 0, ep)
        couches.setLayerInset(2, e1, e1, e1, e1 + ep)
        couches.setLayerInset(3, e2, e2, e2, e2 + ep)
        couches.setLayerInset(4, e2 + 1, e2 + 1, taille / 3, taille / 3 + ep)
        return couches
    }

    /** Le ruban : deux pans au-dessus de la médaille, au métal, posés derrière elle. */
    private fun ruban(metal: Trophees.Metal, l: Int, h: Int) = View(this).apply {
        val (clair, fonce) = tons(metal)
        layoutParams = LinearLayout.LayoutParams(l, h).apply { bottomMargin = -h / 3 }
        background = object : android.graphics.drawable.Drawable() {
            private val p = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
            override fun draw(c: android.graphics.Canvas) {
                val b = bounds; val w = b.width().toFloat(); val hh = b.height().toFloat()
                val chemin = android.graphics.Path().apply {
                    moveTo(0f, 0f); lineTo(w, 0f); lineTo(w * 0.78f, hh); lineTo(w * 0.5f, hh * 0.7f); lineTo(w * 0.22f, hh); close()
                }
                p.color = fonce; c.drawPath(chemin, p)
                p.color = clair; c.drawRect(w * 0.47f, 0f, w * 0.53f, hh * 0.72f, p)
            }
            override fun setAlpha(alpha: Int) { p.alpha = alpha }
            override fun setColorFilter(cf: android.graphics.ColorFilter?) { p.colorFilter = cf }
            @Deprecated("Deprecated in Java")
            override fun getOpacity() = android.graphics.PixelFormat.TRANSLUCENT
        }
    }

    /**
     * La scène (12/10) : l'avatar, de plus en plus joyeux à mesure que le
     * métal monte, et la plus belle médaille sur son piédestal, qui tourne
     * doucement (rien quand les animations sont coupées).
     */
    private fun scene(p: Trophees.Pratique) = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.BOTTOM
        setPadding(dp(10), dp(14), dp(10), dp(12))
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(12) }
        val meilleure = Trophees.Famille.entries.filter { Trophees.rang(p, it) > 0 }
            .maxWithOrNull(compareBy<Trophees.Famille> { Trophees.metal(it, Trophees.rang(p, it)).ordinal }.thenBy { Trophees.rang(p, it) })
        val metal = meilleure?.let { Trophees.metal(it, Trophees.rang(p, it)) }
        val brand = Apparence.couleur(context, R.color.brand)
        val lumiere = when (metal) {
            Trophees.Metal.OR -> Color.rgb(0xF6, 0xD8, 0x6B)
            Trophees.Metal.PLATINE -> Color.rgb(0xAA, 0xC8, 0xFF)
            else -> brand
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
            layoutParams = LinearLayout.LayoutParams(0, dp(170), 1f)
        })
        addView(LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.2f)
            val d = dp(104)
            if (metal != null && meilleure?.forme == Trophees.Forme.MEDAILLE) addView(ruban(metal, dp(46), dp(30)))
            addView(TextView(context).apply {
                text = meilleure?.icone ?: "🏆"
                textSize = 46f
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(d, d)
                background = medaille(metal, d, meilleure?.forme ?: Trophees.Forme.COUPE)
                if ((meilleure?.forme ?: Trophees.Forme.COUPE) == Trophees.Forme.COUPE) setPadding(0, 0, 0, (d * 0.3f).toInt())
                if (metal == null) alpha = 0.55f else elevation = dp(8).toFloat()
                cameraDistance = 8000 * resources.displayMetrics.density
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                if (metal != null && android.animation.ValueAnimator.areAnimatorsEnabled()) {
                    android.animation.ObjectAnimator.ofFloat(this, View.ROTATION_Y, -22f, 22f).apply {
                        duration = 4000
                        repeatCount = android.animation.ValueAnimator.INFINITE
                        repeatMode = android.animation.ValueAnimator.REVERSE
                        interpolator = android.view.animation.AccelerateDecelerateInterpolator()
                        start()
                    }
                }
            })
            addView(View(context).apply {
                // Le piédestal : un socle éclairé d'en haut, avec son ombre.
                layoutParams = LinearLayout.LayoutParams(dp(120), dp(20)).apply { topMargin = dp(10) }
                val encre = Apparence.couleur(context, R.color.ink)
                background = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                    intArrayOf(ColorUtils.setAlphaComponent(encre, 0x22), ColorUtils.setAlphaComponent(encre, 0x55))).apply {
                    cornerRadii = floatArrayOf(dp(6).toFloat(), dp(6).toFloat(), dp(6).toFloat(), dp(6).toFloat(), dp(3).toFloat(), dp(3).toFloat(), dp(3).toFloat(), dp(3).toFloat())
                }
                elevation = dp(4).toFloat()
            })
            addView(VueJour.texte(context, getString(if (meilleure != null) R.string.trophee_vedette else R.string.trophee_vedette_aucune), 12f, R.color.muted, haut = 8).apply { gravity = Gravity.CENTER })
            if (meilleure != null && metal != null) addView(VueJour.texte(context,
                TropheesVue.nom(context, meilleure) + " · " + TropheesVue.metal(context, metal), 14f, R.color.ink, gras = true, haut = 2).apply { gravity = Gravity.CENTER })
            contentDescription = if (meilleure != null && metal != null)
                getString(R.string.trophee_vedette) + ", " + TropheesVue.nom(context, meilleure) + ", " + TropheesVue.metal(context, metal)
            else getString(R.string.trophee_vedette_aucune)
        })
    }

    private fun trophee(f: Trophees.Famille, rang: Int, pr: Trophees.Progres) = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER_HORIZONTAL
        setPadding(dp(4), dp(10), dp(4), dp(10))
        layoutParams = android.widget.GridLayout.LayoutParams(
            android.widget.GridLayout.spec(android.widget.GridLayout.UNDEFINED, 1f),
            android.widget.GridLayout.spec(android.widget.GridLayout.UNDEFINED, 1f)
        ).apply { width = 0 }
        val metal = if (rang > 0) Trophees.metal(f, rang) else null
        val d = dp(70)
        if (metal != null && f.forme == Trophees.Forme.MEDAILLE) addView(ruban(metal, dp(34), dp(22)))
        addView(TextView(context).apply {
            text = f.icone
            textSize = 30f
            gravity = Gravity.CENTER
            setTextColor(metal?.let { tons(it).second } ?: Apparence.couleur(context, R.color.muted))
            layoutParams = LinearLayout.LayoutParams(d, d)
            background = medaille(metal, d, f.forme)
            // Dans la coupe, l'icône se pose dans la vasque, au-dessus du pied.
            if (f.forme == Trophees.Forme.COUPE) setPadding(0, 0, 0, (d * 0.3f).toInt())
            outlineProvider = android.view.ViewOutlineProvider.BACKGROUND
            if (metal != null) { elevation = dp(6).toFloat(); setShadowLayer(2f, 0f, 1f, Color.argb(0x60, 0, 0, 0)) } else alpha = 0.55f
        })
        addView(VueJour.texte(context, TropheesVue.nom(context, f), 13f, if (metal != null) R.color.ink else R.color.muted, gras = true, haut = 8).apply { gravity = Gravity.CENTER })
        addView(VueJour.texte(context,
            if (metal != null) getString(R.string.trophee_niveau, rang, f.seuils.size, TropheesVue.metal(context, metal)) else getString(R.string.trophee_a_venir),
            11f, R.color.brand, gras = true, haut = 2).apply { gravity = Gravity.CENTER })
        // Ce qu'a demandé le niveau atteint ; ou, avant le premier, ce qu'il demande.
        addView(VueJour.texte(context, TropheesVue.description(context, f, f.seuils[(rang - 1).coerceAtLeast(0)]), 11f, R.color.muted, haut = 2).apply { gravity = Gravity.CENTER })
        val sommet = rang >= f.seuils.size
        if (!sommet) {
            addView(barre(pr.fraction, dp(4)).apply {
                (layoutParams as LinearLayout.LayoutParams).apply { topMargin = dp(6); marginStart = dp(10); marginEnd = dp(10) }
            })
            addView(VueJour.texte(context, getString(R.string.trophee_progres, pr.valeur.coerceAtMost(pr.cible), pr.cible), 11f, R.color.muted, haut = 2).apply { gravity = Gravity.CENTER })
        }
        contentDescription = listOfNotNull(TropheesVue.nom(context, f),
            metal?.let { getString(R.string.trophee_niveau, rang, f.seuils.size, TropheesVue.metal(context, it)) },
            if (sommet) getString(R.string.trophee_sommet) else getString(R.string.trophee_prochain, TropheesVue.description(context, f, pr.cible)),
        ).joinToString(", ")
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

/**
 * La coupe, l'écusson et la gemme (12/10) : la même matière que la
 * médaille — l'épaisseur décalée vers le bas, un dégradé qui éclaire en
 * haut à gauche, un reflet — sur une autre silhouette. Les points sont en
 * fractions de la boîte, comme les découpes de la version web.
 */
class FormeTrophee(private val forme: Trophees.Forme, private val clair: Int, private val fonce: Int, private val gagne: Boolean) : android.graphics.drawable.Drawable() {
    private val p = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
    private val points: FloatArray = when (forme) {
        Trophees.Forme.COUPE -> floatArrayOf(.04f, .04f, .96f, .04f, .90f, .38f, .70f, .58f, .58f, .64f, .58f, .78f, .76f, .84f, .76f, .98f,
            .24f, .98f, .24f, .84f, .42f, .78f, .42f, .64f, .30f, .58f, .10f, .38f)
        Trophees.Forme.ECUSSON -> floatArrayOf(.5f, .02f, .96f, .14f, .94f, .58f, .5f, .98f, .06f, .58f, .04f, .14f)
        else -> floatArrayOf(.26f, .04f, .74f, .04f, .98f, .5f, .74f, .96f, .26f, .96f, .02f, .5f)
    }

    private fun chemin(w: Float, h: Float, dx: Float = 0f, dy: Float = 0f) = android.graphics.Path().apply {
        moveTo(points[0] * w + dx, points[1] * h + dy)
        for (i in 2 until points.size step 2) lineTo(points[i] * w + dx, points[i + 1] * h + dy)
        close()
    }

    override fun draw(c: android.graphics.Canvas) {
        val w = bounds.width().toFloat(); val h = bounds.height().toFloat() * 0.95f
        c.save(); c.translate(bounds.left.toFloat(), bounds.top.toFloat())
        val ep = bounds.height() * 0.05f
        p.shader = null
        p.color = if (gagne) ColorUtils.blendARGB(fonce, Color.BLACK, 0.35f) else ColorUtils.setAlphaComponent(fonce, 0x80)
        c.drawPath(chemin(w, h, 0f, ep), p)                                   // l'épaisseur
        val corps = chemin(w, h)
        p.shader = when (forme) {
            Trophees.Forme.COUPE -> android.graphics.LinearGradient(0f, 0f, w, 0f, intArrayOf(fonce, clair, Color.WHITE, clair, fonce),
                floatArrayOf(0f, .35f, .45f, .6f, 1f), android.graphics.Shader.TileMode.CLAMP)
            Trophees.Forme.GEMME -> android.graphics.SweepGradient(w / 2, h / 2, intArrayOf(clair, fonce, clair, fonce, clair, fonce, clair), null)
            else -> android.graphics.RadialGradient(w * .35f, h * .25f, w * .8f, intArrayOf(Color.WHITE, clair, fonce), floatArrayOf(0f, .35f, 1f),
                android.graphics.Shader.TileMode.CLAMP)
        }
        c.drawPath(corps, p)
        // Le reflet, dans la silhouette.
        c.save(); c.clipPath(corps)
        p.shader = android.graphics.LinearGradient(0f, 0f, w * .55f, h * .55f, ColorUtils.setAlphaComponent(Color.WHITE, 0x8C), Color.TRANSPARENT,
            android.graphics.Shader.TileMode.CLAMP)
        c.drawRect(0f, 0f, w, h, p)
        c.restore()
        p.shader = null
        c.restore()
    }

    override fun setAlpha(alpha: Int) { p.alpha = alpha }
    override fun setColorFilter(cf: android.graphics.ColorFilter?) { p.colorFilter = cf }
    @Deprecated("Deprecated in Java")
    override fun getOpacity() = android.graphics.PixelFormat.TRANSLUCENT
}
