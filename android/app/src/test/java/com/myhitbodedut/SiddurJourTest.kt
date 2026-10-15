package com.myhitbodedut

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Les annotations du siddour (10/10) : elles désignent encore le bon bloc,
 * leurs conditions existent, chaque libellé est traduit, et les ajouts
 * principaux sont là dans chaque nusah.
 */
class SiddurJourTest {

    private val json = File("src/main/res/raw/siddur_annotations.json").readText()

    @Test
    fun `chaque annotation designe encore son bloc`() {
        Nusah.entries.forEach { n ->
            val siddur = DataFiles.siddur(n)
            SiddurJour.parse(json, n.cle).forEach { (id, annotations) ->
                val texte = siddur.texte(id)!!
                annotations.forEach { a ->
                    val section = texte.sections[a.section]
                    val vu = if (a.bloc == null) SiddurJour.empreinte(section.titre)
                    else SiddurJour.empreinte(section.blocs[a.bloc].texte)
                    assertEquals("${n.cle}/$id §${a.section}/${a.bloc}", a.empreinte, vu)
                }
            }
        }
    }

    @Test
    fun `les conditions existent et chaque libelle est traduit`() {
        val connues = (CalendrierJuif.C.TOUTES + CalendrierJuif.C.DERIVEES).toSet()
        val xml = listOf("values", "values-en", "values-iw").map { File("src/main/res/$it/strings.xml").readText() }
        Nusah.entries.forEach { n ->
            SiddurJour.parse(json, n.cle).values.flatten().forEach { a ->
                assertTrue("${n.cle} : ${a.condition}", a.condition.removePrefix("!") in connues)
                xml.forEach { assertTrue("${n.cle} : ${SiddurJour.libelle(a.condition)}", it.contains("name=\"${SiddurJour.libelle(a.condition)}\"")) }
                if (a.sorte == SiddurJour.Sorte.BLOC) assertTrue(a.groupe != null && a.racine != null)
                a.libelle?.let { l -> xml.forEach { assertTrue("${n.cle} : $l", it.contains("name=\"$l\"")) } }
            }
        }
    }

    @Test
    fun `les ajouts principaux sont annotes dans les trois nusah`() {
        Nusah.entries.forEach { n ->
            val chaharit = SiddurJour.parse(json, n.cle).getValue("chaharit").map { it.condition }.toSet()
            listOf("yaale_veyavo", "al_hanissim", "hanouka", "pourim", "tal_oumatar", "berakha", "asseret_yemei_techouva")
                .forEach { assertTrue("${n.cle} : $it", it in chaharit) }
            // Été / hiver dans les trois offices (14/10 : l'Arvit ashkénaze n'était pas marqué).
            listOf("chaharit", "minha", "arvit").forEach { o ->
                val c = SiddurJour.parse(json, n.cle).getValue(o).map { it.condition }.toSet()
                listOf("tal_oumatar", "berakha").forEach { assertTrue("${n.cle} $o : $it", it in c) }
            }
            val repas = SiddurJour.parse(json, n.cle).getValue("birkat_hamazon").map { it.condition }.toSet()
            listOf("yaale_veyavo", "al_hanissim", "chabbat").forEach { assertTrue("${n.cle} repas : $it", it in repas) }
        }
    }

    /** Roch 'Hodech (12/10) : dans Cha'harit, « Aujourd'hui · Hallel › » puis « Moussaf › » ; un jour ordinaire, rien. */
    @Test
    fun `roch hodech - chaharit renvoie au hallel puis au moussaf`() {
        val rc = CalendrierJuif.jour(java.time.LocalDate.parse("2026-10-12"), false).conditions
        val ordinaire = CalendrierJuif.jour(java.time.LocalDate.parse("2026-10-14"), false).conditions
        Nusah.entries.forEach { n ->
            val siddur = DataFiles.siddur(n)
            val texte = siddur.texte("chaharit")!!
            val annotations = SiddurJour.parse(json, n.cle).getValue("chaharit")
            val renvois = SiddurJour.lignes(texte, annotations, rc, nusah = n.cle).filterIsInstance<SiddurJour.Ligne.Renvoi>()
            assertEquals(n.cle, listOf("hallel", "moussaf_rosh_hodech"), renvois.map { it.cible })
            renvois.forEach { assertTrue(n.cle, siddur.texte(it.cible) != null) }
            assertTrue(n.cle, SiddurJour.lignes(texte, annotations, ordinaire, nusah = n.cle).none { it is SiddurJour.Ligne.Renvoi })
        }
    }

