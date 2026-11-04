package com.myhitbodedut

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/** Les trophées du Tikoun HaKlali (04/11) : lectures complètes et jours d'affilée. */
class TropheesTikkunTest {

    private val d0 = LocalDate.of(2026, 11, 1)
    private fun jours(vararg n: Long) = n.map { d0.plusDays(it) }.toSet()

    @Test
    fun `les lectures completes et la plus longue suite`() {
        val p = Trophees.Pratique(emptyMap(), 0, 0, jours(0, 1, 2, 5, 6, 9))
        assertEquals(6, Trophees.valeur(p, Trophees.Famille.TIKKUN))
        assertEquals(3, Trophees.valeur(p, Trophees.Famille.TIKKUN_SERIE))
        assertTrue(Trophees.gagnes(p).any { it.cle == "tikkun:2" })
        assertTrue(Trophees.gagnes(p).any { it.cle == "tikkun_serie:1" })
    }

    @Test
    fun `sans Tikoun, aucun de ses trophees`() {
        val p = Trophees.Pratique(mapOf(d0 to 20), 0, 1)
        assertTrue(Trophees.gagnes(p).none { it.famille == Trophees.Famille.TIKKUN || it.famille == Trophees.Famille.TIKKUN_SERIE })
    }

    @Test
    fun `les dates de gain suivent le journal du Tikoun`() {
        val dates = Trophees.datesGain(emptyMap(), emptySet(), jours(0, 1, 2)) { false }
        assertEquals(d0, dates["tikkun:1"])
        assertEquals(d0.plusDays(2), dates["tikkun:2"])
        assertEquals(d0.plusDays(2), dates["tikkun_serie:1"])
    }
}
