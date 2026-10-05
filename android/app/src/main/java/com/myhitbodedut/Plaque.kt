package com.myhitbodedut

import java.time.LocalDate

/**
 * Les faces du centre des anneaux (demande du 04/10) : le hassid, puis vos
 * chiffres, une face par toucher. Chaque face qui parle d'un anneau le met
 * en avant ; toucher un anneau montre sa face.
 *
 * Rien d'Android ici : l'ordre des faces, les chiffres qu'elles montrent et
 * la meilleure série se vérifient sans appareil.
 */
object Plaque {

    enum class Face(val anneau: Anneaux.Quel?) {
        HASSID(null),
        JOUR(Anneaux.Quel.JOUR),
        SEMAINE(Anneaux.Quel.SEMAINE),
        SERIE(Anneaux.Quel.SERIE),
        MEILLEURE(Anneaux.Quel.SERIE),
        TOTAL(null),
        JOURS(null),
        MOYENNE(null),
    }

    /** Le temps sans toucher après lequel le hassid revient. */
    const val RETOUR_MS = 8_000L

    fun suivante(face: Face): Face = Face.entries[(face.ordinal + 1) % Face.entries.size]

    /** La face qui parle de cet anneau : la première dans l'ordre. */
    fun pour(quel: Anneaux.Quel): Face = Face.entries.first { it.anneau == quel }

    /** Les minutes moyennes d'un jour de pratique, arrondies ; 0 sans pratique. */
    fun moyenne(totalMinutes: Int, jours: Int): Int =
        if (jours <= 0) 0 else Math.round(totalMinutes.toFloat() / jours)

    /**
     * La plus longue suite de jours faits. Comme la série du jour, un jour
     * exempté (Shabbat, fête, pause) ne compte pas et ne casse pas.
     */
    fun meilleureSerie(faits: Set<LocalDate>, exempt: (LocalDate) -> Boolean): Int {
        if (faits.isEmpty()) return 0
        var jour = faits.min()
        val fin = faits.max()
        var courante = 0
        var meilleure = 0
        while (!jour.isAfter(fin)) {
            when {
                jour in faits -> {
                    courante++
                    meilleure = maxOf(meilleure, courante)
                }
                exempt(jour) -> Unit
                else -> courante = 0
            }
            jour = jour.plusDays(1)
        }
        return meilleure
    }
}
