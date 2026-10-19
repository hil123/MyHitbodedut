package com.myhitbodedut

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

/**
 * Le journal en carnet spirituel (proposition 5, 19/10) : le sujet du jour,
 * l'humeur avant et après, une gratitude, et une chose à travailler.
 */
class JournalCarnetTest {

    @Test
    fun `le resume dit le sujet, l humeur, la gratitude et la chose a travailler`() {
        assertEquals(
            "Sujet : Famille\n🙁 → 🙂\nG : merci\nT : la patience",
            JournalPlus.resume(JournalPlus.Plus(2, 4, "merci", "la patience"), "G", "T", "Sujet : Famille"),
        )
        assertEquals("T : écouter", JournalPlus.resume(JournalPlus.Plus(travail = "écouter"), "G", "T"))
        // L'ancien appel, sans travail ni sujet, rend ce qu'il rendait.
        assertEquals("🙁 → 🙂\nG : merci", JournalPlus.resume(JournalPlus.Plus(2, 4, "merci"), "G"))
    }

    @Test
    fun `une chose a travailler suffit a ce que le jour ne soit pas vide`() {
        assertFalse(JournalPlus.Plus(travail = "x").vide)
    }
}
