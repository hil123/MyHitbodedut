package com.myhitbodedut

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * La hitbodedout guidee : les quatre temps, leur parcours, et le fait
 * qu'ils sont bien ceux de Likoutei Moharan II, 25 et pas une invention
 * qui prendrait sa caution.
 */
class HitbodedutTest {

    @Test
    fun `les cinq temps sont distincts et complets`() {
        val mouvements = Hitbodedut.MOUVEMENTS
        assertEquals(5, mouvements.size)
        assertEquals(mouvements.size, mouvements.map { it.cle }.toSet().size)
        assertEquals(mouvements.size, mouvements.map { it.he }.toSet().size)
        assertEquals(mouvements.size, mouvements.map { it.titre }.toSet().size)
        assertEquals(mouvements.size, mouvements.map { it.invite }.toSet().size)
        mouvements.forEach {
            assertTrue("« ${it.cle} » n'a pas d'hébreu", it.he.isNotBlank())
            assertTrue("« ${it.cle} » n'a pas d'illustration", it.illustration != 0)
            assertTrue("« ${it.cle} » n'a pas de source", it.source.isNotBlank())
        }
        assertEquals(mouvements.size, mouvements.map { it.illustration }.toSet().size)
    }

    /**
     * Les cinq intitules hebreux sont tires de leur source, mot pour mot,
     * points-voyelles compris. Ce test les fige : une reformulation qui
     * garderait la reference sous une phrase inventee passerait sinon
     * inapercue.
     */
    @Test
    fun `les intitules sont ceux de leurs sources`() {
        val attendus = listOf(
            "sihah" to "לְפָרֵשׁ שִׂיחָתוֹ",
            "harata" to "חֲרָטָה וּתְשׁוּבָה עַל הֶעָבָר",
            "bakasha" to "בַּקָּשַׁת תַחֲנוּנִים",
            "torah" to "לַעֲשׂוֹת מֵהַתּוֹרָה תְּפִלָּה",
            "hodaa" to "נוֹתֵן שֶׁבַח וְהוֹדָיָה",
        )
        assertEquals(attendus.map { it.first }, Hitbodedut.MOUVEMENTS.map { it.cle })
        attendus.forEach { (cle, he) ->
            assertEquals(he, Hitbodedut.MOUVEMENTS.first { it.cle == cle }.he)
        }
        // Les sources suivent la langue de l'interface (lot textes 2) : en
        // hébreu, rien que de l'hébreu, numéros en lettres.
        assertEquals("ליקוטי מוהר״ן תניינא, כ״ה", Reference.localise(Hitbodedut.SOURCE_LIKOUTEI, "he"))
        assertEquals("רמב״ם, הלכות תפילה א׳, ב׳", Reference.localise(Hitbodedut.SOURCE_RAMBAM, "he"))
        assertEquals("Likutei Moharan II, 25", Reference.localise(Hitbodedut.SOURCE_LIKOUTEI, "en"))
        assertEquals("Rambam, Hilkhot Tefillah 1:2", Reference.localise(Hitbodedut.SOURCE_RAMBAM, "en"))
    }

    /**
     * L'action de grace n'est pas dans Likoutei Moharan II, 25 : elle ne
     * doit donc pas s'en reclamer. Les quatre autres en viennent, et
     * doivent s'en reclamer. C'est la seule facon de ne pas preter au
     * chapitre une caution qu'il ne donne pas.
     */
    @Test
    fun `chaque temps se reclame de la source dont il vient`() {
        val parCle = Hitbodedut.MOUVEMENTS.associateBy { it.cle }
        listOf("sihah", "harata", "bakasha", "torah").forEach {
            assertEquals("« $it » vient du chapitre", Hitbodedut.SOURCE_LIKOUTEI, parCle.getValue(it).source)
        }
        assertEquals(Hitbodedut.SOURCE_RAMBAM, parCle.getValue("hodaa").source)
    }

    /**
     * Le Rambam place l'action de grace apres la demande, pas avant :
     * « וְאַחַר כָּךְ נוֹתֵן שֶׁבַח וְהוֹדָיָה ». L'ordre affiche doit le suivre,
     * sinon la reference ne tient plus.
     */
    @Test
    fun `l action de grace vient apres la demande`() {
        val cles = Hitbodedut.MOUVEMENTS.map { it.cle }
        assertTrue(cles.indexOf("hodaa") > cles.indexOf("bakasha"))
        assertTrue(Hitbodedut.estDernier(cles.indexOf("hodaa")))
    }

