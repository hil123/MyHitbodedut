package com.myhitbodedut

import android.content.Context
import com.kosherjava.zmanim.hebrewcalendar.JewishCalendar
import org.json.JSONObject
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * La halakha du jour, hors ligne (demande du 11/10) : le Ben Ish 'Haï,
 * Halakhot, en hébreu (res/raw/ben_ish_hai.json, construit par
 * tools/ben_ish_hai_construction.py depuis l'export Sefaria de Wikisource,
 * domaine public). Le texte de halachayomit.co.il n'est pas repris (tous
 * droits réservés) : l'écran garde le lien vers le site.
 *
 * Le livre suit la paracha de la semaine. Choix de l'application, pas une
 * règle du livre : la section de la paracha du Chabbat qui vient est
 * partagée en sept, du dimanche au Chabbat ; la première année du livre les
 * années hébraïques impaires (5787 …), la deuxième les paires, l'autre année
 * quand la section manque ; les huit jours de 'Hanouka, la section de
 * 'Hanouka partagée en huit.
 */
object Halakha {

    data class Section(val he: String, val halakhot: List<String>)

    data class Livre(val annees: Map<String, Map<String, Section>>, val hanouka: Section?)

    /** Une halakha du jour : le texte, son rang dans la section (1 …), la section, l'année du livre. */
    data class Choisie(val texte: String, val numero: Int, val section: String, val annee: Int)

    fun parse(json: String): Livre {
        val o = JSONObject(json)
        fun section(s: JSONObject) = Section(s.getString("he"), Json.strings(s.getJSONArray("h")))
        val annees = o.getJSONObject("annees")
        return Livre(
            annees = annees.keys().asSequence().associateWith { a ->
                val secs = annees.getJSONObject(a)
                secs.keys().asSequence().associateWith { section(secs.getJSONObject(it)) }
            },
            hanouka = o.optJSONObject("hanouka")?.let(::section),
        )
    }

    /** Les parachiot jointes de KosherJava et leurs deux parts. */
    val JOINTES = mapOf(
        "VAYAKHEL_PEKUDEI" to listOf("VAYAKHEL", "PEKUDEI"),
        "TAZRIA_METZORA" to listOf("TAZRIA", "METZORA"),
        "ACHREI_MOS_KEDOSHIM" to listOf("ACHREI_MOS", "KEDOSHIM"),
        "BEHAR_BECHUKOSAI" to listOf("BEHAR", "BECHUKOSAI"),
        "CHUKAS_BALAK" to listOf("CHUKAS", "BALAK"),
        "MATOS_MASEI" to listOf("MATOS", "MASEI"),
        "NITZAVIM_VAYEILECH" to listOf("NITZAVIM", "VAYEILECH"),
    )

    /** Ce que le livre dit pour [parsha] dans une année : (section, indice de départ, halakhot). */
    fun sections(annee: Map<String, Section>, parsha: String): List<Triple<Section, Int, List<String>>>? {
        annee[parsha]?.let { return listOf(Triple(it, 0, it.halakhot)) }
        JOINTES[parsha]?.let { (a, b) ->
            val sa = sections(annee, a)
            val sb = sections(annee, b)
            return if (sa == null && sb == null) null else sa.orEmpty() + sb.orEmpty()
        }
        // Une paracha lue seule, que le livre traite jointe à sa voisine : sa moitié.
        JOINTES.entries.firstOrNull { parsha in it.value }?.let { (jointe, parts) ->
            val s = annee[jointe] ?: return null
            val milieu = (s.halakhot.size + 1) / 2
            return listOf(if (parts[0] == parsha) Triple(s, 0, s.halakhot.take(milieu))
                          else Triple(s, milieu, s.halakhot.drop(milieu)))
        }
        return null
    }

    /** La part du jour [jour] sur [parts] jours : un septième (ou huitième) ; une section courte, une halakha. */
    fun <T> part(items: List<T>, jour: Int, parts: Int): List<T> {
        if (items.isEmpty()) return emptyList()
        if (items.size < parts) return listOf(items[jour % items.size])
        return items.subList(jour * items.size / parts, (jour + 1) * items.size / parts)
    }

    fun anneeDuLivre(anneeHebraique: Int): Int = if (anneeHebraique % 2 == 1) 1 else 2

    /**
     * Les halakhot du jour : [parsha] du Chabbat qui vient (nom de KosherJava),
     * [jourSemaine] 0 le dimanche … 6 le Chabbat, [jourHanouka] 1 … 8 ou 0.
     */
    fun choix(livre: Livre, parsha: String, jourSemaine: Int, anneeHebraique: Int, jourHanouka: Int = 0): List<Choisie> {
        if (jourHanouka in 1..8 && livre.hanouka != null) {
            val h = livre.hanouka
            return part(h.halakhot.indices.toList(), jourHanouka - 1, 8).map { Choisie(h.halakhot[it], it + 1, h.he, 1) }
        }
        val premiere = anneeDuLivre(anneeHebraique)
        for (a in listOf(premiere, 3 - premiere)) {
            val secs = sections(livre.annees[a.toString()].orEmpty(), parsha) ?: continue
            val toutes = secs.flatMap { (s, depart, l) -> l.mapIndexed { i, t -> Choisie(t, depart + i + 1, s.he, a) } }
            if (toutes.isNotEmpty()) return part(toutes, jourSemaine, 7)
        }
        return emptyList()
    }

    /** Pour une date : la paracha du Chabbat qui vient (le Chabbat même, la sienne), le jour de la semaine, 'Hanouka. */
    fun pour(livre: Livre, date: LocalDate, enIsrael: Boolean): List<Choisie> {
        val cal = JewishCalendar(date).apply { setInIsrael(enIsrael) }
        val parsha = cal.parshah.takeIf { it != JewishCalendar.Parsha.NONE } ?: cal.upcomingParshah
        val jour = if (date.dayOfWeek == DayOfWeek.SUNDAY) 0 else date.dayOfWeek.value
        return choix(livre, parsha.name, jour, cal.jewishYear, cal.dayOfChanukah.coerceAtLeast(0))
    }

    /** La recherche dans tout le livre, sans voyelles ni guillemets. */
    fun recherche(livre: Livre, texte: String): List<Choisie> {
        val nu = { t: String -> t.replace(Regex("[־׀]"), " ").replace(Regex("[֑-ׇ\"'״׳]"), "") }
        val q = nu(texte.trim())
        if (q.length < 2) return emptyList()
        val tout = livre.annees.entries.sortedBy { it.key }.flatMap { (a, secs) ->
            secs.values.flatMap { s -> s.halakhot.mapIndexed { i, t -> Choisie(t, i + 1, s.he, a.toInt()) } }
        } + livre.hanouka?.let { h -> h.halakhot.mapIndexed { i, t -> Choisie(t, i + 1, h.he, 1) } }.orEmpty()
        return tout.filter { nu(it.texte).contains(q) }
    }

    @Volatile private var cache: Livre? = null

    fun livre(context: Context): Livre =
        cache ?: parse(Json.readRaw(context, R.raw.ben_ish_hai)).also { cache = it }
}
