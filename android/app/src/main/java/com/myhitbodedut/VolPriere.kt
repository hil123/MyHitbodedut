package com.myhitbodedut

import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset

/**
 * La prière du moment en vol (28/10) : les horaires de la route (Vol.horaires)
 * passés à ZmanimWidget.prochaine, dont la règle n'est pas réécrite.
 */
object VolPriere {

    /** Ce qui reste couvert par la fenêtre de calcul autour du vol (Vol.MARGE, moins un jour). */
    val COUVERT: Duration = Duration.ofHours(6)

    /** La prière du moment et sa limite ; null loin du vol : la fenêtre ne couvre plus. */
    fun priere(calcul: Vol.Calcul, maintenant: Instant): Pair<String, Instant?>? {
        if (maintenant.isBefore(calcul.plan.decollage - COUVERT) || maintenant.isAfter(calcul.plan.atterrissage + COUVERT)) return null
        val (priere, limite) = ZmanimWidget.prochaine(Vol.horaires(calcul, maintenant), maintenant.atZone(ZoneOffset.UTC))
        return priere to limite?.toInstant()
    }
}
