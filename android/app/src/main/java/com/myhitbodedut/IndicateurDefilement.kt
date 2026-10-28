package com.myhitbodedut

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.graphics.ColorUtils
import androidx.recyclerview.widget.RecyclerView

/**
 * Le niveau de lecture (28/10, « un petit indicateur sur le côté pour indiquer le niveau
 * du scroll ») : un trait fin le long du bord droit du texte, dont un segment plus marqué
 * dit où l'on est et quelle part du texte tient à l'écran. Il apparaît quand le texte
 * défile et s'estompe une seconde et demie après ; il ne prend aucun toucher.
 *
 * Posé par-dessus la vue qui défile, dans le cadre de l'activité, à sa place exacte
 * (suivie à chaque mise en page). Une RecyclerView donne sa propre estimation de
 * position ; une ScrollView, sa position et la hauteur de son contenu.
 */
class IndicateurDefilement(context: Context, private val defilement: View) : View(context) {

    private val d = resources.displayMetrics.density
    private val couleur = Apparence.couleur(context, R.color.brand)
    private val piste = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = ColorUtils.setAlphaComponent(couleur, 0x22) }
    private val curseur = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = ColorUtils.setAlphaComponent(couleur, 0xAA) }
    private val rect = RectF()
    private val estomper = Runnable { animate().alpha(0f).setDuration(400L).start() }

    init {
        isClickable = false
        isFocusable = false
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
        alpha = 0f
    }

    /** offset, étendue visible, étendue totale ; null si rien ne défile. */
    private fun mesures(): Triple<Int, Int, Int>? {
        val v = defilement
        val (offset, vu, total) = if (v is RecyclerView) {
            Triple(v.computeVerticalScrollOffset(), v.computeVerticalScrollExtent(), v.computeVerticalScrollRange())
        } else {
            val contenu = (v as? ViewGroup)?.getChildAt(0)?.height ?: 0
            Triple(v.scrollY, v.height, contenu)
        }
        return if (total <= vu || vu <= 0) null else Triple(offset, vu, total)
    }

    override fun onDraw(canvas: Canvas) {
        val (offset, vu, total) = mesures() ?: return
        val largeur = 3 * d
        val x = width - largeur - 3 * d
        val haut = 6 * d
        val bas = height - 6 * d - defilement.paddingBottom.coerceAtMost((height / 4))
        val course = (bas - haut).coerceAtLeast(1f)
        rect.set(x, haut, x + largeur, bas)
        canvas.drawRoundRect(rect, largeur, largeur, piste)
        val longueur = (course * vu / total).coerceAtLeast(28 * d).coerceAtMost(course)
        val debut = haut + (course - longueur) * (offset.toFloat() / (total - vu)).coerceIn(0f, 1f)
        rect.set(x, debut, x + largeur, debut + longueur)
        canvas.drawRoundRect(rect, largeur, largeur, curseur)
    }

    /** Le texte a bougé : se montrer, se redessiner, s'estomper plus tard. */
    fun reveiller() {
        if (mesures() == null) return
        removeCallbacks(estomper)
        animate().cancel()
        alpha = 1f
        invalidate()
        postDelayed(estomper, 1500L)
    }

    /** Recouvre exactement la vue qui défile. */
    private fun placer(racine: FrameLayout) {
        val a = IntArray(2)
        val b = IntArray(2)
        defilement.getLocationInWindow(a)
        racine.getLocationInWindow(b)
        val lp = layoutParams as FrameLayout.LayoutParams
        val gauche = a[0] - b[0]
        val haut = a[1] - b[1]
        if (lp.leftMargin != gauche || lp.topMargin != haut || lp.width != defilement.width || lp.height != defilement.height) {
            lp.leftMargin = gauche
            lp.topMargin = haut
            lp.width = defilement.width
            lp.height = defilement.height
            layoutParams = lp
        }
    }

    companion object {
        fun poser(racine: FrameLayout, defilement: View): IndicateurDefilement {
            val i = IndicateurDefilement(racine.context, defilement)
            racine.addView(i, FrameLayout.LayoutParams(0, 0))
            racine.viewTreeObserver.addOnGlobalLayoutListener { i.placer(racine) }
            return i
        }
    }
}
