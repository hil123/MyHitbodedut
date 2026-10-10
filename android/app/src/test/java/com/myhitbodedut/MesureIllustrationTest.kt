package com.myhitbodedut

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

/**
 * Le défaut de la vidéo du 07/10 : dans un écran qui défile, une image à
 * adjustViewBounds sans hauteur maximale se mesure d'abord à sa pleine
 * hauteur naturelle. La page dépasse alors l'écran et fillViewport ne répartit
 * plus rien : sur un téléphone haut, le bouton de l'accueil passait sous le
 * bord. Toute image de ce genre dont la hauteur n'est pas fixée
 * (match_parent ou wrap_content) doit donc porter un maxHeight ; une
 * hauteur en dp, elle, est déjà une borne.
 */
class MesureIllustrationTest {

    @Test
    fun `une image ajustee dans un ecran qui defile a une hauteur maximale`() {
        val fautes = File("src/main/res/layout").listFiles { f -> f.extension == "xml" }!!
            .filter { it.readText().contains("ScrollView") }
            .flatMap { f ->
                Regex("""<ImageView\b[^>]*?/>""", RegexOption.DOT_MATCHES_ALL).findAll(f.readText())
                    .map { it.value }
                    .filter { it.contains("""android:adjustViewBounds="true"""") && !it.contains("android:maxHeight") }
                    .filter { Regex("""android:layout_height="(match_parent|wrap_content)"""").containsMatchIn(it) }
                    .map { "${f.name} : ${Regex("""android:id="([^"]+)"""").find(it)?.groupValues?.get(1)}" }
                    .toList()
            }
        assertEquals("images sans maxHeight", emptyList<String>(), fautes)
    }
}
