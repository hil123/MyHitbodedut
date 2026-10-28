package com.myhitbodedut

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** L'aération des paragraphes (28/10) : mêmes vecteurs que web/test/aeration.test.mjs. */
class AerationTest {

    @Test
    fun premierMot() {
        assertEquals("<b>בָּרוּךְ</b> אַתָּה", Aeration.premierMot("בָּרוּךְ אַתָּה"))
        assertEquals("<b>מוֹדֶה אֲנִי</b> לְפָנֶיךָ", Aeration.premierMot("<b>מוֹדֶה אֲנִי</b> לְפָנֶיךָ"))
        assertEquals("<small>בשבת:</small> רְצֵה", Aeration.premierMot("<small>בשבת:</small> רְצֵה"))
        assertEquals("(בעשי״ת) זָכְרֵנוּ", Aeration.premierMot("(בעשי״ת) זָכְרֵנוּ"))
    }

    @Test
    fun courtResteEntier() {
        val court = "א".repeat(300) + ": " + "ב".repeat(100)
        assertEquals(court, Aeration.couper(court))
    }

    @Test
    fun longCoupeHorsSmall() {
        val phrase = "אבגד ".repeat(60).trim()
        val long = "$phrase: $phrase: <small>${"ה".repeat(260)}: ${"ו".repeat(20)}</small> $phrase"
        val r = Aeration.couper(long)
        assertEquals(3, r.split("<br><br>").size)
        assertFalse(Regex("<small>[^<]*<br><br>").containsMatchIn(r))
        assertEquals(long, r.replace("<br><br>", " "))
    }

    @Test
    fun lesDeuxEnsemble() {
        val phrase = "אבגד ".repeat(60).trim()
        val r = Aeration.aerer("$phrase: $phrase")
        assertTrue(r.startsWith("<b>אבגד</b>"))
        assertTrue(r.contains(":<br><br>אבגד"))
    }
}
