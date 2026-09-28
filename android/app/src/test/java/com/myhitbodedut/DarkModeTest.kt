package com.myhitbodedut

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Le mode sombre. Ce qui casse un mode sombre n'est pas le fond, c'est
 * l'encre : une couleur qui s'eclaircit la nuit pour se lire sur une
 * carte sombre devient illisible si elle est posee sur une plaque restee
 * claire. Cela ne leve rien a la compilation et ne se voit qu'en
 * rallumant l'ecran la nuit.
 */
class DarkModeTest {

    private val ANDROID = "http://schemas.android.com/apk/res/android"

    /** Les plaques qui restent claires la nuit, parce qu'une illustration s'y pose. */
    private val plaques = setOf(
        "@drawable/bg_chip_mint", "@drawable/bg_tip_number",
        "@drawable/bg_stage", "@drawable/bg_stage_soft",
    )

    private fun couleurs(dossier: String): Set<String> {
        val xml = File("src/main/res/$dossier/colors.xml").readText()
        return Regex("""<color name="([a-z0-9_]+)"""").findAll(xml)
            .map { it.groupValues[1] }.toSet()
    }

    private fun vues(fichier: File): List<org.w3c.dom.Element> {
        val racine = DocumentBuilderFactory.newInstance()
            .apply { isNamespaceAware = true }
            .newDocumentBuilder().parse(fichier).documentElement
        val tous = mutableListOf<org.w3c.dom.Element>()
        fun descendre(e: org.w3c.dom.Element) {
            tous.add(e)
            val enfants = e.childNodes
            for (i in 0 until enfants.length) {
                (enfants.item(i) as? org.w3c.dom.Element)?.let { descendre(it) }
            }
        }
        descendre(racine)
        return tous
    }

    private fun attribut(e: org.w3c.dom.Element, nom: String): String? =
        e.getAttributeNS(ANDROID, nom).takeIf { it.isNotBlank() }

    /**
     * Le nom de la couleur, qu'elle soit écrite « @color/on_plate » ou, depuis
     * que les couleurs se choisissent, « ?attr/mhOnPlate ».
     */
    private fun jeton(valeur: String): String =
        if (valeur.startsWith("?attr/mh")) {
            valeur.removePrefix("?attr/mh")
                .replace(Regex("([a-z])([A-Z])"), "$1_$2").lowercase()
        } else {
            valeur.removePrefix("@color/")
        }

    /**
     * Le test qui compte. Aucune encre posee sur une plaque restee claire
     * ne doit changer de valeur la nuit.
     */
    @Test
    fun `aucune encre sur plaque claire ne bascule la nuit`() {
        val basculent = couleurs("values-night")
        val fautes = mutableListOf<String>()
        File("src/main/res/layout").listFiles { f -> f.extension == "xml" }!!
            .sortedBy { it.name }
            .forEach { fichier ->
                vues(fichier).forEach { vue ->
                    val fond = attribut(vue, "background") ?: return@forEach
                    if (fond !in plaques) return@forEach
                    val encre = attribut(vue, "textColor")?.let { jeton(it) }
                        ?: return@forEach
                    if (encre in basculent) {
                        val id = attribut(vue, "id")?.removePrefix("@+id/") ?: "?"
                        fautes.add("${fichier.name} : « $id » écrit en « $encre » sur $fond")
                    }
                }
            }
        assertTrue(
            "encre illisible la nuit :\n" + fautes.joinToString("\n"),
            fautes.isEmpty(),
        )
    }

    /** Sans thème DayNight, tout values-night est du poids mort. */
    @Test
    fun `le theme suit le mode de l appareil`() {
        val themes = File("src/main/res/values/themes.xml").readText()
        assertTrue(
            "le thème doit être DayNight",
            themes.contains("Theme.Material3.DayNight"),
        )
    }

    /** Une couleur de nuit sans jour correspondant ne s'appliquerait jamais. */
    @Test
    fun `chaque couleur de nuit existe aussi le jour`() {
        val jour = couleurs("values")
        val nuit = couleurs("values-night")
        assertTrue("values-night est vide", nuit.isNotEmpty())
        val orphelines = nuit - jour
        assertEquals("couleurs de nuit sans jour : $orphelines", emptySet<String>(), orphelines)
    }

    /**
     * Les quatre jetons qui ne doivent jamais basculer. « white » et
     * « mint » sont l'encre des fonds vert foncé, déjà sombres le jour ;
     * « on_plate » et « stage_soft » tiennent les plaques claires.
     */
    @Test
    fun `les jetons fixes ne sont pas redefinis la nuit`() {
        val nuit = couleurs("values-night")
        listOf("white", "mint", "on_plate", "stage_soft", "brand_dark").forEach {
            assertTrue("« $it » ne doit pas être redéfini la nuit", it !in nuit)
        }
    }

    /**
     * Le bouton principal se remplit de « brand », qui s'éclaircit la
     * nuit : son encre doit donc pouvoir s'assombrir, ce que « white »
     * ne peut pas.
     */
    @Test
    fun `l encre du bouton principal peut basculer`() {
        val themes = File("src/main/res/values/themes.xml").readText()
        assertTrue(
            "colorOnPrimary doit être un jeton qui bascule",
            themes.contains("""<item name="colorOnPrimary">@color/on_brand</item>"""),
        )
        assertTrue("on_brand doit être redéfini la nuit", "on_brand" in couleurs("values-night"))
        assertTrue("brand doit être redéfini la nuit", "brand" in couleurs("values-night"))
    }
}
