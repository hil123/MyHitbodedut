package com.myhitbodedut

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** L'accueil du premier lancement, et la promesse qu'il porte. */
class OnboardingTest {

    @Test
    fun `les trois ecrans sont distincts et complets`() {
        val ecrans = Onboarding.ECRANS
        assertEquals(3, ecrans.size)
        assertEquals(ecrans.size, ecrans.map { it.cle }.toSet().size)
        assertEquals(ecrans.size, ecrans.map { it.titre }.toSet().size)
        assertEquals(ecrans.size, ecrans.map { it.corps }.toSet().size)
        assertEquals(ecrans.size, ecrans.map { it.illustration }.toSet().size)
    }

    @Test
    fun `le parcours reste dans ses bornes`() {
        assertEquals(1, Onboarding.suivant(0))
        assertEquals(2, Onboarding.suivant(2))
        assertEquals(2, Onboarding.suivant(50))
        assertEquals(0, Onboarding.precedent(0))
        assertEquals(0, Onboarding.precedent(-3))
        assertTrue(Onboarding.estPremier(0))
        assertTrue(Onboarding.estDernier(2))
        assertFalse(Onboarding.estDernier(1))
        assertEquals(Onboarding.ECRANS.first(), Onboarding.ecran(-1))
        assertEquals(Onboarding.ECRANS.last(), Onboarding.ecran(9))
    }

    /**
     * Le troisieme ecran affirme que l'application n'a aucun acces a
     * Internet. C'est la seule phrase de l'accueil qui puisse devenir
     * fausse sans que personne s'en apercoive : il suffirait qu'une
     * permission soit ajoutee un jour pour une fonction qui semble
     * anodine. Le test la tient au manifeste.
     */
    @Test
    fun `l application ne demande aucun acces reseau`() {
        val manifeste = File("src/main/AndroidManifest.xml").readText()
        listOf("android.permission.INTERNET", "android.permission.ACCESS_NETWORK_STATE").forEach {
            assertFalse(
                "l'accueil promet qu'aucune donnée ne sort ; « $it » le dément",
                manifeste.contains(it),
            )
        }
    }

    /** Sans son entree au manifeste, l'ecran ne s'ouvrirait pas. */
    @Test
    fun `l ecran d accueil est declare et ouvert au premier lancement`() {
        val manifeste = File("src/main/AndroidManifest.xml").readText()
        assertTrue(manifeste.contains("""android:name=".OnboardingActivity""""))
        val principal = File("src/main/java/com/myhitbodedut/MainActivity.kt").readText()
        assertTrue(principal.contains("showOnboardingOnce()"))
        assertTrue(principal.contains("OnboardingActivity::class.java"))
    }

    /** Une installation qui a deja servi ne doit pas voir l'accueil. */
    @Test
    fun `une installation existante est marquee sans voir l accueil`() {
        val principal = File("src/main/java/com/myhitbodedut/MainActivity.kt").readText()
        val bloc = principal.substringAfter("private fun showOnboardingOnce()")
            .substringBefore("\n    }")
        assertTrue("hasHistory doit être consulté", bloc.contains("Store.hasHistory"))
        assertTrue("et marquer l'accueil comme vu", bloc.contains("Store.setOnboarded(this, true)"))
    }

    @Test
    fun `le gabarit porte toutes les vues que l ecran remplit`() {
        val gabarit = File("src/main/res/layout/activity_onboarding.xml").readText()
        val code = File("src/main/java/com/myhitbodedut/OnboardingActivity.kt").readText()
        val attendues = Regex("""R\.id\.(onboarding_[a-z_]+)""").findAll(code)
            .map { it.groupValues[1] }.toSet()
        assertEquals(7, attendues.size)
        attendues.forEach {
            assertTrue("« $it » manque dans activity_onboarding.xml",
                gabarit.contains("""android:id="@+id/$it""""))
        }
    }

    @Test
    fun `les libelles existent dans les trois langues`() {
        val cles = listOf(
            "onboarding_step", "onboarding_next", "onboarding_back", "onboarding_skip",
            "onboarding_start", "onboarding_practice", "onboarding_practice_body",
            "onboarding_app", "onboarding_app_body",
            "onboarding_private", "onboarding_private_body",
        )
        listOf("values", "values-en", "values-iw").forEach { dossier ->
            val xml = File("src/main/res/$dossier/strings.xml").readText()
            cles.forEach {
                assertTrue("$dossier : « $it » manquant", xml.contains("""name="$it""""))
            }
        }
    }
}
