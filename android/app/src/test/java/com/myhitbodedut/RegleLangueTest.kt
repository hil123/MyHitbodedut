package com.myhitbodedut

import java.io.File
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * La règle d'affichage, demandée le 22/09 après un essai sur téléphone :
 * en hébreu rien que de l'hébreu, en anglais l'hébreu et l'anglais, en
 * français l'hébreu et le français. Jamais les trois, jamais une langue
 * étrangère au mode.
 *
 * Ce qui l'a motivée : sur un téléphone en anglais, la note d'une source
 * s'affichait en français, et l'écran de relecture montrait les deux
 * traductions côte à côte.
 */
class RegleLangueTest {

    @Test
    fun `en hebreu, rien que de l hebreu`() {
        assertEquals(setOf("he"), RegleLangue.permises("he"))
        assertEquals(setOf("he"), RegleLangue.permises("iw"))
        assertTrue("le français ne doit pas passer en mode hébreu",
            !RegleLangue.permet("he", "fr"))
        assertTrue("l'anglais ne doit pas passer en mode hébreu",
            !RegleLangue.permet("he", "en"))
        assertTrue("pas de traduction à côté de l'hébreu", !RegleLangue.traduit("iw"))
    }

    @Test
    fun `en anglais, l hebreu et l anglais, pas le francais`() {
        assertEquals(setOf("he", "en"), RegleLangue.permises("en"))
        assertTrue(RegleLangue.permet("en", "he"))
        assertTrue("c'est le défaut que le propriétaire a vu sur son téléphone",
            !RegleLangue.permet("en", "fr"))
    }

    @Test
    fun `en francais, l hebreu et le francais, pas l anglais`() {
        assertEquals(setOf("he", "fr"), RegleLangue.permises("fr"))
        assertTrue(RegleLangue.permet("fr", "he"))
        assertTrue(!RegleLangue.permet("fr", "en"))
    }

    @Test
    fun `aucun repli d une langue sur une autre`() {
        // C'est le cœur de la règle : un texte qui n'existe pas dans la
        // langue du mode ne s'affiche pas, il ne se remplace pas.
        val seulementFrancais = mapOf("fr" to "une note")
        assertEquals("une note", RegleLangue.choisit("fr", seulementFrancais))
        assertNull("l'anglais retombait sur le français", RegleLangue.choisit("en", seulementFrancais))
        assertNull("l'hébreu retombait sur le français", RegleLangue.choisit("he", seulementFrancais))
        assertNull(RegleLangue.choisit("en", mapOf("fr" to "x", "he" to "")))
    }

    @Test
    fun `chaque note de source existe dans les trois langues`() {
        val json = JSONObject(File("src/main/res/raw/bibliotheque.json").readText())
        var vues = 0
        fun marche(n: Any?) {
            when (n) {
                is JSONObject -> {
                    if (n.optString("type") == "source" && n.has("note_fr")
                        && !n.isNull("note_fr")
                    ) {
                        vues++
                        listOf("note_en", "note_he").forEach { cle ->
                            assertTrue(
                                "${n.optString("id")} : $cle manque, la note " +
                                    "disparaîtrait dans cette langue",
                                n.optString(cle).isNotBlank(),
                            )
                        }
                    }
                    n.keys().forEach { marche(n.get(it)) }
                }
                is org.json.JSONArray -> (0 until n.length()).forEach { marche(n.get(it)) }
            }
        }
        marche(json)
        assertTrue("aucune note lue : la lecture est cassée", vues >= 26)
    }

    @Test
    fun `l ecran de relecture ne montre plus deux traductions a la fois`() {
        val src = File("src/main/java/com/myhitbodedut/ProofreadActivity.kt").readText()
        assertTrue(
            "les deux lignes s'affichaient ensemble quelle que soit la langue",
            src.contains("holder.en.visibility = View.GONE"),
        )
        assertTrue("l'écran ne consulte pas la règle", src.contains("RegleLangue.choisit"))
    }

    @Test
    fun `la note d une source passe par la regle`() {
        val src = File("src/main/java/com/myhitbodedut/LibraryEntryActivity.kt").readText()
        assertTrue(
            "la note s'affichait sans égard à la langue de l'interface",
            src.contains("RegleLangue.choisit(ChizukCard.language(this), source.notes)"),
        )
    }

    @Test
    fun `le bouton de langue est dans la premiere carte des reglages`() {
        // Il était ligne 201 sur 338, sous la ligne de flottaison, et le
        // propriétaire a conclu qu'il n'existait pas.
        val layout = File("src/main/res/layout/activity_settings.xml").readText()
        val langue = layout.indexOf("@string/settings_language")
        val premiereAutreCarte = layout.indexOf("@+id/card_gate_state")
        assertTrue("le bouton de langue n'est plus dans le layout", langue > 0)
        assertTrue(
            "le bouton de langue doit précéder toutes les autres cartes",
            langue < premiereAutreCarte,
        )
    }
}
