package com.myhitbodedut

import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

/**
 * Le bouton discret des écrans de lecture (demande du 04/10, sur le modèle
 * d'une capture de CalJ). Fermé, c'est une pastille ronde dans le coin, qui
 * s'efface quand on descend dans le texte et revient quand on remonte.
 * Ouvert, une colonne : lecture sur fond noir, changer de nusah (siddour
 * seulement), lettres plus grandes, plus petites, refermer. Les réglages
 * sont gardés d'une lecture à l'autre (Store).
 */
class ReglagesLecture(
    private val cadre: FrameLayout,
    private val avecNusah: Boolean,
    private val surChangement: () -> Unit,
) {
    private val contexte = cadre.context
    private val densite = contexte.resources.displayMetrics.density
    private fun dp(v: Int) = (v * densite).toInt()

    private val pastille = ImageView(contexte).apply {
        setImageResource(R.drawable.ic_lecture_reglages)
        setBackgroundResource(R.drawable.bg_lecture_pastille)
        scaleType = ImageView.ScaleType.CENTER
        contentDescription = contexte.getString(R.string.lecture_reglages)
        elevation = dp(4).toFloat()
        setOnClickListener { ouvrir(true) }
    }

    private val colonne = LinearLayout(contexte).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER_HORIZONTAL
        setBackgroundResource(R.drawable.bg_lecture_colonne)
        setPadding(0, dp(8), 0, dp(8))
        elevation = dp(4).toFloat()
        visibility = View.GONE
    }

    init {
        colonne.addView(bouton(R.drawable.ic_lecture_ampoule, R.string.lecture_nuit) {
            Store.setLectureSombre(contexte, !Store.lectureSombre(contexte))
            surChangement()
        })
        if (avecNusah) colonne.addView(bouton(R.drawable.ic_lecture_nusah, R.string.siddur_choose) { choisirNusah(it) })
        colonne.addView(lettre(true))
        colonne.addView(lettre(false))
        colonne.addView(bouton(R.drawable.ic_lecture_fermer, R.string.lecture_fermer) { ouvrir(false) })

        fun place(v: View, taille: Int?) = FrameLayout.LayoutParams(
            taille ?: ViewGroup.LayoutParams.WRAP_CONTENT, taille ?: ViewGroup.LayoutParams.WRAP_CONTENT,
            Gravity.BOTTOM or Gravity.END,
        ).apply { setMargins(0, 0, dp(18), dp(24)) }.also { v.layoutParams = it }
        place(pastille, dp(56))
        colonne.layoutParams = place(colonne, null).apply { width = dp(68) }
        cadre.addView(colonne)
        cadre.addView(pastille)
    }

    private fun bouton(icone: Int, description: Int, action: (View) -> Unit): View = ImageView(contexte).apply {
        layoutParams = LinearLayout.LayoutParams(dp(56), dp(56))
        setImageResource(icone)
        scaleType = ImageView.ScaleType.CENTER
        contentDescription = contexte.getString(description)
        setOnClickListener { action(it) }
    }

    /** « א » et une flèche : lettres plus grandes ou plus petites. */
    private fun lettre(plus: Boolean): View = TextView(contexte).apply {
        layoutParams = LinearLayout.LayoutParams(dp(56), dp(56))
        gravity = Gravity.CENTER
        text = if (plus) "א↑" else "א↓"
        textSize = if (plus) 22f else 17f
        setTextColor(0xFFFFFFFF.toInt())
        PoliceHebraique.appliquer(this)
        contentDescription = contexte.getString(if (plus) R.string.lecture_plus else R.string.lecture_moins)
        setOnClickListener {
            val actuelle = Store.lectureEchelle(contexte)
            val nouvelle = (actuelle + if (plus) PAS else -PAS).coerceIn(MIN, MAX)
            if (nouvelle != actuelle) {
                Store.setLectureEchelle(contexte, nouvelle)
                surChangement()
            }
        }
    }

    private fun choisirNusah(ancre: View) {
        PopupMenu(contexte, ancre).apply {
            Nusah.entries.forEachIndexed { i, n -> menu.add(0, i, i, contexte.getString(n.libelle)) }
            setOnMenuItemClickListener { item ->
                Store.setSiddurNusah(contexte, Nusah.entries[item.itemId])
                surChangement()
                true
            }
            show()
        }
    }

    private fun ouvrir(ouvert: Boolean) {
        colonne.visibility = if (ouvert) View.VISIBLE else View.GONE
        pastille.visibility = if (ouvert) View.GONE else View.VISIBLE
        if (!ouvert) montrer(true)
    }

    /** La pastille s'efface en descendant dans le texte, revient en remontant. */
    private fun montrer(visible: Boolean) {
        if (colonne.visibility == View.VISIBLE) return
        pastille.animate().cancel()
        pastille.animate().alpha(if (visible) 1f else 0f).translationY(if (visible) 0f else dp(24).toFloat())
            .setDuration(180L).withEndAction { pastille.isClickable = visible }.start()
    }

    fun suivre(liste: RecyclerView) {
        liste.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (dy > dp(4)) montrer(false) else if (dy < -dp(4)) montrer(true)
            }
        })
    }

    fun suivre(defilement: View) {
        defilement.setOnScrollChangeListener { _, _, y, _, ancien ->
            if (y - ancien > dp(4)) montrer(false) else if (ancien - y > dp(4)) montrer(true)
        }
    }

    /** Les couleurs et la taille du moment, pour l'écran qui dessine le texte. */
    data class Rendu(val echelle: Float, val sombre: Boolean, val fond: Int, val texte: Int, val discret: Int, val titre: Int)

    companion object {
        const val PAS = 0.1f
        const val MIN = 0.8f
        const val MAX = 2.0f

        fun rendu(contexte: android.content.Context): Rendu {
            val sombre = Store.lectureSombre(contexte)
            fun c(id: Int) = Apparence.couleur(contexte, id)
            return if (sombre) {
                Rendu(Store.lectureEchelle(contexte), true, c(R.color.lecture_nuit_fond), c(R.color.lecture_nuit_texte),
                    c(R.color.lecture_nuit_discret), c(R.color.lecture_nuit_titre))
            } else {
                Rendu(Store.lectureEchelle(contexte), false, c(R.color.paper), c(R.color.ink), c(R.color.muted), c(R.color.brand))
            }
        }
    }
}
