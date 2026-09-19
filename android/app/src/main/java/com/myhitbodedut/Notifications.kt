package com.myhitbodedut

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context

/** Les quatre canaux de notification, créés une fois pour toutes. */
object Notifications {

    const val CHANNEL_GATE = "gate"
    const val CHANNEL_SESSION = "session"
    const val CHANNEL_REMINDER = "reminder"

    const val ID_GATE = 1
    const val ID_SESSION = 2
    const val ID_REMINDER = 3
    const val ID_COMPLETED = 4

    fun ensureChannels(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_GATE,
                context.getString(R.string.channel_gate),
                NotificationManager.IMPORTANCE_LOW
            ).apply { description = context.getString(R.string.channel_gate_description) }
        )

        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_SESSION,
                context.getString(R.string.channel_session),
                NotificationManager.IMPORTANCE_LOW
            ).apply { description = context.getString(R.string.channel_session_description) }
        )

        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_REMINDER,
                context.getString(R.string.channel_reminder),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = context.getString(R.string.channel_reminder_description) }
        )
    }
}
