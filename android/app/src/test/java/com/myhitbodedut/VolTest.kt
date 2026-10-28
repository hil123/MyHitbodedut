package com.myhitbodedut

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.math.abs

/**
 * Les zmanim en vol (28/10), phase 1 hors ligne. Ces tests vérifient le
 * calcul (le point fixe, les fuseaux, minuit, la ligne de date, les routes
 * polaires) ; ils ne valident aucune règle halakhique : le choix de
 * calculer au point sous l'avion, au niveau de la mer, est décrit dans
 * ZMANIM.md (« En vol »).
 */
class VolTest {

    private val aeroports = Aeroports.parse(File("src/main/res/raw/aeroports.json").readText())
    private fun a(iata: String) = Aeroports.parIata(aeroports, iata) ?: error("$iata absent")

    private fun plan(de: String, vers: String, decollage: String, atterrissage: String): Vol.Plan {
        val d = a(de)
        val v = a(vers)
        return Vol.Plan("TEST", d, v, Vol.instant(LocalDateTime.parse(decollage), d)!!, Vol.instant(LocalDateTime.parse(atterrissage), v)!!)
    }

    // Tel Aviv → New York, vers l'ouest, de jour ; New York → Tel Aviv, vers l'est, de nuit.
    private val versOuest = plan("TLV", "JFK", "2026-11-10T12:30", "2026-11-10T17:45")
    private val versEst = plan("JFK", "TLV", "2026-11-10T23:00", "2026-11-11T16:40")

    @Test
    fun `les aeroports sont complets et leurs fuseaux connus`() {
        assertTrue(aeroports.size > 3000)
        assertEquals(aeroports.size, aeroports.map { it.iata }.toSet().size)
        aeroports.forEach {
            assertTrue(it.iata, it.iata.matches(Regex("[A-Z0-9]{3}")))
            assertTrue(it.iata, abs(it.latitude) <= 90 && abs(it.longitude) <= 180)
            assertNotNull("${it.iata} ${it.fuseau}", Vol.zone(it.fuseau))
        }
        assertEquals("Asia/Jerusalem", a("TLV").fuseau)
        assertEquals("Europe/Paris", a("CDG").fuseau)
        // À cheval sur la frontière, à l'heure de son aérogare (tools/aeroports_construction.py).
        assertEquals("Australia/Brisbane", a("OOL").fuseau)
        // Fuseau disputé : non proposé.
        assertNull(Aeroports.parIata(aeroports, "URC"))
    }

    @Test
    fun `la recherche trouve le code, la ville sans accents, le nom`() {
        assertEquals("TLV", Aeroports.chercher(aeroports, "tlv").first().iata)
        assertTrue(Aeroports.chercher(aeroports, "zurich").any { it.iata == "ZRH" })
        assertTrue(Aeroports.chercher(aeroports, "Ben Gurion").any { it.iata == "TLV" })
        assertTrue(Aeroports.chercher(aeroports, "  ").isEmpty())
    }

    @Test
    fun `les heures du billet se lisent a l heure de chaque aeroport`() {
        assertEquals(Instant.parse("2026-07-01T08:00:00Z"), Vol.instant(LocalDateTime.parse("2026-07-01T10:00"), a("CDG")))
        assertEquals(Instant.parse("2026-01-01T15:00:00Z"), Vol.instant(LocalDateTime.parse("2026-01-01T10:00"), a("JFK")))
        assertNull(Vol.instant(LocalDateTime.parse("2026-01-01T10:00"), a("JFK").copy(fuseau = "Mars/Olympus")))
    }

    @Test
    fun `une saisie impossible est refusee`() {
        assertNull(Vol.verifier(versOuest))
        assertEquals(Vol.Invalide.MEME_AEROPORT, Vol.verifier(versOuest.copy(arrivee = versOuest.depart)))
        assertEquals(Vol.Invalide.ARRIVEE_AVANT_DEPART, Vol.verifier(versOuest.copy(atterrissage = versOuest.decollage)))
        assertEquals(Vol.Invalide.TROP_LONG, Vol.verifier(versOuest.copy(atterrissage = versOuest.decollage + Duration.ofHours(23))))
        assertEquals(Vol.Invalide.FUSEAU_INCONNU, Vol.verifier(versOuest.copy(arrivee = a("JFK").copy(fuseau = "Mars/Olympus"))))
    }

