package com.myhitbodedut

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/** Les trophées (11/10). */
class TropheesTest {

    private val dimanche = LocalDate.of(2026, 10, 11)
    private fun jours(vararg decalages: Long) = decalages.associate { dimanche.plusDays(it) to 20 }

    @Test
    fun `rien sans pratique, le premier pas des le premier jour`() {
        assertTrue(Trophees.gagnes(Trophees.Pratique(emptyMap(), 0, 0)).isEmpty())
        assertEquals(setOf(Trophees.Trophee.PREMIER_PAS), Trophees.gagnes(Trophees.Pratique(jours(0), 0, 1)))
    }

    @Test
    fun `semaine pleine du dimanche au vendredi, le retour apres sept jours sans`() {
        assertTrue(Trophees.semainePleine(jours(0, 1, 2, 3, 4, 5).keys))
        assertFalse(Trophees.semainePleine(jours(1, 2, 3, 4, 5, 6).keys))
        assertFalse(Trophees.retour(jours(0, 8).keys.minus(dimanche.plusDays(8)) + dimanche.plusDays(7)))
        assertTrue(Trophees.retour(jours(0, 8).keys))
    }

    @Test
    fun `seuils`() {
        val p = Trophees.Pratique(mapOf(dimanche to 125) + (1L..17L).associate { dimanche.plusDays(it * 2) to 30 }, 10, 40)
        val g = Trophees.gagnes(p)
        listOf(Trophees.Trophee.UNE_HEURE, Trophees.Trophee.DEUX_HEURES, Trophees.Trophee.CHAI, Trophees.Trophee.QUARANTE,
            Trophees.Trophee.PLUME, Trophees.Trophee.DIX_HEURES).forEach { assertTrue("$it", it in g) }
        assertFalse(Trophees.Trophee.CENT_HEURES in g)
        assertTrue(Trophees.nouveaux(g, null).isEmpty())
        assertEquals(listOf(Trophees.Trophee.PLUME), Trophees.nouveaux(setOf(Trophees.Trophee.PLUME), setOf("chai")))
    }
}
