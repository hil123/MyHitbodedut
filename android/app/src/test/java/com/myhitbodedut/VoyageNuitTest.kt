package com.myhitbodedut

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

/** Les corrections du 02/11 : la fenêtre de nuit de la barrière, le prochain changement des vignettes. */
class VoyageNuitTest {

    private val nuit = 21 * 60 to 2 * 60 // 21:00–02:00
    private val d = LocalDate.of(2026, 11, 2)

    private fun faite(maintenant: LocalDateTime, faitA: LocalDateTime?, joursFaits: Set<LocalDate> = emptySet()) =
        Store.fenetreFaite(nuit.first, nuit.second, maintenant, faitA) { it in joursFaits }

    @Test fun seanceDuSoirCouvreLaNuitApresMinuit() {
        // La séance de 21 h 30 : à 0 h 30, la barrière ne se réarme plus.
        assertTrue(faite(d.plusDays(1).atTime(0, 30), d.atTime(21, 30), setOf(d)))
    }

    @Test fun seanceApresMinuitCouvreLaNuitMaisPasLeSoirSuivant() {
        val seance = d.plusDays(1).atTime(0, 30)
        assertTrue(faite(d.plusDays(1).atTime(0, 40), seance, setOf(d.plusDays(1))))
        // Le soir suivant, la séance de 0 h 30 appartenait à la nuit d'avant.
        assertFalse(faite(d.plusDays(1).atTime(21, 30), seance, setOf(d.plusDays(1))))
    }

    @Test fun seanceDuMatinCouvreLeSoir() {
        assertTrue(faite(d.atTime(21, 30), d.atTime(10, 0), setOf(d)))
    }

    @Test fun riendeFaitArme() {
        assertFalse(faite(d.atTime(23, 0), d.minusDays(1).atTime(21, 30), setOf(d.minusDays(1))))
    }

    @Test fun sansHeureLeJourDeLaFenetreFaitFoi() {
        assertTrue(faite(d.plusDays(1).atTime(1, 0), null, setOf(d)))
        assertFalse(faite(d.plusDays(1).atTime(1, 0), null, setOf(d.plusDays(1))))
    }

    @Test fun fenetreDeJourInchangee() {
        val matin = Store.fenetreFaite(6 * 60, 12 * 60, d.atTime(7, 0), null) { it == d }
        assertTrue(matin)
        assertFalse(Store.fenetreFaite(6 * 60, 12 * 60, d.atTime(7, 0), d.atTime(7, 0)) { false })
    }

    private val paris = ZoneId.of("Europe/Paris")

    @Test fun vignetteChangeALaFinDeChaharit() {
        val maintenant = ZonedDateTime.of(d.atTime(9, 5), paris)
        val finChaharit = ZonedDateTime.of(d.atTime(9, 12), paris)
        assertEquals(finChaharit, ZmanimVignette.prochainChangement(listOf(finChaharit, null, finChaharit.plusHours(5)), maintenant))
    }

    @Test fun vignetteAuPlusTardDansDixMinutes() {
        val maintenant = ZonedDateTime.of(d.atTime(13, 0), paris)
        assertEquals(maintenant.plusMinutes(10),
            ZmanimVignette.prochainChangement(listOf(maintenant.plusHours(3), maintenant.minusHours(1)), maintenant))
    }

    @Test fun vignetteAMinuitPourLaDate() {
        val maintenant = ZonedDateTime.of(d.atTime(23, 55), paris)
        assertEquals(ZonedDateTime.of(d.plusDays(1).atStartOfDay(), paris), ZmanimVignette.prochainChangement(emptyList(), maintenant))
    }

    @Test fun dateHebraiqueChangeAuTset() {
        val tset = ZonedDateTime.of(d.atTime(17, 52), paris)
        assertEquals(d, ZmanimVignette.jourHebreuAffiche(tset.minusMinutes(1), tset))
        assertEquals(d.plusDays(1), ZmanimVignette.jourHebreuAffiche(tset, tset))
        assertEquals(d.plusDays(1), ZmanimVignette.jourHebreuAffiche(ZonedDateTime.of(d.atTime(23, 0), paris), tset))
        // Sans lieu, minuit.
        assertEquals(d, ZmanimVignette.jourHebreuAffiche(ZonedDateTime.of(d.atTime(23, 0), paris), null))
    }

    @Test fun boussoleSelonLaCarteAuChoix() {
        // New York : environ 54° par le grand cercle, 95° sur la carte de Mercator.
        val (gc, rh) = DirectionJerusalem.directions(40.7128, -74.0060)
        val (suivie, autre) = DirectionJerusalem.directions(40.7128, -74.0060, selonCarte = true)
        assertEquals(rh, suivie, 0.0)
        assertEquals(gc, autre, 0.0)
        assertTrue(gc in 50.0..58.0 && rh in 92.0..98.0)
    }
}
