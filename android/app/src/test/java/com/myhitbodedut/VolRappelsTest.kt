package com.myhitbodedut

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Les rappels en vol (VolRappels) et le suivi OpenSky anonyme (SuiviVol), sur les
 * trajets déjà vérifiés (VolTest) : Melbourne → Los Angeles → Montréal et retour,
 * Sydney → Queenstown, Tel Aviv → Buenos Aires, Tel Aviv ↔ New York.
 * Les bornes des prières sont celles de l'application ; ces tests vérifient
 * qu'elles sont appliquées aux zmanim de la route, pas une règle.
 */
class VolRappelsTest {

    private val aeroports = Aeroports.parse(File("src/main/res/raw/aeroports.json").readText())
    private fun a(iata: String) = Aeroports.parIata(aeroports, iata) ?: error("$iata absent")

    private fun plan(de: String, vers: String, decollage: String, atterrissage: String): Vol.Plan {
        val d = a(de)
        val v = a(vers)
        return Vol.Plan("TEST", d, v, Vol.instant(LocalDateTime.parse(decollage), d)!!, Vol.instant(LocalDateTime.parse(atterrissage), v)!!)
    }

    private val trajets = listOf(
        plan("MEL", "LAX", "2026-12-15T10:30", "2026-12-15T06:45"),
        plan("LAX", "YUL", "2026-12-15T09:30", "2026-12-15T17:55"),
        plan("YUL", "LAX", "2026-12-22T08:00", "2026-12-22T11:05"),
        plan("LAX", "MEL", "2026-12-22T22:30", "2026-12-24T08:45"),
        plan("SYD", "ZQN", "2026-12-18T09:00", "2026-12-18T14:15"),
        plan("TLV", "EZE", "2026-11-10T23:00", "2026-11-11T09:20"),
        plan("TLV", "JFK", "2026-11-10T12:30", "2026-11-10T17:45"),
        plan("JFK", "TLV", "2026-11-10T23:00", "2026-11-11T16:40"),
    )

    /** Ce que tout rappel doit respecter, quel que soit le trajet. */
    private fun verifier(c: Vol.Calcul, r: List<VolRappels.Rappel>) {
        val p = c.plan
        assertEquals(r.sortedBy { it.instant }, r)
        r.forEach { assertTrue("$it hors du vol", !it.instant.isBefore(p.decollage) && !it.instant.isAfter(p.atterrissage)) }
        val derekh = r.first()
        assertEquals(VolRappels.Type.TEFILAT_HADEREKH, derekh.type)
        assertEquals(p.decollage + Duration.ofMinutes(5), derekh.instant)
        assertEquals(VolRappels.ouverte(c, derekh.instant), derekh.priere?.let { it to derekh.limite })
        assertEquals(1, r.count { it.type == VolRappels.Type.TEFILAT_HADEREKH })
        r.filter { it.type == VolRappels.Type.PRIERE }.forEach { x ->
            val (_, debut, fin) = VolRappels.BORNES.first { it.first == x.priere }
            assertTrue(c.enVol.any { it.cle == debut && it.instant == x.instant })
            assertEquals(c.evenements.firstOrNull { it.cle == fin && it.instant.isAfter(x.instant) }?.instant, x.limite)
            assertEquals(x.priere, VolRappels.ouverte(c, x.instant)?.first)
        }
        r.filter { it.type == VolRappels.Type.PRIERE_FIN }.forEach { x ->
            assertTrue(x.priere != "arvit")
            assertEquals(Duration.ofMinutes(30), Duration.between(x.instant, x.limite))
            assertEquals(x.priere to x.limite, VolRappels.ouverte(c, x.instant))
        }
        val omer = VolCalendrier.jalons(c, false, 0).filter { it.type == VolCalendrier.Type.OMER }
        assertEquals(omer.map { it.instant to it.omer }, r.filter { it.type == VolRappels.Type.OMER }.map { it.instant to it.omer })
    }

    @Test
    fun `les rappels des trajets verifies tombent dans le vol, aux bornes de l application`() {
        trajets.forEach { p ->
            val c = Vol.calculer(p)
            val r = VolRappels.rappels(c, false)
            verifier(c, r)
            println("${p.depart.iata}→${p.arrivee.iata} " + r.joinToString(" ; ") { x ->
                "${x.type}${x.priere?.let { "($it)" } ?: ""}${if (x.omer > 0) "(${x.omer})" else ""} ${x.instant}" +
                    (x.limite?.let { " → $it" } ?: "")
            })
        }
        // Tel Aviv → New York de jour : Min'ha ouverte au décollage, sa limite dans l'avion, puis Arvit.
        val ouest = VolRappels.rappels(Vol.calculer(trajets[6]), false)
        assertEquals("minha", ouest.first().priere)
        assertTrue(ouest.any { it.type == VolRappels.Type.PRIERE_FIN && it.priere == "minha" })
        assertTrue(ouest.any { it.type == VolRappels.Type.PRIERE && it.priere == "arvit" })
        // New York → Tel Aviv de nuit : Cha'harit commence dans l'avion, au lever du soleil sous lui.
        assertTrue(VolRappels.rappels(Vol.calculer(trajets[7]), false).any { it.type == VolRappels.Type.PRIERE && it.priere == "chaharit" })
    }

