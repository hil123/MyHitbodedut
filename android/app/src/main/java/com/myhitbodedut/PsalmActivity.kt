package com.myhitbodedut

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

/** Un psaume du Tikoun, verset par verset. */
class PsalmActivity : AppCompatActivity() {

    private lateinit var tikkun: Tikkun
    private lateinit var psaume: Psalm
    private lateinit var done: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_psalm)

        tikkun = Data.tikkun(this)
        val trouve = tikkun.byNumero(intent.getIntExtra(EXTRA_NUMERO, -1))
        if (trouve == null) {
            finish()
            return
        }
        psaume = trouve
        done = findViewById(R.id.psalm_done)

        findViewById<TextView>(R.id.psalm_heading).text =
            getString(R.string.tikkun_psalm_he, psaume.numero)
        val rang = tikkun.psaumes.indexOf(psaume) + 1
        findViewById<TextView>(R.id.psalm_position).text =
            getString(R.string.tikkun_position, rang, tikkun.psaumes.size, psaume.versets.size)

        val conteneur = findViewById<LinearLayout>(R.id.psalm_verses)
        psaume.versets.forEachIndexed { index, verset ->
            conteneur.addView(vue(verset, premier = index == 0))
            // L'indication d'arret suit le verset qu'elle concerne, avec sa
            // source : elle n'est pas du texte, elle dit comment le lire.
            verset.arret?.let { conteneur.addView(vueArret(it)) }
        }

        findViewById<Button>(R.id.psalm_next).apply {
            val suivant = tikkun.psaumes.getOrNull(rang)
            if (suivant == null) {
                visibility = View.GONE
            } else {
                setOnClickListener {
                    startActivity(
                        Intent(this@PsalmActivity, PsalmActivity::class.java)
                            .putExtra(EXTRA_NUMERO, suivant.numero)
                    )
                    finish()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        renderDone()
    }

    /** Le marquage se defait : on peut s'etre trompe de ligne. */
    private fun renderDone() {
        val dit = psaume.numero in Store.tikkunDone(this)
        done.setText(if (dit) R.string.tikkun_unmark else R.string.tikkun_mark)
        done.setOnClickListener {
            Store.setTikkunDone(this, psaume.numero, !dit)
            renderDone()
        }
    }

    /**
     * Un verset : son numero en petit, puis le texte en droite-a-gauche.
     * Rien n'est rétréci ici — un psaume se lit, il ne se survole pas.
     */
    private fun vue(verset: Verse, premier: Boolean): View {
        val ligne = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply { topMargin = if (premier) 0 else dp(12) }
        }
        ligne.addView(
            TextView(this).apply {
                text = verset.numero.toString()
                textSize = 12f
                setTextColor(ContextCompat.getColor(context, R.color.brand))
                layoutParams = LinearLayout.LayoutParams(dp(22), ViewGroup.LayoutParams.WRAP_CONTENT)
                    .apply { topMargin = dp(5) }
            }
        )
        ligne.addView(
            TextView(this).apply {
                text = verset.he
                textSize = 19f
                textDirection = View.TEXT_DIRECTION_RTL
                textAlignment = View.TEXT_ALIGNMENT_VIEW_START
                setLineSpacing(dp(6).toFloat(), 1f)
                setTextColor(ContextCompat.getColor(context, R.color.ink))
                layoutParams = LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f
                )
            }
        )
        return ligne
    }

    /** L'indication d'arret, visiblement distincte du verset. */
    private fun vueArret(arret: Pause): View = TextView(this).apply {
        layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
        ).apply { topMargin = dp(8) }
        text = arret.note
        layoutDirection = View.LAYOUT_DIRECTION_RTL
        textAlignment = View.TEXT_ALIGNMENT_VIEW_START
        textSize = 13f
        setLineSpacing(dp(3).toFloat(), 1f)
        setPadding(dp(12), dp(9), dp(12), dp(9))
        setBackgroundResource(R.drawable.bg_badge_warning)
        setTextColor(ContextCompat.getColor(context, R.color.warning))
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    companion object {
        const val EXTRA_NUMERO = "psalm_numero"
    }
}
