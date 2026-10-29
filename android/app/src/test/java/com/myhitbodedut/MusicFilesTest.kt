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

    /**
     * Deux dossiers depuis le 29/10 : res/raw de main, que toute construction
     * livre, et celui de src/musique_hors_play, que seule la version GitHub
     * livre (PUBLICATION_PLAY_STORE.md § 3.2). Les tests valent pour leur
     * réunion, c'est-à-dire pour la version GitHub ; ceux du bas tiennent la
     * version Play.
     */
    private val rawPlay = File("src/main/res/raw")
    private val rawHorsPlay = File("src/musique_hors_play/res/raw")
    private val codePlay = File("src/main/java/com/myhitbodedut/MusiquePlay.kt").readText()
    private val codeHorsPlay = File("src/musique_hors_play/java/com/myhitbodedut/MusiqueHorsPlay.kt").readText()
    private val code = codePlay + codeHorsPlay

    /** Les morceaux d'un dossier : AAC (.m4a) depuis le 29/10, un .mp3 n'y est relevé que pour être refusé. */
    private fun audio(dossier: File): List<File> =
        dossier.listFiles { f -> f.name.endsWith(".m4a") || f.name.endsWith(".mp3") }.orEmpty().toList()
    private fun embarques(): List<File> = audio(rawPlay) + audio(rawHorsPlay)
    private val strings = File("src/main/res/values/strings.xml").readText()

    /** Les identifiants déclarés dans BUNDLED, dans l'ordre du fichier. */
    private fun declaresDans(code: String): List<String> =
        Regex("""Bundled\("([a-z0-9_]+)", R\.raw\.([a-z0-9_]+), R\.string\.([a-z0-9_]+)\)""")
            .findAll(code)
            .map { it.groupValues[1] + "|" + it.groupValues[2] + "|" + it.groupValues[3] }
            .toList()

    private val declares: List<String> = declaresDans(code)

    @Test
    fun `chaque morceau declare a son fichier et son titre`() {
        assertTrue("aucun morceau déclaré : la lecture est cassée", declares.isNotEmpty())
        declares.forEach { ligne ->
            val (id, res, titre) = ligne.split("|")
            assertEquals("l'identifiant et le fichier doivent porter le même nom", id, res)
            val f = embarques().firstOrNull { it.nameWithoutExtension == res } ?: File(rawPlay, "$res.m4a")
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
        val fichiers = embarques()
        assertTrue("aucun morceau trouvé", fichiers.isNotEmpty())
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
    fun `tous les morceaux livres sont declares, et reciproquement`() {
        val surDisque = embarques().map { it.nameWithoutExtension }.sorted()
        val cites = declares.map { it.split("|")[1] }.sorted()
        assertEquals(
            "un morceau embarqué que personne ne joue, ou un morceau déclaré sans fichier",
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
        val octets = embarques().sumOf { it.length() }
        val mo = octets / 1_048_576.0
        assertTrue(
            "audio embarqué : %.1f Mo. Seuil relevé trois fois ".format(mo) +
                "(40 → 80 → 110 → 130). Le correctif est de n'embarquer qu'une " +
                "sélection et de laisser le reste dans musique/, que " +
                "MusicActivity sait déjà ouvrir depuis le téléphone — pas de " +
                "relever une quatrième fois, et surtout pas de télécharger à " +
                "la demande : l'application ne déclare aucune permission " +
                "INTERNET, et c'est une propriété revendiquée.",
            mo < 130.0,
        )
    }

    /**
     * Les morceaux embarqués sont en AAC 96 kb/s depuis le 29/10 (audit 1.390, écoute validée
     * par le propriétaire) : 130 Mo de MP3 devenus 85. Un MP3 revenu dans raw/ sans passer par
     * tools/audio.py ramènerait le poids d'avant.
     */
    @Test
    fun `chaque morceau embarque est en AAC`() {
        assertTrue("un mp3 dans raw/ : le réencoder (tools/audio.py)",
            embarques().none { it.name.endsWith(".mp3") })
        embarques().forEach { f ->
            val tete = f.inputStream().use { it.readNBytes(8) }
            assertTrue("${f.name} n'est pas un MP4 (« ftyp » absent)",
                tete.size == 8 && String(tete, 4, 4, Charsets.US_ASCII) == "ftyp")
        }
    }

    /**
     * Le dossier `musique/` — les morceaux gardés hors de l'APK — pèse
     * maintenant plus lourd que ce qui est embarqué, et rien ne le regardait.
     * `tools/ajoute_musique.py` contrôle à l'écriture ; ce test contrôle ce
     * qui est dans le dépôt, ce qui n'est pas la même chose : un fichier
     * déposé à la main échappe au premier et pas au second.
     */
    @Test
    fun `le dossier musique ne double ni ne contredit ce qui est embarque`() {
        val hors = File("../../musique").listFiles { f -> f.name.endsWith(".mp3") || f.name.endsWith(".m4a") }.orEmpty()
        assertTrue("dossier musique/ vide ou introuvable", hors.isNotEmpty())

        val embarques = embarques()
            .associateBy { empreinte(it) }
        // Réencodés, les embarqués n'ont plus les octets de leur MP3 d'origine : le nom tranche aussi.
        val nomsEmbarques = embarques.values.map { it.nameWithoutExtension }.toSet()
        val vues = HashMap<String, String>()
        hors.forEach { f ->
            val tete = f.inputStream().use { it.readNBytes(8) }
            assertTrue(
                "${f.name} n'est ni un MP3 (ID3, trame MPEG) ni un MP4",
                tete.copyOf(3).contentEquals("ID3".toByteArray()) ||
                    (tete.size >= 2 && tete[0] == 0xFF.toByte() && (tete[1].toInt() and 0xE0) == 0xE0) ||
                    (tete.size == 8 && String(tete, 4, 4, Charsets.US_ASCII) == "ftyp"),
            )
            assertTrue("${f.name} est à la fois dans musique/ et embarqué", f.nameWithoutExtension !in nomsEmbarques)
            val h = empreinte(f)
            assertTrue(
                "${f.name} est déjà embarqué sous le nom ${embarques[h]?.name} : " +
                    "le même enregistrement livré deux fois",
                !embarques.containsKey(h),
            )
            assertEquals(
                "mêmes octets sous deux noms dans musique/ : ${f.name} = ${vues[h]}",
                null,
                vues.put(h, f.name),
            )
            assertTrue(
                "${f.name} est dans musique/ mais déclaré dans MusicLibrary : " +
                    "il serait joué sans être dans l'APK",
                !code.contains("R.raw.${f.nameWithoutExtension},"),
            )
        }
    }

    /** Ce que la version Play livre, et rien d'autre (29/10). */
    @Test
    fun `la version Play ne livre que les morceaux autorises`() {
        val autorises = listOf("hamelech_basade", "yanuka_nigoun_3")
        assertEquals(
            "res/raw de main est livré par la version Play : n'y mettre qu'un morceau dont la " +
                "diffusion publique est établie (PUBLICATION_PLAY_STORE.md § 3.2), sinon dans src/musique_hors_play",
            autorises,
            audio(rawPlay).map { it.nameWithoutExtension }.sorted(),
        )
        assertEquals(autorises, declaresDans(codePlay).map { it.split("|")[0] }.sorted())
        assertEquals(
            "les morceaux de src/musique_hors_play sont déclarés dans sa propre liste",
            audio(rawHorsPlay).map { it.nameWithoutExtension }.sorted(),
            declaresDans(codeHorsPlay).map { it.split("|")[0] }.sorted(),
        )
    }

    @Test
    fun `la construction Play remplace la liste hors Play par une liste vide`() {
        val vide = File("src/musique_play/java/com/myhitbodedut/MusiqueHorsPlay.kt").readText()
        assertTrue(vide.contains("object MusiqueHorsPlay"))
        assertTrue("aucun morceau ne doit y être déclaré", declaresDans(vide).isEmpty())
        assertTrue(File("src/musique_play/res").exists().not())
        val gradle = File("build.gradle.kts").readText()
        assertTrue(gradle.contains("if (play) \"src/musique_play\" else \"src/musique_hors_play\""))
        assertTrue(gradle.contains("if (!play) res.srcDir"))
    }

    private fun empreinte(f: File): String =
        MessageDigest.getInstance("SHA-256").digest(f.readBytes()).joinToString("") {
            "%02x".format(it)
        }
}
