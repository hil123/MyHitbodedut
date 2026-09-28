package com.myhitbodedut

import kotlin.random.Random

/**
 * La lecture aléatoire (demande du 28/09) : un morceau au hasard, puis un
 * autre, et aucun ne revient avant que tous aient été entendus. Au passage
 * d'un tour au suivant, le dernier morceau joué n'ouvre pas le nouveau tour :
 * sinon on l'entendrait deux fois de suite.
 *
 * Sans Android, pour se tester : ne manipule que des identifiants.
 */
class MelangeMusique(private val hasard: Random = Random.Default) {

    private val sac = ArrayDeque<String>()

    var dernier: String? = null
        private set

    /** Le prochain morceau parmi `ids`, ou null s'il n'y en a aucun. */
    fun suivant(ids: List<String>): String? {
        if (ids.isEmpty()) return null
        // Un morceau retiré entre-temps de la bibliothèque ne doit plus sortir.
        sac.removeAll { it !in ids }
        if (sac.isEmpty()) {
            val tour = ids.distinct().shuffled(hasard).toMutableList()
            if (tour.size > 1 && tour.first() == dernier) {
                tour[0] = tour[tour.lastIndex].also { tour[tour.lastIndex] = tour[0] }
            }
            sac.addAll(tour)
        }
        return sac.removeFirst().also { dernier = it }
    }
}
