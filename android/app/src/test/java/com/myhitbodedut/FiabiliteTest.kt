package com.myhitbodedut

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Les corrections de la revue du 29/09 sur la boucle de chaque jour : ce qui
 * s'inscrit à la fin d'une séance, le jour où elle compte, l'heure des
 * rappels, ce qu'une note ou un import peuvent valider.
 *
 * Les règles sont appelées avec des valeurs, pas cherchées dans le texte du
 * code : c'est leur comportement qui est promis.
 */
class FiabiliteTest {

    private val minute = 60_000L
    private val debut = 1_800_000_000_000L

    // --- Minutes inscrites ---------------------------------------------------

    @Test
    fun `une seance minutee ne compte jamais plus que sa duree`() {
        val fin = debut + 20 * minute
        // Fin constatée 40 minutes trop tard (écran éteint, service tué).
        assertEquals(20, SessionRule.minutesAInscrire(debut, fin, fin + 40 * minute))
        // Constatée à l'heure.
        assertEquals(20, SessionRule.minutesAInscrire(debut, fin, fin))
    }

    @Test
    fun `une seance arretee avant son terme compte le temps passe`() {
        val fin = debut + 20 * minute
        assertEquals(12, SessionRule.minutesAInscrire(debut, fin, debut + 12 * minute + 30_000L))
        assertEquals(0, SessionRule.minutesAInscrire(debut, fin, debut + 30_000L))
    }

    @Test
    fun `une seance ouverte oubliee ne compte pas une journee`() {
        assertEquals(35, SessionRule.minutesAInscrire(debut, 0L, debut + 35 * minute))
        assertEquals(
            SessionRule.PLAFOND_OUVERTE,
            SessionRule.minutesAInscrire(debut, 0L, debut + 20 * 60 * minute)
        )
    }

    @Test
    fun `sans debut rien ne s inscrit`() {
        assertEquals(0, SessionRule.minutesAInscrire(0L, 0L, debut))
    }

    @Test
    fun `une seance orpheline est echue a son terme ou au plafond`() {
        val fin = debut + 20 * minute
        assertFalse(SessionRule.estEchue(debut, fin, 20, fin - 1))
        assertTrue(SessionRule.estEchue(debut, fin, 20, fin))
        assertFalse(SessionRule.estEchue(debut, 0L, SessionRule.OUVERTE, debut + 60 * minute))
        assertTrue(
            SessionRule.estEchue(debut, 0L, SessionRule.OUVERTE, debut + SessionRule.PLAFOND_OUVERTE * minute)
        )
        assertFalse(SessionRule.estEchue(0L, 0L, 0, debut))
    }

    // --- Heure des rappels -----------------------------------------------------

    private val paris = ZoneId.of("Europe/Paris")

    private fun a(annee: Int, mois: Int, jour: Int, h: Int, m: Int = 0) =
        ZonedDateTime.of(LocalDateTime.of(annee, mois, jour, h, m), paris)

    @Test
    fun `le prochain rappel un jour ordinaire`() {
        val maintenant = a(2026, 9, 29, 6, 30)
        assertEquals(a(2026, 9, 29, 7), ReminderRule.prochainDeclenchement(maintenant, listOf(7 * 60)))
        // Toutes passées : la première du lendemain.
        assertEquals(
            a(2026, 9, 30, 7),
            ReminderRule.prochainDeclenchement(a(2026, 9, 29, 21), listOf(7 * 60, 20 * 60))
        )
        assertNull(ReminderRule.prochainDeclenchement(maintenant, emptyList()))
    }

    /**
     * Passage à l'heure d'été (29/03/2026 à Paris) : la nuit dure une heure
     * de moins. Ajouter 9 heures de minutes à 22 h faisait sonner à 8 h.
     */
    @Test
    fun `le rappel garde son heure au passage a l heure d ete`() {
        val veille = a(2026, 3, 28, 22)
        val rappel = ReminderRule.prochainDeclenchement(veille, listOf(7 * 60))!!
        assertEquals(7, rappel.hour)
        assertEquals(29, rappel.dayOfMonth)
        assertEquals(Duration.ofHours(8), Duration.between(veille, rappel))
    }

