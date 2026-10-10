package com.myhitbodedut

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * La finition du 04/10 : ce qui fait passer l'accueil d'« appli maison » à
 * « appli finie ». Chaque point est vérifié là où il se voit.
 */
class FinitionTest {

    private fun lire(chemin: String) = File("src/main/$chemin").readText()

    @Test
    fun `les pistes vides prennent la couleur de leur anneau`() {
        assertTrue(AnneauxDessin.TEINTE_PISTE in 0.1f..0.4f)
        assertTrue(lire("java/com/myhitbodedut/AnneauxDessin.kt").contains("ColorUtils.blendARGB(couleurs.piste, couleur, TEINTE_PISTE)"))
    }

    @Test
    fun `la semaine se lit comme un calendrier`() {
        val accueil = lire("java/com/myhitbodedut/MainActivity.kt")
        assertTrue(accueil.contains("else -> day.date.dayOfMonth.toString()"))
        assertTrue(accueil.contains("R.drawable.bg_day_today"))
        assertTrue(lire("res/drawable/bg_day_today.xml").contains("<stroke"))
        // Le violet des jours de pause jurait avec le vert.
        assertTrue(!lire("res/values/colors.xml").contains("#EDE9F7"))
    }

    @Test
    fun `la ligne de statut separe et se tait a zero`() {
        val gabarit = lire("res/layout/activity_main.xml")
        assertTrue(gabarit.contains("@+id/status_separator"))
        assertTrue(lire("java/com/myhitbodedut/MainActivity.kt").contains("val avecTotal = total > 0"))
    }

    @Test
    fun `les pastilles s estompent au bord et les chiffres ne dansent pas`() {
        val gabarit = lire("res/layout/activity_main.xml")
        assertTrue(gabarit.contains("android:requiresFadingEdge=\"horizontal\""))
        assertTrue(Regex("fontFeatureSettings=\"tnum\"").findAll(gabarit).count() >= 3)
    }

    @Test
    fun `les anneaux grandissent sur les ecrans hauts`() {
        val haut = lire("res/values-h720dp/dimens.xml")
        val taille = Regex("""<dimen name="gauge_size">(\d+)dp""").find(haut)!!.groupValues[1].toInt()
        assertTrue(taille > 236)
    }

    @Test
    fun `chaque onglet a un dessin au repos et un dessin plein`() {
        listOf("today", "texts", "zmanim", "journal", "settings").forEach { nom ->
            val selecteur = lire("res/drawable/ic_nav_$nom.xml")
            assertTrue("$nom : pas de sélecteur", selecteur.contains("<selector"))
            assertTrue(selecteur.contains("@drawable/ic_nav_${nom}_on\" android:state_checked=\"true\""))
            assertTrue(lire("res/drawable/ic_nav_${nom}_on.xml").contains("android:fillColor"))
            assertTrue(lire("res/drawable/ic_nav_${nom}_off.xml").contains("android:strokeColor"))
        }
    }

    @Test
    fun `le doigt sent le toucher du centre et la fete`() {
        val accueil = lire("java/com/myhitbodedut/MainActivity.kt")
        assertEquals(2, Regex("performHapticFeedback").findAll(accueil).count())
        assertTrue(lire("java/com/myhitbodedut/AnneauxView.kt").contains("performHapticFeedback"))
    }
}
