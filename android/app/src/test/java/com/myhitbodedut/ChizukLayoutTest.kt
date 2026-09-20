package com.myhitbodedut

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Point 4 : « jamais de défilement ».
 *
 * La carte tient par construction : hauteur fixe et corps automatique. Une
 * hauteur passée à wrap_content, ou un autoSizeTextType retiré, rendrait la
 * promesse fausse sans rien casser d'autre — d'où ce garde-fou.
 */
class ChizukLayoutTest {

    private val layouts = listOf("view_chizuk.xml", "view_chizuk_compact.xml")

    @Test
    fun `la carte ne contient aucune vue defilante`() {
        forEachLayout { name, xml ->
            listOf("ScrollView", "NestedScrollView", "RecyclerView", "ListView").forEach { vue ->
                assertFalse("$name contient un $vue", xml.contains(vue))
            }
        }
    }

    @Test
    fun `les deux blocs de texte ont une hauteur fixe et un corps automatique`() {
        forEachLayout { name, xml ->
            listOf("chizuk_he", "chizuk_translation").forEach { id ->
                val bloc = balise(xml, id) ?: error("$name : vue $id introuvable")
                assertTrue(
                    "$name / $id : pas de corps automatique",
                    bloc.contains("""android:autoSizeTextType="uniform""""),
                )
                val hauteur = Regex("""android:layout_height="(\d+)dp"""").find(bloc)
                assertTrue(
                    "$name / $id : la hauteur doit être fixée en dp, sinon le texte déborde",
                    hauteur != null,
                )
            }
        }
    }

    @Test
    fun `l hebreu est en direction droite-a-gauche`() {
        forEachLayout { name, xml ->
            val bloc = balise(xml, "chizuk_he") ?: error("$name : chizuk_he introuvable")
            assertTrue(
                "$name : l'hébreu doit être forcé en RTL, quelle que soit la langue de l'UI",
                bloc.contains("""android:layoutDirection="rtl""""),
            )
        }
    }

    /**
     * ChizukCard.bind remplit ces vues sans les chercher deux fois : si l'une
     * disparaît d'une des deux mises en page, findViewById rend null et la
     * carte plante au moment de l'afficher, pas à la compilation.
     */
    @Test
    fun `les deux mises en page portent toutes les vues que la carte remplit`() {
        forEachLayout { name, xml ->
            listOf(
                "chizuk_art", "chizuk_moment", "chizuk_badge",
                "chizuk_he", "chizuk_translation", "chizuk_ref",
            ).forEach { id ->
                assertTrue("$name : vue $id absente", xml.contains("""@+id/$id""""))
            }
        }
    }

    @Test
    fun `l illustration a une taille fixe`() {
        // Une vignette en wrap_content prendrait la taille de l'image source
        // et ferait sauter la hauteur de la carte d'un moment à l'autre.
        forEachLayout { name, xml ->
            val ancre = xml.indexOf("@+id/chizuk_art")
            val cadre = xml.substring(xml.lastIndexOf("<FrameLayout", ancre), ancre)
            assertTrue(
                "$name : largeur du cadre de l'illustration non fixée en dp",
                Regex("""android:layout_width="\d+dp"""").containsMatchIn(cadre),
            )
            assertTrue(
                "$name : hauteur du cadre de l'illustration non fixée en dp",
                Regex("""android:layout_height="\d+dp"""").containsMatchIn(cadre),
            )
        }
    }

    @Test
    fun `l hebreu est place au-dessus de la traduction`() {
        forEachLayout { name, xml ->
            assertTrue(
                "$name : la traduction passe avant l'hébreu",
                xml.indexOf("chizuk_he") < xml.indexOf("chizuk_translation"),
            )
        }
    }

    @Test
    fun `la reference est en petit et sur une ligne`() {
        forEachLayout { name, xml ->
            val bloc = balise(xml, "chizuk_ref") ?: error("$name : chizuk_ref introuvable")
            assertTrue("$name : référence sur plusieurs lignes", bloc.contains("""android:maxLines="1""""))
            val taille = Regex("""android:textSize="(\d+)sp"""").find(bloc)
            assertTrue("$name : taille de la référence non déclarée", taille != null)
            assertTrue(
                "$name : la référence doit rester discrète",
                taille!!.groupValues[1].toInt() <= 12,
            )
        }
    }

    /** Extrait la balise qui porte cet identifiant. */
    private fun balise(xml: String, id: String): String? {
        val ancre = xml.indexOf("""android:id="@+id/$id"""")
        if (ancre < 0) return null
        val debut = xml.lastIndexOf('<', ancre)
        val fin = xml.indexOf("/>", ancre)
        if (debut < 0 || fin < 0) return null
        return xml.substring(debut, fin)
    }

    private fun forEachLayout(check: (String, String) -> Unit) {
        layouts.forEach { name ->
            val file = File("src/main/res/layout/$name")
            assertTrue("mise en page absente : ${file.absolutePath}", file.exists())
            check(name, file.readText())
        }
    }
}