    /** Retour à l'heure d'hiver (25/10/2026) : la nuit dure une heure de plus. */
    @Test
    fun `le rappel garde son heure au retour a l heure d hiver`() {
        val veille = a(2026, 10, 24, 22)
        val rappel = ReminderRule.prochainDeclenchement(veille, listOf(7 * 60))!!
        assertEquals(7, rappel.hour)
        assertEquals(Duration.ofHours(10), Duration.between(veille, rappel))
    }

    @Test
    fun `une heure qui n existe pas ce jour la est repoussee`() {
        val rappel = ReminderRule.prochainDeclenchement(a(2026, 3, 28, 22), listOf(2 * 60 + 30))!!
        assertEquals(29, rappel.dayOfMonth)
        assertEquals(3, rappel.hour)
        assertEquals(30, rappel.minute)
    }

    // --- Journées ----------------------------------------------------------------

    @Test
    fun `une note seule ne fait pas la journee`() {
        assertFalse(DayRecord("2026-09-29", 0, "une pensée", noteSeule = true).faite)
        assertTrue(DayRecord("2026-09-29", 0, "", noteSeule = false).faite)
        assertTrue(DayRecord("2026-09-29", 20, "").faite)
    }

    @Test
    fun `seules les vraies dates entrent au journal`() {
        assertNotNull(Store.dateValide("2026-09-29"))
        listOf("2026-13-45", "2026-2-3", "2026-02-30", "x", "").forEach {
            assertNull("« $it » ne doit pas être acceptée", Store.dateValide(it))
        }
    }

    @Test
    fun `quinze minutes par defaut comme le conseille l application`() {
        assertEquals(15, Store.DEFAULT_TARGET)
    }

    // --- Branchements --------------------------------------------------------------

    private fun lire(chemin: String) = File(chemin).readText()

    /** Android 12 interdit d'ouvrir un écran depuis un receveur : le bouton ouvre l'écran lui-même. */
    @Test
    fun `le bouton commencer du rappel ouvre l ecran de seance`() {
        val rappel = lire("src/main/java/com/myhitbodedut/Reminder.kt")
        val commencer = rappel.substringAfter("val commencer").substringBefore(".build()")
        assertTrue(commencer.contains("PendingIntent.getActivity"))
        assertTrue(commencer.contains("SessionActivity::class.java"))
        assertFalse(rappel.contains("action(1, Reminder.ACTION_START"))
    }

    @Test
    fun `la surveillance reprend apres une mise a jour et suit l heure`() {
        val manifeste = lire("src/main/AndroidManifest.xml")
        listOf(
            "android.intent.action.MY_PACKAGE_REPLACED",
            "android.intent.action.TIME_SET",
            "android.intent.action.TIMEZONE_CHANGED",
            "android.permission.WAKE_LOCK",
        ).forEach { assertTrue("$it manque au manifeste", manifeste.contains(it)) }
    }

    /** 8 points étaient illisibles en hébreu voyellé. */
    @Test
    fun `le texte des cartes ne descend pas sous 12 points`() {
        listOf("view_chizuk.xml", "view_chizuk_compact.xml").forEach { f ->
            Regex("""autoSizeMinTextSize="(\d+)sp"""").findAll(lire("src/main/res/layout/$f")).forEach {
                assertTrue("$f : ${it.groupValues[1]}sp", it.groupValues[1].toInt() >= 12)
            }
        }
    }

    /**
     * targetSdk 36 (29/10) : Android 16 ignore la dérogation au plein écran.
     * Les marges des barres sont rendues une fois pour tous les écrans
     * (Barres, posé par Apparence.installer), et la dérogation ne doit pas
     * revenir : sur Android 15 elle agirait encore, et les deux versions ne
     * se ressembleraient plus.
     */
    @Test
    fun `les barres du systeme sont gerees pour tous les ecrans`() {
        File("src/main/res").walkTopDown().filter { it.extension == "xml" }.forEach { f ->
            assertFalse("${f.path} : dérogation au plein écran", f.readText().contains("windowOptOutEdgeToEdgeEnforcement"))
        }
        assertTrue(lire("src/main/java/com/myhitbodedut/Apparence.kt").contains("Barres.installer(activity)"))
        assertTrue(lire("src/main/res/values/themes.xml").contains("parent=\"Base.Theme.MyHitbodedut\""))
    }
}
