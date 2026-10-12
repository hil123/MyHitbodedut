package com.myhitbodedut

import com.kosherjava.zmanim.hebrewcalendar.JewishCalendar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.time.LocalDate

/** La hilloula du jour et l'illustration de la carte du jour (11/10). */
class HilloulotTest {

    private val table = Hilloulot.parse(File("src/main/res/raw/hilloulot.json").readText())

    @Test
    fun `le cycle de Meton suit KosherJava`() {
        (5700..5900).forEach { a ->
            assertEquals("$a", JewishCalendar(a, JewishCalendar.TISHREI, 1).isJewishLeapYear, Hilloulot.embolismique(a))
        }
    }

    @Test
    fun `adar sans precision se dit en adar II, un 30 absent le 29`() {
        assertEquals(listOf("adar-7", "adar1-7", "adar2-7"), Hilloulot.cles(12, 7, embolismique = false, dernierJour = false))
        assertEquals(listOf("adar1-7"), Hilloulot.cles(12, 7, embolismique = true, dernierJour = false))
        assertEquals(listOf("adar-7", "adar2-7"), Hilloulot.cles(13, 7, embolismique = true, dernierJour = false))
        assertEquals(listOf("8-29", "8-30"), Hilloulot.cles(8, 29, embolismique = false, dernierJour = true))
    }

    @Test
    fun `chaque jour d une annee simple a sa hilloula`() {
        var d = LocalDate.of(2025, 9, 23)
        while (d.isBefore(LocalDate.of(2026, 9, 12))) {
            assertTrue("$d", Hilloulot.pour(table, d, false).isNotEmpty())
            d = d.plusDays(1)
        }
        // 30 Tichri 5787 : Rabbi Tsvi Hirsch Chajes (Wikipédia).
        assertTrue(Hilloulot.pour(table, LocalDate.of(2026, 10, 11), false).any { it.he.contains("צבי הירש חיות") })
        assertTrue(table.keys.all { Regex("""(\d{1,2}|adar[12]?)-\d{1,2}""").matches(it) })
    }

    @Test
    fun `les liens de la carte du jour`() {
        assertEquals("https://hyomi.org.il/day.php?month1=0&dd=11&mm=10&yy=2026", Hilloulot.lienHyomi(LocalDate.of(2026, 10, 11)))
        assertEquals("https://halachayomit.co.il/he/default.aspx", Hilloulot.lienHalakha("iw"))
        assertEquals("https://halachayomit.co.il/en/default.aspx", Hilloulot.lienHalakha("en"))
        assertEquals("https://halachayomit.co.il/fr/default.aspx", Hilloulot.lienHalakha("fr"))
    }

    @Test
    fun `l illustration du jour de fete ou de chabbat`() {
        assertEquals("scene_sukkot", JourAffiche.illustration(listOf("soukkot"), LocalDate.of(2026, 9, 26)))
        assertEquals("char_matsa", JourAffiche.illustration(listOf("pessah"), LocalDate.of(2027, 4, 22)))
        assertEquals("char_shabbat", JourAffiche.illustration(emptyList(), LocalDate.of(2026, 10, 10)))
        assertEquals(null, JourAffiche.illustration(emptyList(), LocalDate.of(2026, 10, 11)))
    }

    @Test
    fun `date lisible et recherche par nom`() {
        assertEquals("ל׳ תשרי", Hilloulot.dateDe("7-30"))
        assertEquals("ז׳ אדר ב׳", Hilloulot.dateDe("adar2-7"))
        assertTrue(Hilloulot.recherche(table, "צבי הירש חיות").any { it.first == "7-30" })
        assertTrue(Hilloulot.recherche(table, "א").isEmpty())
    }

    @Test
    fun `les biographies d autres sources, a part, avec leur source`() {
        val complete = Hilloulot.avecBios(table, File("src/main/res/raw/hilloulot_bios.json").readText())
        assertEquals(table.keys, complete.keys)
        table.forEach { (cle, liste) -> assertEquals(cle, liste.map { it.he }, complete.getValue(cle).map { it.he }) }
        val bios = complete.values.flatten().flatMap { it.bios }
        assertTrue(bios.size > 2000)
        assertTrue(bios.all { it.source in setOf("m", "l") && it.texte.isNotBlank() })
        // La source est dite : chaque biographie de MyTzadik ou de המאיר לארץ garde le lien vers sa page.
        assertTrue(bios.all { it.lien?.startsWith("https://") == true })
        assertTrue(table.values.flatten().all { it.bios.isEmpty() })
    }
}
