package com.myhitbodedut

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * La Méguila d'Esther dans le siddour (13/10, SiddurMoment.megila) : sous Arvit
 * la nuit qui ouvre Pourim, sous Cha'harit le jour ; le 15 Adar dans une ville
 * murée, le vendredi 14 quand le 15 est un Chabbat (Pourim mechoulach, 5785).
 */
class MegilaTest {

    private fun megila(moment: String, date: String, muree: Boolean) =
        SiddurMoment.megila(moment, LocalDate.parse(date), false, muree)

    @Test
    fun `ville ouverte, 14 Adar II 5787`() {
        assertTrue(megila("arvit", "2027-03-22", false))
        assertTrue(megila("chaharit", "2027-03-23", false))
        assertFalse(megila("minha", "2027-03-23", false))
        assertFalse(megila("chaharit", "2027-03-24", false))
        assertFalse(megila("arvit", "2027-03-23", false))
    }

    @Test
    fun `ville muree, 15 Adar II 5787`() {
        assertFalse(megila("chaharit", "2027-03-23", true))
        assertTrue(megila("arvit", "2027-03-23", true))
        assertTrue(megila("chaharit", "2027-03-24", true))
    }

    @Test
    fun `Pourim mechoulach 5785, ville muree le vendredi 14`() {
        assertTrue(megila("arvit", "2025-03-13", true))
        assertTrue(megila("chaharit", "2025-03-14", true))
        assertFalse(megila("chaharit", "2025-03-15", true))
        assertTrue(megila("chaharit", "2025-03-14", false))
    }
}
