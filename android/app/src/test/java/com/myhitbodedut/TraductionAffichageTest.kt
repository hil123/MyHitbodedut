package com.myhitbodedut

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Le texte qui atteint l'écran sans passer par strings.xml : les
 * références des données, les durées, les titres des morceaux, et ce que
 * dessinent les services. Le propriétaire a trouvé du français en anglais
 * (28/09) ; ces tests tiennent l'inventaire de l'audit qui a suivi.
 */
class TraductionAffichageTest {

    private fun lire(chemin: String) = File(chemin).readText()

    private val refs: List<String> by lazy {
        listOf("src/main/res/raw/chizuk.json", "src/main/res/raw/bibliotheque.json").flatMap { f ->
            Regex(""""ref": "([^"]+)"""").findAll(lire(f)).map { it.groupValues[1] }.toList()
        }
    }

    @Test
    fun `chaque reference des donnees a sa forme anglaise et hebraique`() {
        assertTrue("aucune référence lue : la lecture est cassée", refs.size > 60)
        val inconnues = refs.filterNot { Reference.connue(it) }.distinct()
        assertEquals("livres absents de Reference.LIVRES : $inconnues", emptyList<String>(), inconnues)
    }

    @Test
    fun `en hebreu une reference ne garde aucune lettre latine ni aucun chiffre`() {
        refs.forEach { ref ->
            val he = Reference.localise(ref, "iw")
            assertFalse("« $ref » → « $he »", Regex("[A-Za-z0-9]").containsMatchIn(he))
        }
        assertEquals("ליקוטי מוהר״ן תניינא, כ״ה", Reference.localise("Likoutei Moharan II, 25", "he"))
        assertEquals("משלי כ״ד, ט״ז", Reference.localise("Michlé 24:16", "he"))
        // En anglais, les chiffres restent.
        assertEquals("Mishlei 24:16", Reference.localise("Michlé 24:16", "en"))
    }

    @Test
    fun `les nombres en lettres hebraiques`() {
        mapOf(
            1 to "א׳", 7 to "ז׳", 10 to "י׳", 15 to "ט״ו", 16 to "ט״ז", 25 to "כ״ה",
            39 to "ל״ט", 100 to "ק׳", 145 to "קמ״ה", 261 to "רס״א", 437 to "תל״ז", 400 to "ת׳",
        ).forEach { (n, attendu) -> assertEquals("$n", attendu, Reference.lettres(n)) }
    }

    @Test
    fun `en anglais une reference ne garde aucune graphie francaise`() {
        val francais = Regex("""Likoutei|Si'hot|'Hayé|Chiv'hei|Michlé|Etsot|Hilkhot| ; """)
        refs.forEach { ref ->
            val en = Reference.localise(ref, "en")
            assertFalse("« $ref » → « $en »", francais.containsMatchIn(en))
        }
        assertEquals("Likutei Moharan II, 78", Reference.localise("Likoutei Moharan II, 78", "en"))
        assertEquals("Likoutei Moharan II, 78", Reference.localise("Likoutei Moharan II, 78", "fr"))
    }

    @Test
    fun `chaque ecran qui montre une reference la traduit`() {
        mapOf(
            "ChizukCard.kt" to "Reference.localise(card.context, item.ref)",
            "LibraryEntryActivity.kt" to "Reference.localise(this, source.ref)",
            "LibrarySubSectionActivity.kt" to "Reference.localise(holder.itemView.context, entry.ref)",
            "TefilotActivity.kt" to "Reference.localise(view.context, entree.ref)",
        ).forEach { (fichier, appel) ->
            assertTrue("$fichier affiche la référence brute", lire("src/main/java/com/myhitbodedut/$fichier").contains(appel))
        }
    }

    @Test
    fun `les durees prennent leurs unites dans les chaines`() {
        val bloc = lire("src/main/java/com/myhitbodedut/Store.kt")
            .substringAfter("fun formatDuration").substringBefore("\n    }")
        assertTrue(bloc.contains("R.string.duration_minutes"))
        assertFalse("unité écrite en dur", Regex(""""[^"]*\b(min|h)\b[^"]*"""").containsMatchIn(bloc))
    }

    @Test
    fun `aucun titre de morceau ne reste en francais`() {
        listOf("values", "values-en", "values-iw").forEach { d ->
            assertFalse("$d : « titre à préciser »", lire("src/main/res/$d/strings.xml").contains("titre à préciser"))
        }
        val en = lire("src/main/res/values-en/strings.xml")
        assertTrue(en.contains("""<string name="track_niggoun_01">Niggun 01</string>"""))
    }

    @Test
    fun `les donnees de la bibliotheque ne portent pas de phrase francaise hors des champs fr`() {
        val biblio = lire("src/main/res/raw/bibliotheque.json")
        assertFalse(biblio.contains("cité dans l'article"))
        // Publiés sans nommer les auteurs, avec leur accord (28/09).
        assertFalse("un auteur est nommé", Regex(""""auteur": "[^"]""").containsMatchIn(biblio))
        listOf("סלמה", "Moharach", "מוהרא").forEach { assertFalse("nom « $it » resté", biblio.contains(it)) }
        assertFalse("date ambiguë jour/mois", Regex(""""date": "\d{2}/\d{2}/\d{4}"""").containsMatchIn(biblio))
    }

    /** Avant Android 13, un service garde la langue du téléphone s'il n'est pas enveloppé. */
    @Test
    fun `les services et receveurs parlent la langue de l application`() {
        listOf("GateService.kt", "SessionService.kt").forEach {
            assertTrue("$it : attachBaseContext manque",
                lire("src/main/java/com/myhitbodedut/$it").contains("super.attachBaseContext(LangueApp.envelopper(base))"))
        }
        listOf("BootReceiver.kt", "Reminder.kt", "StreakWidget.kt").forEach {
            assertTrue("$it : contexte non enveloppé",
                lire("src/main/java/com/myhitbodedut/$it").contains("LangueApp.envelopper("))
        }
        assertTrue(lire("src/main/java/com/myhitbodedut/SettingsActivity.kt").contains("Store.setLangueApp("))
    }
}
