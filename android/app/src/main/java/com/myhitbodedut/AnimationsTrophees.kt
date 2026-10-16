package com.myhitbodedut

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.app.Activity
import android.content.Context
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.drawable.Drawable
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.graphics.ColorUtils
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Les cinq animations des trophées (16/10, validées) : la ligne qui se remplit (PaliersActivity),
 * le trophée neuf qui se retourne puis qu'un reflet traverse, le compte qui monte et la barre
 * presque pleine qui pulse, les rayons lents du changement de grade, et des étincelles de la
 * couleur du grade — pas de confettis multicolores. Animations coupées sur le téléphone : rien
 * ne bouge, tout est à sa place.
 */
object AnimationsTrophees {

    val actives: Boolean get() = ValueAnimator.areAnimatorsEnabled()

    /** E : une dizaine d'étincelles du grade, autour du centre de [ancre], 0,9 s. */
    fun etincelles(activity: Activity, ancre: View?, m: Trophees.Metal?) {
        if (!actives || ancre == null || !ancre.isAttachedToWindow) return
        val racine = activity.findViewById<FrameLayout>(android.R.id.content) ?: return
        val ici = IntArray(2).also { racine.getLocationInWindow(it) }
        val la = IntArray(2).also { ancre.getLocationInWindow(it) }
        val vue = Etincelles(activity, la[0] - ici[0] + ancre.width / 2f, la[1] - ici[1] + ancre.height / 2f,
            TropheeDessin.teintes(m ?: Trophees.Metal.OR))
        racine.addView(vue, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        vue.lancer()
    }

    private class Etincelles(c: Context, val cx: Float, val cy: Float, teintes: Triple<Int, Int, Int>) : View(c) {
        private val d = resources.displayMetrics.density
        private val points = List(10) { k ->
            val a = k / 10f * 2 * Math.PI.toFloat() + Random.nextFloat() * 0.4f
            val r = (38 + Random.nextFloat() * 26) * d
            Triple(cos(a) * r, sin(a) * r, if (k % 2 == 0) teintes.second else teintes.first)
        }
        private val retards = List(10) { Random.nextFloat() * 0.2f }
        private val pinceau = Paint(Paint.ANTI_ALIAS_FLAG)
        private var t = 0f

        init {
            isClickable = false
            importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
        }

        fun lancer() = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 1100
            interpolator = LinearInterpolator()
            addUpdateListener { t = it.animatedValue as Float; invalidate() }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) { (parent as? ViewGroup)?.removeView(this@Etincelles) }
            })
            start()
        }

        override fun onDraw(canvas: Canvas) {
            points.forEachIndexed { i, (dx, dy, couleur) ->
                val u = ((t - retards[i]) / 0.8f).coerceIn(0f, 1f)
                if (u <= 0f) return@forEachIndexed
                val e = 1f - (1f - u) * (1f - u)
                pinceau.color = couleur
                pinceau.alpha = ((if (u < 0.25f) u / 0.25f else (1f - u) / 0.75f) * 255).toInt()
                canvas.drawCircle(cx + dx * e, cy + dy * e, 3.5f * d * (0.4f + 0.6f * e), pinceau)
            }
        }
    }

    /** A : le trophée neuf se retourne (0,65 s), puis un reflet le traverse une fois. */
    fun retourner(v: View, retard: Long = 400L, apres: () -> Unit = {}) {
        if (!actives) { apres(); return }
        v.cameraDistance = 8000 * v.resources.displayMetrics.density
        v.rotationY = -180f
        v.animate().rotationY(0f).setStartDelay(retard).setDuration(650L).setInterpolator(DecelerateInterpolator(1.6f))
            .withEndAction {
                val reflet = Reflet()
                v.foreground = reflet
                ValueAnimator.ofFloat(0f, 1f).apply {
                    duration = 700
                    addUpdateListener { reflet.position = it.animatedValue as Float }
                    addListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) { v.foreground = null }
                    })
                    start()
                }
                apres()
            }.start()
    }

    /** Le reflet : une bande claire en biais, découpée dans le cercle de l'objet. */
    private class Reflet : Drawable() {
        var position = 0f
            set(v) { field = v; invalidateSelf() }
        private val pinceau = Paint(Paint.ANTI_ALIAS_FLAG)
        private val cercle = Path()

        override fun draw(canvas: Canvas) {
            val b = bounds
            if (b.isEmpty) return
            val x = b.left - b.width() + position * 3 * b.width()
            pinceau.shader = LinearGradient(x - b.width() * 0.3f, b.top.toFloat(), x + b.width() * 0.3f, b.bottom.toFloat(),
                intArrayOf(0, 0xBFFFFFFF.toInt(), 0), floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP)
            cercle.reset()
            cercle.addOval(RectF(b), Path.Direction.CW)
            canvas.save()
            canvas.clipPath(cercle)
            canvas.drawRect(b, pinceau)
            canvas.restore()
        }

        override fun setAlpha(alpha: Int) = Unit
        override fun setColorFilter(colorFilter: ColorFilter?) = Unit
        @Deprecated("Deprecated in Java")
        override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
    }

    /** B : une étape qui apparaît — le trait pousse vers le bas, puis la médaille. */
    fun pousser(trait: View, objet: View, retard: Long) {
        if (!actives) return
        trait.pivotY = 0f
        trait.scaleY = 0f
        trait.animate().scaleY(1f).setStartDelay(retard).setDuration(260L).start()
        objet.alpha = 0f
        objet.scaleX = 0.7f; objet.scaleY = 0.7f
        objet.animate().alpha(objet.tag as? Float ?: 1f).scaleX(1f).scaleY(1f).setStartDelay(retard + 120).setDuration(300L).start()
    }

    /** D : un nombre qui monte de 0 à sa valeur, en 0,7 s. */
    fun monter(t: TextView, fin: Int, texte: (Int) -> String) {
        if (!actives || fin <= 0) { t.text = texte(fin); return }
        ValueAnimator.ofInt(0, fin).apply {
            duration = 700
            interpolator = DecelerateInterpolator(1.5f)
            addUpdateListener { t.text = texte(it.animatedValue as Int) }
            start()
        }
    }

    /** D : une barre presque pleine pulse une fois. */
    fun pulser(v: View, retard: Long = 900L) {
        if (!actives) return
        v.animate().scaleY(1.8f).setStartDelay(retard).setDuration(300L).withEndAction {
            v.animate().scaleY(1f).setStartDelay(0).setDuration(450L).start()
        }.start()
    }

    /** C : des rayons lents (un tour en 12 s), derrière la médaille du nouveau grade. */
    class Rayons(c: Context, couleur: Int) : View(c) {
        private val teinte = ColorUtils.setAlphaComponent(couleur, 0x70)
        private val pinceau = Paint(Paint.ANTI_ALIAS_FLAG)
        private var angle = 0f
        private var anim: ValueAnimator? = null

        init { importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO }

        override fun onAttachedToWindow() {
            super.onAttachedToWindow()
            alpha = 0f
            scaleX = 0.6f; scaleY = 0.6f
            animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(if (actives) 1500L else 0L).start()
            if (!actives) return
            anim = ValueAnimator.ofFloat(0f, 360f).apply {
                duration = 12_000
                repeatCount = ValueAnimator.INFINITE
                interpolator = LinearInterpolator()
                addUpdateListener { angle = it.animatedValue as Float; invalidate() }
                start()
            }
        }

        override fun onDetachedFromWindow() {
            anim?.cancel()
            super.onDetachedFromWindow()
        }

        override fun onDraw(canvas: Canvas) {
            val r = minOf(width, height) / 2f
            val o = RectF(width / 2f - r, height / 2f - r, width / 2f + r, height / 2f + r)
            pinceau.shader = android.graphics.RadialGradient(width / 2f, height / 2f, r,
                intArrayOf(teinte, teinte, 0), floatArrayOf(0f, 0.35f, 1f), Shader.TileMode.CLAMP)
            canvas.save()
            canvas.rotate(angle, width / 2f, height / 2f)
            for (k in 0 until 20) canvas.drawArc(o, k * 18f, 6f, true, pinceau)
            canvas.restore()
        }
    }
}
