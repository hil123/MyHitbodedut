package com.myhitbodedut

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.time.Duration
import java.time.Instant

/**
 * Le suivi d'un vol en direct (28/10), option désactivée par défaut et propre
 * à l'édition complète : la position réelle de l'avion, demandée à un relais
 * (tools/relais_vol, sur Cloudflare Workers) qui garde la clé du fournisseur
 * et interroge OpenSky Network. Aucune clé n'est dans l'application.
 *
 * Ce qui part du téléphone : l'indicatif radio du vol (« ELY1 ») et la position
 * estimée sur la route, pour que le relais cherche autour d'elle. Rien d'autre.
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
