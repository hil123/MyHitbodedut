package com.myhitbodedut

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Relance la surveillance et le rappel après un redémarrage. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(brut: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        // Le rappel et la vignette dans la langue de l'application (LangueApp).
        val context = LangueApp.envelopper(brut)
        if (BuildConfig.HAS_GATE && GateStatus.shouldRunService(context)) {
            GateService.start(context)
        }
        Reminder.reschedule(context)
        StreakWidget.refresh(context)
    }
}
