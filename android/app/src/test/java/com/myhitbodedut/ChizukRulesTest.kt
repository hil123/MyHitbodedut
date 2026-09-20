package com.myhitbodedut

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Point 3 : la règle de publication est bloquante.
 *
 * Ce test est la barrière. Tant qu'il passe, aucune compilation de release
 * ne peut montrer un texte non relevé de la source ou non relu.
 */
class ChizukRulesTest {

    private val chizuk = DataFiles.chizuk

    @Test
    fun `la release ne montre que du verifie et relu`() {
        ChizukRules.visible(chizuk, debug = false).forEach { item ->
            assertEquals(
                "${item.id} visible en release alors que statut_verif vaut « ${item.statutVerif} »",
                ChizukRules.STATUT_PUBLIABLE,
                item.statutVerif,
            )
            assertTrue("${item.id} visible en release alors que relu est faux", item.relu)
        }
    }

    @Test
    fun `la release refuse un texte verifie mais non relu`() {
        val item = gabarit(statut = "verifie_texte", relu = false)
        assertFalse(ChizukRules.isPublishable(item))
        assertTrue(ChizukRules.visible(Chizuk("t", emptyMap(), listOf(item)), false).isEmpty())
    }

    @Test
    fun `la release refuse un texte relu mais non verifie`() {
        val item = gabarit(statut = "a_verifier", relu = true)
        assertFalse(ChizukRules.isPublishable(item))
        assertTrue(ChizukRules.visible(Chizuk("t", emptyMap(), listOf(item)), false).isEmpty())
    }

    @Test
    fun `la release accepte un texte verifie et relu`() {
        val item = gabarit(statut = "verifie_texte", relu = true)
        assertTrue(ChizukRules.isPublishable(item))
        assertEquals(1, ChizukRules.visible(Chizuk("t", emptyMap(), listOf(item)), false).size)
    }

    @Test
    fun `le debug montre tout, et signale ce qui reste a relire`() {
        assertEquals(chizuk.items.size, ChizukRules.visible(chizuk, debug = true).size)
        val aRelire = chizuk.items.filter { ChizukRules.needsProofreading(it) }
        assertEquals(
            chizuk.items.filterNot { ChizukRules.isPublishable(it) }.toSet(),
            aRelire.toSet(),
        )
    }

    @Test
    fun `l hebreu suit le nikkoud de l edition`() {
        chizuk.items.forEach { item ->
            val affiche = ChizukRules.hebrew(chizuk, item)
            val edition = chizuk.editions.getValue(item.edition)
            if (edition.nikkoudOk) {
                assertEquals("${item.id} : devrait afficher he", item.he, affiche)
            } else {
                assertEquals(
                    "${item.id} : devrait afficher he_sans_nikkoud",
                    item.heSansNikkoud,
                    affiche,
                )
            }
            assertTrue("${item.id} : hébreu vide", affiche.isNotBlank())
        }
    }

    @Test
    fun `toute edition sans nikkoud fournit son texte sans nikkoud`() {
        chizuk.items
            .filterNot { chizuk.editions.getValue(it.edition).nikkoudOk }
            .forEach {
                assertNotNull(
                    "${it.id} : édition « ${it.edition} » non vocalisable, he_sans_nikkoud manquant",
                    it.heSansNikkoud,
                )
            }
    }

    @Test
    fun `chaque moment a de quoi tirer en debug`() {
        Moment.entries.forEach { moment ->
            assertTrue(
                "aucun texte pour le moment « ${moment.key} »",
                ChizukRules.forMoment(chizuk, moment, debug = true).isNotEmpty(),
            )
        }
    }

    /**
     * L'écran de porte s'ouvre sur un téléphone qu'on vient d'attraper :
     * _meta limite ce moment aux textes de 15 s de lecture ou moins.
     */
    @Test
    fun `les textes de la porte restent courts`() {
        ChizukRules.forMoment(chizuk, Moment.PORTE, debug = true).forEach {
            assertTrue(
                "${it.id} : ${it.dureeLectureSec} s, trop long pour l'écran de porte",
                it.dureeLectureSec <= 15,
            )
        }
    }

    private fun gabarit(statut: String, relu: Boolean) = ChizukItem(
        id = "t", moments = listOf(Moment.OUVERTURE), theme = null, edition = "lm",
        ref = "ref", refSefaria = null, url = null, he = "he", heSansNikkoud = null,
        fr = "fr", en = "en", note = null, dureeLectureSec = 5,
        statutVerif = statut, relu = relu,
    )
}
