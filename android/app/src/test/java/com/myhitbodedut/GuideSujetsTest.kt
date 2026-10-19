package com.myhitbodedut

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** La hitbodedout guidée selon le sujet (proposition 2, 19/10). */
class GuideSujetsTest {

    private val sujets = JSONObject(File("src/main/res/raw/guide_sujets.json").readText()).getJSONObject("sujets")

    @Test
    fun `chaque sujet sauf autre a une invitation par temps, dans les trois langues`() {
        Theme.entries.filter { it != Theme.AUTRE }.forEach { t ->
            Hitbodedut.MOUVEMENTS.forEach { m ->
                listOf("fr", "en", "he").forEach { l ->
                    val s = GuideSujets.invitation(sujets, t, m.cle, l)
                    assertTrue("${t.cle} / ${m.cle} / $l", !s.isNullOrBlank() && !s.contains("'"))
                }
            }
        }
        assertEquals(Theme.entries.size - 1, sujets.length())
    }

    @Test
    fun `sans sujet, ou autre chose, le guide reste celui de toujours`() {
        assertNull(GuideSujets.invitation(sujets, null, "sihah", "fr"))
        assertNull(GuideSujets.invitation(sujets, Theme.AUTRE, "sihah", "fr"))
        assertEquals(GuideSujets.invitation(sujets, Theme.FAMILLE, "bakasha", "he"), GuideSujets.invitation(sujets, Theme.FAMILLE, "bakasha", "iw"))
    }
}
