package com.myhitbodedut

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Les classes que seul un thème nomme (03/11). R8 ne lit pas les valeurs des thèmes : sans
 * règle -keep, il supprimait PersonnageViewInflater de la version publiée, et AppCompat
 * retombait sans bruit sur son inflater de base (boutons roses ou gris, plus de personnage
 * au féminin). La version de débogage, non minifiée, ne montrait rien.
 */
class GardeR8Test {

    @Test
    fun chaqueClasseNommeeParUnThemeEstGardee() {
        val regles = File("proguard-rules.pro").readText()
        val classes = File("src/main/res").walkTopDown()
            .filter { it.isFile && it.parentFile.name.startsWith("values") && it.extension == "xml" }
            .flatMap { Regex("""name="viewInflaterClass">\s*([\w.]+)\s*<""").findAll(it.readText()).map { m -> m.groupValues[1] } }
            .toSet()
        assertTrue("aucun viewInflaterClass trouvé", classes.isNotEmpty())
        for (c in classes) {
            assertTrue("$c est nommée par un thème mais n'a pas de règle -keep", Regex("""-keep class ${Regex.escape(c)}\b""").containsMatchIn(regles))
        }
    }
}
