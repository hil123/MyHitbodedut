package com.myhitbodedut

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Le Tikoun HaKlali : l'ordre des dix psaumes, et l'integrite du texte.
 *
 * L'ordre n'est pas un detail d'affichage — c'est ce qui fait le Tikoun.
 * Il est donc fige ici plutot que laisse a la bonne tenue du fichier.
 */
class TikkunTest {

    /**
     * Ordre etabli, recoupe le 20/09/2026 sur deux sources independantes
     * (Wikipedia « Tikkun HaKlali » et breslov.org).
     */
    private val ORDRE = listOf(16, 32, 41, 42, 59, 77, 90, 105, 137, 150)

    private val tikkun = DataFiles.tikkun

    @Test
    fun `les dix psaumes sont la, dans l ordre`() {
        assertEquals(ORDRE, tikkun.psaumes.map { it.numero })
    }

    @Test
    fun `chaque psaume porte ses versets, numerotes sans trou`() {
        tikkun.psaumes.forEach { p ->
            assertTrue("${p.id} : aucun verset", p.versets.isNotEmpty())
            assertEquals(
                "${p.id} : numerotation des versets",
                (1..p.versets.size).toList(),
                p.versets.map { it.numero },
            )
            p.versets.forEach {
                assertTrue("${p.id}:${it.numero} vide", it.he.isNotBlank())
            }
        }
    }

    @Test
    fun `le total de versets est celui du texte massoretique`() {
        // 11+11+14+12+18+21+17+45+9+6
        assertEquals(164, tikkun.versets)
    }

    @Test
    fun `les identifiants sont uniques`() {
        val ids = tikkun.psaumes.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun `l edition est du domaine public et vocalisee`() {
        assertEquals("Public Domain", tikkun.edition.licence)
        assertTrue("l'edition doit autoriser le nikkoud", tikkun.edition.nikkoudOk)
    }

    /**
     * Les noms divins sont substitues, comme partout ailleurs dans
     * l'application. Un tetragramme en toutes lettres sur un ecran qu'on
     * jette n'est pas une coquille ordinaire.
     */
    @Test
    fun `aucun nom divin n est ecrit en toutes lettres`() {
        val interdits = listOf("יהוה", "אלהים", "אֱלֹהִים", "אדני")
        tikkun.psaumes.forEach { p ->
            p.versets.forEach { v ->
                val nu = v.he.replace(Regex("[\\u0591-\\u05C7]"), "")
                interdits.forEach { mot ->
                    val nuMot = mot.replace(Regex("[\\u0591-\\u05C7]"), "")
                    assertFalse(
                        "${p.ref}:${v.numero} contient « $nuMot »",
                        nu.contains(nuMot),
                    )
                }
            }
        }
    }

    /** Le qere est retenu : aucune notation « ketiv [qere] » ne doit subsister. */
    @Test
    fun `aucune notation ketiv-qere ne subsiste`() {
        tikkun.psaumes.forEach { p ->
            p.versets.forEach { v ->
                assertFalse("${p.ref}:${v.numero} porte un crochet", v.he.contains("["))
            }
        }
    }

    @Test
    fun `chaque psaume renvoie a sa source`() {
        tikkun.psaumes.forEach { p ->
            assertEquals("Psalms ${p.numero}", p.refSefaria)
            assertTrue("${p.id} : pas de lien", p.url.orEmpty().startsWith("https://"))
        }
    }
}
