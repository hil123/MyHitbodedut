package com.myhitbodedut

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.myhitbodedut.ReglagesLecture.Role
import androidx.appcompat.app.AppCompatActivity

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
            getString(R.string.tikkun_psalm_he, Reference.lettres(psaume.numero))
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

        // Le bouton de lecture : taille des lettres et lecture de nuit.
        val defilement = findViewById<ScrollView>(R.id.psalm_scroll)
        ReglagesLecture.marquer(defilement, Role.FOND)
        ReglagesLecture.marquer(conteneur, Role.FOND)
        ReglagesLecture.installer(this, defilement, auDessusDe = findViewById(R.id.psalm_actions)) {
            ReglagesLecture.appliquer(defilement)
        }
        ReglagesLecture.appliquer(defilement)

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
            ReglagesLecture.marquer(TextView(this).apply {
                text = verset.numero.toString()
                textSize = 12f
                setTextColor(Apparence.couleur(context, R.color.brand))
                // Large selon le nombre : « 45 » tient sur une ligne même à 200 %.
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                    .apply { topMargin = dp(5); marginEnd = dp(6) }
                minWidth = dp(22)
            }, Role.TITRE)
        )
        ligne.addView(
            ReglagesLecture.marquer(TextView(this).apply {
                text = verset.he
                textSize = 19f
                PoliceHebraique.appliquer(this)
                textDirection = View.TEXT_DIRECTION_RTL
                textAlignment = View.TEXT_ALIGNMENT_VIEW_START
                setLineSpacing(dp(9).toFloat(), 1f)
                setTextColor(Apparence.couleur(context, R.color.ink))
                layoutParams = LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f
                )
            }, Role.TEXTE)
        )
        return ligne
    }

    /**
     * L'indication d'arret. Elle se lit comme une parenthese du texte, pas
     * comme un avertissement : ni pictogramme, ni cadre, ni couleur d'alerte.
     * Seule la teinte attenuee la distingue du verset.
     */
    private fun vueArret(arret: Pause): View = ReglagesLecture.marquer(TextView(this).apply {
        layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
        ).apply {
            topMargin = dp(6)
            // Aligne sur le texte du verset, pas sur son numero.
            marginEnd = dp(22)
        }
        text = arret.note
        layoutDirection = View.LAYOUT_DIRECTION_RTL
        textAlignment = View.TEXT_ALIGNMENT_VIEW_START
        textSize = 15f
        setLineSpacing(dp(7).toFloat(), 1f)
        setTextColor(Apparence.couleur(context, R.color.muted))
    }, Role.DISCRET)

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    companion object {
        const val EXTRA_NUMERO = "psalm_numero"
    }
}
