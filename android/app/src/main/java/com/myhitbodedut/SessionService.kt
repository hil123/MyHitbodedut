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
import android.os.PowerManager
import android.widget.Toast

/**
 * Déroule la séance de hitbodedout en arrière-plan, pour que le téléphone
 * puisse être posé. La notification indique le temps restant.
 */
class SessionService : Service() {

    /** La langue choisie dans l'application, même avant Android 13 (LangueApp). */
    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(LangueApp.envelopper(base))
    }

    private val handler = Handler(Looper.getMainLooper())
    private val music by lazy { MusicPlayer(this) }
    private val melange = MelangeMusique()

    /** Le mode de ce qui joue : null tant que rien n'a été lancé. */
    private var modeAleatoire: Boolean? = null

    /** Le son de fin, gardé pour qu'il ne soit pas ramassé en plein jeu. */
    private var ringtone: Ringtone? = null

    /**
     * Tient le processeur éveillé jusqu'au terme d'une séance minutée.
     *
     * Écran éteint et sans musique, le téléphone s'endort et le décompte
     * s'arrête avec lui : la fin n'était constatée qu'au réveil, parfois
     * une heure plus tard, et le son de fin sonnait d'autant (revue du
     * 29/09). Le verrou porte une échéance : il ne peut pas rester pris.
     */
    private var eveil: PowerManager.WakeLock? = null

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
            ACTION_MUSIC -> {
                if (!Store.isSessionRunning(this)) {
                    stopSelf()
                    return START_NOT_STICKY
                }
                if (!Store.musicEnabled(this)) {
                    music.stop()
                    return START_STICKY
                }
                // Passage à la lecture aléatoire, ou retour au morceau choisi,
                // pendant la séance : on coupe, le chemin ordinaire relance.
                if (modeAleatoire != null && modeAleatoire != Store.musicShuffle(this)) {
                    music.stop(fade = false)
                }
                // Réactivée : le chemin ordinaire ci-dessous relance le morceau.
            }
        }
        startForegroundNow()
        enMarche = true
        garderEveille()
        ensureMusic()
        StreakWidget.refresh(this)
        handler.removeCallbacks(tick)
        handler.post(tick)
        return START_STICKY
    }

    override fun onDestroy() {
        handler.removeCallbacks(tick)
        relacherEveil()
        enMarche = false
        super.onDestroy()
    }

    private fun garderEveille() {
        val fin = Store.sessionEndsAt(this)
        if (Store.isSessionOpen(this) || fin <= 0L) return
        val duree = (fin - System.currentTimeMillis()).coerceAtLeast(0L) + MARGE_EVEIL_MS
        relacherEveil()
        eveil = (getSystemService(Context.POWER_SERVICE) as PowerManager)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "myhitbodedut:seance")
            .apply {
                setReferenceCounted(false)
                acquire(duree)
            }
    }

    private fun relacherEveil() {
        eveil?.let { if (it.isHeld) runCatching { it.release() } }
        eveil = null
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
        // Le sujet se lit AVANT la clôture : endSession efface la séance.
        val theme = Store.sessionTheme(this)
        val close = Store.endSession(this)
        if (close.minutes > 0 || !early) {
            inscrire(this, close, theme = theme)
            notifyCompleted(close)
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
        modeAleatoire = Store.musicShuffle(this)
        if (Store.musicShuffle(this)) {
            jouerAuHasard()
            return
        }
        val track = MusicLibrary.selected(this) ?: return
        Store.setMusicNow(this, track.id)
        if (!music.start(track, Store.musicVolume(this))) {
            Toast.makeText(this, R.string.music_play_failed, Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Lecture aléatoire : un morceau, puis à sa fin le suivant tiré du même
     * sac, jusqu'à la fin de la séance. Un morceau illisible (fichier du
     * téléphone supprimé) est sauté plutôt que de laisser la séance muette.
     */
    private fun jouerAuHasard(essais: Int = 3) {
        val morceaux = MusicLibrary.all(this)
        val id = melange.suivant(morceaux.map { it.id }) ?: return
        val track = morceaux.first { it.id == id }
        Store.setMusicNow(this, track.id)
        val lance = music.start(track, Store.musicVolume(this), loop = false) {
            if (Store.isSessionRunning(this) && Store.musicEnabled(this)) jouerAuHasard()
        }
        if (!lance) {
            if (essais > 1) jouerAuHasard(essais - 1)
            else Toast.makeText(this, R.string.music_play_failed, Toast.LENGTH_LONG).show()
        }
    }

    private fun stopSelfAndClear() {
        relacherEveil()
        music.stop()
        Store.setMusicNow(this, null)
        handler.removeCallbacks(tick)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun notifyCompleted(close: SeanceClose) {
        val minutes = close.minutes
        // Code de requête propre et FLAG_UPDATE_CURRENT : les autres
        // notifications ouvrent aussi l'accueil, et Android rendait la même
        // intention sans le jour de la note (revue du 29/09).
        val open = PendingIntent.getActivity(
            this,
            REQUETE_FIN,
            Intent(this, MainActivity::class.java).putExtra(MainActivity.EXTRA_ASK_NOTE, close.date),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = Notification.Builder(this, Notifications.CHANNEL_REMINDER)
            .setContentTitle(getString(R.string.session_done_title))
            .setContentText(getString(R.string.session_done_text, Store.formatDuration(this, minutes)))
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
            getString(R.string.session_elapsed, Store.formatDuration(this, Store.sessionElapsedMinutes(this)))
        SessionRule.Compteur.RESTE ->
            getString(R.string.session_remaining, Store.formatDuration(this, remainingMinutes()))
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
        private const val MARGE_EVEIL_MS = 2 * 60_000L
        private const val REQUETE_FIN = 30

        /**
         * Le service tourne dans ce processus. Un processus tué repart à
         * faux : une séance encore inscrite n'a alors plus personne pour la
         * suivre, et reconcilier() la reprend.
         */
        @Volatile
        var enMarche = false
            private set

        /** Inscrit une séance close : la journée de son début, et le bravo. */
        fun inscrire(context: Context, close: SeanceClose, theme: Theme?) {
            Store.addSession(context, close.minutes, theme = theme, date = close.date)
            Store.setPendingCelebration(context, close.minutes, close.date)
        }

        /**
         * Reprend une séance restée inscrite après la mort du service
         * (application tuée par le système, téléphone redémarré).
         *
         * Échue, elle est inscrite pour sa durée, au jour de son début — sans
         * cela ses minutes étaient perdues et la série cassait. Encore en
         * cours, le service est relancé, s'il peut l'être d'ici.
         */
        fun reconcilier(context: Context) {
            if (enMarche || !Store.hasSessionRecord(context)) return
            val maintenant = System.currentTimeMillis()
            val echue = SessionRule.estEchue(
                Store.sessionStartedAt(context),
                Store.sessionEndsAt(context),
                Store.sessionTarget(context),
                maintenant
            )
            if (echue) {
                val theme = Store.sessionTheme(context)
                val close = Store.endSession(context)
                if (close.minutes > 0) inscrire(context, close, theme)
                StreakWidget.refresh(context)
            } else {
                runCatching {
                    context.startForegroundService(Intent(context, SessionService::class.java))
                }
            }
        }
        const val ACTION_CANCEL = "com.myhitbodedut.CANCEL_SESSION"
        const val ACTION_FINISH = "com.myhitbodedut.FINISH_SESSION"
        const val ACTION_MUSIC = "com.myhitbodedut.MUSIC_CHANGED"

        /** L'interrupteur de musique a bougé : la séance en cours doit suivre. */
        fun musicChanged(context: Context) {
            if (!Store.isSessionRunning(context)) return
            context.startService(
                Intent(context, SessionService::class.java).setAction(ACTION_MUSIC)
            )
        }

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
