package com.myhitbodedut

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.time.Duration
import java.time.LocalDate
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * La position exacte limitée au GPS en vol (31/10, Google Play) : les zmanim
 * et la boussole se contentent de la position approximative, qu'Android donne
 * à 2 ou 3 km près. Ici, ce qui le garantit dans le code, et ce que l'écart
 * fait aux calculs.
 */
class PositionApprocheeTest {

    private val dossier = File("src/main/java/com/myhitbodedut")

    /** Les lignes de code, hors commentaires, d'un fichier. */
    private fun code(f: File) = f.readLines()
        .filterNot { it.trimStart().let { l -> l.startsWith("*") || l.startsWith("/**") || l.startsWith("//") } }
        .joinToString("\n")

    @Test
    fun `seul le GPS en vol demande la position exacte`() {
        val demandeurs = dossier.listFiles { f -> f.extension == "kt" }!!
            .filter { code(it).contains("ACCESS_FINE_LOCATION") && Regex("""\.launch\([^)]*ACCESS_FINE_LOCATION""").containsMatchIn(code(it)) }
            .map { it.name }
        assertEquals(listOf("VolActivity.kt"), demandeurs)
    }

    @Test
    fun `le choix du lieu ne demande que la position approximative`() {
        val choix = code(File(dossier, "ChoixLieu.kt"))
        assertTrue(choix.contains("autorisation.launch(Manifest.permission.ACCESS_COARSE_LOCATION)"))
        assertFalse("la position exacte est encore demandée", Regex("""launch\([^)]*FINE""").containsMatchIn(choix))
        // Le libellé « Position exacte » n'a plus de sens : il a disparu des trois langues.
        listOf("values", "values-en", "values-iw").forEach { d ->
            val xml = File("src/main/res/$d/strings.xml").readText()
            assertFalse("$d : boussole_position_exacte", xml.contains("boussole_position_exacte"))
            assertTrue("$d : boussole_actualiser_position", xml.contains("name=\"boussole_actualiser_position\""))
        }
    }

    @Test
    fun `le manifeste garde les deux et dit a quoi sert l exacte`() {
        val manifeste = File("src/main/AndroidManifest.xml").readText()
        assertTrue(manifeste.contains("android.permission.ACCESS_COARSE_LOCATION"))
        val exacte = manifeste.substringBefore("<uses-permission android:name=\"android.permission.ACCESS_FINE_LOCATION\"")
            .substringAfterLast("<!--")
        assertTrue("le commentaire doit réserver l'exacte au GPS en vol", exacte.contains("GPS") && exacte.contains("vol"))
        assertFalse(exacte.contains("boussole"))
    }

    // --- Ce que 3 km d'écart font aux calculs --------------------------------------

    /** Le point à [km] kilomètres de (lat, lon), vers le cap [cap] en degrés. */
    private fun decale(lat: Double, lon: Double, km: Double, cap: Double): Pair<Double, Double> {
        val r = Math.toRadians(cap)
        val dLat = km * cos(r) / 111.2
        val dLon = km * sin(r) / (111.2 * cos(Math.toRadians(lat)))
        return (lat + dLat) to (lon + dLon)
    }

    private fun ecartMax(lat: Double, lon: Double, km: Double): Double =
        (0 until 360 step 15).maxOf { cap ->
            val (la, lo) = decale(lat, lon, km, cap.toDouble())
            abs(DirectionJerusalem.ecart(DirectionJerusalem.grandCercle(la, lo), DirectionJerusalem.grandCercle(lat, lon)))
        }

    @Test
    fun `la direction de Jerusalem tient avec 3 km d ecart`() {
        val loin = listOf(48.8534 to 2.3488, 40.7143 to -74.006, -37.814 to 144.9633, 32.0853 to 34.7818, 32.7940 to 34.9896)
        loin.forEach { (lat, lon) ->
            val d = DirectionJerusalem.distanceKm(lat, lon)
            val e = ecartMax(lat, lon, 3.0)
            if (d >= 200) assertTrue("à $d km : $e°", e < 1.0)
            if (d >= 50) assertTrue("à $d km : $e° dépasse la tolérance", e < DirectionJerusalem.TOLERANCE)
        }
        // Tout près de Har haBayit, l'approximative ne suffit plus à la précision de la boussole :
        // c'est une limite connue (PUBLICATION_PLAY_STORE.md § 6.7), pas une erreur de calcul.
        assertTrue(ecartMax(31.7683, 35.2137, 3.0) > DirectionJerusalem.TOLERANCE)
    }

    /**
     * Les zmanim s'affichent à la minute. 3 km d'écart les déplacent de quelques secondes, et
     * de 34 s au pire relevé : l'aube (16,1°) à Paris au solstice d'été, quand le soleil
     * passe à peine sous l'horizon.
     */
    @Test
    fun `les zmanim bougent de moins d une minute avec 3 km d ecart`() {
        val lieux = listOf(
            Zmanim.Lieu("Jérusalem", 31.76904, 35.21633, 0.0, "Asia/Jerusalem"),
            Zmanim.Lieu("Paris", 48.85341, 2.3488, 0.0, "Europe/Paris"),
            Zmanim.Lieu("New York", 40.71427, -74.00597, 0.0, "America/New_York"),
        )
        listOf("2026-06-21", "2026-12-21", "2026-03-20").map(LocalDate::parse).forEach { date ->
            lieux.forEach { lieu ->
                val ref = Zmanim.jour(lieu, date).associateBy { it.cle }
                listOf(0.0, 90.0, 180.0, 270.0).forEach { cap ->
                    val (la, lo) = decale(lieu.latitude, lieu.longitude, 3.0, cap)
                    Zmanim.jour(lieu.copy(latitude = la, longitude = lo), date).forEach { z ->
                        val a = ref.getValue(z.cle).instant ?: return@forEach
                        val b = z.instant ?: return@forEach
                        val s = abs(Duration.between(a, b).seconds)
                        assertTrue("${lieu.nom} $date ${z.cle} : $s s", s < 60)
                    }
                }
            }
        }
    }
}
