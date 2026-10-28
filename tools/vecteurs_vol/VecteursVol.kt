// Produit web/test/vecteurs_vol.json (28/10/2026) : ce que Vol.kt (sur
// Zmanim.kt et KosherJava 2.5.0) répond pour quelques vols, pour que vol.js
// (sur kosher-zmanim) lui soit comparé sans recopier une valeur à la main.
//
// À compiler avec Vol.kt, VolCalendrier.kt, CalendrierJuif.kt et Zmanim.kt, le jar com.kosherjava:zmanim:2.5.0 et
// kotlin-stdlib 2.0.21, sur la JVM seule (README.md à côté de ce fichier).
// Lancé depuis la racine du dépôt : il lit android/app/src/main/res/raw/aeroports.json.
import com.myhitbodedut.CalendrierJuif
import com.myhitbodedut.Vol
import com.myhitbodedut.VolCalendrier
import java.io.File
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime

/** Les vols des vecteurs : [départ, arrivée, décollage local, atterrissage local]. */
val VOLS = listOf(
    listOf("TLV", "JFK", "2026-11-10T12:30", "2026-11-10T17:45"),
    listOf("JFK", "TLV", "2026-11-10T23:00", "2026-11-11T16:40"),
    listOf("TLV", "CDG", "2026-11-10T22:30", "2026-11-11T02:00"),
    listOf("SFO", "HKG", "2026-11-10T11:00", "2026-11-11T19:00"),
    listOf("HKG", "SFO", "2026-11-10T21:00", "2026-11-10T18:00"),
    listOf("JFK", "HKG", "2026-06-20T11:00", "2026-06-21T15:00"),
    listOf("TLV", "ETM", "2026-11-10T09:50", "2026-11-10T10:40"),
    listOf("CDG", "TLV", "2026-03-29T01:30", "2026-03-29T07:45"),
    listOf("TLV", "MEL", "2026-12-20T23:55", "2026-12-21T23:30"),
    // Melbourne → Los Angeles → Montréal et retour, en été et en hiver austral ; Sydney → Queenstown ; Tel Aviv → Buenos Aires.
    listOf("MEL", "LAX", "2026-12-15T10:30", "2026-12-15T06:45"),
    listOf("LAX", "YUL", "2026-12-15T09:30", "2026-12-15T17:55"),
    listOf("YUL", "LAX", "2026-12-22T07:00", "2026-12-22T10:05"),
    listOf("LAX", "MEL", "2026-12-22T22:30", "2026-12-24T08:45"),
    listOf("MEL", "LAX", "2026-07-15T10:30", "2026-07-15T06:45"),
    listOf("LAX", "YUL", "2026-07-15T09:30", "2026-07-15T17:55"),
    listOf("YUL", "LAX", "2026-07-22T07:00", "2026-07-22T10:05"),
    listOf("LAX", "MEL", "2026-07-22T22:30", "2026-07-24T08:45"),
    listOf("SYD", "ZQN", "2026-12-18T09:00", "2026-12-18T14:15"),
    listOf("SYD", "ZQN", "2026-07-17T09:00", "2026-07-17T14:10"),
    listOf("TLV", "EZE", "2026-11-10T23:00", "2026-11-11T09:20"),
    listOf("TLV", "EZE", "2026-06-10T23:00", "2026-06-11T08:20"),
    // Un relevé GPS en vol (latitude, longitude, instant UTC) : 4° au nord de la route, au-dessus de l'Atlantique.
    listOf("TLV", "JFK", "2026-11-10T12:30", "2026-11-10T17:45", "56.0", "-30.0", "2026-11-10T18:30:00Z"),
) + calendrier()

/**
 * Des vols vers l'ouest, de Tel Aviv à New York, un jour que le
 * calendrier choisit lui-même : la chki'a ou tsét passe en vol. Aucune date tapée.
 */
