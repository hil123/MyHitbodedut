package com.myhitbodedut

import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.time.LocalDate
import java.time.YearMonth

/**
 * Ce qu'on peut dire d'un parcours, sans appareil (proposition 6, 19/10) : les
 * jours de hitbodedout, le temps, la moyenne, les sujets les plus fréquents et
 * l'humeur avant et après. Le web en calcule autant (regles.js, Parcours).
 */
object Parcours {

    data class Bilan(val jours: Int, val minutes: Int, val sujets: List<Pair<Theme, Int>>) {
        /** Les minutes par jour de hitbodedout, arrondies ; 0 sans aucun jour. */
        val moyenne: Int get() = if (jours == 0) 0 else Math.round(minutes.toDouble() / jours).toInt()
    }

    fun bilan(jours: Collection<DayRecord>): Bilan {
        val faits = jours.filter { it.faite }
        return Bilan(faits.size, faits.sumOf { it.minutes }, ThemeRules.frequents(jours.flatMap { it.themes }, max = 5))
    }

    /** L'humeur moyenne avant et après (1 à 5), sur les jours où les deux sont notées ; null sinon. */
    fun humeur(plus: Collection<JournalPlus.Plus>): Triple<Int, Int, Int>? {
        val notes = plus.filter { it.avant > 0 && it.apres > 0 }
        if (notes.isEmpty()) return null
        return Triple(
            Math.round(notes.map { it.avant }.average()).toInt(),
            Math.round(notes.map { it.apres }.average()).toInt(),
            notes.size,
        )
    }
}

/** « Mon parcours » : une page qui rassemble ce que l'accueil ne montre qu'en partie. */
class ParcoursActivity : AppCompatActivity() {

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val jours = Store.days(this)
        val bilan = Parcours.bilan(jours.values)
        val humeur = Parcours.humeur(jours.keys.map { JournalPlus.lire(this, it) })

        val contenu = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(4), dp(16), dp(24))
        }
        val racine = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(bandeau())
            addView(ScrollView(this@ParcoursActivity).apply {
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
                clipToPadding = false
                addView(contenu)
            })
        }
        setContentView(racine)

        // Les chiffres.
        contenu.addView(carte().apply {
            ligne(this, R.string.parcours_serie, resources.getQuantityString(R.plurals.parcours_jours, Store.streak(this@ParcoursActivity), Store.streak(this@ParcoursActivity)))
            ligne(this, R.string.parcours_meilleure, resources.getQuantityString(R.plurals.parcours_jours, Store.meilleureSerie(this@ParcoursActivity), Store.meilleureSerie(this@ParcoursActivity)))
            ligne(this, R.string.parcours_jours_faits, bilan.jours.toString())
            ligne(this, R.string.parcours_total, Store.formatDuration(this@ParcoursActivity, bilan.minutes))
            ligne(this, R.string.parcours_moyenne, Store.formatDuration(this@ParcoursActivity, bilan.moyenne))
        })

        // Le mois, en barres.
        contenu.addView(titre(R.string.parcours_mois))
        contenu.addView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(R.drawable.bg_hero)
            setPadding(dp(16), dp(16), dp(16), dp(12))
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(8) }
            addView(JournalPlus.Graphique(this@ParcoursActivity, JournalPlus.minutesDuMois(jours, YearMonth.now()), LocalDate.now().dayOfMonth))
        })

        // Les sujets les plus fréquents.
        contenu.addView(titre(R.string.parcours_sujets))
        contenu.addView(carte().apply {
            if (bilan.sujets.isEmpty()) addView(texte(getString(R.string.parcours_sujets_vide), R.color.muted))
            else bilan.sujets.forEach { (t, n) -> ligne(this, t.titre, resources.getQuantityString(R.plurals.parcours_fois, n, n)) }
        })

        // L'humeur, quand elle a été notée.
        if (humeur != null) {
            contenu.addView(titre(R.string.parcours_humeur))
            contenu.addView(carte().apply {
                addView(texte(
                    getString(R.string.parcours_humeur_moyenne, JournalPlus.HUMEURS[humeur.first - 1], JournalPlus.HUMEURS[humeur.second - 1]) +
                        "\n" + resources.getQuantityString(R.plurals.parcours_humeur_jours, humeur.third, humeur.third),
                    R.color.ink,
                ))
            })
        }
    }

    private fun bandeau(): View = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        tag = "bandeau"
        setBackgroundResource(R.drawable.bg_hero)
        setPadding(dp(20), dp(24), dp(20), dp(24))
        addView(TextView(this@ParcoursActivity).apply {
            setText(R.string.parcours_title)
            setTextColor(getColor(R.color.white))
            textSize = 26f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        })
        addView(TextView(this@ParcoursActivity).apply {
            setText(R.string.parcours_hint)
            setTextColor(Apparence.couleur(this@ParcoursActivity, R.color.mint))
            textSize = 14f
            setPadding(0, dp(4), 0, 0)
        })
    }

    private fun carte() = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setBackgroundResource(R.drawable.bg_card)
        setPadding(dp(16), dp(8), dp(16), dp(8))
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(12) }
    }

    private fun titre(res: Int) = TextView(this).apply {
        setText(res)
        isAllCaps = true
        letterSpacing = 0.08f
        textSize = 12f
        setTextColor(Apparence.couleur(this@ParcoursActivity, R.color.muted))
        setPadding(dp(4), dp(20), dp(4), 0)
    }

    private fun texte(t: String, couleur: Int) = TextView(this).apply {
        text = t
        textSize = 15f
        setTextColor(Apparence.couleur(this@ParcoursActivity, couleur))
        setPadding(0, dp(8), 0, dp(8))
    }

    private fun ligne(parent: LinearLayout, libelle: Int, valeur: String) {
        parent.addView(LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(8), 0, dp(8))
            addView(TextView(this@ParcoursActivity).apply {
                setText(libelle)
                textSize = 15f
                setTextColor(Apparence.couleur(this@ParcoursActivity, R.color.ink))
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            })
            addView(TextView(this@ParcoursActivity).apply {
                text = valeur
                textSize = 15f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                setTextColor(Apparence.couleur(this@ParcoursActivity, R.color.brand))
            })
        })
    }
}
