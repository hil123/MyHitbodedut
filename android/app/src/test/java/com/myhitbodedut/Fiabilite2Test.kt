package com.myhitbodedut

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Le lot fiabilité 2 (revue du 01/10). Les règles pures sont dans
 * FiabiliteTest ; ici, les branchements qui ne se voient qu'en lisant le
 * code : chaque garde doit exister à l'endroit où il protège.
 */
class Fiabilite2Test {

    private fun lire(f: String) = File("src/main/java/com/myhitbodedut/$f").readText()

    /** La séance finie n'est plus écrasée par une nouvelle dans les 5 s qui suivent. */
    @Test
    fun `une nouvelle seance clot d abord la precedente`() {
        val service = lire("SessionService.kt")
        val start = service.substringAfter("fun start(context: Context, minutes: Int)").substringBefore("}")
        assertTrue("start() doit clore la séance restée inscrite", start.contains("cloreLaPrecedente(context)"))
        assertTrue(start.indexOf("cloreLaPrecedente") < start.indexOf("Store.beginSession"))
        assertTrue("le tour du service doit viser le terme", service.contains("private fun delaiTick()"))
    }

    /** Un redémarrage du service sans séance n'inscrit plus une journée de 0 min. */
    @Test
    fun `le service relance sans seance s arrete sans rien inscrire`() {
        val service = lire("SessionService.kt")
        val debut = service.substringAfter("override fun onStartCommand").substringBefore("when (intent?.action)")
        assertTrue(debut.contains("intent == null && !Store.hasSessionRecord(this)"))
        val fin = service.substringAfter("private fun finishSession").substringBefore("val theme")
        assertTrue(fin.contains("!Store.hasSessionRecord(this)"))
    }

    @Test
    fun `une note seule effacee disparait du journal`() {
        val note = lire("Store.kt").substringAfter("fun setNote").substringBefore("writeDays")
        assertTrue(note.contains("existing?.noteSeule == true && note.isBlank() -> all.remove(date)"))
    }

    @Test
    fun `un journal illisible est mis de cote avant d etre ecrase`() {
        assertTrue(lire("Store.kt").contains("KEY_DAYS_CORROMPU"))
    }

    @Test
    fun `l import a un plafond et les notes une longueur`() {
        val reglages = lire("SettingsActivity.kt")
        assertTrue(reglages.contains("IMPORT_MAX_OCTETS = 2 * 1024 * 1024"))
        assertTrue(reglages.contains("if (total > IMPORT_MAX_OCTETS) return@use null"))
        assertTrue(lire("NoteDialog.kt").contains("LengthFilter(Store.NOTE_MAX)"))
        assertTrue(lire("Store.kt").contains("const val NOTE_MAX = 4000"))
    }

    @Test
    fun `le rappel se tait pendant une hitbodedout`() {
        assertTrue(lire("Reminder.kt").contains("!Store.isSessionRunning(context) && ReminderRule.shouldNotify("))
    }

    @Test
    fun `le journal refuse la capture d ecran`() {
        assertTrue(lire("JournalActivity.kt").contains("FLAG_SECURE"))
    }

    @Test
    fun `la vignette se met a jour apres minuit et apres un import`() {
        assertTrue(lire("StreakWidget.kt").contains("private fun programmerMinuit"))
        val reglages = lire("SettingsActivity.kt")
        val apresImport = reglages.substringAfter("val imported = Store.importJson(this, text)").substringBefore("Toast.makeText(\n")
        assertTrue("import : vignette non rafraîchie", apresImport.contains("StreakWidget.refresh(this)"))
    }

    @Test
    fun `israel ou diaspora est fige au premier acces`() {
        val bloc = lire("Store.kt").substringAfter("fun inIsrael(context: Context)").substringBefore("}")
        assertTrue(bloc.contains("if (!p.contains(KEY_IN_ISRAEL))"))
    }

    @Test
    fun `hors telephone fige ses dates et n emprunte pas le sujet de la prochaine seance`() {
        val accueil = lire("MainActivity.kt")
        val bloc = accueil.substringAfter("private fun declarerHorsTelephone").substringBefore("private fun hierOuvert")
        assertTrue(bloc.contains("val aujourdhui = Store.today()"))
        assertFalse(bloc.contains("Store.sessionTheme(this)"))
        assertTrue(accueil.contains("Store.addSession(this, minutes, theme = null, date = date)"))
        assertTrue(accueil.contains("(!done || hierOuvert())"))
    }

    @Test
    fun `le bravo survit a une rotation`() {
        val manifeste = File("src/main/AndroidManifest.xml").readText()
        val bloc = manifeste.substringAfter("android:name=\".CelebrationActivity\"").substringBefore("/>")
        assertTrue(bloc.contains("android:configChanges=\"orientation|screenSize"))
    }
}
