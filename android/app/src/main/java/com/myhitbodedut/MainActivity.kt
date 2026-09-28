package com.myhitbodedut

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.materialswitch.MaterialSwitch
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Écran principal : état du jour, diagnostic de la barrière, réglages. */
class MainActivity : AppCompatActivity() {

    /** Le texte de relance a déjà été tiré pour la rupture en cours. */
    private var relanceTiree = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Notifications.ensureChannels(this)
        // Recopie la langue choisie pour les services réveillés seuls (LangueApp).
        LangueApp.retenir(this)
        // Une rotation ou un redimensionnement de fenêtre recrée l'écran :
        // sans cela, le texte de relance serait retiré à chaque fois.
        relanceTiree = savedInstanceState?.getBoolean(ETAT_RELANCE, false) ?: false
        showOnboardingOnce()
        wireActions()
        wireTabs(savedInstanceState?.getInt(ETAT_ONGLET, R.id.nav_today) ?: R.id.nav_today)
        if (intent?.getBooleanExtra(EXTRA_ASK_NOTE, false) == true) {
            NoteDialog.show(this, Store.today()) { refresh() }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(ETAT_RELANCE, relanceTiree)
        outState.putInt(ETAT_ONGLET, findViewById<BottomNavigationView>(R.id.bottom_nav).selectedItemId)
    }

    // --- Onglets ------------------------------------------------------------

