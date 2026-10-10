package com.myhitbodedut

import com.myhitbodedut.CalendrierJuif.C
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.LocalDate
import java.time.YearMonth

/** L'onglet Zmanim et le calendrier (10/10) : ajouts nommés, entrée et sortie, grille du mois. */
class JourAfficheTest {

    private val jerusalem = Zmanim.Lieu("Jérusalem", 31.76904, 35.21633, 0.0, "Asia/Jerusalem")
    private val paris = Zmanim.Lieu("Paris", 48.85341, 2.3488, 0.0, "Europe/Paris")

    private fun horaires(lieu: Zmanim.Lieu, iso: String, israel: Boolean) =
        JourAffiche.horaires(lieu, LocalDate.parse(iso), israel, 18).associateBy { it.cle }

    @Test
    fun `vendredi - l allumage dix-huit minutes avant la chkia, samedi - la sortie`() {
        val vendredi = horaires(jerusalem, "2026-10-16", true)
        assertEquals(18L * 60, Duration.between(vendredi.getValue("allumage").instant, vendredi.getValue("chkia").instant).seconds)
        assertFalse("sortie" in vendredi)
        val samedi = horaires(jerusalem, "2026-10-17", true)
        assertEquals(samedi.getValue("tset").instant, samedi.getValue("sortie").instant)
        assertFalse("allumage" in samedi)
        assertFalse("allumage" in horaires(jerusalem, "2026-10-14", true) || "sortie" in horaires(jerusalem, "2026-10-14", true))
    }

    @Test
    fun `deuxieme jour de fete - allumer pas avant la sortie du premier`() {
        // Roch Hachana 5787, diaspora : samedi 12 et dimanche 13 septembre 2026.
        val premier = horaires(paris, "2026-09-12", false)
        assertEquals("apres_tset", premier.getValue("allumage").methode.cle)
        assertEquals(premier.getValue("tset").instant, premier.getValue("allumage").instant)
        assertFalse("sortie" in premier)
        assertTrue("sortie" in horaires(paris, "2026-09-13", false))
        assertEquals("avant_chkia", horaires(paris, "2026-09-11", false).getValue("allumage").methode.cle)
    }

    @Test
    fun `les ajouts nommes suivent les conditions`() {
        val rc = JourAffiche.ajouts(CalendrierJuif.jour(LocalDate.parse("2026-10-12"), false).conditions)
        assertEquals(listOf("ajout_machiv_harouah", "ajout_berakha", "ajout_yaale_veyavo", "ajout_hallel_partiel_sans_berakha",
            "ajout_sans_tahanoun"), rc)
        val hanouka = JourAffiche.ajouts(CalendrierJuif.jour(LocalDate.parse("2026-12-07"), false).conditions)
        assertTrue("ajout_al_hanissim" in hanouka && "ajout_hallel_complet" in hanouka && "ajout_tal_oumatar" in hanouka)
        val chabbat = JourAffiche.ajouts(CalendrierJuif.jour(LocalDate.parse("2026-10-10"), false).conditions)
        assertTrue(chabbat.none { it.contains("tahanoun") })
        // Chaque clé a son libellé dans la table.
        (rc + hanouka).forEach { assertTrue(it, Libelles.res(it) != null) }
    }

