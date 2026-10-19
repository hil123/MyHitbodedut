package com.myhitbodedut

import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import com.google.android.material.button.MaterialButton
import java.time.LocalDate

/**
 * La source du jour (proposition 10 de la feuille de route, 19/10) : un
 * paragraphe des sources de la bibliothèque, le même toute la journée et pour
 * tous, distinct du 'hizouk, avec deux sorties : commencer, ou lire la suite.
 *
 * Seules les sources libres y entrent (les entrées « source » de
 * bibliotheque.json, dans la forme que leur licence permet) ; jamais les
 * articles sans autorisation. Le web choisit le même paragraphe le même jour
 * (regles.js, SourceDuJour).
 */
object SourceDuJour {

    data class Pensee(val source: SourceEntry, val texte: String)

    /** Un paragraphe plus court n'a pas assez à dire seul. */
    const val MIN = 80

    /** Le pas entre deux jours : premier, il fait passer d'une source à l'autre plutôt que de paragraphe en paragraphe. */
    private const val PAS = 37L

    fun candidats(library: Library): List<Pensee> =
        library.entrees.filterIsInstance<SourceEntry>().flatMap { s ->
            LibraryRules.paragraphs(library, s).filter { it.length >= MIN }.map { Pensee(s, it) }
        }

    fun choix(library: Library, jour: LocalDate): Pensee? {
        val c = candidats(library)
        if (c.isEmpty()) return null
        return c[Math.floorMod(jour.toEpochDay() * PAS, c.size.toLong()).toInt()]
    }

    /** Un extrait qui tient sur l'accueil : coupé à un mot, jamais au milieu. */
    fun extrait(texte: String, max: Int = 320): String =
        if (texte.length <= max) texte else texte.take(max).substringBeforeLast(' ').trimEnd(',', ' ', ':', ';') + "…"

    fun carte(c: Context): View? {
        val p = choix(Data.library(c), LocalDate.now()) ?: return null
        fun dp(v: Int) = (v * c.resources.displayMetrics.density).toInt()
        return LinearLayout(c).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(R.drawable.bg_card)
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(12) }
            setPadding(dp(16), dp(12), dp(16), dp(8))
            addView(TextView(c).apply {
                setText(R.string.source_du_jour)
                textSize = 12f
                isAllCaps = true
                letterSpacing = 0.06f
                setTextColor(Apparence.couleur(c, R.color.brand))
            })
            addView(TextView(c).apply {
                text = extrait(p.texte)
                textSize = 18f
                setLineSpacing(dp(4).toFloat(), 1f)
                textDirection = View.TEXT_DIRECTION_RTL
                textAlignment = View.TEXT_ALIGNMENT_VIEW_START
                layoutDirection = View.LAYOUT_DIRECTION_RTL
                setTextColor(Apparence.couleur(c, R.color.ink))
                setPadding(0, dp(8), 0, dp(4))
                PoliceHebraique.appliquer(this)
            })
            addView(TextView(c).apply {
                text = Reference.localise(c, p.source.ref)
                textSize = 13f
                setTypeface(typeface, Typeface.ITALIC)
                setTextColor(Apparence.couleur(c, R.color.muted))
            })
            addView(LinearLayout(c).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, dp(8), 0, 0)
                addView(MaterialButton(c, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
                    setText(R.string.source_du_jour_commencer)
                    setOnClickListener { c.startActivity(Intent(c, SessionActivity::class.java)) }
                })
                addView(MaterialButton(c, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
                    layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { marginStart = dp(8) }
                    setText(R.string.source_du_jour_lire)
                    setOnClickListener {
                        c.startActivity(Intent(c, LibraryEntryActivity::class.java).putExtra(LibraryEntryActivity.EXTRA_ID, p.source.id))
                    }
                })
            })
        }
    }
}
