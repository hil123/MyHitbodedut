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
            }
        }
    }

    @Test
    fun `les ajouts principaux sont annotes dans les trois nusah`() {
        Nusah.entries.forEach { n ->
            val chaharit = SiddurJour.parse(json, n.cle).getValue("chaharit").map { it.condition }.toSet()
            listOf("yaale_veyavo", "al_hanissim", "hanouka", "pourim", "tal_oumatar", "berakha", "asseret_yemei_techouva")
                .forEach { assertTrue("${n.cle} : $it", it in chaharit) }
            val repas = SiddurJour.parse(json, n.cle).getValue("birkat_hamazon").map { it.condition }.toSet()
            listOf("yaale_veyavo", "al_hanissim", "chabbat").forEach { assertTrue("${n.cle} repas : $it", it in repas) }
        }
    }

    @Test
    fun `roch hodech - yaale veyavo en evidence, al hanissim replie puis ouvert`() {
        val texte = DataFiles.siddur(Nusah.EDOT).texte("chaharit")!!
        val annotations = SiddurJour.parse(json, "edot").getValue("chaharit")
        val rc = CalendrierJuif.jour(java.time.LocalDate.parse("2026-10-12"), false).conditions
        val lignes = SiddurJour.lignes(texte, annotations, rc)
        assertTrue(SiddurJour.Ligne.Etiquette("yaale_veyavo") in lignes)
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
    fun `une condition niee s applique quand la cle manque`() {
        assertTrue(SiddurJour.vrai("!tahanoun_minha", setOf("chabbat")))
        assertFalse(SiddurJour.vrai("!tahanoun_minha", setOf("tahanoun_minha")))
        assertTrue(SiddurJour.vrai("roch_hodech", setOf("roch_hodech")))
        assertEquals("ajout_sans_tahanoun", SiddurJour.libelle("!tahanoun_chaharit"))
        assertEquals("fete_yom_kippour", SiddurJour.libelle("kippour"))
    }
}
