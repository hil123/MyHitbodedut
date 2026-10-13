package com.myhitbodedut

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

/** « Chabbat dans 3 h » (13/10) : trois heures avant l'allumage du vendredi qui vient. */
class ChabbatAlerteTest {

    private val zone = ZoneId.of("Asia/Jerusalem")
    private fun allumage(d: LocalDate) = d.atTime(17, 45).atZone(zone)
    private fun le(y: Int, m: Int, j: Int, h: Int, min: Int) = ZonedDateTime.of(y, m, j, h, min, 0, 0, zone)

    @Test
    fun `le mardi, l alerte est vendredi trois heures avant l allumage`() {
        assertEquals(le(2026, 10, 16, 14, 45), ChabbatAlerte.prochaine(le(2026, 10, 13, 9, 0), ::allumage))
    }

    @Test
    fun `le vendredi avant l alerte c est ce vendredi, apres le suivant`() {
        assertEquals(le(2026, 10, 16, 14, 45), ChabbatAlerte.prochaine(le(2026, 10, 16, 10, 0), ::allumage))
        assertEquals(le(2026, 10, 23, 14, 45), ChabbatAlerte.prochaine(le(2026, 10, 16, 15, 0), ::allumage))
    }

    @Test
    fun `sans heure d allumage, rien n est programme`() {
        assertNull(ChabbatAlerte.prochaine(le(2026, 10, 13, 9, 0)) { null })
    }

    @Test
    fun `le message est dans les trois langues`() {
        listOf("values", "values-en", "values-iw").forEach { d ->
            val xml = java.io.File("src/main/res/$d/strings.xml").readText()
            listOf("chabbat_alerte_titre", "chabbat_alerte_texte").forEach {
                assert(xml.contains("name=\"$it\"")) { "$d : $it" }
            }
        }
    }
}
