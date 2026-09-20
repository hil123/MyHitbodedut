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
        assertEquals(item.fr, ChizukCard.translationOf(item, "es"))
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
