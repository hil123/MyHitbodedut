package com.myhitbodedut

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Les livres entiers de la bibliothèque (12/10, tools/livres_construction.py) :
 * Likoutei Etsot sans la vocalisation de licence non déclarée, Sefer HaMiddot
 * vocalisé (Public Domain), chacun lisible par le lecteur du siddour.
 */
class LivresTest {

    private val livres = SiddurLoader.parse(File("src/main/res/raw/livres.json").readText())

    @Test
    fun `les deux livres sont la, chapitre par chapitre`() {
        assertEquals(listOf("likoutei_etsot", "sefer_hamidot"), livres.textes.map { it.id })
        livres.textes.forEach { t ->
            assertTrue(t.id, t.sections.size > 50)
            assertTrue(t.id, t.sections.all { it.titre.isNotBlank() && it.blocs.isNotEmpty() })
            assertTrue(t.id, t.titre.he.isNotBlank() && t.titre.fr.isNotBlank() && t.titre.en.isNotBlank())
        }
    }

    @Test
    fun `likoutei etsot sans nikkoud, sefer hamiddot vocalise`() {
        val voyelles = Regex("[\\u05B0-\\u05BC\\u05C1\\u05C2\\u05C7]")
        val etsot = livres.texte("likoutei_etsot")!!.sections.flatMap { it.blocs }
        assertTrue(etsot.none { voyelles.containsMatchIn(it.texte) })
        val midot = livres.texte("sefer_hamidot")!!.sections.flatMap { it.blocs }.filter { !it.consigne }
        assertTrue(midot.count { voyelles.containsMatchIn(it.texte) } > midot.size * 9 / 10)
    }
}
