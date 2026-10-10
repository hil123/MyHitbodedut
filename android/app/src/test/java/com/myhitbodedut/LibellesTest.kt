package com.myhitbodedut

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** La table des libellés (Libelles.kt) couvre toutes les clés ajout_, fete_, zman_ de strings.xml. */
class LibellesTest {

    @Test
    fun `aucune cle ne manque a la table`() {
        val xml = File("src/main/res/values/strings.xml").readText()
        val cles = Regex("""name="((?:ajout|fete|zman)_[a-z0-9_]+)"""").findAll(xml).map { it.groupValues[1] }.toSet()
        val table = File("src/main/java/com/myhitbodedut/Libelles.kt").readText()
        val manquantes = cles.filter { !table.contains("\"$it\" -> R.string.$it") }
        assertEquals("régénérer Libelles.kt", emptyList<String>(), manquantes)
        assertTrue(cles.size >= 75)
    }
}
