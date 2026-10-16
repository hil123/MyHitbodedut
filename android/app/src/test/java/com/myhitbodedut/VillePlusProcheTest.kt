package com.myhitbodedut

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** La ville la plus proche d'une position (16/10) : elle nomme la position et dit Israël ou diaspora. */
class VillePlusProcheTest {

    private fun ville(id: Long, nom: String, pays: String, lat: Double, lon: Double) =
        Ville(id, LocalizedText(nom, nom, nom), pays, lat, lon, 0.0, if (pays == "IL") "Asia/Jerusalem" else "Europe/Paris")

    private val villes = listOf(
        ville(1, "Jérusalem", "IL", 31.76904, 35.21633),
        ville(2, "Tel Aviv", "IL", 32.08088, 34.78057),
        ville(3, "Paris", "FR", 48.85341, 2.3488),
    )

    @Test
    fun `la distance Jerusalem Tel Aviv est d environ 54 km`() {
        val d = Villes.distanceKm(31.76904, 35.21633, 32.08088, 34.78057)
        assertTrue("$d", d in 50.0..58.0)
    }

    @Test
    fun `la plus proche, et rien au-dela de 60 km`() {
        assertEquals(1L, Villes.plusProche(villes, 31.78, 35.20)?.id) // Jérusalem, quartier ouest
        assertEquals(2L, Villes.plusProche(villes, 32.05, 34.77)?.id) // Jaffa
        assertNull(Villes.plusProche(villes, 45.76, 4.84)) // Lyon : Paris est à près de 400 km
    }
}
