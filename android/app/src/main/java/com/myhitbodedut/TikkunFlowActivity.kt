package com.myhitbodedut

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

/**
 * Le Tikoun lu d'un seul trait.
 *
 * L'ecran des dix psaumes sert a reprendre ou l'on s'est arrete ; celui-ci
 * sert au cas normal, qui est de tout dire d'affilee. La source est
 * explicite : « להמשיך מזה לזה ברצף אחד, ללא הפסק ». Il n'y a donc aucun
 * bouton entre les psaumes, rien a toucher, rien qui interrompe — on fait
 * defiler et on continue.
 *
 * Tout est construit d'avance dans une seule vue defilante : l'ouverture,
 * les dix psaumes a la suite, les versets de cloture. La kavana et la
 * priere de Rabbi Natan restent a part, sur l'ecran de la liste : elles
 * ne font pas partie du trait.
 */
class TikkunFlowActivity : AppCompatActivity() {

    private lateinit var tikkun: Tikkun

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tikkun_flow)

        tikkun = Data.tikkun(this)
        val langue = ChizukCard.language(this)

        findViewById<TextView>(R.id.flow_subtitle).text =
            tikkun.continuite.forLanguage(langue).ifBlank { tikkun.continuite.he }

        val contenu = findViewById<LinearLayout>(R.id.flow_content)
        var premier = true

        tikkun.avant.forEach { bloc ->
            contenu.addView(titre(bloc.titre.he, premier))
            premier = false
            bloc.paragraphes.forEach { contenu.addView(paragraphe(it)) }
        }

        tikkun.psaumes.forEach { psaume ->
            contenu.addView(titre(getString(R.string.tikkun_psalm_he, psaume.numero), premier))
            premier = false
            psaume.versets.forEach { verset ->
                contenu.addView(vueVerset(verset))
                verset.arret?.let { contenu.addView(vueArret(it)) }
            }
        }

        tikkun.bloc(Tikkun.CLOTURE)?.let { bloc ->
            contenu.addView(titre(bloc.titre.he, false))
            bloc.paragraphes.forEach { contenu.addView(paragraphe(it)) }
        }

        findViewById<Button>(R.id.flow_done).apply {
            setText(R.string.tikkun_flow_done)
            setOnClickListener {
                // Tout dire d'un trait vaut les dix psaumes.
                tikkun.psaumes.forEach { Store.setTikkunDone(this@TikkunFlowActivity, it.numero, true) }
                finish()
            }
        }
    }

    /** Le nom d'un psaume ou d'une partie, sur un filet, sans rien a toucher. */
    private fun titre(texte: String, premier: Boolean): View = TextView(this).apply {
        layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
        ).apply {
            topMargin = if (premier) 0 else dp(26)
            bottomMargin = dp(6)
            marginStart = dp(20)
            marginEnd = dp(20)
        }
        text = texte
        layoutDirection = View.LAYOUT_DIRECTION_RTL
        textAlignment = View.TEXT_ALIGNMENT_VIEW_START
        textSize = 20f
        setTypeface(typeface, android.graphics.Typeface.BOLD)
        setTextColor(Apparence.couleur(context, R.color.brand))
    }

    private fun vueVerset(verset: Verse): View = TextView(this).apply {
        layoutParams = ligneParams()
        text = verset.he
        layoutDirection = View.LAYOUT_DIRECTION_RTL
        textAlignment = View.TEXT_ALIGNMENT_VIEW_START
        textSize = 19f
        setLineSpacing(dp(6).toFloat(), 1f)
        setTextColor(Apparence.couleur(context, R.color.ink))
    }

    private fun paragraphe(texte: String): View = TextView(this).apply {
        layoutParams = ligneParams()
        text = texte
        layoutDirection = View.LAYOUT_DIRECTION_RTL
        textAlignment = View.TEXT_ALIGNMENT_VIEW_START
        textSize = 18f
        setLineSpacing(dp(6).toFloat(), 1f)
        setTextColor(Apparence.couleur(context, R.color.ink))
    }

    private fun vueArret(arret: Pause): View = TextView(this).apply {
        layoutParams = ligneParams()
        text = arret.note
        layoutDirection = View.LAYOUT_DIRECTION_RTL
        textAlignment = View.TEXT_ALIGNMENT_VIEW_START
        textSize = 15f
        setLineSpacing(dp(4).toFloat(), 1f)
        setTextColor(Apparence.couleur(context, R.color.muted))
    }

    private fun ligneParams() = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT,
    ).apply {
        topMargin = dp(8)
        marginStart = dp(20)
        marginEnd = dp(20)
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
