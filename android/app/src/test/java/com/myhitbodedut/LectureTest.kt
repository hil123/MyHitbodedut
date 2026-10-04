package com.myhitbodedut

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.time.LocalTime

/**
 * Le bouton de lecture (demandes du 04/10) : sur tous les écrans de texte
 * saint, aux couleurs choisies, avec la taille des lettres, et qu'on peut
 * faire disparaître tout à fait. Plus rien de la pastille bleue de CalJ.
 */
class LectureTest {

    private fun source(nom: String) = File("src/main/java/com/myhitbodedut/$nom.kt").readText()

    @Test
    fun `tous les ecrans de lecture ont le bouton`() {
        listOf(
            "SiddurLectureActivity", "TehilimLectureActivity", "TikkunFlowActivity",
            "PsalmActivity", "LiturgyActivity", "LibraryEntryActivity",
        ).forEach { assertTrue("$it n'a pas le bouton de lecture", source(it).contains("ReglagesLecture.installer(")) }
        // Le nusah ne se change que dans le siddour.
        assertTrue(source("SiddurLectureActivity").contains("avecNusah = true"))
        assertFalse(source("TehilimLectureActivity").contains("avecNusah = true"))
    }

    @Test
    fun `le bouton prend les couleurs choisies, plus le bleu fixe`() {
        val code = source("ReglagesLecture")
        assertTrue(code.contains("Apparence.couleur(contexte, R.color.brand)"))
        assertTrue(code.contains("Apparence.couleur(contexte, R.color.on_brand)"))
        assertFalse(code.contains("0xFF"))
        assertFalse(File("src/main/res/values/colors.xml").readText().contains("lecture_bouton"))
        listOf("bg_lecture_pastille", "bg_lecture_colonne", "ic_lecture_ampoule", "bg_nusah_on").forEach {
            assertFalse("$it existe encore", File("src/main/res/drawable/$it.xml").exists())
        }
    }

    @Test
    fun `le bouton se masque tout a fait et revient par un appui long`() {
        val code = source("ReglagesLecture")
        assertTrue(code.contains("Store.setLectureMasquee(contexte, true)"))
        assertTrue(code.contains("override fun onLongPress"))
        assertTrue(code.contains("Store.lectureMasquee(contexte)"))
        assertTrue(source("Store").contains("fun lectureMasquee"))
    }

    @Test
    fun `la taille va de 80 a 200 pour cent par dixiemes`() {
        assertEquals(0.8f, ReglagesLecture.MIN, 0f)
        assertEquals(2.0f, ReglagesLecture.MAX, 0f)
        assertEquals(0.1f, ReglagesLecture.PAS, 0f)
    }

    @Test
    fun `les chaines du bouton existent dans les trois langues`() {
        listOf("values", "values-en", "values-iw").forEach { d ->
            val xml = File("src/main/res/$d/strings.xml").readText()
            listOf(
                "lecture_reglages", "lecture_nuit", "lecture_plus", "lecture_moins", "lecture_replier",
                "lecture_masquer", "lecture_masque_info", "lecture_annuler_masque", "lecture_taille",
                "lecture_pourcent", "lecture_nuit_oui", "lecture_nuit_non", "siddur_maintenant",
            ).forEach { assertTrue("$d : $it", xml.contains("name=\"$it\"")) }
        }
    }

    /** La suggestion de l'heure, en tête du siddour : toujours un texte qui existe, dans chaque nusah. */
    @Test
    fun `la suggestion de l heure existe dans les trois nusahim`() {
        assertEquals("tikoun_hatsot", SiddurMoment.suggestion(LocalTime.of(1, 0)))
        assertEquals("chaharit", SiddurMoment.suggestion(LocalTime.of(8, 0)))
        assertEquals("minha", SiddurMoment.suggestion(LocalTime.of(14, 0)))
        assertEquals("arvit", SiddurMoment.suggestion(LocalTime.of(20, 0)))
        assertEquals("chema_coucher", SiddurMoment.suggestion(LocalTime.of(23, 30)))
        Nusah.entries.forEach { n ->
            val siddur = DataFiles.siddur(n)
            (0 until 24 * 60 step 15).forEach { m ->
                val id = SiddurMoment.suggestion(LocalTime.of(m / 60, m % 60))
                assertTrue("$n : $id", siddur.texte(id) != null)
            }
        }
    }
}
