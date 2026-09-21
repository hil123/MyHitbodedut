package com.myhitbodedut

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * L'icone de lancement.
 *
 * Le detourage du visage est dechire : une frange du fond blanc d'origine
 * est restee collee a la silhouette. Elle ne peut pas etre recoupee — sa
 * luminance mediane est 242, celle de la chemise 246, celle de la kippa
 * 237, et aucun seuil ne les separe. Le fond de l'icone doit donc rester
 * clair, seule facon de ne pas la voir.
 *
 * Ce test existe parce que rien, dans le fichier de l'icone, n'empeche
 * quelqu'un d'y remettre du vert fonce en trouvant que c'est plus joli.
 */
class LauncherIconTest {

    private val icone = File("src/main/res/mipmap-anydpi/ic_launcher.xml").readText()

    private fun couleur(nom: String, dossier: String = "values"): String? {
        val xml = File("src/main/res/$dossier/colors.xml").readText()
        return Regex("""<color name="$nom">#([0-9A-Fa-f]{6,8})</color>""")
            .find(xml)?.groupValues?.get(1)
    }

    /** Moyenne simple des trois canaux : le seuil est large, pas besoin de mieux. */
    private fun clarte(hex: String): Int {
        val rgb = hex.takeLast(6)
        return (0..2).sumOf { rgb.substring(it * 2, it * 2 + 2).toInt(16) } / 3
    }

    @Test
    fun `le fond de l icone est clair`() {
        val nom = Regex("""<background android:drawable="@color/([a-z0-9_]+)" */>""")
            .find(icone)?.groupValues?.get(1)
        assertTrue("le fond de l'icône doit être une couleur nommée", nom != null)
        val hex = couleur(nom!!)
        assertTrue("« $nom » introuvable dans values/colors.xml", hex != null)
        assertTrue(
            "le fond « $nom » (#$hex) est trop sombre : la frange blanche du " +
                "détourage s'y verrait comme une déchirure autour du personnage",
            clarte(hex!!) >= 200,
        )
    }

    /**
     * Le lanceur dessine l'icone avec son propre theme. Un fond qui
     * basculerait la nuit la rendrait sombre sans prevenir, et la frange
     * reviendrait.
     */
    @Test
    fun `le fond de l icone ne bascule pas la nuit`() {
        val nom = Regex("""<background android:drawable="@color/([a-z0-9_]+)" */>""")
            .find(icone)!!.groupValues[1]
        assertTrue(
            "« $nom » ne doit pas être redéfini dans values-night",
            couleur(nom, "values-night") == null,
        )
    }
}
