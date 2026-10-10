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
 * case avec le quantième hébreu et le quantième civil ; Chabbat et fêtes
 * teintés, les fêtes marquées d'un point. Le mois hébreu est la vue
 * principale, le mois civil au choix (Store.calendrierCivil) ; le quantième
 * principal est en haut de la case. Le jour touché montre sa carte et ses
 * horaires sous la grille. Les grilles viennent de JourAffiche.
 */
class CalendrierActivity : AppCompatActivity() {

    private val choixLieu = ChoixLieu(this) { afficher() }
    private var mois: YearMonth = YearMonth.now()
    private var choisi: LocalDate = LocalDate.now()
    /** Le 1er du mois hébreu affiché ; null : celui du jour choisi. */
    private var moisHebreu: LocalDate? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calendrier)
        savedInstanceState?.getString(ETAT_JOUR)?.let { choisi = LocalDate.parse(it); mois = YearMonth.from(choisi) }
        savedInstanceState?.getString(ETAT_MOIS_HEBREU)?.let { moisHebreu = LocalDate.parse(it) }
        afficher()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(ETAT_JOUR, choisi.toString())
        moisHebreu?.let { outState.putString(ETAT_MOIS_HEBREU, it.toString()) }
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
        val civil = Store.calendrierCivil(this)
        val debutHebreu = moisHebreu ?: JourAffiche.debutMoisHebreu(choisi)
        val semaines = if (civil) JourAffiche.semaines(mois) else JourAffiche.semainesHebreu(debutHebreu)
        val jours = semaines.flatten().filterNotNull()

        // La vue : le mois hébreu, ou le mois civil.
        carte.addView(LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                .apply { gravity = Gravity.CENTER_HORIZONTAL; bottomMargin = dp(8) }
            setPadding(dp(3), dp(3), dp(3), dp(3))
            background = GradientDrawable().apply {
                cornerRadius = dp(999).toFloat()
                setColor(ColorUtils.setAlphaComponent(Apparence.couleur(context, R.color.brand), 0x17))
            }
            contentDescription = getString(R.string.calendrier_vue)
            addView(choixVue(R.string.calendrier_vue_hebreu, !civil) { basculer(false) })
            addView(choixVue(R.string.calendrier_vue_civil, civil) { basculer(true) })
        })

        // L'en-tête : mois précédent, le mois principal et l'autre calendrier, mois suivant.
        val (titre, sousTitre) = if (civil) {
            val premier = CalendrierJuif.jour(jours.first(), israel).moisEtAnnee
            val dernier = CalendrierJuif.jour(jours.last(), israel).moisEtAnnee
            initiale(mois.format(DateTimeFormatter.ofPattern("LLLL yyyy", locale)), locale) to
                (if (premier == dernier) premier else "$premier – $dernier")
        } else {
            val d0 = jours.first()
            val d1 = jours.last()
            val debutCivil = d0.format(DateTimeFormatter.ofPattern(if (d0.year == d1.year) "LLLL" else "LLLL yyyy", locale))
            CalendrierJuif.jour(debutHebreu, israel).moisEtAnnee to
                initiale("$debutCivil – ${d1.format(DateTimeFormatter.ofPattern("LLLL yyyy", locale))}", locale)
        }
        val tete = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        tete.addView(fleche(if (rtl) "›" else "‹", R.string.calendrier_precedent) { decaler(civil, debutHebreu, -1) })
        tete.addView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            addView(VueJour.texte(this@CalendrierActivity, titre, if (civil) 17f else 20f, R.color.ink, gras = true).apply {
                gravity = Gravity.CENTER
                if (!civil) PoliceHebraique.appliquer(this)
            })
            addView(VueJour.texte(this@CalendrierActivity, sousTitre, 15f, R.color.brand).apply {
                gravity = Gravity.CENTER
                if (civil) PoliceHebraique.appliquer(this)
            })
        })
        tete.addView(fleche(if (rtl) "‹" else "›", R.string.calendrier_suivant) { decaler(civil, debutHebreu, 1) })
        carte.addView(tete)

        // Les initiales, du dimanche au samedi.
        carte.addView(rangee().apply {
            (0 until 7).forEach { i ->
                val jour = DayOfWeek.SUNDAY.plus(i.toLong())
                addView(TextView(this@CalendrierActivity).apply {
                    gravity = Gravity.CENTER
                    layoutParams = LinearLayout.LayoutParams(0, dp(24), 1f).apply { marginStart = dp(2); marginEnd = dp(2) }
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
                        // Le 1er d'un mois civil porte le nom du mois, pour qu'on lise où il bascule.
                        val quantieme = if (!civil && d.dayOfMonth == 1)
                            "1 " + d.format(DateTimeFormatter.ofPattern("LLL", locale)) else d.dayOfMonth.toString()
                        val haut = TextView(context).apply {
                            text = (if (civil) quantieme else j.jourEnLettres) + point
                            textSize = if (civil) 15f else 17f
                            gravity = Gravity.CENTER
                            setTextColor(Apparence.couleur(context, if (d == aujourdhui) R.color.brand else R.color.ink))
                            if (d == aujourdhui) setTypeface(typeface, Typeface.BOLD)
                            if (!civil) PoliceHebraique.appliquer(this)
                        }
                        val bas = TextView(context).apply {
                            text = if (civil) j.jourEnLettres else quantieme
                            textSize = 11f
                            gravity = Gravity.CENTER
                            maxLines = 1
                            setTextColor(Apparence.couleur(context, R.color.muted))
                            if (civil) PoliceHebraique.appliquer(this)
                        }
                        addView(haut)
                        addView(bas)
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
            setOnClickListener { choisi = aujourdhui; mois = YearMonth.from(aujourdhui); moisHebreu = null; afficher() }
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

    private fun case() = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER
        layoutParams = LinearLayout.LayoutParams(0, dp(52), 1f).apply { marginStart = dp(2); marginEnd = dp(2) }
    }

    private fun choixVue(texte: Int, actif: Boolean, action: () -> Unit) = TextView(this).apply {
        setText(texte)
        textSize = 14f
        setPadding(dp(16), dp(6), dp(16), dp(6))
        setTextColor(Apparence.couleur(context, if (actif) R.color.on_brand else R.color.brand))
        if (actif) {
            setTypeface(typeface, Typeface.BOLD)
            background = GradientDrawable().apply { cornerRadius = dp(999).toFloat(); setColor(Apparence.couleur(context, R.color.brand)) }
        }
        isSelected = actif
        setOnClickListener { action() }
    }

    private fun basculer(civil: Boolean) {
        Store.setCalendrierCivil(this, civil)
        mois = YearMonth.from(choisi)
        moisHebreu = null
        afficher()
    }

    private fun decaler(civil: Boolean, debutHebreu: LocalDate, sens: Int) {
        if (civil) mois = mois.plusMonths(sens.toLong()) else moisHebreu = JourAffiche.moisHebreuVoisin(debutHebreu, sens)
        afficher()
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

    /** La majuscule à l'initiale seulement : « Septembre – octobre 2026 ». */
    private fun initiale(s: String, locale: Locale) = s.replaceFirstChar { it.titlecase(locale) }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private companion object {
        const val ETAT_JOUR = "jour"
        const val ETAT_MOIS_HEBREU = "mois_hebreu"
    }
}
