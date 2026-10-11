package com.myhitbodedut

import org.json.JSONObject

/**
 * Les villes proposées pour les zmanim (res/raw/villes.json), quand on ne
 * prend pas la position du téléphone. Source GeoNames (CC-BY 4.0), tirée par
 * tools/villes_construction.py ; chaque ville garde son identifiant GeoNames.
 */
data class Ville(
    val id: Long,
    val nom: LocalizedText,
    val pays: String,
    val latitude: Double,
    val longitude: Double,
    val altitude: Double,
    val fuseau: String,
) {
    fun lieu(langue: String) = Zmanim.Lieu(nom.forLanguage(langue), latitude, longitude, altitude, fuseau)

    /** Israël pour le calendrier (un jour de fête, pas deux) : la ville le dit. */
    val enIsrael: Boolean get() = pays == "IL"
}

object Villes {
    const val SOURCE = "GeoNames (CC-BY 4.0)"

    fun parse(json: String): List<Ville> =
        Json.array(JSONObject(json).getJSONArray("villes")) { v ->
            Ville(
                id = v.getLong("id"),
                nom = LocalizedText.parse(v.getJSONObject("nom")),
                pays = v.getString("pays"),
                latitude = v.getDouble("latitude"),
                longitude = v.getDouble("longitude"),
                altitude = v.optDouble("altitude", 0.0),
                fuseau = v.getString("fuseau"),
            )
        }
}
