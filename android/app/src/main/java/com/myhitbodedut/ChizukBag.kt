package com.myhitbodedut

import android.content.Context

/**
 * La rotation des textes : un sac mélangé par moment, qui persiste.
 *
 * Tirer au hasard à chaque fois redonne le même texte deux fois de suite
 * une fois sur N. On tire donc sans remise : le sac garde les textes qui
 * n'ont pas encore servi, et ne se remplit à nouveau, mélangé, que
 * lorsqu'il est vide. Comme il est écrit dans les préférences, fermer
 * l'application ne relance pas le cycle.
 *
 * La logique est séparée de l'écriture pour être vérifiable sans appareil :
 * [next] est une fonction pure de l'état du sac.
 */
object ChizukBag {

    /** Ce qu'un tirage rend : le texte sorti, et le sac tel qu'il reste. */
    data class Draw(val id: String?, val remaining: List<String>)

    /**
     * Tire le prochain identifiant.
     *
     * @param remaining ce qu'il restait du sac, tel qu'il a été relu.
     * @param pool les identifiants actuellement affichables pour ce moment.
     * @param last le dernier identifiant sorti, pour ne pas le redonner
     *        aussitôt quand un cycle recommence.
     * @param shuffle le mélange, injecté pour que les tests soient
     *        déterministes.
     */
    fun next(
        remaining: List<String>,
        pool: List<String>,
        last: String? = null,
        shuffle: (List<String>) -> List<String> = { it.shuffled() },
    ): Draw {
        if (pool.isEmpty()) return Draw(null, emptyList())

        // Un texte retiré des données, ou devenu non publiable, ne doit pas
        // ressortir : le sac est filtré sur le vivier avant d'être lu.
        val usable = remaining.filter { it in pool }
        val bag = usable.ifEmpty { refill(pool, last, shuffle) }
        return Draw(bag.first(), bag.drop(1))
    }

    /**
     * Remplit un sac vide.
     *
     * Le tirage sans remise laisse passer une seule répétition immédiate :
     * celle du dernier texte d'un cycle qui ouvrirait le suivant. On la
     * retire en renvoyant ce texte en fin de sac.
     */
    private fun refill(
        pool: List<String>,
        last: String?,
        shuffle: (List<String>) -> List<String>,
    ): List<String> {
        val shuffled = shuffle(pool)
        if (pool.size > 1 && shuffled.firstOrNull() == last) {
            return shuffled.drop(1) + shuffled.first()
        }
        return shuffled
    }

    // --- Persistance --------------------------------------------------------

    /**
     * Tire un texte pour ce moment et enregistre le sac restant.
     * Rend null quand rien n'est affichable, ce qui est le cas d'une release
     * tant qu'aucun texte n'est relu.
     */
    fun draw(context: Context, moment: Moment, chizuk: Chizuk, debug: Boolean): ChizukItem? {
        val pool = ChizukRules.forMoment(chizuk, moment, debug).map { it.id }
        val draw = next(
            remaining = Store.chizukBag(context, moment),
            pool = pool,
            last = Store.chizukLast(context, moment),
        )
        Store.setChizukBag(context, moment, draw.remaining, draw.id)
        return draw.id?.let { chizuk.byId(it) }
    }
}
