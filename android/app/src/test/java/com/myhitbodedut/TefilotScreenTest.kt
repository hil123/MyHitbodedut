package com.myhitbodedut

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * L'écran Tfilot.
 *
 * La logique est dans TefilotRules, qui ne connaît pas Android : c'est elle
 * qu'on éprouve ici. Le reste — la ligne d'accueil, la déclaration au
 * manifeste, les chaînes des trois langues — se lit dans les fichiers, parce
 * qu'un écran déclaré nulle part planterait à l'ouverture sans qu'aucun test
 * de logique s'en aperçoive.
 */
class TefilotScreenTest {

    private val library = DataFiles.library

    private fun lire(chemin: String): String {
        val f = File(chemin)
        require(f.exists()) { "fichier absent : ${f.absolutePath}" }
        return f.readText()
    }

    @Test
    fun `les dix sujets de l ecran sont ceux du fichier`() {
        val dansLeCode = SujetTefila.values().map { it.cle }
        val dansLeFichier = TefilotRules.prieres(library).mapNotNull { it.sujet }.distinct()
        assertEquals(
            "l'écran et la bibliothèque ne parlent pas des mêmes sujets",
            dansLeCode.sorted(),
            dansLeFichier.sorted(),
        )
    }

    @Test
    fun `chaque sujet porte au moins deux prieres`() {
        SujetTefila.values().forEach { sujet ->
            val n = TefilotRules.parSujet(library, sujet).size
            assertTrue("« ${sujet.cle} » : $n prière(s), il en faut au moins deux", n >= 2)
        }
    }

    /**
     * L'ordre est voulu : l'hitbodedout ouvre, la guérison ferme. Un tri
     * alphabétique passerait inaperçu sans ce test.
     */
    @Test
    fun `l ordre des sujets est celui qui a ete demande`() {
        val attendu = listOf(
            "hitbodedut", "simcha", "uman", "kdusha", "zera",
            "parnassa", "brit", "geula", "zivug", "refua",
        )
        assertEquals("l'énumération a changé d'ordre", attendu, SujetTefila.values().map { it.cle })

        // Et surtout : l'ordre réellement produit. Vérifier la seule
        // énumération laissait passer un tri glissé dans lignes().
        val affiche = TefilotRules.lignes(library)
            .filterIsInstance<TefilotRules.Ligne.Titre>()
            .map { it.sujet.cle }
        assertEquals("les titres ne sortent pas dans l'ordre demandé", attendu, affiche)
    }

    @Test
    fun `les lignes alternent un titre puis ses prieres`() {
        val lignes = TefilotRules.lignes(library)
        val prieres = TefilotRules.prieres(library)
        assertEquals(
            "un titre par sujet, plus chaque prière",
            SujetTefila.values().size + prieres.size,
            lignes.size,
        )

        var sujetCourant: SujetTefila? = null
        var annonce = 0
        var vues = 0
        lignes.forEach { ligne ->
            when (ligne) {
                is TefilotRules.Ligne.Titre -> {
                    if (sujetCourant != null) {
                        assertEquals("« ${sujetCourant?.cle} » : compte annoncé faux", annonce, vues)
                    }
                    sujetCourant = ligne.sujet
                    annonce = ligne.nombre
                    vues = 0
                    assertEquals(
                        "titre « ${ligne.sujet.cle} » : nombre annoncé faux",
                        TefilotRules.parSujet(library, ligne.sujet).size,
                        ligne.nombre,
                    )
                }
                is TefilotRules.Ligne.Priere -> {
                    assertNotNull("une prière avant tout titre", sujetCourant)
                    assertEquals(
                        "prière rangée sous le mauvais titre",
                        sujetCourant,
                        ligne.sujet,
                    )
                    assertEquals(
                        "${ligne.entree.id} : sujet de l'entrée différent du titre",
                        sujetCourant?.cle,
                        ligne.entree.sujet,
                    )
                    vues++
                }
            }
        }
        assertEquals("dernier sujet : compte annoncé faux", annonce, vues)
    }

    /** Un sujet sans prière ne doit pas produire un titre vide. */
    @Test
    fun `un sujet sans priere ne produit aucune ligne`() {
        val vide = LibraryLoader.parse(
            """{"_meta":{"version":"x"},"sections":[{"id":"tefilot","titre":{},
               "sous_sections":[{"id":"tefilot-lt","titre":{},"entrees":[]}]}]}"""
        )
        assertTrue(TefilotRules.lignes(vide).isEmpty())
        assertTrue(TefilotRules.prieres(vide).isEmpty())
    }

    /**
     * L'écran avertit quand la leçon citée parle d'autre chose. Si plus
     * personne ne déclenche l'avertissement, il a cessé de dire quelque chose.
     */
    @Test
    fun `l avertissement se declenche, et pas pour tout le monde`() {
        val prieres = TefilotRules.prieres(library)
        val avertis = prieres.filter { TefilotRules.prevenir(it) }
        assertTrue("aucune prière n'avertit : l'avertissement ne sert plus", avertis.isNotEmpty())
        assertTrue("toutes avertissent : l'avertissement ne distingue plus", avertis.size < prieres.size)
        avertis.forEach {
            assertEquals("${it.id} : averti sans raison", "non", it.leconSurLeSujet)
        }
    }

    @Test
    fun `l ecran est joignable depuis l accueil et declare au manifeste`() {
        val layout = lire("src/main/res/layout/activity_main.xml")
        assertTrue("pas de ligne Tfilot sur l'accueil", layout.contains("\"@+id/tefilot_button\""))
        assertTrue("la ligne ne porte pas le titre", layout.contains("@string/tefilot_title"))

        val code = lire("src/main/java/com/myhitbodedut/MainActivity.kt")
        assertTrue(
            "la ligne Tfilot n'ouvre rien",
            Regex("""R\.id\.tefilot_button\b""").containsMatchIn(code) &&
                code.contains("TefilotActivity::class.java"),
        )

        val manifeste = lire("src/main/AndroidManifest.xml")
        assertTrue(
            "TefilotActivity n'est pas déclarée : l'écran planterait à l'ouverture",
            manifeste.contains("android:name=\".TefilotActivity\""),
        )
    }

    @Test
    fun `l ecran a ses chaines dans les trois langues`() {
        val cles = SujetTefila.values().map { it.cle }
            .map { "tefila_$it" } + listOf("tefilot_title", "tefilot_hint", "tefilot_count", "tefilot_warning")
        listOf("values", "values-en", "values-iw").forEach { dossier ->
            val xml = lire("src/main/res/$dossier/strings.xml")
            cles.forEach { cle ->
                assertTrue("$dossier : chaîne « $cle » manquante", xml.contains("name=\"$cle\""))
            }
        }
    }

    /** Le gabarit du titre de sujet doit porter les deux vues que l'écran remplit. */
    @Test
    fun `le gabarit du titre de sujet a ses deux vues`() {
        val xml = lire("src/main/res/layout/item_tefila_sujet.xml")
        assertTrue(xml.contains("\"@+id/tefila_sujet\""))
        assertTrue(xml.contains("\"@+id/tefila_sujet_count\""))
        assertFalse("le gabarit ne doit contenir aucun texte en dur", xml.contains("android:text=\""))
    }
}
