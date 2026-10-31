package com.myhitbodedut

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Les règles de confidentialité (31/10, Google Play) : la page publique
 * existe, en trois langues, le site la publie, les réglages y mènent, et ce
 * qu'elle affirme tient encore dans le code. Si l'un de ces faits change, ce
 * test échoue : la page est à relire avant de toucher au test.
 */
class ConfidentialiteTest {

    private val page = File("../../web/src/confidentialite.html")
    private val html by lazy { page.readText() }
    private fun source(f: String) = File("src/main/java/com/myhitbodedut/$f").readText()

    private fun section(id: String) = html.substringAfter("<section id=\"$id\"").substringBefore("</section>")

    @Test
    fun `la page existe en trois langues`() {
        assertTrue("web/src/confidentialite.html manque", page.isFile)
        listOf("fr", "en", "he").forEach { assertTrue("section $it", html.contains("<section id=\"$it\"")) }
        assertTrue(html.contains("<section id=\"he\" lang=\"he\" dir=\"rtl\">"))
        // Une page à part, sans script ni ressource d'ailleurs.
        assertFalse(html.contains("<script"))
        assertFalse(Regex("""(src|href)="https?://""").containsMatchIn(html))
    }

    @Test
    fun `le site la copie et les reglages y menent`() {
        val build = File("../../web/build.mjs").readText()
        assertTrue("build.mjs ne copie plus web/src en entier", build.contains("cpSync(SRC, DIST, { recursive: true })"))
        val reglages = source("SettingsActivity.kt")
        assertTrue(reglages.contains("CelebrationActivity.LIEN_WEB + \"confidentialite.html\""))
        assertTrue(source("CelebrationActivity.kt").contains("const val LIEN_WEB = \"https://hil123.github.io/myhitbodedut-web/\""))
        assertTrue(File("src/main/res/layout/activity_settings.xml").readText().contains("@string/settings_confidentialite"))
    }

    @Test
    fun `chaque langue dit les memes faits`() {
        val faits = mapOf(
            "fr" to listOf("SharedPreferences", "exclus", "Transfert d’appareil", "JSON", "approximative", "opensky-network.org",
                "dixième de degré", "désactivée par défaut", "api.adsbdb.com", "numéro de vol", "Ni compte, ni publicité, ni outil d’analyse"),
            "en" to listOf("SharedPreferences", "excluded", "Device transfer", "JSON", "Approximate location", "opensky-network.org",
                "tenth of a degree", "off by default", "api.adsbdb.com", "flight number", "No account, no ads, no audience analytics"),
            "he" to listOf("SharedPreferences", "אינם נכללים", "העברה למכשיר חדש", "JSON", "מיקום משוער", "opensky-network.org",
                "לעשירית מעלה", "כבויה כברירת מחדל", "api.adsbdb.com", "מספר הטיסה", "אין חשבון, אין פרסומות"),
        )
        faits.forEach { (langue, mots) ->
            val texte = section(langue)
            mots.forEach { assertTrue("$langue : « $it » manque", texte.contains(it)) }
            assertTrue("$langue : le contact", texte.contains("TODO_CONTACT") || texte.contains("mailto:"))
        }
    }

    // --- Ce que la page affirme, relu dans le code ----------------------------------

    @Test
    fun `journal exclu du nuage et inclus au transfert`() {
        val sauvegarde = File("src/main/res/xml/backup_rules.xml").readText()
        assertFalse(sauvegarde.contains("<include domain=\"sharedpref\" path=\"myhitbodedut-journal.xml\""))
        assertTrue(sauvegarde.contains("<include domain=\"sharedpref\" path=\"myhitbodedut.xml\""))
        val regles = File("src/main/res/xml/data_extraction_rules.xml").readText()
        val nuage = regles.substringAfter("<cloud-backup>").substringBefore("</cloud-backup>")
        val transfert = regles.substringAfter("<device-transfer>").substringBefore("</device-transfer>")
        assertFalse(nuage.contains("myhitbodedut-journal.xml"))
        assertTrue(nuage.contains("path=\"myhitbodedut.xml\""))
        assertTrue(transfert.contains("path=\"myhitbodedut-journal.xml\""))
        // Le prénom vit avec le journal.
        assertTrue(source("Store.kt").contains("journalPrefs(context).edit().putString(KEY_NAME"))
    }

    @Test
    fun `l export ne contient pas la position`() {
        val export = source("Store.kt").substringAfter("fun exportJson").substringBefore("fun importJson")
        assertFalse(export.contains("KEY_LIEU"))
        assertFalse(export.contains("lieuZmanim"))
    }

    @Test
    fun `opensky recoit un carre de dix degres arrondi au dixieme`() {
        val suivi = source("SuiviVol.kt")
        assertTrue(suivi.contains("const val DEMI_COTE = 5.0"))
        assertTrue(suivi.contains("val f = { v: Double -> \"%.1f\".format(java.util.Locale.ROOT, v) }"))
        assertEquals("https://opensky-network.org/api/states/all?lamin=27.8&lomin=30.2&lamax=37.8&lomax=40.2",
            SuiviVol.adresseEtats(SuiviVol.carres(32.8123, 35.2456).single()))
        // Le suivi en direct est désactivé tant que l'utilisateur ne l'active pas.
        assertTrue(source("Store.kt").contains("getBoolean(KEY_SUIVI_DIRECT, false)"))
        // L'indicatif ne part pas : il est cherché dans la réponse.
        assertFalse(SuiviVol.adresseEtats(SuiviVol.carres(32.8, 35.2).single()).contains("callsign"))
    }

    @Test
    fun `adsbdb ne recoit que le numero de vol`() {
        val demandees = mutableListOf<String>()
        RouteVol.chercher("LY 81", null) { adresse -> demandees += adresse; 404 to "" }
        assertTrue(demandees.isNotEmpty())
        demandees.forEach {
            assertTrue(it, it.startsWith("https://api.adsbdb.com/v0/callsign/") || it.startsWith("https://api.adsbdb.com/v0/airline/"))
            assertFalse(it, it.contains("?"))
        }
    }

    @Test
    fun `seule l edition complete declare Internet`() {
        assertFalse(File("src/main/AndroidManifest.xml").readText().contains("android.permission.INTERNET"))
        assertTrue(File("src/full/AndroidManifest.xml").readText().contains("android.permission.INTERNET"))
        assertFalse(File("src/light/AndroidManifest.xml").let { it.isFile && it.readText().contains("android.permission.INTERNET") })
    }
}
