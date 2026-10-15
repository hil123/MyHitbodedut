package com.myhitbodedut

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/** Le chemin des trophées (15/10) : une seule liste, du plus facile au plus dur, et le prochain. */
class CheminTropheesTest {

    private fun pratique(jours: Int, minutes: Int = 15, notes: Int = 0, serie: Int = jours) = Trophees.Pratique(
        (0 until jours).associate { LocalDate.of(2026, 9, 1).plusDays(it.toLong()) to minutes }, notes, serie)

    @Test
    fun `tous les niveaux une seule fois, du plus facile au plus dur`() {
        val c = Trophees.chemin
        assertEquals(Trophees.Famille.entries.sumOf { it.seuils.size }, c.size)
        assertEquals(c.size, c.map { it.cle }.toSet().size)
        assertEquals(Trophees.Niveau(Trophees.Famille.PREMIER_PAS, 1), c.first())
        val couts = c.map { Trophees.cout(it.famille, it.seuil) }
        assertEquals(couts.sorted(), couts)
    }

    @Test
    fun `les gagnes sont ceux des niveaux, et le prochain n est pas gagne`() {
        val p = pratique(11, notes = 15)
        assertEquals(Trophees.gagnes(p), Trophees.chemin.filter { Trophees.gagne(p, it) }.toSet())
        val n = Trophees.prochain(p)!!
        assertTrue(!Trophees.gagne(p, n))
    }

    @Test
    fun `un materiau par grade`() {
        assertEquals(Trophees.Metal.BRONZE, Trophees.grade(0))
        assertEquals(Trophees.Metal.BRONZE, Trophees.grade(9))
        assertEquals(Trophees.Metal.ARGENT, Trophees.grade(10))
        assertEquals(Trophees.Metal.OR, Trophees.grade(25))
        assertEquals(Trophees.Metal.PLATINE, Trophees.grade(60))
    }

    @Test
    fun `sans pratique, le prochain est la premiere hitbodedout`() {
        assertEquals(Trophees.Niveau(Trophees.Famille.PREMIER_PAS, 1), Trophees.prochain(pratique(0)))
    }

    @Test
    fun `le prochain est le plus proche`() {
        // 11 jours d'affilée : la série de 15 est à 73 % ; 15 jours de pratique aussi, mais plus loin au chemin.
        val n = Trophees.prochain(pratique(11, notes = 0))!!
        assertTrue(Trophees.fraction(pratique(11), n) >= 0.7f)
    }
}
