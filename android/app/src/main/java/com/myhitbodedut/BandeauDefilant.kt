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
        // Le défilement réel d'une liste, au pixel près, additionné à chaque pas (03/11) : celui
        // qu'Android estime (computeVerticalScrollOffset) se trompe quand les lignes n'ont pas la
        // même hauteur, et la banderole partait trop tôt puis revenait en boucle.
        var cumul = 0
        (defile as? androidx.recyclerview.widget.RecyclerView)?.addOnScrollListener(object : androidx.recyclerview.widget.RecyclerView.OnScrollListener() {
            override fun onScrolled(rv: androidx.recyclerview.widget.RecyclerView, dx: Int, dy: Int) {
                cumul = if (rv.canScrollVertically(-1)) (cumul + dy).coerceAtLeast(0) else 0
            }
        })
        fun decalage(): Int = when (defile) {
            is ScrollView -> defile.scrollY
            is androidx.recyclerview.widget.RecyclerView -> cumul
            else -> if (defile.canScrollVertically(-1)) Int.MAX_VALUE else 0
        }
        defile.viewTreeObserver.addOnScrollChangedListener {
            val descendu = defile.canScrollVertically(-1)
            val h = bandeau.height
            // Effacée seulement après un vrai défilement (plus que sa hauteur et une marge) : en
            // descendant doucement, le premier pixel ne la fait plus partir (03/11).
            if (descendu && !cache && decalage() > h + marge(defile) && reste(defile) > h + marge(defile)) {
                cache = true
                replier(bandeau, defile, ::decalage) { cache }
            } else if (!descendu && cache) {
                cache = false
                deplier(bandeau)
            }
        }
    }

    private val mesurable = { v: View -> v is ScrollView || v is androidx.recyclerview.widget.RecyclerView }

    /**
     * La banderole se replie (03/11) : sa hauteur fond en [DUREE_MS] pendant qu'elle s'efface, et
     * à chaque image, juste avant le dessin, le texte est redescendu d'autant qu'il est monté —
     * il ne bouge pas, seule la bande sous la banderole (les signets) glisse vers le haut. Avant,
     * elle disparaissait d'un coup et cette bande sautait de toute sa hauteur.
     */
    private fun replier(bandeau: View, defile: View, decalage: () -> Int, toujoursCache: () -> Boolean) {
        val hauteur = bandeau.height
        if (hauteur <= 0) { bandeau.visibility = View.GONE; return }
        var y = ecranY(defile)
        val compenser = object : android.view.ViewTreeObserver.OnPreDrawListener {
            override fun onPreDraw(): Boolean {
                val maintenant = ecranY(defile)
                val monte = y - maintenant
                y = maintenant
                if (monte > 0 && mesurable(defile) && decalage() > monte) defile.scrollBy(0, -monte)
                return true
            }
        }
        defile.viewTreeObserver.addOnPreDrawListener(compenser)
        val lp = bandeau.layoutParams
        android.animation.ValueAnimator.ofInt(hauteur, 0).apply {
            duration = DUREE_MS
            interpolator = android.view.animation.DecelerateInterpolator()
            addUpdateListener {
                if (!toujoursCache()) { cancel(); return@addUpdateListener }
                lp.height = it.animatedValue as Int
                bandeau.alpha = (it.animatedValue as Int).toFloat() / hauteur
                bandeau.layoutParams = lp
            }
            addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    // Une dernière image compensée, puis on rend la hauteur naturelle à la vue cachée.
                    defile.post {
                        if (defile.viewTreeObserver.isAlive) defile.viewTreeObserver.removeOnPreDrawListener(compenser)
                    }
                    lp.height = ViewGroup.LayoutParams.WRAP_CONTENT
                    bandeau.layoutParams = lp
                    if (toujoursCache()) bandeau.visibility = View.GONE
                }
            })
            start()
        }
    }

    /** En haut de la page : elle revient en se dépliant, et la page descend avec elle, sans saut. */
    private fun deplier(bandeau: View) {
        bandeau.animate().cancel()
        val parent = bandeau.parent as? View
        val largeur = (parent?.width ?: 0) - (parent?.paddingLeft ?: 0) - (parent?.paddingRight ?: 0)
        bandeau.measure(View.MeasureSpec.makeMeasureSpec(largeur.coerceAtLeast(0), View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED))
        val hauteur = bandeau.measuredHeight
        val lp = bandeau.layoutParams
        bandeau.visibility = View.VISIBLE
        if (hauteur <= 0 || largeur <= 0) { lp.height = ViewGroup.LayoutParams.WRAP_CONTENT; bandeau.layoutParams = lp; bandeau.alpha = 1f; return }
        android.animation.ValueAnimator.ofInt(0, hauteur).apply {
            duration = DUREE_MS
            interpolator = android.view.animation.DecelerateInterpolator()
            addUpdateListener {
                lp.height = it.animatedValue as Int
                bandeau.alpha = (it.animatedValue as Int).toFloat() / hauteur
                bandeau.layoutParams = lp
            }
            addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    lp.height = ViewGroup.LayoutParams.WRAP_CONTENT
                    bandeau.layoutParams = lp
                    bandeau.alpha = 1f
                }
            })
            start()
        }
    }

    private fun ecranY(v: View): Int = IntArray(2).also { v.getLocationOnScreen(it) }[1]

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