    /** On avance, on recule, et on ne sort jamais des quatre. */
    @Test
    fun `le parcours reste dans ses bornes`() {
        assertEquals(4, Hitbodedut.DERNIER)
        assertEquals(1, Hitbodedut.suivant(0))
        assertEquals(4, Hitbodedut.suivant(3))
        assertEquals(4, Hitbodedut.suivant(4))
        assertEquals(4, Hitbodedut.suivant(99))
        assertEquals(0, Hitbodedut.precedent(1))
        assertEquals(0, Hitbodedut.precedent(0))
        assertEquals(0, Hitbodedut.precedent(-5))
        assertTrue(Hitbodedut.estPremier(0))
        assertFalse(Hitbodedut.estPremier(1))
        assertTrue(Hitbodedut.estDernier(4))
        assertFalse(Hitbodedut.estDernier(3))
    }

    /** Un indice aberrant rend un temps, jamais une exception. */
    @Test
    fun `un indice hors bornes ne rompt pas`() {
        assertEquals(Hitbodedut.MOUVEMENTS.first(), Hitbodedut.mouvement(-1))
        assertEquals(Hitbodedut.MOUVEMENTS.last(), Hitbodedut.mouvement(400))
    }

    @Test
    fun `les libelles existent dans les trois langues`() {
        val cles = listOf(
            "settings_hitbodedut", "settings_hitbodedut_hint", "hitbodedut_step",
            "hitbodedut_previous", "hitbodedut_next",
            "hitbodedut_sihah", "hitbodedut_sihah_invite",
            "hitbodedut_harata", "hitbodedut_harata_invite",
            "hitbodedut_bakasha", "hitbodedut_bakasha_invite",
            "hitbodedut_torah", "hitbodedut_torah_invite",
            "hitbodedut_hodaa", "hitbodedut_hodaa_invite",
        )
        listOf("values", "values-en", "values-iw").forEach { dossier ->
            val xml = File("src/main/res/$dossier/strings.xml").readText()
            cles.forEach {
                assertTrue("$dossier : « $it » manquant", xml.contains("""name="$it""""))
            }
        }
    }

    /** Chaque vue que la carte remplit doit exister dans son gabarit. */
    @Test
    fun `le gabarit porte toutes les vues que la carte remplit`() {
        val gabarit = File("src/main/res/layout/view_hitbodedut.xml").readText()
        val code = File("src/main/java/com/myhitbodedut/HitbodedutCard.kt").readText()
        val attendues = Regex("""R\.id\.(hitbodedut_[a-z_]+)""").findAll(code)
            .map { it.groupValues[1] }.toSet()
        assertEquals(8, attendues.size)
        attendues.forEach {
            assertTrue(
                "« $it » manque dans view_hitbodedut.xml",
                gabarit.contains("""android:id="@+id/$it""""),
            )
        }
    }

    /** L'hebreu doit etre pose de droite a gauche, comme sur la carte de 'hizouk. */
    @Test
    fun `l hebreu de la carte est en direction droite-a-gauche`() {
        val gabarit = File("src/main/res/layout/view_hitbodedut.xml").readText()
        val bloc = gabarit.substringAfter("""android:id="@+id/hitbodedut_he"""")
            .substringBefore("/>")
        assertTrue("layoutDirection=rtl manquant", bloc.contains("""android:layoutDirection="rtl""""))
        assertTrue("textAlignment=viewStart manquant", bloc.contains("viewStart"))
    }

    /**
     * La carte doit etre posee dans l'ecran de seance, et son reglage
     * atteignable. Le reglage vit dans l'ecran des reglages depuis qu'il
     * existe ; ce qui compte est qu'il existe quelque part, pas ou.
     */
    @Test
    fun `la carte et son reglage sont en place`() {
        val seance = File("src/main/res/layout/activity_session.xml").readText()
        assertTrue(seance.contains("""android:id="@+id/session_hitbodedut""""))
        assertTrue(seance.contains("@layout/view_hitbodedut"))
        val reglages = File("src/main/res/layout/activity_settings.xml").readText()
        assertTrue(reglages.contains("""android:id="@+id/guided_switch""""))
        assertTrue(reglages.contains("@string/settings_hitbodedut"))
    }
}
