package com.myhitbodedut

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Relance la surveillance et le rappel quand ce qui les portait a disparu.
 *
 * Au redémarrage, bien sûr ; mais aussi après une mise à jour de
 * l'application, qui arrêtait la barrière du matin jusqu'à la prochaine
 * ouverture, et quand l'heure ou le fuseau changent — en voyage, le rappel
 * sonnait encore à l'heure de l'endroit quitté (revue du 29/09).
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(brut: Context, intent: Intent) {
        if (intent.action !in ACTIONS) return
        // Le rappel et la vignette dans la langue de l'application (LangueApp).
        val context = LangueApp.envelopper(brut)
        if (BuildConfig.HAS_GATE && GateStatus.shouldRunService(context)) {
            runCatching { GateService.start(context) }
        }
        // Une séance en cours quand le téléphone s'est éteint : échue, elle
        // est inscrite pour sa durée ; sinon elle reprend si Android le permet.
        SessionService.reconcilier(context)
        Reminder.reschedule(context)
        StreakWidget.refresh(context)
    }

    private companion object {
        val ACTIONS = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
        )
    }
}
