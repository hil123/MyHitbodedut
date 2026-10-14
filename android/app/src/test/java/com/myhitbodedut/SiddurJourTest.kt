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
        val connues = CalendrierJuif.C.TOUTES.toSet()
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

    @Test
    fun `une condition niee s applique quand la cle manque`() {
        assertTrue(SiddurJour.vrai("!tahanoun_minha", setOf("chabbat")))
        assertFalse(SiddurJour.vrai("!tahanoun_minha", setOf("tahanoun_minha")))
        assertTrue(SiddurJour.vrai("roch_hodech", setOf("roch_hodech")))
        assertEquals("ajout_sans_tahanoun", SiddurJour.libelle("!tahanoun_chaharit"))
        assertEquals("fete_yom_kippour", SiddurJour.libelle("kippour"))
    }
}
