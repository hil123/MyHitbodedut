package com.myhitbodedut

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Chaque édition dit sous quelle licence ses textes sont livrés (29/10,
 * PUBLICATION_PLAY_STORE.md § 3.3) : Google Play ne s'accommode pas d'un
 * « unknown ».
 *
 * Quatre éditions de Sefaria n'ont pas de licence déclarée. L'œuvre, de
 * Rabbi Natan de Breslev (m. 1844), est du domaine public ; seule l'est moins
 * la vocalisation de l'éditeur moderne. Elles sont donc livrées en
 * « texte seul », et ce test tombe si l'une d'elles reprend sa vocalisation.
 */
class LicencesEditionsTest {

    private val editions = DataFiles.chizuk.editions.values + DataFiles.library.editions.values

    @Test
    fun `aucune edition sans licence`() {
        assertTrue("rien à vérifier : aucune édition lue", editions.isNotEmpty())
        editions.forEach { e ->
            assertTrue("${e.key} : licence vide", e.licence.isNotBlank())
            assertFalse("${e.key} : licence « unknown »", e.licence.equals("unknown", ignoreCase = true))
        }
    }

    @Test
    fun `une edition en texte seul ne reprend pas sa vocalisation`() {
        val texteSeul = editions.filter { it.licence.contains("texte seul") }
        assertTrue("les quatre éditions en texte seul ont disparu", texteSeul.map { it.key }.toSet() == setOf("sh", "shr", "le", "lt"))
        texteSeul.forEach { e ->
            assertFalse("${e.key} : édition en texte seul, nikkoud_ok doit rester faux", e.nikkoudOk)
        }
    }
}
