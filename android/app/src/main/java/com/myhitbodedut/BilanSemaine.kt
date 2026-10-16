package com.myhitbodedut

import java.time.LocalDate

/**
 * Le bilan de la semaine (16/10) : à la première ouverture de la semaine, ce que la
 * semaine écoulée (dimanche → samedi) a donné — minutes, jours, la semaine d'avant,
 * un record, les trophées gagnés. Une semaine vide ne fait pas de bilan : l'application
 * ne rappelle pas une dette.
 *
 * Rien d'Android ici.
 */
object BilanSemaine {

    data class Bilan(
        /** Le dimanche de la semaine écoulée. */
        val debut: LocalDate,
        val minutes: Int,
        val jours: Int,
        /** Les minutes de la semaine d'avant. */
        val precedente: Int,
        /** La meilleure semaine jusque-là : seulement s'il y en avait une avant. */
        val record: Boolean,
        val trophees: List<Trophees.Niveau>,
    ) {
        val ecart: Int get() = minutes - precedente
    }

    fun debutSemaine(d: LocalDate): LocalDate = d.minusDays((d.dayOfWeek.value % 7).toLong())

    /** Le bilan de la semaine écoulée vue depuis [aujourdhui] ; null si elle n'a rien compté. */
    fun precedente(aujourdhui: LocalDate, minutes: Map<LocalDate, Int>, dates: Map<String, LocalDate>): Bilan? {
        val debut = debutSemaine(aujourdhui).minusDays(7)
        val fin = debut.plusDays(6)
        fun entre(a: LocalDate, b: LocalDate) = minutes.filterKeys { !it.isBefore(a) && !it.isAfter(b) }
        val semaine = entre(debut, fin)
        val total = semaine.values.sum()
        if (total <= 0) return null
        val avant = minutes.filterKeys { it.isBefore(debut) }.entries
            .groupBy { debutSemaine(it.key) }.mapValues { e -> e.value.sumOf { it.value } }
        val meilleureAvant = avant.values.maxOrNull() ?: 0
        return Bilan(
            debut = debut,
            minutes = total,
            jours = semaine.count { it.value > 0 },
            precedente = entre(debut.minusDays(7), debut.minusDays(1)).values.sum(),
            record = meilleureAvant > 0 && total > meilleureAvant,
            trophees = Trophees.chemin.filter { n -> dates[n.cle]?.let { !it.isBefore(debut) && !it.isAfter(fin) } == true },
        )
    }
}
