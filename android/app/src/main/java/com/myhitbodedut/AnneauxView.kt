package com.myhitbodedut

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator

/**
 * Les trois anneaux de l'accueil, autour du hassid (Anneaux, AnneauxDessin).
 *
 * Ils partent d'en haut et tournent dans le sens de lecture : à droite en
 * français, à gauche en hébreu. Les couleurs sont lues au dessin, pour
 * suivre la palette choisie dans Réglages → Apparence.
 *
 * À l'ouverture, ils se remplissent depuis zéro. Un toucher sur un anneau
 * le met en avant ; un second le remet avec les autres. L'écran est
 * prévenu par [surChoix] pour changer la légende.
 */
class AnneauxView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val densite = resources.displayMetrics.density
    val geometrie = AnneauxDessin.Geometrie(epaisseur = 8f * densite, ecart = 3f * densite)
    private val carre = RectF()

    private var etat: Anneaux.Etat? = null
    private var avance = 1f
    private var animation: ValueAnimator? = null

    /** Un remplissage demandé avant que la vue soit à l'écran : il part à l'attache. */
    private var remplissageEnAttente = false

    /** L'anneau mis en avant, ou null. */
    var misEnAvant: Anneaux.Quel? = null
        private set

    /** Appelé quand le toucher change l'anneau mis en avant. */
    var surChoix: ((Anneaux.Quel?) -> Unit)? = null

    /** Le nouvel état. [depuisZero] rejoue le remplissage depuis le début. */
    fun montrer(nouvel: Anneaux.Etat, depuisZero: Boolean = false) {
        val premier = etat == null
        etat = nouvel
        animation?.cancel()
        if (!ValueAnimator.areAnimatorsEnabled() || !(premier || depuisZero)) {
            avance = 1f
            invalidate()
            return
        }
        // Au premier affichage la vue n'est pas encore attachée : l'animation
        // partirait dans le vide. Elle attend l'attache.
        if (!isAttachedToWindow) {
            avance = 0f
            remplissageEnAttente = true
            return
        }
        remplir()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (remplissageEnAttente) {
            remplissageEnAttente = false
            remplir()
        }
    }

    private fun remplir() {
        animation = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 900L
            interpolator = DecelerateInterpolator(1.6f)
            addUpdateListener {
                avance = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    /** Met un anneau en avant depuis l'écran, ou revient aux trois (null). */
    fun mettreEnAvant(quel: Anneaux.Quel?) {
        if (misEnAvant == quel) return
        misEnAvant = quel
        invalidate()
        surChoix?.invoke(quel)
    }

    override fun onDetachedFromWindow() {
        animation?.cancel()
        super.onDetachedFromWindow()
    }

    private fun placer() {
        val cote = minOf(width, height).toFloat()
        val gauche = (width - cote) / 2f
        val haut = (height - cote) / 2f
        carre.set(gauche, haut, gauche + cote, haut + cote)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val courant = etat ?: return
        placer()
        AnneauxDessin.dessiner(
            canvas, carre, courant,
            AnneauxDessin.Couleurs(
                jour = Apparence.couleur(context, R.color.brand),
                semaine = Apparence.couleur(context, R.color.ring_week),
                serie = Apparence.couleur(context, R.color.ring_streak),
                piste = Apparence.couleur(context, R.color.day_todo)
            ),
            geometrie,
            rtl = layoutDirection == LAYOUT_DIRECTION_RTL,
            avance = avance,
            misEnAvant = misEnAvant
        )
    }

    // Un toucher sur un anneau le choisit ; ailleurs sur la vue (le creux
    // est couvert par la plaque du hassid), rien.
    private var toucheSur: Anneaux.Quel? = null

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (etat == null) return false
        placer()
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                toucheSur = AnneauxDessin.sousLePoint(carre, geometrie, event.x, event.y)
                return toucheSur != null
            }
            MotionEvent.ACTION_UP -> {
                val cible = AnneauxDessin.sousLePoint(carre, geometrie, event.x, event.y)
                if (cible != null && cible == toucheSur) {
                    performClick()
                    mettreEnAvant(if (misEnAvant == cible) null else cible)
                }
                toucheSur = null
                return true
            }
            MotionEvent.ACTION_CANCEL -> toucheSur = null
        }
        return super.onTouchEvent(event)
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }
}
