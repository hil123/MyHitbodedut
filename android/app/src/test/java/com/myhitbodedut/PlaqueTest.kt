package com.myhitbodedut

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.time.LocalDate

/**
 * Le centre des anneaux (demande du 04/10) : un toucher fait tourner la
 * plaque et montre vos chiffres, face après face, puis revient au hassid.
 */
class PlaqueTest {

    @Test
    fun `les faces se suivent et reviennent au hassid`() {
        var face = Plaque.Face.HASSID
        val vues = mutableListOf<Plaque.Face>()
        repeat(Plaque.Face.entries.size) {
            face = Plaque.suivante(face)
            vues += face
        }
        assertEquals(Plaque.Face.HASSID, vues.last())
        assertEquals(Plaque.Face.entries.toSet(), vues.toSet())
        assertEquals(Plaque.Face.JOUR, vues.first())
    }

    @Test
    fun `chaque anneau a sa face`() {
        Anneaux.Quel.entries.forEach { assertEquals(it, Plaque.pour(it).anneau) }
        assertEquals(Plaque.Face.SERIE, Plaque.pour(Anneaux.Quel.SERIE))
        assertEquals(null, Plaque.Face.TOTAL.anneau)
    }

    @Test
    fun `la moyenne d un jour de pratique`() {
        assertEquals(0, Plaque.moyenne(0, 0))
        assertEquals(17, Plaque.moyenne(50, 3))
        assertEquals(20, Plaque.moyenne(200, 10))
    }

    private val lundi = LocalDate.of(2026, 9, 28)

    @Test
    fun `la meilleure serie ne casse pas sur un jour exempte`() {
        // Lundi à mercredi faits, jeudi exempté, vendredi fait, samedi manqué, dimanche fait.
        val faits = listOf(0, 1, 2, 4, 6).map { lundi.plusDays(it.toLong()) }.toSet()
        val jeudi = lundi.plusDays(3)
        assertEquals(4, Plaque.meilleureSerie(faits) { it == jeudi })
        assertEquals(3, Plaque.meilleureSerie(faits) { false })
        assertEquals(0, Plaque.meilleureSerie(emptySet()) { false })
        assertEquals(1, Plaque.meilleureSerie(setOf(lundi)) { false })
    }

    @Test
    fun `l accueil fait tourner la plaque et compte depuis zero`() {
        val accueil = File("src/main/java/com/myhitbodedut/MainActivity.kt").readText()
        assertTrue(accueil.contains("plaque.animate().rotationY(90f)"))
        assertTrue(accueil.contains("ValueAnimator.ofInt(0, cible)"))
        assertTrue("le hassid doit revenir seul", accueil.contains("plaque.postDelayed(retourHassid, Plaque.RETOUR_MS)"))
        val gabarit = File("src/main/res/layout/activity_main.xml").readText()
        listOf("hero_face", "hero_face_value", "hero_face_caption").forEach {
            assertTrue("$it manque", gabarit.contains("@+id/$it\""))
        }
    }

    @Test
    fun `les faces parlent les trois langues`() {
        listOf("values", "values-en", "values-iw").forEach { d ->
            val xml = File("src/main/res/$d/strings.xml").readText()
            listOf("plaque_jour", "plaque_semaine", "plaque_meilleure", "plaque_total", "plaque_moyenne",
                "plaque_serie", "plaque_jours").forEach {
                assertTrue("$d : $it", xml.contains("name=\"$it\""))
            }
        }
    }
}
