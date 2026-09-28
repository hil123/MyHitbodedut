package com.myhitbodedut

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator

/**
 * L'anneau de la semaine, autour du hassid de l'accueil.
 *
 * Il part d'en haut et tourne dans le sens de lecture : à droite en
 * français, à gauche en hébreu. Les couleurs sont lues au dessin, pour
 * suivre la palette choisie dans Réglages → Apparence.
 */
class JaugeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val epaisseur = 12f * resources.displayMetrics.density
    private val cadre = RectF()
    private val piste = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = epaisseur
    }
    private val arc = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = epaisseur
        strokeCap = Paint.Cap.ROUND
    }

    private var affichee = 0f
    private var animation: ValueAnimator? = null

    /** La nouvelle valeur, de 0 à 1. L'anneau y glisse, sauf animations coupées. */
    fun montrer(fraction: Float) {
        val cible = fraction.coerceIn(0f, 1f)
        animation?.cancel()
        if (!ValueAnimator.areAnimatorsEnabled() || !isAttachedToWindow) {
            affichee = cible
            invalidate()
            return
        }
        animation = ValueAnimator.ofFloat(affichee, cible).apply {
            duration = 700L
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                affichee = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    override fun onDetachedFromWindow() {
        animation?.cancel()
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cote = minOf(width, height).toFloat()
        val marge = epaisseur / 2f
        val gauche = (width - cote) / 2f
        val haut = (height - cote) / 2f
        cadre.set(gauche + marge, haut + marge, gauche + cote - marge, haut + cote - marge)

        piste.color = Apparence.couleur(context, R.color.day_todo)
        arc.color = Apparence.couleur(context, R.color.brand)
        canvas.drawOval(cadre, piste)
        if (affichee <= 0f) return
        val sens = if (layoutDirection == LAYOUT_DIRECTION_RTL) -1f else 1f
        canvas.drawArc(cadre, -90f, sens * 360f * affichee, false, arc)
    }
}
