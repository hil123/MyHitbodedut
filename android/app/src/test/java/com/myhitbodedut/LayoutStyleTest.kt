package com.myhitbodedut

import org.junit.Assert.assertTrue
import java.io.File
import org.junit.Test

/**
 * Garde anti-régression du défaut de mise en page corrigé le 20/09/2026.
 *
 * Le style SettingLabel porte une largeur de 0dp et un poids de 1. Dans une
 * ligne horizontale c'est correct : le libellé prend la place que le bouton de
 * valeur laisse. Dans une colonne verticale, la largeur de 0dp est prise au
 * pied de la lettre, donc le libellé s'affiche sur zéro pixel et disparaît, et
 * le poids s'applique alors à la hauteur, donc il avale tout l'espace restant.
 *
 * Ce test relit les mises en page et échoue si le style réapparaît dans une
 * colonne. Il ne dépend d'aucun appareil : la taille d'écran et l'échelle de
 * police ne changent rien au défaut, qui tient à l'axe du conteneur.
 */
class LayoutStyleTest {

    private val layouts = File("src/main/res/layout")

    @Test
    fun `aucun libelle a largeur nulle dans une colonne verticale`() {
        val faults = mutableListOf<String>()
        layouts.listFiles { f -> f.extension == "xml" }?.sortedBy { it.name }?.forEach { file ->
            val lines = file.readText().split("\n")
            lines.forEachIndexed { index, line ->
                if (!line.contains("@style/SettingLabel\"")) return@forEachIndexed
                if (parentIsVertical(lines, index)) {
                    faults += "${file.name}:${index + 1}"
                }
            }
        }
        assertTrue(
            "SettingLabel dans un conteneur vertical, le libelle sera invisible : $faults",
            faults.isEmpty()
        )
    }

    @Test
    fun `les mises en page existent bien`() {
        assertTrue("dossier des layouts introuvable", layouts.isDirectory)
        assertTrue("aucune mise en page lue", layouts.listFiles().orEmpty().isNotEmpty())
    }

    /** Remonte jusqu'au LinearLayout ouvrant le plus proche et lit son orientation. */
    private fun parentIsVertical(lines: List<String>, from: Int): Boolean {
        var depth = 0
        for (j in from - 1 downTo 0) {
            val line = lines[j]
            when {
                line.contains("</LinearLayout>") -> depth++
                line.contains("<LinearLayout") -> {
                    if (depth == 0) {
                        val block = lines.subList(j, from).joinToString("\n")
                        return block.contains("orientation=\"vertical\"") ||
                            line.contains("@style/Card")
                    }
                    depth--
                }
            }
        }
        return false
    }
}
