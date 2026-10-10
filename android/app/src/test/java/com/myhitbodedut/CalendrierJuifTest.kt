package com.myhitbodedut

import com.myhitbodedut.CalendrierJuif.C
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

/**
 * Le calendrier hébraïque et les ajouts de prière (10/10).
 *
 * Les dates civiles de l'année 5787 ont été relevées sur deux calendriers
 * indépendants de KosherJava, qui concordent : @hebcal/core 6.14.1 et ICU
 * (Intl, calendrier « hebrew » de Node 22). Les conditions sont celles de
 * TefilaRules ; ces cas vérifient qu'elles arrivent bien jusqu'à l'écran,
 * pas que la bibliothèque a raison : c'est l'affaire d'un Rav (ZMANIM.md).
 */
class CalendrierJuifTest {

    private fun jour(iso: String, israel: Boolean = false) = CalendrierJuif.jour(LocalDate.parse(iso), israel)

    @Test
    fun `les dates hebraiques de 5787 concordent avec hebcal et ICU`() {
        val cas = mapOf(
            "2026-09-12" to Triple(1, 7, 5787),   // 1 Tichri, Roch Hachana
            "2026-10-11" to Triple(30, 7, 5787),  // 30 Tichri
            "2026-10-12" to Triple(1, 8, 5787),   // 1 Hechvan
            "2026-12-05" to Triple(25, 9, 5787),  // 25 Kislev
            "2027-03-23" to Triple(14, 13, 5787), // 14 Adar II (année embolismique)
            "2027-04-22" to Triple(15, 1, 5787),  // 15 Nissan
            "2027-08-12" to Triple(9, 5, 5787),   // 9 Av
        )
        cas.forEach { (iso, attendu) ->
            val j = jour(iso)
            assertEquals(iso, attendu, Triple(j.jourHebreu, j.moisHebreu, j.anneeHebraique))
        }
        assertEquals("כ״ט תשרי תשפ״ז", jour("2026-10-10").dateHebraique)
    }

    @Test
    fun `roch hodech donne yaale veyavo et le hallel partiel, sans tahanoun`() {
        listOf("2026-10-11", "2026-10-12").forEach { iso ->
            val c = jour(iso).conditions
            assertTrue(iso, C.YAALE_VEYAVO in c && C.ROCH_HODECH in c && C.HALLEL in c)
            assertFalse(iso, C.HALLEL_COMPLET in c || C.TAHANOUN_CHAHARIT in c || C.TAHANOUN_MINHA in c)
            assertTrue(iso, "roch_hodech" in jour(iso).fetes)
        }
    }

    @Test
    fun `hanouka donne al hanissim et le hallel complet`() {
        val c = jour("2026-12-06").conditions // 26 Kislev, dimanche
        assertTrue(C.AL_HANISSIM in c && C.HANOUKA in c && C.HALLEL_COMPLET in c)
        assertFalse(C.TAHANOUN_CHAHARIT in c)
        assertEquals(listOf("hanouka"), jour("2026-12-06").fetes)
    }

    @Test
    fun `machiv harouah des chemini atseret, tal oumatar selon le lieu`() {
        assertTrue(C.MORID_HATAL in jour("2026-10-02").conditions)           // 21 Tichri
        assertFalse(C.MACHIV_HAROUAH in jour("2026-10-02").conditions)
        assertTrue(C.MACHIV_HAROUAH in jour("2026-10-04").conditions)        // 23 Tichri
        assertFalse(C.MORID_HATAL in jour("2026-10-04").conditions)
        // En Israël, la demande de pluie commence le 7 Hechvan.
        assertFalse(C.TAL_OUMATAR in jour("2026-10-17", israel = true).conditions)
        assertTrue(C.TAL_OUMATAR in jour("2026-10-18", israel = true).conditions)
        // En diaspora, le soir du 4 décembre 2026 : le jour hébreu du 5.
        assertTrue(C.BERAKHA in jour("2026-12-04").conditions)
        assertTrue(C.TAL_OUMATAR in jour("2026-12-05").conditions)
        // 2027 précède une année civile bissextile : un jour plus tard.
        assertTrue(C.BERAKHA in jour("2027-12-05").conditions)
        assertTrue(C.TAL_OUMATAR in jour("2027-12-06").conditions)
    }

