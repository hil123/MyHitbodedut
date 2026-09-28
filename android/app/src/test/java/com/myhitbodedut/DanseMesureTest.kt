package com.myhitbodedut

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.math.abs

/** La danse du bravo : deux pas qui s'enchaînent sans à-coup. */
class DanseMesureTest {

    private val instants = (0..400).map { it / 400f }

    @Test
    fun `chaque valeur reste dans ses bornes`() {
        instants.forEach { t ->
            val pas = DanseMesure.pas(t)
            assertTrue(pas.hauteur in 0f..1f)
            assertTrue(pas.flexion in 0f..1f)
            assertTrue(pas.balance in -1f..1f)
            assertTrue(pas.poseB in 0f..1f)
        }
    }

    /** Aucun saut d'image : la boucle, et le passage d'un temps à l'autre, sont continus. */
    @Test
    fun `le mouvement est continu, boucle comprise`() {
        (instants + 1f).zipWithNext().forEach { (a, b) ->
            val x = DanseMesure.pas(a)
            val y = DanseMesure.pas(b)
            assertTrue("hauteur à $a", abs(x.hauteur - y.hauteur) < 0.05f)
            assertTrue("balance à $a", abs(x.balance - y.balance) < 0.05f)
            // Le relais des poses est bref (un dixième de temps) : il reste
            // continu, simplement plus raide.
            assertTrue("pose à $a", abs(x.poseB - y.poseB) < 0.3f)
        }
    }

    @Test
    fun `il change de jambe a chaque temps et touche le sol en changeant`() {
        assertEquals(0f, DanseMesure.pas(0.2f).poseB, 0.001f)
        assertEquals(1f, DanseMesure.pas(0.7f).poseB, 0.001f)
        assertEquals(0f, DanseMesure.pas(0f).hauteur, 0.001f)
        assertEquals(0f, DanseMesure.pas(0.5f).hauteur, 0.001f)
        assertEquals(1f, DanseMesure.pas(0.25f).hauteur, 0.001f)
        assertEquals(1f, DanseMesure.pas(0.5f).flexion, 0.001f)
    }

    /** Les deux poses superposées, et elles seules, dans la scène. */
    @Test
    fun `la scene porte les deux poses de danse`() {
        val gabarit = File("src/main/res/layout/activity_celebration.xml").readText()
        assertTrue(gabarit.contains("@drawable/char_dancing\""))
        assertTrue(gabarit.contains("@drawable/char_dancing_joy\""))
        assertTrue(gabarit.contains("@+id/celebration_dancer"))
        val code = File("src/main/java/com/myhitbodedut/CelebrationActivity.kt").readText()
        assertTrue("les animations coupées doivent être respectées", code.contains("areAnimatorsEnabled"))
        assertFalse(code.contains("AnimatorSet"))
    }
}