    @Test
    fun `le nusah - hallel partiel, morid hatal, anenou`() {
        val rc = CalendrierJuif.jour(LocalDate.parse("2026-10-12"), false).conditions
        // Bénédiction du Hallel partiel : sans, même en communauté (Choul'han
        // Aroukh, OC 422:2, Edot), avec (Rema, Ashkenaz et Sfard).
        assertTrue("ajout_hallel_partiel_sans_berakha" in JourAffiche.ajouts(rc, "edot"))
        assertTrue("ajout_hallel_partiel_avec_berakha" in JourAffiche.ajouts(rc, "sefard"))
        assertTrue("ajout_hallel_partiel_avec_berakha" in JourAffiche.ajouts(rc, "ashkenaz"))
        // Morid hatal, l'été : beaucoup d'Ashkénazes ne le disent pas hors d'Israël.
        val ete = CalendrierJuif.jour(LocalDate.parse("2027-06-01"), false).conditions
        assertTrue("ajout_morid_hatal" in JourAffiche.ajouts(ete, "edot"))
        assertTrue("ajout_morid_hatal" in JourAffiche.ajouts(ete, "sefard"))
        assertFalse("ajout_morid_hatal" in JourAffiche.ajouts(ete, "ashkenaz"))
        assertTrue("ajout_morid_hatal" in JourAffiche.ajouts(ete, "ashkenaz", enIsrael = true))
        // Anénou : l'officiant partout ; le particulier, à Min'ha seulement chez Ashkenaz et Sfard.
        val jeune = CalendrierJuif.jour(LocalDate.parse("2026-12-20"), false).conditions
        assertEquals(listOf("ajout_anenou_officiant", "ajout_anenou_particulier_matin_minha"),
            JourAffiche.ajouts(jeune, "edot").filter { it.startsWith("ajout_anenou") })
        assertEquals(listOf("ajout_anenou_officiant", "ajout_anenou_particulier_minha"),
            JourAffiche.ajouts(jeune, "ashkenaz").filter { it.startsWith("ajout_anenou") })
        // Tich'a bèAv 5787 (12/08/2027) : chez les Edot, Arvit aussi.
        val av = CalendrierJuif.jour(LocalDate.parse("2027-08-12"), false).conditions
        assertTrue(C.TICHA_BEAV in av)
        assertEquals(listOf("ajout_anenou_officiant", "ajout_anenou_particulier_trois"),
            JourAffiche.ajouts(av, "edot").filter { it.startsWith("ajout_anenou") })
        assertEquals(listOf("ajout_anenou_officiant", "ajout_anenou_particulier_minha"),
            JourAffiche.ajouts(av, "sefard").filter { it.startsWith("ajout_anenou") })
    }

    @Test
    fun `la grille d octobre 2026 commence un jeudi, du dimanche au samedi`() {
        val s = JourAffiche.semaines(YearMonth.of(2026, 10))
        assertTrue(s.all { it.size == 7 })
        assertEquals(LocalDate.parse("2026-10-01"), s[0][4])
        assertEquals(listOf(null, null, null, null), s[0].take(4))
        assertEquals(31, s.flatten().count { it != null })
        assertEquals(5, s.size)
    }

    @Test
    fun `le siddour suit les zmanim du lieu`() {
        val jer = Zmanim.Lieu("Jérusalem", 31.76904, 35.21633, 0.0, "Asia/Jerusalem")
        fun a(h: String) = java.time.ZonedDateTime.parse("2026-10-16T$h+03:00[Asia/Jerusalem]").toInstant()
        val z = Zmanim.jour(jer, LocalDate.parse("2026-10-16")).associate { it.cle to it.instant!! }
        fun juste(cle: String, secondes: Long) = z.getValue(cle).plusSeconds(secondes)
        assertEquals("birkot_hachahar", JourAffiche.momentDuSiddour(jer, juste("alot", 60)))
        assertEquals("chaharit", JourAffiche.momentDuSiddour(jer, juste("misheyakir", 60)))
        assertEquals("chaharit", JourAffiche.momentDuSiddour(jer, juste("chatsot", -60)))
        assertEquals(null, JourAffiche.momentDuSiddour(jer, juste("chatsot", 60)))
        assertEquals("minha", JourAffiche.momentDuSiddour(jer, juste("minha_gedola", 60)))
        assertEquals("minha", JourAffiche.momentDuSiddour(jer, juste("chkia", -60)))
        assertEquals(null, JourAffiche.momentDuSiddour(jer, juste("chkia", 60)))
        assertEquals("arvit", JourAffiche.momentDuSiddour(jer, juste("tset", 60)))
        assertEquals("chema_coucher", JourAffiche.momentDuSiddour(jer, a("22:45:00")))
        assertEquals("tikoun_hatsot", JourAffiche.momentDuSiddour(jer, a("02:00:00")))
        // Soleil de minuit à Tromsø : les heures fixes.
        val tromso = Zmanim.Lieu("Tromsø", 69.6492, 18.9553, 0.0, "Europe/Oslo")
        val juin = java.time.ZonedDateTime.parse("2026-06-21T14:00:00+02:00[Europe/Oslo]").toInstant()
        assertEquals(SiddurMoment.suggestion(java.time.LocalTime.of(14, 0)), JourAffiche.momentDuSiddour(tromso, juin))
    }

}
