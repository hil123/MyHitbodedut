package com.myhitbodedut

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/** Birkat haIlanot (14/10) : Nissan au nord ; Eloul et Tichri au sud (Siddur.kt, sources dans moisIlanotSud). */
class IlanotTest {

    private val jerusalem = 31.77
    private val melbourne = -37.81

    @Test
    fun `Nissan au nord, pas au sud`() {
        val nissan = LocalDate.of(2027, 4, 10) // 3 Nissan 5787
        assertTrue(SiddurMoment.ilanot(nissan, jerusalem))
        assertTrue(SiddurMoment.ilanot(nissan, null))
        assertFalse(SiddurMoment.ilanot(nissan, melbourne))
    }

    @Test
    fun `Eloul et Tichri au sud, pas au nord`() {
        val tichri = LocalDate.of(2026, 9, 20) // 9 Tichri 5787
        val eloul = LocalDate.of(2027, 9, 5)   // Eloul 5787
        val hechvan = LocalDate.of(2026, 10, 25)
        assertTrue(SiddurMoment.ilanot(tichri, melbourne))
        assertTrue(SiddurMoment.ilanot(eloul, melbourne))
        assertFalse(SiddurMoment.ilanot(hechvan, melbourne))
        assertFalse(SiddurMoment.ilanot(tichri, jerusalem))
    }
}
