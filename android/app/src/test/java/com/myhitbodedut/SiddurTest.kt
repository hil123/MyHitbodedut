package com.myhitbodedut

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Le siddour (demande du 04/10) : les seize textes de la liste CalJ, dans
 * les trois nusah, leur balisage, et le partage entre prière et consigne.
 */
class SiddurTest {

    private val nusah = Nusah.entries.associateWith { DataFiles.siddur(it) }

    private val attendus = listOf(
        "birkot_hachahar", "chaharit", "minha", "arvit", "chema_coucher", "tikoun_hatsot",
        "acher_yatsar", "birkat_hamazon", "meen_chaloch", "bore_nefachot", "tefilat_haderekh",
        "birkat_halevana", "cheva_berakhot", "parachat_haman", "kaddich", "hachkavot",
    )

    @Test
    fun `chaque nusah a les seize textes demandes`() {
        nusah.forEach { (n, siddur) ->
            assertEquals("$n", attendus, siddur.textes.map { it.id })
            assertTrue("$n sans nom", siddur.nusah.he.isNotBlank() && siddur.sources.isNotEmpty())
            siddur.textes.forEach { t ->
                assertTrue("$n/${t.id} vide", t.sections.isNotEmpty() && t.sections.all { it.blocs.isNotEmpty() })
                assertTrue("$n/${t.id} sans titre", t.titre.he.isNotBlank() && t.titre.fr.isNotBlank() && t.titre.en.isNotBlank())
                assertTrue("$n/${t.id} : groupe inconnu", siddur.groupes.any { it.cle == t.groupe })
                assertTrue("$n/${t.id} sans source", t.source.isNotBlank())
            }
        }
    }

    /** Ce qui manque aux sources libres d'un nusah est emprunté à un autre, et le dit. */
    @Test
    fun `les emprunts sont marques`() {
        fun empruntes(n: Nusah) = nusah.getValue(n).textes.filter { it.emprunt != null }.map { it.id }
        assertEquals(emptyList<String>(), empruntes(Nusah.EDOT))
        assertEquals(listOf("tikoun_hatsot"), empruntes(Nusah.SEFARD))
        assertEquals(listOf("tikoun_hatsot", "cheva_berakhot", "hachkavot"), empruntes(Nusah.ASHKENAZ))
        // Les Hachkavot, coutume séfarade, deviennent El malé ra'hamim.
        listOf(Nusah.SEFARD, Nusah.ASHKENAZ).forEach {
            assertEquals("El malé ra’hamim", nusah.getValue(it).texte("hachkavot")!!.titre.fr)
        }
    }

    @Test
    fun `le balisage se limite au gras aux petits caracteres et aux sauts`() {
        nusah.forEach { (n, siddur) ->
            siddur.textes.forEach { t ->
                t.sections.flatMap { it.blocs }.forEach { b ->
                    val balises = Regex("</?([a-z]+)").findAll(b.texte).map { it.groupValues[1] }.toSet()
                    assertTrue("$n/${t.id} : balise interdite $balises", (balises - setOf("b", "small", "br")).isEmpty())
                    listOf("b", "small").forEach { tag ->
                        assertEquals("$n/${t.id} : <$tag> mal fermée", Regex("<$tag>").findAll(b.texte).count(), Regex("</$tag>").findAll(b.texte).count())
                    }
                    assertTrue("$n/${t.id} : du latin", Regex("[A-Za-z]{4,}").find(b.texte.replace(Regex("<[^>]+>"), "")) == null)
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
        nusah.forEach { (n, siddur) ->
            siddur.textes.forEach { t ->
                t.sections.flatMap { it.blocs }.forEach { b ->
                    if (b.consigne) assertTrue("$n/${t.id} : consigne vocalisée", proportion(b.texte) < 0.2)
                    else assertTrue("$n/${t.id} : prière non vocalisée « ${b.texte.take(40)} »", proportion(b.texte) >= 0.2)
                }
            }
        }
    }

    @Test
    fun `parachat hamane est chemot 16 versets 4 a 36`() {
        fun lettres(s: String) = s.filter { it in 'א'..'ת' }
        nusah.values.forEach { siddur ->
            val p = siddur.texte("parachat_haman")!!.sections.single().blocs
            assertEquals(33, p.size)
            assertTrue(lettres(p.first().texte).startsWith("ויאמריהוהאלמשההנניממטיר"))
            assertTrue(lettres(p.last().texte).endsWith("עשריתהאיפההוא"))
        }
    }

    @Test
    fun `le kaddich est complet dans chaque nusah`() {
        nusah.forEach { (n, siddur) ->
            val k = siddur.texte("kaddich")!!.sections.flatMap { it.blocs }.joinToString(" ") { it.texte }
                .replace(Regex("<[^>]+>"), "").replace(Regex("[\\u0591-\\u05C7]"), "")
            listOf("יתגדל ויתקדש", "רבא מברך", "עושה שלום").forEach {
                assertTrue("$n : le Kaddich n'a pas « $it »", k.contains(it))
            }
        }
        val edot = nusah.getValue(Nusah.EDOT).texte("kaddich")!!.sections.single().blocs.joinToString(" ") { it.texte }
            .replace(Regex("<[^>]+>"), "").replace(Regex("[\\u0591-\\u05C7]"), "")
        assertTrue(edot.contains("על ישראל ועל רבנן") && edot.contains("יהא שלמא רבא"))
    }

    @Test
    fun `l onglet textes mene au siddour et le nusah se choisit`() {
        val accueil = File("src/main/java/com/myhitbodedut/MainActivity.kt").readText()
        assertTrue(accueil.contains("Intent(this, SiddurActivity::class.java)"))
        val manifeste = File("src/main/AndroidManifest.xml").readText()
        listOf(".SiddurActivity", ".SiddurLectureActivity").forEach { assertTrue(manifeste.contains("android:name=\"$it\"")) }
        assertTrue(File("src/main/java/com/myhitbodedut/SiddurActivity.kt").readText().contains("Store.setSiddurNusah(context, n)"))
        listOf("values", "values-en", "values-iw").forEach { d ->
            val xml = File("src/main/res/$d/strings.xml").readText()
            listOf("siddur_title", "siddur_hint", "siddur_books", "siddur_choose", "siddur_nusah_edot", "siddur_nusah_sefard",
                "siddur_nusah_ashkenaz", "siddur_sources", "siddur_source").forEach { assertTrue("$d : $it", xml.contains("name=\"$it\"")) }
        }
    }

    /** Le bouton discret de lecture (capture de CalJ, 04/10) : dans les deux lectures, réglages gardés. */
    @Test
    fun `les lectures ont le bouton discret`() {
        val siddur = File("src/main/java/com/myhitbodedut/SiddurLectureActivity.kt").readText()
        assertTrue(siddur.contains("ReglagesLecture(") && siddur.contains("avecNusah = true"))
        val tehilim = File("src/main/java/com/myhitbodedut/TehilimLectureActivity.kt").readText()
        assertTrue(tehilim.contains("ReglagesLecture(") && tehilim.contains("avecNusah = false"))
        val store = File("src/main/java/com/myhitbodedut/Store.kt").readText()
        assertTrue(store.contains("fun lectureEchelle") && store.contains("fun lectureSombre"))
        listOf("values", "values-en", "values-iw").forEach { d ->
            val xml = File("src/main/res/$d/strings.xml").readText()
            listOf("lecture_reglages", "lecture_nuit", "lecture_fermer", "lecture_plus", "lecture_moins").forEach {
                assertTrue("$d : $it", xml.contains("name=\"$it\""))
            }
        }
    }
}
