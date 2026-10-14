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
}
