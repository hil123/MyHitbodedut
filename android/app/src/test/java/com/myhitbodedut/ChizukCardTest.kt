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
    fun `chaque texte a ses trois traductions`() {
        // Ce test a remplace celui qui comptait les textes espagnols absents.
        // Il tombait le jour ou ils seraient ecrits ; ils le sont.
        DataFiles.chizuk.items.forEach {
            assertTrue("${it.id} : pas de francais", it.fr.isNotBlank())
            assertTrue("${it.id} : pas d'anglais", it.en.isNotBlank())
            assertTrue("${it.id} : pas d'espagnol", it.es.isNotBlank())
        }
    }

    @Test
    fun `aucun texte espagnol n est une copie du francais ou de l anglais`() {
        // Le risque d'une traduction faite en lot n'est pas la faute de
        // langue, c'est la ligne oubliee et recopiee telle quelle.
        DataFiles.chizuk.items.forEach {
            assertTrue("${it.id} : l'espagnol recopie le francais", it.es != it.fr)
            assertTrue("${it.id} : l'espagnol recopie l'anglais", it.es != it.en)
        }
    }

    @Test
    fun `chaque texte espagnol est de longueur plausible`() {
        // Une traduction espagnole fait grosso modo la longueur de l'anglais.
        // Un tiers ou le triple signale une ligne tronquee ou dedoublee.
        DataFiles.chizuk.items.forEach {
            val rapport = it.es.length.toDouble() / it.en.length
            assertTrue(
                "${it.id} : espagnol ${it.es.length} car. contre ${it.en.length} en anglais",
                rapport in 0.6..1.8,
            )
        }
    }

    @Test
    fun `chaque texte a sa reference`() {
        DataFiles.chizuk.items.forEach {
            assertTrue("${it.id} : pas de référence", it.ref.isNotBlank())
        }
    }
}
