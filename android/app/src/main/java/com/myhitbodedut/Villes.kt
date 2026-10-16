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

    /** La distance en kilomètres entre deux points (haversine, rayon moyen de la Terre). */
    fun distanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2).let { it * it } +
            Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.sin(dLon / 2).let { it * it }
        return 2 * r * Math.asin(Math.sqrt(a.coerceIn(0.0, 1.0)))
    }

    /**
     * La ville de la liste la plus proche d'une position, à moins de [maxKm] (16/10) : elle
     * nomme la position (« près de Jérusalem »), donne son altitude et dit Israël ou diaspora,
     * que le seul fuseau devinait mal.
     */
    fun plusProche(villes: List<Ville>, lat: Double, lon: Double, maxKm: Double = 60.0): Ville? =
        villes.minByOrNull { distanceKm(lat, lon, it.latitude, it.longitude) }
            ?.takeIf { distanceKm(lat, lon, it.latitude, it.longitude) <= maxKm }
}
