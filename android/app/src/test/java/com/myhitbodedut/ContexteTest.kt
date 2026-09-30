package com.myhitbodedut

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Des textes justes, mais pas au bon moment ou pas pour la bonne personne :
 * « Bonjour » à 3 h du matin a ouvert la recherche (30/09). Ces tests
 * tiennent ce qui a été trouvé à sa suite.
 */
class ContexteTest {

    private fun lire(chemin: String) = File(chemin).readText()
    private fun chaines(d: String) = lire("src/main/res/$d/strings.xml")

    /** Une hitbodedout en cours n'est pas « pas encore faite », Shabbat n'attend rien. */
    @Test
    fun `l etat du jour dit en cours et jour sans obligation`() {
        listOf("values", "values-en", "values-iw").forEach { d ->
            listOf("status_running", "status_rest", "widget_rest").forEach {
                assertTrue("$d : $it manque", chaines(d).contains("name=\"$it\""))
            }
        }
        val accueil = lire("src/main/java/com/myhitbodedut/MainActivity.kt")
        assertTrue(accueil.contains("R.string.status_running"))
        assertTrue(accueil.contains("R.string.status_rest"))
        assertTrue(lire("src/main/java/com/myhitbodedut/StreakWidget.kt").contains("R.string.widget_rest"))
    }

    /** L'application vouvoie partout, et ne suppose pas qu'on est un homme. */
    @Test
    fun `le francais vouvoie et reste neutre`() {
        val fr = chaines("values")
        assertFalse("tutoiement", Regex(""">Tu |\bta hitbodedout\b""").containsMatchIn(fr))
        assertFalse(fr.contains("vous seul "))
        assertFalse(fr.contains("Un moment seul "))
        assertFalse("« 1 psaumes dits »", fr.contains("%1\$d psaumes dits"))
    }

    /** L'hébreu s'adresse au pluriel, jamais au masculin singulier seul. */
    @Test
    fun `l hebreu ne s adresse pas au masculin singulier`() {
        val he = chaines("values-iw")
        listOf("שתבחר ", "הפעל את", "בחר לפחות", "בחר משך", "הקש כדי", "אני לא מצליח", "מה שאני מרגיש", "איך אני יכול")
            .forEach { assertFalse("« $it » reste", he.contains(it)) }
    }

    /** « 1 יום » ne se dit pas : « יום אחד ». */
    @Test
    fun `en hebreu le singulier ne porte pas de chiffre`() {
        Regex("""<item quantity="one">([^<]*)</item>""").findAll(chaines("values-iw")).forEach {
            // Le premier argument est le nombre compté ; les autres (« sur 6 ») restent.
            assertFalse("« ${it.groupValues[1]} »", Regex("""%(1\$)?d""").containsMatchIn(it.groupValues[1]))
        }
    }

    /** Les dates suivent la langue de l'application, pas celle du téléphone. */
    @Test
    fun `les dates sont dans la langue de l application`() {
        listOf("MainActivity.kt", "JournalActivity.kt").forEach {
            val code = lire("src/main/java/com/myhitbodedut/$it")
            assertFalse("$it : Locale.getDefault()", code.contains("Locale.getDefault()"))
            assertTrue(code.contains("resources.configuration.locales[0]"))
        }
    }
}
