package com.myhitbodedut

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MarquesPagesTest {

    private val a = MarquesPages.Marque("s:chaharit", "edot", 3, 12, "ברוך שאמר")
    private val b = MarquesPages.Marque("s:chaharit", "edot", 1, 4, "הודו ליהוה")

    @Test
    fun `poser puis retirer la meme place`() {
        val une = MarquesPages.basculer(emptyList(), a)
        assertEquals(listOf(a), une)
        assertTrue(MarquesPages.basculer(une, a.copy(apercu = "autre")).isEmpty())
    }

    @Test
    fun `aller retour par le texte enregistre`() {
        val l = listOf(a, b, MarquesPages.Marque("l:likoutei_etsot", "", 0, 0, "אמת"))
        assertEquals(l, MarquesPages.decoder(MarquesPages.encoder(l)))
        assertEquals(emptyList<MarquesPages.Marque>(), MarquesPages.decoder(null))
    }

    @Test
    fun `celles du texte dans l ordre du texte et pas d un autre noussah`() {
        val l = listOf(a, b, a.copy(nusah = "sefard"))
        assertEquals(listOf(b, a), MarquesPages.duTexte(l, "s:chaharit", "edot"))
    }

    @Test
    fun `apercu sans voyelles ni balises`() {
        assertEquals("ברוך שאמר והיה העולם", MarquesPages.apercu("<b>בָּרוּךְ</b> שֶׁאָמַר וְהָיָה הָעוֹלָם", 4))
    }

    @Test
    fun `la liste ne grossit pas sans fin`() {
        var l = emptyList<MarquesPages.Marque>()
        repeat(MarquesPages.MAX + 5) { l = MarquesPages.basculer(l, a.copy(bloc = it)) }
        assertEquals(MarquesPages.MAX, l.size)
        assertEquals(MarquesPages.MAX + 4, l.first().bloc)
    }
}
