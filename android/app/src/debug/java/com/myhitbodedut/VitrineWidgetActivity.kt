package com.myhitbodedut

import android.app.Activity
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.TypedValue
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

/**
 * La vitrine des vignettes, pour l'édition de test seulement : les trois
 * tailles de StreakWidget, l'une sous l'autre, sur un fond qui imite un
 * fond d'écran, à leur place ordinaire (2 × 2, 4 × 2 et 4 × 4 cases).
 *
 * Elles sont construites exactement comme l'écran d'accueil les reçoit
 * (StreakWidget.apercu, les mêmes RemoteViews), puis gonflées ici par
 * RemoteViews.apply, comme le ferait un lanceur. On peut ainsi les voir et
 * les capturer sur un émulateur sans lanceur à piloter :
 *
 *     adb shell am start -n com.myhitbodedut.debug/com.myhitbodedut.VitrineWidgetActivity
 *
 * Elles montrent l'état du moment, dans la langue et les couleurs choisies.
 */
class VitrineWidgetActivity : Activity() {

    private fun dp(valeur: Float): Int =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, valeur, resources.displayMetrics).toInt()

    private lateinit var colonne: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        colonne = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(16f), dp(56f), dp(16f), dp(48f))
        }
        val defilement = ScrollView(this).apply {
            isFillViewport = true
            // Un fond d'écran neutre : un crépuscule doux, ni clair ni sombre.
            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(Color.rgb(0x2F, 0x3B, 0x4E), Color.rgb(0x6E, 0x7F, 0x99), Color.rgb(0xC9, 0xB8, 0xA8))
            )
            addView(colonne)
        }
        setContentView(defilement)
    }

    /** À chaque retour : l'état a pu changer (une hitbodedout, une palette). */
    override fun onResume() {
        super.onResume()
        remplir()
    }

    private fun remplir() {
        colonne.removeAllViews()
        // Le thème du système, comme chez un lanceur, et non celui de l'application.
        val gonfleur = ContextThemeWrapper(this, android.R.style.Theme_DeviceDefault)
        WidgetTaille.entries.forEach { taille ->
            colonne.addView(TextView(this).apply {
                text = "${taille.name} · ${taille.largeurType.toInt()} × ${taille.hauteurType.toInt()} dp"
                setTextColor(Color.argb(200, 255, 255, 255))
                textSize = 12f
                setPadding(0, dp(18f), 0, dp(8f))
            })
            val place = FrameLayout(this)
            colonne.addView(place, LinearLayout.LayoutParams(dp(taille.largeurType), dp(taille.hauteurType)))
            val vue = runCatching {
                StreakWidget.apercu(this, taille).apply(gonfleur, place)
            }.getOrElse { erreur ->
                // Une vignette que le lanceur refuserait : on le dit ici plutôt qu'un vide.
                TextView(this).apply {
                    text = erreur.toString()
                    setTextColor(Color.WHITE)
                    textSize = 11f
                }
            }
            place.addView(vue, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
        }
    }
}
