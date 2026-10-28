package com.myhitbodedut

import org.json.JSONObject
import java.text.Normalizer

/**
 * Les aéroports des zmanim en vol (res/raw/aeroports.json, 28/10) : OurAirports
 * (domaine public), fuseaux par timezone-boundary-builder (ODbL), tirés par
 * tools/aeroports_construction.py. Une ligne par aéroport, colonnes nommées
 * dans _meta. Les noms en hébreu et en français (res/raw/aeroports_noms.json,
 * Wikidata CC0, tools/aeroports_noms.py) s'y ajoutent pour la recherche.
 */
object Aeroports {
    const val SOURCE = "OurAirports (domaine public) ; fuseaux : timezone-boundary-builder (ODbL, © contributeurs OpenStreetMap) ; noms : Wikidata (CC0)"

    private val LANGUES = listOf("he", "fr")

    fun parse(json: String, noms: String? = null): List<Vol.Aeroport> {
        val lignes = JSONObject(json).getJSONArray("aeroports")
        val autres = noms?.let { JSONObject(it).getJSONObject("noms") }
        return (0 until lignes.length()).map { i ->
            val a = lignes.getJSONArray(i)
            val iata = a.getString(0)
            // [nom he, [villes he], nom fr, [villes fr]]
            val n = autres?.optJSONArray(iata)
            val parLangue = mutableMapOf<String, String>()
            val alias = mutableListOf<String>()
            if (n != null) LANGUES.forEachIndexed { k, langue ->
                n.getString(2 * k).takeIf { it.isNotEmpty() }?.let { parLangue[langue] = it; alias += it }
                val villes = n.getJSONArray(2 * k + 1)
                for (j in 0 until villes.length()) alias += villes.getString(j)
            }
            Vol.Aeroport(iata, a.getString(1), a.getString(2), a.getString(3), a.getDouble(4), a.getDouble(5), a.getString(6), parLangue, alias)
        }
    }

    /**
     * « Zurich », « zur », « ZRH », « תל אביב », « Londres » : sans accents, points-voyelles, casse
     * ni ponctuation ; le code IATA exact d'abord, puis la ville, puis le nom et les autres noms.
     */
    fun chercher(aeroports: List<Vol.Aeroport>, texte: String, max: Int = 20): List<Vol.Aeroport> {
        val q = plier(texte)
        if (q.isEmpty()) return emptyList()
        val exact = aeroports.filter { plier(it.iata) == q }
        val ville = aeroports.filter { plier(it.ville).startsWith(q) || it.alias.any { a -> plier(a).startsWith(q) } }
        val nom = aeroports.filter { plier(it.nom).contains(q) || plier(it.ville).contains(q) || it.alias.any { a -> plier(a).contains(q) } }
        return (exact + ville + nom).distinct().take(max)
    }

    /** Les marques (accents, nikoud) ôtées ; tiret, maqaf, apostrophe, guillemets, géresh et points deviennent des espaces. */
    internal fun plier(s: String) = Normalizer.normalize(s, Normalizer.Form.NFD).replace(Regex("\\p{M}"), "").lowercase()
        .replace(Regex("[-־'’\"׳״.]"), " ").replace(Regex("\\s+"), " ").trim()

    /** Le nom dans la langue de l'application s'il existe, sinon celui d'OurAirports (en anglais). */
    fun nomLocal(a: Vol.Aeroport, langue: String): String = a.noms[if (langue == "iw") "he" else langue] ?: a.nom

    fun parIata(aeroports: List<Vol.Aeroport>, iata: String): Vol.Aeroport? = aeroports.firstOrNull { it.iata == iata }
}
