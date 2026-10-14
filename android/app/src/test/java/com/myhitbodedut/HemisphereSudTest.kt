package com.myhitbodedut

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * Hémisphère sud, Edot (14/10) : Yalkut Yosef, Kitsour Choulhan Aroukh, O.H. 117:20 —
 * « מוריד הטל » toute l'année ; ותן טל ומטר comme la diaspora. Les autres noussa'h ne changent pas.
 */
class HemisphereSudTest {
    private val C = CalendrierJuif.C

    @Test
    fun `Edot au sud, Morid hatal en hiver, Tal ou-matar inchange`() {
        val hiver = CalendrierJuif.jour(LocalDate.of(2027, 1, 10), false).conditions
        assertTrue(C.MACHIV_HAROUAH in hiver && C.TAL_OUMATAR in hiver)
        val sud = SiddurJour.conditionsPour("edot", false, hiver, true, sud = true)
        assertFalse(C.MACHIV_HAROUAH in sud)
        assertTrue(C.MORID_HATAL in sud && C.TAL_OUMATAR in sud)
        assertTrue("ajout_morid_hatal" in JourAffiche.ajouts(hiver, "edot", false, true, sud = true))
        assertTrue(C.MACHIV_HAROUAH in SiddurJour.conditionsPour("edot", false, hiver, true, sud = false))
        assertTrue(C.MACHIV_HAROUAH in SiddurJour.conditionsPour("ashkenaz", false, hiver, false, sud = true))
        assertTrue(C.MACHIV_HAROUAH in SiddurJour.conditionsPour("sefard", false, hiver, false, sud = true))
    }
}
