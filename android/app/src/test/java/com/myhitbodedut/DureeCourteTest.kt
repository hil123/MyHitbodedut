package com.myhitbodedut

import org.junit.Assert.assertEquals
import org.junit.Test

/** La durée à la place du ✓ (14/10) : tient dans la case d'un jour. */
class DureeCourteTest {
    @Test
    fun formats() {
        assertEquals("5′", JaugeSemaine.dureeCourte(5))
        assertEquals("59′", JaugeSemaine.dureeCourte(59))
        assertEquals("1h", JaugeSemaine.dureeCourte(60))
        assertEquals("1h05", JaugeSemaine.dureeCourte(65))
        assertEquals("2h30", JaugeSemaine.dureeCourte(150))
    }
}
