package com.myhitbodedut

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Les langues de lecture (04/11) : six façons de lire, la traduction du domaine public de
 * Tehilim, du Tikoun, d'Esther et de Chir HaChirim, et l'hébreu quand il n'y en a pas.
 */
class LanguesTexteTest {

    @Test
    fun `par defaut, la regle d avant`() {
        assertEquals(LanguesTexte.Mode.HE, LanguesTexte.defaut("he"))
        assertEquals(LanguesTexte.Mode.HE, LanguesTexte.defaut("iw"))
        assertEquals(LanguesTexte.Mode.HE_FR, LanguesTexte.defaut("fr"))
        assertEquals(LanguesTexte.Mode.HE_EN, LanguesTexte.defaut("en"))
    }

    @Test
    fun `les textes dans l ordre du mode, l hebreu faute de traduction`() {
        val m = LanguesTexte.Mode.FR_EN
        assertEquals(listOf("fr" to "un", "en" to "one"), LanguesTexte.textes(m, "אחד", "un", "one"))
        assertEquals(listOf("he" to "אחד"), LanguesTexte.textes(LanguesTexte.Mode.FR, "אחד", null, "one"))
        assertEquals(listOf("he" to "אחד", "en" to "one"), LanguesTexte.textes(LanguesTexte.Mode.HE_EN, "אחד", "un", "one"))
        assertTrue(LanguesTexte.manque(LanguesTexte.Mode.HE_FR, aTraduction = false))
        assertTrue(!LanguesTexte.manque(LanguesTexte.Mode.HE, aTraduction = false))
    }

    @Test
    fun `tehilim traduit verset par verset, domaine public`() {
        val t = TehilimLoader.parse(DataFiles.tehilimJson)
        (1..150).forEach { n ->
            listOf("fr", "en").forEach { l ->
                (1..t.versets(n).size).forEach { v -> assertTrue("$l $n:$v", !t.traduction(l, n, v).isNullOrBlank()) }
                assertEquals(null, t.traduction(l, n, t.versets(n).size + 1))
            }
        }
        assertTrue(t.traduction("fr", 23, 1)!!.contains("Éternel est mon berger"))
        assertTrue(t.traduction("en", 23, 1)!!.contains("The LORD is my shepherd"))
        val meta = JSONObject(DataFiles.tehilimJson).getJSONObject("traductions")
        listOf("fr", "en").forEach { assertEquals("Public Domain", meta.getJSONObject(it).getJSONObject("edition").getString("licence")) }
    }

    @Test
    fun `le tikkun porte la meme traduction que tehilim`() {
        val t = TehilimLoader.parse(DataFiles.tehilimJson)
        val tik = JSONObject(DataFiles.tikkunJson).getJSONArray("psaumes")
        for (i in 0 until tik.length()) {
            val p = tik.getJSONObject(i)
            val versets = p.getJSONArray("versets")
            for (k in 0 until versets.length()) {
                val v = versets.getJSONObject(k)
                assertEquals(t.traduction("fr", p.getInt("numero"), v.getInt("n")), v.getString("fr"))
                assertEquals(t.traduction("en", p.getInt("numero"), v.getInt("n")), v.getString("en"))
            }
        }
    }
}
