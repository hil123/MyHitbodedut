package com.myhitbodedut

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.myhitbodedut.ReglagesLecture.Role
import androidx.appcompat.app.AppCompatActivity

/**
 * Une partie du Tikoun qui n'est pas un psaume : ce qui se dit avant, les
 * versets de cloture, la kavana, la priere de Rabbi Natan.
 *
 * Meme mise en page que le lecteur de psaume, sans les boutons de
 * progression : ces parties ne se comptent pas dans les dix.
 */
class LiturgyActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_psalm)

        val bloc = Data.tikkun(this).bloc(intent.getStringExtra(EXTRA_CLE).orEmpty())
        if (bloc == null) {
            finish()
            return
        }

        val langue = ChizukCard.language(this)
        findViewById<TextView>(R.id.psalm_heading).text = bloc.titre.he
        findViewById<TextView>(R.id.psalm_position).apply {
            val sous = bloc.note?.forLanguage(langue)?.ifBlank { null }
                ?: bloc.titre.forLanguage(langue).takeIf { it != bloc.titre.he }
            text = sous.orEmpty()
            visibility = if (sous == null) View.GONE else View.VISIBLE
        }

        val conteneur = findViewById<LinearLayout>(R.id.psalm_verses)
        // La formule de la kavana est centree : c'est un tableau de lettres,
        // pas une phrase qui se lit d'un bout a l'autre.
        bloc.formule?.let { conteneur.addView(paragraphe(it, premier = true, centre = true)) }
        bloc.paragraphes.forEachIndexed { index, p ->
            conteneur.addView(
                paragraphe(p, premier = index == 0 && bloc.formule == null, centre = false)
            )
        }

        findViewById<Button>(R.id.psalm_done).visibility = View.GONE
        findViewById<Button>(R.id.psalm_next).visibility = View.GONE

        // Le bouton de lecture : taille des lettres et lecture de nuit.
        val defilement = findViewById<ScrollView>(R.id.psalm_scroll)
        ReglagesLecture.marquer(defilement, Role.FOND)
        ReglagesLecture.marquer(conteneur, Role.FOND)
        ReglagesLecture.installer(this, defilement) { ReglagesLecture.appliquer(defilement) }
        ReglagesLecture.appliquer(defilement)
    }

    private fun paragraphe(texte: String, premier: Boolean, centre: Boolean): View =
        ReglagesLecture.marquer(TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply { topMargin = if (premier) 0 else dp(12) }
            text = texte
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            textAlignment =
                if (centre) View.TEXT_ALIGNMENT_CENTER else View.TEXT_ALIGNMENT_VIEW_START
            textSize = if (centre) 20f else 18f
            setLineSpacing(dp(9).toFloat(), 1f)
            setTextColor(Apparence.couleur(context, R.color.ink))
            PoliceHebraique.appliquer(this)
        }, Role.TEXTE)

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    companion object {
        const val EXTRA_CLE = "bloc_cle"
    }
}
