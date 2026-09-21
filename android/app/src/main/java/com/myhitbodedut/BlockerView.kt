package com.myhitbodedut

import android.annotation.SuppressLint
import android.content.Context
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView

/** Ce que l'écran de garde propose, selon le moment. */
enum class BlockerMode {
    /** Rien n'a encore été fait aujourd'hui. */
    PROMPT,

    /** Une séance est en cours : la seule issue est d'y retourner. */
    SESSION_RUNNING,

    /** Aperçu déclenché depuis les réglages : aucun effet. */
    PREVIEW
}

/** L'écran plein affiché par-dessus une application bloquée. */
@SuppressLint("ViewConstructor")
class BlockerView(
    context: Context,
    private val mode: BlockerMode,
    private val onStartSession: () -> Unit,
    private val onAlreadyDone: () -> Unit,
    private val onSnooze: () -> Unit,
    private val onDismiss: () -> Unit
) : FrameLayout(context) {

    init {
        LayoutInflater.from(context).inflate(R.layout.view_blocker, this, true)
        isClickable = true
        isFocusable = true
        isFocusableInTouchMode = true

        val title = findViewById<TextView>(R.id.blocker_title)
        val subtitle = findViewById<TextView>(R.id.blocker_subtitle)
        val streakLabel = findViewById<TextView>(R.id.blocker_streak)
        val preview = findViewById<TextView>(R.id.blocker_preview_badge)
        val art = findViewById<ImageView>(R.id.blocker_character)
        val primary = findViewById<Button>(R.id.blocker_primary)
        val secondary = findViewById<Button>(R.id.blocker_secondary)
        val snooze = findViewById<Button>(R.id.blocker_snooze)

        val streak = Store.streak(context)
        streakLabel.text = if (streak > 0) {
            context.resources.getQuantityString(R.plurals.blocker_streak, streak, streak)
        } else {
            context.getString(R.string.blocker_streak_none)
        }

        preview.visibility = if (mode == BlockerMode.PREVIEW) View.VISIBLE else View.GONE

        /*
         * Point 4 : le moment « porte ». L'écran d'une séance en cours ne
         * demande rien et n'en reçoit pas ; l'aperçu en montre un, sans quoi
         * il ne montrerait pas l'écran tel qu'il sera.
         */
        val chizuk = findViewById<View>(R.id.blocker_chizuk)
        if (mode == BlockerMode.SESSION_RUNNING) {
            chizuk.visibility = View.GONE
        } else {
            ChizukCard.show(chizuk, Moment.PORTE)
        }

        when (mode) {
            BlockerMode.PROMPT -> {
                art.setImageResource(Character.call())
                title.setText(R.string.blocker_question)
                subtitle.setText(R.string.blocker_subtitle)
                primary.text = context.getString(
                    R.string.blocker_start,
                    Store.formatDuration(Store.targetMinutes(context))
                )
                primary.setOnClickListener { onStartSession() }
                secondary.visibility = View.VISIBLE
                secondary.setText(R.string.blocker_already_done)
                secondary.setOnClickListener { onAlreadyDone() }
                snooze.visibility = View.VISIBLE
                snooze.text = context.getString(R.string.blocker_snooze, Store.snoozeMinutes(context))
                snooze.setOnClickListener { onSnooze() }
            }

            BlockerMode.SESSION_RUNNING -> {
                art.setImageResource(Character.during())
                title.setText(R.string.blocker_session_title)
                // Une séance ouverte n'a pas de reste à annoncer : elle dit
                // le temps déjà passé.
                subtitle.text = if (Store.isSessionOpen(context)) {
                    context.getString(
                        R.string.blocker_session_open_subtitle,
                        Store.formatDuration(Store.sessionElapsedMinutes(context))
                    )
                } else {
                    val minutes = SessionRule.minutesRestantes(
                        Store.sessionEndsAt(context), System.currentTimeMillis()
                    )
                    context.getString(
                        R.string.blocker_session_subtitle,
                        Store.formatDuration(minutes)
                    )
                }
                primary.setText(R.string.blocker_back_to_session)
                primary.setOnClickListener { onStartSession() }
                secondary.visibility = View.GONE
                snooze.visibility = View.GONE
            }

            BlockerMode.PREVIEW -> {
                art.setImageResource(Character.call())
                title.setText(R.string.blocker_question)
                subtitle.setText(R.string.blocker_subtitle)
                primary.setText(R.string.blocker_close_preview)
                primary.setOnClickListener { onDismiss() }
                secondary.visibility = View.GONE
                snooze.visibility = View.GONE
            }
        }
    }

    /** Le bouton retour ne doit pas contourner l'écran. */
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode == KeyEvent.KEYCODE_BACK) {
            if (mode == BlockerMode.PREVIEW) onDismiss()
            return true
        }
        return super.dispatchKeyEvent(event)
    }
}
