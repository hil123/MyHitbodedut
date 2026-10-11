package com.myhitbodedut

import java.time.LocalDate

/**
 * Les paliers d'objectifs (יעדים, demande du 11/10) : après chaque palier
 * atteint, le suivant. Six pistes, chacune avec son échelle :
 *
 *  - SERIE : jours d'affilée (un jour exempté ne casse pas, comme la série) ;
 *  - JOUR, SEMAINE, MOIS : minutes de la journée, de la semaine (dimanche à
 *    samedi) et du mois hébreu en cours ; elles repartent à chaque période ;
 *  - CUMUL : minutes de toute la pratique ;
 *  - JOURS : jours de pratique, en tout.
 *
 * L'échelle part bas (le premier pas doit être à portée), se resserre là où
 * l'on hésite, s'espace ensuite ; quelques jalons parlent d'eux-mêmes : 18
 * (חי), 40 jours, 613. Au-delà du dernier palier, un pas régulier. Le palier
 * du jour inclut l'objectif quotidien réglé, pour que l'atteindre compte.
 *
 * Rien d'Android ici.
 */
object Paliers {

    enum class Piste(val cle: String) { SERIE("serie"), JOUR("jour"), SEMAINE("semaine"), MOIS("mois"), CUMUL("cumul"), JOURS("jours") }

    private val ECHELLES = mapOf(
        Piste.SERIE to (intArrayOf(3, 7, 14, 18, 21, 30, 40, 50, 70, 100, 150, 180, 250, 365) to 365),
        Piste.JOUR to (intArrayOf(10, 15, 20, 30, 45, 60, 90, 120) to 60),
        Piste.SEMAINE to (intArrayOf(30, 60, 90, 120, 180, 240, 300, 420, 600, 840) to 300),
        Piste.MOIS to (intArrayOf(60, 120, 180, 300, 450, 600, 900, 1200, 1800, 2400, 3000, 3600) to 1200),
        Piste.CUMUL to (intArrayOf(60, 180, 300, 600, 1080, 1500, 2160, 3000, 4320, 6000, 9000, 12000, 18000, 24000, 30000, 36000, 48000, 60000) to 12000),
        Piste.JOURS to (intArrayOf(1, 3, 7, 10, 18, 30, 40, 50, 70, 100, 150, 180, 250, 365, 500, 613, 800, 1000) to 250),
    )

    /** L'échelle d'une piste jusqu'à couvrir [jusqua] ; JOUR y ajoute l'objectif quotidien. */
    fun echelle(piste: Piste, jusqua: Int, objectifJour: Int = 0): List<Int> {
        val (base, pas) = ECHELLES.getValue(piste)
        val liste = base.toMutableList()
        if (piste == Piste.JOUR && objectifJour > 0 && objectifJour !in liste) liste += objectifJour
        liste.sort()
        while (liste.last() <= jusqua) liste += liste.last() + pas
        return liste
    }

    data class Etat(
        val piste: Piste,
        val valeur: Int,
        /** Le dernier palier atteint, 0 s'il n'y en a pas encore. */
        val atteint: Int,
        /** Le palier visé : le premier au-dessus de la valeur. */
        val prochain: Int,
        /** Combien de paliers sont atteints. */
        val rang: Int,
    ) {
        val reste: Int get() = prochain - valeur
        /** De 0 à 1, entre le palier atteint et le prochain. */
        val avancee: Float get() = ((valeur - atteint).toFloat() / (prochain - atteint)).coerceIn(0f, 1f)
    }

    fun etat(piste: Piste, valeur: Int, objectifJour: Int = 0): Etat {
        val v = valeur.coerceAtLeast(0)
        val e = echelle(piste, v, objectifJour)
        val faits = e.filter { it <= v }
        return Etat(piste, v, faits.lastOrNull() ?: 0, e.first { it > v }, faits.size)
    }

    /** Ce que la pratique donne : minutes par jour fait. */
    data class Bilan(
        val aujourdhui: LocalDate,
        val minutes: Map<LocalDate, Int>,
        val serie: Int,
        val debutMoisHebreu: LocalDate,
        val objectifJour: Int,
    ) {
        val debutSemaine: LocalDate get() = aujourdhui.minusDays((aujourdhui.dayOfWeek.value % 7).toLong())
        fun entre(debut: LocalDate, fin: LocalDate = aujourdhui) =
            minutes.filterKeys { !it.isBefore(debut) && !it.isAfter(fin) }.values.sum()
    }

    fun etats(b: Bilan): List<Etat> = listOf(
        etat(Piste.SERIE, b.serie),
        etat(Piste.JOUR, b.minutes[b.aujourdhui] ?: 0, b.objectifJour),
        etat(Piste.SEMAINE, b.entre(b.debutSemaine)),
        etat(Piste.MOIS, b.entre(b.debutMoisHebreu)),
        etat(Piste.CUMUL, b.minutes.values.sum()),
        etat(Piste.JOURS, b.minutes.count { it.value > 0 }),
    )

    /**
     * Le palier à mettre en avant à l'accueil : parmi la série, le jour, la
     * semaine et le mois, celui dont on est le plus près ; à égalité, la série.
     */
    fun aLaUne(etats: List<Etat>): Etat =
        etats.filter { it.piste in setOf(Piste.SERIE, Piste.JOUR, Piste.SEMAINE, Piste.MOIS) }
            .maxByOrNull { it.avancee }!!

    /**
     * Les marques des paliers atteints : une fois pour toutes pour la série,
     * le cumul et les jours ; par période pour le jour, la semaine et le mois
     * (le palier de 30 min se fête chaque jour où on l'atteint).
     */
    fun marques(etats: List<Etat>, b: Bilan): Set<String> = etats.flatMap { e ->
        val periode = when (e.piste) {
            Piste.JOUR -> ":${b.aujourdhui}"
            Piste.SEMAINE -> ":${b.debutSemaine}"
            Piste.MOIS -> ":${b.debutMoisHebreu}"
            else -> ""
        }
        echelle(e.piste, e.valeur, b.objectifJour).filter { it <= e.valeur }.map { "${e.piste.cle}$periode:$it" }
    }.toSet()

    /** Les paliers atteints depuis la dernière visite : le plus haut de chaque piste. */
    fun nouveaux(etats: List<Etat>, b: Bilan, vus: Set<String>): List<Etat> {
        val neuves = (marques(etats, b) - vus).map { it.substringBefore(':') }.toSet()
        return etats.filter { it.piste.cle in neuves }
    }

    /** Les records : meilleure semaine, meilleur mois (mois hébreu, par [moisDe]), meilleur jour. */
    data class Records(val jour: Int, val semaine: Int, val mois: Int)

    fun records(minutes: Map<LocalDate, Int>, moisDe: (LocalDate) -> String): Records {
        val semaines = minutes.entries.groupBy { it.key.minusDays((it.key.dayOfWeek.value % 7).toLong()) }
            .mapValues { e -> e.value.sumOf { it.value } }
        val mois = minutes.entries.groupBy { moisDe(it.key) }.mapValues { e -> e.value.sumOf { it.value } }
        return Records(minutes.values.maxOrNull() ?: 0, semaines.values.maxOrNull() ?: 0, mois.values.maxOrNull() ?: 0)
    }
}
