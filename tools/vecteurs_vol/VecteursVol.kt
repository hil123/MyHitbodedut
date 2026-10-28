// Produit web/test/vecteurs_vol.json (28/10/2026) : ce que Vol.kt (sur
// Zmanim.kt et KosherJava 2.5.0) répond pour quelques vols, pour que vol.js
// (sur kosher-zmanim) lui soit comparé sans recopier une valeur à la main.
//
// À compiler avec Vol.kt et Zmanim.kt, le jar com.kosherjava:zmanim:2.5.0 et
// kotlin-stdlib 2.0.21, sur la JVM seule (README.md à côté de ce fichier).
// Lancé depuis la racine du dépôt : il lit android/app/src/main/res/raw/aeroports.json.
import com.myhitbodedut.Vol
import java.io.File
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
)

fun main() {
    val texte = File("android/app/src/main/res/raw/aeroports.json").readText()
    // Pas d'org.json ici : une ligne par aéroport, ["IATA","nom","ville","PA",lat,lon,"fuseau"].
    val ligne = Regex("""\["([A-Z0-9]{3})","((?:[^"\\]|\\.)*)","((?:[^"\\]|\\.)*)","([A-Z0-9]*)",(-?[\d.]+),(-?[\d.]+),"([^"]+)"]""")
    val aeroports = ligne.findAll(texte).associate { m ->
        val g = m.groupValues
        g[1] to Vol.Aeroport(g[1], g[2], g[3], g[4], g[5].toDouble(), g[6].toDouble(), g[7])
    }
    val sb = StringBuilder("{\n\"outil\": \"tools/vecteurs_vol/VecteursVol.kt\",\n\"vols\": [\n")
    VOLS.forEachIndexed { i, (de, vers, decollage, atterrissage) ->
        val d = aeroports.getValue(de)
        val v = aeroports.getValue(vers)
        val plan = Vol.Plan("", d, v, Vol.instant(LocalDateTime.parse(decollage), d)!!, Vol.instant(LocalDateTime.parse(atterrissage), v)!!)
        val c = Vol.calculer(plan)
        sb.append("{\"depart\": \"$de\", \"arrivee\": \"$vers\", \"decollage\": \"$decollage\", \"atterrissage\": \"$atterrissage\",\n")
        sb.append(" \"enVol\": [${c.enVol.joinToString { "{\"cle\": \"${it.cle}\", \"instant\": \"${it.instant}\"}" }}],\n")
        sb.append(" \"reserves\": [${c.reserves.map { it.name.lowercase() }.sorted().joinToString { "\"$it\"" }}],\n")
        sb.append(" \"lignesDate\": [${c.lignesDate.joinToString { "[\"${it.avant}\", \"${it.apres}\"]" }}]}")
        sb.append(if (i < VOLS.size - 1) ",\n" else "\n")
    }
    sb.append("]\n}\n")
    print(sb)
}
