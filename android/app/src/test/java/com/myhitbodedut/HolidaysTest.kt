package com.myhitbodedut

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * Jours chômés de Tichri 5787, en septembre et octobre 2026.
 *
 * Attendu : 21/09 Yom Kippour, 26/09 premier jour de Souccot tombant un
 * samedi, 27/09 deuxième jour de fête en diaspora seulement, 03/10 Chemini
 * Atseret un samedi, 04/10 Simhat Torah en diaspora seulement, et 28/09 Hol
 * Hamoed, jour ordinaire partout.
 */
class HolidaysTest {

    private fun d(month: Int, day: Int) = LocalDate.of(2026, month, day)

    @Test
    fun `diaspora, cinq jours sans porte`() {
        listOf(d(9, 21), d(9, 26), d(9, 27), d(10, 3), d(10, 4)).forEach {
            assertTrue("$it devrait être chômé en diaspora", Holidays.isRestDay(it, inIsrael = false))
        }
    }

    @Test
    fun `israel, trois jours sans porte`() {
        listOf(d(9, 21), d(9, 26), d(10, 3)).forEach {
            assertTrue("$it devrait être chômé en Israël", Holidays.isRestDay(it, inIsrael = true))
        }
    }

    @Test
    fun `le deuxieme jour de fete ne vaut qu en diaspora`() {
        listOf(d(9, 27), d(10, 4)).forEach {
            assertFalse("$it n'est pas chômé en Israël", Holidays.isRestDay(it, inIsrael = true))
            assertTrue("$it est chômé en diaspora", Holidays.isRestDay(it, inIsrael = false))
        }
    }

    @Test
    fun `le 28 septembre est ordinaire partout`() {
        assertFalse(Holidays.isRestDay(d(9, 28), inIsrael = true))
        assertFalse(Holidays.isRestDay(d(9, 28), inIsrael = false))
    }

    @Test
    fun `un samedi ordinaire reste chome`() {
        assertTrue(Holidays.isRestDay(d(9, 19), inIsrael = true))
        assertTrue(Holidays.isRestDay(d(10, 10), inIsrael = false))
    }

    @Test
    fun `un jour de semaine ordinaire ne l est pas`() {
        assertFalse(Holidays.isRestDay(d(9, 22), inIsrael = true))
        assertFalse(Holidays.isRestDay(d(9, 22), inIsrael = false))
    }

    @Test
    fun `la veille est reconnue`() {
        assertTrue(Holidays.isEveOfRestDay(d(9, 25), inIsrael = true))
        assertFalse(Holidays.isEveOfRestDay(d(9, 22), inIsrael = true))
        // Veille du deuxième jour de fête : vraie en diaspora seulement.
        assertTrue(Holidays.isEveOfRestDay(d(9, 26), inIsrael = false))
    }

    @Test
    fun `la region par defaut suit le fuseau`() {
        assertEquals(true, Holidays.defaultInIsrael("Asia/Jerusalem"))
        assertEquals(false, Holidays.defaultInIsrael("Europe/Paris"))
        assertEquals(false, Holidays.defaultInIsrael("America/New_York"))
    }
}