    @Test
    fun `roch hodech - yaale veyavo en evidence, al hanissim replie puis ouvert`() {
        val texte = DataFiles.siddur(Nusah.EDOT).texte("chaharit")!!
        val annotations = SiddurJour.parse(json, "edot").getValue("chaharit")
        val rc = CalendrierJuif.jour(java.time.LocalDate.parse("2026-10-12"), false).conditions
        val lignes = SiddurJour.lignes(texte, annotations, rc)
        assertTrue(SiddurJour.Ligne.Etiquette("yaale_veyavo", "ajout_yaale_veyavo") in lignes)
        assertTrue(lignes.any { it is SiddurJour.Ligne.Replie && it.condition == "al_hanissim" && !it.ouvert })
        // Dans Ya'alé véyavo du jour, la variante de Pessa'h reste lisible, estompée.
        val pessah = lignes.filterIsInstance<SiddurJour.Ligne.Bloc>()
            .first { SiddurJour.empreinte(it.bloc.texte).contains("חג המצות הזה") }
        assertEquals(SiddurJour.Style.HORS, pessah.style)
        // Le repli d'Al hanissim, une fois ouvert, montre ses blocs.
        val cle = lignes.filterIsInstance<SiddurJour.Ligne.Replie>().first { it.condition == "al_hanissim" }.cle
        val ouvert = SiddurJour.lignes(texte, annotations, rc, setOf(cle))
        assertTrue(ouvert.size > lignes.size)
        // Aucun bloc ne se perd : replié, il revient à l'ouverture de tous les groupes.
        val tous = lignes.filterIsInstance<SiddurJour.Ligne.Replie>().map { it.cle }.toSet()
        val blocsTous = SiddurJour.lignes(texte, annotations, rc, tous).count { it is SiddurJour.Ligne.Bloc }
        assertEquals(texte.sections.sumOf { it.blocs.size }, blocsTous)
    }

    @Test
    fun `anenou - l officiant et le particulier, selon le nusah`() {
        val jeune = CalendrierJuif.jour(java.time.LocalDate.parse("2026-12-20"), false).conditions // 10 Tevet
        fun etiquettes(n: Nusah, id: String) = SiddurJour.lignes(DataFiles.siddur(n).texte(id)!!,
            SiddurJour.parse(json, n.cle).getValue(id), jeune, nusah = n.cle)
            .filterIsInstance<SiddurJour.Ligne.Etiquette>().map { it.libelle }
        // Edot : l'officiant et le particulier, à Cha'harit comme à Min'ha.
        assertTrue(etiquettes(Nusah.EDOT, "chaharit").containsAll(listOf("ajout_anenou_officiant", "ajout_anenou_particulier")))
        assertTrue(etiquettes(Nusah.EDOT, "minha").containsAll(listOf("ajout_anenou_officiant", "ajout_anenou_particulier")))
        // Ashkenaz et Sfard : à Cha'harit, l'officiant seul ; à Min'ha, les deux.
        listOf(Nusah.ASHKENAZ, Nusah.SEFARD).forEach { n ->
            assertEquals("${n.cle}", listOf("ajout_anenou_officiant"), etiquettes(n, "chaharit").filter { it.startsWith("ajout_anenou") })
            assertTrue("${n.cle}", etiquettes(n, "minha").containsAll(listOf("ajout_anenou_officiant", "ajout_anenou_particulier")))
        }
        // Un jour ordinaire, tous les Anénou sont repliés.
        val ordinaire = CalendrierJuif.jour(java.time.LocalDate.parse("2026-12-21"), false).conditions
        val replis = SiddurJour.lignes(DataFiles.siddur(Nusah.SEFARD).texte("minha")!!,
            SiddurJour.parse(json, "sefard").getValue("minha"), ordinaire, nusah = "sefard")
            .filterIsInstance<SiddurJour.Ligne.Replie>().map { it.libelle }
        assertTrue(replis.containsAll(listOf("ajout_anenou_officiant", "ajout_anenou_particulier")))
    }

    @Test
    fun `ashkenaz hors d israel - morid hatal replie l ete`() {
        val ete = CalendrierJuif.jour(java.time.LocalDate.parse("2027-06-01"), false).conditions
        fun etat(n: Nusah, israel: Boolean) = SiddurJour.lignes(DataFiles.siddur(n).texte("chaharit")!!,
            SiddurJour.parse(json, n.cle).getValue("chaharit"), ete, nusah = n.cle, enIsrael = israel)
        assertTrue(etat(Nusah.ASHKENAZ, false).any { it is SiddurJour.Ligne.Replie && it.condition == "morid_hatal" })
        assertTrue(SiddurJour.Ligne.Etiquette("morid_hatal", "ajout_morid_hatal") in etat(Nusah.ASHKENAZ, true))
        assertTrue(SiddurJour.Ligne.Etiquette("morid_hatal", "ajout_morid_hatal") in etat(Nusah.SEFARD, false))
    }

