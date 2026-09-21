package com.myhitbodedut

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes

/** Un temps de la hitbodedout guidée. */
data class Mouvement(
    val cle: String,
    val he: String,
    @StringRes val titre: Int,
    @StringRes val invite: Int,
    @DrawableRes val illustration: Int
)

/**
 * La hitbodedout guidée.
 *
 * Rester seul une heure devant un silence est une épreuve, et beaucoup
 * s'arrêtent non par manque de volonté mais faute de savoir par quoi
 * continuer. Quatre temps y répondent, et ils ne sont pas inventés : ce
 * sont ceux que Likoutei Moharan II, 25 énumère, dans l'ordre où ce
 * chapitre les nomme.
 *
 *   - « לְפָרֵשׁ שִׂיחָתוֹ » : dire ce qu'on a sur le cœur, dans sa propre
 *     langue, « כִּי בִּלְשׁוֹן־הַקֹּדֶשׁ קָשֶׁה לוֹ לְפָרֵשׁ כָּל שִׂיחָתוֹ ».
 *   - « חֲרָטָה וּתְשׁוּבָה עַל הֶעָבָר ».
 *   - « בַּקָּשַׁת תַחֲנוּנִים לִזְכּוֹת לְהִתְקָרֵב אֵלָיו יִתְבָּרַךְ ».
 *   - « לַעֲשׂוֹת מֵהַתּוֹרָה תְּפִלָּה », reprendre un enseignement entendu et
 *     en faire une demande.
 *
 * Le même chapitre répond déjà à celui qui n'arrive pas à parler —
 * « וַאֲפִלּוּ אִם לִפְעָמִים נִסְתַּתְּמִין דְּבָרָיו » — et c'est ce que donne
 * depuis toujours le bouton « je n'arrive pas à parler » de l'écran de
 * séance. Les quatre temps ne le remplacent pas.
 *
 * Rien ici n'oblige : le guide est éteint par défaut, on passe d'un temps
 * à l'autre quand on veut, et on peut n'en faire aucun.
 */
object Hitbodedut {

    /** Le chapitre d'où viennent les quatre temps. */
    const val SOURCE = "לקוטי מוהר\"ן תנינא, סימן כ\"ה"

    val MOUVEMENTS: List<Mouvement> = listOf(
        Mouvement(
            "sihah",
            "לְפָרֵשׁ שִׂיחָתוֹ",
            R.string.hitbodedut_sihah,
            R.string.hitbodedut_sihah_invite,
            R.drawable.char_open_arms
        ),
        Mouvement(
            "harata",
            "חֲרָטָה וּתְשׁוּבָה עַל הֶעָבָר",
            R.string.hitbodedut_harata,
            R.string.hitbodedut_harata_invite,
            R.drawable.char_sitting_low
        ),
        Mouvement(
            "bakasha",
            "בַּקָּשַׁת תַחֲנוּנִים",
            R.string.hitbodedut_bakasha,
            R.string.hitbodedut_bakasha_invite,
            R.drawable.char_praying
        ),
        Mouvement(
            "torah",
            "לַעֲשׂוֹת מֵהַתּוֹרָה תְּפִלָּה",
            R.string.hitbodedut_torah,
            R.string.hitbodedut_torah_invite,
            R.drawable.char_torah
        )
    )

    val DERNIER: Int = MOUVEMENTS.lastIndex

    /** Le temps à cet indice, ramené dans les bornes plutôt que de rompre. */
    fun mouvement(index: Int): Mouvement = MOUVEMENTS[index.coerceIn(0, DERNIER)]

    fun suivant(index: Int): Int = (index + 1).coerceIn(0, DERNIER)

    fun precedent(index: Int): Int = (index - 1).coerceIn(0, DERNIER)

    fun estPremier(index: Int): Boolean = index <= 0

    fun estDernier(index: Int): Boolean = index >= DERNIER
}
