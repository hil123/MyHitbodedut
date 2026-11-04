package com.myhitbodedut

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.net.URLDecoder

/** La dédicace (04/11) : la demande va à l'équipe, par la messagerie de l'utilisateur. */
class DedicaceTest {

    @Test
    fun `le corps porte la formule hebraique et le nom`() {
        val c = Dedicace.corps("Bonjour,", "Je souhaite dédier ma hitbodedout :", "לעילוי נשמת", " Moché ben Sarah ", "", "Envoyé depuis l’application MyHitbodedut.")
        assertEquals("Bonjour,\n\nJe souhaite dédier ma hitbodedout :\nלעילוי נשמת Moché ben Sarah\n\nEnvoyé depuis l’application MyHitbodedut.", c)
        val autre = Dedicace.corps("Hello,", "I would like to dedicate my study:", null, "Sarah bat Rivka", "Merci", "Sent.")
        assertTrue(autre.contains("\nSarah bat Rivka\n\nMerci\n"))
    }

    @Test
    fun `le lien va a l equipe, encode sans plus`() {
        val l = Dedicace.lien("Dédicace – Mon étude", "a b\nc & d")
        assertTrue(l.startsWith("mailto:myhitbodedut26@gmail.com?subject="))
        assertFalse(l.contains("+"))
        val corps = URLDecoder.decode(l.substringAfter("&body="), "UTF-8")
        assertEquals("a b\nc & d", corps)
    }

    @Test
    fun `les reglages et le site y menent`() {
        assertTrue(File("src/main/java/com/myhitbodedut/SettingsActivity.kt").readText().contains("Dedicace.ouvrir(this)"))
        val site = File("../../web/src/js/regles.js").readText()
        assertTrue("le site écrit à la même adresse", site.contains("myhitbodedut26@gmail.com"))
    }
}
