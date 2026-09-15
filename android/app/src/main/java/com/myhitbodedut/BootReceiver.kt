package com.myhitbodedut

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Relance la surveillance après un redémarrage. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        if (Store.isEnabled(context) && Permissions.allGranted(context)) {
            GateService.start(context)
        }
    }
}
