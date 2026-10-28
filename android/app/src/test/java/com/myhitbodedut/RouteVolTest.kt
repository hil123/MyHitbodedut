package com.myhitbodedut

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** La route d'un numéro de vol (RouteVol, adsbdb) : les mêmes cas que vol.test.mjs, contre un faux adsbdb. */
class RouteVolTest {
    private fun route(icao: String, vararg escales: String): String {
        val p = { iata: String -> "{\"iata_code\":\"$iata\"}" }
        val milieu = if (escales.size == 3) ",\"midpoint\":${p(escales[1])}" else ""
        return "{\"response\":{\"flightroute\":{\"callsign_icao\":\"$icao\",\"airline\":{\"name\":\"Cie\"}," +
            "\"origin\":${p(escales.first())}$milieu,\"destination\":${p(escales.last())}}}}"
    }

    private val demandes = mutableListOf<String>()
    private val table = mapOf(
        "callsign/LY81" to route("ELY81", "TLV", "BKK"),
        "airline/QF" to "{\"response\":[{\"icao\":\"QFA\"}]}",
        "callsign/QFA9" to route("QFA9", "MEL", "PER", "LHR"),
    )
    private val faux = RouteVol.Lecteur { adresse ->
        demandes += adresse
        val corps = table[adresse.removePrefix(RouteVol.ADSBDB + "/")]
        if (corps != null) 200 to corps else (if (adresse.endsWith("/callsign/QF9")) 400 else 404) to "{}"
    }

    @Test
    fun `numeros IATA et OACI`() {
        assertEquals(RouteVol.Numero("LY", "81", ""), RouteVol.normaliser("ly 81"))
        assertEquals(RouteVol.Numero("ELY", "81", ""), RouteVol.normaliser("ELY081"))
        assertEquals("U2", RouteVol.normaliser("U22148")?.code)
        assertNull(RouteVol.normaliser("bonjour"))
    }

    @Test
    fun `troncon depuis le depart, repli OACI, erreurs`() {
        assertEquals(RouteVol.Resultat.Trouve("ELY81", "Cie", listOf("TLV", "BKK"), "TLV", "BKK"), RouteVol.chercher("LY81", "TLV", faux))
        assertTrue(RouteVol.chercher("LY81", "MEL", faux) is RouteVol.Resultat.AutreDepart)
        val qf = RouteVol.chercher("QF9", "PER", faux) as RouteVol.Resultat.Trouve
        assertEquals(listOf("PER", "LHR", "QFA9"), listOf(qf.de, qf.vers, qf.indicatif))
        assertEquals("PER", (RouteVol.chercher("QF9", null, faux) as RouteVol.Resultat.Trouve).vers)
        assertEquals(RouteVol.Resultat.Introuvable, RouteVol.chercher("XX999", null, faux))
        assertEquals(RouteVol.Resultat.Invalide, RouteVol.chercher("nimporte", null, faux))
        assertEquals(RouteVol.Resultat.Erreur("HTTP 503"), RouteVol.chercher("LY81", null) { 503 to "" })
        // Seul le numéro part : ni position, ni aéroport de départ dans l'adresse.
        assertTrue(demandes.all { it.startsWith(RouteVol.ADSBDB + "/") && '?' !in it })
    }
}
