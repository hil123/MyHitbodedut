package com.myhitbodedut

import android.content.Context

/**
 * Une référence de source (« Likoutei Moharan II, 25 ») dans la langue de
 * l'interface.
 *
 * Les données portent la graphie française. Affichée telle quelle, elle
 * restait française en anglais et latine en hébreu, contre la règle
 * « en hébreu, rien que de l'hébreu » (audit des traductions, 28/09). Les
 * livres cités sont peu nombreux : on remplace le nom du livre, les numéros
 * restent.
 *
 * L'anglais reprend les graphies des chaînes anglaises (Likutei, Sichot,
 * Chayei, Shivchei) ; l'hébreu, les titres usuels avec leurs guerchayim.
 */
object Reference {

    private data class Livre(val fr: String, val en: String, val he: String)

    /** Du plus long au plus court : « Likoutei Moharan II » avant « … I ». */
    private val LIVRES = listOf(
        Livre("Likoutei Etsot, Hit'hazkout ", "Likutei Etzot, Strengthening ", "ליקוטי עצות, התחזקות "),
        Livre("Rambam, Hilkhot Tefila ", "Rambam, Hilchot Tefillah ", "רמב״ם, הלכות תפילה "),
        Livre("Likoutei Moharan II, ", "Likutei Moharan II, ", "ליקוטי מוהר״ן תניינא, "),
        Livre("Likoutei Moharan I, ", "Likutei Moharan I, ", "ליקוטי מוהר״ן, "),
        Livre("Likoutei Tefilot II, ", "Likutei Tefilot II, ", "ליקוטי תפילות ב, "),
        Livre("Likoutei Tefilot I, ", "Likutei Tefilot I, ", "ליקוטי תפילות א, "),
        Livre("Si'hot HaRan ", "Sichot HaRan ", "שיחות הר״ן "),
        Livre("'Hayé Moharan ", "Chayei Moharan ", "חיי מוהר״ן "),
        Livre("Chiv'hei HaRan ", "Shivchei HaRan ", "שבחי הר״ן "),
        Livre("Tehilim ", "Tehillim ", "תהלים "),
        Livre("Michlé ", "Mishlei ", "משלי "),
        Livre("Eikha ", "Eichah ", "איכה "),
    )

    /** La langue est passée plutôt que lue, pour que la règle se teste sans appareil. */
    fun localise(ref: String, langue: String): String {
        val cible = RegleLangue.normalise(langue)
        if (cible != "en" && cible != "he") return ref
        var texte = ref
        LIVRES.forEach { livre ->
            if (texte.startsWith(livre.fr)) {
                texte = (if (cible == "en") livre.en else livre.he) + texte.removePrefix(livre.fr)
            }
        }
        // L'espace avant le point-virgule est une typographie française.
        return texte.replace(" ; ", "; ")
    }

    fun localise(context: Context, ref: String): String =
        localise(ref, ChizukCard.language(context))

    /** Vrai si la référence est déjà dans une forme que l'anglais et l'hébreu savent rendre. */
    fun connue(ref: String): Boolean = LIVRES.any { ref.startsWith(it.fr) }
}
