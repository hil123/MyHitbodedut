package com.myhitbodedut

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Point 6 : l'écran de relecture montre tout, et ne sort pas de debug.
 */
class ProofreadTest {

    private val item = File("src/main/res/layout/item_proofread.xml").readText()
    private val activity = File("src/main/java/com/myhitbodedut/ProofreadActivity.kt").readText()
    private val main = File("src/main/java/com/myhitbodedut/MainActivity.kt").readText()
    private val mainLayout = File("src/main/res/layout/activity_main.xml").readText()

    @Test
    fun `la fiche montre l hebreu, les deux traductions et la reference`() {
        listOf("proofread_he", "proofread_fr", "proofread_en", "proofread_ref").forEach {
            assertTrue("item_proofread.xml : vue $it absente", item.contains("""@+id/$it"""))
        }
    }

    @Test
    fun `rien n est tronque ni retreci sur une fiche de relecture`() {
        // L'inverse de la carte de 'hizouk : ici il faut lire le texte entier
        // pour le comparer à la source.
        listOf("android:maxLines", "android:ellipsize", "android:autoSizeTextType").forEach {
            assertFalse("item_proofread.xml contient $it", item.contains(it))
        }
    }

    @Test
    fun `l hebreu de la fiche est en droite-a-gauche`() {
        assertTrue(item.contains("""android:layoutDirection="rtl""""))
    }

    @Test
    fun `l ecran parcourt tous les items, sans filtre de publication`() {
        assertTrue(
            "ProofreadActivity doit lister chizuk.items en entier",
            activity.contains("items = chizuk.items"),
        )
        assertFalse(
            "aucun filtre de publication ici : c'est ce qui reste à relire qu'on relit",
            activity.contains("ChizukRules.visible"),
        )
        assertEquals(45, DataFiles.chizuk.items.size)
    }

    @Test
    fun `l hebreu montre est celui que l application afficherait`() {
        assertTrue(
            "relire une variante que l'app n'affiche pas ne sert à rien",
            activity.contains("ChizukRules.hebrew(chizuk, item)"),
        )
    }

    @Test
    fun `le bouton d acces est masque hors compilation de test`() {
        assertTrue(
            "activity_main.xml : le bouton doit partir masqué",
            Regex(
                """@\+id/proofread_button[\s\S]{0,400}?android:visibility="gone""""
            ).containsMatchIn(mainLayout),
        )
        assertTrue(
            "MainActivity : la visibilité doit dépendre de BuildConfig.DEBUG",
            Regex(
                """R\.id\.proofread_button[\s\S]{0,200}?BuildConfig\.DEBUG"""
            ).containsMatchIn(main),
        )
    }
}
