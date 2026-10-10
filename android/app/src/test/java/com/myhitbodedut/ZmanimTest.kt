package com.myhitbodedut

import org.json.JSONArray
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime

/**
 * Les zmanim (10/10). Deux familles de tests, séparées exprès :
 *
 * - astronomiques : le lever, le coucher, les angles et le passage au
 *   méridien, comparés à PyEphem 4.2.1 (libastro, VSOP87), qui ne partage
 *   rien avec l'algorithme NOAA de KosherJava. Tolérance : 60 s, l'écart
 *   habituel entre modèles de réfraction (mesuré ici : moins de 15 s) ;
 * - de structure : l'ordre de la journée, les heures saisonnières, les
 *   événements absents. Ce ne sont pas des validations halakhiques : le
 *   choix des méthodes revient à un Rav (ZMANIM.md).
 */
class ZmanimTest {

    private val lieux = mapOf(
        "Jérusalem" to Zmanim.Lieu("Jérusalem", 31.76904, 35.21633, 0.0, "Asia/Jerusalem"),
        "Paris" to Zmanim.Lieu("Paris", 48.85341, 2.3488, 0.0, "Europe/Paris"),
        "New York" to Zmanim.Lieu("New York", 40.71427, -74.00597, 0.0, "America/New_York"),
        "Melbourne" to Zmanim.Lieu("Melbourne", -37.814, 144.96332, 0.0, "Australia/Melbourne"),
    )
    private val tromso = Zmanim.Lieu("Tromsø", 69.6492, 18.9553, 0.0, "Europe/Oslo")

    private fun zmanim(lieu: Zmanim.Lieu, iso: String) =
        Zmanim.jour(lieu, LocalDate.parse(iso)).associateBy { it.cle }

    @Test
    fun `astronomie - concorde avec PyEphem a une minute pres`() {
        val reference = JSONArray(File("src/test/resources/zmanim_reference_pyephem.json").readText())
        assertTrue("référence vide", reference.length() >= 20)
        for (i in 0 until reference.length()) {
            val r = reference.getJSONObject(i)
            val z = zmanim(lieux.getValue(r.getString("lieu")), r.getString("date"))
            listOf("netz", "chkia", "alot", "misheyakir", "tset", "chatsot").forEach { cle ->
                val attendu = LocalTime.parse(r.getString(cle))
                val calcule = z.getValue(cle).local!!.toLocalTime()
                val ecart = Duration.between(attendu, calcule).abs().seconds
                assertTrue("${r.getString("lieu")} ${r.getString("date")} $cle : $calcule contre $attendu", ecart <= 60)
            }
        }
    }

    @Test
    fun `la journee est dans l ordre, partout et en toute saison`() {
        val ordre = listOf("alot", "misheyakir", "netz", "szks_mga", "szks_gra", "szt_mga", "szt_gra",
            "chatsot", "minha_gedola", "minha_ketana", "plag", "chkia", "tset", "tset_rt", "chatsot_layla")
        assertEquals(ordre, Zmanim.PROFIL.map { it.first })
        lieux.values.forEach { lieu ->
            listOf("2026-03-29", "2026-06-21", "2026-10-25", "2026-12-21").forEach { iso ->
                val z = Zmanim.jour(lieu, LocalDate.parse(iso))
                assertTrue("${lieu.nom} $iso", z.all { it.statut == Zmanim.Statut.OK })
                val instants = z.map { it.instant!! }
                assertEquals("${lieu.nom} $iso", instants.sorted(), instants)
                assertEquals(LocalDate.parse(iso), z.first { it.cle == "netz" }.local!!.toLocalDate())
                // 'Hatsot de la nuit tombe entre la chki'a et le lever du lendemain ; à
                // l'horloge, avant ou après minuit selon le fuseau (Jérusalem en
                // hiver, UTC+2 : vers 23 h 25 le jour même).
                val nuit = z.first { it.cle == "chatsot_layla" }.instant!!
                val lever = Zmanim.jour(lieu, LocalDate.parse(iso).plusDays(1)).first { it.cle == "netz" }.instant!!
                assertTrue("${lieu.nom} $iso", nuit.isAfter(z.first { it.cle == "chkia" }.instant) && nuit.isBefore(lever))
            }
        }
    }

