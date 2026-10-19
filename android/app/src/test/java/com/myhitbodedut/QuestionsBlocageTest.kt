package com.myhitbodedut

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** « Je ne sais pas quoi dire » et « Commencer simplement » (propositions 3 et 15, 19/10). */
class QuestionsBlocageTest {

    @Test
    fun `dix questions, une a la fois, puis on recommence`() {
        assertEquals(10, QuestionsBlocage.QUESTIONS.size)
        assertEquals(0, QuestionsBlocage.suivante(-1))
        assertEquals(1, QuestionsBlocage.suivante(0))
        assertEquals(0, QuestionsBlocage.suivante(9))
    }

    @Test
    fun `les questions et les quatre entrees existent dans les trois langues et les deux orientations`() {
        val cles = listOf("question_merci", "question_fin", "question_suivante", "question_rang",
            "simplement_parler", "simplement_ecrire", "simplement_prier", "simplement_question")
        listOf("values", "values-en", "values-iw").forEach { d ->
            val xml = File("src/main/res/$d/strings.xml").readText()
            cles.forEach { assertTrue("$d : $it", xml.contains("name=\"$it\"")) }
        }
        listOf("layout", "layout-land").forEach { d ->
            val xml = File("src/main/res/$d/activity_session.xml").readText()
            listOf("session_questions", "session_question_suivante", "session_simplement", "simplement_question")
                .forEach { assertTrue("$d : $it", xml.contains("@+id/$it")) }
        }
    }
}