    @Test
    fun `le Omer en vol a son rappel, le jour du soir`() {
        val veille = generateSequence(LocalDate.parse("2027-01-01")) { it.plusDays(1) }.first { CalendrierJuif.jour(it.plusDays(1), false).omer == 4 }
        val c = Vol.calculer(plan("TLV", "JFK", "${veille}T17:00", "${veille}T22:15"))
        val r = VolRappels.rappels(c, false)
        verifier(c, r)
        assertEquals(listOf(4), r.filter { it.type == VolRappels.Type.OMER }.map { it.omer })
        // Le Omer tombe à la sortie des étoiles, avec Arvit : deux rappels à la même minute.
        val omer = r.single { it.type == VolRappels.Type.OMER }
        assertTrue(r.any { it.type == VolRappels.Type.PRIERE && it.priere == "arvit" && it.instant == omer.instant })
    }

    @Test
    fun `un vol de moins de cinq minutes n a pas de rappel, un rappel n est montre qu une fois`() {
        val court = Vol.Plan("T", a("TLV"), a("ETM"), Instant.parse("2026-11-10T08:00:00Z"), Instant.parse("2026-11-10T08:04:00Z"))
        assertTrue(VolRappels.rappels(Vol.calculer(court), false).isEmpty())
        val r = VolRappels.rappels(Vol.calculer(trajets[6]), false)
        val t = r[1].instant.toEpochMilli()
        assertEquals(listOf(r[1]), VolAlerte.dus(r, r[0].instant.toEpochMilli(), t).filter { it == r[1] })
        assertTrue(VolAlerte.dus(r, t, t).none { it.instant.toEpochMilli() <= t })
        // Trop tard (plus de 20 min) : passé, pas montré.
        assertTrue(VolAlerte.dus(r, 0L, t + Duration.ofMinutes(25).toMillis()).none { it == r[1] })
    }

    @Test
    fun `OpenSky anonyme - la reponse brute, les carres, l adresse`() {
        val maintenant = Instant.parse("2026-11-10T18:35:00Z")
        val t = Instant.parse("2026-11-10T18:30:00Z").epochSecond
        val json = """{"time":$t,"states":[
            ["abc","AFR12   ","France",$t,$t,-20.0,51.0,11000,false,250,270,0,null,11200,"1",false,0],
            ["def","ELY001  ","Israel",$t,$t,-21.2,50.5,10900,false,250,270,0,null,11150,"2",false,0]]}"""
        assertEquals(SuiviVol.Resultat.Trouve(Vol.Point(Instant.ofEpochSecond(t), 50.5, -21.2), 11150.0), SuiviVol.lireEtats(json, "ELY1", maintenant))
        // Au sol, ou sans position : ignoré.
        val sol = """{"time":$t,"states":[["def","ELY1","Israel",$t,$t,34.9,32.0,null,true,0,0,0,null,null,"2",false,0],
            ["def","ELY1","Israel",null,$t,null,null,null,false,0,0,0,null,null,"2",false,0]]}"""
        assertEquals(SuiviVol.Resultat.Introuvable, SuiviVol.lireEtats(sol, "ELY1", maintenant))
        assertEquals(SuiviVol.Resultat.Introuvable, SuiviVol.lireEtats("""{"time":0,"states":null}""", "ELY1", maintenant))
        assertTrue(SuiviVol.lireEtats("<html>", "ELY1", maintenant) is SuiviVol.Resultat.Erreur)
        // Le carré de ±5° (2 crédits), coupé en deux au méridien de 180°, comme le relais.
        assertEquals(listOf(listOf(45.1, -25.3, 55.1, -15.3)), SuiviVol.carres(50.1, -20.3).map { it.toList() })
        assertEquals(listOf(listOf(5.0, 173.0, 15.0, 180.0), listOf(5.0, -180.0, 15.0, -177.0)), SuiviVol.carres(10.0, 178.0).map { it.toList() })
        assertEquals("https://opensky-network.org/api/states/all?lamin=45.1&lomin=-25.3&lamax=55.1&lomax=-15.3",
            SuiviVol.adresseEtats(SuiviVol.carres(50.1, -20.3).single()))
        // Sans relais configuré, l'édition interroge OpenSky en accès anonyme.
        assertTrue(SuiviVol.fournisseur("") === SuiviVol.OpenSkyAnonyme)
        assertTrue(SuiviVol.fournisseur("https://r.test") is SuiviVol.Relais)
        assertNull((SuiviVol.fournisseur("http://r.test") as? SuiviVol.Relais))
    }
}
