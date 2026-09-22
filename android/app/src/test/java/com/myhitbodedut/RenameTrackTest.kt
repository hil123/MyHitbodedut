package com.myhitbodedut

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Renommer un morceau depuis l'application.
 *
 * Le code savait le faire depuis le début — mais seulement sur appui long, et
 * le propriétaire a demandé la fonction alors qu'elle existait déjà : une
 * capacité qu'on ne trouve pas n'existe pas. Ces tests portent donc sur ce
 * qui la rend trouvable, pas seulement sur ce qui la rend possible.
 */
class RenameTrackTest {

    private val layout = File("src/main/res/layout/item_track.xml").readText()
    private val activity = File("src/main/java/com/myhitbodedut/MusicActivity.kt").readText()
    private val library = File("src/main/java/com/myhitbodedut/MusicLibrary.kt").readText()

    @Test
    fun `chaque morceau porte un bouton de renommage visible`() {
        assertTrue(
            "la ligne d'un morceau n'a plus de bouton de renommage : " +
                "il ne resterait que l'appui long, que personne ne devine",
            layout.contains("@+id/track_rename"),
        )
        assertTrue(
            "le bouton n'a pas de description pour l'accessibilité",
            layout.contains("android:contentDescription=\"@string/music_rename\""),
        )
    }

    @Test
    fun `le bouton et l appui long ouvrent tous deux le renommage`() {
        assertTrue(
            "le bouton de renommage n'est relié à rien",
            activity.contains("holder.rename.setOnClickListener { askRename(track) }"),
        )
        assertTrue(
            "l'appui long a disparu : il reste le raccourci de ceux qui le connaissent",
            activity.contains("setOnLongClickListener { askRename(track); true }"),
        )
    }

    @Test
    fun `le titre livre n est pas recopie dans le champ`() {
        // Pre-remplir avec « Niggoun 07 — titre à préciser » obligeait à
        // l'effacer d'abord, et enregistrer sans rien changer le figeait comme
        // s'il avait été choisi. Il sert d'indication, pas de contenu.
        assertTrue("le champ recopie le titre livré", !activity.contains("setText(track.title)"))
        assertTrue(
            "le champ ne reprend pas le nom déjà donné",
            activity.contains("setText(custom.orEmpty())"),
        )
        assertTrue("le titre livré ne s'affiche plus en indication", activity.contains("hint = track.title"))
    }

    @Test
    fun `on peut revenir au titre d origine`() {
        assertTrue(
            "aucun moyen d'annuler un renommage d'un morceau livré",
            activity.contains("R.string.music_rename_reset"),
        )
        assertTrue(
            "un nom vide doit effacer l'entrée, sinon le retour en arrière ne fait rien",
            library.contains("if (title.isBlank()) names.remove(id) else names.put(id, title)"),
        )
    }

    @Test
    fun `le renommage vaut aussi pour les morceaux livres`() {
        assertTrue(
            "le nom donné doit primer sur le titre livré",
            library.contains("nameOf(context, b.id) ?: context.getString(b.titleRes)"),
        )
    }

    @Test
    fun `l aide de l ecran dit ou est le renommage, dans les trois langues`() {
        listOf(
            "values" to "crayon",
            "values-en" to "pencil",
            "values-iw" to "העיפרון",
        ).forEach { (dossier, mot) ->
            val texte = File("src/main/res/$dossier/strings.xml").readText()
            val ligne = Regex("""<string name="music_library_hint">(.*?)</string>""")
                .find(texte)?.groupValues?.get(1)
            assertTrue("music_library_hint absent de $dossier", ligne != null)
            assertTrue(
                "l'aide de $dossier ne désigne pas le bouton : « $ligne »",
                ligne!!.contains(mot),
            )
        }
    }
}
