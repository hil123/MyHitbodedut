package com.myhitbodedut

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/** Les dates de gain, les trophées proches et le bilan de la semaine (16/10). */
class BilanTropheesTest {

    private val d0 = LocalDate.of(2026, 9, 6) // un dimanche

    private fun jours(n: Int, min: Int = 15, depuis: LocalDate = d0) =
        (0 until n).associate { depuis.plusDays(it.toLong()) to min }

    @Test
    fun `chaque niveau est date du jour ou le seuil est atteint`() {
        val m = jours(10)
        val dates = Trophees.datesGain(m, emptySet()) { false }
        assertEquals(d0, dates["premier_pas:1"])
        assertEquals(d0.plusDays(2), dates["premier_pas:3"])
        assertEquals(d0.plusDays(6), dates["jours:1"]) // 7 jours de pratique
        assertEquals(d0.plusDays(2), dates["serie:1"]) // 3 jours d'affilée
        // Tout ce qui est gagné a une date, rien de ce qui ne l'est pas.
        val p = Trophees.Pratique(m, 0, 10)
        assertEquals(Trophees.gagnes(p).map { it.cle }.toSet(), dates.keys)
    }

    @Test
    fun `les notes datent la plume`() {
        val dates = Trophees.datesGain(jours(2), setOf(d0.plusDays(5))) { false }
        assertEquals(d0.plusDays(5), dates["plume:1"])
    }

    @Test
    fun `un trophee proche est a un dixieme ou a une unite`() {
        // 6 jours d'affilée : « 7 jours d'affilée » est à un jour.
        val p = Trophees.Pratique(jours(6), 0, 6)
        val n = Trophees.proche(p)
        assertTrue(n != null && n.seuil - Trophees.valeur(p, n.famille) == 1)
        // Rien n'est proche au tout début.
        assertNull(Trophees.proche(Trophees.Pratique(emptyMap(), 0, 0))?.takeIf { it.seuil > 1 })
    }

    @Test
    fun `les plus proches sont a gagner et du plus avance au moins avance`() {
        val p = Trophees.Pratique(jours(8), 0, 8)
        val l = Trophees.plusProches(p)
        assertEquals(3, l.size)
        assertTrue(l.none { Trophees.gagne(p, it) })
        val f = l.map { Trophees.fraction(p, it) }
        assertEquals(f.sortedDescending(), f)
    }

    @Test
    fun `le bilan de la semaine ecoulee`() {
        val m = jours(3, 20, d0.minusDays(7)) + jours(4, 30) // deux semaines
        val dates = Trophees.datesGain(m, emptySet()) { false }
        val b = BilanSemaine.precedente(d0.plusDays(8), m, dates)!! // le lundi suivant
        assertEquals(d0, b.debut)
        assertEquals(120, b.minutes)
        assertEquals(4, b.jours)
        assertEquals(60, b.precedente)
        assertEquals(60, b.ecart)
        assertTrue(b.record)
        assertTrue(b.trophees.all { dates[it.cle]!! >= d0 })
    }

    @Test
    fun `une semaine vide ne fait pas de bilan, et la premiere n est pas un record`() {
        assertNull(BilanSemaine.precedente(d0.plusDays(30), jours(3), emptyMap()))
        val b = BilanSemaine.precedente(d0.plusDays(7), jours(3), emptyMap())!!
        assertFalse(b.record)
        assertEquals(0, b.precedente)
    }

    @Test
    fun `le rejeu incremental rend les memes niveaux que le calcul complet`() {
        val exempt = { d: LocalDate -> d.dayOfWeek == java.time.DayOfWeek.SATURDAY }
        val m = mutableMapOf<LocalDate, Int>()
        val notes = mutableSetOf<LocalDate>()
        for (i in 0 until 400) {
            val d = LocalDate.of(2025, 3, 1).plusDays(i.toLong())
            if (!exempt(d) && (i * 37) % 11 != 3 && i !in 121..131) m[d] = 5 + (i * 13) % 70
            if (i % 9 == 0) notes += d
        }
        val dates = Trophees.datesGain(m, notes, exempt)
        val p = Trophees.Pratique(m, notes.size, Plaque.meilleureSerie(m.keys, exempt))
        assertEquals(Trophees.gagnes(p).map { it.cle }.toSet(), dates.keys)
        dates.forEach { (cle, d) ->
            val n = Trophees.chemin.first { it.cle == cle }
            val avant = m.filterKeys { it.isBefore(d) }
            val pAvant = Trophees.Pratique(avant, notes.count { it.isBefore(d) }, Plaque.meilleureSerie(avant.keys, exempt))
            assertFalse("$cle déjà gagné avant $d", Trophees.gagne(pAvant, n))
        }
    }
}
