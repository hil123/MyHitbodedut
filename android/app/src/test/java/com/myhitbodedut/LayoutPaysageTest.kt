package com.myhitbodedut

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Les mises en page couchées (res/layout-land) et l'air des grands écrans.
 *
 * Une mise en page couchée remplace la debout sans que l'activité le sache :
 * il lui faut donc exactement les mêmes identifiants, sinon findViewById
 * rend null au premier quart de tour, sur le téléphone de quelqu'un d'autre.
 */
class LayoutPaysageTest {

    private fun res(chemin: String) = File("src/main/res/$chemin")
    private fun ids(f: File) = Regex("""@\+id/(\w+)""").findAll(f.readText()).map { it.groupValues[1] }.toSortedSet()

    private fun couchees() = res("layout-land").listFiles { f -> f.extension == "xml" }.orEmpty().sortedBy { it.name }

    @Test
    fun `chaque mise en page couchee a les identifiants de la debout`() {
        assertTrue("aucune mise en page couchée lue", couchees().isNotEmpty())
        couchees().forEach { f ->
            val debout = res("layout/${f.name}")
            assertTrue("${f.name} couchée sans version debout", debout.exists())
            assertEquals("${f.name} : identifiants différents de la version debout", ids(debout), ids(f))
        }
    }

    @Test
    fun `l accueil la seance et la presentation ont leur version couchee`() {
        listOf("activity_main.xml", "activity_session.xml", "activity_onboarding.xml").forEach {
            assertTrue("$it sans version couchée", res("layout-land/$it").exists())
        }
    }

    /** Couché, il ne reste que 320dp de haut sur un 480x854 : tout doit défiler. */
    @Test
    fun `les mises en page couchees defilent`() {
        couchees().forEach { f ->
            assertTrue("${f.name} couchée ne défile pas", f.readText().contains("ScrollView"))
        }
    }

    @Test
    fun `aucune mise en page couchee n inscrit sa marge en dur`() {
        couchees().forEach { f ->
            val dures = Regex("""android:paddingHorizontal="(\d+)dp"""").findAll(f.readText())
                .map { it.groupValues[1].toInt() }.filter { it >= 18 }.toList()
            assertTrue("${f.name} couchée : marges en dur $dures", dures.isEmpty())
        }
    }

    /**
     * Couchée, la présentation garde « Suivant » visible : le bouton est hors
     * du ScrollView, sous lui, et le ScrollView seul porte un poids.
     */
    @Test
    fun `couchee la presentation garde Suivant hors du defilement`() {
        val xml = res("layout-land/activity_onboarding.xml").readText()
        val defile = xml.substringAfter("<ScrollView").substringBefore("</ScrollView>")
        assertTrue("« Suivant » ne doit pas défiler", !defile.contains("@+id/onboarding_next"))
        assertTrue("le texte doit défiler", defile.contains("@+id/onboarding_body"))
    }

    /** Couchée, la séance montre son bouton principal en tête de la colonne des actions. */
    @Test
    fun `couchee la seance met le bouton principal avant les durees`() {
        val xml = res("layout-land/activity_session.xml").readText()
        assertTrue(xml.indexOf("@+id/session_primary") < xml.indexOf("@+id/session_durations"))
    }

    private fun dimen(dossier: String, nom: String): Double? =
        Regex("""<dimen name="$nom">([\d.]+)dp</dimen>""").find(res("$dossier/dimens.xml").readText())
            ?.groupValues?.get(1)?.toDouble()

    /**
     * Ce qui est posé dans l'air des grands écrans (wide_gutter) prend
     * edge_inner, pas edge : les deux s'ajouteraient. Sur une tablette
     * couchée de 1280dp, edge + wide_gutter de chaque côté ne laissaient que
     * 32dp, et l'accueil s'affichait vide.
     */
    @Test
    fun `edge_inner complete l air jusqu a edge`() {
        listOf("values", "values-w360dp", "values-w600dp", "values-w840dp", "values-w1200dp").forEach { p ->
            val bord = dimen(p, "edge")!!
            val air = dimen(p, "wide_gutter")!!
            val dedans = dimen(p, "edge_inner")
            assertTrue("« edge_inner » manque dans $p", dedans != null)
            assertEquals("$p : wide_gutter + edge_inner doit valoir edge", bord, air + dedans!!, 0.0)
        }
    }

    @Test
    fun `rien dans l air des grands ecrans ne reprend edge`() {
        (res("layout").listFiles().orEmpty().toList() + couchees()).filter { it.extension == "xml" }.forEach { f ->
            val xml = f.readText()
            var reste = xml
            while (reste.contains("""android:paddingHorizontal="@dimen/wide_gutter"""")) {
                reste = reste.substringAfter("""android:paddingHorizontal="@dimen/wide_gutter"""")
                val bloc = reste.substringBefore("</ScrollView>")
                assertTrue("${f.parentFile.name}/${f.name} : « edge » dans l'air des grands écrans, il faut « edge_inner »",
                    !bloc.contains(""""@dimen/edge""""))
            }
        }
    }

    /**
     * Debout sur un 480x854, il ne reste qu'environ 420dp entre la barre
     * d'état et les onglets : à 236dp, la jauge repoussait « Commencer »
     * sous les onglets. Le défaut reste petit, les paliers hauts grandissent.
     */
    @Test
    fun `la jauge reste petite sur un ecran court`() {
        val court = dimen("values", "gauge_size")!!
        val moyen = dimen("values-h640dp", "gauge_size")!!
        val haut = dimen("values-h720dp", "gauge_size")!!
        assertTrue("défaut $court dp : trop grand pour un 480x854", court <= 180)
        assertTrue(court < moyen && moyen < haut)
    }
}
