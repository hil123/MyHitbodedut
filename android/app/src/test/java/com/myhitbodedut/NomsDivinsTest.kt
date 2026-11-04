package com.myhitbodedut

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Le Nom en toutes lettres dans les prières et les versets (demande du
 * propriétaire, 04/10/2026) : siddur des trois nusa'him, Tehilim, Tikkun.
 * Dans les livres (citations du chizuk, bibliothèque), il est abrégé ה' hors
 * des versets cités (demande du 04/11/2026, tools/noms_abreges.py). Les
 * consignes (« c »), les dates et les notes peuvent garder ה׳ (« ה׳ בכסלו »).
 */
class NomsDivinsTest {

    private val ignores = setOf("_meta", "meta", "note", "notes", "c", "date", "source", "sources")

    private val abreges = listOf(
        Regex("""(?:^|[\s(\[>־])[ובלמשכה]{0,2}ה['׳](?!["'״])"""),
        Regex("""(?:^|[\s(\[>־])[ובלמשכה]{0,2}אלק(?:ים|י|ינו|יך|יכם|יהם|יו)(?=$|[\s,.:;!?)\]׃<־])"""),
        Regex("""אד[-־]ני"""),
    )

    /**
     * יְיָ ou יי nu : cherché sur le texte vocalisé, pour ne pas confondre
     * avec un mot comme בִּכְיִי (« mes pleurs »).
     */
    private val yudYud = Regex("""(?:^|[\s(\[>־])(?:[ובלמשכה][\u05B0-\u05BC\u05C1\u05C2]*){0,2}(?:יְיָ|יי)(?=$|[\s,.:;!?)\]׃<])""")

    /** Le texte sans voyelles ni signes de cantillation, maqaf et sof pasuq gardés. */
    private fun nu(t: String) = java.text.Normalizer.normalize(t, java.text.Normalizer.Form.NFC)
        .replace(Regex("""[\u0591-\u05BD\u05BF-\u05C2\u05C4\u05C5\u05C7]"""), "")

    private fun abreviations(o: Any?, chemin: String, sortie: MutableList<String>) {
        when (o) {
            is JSONObject -> o.keys().forEach { k -> if (k !in ignores) abreviations(o.get(k), "$chemin.$k", sortie) }
            is JSONArray -> for (i in 0 until o.length()) abreviations(o.get(i), "$chemin[$i]", sortie)
            is String -> {
                abreges.forEach { r -> r.find(nu(o))?.let { sortie += "$chemin : « ${it.value.trim()} »" } }
                yudYud.find(java.text.Normalizer.normalize(o, java.text.Normalizer.Form.NFC))?.let { sortie += "$chemin : « ${it.value.trim()} »" }
            }
        }
    }

    private fun verifie(nom: String, json: String) {
        val trouves = mutableListOf<String>()
        abreviations(JSONObject(json), nom, trouves)
        assertTrue("Nom abrégé :\n" + trouves.joinToString("\n"), trouves.isEmpty())
    }

    @Test
    fun `siddur des trois nusahim en toutes lettres`() =
        Nusah.entries.forEach { verifie("siddur_${it.cle}", DataFiles.siddurJson(it)) }

    /**
     * Les livres (04/11) : « לִפְנֵי ה' יִתְבָּרַךְ », jamais le Nom entier devant
     * « יתברך » (aucun verset ne le dit) ; les versets cités gardent le Nom entier.
     */
    @Test
    fun `livres abreges hors des versets`() {
        listOf("chizuk" to DataFiles.chizukJson, "bibliotheque" to DataFiles.bibliothequeJson).forEach { (nom, json) ->
            val t = java.text.Normalizer.normalize(json, java.text.Normalizer.Form.NFC)
            assertTrue("$nom : le Nom entier devant יתברך", !Regex("י\\p{M}*ה\\p{M}*ו\\p{M}*ה\\p{M}*\\s+יִתְבָּרַךְ|יהוה\\s+יתברך").containsMatchIn(t))
            assertTrue("$nom : ה' manquant", t.contains("ה' יִתְבָּרַךְ") || t.contains("ה' יתברך"))
            assertTrue("$nom : le verset de Tehilim 145:18 a perdu le Nom", t.contains("קָרוֹב יְהֹוָה לְכָל"))
        }
    }

    @Test
    fun `tehilim et tikkun en toutes lettres`() {
        verifie("tehilim", DataFiles.tehilimJson)
        verifie("tikkun", DataFiles.tikkunJson)
    }

    /** Un préfixe sans voyelle reprend celle du Tanakh : לַיהֹוָה, et non ליְהֹוָה. */
    @Test
    fun `prefixes vocalises comme dans le Tanakh`() {
        listOf(DataFiles.chizukJson, DataFiles.bibliothequeJson).forEach { json ->
            val t = java.text.Normalizer.normalize(json, java.text.Normalizer.Form.NFC)
            assertTrue(!Regex("(?:^|[\\s\"])[לבכו]יְהֹוָה").containsMatchIn(t))
        }
        // Abrégé, le préfixe garde sa voyelle : לַה'.
        assertTrue(DataFiles.bibliothequeJson.contains("לַה'") || DataFiles.chizukJson.contains("לַה'"))
    }

    /** Le seul ה׳ gardé dans la bibliothèque est une date, hors du texte. */
    @Test
    fun `la date du 5 kislev reste ecrite en chiffre hebreu`() {
        assertTrue(DataFiles.bibliothequeJson.contains("ה׳ בכסלו"))
    }
}
