package com.myhitbodedut

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Le livre de la halakha (14/10) : ce que montre chaque choix, et les clés partagées avec le web. */
class PersonnalisationTest {

    @Test
    fun `chaque choix montre son livre`() {
        assertTrue(Store.SourceHalakha.YALKUT.yalkut); assertFalse(Store.SourceHalakha.YALKUT.benIshHai)
        assertFalse(Store.SourceHalakha.BEN_ISH_HAI.yalkut); assertTrue(Store.SourceHalakha.BEN_ISH_HAI.benIshHai)
        assertTrue(Store.SourceHalakha.LES_DEUX.yalkut); assertTrue(Store.SourceHalakha.LES_DEUX.benIshHai)
    }

    @Test
    fun `memes cles que le web`() {
        assertEquals(listOf("yalkut", "ben_ish_hai", "les_deux"), Store.SourceHalakha.values().map { it.cle })
        assertEquals(listOf("aujourdhui", "textes", "zmanim"), Store.Ouverture.values().map { it.cle })
    }
}
