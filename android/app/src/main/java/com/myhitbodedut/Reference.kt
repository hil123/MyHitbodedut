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
 * L'anglais reprend les graphies des chaînes anglaises (Likutei, Sihot,
 * Hayei, Shivhei) ; l'hébreu, les titres usuels avec leurs guerchayim.
 */
object Reference {

    private data class Livre(val fr: String, val en: String, val he: String)

    /** Du plus long au plus court : « Likoutei Moharan II » avant « … I ». */
    private val LIVRES = listOf(
        Livre("Likoutei Etsot, Hit'hazkout ", "Likutei Etsot, Strengthening ", "ליקוטי עצות, התחזקות "),
        Livre("Rambam, Hilkhot Tefila ", "Rambam, Hilkhot Tefillah ", "רמב״ם, הלכות תפילה "),
        Livre("Likoutei Moharan II, ", "Likutei Moharan II, ", "ליקוטי מוהר״ן תניינא, "),
        Livre("Likoutei Moharan I, ", "Likutei Moharan I, ", "ליקוטי מוהר״ן, "),
        Livre("Likoutei Tefilot II, ", "Likutei Tefilot II, ", "ליקוטי תפילות ב, "),
        Livre("Likoutei Tefilot I, ", "Likutei Tefilot I, ", "ליקוטי תפילות א, "),
        Livre("Si'hot HaRan ", "Sihot HaRan ", "שיחות הר״ן "),
        Livre("'Hayé Moharan ", "Hayei Moharan ", "חיי מוהר״ן "),
        Livre("Chiv'hei HaRan ", "Shivhei HaRan ", "שבחי הר״ן "),
        Livre("Tehilim ", "Tehillim ", "תהילים "),
        Livre("Michlé ", "Mishlei ", "משלי "),
        Livre("Eikha ", "Eikha ", "איכה "),
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
        texte = texte.replace(" ; ", "; ")
        // En hébreu, les numéros en lettres (choix du propriétaire, 28/09) :
        // « משלי 24:16 » devient « משלי כ״ד, ט״ז ».
        if (cible == "he") {
            texte = Regex("""(\d+):(\d+)""").replace(texte) {
                "${lettres(it.groupValues[1].toInt())}, ${lettres(it.groupValues[2].toInt())}"
            }
            texte = Regex("""\d+""").replace(texte) { lettres(it.value.toInt()) }
        }
        return texte
    }

    /**
     * Un nombre en lettres hébraïques, de 1 à 999 : כ״ה, תל״ז, ז׳.
     * 15 et 16 s'écrivent ט״ו et ט״ז, pour ne pas former le Nom.
     */
    fun lettres(n: Int): String {
        if (n <= 0 || n >= 1000) return n.toString()
        val b = StringBuilder()
        var reste = n
        while (reste >= 400) { b.append('ת'); reste -= 400 }
        if (reste >= 100) { b.append(CENTAINES[reste / 100]); reste %= 100 }
        when (reste) {
            15 -> b.append("טו")
            16 -> b.append("טז")
            else -> {
                if (reste >= 10) b.append(DIZAINES[reste / 10])
                if (reste % 10 > 0) b.append(UNITES[reste % 10])
            }
        }
        return if (b.length == 1) "$b׳" else b.insert(b.length - 1, '״').toString()
    }

    private const val UNITES = " אבגדהוזחט"
    private const val DIZAINES = " יכלמנסעפצ"
    private const val CENTAINES = " קרש"

    fun localise(context: Context, ref: String): String =
        localise(ref, ChizukCard.language(context))

    /** Vrai si la référence est déjà dans une forme que l'anglais et l'hébreu savent rendre. */
    fun connue(ref: String): Boolean = LIVRES.any { ref.startsWith(it.fr) }
}
