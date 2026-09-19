package com.myhitbodedut

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper

/**
 * Déroule la séance de hitbodedout en arrière-plan, pour que le téléphone
 * puisse être posé. La notification indique le temps restant.
 */
class SessionService : Service() {

    private val handler = Handler(Looper.getMainLooper())

    private val tick = object : Runnable {
        override fun run() {
            if (refresh()) handler.postDelayed(this, TICK_MS)
        }
    }

    override fun onCreate() {
        super.onCreate()
        Notifications.ensureChannels(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_CANCEL -> {
                Store.endSession(this)
                stopSelfAndClear()
                return START_NOT_STICKY
            }
            ACTION_FINISH -> {
                finishSession(early = true)
                return START_NOT_STICKY
            }
        }
        startForegroundNow(remainingMinutes())
        handler.removeCallbacks(tick)
        handler.post(tick)
        return START_STICKY
    }

    override fun onDestroy() {
        handler.removeCallbacks(tick)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    /** Renvoie vrai tant que la séance continue. */
    private fun refresh(): Boolean {
        if (!Store.isSessionRunning(this)) {
            finishSession(early = false)
            return false
        }
        startForegroundNow(remainingMinutes())
        return true
    }

    private fun remainingMinutes(): Int {
        val remaining = Store.sessionEndsAt(this) - System.currentTimeMillis()
        if (remaining <= 0) return 0
        // Arrondi au supérieur : afficher « 1 min » jusqu'à la toute fin.
        return ((remaining + 59_999L) / 60_000L).toInt()
    }

    private fun finishSession(early: Boolean) {
        val elapsed = Store.endSession(this)
        if (elapsed > 0 || !early) {
            Store.addSession(this, elapsed)
            notifyCompleted(elapsed)
        }
        stopSelfAndClear()
    }

    private fun stopSelfAndClear() {
        handler.removeCallbacks(tick)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun notifyCompleted(minutes: Int) {
        val open = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).putExtra(MainActivity.EXTRA_ASK_NOTE, true),
            PendingIntent.FLAG_IMMUTABLE
        )
        val notification = Notification.Builder(this, Notifications.CHANNEL_REMINDER)
            .setContentTitle(getString(R.string.session_done_title))
            .setContentText(getString(R.string.session_done_text, Store.formatDuration(minutes)))
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(Notifications.ID_COMPLETED, notification)
    }

    private fun startForegroundNow(remaining: Int) {
        val open = PendingIntent.getActivity(
            this,
            0,
            Intent(this, SessionActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        val finish = PendingIntent.getService(
            this,
            1,
            Intent(this, SessionService::class.java).setAction(ACTION_FINISH),
            PendingIntent.FLAG_IMMUTABLE
        )
        val notification = Notification.Builder(this, Notifications.CHANNEL_SESSION)
            .setContentTitle(getString(R.string.session_running_title))
            .setContentText(getString(R.string.session_remaining, Store.formatDuration(remaining)))
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(open)
            .setOngoing(true)
            .addAction(
                Notification.Action.Builder(
                    Icon.createWithResource(this, R.drawable.ic_notification),
                    getString(R.string.session_finish_now),
                    finish
                ).build()
            )
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(Notifications.ID_SESSION, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(Notifications.ID_SESSION, notification)
        }
    }

    companion object {
        private const val TICK_MS = 5_000L
        const val ACTION_CANCEL = "com.myhitbodedut.CANCEL_SESSION"
        const val ACTION_FINISH = "com.myhitbodedut.FINISH_SESSION"

        fun start(context: Context, minutes: Int) {
            Store.beginSession(context, minutes)
            context.startForegroundService(Intent(context, SessionService::class.java))
        }

        fun cancel(context: Context) {
            context.startService(
                Intent(context, SessionService::class.java).setAction(ACTION_CANCEL)
            )
        }

        fun finishNow(context: Context) {
            context.startService(
                Intent(context, SessionService::class.java).setAction(ACTION_FINISH)
            )
        }
    }
}
