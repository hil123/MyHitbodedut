package com.myhitbodedut

import android.Manifest
import android.app.TimePickerDialog
import android.content.Intent
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.google.android.material.materialswitch.MaterialSwitch

/**
 * Les réglages, et les autorisations rassemblées.
 *
 * Elles étaient dispersées : les notifications se demandaient seules au
 * moment d'activer la barrière et se rappelaient ailleurs, dans l'écran des
 * rappels ; les trois autres vivaient tout au bas de l'écran principal, sous
 * six cartes qui servent tous les jours. On ne cherche une autorisation que
 * lorsque quelque chose ne marche pas, et c'est justement ce moment-là
 * qu'il ne faut pas rendre difficile.
 *
 * Chaque ligne dit ce que l'autorisation est, à quoi elle sert, où elle en
 * est, et l'accorde. La raison y est parce qu'une autorisation qu'on ne
 * comprend pas est une autorisation qu'on refuse.
 */
class SettingsActivity : AppCompatActivity() {

    /**
     * La demande de notification a déjà été présentée. Android cesse
     * d'afficher la boîte après deux refus : il faut alors passer par
     * l'écran système, sinon le bouton ne ferait plus rien.
     */
    private var notificationsDemandees = false

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
        setContentView(R.layout.activity_settings)
        bornerValeurs(window.decorView)
        notificationsDemandees = savedInstanceState?.getBoolean(ETAT_DEMANDE, false) ?: false
        wireActions()
        applyEdition()
        // Le moment « lieu » accompagne les réglages de lieu et de moment :
        // il ne change pas d'un aller-retour à l'autre, on le tire une fois.
        ChizukCard.show(findViewById(R.id.lieu_chizuk), Moment.LIEU)
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(ETAT_DEMANDE, notificationsDemandees)
    }

    // --- Actions ------------------------------------------------------------

    private fun wireActions() {
        findViewById<TextView>(R.id.app_version).text = getString(
            R.string.app_version,
            BuildConfig.VERSION_NAME,
            BuildConfig.VERSION_CODE,
            getString(if (BuildConfig.HAS_GATE) R.string.edition_full else R.string.edition_light)
        )
        findViewById<Button>(R.id.name_button).setOnClickListener { askName() }
        findViewById<Button>(R.id.language_button).setOnClickListener { askLanguage() }
        findViewById<Button>(R.id.adresse_button).setOnClickListener { askAdresse() }
        findViewById<Button>(R.id.onboarding_again).setOnClickListener {
            startActivity(Intent(this, OnboardingActivity::class.java))
        }
        findViewById<Button>(R.id.mode_button).setOnClickListener { askMode() }
        findViewById<Button>(R.id.fond_button).setOnClickListener { askFond() }
        findViewById<Button>(R.id.accent_button).setOnClickListener { askAccent() }
        // Personnalisation (14/10) : police hébraïque, écran d'ouverture, livre de la halakha.
        findViewById<Button>(R.id.police_button).setOnClickListener {
            val polices = PoliceHebraique.Police.values()
            choisirParmi(R.string.settings_police, polices.map { it.libelle }, polices.indexOf(Store.lecturePolice(this))) {
                Store.setLecturePolice(this, polices[it]); renderValues()
            }
        }
        findViewById<Button>(R.id.ouverture_button).setOnClickListener {
            val choix = Store.Ouverture.values()
            choisirParmi(R.string.settings_ouverture, choix.map { it.libelle }, choix.indexOf(Store.ouverture(this))) {
                Store.setOuverture(this, choix[it]); renderValues()
            }
        }
        // Birkat haLevana (14/10) : selon le noussa'h, ou l'un des trois usages.
        findViewById<Button>(R.id.levana_usage_button).setOnClickListener {
            val choix = listOf<Levana.Usage?>(null) + Levana.Usage.values()
            choisirParmi(R.string.levana_usage_titre, choix.map { it?.libelle ?: R.string.levana_usage_auto }, choix.indexOf(Store.levanaUsage(this))) {
                Store.setLevanaUsage(this, choix[it]); renderValues(); LevanaAlerte.programmer(this)
            }
        }
        findViewById<Button>(R.id.halakha_source_button).setOnClickListener {
            val choix = Store.SourceHalakha.values()
            choisirParmi(R.string.halakha_source_titre, choix.map { it.libelle }, choix.indexOf(Store.sourceHalakha(this))) {
                Store.setSourceHalakha(this, choix[it]); renderValues()
            }
        }
        findViewById<Button>(R.id.region_button).setOnClickListener {
            Store.setInIsrael(this, !Store.inIsrael(this))
            refresh()
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
            DureeSeance.choisir(this, R.string.settings_target) {
                Store.setTargetMinutes(this, it)
                refresh()
            }
        }
        findViewById<Button>(R.id.daily_goal).setOnClickListener {
            DureeSeance.choisir(this, R.string.settings_daily_goal) {
                Store.setDailyGoal(this, it)
                refresh()
            }
        }
        findViewById<Button>(R.id.export_data).setOnClickListener {
            exportFile.launch("myhitbodedut-${Store.today()}.json")
        }
        findViewById<Button>(R.id.import_data).setOnClickListener {
            importFile.launch(arrayOf("application/json", "text/plain", "*/*"))
        }
        findViewById<Button>(R.id.reset_app).setOnClickListener { confirmerReinitialisation() }
    }

    /** Tout effacer, après confirmation : séance, barrière, rappels, notifications, puis les données ; on repart de la présentation. */
    private fun confirmerReinitialisation() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.reset_confirm_title)
            .setMessage(R.string.reset_confirm_text)
            .setNegativeButton(R.string.reset_confirm_no, null)
            .setPositiveButton(R.string.reset_confirm_yes) { _, _ ->
                stopService(Intent(this, SessionService::class.java))
                GateService.stop(this)
                Reminder.cancel(this)
                (getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager).cancelAll()
                Store.reinitialiser(this)
                StreakWidget.refresh(this)
                startActivity(Intent(this, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
                finish()
            }
            .show()
    }

    /**
     * Le prénom. Vide est une réponse : l'accueil se tait alors plutôt que
     * de saluer un blanc, et le champ se vide aussi facilement qu'il se
     * remplit.
     */
    private fun askName() {
        val champ = EditText(this).apply {
            setText(Store.firstName(this@SettingsActivity))
            setSingleLine()
            setSelection(text.length)
        }
        AlertDialog.Builder(this)
            .setTitle(R.string.settings_name)
            .setView(champ)
            .setPositiveButton(R.string.action_save) { _, _ ->
                Store.setFirstName(this, champ.text.toString())
                refresh()
            }
            .setNegativeButton(R.string.action_back, null)
            .show()
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
        if (BuildConfig.HAS_GATE) renderDiagnostic()
        renderSwitches()
        renderValues()
        renderPermissions()
    }

    /** Une phrase en tête : ce qui manque, ou que rien ne manque. */
    private fun renderSummary(manquantes: Int) {
        findViewById<TextView>(R.id.settings_summary).text = if (manquantes == 0) {
            getString(R.string.settings_all_granted)
        } else {
            resources.getQuantityString(R.plurals.permissions_missing, manquantes, manquantes)
        }
    }

    /**
     * Une ligne par autorisation que cette édition demande. L'édition légère
     * n'a pas de barrière : les deux autorisations qui la servent ne sont
     * pas masquées, elles ne sont pas posées du tout.
     */
    private fun renderPermissions() {
        val liste = findViewById<LinearLayout>(R.id.permission_list)
        liste.removeAllViews()
        var manquantes = 0
        PermissionRules.demandees(BuildConfig.HAS_GATE).forEach { autorisation ->
            val accordee = Permissions.isGranted(this, autorisation)
            if (!accordee) manquantes++
            val ligne = layoutInflater.inflate(R.layout.item_permission, liste, false)
            ligne.findViewById<TextView>(R.id.permission_title).setText(autorisation.titre)
            ligne.findViewById<TextView>(R.id.permission_why).setText(autorisation.pourquoi)
            ligne.findViewById<TextView>(R.id.permission_state).apply {
                setText(if (accordee) R.string.permission_granted else R.string.permission_missing)
                setTextColor(
                    Apparence.couleur(
                        this@SettingsActivity,
                        if (accordee) R.color.brand else R.color.warning
                    )
                )
            }
            ligne.findViewById<Button>(R.id.permission_grant).apply {
                isEnabled = !accordee
                setOnClickListener { demander(autorisation) }
            }
            liste.addView(ligne)
        }
        renderSummary(manquantes)
    }

    private fun demander(autorisation: Autorisation) = when (autorisation) {
        Autorisation.NOTIFICATIONS -> askNotifications()
        Autorisation.BATTERIE -> safeStart(Permissions.batteryIntent())
        Autorisation.OVERLAY -> safeStart(Permissions.overlayIntent(this))
        Autorisation.USAGE -> safeStart(Permissions.usageAccessIntent())
    }

    private fun renderDiagnostic() {
        val state = GateStatus.evaluate(this)
        val missing = state == GateState.MISSING_PERMISSIONS

        val title = findViewById<TextView>(R.id.gate_state_title)
        title.setText(GateStatus.titleOf(state))
        title.setTextColor(Apparence.couleur(this, GateStatus.colorOf(state)))
        // Le titre cède la place au bouton : le même message, mais actionnable.
        title.visibility = if (missing) View.GONE else View.VISIBLE

        findViewById<TextView>(R.id.gate_state_detail).text = GateStatus.detailOf(this, state)
        findViewById<Button>(R.id.gate_fix_permissions).visibility =
            if (missing) View.VISIBLE else View.GONE
    }

    /** Amène la carte des autorisations sous les yeux, plutôt que « plus bas ». */
    private fun scrollToPermissions() {
        val scroll = findViewById<ScrollView>(R.id.settings_scroll)
        val card = findViewById<View>(R.id.card_permissions)
        scroll.post { scroll.smoothScrollTo(0, card.top) }
    }

    private fun renderSwitches() {
        if (BuildConfig.HAS_GATE) bindSwitch(R.id.enable_switch, Store.isEnabled(this)) { checked ->
            Store.setEnabled(this, checked)
            if (checked) askNotifications()
            applyServiceState()
            refresh()
        }
        // Ne pas déranger (14/10) : sans l'accès d'Android, la case mène à l'écran qui le donne.
        bindSwitch(R.id.ne_pas_deranger_switch, Store.nePasDeranger(this) && NePasDeranger.accesAccorde(this)) { checked ->
            Store.setNePasDeranger(this, checked)
            if (checked && !NePasDeranger.accesAccorde(this)) {
                safeStart(Intent(android.provider.Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
            }
        }
        bindSwitch(R.id.hide_countdown_switch, Store.hideCountdown(this)) { checked ->
            Store.setHideCountdown(this, checked)
        }
        bindSwitch(R.id.guided_switch, Store.guidedHitbodedut(this)) { checked ->
            Store.setGuidedHitbodedut(this, checked)
        }
        bindSwitch(R.id.shabbat_switch, Store.shabbatMode(this)) { checked ->
            Store.setShabbatMode(this, checked)
            refresh()
        }
        // Grand texte (14/10) : les textes de lecture à 140 % ; la barre « Aa » règle toujours finement.
        bindSwitch(R.id.grand_texte_switch, Store.lectureEchelle(this) >= GRAND_TEXTE) { Store.setLectureEchelle(this, if (it) GRAND_TEXTE else 1f) }
        bindSwitch(R.id.levana_switch, Store.levanaAffichee(this)) { Store.setLevanaAffichee(this, it) }
        bindSwitch(R.id.levana_rappel_switch, Store.levanaRappel(this)) { Store.setLevanaRappel(this, it); LevanaAlerte.programmer(this) }
        bindSwitch(R.id.hilloulot_switch, Store.hilloulotAffichees(this)) { Store.setHilloulotAffichees(this, it) }
        bindSwitch(R.id.halakha_switch, Store.halakhaAffichee(this)) { Store.setHalakhaAffichee(this, it) }
        bindSwitch(R.id.exempt_today_switch, Store.pausedDates(this).contains(Store.today())) {
            Store.togglePausedDate(this, Store.today())
            StreakWidget.refresh(this)
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

    private fun renderValues() {
        findViewById<Button>(R.id.window_start).text = Store.formatMinutes(Store.windowStart(this))
        findViewById<Button>(R.id.window_end).text = Store.formatMinutes(Store.windowEnd(this))
        findViewById<Button>(R.id.snooze_length).text =
            Store.formatDuration(this, Store.snoozeMinutes(this))
        findViewById<Button>(R.id.target_length).text =
            Store.formatDuration(this, Store.targetMinutes(this))
        findViewById<Button>(R.id.daily_goal).text =
            Store.formatDuration(this, Store.dailyGoal(this))
        findViewById<Button>(R.id.region_button).setText(
            if (Store.inIsrael(this)) R.string.region_israel else R.string.region_diaspora
        )
        // Le nom de chaque langue est ecrit dans cette langue : « עברית » se
        // lit meme par qui ne lit pas l'hebreu de l'interface qui l'entoure.
        findViewById<Button>(R.id.language_button).text = Langues.nomDe(
            AppCompatDelegate.getApplicationLocales().toLanguageTags()
                .ifBlank { resources.configuration.locales[0].language }
        )
        findViewById<Button>(R.id.adresse_button).setText(
            Store.adresse(this)?.nom ?: R.string.adresse_none
        )
        findViewById<Button>(R.id.mode_button).setText(Store.modeApparence(this).nom)
        findViewById<Button>(R.id.fond_button).setText(Store.fond(this).nom)
        findViewById<Button>(R.id.accent_button).setText(Store.accent(this).nom)
        findViewById<Button>(R.id.police_button).setText(Store.lecturePolice(this).libelle)
        findViewById<Button>(R.id.ouverture_button).setText(Store.ouverture(this).libelle)
        findViewById<Button>(R.id.halakha_source_button).setText(Store.sourceHalakha(this).libelle)
        findViewById<Button>(R.id.levana_usage_button).text = Store.levanaUsage(this)?.let { getString(it.libelle) }
            ?: getString(R.string.levana_usage_auto) + " · " + getString(VueJour.usageLevana(this).libelle)
        val prenom = Store.firstName(this)
        findViewById<Button>(R.id.name_button).text =
            prenom.ifBlank { getString(R.string.settings_name_empty) }

        val count = Store.blockedApps(this).size
        val summary = findViewById<TextView>(R.id.apps_summary)
        summary.text = if (count == 0) {
            getString(R.string.apps_none_warning)
        } else {
            resources.getQuantityString(R.plurals.apps_selected, count, count)
        }
        summary.setTextColor(
            Apparence.couleur(this, if (count == 0) R.color.warning else R.color.muted)
        )
    }

    // --- Service, autorisations, sauvegarde ---------------------------------

    private fun applyServiceState() {
        if (BuildConfig.HAS_GATE) {
            if (GateStatus.shouldRunService(this)) GateService.start(this) else GateService.stop(this)
        }
        Reminder.reschedule(this)
    }

    /**
     * Choisir la langue de l'application, indépendamment de celle du téléphone.
     *
     * `setApplicationLocales` recrée l'activité : la liste est donc refermée
     * avant, et le reste de l'écran se redessine tout seul en revenant.
     */
    private fun askLanguage() {
        val noms = Langues.TOUTES.map { it.nom }.toTypedArray()
        val courante = Langues.indexDe(AppCompatDelegate.getApplicationLocales().toLanguageTags()
            .ifBlank { resources.configuration.locales[0].language })
        AlertDialog.Builder(this)
            .setTitle(R.string.settings_language)
            .setSingleChoiceItems(noms, courante) { dialog, choisi ->
                dialog.dismiss()
                Store.setLangueApp(this, Langues.TOUTES[choisi].etiquette)
                AppCompatDelegate.setApplicationLocales(
                    LocaleListCompat.forLanguageTags(Langues.TOUTES[choisi].etiquette)
                )
            }
            .setNegativeButton(R.string.action_back, null)
            .show()
    }

    /** צדיק, צדקת, ou pas de choix : le bravo reste alors neutre. */
    private fun askAdresse() {
        val choix = listOf<Adresse?>(Adresse.HOMME, Adresse.FEMME, null)
        val noms = choix.map { getString(it?.nom ?: R.string.adresse_none) }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle(R.string.settings_adresse)
            .setSingleChoiceItems(noms, choix.indexOf(Store.adresse(this))) { dialog, choisi ->
                dialog.dismiss()
                Store.setAdresse(this, choix[choisi])
                refresh()
            }
            .setNegativeButton(R.string.action_back, null)
            .show()
    }

    /** AppCompat recrée lui-même les écrans ouverts quand le mode change. */
    private fun askMode() {
        val modes = Apparence.Mode.values()
        choisirParmi(R.string.settings_mode, modes.map { it.nom }, modes.indexOf(Store.modeApparence(this))) {
            Store.setModeApparence(this, modes[it])
            // La vignette n'est pas un écran : elle se redessine à part.
            StreakWidget.refresh(this)
            Apparence.appliquerMode(this)
        }
    }

    /**
     * Le fond et les boutons se posent à la création d'un écran : celui-ci
     * se recrée tout de suite, les autres en reprenant la main. La vignette,
     * dessinée par l'écran d'accueil, est redessinée tout de suite.
     */
    private fun askFond() {
        val fonds = Apparence.Fond.values()
        choisirParmi(R.string.settings_fond, fonds.map { it.nom }, fonds.indexOf(Store.fond(this))) {
            Store.setFond(this, fonds[it])
            StreakWidget.refresh(this)
            recreate()
        }
    }

    private fun askAccent() {
        val accents = Apparence.Accent.values()
        choisirParmi(R.string.settings_accent, accents.map { it.nom }, accents.indexOf(Store.accent(this))) {
            Store.setAccent(this, accents[it])
            StreakWidget.refresh(this)
            recreate()
        }
    }

    private fun choisirParmi(titre: Int, noms: List<Int>, coche: Int, onPicked: (Int) -> Unit) {
        AlertDialog.Builder(this)
            .setTitle(titre)
            .setSingleChoiceItems(noms.map { getString(it) }.toTypedArray(), coche) { dialog, choisi ->
                dialog.dismiss()
                if (choisi != coche) onPicked(choisi)
            }
            .setNegativeButton(R.string.action_back, null)
            .show()
    }

    /** L'édition légère n'embarque pas la barrière : on retire ce qui la concerne. */
    private fun applyEdition() {
        if (BuildConfig.HAS_GATE) return
        intArrayOf(R.id.card_gate_state, R.id.card_gate_settings)
            .forEach { findViewById<View>(it).visibility = View.GONE }
    }

    /**
     * Demande les notifications. Après deux refus, Android n'affiche plus
     * rien : le bouton ouvre alors l'écran système, sinon il resterait mort
     * sans que personne comprenne pourquoi.
     */
    private fun askNotifications() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        if (Permissions.hasNotifications(this)) return
        val refuseeDefinitivement = notificationsDemandees &&
            !shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)
        if (refuseeDefinitivement) {
            safeStart(Permissions.notificationSettingsIntent(this))
            return
        }
        notificationsDemandees = true
        requestNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
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
        // Au plus 2 Mo : une sauvegarde réelle pèse quelques dizaines de Ko,
        // et un fichier énorme restait ensuite relu à chaque ouverture.
        val octets = runCatching {
            contentResolver.openInputStream(uri)?.use { flux ->
                val sortie = java.io.ByteArrayOutputStream()
                val tampon = ByteArray(64 * 1024)
                var total = 0
                while (true) {
                    val lu = flux.read(tampon)
                    if (lu < 0) break
                    total += lu
                    if (total > IMPORT_MAX_OCTETS) return@use null
                    sortie.write(tampon, 0, lu)
                }
                sortie.toByteArray()
            }
        }.getOrNull()
        if (octets == null) {
            Toast.makeText(this, R.string.import_failed, Toast.LENGTH_LONG).show()
            return
        }
        val text = String(octets, Charsets.UTF_8)
        val imported = Store.importJson(this, text)
        if (imported == null) {
            Toast.makeText(this, R.string.import_invalid, Toast.LENGTH_LONG).show()
            return
        }
        // Le clair ou le sombre de la sauvegarde, tout de suite (audit du 04/10).
        Apparence.appliquerMode(this)
        StreakWidget.refresh(this)
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
        val labels = options.map { Store.formatDuration(this, it) }.toTypedArray()
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


    /**
     * Une valeur longue (« Comme le téléphone », texte agrandi) ne doit pas écraser
     * l'intitulé en colonne d'une lettre (captures du 14 et du 15/10) : chaque bouton
     * de valeur posé à côté d'un intitulé prend au plus 45 % de la largeur.
     */
    private fun bornerValeurs(v: android.view.View) {
        if (v !is android.view.ViewGroup) return
        val rangee = v is android.widget.LinearLayout && v.orientation == android.widget.LinearLayout.HORIZONTAL &&
            (0 until v.childCount).any { v.getChildAt(it) is TextView && v.getChildAt(it) !is Button }
        for (i in 0 until v.childCount) {
            val e = v.getChildAt(i)
            if (rangee && e is Button && e.layoutParams.width == android.view.ViewGroup.LayoutParams.WRAP_CONTENT) {
                e.maxWidth = (resources.displayMetrics.widthPixels * 0.45).toInt()
            }
            bornerValeurs(e)
        }
    }

    private companion object {
        const val ETAT_DEMANDE = "notifications_demandees"
        /** Au-delà, ce n'est pas une sauvegarde de cette application. */
        const val IMPORT_MAX_OCTETS = 2 * 1024 * 1024
        /** Grand texte (14/10) : l'échelle des textes de lecture. */
        const val GRAND_TEXTE = 1.4f
    }
}
