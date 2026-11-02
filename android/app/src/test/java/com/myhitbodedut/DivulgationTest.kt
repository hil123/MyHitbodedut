package com.myhitbodedut

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * La divulgation bien visible (31/10, politique « Données utilisateur » de
 * Google Play) : aucun chemin ne mène aux réglages Android de l'affichage
 * par-dessus ou des données d'utilisation sans que la fenêtre de Divulgation
 * ait été montrée et que l'utilisateur ait touché « Continuer ».
 *
 * La garde lit tout le code de l'application, toutes éditions confondues :
 * un appel ajouté demain dans l'accueil ou l'onboarding la ferait échouer.
 */
class DivulgationTest {

    private val sources = File("src").walkTopDown()
        .filter { it.isFile && it.extension == "kt" && !it.path.contains("${File.separator}test${File.separator}") }
        .toList()

    /** Les lignes de code (hors commentaires) qui correspondent au motif : « chemin:ligne » et le texte. */
    private fun appelants(motif: Regex): List<Pair<String, String>> = sources.flatMap { f ->
        f.readLines().mapIndexedNotNull { i, ligne ->
            val code = ligne.substringBefore("//")
            if (code.trimStart().startsWith("*") || !motif.containsMatchIn(code)) null
            else "${f.path}:${i + 1}" to code.trim()
        }
    }

    private fun code(f: String) = File(f).readLines()
        .filterNot { it.trimStart().startsWith("*") || it.trimStart().startsWith("/**") || it.trimStart().startsWith("//") }
        .joinToString("\n")

    @Test
    fun `les sources de l application sont bien lues`() {
        assertTrue("aucun fichier lu : la lecture est cassée", sources.size > 100)
        assertTrue(sources.any { it.name == "SettingsActivity.kt" })
    }

    @Test
    fun `les deux intents speciaux ne s appellent que depuis la divulgation`() {
        val fautes = appelants(Regex("""\b(overlayIntent|usageAccessIntent)\s*\(""")).filterNot { (ou, ligne) ->
            // La définition elle-même, et le seul appelant permis.
            (ou.contains("${File.separator}Permissions.kt:") && ligne.startsWith("fun ")) ||
                ou.contains("${File.separator}Divulgation.kt:")
        }.map { it.first }
        assertEquals("ouvre un écran d'accès spécial sans divulgation préalable", emptyList<String>(), fautes)
    }

    @Test
    fun `les ecrans systeme ne se construisent que dans Permissions`() {
        val fautes = appelants(Regex("""ACTION_MANAGE_OVERLAY_PERMISSION|ACTION_USAGE_ACCESS_SETTINGS"""))
            .map { it.first }.filterNot { it.contains("${File.separator}Permissions.kt:") }
        assertEquals("un écran d'accès spécial est construit hors de Permissions", emptyList<String>(), fautes)
    }

    @Test
    fun `dans la divulgation les reglages ne s ouvrent qu au bouton continuer`() {
        val code = code("src/main/java/com/myhitbodedut/Divulgation.kt")
        val continuer = code.substringAfter("setPositiveButton(R.string.divulgation_continuer)")
            .substringBefore(".setNegativeButton")
        listOf("Permissions.overlayIntent(", "Permissions.usageAccessIntent(", "ouvrir(").forEach {
            assertEquals("« $it » doit apparaître une fois, sous « Continuer »", 1, code.split(it).size - 1)
            assertTrue("« $it » hors du bouton « Continuer »", continuer.contains(it))
        }
        // « Pas maintenant » ferme, sans action.
        assertTrue(code.contains(".setNegativeButton(R.string.divulgation_pas_maintenant, null)"))
        // Les réglages passent par la divulgation pour les deux accès spéciaux.
        val reglages = code("src/main/java/com/myhitbodedut/SettingsActivity.kt")
        assertEquals(2, reglages.split("Divulgation.montrer(").size - 1)
    }

    @Test
    fun `chaque acces special a son propre texte et les autres n en ont pas`() {
        val overlay = Divulgation.texte(Autorisation.OVERLAY)
        val usage = Divulgation.texte(Autorisation.USAGE)
        assertNotNull(overlay)
        assertNotNull(usage)
        assertNotEquals(overlay, usage)
        assertNull(Divulgation.texte(Autorisation.NOTIFICATIONS))
        assertNull(Divulgation.texte(Autorisation.BATTERIE))
        // Toutes celles de la barrière ont une divulgation.
        Autorisation.values().filter { it.barriere }.forEach { assertNotNull(it.name, Divulgation.texte(it)) }
    }

    /** Ce que la politique demande de dire, dans chacune des trois langues. */
    @Test
    fun `le texte dit la donnee le motif la confidentialite et la sortie`() {
        val attendus = mapOf(
            "values" to listOf("premier plan", "par-dessus", "barrière du matin", "Rien n’est", "reste sur ce téléphone", "couper la barrière"),
            "values-en" to listOf("foreground", "over other apps", "morning gate", "Nothing is", "stays on this phone", "turn the gate off"),
            "values-iw" to listOf("בחזית", "מעל אפליקציות אחרות", "מחסום הבוקר", "שום דבר לא", "נשאר בטלפון הזה", "לכבות את המחסום"),
        )
        attendus.forEach { (dossier, mots) ->
            val xml = File("src/main/res/$dossier/strings.xml").readText()
            fun chaine(cle: String) = Regex("""<string name="$cle">([^<]*)</string>""").find(xml)?.groupValues?.get(1)
                ?: error("$dossier : $cle manque")
            val overlay = chaine("divulgation_overlay_texte")
            val usage = chaine("divulgation_usage_texte")
            assertTrue("$dossier : l'overlay ne dit pas ce qu'il affiche", overlay.contains(mots[1]))
            assertTrue("$dossier : l'usage ne dit pas ce qu'il lit", usage.contains(mots[0]))
            listOf(overlay, usage).forEach { t ->
                mots.drop(2).forEach { m -> assertTrue("$dossier : « $m » manque dans « $t »", t.contains(m)) }
            }
            chaine("divulgation_continuer")
            chaine("divulgation_pas_maintenant")
        }
    }
}
