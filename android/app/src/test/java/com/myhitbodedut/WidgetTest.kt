package com.myhitbodedut

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.math.pow

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

    private val code by lazy { lire("src/main/java/com/myhitbodedut/StreakWidget.kt") }

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
        assertTrue("l'aperçu d'Android 12 est le gabarit lui-même", info.contains("""android:previewLayout="@layout/widget_streak""""))
        assertTrue(info.contains("@string/widget_description"))
        assertTrue(info.contains("""android:widgetCategory="home_screen""""))
        assertTrue("la mise à jour horaire", info.contains("""android:updatePeriodMillis="3600000""""))
    }

    private val gabarits = listOf("widget_small", "widget_streak", "widget_large")
        .associateWith { lire("src/main/res/layout/$it.xml") }

    /**
     * Chaque vue que StreakWidget remplit doit exister dans un des trois
     * gabarits, et chaque gabarit dans le code.
     */
    @Test
    fun `les gabarits portent toutes les vues que le code remplit`() {
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
        // Communes aux trois : la vignette entière qui ouvre l'application,
        // son fond, les anneaux et le hassid, la série et le temps en direct.
        gabarits.forEach { (nom, xml) ->
            listOf("widget_body", "widget_fond", "widget_gauge", "widget_badge", "widget_streak", "widget_chrono").forEach {
                assertTrue("$nom : « $it » manque", xml.contains("""android:id="@+id/$it""""))
            }
            // Le lanceur d'Android 12 y reconnaît le fond, et l'arrondit comme les autres.
            assertTrue("$nom : pas de fond reconnu", xml.contains("""android:id="@android:id/background""""))
        }
        // La taille habituelle et la grande : la salutation, les minutes du jour, la semaine, le bouton.
        listOf("widget_streak", "widget_large").forEach { nom ->
            listOf("widget_greeting", "widget_minutes", "widget_week_label", "widget_start", "widget_start_fond").forEach {
                assertTrue("$nom : « $it » manque", gabarits.getValue(nom).contains("""android:id="@+id/$it""""))
            }
        }
        // La grande : les sept jours et le texte du jour avec sa référence.
        val grande = gabarits.getValue("widget_large")
        (0..6).forEach { i ->
            listOf("widget_day_$i", "widget_day_label_$i", "widget_day_fond_$i").forEach {
                assertTrue("grande : « $it » manque", grande.contains("""android:id="@+id/$it""""))
            }
        }
        listOf("widget_text", "widget_ref", "widget_text_fond").forEach {
            assertTrue("grande : « $it » manque", grande.contains("""android:id="@+id/$it""""))
        }
    }

    /** Une case, la taille habituelle, une grande place (demande du 28/09). */
    @Test
    fun `la version suit la place donnee`() {
        assertEquals(WidgetTaille.PETITE, WidgetTaille.pour(70f, 70f))
        assertEquals(WidgetTaille.MOYENNE, WidgetTaille.pour(250f, 150f))
        assertEquals(WidgetTaille.GRANDE, WidgetTaille.pour(320f, 300f))
        assertEquals(WidgetTaille.PETITE, WidgetTaille.pour(0f, 0f))
        // La place ordinaire de chaque version la redonne bien.
        WidgetTaille.entries.forEach {
            assertEquals(it, WidgetTaille.pour(it.largeurType, it.hauteurType))
        }
        val info = lire("src/main/res/xml/widget_streak_info.xml")
        assertTrue("la vignette doit pouvoir tenir sur une case", info.contains("""android:minResizeWidth="40dp""""))
        // Sous Android 12, le lanceur choisit parmi les versions ; avant, le code.
        assertTrue(code.contains("RemoteViews(") && code.contains("WidgetTaille.entries.associate"))
        assertTrue(code.contains("AppWidgetManager.OPTION_APPWIDGET_SIZES"))
        assertTrue(code.contains("WidgetTaille.pour("))
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
     * Le lanceur gonfle la vignette dans un contexte restreint, où TextView
     * ne charge pas les polices de l'application : « @font/… » y retomberait
     * sur la police par défaut sans rien dire. Le texte du jour prend la
     * famille serif du système.
     */
    @Test
    fun `le texte du jour est en hebreu a empattements sans police de l application`() {
        gabarits.forEach { (nom, xml) -> assertTrue("$nom : police de l'application", !xml.contains("@font/")) }
        val texte = gabarits.getValue("widget_large").substringAfter("""android:id="@+id/widget_text"""")
            .substringBefore("/>")
        assertTrue(texte.contains("""android:fontFamily="serif""""))
        assertTrue(texte.contains("""android:textDirection="rtl""""))
        assertTrue(code.contains("ChizukRules.hebrew(") && code.contains("Reference.localise("))
    }

    /**
     * Pendant la hitbodedout (demande du 28/09) : le temps en direct, tenu par
     * le lanceur, qui obéit au compteur masqué, et un bouton qui termine la
     * hitbodedout par la même action que la notification.
     */
    @Test
    fun `pendant la seance la vignette montre le temps et termine`() {
        assertTrue(code.contains("Store.sessionCounter(context)"))
        assertTrue("le compteur masqué doit rester masqué", code.contains("SessionRule.Compteur.MASQUE"))
        assertTrue(code.contains("setChronometerCountDown(R.id.widget_chrono"))
        assertTrue(code.contains("setAction(SessionService.ACTION_FINISH)"))
        assertTrue(code.contains("SessionActivity.EXTRA_START_NOW"))
        gabarits.forEach { (nom, xml) ->
            assertTrue("$nom : pas de compteur", xml.contains("""android:id="@+id/widget_chrono""""))
        }
    }

    /**
     * Les couleurs choisies dans les réglages valent aussi pour la vignette
     * (demande du 04/10). RemoteViews ne lit pas les attributs du thème, et
     * avant Android 12 ne teint pas un fond de vue : les couleurs passent par
     * Apparence, le fond est une image dessinée par le code, et chaque autre
     * fond arrondi est une image teinte par le code.
     */
    @Test
    fun `la vignette suit la palette choisie`() {
        assertTrue("la palette doit être résolue par Apparence", code.contains("Apparence.contexte(context)"))
        assertTrue(code.contains("Apparence.couleur("))
        assertTrue("une couleur lue sans la palette resterait verte", !code.contains("ContextCompat.getColor"))
        // Le dégradé part des jetons fixes de la palette, que le contraste ci-dessous vérifie.
        assertTrue(code.contains("VignetteDecor.degrade(c(R.color.day_done), c(R.color.brand_dark), nuit)"))
        assertTrue("le fond n'est pas dessiné par le code", code.contains("setImageViewBitmap(R.id.widget_fond"))
        assertTrue("les anneaux ne sont pas dessinés par le code", code.contains("setImageViewBitmap("))
        listOf("widget_start_fond", "widget_day_fond_0", "widget_badge_fond", "widget_text_fond",
               "widget_greeting", "widget_flame").forEach {
            assertTrue("« $it » n'est pas teint par le code", code.contains("R.id.$it"))
        }
        assertTrue(code.contains("\"setColorFilter\""))
        assertTrue("le voile d'un jour à venir", code.contains("\"setImageAlpha\""))

        gabarits.forEach { (nom, xml) ->
            // Un fond posé sur la vue elle-même ne pourrait plus changer de couleur.
            assertTrue("$nom : fond figé", !xml.contains("android:background="))
            assertTrue("$nom : pas de fond à peindre", xml.contains("""android:id="@+id/widget_fond""""))
        }

        // L'aperçu du sélecteur de vignettes est dessiné sans le code : les
        // formes gardent le vert d'origine, que le code recouvre ensuite.
        val formes = "src/main/res/drawable"
        assertTrue(lire("$formes/bg_widget.xml").contains("@color/brand_dark"))
        assertTrue(lire("$formes/bg_widget.xml").contains("<gradient"))
        assertTrue(lire("$formes/bg_widget_button.xml").contains("@color/stage_soft"))
        assertTrue(lire("$formes/bg_widget_day_done.xml").contains("@color/mint"))
        listOf("bg_widget.xml", "bg_widget_button.xml", "bg_widget_voile.xml").forEach {
            assertTrue("$it : coins carrés", lire("$formes/$it").contains("<corners"))
        }
        listOf("bg_widget_day_done.xml", "bg_widget_day_todo.xml").forEach {
            assertTrue("$it : un jour est une pastille ronde", lire("$formes/$it").contains("""android:shape="oval""""))
        }

        // Changer de palette ou de mode redessine la vignette tout de suite.
        val reglages = lire("src/main/java/com/myhitbodedut/SettingsActivity.kt")
        listOf("askFond", "askAccent", "askMode").forEach { f ->
            val corps = reglages.substringAfter("private fun $f(").substringBefore("private fun ")
            assertTrue("$f : la vignette n'est pas redessinée", corps.contains("StreakWidget.refresh(this)"))
        }
    }

    // --- Le contraste sur le dégradé ------------------------------------------

    private fun couleur(hex: String): Int {
        val h = hex.removePrefix("#")
        val rgb = h.takeLast(6).toLong(16).toInt()
        return (0xFF shl 24) or rgb
    }

    private fun luminance(c: Int): Double =
        listOf(16, 8, 0).map { decalage ->
            val v = ((c ushr decalage) and 0xFF) / 255.0
            if (v <= 0.03928) v / 12.92 else ((v + 0.055) / 1.055).pow(2.4)
        }.let { (r, g, b) -> 0.2126 * r + 0.7152 * g + 0.0722 * b }

    private fun contraste(a: Int, b: Int): Double {
        val (x, y) = luminance(a) to luminance(b)
        return (maxOf(x, y) + 0.05) / (minOf(x, y) + 0.05)
    }

    /** Pour chaque couleur des boutons, de jour et de nuit : day_done, brand_dark et mint. */
    private fun accents(nuit: Boolean): Map<String, Triple<Int, Int, Int>> {
        fun couleurs(dossier: String) =
            Regex("""<color name="([a-z0-9_]+)">(#[0-9A-Fa-f]{6,8})</color>""")
                .findAll(lire("src/main/res/$dossier/colors.xml"))
                .associate { it.groupValues[1] to it.groupValues[2] }
        val base = couleurs("values") + if (nuit) couleurs("values-night") else emptyMap()
        val vert = "Accent.Vert" to Triple(
            couleur(base.getValue("day_done")), couleur(base.getValue("brand_dark")), couleur(base.getValue("mint"))
        )
        val styles = Regex("""<style name="(Accent\.[A-Za-z]+)"[^>]*>([\s\S]*?)</style>""")
            .findAll(lire("src/main/res/${if (nuit) "values-night" else "values"}/palettes.xml"))
            .associate { style ->
                val items = Regex("""<item name="([A-Za-z:]+)">([^<]+)</item>""").findAll(style.groupValues[2])
                    .associate { it.groupValues[1] to it.groupValues[2] }
                style.groupValues[1] to Triple(
                    couleur(items.getValue("mhDayDone")), couleur(items.getValue("mhBrandDark")),
                    couleur(items.getValue("mhMint"))
                )
            }
        assertTrue("aucune palette lue", styles.isNotEmpty())
        return mapOf(vert) + styles
    }

    /**
     * Le fond n'est plus une carte unie : un dégradé et deux arcs pâles. Le
     * blanc des chiffres et le « mint » des libellés doivent s'y lire
     * partout, pour chaque couleur des boutons, de jour comme de nuit — au
     * seuil AA du texte courant, comme le reste de l'application.
     */
    @Test
    fun `le texte reste lisible sur tout le degrade`() {
        val fautes = mutableListOf<String>()
        listOf(false, true).forEach { nuit ->
            accents(nuit).forEach { (nom, c) ->
                val (jourFait, brandDark, mint) = c
                val (clair, fonce) = VignetteDecor.degrade(jourFait, brandDark, nuit)
                val pire = VignetteDecor.pireFond(jourFait, brandDark, nuit)
                listOf(clair, fonce, pire).forEach { fond ->
                    listOf("blanc" to VignetteDecor.BLANC, "mint" to mint).forEach { (encre, e) ->
                        val r = contraste(e, fond)
                        if (r < 4.5) fautes += "$nom ${if (nuit) "nuit" else "jour"}, $encre : ${"%.2f".format(r)}"
                    }
                }
            }
        }
        assertTrue("contrastes insuffisants :\n" + fautes.joinToString("\n"), fautes.isEmpty())
        // Les voiles posés sous le texte sont sombres : ils ne font que gagner du contraste.
        assertTrue(lire("src/main/res/drawable/bg_widget_voile.xml").contains("#000000"))
        assertTrue(code.contains("setInt(R.id.widget_text_fond, \"setColorFilter\", VignetteDecor.NOIR)"))
        assertTrue(code.contains("setInt(R.id.widget_badge_fond, \"setColorFilter\", VignetteDecor.NOIR)"))
    }

    /** Une image trop grande ferait refuser la vignette entière par RemoteViews. */
    @Test
    fun `le fond reste petit et garde ses proportions`() {
        listOf(1f, 2.625f, 3.5f, 4f).forEach { densite ->
            WidgetTaille.entries.forEach { t ->
                val (l, h) = VignetteDecor.pixels(t.largeurType, t.hauteurType, densite)
                assertTrue("$t × $densite : $l × $h", l <= VignetteDecor.COTE_MAX && h <= VignetteDecor.COTE_MAX)
                assertEquals(t.largeurType / t.hauteurType, l.toFloat() / h, 0.02f)
            }
        }
        assertEquals(150 to 75, VignetteDecor.pixels(150f, 75f, 1f))
        assertEquals(VignetteDecor.BLANC, VignetteDecor.melange(VignetteDecor.NOIR, VignetteDecor.BLANC, 1f))
        assertEquals(VignetteDecor.NOIR, VignetteDecor.melange(VignetteDecor.NOIR, VignetteDecor.BLANC, 0f))
        assertEquals(0x80FFFFFF.toInt(), VignetteDecor.opacite(VignetteDecor.BLANC, 128 / 255f))
    }

    /**
     * La vitrine de l'édition de test montre les trois versions sans
     * lanceur, construites par le même code que l'écran d'accueil.
     */
    @Test
    fun `la vitrine de debug montre les vignettes telles quelles`() {
        val manifeste = lire("src/debug/AndroidManifest.xml")
        assertTrue(manifeste.contains("com.myhitbodedut.VitrineWidgetActivity"))
        assertTrue(manifeste.contains("""android:exported="true""""))
        assertTrue("absente de la release", !lire("src/main/AndroidManifest.xml").contains("VitrineWidgetActivity"))
        val vitrine = lire("src/debug/java/com/myhitbodedut/VitrineWidgetActivity.kt")
        assertTrue(vitrine.contains("StreakWidget.apercu(") && vitrine.contains(".apply("))
        assertTrue(vitrine.contains("WidgetTaille.entries"))
        assertTrue(code.contains("internal fun apercu(context: Context, taille: WidgetTaille): RemoteViews"))
    }

    @Test
    fun `les libelles de la vignette existent dans les trois langues`() {
        val cles = listOf(
            "widget_label", "widget_description", "widget_done", "widget_todo",
            "widget_running", "widget_start", "widget_resume", "widget_minutes",
        )
        listOf("values", "values-en", "values-iw").forEach { dossier ->
            val xml = lire("src/main/res/$dossier/strings.xml")
            cles.forEach {
                assertTrue("$dossier : « $it » manquant", xml.contains("""name="$it""""))
            }
        }
        // La salutation de l'accueil, et la semaine en toutes lettres : les mêmes chaînes.
        assertTrue(code.contains("Salutation.chaine(") && code.contains("R.plurals.week_gauge"))
    }
}
