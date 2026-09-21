package com.myhitbodedut

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.drawable.Icon
import android.media.Ringtone
import android.media.RingtoneManager
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
    private val music by lazy { MusicPlayer(this) }

    /** Le son de fin, gardé pour qu'il ne soit pas ramassé en plein jeu. */
    private var ringtone: Ringtone? = null

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
                StreakWidget.refresh(this)
                stopSelfAndClear()
                return START_NOT_STICKY
            }
            ACTION_FINISH -> {
                finishSession(early = true)
                return START_NOT_STICKY
            }
        }
        startForegroundNow()
        ensureMusic()
        StreakWidget.refresh(this)
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
        startForegroundNow()
        return true
    }

    private fun remainingMinutes(): Int =
        SessionRule.minutesRestantes(Store.sessionEndsAt(this), System.currentTimeMillis())

    private fun finishSession(early: Boolean) {
        val elapsed = Store.endSession(this)
        if (elapsed > 0 || !early) {
            Store.addSession(this, elapsed)
            notifyCompleted(elapsed)
        }
        // Décompte masqué : rien n'a prévenu que le temps s'écoulait, il faut
        // donc que quelque chose dise qu'il est écoulé. Seulement quand la
        // séance arrive d'elle-même à son terme : celui qui l'arrête lui-même
        // n'a pas besoin qu'on le lui apprenne.
        if (!early && Store.hideCountdown(this)) chime()
        StreakWidget.refresh(this)
        stopSelfAndClear()
    }

    /**
     * Le son de fin. La sonnerie de notification du téléphone, celle que son
     * propriétaire a déjà choisie et déjà réglée : rien de plus discret qu'un
     * son qu'on reconnaît. La référence est gardée le temps qu'il finisse.
     */
    private fun chime() {
        runCatching {
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                ?: return
            ringtone = RingtoneManager.getRingtone(applicationContext, uri)?.also { it.play() }
        }
    }

    /** Lance le morceau retenu, si la musique est active et rien ne joue déjà. */
    private fun ensureMusic() {
        if (!Store.musicEnabled(this) || music.isPlaying) return
        if (!Store.isSessionRunning(this)) return
        val track = MusicLibrary.selected(this) ?: return
        music.start(track, Store.musicVolume(this))
    }

    private fun stopSelfAndClear() {
        music.stop()
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

    /**
     * Ce que la notification annonce pendant la séance : le temps restant
     * quand il y en a un, le temps déjà passé quand la séance est ouverte.
     */
    private fun runningText(): String = when (Store.sessionCounter(this)) {
        SessionRule.Compteur.MASQUE -> getString(R.string.session_running_hidden)
        SessionRule.Compteur.ECOULE ->
            getString(R.string.session_elapsed, Store.formatDuration(Store.sessionElapsedMinutes(this)))
        SessionRule.Compteur.RESTE ->
            getString(R.string.session_remaining, Store.formatDuration(remainingMinutes()))
    }

    private fun startForegroundNow() {
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
            .setContentText(runningText())
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
