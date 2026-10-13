package com.myhitbodedut

import com.kosherjava.zmanim.hebrewcalendar.JewishDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.time.LocalDate

/**
 * La halakha courte du jour (13/10, tools/yalkut_yosef_construction.py) : une
 * sélection du Yalkut Yosef, jamais le livre entier, deux se'ifim par jour selon
 * la période. Les mêmes vecteurs que web/test/yalkut.test.mjs.
 */
class YalkutYosefTest {

    private val livre = YalkutYosef.parse(File("src/main/res/raw/yalkut_yosef.json").readText())

    @Test
    fun `une selection de halakhot courtes`() {
        assertEquals(
            setOf("ordinaire", "chabbat", "roch_hodech", "pessah", "omer", "bein_hametsarim", "eloul", "kippour",
                "soukkot", "hanouka", "tou_bichvat", "pourim"),
            livre.periodes.keys,
        )
        livre.periodes.forEach { (p, seifs) ->
            assertTrue(p, seifs.size >= 4)
            seifs.forEach { assertTrue("$p ${it.siman} ${it.lettre}", it.texte.isNotBlank() && it.texte.length <= 420 && it.siman.isNotBlank()) }
        }
    }

    @Test
    fun `chaque jour de trois annees a ses deux halakhot, et elles changent`() {
        var avant: String? = null
        var d = LocalDate.of(2025, 9, 1)
        while (d.isBefore(LocalDate.of(2028, 9, 1))) {
            listOf(false, true).forEach { assertEquals("$d $it", YalkutYosef.PAR_JOUR, YalkutYosef.pour(livre, d, it).size) }
            val ici = YalkutYosef.pour(livre, d, false).joinToString("|") { YalkutYosef.source(it) }
            assertNotEquals(d.toString(), avant, ici)
            avant = ici
            d = d.plusDays(1)
        }
    }

    @Test
    fun `les fenetres de fete`() {
        assertEquals("pourim", YalkutYosef.fenetre(JewishDate.ADAR, 14, false))
        assertEquals("pourim", YalkutYosef.fenetre(JewishDate.ADAR_II, 14, true))
        assertNull(YalkutYosef.fenetre(JewishDate.ADAR, 14, true))
        assertEquals("pessah", YalkutYosef.fenetre(JewishDate.NISSAN, 15, false))
        assertEquals("omer", YalkutYosef.fenetre(JewishDate.SIVAN, 6, false))
        assertEquals("bein_hametsarim", YalkutYosef.fenetre(JewishDate.AV, 9, false))
        assertEquals("eloul", YalkutYosef.fenetre(JewishDate.TISHREI, 1, false))
        assertEquals("kippour", YalkutYosef.fenetre(JewishDate.TISHREI, 10, false))
        assertEquals("soukkot", YalkutYosef.fenetre(JewishDate.TISHREI, 15, false))
        assertEquals("hanouka", YalkutYosef.fenetre(JewishDate.TEVES, 2, false))
        assertEquals("tou_bichvat", YalkutYosef.fenetre(JewishDate.SHEVAT, 15, false))
        assertNull(YalkutYosef.fenetre(JewishDate.CHESHVAN, 10, false))
        assertEquals("pessah" to 0, YalkutYosef.periode(LocalDate.of(2027, 3, 25), false)) // 16 Adar II 5787
        assertEquals("pourim", YalkutYosef.periode(LocalDate.of(2027, 3, 24), false).first)
        assertEquals("roch_hodech", YalkutYosef.periode(LocalDate.of(2026, 10, 12), false).first)
        assertEquals("chabbat", YalkutYosef.periode(LocalDate.of(2026, 10, 23), false).first)
        assertEquals("ordinaire", YalkutYosef.periode(LocalDate.of(2026, 10, 21), false).first)
        assertTrue(YalkutYosef.recherche(livre, "ציצית").isNotEmpty())
    }
}