fun calendrier(): List<List<String>> {
    fun premier(depuis: String, ok: (LocalDate) -> Boolean): LocalDate =
        generateSequence(LocalDate.parse(depuis)) { it.plusDays(1) }.first(ok)
    fun j(d: LocalDate) = CalendrierJuif.jour(d, false)
    val vendredi = premier("2026-11-01") { it.dayOfWeek == DayOfWeek.FRIDAY }
    val samedi = premier("2026-11-01") { it.dayOfWeek == DayOfWeek.SATURDAY && !j(it.plusDays(1)).chome }
    val enchaine = premier("2026-11-01") { j(it).chome && j(it.plusDays(1)).chome }
    val omer = premier("2027-01-01") { j(it.plusDays(1)).omer > 0 && !j(it).chome }
    // Au printemps, tsét passe en vol sur un départ de fin d'après-midi.
    return listOf(vendredi, samedi).map { listOf("TLV", "JFK", "${it}T12:30", "${it}T17:45") } +
        listOf(enchaine, omer).map { listOf("TLV", "JFK", "${it}T17:00", "${it}T22:15") }
}

fun main() {
    val texte = File("android/app/src/main/res/raw/aeroports.json").readText()
    // Pas d'org.json ici : une ligne par aéroport, ["IATA","nom","ville","PA",lat,lon,"fuseau"].
    val ligne = Regex("""\["([A-Z0-9]{3})","((?:[^"\\]|\\.)*)","((?:[^"\\]|\\.)*)","([A-Z0-9]*)",(-?[\d.]+),(-?[\d.]+),"([^"]+)"]""")
    val aeroports = ligne.findAll(texte).associate { m ->
        val g = m.groupValues
        g[1] to Vol.Aeroport(g[1], g[2], g[3], g[4], g[5].toDouble(), g[6].toDouble(), g[7])
    }
    val sb = StringBuilder("{\n\"outil\": \"tools/vecteurs_vol/VecteursVol.kt\",\n\"vols\": [\n")
    VOLS.forEachIndexed { i, ligneVol ->
        val (de, vers, decollage, atterrissage) = ligneVol
        val d = aeroports.getValue(de)
        val v = aeroports.getValue(vers)
        val releve = if (ligneVol.size > 4) Vol.Point(java.time.Instant.parse(ligneVol[6]), ligneVol[4].toDouble(), ligneVol[5].toDouble()) else null
        val plan = Vol.Plan("", d, v, Vol.instant(LocalDateTime.parse(decollage), d)!!, Vol.instant(LocalDateTime.parse(atterrissage), v)!!, releve)
        val c = Vol.calculer(plan)
        sb.append("{\"depart\": \"$de\", \"arrivee\": \"$vers\", \"decollage\": \"$decollage\", \"atterrissage\": \"$atterrissage\",\n")
        releve?.let { sb.append(" \"releve\": {\"latitude\": ${it.latitude}, \"longitude\": ${it.longitude}, \"instant\": \"${it.instant}\"},\n") }
        sb.append(" \"fiabilite\": \"${c.fiabilite.name.lowercase()}\",\n")
        sb.append(" \"enVol\": [${c.enVol.joinToString { "{\"cle\": \"${it.cle}\", \"instant\": \"${it.instant}\"}" }}],\n")
        sb.append(" \"reserves\": [${c.reserves.map { it.name.lowercase() }.sorted().joinToString { "\"$it\"" }}],\n")
        sb.append(" \"lignesDate\": [${c.lignesDate.joinToString { "[\"${it.avant}\", \"${it.apres}\"]" }}],\n")
        sb.append(" \"jalons\": [${VolCalendrier.jalons(c, false, 18).joinToString { "{\"type\": \"${it.type.name.lowercase()}\", \"instant\": \"${it.instant}\", \"date\": \"${it.date}\", \"omer\": ${it.omer}}" }}]}")
        sb.append(if (i < VOLS.size - 1) ",\n" else "\n")
    }
    sb.append("]\n}\n")
    print(sb)
}
