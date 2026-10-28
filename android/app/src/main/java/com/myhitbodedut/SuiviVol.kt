package com.myhitbodedut

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.time.Duration
import java.time.Instant

/**
 * Le suivi d'un vol en direct (28/10), option désactivée par défaut et propre
 * à l'édition complète : la position réelle de l'avion, demandée à OpenSky
 * Network. Par défaut, l'accès anonyme et gratuit (OpenSky) : sans compte ni
 * clé, 400 crédits par jour et par adresse IP, données à 10 s près ; le
 * téléphone demande les avions d'un carré autour de la position estimée et y
 * cherche l'indicatif lui-même : l'indicatif ne part pas. Si l'édition est
 * construite avec un relais (-PrelaisVol, tools/relais_vol), c'est lui qu'on
 * interroge : il reçoit l'indicatif et la position estimée. Aucune clé n'est
 * dans l'application.
 * La position reçue devient un relevé de la route, comme celui du GPS
 * (Vol.Plan.releve) ; le calcul reste celui de Vol, sur l'appareil.
 *
 * Le fournisseur est interchangeable : FournisseurPosition.
 */
object SuiviVol {

    sealed class Resultat {
        data class Trouve(val point: Vol.Point, val altitudeMetres: Double?) : Resultat()

        /** L'avion n'est pas vu autour de la position estimée (hors couverture, indicatif inexact). */
        object Introuvable : Resultat()

        data class Erreur(val raison: String) : Resultat()
    }

    /** D'où vient la position : un relais, un autre service plus tard, un faux dans les tests. */
    fun interface FournisseurPosition {
        fun position(indicatif: String, estime: Vol.Point): Resultat
    }

    /** Une position plus vieille ne recale plus la route : l'avion a fait 150 km depuis. */
    val FRAICHEUR_MAX: Duration = Duration.ofMinutes(10)

    /** « ely 001 » → « ELY001 » ; null si ce n'est pas un indicatif OACI plausible (3 lettres puis 1 à 4 chiffres, une lettre au plus). */
    fun normaliser(texte: String): String? = texte.uppercase().replace(" ", "").takeIf { Regex("[A-Z]{3}[0-9]{1,4}[A-Z]{0,2}").matches(it) }

    /** ELY001 et ELY1 sont le même vol : même préfixe, même numéro, mêmes lettres finales. */
    fun memeIndicatif(a: String, b: String): Boolean {
        fun parties(x: String) = Regex("([A-Z]{3})0*([0-9]+)([A-Z]*)").matchEntire(x.uppercase().trim())?.groupValues?.drop(1)
        val pa = parties(a) ?: return false
        return pa == parties(b)
    }

    /** La réponse du relais : { trouve, latitude, longitude, altitude_geo, altitude_baro, instant }. */
    fun lire(json: String, maintenant: Instant): Resultat = try {
        val o = JSONObject(json)
        when {
            o.has("erreur") -> Resultat.Erreur(o.getString("erreur"))
            !o.optBoolean("trouve", false) -> Resultat.Introuvable
            else -> {
                val instant = Instant.parse(o.getString("instant"))
                val lat = o.getDouble("latitude")
                val lon = o.getDouble("longitude")
                when {
                    lat !in -90.0..90.0 || lon !in -180.0..180.0 -> Resultat.Erreur("position invalide")
                    Duration.between(instant, maintenant) > FRAICHEUR_MAX -> Resultat.Introuvable
                    else -> Resultat.Trouve(Vol.Point(instant, lat, lon),
                        listOf("altitude_geo", "altitude_baro").firstNotNullOfOrNull { k -> o.optDouble(k).takeIf { !it.isNaN() } })
                }
            }
        }
    } catch (e: Exception) {
        Resultat.Erreur("réponse illisible")
    }

    /** L'adresse demandée au relais : seulement l'indicatif et la position estimée, arrondie au dixième de degré. */
    fun adresse(relais: String, indicatif: String, estime: Vol.Point): String {
        val q = { v: String -> URLEncoder.encode(v, "UTF-8") }
        return "${relais.trimEnd('/')}/position?indicatif=${q(indicatif)}" +
            "&lat=${"%.1f".format(java.util.Locale.ROOT, estime.latitude)}&lon=${"%.1f".format(java.util.Locale.ROOT, estime.longitude)}"
    }

    const val ETATS = "https://opensky-network.org/api/states/all"
    const val DEMI_COTE = 5.0

