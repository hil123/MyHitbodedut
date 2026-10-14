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
            filters = arrayOf(android.text.InputFilter.LengthFilter(Store.NOTE_MAX))
            setText(initiale)
            setHint(R.string.note_hint)
            setLines(4)
            gravity = Gravity.TOP or Gravity.START
        }

        // Le journal enrichi (14/10) : l'humeur avant et après, une gratitude.
        val plus = JournalPlus.lire(activity, date)
        var avant = plus.avant
        var apres = plus.apres
        fun humeurs(titre: Int, initiale: Int, choisir: (Int) -> Unit) = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, (8 * density).toInt(), 0, 0)
            addView(android.widget.TextView(activity).apply { setText(titre); textSize = 13f })
            val rang = LinearLayout(activity).apply { orientation = LinearLayout.HORIZONTAL }
            val boutons = JournalPlus.HUMEURS.mapIndexed { i, e ->
                android.widget.TextView(activity).apply {
                    text = e
                    textSize = 26f
                    gravity = Gravity.CENTER
                    layoutParams = LinearLayout.LayoutParams(0, (44 * density).toInt(), 1f)
                    alpha = if (initiale == i + 1) 1f else 0.35f
                    contentDescription = "${i + 1}/5"
                }
            }
            boutons.forEachIndexed { i, b ->
                b.setOnClickListener {
                    val n = if (b.alpha == 1f) 0 else i + 1
                    boutons.forEachIndexed { j, x -> x.alpha = if (n == j + 1) 1f else 0.35f }
                    choisir(n)
                }
                rang.addView(b)
            }
            addView(rang)
        }
        val gratitude = EditText(activity).apply {
            filters = arrayOf(android.text.InputFilter.LengthFilter(JournalPlus.GRATITUDE_MAX))
            setText(plus.gratitude)
            setHint(R.string.journal_gratitude_hint)
            setSingleLine()
        }

        val layout = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad, pad, 0)
            addView(art)
            addView(input)
            addView(humeurs(R.string.journal_humeur_avant, avant) { avant = it })
            addView(humeurs(R.string.journal_humeur_apres, apres) { apres = it })
            addView(gratitude)
        }
        val defile = android.widget.ScrollView(activity).apply { addView(layout) }

        val dialog = AlertDialog.Builder(activity)
            .setTitle(R.string.note_title)
            .setView(defile)
            .setPositiveButton(R.string.action_save) { _, _ ->
                Store.setNote(activity, date, input.text.toString().trim())
                JournalPlus.ecrire(activity, date, JournalPlus.Plus(avant, apres, gratitude.text.toString().trim()))
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
