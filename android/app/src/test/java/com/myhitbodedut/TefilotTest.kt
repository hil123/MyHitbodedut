package com.myhitbodedut

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Les dix prières de Likoutei Tefilot, une par sujet demandé.
 *
 * Ce que ce test garde : qu'elles soient toutes là, qu'aucune ne soit vide,
 * que chacune renvoie à sa prière et à sa leçon, et surtout que la règle de
 * vocalisation s'applique — l'édition 2021 n'a pas de licence déclarée, donc
 * c'est la forme sans nikkoud qui doit s'afficher.
 */
class TefilotTest {

    private val library = DataFiles.library
    private val sujets = listOf(
        "hitbodedut", "simcha", "uman", "kdusha", "zera",
        "parnassa", "brit", "geula", "zivug", "refua",
    )

    private val tefilot: List<SourceEntry>
        get() = library.subSection("tefilot-lt")
            ?.entrees
            ?.filterIsInstance<SourceEntry>()
            .orEmpty()

    @Test
    fun `les dix sujets demandes sont tous servis`() {
        assertEquals(
            "une prière par sujet, ni plus ni moins",
            sujets.size,
            tefilot.size,
        )
        sujets.forEach { sujet ->
            assertNotNull(
                "aucune prière pour « $sujet »",
                tefilot.firstOrNull { it.id == "src-lt-$sujet" },
            )
        }
    }

    @Test
    fun `aucune priere n est vide ni tronquee`() {
        assertEquals("rien à vérifier : la sous-section est vide", sujets.size, tefilot.size)
        tefilot.forEach { t ->
            assertTrue("${t.id} : aucun paragraphe", t.paragraphes.isNotEmpty())
            t.paragraphes.forEachIndexed { i, p ->
                assertTrue("${t.id} paragraphe $i vide", p.isNotBlank())
                assertFalse("${t.id} paragraphe $i : balise HTML restée", p.contains('<'))
                assertFalse("${t.id} paragraphe $i : en-tête d'éditeur restée", p.contains('['))
            }
        }
    }

    @Test
    fun `chaque priere dit d ou elle vient`() {
        assertEquals("rien à vérifier : la sous-section est vide", sujets.size, tefilot.size)
        tefilot.forEach { t ->
            assertTrue("${t.id} : titre hébreu manquant", t.titreHe.isNotBlank())
            assertTrue(
                "${t.id} : la référence doit nommer Likoutei Tefilot, pas « ${t.ref} »",
                t.ref.startsWith("Likoutei Tefilot "),
            )
            assertEquals("${t.id} : mauvaise édition", "lt", t.edition)
            assertEquals("${t.id} : texte non vérifié", "verifie_texte", t.statutVerif)
        }
    }

    /**
     * Le cœur : la licence de l'édition 2021 n'est pas déclarée, donc sa
     * vocalisation ne se reprend pas. Si quelqu'un passe `lt` à
     * nikkoud_ok = true, ce test tombe.
     */
    @Test
    fun `c est la forme sans nikkoud qui s affiche`() {
        val edition = library.editions["lt"]
        assertNotNull("l'édition lt est absente du fichier", edition)
        assertFalse(
            "licence non déclarée : nikkoud_ok ne peut pas valoir vrai",
            edition!!.nikkoudOk,
        )
        assertEquals("rien à vérifier : la sous-section est vide", sujets.size, tefilot.size)
        tefilot.forEach { t ->
            assertEquals(
                "${t.id} : il faut autant de paragraphes dans les deux formes",
                t.paragraphes.size,
                t.paragraphesSansNikkoud.size,
            )
            val affiches = LibraryRules.paragraphs(library, t)
            assertEquals(
                "${t.id} : c'est la forme sans nikkoud qui doit sortir",
                t.paragraphesSansNikkoud,
                affiches,
            )
            affiches.forEach { p ->
                assertFalse(
                    "${t.id} : un signe de vocalisation a survécu dans « ${p.take(40)} »",
                    p.any { it.code in 0x0591..0x05BD || it.code in 0x05BF..0x05C7 },
                )
            }
        }
    }

    /**
     * Le maqqef (U+05BE) est à l'intérieur de la plage des signes hébreux
     * mais n'est pas un signe de vocalisation : le retirer soude les mots.
     * L'erreur a déjà été commise une fois, elle ne se refera pas en silence.
     */
    @Test
    fun `le maqqef survit au retrait du nikkoud`() {
        val avec = tefilot.sumOf { t -> t.paragraphes.sumOf { p -> p.count { it == '־' } } }
        val sans = tefilot.sumOf { t -> t.paragraphesSansNikkoud.sumOf { p -> p.count { it == '־' } } }
        assertTrue("aucun maqqef dans le corpus : le test ne prouve rien", avec > 0)
        assertEquals("des maqqef ont été perdus en retirant le nikkoud", avec, sans)
    }
}
