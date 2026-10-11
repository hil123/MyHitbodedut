package com.myhitbodedut

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.time.LocalDate

/** La halakha du jour hors ligne : le Ben Ish 'Haï, par paracha (11/10). */
class HalakhaTest {

    private val livre = Halakha.parse(File("src/main/res/raw/ben_ish_hai.json").readText())

    @Test
    fun `le livre embarque est complet`() {
        assertEquals(2002, livre.annees.values.sumOf { a -> a.values.sumOf { it.halakhot.size } } + (livre.hanouka?.halakhot?.size ?: 0))
        assertEquals("בראשית", livre.annees["1"]!!["BERESHIS"]!!.he)
    }

    @Test
    fun `chaque jour de trois annees a sa halakha, la semaine couvre toute la section`() {
        var d = LocalDate.of(2025, 9, 1)
        while (d.isBefore(LocalDate.of(2028, 9, 1))) {
            listOf(false, true).forEach { il -> assertTrue("$d $il", Halakha.pour(livre, d, il).isNotEmpty()) }
            d = d.plusDays(1)
        }
        // Semaine de Lekh Lekha 5787 (dimanche 18 → Chabbat 24/10/2026), première année : les 21 halakhot, une fois chacune.
        val semaine = (0L..6L).flatMap { Halakha.pour(livre, LocalDate.of(2026, 10, 18).plusDays(it), false) }
        assertEquals((1..21).toList(), semaine.map { it.numero })
        assertTrue(semaine.all { it.annee == 1 && it.section == "לך לך" })
    }

    @Test
    fun `parachiot jointes ou separees, Hanouka`() {
        val un = livre.annees["1"]!!
        // Tazria lue seule : la première moitié de la section jointe de la première année.
        val t = Halakha.sections(un, "TAZRIA")!!.single()
        assertEquals(0, t.second)
        val m = Halakha.sections(un, "METZORA")!!.single()
        assertEquals(t.third.size, m.second)
        // Jointes, la deuxième année les a séparées : les deux à la suite.
        assertEquals(2, Halakha.sections(livre.annees["2"]!!, "TAZRIA_METZORA")!!.size)
        // Le premier jour de 'Hanouka : la section de 'Hanouka.
        val h = Halakha.choix(livre, "VAYESHEV", 0, 5787, jourHanouka = 1)
        assertEquals("הלכות חנוכה", h.first().section)
        assertEquals(1, h.first().numero)
    }

    @Test
    fun `annee du livre et part du jour`() {
        assertEquals(1, Halakha.anneeDuLivre(5787))
        assertEquals(2, Halakha.anneeDuLivre(5788))
        assertEquals(listOf(3), Halakha.part(listOf(1, 2, 3), 2, 7))
        assertEquals(listOf(1), Halakha.part(listOf(1, 2, 3), 3, 7))
        assertEquals((0 until 14).toList(), (0 until 7).flatMap { Halakha.part((0 until 14).toList(), it, 7) })
    }

    @Test
    fun `la recherche trouve sans voyelles`() {
        assertTrue(Halakha.recherche(livre, "ציצית").size > 10)
        assertTrue(Halakha.recherche(livre, "צ").isEmpty())
    }
}
