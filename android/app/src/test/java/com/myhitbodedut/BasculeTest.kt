package com.myhitbodedut

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/** Les jours de bascule de la pluie et Avinou malkénou la veille de Kippour un vendredi (audit du 16/10). */
class BasculeTest {

    private fun c(iso: String, israel: Boolean = true) = CalendrierJuif.jour(LocalDate.parse(iso), israel).conditions

    @Test
    fun `22 Tichri et 15 Nissan ont les deux formules`() {
        for (iso in listOf("2026-10-03", "2027-04-22")) {
            val s = SiddurJour.conditionsPour("edot", true, c(iso))
            assertTrue(iso, CalendrierJuif.C.MACHIV_HAROUAH in s && CalendrierJuif.C.MORID_HATAL in s)
            val a = JourAffiche.ajouts(c(iso), "edot", true)
            assertTrue(iso, "ajout_machiv_harouah" in a && "ajout_morid_hatal" in a)
        }
        val a = JourAffiche.ajouts(c("2026-10-04"), "edot", true)
        assertTrue("ajout_machiv_harouah" in a && "ajout_morid_hatal" !in a)
        assertFalse(CalendrierJuif.C.MORID_HATAL in SiddurJour.conditionsPour("ashkenaz", false, c("2026-10-03", false)))
    }

    @Test
    fun `Avinou malkenou la veille de Kippour un vendredi`() {
        assertTrue(SiddurJour.avinouMalkenou(c("2028-09-29", false)))
        assertFalse(SiddurJour.avinouMalkenou(c("2026-09-20", false)))
    }

    @Test
    fun `Jerusalem la ville, pas les localites voisines`() {
        val dedans = listOf(31.7767 to 35.2345, 31.8466 to 35.1975, 31.735 to 35.188, 31.768 to 35.159, 31.722 to 35.226, 31.825 to 35.238, 31.796 to 35.249)
        val dehors = listOf(31.777 to 35.298, 31.8017 to 35.1478, 31.705 to 35.2, 31.762 to 35.262, 31.696 to 35.115, 32.08 to 34.78)
        dedans.forEach { (la, lo) -> assertTrue("$la,$lo", CalendrierJuif.aJerusalem(la, lo)) }
        dehors.forEach { (la, lo) -> assertFalse("$la,$lo", CalendrierJuif.aJerusalem(la, lo)) }
    }

    @Test
    fun `conditions deduites du releve du 19-10`() {
        val C = CalendrierJuif.C
        assertTrue(C.LAMNATSEAH in c("2026-10-19"))
        assertFalse(C.LAMNATSEAH in c("2026-10-11"))
        assertFalse(C.LAMNATSEAH in c("2027-04-21"))
        assertTrue(C.LEDAVID in c("2026-08-20") && C.LEDAVID in c("2026-10-02"))
        assertFalse(C.LEDAVID in c("2026-10-19"))
        assertTrue(C.TECHOUVA_HOCHAANA in c("2026-10-02"))
        assertTrue(C.YOM_TOV in c("2027-04-22") && C.MOED in c("2027-04-22"))
        assertTrue(C.MOTSAE_CHABBAT_YT in c("2027-04-23"))
        assertFalse(C.MOTSAE_CHABBAT_YT in c("2027-04-24"))
        assertFalse(C.BEIT_AVEL in c("2026-10-19") || C.OUBLI in c("2026-10-19"))
    }
}
