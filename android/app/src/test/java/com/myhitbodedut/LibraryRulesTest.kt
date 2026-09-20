package com.myhitbodedut

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Point 5 : test bloquant — aucun texte_integral affiché si autorisation
 * est null.
 */
class LibraryRulesTest {

    private val library = DataFiles.library

    @Test
    fun `aucun article livre ne montre son texte integral`() {
        library.entrees.filterIsInstance<ArticleEntry>().forEach { article ->
            assertNull(
                "${article.id} : texte intégral affiché alors que autorisation vaut " +
                    "« ${article.autorisation} »",
                LibraryRules.fullText(article),
            )
        }
    }

    @Test
    fun `sans autorisation, le texte reste cache meme s il est present`() {
        val article = gabarit(autorisation = null, texte = "le texte complet de l'article")
        assertNull(LibraryRules.fullText(article))
    }

    @Test
    fun `une autorisation vide ne vaut pas autorisation`() {
        assertNull(LibraryRules.fullText(gabarit(autorisation = "", texte = "texte")))
        assertNull(LibraryRules.fullText(gabarit(autorisation = "   ", texte = "texte")))
    }

    @Test
    fun `avec autorisation, le texte s affiche`() {
        val article = gabarit(autorisation = "accord écrit de l'auteur, 20/09/2026", texte = "texte")
        assertEquals("texte", LibraryRules.fullText(article))
    }

    @Test
    fun `une autorisation sans texte ne fabrique rien`() {
        assertNull(LibraryRules.fullText(gabarit(autorisation = "accord", texte = null)))
    }

    @Test
    fun `les paragraphes suivent le nikkoud de l edition`() {
        library.entrees.filterIsInstance<SourceEntry>().forEach { source ->
            val affiches = LibraryRules.paragraphs(library, source)
            val edition = library.editions.getValue(source.edition)
            if (edition.nikkoudOk) {
                assertEquals("${source.id}", source.paragraphes, affiches)
            } else {
                assertTrue(
                    "${source.id} : édition non vocalisable sans paragraphes_sans_nikkoud",
                    source.paragraphesSansNikkoud.isNotEmpty(),
                )
                assertEquals("${source.id}", source.paragraphesSansNikkoud, affiches)
            }
            assertTrue("${source.id} : aucun paragraphe à afficher", affiches.isNotEmpty())
        }
    }

    @Test
    fun `chaque article a un resume`() {
        library.entrees.filterIsInstance<ArticleEntry>().forEach {
            assertNotNull("${it.id} : pas de résumé", LibraryRules.summary(it, "fr"))
        }
    }

    @Test
    fun `une langue sans resume retombe sur celui qui existe`() {
        val article = gabarit(autorisation = null, texte = null)
        assertEquals("résumé", LibraryRules.summary(article, "en"))
    }

    @Test
    fun `les sources passent avant les articles`() {
        val ordre = LibraryRules.ordered(library.subSection("hitbodedut-ma-velama")!!.entrees)
        val dernierSource = ordre.indexOfLast { it is SourceEntry }
        val premierArticle = ordre.indexOfFirst { it is ArticleEntry }
        assertTrue("les deux natures se mélangent", dernierSource < premierArticle)
        assertEquals(library.subSection("hitbodedut-ma-velama")!!.entrees.size, ordre.size)
    }

    private fun gabarit(autorisation: String?, texte: String?) = ArticleEntry(
        id = "t", titre = "titre", auteur = null, site = null, date = null, url = null,
        resumes = mapOf("fr" to "résumé"), droits = null, autorisation = autorisation,
        texteIntegral = texte, sourcesCitees = emptyList(), erreursRelevees = emptyList(),
    )
}