    /** Revenir d'« Textes » ramène à « Aujourd'hui » avant de quitter l'application. */
    private val retourAujourdhui = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            findViewById<BottomNavigationView>(R.id.bottom_nav).selectedItemId = R.id.nav_today
        }
    }

    /**
     * Aujourd'hui et Textes changent de panneau sur place. Journal et
     * Réglages ouvrent leurs écrans sans devenir l'onglet actif : au retour,
     * on retrouve le panneau qu'on avait quitté.
     */
    private fun wireTabs(onglet: Int) {
        onBackPressedDispatcher.addCallback(this, retourAujourdhui)
        val nav = findViewById<BottomNavigationView>(R.id.bottom_nav)
        nav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_today, R.id.nav_texts -> {
                    montrerPanneau(item.itemId)
                    true
                }
                R.id.nav_journal -> {
                    startActivity(Intent(this, JournalActivity::class.java))
                    false
                }
                R.id.nav_settings -> {
                    startActivity(Intent(this, SettingsActivity::class.java))
                    false
                }
                else -> false
            }
        }
        nav.selectedItemId = onglet
        montrerPanneau(onglet)
    }

    private fun montrerPanneau(onglet: Int) {
        val textes = onglet == R.id.nav_texts
        findViewById<View>(R.id.main_scroll).visibility = if (textes) View.GONE else View.VISIBLE
        findViewById<View>(R.id.texts_scroll).visibility = if (textes) View.VISIBLE else View.GONE
        retourAujourdhui.isEnabled = textes
    }

    override fun onResume() {
        super.onResume()
        applyServiceState()
        refresh()
        // La séance s'est close téléphone posé : le bravo attendait ici.
        CelebrationActivity.showIfPending(this)
        // La vignette d'accueil suit ce que l'écran vient de montrer.
        StreakWidget.refresh(this)
    }

    /**
     * La présentation, une fois par version d'elle-même, installation
     * existante comprise : c'est là seulement qu'on choisit צדיק / צדקת.
     * Elle se passe d'un geste, et on la revoit depuis les réglages.
     */
    private fun showOnboardingOnce() {
        if (Store.hasOnboarded(this)) return
        startActivity(Intent(this, OnboardingActivity::class.java))
    }

    // --- Actions ------------------------------------------------------------

    private fun wireActions() {
        findViewById<Button>(R.id.start_session).setOnClickListener {
            startActivity(Intent(this, SessionActivity::class.java))
        }
        findViewById<View>(R.id.start_duration).setOnClickListener {
            pickDuration()
        }
        findViewById<View>(R.id.start_theme).setOnClickListener {
            pickTheme()
        }
        findViewById<View>(R.id.start_music).setOnClickListener {
            startActivity(Intent(this, MusicActivity::class.java))
        }
        findViewById<View>(R.id.start_focus).setOnClickListener {
            // La barrière du matin. En édition légère elle n'existe pas,
            // et la ligne est retirée plutôt que rendue inopérante.
            Store.setEnabled(this, !Store.isEnabled(this))
            applyServiceState()
            refresh()
        }
        findViewById<View>(R.id.music_button).setOnClickListener {
            startActivity(Intent(this, MusicActivity::class.java))
        }
        findViewById<View>(R.id.tips_button).setOnClickListener {
            startActivity(Intent(this, TipsActivity::class.java))
        }
        findViewById<View>(R.id.reminders_button).setOnClickListener {
            startActivity(Intent(this, RemindersActivity::class.java))
        }
        findViewById<View>(R.id.tikkun_button).setOnClickListener {
            startActivity(Intent(this, TikkunActivity::class.java))
        }
        findViewById<View>(R.id.tefilot_button).setOnClickListener {
            startActivity(Intent(this, TefilotActivity::class.java))
        }
        findViewById<View>(R.id.library_button).setOnClickListener {
            startActivity(Intent(this, LibraryActivity::class.java))
        }

        // L'écran de relecture n'existe que pour lever les relu=false des
        // données : il n'a rien à faire dans une version publiée.
        findViewById<View>(R.id.proofread_button).apply {
            visibility = if (BuildConfig.DEBUG) View.VISIBLE else View.GONE
            setOnClickListener {
                startActivity(Intent(this@MainActivity, ProofreadActivity::class.java))
            }
        }
        // La plaque ronde coupe l'illustration à son bord, au creux de la jauge.
        findViewById<View>(R.id.hero_plate).clipToOutline = true
        findViewById<Button>(R.id.today_note).setOnClickListener {
            NoteDialog.show(this, Store.today()) { refresh() }
        }
        findViewById<View>(R.id.settings_button).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }

    /**
     * La durée de la prochaine séance. Le même choix que dans les
     * réglages : c'est la même valeur, atteinte de plus près.
     */
    private fun pickDuration() {
        DureeSeance.choisir(this, R.string.settings_target) {
            Store.setTargetMinutes(this, it)
            refresh()
        }
    }

    /** Le sujet de la prochaine séance. « Aucun » en tête : il est facultatif. */
    private fun pickTheme() {
        val sujets = Theme.values()
        val libelles = (listOf(getString(R.string.session_theme_none_short)) +
            sujets.map { getString(it.titre) }).toTypedArray()
        AlertDialog.Builder(this)
            .setTitle(R.string.session_theme_title)
            .setItems(libelles) { _, index ->
                Store.setSessionTheme(this, if (index == 0) null else sujets[index - 1])
                refresh()
            }
            .show()
    }

    // --- Rendu --------------------------------------------------------------

    private fun refresh() {
        renderToday()
        renderWeek()
        renderGauge()
        renderSwitches()
        renderMusic()
        renderRelance()
        renderTikkun()
        renderReminders()
        renderSettingsEntry()
        renderStartCard()
    }

    /**
     * La carte de départ : ce avec quoi on entre dans la séance. Rien n'y
     * est obligatoire — la durée a sa valeur habituelle, le sujet peut
     * rester « aucun », la musique et la concentration peuvent être
     * éteintes.
     */
    private fun renderStartCard() {
        findViewById<TextView>(R.id.start_duration_value).text =
            Store.formatDuration(this, Store.targetMinutes(this))

        val sujet = Store.sessionTheme(this)
        findViewById<TextView>(R.id.start_theme_value).setText(
            sujet?.titre ?: R.string.session_theme_none_short
        )

        val track = if (Store.musicEnabled(this)) MusicLibrary.selected(this) else null
        findViewById<TextView>(R.id.start_music_value).text =
            track?.title ?: getString(R.string.start_music_off)

        val focus = findViewById<View>(R.id.start_focus)
        focus.visibility = if (BuildConfig.HAS_GATE) View.VISIBLE else View.GONE
        if (BuildConfig.HAS_GATE) {
            findViewById<TextView>(R.id.start_focus_value).setText(
                if (Store.isEnabled(this)) R.string.start_focus_on else R.string.start_focus_off
            )
        }
    }

    /**
     * « שלום Avraham » : l'accueil se tait s'il n'a personne à nommer, et
     * l'état du jour prend alors la place du titre.
     */
    private fun renderGreeting() {
        val prenom = Store.firstName(this)
        findViewById<TextView>(R.id.hero_greeting).apply {
            visibility = if (prenom.isBlank()) View.GONE else View.VISIBLE
            if (prenom.isNotBlank()) text = getString(R.string.hero_greeting, prenom)
        }
        findViewById<TextView>(R.id.status_title).apply {
            val titre = prenom.isBlank()
            textSize = if (titre) 24f else 15f
            setTypeface(null, if (titre) Typeface.BOLD else Typeface.NORMAL)
            setTextColor(Apparence.couleur(this@MainActivity, if (titre) R.color.ink else R.color.muted))
        }
    }

    /** La jauge : les jours faits de la semaine, sur ceux qui sont dus. */
    private fun renderGauge() {
        val semaine = JaugeSemaine.progression(Store.currentWeek(this))
        findViewById<JaugeView>(R.id.week_gauge).montrer(semaine.fraction)
        findViewById<TextView>(R.id.week_gauge_label).text = resources.getQuantityString(
            R.plurals.week_gauge, semaine.faits, semaine.faits, semaine.dus
        )
    }

    /**
     * Ce que l'entrée « Réglages » annonce. Ce qui manque doit se voir sans
     * l'ouvrir : sinon personne ne va chercher une autorisation avant que
     * quelque chose ait déjà cessé de marcher.
     */
    private fun renderSettingsEntry() {
        val manquantes = Permissions.missing(this).size
        findViewById<TextView>(R.id.settings_state).apply {
            text = if (manquantes == 0) {
                getString(R.string.settings_all_granted)
            } else {
                resources.getQuantityString(R.plurals.permissions_missing, manquantes, manquantes)
            }
            setTextColor(
                Apparence.couleur(
                    this@MainActivity,
                    if (manquantes == 0) R.color.muted else R.color.warning
                )
            )
        }
        // Le même manque, visible depuis n'importe quel onglet.
        findViewById<BottomNavigationView>(R.id.bottom_nav).apply {
            if (manquantes == 0) {
                removeBadge(R.id.nav_settings)
            } else {
                getOrCreateBadge(R.id.nav_settings).number = manquantes
            }
        }
    }

    /** L'état de la musique, sur la carte qui lui est consacrée. */
    private fun renderMusic() {
        val track = MusicLibrary.selected(this)
        findViewById<TextView>(R.id.music_state).text = when {
            !Store.musicEnabled(this) -> getString(R.string.music_off_summary)
            track == null -> getString(R.string.music_none_summary)
            else -> getString(R.string.music_on_summary, track.title, Store.musicVolume(this))
        }
    }

    /** L'état des rappels, lisible sans ouvrir leur écran. */
    private fun renderReminders() {
        val heures = Store.reminderTimes(this)
        findViewById<TextView>(R.id.reminders_state).text = when {
            !Store.reminderEnabled(this) -> getString(R.string.reminders_off)
            heures.isEmpty() -> getString(R.string.reminders_none)
            else -> resources.getQuantityString(
                R.plurals.reminders_count, heures.size, heures.size
            )
        }
    }

    /** Ou en est le Tikoun du jour, sans avoir a ouvrir la section. */
    private fun renderTikkun() {
        val tikkun = Data.tikkun(this)
        val dits = Store.tikkunDone(this).size
        val total = tikkun.psaumes.size
        findViewById<TextView>(R.id.tikkun_state).text = when (dits) {
            0 -> getString(R.string.tikkun_progress_none, total, tikkun.versets)
            total -> getString(R.string.tikkun_progress_all)
            else -> getString(R.string.tikkun_progress, dits, total)
        }
    }

    /**
     * Le moment « relance » : un mot après une rupture de série.
     *
     * La carte n'apparaît qu'à quelqu'un qui a déjà pratiqué et dont la série
     * est retombée à zéro. Elle n'a rien à dire à un premier jour, ni à une
     * série en cours.
     */
    private fun renderRelance() {
        val card = findViewById<View>(R.id.relance_chizuk)
        val brisee = Store.streak(this) == 0 && Store.days(this).isNotEmpty()
        if (!brisee) {
            card.visibility = View.GONE
            relanceTiree = false
            return
        }
        // refresh() repasse à chaque retour d'écran et après chaque permission
        // accordée : on ne retire pas un texte que l'utilisateur est en train
        // de lire, seulement au premier affichage de la rupture.
        if (!relanceTiree) {
            ChizukCard.show(card, Moment.RELANCE)
            relanceTiree = true
        }
    }

    private fun renderToday() {
        renderGreeting()
        val record = Store.record(this)
        val done = record != null
        findViewById<ImageView>(R.id.hero_character).setImageResource(Character.greeting(done))
        findViewById<TextView>(R.id.status_title)
            .setText(if (done) R.string.status_done else R.string.status_pending)

        val streak = Store.streak(this)
        findViewById<TextView>(R.id.status_streak).text = if (streak > 0) {
            resources.getQuantityString(R.plurals.status_streak, streak, streak)
        } else {
            getString(R.string.status_streak_none)
        }

        val total = Store.totalMinutes(this)
        findViewById<TextView>(R.id.status_total).text =
            getString(R.string.status_total, Store.formatDuration(this, total))

        val minutesToday = record?.minutes ?: 0
        val detail = findViewById<TextView>(R.id.today_detail)
        detail.visibility = if (done) View.VISIBLE else View.GONE
        if (done) {
            detail.text = if (minutesToday > 0) {
                getString(R.string.today_minutes, Store.formatDuration(this, minutesToday))
            } else {
                getString(R.string.today_declared)
            }
        }
        findViewById<Button>(R.id.today_note).visibility = if (done) View.VISIBLE else View.GONE

        findViewById<Button>(R.id.start_session).text = when {
            Store.isSessionRunning(this) -> getString(R.string.action_back_to_session)
            done -> getString(R.string.action_another_session)
            else -> getString(
                R.string.action_start_session,
                Store.formatDuration(this, Store.targetMinutes(this))
            )
        }
    }

    private fun renderWeek() {
        val strip = findViewById<LinearLayout>(R.id.week_strip)
        strip.removeAllViews()
        val formatter = DateTimeFormatter.ofPattern("EEE", Locale.getDefault())
        Store.currentWeek(this).forEach { day ->
            val cell = layoutInflater.inflate(R.layout.item_day, strip, false)
            val label = cell.findViewById<TextView>(R.id.day_label)
            val mark = cell.findViewById<TextView>(R.id.day_mark)
            label.text = day.date.format(formatter)

            // Le samedi chômé ne porte pas de point à cocher : il porte sa marque.
            val restDay = day.shabbat && day.exempt
            mark.text = when {
                day.done -> "✓"
                restDay -> getString(R.string.week_shabbat_mark)
                day.exempt -> "—"
                day.future -> ""
                else -> "·"
            }
            cell.contentDescription = when {
                day.done -> getString(R.string.week_done)
                restDay -> getString(R.string.settings_shabbat)
                day.exempt -> getString(R.string.gate_exempt_title)
                else -> getString(R.string.week_todo)
            }

            // La case du jour fait est vert foncé : un texte sombre y disparaît.
            label.setTextColor(Apparence.couleur(this, if (day.done) R.color.white else R.color.muted))
            mark.setTextColor(
                Apparence.couleur(
                    this,
                    when {
                        day.done -> R.color.white
                        restDay -> R.color.brand
                        else -> R.color.ink
                    }
                )
            )
            cell.setBackgroundResource(
                when {
                    day.done -> R.drawable.bg_day_done
                    day.exempt -> R.drawable.bg_day_exempt
                    else -> R.drawable.bg_day_todo
                }
            )
            strip.addView(cell)
        }
    }

    /** Le seul interrupteur restant ici : la musique, sur sa propre carte. */
    private fun renderSwitches() {
        bindSwitch(R.id.music_switch_main, Store.musicEnabled(this)) { checked ->
            Store.setMusicEnabled(this, checked)
            SessionService.musicChanged(this)
            refresh()
        }
    }

    /** Pose la valeur sans déclencher l'écouteur, puis le réarme. */
    private fun bindSwitch(id: Int, checked: Boolean, onChange: (Boolean) -> Unit) {
        val view = findViewById<MaterialSwitch>(id)
        view.setOnCheckedChangeListener(null)
        view.isChecked = checked
        view.setOnCheckedChangeListener { _, value -> onChange(value) }
    }

    // --- Service, permissions, sauvegarde -----------------------------------

    private fun applyServiceState() {
        if (BuildConfig.HAS_GATE) {
            if (GateStatus.shouldRunService(this)) GateService.start(this) else GateService.stop(this)
        }
        Reminder.reschedule(this)
    }

    // --- Sélecteurs ---------------------------------------------------------

    companion object {
        const val EXTRA_ASK_NOTE = "ask_note"

        /** Survit à une rotation ou au redimensionnement d'une fenêtre. */
        private const val ETAT_RELANCE = "relance_tiree"

        /** L'onglet affiché, pour qu'une rotation ne ramène pas à « Aujourd'hui ». */
        private const val ETAT_ONGLET = "onglet"
    }
}
