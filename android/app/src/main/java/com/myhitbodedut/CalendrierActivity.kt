package com.myhitbodedut

import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.ColorUtils
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/**
 * Le calendrier (demande du 10/10) : un mois, du dimanche au samedi, chaque
 * case avec le quantième civil et le quantième hébreu ; Chabbat et fêtes
 * teintés, les fêtes marquées d'un point. Le jour touché montre sa carte et
 * ses horaires sous la grille. La grille vient de JourAffiche.semaines.
 */
class CalendrierActivity : AppCompatActivity() {

    private val choixLieu = ChoixLieu(this) { afficher() }
    private var mois: YearMonth = YearMonth.now()
    private var choisi: LocalDate = LocalDate.now()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calendrier)
        savedInstanceState?.getString(ETAT_JOUR)?.let { choisi = LocalDate.parse(it); mois = YearMonth.from(choisi) }
        afficher()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(ETAT_JOUR, choisi.toString())
    }

    private fun afficher() {
        val israel = Store.inIsrael(this)
        val langue = ChizukCard.language(this)
        val locale = Locale.forLanguageTag(langue)
        val rtl = resources.configuration.layoutDirection == View.LAYOUT_DIRECTION_RTL
        val aujourdhui = LocalDate.now()
        val contenu = findViewById<LinearLayout>(R.id.calendrier_contenu)
        contenu.removeAllViews()

        val carte = VueJour.carte(this)
        val semaines = JourAffiche.semaines(mois)
        val jours = semaines.flatten().filterNotNull()
        val premier = CalendrierJuif.jour(jours.first(), israel).moisEtAnnee
        val dernier = CalendrierJuif.jour(jours.last(), israel).moisEtAnnee

        // L'en-tête : mois précédent, le mois civil et ses mois hébreux, mois suivant.
        val tete = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        tete.addView(fleche(if (rtl) "›" else "‹", R.string.calendrier_precedent) { mois = mois.minusMonths(1); afficher() })
        tete.addView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            addView(VueJour.texte(this@CalendrierActivity, mois.format(DateTimeFormatter.ofPattern("LLLL yyyy", locale)), 17f,
                R.color.ink, gras = true).apply { gravity = Gravity.CENTER })
            addView(VueJour.texte(this@CalendrierActivity, if (premier == dernier) premier else "$premier – $dernier", 15f,
                R.color.brand).apply { gravity = Gravity.CENTER; PoliceHebraique.appliquer(this) })
        })
        tete.addView(fleche(if (rtl) "‹" else "›", R.string.calendrier_suivant) { mois = mois.plusMonths(1); afficher() })
        carte.addView(tete)

        // Les initiales, du dimanche au samedi.
        carte.addView(rangee().apply {
            (0 until 7).forEach { i ->
                val jour = DayOfWeek.SUNDAY.plus(i.toLong())
                addView(case().apply {
                    text = jour.getDisplayName(TextStyle.NARROW, locale)
                    textSize = 12f
                    setTextColor(Apparence.couleur(context, R.color.muted))
                })
            }
        })
        val brand = Apparence.couleur(this, R.color.brand)
        semaines.forEach { semaine ->
            carte.addView(rangee().apply {
                semaine.forEach { d ->
                    addView(case().apply {
                        if (d == null) return@apply
                        val j = CalendrierJuif.jour(d, israel, Store.mukafChoma(this@CalendrierActivity))
                        val point = if (j.fetes.isNotEmpty()) " •" else ""
                        text = "${d.dayOfMonth}$point\n${j.jourEnLettres}"
                        textSize = 14f
                        setTextColor(Apparence.couleur(context, if (d == aujourdhui) R.color.brand else R.color.ink))
                        if (d == aujourdhui) setTypeface(typeface, Typeface.BOLD)
                        background = GradientDrawable().apply {
                            cornerRadius = dp(10).toFloat()
                            setColor(if (j.chome) ColorUtils.setAlphaComponent(brand, 0x1A) else 0)
                            if (d == choisi) setStroke(dp(2), brand)
                        }
                        contentDescription = listOf(d.toString(), j.dateHebraique).plus(
                            j.fetes.mapNotNull { Libelles.res("fete_$it")?.let(::getString) }).joinToString(" · ")
                        setOnClickListener { choisi = d; afficher() }
                    })
                }
            })
        }
        carte.addView(VueJour.texte(this, getString(R.string.calendrier_aujourdhui), 15f, R.color.brand, gras = true, haut = 10).apply {
            gravity = Gravity.CENTER
            setPadding(0, dp(6), 0, dp(6))
            setOnClickListener { choisi = aujourdhui; mois = YearMonth.from(aujourdhui); afficher() }
        })
        contenu.addView(carte)
        contenu.addView(VueJour.carteJour(this, choisi, israel))
        contenu.addView(VueJour.horaires(this, choisi, israel) { choixLieu.montrer() })
        contenu.addView(VueJour.pied(this))
    }

    private fun rangee() = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            .apply { topMargin = dp(4) }
    }

    private fun case() = TextView(this).apply {
        gravity = Gravity.CENTER
        layoutParams = LinearLayout.LayoutParams(0, dp(48), 1f).apply { marginStart = dp(2); marginEnd = dp(2) }
        setLineSpacing(0f, 0.95f)
    }

    private fun fleche(signe: String, description: Int, action: () -> Unit) = TextView(this).apply {
        text = signe
        textSize = 26f
        gravity = Gravity.CENTER
        setTextColor(Apparence.couleur(context, R.color.brand))
        contentDescription = getString(description)
        layoutParams = LinearLayout.LayoutParams(dp(48), dp(48))
        setOnClickListener { action() }
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private companion object {
        const val ETAT_JOUR = "jour"
    }
}
