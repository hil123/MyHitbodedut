package com.myhitbodedut

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes

/** Un temps de la hitbodedout guidée. */
data class Mouvement(
    val cle: String,
    val he: String,
    @StringRes val titre: Int,
    @StringRes val invite: Int,
    @DrawableRes val illustration: Int,
    /** D'où vient ce temps. Ils ne viennent pas tous du même endroit. */
    val source: String
)

/**
 * La hitbodedout guidée.
 *
 * Rester seul une heure devant un silence est une épreuve, et beaucoup
 * s'arrêtent non par manque de volonté mais faute de savoir par quoi
 * continuer. Cinq temps y répondent, et ils ne sont pas inventés.
 *
 * Les quatre premiers sont ceux que Likoutei Moharan II, 25 énumère,
 * dans l'ordre où ce chapitre les nomme.
 *
 *   - « לְפָרֵשׁ שִׂיחָתוֹ » : dire ce qu'on a sur le cœur, dans sa propre
 *     langue, « כִּי בִּלְשׁוֹן־הַקֹּדֶשׁ קָשֶׁה לוֹ לְפָרֵשׁ כָּל שִׂיחָתוֹ ».
 *   - « חֲרָטָה וּתְשׁוּבָה עַל הֶעָבָר ».
 *   - « בַּקָּשַׁת תַחֲנוּנִים לִזְכּוֹת לְהִתְקָרֵב אֵלָיו יִתְבָּרַךְ ».
 *   - « לַעֲשׂוֹת מֵהַתּוֹרָה תְּפִלָּה », reprendre un enseignement entendu et
 *     en faire une demande.
 *
 * L'action de grâce, elle, n'est pas dans ce chapitre, et elle ne se
 * réclame donc pas de lui. Elle vient de la règle générale de la prière
 * chez le Rambam, Hilkhot Tefila I, 2, qui la place précisément là où
 * elle se trouve ici, après la demande : « וְאַחַר כָּךְ נוֹתֵן שֶׁבַח
 * וְהוֹדָיָה לַה' עַל הַטּוֹבָה שֶׁהִשְׁפִּיעַ לוֹ, כָּל אֶחָד לְפִי כֹּחוֹ ». La même
 * halakha décrit d'ailleurs déjà celui qui n'a pas les mots —
 * « וְאִם הָיָה עֲרֵל שְׂפָתַיִם מְדַבֵּר כְּפִי יְכָלְתּוֹ ». Chaque temps porte donc
 * sa propre source, affichée avec lui : emprunter au chapitre une
 * caution qu'il ne donne pas aurait été le manquement facile.
 *
 * Le même chapitre répond déjà à celui qui n'arrive pas à parler —
 * « וַאֲפִלּוּ אִם לִפְעָמִים נִסְתַּתְּמִין דְּבָרָיו » — et c'est ce que donne
 * depuis toujours le bouton « je n'arrive pas à parler » de l'écran de
 * séance. Les cinq temps ne le remplacent pas.
 *
 * Rien ici n'oblige : le guide est éteint par défaut, on passe d'un temps
 * à l'autre quand on veut, et on peut n'en faire aucun.
 */
object Hitbodedut {

    /** Le chapitre d'où viennent les quatre premiers temps. */
    const val SOURCE_LIKOUTEI = "לקוטי מוהר\"ן תנינא, סימן כ\"ה"

    /** Et celui d'où vient l'action de grâce, qui n'est pas dans ce chapitre. */
    const val SOURCE_RAMBAM = "רמב\"ם, הלכות תפילה א, ב"

    val MOUVEMENTS: List<Mouvement> = listOf(
        Mouvement(
            "sihah",
            "לְפָרֵשׁ שִׂיחָתוֹ",
            R.string.hitbodedut_sihah,
            R.string.hitbodedut_sihah_invite,
            R.drawable.char_open_arms,
            SOURCE_LIKOUTEI
        ),
        Mouvement(
            "harata",
            "חֲרָטָה וּתְשׁוּבָה עַל הֶעָבָר",
            R.string.hitbodedut_harata,
            R.string.hitbodedut_harata_invite,
            R.drawable.char_sitting_low,
            SOURCE_LIKOUTEI
        ),
        Mouvement(
            "bakasha",
            "בַּקָּשַׁת תַחֲנוּנִים",
            R.string.hitbodedut_bakasha,
            R.string.hitbodedut_bakasha_invite,
            R.drawable.char_praying,
            SOURCE_LIKOUTEI
        ),
        Mouvement(
            "torah",
            "לַעֲשׂוֹת מֵהַתּוֹרָה תְּפִלָּה",
            R.string.hitbodedut_torah,
            R.string.hitbodedut_torah_invite,
            R.drawable.char_torah,
            SOURCE_LIKOUTEI
        ),
        Mouvement(
            "hodaa",
            "נוֹתֵן שֶׁבַח וְהוֹדָיָה",
            R.string.hitbodedut_hodaa,
            R.string.hitbodedut_hodaa_invite,
            R.drawable.char_praise,
            SOURCE_RAMBAM
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