    /** Les carrés à demander : un seul, ou deux de part et d'autre du méridien de 180° (tools/relais_vol, carres). */
    fun carres(lat: Double, lon: Double, d: Double = DEMI_COTE): List<DoubleArray> {
        val lamin = maxOf(-90.0, lat - d)
        val lamax = minOf(90.0, lat + d)
        val lomin = lon - d
        val lomax = lon + d
        return when {
            lomin < -180 -> listOf(doubleArrayOf(lamin, lomin + 360, lamax, 180.0), doubleArrayOf(lamin, -180.0, lamax, lomax))
            lomax > 180 -> listOf(doubleArrayOf(lamin, lomin, lamax, 180.0), doubleArrayOf(lamin, -180.0, lamax, lomax - 360))
            else -> listOf(doubleArrayOf(lamin, lomin, lamax, lomax))
        }
    }

    fun adresseEtats(c: DoubleArray): String {
        val f = { v: Double -> "%.1f".format(java.util.Locale.ROOT, v) }
        return "$ETATS?lamin=${f(c[0])}&lomin=${f(c[1])}&lamax=${f(c[2])}&lomax=${f(c[3])}"
    }

    /**
     * La réponse d'OpenSky (/states/all) : l'avion en vol dont l'indicatif correspond, au format
     * du relais. [icao24, callsign, pays, time_position, last_contact, longitude, latitude,
     * baro_altitude, on_ground, …, geo_altitude (13)].
     */
    fun lireEtats(json: String, indicatif: String, maintenant: Instant): Resultat = try {
        val etats = JSONObject(json).optJSONArray("states")
        var trouve: Resultat = Resultat.Introuvable
        for (i in 0 until (etats?.length() ?: 0)) {
            val e = etats!!.getJSONArray(i)
            if (e.isNull(1) || e.isNull(3) || e.isNull(5) || e.isNull(6) || e.optBoolean(8, false)) continue
            if (!memeIndicatif(e.getString(1), indicatif)) continue
            val o = JSONObject().put("trouve", true).put("latitude", e.getDouble(6)).put("longitude", e.getDouble(5))
                .put("instant", Instant.ofEpochSecond(e.getLong(3)).toString())
            if (!e.isNull(13)) o.put("altitude_geo", e.getDouble(13))
            if (!e.isNull(7)) o.put("altitude_baro", e.getDouble(7))
            trouve = lire(o.toString(), maintenant)
            break
        }
        trouve
    } catch (e: Exception) {
        Resultat.Erreur("réponse illisible")
    }

    private fun lireUrl(adresse: String): Pair<Int, String> {
        val c = URL(adresse).openConnection() as HttpURLConnection
        c.connectTimeout = 15_000
        c.readTimeout = 20_000
        c.instanceFollowRedirects = false
        try {
            val code = c.responseCode
            val flux = (if (code in 200..299) c.inputStream else c.errorStream) ?: return code to ""
            return code to flux.bufferedReader().use { it.readText() }
        } finally {
            c.disconnect()
        }
    }

    /** OpenSky, accès anonyme et gratuit : rien que la zone part, l'indicatif est cherché ici. */
    object OpenSkyAnonyme : FournisseurPosition {
        override fun position(indicatif: String, estime: Vol.Point): Resultat = try {
            var r: Resultat = Resultat.Introuvable
            for (carre in carres(estime.latitude, estime.longitude)) {
                val (code, corps) = lireUrl(adresseEtats(carre))
                if (code == 429) { r = Resultat.Erreur("quota OpenSky du jour épuisé"); break }
                if (code !in 200..299) { r = Resultat.Erreur("opensky $code"); break }
                r = lireEtats(corps, indicatif, Instant.now())
                if (r !is Resultat.Introuvable) break
            }
            r
        } catch (e: java.io.IOException) {
            Resultat.Erreur("pas de connexion")
        }
    }

    /** Le fournisseur de l'édition : le relais s'il est configuré à la construction, sinon OpenSky anonyme. */
    fun fournisseur(relais: String): FournisseurPosition = if (relais.startsWith("https://")) Relais(relais) else OpenSkyAnonyme

    /** Le relais OpenSky (tools/relais_vol), en HTTPS seulement ; à appeler hors du fil de l'interface. */
    class Relais(private val relais: String) : FournisseurPosition {
        override fun position(indicatif: String, estime: Vol.Point): Resultat {
            if (!relais.startsWith("https://")) return Resultat.Erreur("relais non configuré")
            return try {
                val c = URL(adresse(relais, indicatif, estime)).openConnection() as HttpURLConnection
                c.connectTimeout = 15_000
                c.readTimeout = 20_000
                c.instanceFollowRedirects = false
                try {
                    val flux = if (c.responseCode in 200..499) (c.errorStream ?: c.inputStream) else return Resultat.Erreur("relais ${c.responseCode}")
                    lire(flux.bufferedReader().use { it.readText() }, Instant.now())
                } finally {
                    c.disconnect()
                }
            } catch (e: java.io.IOException) {
                Resultat.Erreur("pas de connexion")
            }
        }
    }
}
