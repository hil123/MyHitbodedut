package com.myhitbodedut

import androidx.annotation.StringRes

/**
 * Une autorisation que l'application demande, et la raison qu'elle en donne.
 *
 * Elles étaient dispersées : les notifications se demandaient toutes seules
 * en activant la barrière, et se signalaient encore ailleurs dans l'écran
 * des rappels ; les trois autres vivaient au bas de l'écran principal, sous
 * six cartes. Personne ne pouvait dire d'un coup d'œil ce qui manquait.
 *
 * L'ordre est celui de l'utilité décroissante : sans notifications rien ne
 * s'affiche, sans batterie libre le rappel peut arriver en retard, et les
 * deux dernières ne servent qu'à la barrière du matin — elles disparaissent
 * de l'édition légère, qui n'en a pas.
 */
enum class Autorisation(
    val cle: String,
    @StringRes val titre: Int,
    @StringRes val pourquoi: Int,
    /** Elle ne sert qu'à la barrière, absente de l'édition légère. */
    val barriere: Boolean
) {
    NOTIFICATIONS(
        "notifications",
        R.string.permission_notifications,
        R.string.permission_notifications_why,
        false
    ),
    BATTERIE(
        "batterie",
        R.string.permission_battery,
        R.string.permission_battery_why,
        false
    ),
    OVERLAY(
        "overlay",
        R.string.permission_overlay,
        R.string.permission_overlay_why,
        true
    ),
    USAGE(
        "usage",
        R.string.permission_usage,
        R.string.permission_usage_why,
        true
    );
}

/** Ce qui est demandé, et ce qui manque. Sans appareil, pour être vérifiable. */
object PermissionRules {

    /** Les autorisations qu'il y a lieu de demander dans cette édition. */
    fun demandees(avecBarriere: Boolean): List<Autorisation> =
        Autorisation.values().filter { avecBarriere || !it.barriere }

    /**
     * Celles qui manquent, dans l'ordre. L'état est passé plutôt que lu :
     * il demande un contexte Android, la règle non.
     */
    fun manquantes(avecBarriere: Boolean, accordee: (Autorisation) -> Boolean): List<Autorisation> =
        demandees(avecBarriere).filterNot(accordee)
}