    @Test
    fun `la route va d un aeroport a l autre par le grand cercle`() {
        val debut = Vol.position(versOuest, versOuest.decollage - Duration.ofHours(3))
        assertEquals(a("TLV").latitude, debut.latitude, 1e-9)
        assertEquals(a("TLV").longitude, debut.longitude, 1e-9)
        val fin = Vol.position(versOuest, versOuest.atterrissage + Duration.ofHours(3))
        assertEquals(a("JFK").latitude, fin.latitude, 1e-6)
        assertEquals(a("JFK").longitude, fin.longitude, 1e-6)
        val milieu = Vol.position(versOuest, versOuest.decollage + Duration.between(versOuest.decollage, versOuest.atterrissage).dividedBy(2))
        val total = Vol.distanceKm(versOuest)
        assertEquals(total / 2, Vol.distanceKm(a("TLV").latitude, a("TLV").longitude, milieu.latitude, milieu.longitude), 1.0)
        // Le grand cercle de Tel Aviv à New York passe bien au nord des deux villes.
        assertTrue(milieu.latitude > 50)
    }

    @Test
    fun `le fuseau nautique suit la longitude et son signe inverse`() {
        assertEquals("Etc/GMT-2", Vol.fuseauNautique(35.0))
        assertEquals("Etc/GMT+5", Vol.fuseauNautique(-74.0))
        assertEquals("Etc/GMT", Vol.fuseauNautique(3.0))
        assertEquals("Etc/GMT-12", Vol.fuseauNautique(179.9))
        assertEquals("Etc/GMT+12", Vol.fuseauNautique(-179.9))
        listOf(35.0, -74.0, 3.0, 179.9, -179.9).forEach { ZoneId.of(Vol.fuseauNautique(it)) }
    }

    /** Au sol, avant le décollage : exactement les zmanim du moteur à l'aéroport, à la seconde. */
    @Test
    fun `au sol, le calcul rend les zmanim du moteur`() {
        val c = Vol.calculer(versOuest)
        val tlv = a("TLV").let { Zmanim.Lieu(it.ville, it.latitude, it.longitude, 0.0, it.fuseau) }
        val jour = Zmanim.jour(tlv, LocalDate.parse("2026-11-10")).associateBy { it.cle }
        listOf("alot", "netz", "szt_gra").forEach { cle ->
            val attendu = jour.getValue(cle).instant!!
            val trouve = c.evenements.first { it.cle == cle && abs(Duration.between(attendu, it.instant).seconds) < 3600 }
            assertTrue("$cle : ${trouve.instant} contre $attendu", abs(Duration.between(attendu, trouve.instant).toMillis()) <= 1000)
        }
    }

    /** Chaque zman trouvé en vol est le zman du point où est l'avion à cet instant (le point fixe). */
    @Test
    fun `chaque zman en vol est le zman du point sous l avion`() {
        listOf(versOuest, versEst).forEach { plan ->
            val c = Vol.calculer(plan)
            assertTrue(c.enVol.isNotEmpty())
            c.enVol.forEach { e ->
                val p = Vol.Point(e.instant, e.latitude, e.longitude)
                val lieu = Vol.lieuSous(p)
                val jour = LocalDate.ofInstant(e.instant, ZoneId.of(lieu.fuseau))
                val zmanim = (-1L..1L).flatMap { Zmanim.jour(lieu, jour.plusDays(it)) }.filter { it.cle == e.cle && it.instant != null }
                val ecart = zmanim.minOf { abs(Duration.between(it.instant, e.instant).toMillis()) }
                assertTrue("${e.cle} à ${e.instant} : écart $ecart ms", ecart <= 2000)
            }
        }
    }

