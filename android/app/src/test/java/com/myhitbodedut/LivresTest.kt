package com.myhitbodedut

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Les livres entiers de la bibliothèque (12/10, tools/livres_construction.py) :
 * Likoutei Etsot sans la vocalisation de licence non déclarée, Sefer HaMiddot
 * vocalisé (Public Domain), chacun lisible par le lecteur du siddour.
 */
class LivresTest {

    private val livres = SiddurLoader.parse(File("src/main/res/raw/livres.json").readText())

    @Test
    fun `les livres sont la, chapitre par chapitre`() {
        assertEquals(
            listOf("guide_hitbodedut", "likoutei_etsot", "sefer_hamidot", "likoutei_moharan", "hichtapkhout", "kokhvei_or",
                "tefila_lishlomo", "meguilat_esther", "chir_hachirim", "nichmat", "perek_chira", "yalkut_yosef_kitsour"),
            livres.textes.map { it.id },
        )
        // Le nombre de chapitres de chaque livre, tel que l'imprime son édition (13/10).
        val chapitres = mapOf("likoutei_moharan" to 286 + 125, "hichtapkhout" to 105, "meguilat_esther" to 10,
            "chir_hachirim" to 8, "nichmat" to 1, "perek_chira" to 7,
            // Les 1 048 simanim du Kitsour Yalkut Yosef, le crédit en tête du premier (19/10).
            "yalkut_yosef_kitsour" to 1048,
            // Le guide du propriétaire, un chapitre par partie ; les dix feuillets du Yanuka (19/10).
            "guide_hitbodedut" to 18, "tefila_lishlomo" to 10)
        chapitres.forEach { (id, n) -> assertEquals(id, n, livres.texte(id)!!.sections.size) }
        assertTrue(livres.texte("likoutei_etsot")!!.sections.size > 50)
        assertTrue(livres.texte("sefer_hamidot")!!.sections.size > 50)
        livres.textes.forEach { t ->
            assertTrue(t.id, t.sections.all { it.titre.isNotBlank() && it.blocs.isNotEmpty() })
            assertTrue(t.id, t.titre.he.isNotBlank() && t.titre.fr.isNotBlank() && t.titre.en.isNotBlank())
        }
    }

    @Test
    fun `likoutei etsot sans nikkoud, sefer hamiddot vocalise`() {
        val voyelles = Regex("[\\u05B0-\\u05BC\\u05C1\\u05C2\\u05C7]")
        val etsot = livres.texte("likoutei_etsot")!!.sections.flatMap { it.blocs }
        assertTrue(etsot.none { voyelles.containsMatchIn(it.texte) })
        val midot = livres.texte("sefer_hamidot")!!.sections.flatMap { it.blocs }.filter { !it.consigne }
        assertTrue(midot.count { voyelles.containsMatchIn(it.texte) } > midot.size * 9 / 10)
    }

    @Test
    fun `les ajouts du 13 10 sont vocalises, deux groupes`() {
        val voyelles = Regex("[\\u05B0-\\u05BC\\u05C1\\u05C2\\u05C7]")
        listOf("likoutei_moharan", "hichtapkhout", "kokhvei_or", "meguilat_esther", "chir_hachirim", "nichmat", "perek_chira").forEach { id ->
            val blocs = livres.texte(id)!!.sections.flatMap { it.blocs }.filter { !it.consigne && it.texte.length > 40 }
            assertTrue(id, blocs.count { voyelles.containsMatchIn(it.texte) } > blocs.size * 9 / 10)
        }
        assertEquals(listOf("livres", "meguilot", "halakha"), livres.groupes.map { it.cle })
        assertTrue(livres.texte("meguilat_esther")!!.sections[0].blocs[0].texte.startsWith("(א) וַיְהִי בִּימֵי אֲחַשְׁוֵרוֹשׁ"))
        assertTrue(livres.texte("nichmat")!!.sections[0].blocs[0].texte.contains("נִשְׁמַת כָּל חַי"))
    }

    /**
     * Les deux textes du propriétaire (19/10, tools/livres_textes_proprietaire.py) :
     * rien des notes de transcription en français n'y passe, et les feuillets du
     * Yanuka, qui sont des prières, ont le Nom en entier hors des consignes.
     */
    @Test
    fun `le guide et les feuillets du yanuka, sans notes ni abreviations`() {
        val latin = Regex("[A-Za-zÀ-ÿ]{3,}")
        listOf("guide_hitbodedut", "tefila_lishlomo").forEach { id ->
            livres.texte(id)!!.sections.flatMap { it.blocs }.forEach { b ->
                val nu = b.texte.replace(Regex("<[^>]+>"), "").replace("MyHitbodedut", "")
                assertTrue("$id : $nu", !latin.containsMatchIn(nu) && !nu.contains("**") && !nu.startsWith("#"))
            }
        }
        val abrege = Regex("(?<![\\u05D0-\\u05EA\\u0591-\\u05C7])[\\u05D5\\u05D1\\u05DC\\u05DE\\u05E9\\u05DB]{0,2}[\\u0591-\\u05C7]*\\u05D4['\\u05F3]|\\u05D0[\\u0591-\\u05C7]*-[\\u05DC\\u05D3]")
        val yanuka = livres.texte("tefila_lishlomo")!!
        yanuka.sections.flatMap { it.blocs }.filter { !it.consigne }.forEach { b ->
            assertTrue(b.texte, !abrege.containsMatchIn(b.texte))
        }
        assertTrue(yanuka.sections.all { it.blocs.first().consigne && it.blocs.first().texte.contains("שליט״א") })
        assertTrue(yanuka.sections[2].blocs[1].texte.startsWith("אֱלֹהֵי אַבְרָהָם"))
    }
}
