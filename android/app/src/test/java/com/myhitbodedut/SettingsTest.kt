package com.myhitbodedut

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * L'ecran des reglages, et les autorisations qu'il rassemble.
 *
 * Le decoupage d'un ecran en deux se rate d'une seule facon : une vue
 * cherchee dans le gabarit ou elle n'est plus. findViewById rend null, et
 * l'ecran se ferme des l'ouverture, sans rien dire a la compilation.
 */
class SettingsTest {

    private fun lire(chemin: String) = File(chemin).readText()

    /** Les identifiants d'un gabarit, les vues incluses comprises. */
    private fun idsDe(gabarit: String): Set<String> {
        val xml = lire("src/main/res/layout/$gabarit.xml")
        val propres = Regex("""android:id="@\+id/([a-z0-9_]+)"""")
            .findAll(xml).map { it.groupValues[1] }.toSet()
        val inclus = Regex("""layout="@layout/([a-z0-9_]+)"""")
            .findAll(xml).flatMap { idsDe(it.groupValues[1]).asSequence() }.toSet()
        return propres + inclus
    }

    /**
     * Le test du decoupage : chaque vue qu'un ecran remplit doit exister
     * dans son gabarit, ou dans un gabarit qu'il gonfle lui-meme.
     */
    @Test
    fun `chaque ecran ne cherche que des vues qu il possede`() {
        listOf("MainActivity" to "activity_main",
               "SettingsActivity" to "activity_settings").forEach { (classe, gabarit) ->
            val code = lire("src/main/java/com/myhitbodedut/$classe.kt")
            val cherchees = Regex("""R\.id\.([a-z0-9_]+)""")
                .findAll(code).map { it.groupValues[1] }.toSet()
            val gonfles = Regex("""R\.layout\.([a-z0-9_]+)""")
                .findAll(code).map { it.groupValues[1] }.toSet()
            val disponibles = idsDe(gabarit) + gonfles.flatMap { idsDe(it) }
            val manquantes = cherchees - disponibles
            assertTrue(
                "$classe cherche des vues absentes de $gabarit : $manquantes",
                manquantes.isEmpty(),
            )
            assertTrue("$classe ne remplit aucune vue", cherchees.isNotEmpty())
        }
    }

    /** Les reglages ont bien quitte l'ecran principal. */
    @Test
    fun `les reglages ne sont plus sur l ecran principal`() {
        val principal = lire("src/main/res/layout/activity_main.xml")
        listOf("card_permissions", "card_gate_settings", "window_start", "snooze_length",
               "target_length", "export_data", "import_data", "shabbat_switch",
               "guided_switch", "hide_countdown_switch").forEach {
            assertTrue(
                "« $it » est resté sur l'écran principal",
                !principal.contains("""@+id/$it"""),
            )
        }
        assertTrue(
            "l'écran principal doit porter l'entrée vers les réglages",
            principal.contains("""@+id/settings_button"""),
        )
    }

    /** Sans son entree au manifeste, l'ecran ne s'ouvrirait pas. */
    @Test
    fun `l ecran des reglages est declare et atteignable`() {
        val manifeste = lire("src/main/AndroidManifest.xml")
        assertTrue(manifeste.contains("""android:name=".SettingsActivity""""))
        val principal = lire("src/main/java/com/myhitbodedut/MainActivity.kt")
        assertTrue(principal.contains("SettingsActivity::class.java"))
    }

    /**
     * Les quatre autorisations, dans l'ordre d'utilite decroissante. Les
     * deux dernieres ne servent qu'a la barriere : l'edition legere n'en a
     * pas, et ne doit donc pas les reclamer.
     */
    @Test
    fun `l edition legere ne demande que ce dont elle se sert`() {
        assertEquals(4, PermissionRules.demandees(true).size)
        assertEquals(
            listOf(Autorisation.NOTIFICATIONS, Autorisation.BATTERIE),
            PermissionRules.demandees(false),
        )
        assertTrue(PermissionRules.demandees(false).none { it.barriere })
    }

    @Test
    fun `les autorisations sont distinctes et ordonnees par utilite`() {
        val toutes = Autorisation.values().toList()
        assertEquals(toutes.size, toutes.map { it.cle }.toSet().size)
        assertEquals(toutes.size, toutes.map { it.titre }.toSet().size)
        assertEquals(toutes.size, toutes.map { it.pourquoi }.toSet().size)
        // Celles de la barriere ferment la marche : elles ne concernent
        // qu'une edition sur deux.
        val barriere = toutes.map { it.barriere }
        assertEquals(barriere.sortedBy { it }, barriere)
    }

    /** Ce qui manque se compte a partir de la meme liste, pas d'une autre. */
    @Test
    fun `le compte de ce qui manque suit l edition`() {
        val rien = PermissionRules.manquantes(true) { true }
        assertTrue("tout accordé : rien ne doit manquer", rien.isEmpty())
        assertEquals(4, PermissionRules.manquantes(true) { false }.size)
        assertEquals(2, PermissionRules.manquantes(false) { false }.size)
        // Une seule manquante, et c'est bien celle-la qui est rendue.
        assertEquals(
            listOf(Autorisation.BATTERIE),
            PermissionRules.manquantes(true) { it != Autorisation.BATTERIE },
        )
    }

    @Test
    fun `chaque autorisation a son titre et sa raison dans les trois langues`() {
        val cles = Autorisation.values().flatMap {
            listOf("permission_${it.cle}", "permission_${it.cle}_why")
        }
        // Les noms de ressource ne suivent pas tous la cle : on verifie les
        // raisons, qui sont nouvelles, et les titres par leur libelle connu.
        val titres = listOf("permission_notifications", "permission_battery",
                            "permission_overlay", "permission_usage")
        val raisons = titres.map { it + "_why" }
        listOf("values", "values-en", "values-iw").forEach { dossier ->
            val xml = lire("src/main/res/$dossier/strings.xml")
            (titres + raisons + listOf("settings_title", "action_open",
                                       "settings_all_granted")).forEach {
                assertTrue("$dossier : « $it » manquant", xml.contains("""name="$it""""))
            }
            assertTrue(
                "$dossier : le pluriel permissions_missing manque",
                xml.contains("""<plurals name="permissions_missing">"""),
            )
        }
        assertTrue(cles.isNotEmpty())
    }
}
