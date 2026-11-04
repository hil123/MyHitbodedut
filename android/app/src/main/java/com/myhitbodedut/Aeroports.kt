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
        // Les noms pliés une fois pour toutes (04/11) : avant, chaque lettre tapée repliait
        // les 3 226 aéroports et leurs alias sur le fil de l'interface — la saisie ramait.
        val idx = index(aeroports)
        val exact = ArrayList<Int>()
        val ville = ArrayList<Int>()
        val nom = ArrayList<Int>()
        for (i in idx.indices) {
            val p = idx[i]
            when {
                p.iata == q -> exact += i
                p.ville.startsWith(q) || p.alias.any { it.startsWith(q) } -> ville += i
                p.nom.contains(q) || p.ville.contains(q) || p.alias.any { it.contains(q) } -> nom += i
            }
        }
        return (exact + ville + nom).asSequence().take(max).map { aeroports[it] }.toList()
    }

    /** Les champs d'un aéroport, déjà pliés. */
    private class Plie(val iata: String, val ville: String, val nom: String, val alias: List<String>)

    @Volatile
    private var cache: Pair<List<Vol.Aeroport>, List<Plie>>? = null

    private fun index(aeroports: List<Vol.Aeroport>): List<Plie> {
        cache?.let { (liste, plies) -> if (liste === aeroports) return plies }
        val plies = aeroports.map { Plie(plier(it.iata), plier(it.ville), plier(it.nom), it.alias.map(::plier)) }
        cache = aeroports to plies
        return plies
    }

    /** L'aéroport dont la ville (ou un autre nom) est exactement `texte`, déjà plié ; null sinon. */
    fun exact(aeroports: List<Vol.Aeroport>, texte: String): Vol.Aeroport? {
        val idx = index(aeroports)
        for (i in idx.indices) if (idx[i].ville == texte || idx[i].alias.any { it == texte }) return aeroports[i]
        return null
    }

    /** Prépare l'index hors du fil de l'interface, pour que la première lettre réponde aussitôt. */
    fun preparer(aeroports: List<Vol.Aeroport>) { index(aeroports) }

    /** Les marques (accents, nikoud) ôtées ; tiret, maqaf, apostrophe, guillemets, géresh et points deviennent des espaces. */
    internal fun plier(s: String) = Normalizer.normalize(s, Normalizer.Form.NFD).replace(MARQUES, "").lowercase()
        .replace(PONCTUATION, " ").replace(BLANCS, " ").trim()

    private val MARQUES = Regex("\\p{M}")
    private val PONCTUATION = Regex("[-־'’\"׳״.]")
    private val BLANCS = Regex("\\s+")

    /** Le nom dans la langue de l'application s'il existe, sinon celui d'OurAirports (en anglais). */
    fun nomLocal(a: Vol.Aeroport, langue: String): String = a.noms[if (langue == "iw") "he" else langue] ?: a.nom

    fun parIata(aeroports: List<Vol.Aeroport>, iata: String): Vol.Aeroport? = aeroports.firstOrNull { it.iata == iata }
}
