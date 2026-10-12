package com.myhitbodedut

import android.graphics.Typeface
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat

/**
 * La police des textes saints en hébreu (demande du 04/10) : psaumes,
 * prières, chizuk, sources de la bibliothèque, halakha, hilloulot.
 * L'interface, boutons et menus, garde la police du téléphone.
 *
 * Au choix depuis le 12/10 (bouton « Aa », pour tous les écrans) : la
 * police hébraïque à empattements de l'application, celle du téléphone
 * sans empattements, ou l'arrondie de l'application. Toutes sont dans
 * l'application ou le téléphone : rien ne se télécharge.
 */
object PoliceHebraique {

    enum class Police(val cle: String, val libelle: Int) {
        SERIF("serif", R.string.police_serif),
        SANS("sans", R.string.police_sans),
        ARRONDIE("arrondie", R.string.police_arrondie),
    }

    private val polices = mutableMapOf<Police, Typeface>()

    /**
     * Pose la police choisie sur [vue], en gardant le gras s'il y en a ; la
     * vue est marquée pour que ReglagesLecture.appliquer la repose quand on
     * change de police.
     */
    fun appliquer(vue: TextView) {
        val choix = Store.lecturePolice(vue.context)
        val base = polices[choix] ?: when (choix) {
            Police.SERIF -> ResourcesCompat.getFont(vue.context, R.font.hebreu_serif)
            Police.ARRONDIE -> ResourcesCompat.getFont(vue.context, R.font.varela_round)
            Police.SANS -> Typeface.SANS_SERIF
        }?.also { polices[choix] = it } ?: return
        val gras = vue.typeface?.isBold == true
        vue.setTag(R.id.lecture_police, true)
        vue.typeface = if (gras) Typeface.create(base, Typeface.BOLD) else base
    }
}
