package com.myhitbodedut

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator

/**
 * Les trois anneaux de l'accueil, autour du hassid (Anneaux, AnneauxDessin).
 *
 * Ils partent d'en haut et tournent dans le sens de lecture : à droite en
 * français, à gauche en hébreu. Les couleurs sont lues au dessin, pour
 * suivre la palette choisie dans Réglages → Apparence.
 *
 * Ils se remplissent depuis zéro à chaque retour sur l'accueil et chaque
 * fois que leurs valeurs changent, sans qu'on touche à rien (demande du
 * 04/10) ; [surRempli] prévient l'écran à la fin. Un toucher sur un anneau
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

    /** Appelé à la fin d'un remplissage, pour enchaîner le saut du hassid ou la fête. */
    var surRempli: (() -> Unit)? = null

    /** Le remplissage en cours doit-il prévenir [surRempli] ? Non quand l'écran a déjà réagi. */
    private var prevenir = true

    /** Appelé quand le toucher change l'anneau mis en avant. */
    var surChoix: ((Anneaux.Quel?) -> Unit)? = null

    /**
     * Le nouvel état. Il se remplit depuis zéro au premier affichage, quand
     * il a changé, ou si [depuisZero] le demande.
     */
    fun montrer(nouvel: Anneaux.Etat, depuisZero: Boolean = false, prevenir: Boolean = true) {
        val rejouer = etat == null || etat != nouvel || depuisZero
        etat = nouvel
        if (!rejouer) {
            // Rien n'a changé : un remplissage en cours va jusqu'au bout.
            if (animation?.isRunning != true) {
                avance = 1f
                invalidate()
            }
            return
        }
        animation?.cancel()
        this.prevenir = prevenir
        if (!ValueAnimator.areAnimatorsEnabled()) {
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

    /** Rejoue le remplissage depuis zéro avec l'état déjà montré. */
    fun rejouer(prevenir: Boolean = true) {
        etat?.let { montrer(it, depuisZero = true, prevenir = prevenir) }
    }

    private fun remplir() {
        avance = 0f
        animation = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 900L
            interpolator = DecelerateInterpolator(1.6f)
            addUpdateListener {
                avance = it.animatedValue as Float
                invalidate()
            }
            addListener(object : AnimatorListenerAdapter() {
                private var annule = false
                override fun onAnimationCancel(animation: Animator) {
                    annule = true
                }

                override fun onAnimationEnd(animation: Animator) {
                    if (!annule && prevenir) surRempli?.invoke()
                }
            })
            start()
        }
    }

    /**
     * La fête d'un anneau qui vient de se fermer, comme sur la montre : les
     * anneaux battent deux fois, et celui qui s'est fermé passe devant le
     * temps que sa légende se lise, puis rend la place aux autres.
     */
    fun feter(quel: Anneaux.Quel) {
        if (!ValueAnimator.areAnimatorsEnabled()) return
        mettreEnAvant(quel)
        animate().cancel()
        fun battement(reste: Int) {
            animate().scaleX(1.07f).scaleY(1.07f).setDuration(180L)
                .setInterpolator(DecelerateInterpolator())
                .withEndAction {
                    animate().scaleX(1f).scaleY(1f).setDuration(260L)
                        .setInterpolator(OvershootInterpolator(3f))
                        .withEndAction { if (reste > 1) battement(reste - 1) }
                        .start()
                }
                .start()
        }
        battement(2)
        postDelayed({ if (misEnAvant == quel) mettreEnAvant(null) }, 2800L)
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
        animate().cancel()
        scaleX = 1f
        scaleY = 1f
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
                    performHapticFeedback(android.view.HapticFeedbackConstants.CONTEXT_CLICK)
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
