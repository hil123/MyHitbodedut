package com.myhitbodedut

import android.animation.ValueAnimator
import android.view.View
import android.view.ViewGroup
import android.view.animation.PathInterpolator
import android.widget.TextView

/**
 * Les effets (19/10, « ajoute des effets professionnels ») : discrets, courts, sur
 * la courbe de décélération des écrans Material. Un écran qu'on ouvre fait entrer
 * ses blocs l'un après l'autre ; une carte qui change de texte se fond ; un chiffre
 * monte jusqu'à sa valeur. Animations coupées sur le téléphone : rien ne bouge, tout
 * est à sa place — ValueAnimator le dit, comme pour les trophées.
 */
object Effets {

    val actifs: Boolean get() = ValueAnimator.areAnimatorsEnabled()

    private val courbe = PathInterpolator(0.2f, 0.8f, 0.2f, 1f)

    private fun dp(v: View, n: Int) = n * v.resources.displayMetrics.density

    /** Les vues entrent en cascade : 14 dp plus bas, transparentes, puis à leur place, 45 ms d'écart. */
    fun cascade(vues: List<View>, depart: Long = 60) {
        if (!actifs) return
        vues.forEachIndexed { i, v ->
            v.alpha = 0f
            v.translationY = dp(v, 14)
            v.animate().alpha(1f).translationY(0f)
                .setStartDelay(depart + minOf(i, 8) * 45L).setDuration(420).setInterpolator(courbe).start()
        }
    }

    /** Les enfants d'un groupe, en cascade. */
    fun enfants(groupe: ViewGroup, depart: Long = 60) = cascade((0 until groupe.childCount).map(groupe::getChildAt), depart)

    /** Une carte dont le contenu vient de changer se fond. */
    fun fondu(v: View) {
        if (!actifs) return
        v.animate().cancel()
        v.alpha = 0f
        v.translationY = dp(v, 6)
        v.animate().alpha(1f).translationY(0f).setStartDelay(0).setDuration(320).setInterpolator(courbe).start()
    }

    /** Un nombre qui monte de 0 à [fin] en 0,7 s, en ralentissant ; [texte] le met en forme. */
    fun compte(tv: TextView, fin: Int, texte: (Int) -> String = { it.toString() }) {
        tv.text = texte(fin)
        if (!actifs || fin <= 0) return
        ValueAnimator.ofInt(0, fin).apply {
            duration = 700
            startDelay = 120
            interpolator = courbe
            addUpdateListener { tv.text = texte(it.animatedValue as Int) }
            start()
        }
    }

    /** La part d'une barre, de 0 à 1, en 0,7 s ; [maj] redessine. */
    fun remplir(maj: (Float) -> Unit) {
        if (!actifs) { maj(1f); return }
        ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 700
            startDelay = 300
            interpolator = courbe
            addUpdateListener { maj(it.animatedValue as Float) }
            start()
        }
    }

    /** Une carte qu'on touche se creuse un peu (res/animator/presse.xml). */
    fun presse(v: View) {
        v.stateListAnimator = android.animation.AnimatorInflater.loadStateListAnimator(v.context, R.animator.presse)
    }
}
