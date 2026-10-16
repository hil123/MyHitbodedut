package com.myhitbodedut

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Les icônes des familles (16/10, variante B) : une par famille, au trait, dans le fichier partagé avec le web. */
class IconesTropheesTest {

    private val icones = JSONObject(File("src/main/res/raw/trophees_formes.json").readText()).getJSONObject("icones")

    @Test
    fun `chaque famille a son icone au trait`() {
        Trophees.Famille.entries.forEach { f ->
            assertTrue("${f.cle} sans icône", icones.has(f.icone))
            assertTrue("${f.cle} : un tracé commence par M", icones.getString(f.icone).startsWith("M"))
        }
        assertEquals(Trophees.Famille.entries.size, icones.length())
    }

    @Test
    fun `plus aucun emoji`() {
        Trophees.Famille.entries.forEach { f -> assertTrue(f.icone.all { it.code < 128 }) }
    }
}