    /** Vers l'ouest, on suit le soleil : le jour en vol dure plus qu'au sol ; vers l'est, la nuit raccourcit. */
    @Test
    fun `vers l ouest le jour s allonge, vers l est la nuit raccourcit`() {
        fun sol(iata: String, iso: String, cle: String) = a(iata).let { Zmanim.Lieu(it.ville, it.latitude, it.longitude, 0.0, it.fuseau) }
            .let { Zmanim.jour(it, LocalDate.parse(iso)).first { z -> z.cle == cle }.instant!! }
        val ouest = Vol.calculer(versOuest)
        val chkiaVol = ouest.enVol.first { it.cle == "chkia" }.instant
        val jourVol = Duration.between(sol("TLV", "2026-11-10", "netz"), chkiaVol)
        listOf("TLV", "JFK").forEach {
            val jourSol = Duration.between(sol(it, "2026-11-10", "netz"), sol(it, "2026-11-10", "chkia"))
            assertTrue("$it : $jourVol contre $jourSol", jourVol > jourSol + Duration.ofMinutes(15))
        }
        val est = Vol.calculer(versEst)
        val netzVol = est.enVol.first { it.cle == "netz" }.instant
        val nuitVol = Duration.between(sol("JFK", "2026-11-10", "chkia"), netzVol)
        listOf("TLV", "JFK").forEach {
            val nuitSol = Duration.between(sol(it, "2026-11-10", "chkia"), sol(it, "2026-11-11", "netz"))
            assertTrue("$it : $nuitVol contre $nuitSol", nuitVol < nuitSol - Duration.ofHours(2))
        }
        // Sans doublon : un même zman ne revient pas en vol à moins de 12 h, sauf à rebours.
        listOf(ouest, est).forEach { c ->
            assertFalse(Vol.Reserve.A_REBOURS in c.reserves)
            c.enVol.groupBy { it.cle }.values.forEach { l ->
                l.zipWithNext().forEach { (x, y) -> assertTrue(x.cle, Duration.between(x.instant, y.instant) > Duration.ofHours(12)) }
            }
        }
    }

    @Test
    fun `minuit UTC et minuit local traverses sans perte`() {
        // Tel Aviv 22:30 → Paris 02:00 : minuit local au départ, minuit UTC (22:00 Z) avant.
        val nuit = plan("TLV", "CDG", "2026-11-10T22:30", "2026-11-11T02:00")
        val c = Vol.calculer(nuit)
        assertEquals(1, c.enVol.count { it.cle == "chatsot_layla" })
        assertTrue(c.evenements.zipWithNext().all { (x, y) -> !y.instant.isBefore(x.instant) })
        // Avant minuit de la nuit : Arvit jusqu'à minuit ; après, Arvit sans limite.
        val minuit = c.enVol.first { it.cle == "chatsot_layla" }.instant
        assertEquals("arvit" to minuit, VolPriere.priere(c, minuit - Duration.ofMinutes(10)))
        assertEquals("arvit" to null, VolPriere.priere(c, minuit + Duration.ofMinutes(10)))
    }

    @Test
    fun `la priere du moment suit les zmanim de la route, par ZmanimWidget`() {
        val c = Vol.calculer(versOuest)
        val chkia = c.enVol.first { it.cle == "chkia" }.instant
        assertEquals("minha" to chkia, VolPriere.priere(c, chkia - Duration.ofMinutes(5)))
        val (priere, limite) = VolPriere.priere(c, chkia + Duration.ofMinutes(5))!!
        assertEquals("arvit", priere)
        assertEquals(c.evenements.first { it.cle == "chatsot_layla" && it.instant.isAfter(chkia) }.instant, limite)
        // Loin du vol, la fenêtre ne couvre plus : rien plutôt qu'une heure fausse.
        assertNull(VolPriere.priere(c, versOuest.atterrissage + Duration.ofHours(7)))
        assertNull(VolPriere.priere(c, versOuest.decollage - Duration.ofHours(7)))
    }

