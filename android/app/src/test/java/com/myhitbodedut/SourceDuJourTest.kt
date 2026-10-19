package com.myhitbodedut

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.time.LocalDate

/**
 * La source du jour (proposition 10, 19/10) : un paragraphe des sources libres,
 * le même toute la journée, et le même que le web (web/test/regles.test.mjs).
 */
class SourceDuJourTest {

    private val library = DataFiles.library

    private fun ici(jour: String): Pair<String, Int> {
        val p = SourceDuJour.choix(library, LocalDate.parse(jour))!!
        return p.source.id to LibraryRules.paragraphs(library, p.source).indexOf(p.texte)
    }

    @Test
    fun `le meme paragraphe que le web, le meme jour`() {
        assertEquals("src-lm2-25" to 9, ici("2026-10-19"))
        assertEquals("src-lt-zera" to 1, ici("2026-10-20"))
        assertEquals("src-lt-zera" to 2, ici("2027-01-01"))
    }

    @Test
    fun `seules les sources, jamais les articles, et assez longues`() {
        val c = SourceDuJour.candidats(library)
        assertTrue(c.size > 50)
        assertTrue(c.all { it.texte.length >= SourceDuJour.MIN })
        assertTrue(SourceDuJour.extrait("א".repeat(10) + " " + "ב".repeat(400)).endsWith("…"))
        assertTrue(File("src/main/java/com/myhitbodedut/MainActivity.kt").readText().contains("SourceDuJour.carte("))
    }
}
