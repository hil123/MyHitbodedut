package com.myhitbodedut

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * L'ecran principal, apres son degraissage.
 *
 * Six cartes pleines y prenaient environ 970dp de haut : il fallait
 * traverser trois ecrans avant d'avoir tout vu, alors que la seule
 * question a l'ouverture est « je commence maintenant ». Elles sont
 * devenues six lignes dans une seule carte.
 *
 * Le risque d'un tel degraissage n'est pas qu'il soit laid, c'est qu'il
 * emporte quelque chose au passage — un etat qui ne s'affiche plus, une
 * section devenue inatteignable. Ces tests tiennent l'inventaire.
 */
class HomeTest {

    private val layout = File("src/main/res/layout/activity_main.xml").readText()
    private val code = File("src/main/java/com/myhitbodedut/MainActivity.kt").readText()

    /** Toutes les sections restent atteignables depuis l'accueil. */
    @Test
    fun `aucune section n est devenue inatteignable`() {
        listOf("tikkun_button", "library_button", "reminders_button", "music_button",
               "tips_button", "settings_button", "proofread_button").forEach {
            assertTrue("« $it » a disparu de l'écran principal", layout.contains("@+id/$it\""))
            assertTrue("« $it » n'est plus branché", Regex("""R\.id\.$it\b""").containsMatchIn(code))
        }
    }

    /**
     * Les etats que les cartes affichaient doivent survivre au
     * degraissage : c'est ce qu'on perd le plus facilement en compactant.
     */
    @Test
    fun `les etats des sections sont toujours affiches`() {
        listOf("tikkun_state", "reminders_state", "music_state", "settings_state").forEach {
            assertTrue(
                "l'état « $it » n'est plus dans la mise en page",
                layout.contains("@+id/$it\""),
            )
            assertTrue(
                "l'état « $it » n'est plus rempli",
                Regex("""R\.id\.$it\b""").containsMatchIn(code),
            )
        }
        assertTrue(
            "l'interrupteur de musique doit rester sur l'accueil",
            layout.contains("@+id/music_switch_main\""),
        )
    }

    /**
     * Le degraissage ne vaut que par ce qu'il retire. Deux cartes : celle
     * de la pratique, et celle des sections. Si une troisieme reapparait,
     * l'ecran redevient un menu.
     */
    @Test
    fun `l accueil ne porte plus que deux cartes`() {
        val cartes = Regex("""style="@style/Card"""").findAll(layout).count()
        assertTrue("l'écran principal porte $cartes cartes, il n'en faut que 2", cartes == 2)
        val lignes = Regex("""style="@style/SectionRow"""").findAll(layout).count()
        assertTrue("il manque des entrées de section : $lignes", lignes >= 6)
    }

    /** L'action doit rester la plus grosse chose de l'ecran. */
    @Test
    fun `commencer une seance reste l action principale`() {
        val bloc = layout.substringAfter("""@+id/start_session""").substringBefore("/>")
        assertTrue("le bouton doit occuper la largeur", bloc.contains("match_parent"))
        val taille = Regex("""android:textSize="(\d+)sp"""").find(bloc)!!.groupValues[1].toInt()
        assertTrue("le bouton principal est trop discret ($taille sp)", taille >= 16)
        // Aucune entrée de section ne doit le concurrencer visuellement.
        assertTrue(
            "les entrées de section ne doivent pas être des boutons pleins",
            !layout.contains("""android:id="@+id/tikkun_button"
            style="@style/Widget"""),
        )
    }

    /**
     * En hebreu la liste se lit de droite a gauche : un chevron qui ne se
     * retourne pas pointe vers la sortie de l'ecran.
     */
    @Test
    fun `le chevron se retourne en hebreu`() {
        val chevron = File("src/main/res/drawable/ic_chevron.xml").readText()
        assertTrue(
            "ic_chevron doit être autoMirrored",
            chevron.contains("""android:autoMirrored="true""""),
        )
    }
}
