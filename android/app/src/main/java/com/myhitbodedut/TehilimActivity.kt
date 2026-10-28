package com.myhitbodedut

import android.content.Context
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.time.LocalDate
import java.time.format.TextStyle

/**
 * Le livre de Tehilim (demande du 04/10).
 *
 * En haut, la portion du jour, des deux façons de dire tout le livre : en
 * un mois hébraïque ou en une semaine. Dessous, les 150 psaumes, un
 * toucher chacun. Les numéros suivent la langue : lettres en hébreu,
 * chiffres sinon, comme les références.
 */
class TehilimActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tehilim)
        val livre = Data.tehilim(this)
        findViewById<TextView>(R.id.tehilim_count).text =
            getString(
                R.string.tehilim_count, Tehilim.PSAUMES,
                // « 2,527 » en anglais, « 2 527 » en français : le séparateur de la langue.
                java.text.NumberFormat.getIntegerInstance(resources.configuration.locales[0]).format(livre.versetsTotal)
            )
        construireGrille()
    }

    override fun onResume() {
        super.onResume()
        // Le jour a pu changer depuis l'ouverture de l'écran.
        renderDuJour()
        // Les marque-pages des Tehilim (28/10), en tête : la même feuille que la Bibliothèque.
        val carte = findViewById<View>(R.id.tehilim_month_button).parent as View
        val contenu = carte.parent as android.view.ViewGroup
        signets?.let { contenu.removeView(it) }
        signets = RepriseFavoris.ligneSignets(this, "t")?.also { contenu.addView(it, contenu.indexOfChild(carte)) }
    }

    private var signets: View? = null

    private fun renderDuJour() {
        val aujourdhui = LocalDate.now()
        val (jour, longueur) = CycleTehilim.jourHebreu(aujourdhui)
        val duMois = CycleTehilim.duMois(jour, longueur)
        findViewById<TextView>(R.id.tehilim_month_state).text = getString(
            R.string.tehilim_month_state, TehilimTexte.nombre(this, jour), TehilimTexte.portion(this, duMois)
        )
        findViewById<View>(R.id.tehilim_month_button).setOnClickListener {
            TehilimLectureActivity.ouvrir(this, duMois)
        }

        val deLaSemaine = CycleTehilim.deLaSemaine(aujourdhui.dayOfWeek)
        val nomDuJour = aujourdhui.dayOfWeek
            .getDisplayName(TextStyle.FULL, resources.configuration.locales[0])
            .replaceFirstChar { it.titlecase(resources.configuration.locales[0]) }
        findViewById<TextView>(R.id.tehilim_week_state).text = getString(
            R.string.tehilim_week_state, nomDuJour, TehilimTexte.portion(this, deLaSemaine)
        )
        findViewById<View>(R.id.tehilim_week_button).setOnClickListener {
            TehilimLectureActivity.ouvrir(this, deLaSemaine)
        }
    }

    /** Cinq psaumes par ligne, chacun une case comme celles de la semaine. */
    private fun construireGrille() {
        val grille = findViewById<LinearLayout>(R.id.tehilim_grid)
        val parLigne = 5
        // L'ondulation du toucher, celle des autres lignes de l'application.
        val ondulation = android.util.TypedValue().also {
            theme.resolveAttribute(android.R.attr.selectableItemBackground, it, true)
        }.resourceId
        (1..Tehilim.PSAUMES).chunked(parLigne).forEach { numeros ->
            val ligne = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            numeros.forEach { n ->
                ligne.addView(TextView(this).apply {
                    layoutParams = LinearLayout.LayoutParams(0, dp(48), 1f).apply {
                        setMargins(dp(4), dp(4), dp(4), dp(4))
                    }
                    gravity = Gravity.CENTER
                    text = TehilimTexte.nombre(this@TehilimActivity, n)
                    textSize = 16f
                    fontFeatureSettings = "tnum"
                    setTextColor(Apparence.couleur(context, R.color.ink))
                    setBackgroundResource(R.drawable.bg_day_todo)
                    contentDescription = TehilimTexte.portion(this@TehilimActivity, Portion(n, n))
                    isClickable = true
                    isFocusable = true
                    foreground = getDrawable(ondulation)
                    setOnClickListener { TehilimLectureActivity.ouvrir(this@TehilimActivity, Portion(n, n)) }
                })
            }
            grille.addView(ligne)
        }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}

/** Les numéros et les titres de passages, dans la langue de l'interface. */
object TehilimTexte {

    fun nombre(context: Context, n: Int): String =
        if (ChizukCard.language(context) == "he") Reference.lettres(n) else n.toString()

    fun portion(context: Context, p: Portion): String = when {
        p.partielle -> context.getString(
            R.string.tehilim_range_verses,
            nombre(context, p.debut), nombre(context, p.debutVerset), nombre(context, p.finVerset ?: 0)
        )
        p.debut == p.fin -> context.getString(R.string.tehilim_single, nombre(context, p.debut))
        else -> context.getString(R.string.tehilim_range, nombre(context, p.debut), nombre(context, p.fin))
    }
}
