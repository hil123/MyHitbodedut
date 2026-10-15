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
 * Les trophées en ligne (15/10, maquette validée) : en tête, le grade (Trophees.grade) et
 * ce qu'il reste pour le suivant ; puis une seule ligne verticale. Au-dessus, les
 * trophées gagnés, du premier au dernier, chacun du matériau du grade d'alors, une
 * bannière à chaque changement de grade ; au milieu, « Tu es ici » et le prochain
 * objectif (l'écran s'ouvre là) ; en dessous, les suivants, en gris. Un nouveau grade
 * se fête une fois, à l'ouverture. Les records restent en bas.
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
        val pratique = Store.pratique(this)
        val faits = Trophees.chemin.filter { Trophees.gagne(pratique, it) }
        val prochain = Trophees.prochain(pratique)
        val futurs = Trophees.chemin.filterNot { Trophees.gagne(pratique, it) || it == prochain }
        val n = faits.size
        val grade = Trophees.grade(n)
        val suivant = Trophees.SEUILS_GRADES.firstOrNull { it > n }

        // Le grade, en tête.
        contenu.addView(VueJour.carte(this).apply {
            addView(LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                addView(medaille(grade, "★", 48))
                addView(LinearLayout(context).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = dp(12) }
                    addView(VueJour.texte(context, getString(R.string.grade_titre, TropheesVue.metal(context, grade)), 18f, R.color.ink, gras = true))
                    addView(VueJour.texte(context, if (suivant != null) getString(R.string.grade_sous, n, suivant - n,
                        TropheesVue.metal(context, Trophees.grade(suivant))) else getString(R.string.grade_sous_max, n), 13f, R.color.muted, haut = 2))
                    if (suivant != null) {
                        val de = Trophees.SEUILS_GRADES.last { it <= n }
                        addView(barre((n - de).toFloat() / (suivant - de), dp(6), grade).apply { (layoutParams as LinearLayout.LayoutParams).topMargin = dp(6) })
                    }
                })
            })
        })

        // La ligne : les gagnés, le prochain, les suivants.
        var ici: View? = null
        contenu.addView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                .apply { setMargins(dp(16), dp(12), dp(16), 0) }
            // Le k-ième trophée (de 1) a le matériau du grade qu'il fait atteindre.
            val passage = { k: Int -> k > 0 && k in Trophees.SEUILS_GRADES }
            faits.forEachIndexed { i, t ->
                if (passage(i + 1)) addView(banniere(Trophees.grade(i + 1), i + 1, atteint = true))
                addView(etape(t, Trophees.grade(i + 1), getString(R.string.trophee_gagne), gagne = true))
            }
            if (prochain != null) {
                if (passage(n + 1)) addView(banniere(Trophees.grade(n + 1), n + 1, atteint = false))
                ici = carteIci(prochain, pratique)
                addView(ici)
            } else addView(VueJour.texte(context, getString(R.string.trophees_tous), 18f, R.color.ink, gras = true, haut = 12))
            futurs.take(MONTRES).forEachIndexed { k, t ->
                val rang = n + 2 + k
                if (passage(rang)) addView(banniere(Trophees.grade(rang), rang, atteint = false))
                val v = Trophees.valeur(pratique, t.famille)
                addView(etape(t, null, if (v > 0) getString(R.string.trophee_progres, v.coerceAtMost(t.seuil), t.seuil) else null, gagne = false))
            }
            if (futurs.size > MONTRES) addView(VueJour.texte(context, getString(R.string.trophees_autres, futurs.size - MONTRES), 13f, R.color.muted, haut = 12)
                .apply { gravity = Gravity.CENTER })
        })

        // Les records.
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

        // L'écran s'ouvre sur « Tu es ici ».
        val defile = findViewById<android.widget.ScrollView>(R.id.paliers_defile)
        ici?.let { v -> defile.post { defile.scrollTo(0, (v.top + (v.parent as View).top - defile.height / 3).coerceAtLeast(0)) } }

        // Un nouveau grade : la fête, une fois.
        val niveauGrade = if (n == 0) 0 else grade.ordinal + 1 // 0 sans trophée, 1 bronze, 2 argent…
        val vu = Store.gradeVu(this)
        if (vu != null && niveauGrade > vu && grade != Trophees.Metal.BRONZE) fete(grade, n)
        Store.setGradeVu(this, niveauGrade)
        // Ce qui est vu ici est acquis : l'accueil ne le refête pas.
        Store.setPaliersVus(this, Paliers.marques(Paliers.etats(bilan), bilan))
        Store.setTropheesVus(this, Trophees.gagnes(pratique).map { it.cle }.toSet())
    }

    /** Une étape de la ligne : le trait, la médaille, le texte. */
    private fun etape(t: Trophees.Niveau, metal: Trophees.Metal?, sous: String?, gagne: Boolean) = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        addView(android.widget.FrameLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(dp(60), dp(72))
            addView(View(context).apply { background = trait(metal) },
                android.widget.FrameLayout.LayoutParams(dp(6), ViewGroup.LayoutParams.MATCH_PARENT, Gravity.CENTER_HORIZONTAL))
            addView(View(context).apply {
                background = TropheeDessin(context, t.famille.forme, metal, t.famille.icone)
                if (metal == null) alpha = 0.5f
            }, android.widget.FrameLayout.LayoutParams(dp(if (gagne) 52 else 44), dp(if (gagne) 52 else 44), Gravity.CENTER))
        })
        addView(LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = dp(12) }
            addView(VueJour.texte(context, TropheesVue.description(context, t.famille, t.seuil), 15f,
                if (gagne) R.color.ink else R.color.muted, gras = gagne))
            sous?.let { addView(VueJour.texte(context, it, 12f, R.color.muted, haut = 2)) }
        })
        contentDescription = TropheesVue.description(context, t.famille, t.seuil) + (sous?.let { ", $it" } ?: "")
    }

    /** « Tu es ici » : le prochain objectif, sa barre et ce qu'il reste. */
    private fun carteIci(t: Trophees.Niveau, p: Trophees.Pratique) = VueJour.carte(this).apply {
        val brand = Apparence.couleur(context, R.color.brand)
        (layoutParams as? ViewGroup.MarginLayoutParams)?.setMargins(0, dp(8), 0, dp(8))
        background = GradientDrawable().apply {
            cornerRadius = dp(18).toFloat()
            setColor(Apparence.couleur(context, R.color.card))
            setStroke(dp(2), brand)
        }
        addView(VueJour.texte(context, getString(R.string.trophees_ici), 11f, R.color.brand, gras = true).apply { letterSpacing = 0.06f })
        addView(LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(8) }
            addView(View(context).apply {
                background = TropheeDessin(context, t.famille.forme, null, t.famille.icone)
                alpha = 0.7f
            }, LinearLayout.LayoutParams(dp(64), dp(64)))
            addView(LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = dp(12) }
                addView(VueJour.texte(context, TropheesVue.description(context, t.famille, t.seuil), 19f, R.color.ink, gras = true))
                addView(barre(Trophees.fraction(p, t), dp(10), null).apply { (layoutParams as LinearLayout.LayoutParams).topMargin = dp(10) })
                val v = Trophees.valeur(p, t.famille).coerceAtMost(t.seuil)
                addView(VueJour.texte(context, getString(R.string.trophee_encore, v, t.seuil, t.seuil - v), 14f, R.color.brand, gras = true, haut = 4))
            })
        })
    }

    /** La bannière d'un grade : vive quand il est atteint, grise quand il est à venir. */
    private fun banniere(g: Trophees.Metal, rang: Int, atteint: Boolean) = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(dp(12), dp(8), dp(12), dp(8))
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { setMargins(0, dp(6), 0, dp(6)) }
        val (clair, fonce, _) = TropheeDessin.teintes(g)
        background = GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
            if (atteint) intArrayOf(clair, fonce) else intArrayOf(0x1AFFFFFF, 0x1AFFFFFF)).apply { cornerRadius = dp(14).toFloat() }
        addView(medaille(if (atteint) g else null, "★", 34))
        addView(LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = dp(10) }
            val encre = if (atteint) Color.rgb(0x1A, 0x13, 0x06) else Apparence.couleur(context, R.color.muted)
            addView(TextView(context).apply {
                text = getString(if (atteint) R.string.grade_atteint else R.string.grade_titre, TropheesVue.metal(context, g))
                textSize = 15f; setTypeface(typeface, Typeface.BOLD); setTextColor(encre)
            })
            addView(TextView(context).apply { text = getString(R.string.grade_au, rang); textSize = 12f; setTextColor(encre); alpha = 0.75f })
        })
    }

    /** La fête d'un nouveau grade. */
    private fun fete(g: Trophees.Metal, n: Int) {
        val suivant = Trophees.SEUILS_GRADES.firstOrNull { it > n }
        val vue = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(24), dp(24), dp(24), dp(8))
            addView(medaille(g, "★", 140))
            addView(VueJour.texte(context, getString(R.string.grade_nouveau, TropheesVue.metal(context, g)), 24f, R.color.ink, gras = true, haut = 14)
                .apply { gravity = Gravity.CENTER })
            addView(VueJour.texte(context, getString(R.string.grade_sous_max, n), 15f, R.color.muted, haut = 4).apply { gravity = Gravity.CENTER })
            suivant?.let {
                addView(VueJour.texte(context, getString(R.string.grade_prochain, TropheesVue.metal(context, Trophees.grade(it)), it), 13f, R.color.muted, haut = 6)
                    .apply { gravity = Gravity.CENTER })
            }
        }
        com.google.android.material.dialog.MaterialAlertDialogBuilder(this).setView(vue)
            .setPositiveButton(R.string.celebration_continue, null).show()
    }

    private fun medaille(m: Trophees.Metal?, icone: String, taille: Int) = View(this).apply {
        layoutParams = LinearLayout.LayoutParams(dp(taille), dp(taille))
        background = TropheeDessin(context, Trophees.Forme.MEDAILLE, m, icone)
        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
    }

    /** Le trait de la ligne : du matériau du grade, ou gris à venir. */
    private fun trait(m: Trophees.Metal?) = if (m == null) GradientDrawable().apply {
        setColor(ColorUtils.setAlphaComponent(Apparence.couleur(this@PaliersActivity, R.color.muted), 0x40)); cornerRadius = dp(3).toFloat()
    } else TropheeDessin.teintes(m).let { (clair, fonce, _) ->
        GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(clair, fonce)).apply { cornerRadius = dp(3).toFloat() }
    }

    private fun barre(fraction: Float, hauteur: Int, m: Trophees.Metal?) = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
        max = 1000
        progress = (fraction.coerceIn(0f, 1f) * 1000).toInt()
        val couleur = m?.let { TropheeDessin.teintes(it).second } ?: Apparence.couleur(context, R.color.brand)
        progressTintList = android.content.res.ColorStateList.valueOf(couleur)
        progressBackgroundTintList = android.content.res.ColorStateList.valueOf(ColorUtils.setAlphaComponent(couleur, 0x30))
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, hauteur)
        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private companion object {
        /** Les trophées à venir montrés sous « Tu es ici » ; le reste est annoncé d'une ligne. */
        const val MONTRES = 15
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

