package com.myhitbodedut

import androidx.annotation.StringRes

/**
 * Le sujet d'une séance : de quoi on vient parler.
 *
 * **Il est facultatif, et c'est une décision, pas un oubli.** Obliger à
 * choisir un sujet avant de commencer ajouterait un obstacle là où il faut
 * en retirer, et la hitbodedout n'a pas besoin d'ordre du jour : on vient
 * souvent sans savoir. « Aucun » se représente par l'absence de valeur —
 * null — et non par un membre de cette liste.
 *
 * AUTRE n'est pas « aucun » : c'est un sujet choisi, qui n'est simplement
 * pas dans la liste.
 *
 * Le sujet est inscrit avec la journée dès aujourd'hui, même si rien ne
 * l'affiche encore en retour. Une donnée qu'on n'enregistre pas est une
 * donnée qu'on n'aura jamais : le jour où « mes sujets les plus fréquents »
 * existera, il aura un passé à montrer au lieu de repartir de zéro.
 */
enum class Theme(val cle: String, @StringRes val titre: Int) {
    EMOTIONS("emotions", R.string.theme_emotions),
    GRATITUDE("gratitude", R.string.theme_gratitude),
    DECISIONS("decisions", R.string.theme_decisions),
    FAMILLE("famille", R.string.theme_famille),
    ZIVOUG("zivoug", R.string.theme_zivoug),
    PARNASSA("parnassa", R.string.theme_parnassa),
    AVODA("avoda", R.string.theme_avoda),
    DIFFICULTES("difficultes", R.string.theme_difficultes),
    CROISSANCE("croissance", R.string.theme_croissance),
    AUTRE("autre", R.string.theme_autre);
}

/** Ce qu'on peut dire des sujets, sans appareil. */
object ThemeRules {

    /**
     * Retrouve un sujet par sa clé. Une clé inconnue rend null plutôt que
     * de rompre : un journal exporté d'une version plus récente peut
     * contenir un sujet que celle-ci ne connaît pas encore.
     */
    fun parse(cle: String?): Theme? =
        if (cle.isNullOrBlank()) null else Theme.values().firstOrNull { it.cle == cle }

    /**
     * Les sujets les plus fréquents, du plus au moins. À égalité, l'ordre
     * de la liste tranche, pour que deux appels rendent la même chose.
     */
    fun frequents(cles: List<String>, max: Int = 4): List<Pair<Theme, Int>> {
        val comptes = linkedMapOf<Theme, Int>()
        Theme.values().forEach { comptes[it] = 0 }
        cles.mapNotNull { parse(it) }.forEach { comptes[it] = comptes.getValue(it) + 1 }
        return comptes.entries
            .filter { it.value > 0 }
            .sortedByDescending { it.value }
            .take(max)
            .map { it.key to it.value }
    }
}
