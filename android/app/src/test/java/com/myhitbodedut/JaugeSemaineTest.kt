package com.myhitbodedut

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

/** La jauge de l'accueil : les jours faits, sur les jours dus. */
class JaugeSemaineTest {

    private val dimanche = LocalDate.of(2026, 9, 27)

    private fun semaine(vararg etats: String) = etats.mapIndexed { i, e ->
        WeekDay(
            date = dimanche.plusDays(i.toLong()),
            done = 'f' in e,
            exempt = 'x' in e,
            shabbat = i == 6,
            future = 'a' in e
        )
    }

    @Test
    fun `le chabbat chome ne compte pas parmi les jours dus`() {
        val p = JaugeSemaine.progression(semaine("f", "f", "", "a", "a", "a", "x"))
        assertEquals(2, p.faits)
        assertEquals(6, p.dus)
        assertEquals(2f / 6f, p.fraction, 0.0001f)
    }

    @Test
    fun `une seance faite un jour exempte compte quand meme`() {
        val p = JaugeSemaine.progression(semaine("f", "f", "f", "f", "f", "f", "fx"))
        assertEquals(7, p.faits)
        assertEquals(7, p.dus)
        assertEquals(1f, p.fraction, 0.0001f)
    }

    @Test
    fun `une semaine sans jour du reste vide`() {
        val p = JaugeSemaine.progression(semaine("x", "x", "x", "x", "x", "x", "x"))
        assertEquals(0f, p.fraction, 0f)
    }
}
