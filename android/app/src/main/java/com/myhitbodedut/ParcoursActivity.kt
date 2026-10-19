package com.myhitbodedut

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
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
    private fun couleur(res: Int) = Apparence.couleur(this, res)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_parcours)
        val jours = Store.days(this)
        val bilan = Parcours.bilan(jours.values)
        val humeur = Parcours.humeur(jours.keys.map { JournalPlus.lire(this, it) })
        val contenu = findViewById<LinearLayout>(R.id.parcours_contenu).apply { setPadding(dp(16), dp(8), dp(16), dp(24)) }

        // Les chiffres, en quatre tuiles.
        val serie = Store.streak(this)
        val meilleure = Store.meilleureSerie(this)
        contenu.addView(rangee(
            tuile(serie.toString(), resources.getQuantityString(R.plurals.parcours_unite_jours, serie), R.string.parcours_serie, R.color.ring_streak),
            tuile(meilleure.toString(), resources.getQuantityString(R.plurals.parcours_unite_jours, meilleure), R.string.parcours_meilleure, R.color.ring_streak),
        ))
        contenu.addView(rangee(
            tuile(bilan.jours.toString(), "", R.string.parcours_jours_faits, R.color.ring_week),
            tuile(Store.formatDuration(this, bilan.minutes), "", R.string.parcours_total, R.color.brand),
        ))
        if (bilan.jours > 0) contenu.addView(TextView(this).apply {
            text = getString(R.string.parcours_moyenne_ligne, Store.formatDuration(this@ParcoursActivity, bilan.moyenne))
            textSize = 13f
            gravity = Gravity.CENTER
            setTextColor(couleur(R.color.muted))
            setPadding(0, dp(10), 0, 0)
        })

        // Le mois, en barres, sur le vert du bandeau.
        val mois = JournalPlus.minutesDuMois(jours, YearMonth.now())
        contenu.addView(titre(R.string.parcours_mois))
        contenu.addView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(R.drawable.bg_carte_sombre)
            setPadding(dp(16), dp(14), dp(16), dp(14))
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(8) }
            addView(TextView(this@ParcoursActivity).apply {
                val total = mois.sum()
                val n = mois.count { it > 0 }
                text = if (total == 0) getString(R.string.parcours_mois_vide) else resources.getQuantityString(R.plurals.parcours_mois_total, n, Store.formatDuration(this@ParcoursActivity, total), n)
                textSize = 13f
                setTextColor(couleur(R.color.mint))
                setPadding(0, 0, 0, dp(10))
            })
            addView(JournalPlus.Graphique(this@ParcoursActivity, mois, LocalDate.now().dayOfMonth))
        })

        // Les sujets les plus fréquents, en barres.
        contenu.addView(titre(R.string.parcours_sujets))
        contenu.addView(carte().apply {
            if (bilan.sujets.isEmpty()) addView(texte(getString(R.string.parcours_sujets_vide), R.color.muted))
            else {
                val max = bilan.sujets.maxOf { it.second }
                bilan.sujets.forEach { (t, n) -> addView(barreSujet(getString(t.titre), resources.getQuantityString(R.plurals.parcours_fois, n, n), n.toFloat() / max)) }
            }
        })

        // L'humeur, quand elle a été notée : grande, avant → après.
        if (humeur != null) {
            contenu.addView(titre(R.string.parcours_humeur))
            contenu.addView(carte().apply {
                gravity = Gravity.CENTER_HORIZONTAL
                addView(TextView(this@ParcoursActivity).apply {
                    text = "${JournalPlus.HUMEURS[humeur.first - 1]}  →  ${JournalPlus.HUMEURS[humeur.second - 1]}"
                    textSize = 36f
                    gravity = Gravity.CENTER
                })
                addView(texte(getString(R.string.parcours_humeur_legende) +
                    " · " + resources.getQuantityString(R.plurals.parcours_humeur_jours, humeur.third, humeur.third), R.color.muted).apply { gravity = Gravity.CENTER })
            })
        }
        // Les effets (19/10) : les blocs entrent l'un après l'autre.
        Effets.enfants(contenu)
    }

    private fun rangee(a: View, b: View) = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(10) }
        addView(a, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f).apply { marginEnd = dp(5) })
        addView(b, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f).apply { marginStart = dp(5) })
    }

    /** Une tuile : le chiffre en grand, son unité, puis ce qu'il compte ; une pastille de la couleur de son anneau. */
    private fun tuile(valeur: String, unite: String, libelle: Int, teinte: Int) = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setBackgroundResource(R.drawable.bg_card)
        setPadding(dp(16), dp(14), dp(16), dp(14))
        addView(View(this@ParcoursActivity).apply {
            background = android.graphics.drawable.GradientDrawable().apply { cornerRadius = dp(3).toFloat(); setColor(couleur(teinte)) }
            layoutParams = LinearLayout.LayoutParams(dp(22), dp(5)).apply { bottomMargin = dp(10) }
        })
        addView(LinearLayout(this@ParcoursActivity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.BOTTOM
            addView(TextView(this@ParcoursActivity).apply {
                text = valeur
                // Un chiffre seul monte jusqu'à sa valeur ; une durée (« 1 h 10 ») s'affiche telle quelle.
                valeur.toIntOrNull()?.let { Effets.compte(this, it) }
                textSize = 28f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                setTextColor(couleur(R.color.ink))
                maxLines = 1
            })
            if (unite.isNotBlank()) addView(TextView(this@ParcoursActivity).apply {
                text = " $unite"
                textSize = 14f
                setTextColor(couleur(R.color.muted))
                setPadding(0, 0, 0, dp(5))
            })
        })
        addView(TextView(this@ParcoursActivity).apply {
            setText(libelle)
            textSize = 13f
            setTextColor(couleur(R.color.muted))
        })
    }

    private fun barreSujet(nom: String, fois: String, part: Float) = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(0, dp(8), 0, dp(8))
        addView(LinearLayout(this@ParcoursActivity).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(TextView(this@ParcoursActivity).apply {
                text = nom
                textSize = 15f
                setTextColor(couleur(R.color.ink))
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            })
            addView(TextView(this@ParcoursActivity).apply {
                text = fois
                textSize = 13f
                setTextColor(couleur(R.color.muted))
            })
        })
        addView(Barre(this@ParcoursActivity, part, couleur(R.color.brand), couleur(R.color.line)).apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(8)).apply { topMargin = dp(6) }
        })
    }

    /** Une barre arrondie : la part remplie, sur sa piste. */
    private class Barre(c: Context, private val part: Float, plein: Int, piste: Int) : View(c) {
        private val pPlein = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = plein }
        private val pPiste = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = piste }
        /** La barre se remplit à l'ouverture (19/10, effets). */
        private var avance = 0f
        init { Effets.remplir { avance = it; invalidate() } }
        override fun onDraw(canvas: Canvas) {
            val r = height / 2f
            canvas.drawRoundRect(RectF(0f, 0f, width.toFloat(), height.toFloat()), r, r, pPiste)
            val w = (width * part.coerceIn(0.04f, 1f) * avance)
            if (w <= 0f) return
            val x = if (layoutDirection == LAYOUT_DIRECTION_RTL) width - w else 0f
            canvas.drawRoundRect(RectF(x, 0f, x + w, height.toFloat()), r, r, pPlein)
        }
    }

    private fun carte() = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setBackgroundResource(R.drawable.bg_card)
        setPadding(dp(16), dp(8), dp(16), dp(8))
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(8) }
    }

    private fun titre(res: Int) = TextView(this).apply {
        setText(res)
        isAllCaps = true
        letterSpacing = 0.08f
        textSize = 12f
        setTextColor(couleur(R.color.muted))
        setPadding(dp(4), dp(22), dp(4), 0)
    }

    private fun texte(t: String, c: Int) = TextView(this).apply {
        text = t
        textSize = 14f
        setTextColor(couleur(c))
        setPadding(0, dp(8), 0, dp(8))
    }
}
