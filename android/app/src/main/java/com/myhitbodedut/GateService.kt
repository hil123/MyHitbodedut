package com.myhitbodedut

import android.app.Notification
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
 * l'écran de garde tant que la pratique du jour n'est pas faite.
 */
class GateService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var windowManager: WindowManager
    private var overlay: BlockerView? = null
    private var overlayMode: BlockerMode? = null
    private var lastQuery = System.currentTimeMillis() - QUERY_BACKFILL_MS
    private var foregroundPackage: String? = null
    private var previewing = false

    private val tick = object : Runnable {
        override fun run() {
            evaluate()
            handler.postDelayed(this, POLL_MS)
        }
    }

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        Notifications.ensureChannels(this)
    }

    /**
     * Android exige startForeground dans les secondes qui suivent chaque
     * startForegroundService, service déjà lancé ou non.
     */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startInForeground()
        if (intent?.action == ACTION_PREVIEW) {
            previewing = true
            showOverlay(BlockerMode.PREVIEW)
        }
        handler.removeCallbacks(tick)
        handler.post(tick)
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
        if (previewing) return

        val running = Store.isSessionRunning(this)
        val armed = GateStatus.isArmed(this) || (running && GateStatus.shouldRunService(this))
        if (!armed) {
            hideOverlay()
            return
        }

        val current = readForegroundPackage()
        val blocked = current != null &&
            current != packageName &&
            Store.blockedApps(this).contains(current)

        if (blocked) {
            showOverlay(if (running) BlockerMode.SESSION_RUNNING else BlockerMode.PROMPT)
        } else {
            hideOverlay()
        }
    }

    /** Dernière application passée au premier plan, mémorisée entre deux tours. */
    private fun readForegroundPackage(): String? {
        val usage = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val now = System.currentTimeMillis()
        val events = runCatching { usage.queryEvents(lastQuery - QUERY_OVERLAP_MS, now) }.getOrNull()
        if (events != null) {
            val event = UsageEvents.Event()
            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                if (event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                    foregroundPackage = event.packageName
                }
            }
        }
        lastQuery = now
        return foregroundPackage
    }

    // --- Écran de garde -----------------------------------------------------

    private fun showOverlay(mode: BlockerMode) {
        if (overlay != null && overlayMode == mode) return
        hideOverlay()
        if (!Permissions.canDrawOverlays(this)) return

        val view = BlockerView(
            context = this,
            mode = mode,
            onStartSession = {
                hideOverlay()
                previewing = false
                if (!Store.isSessionRunning(this)) {
                    // Démarrage d'un service de premier plan depuis un autre :
                    // autorisé tant que celui-ci tourne, mais on ne parie pas dessus.
                    runCatching { SessionService.start(this, Store.targetMinutes(this)) }
                }
                openSession()
            },
            onAlreadyDone = {
                Store.addSession(this, 0)
                hideOverlay()
                goHome()
            },
            onSnooze = {
                Store.snooze(this)
                hideOverlay()
                goHome()
            },
            onDismiss = {
                previewing = false
                hideOverlay()
                if (!GateStatus.shouldRunService(this)) stopSelf()
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

        runCatching {
            windowManager.addView(view, params)
            overlay = view
            overlayMode = mode
        }.onFailure {
            overlay = null
            overlayMode = null
        }
    }

    private fun hideOverlay() {
        val view = overlay ?: return
        overlay = null
        overlayMode = null
        runCatching { windowManager.removeView(view) }
    }

    private fun openSession() {
        runCatching {
            startActivity(
                Intent(this, SessionActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            )
        }
    }

    private fun goHome() {
        runCatching {
            startActivity(
                Intent(Intent.ACTION_MAIN)
                    .addCategory(Intent.CATEGORY_HOME)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }

    // --- Notification permanente -------------------------------------------

    private fun startInForeground() {
        val open = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        val state = GateStatus.evaluate(this)
        val notification: Notification = Notification.Builder(this, Notifications.CHANNEL_GATE)
            .setContentTitle(getString(GateStatus.titleOf(state)))
            .setContentText(GateStatus.detailOf(this, state))
            .setStyle(Notification.BigTextStyle().bigText(GateStatus.detailOf(this, state)))
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(open)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(Notifications.ID_GATE, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(Notifications.ID_GATE, notification)
        }
    }

    companion object {
        private const val POLL_MS = 900L
        private const val QUERY_BACKFILL_MS = 60_000L
        private const val QUERY_OVERLAP_MS = 2_000L
        const val ACTION_PREVIEW = "com.myhitbodedut.PREVIEW_GATE"

        fun start(context: Context) {
            context.startForegroundService(Intent(context, GateService::class.java))
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, GateService::class.java))
        }

        /** Affiche l'écran de garde une fois, sans effet, pour vérifier qu'il fonctionne. */
        fun preview(context: Context) {
            context.startForegroundService(
                Intent(context, GateService::class.java).setAction(ACTION_PREVIEW)
            )
        }
    }
}
