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
            "TextView", "ImageView", "Button", "ImageButton", "ProgressBar", "Chronometer",
        )
        gabarits.forEach { (nom, gabarit) ->
            val vues = Regex("""<([A-Za-z][A-Za-z0-9.]*)""").findAll(gabarit)
                .map { it.groupValues[1] }.filterNot { it == "shape" }.toSet()
            vues.forEach {
                assertTrue("$nom : « $it » n'est pas acceptée par RemoteViews", it in permises)
            }
        }
    }

    /**
     * Pendant la séance (demande du 28/09) : le temps en direct, tenu par
     * le lanceur, qui obéit au compteur masqué, et un bouton qui termine la
     * séance par la même action que la notification.
     */
    @Test
    fun `pendant la seance la vignette montre le temps et termine`() {
        val code = lire("src/main/java/com/myhitbodedut/StreakWidget.kt")
        assertTrue(code.contains("Store.sessionCounter(context)"))
        assertTrue("le compteur masqué doit rester masqué", code.contains("SessionRule.Compteur.MASQUE"))
        assertTrue(code.contains("setChronometerCountDown(R.id.widget_chrono"))
        assertTrue(code.contains("setAction(SessionService.ACTION_FINISH)"))
        gabarits.forEach { (nom, xml) ->
            assertTrue("$nom : pas de compteur", xml.contains("""android:id="@+id/widget_chrono""""))
        }
    }

    /**
     * Les couleurs choisies dans les réglages valent aussi pour la vignette
     * (demande du 04/10). RemoteViews ne lit pas les attributs du thème, et
     * avant Android 12 ne teint pas un fond de vue : les couleurs passent par
     * Apparence, et chaque fond arrondi est une image teinte par le code.
     */
    @Test
    fun `la vignette suit la palette choisie`() {
        val code = lire("src/main/java/com/myhitbodedut/StreakWidget.kt")
        assertTrue("la palette doit être résolue par Apparence", code.contains("Apparence.contexte(context)"))
        assertTrue(code.contains("Apparence.couleur("))
        assertTrue("une couleur lue sans la palette resterait verte", !code.contains("ContextCompat.getColor"))
        listOf("widget_fond", "widget_start_fond", "widget_day_fond_0", "widget_title").forEach {
            assertTrue("« $it » n'est pas teint par le code", code.contains("R.id.$it"))
        }
        assertTrue(code.contains("\"setColorFilter\""))
        assertTrue("le voile d'un jour à venir", code.contains("\"setImageAlpha\""))

        gabarits.forEach { (nom, xml) ->
            // Un fond posé sur la vue elle-même ne pourrait plus changer de couleur.
            assertTrue("$nom : fond figé", !xml.contains("android:background="))
            assertTrue("$nom : pas de fond à teindre", xml.contains("""android:id="@+id/widget_fond""""))
        }
        listOf("widget_streak", "widget_large").forEach { nom ->
            assertTrue(
                "$nom : le bouton n'a pas de fond à teindre",
                gabarits.getValue(nom).contains("""android:id="@+id/widget_start_fond""""),
            )
        }
        (0..6).forEach {
            assertTrue(
                "jour $it : pas de fond à teindre",
                gabarits.getValue("widget_large").contains("""android:id="@+id/widget_day_fond_$it""""),
            )
        }

        // L'aperçu du sélecteur de vignettes est dessiné sans le code : les
        // formes gardent le vert d'origine, que le code recouvre ensuite.
        val formes = "src/main/res/drawable"
        assertTrue(lire("$formes/bg_widget.xml").contains("@color/brand_dark"))
        assertTrue(lire("$formes/bg_widget_button.xml").contains("@color/mint"))
        assertTrue(lire("$formes/bg_widget_day_done.xml").contains("@color/mint"))
        listOf("bg_widget.xml", "bg_widget_button.xml", "bg_widget_day_done.xml").forEach {
            assertTrue("$it : coins carrés", lire("$formes/$it").contains("<corners"))
        }

        // Changer de palette ou de mode redessine la vignette tout de suite.
        val reglages = lire("src/main/java/com/myhitbodedut/SettingsActivity.kt")
        listOf("askFond", "askAccent", "askMode").forEach { f ->
            val corps = reglages.substringAfter("private fun $f(").substringBefore("private fun ")
            assertTrue("$f : la vignette n'est pas redessinée", corps.contains("StreakWidget.refresh(this)"))
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
