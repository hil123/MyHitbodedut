package com.myhitbodedut

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Le siddour (demande du 04/10) : les seize textes de la liste CalJ, leur
 * balisage, et le partage entre prière et consigne.
 */
class SiddurTest {

    private val siddur = DataFiles.siddur

    @Test
    fun `les seize textes demandes sont la`() {
        val attendus = listOf(
            "birkot_hachahar", "chaharit", "minha", "arvit", "chema_coucher", "tikoun_hatsot",
            "acher_yatsar", "birkat_hamazon", "meen_chaloch", "bore_nefachot", "tefilat_haderekh",
            "birkat_halevana", "cheva_berakhot", "parachat_haman", "kaddich", "hachkavot",
        )
        assertEquals(attendus, siddur.textes.map { it.id })
        siddur.textes.forEach { t ->
            assertTrue("${t.id} vide", t.sections.isNotEmpty() && t.sections.all { it.blocs.isNotEmpty() })
            assertTrue("${t.id} sans titre hébreu", t.titre.he.isNotBlank() && t.titre.fr.isNotBlank() && t.titre.en.isNotBlank())
            assertTrue("${t.id} : groupe inconnu", siddur.groupes.any { it.cle == t.groupe })
            assertTrue("${t.id} sans source", t.source.isNotBlank())
        }
    }

    @Test
    fun `le balisage se limite au gras aux petits caracteres et aux sauts`() {
        siddur.textes.forEach { t ->
            t.sections.flatMap { it.blocs }.forEach { b ->
                val balises = Regex("</?([a-z]+)").findAll(b.texte).map { it.groupValues[1] }.toSet()
                assertTrue("${t.id} : balise interdite $balises", (balises - setOf("b", "small", "br")).isEmpty())
                listOf("b", "small").forEach { tag ->
                    assertEquals("${t.id} : <$tag> mal fermée", Regex("<$tag>").findAll(b.texte).count(), Regex("</$tag>").findAll(b.texte).count())
                }
            }
        }
    }

    /** La prière est vocalisée, la consigne non : c'est la règle de tools/siddur_construction.py. */
    @Test
    fun `les consignes ne sont pas vocalisees et les prieres le sont`() {
        fun proportion(s: String): Double {
            val nu = s.replace(Regex("<[^>]+>"), "")
            val lettres = nu.count { it in 'א'..'ת' }
            val voyelles = nu.count { it in 'ְ'..'ּ' || it == 'ׁ' || it == 'ׂ' || it == 'ׇ' }
            return if (lettres == 0) 1.0 else voyelles.toDouble() / lettres
        }
        siddur.textes.forEach { t ->
            t.sections.flatMap { it.blocs }.forEach { b ->
                if (b.consigne) assertTrue("${t.id} : consigne vocalisée", proportion(b.texte) < 0.2)
                else assertTrue("${t.id} : prière non vocalisée « ${b.texte.take(40)} »", proportion(b.texte) >= 0.2)
            }
        }
    }

    @Test
    fun `parachat hamane est chemot 16 versets 4 a 36`() {
        val p = siddur.texte("parachat_haman")!!.sections.single().blocs
        assertEquals(33, p.size)
        fun lettres(s: String) = s.filter { it in 'א'..'ת' }
        assertTrue(lettres(p.first().texte).startsWith("ויאמריהוהאלמשההנניממטיר"))
        assertTrue(lettres(p.last().texte).endsWith("עשריתהאיפההוא"))
    }

    @Test
    fun `le kaddich est complet`() {
        val k = siddur.texte("kaddich")!!.sections.single().blocs.joinToString(" ") { it.texte }
            .replace(Regex("<[^>]+>"), "").replace(Regex("[\\u0591-\\u05C7]"), "")
        listOf("יתגדל ויתקדש", "על ישראל ועל רבנן", "יהא שלמא רבא", "עושה שלום").forEach {
            assertTrue("le Kaddich n'a pas « $it »", k.contains(it))
        }
    }

    @Test
    fun `l onglet textes mene au siddour`() {
        val accueil = File("src/main/java/com/myhitbodedut/MainActivity.kt").readText()
        assertTrue(accueil.contains("Intent(this, SiddurActivity::class.java)"))
        val manifeste = File("src/main/AndroidManifest.xml").readText()
        listOf(".SiddurActivity", ".SiddurLectureActivity").forEach { assertTrue(manifeste.contains("android:name=\"$it\"")) }
        listOf("values", "values-en", "values-iw").forEach { d ->
            val xml = File("src/main/res/$d/strings.xml").readText()
            listOf("siddur_title", "siddur_hint", "siddur_books", "siddur_nusah").forEach { assertTrue("$d : $it", xml.contains("name=\"$it\"")) }
        }
    }
}
