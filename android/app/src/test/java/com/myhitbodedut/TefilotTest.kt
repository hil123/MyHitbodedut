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
    fun `les dix sujets demandes ont chacun au moins deux prieres`() {
        sujets.forEach { sujet ->
            assertTrue(
                "« $sujet » : il faut au moins deux prières, il y en a " +
                    "${tefilot.count { it.sujet == sujet }}",
                tefilot.count { it.sujet == sujet } >= 2,
            )
            listOf("src-lt-$sujet", "src-lt-$sujet-2").forEach { id ->
                assertNotNull("entrée manquante : $id", tefilot.firstOrNull { it.id == id })
            }
        }
        assertEquals(
            "des prières traînent hors des dix sujets",
            tefilot.size,
            tefilot.count { it.sujet in sujets },
        )
    }

    /**
     * Deux prières sur le même sujet doivent être deux prières différentes.
     * Sans ce test, dupliquer la première passerait inaperçu.
     */
    @Test
    fun `les prieres d un meme sujet sont toutes differentes`() {
        sujets.forEach { sujet ->
            val siennes = tefilot.filter { it.sujet == sujet }
            assertEquals("« $sujet » : deux prières partagent une référence",
                siennes.size, siennes.map { it.ref }.distinct().size)
            assertEquals("« $sujet » : deux prières partagent leur texte",
                siennes.size, siennes.map { it.paragraphes }.distinct().size)
        }
    }

    /**
     * Le constat honnête : la leçon annoncée porte-t-elle sur le sujet ?
     * Trois réponses seulement, et aucune entrée n'a le droit de se taire.
     */
    @Test
    fun `chaque priere dit si sa lecon porte sur le sujet`() {
        assertTrue("rien à vérifier : la sous-section est vide", tefilot.size >= sujets.size * 2)
        tefilot.forEach { t ->
            assertTrue(
                "${t.id} : constat manquant ou hors barème (« ${t.leconSurLeSujet} »)",
                t.leconSurLeSujet in listOf("oui", "partiel", "non"),
            )
        }
        assertTrue(
            "aucune entrée ne dit « non » : le constat ne distingue plus rien",
            tefilot.any { it.leconSurLeSujet == "non" },
        )
    }

    @Test
    fun `aucune priere n est vide ni tronquee`() {
        assertTrue("rien à vérifier : la sous-section est vide", tefilot.size >= sujets.size * 2)
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
        assertTrue("rien à vérifier : la sous-section est vide", tefilot.size >= sujets.size * 2)
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
        assertTrue("rien à vérifier : la sous-section est vide", tefilot.size >= sujets.size * 2)
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
