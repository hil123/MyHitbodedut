package com.myhitbodedut

/**
 * La jauge de l'accueil : les jours faits de la semaine, sur les jours dus.
 *
 * Un Chabbat ou un jour de pause n'est pas dû : il ne creuse pas la jauge.
 * Une séance faite un jour exempté compte quand même — on ne retire pas à
 * quelqu'un ce qu'il a fait.
 */
object JaugeSemaine {

    data class Progression(val faits: Int, val dus: Int) {
        /** De 0 à 1 ; une semaine sans jour dû reste vide plutôt que pleine. */
        val fraction: Float
            get() = if (dus == 0) 0f else (faits.toFloat() / dus).coerceIn(0f, 1f)
    }

    /**
     * La durée d'un jour fait, assez courte pour la case de la bande (14/10,
     * « le temps à la place du ✓ ») : « 25′ », « 1h », « 1h20 ».
     */
    fun dureeCourte(minutes: Int): String = when {
        minutes < 60 -> "$minutes′"
        minutes % 60 == 0 -> "${minutes / 60}h"
        else -> "${minutes / 60}h%02d".format(minutes % 60)
    }

    fun progression(jours: List<WeekDay>): Progression = Progression(
        faits = jours.count { it.done },
        dus = jours.count { it.done || !it.exempt }
    )
}
