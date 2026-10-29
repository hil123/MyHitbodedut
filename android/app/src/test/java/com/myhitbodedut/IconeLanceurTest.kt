package com.myhitbodedut

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * L'icône de lancement selon צדיק / צדקת et la couleur des boutons (29/10) :
 * une entrée du manifeste par combinaison, chacune avec son icône, sur une
 * plaque claire (la frange du détourage, voir LauncherIconTest).
 */
class IconeLanceurTest {

    private val manifeste = File("src/main/AndroidManifest.xml").readText()
    private val couleurs = File("src/main/res/values/colors.xml").readText()
    private val palettes = File("src/main/res/values/palettes.xml").readText()

    private fun couleur(nom: String, xml: String = couleurs): String? =
        Regex("""<color name="$nom">#([0-9A-Fa-f]{6})</color>""").find(xml)?.groupValues?.get(1)

    @Test
    fun `une entree par combinaison, le hassid sur le vert par defaut`() {
        val alias = Regex("""<activity-alias\s+android:name="\.(Icone_[a-z]+_[a-z]+)"\s+android:enabled="(true|false)"""")
            .findAll(manifeste).associate { it.groupValues[1] to (it.groupValues[2] == "true") }
        assertEquals(IconeLanceur.tous().map { it.removePrefix("com.myhitbodedut.") }.sorted(), alias.keys.sorted())
        assertEquals(listOf("Icone_m_vert"), alias.filterValues { it }.keys.toList())
        assertEquals("com.myhitbodedut.Icone_m_vert", IconeLanceur.PAR_DEFAUT)
        assertEquals("com.myhitbodedut.Icone_f_bordeaux", IconeLanceur.alias(Adresse.FEMME, Apparence.Accent.BORDEAUX))
        assertEquals("com.myhitbodedut.Icone_m_bleu", IconeLanceur.alias(null, Apparence.Accent.BLEU))
        // Une seule porte d'entrée : MainActivity ne se lance que par les alias.
        assertEquals(IconeLanceur.tous().size, Regex("category.LAUNCHER").findAll(manifeste).count())
    }

    @Test
    fun `chaque icone a son visage et la plaque claire de sa palette`() {
        listOf("m" to "ic_launcher_face", "f" to "ic_launcher_face_f").forEach { (g, visage) ->
            assertTrue(File("src/main/res/drawable-nodpi/$visage.webp").exists())
            Apparence.Accent.values().forEach { a ->
                val nom = "ic_launcher_${g}_${a.cle}"
                assertTrue("$nom dans le manifeste", "@mipmap/$nom\"" in manifeste)
                val xml = File("src/main/res/mipmap-anydpi/$nom.xml").readText()
                assertTrue(nom, "@drawable/$visage\"" in xml && "@color/icone_fond_${a.cle}\"" in xml)
                val hex = couleur("icone_fond_${a.cle}")!!
                val attendu = if (a == Apparence.Accent.VERT) couleur("mint")!!
                else Regex("""<style name="Accent\.${a.cle.replaceFirstChar { it.uppercase() }}"[\s\S]*?<item name="mhMint">#([0-9A-Fa-f]{6})""")
                    .find(palettes)!!.groupValues[1]
                assertEquals(nom, attendu.uppercase(), hex.uppercase())
                val clarte = (0..2).sumOf { hex.substring(it * 2, it * 2 + 2).toInt(16) } / 3
                assertTrue("$nom : fond trop sombre", clarte >= 200)
            }
        }
    }

    @Test
    fun `les reglages qui changent l icone la mettent a jour`() {
        val store = File("src/main/java/com/myhitbodedut/Store.kt").readText()
        listOf("fun setAdresse", "fun setAccent").forEach { f ->
            val corps = store.substringAfter(f).substringBefore("\n    }")
            assertTrue(f, "IconeLanceur.appliquer(context)" in corps)
        }
        assertTrue("IconeLanceur.appliquer(this)" in File("src/main/java/com/myhitbodedut/MyHitbodedutApp.kt").readText())
    }
}
