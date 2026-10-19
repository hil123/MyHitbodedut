package com.myhitbodedut

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** « Mon parcours » (proposition 6, 19/10) : les chiffres, les sujets fréquents, l'humeur. */
class ParcoursTest {

    @Test
    fun `le bilan compte les jours faits, le temps et les sujets`() {
        val jours = listOf(
            DayRecord("2026-10-01", 20, "", listOf("famille", "parnassa")),
            DayRecord("2026-10-02", 10, "", listOf("famille")),
            DayRecord("2026-10-03", 0, "une note", emptyList(), noteSeule = true),
        )
        val b = Parcours.bilan(jours)
        assertEquals(2, b.jours)
        assertEquals(30, b.minutes)
        assertEquals(15, b.moyenne)
        assertEquals(listOf(Theme.FAMILLE to 2, Theme.PARNASSA to 1), b.sujets)
        assertEquals(0, Parcours.bilan(emptyList()).moyenne)
    }

    @Test
    fun `l humeur moyenne ne compte que les jours notes avant et apres`() {
        assertNull(Parcours.humeur(listOf(JournalPlus.Plus(avant = 2))))
        assertEquals(Triple(2, 4, 2), Parcours.humeur(listOf(JournalPlus.Plus(2, 4), JournalPlus.Plus(2, 4), JournalPlus.Plus(apres = 5))))
    }

    @Test
    fun `l accueil ouvre la page, dans les deux orientations`() {
        listOf("layout", "layout-land").forEach {
            assertTrue(it, File("src/main/res/$it/activity_main.xml").readText().contains("@+id/parcours_button"))
        }
    }
}
