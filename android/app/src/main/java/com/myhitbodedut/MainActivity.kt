package com.myhitbodedut

import android.Manifest
import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.materialswitch.MaterialSwitch
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Écran principal : état du jour, diagnostic de la barrière, réglages. */
class MainActivity : AppCompatActivity() {

    /** Le texte de relance a déjà été tiré pour la rupture en cours. */
    private var relanceTiree = false

    private val requestNotifications =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { refresh() }

    private val exportFile =
        registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
            if (uri != null) writeBackup(uri)
        }

    private val importFile =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) readBackup(uri)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Notifications.ensureChannels(this)
        wireActions()
        applyEdition()
        // Le moment « lieu » accompagne les réglages de lieu et de moment :
        // il ne change pas d'un aller-retour à l'autre, on le tire une fois.
        ChizukCard.show(findViewById(R.id.lieu_chizuk), Moment.LIEU)
        if (intent?.getBooleanExtra(EXTRA_ASK_NOTE, false) == true) {
            NoteDialog.show(this, Store.today()) { refresh() }
        }
    }

    override fun onResume() {
        super.onResume()
        applyServiceState()
        refresh()
    }

    // --- Actions ------------------------------------------------------------

    private fun wireActions() {
        findViewById<Button>(R.id.start_session).setOnClickListener {
            startActivity(Intent(this, SessionActivity::class.java))
        }
        findViewById<Button>(R.id.region_button).setOnClickListener {
            Store.setInIsrael(this, !Store.inIsrael(this))
            refresh()
        }
        findViewById<Button>(R.id.music_button).setOnClickListener {
            startActivity(Intent(this, MusicActivity::class.java))
        }
        findViewById<TextView>(R.id.app_version).text = getString(
            R.string.app_version,
            BuildConfig.VERSION_NAME,
            BuildConfig.VERSION_CODE,
            getString(if (BuildConfig.HAS_GATE) R.string.edition_full else R.string.edition_light)
        )
        findViewById<Button>(R.id.tips_button).setOnClickListener {
            startActivity(Intent(this, TipsActivity::class.java))
        }
        findViewById<Button>(R.id.reminders_button).setOnClickListener {
            startActivity(Intent(this, RemindersActivity::class.java))
        }
        findViewById<Button>(R.id.tikkun_button).setOnClickListener {
            startActivity(Intent(this, TikkunActivity::class.java))
        }
        findViewById<Button>(R.id.library_button).setOnClickListener {
            startActivity(Intent(this, LibraryActivity::class.java))
        }

        // L'écran de relecture n'existe que pour lever les relu=false des
        // données : il n'a rien à faire dans une version publiée.
        findViewById<Button>(R.id.proofread_button).apply {
            visibility = if (BuildConfig.DEBUG) View.VISIBLE else View.GONE
            setOnClickListener {
                startActivity(Intent(this@MainActivity, ProofreadActivity::class.java))
            }
        }
        findViewById<Button>(R.id.open_journal).setOnClickListener {
            startActivity(Intent(this, JournalActivity::class.java))
        }
        findViewById<Button>(R.id.today_note).setOnClickListener {
            NoteDialog.show(this, Store.today()) { refresh() }
        }
        findViewById<Button>(R.id.preview_gate).setOnClickListener { previewGate() }
        findViewById<Button>(R.id.gate_fix_permissions).setOnClickListener { scrollToPermissions() }
        findViewById<Button>(R.id.pick_apps).setOnClickListener {
            startActivity(Intent(this, AppPickerActivity::class.java))
        }
        findViewById<Button>(R.id.window_start).setOnClickListener {
            pickTime(Store.windowStart(this)) {
                Store.setWindow(this, it, Store.windowEnd(this))
                refresh()
            }
        }
        findViewById<Button>(R.id.window_end).setOnClickListener {
            pickTime(Store.windowEnd(this)) {
                Store.setWindow(this, Store.windowStart(this), it)
                refresh()
            }
        }
        findViewById<Button>(R.id.snooze_length).setOnClickListener {
            pickMinutes(R.string.settings_snooze, intArrayOf(5, 10, 15, 30, 60)) {
                Store.setSnoozeMinutes(this, it)
                refresh()
            }
        }
        findViewById<Button>(R.id.target_length).setOnClickListener {
            pickMinutes(R.string.settings_target, intArrayOf(5, 10, 15, 20, 30, 45, 60)) {
                Store.setTargetMinutes(this, it)
                refresh()
            }
        }
        findViewById<Button>(R.id.grant_overlay).setOnClickListener {
            safeStart(Permissions.overlayIntent(this))
        }
        findViewById<Button>(R.id.grant_usage).setOnClickListener {
            safeStart(Permissions.usageAccessIntent())
        }
        findViewById<Button>(R.id.grant_battery).setOnClickListener {
            safeStart(Permissions.batteryIntent())
        }
        findViewById<Button>(R.id.export_data).setOnClickListener {
            exportFile.launch("myhitbodedut-${Store.today()}.json")
        }
        findViewById<Button>(R.id.import_data).setOnClickListener {
            importFile.launch(arrayOf("application/json", "text/plain", "*/*"))
        }
    }

    private fun previewGate() {
        if (!BuildConfig.HAS_GATE) return
        if (!Permissions.canDrawOverlays(this)) {
            Toast.makeText(this, R.string.preview_needs_overlay, Toast.LENGTH_LONG).show()
            safeStart(Permissions.overlayIntent(this))
            return
        }
        GateService.preview(this)
        moveTaskToBack(true)
    }

    // --- Rendu --------------------------------------------------------------

    private fun refresh() {
        renderToday()
        if (BuildConfig.HAS_GATE) renderDiagnostic()
        renderWeek()
        renderSwitches()
        renderSettings()
        renderPermissions()
        renderRelance()
        renderTikkun()
        renderReminders()
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
            getString(R.string.status_total, Store.formatDuration(total))

        val minutesToday = record?.minutes ?: 0
        val detail = findViewById<TextView>(R.id.today_detail)
        detail.visibility = if (done) View.VISIBLE else View.GONE
        if (done) {
            detail.text = if (minutesToday > 0) {
                getString(R.string.today_minutes, Store.formatDuration(minutesToday))
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
                Store.formatDuration(Store.targetMinutes(this))
            )
        }
    }

    private fun renderDiagnostic() {
        val state = GateStatus.evaluate(this)
        val missing = state == GateState.MISSING_PERMISSIONS

        val title = findViewById<TextView>(R.id.gate_state_title)
        title.setText(GateStatus.titleOf(state))
        title.setTextColor(ContextCompat.getColor(this, GateStatus.colorOf(state)))
        // Le titre cède la place au bouton : le même message, mais actionnable.
        title.visibility = if (missing) View.GONE else View.VISIBLE

        findViewById<TextView>(R.id.gate_state_detail).text = GateStatus.detailOf(this, state)
        findViewById<Button>(R.id.gate_fix_permissions).visibility =
            if (missing) View.VISIBLE else View.GONE
    }

    /** Amène la carte des autorisations sous les yeux, plutôt que « plus bas ». */
    private fun scrollToPermissions() {
        val scroll = findViewById<ScrollView>(R.id.main_scroll)
        val card = findViewById<View>(R.id.card_permissions)
        scroll.post { scroll.smoothScrollTo(0, card.top) }
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
            label.setTextColor(ContextCompat.getColor(this, if (day.done) R.color.white else R.color.muted))
            mark.setTextColor(
                ContextCompat.getColor(
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

    private fun renderSwitches() {
        if (BuildConfig.HAS_GATE) bindSwitch(R.id.enable_switch, Store.isEnabled(this)) { checked ->
            Store.setEnabled(this, checked)
            if (checked) askNotifications()
            applyServiceState()
            refresh()
        }
        bindSwitch(R.id.music_switch_main, Store.musicEnabled(this)) { checked ->
            Store.setMusicEnabled(this, checked)
            refresh()
        }
        bindSwitch(R.id.shabbat_switch, Store.shabbatMode(this)) { checked ->
            Store.setShabbatMode(this, checked)
            refresh()
        }
        bindSwitch(R.id.exempt_today_switch, Store.pausedDates(this).contains(Store.today())) {
            Store.togglePausedDate(this, Store.today())
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

    private fun renderSettings() {
        findViewById<Button>(R.id.window_start).text = Store.formatMinutes(Store.windowStart(this))
        findViewById<Button>(R.id.window_end).text = Store.formatMinutes(Store.windowEnd(this))
        findViewById<Button>(R.id.snooze_length).text =
            Store.formatDuration(Store.snoozeMinutes(this))
        findViewById<Button>(R.id.target_length).text =
            Store.formatDuration(Store.targetMinutes(this))
        findViewById<Button>(R.id.region_button).setText(
            if (Store.inIsrael(this)) R.string.region_israel else R.string.region_diaspora
        )

        val track = MusicLibrary.selected(this)
        findViewById<TextView>(R.id.music_state).text = when {
            !Store.musicEnabled(this) -> getString(R.string.music_off_summary)
            track == null -> getString(R.string.music_none_summary)
            else -> getString(R.string.music_on_summary, track.title, Store.musicVolume(this))
        }

        val count = Store.blockedApps(this).size
        val summary = findViewById<TextView>(R.id.apps_summary)
        summary.text = if (count == 0) {
            getString(R.string.apps_none_warning)
        } else {
            resources.getQuantityString(R.plurals.apps_selected, count, count)
        }
        summary.setTextColor(
            ContextCompat.getColor(this, if (count == 0) R.color.warning else R.color.muted)
        )
    }

    private fun renderPermissions() {
        if (BuildConfig.HAS_GATE) {
            renderPermission(R.id.state_overlay, R.id.grant_overlay, Permissions.canDrawOverlays(this))
            renderPermission(R.id.state_usage, R.id.grant_usage, Permissions.hasUsageAccess(this))
        }
        renderPermission(
            R.id.state_battery,
            R.id.grant_battery,
            Permissions.isBatteryUnrestricted(this)
        )
    }

    private fun renderPermission(stateId: Int, buttonId: Int, granted: Boolean) {
        findViewById<TextView>(stateId).apply {
            setText(if (granted) R.string.permission_granted else R.string.permission_missing)
            setTextColor(
                ContextCompat.getColor(this@MainActivity, if (granted) R.color.brand else R.color.warning)
            )
        }
        findViewById<Button>(buttonId).isEnabled = !granted
    }

    // --- Service, permissions, sauvegarde -----------------------------------

    private fun applyServiceState() {
        if (BuildConfig.HAS_GATE) {
            if (GateStatus.shouldRunService(this)) GateService.start(this) else GateService.stop(this)
        }
        Reminder.reschedule(this)
    }

    /** L'édition légère n'embarque pas la barrière : on retire ce qui la concerne. */
    private fun applyEdition() {
        if (BuildConfig.HAS_GATE) return
        intArrayOf(R.id.card_gate_state, R.id.card_gate_settings, R.id.row_overlay, R.id.row_usage)
            .forEach { findViewById<View>(it).visibility = View.GONE }
    }

    private fun askNotifications() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
        if (granted != PackageManager.PERMISSION_GRANTED) {
            requestNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun writeBackup(uri: Uri) {
        val ok = runCatching {
            contentResolver.openOutputStream(uri)?.use { stream ->
                stream.write(Store.exportJson(this).toByteArray())
            } ?: error("flux indisponible")
        }.isSuccess
        Toast.makeText(
            this,
            if (ok) R.string.export_ok else R.string.export_failed,
            Toast.LENGTH_LONG
        ).show()
    }

    private fun readBackup(uri: Uri) {
        val text = runCatching {
            contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
        }.getOrNull()
        if (text == null) {
            Toast.makeText(this, R.string.import_failed, Toast.LENGTH_LONG).show()
            return
        }
        val imported = Store.importJson(this, text)
        if (imported == null) {
            Toast.makeText(this, R.string.import_invalid, Toast.LENGTH_LONG).show()
            return
        }
        Toast.makeText(
            this,
            resources.getQuantityString(R.plurals.import_ok, imported, imported),
            Toast.LENGTH_LONG
        ).show()
        applyServiceState()
        refresh()
    }

    // --- Sélecteurs ---------------------------------------------------------

    private fun pickTime(currentMinutes: Int, onPicked: (Int) -> Unit) {
        TimePickerDialog(
            this,
            { _, hour, minute -> onPicked(hour * 60 + minute) },
            (currentMinutes / 60) % 24,
            currentMinutes % 60,
            true
        ).show()
    }

    private fun pickMinutes(titleId: Int, options: IntArray, onPicked: (Int) -> Unit) {
        val labels = options.map { Store.formatDuration(it) }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle(titleId)
            .setItems(labels) { _, which -> onPicked(options[which]) }
            .show()
    }

    private fun safeStart(intent: Intent) {
        runCatching { startActivity(intent) }.onFailure {
            Toast.makeText(this, R.string.settings_screen_missing, Toast.LENGTH_LONG).show()
        }
    }

    companion object {
        const val EXTRA_ASK_NOTE = "ask_note"
    }
}
