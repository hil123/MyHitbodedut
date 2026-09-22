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
    fun `chaque article a un resume dans les trois langues`() {
        library.entrees.filterIsInstance<ArticleEntry>().forEach { article ->
            listOf("fr", "en", "he").forEach { langue ->
                assertNotNull(
                    "${article.id} : pas de résumé en $langue",
                    LibraryRules.summary(article, langue),
                )
            }
        }
    }

    @Test
    fun `chaque article a ses droits dans les trois langues`() {
        library.entrees.filterIsInstance<ArticleEntry>().forEach { article ->
            listOf("fr", "en", "he").forEach { langue ->
                assertNotNull(
                    "${article.id} : pas de droits en $langue",
                    LibraryRules.rights(article, langue),
                )
            }
        }
    }

    @Test
    fun `l article avec des erreurs relevees les porte dans les trois langues`() {
        val avecErreurs = library.entrees.filterIsInstance<ArticleEntry>()
            .filter { it.erreursRelevees.values.any { l -> l.isNotEmpty() } }
        assertTrue("aucun article ne porte d'erreurs relevées : la lecture est cassée", avecErreurs.isNotEmpty())
        avecErreurs.forEach { article ->
            val tailleFr = LibraryRules.errors(article, "fr").size
            assertTrue("${article.id} : liste française vide", tailleFr > 0)
            listOf("en", "he").forEach { langue ->
                assertEquals(
                    "${article.id} : $langue n'a pas le même nombre d'erreurs que le français",
                    tailleFr,
                    LibraryRules.errors(article, langue).size,
                )
            }
        }
    }

    @Test
    fun `un resume dans une langue absente ne retombe plus sur le francais`() {
        // La règle : un texte manquant dans la langue en cours n'est pas
        // remplacé par le français. Vérifié directement sur la règle, pas
        // sur les données — les données, elles, ont les trois langues.
        val article = gabarit(autorisation = null, texte = null)
        assertNull(
            "l'anglais retombait sur le résumé français",
            LibraryRules.summary(article, "en"),
        )
        assertEquals("résumé", LibraryRules.summary(article, "fr"))
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
        resumes = mapOf("fr" to "résumé"), droits = emptyMap(), autorisation = autorisation,
        texteIntegral = texte, sourcesCitees = emptyList(), erreursRelevees = emptyMap(),
    )
}
