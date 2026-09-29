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

    /** Ce que le compteur d'une seance en cours a le droit de dire. */
    enum class Compteur {
        /** Le temps restant, pour une seance minutee. */
        RESTE,

        /** Le temps deja passe, pour une seance ouverte. */
        ECOULE,

        /** Rien : le compteur est masque jusqu'au signal de fin. */
        MASQUE,
    }

    /**
     * Le choix est fait une fois ici pour les trois endroits qui affichent
     * un compteur — l'ecran de seance, la notification en cours, l'ecran de
     * barrage. Le masquage l'emporte sur le reste : promettre « rien a
     * l'ecran » et laisser le chiffre dans la notification ne serait pas
     * tenir parole.
     */
    fun compteur(ouverte: Boolean, masque: Boolean): Compteur = when {
        masque -> Compteur.MASQUE
        ouverte -> Compteur.ECOULE
        else -> Compteur.RESTE
    }

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
     * Les minutes a inscrire au journal quand la seance se clot.
     *
     * Une seance minutee ne compte jamais plus que sa duree : sa fin peut
     * n'etre constatee que bien plus tard (ecran eteint, application tuee,
     * telephone redemarre), et c'est alors l'heure du constat qui etait
     * inscrite — 20 minutes choisies devenaient 60 (revue du 29/09).
     * Arretee avant son terme, elle compte le temps reellement passe.
     *
     * Une seance ouverte compte le temps passe, borne a [PLAFOND_OUVERTE]
     * minutes : oubliee en marche, elle ne doit pas inscrire une journee.
     */
    fun minutesAInscrire(debutMillis: Long, finMillis: Long, maintenant: Long): Int {
        if (debutMillis <= 0L) return 0
        val borne = if (finMillis > 0L) minOf(maintenant, finMillis) else maintenant
        val minutes = ((borne - debutMillis) / 60_000L).toInt().coerceAtLeast(0)
        return if (finMillis > 0L) minutes else minutes.coerceAtMost(PLAFOND_OUVERTE)
    }

    /**
     * Une seance inscrite dont personne ne s'occupe plus (le service a ete
     * tue) est-elle terminee ? Minutee : son terme est passe. Ouverte : elle
     * a atteint le plafond.
     */
    fun estEchue(debutMillis: Long, finMillis: Long, duree: Int, maintenant: Long): Boolean {
        if (debutMillis <= 0L) return false
        if (estOuverte(debutMillis, duree)) {
            return maintenant - debutMillis >= PLAFOND_OUVERTE * 60_000L
        }
        return finMillis <= maintenant
    }

    /** La plus longue seance qu'on puisse choisir ; une seance ouverte s'y arrete aussi. */
    const val PLAFOND_OUVERTE = 240

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
