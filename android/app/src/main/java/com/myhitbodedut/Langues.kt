package com.myhitbodedut

/**
 * Les langues que l'application parle, et celle qu'on a choisie.
 *
 * Jusqu'ici la langue était celle du téléphone, sans recours : un appareil
 * réglé en anglais ne pouvait pas afficher l'hébreu, alors que les trois
 * traductions sont livrées. Le choix est maintenant explicite.
 *
 * Les règles sont ici, sans Android, pour qu'elles se vérifient sans
 * appareil ; SettingsActivity ne fait que les appliquer.
 */
object Langues {

    /**
     * L'étiquette est celle du dossier de ressources, pas celle du BCP-47
     * moderne : l'hébreu est livré dans `values-iw`, l'ancien code qu'Android
     * emploie toujours. Demander « he » marcherait aussi — la plateforme
     * traite les deux comme une seule langue — mais viser exactement le
     * dossier livré enlève une hypothèse.
     */
    data class Langue(val etiquette: String, val nom: String)

    /** Dans l'ordre demandé : עברית, English, Français. */
    val TOUTES = listOf(
        Langue("iw", "עברית"),
        Langue("en", "English"),
        Langue("fr", "Français"),
    )

    /**
     * Le français est la langue de `values/`. Un téléphone dont aucune langue n'est livrée
     * passe pourtant en anglais : LangueApp.repli, au lancement (29/10).
     */
    const val DEFAUT = "fr"

    /** Ramène une étiquette de langue, quelle que soit sa forme, à l'une des nôtres. */
    fun normalise(etiquette: String?): String {
        val brute = etiquette.orEmpty().substringBefore('-').substringBefore('_').lowercase()
        // « he » est le code actuel de l'hébreu, « iw » celui qu'Android rend
        // encore. Les deux désignent la même langue et le même dossier.
        val code = if (brute == "he") "iw" else brute
        return TOUTES.firstOrNull { it.etiquette == code }?.etiquette ?: DEFAUT
    }

    fun nomDe(etiquette: String?): String {
        val code = normalise(etiquette)
        return TOUTES.first { it.etiquette == code }.nom
    }

    /** L'index de la langue courante dans TOUTES, pour cocher la bonne ligne. */
    fun indexDe(etiquette: String?): Int {
        val code = normalise(etiquette)
        return TOUTES.indexOfFirst { it.etiquette == code }
    }
}
