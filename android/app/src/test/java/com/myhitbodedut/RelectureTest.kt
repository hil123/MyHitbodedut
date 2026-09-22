package com.myhitbodedut

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Ce que « relu » veut dire.
 *
 * Le drapeau existait dans les deux fichiers de textes, mais la regle
 * bloquante du point 3 ne s'applique qu'aux textes de 'hizouk : pour le
 * Tikoun, il ne commandait rien et personne ne le verifiait. Un champ qui
 * ne decide de rien et que rien ne controle ne vaut pas mieux qu'une case
 * vide — n'importe qui pouvait ecrire relu=true sans que cela engage a
 * quoi que ce soit.
 *
 * Il engage maintenant : un fichier qui se declare relu doit dire QUI l'a
 * relu et SI la relecture etait independante. Et la sanction d'un fichier
 * non relu est une compilation rouge, pas un ecran vide chez celui qui
 * prie : mieux vaut que le defaut arrive a celui qui peut le corriger.
 */
class RelectureTest {

    private val chizuk = JSONObject(DataFiles.chizukJson)
    private val tikkun = JSONObject(DataFiles.tikkunJson)

    private fun meta(fichier: JSONObject) = fichier.getJSONObject("_meta")

    /** Le bloc de relecture d'un fichier, ou null s'il n'en a pas. */
    private fun relecture(fichier: JSONObject): JSONObject? =
        meta(fichier).optJSONObject("verification")?.optJSONObject("relecture")

    /**
     * Le coeur : se declarer relu oblige a dire par qui. Sans cela le
     * drapeau redevient une case qu'on coche.
     */
    @Test
    fun `se declarer relu oblige a dire par qui`() {
        listOf("chizuk.json" to chizuk, "tikkun_haklali.json" to tikkun)
            .forEach { (nom, fichier) ->
                val bloc = relecture(fichier)
                assertTrue("$nom : aucun bloc _meta.verification.relecture", bloc != null)
                val par = bloc!!.optString("par")
                assertTrue("$nom : la relecture ne dit pas par qui", par.isNotBlank())
                assertTrue(
                    "$nom : la relecture ne dit pas si elle était indépendante",
                    bloc.has("independante"),
                )
                // Une relecture non independante doit porter sa reserve,
                // sinon « relu » se lirait comme une garantie qu'il n'est pas.
                if (!bloc.getBoolean("independante")) {
                    assertTrue(
                        "$nom : relecture non indépendante sans réserve écrite",
                        bloc.optString("reserve").length > 40,
                    )
                }
            }
    }

    /**
     * Le Tikoun se declare publiable comme les textes de 'hizouk. Si
     * quelqu'un remet relu a false, c'est ici que cela doit s'arreter — et
     * non sur le telephone de quelqu'un qui ouvrirait une section vide.
     */
    @Test
    fun `le Tikoun est verifie et relu`() {
        assertEquals("verifie_texte", meta(tikkun).getString("statut_verif"))
        assertTrue(
            "le Tikoun se déclare non relu : il ne devrait pas être livré",
            meta(tikkun).getBoolean("relu"),
        )
    }

    /** La regle du point 3 tient toujours sur les 37 textes de 'hizouk. */
    @Test
    fun `les trente-sept textes de hizouk restent publiables`() {
        val items = chizuk.getJSONArray("items")
        assertEquals(37, items.length())
        (0 until items.length()).forEach { i ->
            val item = items.getJSONObject(i)
            val id = item.optString("id", "#$i")
            assertEquals("« $id » : statut", "verifie_texte", item.getString("statut_verif"))
            assertTrue("« $id » : non relu", item.getBoolean("relu"))
        }
    }

    /**
     * La reserve du Tikoun doit rester ecrite : les parties liturgiques
     * n'ont qu'un seul temoin, la page fournie, alors que les psaumes en
     * ont deux. C'est ce que le fichier doit continuer de dire de lui-meme.
     */
    @Test
    fun `le Tikoun dit ce que sa verification ne couvre pas`() {
        val verification = meta(tikkun).getJSONObject("verification")
        val liturgie = verification.optString("liturgie")
        assertTrue("la réserve sur la liturgie a disparu", liturgie.length > 60)
        assertTrue(
            "la réserve doit dire que la liturgie n'a qu'un seul témoin",
            liturgie.contains("TEMOIN") || liturgie.contains("témoin"),
        )
        assertTrue(
            "la vérification doit citer l'édition confrontée aux psaumes",
            verification.optString("psaumes").contains("164/164"),
        )
    }
}
