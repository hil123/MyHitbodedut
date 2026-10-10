package com.myhitbodedut

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * La police des textes saints en hébreu (demande du 04/10) : livrée avec sa
 * licence, et posée partout où un texte saint s'affiche.
 */
class PoliceHebraiqueTest {

    private fun lire(chemin: String) = File("src/main/$chemin").readText()

    @Test
    fun `la police est livree avec sa licence`() {
        listOf("hebreu_serif_regular.ttf", "hebreu_serif_bold.ttf").forEach {
            val f = File("src/main/res/font/$it")
            assertTrue("$it absent", f.isFile && f.length() > 10_000)
        }
        val famille = lire("res/font/hebreu_serif.xml")
        assertTrue(famille.contains("@font/hebreu_serif_regular") && famille.contains("@font/hebreu_serif_bold"))
        assertTrue(lire("assets/licences/OFL-NotoSerifHebrew.txt").contains("SIL Open Font License"))
    }

    @Test
    fun `les lignes d'etat de l'accueil sont en Varela Round, livree avec sa licence`() {
        val f = File("src/main/res/font/varela_round.ttf")
        assertTrue(f.isFile && f.length() > 100_000)
        assertTrue(lire("assets/licences/OFL-VarelaRound.txt").contains("SIL Open Font License"))
        val accueil = lire("res/layout/activity_main.xml")
        listOf("status_title", "status_streak", "status_separator", "status_total", "today_detail").forEach { id ->
            val bloc = accueil.substringAfter("@+id/$id\"").substringBefore("/>")
            assertTrue("$id sans Varela Round", bloc.contains("@font/varela_round"))
        }
    }

    @Test
    fun `chaque ecran de texte saint pose la police`() {
        listOf(
            "TehilimLectureActivity.kt", "TikkunFlowActivity.kt", "PsalmActivity.kt",
            "LiturgyActivity.kt", "LibraryEntryActivity.kt", "TikkunActivity.kt",
        ).forEach { assertTrue("$it sans la police hébraïque", lire("java/com/myhitbodedut/$it").contains("PoliceHebraique.appliquer(")) }
        listOf(
            "view_chizuk.xml", "view_chizuk_compact.xml", "view_hitbodedut.xml",
            "item_library_section.xml", "item_library_subsection.xml", "activity_psalm.xml",
        ).forEach { assertTrue("$it sans la police hébraïque", lire("res/layout/$it").contains("@font/hebreu_serif")) }
    }
}
