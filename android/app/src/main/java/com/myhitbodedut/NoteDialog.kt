package com.myhitbodedut

import android.app.Activity
import android.view.Gravity
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog

/** Petite note libre laissée après une séance. */
object NoteDialog {

    fun show(activity: Activity, date: String, onSaved: () -> Unit) {
        val density = activity.resources.displayMetrics.density
        val pad = (16 * density).toInt()

        val art = ImageView(activity).apply {
            setImageResource(Character.joy())
            scaleType = ImageView.ScaleType.FIT_CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (120 * density).toInt()
            )
        }

        val input = EditText(activity).apply {
            setText(Store.record(activity, date)?.note.orEmpty())
            setHint(R.string.note_hint)
            setLines(4)
            gravity = Gravity.TOP or Gravity.START
        }

        val layout = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad, pad, 0)
            addView(art)
            addView(input)
        }

        AlertDialog.Builder(activity)
            .setTitle(R.string.note_title)
            .setView(layout)
            .setPositiveButton(R.string.action_save) { _, _ ->
                Store.setNote(activity, date, input.text.toString().trim())
                onSaved()
            }
            .setNegativeButton(R.string.action_later, null)
            .show()
    }
}
