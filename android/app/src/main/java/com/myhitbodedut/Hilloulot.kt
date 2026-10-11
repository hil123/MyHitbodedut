package com.myhitbodedut

import android.content.Context
import org.json.JSONObject
import java.time.LocalDate

/**
 * La hilloula du jour (demande du 11/10) : res/raw/hilloulot.json, construit
 * par tools/hilloulot_construction.py depuis Wikipédia hébreu et החכם היומי.
 * Avec les deux liens de la carte du jour : la page du jour de החכם היומי,
 * et la halakha du jour de halachayomit.co.il, dont le texte n'est pas repris.
 */
object Hilloulot {

    data class Hilloula(
        val he: String,
        val fr: String?,
        val en: String?,
        val annee: String,
        /** « w » Wikipédia, « h » החכם היומי, « hw » les deux. */
        val sources: String,
        val doute: String?,
    )

    fun parse(json: String): Map<String, List<Hilloula>> {
        val jours = JSONObject(json).getJSONObject("jours")
        return jours.keys().asSequence().associateWith { cle ->
            Json.array(jours.getJSONArray(cle)) { o ->
                Hilloula(
                    he = o.getString("he"),
                    fr = Json.nullable(o, "fr"),
                    en = Json.nullable(o, "en"),
                    annee = o.optString("a", ""),
                    sources = o.optString("s", ""),
                    doute = Json.nullable(o, "d"),
                )
            }
        }
    }

    /**
     * Les clés d'un jour hébreu ([mois] de KosherJava, NISSAN = 1 … ADAR_II = 13).
     * Un « adar » sans précision se dit en Adar II les années embolismiques
     * (Choul'han Aroukh OH 568:7) ; un 30 absent cette année ([dernierJour]
     * le 29), le 29.
     */
    fun cles(mois: Int, jour: Int, embolismique: Boolean, dernierJour: Boolean): List<String> {
        val m = when {
            mois == 12 && !embolismique -> listOf("adar", "adar1", "adar2")
            mois == 12 -> listOf("adar1")
            mois == 13 -> listOf("adar", "adar2")
            else -> listOf(mois.toString())
        }
        val jours = if (jour == 29 && dernierJour) listOf(29, 30) else listOf(jour)
        return m.flatMap { k -> jours.map { "$k-$it" } }
    }

    /** D'abord ceux que les deux sources donnent, puis ceux sans doute relevé. */
    fun duJour(table: Map<String, List<Hilloula>>, cles: List<String>): List<Hilloula> =
        cles.flatMap { table[it].orEmpty() }
            .sortedBy { (if (it.sources.length == 2) 0 else 2) + (if (it.doute == null) 0 else 1) }

    /** Cycle de Méton : 7 années embolismiques sur 19 (3, 6, 8, 11, 14, 17, 19). */
    fun embolismique(annee: Int): Boolean = (7 * annee + 1) % 19 < 7

    fun pour(table: Map<String, List<Hilloula>>, date: LocalDate, enIsrael: Boolean): List<Hilloula> {
        val j = CalendrierJuif.jour(date, enIsrael)
        val dernier = CalendrierJuif.jour(date.plusDays(1), enIsrael).jourHebreu == 1
        return duJour(table, cles(j.moisHebreu, j.jourHebreu, embolismique(j.anneeHebraique), dernier))
    }

    /** La page du jour de החכם היומי, qui prend la date civile. */
    fun lienHyomi(date: LocalDate): String =
        "https://hyomi.org.il/day.php?month1=0&dd=${date.dayOfMonth}&mm=${date.monthValue}&yy=${date.year}"

    /** La halakha du jour, dans la langue de l'application (le site a he, fr, en). */
    fun lienHalakha(langue: String): String {
        val l = langue.lowercase()
        val code = if (l.startsWith("he") || l.startsWith("iw")) "he" else if (l.startsWith("en")) "en" else "fr"
        return "https://halachayomit.co.il/$code/default.aspx"
    }

    @Volatile private var cache: Map<String, List<Hilloula>>? = null

    fun table(context: Context): Map<String, List<Hilloula>> =
        cache ?: parse(Json.readRaw(context, R.raw.hilloulot)).also { cache = it }
}
