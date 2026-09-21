package com.myhitbodedut

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * La seance ouverte. Ce qu'elle promet : pas de duree, on arrete quand on
 * arrete, et la duree reelle est tout de meme enregistree — sans quoi la
 * serie et le total cesseraient de compter.
 */
class SessionRuleTest {

    private val minute = 60_000L
    private val debut = 1_700_000_000_000L

    @Test
    fun `une seance minutee s arrete a son terme`() {
        val fin = debut + 20 * minute
        assertTrue(SessionRule.enCours(debut, fin, 20, fin - 1))
        assertFalse(SessionRule.enCours(debut, fin, 20, fin))
        assertFalse(SessionRule.enCours(debut, fin, 20, fin + minute))
    }

    /**
     * Le coeur de l'affaire : aucune fin inscrite ne doit interrompre une
     * seance ouverte, meme des heures plus tard.
     */
    @Test
    fun `une seance ouverte ne s arrete pas d elle-meme`() {
        assertTrue(SessionRule.estOuverte(debut, SessionRule.OUVERTE))
        listOf(1L, 60L, 600L, 86_400L).forEach { secondes ->
            assertTrue(
                "elle doit courir encore apres $secondes s",
                SessionRule.enCours(debut, 0L, SessionRule.OUVERTE, debut + secondes * 1_000L)
            )
        }
    }

    /** Sans seance en cours, rien n'est ouvert : le debut a zero le dit. */
    @Test
    fun `aucune seance n est une seance ouverte`() {
        assertFalse(SessionRule.estOuverte(0L, SessionRule.OUVERTE))
        assertFalse(SessionRule.enCours(0L, 0L, SessionRule.OUVERTE, debut))
    }

    /** La duree reelle est mesuree de la meme facon dans les deux cas. */
    @Test
    fun `la duree reelle est mesuree depuis le debut`() {
        assertEquals(0, SessionRule.minutesEcoulees(debut, debut + 59_999L))
        assertEquals(1, SessionRule.minutesEcoulees(debut, debut + minute))
        assertEquals(47, SessionRule.minutesEcoulees(debut, debut + 47 * minute + 30_000L))
        // Une horloge qui recule ne doit pas produire de duree negative.
        assertEquals(0, SessionRule.minutesEcoulees(debut, debut - minute))
        assertEquals(0, SessionRule.minutesEcoulees(0L, debut))
    }

    @Test
    fun `le reste est arrondi au superieur et jamais negatif`() {
        val fin = debut + 10 * minute
        assertEquals(10, SessionRule.minutesRestantes(fin, debut))
        assertEquals(1, SessionRule.minutesRestantes(fin, fin - 1_000L))
        assertEquals(0, SessionRule.minutesRestantes(fin, fin))
        assertEquals(0, SessionRule.minutesRestantes(fin, fin + minute))
    }

    /** Les libelles que l'ecran et la notification promettent. */
    @Test
    fun `les libelles de la seance ouverte existent dans les trois langues`() {
        val cles = listOf(
            "session_open", "session_open_caption", "session_elapsed",
            "blocker_session_open_subtitle"
        )
        listOf("values", "values-en", "values-iw").forEach { dossier ->
            val xml = File("src/main/res/$dossier/strings.xml").readText()
            cles.forEach {
                assertTrue("$dossier : « $it » manquant", xml.contains("""name="$it""""))
            }
        }
    }

    /** Le bouton doit exister dans l'ecran de seance, sinon il n'est nulle part. */
    @Test
    fun `l ecran de seance porte le bouton sans duree`() {
        val xml = File("src/main/res/layout/activity_session.xml").readText()
        assertTrue(xml.contains("""android:id="@+id/session_open""""))
        assertTrue(xml.contains("""android:text="@string/session_open""""))
    }
}

/**
 * Le compteur masque. Ce qui est promis : rien a l'ecran pendant la
 * seance. Le piege serait de retirer le chiffre de l'ecran et de le
 * laisser dans la notification en cours ou sur l'ecran de barrage, ou il
 * se lit tout aussi bien.
 */
class SessionCounterTest {

    @Test
    fun `masque, le compteur ne dit rien, seance ouverte ou non`() {
        assertEquals(SessionRule.Compteur.MASQUE, SessionRule.compteur(false, true))
        assertEquals(SessionRule.Compteur.MASQUE, SessionRule.compteur(true, true))
    }

    @Test
    fun `sans masquage, chaque seance dit ce qu elle a`() {
        assertEquals(SessionRule.Compteur.RESTE, SessionRule.compteur(false, false))
        assertEquals(SessionRule.Compteur.ECOULE, SessionRule.compteur(true, false))
    }

    /**
     * Les trois endroits qui affichent un compteur doivent avoir un texte
     * pour le cas masque : sans lui, l'un d'eux retomberait sur le chiffre.
     */
    @Test
    fun `les trois endroits ont un libelle pour le compteur masque`() {
        val cles = listOf(
            "session_hidden_caption", "session_running_hidden",
            "blocker_session_hidden_subtitle",
            "settings_hide_countdown", "settings_hide_countdown_hint"
        )
        listOf("values", "values-en", "values-iw").forEach { dossier ->
            val xml = File("src/main/res/$dossier/strings.xml").readText()
            cles.forEach {
                assertTrue("$dossier : « $it » manquant", xml.contains("""name="$it""""))
            }
        }
    }

    /** Le reglage doit etre atteignable : un interrupteur dans l'ecran principal. */
    @Test
    fun `le reglage a son interrupteur dans l ecran principal`() {
        val xml = File("src/main/res/layout/activity_main.xml").readText()
        assertTrue(xml.contains("""android:id="@+id/hide_countdown_switch""""))
        assertTrue(xml.contains("@string/settings_hide_countdown"))
    }
}
