package com.myhitbodedut

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** « Bonjour Avraham » à 3 h du matin (capture du 30/09) : la salutation suit l'heure. */
class SalutationTest {

    @Test
    fun `bonjour le jour bonsoir le soir et la nuit`() {
        mapOf(
            5 to Salutation.Partie.MATIN, 11 to Salutation.Partie.MATIN,
            12 to Salutation.Partie.APRES_MIDI, 17 to Salutation.Partie.APRES_MIDI,
            18 to Salutation.Partie.SOIR, 22 to Salutation.Partie.SOIR,
            0 to Salutation.Partie.SOIR, 3 to Salutation.Partie.SOIR, 4 to Salutation.Partie.SOIR,
        ).forEach { (h, attendu) -> assertEquals("$h h", attendu, Salutation.partie(h)) }
    }

    @Test
    fun `chaque langue nomme les trois moments et le francais dit bonsoir`() {
        mapOf(
            "values" to "Bonsoir, %1\$s",
            "values-en" to "Good evening, %1\$s",
            "values-iw" to "ערב טוב, %1\$s",
        ).forEach { (d, soir) ->
            val xml = File("src/main/res/$d/strings.xml").readText()
            listOf("hero_greeting_morning", "hero_greeting_afternoon").forEach {
                assertTrue("$d : $it manque", xml.contains("name=\"$it\""))
            }
            assertTrue("$d : soir", xml.contains("<string name=\"hero_greeting_evening\">$soir</string>"))
        }
        val accueil = File("src/main/java/com/myhitbodedut/MainActivity.kt").readText()
        assertTrue(accueil.contains("Salutation.chaine("))
    }

    /** Le mot du moment (proposition 7, 19/10) : matin, journée, soir, et la nuit. */
    @Test
    fun `le mot du moment couvre les quatre moments`() {
        assertEquals(R.string.moment_mot_matin, Salutation.mot(5))
        assertEquals(R.string.moment_mot_journee, Salutation.mot(12))
        assertEquals(R.string.moment_mot_soir, Salutation.mot(18))
        assertEquals(R.string.moment_mot_nuit, Salutation.mot(23))
        assertEquals(R.string.moment_mot_nuit, Salutation.mot(2))
        assertTrue(File("src/main/java/com/myhitbodedut/AccueilMoment.kt").readText().contains("Salutation.mot("))
    }
}
