package com.myhitbodedut

import android.app.Activity
import android.view.View
import android.view.ViewGroup
import android.widget.AbsListView
import android.widget.ScrollView

/**
 * La banderole du haut (bg_hero, android:tag="bandeau") s'efface quand on fait
 * défiler vers le bas et revient en haut de la page (demande du 13/10).
 *
 * Posée pour tous les écrans à la fois (Apparence.installer) : le premier
 * conteneur qui défile sous la banderole est surveillé, sans prendre la place de
 * l'écouteur de défilement que l'écran aurait déjà posé.
 */
object BandeauDefilant {

    private const val DUREE_MS = 220L

    fun installer(activity: Activity) {
        val racine = activity.findViewById<ViewGroup>(android.R.id.content) ?: return
        val bandeau = racine.findViewWithTag<View>("bandeau") ?: return
        if (bandeau.getTag(R.id.bandeau_defilant) != null) return
        val defile = premierDefilant(racine) ?: return
        bandeau.setTag(R.id.bandeau_defilant, true)
        var cache = false
        defile.viewTreeObserver.addOnScrollChangedListener {
            val descendu = defile.canScrollVertically(-1)
            if (descendu && !cache && reste(defile) > bandeau.height + marge(defile)) {
                cache = true
                bandeau.animate().alpha(0f).setDuration(DUREE_MS).withEndAction { if (cache) bandeau.visibility = View.GONE }
            } else if (!descendu && cache) {
                cache = false
                bandeau.visibility = View.VISIBLE
                bandeau.animate().alpha(1f).setDuration(DUREE_MS)
            }
        }
    }

    /**
     * Ce que la page peut défiler en tout : la banderole ne s'efface que si la page la
     * dépasse nettement, sinon la place qu'elle libère ramènerait la page en haut, et elle
     * reviendrait aussitôt. Les listes (siddour, Tehilim, livres) comptaient pour infinies :
     * un texte court comme Acher Yatsar faisait clignoter la banderole sans fin (03/11).
     */
    private fun reste(v: View): Int = when (v) {
        is ScrollView -> (v.getChildAt(0)?.height ?: 0) + v.paddingTop + v.paddingBottom - v.height
        is androidx.recyclerview.widget.RecyclerView -> v.computeVerticalScrollRange() - v.computeVerticalScrollExtent()
        else -> Int.MAX_VALUE
    }

    /** Un peu de jeu en plus : la hauteur d'une liste se devine à quelques pixels près. */
    private fun marge(v: View): Int = (48 * v.resources.displayMetrics.density).toInt()

    private fun premierDefilant(v: View): View? {
        if (v is ScrollView || v is AbsListView || v.javaClass.name.endsWith("RecyclerView") ||
            v.javaClass.name.endsWith("NestedScrollView")) return v
        if (v is ViewGroup) for (i in 0 until v.childCount) premierDefilant(v.getChildAt(i))?.let { return it }
        return null
    }
}
