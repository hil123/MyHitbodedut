package com.myhitbodedut

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

/**
 * Birkat haLevana (13/10) : la fenêtre de 'Hechvan 5787, de sept jours après le
 * molad jusqu'à la mi-mois, est du 18 au 25 octobre 2026 au soir — ce qu'affiche
 * CalJ pour Jérusalem (capture du propriétaire, 14/10).
 */
class LevanaTest {

    private val zone = ZoneId.of("Asia/Jerusalem")

    @Test
    fun `Hechvan 5787 du 18 au 25 octobre`() {
        val f = Levana.fenetre(LocalDate.of(2026, 10, 14), zone)
        assertEquals(LocalDate.of(2026, 10, 18), f.premierSoir(zone))
        assertEquals(LocalDate.of(2026, 10, 25), f.dernierSoir(zone))
        assertEquals(Levana.Etat.Dans(4), Levana.etat(f, LocalDate.of(2026, 10, 14), zone))
        assertEquals(Levana.Etat.CeSoir, Levana.etat(f, LocalDate.of(2026, 10, 18), zone))
        assertEquals(Levana.Etat.DernierSoir, Levana.etat(f, LocalDate.of(2026, 10, 25), zone))
        assertTrue(Levana.part(f, f.debut) < Levana.part(f, f.fin))
    }

    @Test
    fun `la fenetre passee, celle du mois suivant`() {
        val f = Levana.fenetre(LocalDate.of(2026, 10, 26), zone)
        assertTrue(f.premierSoir(zone).isAfter(LocalDate.of(2026, 11, 1)))
        assertTrue(Levana.etat(f, LocalDate.of(2026, 10, 26), zone) is Levana.Etat.Dans)
    }

    // Revue du 14/10 sur le texte (Choulhan Aroukh O.H. 426 ; Michna Beroura 426), mêmes valeurs que le web.

    @Test
    fun `trois usages, Hechvan 5787`() {
        val trois = Levana.fenetre(LocalDate.of(2026, 10, 14), zone, Levana.Usage.TROIS_MI_MOIS)
        assertEquals(LocalDate.of(2026, 10, 14), trois.premierSoir(zone))
        assertEquals(LocalDate.of(2026, 10, 25), trois.dernierSoir(zone))
        val quinze = Levana.fenetre(LocalDate.of(2026, 10, 14), zone, Levana.Usage.SEPT_QUINZE)
        assertEquals(LocalDate.of(2026, 10, 18), quinze.premierSoir(zone))
        // Quinze jours depuis le molad : plus tard que la mi-mois (14 j 18 h 22 min).
        assertTrue(quinze.fin.isAfter(trois.fin))
        assertEquals(java.time.Duration.ofDays(15), java.time.Duration.between(quinze.molad, quinze.fin))
    }

    @Test
    fun `motsae Chabbat conseille avant le 10`() {
        val trois = Levana.fenetre(LocalDate.of(2026, 10, 14), zone, Levana.Usage.TROIS_MI_MOIS)
        // Samedi 17/10 : sa nuit est le 8 'Hechvan.
        assertEquals(LocalDate.of(2026, 10, 17), Levana.motsaeChabbat(trois, LocalDate.of(2026, 10, 14), zone))
        // Sept jours : le premier samedi (24/10) a sa nuit le 15, rien de conseillé.
        val sept = Levana.fenetre(LocalDate.of(2026, 10, 14), zone, Levana.Usage.SEPT_MI_MOIS)
        assertEquals(null, Levana.motsaeChabbat(sept, LocalDate.of(2026, 10, 14), zone))
    }

    @Test
    fun `pas avant Kippour ni Ticha beAv`() {
        val tichri = Levana.fenetre(LocalDate.of(2026, 9, 15), zone, Levana.Usage.TROIS_MI_MOIS)
        assertEquals(LocalDate.of(2026, 9, 21), tichri.premierSoir(zone)) // Kippour 5787 : lundi 21/9
        assertEquals(Levana.Restriction.KIPPOUR, tichri.restriction)
        val av = Levana.fenetre(LocalDate.of(2026, 7, 20), zone, Levana.Usage.TROIS_MI_MOIS)
        assertEquals(LocalDate.of(2026, 7, 23), av.premierSoir(zone)) // 9 Av 5786 : jeudi 23/7
        assertEquals(Levana.Restriction.TISHA_BEAV, av.restriction)
    }

    @Test
    fun `la lune nouvelle, au premier quartier, pleine`() {
        fun a(iso: String) = java.time.Instant.parse(iso)
        assertTrue(Levana.phase(a("2026-10-10T12:00:00Z")).eclairage < 0.01)
        val quartier = Levana.phase(a("2026-10-18T12:00:00Z"))
        assertTrue(quartier.eclairage in 0.4..0.6 && quartier.croissante)
        assertTrue(Levana.phase(a("2026-10-26T12:00:00Z")).eclairage > 0.99)
    }
}
