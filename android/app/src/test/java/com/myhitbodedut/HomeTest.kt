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
               "tips_button", "proofread_button").forEach {
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
        listOf("tikkun_state", "reminders_state", "music_state").forEach {
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
     * La carte de depart rassemble ce avec quoi on entre dans la seance.
     * Chaque ligne doit exister ET etre branchee : une ligne posee que
     * personne ne remplit reste vide, et une ligne remplie que personne
     * n'ecoute ne se change pas.
     */
    @Test
    fun `la carte de depart est complete et branchee`() {
        listOf("start_duration", "start_theme", "start_music", "start_focus").forEach {
            assertTrue("la ligne « $it » manque", layout.contains("@+id/$it\""))
            assertTrue("la ligne « $it » n'est pas branchée",
                Regex("""R\.id\.$it\b""").containsMatchIn(code))
            assertTrue("la valeur de « $it » n'est pas affichée",
                layout.contains("@+id/${it}_value\""))
            // Limite de mot obligatoire : « start_theme_value » se retrouve
            // dans « start_theme_valueX », et le test laisserait passer un
            // identifiant renomme.
            assertTrue("la valeur de « $it » n'est pas remplie",
                Regex("""R\.id\.${it}_value\b""").containsMatchIn(code))
        }
    }

    /**
     * La barriere n'existe pas en edition legere : la ligne doit etre
     * retiree, pas laissee inoperante.
     */
    @Test
    fun `la ligne de concentration depend de l edition`() {
        val bloc = code.substringAfter("private fun renderStartCard")
            .substringBefore("\n    }")
        assertTrue(
            "start_focus doit dépendre de BuildConfig.HAS_GATE",
            Regex("""start_focus[\s\S]{0,300}?BuildConfig\.HAS_GATE""").containsMatchIn(bloc),
        )
    }

    /** Sans prenom, l'accueil se tait plutot que de saluer un blanc. */
    @Test
    fun `la salutation se tait sans prenom`() {
        assertTrue("la salutation manque", layout.contains("@+id/hero_greeting\""))
        val bloc = code.substringAfter("private fun renderGreeting")
            .substringBefore("\n    }")
        assertTrue("le prénom doit être lu", bloc.contains("Store.firstName(this)"))
        assertTrue(
            "la salutation doit disparaître quand le prénom est vide",
            bloc.contains("isBlank()") && bloc.contains("View.GONE"),
        )
        assertTrue(
            "la salutation part masquée",
            Regex("""hero_greeting"[\s\S]{0,400}?android:visibility="gone"""")
                .containsMatchIn(layout),
        )
    }

    /**
     * L'accueil A + D (28/09) : le hassid au creux de la jauge de la
     * semaine, et quatre onglets. Journal et Reglages ouvrent leurs ecrans ;
     * Aujourd'hui et Textes changent de panneau.
     */
    @Test
    fun `la jauge entoure le hassid et les onglets menent partout`() {
        val jauge = layout.substringAfter("@+id/week_gauge\"")
        assertTrue("le hassid doit être dans la jauge", jauge.substringBefore("week_gauge_label").contains("@+id/hero_character\""))
        assertTrue(code.contains("JaugeSemaine.progression(Store.currentWeek(this))"))
        val menu = File("src/main/res/menu/nav_main.xml").readText()
        listOf("nav_today", "nav_texts", "nav_zmanim", "nav_journal", "nav_settings").forEach {
            assertTrue("onglet « $it » absent", menu.contains("@+id/$it\""))
            assertTrue("onglet « $it » non branché", Regex("""R\.id\.$it\b""").containsMatchIn(code))
        }
        assertTrue(code.contains("JournalActivity::class.java"))
        // Les réglages : l'onglet seul (la ligne en double est retirée le 29/09),
        // Plus de pastille sur cet onglet quand des autorisations manquent
        // (demande du 10/10) : elles se lisent dans les Réglages.
        assertTrue(!layout.contains("@+id/settings_button\""))
        assertTrue(!code.contains("getOrCreateBadge("))
        assertTrue(code.contains("removeBadge(R.id.nav_settings)"))
        assertTrue("l'onglet doit survivre à une rotation", code.contains("ETAT_ONGLET"))
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
