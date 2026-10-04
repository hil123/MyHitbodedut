package com.myhitbodedut

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.text.Normalizer
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * Le livre de Tehilim entier (demande du 04/10) : le texte, ses
 * conventions, et les deux cycles qui le disent en un mois ou en une semaine.
 */
class TehilimTest {

    private val livre = DataFiles.tehilim
    private fun nfc(s: String) = Normalizer.normalize(s, Normalizer.Form.NFC)
    private fun lettres(s: String) = s.filter { it in 'א'..'ת' }

    @Test
    fun `le livre a ses 150 psaumes et ses 2527 versets`() {
        assertEquals(150, livre.psaumes.size)
        assertEquals(2527, livre.versetsTotal)
        assertEquals(176, livre.versets(119).size)
        assertEquals(2, livre.versets(117).size)
        assertEquals(6, livre.versets(150).size)
    }

    @Test
    fun `chaque verset est lu et non ecrit`() {
        livre.psaumes.forEachIndexed { i, versets ->
            versets.forEachIndexed { j, v ->
                val ou = "Tehilim ${i + 1}:${j + 1}"
                assertTrue("$ou ne finit pas par un sof pasouq", v.trimEnd().endsWith("׃"))
                assertFalse("$ou garde un crochet de qere", v.contains('[') || v.contains(']'))
                assertFalse("$ou garde le Nom en toutes lettres", lettres(v).contains("יהוה"))
                assertFalse("$ou garde un caractère invisible", v.contains('‍'))
                // Un mot sans aucune voyelle serait un ketiv resté en place.
                v.split(' ', '־').filter { lettres(it).length >= 2 && it != "ה'" }.forEach { mot ->
                    assertTrue("$ou : « $mot » sans voyelles", mot.any { it in 'ְ'..'ּ' || it == 'ׁ' || it == 'ׂ' })
                }
            }
        }
    }

    /** Les dix psaumes du Tikoun et le livre entier disent exactement le même texte. */
    @Test
    fun `le tikoun et le livre concordent verset par verset`() {
        val tikkun = DataFiles.tikkun
        var compares = 0
        tikkun.psaumes.forEach { p ->
            p.versets.forEach { v ->
                assertEquals("Tehilim ${p.numero}:${v.numero}", nfc(livre.versets(p.numero)[v.numero - 1]), nfc(v.he))
                compares++
            }
        }
        assertEquals(164, compares)
    }

    /** Les corrections du 04/10 dans le Tikoun : les mots que la résolution du qere avait perdus. */
    @Test
    fun `le qere ne fait plus perdre de mot`() {
        assertTrue(lettres(livre.versets(77)[0]).startsWith("למנצחעלידותון"))
        assertTrue(lettres(livre.versets(105)[27]).endsWith("אתדברו"))
        assertTrue(lettres(livre.versets(21)[1]).contains("מהיגל"))
        assertTrue(livre.versets(42)[6].startsWith(nfc("אֱלֹקַי")))
    }

    @Test
    fun `le cycle du mois couvre tout le livre en trente jours`() {
        assertEquals(30, CycleTehilim.MOIS.size)
        var attendu = 1
        CycleTehilim.MOIS.filterNot { it.partielle }.forEach {
            if (it.debut == 120) attendu = 120 // 119 est coupé en deux portions
            assertEquals("portion qui commence à ${it.debut}", attendu, it.debut)
            attendu = it.fin + 1
        }
        assertEquals(151, attendu)
        val coupe = CycleTehilim.MOIS.filter { it.partielle }
        assertEquals(listOf(Portion(119, 119, 1, 96), Portion(119, 119, 97, 176)), coupe)
        assertEquals(Portion(1, 9), CycleTehilim.duMois(1, 30))
        assertEquals(Portion(145, 150), CycleTehilim.duMois(30, 30))
        // Un mois de 29 jours finit le livre le 29.
        assertEquals(Portion(140, 150), CycleTehilim.duMois(29, 29))
        assertEquals(Portion(140, 144), CycleTehilim.duMois(29, 30))
    }

    @Test
    fun `le cycle de la semaine couvre tout le livre du dimanche au shabbat`() {
        assertEquals(7, CycleTehilim.SEMAINE.size)
        CycleTehilim.SEMAINE.zipWithNext().forEach { (a, b) -> assertEquals(a.fin + 1, b.debut) }
        assertEquals(1, CycleTehilim.SEMAINE.first().debut)
        assertEquals(150, CycleTehilim.SEMAINE.last().fin)
        assertEquals(Portion(1, 29), CycleTehilim.deLaSemaine(DayOfWeek.SUNDAY))
        assertEquals(Portion(107, 119), CycleTehilim.deLaSemaine(DayOfWeek.FRIDAY))
        assertEquals(Portion(120, 150), CycleTehilim.deLaSemaine(DayOfWeek.SATURDAY))
    }

    @Test
    fun `une portion partielle ne lit que ses versets`() {
        val p = Portion(119, 119, 97, 176)
        assertEquals(97..176, p.versets(119, 176))
        assertEquals(1..6, Portion(1, 9).versets(1, 6))
        assertTrue(p.partielle)
        assertFalse(Portion(5, 5).partielle)
    }

    /** Roch Hachana 5787 tombe le 12/09/2026 ; Tichri a toujours 30 jours. */
    @Test
    fun `le jour hebreu vient du calendrier`() {
        assertEquals(1 to 30, CycleTehilim.jourHebreu(LocalDate.of(2026, 9, 12)))
        assertEquals(23 to 30, CycleTehilim.jourHebreu(LocalDate.of(2026, 10, 4)))
    }

    @Test
    fun `l onglet textes mene au livre et les ecrans existent`() {
        val accueil = File("src/main/java/com/myhitbodedut/MainActivity.kt").readText()
        assertTrue(accueil.contains("Intent(this, TehilimActivity::class.java)"))
        val manifeste = File("src/main/AndroidManifest.xml").readText()
        listOf(".TehilimActivity", ".TehilimLectureActivity").forEach { assertTrue(manifeste.contains("android:name=\"$it\"")) }
        listOf("values", "values-en", "values-iw").forEach { d ->
            val xml = File("src/main/res/$d/strings.xml").readText()
            listOf("tehilim_title", "tehilim_month", "tehilim_week", "tehilim_single", "tehilim_range",
                "tehilim_range_verses", "tehilim_verses", "tehilim_today").forEach {
                assertTrue("$d : $it", xml.contains("name=\"$it\""))
            }
        }
    }
}
