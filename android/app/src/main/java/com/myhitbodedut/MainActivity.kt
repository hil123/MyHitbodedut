package com.myhitbodedut

import android.Manifest
import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.materialswitch.MaterialSwitch
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Écran unique : état du jour, réglages et accès spéciaux à accorder. */
class MainActivity : AppCompatActivity() {

    private val requestNotifications =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    private lateinit var statusTitle: TextView
    private lateinit var statusStreak: TextView
    private lateinit var weekStrip: LinearLayout
    private lateinit var markButton: Button
    private lateinit var enableSwitch: MaterialSwitch

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusTitle = findViewById(R.id.status_title)
        statusStreak = findViewById(R.id.status_streak)
        weekStrip = findViewById(R.id.week_strip)
        markButton = findViewById(R.id.mark_button)
        enableSwitch = findViewById(R.id.enable_switch)

        markButton.setOnClickListener {
            if (Store.isDone(this)) Store.unmarkToday(this) else Store.markDoneToday(this)
            refresh()
        }

        enableSwitch.setOnCheckedChangeListener { _, checked ->
            Store.setEnabled(this, checked)
            if (checked) askNotifications()
            applyServiceState()
            refresh()
        }

        findViewById<Button>(R.id.grant_overlay).setOnClickListener {
            startActivity(Permissions.overlayIntent(this))
        }
        findViewById<Button>(R.id.grant_usage).setOnClickListener {
            startActivity(Permissions.usageAccessIntent())
        }
        findViewById<Button>(R.id.grant_battery).setOnClickListener {
            startActivity(Permissions.batteryIntent())
        }
        findViewById<Button>(R.id.pick_apps).setOnClickListener {
            startActivity(Intent(this, AppPickerActivity::class.java))
        }
        findViewById<Button>(R.id.window_start).setOnClickListener {
            pickTime(Store.windowStart(this)) { minutes ->
                Store.setWindow(this, minutes, Store.windowEnd(this))
                refresh()
            }
        }
        findViewById<Button>(R.id.window_end).setOnClickListener {
            pickTime(Store.windowEnd(this)) { minutes ->
                Store.setWindow(this, Store.windowStart(this), minutes)
                refresh()
            }
        }
        findViewById<Button>(R.id.snooze_length).setOnClickListener {
            val options = intArrayOf(5, 10, 15, 30, 60)
            val labels = options.map { getString(R.string.minutes_value, it) }.toTypedArray()
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(R.string.settings_snooze)
                .setItems(labels) { _, which ->
                    Store.setSnoozeMinutes(this, options[which])
                    refresh()
                }
                .show()
        }
    }

    override fun onResume() {
        super.onResume()
        applyServiceState()
        refresh()
    }

    // --- Rendu --------------------------------------------------------------

    private fun refresh() {
        val done = Store.isDone(this)
        statusTitle.setText(if (done) R.string.status_done else R.string.status_pending)
        val streak = Store.streak(this)
        statusStreak.text = if (streak > 0) {
            getString(R.string.status_streak, streak)
        } else {
            getString(R.string.status_streak_none)
        }
        markButton.setText(if (done) R.string.action_undo else R.string.action_mark_done)

        enableSwitch.setOnCheckedChangeListener(null)
        enableSwitch.isChecked = Store.isEnabled(this)
        enableSwitch.setOnCheckedChangeListener { _, checked ->
            Store.setEnabled(this, checked)
            if (checked) askNotifications()
            applyServiceState()
            refresh()
        }

        renderPermission(R.id.state_overlay, R.id.grant_overlay, Permissions.canDrawOverlays(this))
        renderPermission(R.id.state_usage, R.id.grant_usage, Permissions.hasUsageAccess(this))
        renderPermission(R.id.state_battery, R.id.grant_battery, Permissions.isBatteryUnrestricted(this))

        findViewById<Button>(R.id.window_start).text = Store.formatMinutes(Store.windowStart(this))
        findViewById<Button>(R.id.window_end).text = Store.formatMinutes(Store.windowEnd(this))
        findViewById<Button>(R.id.snooze_length).text =
            getString(R.string.minutes_value, Store.snoozeMinutes(this))

        val blocked = Store.blockedApps(this).size
        findViewById<TextView>(R.id.apps_summary).text =
            resources.getQuantityString(R.plurals.apps_selected, blocked, blocked)

        renderWeek()
    }

    private fun renderPermission(stateId: Int, buttonId: Int, granted: Boolean) {
        val state = findViewById<TextView>(stateId)
        state.setText(if (granted) R.string.permission_granted else R.string.permission_missing)
        state.setTextColor(
            ContextCompat.getColor(this, if (granted) R.color.brand else R.color.warning)
        )
        findViewById<Button>(buttonId).isEnabled = !granted
    }

    private fun renderWeek() {
        weekStrip.removeAllViews()
        val formatter = DateTimeFormatter.ofPattern("EEE", Locale.getDefault())
        Store.lastWeek(this).forEach { (day, done) ->
            val cell = layoutInflater.inflate(R.layout.item_day, weekStrip, false)
            cell.findViewById<TextView>(R.id.day_label).text = day.format(formatter)
            cell.findViewById<TextView>(R.id.day_mark).text = if (done) "✓" else "·"
            cell.setBackgroundResource(if (done) R.drawable.bg_day_done else R.drawable.bg_day_todo)
            weekStrip.addView(cell)
        }
    }

    // --- Service et permissions --------------------------------------------

    private fun applyServiceState() {
        if (Store.isEnabled(this) && Permissions.allGranted(this)) {
            GateService.start(this)
        } else {
            GateService.stop(this)
        }
    }

    private fun askNotifications() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
        if (granted != PackageManager.PERMISSION_GRANTED) {
            requestNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun pickTime(currentMinutes: Int, onPicked: (Int) -> Unit) {
        TimePickerDialog(
            this,
            { _, hour, minute -> onPicked(hour * 60 + minute) },
            currentMinutes / 60,
            currentMinutes % 60,
            true
        ).show()
    }
}