    @Test
    fun `un vol court sans zman reste calculable`() {
        val court = plan("TLV", "ETM", "2026-11-10T09:50", "2026-11-10T10:40")
        val c = Vol.calculer(court)
        assertTrue(c.enVol.isEmpty())
        assertEquals("minha", VolPriere.priere(c, court.decollage + Duration.ofMinutes(20))!!.first)
        assertTrue(c.reserves.isEmpty())
        assertTrue(c.lignesDate.isEmpty())
    }

    @Test
    fun `chaque horaire en vol dit de combien il bouge si le vol part 30 minutes plus tard`() {
        val c = Vol.calculer(versEst)
        c.enVol.forEach { e ->
            val ecart = e.ecartSiRetard
            assertNotNull("${e.cle} sans correspondant", ecart)
            assertTrue("${e.cle} : $ecart", ecart!!.abs() <= Duration.ofMinutes(45))
        }
    }

    @Test
    fun `la ligne de date est datee de part et d autre, vers l ouest et vers l est`() {
        val ouest = Vol.calculer(plan("SFO", "HKG", "2026-11-10T11:00", "2026-11-11T19:00"))
        assertEquals(1, ouest.lignesDate.size)
        ouest.lignesDate.single().let { assertEquals(it.avant.plusDays(1), it.apres) }
        val est = Vol.calculer(plan("HKG", "SFO", "2026-11-10T21:00", "2026-11-10T18:00"))
        assertEquals(1, est.lignesDate.size)
        est.lignesDate.single().let { assertEquals(it.avant.minusDays(1), it.apres) }
        // La ligne passe entre le décollage et l'atterrissage.
        listOf(ouest, est).forEach { c ->
            val t = c.lignesDate.single().instant
            assertTrue(t.isAfter(c.plan.decollage) && t.isBefore(c.plan.atterrissage))
        }
        assertTrue(Vol.calculer(versOuest).lignesDate.isEmpty())
    }

    @Test
    fun `une route polaire en ete signale la haute latitude et les zmanim absents`() {
        // New York → Hong Kong en juin : le grand cercle passe près du pôle, l'aube à 16,1° n'y a pas lieu.
        val c = Vol.calculer(plan("JFK", "HKG", "2026-06-20T11:00", "2026-06-21T15:00"))
        assertTrue(Vol.Reserve.HAUTE_LATITUDE in c.reserves)
        assertTrue(Vol.Reserve.ABSENT in c.reserves)
        assertFalse(Vol.Reserve.ERREUR in c.reserves)
        assertTrue(c.enVol.zipWithNext().all { (x, y) -> !y.instant.isBefore(x.instant) })
    }

    /** web/test/vecteurs_vol.json (tools/vecteurs_vol) reste ce que Vol.kt répond : sinon, le régénérer. */
    @Test
    fun `les vecteurs du web sont ceux de Vol`() {
        val vols = org.json.JSONObject(File("../../web/test/vecteurs_vol.json").readText()).getJSONArray("vols")
        assertTrue(vols.length() >= 6)
        for (i in 0 until vols.length()) {
            val v = vols.getJSONObject(i)
            val c = Vol.calculer(plan(v.getString("depart"), v.getString("arrivee"), v.getString("decollage"), v.getString("atterrissage")))
            val attendus = v.getJSONArray("enVol")
            val nom = "${v.getString("depart")}-${v.getString("arrivee")}"
            assertEquals(nom, attendus.length(), c.enVol.size)
            c.enVol.forEachIndexed { j, e ->
                val x = attendus.getJSONObject(j)
                assertEquals(nom, x.getString("cle"), e.cle)
                assertTrue("$nom ${e.cle}", abs(Duration.between(Instant.parse(x.getString("instant")), e.instant).toMillis()) <= 1000)
            }
            val reserves = v.getJSONArray("reserves").let { r -> (0 until r.length()).map { r.getString(it) } }
            assertEquals(nom, reserves, c.reserves.map { it.name.lowercase() }.sorted())
            assertEquals(nom, v.getJSONArray("lignesDate").length(), c.lignesDate.size)
        }
    }
}
