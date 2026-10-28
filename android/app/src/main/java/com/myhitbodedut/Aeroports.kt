package com.myhitbodedut

import org.json.JSONObject
import java.text.Normalizer

/**
 * Les aéroports des zmanim en vol (res/raw/aeroports.json, 28/10) : OurAirports
 * (domaine public), fuseaux par timezone-boundary-builder (ODbL), tirés par
 * tools/aeroports_construction.py. Une ligne par aéroport, colonnes nommées
 * dans _meta.
 */
object Aeroports {
    const val SOURCE = "OurAirports (domaine public) ; fuseaux : timezone-boundary-builder (ODbL, © contributeurs OpenStreetMap)"

    fun parse(json: String): List<Vol.Aeroport> {
        val lignes = JSONObject(json).getJSONArray("aeroports")
        return (0 until lignes.length()).map { i ->
            val a = lignes.getJSONArray(i)
            Vol.Aeroport(a.getString(0), a.getString(1), a.getString(2), a.getString(3), a.getDouble(4), a.getDouble(5), a.getString(6))
        }
    }

    /** « Zurich », « zur », « ZRH » : sans accents ni casse ; le code IATA exact d'abord, puis la ville, puis le nom. */
    fun chercher(aeroports: List<Vol.Aeroport>, texte: String, max: Int = 20): List<Vol.Aeroport> {
        val q = plier(texte.trim())
        if (q.isEmpty()) return emptyList()
        val exact = aeroports.filter { plier(it.iata) == q }
        val ville = aeroports.filter { plier(it.ville).startsWith(q) }
        val nom = aeroports.filter { plier(it.nom).contains(q) || plier(it.ville).contains(q) }
        return (exact + ville + nom).distinct().take(max)
    }

    private fun plier(s: String) = Normalizer.normalize(s, Normalizer.Form.NFD).replace(Regex("\\p{M}"), "").lowercase()

    fun parIata(aeroports: List<Vol.Aeroport>, iata: String): Vol.Aeroport? = aeroports.firstOrNull { it.iata == iata }
}
