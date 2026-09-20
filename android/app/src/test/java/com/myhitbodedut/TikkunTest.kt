package com.myhitbodedut

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Le Tikoun HaKlali : l'ordre des dix psaumes, et l'integrite du texte.
 *
 * L'ordre n'est pas un detail d'affichage — c'est ce qui fait le Tikoun.
 * Il est donc fige ici plutot que laisse a la bonne tenue du fichier.
 */
class TikkunTest {

    /**
     * Ordre etabli, recoupe le 20/09/2026 sur deux sources independantes
     * (Wikipedia « Tikkun HaKlali » et breslov.org).
     */
    private val ORDRE = listOf(16, 32, 41, 42, 59, 77, 90, 105, 137, 150)

    private val tikkun = DataFiles.tikkun

    @Test
    fun `les dix psaumes sont la, dans l ordre`() {
        assertEquals(ORDRE, tikkun.psaumes.map { it.numero })
    }

    @Test
    fun `chaque psaume porte ses versets, numerotes sans trou`() {
        tikkun.psaumes.forEach { p ->
            assertTrue("${p.id} : aucun verset", p.versets.isNotEmpty())
            assertEquals(
                "${p.id} : numerotation des versets",
                (1..p.versets.size).toList(),
                p.versets.map { it.numero },
            )
            p.versets.forEach {
                assertTrue("${p.id}:${it.numero} vide", it.he.isNotBlank())
            }
        }
    }

    @Test
    fun `le total de versets est celui du texte massoretique`() {
        // 11+11+14+12+18+21+17+45+9+6
        assertEquals(164, tikkun.versets)
    }

