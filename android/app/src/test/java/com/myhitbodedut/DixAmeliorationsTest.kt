package com.myhitbodedut

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZonedDateTime

/** Les dix améliorations du 14/10 : ce qui se teste sans écran. */
class DixAmeliorationsTest {

    private val zone = ZoneId.of("Asia/Jerusalem")

    @Test
    fun `rappel Levana, premier soir puis motsae Chabbat puis dernier`() {
        val heure = { d: LocalDate -> d.atTime(19, 0).atZone(zone) }
        val (q1, s1) = LevanaAlerte.prochain(ZonedDateTime.of(2026, 10, 14, 9, 0, 0, 0, zone), Levana.Usage.TROIS_MI_MOIS, heure)!!
        assertEquals(LocalDate.of(2026, 10, 14), q1.toLocalDate()); assertEquals(LevanaAlerte.Soir.PREMIER, s1)
        val (q2, s2) = LevanaAlerte.prochain(q1.plusMinutes(1), Levana.Usage.TROIS_MI_MOIS, heure)!!
        assertEquals(LocalDate.of(2026, 10, 17), q2.toLocalDate()); assertEquals(LevanaAlerte.Soir.MOTSAE, s2)
        val (q3, s3) = LevanaAlerte.prochain(q2.plusMinutes(1), Levana.Usage.TROIS_MI_MOIS, heure)!!
        assertEquals(LocalDate.of(2026, 10, 25), q3.toLocalDate()); assertEquals(LevanaAlerte.Soir.DERNIER, s3)
        // Après le dernier soir : le premier soir du mois suivant.
        val (q4, s4) = LevanaAlerte.prochain(q3.plusMinutes(1), Levana.Usage.TROIS_MI_MOIS, heure)!!
        assertTrue(q4.toLocalDate().isAfter(LocalDate.of(2026, 11, 5))); assertEquals(LevanaAlerte.Soir.PREMIER, s4)
    }

    @Test
    fun `widget, la priere du moment et sa limite`() {
        val j = LocalDate.of(2026, 10, 14)
        fun t(h: Int, m: Int) = j.atTime(h, m).atZone(zone)
        val horaires = mapOf("szt_gra" to t(10, 30), "chkia" to t(17, 50), "chatsot_layla" to t(23, 40))
        assertEquals("chaharit" to t(10, 30), ZmanimWidget.prochaine(horaires, t(7, 0)))
        assertEquals("minha" to t(17, 50), ZmanimWidget.prochaine(horaires, t(13, 0)))
        assertEquals("arvit" to t(23, 40), ZmanimWidget.prochaine(horaires, t(19, 0)))
        assertEquals("arvit" to null, ZmanimWidget.prochaine(horaires, t(23, 50)))
    }

    @Test
    fun `recherche, sans voyelles ni balises`() {
        assertEquals("ברוך אתה", RechercheGlobale.nu("<b>בָּרוּךְ</b> אַתָּה"))
        val s = Siddur(emptyList(), listOf(SiddurTexte("t", "g", LocalizedText("עלינו", "Alénou", "Aleinu"), "",
            listOf(SiddurSection("", listOf(SiddurBloc("וַאֲנַחְנוּ כּוֹרְעִים", false)))))))
        assertEquals("t", RechercheGlobale.dansRecueil(s, RechercheGlobale.nu("כורעים"), RechercheGlobale.Sorte.SIDDOUR, "fr").single().id)
        assertEquals("t", RechercheGlobale.dansRecueil(s, "alénou", RechercheGlobale.Sorte.SIDDOUR, "fr").single().id)
        assertTrue(RechercheGlobale.dansRecueil(s, "שבת", RechercheGlobale.Sorte.SIDDOUR, "fr").isEmpty())
    }

    @Test
    fun `journal, les minutes du mois et le resume`() {
        val jours = mapOf(
            "2026-10-01" to DayRecord("2026-10-01", 20, ""),
            "2026-10-03" to DayRecord("2026-10-03", 0, "note", noteSeule = true),
            "2026-10-31" to DayRecord("2026-10-31", 45, ""),
        )
        val m = JournalPlus.minutesDuMois(jours, YearMonth.of(2026, 10))
        assertEquals(31, m.size); assertEquals(20, m[0]); assertEquals(0, m[2]); assertEquals(45, m[30])
        assertEquals("🙁 → 🙂\nG : merci", JournalPlus.resume(JournalPlus.Plus(2, 4, "merci"), "G"))
        assertEquals("", JournalPlus.resume(JournalPlus.Plus(), "G"))
    }
}
