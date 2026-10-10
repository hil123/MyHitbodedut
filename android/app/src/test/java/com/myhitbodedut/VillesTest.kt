package com.myhitbodedut

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.time.ZoneId

/** Les villes des zmanim (10/10) : GeoNames, identifiants uniques, fuseaux valides. */
class VillesTest {

    private val villes = Villes.parse(File("src/main/res/raw/villes.json").readText())

    @Test
    fun `les villes sont lues et bien formees`() {
        assertTrue(villes.size >= 40)
        assertEquals(villes.size, villes.map { it.id }.toSet().size)
        villes.forEach { v ->
            assertTrue("${v.id}", v.nom.he.isNotBlank() && v.nom.fr.isNotBlank() && v.nom.en.isNotBlank())
            assertTrue("${v.id}", v.latitude in -90.0..90.0 && v.longitude in -180.0..180.0)
            ZoneId.of(v.fuseau)
        }
        assertEquals("Jérusalem", villes.first().nom.fr)
        assertTrue(villes.first().enIsrael)
        assertTrue(villes.none { it.enIsrael && it.fuseau != "Asia/Jerusalem" })
    }

    @Test
    fun `la source est citee, comme CC-BY le demande`() {
        val json = File("src/main/res/raw/villes.json").readText()
        assertTrue(json.contains("GeoNames") && json.contains("CC-BY 4.0"))
        listOf("values", "values-en", "values-iw").forEach { d ->
            assertTrue(d, File("src/main/res/$d/strings.xml").readText().contains("GeoNames"))
        }
    }
}
