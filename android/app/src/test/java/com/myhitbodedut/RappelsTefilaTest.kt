package com.myhitbodedut

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/** Les rappels de tefila (16/10) : les dates des changements, en Israël et en diaspora. */
class RappelsTefilaTest {

    private fun rappels(d: LocalDate, israel: Boolean, avance: Int = 3) =
        RappelsTefila.prochains(d, avance) { CalendrierJuif.jour(it, israel) }

    @Test
    fun `la benediction d hiver en Israel le 7 Hechvan 5787, dimanche 18 octobre 2026`() {
        // CalJ (Jérusalem) l'annonce pour Motsaé Chabbat, à Arvit : le soir qui ouvre le 7 'Hechvan.
        val r = rappels(LocalDate.of(2026, 10, 16), israel = true)
        assertEquals(listOf(RappelsTefila.Rappel(RappelsTefila.Type.PLUIE_DEBUT, LocalDate.of(2026, 10, 18))), r)
        assertEquals(7, CalendrierJuif.jour(LocalDate.of(2026, 10, 18), true).jourHebreu)
    }

    @Test
    fun `en diaspora, rien en octobre, et le debut en decembre`() {
        assertTrue(rappels(LocalDate.of(2026, 10, 16), israel = false).none { it.type == RappelsTefila.Type.PLUIE_DEBUT })
        val dec = rappels(LocalDate.of(2026, 12, 2), israel = false, avance = 6).filter { it.type == RappelsTefila.Type.PLUIE_DEBUT }
        // Premier jour le 5 décembre : on commence à Arvit du 4 au soir.
        assertEquals(listOf(LocalDate.of(2026, 12, 5)), dec.map { it.date })
    }

    @Test
    fun `Pessah 5787 en Israel - fin de la pluie et de machiv harouah, debut de l omer`() {
        // 15 Nissan 5787 : jeudi 22 avril 2027.
        val r = rappels(LocalDate.of(2027, 4, 20), israel = true).map { it.type to it.date }.toSet()
        val p = LocalDate.of(2027, 4, 22)
        assertTrue(r.contains(RappelsTefila.Type.PLUIE_FIN to p))
        assertTrue(r.contains(RappelsTefila.Type.GUECHEM_FIN to p))
        assertTrue(r.contains(RappelsTefila.Type.OMER to p.plusDays(1)))
    }

    @Test
    fun `Roch Hachana et Chemini Atseret 5788`() {
        // 1 Tichri 5788 : samedi 2 octobre 2027 ; 22 Tichri : samedi 23 octobre 2027.
        assertTrue(rappels(LocalDate.of(2027, 9, 30), israel = false).any { it.type == RappelsTefila.Type.DIX_JOURS && it.date == LocalDate.of(2027, 10, 2) })
        assertTrue(rappels(LocalDate.of(2027, 10, 21), israel = true).any { it.type == RappelsTefila.Type.GUECHEM_DEBUT && it.date == LocalDate.of(2027, 10, 23) })
    }
}
