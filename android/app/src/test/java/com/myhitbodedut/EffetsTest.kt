package com.myhitbodedut

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Les effets (19/10) : le passage d'écran dans le thème, et les écrans neufs qui les portent. */
class EffetsTest {

    private fun lire(p: String) = File("src/main/$p").readText()

    @Test
    fun `le theme anime le passage d un ecran a l autre`() {
        val themes = lire("res/values/themes.xml")
        assertTrue(themes.contains("<item name=\"android:windowAnimationStyle\">@style/Effets.Fenetres</item>"))
        listOf("effet_ouvre_entre", "effet_ferme_sort", "effet_reste").forEach {
            assertTrue(it, File("src/main/res/anim/$it.xml").isFile)
            assertTrue(it, themes.contains("@anim/$it"))
        }
        assertTrue(File("src/main/res/animator/presse.xml").isFile)
    }

    @Test
    fun `les ecrans neufs portent leurs effets, et rien ne bouge si les animations sont coupees`() {
        mapOf(
            "ParcoursActivity" to listOf("Effets.enfants(", "Effets.compte(", "Effets.remplir"),
            "ChizukActivity" to listOf("Effets.enfants(", "Effets.fondu("),
            "SessionActivity" to listOf("Effets.fondu("),
            "MainActivity" to listOf("Effets.enfants("),
        ).forEach { (f, appels) ->
            val code = lire("java/com/myhitbodedut/$f.kt")
            appels.forEach { assertTrue("$f : $it", code.contains(it)) }
        }
        val effets = lire("java/com/myhitbodedut/Effets.kt")
        assertTrue(effets.contains("ValueAnimator.areAnimatorsEnabled()"))
        listOf("fun cascade", "fun fondu", "fun compte", "fun remplir").forEach {
            val corps = effets.substringAfter(it).substringBefore("\n    }")
            assertTrue("$it doit se taire sans animations", corps.contains("!actifs"))
        }
    }
}
