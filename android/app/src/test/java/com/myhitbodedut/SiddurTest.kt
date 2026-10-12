package com.myhitbodedut

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Le siddour (demande du 04/10) : les seize textes de la liste CalJ, et le
 * Moussaf de Roch 'Hodech (11/10), dans les trois nusah, leur balisage, et
 * le partage entre prière et consigne.
 */
class SiddurTest {

    private val nusah = Nusah.entries.associateWith { DataFiles.siddur(it) }

    private val attendus = listOf(
        "birkot_hachahar", "chaharit", "minha", "arvit", "chema_coucher", "tikoun_hatsot",
        "acher_yatsar", "birkat_hamazon", "meen_chaloch", "bore_nefachot", "tefilat_haderekh",
        "birkat_halevana", "hallel", "moussaf_rosh_hodech", "cheva_berakhot", "parachat_haman", "kaddich", "hachkavot",
    )

    @Test
    fun `seules les mentions imposees par les licences restent, sans Sefaria`() {
        assertTrue(nusah.getValue(Nusah.EDOT).mentions.isEmpty())
        nusah.forEach { (n, siddur) ->
            assertTrue("$n", siddur.mentions.all { "CC-BY" in it && "Sefaria" !in it && "Public Domain" !in it })
            assertEquals("$n", siddur.sources.count { "CC-BY" in it }, siddur.mentions.size)
        }
    }

