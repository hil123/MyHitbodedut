package com.myhitbodedut

import android.app.NotificationManager
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
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
    private lateinit var theme: Button
    private lateinit var guide: View
    private lateinit var souffle: SouffleView
    private lateinit var calme: PratiqueCalme

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
        theme = findViewById(R.id.session_theme)
        theme.setOnClickListener { pickTheme() }

        // La pratique calme (16/10) : la lueur derrière le minuteur, et ce qui
        // s'efface après six secondes sans toucher. Restent le minuteur,
        // « Terminer » et, si elle est demandée, la hitbodedout guidée.
        souffle = findViewById(R.id.session_souffle)
        souffle.cible = countdown
        souffle.couleur = Apparence.couleur(this, R.color.mint)
        val fond = findViewById<View>(R.id.session_fond)
        calme = PratiqueCalme(this, fond, Apparence.couleur(this, R.color.brand_dark)) {
            listOf(R.id.session_titre, R.id.session_scene, R.id.session_caption, R.id.session_music,
                R.id.session_secondary, R.id.session_theme, R.id.session_chizuk, R.id.session_stuck)
                .map { findViewById(it) }
        }

        // Une rotation, un passage en écran partagé ou le redimensionnement
        // d'une fenêtre recréent l'écran. Sans cela, le guide repartirait au
        // premier temps et le mot de clôture serait tiré une seconde fois.
        savedInstanceState?.let {
            guideStep = it.getInt(ETAT_GUIDE, 0)
            wasRunning = it.getBoolean(ETAT_EN_COURS, false)
            cancelling = it.getBoolean(ETAT_ABANDON, false)
        }

        // « Je n'arrive pas à parler » : le moment « blocage ».
        stuck.setOnClickListener { ChizukCard.show(chizuk, Moment.BLOCAGE) }

        // « ouverture » : le mot du début de séance.
        drawOpening()

        // Une recréation (rotation, thème, langue) rejoue onCreate avec la
        // même intention : la demande de départ n'est lue qu'une fois, sinon
        // une nouvelle séance repartait seule après la fin de la précédente.
        if (savedInstanceState == null) lireDemande(intent)

        // « Sans durée » : la séance court jusqu'à ce qu'on l'arrête. Elle ne
        // touche pas à la durée habituelle, qui reste celle du bouton principal.
        openSession.setOnClickListener {
            SessionService.start(this, SessionRule.OUVERTE)
            guideStep = 0
            drawOpening()
            render()
        }

        intArrayOf(
            R.id.duration_5, R.id.duration_10, R.id.duration_15,
            R.id.duration_30, R.id.duration_60, R.id.duration_90
        ).forEach { id ->
            val button = findViewById<Button>(id)
            val minutes = button.tag.toString().toInt()
            button.text = Store.formatDuration(this, minutes)
            button.setOnClickListener { commencer(minutes) }
        }
        // Un temps choisi, entre 1 et 240 minutes.
        findViewById<Button>(R.id.session_custom).setOnClickListener {
            DureeSeance.saisir(this) { commencer(it) }
        }
    }

    /** L'écran est déjà ouvert (singleTop) : la nouvelle demande est lue aussi. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        lireDemande(intent)
        render()
    }

    /**
     * Venu du bouton « Commencer » d'un rappel, de l'accueil ou de la
     * vignette : la séance part seule, sinon le geste n'aurait fait
     * qu'ouvrir un écran de plus. Les extras sont retirés une fois lus.
     */
    private fun lireDemande(intent: Intent?) {
        intent ?: return
        if (intent.getBooleanExtra(EXTRA_DEPUIS_RAPPEL, false)) {
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
                .cancel(Notifications.ID_REMINDER)
            Reminder.reschedule(this)
        }
        if (intent.getBooleanExtra(EXTRA_START_NOW, false) && !Store.isSessionRunning(this)) {
            SessionService.start(this, Store.targetMinutes(this))
            guideStep = 0
            drawOpening()
        }
        intent.removeExtra(EXTRA_START_NOW)
        intent.removeExtra(EXTRA_DEPUIS_RAPPEL)
    }

    /** Lance une séance de cette durée, qui devient aussi la durée habituelle. */
    private fun commencer(minutes: Int) {
        Store.setTargetMinutes(this, minutes)
        SessionService.start(this, minutes)
        guideStep = 0
        drawOpening()
        render()
    }

    override fun onResume() {
        super.onResume()
        SessionService.reconcilier(this)
        handler.removeCallbacks(tick)
        handler.post(tick)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(ETAT_GUIDE, guideStep)
        outState.putBoolean(ETAT_EN_COURS, wasRunning)
        outState.putBoolean(ETAT_ABANDON, cancelling)
    }

    override fun onPause() {
        handler.removeCallbacks(tick)
        super.onPause()
    }

    override fun onDestroy() {
        calme.liberer()
        super.onDestroy()
    }

    /** Un toucher réveille l'écran calme ; ce premier geste ne touche rien d'autre. */
    override fun dispatchTouchEvent(ev: MotionEvent): Boolean =
        calme.toucher(ev) || super.dispatchTouchEvent(ev)

    private fun render() {
        renderMusicLine()
        renderTheme()

        // « cloture » : au passage de « en cours » à « terminée », que la
        // séance se soit achevée d'elle-même ou par le bouton.
        val running = Store.isSessionRunning(this)
        if (wasRunning && !running) {
            if (cancelling) cancelling = false else ChizukCard.show(chizuk, Moment.CLOTURE)
        }
        wasRunning = running
        souffle.actif = running
        calme.suivre(running)
        // Chaque seconde, et pas au seul passage à « terminée » : c'est le
        // service qui clôt la séance, et il peut le faire quelques secondes
        // après que le décompte a touché zéro ici.
        if (!running) CelebrationActivity.showIfPending(this)

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
                    Store.formatDuration(this, Store.sessionTarget(this))
                )
            }
            durations.visibility = View.GONE
            openSession.visibility = View.GONE
            findViewById<View>(R.id.session_custom).visibility = View.GONE
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
            countdown.text = Store.formatDuration(this, Store.targetMinutes(this))
            // Fait, le grand chiffre reste la durée de la prochaine : la légende dit ce qui a
            // été fait aujourd'hui (13/10 : 4 min faites, l'écran montrait « 5 min »).
            caption.text = if (Store.isDone(this))
                getString(R.string.session_idle_done, Store.formatDuration(this, Store.record(this)?.minutes ?: 0))
            else getString(R.string.session_idle_caption)
            durations.visibility = View.VISIBLE
            openSession.visibility = View.VISIBLE
            findViewById<View>(R.id.session_custom).visibility = View.VISIBLE
            stuck.visibility = View.GONE
            HitbodedutCard.hide(guide)

            primary.visibility = View.VISIBLE
            primary.text = getString(
                R.string.session_start,
                Store.formatDuration(this, Store.targetMinutes(this))
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

    /**
     * Le sujet, et son choix. « Aucun » ouvre la liste : c'est la valeur
     * de depart, et il faut pouvoir y revenir.
     */
    private fun renderTheme() {
        val choisi = Store.sessionTheme(this)
        theme.text = if (choisi == null) {
            getString(R.string.session_theme_none)
        } else {
            getString(R.string.session_theme_chosen, getString(choisi.titre))
        }
    }

    private fun pickTheme() {
        val sujets = Theme.values()
        // « Aucun » en tête : c'est la valeur de départ, et le choix le
        // plus fréquent. L'enfouir sous dix entrées le rendrait difficile
        // à reprendre.
        val libelles = (listOf(getString(R.string.session_theme_none_short)) +
            sujets.map { getString(it.titre) }).toTypedArray()
        AlertDialog.Builder(this)
            .setTitle(R.string.session_theme_title)
            .setItems(libelles) { _, index ->
                Store.setSessionTheme(this, if (index == 0) null else sujets[index - 1])
                renderTheme()
            }
            .show()
    }

    private fun renderMusicLine() {
        // En lecture aléatoire, le morceau qui joue réellement, pas le morceau choisi.
        val choisi = if (Store.musicEnabled(this)) MusicLibrary.selected(this) else null
        val track = if (choisi != null && Store.musicShuffle(this)) {
            MusicLibrary.byId(this, Store.musicNow(this)) ?: choisi
        } else {
            choisi
        }
        musicLine.visibility = if (track == null) View.GONE else View.VISIBLE
        if (track != null) {
            musicLine.text = if (Store.musicShuffle(this) && Store.musicNow(this) == null) {
                getString(R.string.session_music_line, getString(R.string.music_shuffle_short))
            } else {
                getString(R.string.session_music_line, track.title)
            }
        }
    }

    private fun confirmFinish() {
        AlertDialog.Builder(this)
            .setTitle(R.string.session_finish_title)
            .setMessage(R.string.session_finish_message)
            .setPositiveButton(R.string.session_finish_now) { _, _ ->
                // La note est proposée par le bravo, après lui : ouverte ici,
                // elle se retrouvait sous l'écran du bravo (revue du 29/09).
                SessionService.finishNow(this)
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
                CelebrationActivity.annulation(this)
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

    companion object {
        /** Posé par le bouton « Commencer » d'une notification de rappel. */
        const val EXTRA_START_NOW = "start_now"

        /** Posé par le rappel : l'écran retire la notification. */
        const val EXTRA_DEPUIS_RAPPEL = "depuis_rappel"

        private const val ETAT_GUIDE = "guide_step"
        private const val ETAT_EN_COURS = "was_running"
        private const val ETAT_ABANDON = "cancelling"
    }
}
