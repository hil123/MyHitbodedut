package com.myhitbodedut

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/**
 * Rappel quotidien à l'heure choisie.
 * Alarme inexacte volontairement : elle se déclenche même en veille profonde
 * et n'exige pas la permission d'alarme exacte, que Google Play restreint.
 */
object Reminder {

    private const val REQUEST = 100

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

        val minutes = Store.reminderMinutes(context)
        val time = LocalTime.of((minutes / 60) % 24, minutes % 60)
        var next = LocalDateTime.of(LocalDate.now(), time)
        if (!next.isAfter(LocalDateTime.now())) next = next.plusDays(1)
        val millis = next.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        runCatching { alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pending) }
    }

    fun cancel(context: Context) {
        val alarms = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarms.cancel(intent(context))
    }
}

/** Affiche le rappel, puis reprogramme celui du lendemain. */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val now = java.time.LocalTime.now()
        val allowed = Store.reminderEnabled(context) && ReminderRule.shouldNotify(
            nowMinutes = now.hour * 60 + now.minute,
            doneToday = Store.isDone(context),
            todayIsRestDay = Store.isExempt(context, LocalDate.now()),
            tomorrowIsRestDay = Store.isEveOfRestDay(context, LocalDate.now()),
            // Aucune ville n'est réglée : on s'en tient à la limite de midi.
            sunsetMinutes = null
        )
        if (allowed) {
            Notifications.ensureChannels(context)
            val open = PendingIntent.getActivity(
                context,
                0,
                Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE
            )
            val notification: Notification =
                Notification.Builder(context, Notifications.CHANNEL_REMINDER)
                    .setContentTitle(context.getString(R.string.reminder_title))
                    .setContentText(
                        context.getString(
                            R.string.reminder_text,
                            Store.formatDuration(Store.targetMinutes(context))
                        )
                    )
                    .setSmallIcon(R.drawable.ic_notification)
                    .setContentIntent(open)
                    .setAutoCancel(true)
                    .build()
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.notify(Notifications.ID_REMINDER, notification)
        }
        Reminder.reschedule(context)
    }
}
