package com.myhitbodedut

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * L'écran annonce que le journal ne part vers aucun serveur. La sauvegarde
 * automatique d'Android ne doit donc pas emporter le journal ni les notes,
 * tout en gardant les réglages. Elle travaille par fichier, d'où deux
 * fichiers distincts. Le transfert direct vers un nouveau téléphone, lui,
 * emporte le journal.
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

    /** Tout ce qui part vers un serveur : l'ancienne sauvegarde, et le nuage. */
    private fun versLeNuage(): List<String> =
        includedPaths(read("backup_rules.xml")) +
            includedPaths(
                read("data_extraction_rules.xml")
                    .substringAfter("<cloud-backup>").substringBefore("</cloud-backup>")
            )

    @Test
    fun `le journal ne part jamais dans le nuage`() {
        assertFalse(
            "le journal ne doit pas partir vers un serveur",
            versLeNuage().any { it.contains("journal") }
        )
    }

    /**
     * Le transfert d'un téléphone à l'autre va directement de l'un à
     * l'autre, sans serveur. Le journal y passe : l'en exclure faisait
     * perdre tout l'historique à chaque changement de téléphone (revue du
     * 29/09).
     */
    @Test
    fun `le transfert d appareil emporte reglages et journal`() {
        val transfer = read("data_extraction_rules.xml")
            .substringAfter("<device-transfer>").substringBefore("</device-transfer>")
        val chemins = includedPaths(transfer)
        assertTrue("le transfert devrait inclure les réglages", chemins.contains(settingsFile))
        assertTrue("le transfert devrait inclure le journal", chemins.contains(journalFile))
    }
}
