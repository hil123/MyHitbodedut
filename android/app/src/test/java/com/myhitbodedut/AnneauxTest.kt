package com.myhitbodedut

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.time.LocalDate

/**
 * Les trois anneaux de l'accueil (demande du 01/10) : ce qu'ils mesurent,
 * le second tour au-dela de l'objectif, et les paliers de la serie.
 */
class AnneauxTest {

    private val semaine = JaugeSemaine.Progression(faits = 4, dus = 6)

    @Test
    fun `le jour mesure les minutes sur l objectif`() {
        val e = Anneaux.etat(minutesJour = 8, objectifJour = 15, semaine = semaine, serie = 4)
        assertEquals(8, e.jour.fait)
        assertEquals(15, e.jour.objectif)
        assertEquals(8f / 15f, e.jour.fraction, 1e-6f)
        assertFalse(e.jour.atteint)
        assertEquals(0, e.jour.tours)
    }

    /** Comme chez Apple : l'anneau ne se ferme pas, il repasse sur lui-meme. */
    @Test
    fun `au dela de l objectif l anneau fait un second tour`() {
        val e = Anneaux.etat(minutesJour = 23, objectifJour = 15, semaine = semaine, serie = 4)
        assertTrue(e.jour.atteint)
        assertEquals(1, e.jour.tours)
        assertEquals(8f / 15f, e.jour.reste, 1e-6f)
        val double = Anneaux.etat(minutesJour = 30, objectifJour = 15, semaine = semaine, serie = 4)
        assertEquals(2, double.jour.tours)
        assertEquals(0f, double.jour.reste, 1e-6f)
    }

    @Test
    fun `la semaine reprend la jauge existante`() {
        val e = Anneaux.etat(0, 15, semaine, 0)
        assertEquals(4, e.semaine.fait)
        assertEquals(6, e.semaine.objectif)
        assertEquals(semaine.fraction, e.semaine.fraction, 1e-6f)
    }

    @Test
    fun `la serie vise le premier palier qui l atteint`() {
        mapOf(0 to 7, 1 to 7, 7 to 7, 8 to 14, 14 to 14, 15 to 30, 30 to 30, 31 to 60,
              100 to 100, 101 to 180, 365 to 365, 366 to 730, 730 to 730, 731 to 1095)
            .forEach { (serie, palier) -> assertEquals("série $serie", palier, Anneaux.palier(serie)) }
    }

    @Test
    fun `le jour du palier l anneau de la serie est plein`() {
        val e = Anneaux.etat(0, 15, semaine, serie = 7)
        assertTrue(e.serie.atteint)
        assertEquals(1f, e.serie.fraction, 1e-6f)
        // Le lendemain, le palier suivant commence.
        val lendemain = Anneaux.etat(0, 15, semaine, serie = 8)
        assertFalse(lendemain.serie.atteint)
        assertEquals(8f / 14f, lendemain.serie.fraction, 1e-6f)
    }

    @Test
    fun `sans objectif rien ne se remplit et rien ne plante`() {
        val e = Anneaux.etat(10, 0, JaugeSemaine.Progression(0, 0), 0)
        assertEquals(0f, e.jour.fraction, 1e-6f)
        assertEquals(0f, e.semaine.fraction, 1e-6f)
        assertEquals(0f, e.serie.fraction, 1e-6f)
        assertFalse(e.jour.atteint)
        // Des valeurs negatives (horloge reculee) sont ramenees a zero.
        assertEquals(0, Anneaux.etat(-5, 15, semaine, -2).jour.fait)
    }

    @Test
    fun `l objectif quotidien retombe sur la duree habituelle`() {
        assertEquals(15, Anneaux.objectifEffectif(0, 15))
        assertEquals(20, Anneaux.objectifEffectif(20, 15))
        assertEquals(1, Anneaux.objectifEffectif(0, 0))
    }

    @Test
    fun `du centre vers l exterieur jour semaine serie`() {
        val e = Anneaux.etat(8, 15, semaine, 4)
        assertEquals(listOf(Anneaux.Quel.JOUR, Anneaux.Quel.SEMAINE, Anneaux.Quel.SERIE), e.duCentre.map { it.quel })
    }

