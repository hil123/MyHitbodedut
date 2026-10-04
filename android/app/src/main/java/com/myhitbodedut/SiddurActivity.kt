package com.myhitbodedut

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

/**
 * La liste du siddour (demande du 04/10) : chaque texte sous son nom hébreu,
 * en grand et dans la police des textes saints, comme dans CalJ ; son nom
 * dans la langue de l'interface en dessous. Tehilim et le Tikoun HaKlali
 * y ont leur place, en tête, et ouvrent leurs propres écrans.
 */
class SiddurActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_siddur)
        afficher()
    }

    /** Toute la liste, pour le nusah choisi ; rebâtie quand on en change. */
    private fun afficher() {
        val nusah = Store.siddurNusah(this)
        val siddur = Data.siddur(this, nusah)
        val langue = ChizukCard.language(this)
        findViewById<TextView>(R.id.siddur_heading_nusah).text = siddur.nusah.forLanguage(langue)
        val liste = findViewById<LinearLayout>(R.id.siddur_list)
        liste.removeAllViews()

        // Trois nusah, au choix (demande du 04/10) ; le choix est gardé.
        liste.addView(selecteur(nusah))

        liste.addView(entete(getString(R.string.siddur_books)))
        liste.addView(carte(listOf(
            ligne(getString(R.string.siddur_tehilim_he), getString(R.string.tehilim_title)) {
                startActivity(Intent(this, TehilimActivity::class.java))
            },
            ligne(getString(R.string.siddur_tikkun_he), getString(R.string.tikkun_title)) {
                startActivity(Intent(this, TikkunActivity::class.java))
            },
        )))
        siddur.groupes.forEach { groupe ->
            liste.addView(entete(groupe.titre.forLanguage(langue)))
            liste.addView(carte(siddur.du(groupe.cle).map { texte ->
                val traduction = texte.titre.forLanguage(langue).takeIf { it != texte.titre.he }.orEmpty()
                // Un texte absent des sources de ce nusah, emprunté à un autre, le dit.
                val detail = listOfNotNull(traduction.ifBlank { null }, texte.emprunt?.forLanguage(langue)).joinToString(" · ")
                ligne(texte.titre.he, detail) { SiddurLectureActivity.ouvrir(this, texte.id) }
            }))
        }
        // Les sources, comme la licence CC-BY du siddour Metsudah le demande.
        liste.addView(TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                .apply { topMargin = dp(20) }
            setPadding(dp(20), 0, dp(20), 0)
            text = getString(R.string.siddur_sources, siddur.sources.joinToString(" ; "))
            textSize = 12f
            setTextColor(Apparence.couleur(context, R.color.muted))
        })
    }

    /** Les trois nusah côte à côte ; celui qui est choisi est plein. */
    private fun selecteur(choisi: Nusah): View = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            .apply { setMargins(dp(16), dp(16), dp(16), 0) }
        Nusah.entries.forEach { n ->
            addView(TextView(context).apply {
                layoutParams = LinearLayout.LayoutParams(0, dp(40), 1f).apply { setMargins(dp(3), 0, dp(3), 0) }
                gravity = Gravity.CENTER
                text = getString(n.libelle)
                textSize = 14f
                val actif = n == choisi
                setBackgroundResource(if (actif) R.drawable.bg_nusah_on else R.drawable.bg_nusah_off)
                setTextColor(Apparence.couleur(context, if (actif) R.color.white else R.color.brand))
                isSelected = actif
                contentDescription = getString(R.string.siddur_choose) + " : " + getString(n.libelle)
                setOnClickListener {
                    if (n != Store.siddurNusah(context)) {
                        Store.setSiddurNusah(context, n)
                        afficher()
                    }
                }
            })
        }
    }

    private fun entete(texte: String): View = TextView(this).apply {
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            .apply { topMargin = dp(20) }
        setPadding(dp(20), 0, dp(20), 0)
        text = texte
        isAllCaps = true
        letterSpacing = 0.08f
        textSize = 12f
        setTextColor(Apparence.couleur(context, R.color.muted))
    }

    private fun carte(lignes: List<View>): View = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setBackgroundResource(R.drawable.bg_card)
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            .apply { setMargins(dp(16), dp(8), dp(16), 0) }
        setPadding(dp(18), dp(4), dp(18), dp(4))
        lignes.forEachIndexed { i, v ->
            if (i > 0) addView(View(context).apply {
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1))
                setBackgroundColor(Apparence.couleur(context, R.color.line))
            })
            addView(v)
        }
    }

    /** Une ligne : le nom hébreu, à droite, et sa traduction dessous. */
    private fun ligne(hebreu: String, traduction: String, ouvrir: () -> Unit): View =
        LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            minimumHeight = dp(64)
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(10), 0, dp(10))
            val ondulation = android.util.TypedValue().also {
                context.theme.resolveAttribute(android.R.attr.selectableItemBackground, it, true)
            }.resourceId
            setBackgroundResource(ondulation)
            isClickable = true
            isFocusable = true
            setOnClickListener { ouvrir() }
            addView(TextView(context).apply {
                text = hebreu
                textSize = 22f
                layoutDirection = View.LAYOUT_DIRECTION_RTL
                textDirection = View.TEXT_DIRECTION_RTL
                textAlignment = View.TEXT_ALIGNMENT_VIEW_START
                setTextColor(Apparence.couleur(context, R.color.ink))
                PoliceHebraique.appliquer(this)
            })
            if (traduction.isNotBlank()) addView(TextView(context).apply {
                text = traduction
                textSize = 13f
                textAlignment = View.TEXT_ALIGNMENT_VIEW_START
                setTextColor(Apparence.couleur(context, R.color.muted))
            })
        }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
