package com.myhitbodedut

import java.time.DayOfWeek
import java.time.LocalDate

/**
 * Les trophées (demande du 11/10) : à côté des paliers, qui montent sans
 * fin, des moments qui se gagnent une fois pour toutes et restent dans la
 * vitrine. Certains reprennent un jalon des paliers qui parle de lui-même
 * (18 jours, 40 jours d'affilée, 613 jours) ; les autres regardent la forme
 * de la pratique : une heure d'un coup, une semaine pleine, le retour après
 * une pause, le journal. Choix de l'application, sans source à citer.
 *
 * Rien d'Android ici.
 */
object Trophees {

    enum class Trophee(val cle: String, val icone: String) {
        PREMIER_PAS("premier_pas", "🌱"),
        UNE_HEURE("une_heure", "⏳"),
        DEUX_HEURES("deux_heures", "🔥"),
        SEMAINE_PLEINE("semaine_pleine", "🏅"),
        EREV_CHABBAT("erev_chabbat", "🕯️"),
        RETOUR("retour", "🔄"),
        CHAI("chai", "חי"),
        QUARANTE("quarante", "⛰️"),
        DIX_HEURES("dix_heures", "⭐"),
        CENT_HEURES("cent_heures", "🌟"),
        PLUME("plume", "✍️"),
        PLUME_FIDELE("plume_fidele", "📖"),
        TARYAG("taryag", "👑"),
    }

    /** Ce que les trophées regardent : les minutes par jour fait, les notes écrites, la meilleure série. */
    data class Pratique(val minutes: Map<LocalDate, Int>, val notes: Int, val meilleureSerie: Int)

    /** Les six jours, du dimanche au vendredi, d'une même semaine. */
    fun semainePleine(jours: Set<LocalDate>): Boolean = jours.any { d ->
        d.dayOfWeek == DayOfWeek.SUNDAY && (0L..5L).all { d.plusDays(it) in jours }
    }

    /** Un jour de pratique après au moins sept jours sans. */
    fun retour(jours: Set<LocalDate>): Boolean =
        jours.sorted().zipWithNext().any { (a, b) -> b.toEpochDay() - a.toEpochDay() > 7 }

    fun gagnes(p: Pratique): Set<Trophee> {
        val jours = p.minutes.filterValues { it > 0 }.keys
        val total = p.minutes.values.sum()
        val max = p.minutes.values.maxOrNull() ?: 0
        return buildSet {
            if (jours.isNotEmpty()) add(Trophee.PREMIER_PAS)
            if (max >= 60) add(Trophee.UNE_HEURE)
            if (max >= 120) add(Trophee.DEUX_HEURES)
            if (semainePleine(jours)) add(Trophee.SEMAINE_PLEINE)
            if (jours.count { it.dayOfWeek == DayOfWeek.FRIDAY } >= 4) add(Trophee.EREV_CHABBAT)
            if (retour(jours)) add(Trophee.RETOUR)
            if (jours.size >= 18) add(Trophee.CHAI)
            if (p.meilleureSerie >= 40) add(Trophee.QUARANTE)
            if (total >= 600) add(Trophee.DIX_HEURES)
            if (total >= 6000) add(Trophee.CENT_HEURES)
            if (p.notes >= 10) add(Trophee.PLUME)
            if (p.notes >= 50) add(Trophee.PLUME_FIDELE)
            if (jours.size >= 613) add(Trophee.TARYAG)
        }
    }

    /** Les trophées gagnés depuis la dernière fois ; rien la première fois ([vus] null). */
    fun nouveaux(gagnes: Set<Trophee>, vus: Set<String>?): List<Trophee> =
        if (vus == null) emptyList() else gagnes.filter { it.cle !in vus }
}
