package com.myhitbodedut

import android.app.Activity
import android.content.DialogInterface
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog

/** Petite note libre laissée après une séance. */
object NoteDialog {

    /**
     * [onClosed] est appelé à la fermeture, enregistrée ou non.
     *
     * Ce qu'on a écrit ne se perd plus d'un geste (revue du 29/09) : un
     * toucher à côté ne ferme plus la fenêtre, et dès que le texte change,
     * le retour ne la ferme plus non plus ; le bouton dit alors clairement
     * « Ne pas enregistrer » au lieu de « Plus tard ».
     */
    fun show(
        activity: Activity,
        date: String,
        onSaved: () -> Unit,
        onClosed: (() -> Unit)? = null
    ) {
        val density = activity.resources.displayMetrics.density
        val pad = (16 * density).toInt()

        // Le hassid qui écrit : c'est une note qu'on laisse (demande du 29/09).
        val art = ImageView(activity).apply {
            setImageResource(R.drawable.char_writing)
            scaleType = ImageView.ScaleType.FIT_CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (120 * density).toInt()
            )
        }

        val initiale = Store.record(activity, date)?.note.orEmpty()
        val input = EditText(activity).apply {
            setText(initiale)
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

        val dialog = AlertDialog.Builder(activity)
            .setTitle(R.string.note_title)
            .setView(layout)
            .setPositiveButton(R.string.action_save) { _, _ ->
                Store.setNote(activity, date, input.text.toString().trim())
                onSaved()
            }
            .setNegativeButton(R.string.action_later, null)
            .setOnDismissListener { onClosed?.invoke() }
            .create()
        dialog.setCanceledOnTouchOutside(false)
        dialog.show()
        input.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                val change = s.toString().trim() != initiale.trim()
                dialog.setCancelable(!change)
                dialog.getButton(DialogInterface.BUTTON_NEGATIVE)
                    ?.setText(if (change) R.string.note_discard else R.string.action_later)
            }
        })
    }
}
