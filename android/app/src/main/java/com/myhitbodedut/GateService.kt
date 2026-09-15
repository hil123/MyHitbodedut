package com.myhitbodedut

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.WindowManager

/**
 * Service au premier plan : surveille l'application affichée et interpose
 * l'écran de garde tant que la pratique du jour n'est pas validée.
 */
class GateService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var windowManager: WindowManager
    private var overlay: BlockerView? = null
    private var lastQuery = System.currentTimeMillis() - QUERY_BACKFILL_MS
    private var foregroundPackage: String? = null

    private val tick = object : Runnable {
        override fun run() {
            evaluate()
            handler.postDelayed(this, POLL_MS)
        }
    }

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        createChannel()
        startInForeground()
        handler.removeCallbacks(tick)
        handler.post(tick)
    }

    /**
     * Rappelé à chaque startForegroundService, service déjà lancé ou non.
     * Android exige startForeground dans les secondes qui suivent chacun de ces
     * appels, sinon le processus est tué : on le refait donc à chaque fois.
     */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startInForeground()
        return START_STICKY
    }

    override fun onDestroy() {
        handler.removeCallbacks(tick)
        hideOverlay()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // --- Boucle de décision -------------------------------------------------

    private fun evaluate() {
        if (!shouldGuard()) {
            hideOverlay()
            return
        }
        val current = readForegroundPackage()
        if (current != null && current != packageName && Store.blockedApps(this).contains(current)) {
            showOverlay()
        } else {
            hideOverlay()
        }
    }

    private fun shouldGuard(): Boolean =
        Store.isEnabled(this) &&
            !Store.isDone(this) &&
            !Store.isSnoozed(this) &&
            Store.inWindow(this) &&
            Permissions.canDrawOverlays(this) &&
            Permissions.hasUsageAccess(this)

    /** Dernière application passée au premier plan, mémorisée entre deux tours. */
    private fun readForegroundPackage(): String? {
        val usage = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val now = System.currentTimeMillis()
        val events = usage.queryEvents(lastQuery - QUERY_OVERLAP_MS, now)
        val event = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                foregroundPackage = event.packageName
            }
        }
        lastQuery = now
        return foregroundPackage
    }

    // --- Écran de garde -----------------------------------------------------

    private fun showOverlay() {
        if (overlay != null) return
        val view = BlockerView(
            this,
            onDone = {
                Store.markDoneToday(this)
                hideOverlay()
                goHome()
            },
            onSnooze = {
                Store.snooze(this)
                hideOverlay()
                goHome()
            }
        )
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.OPAQUE
        )
        params.gravity = Gravity.TOP or Gravity.START
        try {
            windowManager.addView(view, params)
            overlay = view
        } catch (error: Exception) {
            overlay = null
        }
    }

    private fun hideOverlay() {
        val view = overlay ?: return
        overlay = null
        try {
            windowManager.removeView(view)
        } catch (error: Exception) {
            // La vue avait déjà été retirée.
        }
    }

    private fun goHome() {
        val home = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            startActivity(home)
        } catch (error: Exception) {
            // Aucun lanceur disponible : on se contente de retirer l'écran.
        }
    }

    // --- Notification permanente -------------------------------------------

    private fun createChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.channel_name),
            NotificationManager.IMPORTANCE_LOW
        )
        channel.description = getString(R.string.channel_description)
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    private fun startInForeground() {
        val open = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        val notification: Notification = Notification.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.service_title))
            .setContentText(getString(R.string.service_text, Store.formatMinutes(Store.windowStart(this)), Store.formatMinutes(Store.windowEnd(this))))
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(open)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIF_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIF_ID, notification)
        }
    }

    companion object {
        private const val CHANNEL_ID = "gate"
        private const val NOTIF_ID = 1
        private const val POLL_MS = 900L
        private const val QUERY_BACKFILL_MS = 60_000L
        private const val QUERY_OVERLAP_MS = 2_000L

        fun start(context: Context) {
            val intent = Intent(context, GateService::class.java)
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, GateService::class.java))
        }
    }
}
