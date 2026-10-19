package com.myhitbodedut

import android.content.Context
import com.kosherjava.zmanim.hebrewcalendar.JewishCalendar
import com.kosherjava.zmanim.hebrewcalendar.JewishDate
import org.json.JSONObject
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * La halakha du jour (13/10) : une sélection du ילקוט יוסף – קיצור שולחן ערוך
 * du Rav Yitzhak Yosef (res/raw/yalkut_yosef.json, construit par
 * tools/yalkut_yosef_construction.py ; droits et source dans l'outil). Jamais le
 * livre entier : une dizaine de pour cent, choisie par période.
 *
 * Choix de l'application, pas du livre : deux se'ifim par jour ; le jour de Roch
 * 'Hodech ses lois ; avant et pendant chaque fête les siennes (Pessa'h dès le
 * lendemain de Pourim, Roch Hachana dès Eloul …) ; les vendredis ordinaires le
 * Chabbat ; les autres jours la vie de chaque jour. Dans une période, on avance
 * de deux se'ifim par jour depuis son début ; le Chabbat et les jours ordinaires
 * avancent avec le calendrier, de semaine en semaine ou de jour en jour.
 */
object YalkutYosef {

    data class Seif(val siman: String, val lettre: String, val texte: String)

    data class Livre(val periodes: Map<String, List<Seif>>)

    const val PAR_JOUR = 2

    fun parse(json: String): Livre {
        val p = JSONObject(json).getJSONObject("periodes")
        return Livre(p.keys().asSequence().associateWith { k ->
            val a = p.getJSONArray(k)
            (0 until a.length()).map { i ->
                val o = a.getJSONObject(i)
                Seif(o.getString("s"), o.getString("l"), o.getString("t"))
            }
        })
    }

    /** La fenêtre de fête d'un jour hébreu, ou null. [mois] et [jour] comme KosherJava. */
    fun fenetre(mois: Int, jour: Int, embolismique: Boolean): String? {
        val adar = if (embolismique) JewishDate.ADAR_II else JewishDate.ADAR
        return when {
            mois == adar && jour <= 15 -> "pourim"
            mois == adar || (mois == JewishDate.NISSAN && jour <= 22) -> "pessah"
            mois == JewishDate.NISSAN || mois == JewishDate.IYAR || (mois == JewishDate.SIVAN && jour <= 7) -> "omer"
            (mois == JewishDate.TAMMUZ && jour >= 17) || (mois == JewishDate.AV && jour <= 9) -> "bein_hametsarim"
            mois == JewishDate.ELUL || (mois == JewishDate.TISHREI && jour <= 2) -> "eloul"
            mois == JewishDate.TISHREI && jour <= 10 -> "kippour"
            mois == JewishDate.TISHREI && jour <= 23 -> "soukkot"
            (mois == JewishDate.KISLEV && jour >= 15) || (mois == JewishDate.TEVES && jour <= 3) -> "hanouka"
            mois == JewishDate.SHEVAT && jour <= 15 -> "tou_bichvat"
            else -> null
        }
    }

    private fun cal(date: LocalDate, israel: Boolean) = JewishCalendar(date).apply { setInIsrael(israel) }

    private fun fenetreDu(date: LocalDate, israel: Boolean): String? {
        val c = cal(date, israel)
        return fenetre(c.jewishMonth, c.jewishDayOfMonth, c.isJewishLeapYear)
    }

    /** (période, rang du jour dans la période) pour une date. */
    fun periode(date: LocalDate, israel: Boolean): Pair<String, Int> {
        val c = cal(date, israel)
        // Le 30 ouvre le Roch 'Hodech du mois suivant, le 1er le poursuit : deux rangs qui se suivent.
        if (c.isRoshChodesh) return "roch_hodech" to (if (c.jewishDayOfMonth == 30) (c.jewishMonth + 1) * 2 else c.jewishMonth * 2 + 1)
        fenetreDu(date, israel)?.let { f ->
            var n = 0
            while (n < 60 && fenetreDu(date.minusDays(n + 1L), israel) == f) n++
            return f to n
        }
        return if (date.dayOfWeek == DayOfWeek.FRIDAY) "chabbat" to (date.toEpochDay() / 7).toInt()
        else "ordinaire" to date.toEpochDay().toInt()
    }

    /** Les se'ifim d'un rang : [PAR_JOUR] à la suite, en boucle. */
    fun du(seifs: List<Seif>, rang: Int): List<Seif> {
        if (seifs.isEmpty()) return emptyList()
        val debut = Math.floorMod(rang.toLong() * PAR_JOUR, seifs.size.toLong()).toInt()
        return (0 until minOf(PAR_JOUR, seifs.size)).map { seifs[(debut + it) % seifs.size] }
    }

    fun pour(livre: Livre, date: LocalDate, israel: Boolean): List<Seif> {
        val (p, rang) = periode(date, israel)
        return du(livre.periodes[p].orEmpty(), rang)
    }

    fun recherche(livre: Livre, texte: String): List<Seif> {
        val nu = { t: String -> t.replace(Regex("[־׀]"), " ").replace(Regex("[֑-ׇ\"'״׳]"), "") }
        val q = nu(texte.trim())
        if (q.length < 2) return emptyList()
        return livre.periodes.values.flatten().distinct().filter { nu(it.texte).contains(q) || nu(it.siman).contains(q) }
    }

    /** « ילקוט יוסף · סימן א - הלכות השכמת הבוקר · סעיף ב » */
    fun source(s: Seif): String =
        listOf("ילקוט יוסף", s.siman, if (s.lettre.isNotEmpty()) "סעיף ${s.lettre}" else null).filterNotNull().joinToString(" · ")

    /** Les halakhot du jour en une seule carte (13/10) : le siman une fois s'il est commun. */
    fun titreCarte(seifs: List<Seif>): String =
        if (seifs.map { it.siman }.distinct().size == 1) "ילקוט יוסף · ${seifs.first().siman}" else "ילקוט יוסף"

    /** Chaque se'if sous sa lettre (et son siman, s'ils diffèrent). */
    fun texteCarte(seifs: List<Seif>): String {
        val commun = seifs.map { it.siman }.distinct().size == 1
        return seifs.joinToString("\n\n") { s ->
            val l = if (s.lettre.isNotEmpty()) "${s.lettre}. " else ""
            if (commun) "$l${s.texte}" else "${s.siman}\n$l${s.texte}"
        }
    }

    @Volatile private var cache: Livre? = null

    fun livre(context: Context): Livre =
        cache ?: parse(Json.readRaw(context, R.raw.yalkut_yosef)).also { cache = it }
}
