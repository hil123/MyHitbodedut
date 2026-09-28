package com.myhitbodedut

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Les durées proposées (28/09) : 5, 10, 15, 30 min, 1 h, 1 h 30, ou un temps choisi. */
class DureeSeanceTest {

    @Test
    fun `les durees demandees par le proprietaire`() {
        assertArrayEquals(intArrayOf(5, 10, 15, 30, 60, 90), DureeSeance.PROPOSEES)
    }

    @Test
    fun `le temps saisi est borne entre 1 et 240 minutes`() {
        assertEquals(25, DureeSeance.lire(" 25 "))
        assertEquals(1, DureeSeance.lire("0"))
        assertEquals(240, DureeSeance.lire("600"))
        assertNull(DureeSeance.lire(""))
        assertNull(DureeSeance.lire("vingt"))
    }

    /** La même liste partout : accueil, réglages, et la durée réglable sur l'écran de séance. */
    @Test
    fun `chaque choix de duree passe par la meme liste`() {
        val src = "src/main/java/com/myhitbodedut"
        listOf("MainActivity.kt", "SettingsActivity.kt").forEach {
            assertTrue("$it n'utilise pas DureeSeance.choisir",
                File("$src/$it").readText().contains("DureeSeance.choisir(this, R.string.settings_target)"))
        }
        val seance = File("$src/SessionActivity.kt").readText()
        assertTrue(seance.contains("DureeSeance.saisir(this)"))
        DureeSeance.PROPOSEES.forEach {
            assertTrue("bouton duration_$it absent", seance.contains("R.id.duration_$it"))
        }
    }
}
