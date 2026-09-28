package com.myhitbodedut

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.math.pow

/**
 * Les palettes de l'écran « Apparence ».
 *
 * Le choix est limité à des palettes pour une raison que ce test tient :
 * chaque fond combiné à chaque couleur de boutons doit rester lisible, de
 * jour comme de nuit. Les seuils sont ceux de la palette verte d'origine,
 * qui les passe tous ; une palette ajoutée qui ferait moins bien échoue ici.
 */
class PaletteTest {

    private val ACCENT = listOf("mhBrand", "mhBrandDark", "mhMint", "mhOnBrand",
                                "mhOnPlate", "mhStageSoft", "mhDayDone")
    private val FOND = listOf("mhPaper", "mhCard", "mhLine", "mhInk", "mhMuted", "mhDayTodo")

    /** Plaques claires sous les illustrations, et l'encre qui s'y pose : jamais de bascule. */
    private val FIXES = listOf("mhBrandDark", "mhMint", "mhOnPlate", "mhStageSoft", "mhDayDone")

    private fun lire(chemin: String) = File("src/main/res/$chemin").readText()

    private fun couleurs(dossier: String): Map<String, String> =
        Regex("""<color name="([a-z0-9_]+)">(#[0-9A-Fa-f]{6,8})</color>""")
            .findAll(lire("$dossier/colors.xml"))
            .associate { it.groupValues[1] to it.groupValues[2] }

    /** Les styles d'un fichier de palettes : nom → (attribut → valeur). */
    private fun styles(dossier: String): Map<String, Map<String, String>> =
        Regex("""<style name="([A-Za-z.]+)"[^>]*>([\s\S]*?)</style>""")
            .findAll(lire("$dossier/palettes.xml"))
            .associate { style ->
                style.groupValues[1] to Regex("""<item name="([A-Za-z:]+)">([^<]+)</item>""")
                    .findAll(style.groupValues[2])
                    .associate { it.groupValues[1] to it.groupValues[2] }
            }

    /** La palette verte d'origine, lue dans colors.xml sous les noms d'attributs. */
    private fun origine(nuit: Boolean): Map<String, String> {
        val jour = couleurs("values")
        val c = if (nuit) jour + couleurs("values-night") else jour
        return (ACCENT + FOND).associateWith { attr ->
            val nom = attr.removePrefix("mh").replace(Regex("([a-z])([A-Z])"), "$1_$2").lowercase()
            c.getValue(nom)
        }
    }

    private fun luminance(hex: String): Double {
        val h = hex.removePrefix("#").takeLast(6)
        return listOf(0, 2, 4).map { i ->
            val c = h.substring(i, i + 2).toInt(16) / 255.0
            if (c <= 0.03928) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
        }.let { (r, g, b) -> 0.2126 * r + 0.7152 * g + 0.0722 * b }
    }

    private fun contraste(a: String, b: String): Double {
        val (x, y) = luminance(a) to luminance(b)
        return (maxOf(x, y) + 0.05) / (minOf(x, y) + 0.05)
    }

    private fun accents(nuit: Boolean): Map<String, Map<String, String>> {
        val origine = origine(nuit)
        return mapOf("Accent.Vert" to origine.filterKeys { it in ACCENT }) +
            styles(if (nuit) "values-night" else "values").filterKeys { it.startsWith("Accent.") }
    }

    private fun fonds(nuit: Boolean): Map<String, Map<String, String>> {
        val origine = origine(nuit)
        return mapOf("Fond.Vert" to origine.filterKeys { it in FOND }) +
            styles(if (nuit) "values-night" else "values").filterKeys { it.startsWith("Fond.") }
    }

