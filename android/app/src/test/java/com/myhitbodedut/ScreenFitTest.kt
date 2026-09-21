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
     * Material3 reclame 88dp ; quatre en reclament 370 avec leurs marges,
     * et il n'y en avait que 268 de disponibles. « 20 min » se faisait
     * couper sur un ecran de 320dp.
     */
    @Test
    fun `les quatre durees tiennent sur un ecran de 320dp`() {
        val bord = dimen("values", "edge")!!
        val seance = res("layout/activity_session.xml").readText()
        val boutons = Regex("""android:id="@\+id/duration_\d+"""").findAll(seance).count()
        assertTrue("il faut quatre boutons de durée", boutons == 4)

        val marges = Regex("""android:layout_marginStart="([\d.]+)dp"""")
            .findAll(seance.substringAfter("session_durations").substringBefore("</LinearLayout>"))
            .sumOf { it.groupValues[1].toDouble() }

        val themes = res("values/themes.xml").readText()
        val style = themes.substringAfter("""<style name="DurationButton"""").substringBefore("</style>")
        val remplissage = Regex("""<item name="android:paddingHorizontal">([\d.]+)dp</item>""")
            .find(style)?.groupValues?.get(1)?.toDouble() ?: 24.0
        assertTrue(
            "le bouton doit renoncer à sa largeur minimale, sinon il déborde",
            style.contains("""<item name="android:minWidth">0dp</item>"""),
        )

        val disponible = PLANCHER - 2 * bord - marges
        val parBouton = disponible / boutons
        val pourLeTexte = parBouton - 2 * remplissage
        // « 20 min » en 13sp demande environ 40dp.
        assertTrue(
            "sur 320dp il ne reste que ${"%.1f".format(pourLeTexte)}dp par étiquette, " +
                "il en faut 40 pour « 20 min »",
            pourLeTexte >= 40.0,
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
        // Et sur une fenetre de bureau, il doit vraiment brider la largeur.
        val large = dimen("values-w1200dp", "wide_gutter")!! * 2 + dimen("values-w1200dp", "edge")!! * 2
        assertTrue(
            "sur 1200dp il resterait ${1200 - large}dp de ligne : trop long à lire",
            1200 - large <= 700,
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
