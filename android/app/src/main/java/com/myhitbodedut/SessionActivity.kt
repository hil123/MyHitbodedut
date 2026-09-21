package com.myhitbodedut

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale

/** La séance elle-même : choix de la durée, puis décompte. */
class SessionActivity : AppCompatActivity() {

    private val handler = Handler(Looper.getMainLooper())

    private lateinit var countdown: TextView
    private lateinit var caption: TextView
    private lateinit var primary: Button
    private lateinit var secondary: Button
    private lateinit var durations: View
    private lateinit var art: ImageView
    private lateinit var musicLine: TextView
    private lateinit var chizuk: View
    private lateinit var stuck: Button

    /**
     * Le bouton de blocage n'a de sens que s'il a un texte à donner. En
     * release, tant qu'aucun texte n'est relu, il n'y en a pas.
     */
    private val hasBlocage: Boolean by lazy {
        ChizukRules.forMoment(Data.chizuk(this), Moment.BLOCAGE, BuildConfig.DEBUG).isNotEmpty()
    }

    /** Pour repérer la fin d'une séance, qu'elle vienne du décompte ou du bouton. */
    private var wasRunning = false

    /** Une séance abandonnée n'est pas une séance close : pas de mot de fin. */
    private var cancelling = false

    private val tick = object : Runnable {
        override fun run() {
            render()
            handler.postDelayed(this, 1_000L)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_session)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        countdown = findViewById(R.id.session_countdown)
        caption = findViewById(R.id.session_caption)
        primary = findViewById(R.id.session_primary)
        secondary = findViewById(R.id.session_secondary)
        durations = findViewById(R.id.session_durations)
        art = findViewById(R.id.session_character)
        musicLine = findViewById(R.id.session_music)
        chizuk = findViewById(R.id.session_chizuk)
        stuck = findViewById(R.id.session_stuck)

        // « Je n'arrive pas à parler » : le moment « blocage ».
        stuck.setOnClickListener { ChizukCard.show(chizuk, Moment.BLOCAGE) }

        // « ouverture » : le mot du début de séance.
        drawOpening()

        // Venu du bouton « Commencer » d'un rappel : la séance part seule,
        // sinon le rappel n'aurait fait qu'ouvrir un écran de plus.
        if (intent?.getBooleanExtra(EXTRA_START_NOW, false) == true &&
            !Store.isSessionRunning(this)
        ) {
            SessionService.start(this, Store.targetMinutes(this))
        }

        intArrayOf(R.id.duration_10, R.id.duration_20, R.id.duration_30, R.id.duration_60)
            .forEach { id ->
                val button = findViewById<Button>(id)
                val minutes = button.tag.toString().toInt()
                button.text = Store.formatDuration(minutes)
                button.setOnClickListener {
                    Store.setTargetMinutes(this, minutes)
                    SessionService.start(this, minutes)
                    drawOpening()
                    render()
                }
            }
    }

    override fun onResume() {
        super.onResume()
        handler.removeCallbacks(tick)
        handler.post(tick)
    }

    override fun onPause() {
        handler.removeCallbacks(tick)
        super.onPause()
    }

    private fun render() {
        renderMusicLine()

        // « cloture » : au passage de « en cours » à « terminée », que la
        // séance se soit achevée d'elle-même ou par le bouton.
        val running = Store.isSessionRunning(this)
        if (wasRunning && !running) {
            if (cancelling) cancelling = false else ChizukCard.show(chizuk, Moment.CLOTURE)
        }
        wasRunning = running

        if (running) {
            art.setImageResource(Character.during())
            val remaining = (Store.sessionEndsAt(this) - System.currentTimeMillis()).coerceAtLeast(0)
            val minutes = remaining / 60_000L
            val seconds = (remaining / 1_000L) % 60
            countdown.text = String.format(Locale.US, "%02d:%02d", minutes, seconds)
            caption.text = getString(
                R.string.session_target,
                Store.formatDuration(Store.sessionTarget(this))
            )
            durations.visibility = View.GONE
            stuck.visibility = if (hasBlocage) View.VISIBLE else View.GONE

            primary.visibility = View.VISIBLE
            primary.setText(R.string.session_finish_now)
            primary.setOnClickListener { confirmFinish() }

            secondary.visibility = View.VISIBLE
            secondary.setText(R.string.session_cancel)
            secondary.setOnClickListener { confirmCancel() }
        } else {
            art.setImageResource(
                if (Store.isDone(this)) Character.joy() else Character.call()
            )
            countdown.text = Store.formatDuration(Store.targetMinutes(this))
            caption.setText(
                if (Store.isDone(this)) R.string.session_idle_done else R.string.session_idle_caption
            )
            durations.visibility = View.VISIBLE
            stuck.visibility = View.GONE

            primary.visibility = View.VISIBLE
            primary.text = getString(
                R.string.session_start,
                Store.formatDuration(Store.targetMinutes(this))
            )
            primary.setOnClickListener {
                SessionService.start(this, Store.targetMinutes(this))
                drawOpening()
                render()
            }
            secondary.visibility = View.GONE
        }
    }

    private fun renderMusicLine() {
        val track = if (Store.musicEnabled(this)) MusicLibrary.selected(this) else null
        musicLine.visibility = if (track == null) View.GONE else View.VISIBLE
        if (track != null) musicLine.text = getString(R.string.session_music_line, track.title)
    }

    private fun confirmFinish() {
        AlertDialog.Builder(this)
            .setTitle(R.string.session_finish_title)
            .setMessage(R.string.session_finish_message)
            .setPositiveButton(R.string.session_finish_now) { _, _ ->
                SessionService.finishNow(this)
                handler.postDelayed({ askNote() }, 400L)
            }
            .setNegativeButton(R.string.action_back, null)
            .show()
    }

    private fun confirmCancel() {
        AlertDialog.Builder(this)
            .setTitle(R.string.session_cancel_title)
            .setMessage(R.string.session_cancel_message)
            .setPositiveButton(R.string.session_cancel) { _, _ ->
                cancelling = true
                SessionService.cancel(this)
                render()
            }
            .setNegativeButton(R.string.action_back, null)
            .show()
    }

    /**
     * Tire le mot d'ouverture. Appelé à l'entrée sur l'écran et au démarrage
     * de chaque séance, jamais depuis render(), qui repasse chaque seconde.
     */
    private fun drawOpening() {
        ChizukCard.show(chizuk, Moment.OUVERTURE)
    }

    private fun askNote() {
        NoteDialog.show(this, Store.today()) { render() }
    }

    companion object {
        /** Posé par le bouton « Commencer » d'une notification de rappel. */
        const val EXTRA_START_NOW = "start_now"
    }
}
