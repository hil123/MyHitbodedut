package com.myhitbodedut

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.random.Random

/** La lecture aléatoire (28/09) : tout, dans un ordre au hasard, sans répétition. */
class MelangeMusiqueTest {

    private val ids = (1..12).map { "niggoun_%02d".format(it) }

    @Test
    fun `aucun morceau ne revient avant que tous aient joue`() {
        val melange = MelangeMusique(Random(7))
        repeat(5) {
            val tour = List(ids.size) { melange.suivant(ids)!! }
            assertEquals("un tour doit tout jouer une fois", ids.toSet(), tour.toSet())
        }
    }

    @Test
    fun `jamais deux fois de suite le meme morceau`() {
        (1..50).forEach { graine ->
            val melange = MelangeMusique(Random(graine))
            val suite = List(ids.size * 6) { melange.suivant(ids)!! }
            suite.zipWithNext().forEach { (a, b) -> assertNotEquals("graine $graine", a, b) }
        }
    }

    @Test
    fun `l ordre change d un tour a l autre`() {
        val melange = MelangeMusique(Random(3))
        val premier = List(ids.size) { melange.suivant(ids)!! }
        val second = List(ids.size) { melange.suivant(ids)!! }
        assertNotEquals(premier, second)
    }

    @Test
    fun `un morceau retire ne sort plus et une bibliotheque vide ne joue rien`() {
        val melange = MelangeMusique(Random(1))
        melange.suivant(ids)
        val restants = ids.drop(6)
        repeat(20) { assertTrue(melange.suivant(restants) in restants) }
        assertNull(melange.suivant(emptyList()))
        assertEquals("seul", MelangeMusique().suivant(listOf("seul")))
    }

    @Test
    fun `la seance enchaine les morceaux en lecture aleatoire`() {
        val service = File("src/main/java/com/myhitbodedut/SessionService.kt").readText()
        assertTrue(service.contains("Store.musicShuffle(this)"))
        assertTrue(service.contains("melange.suivant("))
        val lecteur = File("src/main/java/com/myhitbodedut/MusicPlayer.kt").readText()
        assertTrue("sans fin de morceau, rien ne s'enchaîne", lecteur.contains("setOnCompletionListener"))
        val ecran = File("src/main/res/layout/activity_music.xml").readText()
        assertTrue(ecran.contains("@+id/music_shuffle"))
    }
}
