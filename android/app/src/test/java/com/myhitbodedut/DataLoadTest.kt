package com.myhitbodedut

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Point 2 : les deux fichiers se chargent, et leurs identifiants sont uniques. */
class DataLoadTest {

    @Test
    fun `le fichier chizuk se charge`() {
        val chizuk = DataFiles.chizuk
        assertEquals("0.2", chizuk.version)
        assertTrue("aucun item chargé", chizuk.items.isNotEmpty())
        assertTrue("aucune édition chargée", chizuk.editions.isNotEmpty())
    }

    @Test
    fun `les identifiants chizuk sont uniques`() {
        val ids = DataFiles.chizuk.items.map { it.id }
        assertEquals(doublons(ids).toString(), ids.size, ids.toSet().size)
    }

    @Test
    fun `chaque item chizuk porte au moins un moment connu`() {
        DataFiles.chizuk.items.forEach { item ->
            assertTrue("${item.id} : aucun moment reconnu", item.moments.isNotEmpty())
        }
    }

    @Test
    fun `chaque item chizuk designe une edition declaree`() {
        val chizuk = DataFiles.chizuk
        chizuk.items.forEach { item ->
            assertNotNull(
                "${item.id} : édition « ${item.edition} » absente de _meta",
                chizuk.editions[item.edition],
            )
        }
    }

    @Test
    fun `le fichier bibliotheque se charge`() {
        val library = DataFiles.library
        assertEquals("0.1", library.version)
        assertTrue("aucune section chargée", library.sections.isNotEmpty())
        assertTrue("aucune entrée chargée", library.entrees.isNotEmpty())
    }

    @Test
    fun `les identifiants de la bibliotheque sont uniques`() {
        val ids = DataFiles.library.entrees.map { it.id }
        assertEquals(doublons(ids).toString(), ids.size, ids.toSet().size)
    }

    @Test
    fun `les deux natures d entree sont typees`() {
        val entrees = DataFiles.library.entrees
        assertTrue("aucune source", entrees.any { it is SourceEntry })
        assertTrue("aucun article", entrees.any { it is ArticleEntry })
        entrees.filterIsInstance<SourceEntry>().forEach {
            assertTrue("${it.id} : source sans paragraphe", it.paragraphes.isNotEmpty())
        }
        entrees.filterIsInstance<ArticleEntry>().forEach {
            assertTrue("${it.id} : article sans titre", it.titre.isNotBlank())
        }
    }

    @Test
    fun `la sous-section visee par la specification existe`() {
        val library = DataFiles.library
        val section = library.sections.first { it.id == "sichat-chaverim" }
        assertEquals("שיחת חברים", section.titre.he)
        val sub = library.subSection("hitbodedut-ma-velama")
        assertNotNull("sous-section « התבודדות מה ולמה » absente", sub)
        assertEquals("התבודדות מה ולמה", sub!!.titre.he)
    }

    private fun doublons(ids: List<String>) =
        ids.groupBy { it }.filterValues { it.size > 1 }.keys
}
