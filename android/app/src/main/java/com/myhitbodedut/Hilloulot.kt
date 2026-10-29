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
        /** « w » Wikipédia, « h » החכם היומי, « m » MyTzadik, « l » המאיר לארץ, « i » Hidabroot, « e » תורת אמת. */
        val sources: String,
        val doute: String?,
        /** La première phrase de l'article de Wikipédia hébreu (CC BY-SA), s'il existe. */
        val description: String? = null,
        /** Le titre de cet article. */
        val article: String? = null,
        /** Les biographies d'autres sources, reprises avec leur accord (12/10). */
        val bios: List<Bio> = emptyList(),
        /** L'article de Hidabroot sur ce tsaddik : un lien seulement. */
        val hidabroot: String? = null,
    )

    /** Une biographie, mot pour mot, et sa source : « m » MyTzadik, « l » המאיר לארץ. */
    data class Bio(val source: String, val texte: String, val lien: String?)

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
                    description = Json.nullable(o, "t"),
                    article = Json.nullable(o, "w"),
                    hidabroot = Json.nullable(o, "i"),
                )
            }
        }
    }

    /**
     * Les clés d'un jour hébreu ([mois] de KosherJava, NISSAN = 1 … ADAR_II = 13).
     * Un « adar » sans précision se dit en Adar II les années embolismiques
     * (Choul'han Aroukh OH 568:7) ; en Adar I pour les Ashkénazes, comme le Rema
     * l'y note (« וכן המנהג », audit du 16/10). Un 30 absent cette année
     * ([dernierJour] le 29), le 29.
     */
    fun cles(mois: Int, jour: Int, embolismique: Boolean, dernierJour: Boolean, ashkenaz: Boolean = false): List<String> {
        val m = when {
            mois == 12 && !embolismique -> listOf("adar", "adar1", "adar2")
            mois == 12 -> if (ashkenaz) listOf("adar", "adar1") else listOf("adar1")
            mois == 13 -> if (ashkenaz) listOf("adar2") else listOf("adar", "adar2")
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

    fun pour(table: Map<String, List<Hilloula>>, date: LocalDate, enIsrael: Boolean, ashkenaz: Boolean = false): List<Hilloula> {
        val j = CalendrierJuif.jour(date, enIsrael)
        val dernier = CalendrierJuif.jour(date.plusDays(1), enIsrael).jourHebreu == 1
        return duJour(table, cles(j.moisHebreu, j.jourHebreu, embolismique(j.anneeHebraique), dernier, ashkenaz))
    }

    private val MOIS = mapOf(
        "1" to "ניסן", "2" to "אייר", "3" to "סיון", "4" to "תמוז", "5" to "אב", "6" to "אלול",
        "7" to "תשרי", "8" to "חשון", "9" to "כסלו", "10" to "טבת", "11" to "שבט",
        "adar" to "אדר", "adar1" to "אדר א׳", "adar2" to "אדר ב׳",
    )

    /** « 7-30 » → « ל׳ תשרי ». */
    fun dateDe(cle: String): String {
        val (m, j) = cle.split("-")
        return "${Reference.lettres(j.toInt())} ${MOIS[m] ?: m}"
    }

    /** La recherche par nom, sans voyelles ni guillemets, dans toute l'année. */
    fun recherche(table: Map<String, List<Hilloula>>, texte: String): List<Pair<String, Hilloula>> {
        val nu = RechercheGlobale::nu
        val q = nu(texte.trim())
        if (q.length < 2) return emptyList()
        return table.entries.flatMap { (cle, l) -> l.map { cle to it } }
            .filter { (_, h) -> nu(h.he).contains(q) || (h.fr?.let { nu(it).contains(q) } ?: false) || (h.en?.let { nu(it).contains(q) } ?: false) }
    }

    /** L'article de Wikipédia hébreu. */
    fun lienArticle(titre: String): String = "https://he.wikipedia.org/wiki/" + java.net.URLEncoder.encode(titre.replace(' ', '_'), "UTF-8")

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

    /**
     * Les biographies d'autres sources (res/raw/hilloulot_bios.json), à part pour que la
     * carte du jour reste légère : par jour, une entrée par hilloula, dans le même ordre.
     */
    fun avecBios(table: Map<String, List<Hilloula>>, json: String): Map<String, List<Hilloula>> {
        val bios = JSONObject(json)
        return table.mapValues { (cle, liste) ->
            val b = bios.optJSONArray(cle) ?: return@mapValues liste
            liste.mapIndexed { i, h ->
                val x = b.optJSONArray(i) ?: return@mapIndexed h
                h.copy(bios = (0 until x.length()).map { j -> x.getJSONArray(j).let { y -> Bio(y.getString(0), y.getString(1), y.optString(2).ifBlank { null }) } })
            }
        }
    }

    @Volatile private var complete: Map<String, List<Hilloula>>? = null

    /** La table avec les biographies, pour l'écran des hilloulot seulement. */
    fun tableComplete(context: Context): Map<String, List<Hilloula>> =
        complete ?: avecBios(table(context), Json.readRaw(context, R.raw.hilloulot_bios)).also { complete = it }
}
