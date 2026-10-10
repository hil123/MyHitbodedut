package com.myhitbodedut

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
        assertEquals(listOf("ajout_machiv_harouah", "ajout_berakha", "ajout_yaale_veyavo", "ajout_hallel", "ajout_sans_tahanoun"), rc)
        val hanouka = JourAffiche.ajouts(CalendrierJuif.jour(LocalDate.parse("2026-12-07"), false).conditions)
        assertTrue("ajout_al_hanissim" in hanouka && "ajout_hallel_complet" in hanouka && "ajout_tal_oumatar" in hanouka)
        val chabbat = JourAffiche.ajouts(CalendrierJuif.jour(LocalDate.parse("2026-10-10"), false).conditions)
        assertTrue(chabbat.none { it.contains("tahanoun") })
        // Chaque clé a son libellé dans la table.
        (rc + hanouka).forEach { assertTrue(it, Libelles.res(it) != null) }
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
}
