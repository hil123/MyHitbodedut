package com.myhitbodedut

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.WindowManager
import android.widget.Button
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

        intArrayOf(R.id.duration_10, R.id.duration_20, R.id.duration_30, R.id.duration_60)
            .forEach { id ->
                val button = findViewById<Button>(id)
                val minutes = button.tag.toString().toInt()
                button.text = Store.formatDuration(minutes)
                button.setOnClickListener {
                    Store.setTargetMinutes(this, minutes)
                    SessionService.start(this, minutes)
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
        if (Store.isSessionRunning(this)) {
            val remaining = (Store.sessionEndsAt(this) - System.currentTimeMillis()).coerceAtLeast(0)
            val minutes = remaining / 60_000L
            val seconds = (remaining / 1_000L) % 60
            countdown.text = String.format(Locale.US, "%02d:%02d", minutes, seconds)
            caption.text = getString(
                R.string.session_target,
                Store.formatDuration(Store.sessionTarget(this))
            )
            durations.visibility = View.GONE

            primary.visibility = View.VISIBLE
            primary.setText(R.string.session_finish_now)
            primary.setOnClickListener { confirmFinish() }

            secondary.visibility = View.VISIBLE
            secondary.setText(R.string.session_cancel)
            secondary.setOnClickListener { confirmCancel() }
        } else {
            countdown.text = Store.formatDuration(Store.targetMinutes(this))
            caption.setText(
                if (Store.isDone(this)) R.string.session_idle_done else R.string.session_idle_caption
            )
            durations.visibility = View.VISIBLE

            primary.visibility = View.VISIBLE
            primary.text = getString(
                R.string.session_start,
                Store.formatDuration(Store.targetMinutes(this))
            )
            primary.setOnClickListener {
                SessionService.start(this, Store.targetMinutes(this))
                render()
            }
            secondary.visibility = View.GONE
        }
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
                SessionService.cancel(this)
                render()
            }
            .setNegativeButton(R.string.action_back, null)
            .show()
    }

    private fun askNote() {
        NoteDialog.show(this, Store.today()) { render() }
    }
}
