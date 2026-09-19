package com.myhitbodedut

import android.app.Activity
import android.widget.EditText
import androidx.appcompat.app.AlertDialog

/** Petite note libre laissée après une séance. */
object NoteDialog {

    fun show(activity: Activity, date: String, onSaved: () -> Unit) {
        val input = EditText(activity).apply {
            setText(Store.record(activity, date)?.note.orEmpty())
            setHint(R.string.note_hint)
            setLines(4)
            gravity = android.view.Gravity.TOP or android.view.Gravity.START
            val pad = (16 * resources.displayMetrics.density).toInt()
            setPadding(pad, pad, pad, pad)
        }
        AlertDialog.Builder(activity)
            .setTitle(R.string.note_title)
            .setView(input)
            .setPositiveButton(R.string.action_save) { _, _ ->
                Store.setNote(activity, date, input.text.toString().trim())
                onSaved()
            }
            .setNegativeButton(R.string.action_later, null)
            .show()
    }
}