    @Test
    fun `chaque nusah a les textes demandes`() {
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

    /**
     * Le Hallel (12/10) dans les trois nusah, avec le saut de Roch 'Hodech
     * (Tehilim 115 et 116, 1-11) ; le Moussaf de Roch 'Hodech, celui de
     * semaine seulement : rien sur Chabbat Roch 'Hodech.
     */
    @Test
    fun `le hallel et un moussaf de roch hodech sans chabbat`() {
        // Le maqaf (« מָה־אָשִׁיב ») sépare les mots comme une espace.
        val nu = { s: String -> s.replace("\u05BE", " ").replace(Regex("<[^>]+>"), "").replace(Regex("[\\u0591-\\u05C7]"), "").replace(Regex("\\s+"), " ") }
        nusah.forEach { (n, siddur) ->
            val hallel = siddur.texte("hallel")!!.sections.flatMap { it.blocs }
            val tout = hallel.joinToString(" ") { nu(it.texte) }
            listOf("בצאת ישראל ממצרים", "מה אשיב", "אנא יהוה הושיעה נא", "יהללוך").forEach { assertTrue("$n : $it", it in tout) }
            assertTrue("$n : saut de Roch 'Hodech", hallel.count { it.consigne && "מדלג" in it.texte } >= 2)
            // Ni renvois de versets ni numéros collés au texte.
            assertTrue("$n : renvois", hallel.none { "(תהילים" in it.texte })
            assertTrue("$n : numéros", hallel.none { !it.consigne && Regex("(^|[.:] )[א-ת]{1,2} ").containsMatchIn(it.texte) })
            val moussaf = siddur.texte("moussaf_rosh_hodech")!!.sections.flatMap { it.blocs }
            assertTrue("$n : Chabbat", moussaf.none { it.consigne && "שבת" in nu(it.texte) })
        }
    }

    /**
     * Le Moussaf de Roch 'Hodech (11/10) : la bénédiction du milieu, puis la
     * consigne du Bnei Yissaschar et les douze צירופי הוי״ה des mois.
     */
    @Test
    fun `le moussaf de roch hodech porte les tseroufim du bnei yissaschar`() {
        nusah.forEach { (n, siddur) ->
            val blocs = siddur.texte("moussaf_rosh_hodech")!!.sections.flatMap { it.blocs }
            val nu = { s: String -> s.replace(Regex("<[^>]+>"), "").replace(Regex("[\\u0591-\\u05C7]"), "") }
            val i = blocs.indexOfFirst { it.consigne && it.texte.contains("בני יששכר") }
            assertTrue("$n : consigne", i > 0)
            // Juste après la fin de la bénédiction du milieu.
            assertTrue("$n : placement", nu(blocs[i - 1].texte).contains("מקדש ישראל וראשי חדשים"))
            val c = blocs[i]
            // Avec leur nikkoud et la ligne אהי״ה (12/10, tools/siddur_construction.py).
            listOf("ניסן: <b>יִהַוְהָ</b> · <b>אִהַיְהָ</b>", "תשרי: <b>וַהֹיָהַ</b> · <b>יַהֹאָהַ</b>",
                "אדר: <b>הִהְיְוֲ</b>", "אלול: <b>הוּהִוָיִ</b>", "באדר ב׳ יכוון בכללות כל י״ב הצירופים")
                .forEach { assertTrue("$n : $it", c.texte.contains(java.text.Normalizer.normalize(it, java.text.Normalizer.Form.NFC))) }
            // Les צירופים se pensent, ne se prononcent pas : dit avant la liste.
            assertTrue("$n : avertissement", c.texte.contains("אין להוציא צירופים אלו בפה כלל"))
            assertEquals("$n", 15, Regex("<br>").findAll(c.texte).count())
        }
    }

    /**
     * Le nikkoud des kavanot sur le Nom des 'hatimot de la Amida (11/10) :
     * celui d'Edot, reporté sur Sfard (tools/noms_kavanot.py), recoupé avec
     * le Chaar HaKavanot (drouch 6) et le Pri Etz 'Haïm (Chaar HaAmida).
     */
    @Test
    fun `les hatimot de la amida sfard portent le nikkoud des kavanot`() {
        val sfard = nusah.getValue(Nusah.SEFARD)
        listOf("chaharit", "minha", "arvit").forEach { id ->
            val texte = sfard.texte(id)!!.sections.filter { it.titre.contains("עמידה") }
                .flatMap { it.blocs }.joinToString(" ") { it.texte }.replace(Regex("<[^>]+>"), "")
            // Avot et Da'at en patah, Refoua en holam, Michpat, Tsadikim et Chalom en chourouq.
            assertTrue("$id : patah", Regex("יַהַוַהַ").findAll(texte).count() >= 2)
            assertTrue("$id : holam", texte.contains("יֹהֹוֹהֹ"))
            assertTrue("$id : chourouq", Regex("יוּהוּווּהוּ").findAll(texte).count() >= 3)
            val nu = texte.replace(Regex("[\\u0591-\\u05AF\\u05BD]"), "")
            assertTrue("$id : plus de יְהֹוָה avant « מגן אברהם »", !Regex("יְהֹוָה[,.]?\\s*מָגֵן").containsMatchIn(nu))
        }
    }

    /** Ce qui manque aux sources libres d'un nusah est emprunté à un autre, et le dit. */
    @Test
    fun `les emprunts sont marques`() {
        fun empruntes(n: Nusah) = nusah.getValue(n).textes.filter { it.emprunt != null }.map { it.id }
        assertEquals(emptyList<String>(), empruntes(Nusah.EDOT))
        assertEquals(listOf("tikoun_hatsot"), empruntes(Nusah.SEFARD))
        assertEquals(listOf("tikoun_hatsot"), empruntes(Nusah.ASHKENAZ))
        // Les Hachkavot, coutume séfarade, deviennent l'El malé ra'hamim du
        // souvenir, pour un homme et pour une femme ; jamais l'élégie du
        // 20 Sivan (Rabbi Ye'hiel Mikhal de Nemirov), reprise par erreur.
        listOf(Nusah.SEFARD, Nusah.ASHKENAZ).forEach { n ->
            val t = nusah.getValue(n).texte("hachkavot")!!
            assertEquals("El malé ra’hamim", t.titre.fr)
            val blocs = t.sections.flatMap { it.blocs }
            val texte = blocs.joinToString(" ") { it.texte }
                .replace(Regex("<[^>]+>"), "").replace(Regex("[\\u0591-\\u05C7]"), "")
            assertTrue("$n", texte.contains("אל מלא רחמים שוכן במרומים") || texte.contains("אל מלא רחמים, שוכן במרומים"))
            assertTrue("$n", !texte.contains("יחיאל") && !texte.contains("סיון"))
            assertEquals("$n", 2, blocs.count { !it.consigne })
        }
    }

    /** La bande de signets (10/10) : la section qu'on lit, en haut de page comme tout en bas. */
    @Test
    fun `le signet suit la section qu on lit`() {
        val debuts = listOf(0, 11, 14, 40)
        assertEquals(0, SignetsSiddur.courant(debuts, 0, 6, enBas = false))
        assertEquals(0, SignetsSiddur.courant(debuts, 10, 13, enBas = false))
        assertEquals(1, SignetsSiddur.courant(debuts, 11, 16, enBas = false))
        assertEquals(2, SignetsSiddur.courant(debuts, 39, 45, enBas = false))
        assertEquals(3, SignetsSiddur.courant(debuts, 40, 45, enBas = false))
        // Tout en bas, la dernière section, trop courte pour monter en haut, se marque quand même.
        assertEquals(3, SignetsSiddur.courant(debuts, 35, 45, enBas = true))
        // Avant le premier titre, le premier signet plutôt que rien.
        assertEquals(0, SignetsSiddur.courant(listOf(3, 9), 1, 4, enBas = false))
    }

    /** Les trois offices ont plusieurs sections titrées dans chaque nusah : leur bande existe. */
    @Test
    fun `les offices ont leur bande de signets`() {
        nusah.forEach { (n, siddur) ->
            listOf("chaharit", "minha", "arvit").forEach { id ->
                assertTrue("$n/$id", siddur.texte(id)!!.sections.count { it.titre.isNotBlank() } > 1)
            }
        }
        val source = File("src/main/java/com/myhitbodedut/SiddurLectureActivity.kt").readText()
        assertTrue(source.contains("SignetsSiddur.courant") && source.contains("R.string.siddur_signets"))
    }

    /** Sfard (Ari z"l) n'est pas Sefarad : le libellé dit lequel, dans les trois langues. */
    @Test
    fun `sfard et sefarad ne se confondent pas`() {
        val fr = File("src/main/res/values/strings.xml").readText()
        assertTrue(fr.contains("<string name=\"siddur_nusah_edot\">Séfarade (Edot HaMizra’h)</string>"))
        assertTrue(fr.contains("<string name=\"siddur_nusah_sefard\">Sfard (Arizal)</string>"))
        val he = File("src/main/res/values-iw/strings.xml").readText()
        assertTrue(he.contains("<string name=\"siddur_nusah_sefard\">ספרד (האר״י)</string>"))
        assertTrue(nusah.getValue(Nusah.SEFARD).nusah.fr.contains("Sfard (Arizal)"))
        assertTrue(nusah.getValue(Nusah.EDOT).nusah.fr.contains("séfarade"))
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
                    // Seule exception : la consigne des צירופים, dont les Noms et les versets portent leur nikkoud (12/10).
                    if (b.consigne && "צירוף שם הוי״ה ואהי״ה" in b.texte) return@forEach
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
        assertTrue(File("src/main/java/com/myhitbodedut/SiddurActivity.kt").readText().contains("Store.setSiddurNusah(this, Nusah.entries[i])"))
        listOf("values", "values-en", "values-iw").forEach { d ->
            val xml = File("src/main/res/$d/strings.xml").readText()
            listOf("siddur_title", "siddur_hint", "siddur_books", "siddur_choose", "siddur_nusah_edot", "siddur_nusah_sefard",
                "siddur_nusah_ashkenaz", "siddur_sources").forEach { assertTrue("$d : $it", xml.contains("name=\"$it\"")) }
        }
    }
}