    /** Le dessin est partage : l'accueil, la vignette et les notifications montrent la meme chose. */
    @Test
    fun `l accueil la vignette et les notifications dessinent les memes anneaux`() {
        fun lire(f: String) = File("src/main/java/com/myhitbodedut/$f").readText()
        listOf("AnneauxView.kt", "StreakWidget.kt", "AnneauxImage.kt").forEach {
            assertTrue("$it ne passe pas par AnneauxDessin", lire(it).contains("AnneauxDessin.dessiner("))
        }
        listOf("Reminder.kt", "SessionService.kt").forEach {
            assertTrue("$it : pas d'anneaux dans la notification", lire(it).contains("AnneauxImage.notification("))
        }
        val accueil = lire("MainActivity.kt")
        assertTrue(accueil.contains("Store.anneaux(this)"))
        assertTrue("le centre doit changer de face au toucher", accueil.contains("setOnClickListener { toucherPlaque() }"))
        val gabarit = File("src/main/res/layout/activity_main.xml").readText()
        assertTrue(gabarit.contains("com.myhitbodedut.AnneauxView"))
        listOf("values", "values-en", "values-iw").forEach { d ->
            val xml = File("src/main/res/$d/strings.xml").readText()
            listOf("settings_daily_goal", "ring_day_detail", "ring_streak_detail", "rings_description").forEach {
                assertTrue("$d : $it", xml.contains("name=\"$it\""))
            }
        }
    }

    // --- Animations automatiques (demande du 04/10) ---------------------------

    private fun source(f: String) = File("src/main/java/com/myhitbodedut/$f").readText()

    private val mercredi = LocalDate.of(2026, 10, 7)

    @Test
    fun `un anneau ferme se fete une fois puis plus`() {
        val plein = Anneaux.etat(minutesJour = 15, objectifJour = 15, semaine = semaine, serie = 4)
        assertEquals(listOf(Anneaux.Quel.JOUR), Anneaux.aFeter(plein, mercredi, emptySet()))
        val deja = Anneaux.marques(plein, mercredi)
        assertEquals(emptyList<Anneaux.Quel>(), Anneaux.aFeter(plein, mercredi, deja))
        // Le lendemain, l'anneau du jour refermé se fête de nouveau.
        assertEquals(listOf(Anneaux.Quel.JOUR), Anneaux.aFeter(plein, mercredi.plusDays(1), deja))
    }

    @Test
    fun `rien a feter tant qu aucun anneau n est ferme`() {
        val e = Anneaux.etat(minutesJour = 8, objectifJour = 15, semaine = semaine, serie = 4)
        assertEquals(emptyList<Anneaux.Quel>(), Anneaux.aFeter(e, mercredi, emptySet()))
        assertEquals(emptySet<String>(), Anneaux.marques(e, mercredi))
    }

    @Test
    fun `la semaine se fete une fois par semaine et la serie une fois par palier`() {
        val semainePleine = JaugeSemaine.Progression(faits = 6, dus = 6)
        val e = Anneaux.etat(minutesJour = 0, objectifJour = 15, semaine = semainePleine, serie = 7)
        assertEquals(listOf(Anneaux.Quel.SEMAINE, Anneaux.Quel.SERIE), Anneaux.aFeter(e, mercredi, emptySet()))
        val deja = Anneaux.marques(e, mercredi)
        // Jeudi, même semaine : rien de neuf pour la semaine.
        assertFalse(Anneaux.Quel.SEMAINE in Anneaux.aFeter(e, mercredi.plusDays(1), deja))
        // Dimanche suivant : une autre semaine.
        assertTrue(Anneaux.Quel.SEMAINE in Anneaux.aFeter(e, LocalDate.of(2026, 10, 11), deja))
        // Le palier suivant de la série (14) se fête à son tour.
        val quatorze = Anneaux.etat(0, 15, JaugeSemaine.Progression(0, 0), 14)
        assertEquals(listOf(Anneaux.Quel.SERIE), Anneaux.aFeter(quatorze, mercredi, deja))
    }

    @Test
    fun `l accueil anime les anneaux sans qu on touche`() {
        val accueil = source("MainActivity.kt")
        val reprise = accueil.substringAfter("override fun onResume()").substringBefore("\n    }")
        assertTrue("onResume doit rejouer les anneaux", reprise.contains("R.id.week_gauge).rejouer()"))
        assertTrue(accueil.contains("anneaux.surRempli = { apresRemplissage() }"))
        assertTrue(accueil.contains("Anneaux.aFeter(etat, aujourdhui, Store.anneauxFetes(this))"))
        val vue = source("AnneauxView.kt")
        assertTrue("un état changé doit se remplir à nouveau", vue.contains("etat != nouvel"))
        assertTrue(vue.contains("fun feter(quel: Anneaux.Quel)"))
    }
}
