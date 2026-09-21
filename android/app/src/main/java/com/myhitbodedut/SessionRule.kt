package com.myhitbodedut

/**
 * L'arithmetique d'une seance, a l'ecart de tout contexte Android pour
 * qu'elle puisse etre mise a l'epreuve.
 *
 * Une seance a une duree choisie, ou pas de duree du tout. La seance
 * ouverte n'a pas de fin inscrite : c'est la duree nulle qui la designe,
 * et rien d'autre que le geste de celui qui prie ne l'arrete. Sa duree
 * reelle est mesuree comme celle des autres, pour que la serie et le
 * total continuent de compter.
 */
object SessionRule {

    /** La duree d'une seance sans duree. */
    const val OUVERTE = 0

    fun estOuverte(debutMillis: Long, duree: Int): Boolean =
        debutMillis > 0L && duree == OUVERTE

    /**
     * Une seance ouverte court tant qu'elle n'a pas ete close a la main ;
     * une seance minutee court jusqu'a son terme.
     */
    fun enCours(debutMillis: Long, finMillis: Long, duree: Int, maintenant: Long): Boolean =
        if (estOuverte(debutMillis, duree)) true else finMillis > maintenant

    /** Les minutes pleines ecoulees depuis le debut. */
    fun minutesEcoulees(debutMillis: Long, maintenant: Long): Int {
        if (debutMillis <= 0L) return 0
        return ((maintenant - debutMillis) / 60_000L).toInt().coerceAtLeast(0)
    }

    /**
     * Les minutes restantes, arrondies au superieur : « 1 min » doit
     * s'afficher jusqu'a la toute fin, pas disparaitre a 59 secondes.
     */
    fun minutesRestantes(finMillis: Long, maintenant: Long): Int {
        val reste = finMillis - maintenant
        if (reste <= 0L) return 0
        return ((reste + 59_999L) / 60_000L).toInt()
    }
}