    @Test
    fun `chaque combinaison reste lisible de jour comme de nuit`() {
        val fautes = mutableListOf<String>()
        fun exige(nom: String, encre: String, fond: String, seuil: Double) {
            val r = contraste(encre, fond)
            if (r < seuil) fautes += "$nom : ${"%.2f".format(r)} < $seuil"
        }
        listOf(false, true).forEach { nuit ->
            val moment = if (nuit) "nuit" else "jour"
            accents(nuit).forEach { (an, a) ->
                exige("$an $moment, bouton", a.getValue("mhOnBrand"), a.getValue("mhBrand"), 4.5)
                exige("$an $moment, plaque", a.getValue("mhOnPlate"), a.getValue("mhMint"), 4.5)
                exige("$an $moment, plaque douce", a.getValue("mhOnPlate"), a.getValue("mhStageSoft"), 4.5)
                exige("$an $moment, bandeau", a.getValue("mhMint"), a.getValue("mhBrandDark"), 4.5)
                exige("$an $moment, blanc sur bandeau", "#FFFFFF", a.getValue("mhBrandDark"), 4.5)
                exige("$an $moment, jour fait", "#FFFFFF", a.getValue("mhDayDone"), 4.5)
                fonds(nuit).forEach { (fn, f) ->
                    exige("$an × $fn $moment, titre sur carte", a.getValue("mhBrand"), f.getValue("mhCard"), 4.5)
                    exige("$an × $fn $moment, titre sur fond", a.getValue("mhBrand"), f.getValue("mhPaper"), 4.5)
                }
            }
            fonds(nuit).forEach { (fn, f) ->
                listOf("mhCard", "mhPaper", "mhDayTodo").forEach {
                    exige("$fn $moment, encre sur $it", f.getValue("mhInk"), f.getValue(it), 7.0)
                }
                listOf("mhCard", "mhPaper").forEach {
                    exige("$fn $moment, texte secondaire sur $it", f.getValue("mhMuted"), f.getValue(it), 4.0)
                }
            }
        }
        assertTrue("contrastes insuffisants :\n" + fautes.joinToString("\n"), fautes.isEmpty())
    }

    @Test
    fun `chaque palette existe de jour et de nuit avec les memes attributs`() {
        val jour = styles("values")
        val nuit = styles("values-night")
        assertEquals("palettes de jour et de nuit différentes", jour.keys, nuit.keys)
        jour.forEach { (nom, items) ->
            assertEquals("$nom : attributs de nuit différents", items.keys, nuit.getValue(nom).keys)
            val attendus = if (nom.startsWith("Accent.")) ACCENT else FOND
            assertTrue("$nom : il manque ${attendus - items.keys}", items.keys.containsAll(attendus))
        }
    }

    @Test
    fun `les plaques claires ne basculent pas la nuit`() {
        val jour = styles("values")
        val nuit = styles("values-night")
        jour.filterKeys { it.startsWith("Accent.") }.forEach { (nom, items) ->
            FIXES.forEach {
                assertEquals("$nom : « $it » change la nuit", items[it], nuit.getValue(nom)[it])
            }
        }
    }

    @Test
    fun `chaque choix de l ecran a sa palette et chaque palette son choix`() {
        val noms = styles("values").keys
        val code = File("src/main/java/com/myhitbodedut/Apparence.kt").readText()
        val references = Regex("""R\.style\.((Fond|Accent)_[A-Za-z]+)""").findAll(code)
            .map { it.groupValues[1].replace('_', '.') }.toSet()
        assertEquals(noms, references)
        assertEquals(noms.count { it.startsWith("Fond.") } + 1, Apparence.Fond.values().size)
        assertEquals(noms.count { it.startsWith("Accent.") } + 1, Apparence.Accent.values().size)
    }

    @Test
    fun `le theme de base remplit chaque attribut`() {
        val attrs = Regex("""<attr name="(mh[A-Za-z]+)"""").findAll(lire("values/attrs.xml"))
            .map { it.groupValues[1] }.toSet()
        assertEquals((ACCENT + FOND).toSet(), attrs)
        val theme = lire("values/themes.xml").substringAfter("""<style name="Theme.MyHitbodedut"""")
            .substringBefore("</style>")
        attrs.forEach { assertTrue("le thème de base ne remplit pas « $it »", theme.contains("""name="$it"""")) }
    }

    /**
     * Un écran qui reviendrait à « @color/brand » ne suivrait plus la palette
     * choisie, sans que rien ne le signale. Seul le widget le peut : il est
     * dessiné par l'écran d'accueil du téléphone, sans le thème de l'app.
     */
    @Test
    fun `aucun ecran de l application ne fige une couleur reglable`() {
        val reglables = Regex("""@color/(brand|brand_dark|mint|on_brand|on_plate|stage_soft|""" +
            """day_done|paper|card|line|ink|muted|day_todo)\b""")
        val hors = setOf(
            "widget_streak.xml", "widget_small.xml", "widget_large.xml",
            "bg_widget.xml", "bg_widget_button.xml", "bg_widget_day.xml", "bg_widget_day_done.xml",
        )
        val fautes = (File("src/main/res/layout").listFiles()!! + File("src/main/res/drawable").listFiles()!!)
            .filter { it.extension == "xml" && it.name !in hors }
            .flatMap { f -> reglables.findAll(f.readText()).map { "${f.name} : ${it.value}" } }
        assertTrue("couleurs figées : $fautes", fautes.isEmpty())
    }
}
