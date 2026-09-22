package com.myhitbodedut

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Le sujet d'une seance.
 *
 * Ce qu'il faut tenir, et qui se perd facilement : il est FACULTATIF.
 * Le jour ou « aucun » deviendrait un membre de la liste plutot que
 * l'absence de valeur, l'ecran se mettrait a exiger un choix sans que
 * personne l'ait decide.
 */
class ThemeTest {

    @Test
    fun `aucun sujet s ecrit par l absence de valeur`() {
        assertNull("une clé vide ne vaut aucun sujet", ThemeRules.parse(""))
        assertNull(ThemeRules.parse(null))
        assertNull(ThemeRules.parse("   "))
        // Aucun membre de la liste ne doit s'appeler « aucun » ou « none ».
        Theme.values().forEach {
            assertTrue(
                "« ${it.cle} » : la liste ne doit pas contenir de sujet « aucun »",
                it.cle !in listOf("aucun", "none", "", "vide"),
            )
        }
    }

    /**
     * « Autre » n'est pas « aucun » : c'est un sujet choisi, simplement
     * absent de la liste. Les confondre reviendrait a perdre la
     * distinction entre « je n'ai pas voulu le dire » et « c'est autre
     * chose ».
     */
    @Test
    fun `autre est un sujet, pas une absence`() {
        assertEquals(Theme.AUTRE, ThemeRules.parse("autre"))
    }

    /**
     * Une cle inconnue rend null au lieu de rompre : un journal exporte
     * d'une version plus recente peut porter un sujet que celle-ci ignore.
     */
    @Test
    fun `une cle inconnue ne rompt pas`() {
        assertNull(ThemeRules.parse("teshouva_2031"))
        assertEquals(listOf(Theme.PARNASSA to 1),
            ThemeRules.frequents(listOf("parnassa", "inconnu", "")))
    }

    @Test
    fun `les cles sont distinctes et stables`() {
        val cles = Theme.values().map { it.cle }
        assertEquals(cles.size, cles.toSet().size)
        cles.forEach {
            assertTrue("« $it » doit rester une clé simple", Regex("^[a-z_]+$").matches(it))
        }
        // Les cles partent dans le journal : les renommer effacerait le passe.
        assertEquals(
            listOf("emotions", "gratitude", "decisions", "famille", "zivoug",
                   "parnassa", "avoda", "difficultes", "croissance", "autre"),
            cles,
        )
    }

    @Test
    fun `les sujets frequents se comptent du plus au moins`() {
        val journal = listOf("parnassa", "famille", "parnassa", "zivoug", "parnassa", "famille")
        assertEquals(
            listOf(Theme.PARNASSA to 3, Theme.FAMILLE to 2, Theme.ZIVOUG to 1),
            ThemeRules.frequents(journal),
        )
        assertEquals(2, ThemeRules.frequents(journal, max = 2).size)
        assertTrue("sans journal, aucun sujet fréquent",
            ThemeRules.frequents(emptyList()).isEmpty())
    }

    /**
     * Le sujet est inscrit avec la journee des maintenant, meme si rien ne
     * l'affiche encore en retour : une donnee qu'on n'enregistre pas est
     * une donnee qu'on n'aura jamais.
     */
    @Test
    fun `la journee retient les sujets, et l export les emporte`() {
        val store = File("src/main/java/com/myhitbodedut/Store.kt").readText()
        assertTrue(
            "DayRecord doit porter les sujets",
            store.contains("val themes: List<String>"),
        )
        assertTrue(
            "addSession doit accepter un sujet",
            Regex("""fun addSession\([\s\S]{0,200}?theme: Theme\?""").containsMatchIn(store),
        )
        assertTrue(
            "l'import doit réunir les sujets plutôt qu'en perdre",
            store.contains("(existing.themes + incoming.themes).distinct()"),
        )
        // Dans finishSession, et la seulement : l'abandon appelle aussi
        // endSession, plus haut dans le fichier, et comparer des positions
        // sur tout le fichier revenait a comparer deux choses sans rapport.
        val service = File("src/main/java/com/myhitbodedut/SessionService.kt").readText()
        val cloture = service.substringAfter("private fun finishSession")
            .substringBefore("\n    }")
        assertTrue("finishSession introuvable", cloture.isNotEmpty())
        assertTrue(
            "le sujet doit être lu avant que endSession efface la séance",
            cloture.indexOf("Store.sessionTheme(this)") in
                0 until cloture.indexOf("Store.endSession(this)"),
        )
        assertTrue(
            "le sujet doit être transmis à la journée",
            cloture.contains("theme = theme"),
        )
    }

    @Test
    fun `chaque sujet a son libelle dans les trois langues`() {
        val cles = Theme.values().map { "theme_${it.cle}" } +
            listOf("session_theme_title", "session_theme_none",
                   "session_theme_none_short", "session_theme_chosen")
        listOf("values", "values-en", "values-iw").forEach { dossier ->
            val xml = File("src/main/res/$dossier/strings.xml").readText()
            cles.forEach {
                assertTrue("$dossier : « $it » manquant", xml.contains("""name="$it""""))
            }
        }
    }
}
