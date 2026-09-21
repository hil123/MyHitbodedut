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
    private lateinit var openSession: Button
    private lateinit var art: ImageView
    private lateinit var musicLine: TextView
    private lateinit var chizuk: View
    private lateinit var stuck: Button
    private lateinit var guide: View

    /**
     * Le temps courant de la hitbodedout guidée. Il appartient à la séance
     * et repart à zéro avec elle : un guide qui reprendrait là où l'on
     * s'était arrêté la veille rappellerait une dette.
     */
    private var guideStep = 0

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
        openSession = findViewById(R.id.session_open)
        art = findViewById(R.id.session_character)
        musicLine = findViewById(R.id.session_music)
        chizuk = findViewById(R.id.session_chizuk)
        stuck = findViewById(R.id.session_stuck)
        guide = findViewById(R.id.session_hitbodedut)

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

        // « Sans durée » : la séance court jusqu'à ce qu'on l'arrête. Elle ne
        // touche pas à la durée habituelle, qui reste celle du bouton principal.
        openSession.setOnClickListener {
            SessionService.start(this, SessionRule.OUVERTE)
            guideStep = 0
            drawOpening()
            render()
        }

        intArrayOf(R.id.duration_10, R.id.duration_20, R.id.duration_30, R.id.duration_60)
            .forEach { id ->
                val button = findViewById<Button>(id)
                val minutes = button.tag.toString().toInt()
                button.text = Store.formatDuration(minutes)
                button.setOnClickListener {
                    Store.setTargetMinutes(this, minutes)
                    SessionService.start(this, minutes)
                    guideStep = 0
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
            // Une séance ouverte n'a rien à décompter : elle compte à
            // l'endroit, depuis son début. Un décompte masqué ne compte pas
            // du tout.
            val counter = Store.sessionCounter(this)
            countdown.visibility =
                if (counter == SessionRule.Compteur.MASQUE) View.GONE else View.VISIBLE
            if (counter != SessionRule.Compteur.MASQUE) {
                val millis = if (counter == SessionRule.Compteur.ECOULE) {
                    (System.currentTimeMillis() - Store.sessionStartedAt(this)).coerceAtLeast(0)
                } else {
                    (Store.sessionEndsAt(this) - System.currentTimeMillis()).coerceAtLeast(0)
                }
                countdown.text = String.format(
                    Locale.US, "%02d:%02d", millis / 60_000L, (millis / 1_000L) % 60
                )
            }
            caption.text = when (counter) {
                SessionRule.Compteur.MASQUE -> getString(R.string.session_hidden_caption)
                SessionRule.Compteur.ECOULE -> getString(R.string.session_open_caption)
                SessionRule.Compteur.RESTE -> getString(
                    R.string.session_target,
                    Store.formatDuration(Store.sessionTarget(this))
                )
            }
            durations.visibility = View.GONE
            openSession.visibility = View.GONE
            stuck.visibility = if (hasBlocage) View.VISIBLE else View.GONE
            renderGuide()

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
            countdown.visibility = View.VISIBLE
            countdown.text = Store.formatDuration(Store.targetMinutes(this))
            caption.setText(
                if (Store.isDone(this)) R.string.session_idle_done else R.string.session_idle_caption
            )
            durations.visibility = View.VISIBLE
            openSession.visibility = View.VISIBLE
            stuck.visibility = View.GONE
            HitbodedutCard.hide(guide)

            primary.visibility = View.VISIBLE
            primary.text = getString(
                R.string.session_start,
                Store.formatDuration(Store.targetMinutes(this))
            )
            primary.setOnClickListener {
                SessionService.start(this, Store.targetMinutes(this))
                guideStep = 0
                drawOpening()
                render()
            }
            secondary.visibility = View.GONE
        }
    }

    /**
     * La hitbodedout guidée, quand elle est demandée. Le pas courant ne
     * change que sur les boutons de la carte : render() repasse chaque
     * seconde et avancerait tout seul.
     */
    private fun renderGuide() {
        if (!Store.guidedHitbodedut(this)) {
            HitbodedutCard.hide(guide)
            return
        }
        HitbodedutCard.bind(guide, guideStep) { step ->
            guideStep = step
            renderGuide()
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
