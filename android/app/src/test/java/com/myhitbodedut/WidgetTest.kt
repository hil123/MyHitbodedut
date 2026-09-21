package com.myhitbodedut

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * La vignette d'ecran d'accueil. Rien de son rendu ne peut etre execute
 * ici — il faut un AppWidgetManager — mais tout ce qui la rend seulement
 * possible se verifie : declaree dans le manifeste, decrite dans son
 * fichier de configuration, et pourvue des vues que le code remplit.
 *
 * Une vignette dont le code nomme une vue absente du gabarit ne plante
 * pas : elle reste vide sur l'ecran d'accueil, sans rien dire.
 */
class WidgetTest {

    private fun lire(chemin: String) = File(chemin).readText()

    @Test
    fun `la vignette est declaree dans le manifeste`() {
        val manifeste = lire("src/main/AndroidManifest.xml")
        assertTrue(manifeste.contains("""android:name=".StreakWidget""""))
        assertTrue(manifeste.contains("android.appwidget.action.APPWIDGET_UPDATE"))
        assertTrue(manifeste.contains("@xml/widget_streak_info"))
    }

    @Test
    fun `la configuration pointe sur le gabarit`() {
        val info = lire("src/main/res/xml/widget_streak_info.xml")
        assertTrue(info.contains("@layout/widget_streak"))
        assertTrue(info.contains("@string/widget_description"))
        assertTrue(info.contains("""android:widgetCategory="home_screen""""))
    }

    /** Chaque vue que StreakWidget remplit doit exister dans le gabarit. */
    @Test
    fun `le gabarit porte toutes les vues que le code remplit`() {
        val gabarit = lire("src/main/res/layout/widget_streak.xml")
        val code = lire("src/main/java/com/myhitbodedut/StreakWidget.kt")
        val attendues = Regex("""R\.id\.(widget_[a-z_]+)""").findAll(code)
            .map { it.groupValues[1] }.toSet()
        assertTrue("le code ne remplit aucune vue", attendues.isNotEmpty())
        attendues.forEach {
            assertTrue(
                "« $it » manque dans widget_streak.xml",
                gabarit.contains("""android:id="@+id/$it""""),
            )
        }
    }

    /**
     * RemoteViews ne sait afficher qu'une poignee de vues. Une vue de
     * Material dans ce gabarit ne leverait rien a la compilation et
     * laisserait la vignette blanche sur l'ecran d'accueil.
     */
    @Test
    fun `le gabarit s en tient aux vues que RemoteViews accepte`() {
        val gabarit = lire("src/main/res/layout/widget_streak.xml")
        val vues = Regex("""<([A-Za-z][A-Za-z0-9.]*)""").findAll(gabarit)
            .map { it.groupValues[1] }.filterNot { it == "shape" }.toSet()
        val permises = setOf(
            "LinearLayout", "FrameLayout", "RelativeLayout", "GridLayout",
            "TextView", "ImageView", "Button", "ImageButton", "ProgressBar",
        )
        vues.forEach {
            assertTrue("« $it » n'est pas acceptee par RemoteViews", it in permises)
        }
    }

    @Test
    fun `les libelles de la vignette existent dans les trois langues`() {
        val cles = listOf(
            "widget_label", "widget_description", "widget_done", "widget_todo",
            "widget_running", "widget_start", "widget_resume",
        )
        listOf("values", "values-en", "values-iw").forEach { dossier ->
            val xml = lire("src/main/res/$dossier/strings.xml")
            cles.forEach {
                assertTrue("$dossier : « $it » manquant", xml.contains("""name="$it""""))
            }
        }
    }
}
