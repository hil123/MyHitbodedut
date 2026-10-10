package com.myhitbodedut

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * La tenue des ecrans sur le plus petit Android, et sur le plus grand.
 *
 * 320dp de large est le plancher d'Android — c'est ce que donne un ecran
 * de 480x854 en hdpi, et beaucoup de telephones d'entree de gamme n'ont
 * rien de plus. Rien ne signale un debordement a la compilation : la vue
 * est simplement rognee sur l'appareil de quelqu'un d'autre.
 */
class ScreenFitTest {

    private val PLANCHER = 320.0   // la plus petite largeur qu'Android connaisse
    private val PLAFOND = 320.0    // la hauteur en paysage sur ce meme ecran

    private fun res(chemin: String) = File("src/main/res/$chemin")

    private fun dimen(dossier: String, nom: String): Double? {
        val f = res("$dossier/dimens.xml")
        if (!f.exists()) return null
        return Regex("""<dimen name="$nom">([\d.]+)dp</dimen>""")
            .find(f.readText())?.groupValues?.get(1)?.toDouble()
    }

    private fun ecrans() = res("layout").listFiles { f -> f.name.startsWith("activity_") }!!
        .sortedBy { it.name }

    /**
     * Le cas qui a mordu : quatre boutons de duree cote a cote. Un bouton
     * Material3 reclame 88dp ; quatre en reclamaient 370 avec leurs marges,
     * et il n'y en avait que 268 de disponibles. « 20 min » se faisait
     * couper sur un ecran de 320dp.
     *
     * Depuis le 28/09 : six durees (5, 10, 15, 30 min, 1 h, 1 h 30), en
     * deux rangees de trois. L'etiquette la plus longue, « 1 h 30 min » en
     * anglais, demande environ 70dp en 13sp.
     */
    @Test
    fun `les six durees tiennent sur un ecran de 320dp`() {
        val bord = dimen("values", "edge")!!
        val seance = res("layout/activity_session.xml").readText()
        val bloc = seance.substringAfter("@+id/session_durations").substringBefore("@+id/session_custom")
        val boutons = Regex("""android:id="@\+id/duration_\d+"""").findAll(bloc).count()
        assertTrue("il faut six boutons de durée", boutons == 6)

        val rangee = bloc.substringAfter("android:orientation=\"horizontal\"").substringBefore("</LinearLayout>")
        val parRangee = Regex("""android:id="@\+id/duration_\d+"""").findAll(rangee).count()
        assertTrue("trois boutons par rangée", parRangee == 3)
        val marges = Regex("""android:layout_marginStart="([\d.]+)dp"""")
            .findAll(rangee).sumOf { it.groupValues[1].toDouble() }

        val themes = res("values/themes.xml").readText()
        val style = themes.substringAfter("""<style name="DurationButton"""").substringBefore("</style>")
        val remplissage = Regex("""<item name="android:paddingHorizontal">([\d.]+)dp</item>""")
            .find(style)?.groupValues?.get(1)?.toDouble() ?: 24.0
        assertTrue(
            "le bouton doit renoncer à sa largeur minimale, sinon il déborde",
            style.contains("""<item name="android:minWidth">0dp</item>"""),
        )

        val disponible = PLANCHER - 2 * bord - marges
        val pourLeTexte = disponible / parRangee - 2 * remplissage
        assertTrue(
            "sur 320dp il ne reste que ${"%.1f".format(pourLeTexte)}dp par étiquette, " +
                "il en faut 70 pour « 1 h 30 min »",
            pourLeTexte >= 70.0,
        )
    }

    /**
     * En paysage l'ecran ne fait plus que 320dp de haut. Un ecran qui ne
     * defile pas y perd ses boutons du bas, sans aucun signe.
     */
    @Test
    fun `chaque ecran peut defiler`() {
        val sansDefilement = ecrans().filter { f ->
            val xml = f.readText()
            !xml.contains("ScrollView") && !xml.contains("RecyclerView") &&
                !xml.contains("<ListView")
        }
        assertTrue(
            "ces écrans ne défilent pas et seront tronqués en paysage " +
                "($PLAFOND dp de haut) : ${sansDefilement.map { it.name }}",
            sansDefilement.isEmpty(),
        )
    }

    /** Une marge d'ecran inscrite en dur ne peut pas suivre la largeur. */
    @Test
    fun `aucun ecran n inscrit sa marge en dur`() {
        val fautes = ecrans().mapNotNull { f ->
            val dures = Regex("""android:paddingHorizontal="(\d+)dp"""")
                .findAll(f.readText())
                .map { it.groupValues[1].toInt() }
                .filter { it >= 18 }   // au-delà, c'est une marge d'écran
                .toList()
            if (dures.isEmpty()) null else "${f.name} : ${dures}dp"
        }
        assertTrue("marges d'écran en dur : $fautes", fautes.isEmpty())
    }

    /**
     * Les paliers sont declines par largeur DISPONIBLE (« w ») et non par
     * largeur d'appareil (« sw ») : sur ChromeOS et en fenetre partagee, la
     * fenetre se redimensionne et « sw » ne bouge pas.
     */
    @Test
    fun `les paliers de largeur sont complets et croissants`() {
        val paliers = listOf("values", "values-w360dp", "values-w600dp",
                             "values-w840dp", "values-w1200dp")
        var precedentBord = 0.0
        var precedentAir = 0.0
        paliers.forEach { p ->
            val bord = dimen(p, "edge")
            val air = dimen(p, "wide_gutter")
            assertTrue("« edge » manque dans $p", bord != null)
            assertTrue("« wide_gutter » manque dans $p", air != null)
            assertTrue("$p : « edge » ne doit pas décroître ($precedentBord -> $bord)",
                bord!! >= precedentBord)
            assertTrue("$p : « wide_gutter » ne doit pas décroître ($precedentAir -> $air)",
                air!! >= precedentAir)
            precedentBord = bord
            precedentAir = air
        }
        // Sur telephone, l'ecran principal ne doit rien perdre : les cartes
        // portent deja leur marge.
        assertTrue("sur téléphone l'air des grands écrans doit être nul",
            dimen("values", "wide_gutter") == 0.0 && dimen("values-w360dp", "wide_gutter") == 0.0)
        // Et sur une fenetre de bureau, il doit vraiment brider la largeur,
        // sans l'etrangler : l'air et la marge interieure s'ajoutent
        // (edge_inner, LayoutPaysageTest).
        val large = (dimen("values-w1200dp", "wide_gutter")!! + dimen("values-w1200dp", "edge_inner")!!) * 2
        assertTrue(
            "sur 1200dp il resterait ${1200 - large}dp de ligne : trop long à lire",
            1200 - large <= 700,
        )
        assertTrue(
            "sur 1200dp il ne resterait que ${1200 - large}dp de ligne : l'écran paraît vide",
            1200 - large >= 480,
        )
    }

    /**
     * Les hauteurs fixes se declinent aussi. En paysage sur un 480x854 il
     * ne reste que 320dp de haut : une illustration de 190dp en mangeait
     * 60 %. Le palier par defaut doit donc viser l'ecran COURT, et
     * values-h480dp donner les valeurs confortables — l'inverse serait une
     * regression invisible sur les petits ecrans.
     */
    @Test
    fun `les hauteurs fixes visent l ecran court par defaut`() {
        val tailles = listOf("art_height", "art_height_small",
                             "chizuk_he_height", "chizuk_body_height")
        tailles.forEach { nom ->
            val court = dimen("values", nom)
            val normal = dimen("values-h480dp", nom)
            assertTrue("« $nom » manque dans values/", court != null)
            assertTrue("« $nom » manque dans values-h480dp/", normal != null)
            assertTrue(
                "« $nom » : le défaut ($court) doit être le plus petit, " +
                    "c'est lui qui s'applique en paysage",
                court!! < normal!!,
            )
        }
        // Et la plus grande doit tenir dans les 320dp d'un paysage.
        assertTrue(
            "l'illustration par défaut doit laisser de la place au reste",
            dimen("values", "art_height")!! <= PLAFOND / 2,
        )
    }

    /**
     * Sur un ordinateur la fenetre est recreee a chaque redimensionnement.
     * Un ecran qui garde un etat en memoire doit l'inscrire, sinon il
     * revient au depart chaque fois qu'on tire un coin de la fenetre.
     */
    @Test
    fun `les ecrans a memoire conservent leur etat`() {
        val aMemoire = listOf("SessionActivity", "MainActivity", "OnboardingActivity")
        aMemoire.forEach { nom ->
            val code = File("src/main/java/com/myhitbodedut/$nom.kt").readText()
            assertTrue(
                "$nom garde un état en mémoire sans le sauvegarder : " +
                    "une rotation ou un redimensionnement le perdrait",
                code.contains("override fun onSaveInstanceState"),
            )
            assertTrue(
                "$nom sauvegarde son état mais ne le relit pas",
                code.contains("savedInstanceState?."),
            )
        }
    }

    /**
     * Sans cette declaration, Android tient l'ecran tactile pour requis et
     * Google Play retire l'application des Chromebooks qui n'en ont pas.
     */
    @Test
    fun `l ecran tactile n est pas exige`() {
        val manifeste = File("src/main/AndroidManifest.xml").readText()
        assertTrue(
            "il manque <uses-feature android.hardware.touchscreen required=false>",
            Regex("""android:name="android\.hardware\.touchscreen"\s+android:required="false"""")
                .containsMatchIn(manifeste),
        )
        assertTrue(
            "la fenêtre doit être redimensionnable pour l'écran partagé et le bureau",
            manifeste.contains("""android:resizeableActivity="true""""),
        )
        assertTrue(
            "l'écran d'accueil doit annoncer une taille de fenêtre au lancement",
            manifeste.contains("<layout") && manifeste.contains("android:minWidth="),
        )
    }

    /** Sans palier « w », le redimensionnement d'une fenetre ne changerait rien. */
    @Test
    fun `les paliers reagissent a la largeur de la fenetre`() {
        val dossiers = res(".").listFiles()!!.map { it.name }
        assertTrue(
            "les paliers doivent être en « w » (largeur disponible), pas en « sw »",
            dossiers.any { it.startsWith("values-w") && !it.startsWith("values-sw") },
        )
    }
}
