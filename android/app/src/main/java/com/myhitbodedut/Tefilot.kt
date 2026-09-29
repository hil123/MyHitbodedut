package com.myhitbodedut

import androidx.annotation.StringRes

/**
 * Les dix sujets de l'écran Tfilot, dans l'ordre où ils ont été demandés.
 *
 * L'ordre n'est pas alphabétique et ne doit pas le devenir : il commence par
 * l'hitbodedout, qui est le sujet de l'application, et finit par les deux
 * demandes qu'on vient chercher un mauvais jour — le conjoint et la guérison.
 */
enum class SujetTefila(val cle: String, @StringRes val titre: Int) {
    HITBODEDUT("hitbodedut", R.string.tefila_hitbodedut),
    SIMCHA("simcha", R.string.tefila_simcha),
    UMAN("uman", R.string.tefila_uman),
    KDUSHA("kdusha", R.string.tefila_kdusha),
    ZERA("zera", R.string.tefila_zera),
    PARNASSA("parnassa", R.string.tefila_parnassa),
    BRIT("brit", R.string.tefila_brit),
    GEULA("geula", R.string.tefila_geula),
    ZIVUG("zivug", R.string.tefila_zivug),
    REFUA("refua", R.string.tefila_refua),
}

/**
 * Ce que l'écran Tfilot affiche, calculé sans Android pour être vérifiable.
 *
 * L'écran ne connaît pas les prières : il lit la sous-section de la
 * bibliothèque. Ajouter une prière au fichier suffit à la faire paraître,
 * pourvu qu'elle porte son sujet.
 */
object TefilotRules {

    const val SOUS_SECTION = "tefilot-lt"

    /** Une ligne de l'écran : un sujet, ou une prière sous ce sujet. */
    sealed interface Ligne {
        data class Titre(val sujet: SujetTefila, val nombre: Int) : Ligne
        data class Priere(val sujet: SujetTefila, val entree: SourceEntry) : Ligne
    }

    /** Les prières de la sous-section, articles exclus. */
    fun prieres(library: Library): List<SourceEntry> =
        library.subSection(SOUS_SECTION)?.entrees.orEmpty().filterIsInstance<SourceEntry>()

    fun parSujet(library: Library, sujet: SujetTefila): List<SourceEntry> =
        prieres(library).filter { it.sujet == sujet.cle }

    /**
     * Les lignes, sujet par sujet, dans l'ordre de l'énumération.
     *
     * Un sujet sans prière ne produit aucune ligne : mieux vaut un écran plus
     * court qu'un titre qui promet un texte absent.
     */
    fun lignes(library: Library): List<Ligne> =
        SujetTefila.values().flatMap { sujet ->
            val prieres = parSujet(library, sujet)
            if (prieres.isEmpty()) {
                emptyList()
            } else {
                listOf(Ligne.Titre(sujet, prieres.size)) + prieres.map { Ligne.Priere(sujet, it) }
            }
        }

    /**
     * La leçon annoncée par cette prière porte-t-elle sur le sujet sous lequel
     * on la range ?
     *
     * Rabbi Natan tire de chaque leçon une prière très large : la demande de
     * guérison est dans une prière sur les danses, celle du conjoint dans une
     * prière sur 'Hanoucca. Le fichier le dit ; l'écran le dira aussi, plutôt
     * que de laisser croire que chaque leçon traite du sujet.
     */
    fun prevenir(entree: SourceEntry): Boolean = entree.leconSurLeSujet == "non"
}
