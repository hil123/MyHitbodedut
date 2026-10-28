package com.myhitbodedut

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * La direction de Jérusalem (DirectionJerusalem). Les valeurs attendues ne
 * viennent pas de ce code : grand cercle et distance par geographiclib
 * (Karney, ellipsoïde WGS84), d'où la marge d'un demi-degré et de 0,5 % pour
 * la sphère ; cap constant par la formule de Mercator, calculée à part en
 * Python. New York : 54° et environ 95°, comme Chabad.org.
 */
class DirectionJerusalemTest {

    private data class Cas(val nom: String, val lat: Double, val lon: Double, val gc: Double, val rh: Double, val km: Double)

    private val cas = listOf(
        Cas("Paris", 48.8566, 2.3522, 112.39, 124.51, 3337.0),
        Cas("New York", 40.7128, -74.0060, 54.01, 95.80, 9189.0),
        Cas("Los Angeles", 34.0522, -118.2437, 23.71, 91.01, 12208.0),
        Cas("Melbourne", -37.8136, 144.9631, 286.52, 304.16, 13709.0),
        Cas("Buenos Aires", -34.6037, -58.3816, 64.76, 53.03, 12231.0),
        Cas("Montréal", 45.5017, -73.5673, 54.90, 99.22, 8851.0),
    )

    private fun angle(attendu: Double, obtenu: Double, marge: Double, message: String) =
        assertTrue("$message : $obtenu au lieu de $attendu", kotlin.math.abs(DirectionJerusalem.ecart(attendu, obtenu)) <= marge)

    @Test
    fun `les deux directions et la distance, des grandes villes`() {
        cas.forEach { c ->
            angle(c.gc, DirectionJerusalem.grandCercle(c.lat, c.lon), 0.5, "${c.nom} grand cercle")
            angle(c.rh, DirectionJerusalem.carte(c.lat, c.lon), 0.05, "${c.nom} cap constant")
            val d = DirectionJerusalem.distanceKm(c.lat, c.lon)
            assertTrue("${c.nom} distance $d", kotlin.math.abs(d - c.km) / c.km < 0.005)
        }
    }

    @Test
    fun `par le plus court cote du meridien de 180 degres`() {
        // De Honolulu, la loxodromie passe par l'ouest (cap 270° et plus), pas par l'est.
        val rh = DirectionJerusalem.carte(21.3069, -157.8583)
        assertTrue("$rh", rh > 270 && rh < 300)
        // Depuis Jérusalem même, la distance est nulle, sans NaN.
        assertEquals(0.0, DirectionJerusalem.distanceKm(DirectionJerusalem.LATITUDE, DirectionJerusalem.LONGITUDE), 1e-6)
    }

    @Test
    fun `l ecart, la tolerance, les degres, les points cardinaux`() {
        assertEquals(10.0, DirectionJerusalem.ecart(10.0, 0.0), 1e-9)
        assertEquals(-20.0, DirectionJerusalem.ecart(350.0, 10.0), 1e-9)
        assertEquals(20.0, DirectionJerusalem.ecart(10.0, 350.0), 1e-9)
        assertTrue(DirectionJerusalem.face(4.0))
        assertTrue(DirectionJerusalem.face(-4.0))
        assertFalse(DirectionJerusalem.face(4.1))
        assertEquals(0, DirectionJerusalem.degres(359.6))
        assertEquals(54, DirectionJerusalem.degres(54.01))
        assertEquals(1, DirectionJerusalem.point(54.0))
        assertEquals(2, DirectionJerusalem.point(95.8))
        assertEquals(0, DirectionJerusalem.point(350.0))
        // Le réglage choisit laquelle est en avant ; les deux restent.
        val (p, a) = DirectionJerusalem.directions(40.7128, -74.0060, carteEnAvant = true)
        assertEquals(DirectionJerusalem.carte(40.7128, -74.0060), p, 1e-9)
        assertEquals(DirectionJerusalem.grandCercle(40.7128, -74.0060), a, 1e-9)
    }

    @Test
    fun `le filtre suit le cap par le plus court cote`() {
        assertEquals(10.0, BoussoleActivity.lisser(null, 10.0), 1e-9)
        // De 350° vers 10° : il passe par 354°, pas par 278°.
        assertEquals(354.0, BoussoleActivity.lisser(350.0, 10.0), 1e-9)
        assertEquals(-150.0, BoussoleDessin.xHorizon(-90.0).toDouble(), 1e-6)
        assertEquals(22.0, BoussoleDessin.xHorizon(10.0).toDouble(), 1e-4)
    }
}
