package com.myhitbodedut

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/** Les trophées à niveaux (12/10) : valeurs, rangs, métaux, progression, nouveautés. */
class TropheesTest {

    private val dimanche = LocalDate.of(2026, 10, 4)
    private fun jours(n: Int, pas: Long = 1, minutes: Int = 20) = (0 until n).associate { dimanche.plusDays(it * pas) to minutes }

    @Test
    fun `rien sans pratique, le premier pas des le premier jour`() {
        assertTrue(Trophees.gagnes(Trophees.Pratique(emptyMap(), 0, 0)).isEmpty())
        val un = Trophees.gagnes(Trophees.Pratique(jours(1), 0, 1))
        assertTrue(Trophees.Niveau(Trophees.Famille.PREMIER_PAS, 1) in un)
    }

    @Test
    fun `chaque niveau atteint est gagne, et les rangs suivent les seuils`() {
        val p = Trophees.Pratique(jours(41) + (dimanche.plusDays(60) to 130), 26, 41)
        assertEquals(7, Trophees.rang(p, Trophees.Famille.JOURS))        // 42 jours : 7, 10, 15, 18, 20, 30, 40
        assertEquals(9, Trophees.rang(p, Trophees.Famille.SERIE))        // 41 : 3, 5, 7, 10, 15, 18, 20, 30, 40
        assertEquals(12, Trophees.rang(p, Trophees.Famille.SEANCE_LONGUE)) // 130 min : de 10 à 120
        assertEquals(3, Trophees.rang(p, Trophees.Famille.PLUME))        // 26 notes : 1, 10, 25
        assertEquals(1, Trophees.rang(p, Trophees.Famille.RETOUR))
        assertEquals(6, Trophees.semainesPleines(jours(41).keys)) // du 4/10 au vendredi 13/11
        val g = Trophees.gagnes(p)
        assertTrue((1..7).all { Trophees.Niveau(Trophees.Famille.JOURS, it) in g })
        assertEquals(5, Trophees.rang(p, Trophees.Famille.PREMIER_PAS))  // 42 jours : les cinq premiers
    }

    @Test
    fun `les metaux montent de bronze a platine, sans sauter`() {
        Trophees.Famille.entries.filter { it.seuils.size > 1 }.forEach { f ->
            val m = (1..f.seuils.size).map { Trophees.metal(f, it) }
            assertEquals(f.name, Trophees.Metal.BRONZE, m.first())
            assertEquals(f.name, Trophees.Metal.PLATINE, m.last())
            assertTrue(f.name, m.zipWithNext().all { (a, b) -> b.ordinal - a.ordinal in 0..1 })
            assertTrue(f.name, f.seuils.toList().zipWithNext().all { (a, b) -> b > a })
        }
        assertEquals(Trophees.Metal.BRONZE, Trophees.metal(Trophees.Famille.PREMIER_PAS, 1))
    }

    @Test
    fun `la progression vise le niveau suivant, et le sommet reste au dernier seuil`() {
        listOf(
            Trophees.Pratique(emptyMap(), 0, 0),
            Trophees.Pratique(jours(6, minutes = 70), 12, 6),
            Trophees.Pratique(jours(700, 1, 200), 200, 700),
        ).forEach { p ->
            Trophees.Famille.entries.forEach { f ->
                val r = Trophees.rang(p, f)
                val pr = Trophees.progres(p, f)
                if (r < f.seuils.size) { assertEquals(f.seuils[r], pr.cible); assertTrue(pr.valeur < pr.cible) }
                else assertEquals(f.seuils.last(), pr.cible)
                assertTrue(pr.fraction in 0f..1f)
            }
        }
    }

    @Test
    fun `les nouveautes - le plus haut de chaque famille, rien la premiere fois ni depuis l'ancien format`() {
        val g = Trophees.gagnes(Trophees.Pratique(jours(20), 0, 20))
        assertTrue(Trophees.nouveaux(g, null).isEmpty())
        assertTrue(Trophees.nouveaux(g, setOf("chai", "premier_pas")).isEmpty())
        val n = Trophees.nouveaux(g, setOf("premier_pas:1"))
        assertEquals(n.map { it.famille }.toSet().size, n.size)
        assertTrue(n.any { it.famille == Trophees.Famille.JOURS && it.rang == 5 })
    }
}
