package com.myhitbodedut

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * La route d'un numéro de vol (28/10), édition complète seulement, au toucher : le numéro
 * (« LY81 » ou « ELY81 ») part vers api.adsbdb.com, gratuit, sans compte ni clé ; rien d'autre.
 * La réponse donne la compagnie, l'indicatif OACI (pour le suivi en direct) et les escales
 * connues. Pas les heures : aucune source gratuite ne les donne, elles restent à saisir.
 * Les routes d'adsbdb peuvent être anciennes ; l'écran le dit. Même logique que vol.js
 * (normaliserNumero, lireRoute, chercherRoute).
 */
object RouteVol {
    const val ADSBDB = "https://api.adsbdb.com/v0"

    data class Numero(val code: String, val chiffres: String, val suffixe: String)

    sealed class Resultat {
        data class Trouve(val indicatif: String?, val compagnie: String, val escales: List<String>, val de: String, val vers: String) : Resultat()

        /** Le vol connu ne part pas de l'aéroport choisi. */
        data class AutreDepart(val indicatif: String?, val compagnie: String, val escales: List<String>) : Resultat()
        object Introuvable : Resultat()
        object Invalide : Resultat()
        data class Erreur(val raison: String) : Resultat()
    }

    /** Ce qui lit une adresse : le réseau, ou un faux dans les tests. (code HTTP, corps) */
    fun interface Lecteur {
        fun lire(adresse: String): Pair<Int, String>
    }

    /** « ly 81 » → LY / 81 ; « ELY081 » → ELY / 81 ; null sinon. */
    fun normaliser(texte: String): Numero? {
        val m = Regex("([A-Z]{3}|[A-Z][A-Z0-9]|[0-9][A-Z])([0-9]{1,4})([A-Z]?)").matchEntire(texte.uppercase().replace(Regex("\\s+"), "")) ?: return null
        return Numero(m.groupValues[1], m.groupValues[2].trimStart('0').ifEmpty { "0" }, m.groupValues[3])
    }

    /** `response.flightroute` d'adsbdb : le tronçon qui part de `depart` (IATA) s'il est donné, sinon le premier. */
    fun lire(json: String, depart: String?): Resultat = try {
        val r = JSONObject(json).optJSONObject("response")?.optJSONObject("flightroute")
        if (r == null) Resultat.Introuvable else {
            val escales = listOf("origin", "midpoint", "destination").mapNotNull { k ->
                r.optJSONObject(k)?.optString("iata_code")?.takeIf { Regex("[A-Z0-9]{3}").matches(it) }
            }
            val indicatif = SuiviVol.normaliser(r.optString("callsign_icao"))
            val compagnie = r.optJSONObject("airline")?.optString("name").orEmpty()
            val i = if (depart == null) 0 else escales.indexOf(depart)
            when {
                escales.size < 2 -> Resultat.Introuvable
                i < 0 || i >= escales.size - 1 -> Resultat.AutreDepart(indicatif, compagnie, escales)
                else -> Resultat.Trouve(indicatif, compagnie, escales, escales[i], escales[i + 1])
            }
        }
    } catch (e: Exception) {
        Resultat.Erreur("réponse illisible")
    }

    /**
     * Le numéro → la route. adsbdb refuse certains numéros IATA courts (« QF9 ») : on reprend alors
     * avec l'indicatif OACI de chaque compagnie qui porte ce code (adsbdb /airline). Hors du fil de
     * l'interface.
     */
    fun chercher(numero: String, depart: String?, lecteur: Lecteur = Reseau): Resultat {
        val n = normaliser(numero) ?: return Resultat.Invalide
        val q = { v: String -> URLEncoder.encode(v, "UTF-8") }
        fun demande(chemin: String): String? {
            val (code, corps) = lecteur.lire("$ADSBDB/$chemin")
            if (code == 404 || code == 400) return null
            if (code !in 200..299) throw java.io.IOException("HTTP $code")
            return corps
        }
        return try {
            demande("callsign/${q(n.code + n.chiffres + n.suffixe)}")?.let { return lire(it, depart) }
            if (n.code.length != 2) return Resultat.Introuvable
            val compagnies = demande("airline/${q(n.code)}")?.let { JSONObject(it).optJSONArray("response") } ?: JSONArray()
            for (i in 0 until compagnies.length()) {
                val icao = compagnies.optJSONObject(i)?.optString("icao").orEmpty()
                if (!Regex("[A-Z]{3}").matches(icao)) continue
                demande("callsign/$icao${n.chiffres}${n.suffixe}")?.let { return lire(it, depart) }
            }
            Resultat.Introuvable
        } catch (e: java.io.IOException) {
            Resultat.Erreur(e.message?.takeIf { it.startsWith("HTTP") } ?: "pas de connexion")
        } catch (e: org.json.JSONException) {
            Resultat.Erreur("réponse illisible")
        }
    }

    /** HTTPS seulement, sans redirection. */
    object Reseau : Lecteur {
        override fun lire(adresse: String): Pair<Int, String> {
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
    }
}
