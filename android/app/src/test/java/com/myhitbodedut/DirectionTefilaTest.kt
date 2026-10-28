package com.myhitbodedut

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** La direction de la tefila (28/10) : mêmes vecteurs que web/test/direction.test.mjs. */
class DirectionTefilaTest {

    @Test
    fun newYork() {
        // Nord-est par la route la plus courte, plein est par le cap constant : les deux usages décrits.
        assertEquals(54.1, DirectionTefila.orthodromie(40.7128, -74.0060), 0.2)
        assertEquals(95.8, DirectionTefila.loxodromie(40.7128, -74.0060), 0.2)
    }

    @Test
    fun parisTelAvivMelbourne() {
        assertEquals(112.5, DirectionTefila.orthodromie(48.8566, 2.3522), 0.2)
        assertEquals(124.5, DirectionTefila.loxodromie(48.8566, 2.3522), 0.2)
        assertEquals(128.5, DirectionTefila.orthodromie(32.0853, 34.7818), 0.2)
        assertEquals(286.7, DirectionTefila.orthodromie(-37.8136, 144.9631), 0.2)
        assertEquals(304.2, DirectionTefila.loxodromie(-37.8136, 144.9631), 0.2)
    }

    @Test
    fun distanceEtOctant() {
        val km = DirectionTefila.distanceKm(48.8566, 2.3522)
        assertTrue("Paris-Jérusalem ~3 330 km : $km", km in 3280.0..3380.0)
        assertEquals(0, DirectionTefila.octant(359.0))
        assertEquals(1, DirectionTefila.octant(54.1))
        assertEquals(3, DirectionTefila.octant(128.5))
        assertEquals(7, DirectionTefila.octant(304.2))
    }
}