    @Test
    fun `les identifiants sont uniques`() {
        val ids = tikkun.psaumes.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun `l edition est du domaine public et vocalisee`() {
        assertEquals("Public Domain", tikkun.edition.licence)
        assertTrue("l'edition doit autoriser le nikkoud", tikkun.edition.nikkoudOk)
    }

    /**
     * Les noms divins sont substitues, comme partout ailleurs dans
     * l'application. Un tetragramme en toutes lettres sur un ecran qu'on
     * jette n'est pas une coquille ordinaire.
     */
    @Test
    fun `aucun nom divin n est ecrit en toutes lettres`() {
        val interdits = listOf("יהוה", "אלהים", "אלהי", "אדני")
        fun nu(t: String) = t.replace(Regex("[\\u0591-\\u05C7]"), "")

        tikkun.psaumes.forEach { p ->
            p.versets.forEach { v ->
                interdits.forEach {
                    assertFalse("${p.ref}:${v.numero} contient « $it »", nu(v.he).contains(it))
                }
            }
        }
        // Meme regle dans la liturgie, la kavana exceptee (voir le test dedie).
        tikkun.blocs.values
            .filterNot { it.cle == Tikkun.KAVANA }
            .forEach { bloc ->
                bloc.paragraphes.forEachIndexed { i, para ->
                    interdits.forEach {
                        assertFalse(
                            "${bloc.cle}[$i] contient « $it »",
                            nu(para).contains(it),
                        )
                    }
                }
            }
    }

    /**
     * Les quatre parties liturgiques reprises de la page fournie : ce qui se
     * dit avant, les versets de cloture, la kavana, la priere de Rabbi Nathan.
     */
    @Test
    fun `les parties liturgiques sont chargees`() {
        listOf(Tikkun.OUVERTURE, Tikkun.CLOTURE, Tikkun.KAVANA, Tikkun.PRIERE)
            .forEach { cle ->
                val bloc = tikkun.bloc(cle)
                assertTrue("bloc « $cle » absent", bloc != null)
                assertTrue("bloc « $cle » vide", bloc!!.paragraphes.isNotEmpty())
                assertTrue("bloc « $cle » sans titre", bloc.titre.he.isNotBlank())
                bloc.paragraphes.forEach {
                    assertTrue("bloc « $cle » : paragraphe vide", it.isNotBlank())
                }
            }
        assertEquals(1, tikkun.avant.size)
        assertEquals(3, tikkun.apres.size)
    }

    /**
     * La kavana est la seule exception a la substitution des noms divins, et
     * c'est deliberé : elle enseigne l'epellation des deux Noms et leur
     * milouy, dont la guematria 485 vaut תהלים. Y changer le ה en ק donnerait
     * un autre milouy et ferait tomber le compte. Le fichier doit porter cette
     * justification, sans quoi l'exception passerait pour un oubli.
     */
    @Test
    fun `l exception de la kavana est justifiee dans le fichier`() {
        val kavana = org.json.JSONObject(DataFiles.tikkunJson)
            .getJSONObject(Tikkun.KAVANA)
        assertTrue(
            "la kavana garde l'ecriture de sa source sans dire pourquoi",
            kavana.has("note_substitution"),
        )
    }

    /**
     * L'indication d'arret de Tehilim 32:7 : ce qu'elle dit, et d'ou elle
     * vient. Le pictogramme et le mot « הפסק » de la page fournie ne doivent
     * plus y figurer — elle se lit comme une parenthese, pas comme une alerte.
     */
    @Test
    fun `l indication d arret est une parenthese, avec sa source`() {
        val verset = tikkun.byNumero(32)!!.versets.first { it.numero == 7 }
        val arret = verset.arret
        assertTrue("aucune indication d'arret en 32:7", arret != null)
        assertTrue(
            "l'indication ne dit pas d'ou elle vient",
            arret!!.note.contains("ליקוטי מוהר") && arret.note.contains("רי\"ג"),
        )
        assertTrue(
            "la source doit etre entre parentheses",
            arret.note.contains("(") && arret.note.contains(")"),
        )
        assertFalse("le pictogramme d'arret subsiste", arret.note.contains("\u23F8"))
        assertTrue(
            "le mot d'arret doit se trouver dans le verset",
            verset.he.contains(arret.avant),
        )
    }

    /**
     * Le texte des psaumes ne vient pas de la page fournie mais d'une edition
     * massoretique. La page porte une coquille confirmee sur trois editions —
     * 105:20, dagesh manquant — et c'est la forme correcte qui doit etre ici.
     */
    @Test
    fun `le psaume 105 verset 20 porte le dagesh`() {
        val verset = tikkun.byNumero(105)!!.versets.first { it.numero == 20 }
        assertTrue(
            "105:20 doit lire וַיַּתִּירֵהוּ et non וַיַתִּירֵהוּ",
            verset.he.contains("וַיַּתִּירֵהוּ"),
        )
    }

    /**
     * Graphie massoretique. La page fournie s'en ecartait sur cinq points ;
     * les trois editions consultees s'accordent contre elle sur tous, et ce
     * sont elles qui tranchent. Repasser a la graphie de siddour ne casserait
     * rien d'autre, d'ou ce garde-fou.
     */
    @Test
    fun `les psaumes suivent la graphie massoretique`() {
        val maqqef = '\u05BE'
        val sofPasuk = '\u05C3'
        val tout = tikkun.psaumes.joinToString(" ") { p ->
            p.versets.joinToString(" ") { it.he }
        }
        assertTrue("aucun maqqef : la graphie n'est plus massoretique",
            tout.contains(maqqef))
        assertTrue("aucun sof-pasuk : la graphie n'est plus massoretique",
            tout.contains(sofPasuk))
        assertFalse(
            "un verset finit par « : » : c'est la ponctuation de siddour",
            tikkun.psaumes.any { p -> p.versets.any { it.he.trimEnd().endsWith(":") } },
        )
    }

    /**
     * Les deux graphies lexicales ou les editions contredisent la page.
     *
     * Le pointage de ירושלם varie d'un verset a l'autre dans l'edition
     * massoretique — qamats en 137:5, patah en 137:6 — donc la forme ne se
     * verifie pas par une chaine litterale mais par ce qui distingue les deux
     * graphies : le plene insere un yod avant le mem, le defectif non.
     */
    @Test
    fun `yerushalayim est defectif et halleluyah en deux mots`() {
        val plene = Regex("י\\u05B0רו\\u05BCש\\u05B8\\u05C1ל\\u05B7י\\u05B4ם")
        val defectif = Regex("י\\u05B0רו\\u05BCש\\u05B8\\u05C1ל\\u05B4[\\u05B7\\u05B8]ם")
        val versets = tikkun.byNumero(137)!!.versets.filter { defectif.containsMatchIn(it.he) }
        assertEquals("ירושלם doit apparaitre dans trois versets du psaume 137", 3, versets.size)
        tikkun.byNumero(137)!!.versets.forEach {
            assertFalse(
                "137:${it.numero} porte la graphie plene ירושלים",
                plene.containsMatchIn(it.he),
            )
        }
        listOf(tikkun.byNumero(105)!!.versets.first { it.numero == 45 },
               tikkun.byNumero(150)!!.versets.first { it.numero == 6 })
            .forEach {
                assertTrue("הללו־יה doit rester en deux mots",
                    it.he.contains("הַלְלוּ־יָהּ"))
            }
    }

    /**
     * La marque d'arret vit dans son propre champ. Dans la page fournie elle
     * est ecrite au milieu du verset ; l'y laisser aurait mis « ⏸ הפסק » dans
     * le texte hebreu lui-meme.
     */
    @Test
    fun `aucune marque de lecture n a fui dans le texte`() {
        tikkun.psaumes.forEach { p ->
            p.versets.forEach { v ->
                assertFalse("${p.ref}:${v.numero} contient le pictogramme d'arret",
                    v.he.contains("\u23F8"))
                assertFalse("${p.ref}:${v.numero} contient le mot « הפסק »",
                    v.he.contains("הפסק"))
            }
        }
    }

    /** Le qere est retenu : aucune notation « ketiv [qere] » ne doit subsister. */
    @Test
    fun `aucune notation ketiv-qere ne subsiste`() {
        tikkun.psaumes.forEach { p ->
            p.versets.forEach { v ->
                assertFalse("${p.ref}:${v.numero} porte un crochet", v.he.contains("["))
            }
        }
    }

    @Test
    fun `chaque psaume renvoie a sa source`() {
        tikkun.psaumes.forEach { p ->
            assertEquals("Psalms ${p.numero}", p.refSefaria)
            assertTrue("${p.id} : pas de lien", p.url.orEmpty().startsWith("https://"))
        }
    }
}
