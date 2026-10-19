package com.myhitbodedut

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.math.abs

/** Les ambiances calculées (proposition 9, 19/10) : audibles, douces, jamais saturées, sans fichier. */
class AmbianceTest {

    @Test
    fun `chaque ambiance s entend, sans saturer, et le silence se tait`() {
        Ambiance.entries.forEach { a ->
            val s = AmbianceSynth.generer(a, taux = 8_000, secondes = 24)
            assertEquals(8_000 * 24, s.size)
            val crete = s.maxOf { abs(it) }
            if (a == Ambiance.SILENCE) assertEquals(0f, crete, 0f)
            else {
                assertTrue("${a.cle} : trop faible", AmbianceSynth.rms(s) > 0.02)
                assertTrue("${a.cle} : sature ($crete)", crete <= 0.61f)
                assertEquals("${a.cle} : la boucle doit commencer en fondu", 0f, s[0], 0f)
            }
        }
    }

    @Test
    fun `le calcul est reproductible`() {
        assertArrayEquals(AmbianceSynth.generer(Ambiance.MER, 8_000, 2), AmbianceSynth.generer(Ambiance.MER, 8_000, 2), 0f)
    }

    @Test
    fun `aucun fichier audio n est ajoute pour les ambiances, et la cle inconnue rend le silence`() {
        assertTrue(File("src/main/res/raw").listFiles().orEmpty().none { it.name.startsWith("ambiance") })
        assertEquals(Ambiance.SILENCE, Ambiance.of("orage"))
        assertEquals(Ambiance.PLUIE, Ambiance.of("pluie"))
    }
}
