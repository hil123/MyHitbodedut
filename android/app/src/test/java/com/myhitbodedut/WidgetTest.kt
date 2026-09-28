package com.myhitbodedut

import org.junit.Assert.assertEquals
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

    private val gabarits = listOf("widget_small", "widget_streak", "widget_large")
        .associateWith { lire("src/main/res/layout/$it.xml") }

    /**
     * Chaque vue que StreakWidget remplit doit exister dans un des trois
     * gabarits, et chaque gabarit dans le code.
     */
    @Test
    fun `les gabarits portent toutes les vues que le code remplit`() {
        val code = lire("src/main/java/com/myhitbodedut/StreakWidget.kt")
        val attendues = Regex("""R\.id\.(widget_[a-z_0-9]+)""").findAll(code)
            .map { it.groupValues[1] }.toSet()
        assertTrue("le code ne remplit aucune vue", attendues.isNotEmpty())
        attendues.forEach { id ->
            assertTrue(
                "« $id » n'est dans aucun gabarit de la vignette",
                gabarits.values.any { it.contains("""android:id="@+id/$id"""") },
            )
        }
        gabarits.keys.forEach { assertTrue("$it n'est pas utilisé", code.contains("R.layout.$it")) }
        // Communes aux trois : le compte, et la vignette entière qui ouvre l'application.
        gabarits.forEach { (nom, xml) ->
            listOf("widget_body", "widget_streak").forEach {
                assertTrue("$nom : « $it » manque", xml.contains("""android:id="@+id/$it""""))
            }
        }
    }

    /** Une case, la taille habituelle, une grande place (demande du 28/09). */
    @Test
    fun `la version suit la place donnee`() {
        assertEquals(WidgetTaille.PETITE, WidgetTaille.pour(70f, 70f))
        assertEquals(WidgetTaille.MOYENNE, WidgetTaille.pour(250f, 150f))
        assertEquals(WidgetTaille.GRANDE, WidgetTaille.pour(320f, 300f))
        assertEquals(WidgetTaille.PETITE, WidgetTaille.pour(0f, 0f))
        val info = lire("src/main/res/xml/widget_streak_info.xml")
        assertTrue("la vignette doit pouvoir tenir sur une case", info.contains("""android:minResizeWidth="40dp""""))
    }

    /**
     * RemoteViews ne sait afficher qu'une poignee de vues. Une vue de
     * Material dans ce gabarit ne leverait rien a la compilation et
     * laisserait la vignette blanche sur l'ecran d'accueil.
     */
    @Test
    fun `les gabarits s en tiennent aux vues que RemoteViews accepte`() {
        val permises = setOf(
            "LinearLayout", "FrameLayout", "RelativeLayout", "GridLayout",
            "TextView", "ImageView", "Button", "ImageButton", "ProgressBar",
        )
        gabarits.forEach { (nom, gabarit) ->
            val vues = Regex("""<([A-Za-z][A-Za-z0-9.]*)""").findAll(gabarit)
                .map { it.groupValues[1] }.filterNot { it == "shape" }.toSet()
            vues.forEach {
                assertTrue("$nom : « $it » n'est pas acceptée par RemoteViews", it in permises)
            }
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
