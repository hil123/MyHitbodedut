package com.myhitbodedut

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * L'écran annonce que tout reste sur ce téléphone. La sauvegarde automatique
 * d'Android ne doit donc pas emporter le journal ni les notes, tout en gardant
 * les réglages. Elle travaille par fichier, d'où deux fichiers distincts.
 *
 * La garantie ne s'écrit pas avec un <exclude> : dès qu'un <include> existe,
 * tout le reste est écarté, et lint refuse un <exclude> hors des chemins
 * inclus. Ce test vérifie donc la propriété réelle : les réglages sont inclus,
 * le journal n'apparaît dans aucune inclusion.
 */
class BackupRulesTest {

    private val settingsFile = "myhitbodedut.xml"
    private val journalFile = "myhitbodedut-journal.xml"
    private val files = listOf("backup_rules.xml", "data_extraction_rules.xml")

    private fun read(name: String) = File("src/main/res/xml/$name").readText()

    private fun includedPaths(text: String): List<String> =
        Regex("""<include[^>]*path="([^"]+)"""").findAll(text).map { it.groupValues[1] }.toList()

    @Test
    fun `les reglages sont sauvegardes`() {
        files.forEach { file ->
            assertTrue(
                "$file devrait inclure $settingsFile",
                includedPaths(read(file)).contains(settingsFile)
            )
        }
    }

    @Test
    fun `le journal n est inclus nulle part`() {
        files.forEach { file ->
            assertFalse(
                "$file ne doit pas inclure le journal",
                includedPaths(read(file)).any { it.contains("journal") }
            )
        }
    }

    @Test
    fun `le transfert d appareil applique la meme regle`() {
        val transfer = read("data_extraction_rules.xml").substringAfter("<device-transfer>")
        assertTrue("le transfert devrait inclure les réglages", transfer.contains(settingsFile))
        assertFalse("le transfert ne doit pas inclure le journal", transfer.contains(journalFile))
    }
}
