package com.myhitbodedut

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Les quatre langues : français, anglais, espagnol, hébreu.
 *
 * La parité était vérifiée écran par écran, jamais sur l'ensemble — et
 * l'espagnol a été ajouté longtemps après les trois autres. Ce qui se vérifie
 * ici n'est pas « le fichier existe » mais « aucun texte ne peut échapper à
 * la traduction » : ni une clé oubliée dans une langue, ni une chaîne écrite
 * en dur dans un layout, ni une langue servie par un `else`.
 */
class TraductionTest {

    private val LANGUES = listOf("values", "values-en", "values-es", "values-iw")

    private class Textes(val simples: Map<String, String>, val pluriels: Map<String, String>)

    private fun lire(dossier: String): Textes {
        val f = File("src/main/res/$dossier/strings.xml")
        assertTrue("$dossier/strings.xml manque", f.isFile)
        val racine = DocumentBuilderFactory.newInstance().newDocumentBuilder()
            .parse(f).documentElement
        val simples = LinkedHashMap<String, String>()
        val pluriels = LinkedHashMap<String, String>()
        val noeuds = racine.childNodes
        for (i in 0 until noeuds.length) {
            val e = noeuds.item(i) as? org.w3c.dom.Element ?: continue
            val nom = e.getAttribute("name")
            when (e.tagName) {
                "string" -> if (e.getAttribute("translatable") != "false") {
                    simples[nom] = e.textContent
                }
                "plurals" -> {
                    val items = e.getElementsByTagName("item")
                    for (j in 0 until items.length) {
                        val item = items.item(j) as org.w3c.dom.Element
                        pluriels["$nom[${item.getAttribute("quantity")}]"] = item.textContent
                    }
                }
            }
        }
        return Textes(simples, pluriels)
    }

    private val tout by lazy { LANGUES.associateWith { lire(it) } }

    private fun marqueurs(s: String): List<String> =
        Regex("""%\d+\$[sd]|%[sd]|%%""").findAll(s).map { it.value }.sorted().toList()

    @Test
    fun `les quatre langues portent le meme jeu de cles`() {
        val reference = tout.getValue("values").simples.keys
        assertTrue("aucune chaîne lue : la lecture est cassée", reference.size > 200)
        LANGUES.drop(1).forEach { langue ->
            val autres = tout.getValue(langue).simples.keys
            assertEquals(
                "$langue : clés manquantes ${(reference - autres).sorted()}",
                emptyList<String>(),
                (reference - autres).sorted(),
            )
            assertEquals(
                "$langue : clés en trop ${(autres - reference).sorted()}",
                emptyList<String>(),
                (autres - reference).sorted(),
            )
        }
    }

    @Test
    fun `chaque pluriel a ses formes dans chaque langue`() {
        // L'hébreu porte en plus la forme duelle « two », que le français et
        // l'anglais n'ont pas : on exige que « one » et « other » soient là,
        // pas que les listes soient identiques.
        val attendus = tout.getValue("values").pluriels.keys
        LANGUES.forEach { langue ->
            val siens = tout.getValue(langue).pluriels.keys
            assertTrue(
                "$langue : formes de pluriel manquantes ${(attendus - siens).sorted()}",
                siens.containsAll(attendus),
            )
        }
    }

    @Test
    fun `aucune traduction n est vide`() {
        LANGUES.forEach { langue ->
            val t = tout.getValue(langue)
            (t.simples + t.pluriels).forEach { (cle, valeur) ->
                assertTrue("$langue/$cle est vide", valeur.isNotBlank())
            }
        }
    }

