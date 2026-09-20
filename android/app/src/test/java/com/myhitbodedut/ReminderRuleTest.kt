package com.myhitbodedut

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Le calendrier raisonne en date civile, la fête commence au coucher du soleil.
 * Sans ville réglée, on ne calcule aucun coucher : la veille d'un jour chômé,
 * plus aucun rappel après midi.
 */
class ReminderRuleTest {

    private fun h(hour: Int) = hour * 60

    @Test
    fun `jour ordinaire, le rappel passe`() {
        assertTrue(
            ReminderRule.shouldNotify(
                nowMinutes = h(19), doneToday = false,
                todayIsRestDay = false, tomorrowIsRestDay = false, sunsetMinutes = null
            )
        )
    }

    @Test
    fun `pratique deja faite, pas de rappel`() {
        assertFalse(
            ReminderRule.shouldNotify(h(7), doneToday = true, todayIsRestDay = false,
                tomorrowIsRestDay = false, sunsetMinutes = null)
        )
    }

    @Test
    fun `jour chome, pas de rappel`() {
        assertFalse(
            ReminderRule.shouldNotify(h(7), doneToday = false, todayIsRestDay = true,
                tomorrowIsRestDay = false, sunsetMinutes = null)
        )
    }

    @Test
    fun `veille de fete sans ville, rien apres midi`() {
        assertTrue(
            ReminderRule.shouldNotify(h(11), doneToday = false, todayIsRestDay = false,
                tomorrowIsRestDay = true, sunsetMinutes = null)
        )
        assertFalse(
            ReminderRule.shouldNotify(h(12), doneToday = false, todayIsRestDay = false,
                tomorrowIsRestDay = true, sunsetMinutes = null)
        )
        assertFalse(
            ReminderRule.shouldNotify(h(19), doneToday = false, todayIsRestDay = false,
                tomorrowIsRestDay = true, sunsetMinutes = null)
        )
    }

    @Test
    fun `veille de fete avec ville, le coucher du soleil fait foi`() {
        val sunset = 18 * 60 + 40
        assertTrue(
            ReminderRule.shouldNotify(h(17), doneToday = false, todayIsRestDay = false,
                tomorrowIsRestDay = true, sunsetMinutes = sunset)
        )
        assertFalse(
            ReminderRule.shouldNotify(sunset, doneToday = false, todayIsRestDay = false,
                tomorrowIsRestDay = true, sunsetMinutes = sunset)
        )
    }
}
