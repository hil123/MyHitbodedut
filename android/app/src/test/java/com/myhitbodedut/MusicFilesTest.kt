package com.myhitbodedut

import java.io.File
import java.security.MessageDigest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Les morceaux livrés avec l'application.
 *
 * MusicLibrary a besoin d'un Context, donc on ne l'instancie pas ici : on
 * vérifie ce qui se vérifie sans appareil — que chaque morceau déclaré a bien
 * son fichier, qu'aucun n'est livré deux fois sous deux noms, et que le poids
 * embarqué reste sous les yeux. Deux des fichiers envoyés étaient octet pour
 * octet identiques ; sans ce test le doublon serait parti dans l'APK.
 */
class MusicFilesTest {

    private val raw = File("src/main/res/raw")
    private val code = File("src/main/java/com/myhitbodedut/MusicLibrary.kt").readText()
    private val strings = File("src/main/res/values/strings.xml").readText()

    /** Les identifiants déclarés dans BUNDLED, dans l'ordre du fichier. */
    private val declares: List<String> =
        Regex("""Bundled\("([a-z0-9_]+)", R\.raw\.([a-z0-9_]+), R\.string\.([a-z0-9_]+)\)""")
            .findAll(code)
            .map { it.groupValues[1] + "|" + it.groupValues[2] + "|" + it.groupValues[3] }
            .toList()

    @Test
    fun `chaque morceau declare a son fichier et son titre`() {
        assertTrue("aucun morceau déclaré : la lecture est cassée", declares.isNotEmpty())
        declares.forEach { ligne ->
            val (id, res, titre) = ligne.split("|")
            assertEquals("l'identifiant et le fichier doivent porter le même nom", id, res)
            val f = File(raw, "$res.mp3")
            assertTrue("fichier absent : ${f.path}", f.isFile)
            assertTrue("${f.name} est vide", f.length() > 1_000)
            assertTrue(
                "titre « $titre » absent de values/strings.xml",
                strings.contains("name=\"$titre\""),
            )
        }
    }

    @Test
    fun `aucun morceau n est livre deux fois sous deux noms`() {
        val fichiers = raw.listFiles { f -> f.name.endsWith(".mp3") }.orEmpty()
        assertTrue("aucun mp3 trouvé", fichiers.isNotEmpty())
        val parEmpreinte = fichiers.groupBy { f ->
            MessageDigest.getInstance("SHA-256").digest(f.readBytes()).joinToString("") {
                "%02x".format(it)
            }
        }
        parEmpreinte.forEach { (empreinte, memes) ->
            assertEquals(
                "mêmes octets sous plusieurs noms (${empreinte.take(12)}) : " +
                    memes.joinToString { it.name },
                1,
                memes.size,
            )
        }
    }

    @Test
    fun `tous les mp3 livres sont declares, et reciproquement`() {
        val surDisque = raw.listFiles { f -> f.name.endsWith(".mp3") }
            .orEmpty().map { it.name.removeSuffix(".mp3") }.sorted()
        val cites = declares.map { it.split("|")[1] }.sorted()
        assertEquals(
            "un mp3 embarqué que personne ne joue, ou un morceau déclaré sans fichier",
            surDisque,
            cites,
        )
    }

    /**
     * Le poids de l'audio est le poids de l'APK : il ne se compresse plus, ni
     * par R8 ni par le zip. 30 morceaux pèsent dix fois le reste.
     *
     * **Correction — le remède que ce commentaire recommandait était faux.**
     * Il disait « télécharger les morceaux à la demande ». Or l'application
     * ne déclare **aucune permission INTERNET**, et c'est une propriété
     * revendiquée dans le README : pas de réseau, pas d'analytique. Le
     * téléchargement exigerait donc d'ajouter INTERNET, de trouver un
     * hébergement public — le dépôt est privé, il n'y en a pas — et de rendre
     * ces enregistrements publiquement téléchargeables, ce qui est un acte de
     * diffusion plus large que de livrer un APK.
     *
     * **Le vrai remède ne demande rien de neuf** : MusicActivity sait déjà
     * ouvrir un fichier du téléphone (OpenDocument → addUserTrack). Livrer une
     * petite sélection et laisser chacun ajouter les siens ramène l'APK à une
     * vingtaine de Mo, sans permission nouvelle, sans hébergement, sans
     * question de droits supplémentaire.
     *
     * Le seuil a été relevé trois fois (40 → 80 → 110 → 130), la dernière
     * contre mon avis. Il reste à 130 : c'est le point où la sélection doit
     * remplacer l'accumulation.
     */
    @Test
    fun `le poids embarque reste sous les yeux`() {
        val octets = raw.listFiles { f -> f.name.endsWith(".mp3") }.orEmpty().sumOf { it.length() }
        val mo = octets / 1_048_576.0
        assertTrue(
            "audio embarqué : %.1f Mo. Seuil relevé trois fois ".format(mo) +
                "(40 → 80 → 110 → 130). Le correctif est de télécharger les " +
                "morceaux à la demande, pas de relever une quatrième fois.",
            mo < 130.0,
        )
    }

    @Test
    fun `chaque mp3 est bien un mp3`() {
        raw.listFiles { f -> f.name.endsWith(".mp3") }.orEmpty().forEach { f ->
            val tete = f.inputStream().use { it.readNBytes(3) }
            val ok = tete.contentEquals("ID3".toByteArray()) ||
                (tete.size >= 2 && tete[0] == 0xFF.toByte() && (tete[1].toInt() and 0xE0) == 0xE0)
            assertTrue("${f.name} ne commence ni par ID3 ni par une trame MPEG", ok)
        }
    }
}
