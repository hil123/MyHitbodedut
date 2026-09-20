package com.myhitbodedut

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Les dix conseils doivent exister dans les trois langues. Lint traite une
 * traduction manquante comme une erreur, donc un oubli casserait la
 * compilation ; ce test le signale plus tôt et plus clairement.
 */
class TipsTest {

    private val locales = listOf("values", "values-en", "values-iw")

    private fun keys(locale: String): Set<String> {
        val text = File("src/main/res/$locale/strings.xml").readText()
        return Regex("""<string name="(tip_[0-9]+_(?:title|body)|tips_[a-z]+)"""")
            .findAll(text).map { it.groupValues[1] }.toSet()
    }

    @Test
    fun `les dix conseils sont complets dans chaque langue`() {
        val expected = (1..10).flatMap { listOf("tip_${it}_title", "tip_${it}_body") } +
            listOf("tips_title", "tips_hint", "tips_open")
        locales.forEach { locale ->
            val found = keys(locale)
            val missing = expected.filterNot { found.contains(it) }
            assertTrue("$locale : clés manquantes $missing", missing.isEmpty())
        }
    }

    @Test
    fun `aucune langue ne porte de conseil en trop`() {
        val reference = keys("values")
        locales.forEach { locale ->
            assertEquals("$locale diverge de la langue par défaut", reference, keys(locale))
        }
    }

    @Test
    fun `aucun conseil n est vide`() {
        locales.forEach { locale ->
            val text = File("src/main/res/$locale/strings.xml").readText()
            (1..10).forEach { i ->
                listOf("tip_${i}_title", "tip_${i}_body").forEach { key ->
                    val value = Regex("""<string name="$key">([^<]*)</string>""")
                        .find(text)?.groupValues?.get(1).orEmpty()
                    assertTrue("$locale/$key est vide", value.trim().length > 10)
                }
            }
        }
    }
}