    @Test
    fun `les marqueurs de format sont les memes partout`() {
        val fr = tout.getValue("values")
        LANGUES.drop(1).forEach { langue ->
            val autre = tout.getValue(langue)
            fr.simples.forEach { (cle, valeur) ->
                autre.simples[cle]?.let {
                    assertEquals(
                        "$langue/$cle : « $it » n'a pas les mêmes marqueurs que « $valeur »",
                        marqueurs(valeur), marqueurs(it),
                    )
                }
            }
            // Pour les pluriels on compare à « other » de la même langue :
            // « הוגדרה שעה אחת » se passe légitimement du chiffre au singulier.
            autre.pluriels.keys.groupBy { it.substringBefore('[') }.forEach { (nom, formes) ->
                val other = autre.pluriels["$nom[other]"] ?: return@forEach
                formes.forEach { forme ->
                    val v = autre.pluriels.getValue(forme)
                    assertTrue(
                        "$langue/$forme : « $v » invente un marqueur absent de « $other »",
                        marqueurs(v).all { it in marqueurs(other) },
                    )
                }
            }
        }
    }

    @Test
    fun `aucun texte affiche n est ecrit en dur dans un layout`() {
        val fautes = mutableListOf<String>()
        File("src/main/res/layout").listFiles { f -> f.extension == "xml" }!!.forEach { f ->
            Regex("""android:(text|hint|contentDescription)="([^"]*)"""")
                .findAll(f.readText())
                .filter { it.groupValues[2].isNotBlank() && !it.groupValues[2].startsWith("@") }
                .forEach { fautes += "${f.name} : ${it.value}" }
        }
        assertEquals("du texte écrit en dur ne sera jamais traduit", emptyList<String>(), fautes)
    }

    @Test
    fun `aucun texte affiche n est ecrit en dur dans le code`() {
        val fautes = mutableListOf<String>()
        File("src/main/java/com/myhitbodedut").listFiles { f -> f.extension == "kt" }!!.forEach { f ->
            f.readLines().forEachIndexed { i, ligne ->
                if (ligne.trimStart().startsWith("//") || ligne.trimStart().startsWith("*")) return@forEachIndexed
                val motifs = listOf(
                    Regex("""\.(text|hint|contentDescription)\s*=\s*"([^"]{2,})""""),
                    Regex("""(?:setTitle|setMessage)\("([^"]{2,})""""),
                    Regex("""Toast\.makeText\([^,]+,\s*"([^"]{2,})""""),
                )
                motifs.forEach { m -> m.find(ligne)?.let { fautes += "${f.name}:${i + 1} ${it.value}" } }
            }
        }
        assertEquals("du texte écrit en dur ne sera jamais traduit", emptyList<String>(), fautes)
    }

    @Test
    fun `aucune langue de l interface n est servie par un else`() {
        // Le defaut precedent servait l'hebreu a toute langue non nommee, et
        // le francais pour les textes de 'hizouk : un telephone espagnol
        // affichait un melange, sans que rien ne le signale.
        val json = File("src/main/java/com/myhitbodedut/Json.kt").readText()
        val carte = File("src/main/java/com/myhitbodedut/ChizukCard.kt").readText()
        listOf("fr", "en", "es").forEach { langue ->
            assertTrue(
                "Json.LocalizedText ne nomme pas « $langue »",
                json.contains("\"$langue\" ->"),
            )
            assertTrue(
                "ChizukCard.translationOf ne nomme pas « $langue »",
                carte.contains("\"$langue\" ->"),
            )
        }
    }

    @Test
    fun `les titres de la bibliotheque existent dans les quatre langues`() {
        val json = JSONObject(File("src/main/res/raw/bibliotheque.json").readText())
        val sections = json.getJSONArray("sections")
        var vus = 0
        for (i in 0 until sections.length()) {
            val section = sections.getJSONObject(i)
            val aVerifier = mutableListOf(section)
            section.optJSONArray("sous_sections")?.let { sous ->
                for (j in 0 until sous.length()) aVerifier += sous.getJSONObject(j)
            }
            aVerifier.forEach { noeud ->
                val titre = noeud.getJSONObject("titre")
                listOf("he", "fr", "en", "es").forEach { langue ->
                    assertTrue(
                        "${noeud.getString("id")} n'a pas de titre en « $langue »",
                        titre.optString(langue).isNotBlank(),
                    )
                }
                vus++
            }
        }
        assertTrue("aucun titre lu : la lecture est cassée", vus >= 4)
    }
}
