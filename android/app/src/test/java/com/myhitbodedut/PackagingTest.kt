package com.myhitbodedut

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * L'édition de test légère et son identifiant.
 *
 * Elle portait le même identifiant que Relecture, laquelle est reconstruite
 * à chaque envoi sur `main` (le workflow Android se déclenche sur tout
 * push). Le versionCode de l'édition d'essai, lui, n'avançait qu'à la
 * demande : presque toujours en retard sur Relecture, il faisait refuser
 * l'installation par Android (INSTALL_FAILED_VERSION_DOWNGRADE) — sans que
 * rien ici ne le détecte, puisqu'aucun test ne peut installer un APK.
 *
 * Ce que ce test garde : la trace textuelle des deux moitiés du correctif,
 * pour qu'un retour en arrière sur l'une sans l'autre soit visible avant
 * d'être découvert sur un téléphone.
 */
class PackagingTest {

    private val gradle = File("build.gradle.kts").readText()
    private val workflow = File("../../.github/workflows/apk-essai.yml").readText()

    @Test
    fun `l edition d essai porte un identifiant different de relecture`() {
        assertTrue(
            "build.gradle.kts ne distingue plus l'édition d'essai par son identifiant",
            gradle.contains("""applicationIdSuffix = if (essaiLeger) ".debug.essai" else ".debug""""),
        )
        assertTrue(
            "le drapeau ESSAI_LEGER a disparu de build.gradle.kts",
            gradle.contains("""System.getenv("ESSAI_LEGER") == "true""""),
        )
    }

    @Test
    fun `le flux d essai active le drapeau qui le distingue de relecture`() {
        assertTrue(
            "apk-essai.yml ne pose plus ESSAI_LEGER : l'édition redeviendrait " +
                "celle de Relecture, avec le même risque de versionCode en retard",
            workflow.contains("ESSAI_LEGER: \"true\""),
        )
    }
}
