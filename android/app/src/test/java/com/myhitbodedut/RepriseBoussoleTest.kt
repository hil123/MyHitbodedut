package com.myhitbodedut

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.cos
import kotlin.math.sin

/**
 * Les deux bugs du 02/11 : la boussole qui s'affole téléphone droit, et la
 * prière du matin qui reprenait le lendemain là où on s'était arrêté la veille.
 */
class RepriseBoussoleTest {

    /** La matrice Est-Nord-Haut ← téléphone de la spécification DeviceOrientation (Z·X·Y), ligne par ligne. */
    private fun matrice(alpha: Double, beta: Double, gamma: Double): FloatArray {
        val r = Math.PI / 180
        val (cA, sA) = cos(alpha * r) to sin(alpha * r)
        val (cB, sB) = cos(beta * r) to sin(beta * r)
        val (cG, sG) = cos(gamma * r) to sin(gamma * r)
        return floatArrayOf(
            (cA * cG - sA * sB * sG).toFloat(), (-cB * sA).toFloat(), (cG * sA * sB + cA * sG).toFloat(),
            (cG * sA + cA * sB * sG).toFloat(), (cA * cB).toFloat(), (sA * sG - cA * cG * sB).toFloat(),
            (-cB * sG).toFloat(), sB.toFloat(), (cB * cG).toFloat(),
        )
    }

    private fun proche(attendu: Double, mesure: Double) =
        assertEquals(0.0, DirectionJerusalem.ecart(attendu, mesure), 0.01)

    @Test fun aPlatLeHautDeLEcran() {
        for (a in listOf(0.0, 30.0, 90.0, 200.0, 359.0)) proche(360 - a, BoussoleActivity.capDevant(matrice(a, 0.0, 0.0)))
    }

    @Test fun droitLeDosDuTelephone() {
        proche(0.0, BoussoleActivity.capDevant(matrice(0.0, 90.0, 0.0)))
        proche(180.0, BoussoleActivity.capDevant(matrice(180.0, 90.0, 0.0)))
        // De 30° à la verticale : toujours l'est, sans saut.
        for (b in listOf(30.0, 45.0, 60.0, 80.0, 89.0, 89.9, 90.0)) proche(90.0, BoussoleActivity.capDevant(matrice(270.0, b, 0.0)))
    }

    @Test fun priereReprendSeulementApresUneInterruption() {
        val t0 = 1_793_000_000_000L
        assertTrue(Store.repriseValable("s:chaharit", t0, t0 + 20 * 60_000L))
        assertFalse(Store.repriseValable("s:chaharit", t0, t0 + Store.PRIERE_REPRISE_MS))
        assertFalse(Store.repriseValable("s:chaharit", t0, t0 + 24 * 3_600_000L))
        // Place enregistrée avant la correction, sans heure : au début.
        assertFalse(Store.repriseValable("s:chaharit", 0L, t0))
    }

    @Test fun livreReprendToujours() {
        val t0 = 1_793_000_000_000L
        assertTrue(Store.repriseValable("l:likoutei_etsot", t0, t0 + 10 * 24 * 3_600_000L))
        assertTrue(Store.repriseValable("l:likoutei_etsot", 0L, t0))
    }

    @Test fun tikounBasculeAQuatreHeures() {
        val t = java.time.LocalDateTime.of(2026, 11, 1, 23, 45)
        assertEquals("2026-11-01", Store.jourTikkun(t))
        assertEquals("2026-11-01", Store.jourTikkun(t.plusMinutes(20)))
        assertEquals("2026-11-01", Store.jourTikkun(java.time.LocalDateTime.of(2026, 11, 2, 3, 59)))
        assertEquals("2026-11-02", Store.jourTikkun(java.time.LocalDateTime.of(2026, 11, 2, 9, 0)))
    }
}
