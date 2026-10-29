package com.myhitbodedut

import android.app.Activity
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable
import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat

/**
 * Les barres du système (29/10, passage à targetSdk 36).
 *
 * Android 16 dessine toute application qui le vise sous la barre d'état et la
 * barre de gestes, sans dérogation possible : `windowOptOutEdgeToEdgeEnforcement`
 * n'y agit plus. Aucun écran ne gérait ces marges. Plutôt que de les traiter
 * écran par écran, on les traite une fois, pour tous : le contenu de chaque
 * écran reçoit en marge intérieure la place des barres, de la découpe de
 * l'écran et du clavier, et la bande du haut reprend la couleur que la barre
 * d'état avait jusqu'ici (`android:statusBarColor` du thème, palette comprise).
 *
 * Le même chemin vaut sur toutes les versions d'Android, de 8 à 16 : on
 * demande partout l'affichage sous les barres, puis on rend la place. Un écran
 * qui masque les barres (PratiqueCalme) reçoit des marges nulles, donc
 * occupe tout l'écran, comme avant.
 */
object Barres {

    fun installer(activity: Activity) {
        WindowCompat.setDecorFitsSystemWindows(activity.window, false)
        val contenu = activity.findViewById<View>(android.R.id.content) ?: return
        val bande = BandeEtat(couleurBarreEtat(activity))
        contenu.background = bande
        ViewCompat.setOnApplyWindowInsetsListener(contenu) { vue, insets ->
            val m = marges(insets)
            vue.setPadding(m.gauche, m.haut, m.droite, m.bas)
            bande.hauteur = m.haut
            WindowInsetsCompat.CONSUMED
        }
    }

    data class Marges(val gauche: Int, val haut: Int, val droite: Int, val bas: Int)

    /**
     * Barres et découpe, plus le clavier en bas : sans lui, un champ de saisie
     * en bas d'écran passerait sous le clavier, Android ne redimensionnant plus
     * la fenêtre d'une application affichée sous les barres.
     */
    fun marges(insets: WindowInsetsCompat): Marges {
        val b = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
        val clavier = insets.getInsets(WindowInsetsCompat.Type.ime())
        return Marges(b.left, b.top, b.right, maxOf(b.bottom, clavier.bottom))
    }

    private fun couleurBarreEtat(activity: Activity): Int {
        val a = activity.theme.obtainStyledAttributes(intArrayOf(android.R.attr.statusBarColor))
        return try { a.getColor(0, 0) } finally { a.recycle() }
    }

    /** Peint seulement la bande de la barre d'état ; le reste reste transparent. */
    private class BandeEtat(couleur: Int) : Drawable() {
        private val pinceau = Paint().apply { color = couleur }
        var hauteur = 0
            set(v) { if (field != v) { field = v; invalidateSelf() } }

        override fun draw(canvas: Canvas) {
            if (hauteur > 0) canvas.drawRect(0f, 0f, bounds.width().toFloat(), hauteur.toFloat(), pinceau)
        }

        override fun setAlpha(alpha: Int) { pinceau.alpha = alpha }
        override fun setColorFilter(colorFilter: ColorFilter?) { pinceau.colorFilter = colorFilter }
        @Deprecated("Deprecated in Java")
        override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
    }
}
