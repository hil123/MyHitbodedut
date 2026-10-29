package com.myhitbodedut

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Le personnage au féminin pour qui a choisi צדקת (audit 1.390, point 8). */
class PersonnageTest {

    private val source = File("src/main/java/com/myhitbodedut/Personnage.kt").readText()
    private val table = Regex("""R\.drawable\.(char_\w+) to R\.drawable\.(char_f_\w+)""")
        .findAll(source).associate { it.groupValues[1] to it.groupValues[2] }
    private val dessins = File("src/main/res/drawable-nodpi").list()!!.map { it.substringBefore('.') }

    @Test
    fun `chaque pose du hassid a sa pose feminine`() {
        // char_face ne sert qu'à l'icône de lancement, qui reste la même pour tous.
        val hassid = dessins.filter { it.startsWith("char_") && !it.startsWith("char_f_") && it != "char_face" }
        assertEquals(hassid.sorted(), table.keys.sorted())
    }

    @Test
    fun `chaque pose feminine livree sert, et chaque pose citee existe`() {
        assertEquals(dessins.filter { it.startsWith("char_f_") }.sorted(), table.values.toSet().sorted())
    }

    @Test
    fun `la table ne touche qu au personnage et ne s applique qu une fois`() {
        assertEquals(R.drawable.ic_chevron, Personnage.feminin(R.drawable.ic_chevron))
        assertEquals(R.drawable.char_f_guitar, Personnage.feminin(R.drawable.char_guitar))
        assertEquals(R.drawable.char_f_joy, Personnage.feminin(R.drawable.char_f_joy))
    }

    @Test
    fun `toutes les mises en page passent par le personnage`() {
        val theme = File("src/main/res/values/themes.xml").readText()
        assertTrue(theme.contains("""<item name="viewInflaterClass">com.myhitbodedut.PersonnageViewInflater</item>"""))
        assertTrue(File("src/main/java/com/myhitbodedut/IllustrationSouple.kt").readText().contains(": PersonnageImageView("))
    }

    /** Les notifications, la boussole et les widgets dessinent un Bitmap : pas d'ImageView pour les rattraper. */
    @Test
    fun `chaque image decodee du personnage passe par la table`() {
        File("src/main/java/com/myhitbodedut").listFiles()!!.filter { it.extension == "kt" }.forEach { f ->
            Regex("""decodeResource\([^,]+,\s*([^)\n]+)""").findAll(f.readText()).forEach { m ->
                val arg = m.groupValues[1]
                if (arg.contains("char_") || arg.contains("illustration")) {
                    assertTrue("${f.name} : $arg", arg.startsWith("Personnage.pour("))
                }
            }
        }
    }

    @Test
    fun `le site suit la meme table`() {
        val web = File("../../web/src/js/personnage.js").readText()
        val jsTable = Regex("""(char_\w+): '(char_f_\w+)'""").findAll(web)
            .associate { it.groupValues[1] to it.groupValues[2] }
        assertEquals(table, jsTable)
    }
}
