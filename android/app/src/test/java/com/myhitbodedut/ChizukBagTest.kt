package com.myhitbodedut

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Point 4 : la rotation ne répète pas, et survit à la fermeture de l'app. */
class ChizukBagTest {

    private val pool = listOf("a", "b", "c", "d")

    /** Mélange déterministe : laisse l'ordre tel quel. */
    private val identite: (List<String>) -> List<String> = { it }

    @Test
    fun `un cycle sort chaque texte une fois et une seule`() {
        val sortis = mutableListOf<String>()
        var reste = emptyList<String>()
        repeat(pool.size) {
            val draw = ChizukBag.next(reste, pool, sortis.lastOrNull(), identite)
            sortis += draw.id!!
            reste = draw.remaining
        }
        assertEquals(pool.toSet(), sortis.toSet())
        assertEquals("un texte est sorti deux fois : $sortis", pool.size, sortis.toSet().size)
        assertTrue("le sac devrait être vide", reste.isEmpty())
    }

    @Test
    fun `le cycle suivant ne rouvre pas sur le dernier texte du precedent`() {
        // Le sac est vide, le dernier sorti est « a », et le mélange le
        // remettrait en tête : il doit repartir en fin de sac.
        val draw = ChizukBag.next(emptyList(), pool, last = "a", shuffle = identite)
        assertNotEquals("a", draw.id)
        assertEquals("b", draw.id)
        assertTrue("« a » doit rester dans le cycle", draw.remaining.contains("a"))
    }

    @Test
    fun `un vivier d un seul texte le redonne`() {
        val draw = ChizukBag.next(emptyList(), listOf("a"), last = "a", shuffle = identite)
        assertEquals("a", draw.id)
    }

    @Test
    fun `un vivier vide ne rend rien`() {
        val draw = ChizukBag.next(listOf("a"), emptyList(), null, identite)
        assertNull(draw.id)
        assertTrue(draw.remaining.isEmpty())
    }

    @Test
    fun `un texte retire des donnees ne ressort pas du sac`() {
        // « z » traîne dans le sac enregistré mais n'est plus publiable.
        val draw = ChizukBag.next(listOf("z", "b"), pool, null, identite)
        assertEquals("b", draw.id)
        assertTrue(draw.remaining.none { it == "z" })
    }

    @Test
    fun `le sac enregistre reprend la ou il s etait arrete`() {
        val premier = ChizukBag.next(emptyList(), pool, null, identite)
        // Redémarrage : seul le contenu du sac est relu.
        val second = ChizukBag.next(premier.remaining, pool, premier.id, identite)
        assertEquals("a", premier.id)
        assertEquals("b", second.id)
    }

    @Test
    fun `chaque moment tire dans son propre vivier`() {
        val chizuk = DataFiles.chizuk
        Moment.entries.forEach { moment ->
            val ids = ChizukRules.forMoment(chizuk, moment, debug = true).map { it.id }
            val draw = ChizukBag.next(emptyList(), ids)
            assertTrue(
                "${moment.key} : « ${draw.id} » n'appartient pas à ce moment",
                draw.id in ids,
            )
        }
    }
}
