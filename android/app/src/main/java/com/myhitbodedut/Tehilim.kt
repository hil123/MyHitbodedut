package com.myhitbodedut

import android.content.Context
import com.kosherjava.zmanim.hebrewcalendar.JewishCalendar
import org.json.JSONObject
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * Le livre de Tehilim entier (demande du 04/10) : 150 psaumes, 2 527
 * versets, dans l'édition et selon les conventions du Tikoun (tehilim.json,
 * dont le bloc _meta dit la source et la vérification).
 */
class Tehilim(val psaumes: List<List<String>>) {

    /** Les versets du psaume [numero] (1 à 150), dans l'ordre. */
    fun versets(numero: Int): List<String> = psaumes.getOrElse(numero - 1) { emptyList() }

    val versetsTotal: Int get() = psaumes.sumOf { it.size }

    companion object {
        const val PSAUMES = 150
    }
}

object TehilimLoader {

    fun parse(json: String): Tehilim {
        val tableau = JSONObject(json).getJSONArray("psaumes")
        return Tehilim((0 until tableau.length()).map { i ->
            val versets = tableau.getJSONArray(i)
            (0 until versets.length()).map { versets.getString(it) }
        })
    }

    fun load(context: Context): Tehilim = parse(Json.readRaw(context, R.raw.tehilim))
}

/**
 * Un passage du livre : du psaume [debut] au psaume [fin]. Seul le psaume
 * 119, trop long pour un jour, se coupe en deux, d'où les bornes de versets.
 */
data class Portion(val debut: Int, val fin: Int, val debutVerset: Int = 1, val finVerset: Int? = null) {

    val psaumes: IntRange get() = debut..fin

    /** Les versets à lire dans [psaume], qui en compte [total]. */
    fun versets(psaume: Int, total: Int): IntRange =
        (if (psaume == debut) debutVerset else 1)..(if (psaume == fin) finVerset ?: total else total)

    /** Vrai si la portion ne prend qu'une partie d'un psaume (119:1-96). */
    val partielle: Boolean get() = debut == fin && (debutVerset > 1 || finVerset != null)
}

/**
 * Les deux façons traditionnelles de dire tout le livre : en un mois, une
 * portion par jour du mois hébraïque, ou en une semaine, du dimanche au
 * Shabbat. Découpages relevés le 04/10/2026 et recoupés sur plusieurs
 * sources indépendantes (hebcal.com, tehilim.co, tehillim-online.com,
 * tehillimforall.com).
 */
object CycleTehilim {

    val MOIS: List<Portion> = listOf(
        Portion(1, 9), Portion(10, 17), Portion(18, 22), Portion(23, 28), Portion(29, 34),
        Portion(35, 38), Portion(39, 43), Portion(44, 48), Portion(49, 54), Portion(55, 59),
        Portion(60, 65), Portion(66, 68), Portion(69, 71), Portion(72, 76), Portion(77, 78),
        Portion(79, 82), Portion(83, 87), Portion(88, 89), Portion(90, 96), Portion(97, 103),
        Portion(104, 105), Portion(106, 107), Portion(108, 112), Portion(113, 118),
        Portion(119, 119, 1, 96), Portion(119, 119, 97, 176),
        Portion(120, 134), Portion(135, 139), Portion(140, 144), Portion(145, 150),
    )

    /** Du dimanche au Shabbat. */
    val SEMAINE: List<Portion> = listOf(
        Portion(1, 29), Portion(30, 50), Portion(51, 72), Portion(73, 89),
        Portion(90, 106), Portion(107, 119), Portion(120, 150),
    )

    /**
     * La portion du [jour] (1 à 30) d'un mois hébraïque de [longueur] jours.
     * Un mois de 29 jours dit le 29 les deux dernières portions, pour finir
     * le livre avec le mois.
     */
    fun duMois(jour: Int, longueur: Int): Portion =
        if (jour >= 29 && longueur == 29) Portion(140, 150) else MOIS[jour.coerceIn(1, 30) - 1]

    /** DayOfWeek va de lundi (1) à dimanche (7) : le reste par 7 ramène au dimanche. */
    fun deLaSemaine(jour: DayOfWeek): Portion = SEMAINE[jour.value % 7]

    /**
     * Le jour du mois hébraïque et la longueur de ce mois, pour la date
     * civile : le jour change ici à minuit, pas au coucher du soleil.
     */
    fun jourHebreu(date: LocalDate): Pair<Int, Int> {
        val calendrier = JewishCalendar(date)
        return calendrier.jewishDayOfMonth to calendrier.daysInJewishMonth
    }
}
