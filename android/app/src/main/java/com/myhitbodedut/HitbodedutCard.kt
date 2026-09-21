package com.myhitbodedut

import android.view.View
import android.widget.ImageView
import android.widget.TextView

/**
 * Le remplissage de view_hitbodedut.xml.
 *
 * La carte ne retient rien : l'indice du temps courant appartient à
 * l'écran de séance, et repart à zéro à chaque séance. Un guide qui
 * reprendrait où l'on s'était arrêté la veille rappellerait une dette au
 * lieu d'ouvrir une porte.
 */
object HitbodedutCard {

    /**
     * Affiche le temps [index] et branche les deux boutons.
     *
     * @param onChange appelé avec le nouvel indice, à l'écran de le garder.
     */
    fun bind(card: View, index: Int, onChange: (Int) -> Unit) {
        card.visibility = View.VISIBLE
        val mouvement = Hitbodedut.mouvement(index)
        val context = card.context

        card.findViewById<ImageView>(R.id.hitbodedut_art)
            .setImageResource(mouvement.illustration)
        card.findViewById<TextView>(R.id.hitbodedut_step).text = context.getString(
            R.string.hitbodedut_step, index + 1, Hitbodedut.MOUVEMENTS.size
        )
        card.findViewById<TextView>(R.id.hitbodedut_he).text = mouvement.he
        card.findViewById<TextView>(R.id.hitbodedut_title).setText(mouvement.titre)
        card.findViewById<TextView>(R.id.hitbodedut_invite).setText(mouvement.invite)
        card.findViewById<TextView>(R.id.hitbodedut_source).text = Hitbodedut.SOURCE

        val previous = card.findViewById<TextView>(R.id.hitbodedut_previous)
        previous.visibility = if (Hitbodedut.estPremier(index)) View.INVISIBLE else View.VISIBLE
        previous.setOnClickListener { onChange(Hitbodedut.precedent(index)) }

        val next = card.findViewById<TextView>(R.id.hitbodedut_next)
        next.visibility = if (Hitbodedut.estDernier(index)) View.INVISIBLE else View.VISIBLE
        next.setOnClickListener { onChange(Hitbodedut.suivant(index)) }
    }

    fun hide(card: View) {
        card.visibility = View.GONE
    }
}