    /** Avinou malkénou (14/10) : replié un jour ordinaire, ouvert le 10 Tévèt et pendant les dix jours. */
    @Test
    fun `avinou malkenou - jeunes et dix jours seulement`() {
        fun am(d: String) = CalendrierJuif.C.AVINOU_MALKENOU in
            SiddurJour.conditionsPour("ashkenaz", true, CalendrierJuif.jour(java.time.LocalDate.parse(d), true).conditions)
        listOf("2026-12-20", "2026-09-15", "2026-09-13", "2026-09-14").forEach { assertTrue(it, am(it)) }
        listOf("2026-12-21", "2026-09-12", "2026-09-19", "2026-09-20", "2027-08-12").forEach { assertFalse(it, am(it)) }
        listOf(Nusah.ASHKENAZ, Nusah.SEFARD).forEach { n ->
            fun lignes(d: String) = SiddurJour.lignes(DataFiles.siddur(n).texte("chaharit")!!,
                SiddurJour.parse(json, n.cle).getValue("chaharit"),
                CalendrierJuif.jour(java.time.LocalDate.parse(d), true).conditions, nusah = n.cle, enIsrael = true)
            assertTrue("${n.cle}", lignes("2026-12-21").any { it is SiddurJour.Ligne.Replie && it.condition == "avinou_malkenou" })
            assertTrue("${n.cle}", SiddurJour.Ligne.Etiquette("avinou_malkenou", "ajout_avinou_malkenou") in lignes("2026-12-20"))
        }
    }

    /** La lecture de la Torah (15/10) : ouverte les jours de lecture, repliée un mardi ordinaire. */
    @Test
    fun `kriat hatorah - repliee les jours sans lecture`() {
        fun c(d: String) = CalendrierJuif.jour(java.time.LocalDate.parse(d), true).conditions
        listOf("2026-10-19", "2026-10-15", "2026-10-17", "2026-11-11", "2026-12-20", "2026-09-29")
            .forEach { assertTrue(it, CalendrierJuif.C.KRIAT_HATORAH in c(it)) }
        listOf("2026-10-20", "2026-10-21", "2026-10-23").forEach { assertFalse(it, CalendrierJuif.C.KRIAT_HATORAH in c(it)) }
        Nusah.entries.forEach { n ->
            fun lignes(d: String) = SiddurJour.lignes(DataFiles.siddur(n).texte("chaharit")!!,
                SiddurJour.parse(json, n.cle).getValue("chaharit"), c(d), nusah = n.cle, enIsrael = true)
            assertTrue(n.cle, lignes("2026-10-20").any { it is SiddurJour.Ligne.Replie && it.condition == "kriat_hatorah" })
            assertTrue(n.cle, SiddurJour.Ligne.Etiquette("kriat_hatorah", "ajout_kriat_hatorah") in lignes("2026-10-19"))
            // « אל ארך אפים » : dit un lundi ordinaire, estompé à Pourim (23/03/2027) et à 'Hanouka (07/12/2026), sans Ta'hanoun.
            fun elArekh(d: String) = lignes(d).filterIsInstance<SiddurJour.Ligne.Bloc>()
                .first { SiddurJour.empreinte(it.bloc.texte).startsWith("אל ארך אפים") }.style
            assertEquals(n.cle, SiddurJour.Style.DU_JOUR, elArekh("2026-10-19"))
            assertEquals(n.cle, SiddurJour.Style.HORS, elArekh("2027-03-23"))
            assertEquals(n.cle, SiddurJour.Style.HORS, elArekh("2026-12-07"))
        }
        // Les « יהי רצון » (Sfard, Ashkenaz) : lundi et jeudi avec Ta'hanoun seulement ; pas à Roch 'Hodech (11/11/2026, mercredi).
        listOf(Nusah.SEFARD, Nusah.ASHKENAZ).forEach { n ->
            fun yehiRatson(d: String) = SiddurJour.lignes(DataFiles.siddur(n).texte("chaharit")!!,
                SiddurJour.parse(json, n.cle).getValue("chaharit"), c(d), nusah = n.cle, enIsrael = true)
                .filterIsInstance<SiddurJour.Ligne.Bloc>().first { SiddurJour.empreinte(it.bloc.texte).startsWith("יהי רצון מלפני אבינו") }.style
            assertEquals(n.cle, SiddurJour.Style.DU_JOUR, yehiRatson("2026-10-19"))
            assertEquals(n.cle, SiddurJour.Style.HORS, yehiRatson("2026-11-11"))
        }
    }

    @Test
    fun `une condition niee s applique quand la cle manque`() {
        assertTrue(SiddurJour.vrai("!tahanoun_minha", setOf("chabbat")))
        assertFalse(SiddurJour.vrai("!tahanoun_minha", setOf("tahanoun_minha")))
        assertTrue(SiddurJour.vrai("roch_hodech", setOf("roch_hodech")))
        assertEquals("ajout_sans_tahanoun", SiddurJour.libelle("!tahanoun_chaharit"))
        assertEquals("fete_yom_kippour", SiddurJour.libelle("kippour"))
    }
}