    @Test
    fun `heures saisonnieres du Gra - trois heures apres le lever`() {
        val z = zmanim(lieux.getValue("Paris"), "2026-10-10")
        val jour = Duration.between(z.getValue("netz").instant, z.getValue("chkia").instant).toMillis()
        val attendu = z.getValue("netz").instant!!.plusMillis(jour / 4)
        assertTrue(Duration.between(attendu, z.getValue("szks_gra").instant).abs().seconds <= 1)
        // MGA : de l'aube 72 minutes avant le lever à 72 minutes après le coucher.
        val aube = z.getValue("netz").instant!!.minusSeconds(72 * 60)
        val nuit = z.getValue("chkia").instant!!.plusSeconds(72 * 60)
        val mga = aube.plusMillis(Duration.between(aube, nuit).toMillis() / 4)
        assertTrue(Duration.between(mga, z.getValue("szks_mga").instant).abs().seconds <= 1)
    }

    @Test
    fun `changement d heure - le lever reste a sa place`() {
        // Paris, 25/10/2026 : on recule d'une heure dans la nuit. Le lever
        // du 25 tombe une heure « plus tôt » à l'horloge que celui du 24.
        val samedi = zmanim(lieux.getValue("Paris"), "2026-10-24").getValue("netz").local!!
        val dimanche = zmanim(lieux.getValue("Paris"), "2026-10-25").getValue("netz").local!!
        assertEquals(2 * 3600, samedi.offset.totalSeconds)
        assertEquals(1 * 3600, dimanche.offset.totalSeconds)
        val minutes = Duration.between(samedi.toLocalTime(), dimanche.toLocalTime()).toMinutes()
        assertTrue("$samedi → $dimanche", minutes in -60L..-55L)
    }

    @Test
    fun `soleil de minuit et nuit polaire - absent, jamais en erreur`() {
        val ete = zmanim(tromso, "2026-06-21")
        listOf("netz", "chkia", "szks_gra", "plag").forEach { assertEquals(it, Zmanim.Statut.ABSENT, ete.getValue(it).statut) }
        val hiver = zmanim(tromso, "2026-12-21")
        assertEquals(Zmanim.Statut.ABSENT, hiver.getValue("netz").statut)
        // L'aube à 16,1° existe en décembre à Tromsø : le soleil passe sous l'horizon sans remonter.
        assertEquals(Zmanim.Statut.OK, hiver.getValue("alot").statut)
        (ete.values + hiver.values).forEach { assertTrue(it.cle, it.statut != Zmanim.Statut.ERREUR) }
    }

    @Test
    fun `allumage - dix-huit minutes avant la chkia`() {
        val lieu = lieux.getValue("Jérusalem")
        val chkia = zmanim(lieu, "2026-10-16").getValue("chkia").instant!!
        val allumage = Zmanim.allumage(lieu, LocalDate.parse("2026-10-16"), 18)
        assertNotNull(allumage.instant)
        assertEquals(18L * 60, Duration.between(allumage.instant, chkia).seconds)
        assertEquals("18", allumage.methode.parametres)
        assertTrue(Zmanim.ALLUMAGE_DEFAUT in Zmanim.ALLUMAGES)
    }

    @Test
    fun `chaque zman et chaque methode a son libelle dans les trois langues`() {
        val cles = Zmanim.PROFIL.map { "zman_${it.first}" } + "zman_allumage" + "zman_sortie" +
            (Zmanim.PROFIL.map { it.second.cle } + "avant_chkia").distinct().map { "zman_methode_$it" }
        listOf("values", "values-en", "values-iw").forEach { d ->
            val xml = File("src/main/res/$d/strings.xml").readText()
            cles.forEach { assertTrue("$d : $it", xml.contains("name=\"$it\"")) }
        }
    }
}
