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
        val siddur = Data.siddur(this)
        val langue = ChizukCard.language(this)
        val liste = findViewById<LinearLayout>(R.id.siddur_list)

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
                ligne(texte.titre.he, texte.titre.forLanguage(langue).takeIf { it != texte.titre.he }.orEmpty()) {
                    SiddurLectureActivity.ouvrir(this, texte.id)
                }
            }))
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
