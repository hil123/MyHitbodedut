package com.myhitbodedut

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import android.view.animation.PathInterpolator
import androidx.core.graphics.ColorUtils

/**
 * La respiration lumineuse de la pratique (16/10, point 2) : une lueur douce
 * derrière le minuteur, qui grandit en quatre secondes et décroît en six,
 * sans fin. Elle ne compte rien et ne demande rien : on peut caler son
 * souffle dessus, ou l'ignorer. Animations coupées sur le téléphone : la
 * lueur reste, immobile.
 *
 * Elle couvre tout l'écran, derrière le contenu, et se centre sur [cible]
 * (le minuteur) : une lueur dessinée dans le cadre du seul minuteur serait
 * coupée net à ses bords.
 */
class SouffleView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null,
) : View(context, attrs) {

    var cible: View? = null
    var couleur: Int = 0xFFDFF5E8.toInt()
        set(v) { field = v; base = 0f; post { mesurer() } }

    private val pinceau = Paint(Paint.ANTI_ALIAS_FLAG)
    private var phase = 0.5f
    private var animateur: ValueAnimator? = null

    var actif: Boolean = false
        set(v) {
            if (field == v) return
            field = v
            if (v) demarrer() else arreter()
            invalidate()
        }

    private fun demarrer() {
        if (!ValueAnimator.areAnimatorsEnabled()) { phase = 0.6f; return }
        // Une seule courbe pour l'aller (4 s) et le retour (6 s) : 0 → 1 sur
        // 40 % du cycle, 1 → 0 sur les 60 % restants, chacun adouci.
        val doux = PathInterpolator(0.45f, 0f, 0.55f, 1f)
        animateur = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = CYCLE_MS
            repeatCount = ValueAnimator.INFINITE
            interpolator = null
            addUpdateListener {
                val t = it.animatedValue as Float
                phase = if (t < INSPIRE) doux.getInterpolation(t / INSPIRE)
                else 1f - doux.getInterpolation((t - INSPIRE) / (1f - INSPIRE))
                invalidate()
            }
            start()
        }
    }

    private fun arreter() {
        animateur?.cancel()
        animateur = null
    }

    override fun onDetachedFromWindow() {
        arreter()
        super.onDetachedFromWindow()
    }

    /** Le centre et le rayon, calculés à la mise en page ; le dégradé, une fois (revue du 16/10 : plus rien n'est alloué par image). */
    private var cx = 0f
    private var cy = 0f
    private var base = 0f

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        post { mesurer() }
    }

    private fun mesurer() {
        val c = cible ?: return
        val ici = IntArray(2).also { getLocationInWindow(it) }
        val la = IntArray(2).also { c.getLocationInWindow(it) }
        cx = la[0] - ici[0] + c.width / 2f
        cy = la[1] - ici[1] + c.height / 2f
        val r = minOf(width, resources.displayMetrics.heightPixels) * 0.42f
        if (r > 0f && r != base) {
            base = r
            pinceau.shader = RadialGradient(0f, 0f, r,
                intArrayOf(ColorUtils.setAlphaComponent(couleur, 255), ColorUtils.setAlphaComponent(couleur, 90), ColorUtils.setAlphaComponent(couleur, 0)),
                floatArrayOf(0f, 0.55f, 1f), Shader.TileMode.CLAMP)
        }
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        if (!actif || base <= 0f || cible?.visibility != VISIBLE) return
        val echelle = 0.78f + 0.22f * phase
        pinceau.alpha = ((0.16f + 0.26f * phase) * 255).toInt()
        canvas.save()
        canvas.translate(cx, cy)
        canvas.scale(echelle, echelle)
        canvas.drawCircle(0f, 0f, base, pinceau)
        canvas.restore()
    }

    companion object {
        /** Quatre secondes d'inspiration, six d'expiration. */
        const val CYCLE_MS = 10_000L
        private const val INSPIRE = 0.4f
    }
}