    @Test
    fun `jeunes, dix jours et neuf av`() {
        assertTrue(C.ASSERET_YEMEI_TECHOUVA in jour("2026-09-15").conditions)
        assertTrue(C.JEUNE in jour("2026-09-14").conditions)                // Guedalia, 3 Tichri
        assertTrue(C.JEUNE in jour("2026-12-20").conditions)                // 10 Tevet
        assertFalse(C.JEUNE in jour("2026-09-21").conditions)               // Kippour n'est pas un « jeûne » d'Anénou
        val av = jour("2027-08-12").conditions
        assertTrue(C.JEUNE in av && C.TICHA_BEAV in av)
        assertFalse(C.TAHANOUN_CHAHARIT in av)
    }

    @Test
    fun `lundi et jeudi, quand le tahanoun se dit`() {
        val lundi = jour("2026-10-19").conditions                           // 8 Hechvan
        assertTrue(C.TAHANOUN_CHAHARIT in lundi && C.LUNDI_JEUDI in lundi)
        assertFalse(C.LUNDI_JEUDI in jour("2026-10-20").conditions)         // mardi
        assertFalse(C.LUNDI_JEUDI in jour("2026-10-12").conditions)         // lundi de Roch 'Hodech
    }

    @Test
    fun `chabbat, paracha, omer et pourim`() {
        val chabbat = jour("2026-10-10")
        assertTrue(C.CHABBAT in chabbat.conditions && chabbat.chome)
        assertEquals("בראשית", chabbat.paracha)
        assertEquals(1, jour("2027-04-23").omer)                             // 16 Nissan
        assertEquals(0, jour("2027-04-22").omer)
        assertTrue(C.POURIM in jour("2027-03-23").conditions)
        assertTrue(C.AL_HANISSIM in jour("2027-03-23").conditions)
    }

    @Test
    fun `jerusalem - pourim et al hanissim le 15 adar`() {
        // 14 Adar II 5787 = 23/03/2027, 15 Adar II = 24/03/2027.
        val c14 = CalendrierJuif.jour(LocalDate.parse("2027-03-23"), true, mukafChoma = true).conditions
        val c15 = CalendrierJuif.jour(LocalDate.parse("2027-03-24"), true, mukafChoma = true).conditions
        assertFalse(C.POURIM in c14 || C.AL_HANISSIM in c14)
        assertTrue(C.POURIM in c15 && C.AL_HANISSIM in c15)
        // Ailleurs, le 14.
        assertTrue(C.AL_HANISSIM in CalendrierJuif.jour(LocalDate.parse("2027-03-23"), true).conditions)
        assertFalse(C.AL_HANISSIM in CalendrierJuif.jour(LocalDate.parse("2027-03-24"), true).conditions)
        // Pas de Ta'hanoun les deux jours, partout (TefilaRules).
        listOf("2027-03-23", "2027-03-24").forEach {
            assertFalse(it, C.TAHANOUN_CHAHARIT in CalendrierJuif.jour(LocalDate.parse(it), true, mukafChoma = true).conditions)
        }
    }

    @Test
    fun `arvit releve du jour hebreu suivant des l apres-midi`() {
        val date = LocalDate.parse("2026-10-10")
        assertEquals(date.plusDays(1), CalendrierJuif.jourDeLaPriere("arvit", date, LocalTime.of(19, 30)))
        assertEquals(date.plusDays(1), CalendrierJuif.jourDeLaPriere("chema_coucher", date, LocalTime.of(23, 0)))
        assertEquals(date, CalendrierJuif.jourDeLaPriere("arvit", date, LocalTime.of(0, 40)))
        assertEquals(date, CalendrierJuif.jourDeLaPriere("chaharit", date, LocalTime.of(19, 30)))
        assertEquals(date, CalendrierJuif.jourDeLaPriere("minha", date, LocalTime.of(17, 0)))
        // Samedi soir : Arvit relève du dimanche, d'où la section de motsaé Chabbat.
        assertTrue(C.MOTSAE_CHABBAT in CalendrierJuif.jour(date.plusDays(1), false).conditions)
    }

    @Test
    fun `chaque fete a son libelle dans les trois langues`() {
        val res = java.io.File("src/main/res")
        listOf("values", "values-en", "values-iw").forEach { d ->
            val xml = java.io.File(res, "$d/strings.xml").readText()
            CalendrierJuif.FETES.values.forEach { assertTrue("$d : fete_$it", xml.contains("name=\"fete_$it\"")) }
        }
    }
}
