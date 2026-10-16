package com.myhitbodedut

import android.animation.ValueAnimator
import android.app.Activity
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import androidx.core.graphics.ColorUtils
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

/**
 * L'écran pendant la pratique (16/10, point 2) : six secondes sans toucher,
 * et tout s'efface sauf le minuteur, sa lueur et « Terminer » ; les barres du
 * téléphone se cachent, le fond s'assombrit. Un toucher n'importe où ramène
 * tout pour six secondes, sans déclencher ce qu'il y avait dessous : on ne
 * touche pas un bouton qu'on ne voyait pas.
 *
 * Ce qui s'efface ne change que d'opacité : render(), qui repasse chaque
 * seconde, garde la main sur ce qui est visible ou non.
 */
class PratiqueCalme(
    private val activite: Activity,
    private val fond: View,
    private val couleurFond: Int,
    private val effaces: () -> List<View>,
) {
    private val handler = Handler(Looper.getMainLooper())
    private val sombre = ColorUtils.blendARGB(couleurFond, 0xFF000000.toInt(), 0.55f)
    private var enCours = false
    var calme = false
        private set
    /** Le geste qui a réveillé l'écran : avalé jusqu'au bout. */
    private var avale = false

    private val endormir = Runnable { entrer() }

    /** À chaque passage de render() : en pratique ou non. */
    fun suivre(pratique: Boolean) {
        if (pratique == enCours) return
        enCours = pratique
        if (pratique) attendre() else { handler.removeCallbacks(endormir); sortir(anime = false) }
    }

    /** À appeler depuis dispatchTouchEvent : true si le geste est avalé. */
    fun toucher(e: MotionEvent): Boolean {
        if (e.actionMasked == MotionEvent.ACTION_DOWN) {
            avale = calme
            if (calme) sortir(anime = true)
            if (enCours) attendre()
        }
        val garde = avale
        if (e.actionMasked == MotionEvent.ACTION_UP || e.actionMasked == MotionEvent.ACTION_CANCEL) avale = false
        return garde
    }

    fun liberer() {
        handler.removeCallbacks(endormir)
        barres(true)
    }

    private fun attendre() {
        handler.removeCallbacks(endormir)
        handler.postDelayed(endormir, DELAI_MS)
    }

    private fun entrer() {
        if (!enCours || calme) return
        calme = true
        val d = duree(900L)
        effaces().forEach { it.animate().alpha(0f).setDuration(d).start() }
        teinter(couleurFond, sombre, d)
        barres(false)
    }

    private fun sortir(anime: Boolean) {
        val etait = calme
        calme = false
        val d = if (anime) duree(250L) else 0L
        effaces().forEach { it.animate().cancel(); if (d == 0L) it.alpha = 1f else it.animate().alpha(1f).setDuration(d).start() }
        if (etait) teinter(sombre, couleurFond, d) else fond.setBackgroundColor(couleurFond)
        barres(true)
    }

    private fun teinter(de: Int, a: Int, d: Long) {
        if (d == 0L) { fond.setBackgroundColor(a); return }
        ValueAnimator.ofArgb(de, a).apply {
            duration = d
            addUpdateListener { fond.setBackgroundColor(it.animatedValue as Int) }
            start()
        }
    }

    private fun barres(visibles: Boolean) {
        val c = WindowCompat.getInsetsController(activite.window, activite.window.decorView)
        if (visibles) c.show(WindowInsetsCompat.Type.systemBars())
        else {
            c.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            c.hide(WindowInsetsCompat.Type.systemBars())
        }
    }

    /** Animations coupées sur le téléphone : tout se fait d'un coup. */
    private fun duree(ms: Long) = if (ValueAnimator.areAnimatorsEnabled()) ms else 0L

    companion object {
        const val DELAI_MS = 6_000L
    }
}
