package com.myhitbodedut

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import java.time.LocalDate
import java.time.LocalTime

/**
 * Les rappels de la journée.
 *
 * Plusieurs heures peuvent être réglées : un rappel unique se rate
 * facilement, et la règle ne change pas pour autant — dès que la séance du
 * jour est faite, plus rien ne sonne.
 *
 * Une seule alarme est posée à la fois, celle du prochain rappel, et elle se
 * reprogramme à chaque déclenchement. C'est plus sûr qu'une alarme par
 * heure : rien à nettoyer quand la liste change.
 *
 * Alarme inexacte volontairement : elle se déclenche même en veille profonde
 * et n'exige pas la permission d'alarme exacte, que Google Play restreint.
 */
object Reminder {

    private const val REQUEST = 100

    const val ACTION_START = "com.myhitbodedut.REMINDER_START"
    const val ACTION_SNOOZE = "com.myhitbodedut.REMINDER_SNOOZE"

    /** Report demandé depuis la notification. */
    const val SNOOZE_MINUTES = 30

    private fun intent(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context,
        REQUEST,
        Intent(context, ReminderReceiver::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    fun reschedule(context: Context) {
        val alarms = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pending = intent(context)
        alarms.cancel(pending)
        if (!Store.reminderEnabled(context)) return

        val maintenant = System.currentTimeMillis()
        var quand = ReminderRule.prochainDeclenchement(
            java.time.ZonedDateTime.now(),
            Store.reminderTimes(context),
        )?.toInstant()?.toEpochMilli()

        // Un report passe devant le prochain rappel réglé s'il tombe avant.
        val reporte = Store.reminderSnoozedUntil(context)
        if (reporte > maintenant && (quand == null || reporte < quand)) quand = reporte

        if (quand == null) return
        runCatching { alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, quand, pending) }
    }

    fun cancel(context: Context) {
        val alarms = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarms.cancel(intent(context))
        Store.setReminderSnoozedUntil(context, 0L)
    }

    /** Reporte le rappel, sans toucher aux heures réglées. */
    fun snooze(context: Context) {
        Store.setReminderSnoozedUntil(
            context,
            System.currentTimeMillis() + SNOOZE_MINUTES * 60_000L,
        )
        reschedule(context)
    }
}

/** Affiche le rappel, ou exécute ce qu'on a touché dessus. */
class ReminderReceiver : BroadcastReceiver() {

    private companion object {
        const val REQUETE_OUVRIR = 10
        const val REQUETE_COMMENCER = 11
    }

    override fun onReceive(brut: Context, intent: Intent) {
        // La notification dans la langue de l'application (LangueApp).
        val context = LangueApp.envelopper(brut)
        val manager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        when (intent.action) {
            Reminder.ACTION_SNOOZE -> {
                manager.cancel(Notifications.ID_REMINDER)
                Reminder.snooze(context)
                return
            }

            Reminder.ACTION_START -> {
                // Chemin des notifications posées par une version d'avant le
                // 29/09. Android 12 bloque l'écran ouvert d'ici ; le bouton
                // ouvre désormais l'écran de séance directement (build()).
                manager.cancel(Notifications.ID_REMINDER)
                context.startActivity(
                    Intent(context, SessionActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        .putExtra(SessionActivity.EXTRA_START_NOW, true)
                )
                Reminder.reschedule(context)
                return
            }
        }

        // Le report est consommé : sans cela, il repousserait chaque rappel.
        Store.setReminderSnoozedUntil(context, 0L)

        val now = LocalTime.now()
        // Pas de rappel pendant une hitbodedout : elle est en train de se faire.
        val allowed = Store.reminderEnabled(context) && !Store.isSessionRunning(context) && ReminderRule.shouldNotify(
            nowMinutes = now.hour * 60 + now.minute,
            doneToday = Store.isDone(context),
            todayIsRestDay = Store.isExempt(context, LocalDate.now()),
            tomorrowIsRestDay = Store.isEveOfRestDay(context, LocalDate.now()),
            // Aucune ville n'est réglée : on s'en tient à la limite de midi.
            sunsetMinutes = null
        )
        if (allowed) manager.notify(Notifications.ID_REMINDER, build(context))
        Reminder.reschedule(context)
    }

    private fun build(context: Context): Notification {
        Notifications.ensureChannels(context)
        val duree = Store.formatDuration(context, Store.targetMinutes(context))

        val ouvrir = PendingIntent.getActivity(
            context, REQUETE_OUVRIR,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        // « Commencer » ouvre l'écran de séance lui-même. Passer par un
        // receveur qui ouvre ensuite l'écran est interdit depuis Android 12 :
        // la notification disparaissait et rien ne s'ouvrait (revue du 29/09).
        // L'écran retire la notification et reprogramme le rappel.
        val commencer = Notification.Action.Builder(
            null,
            context.getString(R.string.reminder_action_start),
            PendingIntent.getActivity(
                context, REQUETE_COMMENCER,
                Intent(context, SessionActivity::class.java)
                    .putExtra(SessionActivity.EXTRA_START_NOW, true)
                    .putExtra(SessionActivity.EXTRA_DEPUIS_RAPPEL, true),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        ).build()
        fun action(requete: Int, action: String, libelle: Int) = Notification.Action.Builder(
            null,
            context.getString(libelle),
            PendingIntent.getBroadcast(
                context, requete,
                Intent(context, ReminderReceiver::class.java).setAction(action),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        ).build()

        return Notification.Builder(context, Notifications.CHANNEL_REMINDER)
            .setContentTitle(context.getString(R.string.reminder_title))
            .setContentText(context.getString(R.string.reminder_text, duree))
            .setSmallIcon(R.drawable.ic_notification)
            // Les anneaux du jour, vides ou pleins : on voit d'un coup d'œil
            // ce qui reste à faire (demande du 01/10).
            .setLargeIcon(AnneauxImage.notification(context))
            .setContentIntent(ouvrir)
            .setAutoCancel(true)
            .addAction(commencer)
            .addAction(action(2, Reminder.ACTION_SNOOZE, R.string.reminder_action_later))
            .build()
    }
}
