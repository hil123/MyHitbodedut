package com.myhitbodedut

import java.time.DayOfWeek
import java.time.LocalDate

/**
 * Les trophées (demande du 11/10, à niveaux depuis le 12/10) : neuf familles
 * qui regardent la forme de la pratique — la plus longue séance d'un jour,
 * les jours, la série, les heures, les semaines pleines, les vendredis, les
 * retours après une pause, le journal. Chacune monte par niveaux, et la
 * médaille change de métal en montant : bronze, argent, or, platine. Les
 * seuils reprennent des nombres qui parlent d'eux-mêmes (7, 18, 40, 70, 100,
 * 613) ; choix de l'application, sans source à citer.
 *
 * Rien d'Android ici.
 */
object Trophees {

    /** Le métal de la médaille : où l'on en est dans la famille, à l'œil. */
    enum class Metal { BRONZE, ARGENT, OR, PLATINE }

    /** L'objet (12/10, d'après les planches envoyées) : pas que des pièces. Clé de res/raw/trophees_formes.json. */
    enum class Forme(val cle: String) { COUPE("coupe"), ECUSSON("ecusson"), ETOILE("etoile"), ROSETTE("rosette"), MEDAILLE("medaille"), PIECES("pieces"), CRISTAL("cristal"), GEMME("gemme") }

    enum class Famille(val cle: String, val icone: String, val seuils: IntArray, val forme: Forme) {
        PREMIER_PAS("premier_pas", "🌱", intArrayOf(1), Forme.ROSETTE),
        SEANCE_LONGUE("seance_longue", "⏳", intArrayOf(15, 30, 45, 60, 90, 120, 180), Forme.COUPE),
        JOURS("jours", "🌳", intArrayOf(7, 18, 40, 70, 100, 180, 365, 613), Forme.MEDAILLE),
        SERIE("serie", "🔥", intArrayOf(3, 7, 18, 30, 40, 70, 100), Forme.ETOILE),
        HEURES("heures", "⭐", intArrayOf(1, 5, 10, 25, 50, 100, 200, 300), Forme.PIECES),
        SEMAINES("semaines", "🏅", intArrayOf(1, 2, 4, 8, 12, 26, 52), Forme.ECUSSON),
        EREV_CHABBAT("erev_chabbat", "🕯️", intArrayOf(1, 4, 12, 26, 52), Forme.CRISTAL),
        RETOUR("retour", "🔄", intArrayOf(1, 3, 5, 10), Forme.GEMME),
        PLUME("plume", "✍️", intArrayOf(1, 10, 25, 50, 100, 150), Forme.COUPE),
    }

    /** Un niveau atteint : [rang] de 1 au nombre de seuils de la famille. */
    data class Niveau(val famille: Famille, val rang: Int) {
        val cle: String get() = "${famille.cle}:$rang"
        val seuil: Int get() = famille.seuils[rang - 1]
        val metal: Metal get() = metal(famille, rang)
    }

    /** Le métal d'un rang : un quart de la famille par métal ; une famille d'un seul niveau est de bronze. */
    fun metal(f: Famille, rang: Int): Metal {
        if (f.seuils.size == 1) return Metal.BRONZE
        val i = ((rang - 1) * 4 / f.seuils.size).coerceIn(0, 3)
        return Metal.entries[i]
    }

    /** Ce que les trophées regardent : les minutes par jour fait, les notes écrites, la meilleure série. */
    data class Pratique(val minutes: Map<LocalDate, Int>, val notes: Int, val meilleureSerie: Int)

    /** Les semaines pleines : six jours faits, du dimanche au vendredi. */
    fun semainesPleines(jours: Set<LocalDate>): Int =
        jours.count { d -> d.dayOfWeek == DayOfWeek.SUNDAY && (0L..5L).all { d.plusDays(it) in jours } }

    /** Les retours : un jour de pratique après au moins sept jours sans. */
    fun retours(jours: Set<LocalDate>): Int =
        jours.sorted().zipWithNext().count { (a, b) -> b.toEpochDay() - a.toEpochDay() > 7 }

    /** La valeur que la famille compte : minutes, jours, heures, semaines, vendredis, retours, notes. */
    fun valeur(p: Pratique, f: Famille): Int {
        val jours = p.minutes.filterValues { it > 0 }.keys
        return when (f) {
            Famille.PREMIER_PAS -> jours.size.coerceAtMost(1)
            Famille.SEANCE_LONGUE -> p.minutes.values.maxOrNull() ?: 0
            Famille.JOURS -> jours.size
            Famille.SERIE -> p.meilleureSerie
            Famille.HEURES -> p.minutes.values.sum() / 60
            Famille.SEMAINES -> semainesPleines(jours)
            Famille.EREV_CHABBAT -> jours.count { it.dayOfWeek == DayOfWeek.FRIDAY }
            Famille.RETOUR -> retours(jours)
            Famille.PLUME -> p.notes
        }
    }

    /** Le rang atteint dans une famille : 0 tant que le premier seuil manque. */
    fun rang(p: Pratique, f: Famille): Int = valeur(p, f).let { v -> f.seuils.count { v >= it } }

    /** Tous les niveaux atteints, chacun pour soi (une famille au rang 3 a gagné 1, 2 et 3). */
    fun gagnes(p: Pratique): Set<Niveau> =
        Famille.entries.flatMap { f -> (1..rang(p, f)).map { Niveau(f, it) } }.toSet()

    /** Où l'on en est vers le niveau suivant ; au sommet, le dernier seuil. */
    data class Progres(val valeur: Int, val cible: Int) {
        val fraction: Float get() = if (cible <= 0) 1f else (valeur.coerceAtMost(cible).toFloat() / cible)
    }

    fun progres(p: Pratique, f: Famille): Progres {
        val r = rang(p, f)
        return Progres(valeur(p, f), f.seuils[r.coerceAtMost(f.seuils.size - 1)])
    }

    /**
     * Les niveaux gagnés depuis la dernière fois, le plus haut de chaque
     * famille d'abord ; rien la première fois ([vus] null), ni quand [vus]
     * date d'avant les niveaux (des clés sans « : ») : pas de pluie de
     * vieux trophées.
     */
    fun nouveaux(gagnes: Set<Niveau>, vus: Set<String>?): List<Niveau> {
        if (vus == null || (vus.isNotEmpty() && vus.none { ':' in it })) return emptyList()
        return gagnes.filter { it.cle !in vus }
            .groupBy { it.famille }.values.map { n -> n.maxBy { it.rang } }
            .sortedByDescending { it.metal.ordinal * 100 + it.rang }
    }
}
