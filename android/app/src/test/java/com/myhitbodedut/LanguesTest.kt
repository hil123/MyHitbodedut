package com.myhitbodedut

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Le choix de la langue.
 *
 * L'application livre trois traductions mais suivait la langue du téléphone,
 * sans recours : un appareil réglé en anglais ne pouvait pas afficher
 * l'hébreu. Le bouton donne le choix ; ces tests portent sur la règle, sur
 * son câblage et sur ce qui, absent, rendrait le choix oublié au redémarrage.
 */
class LanguesTest {

    private val reglages = File("src/main/java/com/myhitbodedut/SettingsActivity.kt").readText()
    private val manifeste = File("src/main/AndroidManifest.xml").readText()

    @Test
    fun `les trois langues sont offertes, dans l ordre demande`() {
        assertEquals(listOf("iw", "en", "fr"), Langues.TOUTES.map { it.etiquette })
        assertEquals(listOf("עברית", "English", "Français"), Langues.TOUTES.map { it.nom })
    }

    @Test
    fun `chaque langue est nommee dans sa propre langue`() {
        // « Hébreu » écrit en français ne se lit pas par qui cherche l'hébreu.
        assertEquals("עברית", Langues.nomDe("iw"))
        assertEquals("English", Langues.nomDe("en"))
        assertEquals("Français", Langues.nomDe("fr"))
    }

    @Test
    fun `he et iw designent la meme langue`() {
        // « he » est le code actuel, « iw » celui qu'Android rend encore.
        // Les confondre afficherait le français à qui a choisi l'hébreu.
        assertEquals("iw", Langues.normalise("he"))
        assertEquals("iw", Langues.normalise("iw"))
        assertEquals("iw", Langues.normalise("he-IL"))
        assertEquals("iw", Langues.normalise("iw_IL"))
        assertEquals(0, Langues.indexDe("he"))
    }

    @Test
    fun `une etiquette regionale ou inconnue ne fait pas dérailler le choix`() {
        assertEquals("fr", Langues.normalise("fr-CA"))
        assertEquals("en", Langues.normalise("en-US"))
        assertEquals(Langues.DEFAUT, Langues.normalise("de"))
        assertEquals(Langues.DEFAUT, Langues.normalise(null))
        assertEquals(Langues.DEFAUT, Langues.normalise(""))
        assertTrue("un index hors liste ferait planter la liste de choix",
            Langues.indexDe("de") in Langues.TOUTES.indices)
    }

    @Test
    fun `chaque langue offerte a son dossier de ressources`() {
        Langues.TOUTES.forEach { langue ->
            val dossier = if (langue.etiquette == Langues.DEFAUT) "values"
                          else "values-${langue.etiquette}"
            assertTrue(
                "on propose « ${langue.nom} » sans livrer $dossier/strings.xml",
                File("src/main/res/$dossier/strings.xml").isFile,
            )
        }
    }

    @Test
    fun `le bouton ouvre le choix et affiche la langue courante`() {
        assertTrue("le bouton n'est relié à rien",
            reglages.contains("R.id.language_button).setOnClickListener { askLanguage() }"))
        assertTrue("le bouton n'affiche pas la langue courante",
            reglages.contains("R.id.language_button).text = Langues.nomDe"))
        assertTrue("le choix n'est pas appliqué",
            reglages.contains("AppCompatDelegate.setApplicationLocales("))
    }

    @Test
    fun `le choix survit a la fermeture sous Android 13`() {
        // Sans ce service, AppCompat n'enregistre rien en dessous d'Android 13
        // et la langue choisie serait oubliée au redémarrage — en silence.
        assertTrue(
            "AppLocalesMetadataHolderService manque au manifeste",
            manifeste.contains("androidx.appcompat.app.AppLocalesMetadataHolderService"),
        )
        assertTrue(
            "le service est là mais autoStoreLocales n'est pas à true",
            Regex("""AppLocalesMetadataHolderService[\s\S]{0,400}?autoStoreLocales[\s\S]{0,120}?"true"""")
                .containsMatchIn(manifeste),
        )
    }

    @Test
    fun `les langues livrees sont declarees au systeme`() {
        val config = File("src/main/res/xml/locales_config.xml").readText()
        assertTrue("locales_config n'est pas déclaré au manifeste",
            manifeste.contains("""android:localeConfig="@xml/locales_config""""))
        Langues.TOUTES.forEach {
            assertTrue(
                "« ${it.nom} » manque à locales_config : Android 13 ne la proposerait pas",
                config.contains("""android:name="${it.etiquette}""""),
            )
        }
    }
}
