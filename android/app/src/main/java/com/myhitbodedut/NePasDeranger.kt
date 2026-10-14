package com.myhitbodedut

import android.app.NotificationManager
import android.content.Context

/**
 * « Ne pas déranger » pendant la hitbodedout (14/10) : au début de la séance, le
 * téléphone passe en mode « priorité » (les alarmes et les appels prioritaires
 * passent, comme l'utilisateur les a réglés) ; à la fin, il revient exactement
 * au mode d'avant. Seulement si l'option est cochée et qu'Android a donné l'accès
 * (Réglages → « Ne pas déranger », autorisation à accorder une fois).
 */
object NePasDeranger {

    private fun nm(context: Context) = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    fun accesAccorde(context: Context): Boolean = runCatching { nm(context).isNotificationPolicyAccessGranted }.getOrDefault(false)

    fun activer(context: Context) {
        if (!Store.nePasDeranger(context) || !accesAccorde(context)) return
        val nm = nm(context)
        // Déjà retenu : une relance du service ne doit pas écraser le mode d'avant.
        if (Store.filtreAvantSeance(context) != null) return
        Store.setFiltreAvantSeance(context, nm.currentInterruptionFilter)
        runCatching { nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY) }
    }

    fun retablir(context: Context) {
        val avant = Store.filtreAvantSeance(context) ?: return
        Store.setFiltreAvantSeance(context, null)
        if (accesAccorde(context)) runCatching { nm(context).setInterruptionFilter(avant) }
    }
}
