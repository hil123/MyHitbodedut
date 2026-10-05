package com.myhitbodedut

import java.time.LocalDate

/**
 * Les trois anneaux de l'accueil, a la maniere des anneaux d'activite
 * d'Apple (demande du 01/10) : de l'interieur vers l'exterieur,
 *
 *  - le JOUR : les minutes faites aujourd'hui sur l'objectif quotidien ;
 *  - la SEMAINE : les jours faits sur les jours dus (JaugeSemaine) ;
 *  - la SERIE : les jours d'affilee sur le prochain palier.
 *
 * Un anneau ne se « ferme » pas a l'objectif : au-dela, il repasse sur
 * lui-meme, comme chez Apple ou 150 % font un tour et demi. C'est la
 * fraction brute qui le dit ; le dessin en tire les tours entiers et le
 * reste.
 *
 * Tout est calcule ici, sans Android, pour etre mis a l'epreuve.
 */
object Anneaux {

    /** Les paliers de la serie ; au-dela du dernier, de 365 en 365. */
    val PALIERS = intArrayOf(7, 14, 30, 60, 100, 180, 365)

    enum class Quel { JOUR, SEMAINE, SERIE }

    data class Anneau(val quel: Quel, val fait: Int, val objectif: Int) {
        /** Peut depasser 1 : c'est le second tour. Sans objectif, rien. */
        val fraction: Float
            get() = if (objectif <= 0) 0f else fait.toFloat() / objectif

        /** Les tours complets deja faits. */
        val tours: Int get() = fraction.toInt()

        /** Ce qui depasse les tours complets, de 0 a 1. */
        val reste: Float get() = fraction - tours

        val atteint: Boolean get() = objectif > 0 && fait >= objectif
    }

    data class Etat(val jour: Anneau, val semaine: Anneau, val serie: Anneau) {
        /** Du centre vers l'exterieur : l'ordre de dessin. */
        val duCentre: List<Anneau> get() = listOf(jour, semaine, serie)
        fun anneau(quel: Quel): Anneau = duCentre.first { it.quel == quel }
    }

    /**
     * Le palier que vise la serie : le premier qui l'atteint ou la depasse,
     * pour que le jour ou l'on y arrive l'anneau soit plein. Le lendemain le
     * palier suivant commence, et l'anneau repart plus bas : c'est voulu, un
     * palier atteint est acquis, le suivant est a faire.
     */
    fun palier(serie: Int): Int {
        PALIERS.firstOrNull { it >= serie }?.let { return it }
        // Au-dela d'un an : le prochain multiple de 365.
        return ((serie + 364) / 365) * 365
    }

    fun etat(
        minutesJour: Int,
        objectifJour: Int,
        semaine: JaugeSemaine.Progression,
        serie: Int
    ): Etat = Etat(
        jour = Anneau(Quel.JOUR, minutesJour.coerceAtLeast(0), objectifJour.coerceAtLeast(0)),
        semaine = Anneau(Quel.SEMAINE, semaine.faits, semaine.dus),
        serie = Anneau(Quel.SERIE, serie.coerceAtLeast(0), palier(serie.coerceAtLeast(0)))
    )

    /**
     * L'objectif quotidien effectif : celui qui est regle, sinon la duree
     * habituelle d'une hitbodedout. Un objectif nul ou negatif n'a pas de
     * sens ; il retombe sur la duree.
     */
    /**
     * Ce qui rend « nouveau » un anneau fermé : la date pour le jour, le
     * dimanche de la semaine pour la semaine, le palier pour la série. Un
     * anneau se fête une fois par marque, pas à chaque ouverture de
     * l'accueil (demande du 04/10 : des animations automatiques).
     */
    fun marque(anneau: Anneau, aujourdhui: LocalDate): String? {
        if (!anneau.atteint) return null
        return when (anneau.quel) {
            Quel.JOUR -> "jour:$aujourdhui"
            Quel.SEMAINE -> "semaine:${aujourdhui.minusDays((aujourdhui.dayOfWeek.value % 7).toLong())}"
            Quel.SERIE -> "serie:${anneau.objectif}"
        }
    }

    /** Les marques des anneaux fermés en ce moment : ce qu'on garde une fois la fête faite. */
    fun marques(etat: Etat, aujourdhui: LocalDate): Set<String> =
        etat.duCentre.mapNotNull { marque(it, aujourdhui) }.toSet()

    /** Les anneaux fermés qui n'ont pas encore été fêtés, du centre vers l'extérieur. */
    fun aFeter(etat: Etat, aujourdhui: LocalDate, dejaFetes: Set<String>): List<Quel> =
        etat.duCentre.filter { a -> marque(a, aujourdhui)?.let { it !in dejaFetes } ?: false }.map { it.quel }

    fun objectifEffectif(regle: Int, dureeHabituelle: Int): Int =
        if (regle > 0) regle else dureeHabituelle.coerceAtLeast(1)
}
