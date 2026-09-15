package com.myhitbodedut

import android.annotation.SuppressLint
import android.content.Context
import android.view.KeyEvent
import android.view.LayoutInflater
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView

/**
 * L'écran plein affiché par-dessus une application bloquée.
 * Deux issues seulement : valider la pratique, ou reporter.
 */
@SuppressLint("ViewConstructor")
class BlockerView(
    context: Context,
    private val onDone: () -> Unit,
    private val onSnooze: () -> Unit
) : FrameLayout(context) {

    init {
        LayoutInflater.from(context).inflate(R.layout.view_blocker, this, true)
        isClickable = true
        isFocusable = true
        isFocusableInTouchMode = true

        val streak = Store.streak(context)
        findViewById<TextView>(R.id.blocker_streak).text = if (streak > 0) {
            context.getString(R.string.blocker_streak, streak)
        } else {
            context.getString(R.string.blocker_streak_none)
        }

        findViewById<Button>(R.id.blocker_done).setOnClickListener { onDone() }

        findViewById<Button>(R.id.blocker_snooze).apply {
            text = context.getString(R.string.blocker_snooze, Store.snoozeMinutes(context))
            setOnClickListener { onSnooze() }
        }
    }

    /** Le bouton retour ne doit pas contourner l'écran. */
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode == KeyEvent.KEYCODE_BACK) return true
        return super.dispatchKeyEvent(event)
    }
}
