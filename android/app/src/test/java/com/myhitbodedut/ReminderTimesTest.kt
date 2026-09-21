package com.myhitbodedut

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Plusieurs heures de rappel : le calcul de la prochaine, et ce que
 * l'ecran promet a leur sujet.
 */
class ReminderTimesTest {

    private val jour = ReminderRule.MINUTES_PAR_JOUR

    @Test
    fun `la prochaine heure du jour est retenue`() {
        val heures = listOf(7 * 60, 13 * 60, 21 * 60)
        assertEquals(60, ReminderRule.minutesUntilNext(6 * 60, heures))
        assertEquals(5 * 60, ReminderRule.minutesUntilNext(8 * 60, heures))
        assertEquals(60, ReminderRule.minutesUntilNext(20 * 60, heures))
    }

    /**
     * Quand toutes les heures sont passees, on repart sur la premiere du
     * lendemain. C'est le cas qu'un simple « premiere heure superieure a
     * maintenant » manque, en ne rendant rien.
     */
    @Test
    fun `apres la derniere heure, on passe au lendemain`() {
        val heures = listOf(7 * 60, 21 * 60)
        val attendu = (jour - 22 * 60) + 7 * 60
        assertEquals(attendu, ReminderRule.minutesUntilNext(22 * 60, heures))
        assertEquals(9 * 60, ReminderRule.minutesUntilNext(22 * 60, listOf(7 * 60)))
    }

    @Test
    fun `une heure qui vient de passer n est pas redonnee`() {
        // A l'heure pile, le rappel vient de sonner : la prochaine est ailleurs.
        val heures = listOf(7 * 60, 19 * 60)
        assertEquals(12 * 60, ReminderRule.minutesUntilNext(7 * 60, heures))
    }

    @Test
    fun `sans heure reglee, il n y a rien a programmer`() {
        assertNull(ReminderRule.minutesUntilNext(10 * 60, emptyList()))
    }

    @Test
    fun `les heures sont triees, dedupliquees et bornees au jour`() {
        val sales = listOf(21 * 60, 7 * 60, 7 * 60, -30, jour + 10, 13 * 60)
        assertEquals(60, ReminderRule.minutesUntilNext(6 * 60, sales))
        // Une valeur hors jour ne doit jamais etre choisie comme prochaine.
        assertEquals(6 * 60, ReminderRule.minutesUntilNext(7 * 60, sales))
    }

    @Test
    fun `le resultat tombe toujours dans les vingt-quatre heures`() {
        val heures = listOf(0, 8 * 60, 23 * 60 + 59)
        (0 until jour step 37).forEach { maintenant ->
            val dans = ReminderRule.minutesUntilNext(maintenant, heures)!!
            assertTrue("$maintenant -> $dans", dans in 1..jour)
        }
    }

    /**
     * La regle de silence ne change pas parce qu'il y a plusieurs heures :
     * un jour de repos reste muet, et la veille se tait apres midi.
     */
    @Test
    fun `plusieurs heures ne contournent pas la regle du repos`() {
        listOf(7 * 60, 13 * 60, 21 * 60).forEach { heure ->
            assertTrue(
                "un jour de repos doit rester muet a $heure",
                !ReminderRule.shouldNotify(heure, false, true, false, null),
            )
        }
        assertTrue(ReminderRule.shouldNotify(7 * 60, false, false, true, null))
        assertTrue(!ReminderRule.shouldNotify(13 * 60, false, false, true, null))
    }

    /** L'ecran des rappels annonce ces trois etats ; ils doivent exister. */
    @Test
    fun `les libelles d etat des rappels existent dans les trois langues`() {
        val cles = listOf("reminders_off", "reminders_none", "reminders_permission_missing",
                          "reminder_action_start", "reminder_action_later")
        listOf("values", "values-en", "values-iw").forEach { dossier ->
            val xml = File("src/main/res/$dossier/strings.xml").readText()
            cles.forEach {
                assertTrue("$dossier : « $it » manquant", xml.contains("""name="$it""""))
            }
            assertTrue(
                "$dossier : le pluriel reminders_count manque",
                xml.contains("""<plurals name="reminders_count">"""),
            )
        }
    }
}
