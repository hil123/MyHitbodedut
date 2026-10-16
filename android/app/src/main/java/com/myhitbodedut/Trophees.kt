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
 * 613), avec des paliers intermédiaires à chaque dizaine et quinzaine (12/10) ;
 * choix de l'application, sans source à citer.
 *
 * Rien d'Android ici.
 */
object Trophees {

    /** Le métal de la médaille : où l'on en est dans la famille, à l'œil. */
    enum class Metal { BRONZE, ARGENT, OR, PLATINE }

    /** L'objet (12/10, d'après les planches envoyées) : pas que des pièces. Clé de res/raw/trophees_formes.json. */
    enum class Forme(val cle: String) { COUPE("coupe"), ECUSSON("ecusson"), ETOILE("etoile"), ROSETTE("rosette"), MEDAILLE("medaille"), PIECES("pieces"), CRISTAL("cristal"), GEMME("gemme") }

    /** [icone] : clé de l'icône au trait dans res/raw/trophees_formes.json (variante B choisie le 16/10). */
    enum class Famille(val cle: String, val icone: String, val seuils: IntArray, val forme: Forme) {
        PREMIER_PAS("premier_pas", "premier_pas", intArrayOf(1, 2, 3, 4, 5), Forme.ROSETTE),
        SEANCE_LONGUE("seance_longue", "seance_longue", intArrayOf(10, 15, 20, 30, 40, 45, 50, 60, 75, 90, 105, 120, 150, 180), Forme.COUPE),
        JOURS("jours", "jours", intArrayOf(7, 10, 15, 18, 20, 30, 40, 45, 50, 60, 70, 80, 90, 100, 120, 150, 180, 200, 250, 300, 365, 400, 500, 613), Forme.MEDAILLE),
        SERIE("serie", "serie", intArrayOf(3, 5, 7, 10, 15, 18, 20, 30, 40, 45, 50, 60, 70, 80, 90, 100), Forme.ETOILE),
        HEURES("heures", "heures", intArrayOf(1, 2, 5, 10, 15, 20, 25, 30, 40, 45, 50, 60, 75, 100, 150, 200, 250, 300), Forme.PIECES),
        SEMAINES("semaines", "semaines", intArrayOf(1, 2, 3, 4, 5, 6, 8, 10, 12, 15, 20, 26, 30, 40, 45, 52), Forme.ECUSSON),
        EREV_CHABBAT("erev_chabbat", "erev_chabbat", intArrayOf(1, 4, 12, 26, 52), Forme.CRISTAL),
        RETOUR("retour", "retour", intArrayOf(1, 3, 5, 10), Forme.GEMME),
        PLUME("plume", "plume", intArrayOf(1, 10, 25, 50, 100, 150), Forme.COUPE),
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
        // Un jour validé compte, même à 0 min (« Terminer » avant la première minute, déjà fait) : comme la série.
        val jours = p.minutes.keys
        return when (f) {
            Famille.PREMIER_PAS -> jours.size.coerceAtMost(5)
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

    /**
     * Le chemin (15/10, « fais un système plus simple ») : tous les niveaux de toutes les
     * familles en une seule liste, du plus facile au plus dur. Chaque étape est un petit
     * objectif (« 3 jours d'affilée », « 1 heure en tout ») ; l'écran montre les gagnées
     * et celles à venir, dans cet ordre. Le coût estime le nombre de jours qu'il faut,
     * à raison d'un quart d'heure par jour ; choix de l'application.
     */
    fun cout(f: Famille, seuil: Int): Double = when (f) {
        Famille.PREMIER_PAS, Famille.JOURS -> seuil.toDouble()
        Famille.SEANCE_LONGUE -> seuil / 2.0
        Famille.SERIE -> seuil * 1.5
        Famille.HEURES -> seuil * 4.0
        Famille.SEMAINES -> seuil * 8.0
        Famille.EREV_CHABBAT -> seuil * 7.0
        Famille.RETOUR -> seuil * 30.0 + 10
        Famille.PLUME -> seuil * 1.3
    }

    val chemin: List<Niveau> by lazy {
        Famille.entries.flatMap { f -> f.seuils.indices.map { Niveau(f, it + 1) } }
            .sortedWith(compareBy<Niveau>({ cout(it.famille, it.seuil) }, { it.famille.ordinal }))
    }

    fun gagne(p: Pratique, n: Niveau): Boolean = valeur(p, n.famille) >= n.seuil

    /**
     * Le grade (15/10, « les matériaux sont mélangés ») : un seul matériau à la fois, selon
     * le [compte] de trophées gagnés — bronze, argent dès le 10ᵉ, or dès le 25ᵉ, platine
     * dès le 50ᵉ. Le k-ième trophée gagné (de 1) prend le matériau de grade(k).
     */
    val SEUILS_GRADES = intArrayOf(0, 10, 25, 50)

    fun grade(compte: Int): Metal = Metal.entries[SEUILS_GRADES.count { compte >= it } - 1]

    /** De 0 à 1 vers une étape. */
    fun fraction(p: Pratique, n: Niveau): Float = (valeur(p, n.famille).coerceAtMost(n.seuil).toFloat() / n.seuil)

    /** Le prochain trophée : parmi ceux à gagner, le plus près d'être atteint ; à égalité, le plus facile. */
    fun prochain(p: Pratique): Niveau? = chemin.filterNot { gagne(p, it) }
        .maxWithOrNull(compareBy<Niveau> { fraction(p, it) }.thenByDescending { cout(it.famille, it.seuil) })

    /**
     * Les plus proches (16/10) : les [k] trophées à gagner les plus avancés ; à égalité, les
     * plus faciles. L'en-tête du grade les montre, pour dire comment y monter.
     */
    fun plusProches(p: Pratique, k: Int = 3): List<Niveau> = chemin.filterNot { gagne(p, it) }
        .sortedWith(compareByDescending<Niveau> { fraction(p, it) }.thenBy { cout(it.famille, it.seuil) })
        .take(k)

    /**
     * Le prochain trophée, s'il est tout proche (16/10) : à un dixième du seuil, ou à une
     * unité pour les petits seuils (« 7 jours d'affilée » à 6). Le rappel du jour le dit.
     */
    fun proche(p: Pratique): Niveau? = prochain(p)?.takeIf {
        val reste = it.seuil - valeur(p, it.famille)
        reste in 1..maxOf(1, it.seuil / 10)
    }

    /**
     * Le jour où chaque niveau a été gagné (16/10) : l'historique rejoué jour après jour,
     * le premier jour où la pratique atteint le seuil. [notes] : les jours qui portent une
     * note ; [exempt] : les jours qui ne cassent pas la série (Plaque.meilleureSerie).
     */
    fun datesGain(minutes: Map<LocalDate, Int>, notes: Set<LocalDate>, exempt: (LocalDate) -> Boolean): Map<String, LocalDate> {
        // Le rejeu est incrémental (16/10) : chaque valeur avance jour après jour, sans rien
        // recalculer — rejouer la pratique entière à chaque jour gelait l'accueil. Mêmes règles
        // que valeur() et Plaque.meilleureSerie (un jour exempté ne compte pas et ne casse pas).
        val res = mutableMapOf<String, LocalDate>()
        val restants = chemin.groupBy { it.famille }.mapValues { it.value.sortedBy { n -> n.seuil }.toMutableList() }.toMutableMap()
        val faits = sortedSetOf<LocalDate>()
        var jours = 0; var plusLongue = 0; var total = 0; var vendredis = 0; var retours = 0
        var semaines = 0; var notesVues = 0; var courante = 0; var meilleure = 0
        var precedent: LocalDate? = null
        val semainesPleines = mutableSetOf<LocalDate>()
        for (d in (minutes.keys + notes).toSortedSet()) {
            if (d in notes) notesVues++
            val m = minutes[d] ?: 0
            if (d in minutes) {
                faits += d
                jours++; total += m; plusLongue = maxOf(plusLongue, m)
                if (d.dayOfWeek == DayOfWeek.FRIDAY) vendredis++
                val avant = precedent
                if (avant != null && d.toEpochDay() - avant.toEpochDay() > 7) retours++
                // La série : les jours entre le précédent et celui-ci doivent tous être exemptés.
                courante = if (avant != null && generateSequence(avant.plusDays(1)) { it.plusDays(1) }
                        .takeWhile { it.isBefore(d) }.all(exempt)) courante + 1 else 1
                meilleure = maxOf(meilleure, courante)
                precedent = d
                val dimanche = d.minusDays((d.dayOfWeek.value % 7).toLong())
                if (dimanche !in semainesPleines && (0L..5L).all { dimanche.plusDays(it) in faits }) { semainesPleines += dimanche; semaines++ }
            }
            val vides = mutableListOf<Famille>()
            for ((f, niveaux) in restants) {
                val v = when (f) {
                    Famille.PREMIER_PAS -> jours.coerceAtMost(5)
                    Famille.SEANCE_LONGUE -> plusLongue
                    Famille.JOURS -> jours
                    Famille.SERIE -> meilleure
                    Famille.HEURES -> total / 60
                    Famille.SEMAINES -> semaines
                    Famille.EREV_CHABBAT -> vendredis
                    Famille.RETOUR -> retours
                    Famille.PLUME -> notesVues
                }
                while (niveaux.isNotEmpty() && niveaux.first().seuil <= v) res[niveaux.removeAt(0).cle] = d
                if (niveaux.isEmpty()) vides += f
            }
            vides.forEach { restants.remove(it) }
            if (restants.isEmpty()) break
        }
        return res
    }

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
