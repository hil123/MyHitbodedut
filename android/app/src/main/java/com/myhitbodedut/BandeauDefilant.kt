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
        // Marge du bas de la liste avant qu'on y ajoute la place de la banderole (null : rien d'ajouté).
        var basOrigine: Int? = null
        var coupeOrigine = true
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
                bandeau.animate().alpha(0f).setDuration(DUREE_MS).withEndAction {
                    if (!cache) return@withEndAction
                    // La place qu'elle libère remonterait tout le texte d'un coup : juste avant
                    // l'image suivante, on mesure de combien le texte est monté et on le redescend
                    // d'autant — rien ne bouge à l'écran (03/11 ; décalé d'une image, il sautait).
                    val avant = IntArray(2).also { defile.getLocationOnScreen(it) }[1]
                    // Ce qui est sous la banderole sans contenir le texte (la bande des signets, son
                    // trait) : il remonte de toute sa hauteur ; on le fait glisser au lieu de sauter.
                    val dessous = (bandeau.parent as? ViewGroup)?.let { p ->
                        (p.indexOfChild(bandeau) + 1 until p.childCount).map { p.getChildAt(it) }
                            .filter { it.visibility == View.VISIBLE && !contient(it, defile) }
                    }.orEmpty()
                    // La liste reçoit en bas la place libérée (03/11) : sans elle, quand il reste moins
                    // qu'une banderole de texte sous l'écran (sections repliées, que la longueur estimée
                    // ne voit pas), Android recalait la liste vers le haut, elle retombait en haut de
                    // page et la banderole revenait (vidéo de l'étape 24, Cha'harit, cinq fois).
                    if (defile is androidx.recyclerview.widget.RecyclerView && basOrigine == null) {
                        basOrigine = defile.paddingBottom
                        coupeOrigine = defile.clipToPadding
                        defile.clipToPadding = false // le texte passe dans cette marge, pas de bande vide
                        defile.setPadding(defile.paddingLeft, defile.paddingTop, defile.paddingRight, defile.paddingBottom + bandeau.height)
                    }
                    bandeau.visibility = View.GONE
                    // Une liste se recale parfois d'elle-même sur la place libérée, parfois non, et pas
                    // toujours à la même image : la compensation unique la redescendait alors de trop,
                    // jusqu'en haut, et la banderole revenait (étape 24, Cha'harit, 03/11). On retient la
                    // ligne du haut et son ordonnée à l'écran ; pendant quelques images, on l'y remet,
                    // défilement du doigt compris : seul l'écart venu de la mise en page est corrigé.
                    val rv = defile as? androidx.recyclerview.widget.RecyclerView
                    val repere = rv?.getChildAt(0)
                    val position = repere?.let { rv.getChildAdapterPosition(it) } ?: androidx.recyclerview.widget.RecyclerView.NO_POSITION
                    val yAvant = repere?.let { ecranY(it) } ?: 0
                    val cumulAvant = cumul
                    var propre = 0 // ce que nous avons défilé nous-mêmes
                    val fin = android.os.SystemClock.uptimeMillis() + 400
                    var glisse = false
                    val vue = defile.viewTreeObserver
                    vue.addOnPreDrawListener(object : android.view.ViewTreeObserver.OnPreDrawListener {
                        override fun onPreDraw(): Boolean {
                            val monte = avant - ecranY(defile)
                            if (!glisse && monte > 0) { glisse = true; glisser(dessous, monte) }
                            if (rv == null || position == androidx.recyclerview.widget.RecyclerView.NO_POSITION) {
                                // Une ScrollView ne se recale pas d'elle-même : une seule compensation.
                                if (defile.viewTreeObserver.isAlive) defile.viewTreeObserver.removeOnPreDrawListener(this)
                                if (defile !is ScrollView || monte <= 0 || defile.scrollY <= monte) return true
                                defile.scrollBy(0, -monte)
                                return false
                            }
                            if (android.os.SystemClock.uptimeMillis() > fin || !defile.viewTreeObserver.isAlive) {
                                defile.viewTreeObserver.takeIf { it.isAlive }?.removeOnPreDrawListener(this)
                                return true
                            }
                            val ligne = rv.layoutManager?.findViewByPosition(position) ?: return true
                            val attendu = yAvant - (cumul - cumulAvant - propre)
                            // Pas jusqu'en haut : la banderole reviendrait par notre faute.
                            val ecart = (ecranY(ligne) - attendu).coerceAtLeast(-(cumul - 1).coerceAtLeast(0))
                            if (ecart == 0) return true
                            propre += ecart
                            rv.scrollBy(0, ecart)
                            return false // cette image est redessinée, texte en place
                        }
                    })
                }
            } else if (!descendu && cache) {
                cache = false
                basOrigine?.let {
                    defile.setPadding(defile.paddingLeft, defile.paddingTop, defile.paddingRight, it)
                    (defile as ViewGroup).clipToPadding = coupeOrigine
                }
                basOrigine = null
                bandeau.visibility = View.VISIBLE
                bandeau.animate().alpha(1f).setDuration(DUREE_MS)
            }
        }
    }

    private fun ecranY(v: View): Int = IntArray(2).also { v.getLocationOnScreen(it) }[1]

    /** [v] est-elle [cible] ou l'un de ses parents ? */
    private fun contient(v: View, cible: View): Boolean {
        var c: View? = cible
        while (c != null) { if (c === v) return true; c = c.parent as? View }
        return false
    }

    /**
     * La bande sous la banderole (03/11) : remontée d'un coup de [hauteur] quand celle-ci passe à
     * GONE, elle repart d'où elle était et glisse à sa place, au-dessus du texte qui, lui, ne bouge pas.
     */
    private fun glisser(vues: List<View>, hauteur: Int) {
        vues.forEach { v ->
            v.animate().cancel()
            // Sans fond, elle laisserait voir le texte qu'elle survole : celui de la page, le temps du glissement.
            val sansFond = v.background == null
            if (sansFond) fondDeLaPage(v)?.let { v.setBackgroundColor(it) }
            v.translationY = hauteur.toFloat()
            v.translationZ = 1f
            v.animate().translationY(0f).setDuration(DUREE_MS)
                .setInterpolator(android.view.animation.DecelerateInterpolator())
                .withEndAction { v.translationZ = 0f; if (sansFond) v.background = null }
        }
    }

    /** La couleur unie du premier parent qui en a une (la page, en mode jour comme de nuit). */
    private fun fondDeLaPage(v: View): Int? {
        var p = v.parent as? View
        while (p != null) {
            (p.background as? android.graphics.drawable.ColorDrawable)?.let { return it.color }
            p = p.parent as? View
        }
        return null
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
