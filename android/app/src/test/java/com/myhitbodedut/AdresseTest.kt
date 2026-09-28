package com.myhitbodedut

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** צדיק / צדקת : le bravo de fin de séance, et le ’hazak d'une séance annulée. */
class AdresseTest {

    @Test
    fun `une cle inconnue ou vide reste sans choix`() {
        assertEquals(Adresse.HOMME, Adresse.parse("m"))
        assertEquals(Adresse.FEMME, Adresse.parse("f"))
        assertNull(Adresse.parse(""))
        assertNull(Adresse.parse(null))
        assertNull(Adresse.parse("x"))
    }

    /** Sans choix, ni « Tsadik » ni « ’Hazak » : deviner le genre se tromperait pour quelqu'un. */
    @Test
    fun `chaque forme a ses propres mots, et l absence de choix reste neutre`() {
        val bravos = listOf(Adresse.bravo(Adresse.HOMME), Adresse.bravo(Adresse.FEMME), Adresse.bravo(null))
        val hazaks = listOf(Adresse.hazak(Adresse.HOMME), Adresse.hazak(Adresse.FEMME), Adresse.hazak(null))
        assertEquals(3, bravos.toSet().size)
        assertEquals(3, hazaks.toSet().size)
        assertEquals(R.string.celebration_neutral, Adresse.bravo(null))
        assertEquals(R.string.annulation_neutral, Adresse.hazak(null))
    }

    @Test
    fun `l hebreu accorde le mot au genre`() {
        val he = File("src/main/res/values-iw/strings.xml").readText()
        listOf(
            "celebration_tsadik" to "צדיק", "celebration_tsadeket" to "צדקת",
            "annulation_hazak" to "חזק!", "annulation_hizki" to "חזקי!",
        ).forEach { (cle, mot) ->
            val texte = Regex("""<string name="$cle">([^<]*)</string>""").find(he)?.groupValues?.get(1)
            assertTrue("$cle devrait contenir « $mot » : $texte", texte?.contains(mot) == true)
        }
    }
}
