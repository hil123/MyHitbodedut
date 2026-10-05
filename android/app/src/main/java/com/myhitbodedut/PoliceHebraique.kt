package com.myhitbodedut

import android.graphics.Typeface
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat

/**
 * La police des textes saints en hébreu (demande du 04/10) : psaumes,
 * prières, chizuk, sources de la bibliothèque. L'interface, boutons et
 * menus, garde la police du téléphone.
 */
object PoliceHebraique {

    @Volatile
    private var police: Typeface? = null

    /** Pose la police sur [vue], en gardant le gras s'il y en a. */
    fun appliquer(vue: TextView) {
        val base = police ?: ResourcesCompat.getFont(vue.context, R.font.hebreu_serif)?.also { police = it } ?: return
        val gras = vue.typeface?.isBold == true
        vue.typeface = if (gras) Typeface.create(base, Typeface.BOLD) else base
    }
}
