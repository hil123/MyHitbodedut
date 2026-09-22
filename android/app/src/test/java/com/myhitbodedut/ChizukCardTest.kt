package com.myhitbodedut

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Point 4 : la traduction suit la langue de l'interface. */
class ChizukCardTest {

    private val item = DataFiles.chizuk.items.first()

    @Test
    fun `en francais, la traduction francaise`() {
        assertEquals(item.fr, ChizukCard.translationOf(item, "fr"))
    }

    @Test
    fun `en anglais, la traduction anglaise`() {
        assertEquals(item.en, ChizukCard.translationOf(item, "en"))
    }

    @Test
    fun `en hebreu, pas de traduction`() {
        // L'hébreu est déjà au-dessus ; le répéter mangerait la carte.
        assertEquals("", ChizukCard.translationOf(item, "he"))
        // « iw » est l'ancien code d'Android pour l'hébreu, encore rendu.
        assertEquals("", ChizukCard.translationOf(item, "iw"))
    }

    @Test
    fun `une langue inconnue retombe sur le francais`() {
        // « es » servait d'exemple ici, avant que l'espagnol ne devienne une
        // langue de l'application. Un test qui illustre une regle par un cas
        // devenu faux ne protege plus rien : il fallait un inconnu reel.
        assertEquals(item.fr, ChizukCard.translationOf(item, "de"))
        assertEquals(item.fr, ChizukCard.translationOf(item, "fr"))
    }

    @Test
    fun `l espagnol est servi en espagnol, et en anglais tant qu il manque`() {
        val avecEspagnol = item.copy(es = "texto")
        assertEquals("texto", ChizukCard.translationOf(avecEspagnol, "es"))
        assertEquals(
            "sans texte espagnol, le repli doit etre declare, pas le francais par defaut",
            item.en,
            ChizukCard.translationOf(item.copy(es = ""), "es"),
        )
    }

    @Test
    fun `les textes espagnols ne sont pas encore ecrits, et cela se sait`() {
        // Ce test ne protege pas un acquis, il tient un compte. L'interface
        // est traduite en espagnol ; les trente-sept textes de 'hizouk ne le
        // sont pas. Le jour ou ils le seront, ce test tombera — et c'est
        // ainsi qu'on saura qu'il faut le remplacer par son contraire.
        val ecrits = DataFiles.chizuk.items.count { it.es.isNotBlank() }
        assertEquals(
            "des textes espagnols existent maintenant : remplacer ce test par " +
                "« chaque texte a ses trois traductions »",
            0,
            ecrits,
        )
    }

    @Test
    fun `chaque texte a ses deux traductions`() {
        DataFiles.chizuk.items.forEach {
            assertTrue("${it.id} : pas de français", it.fr.isNotBlank())
            assertTrue("${it.id} : pas d'anglais", it.en.isNotBlank())
            assertTrue("${it.id} : pas de référence", it.ref.isNotBlank())
        }
    }
}
