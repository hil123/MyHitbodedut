package com.myhitbodedut

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Historique des séances, avec les notes laissées après chacune. */
class JournalActivity : AppCompatActivity() {

    private val rows = mutableListOf<DayRecord>()
    private lateinit var adapter: Adapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_journal)
        adapter = Adapter()
        findViewById<RecyclerView>(R.id.journal_list).apply {
            layoutManager = LinearLayoutManager(this@JournalActivity)
            adapter = this@JournalActivity.adapter
        }
    }

    override fun onResume() {
        super.onResume()
        reload()
    }

    private fun reload() {
        rows.clear()
        rows.addAll(Store.days(this).values.sortedByDescending { it.date })
        adapter.notifyDataSetChanged()

        val empty = findViewById<View>(R.id.journal_empty)
        empty.visibility = if (rows.isEmpty()) View.VISIBLE else View.GONE

        // Les journées faites seulement : une note seule n'en est pas une.
        val faites = rows.count { it.faite }
        val total = Store.totalMinutes(this)
        findViewById<TextView>(R.id.journal_summary).text = resources.getQuantityString(
            R.plurals.journal_summary,
            faites,
            faites,
            Store.formatDuration(this, total)
        )
    }

    private inner class Adapter : RecyclerView.Adapter<Adapter.Holder>() {

        // Les dates dans la langue de l'application : Locale.getDefault()
        // gardait celle du téléphone, et un journal en anglais affichait
        // « mardi 29 septembre » sur un téléphone réglé en français.
        private val langue: Locale = resources.configuration.locales[0]
        private val formatter = DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", langue)

        inner class Holder(view: View) : RecyclerView.ViewHolder(view) {
            val date: TextView = view.findViewById(R.id.entry_date)
            val duration: TextView = view.findViewById(R.id.entry_duration)
            val note: TextView = view.findViewById(R.id.entry_note)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder =
            Holder(LayoutInflater.from(parent.context).inflate(R.layout.item_journal, parent, false))

        override fun getItemCount(): Int = rows.size

        override fun onBindViewHolder(holder: Holder, position: Int) {
            val row = rows[position]
            // Une date illisible s'affiche telle quelle plutôt que de faire
            // tomber tout l'écran (revue du 29/09).
            holder.date.text = (Store.dateValide(row.date)?.format(formatter) ?: row.date)
                .replaceFirstChar { if (it.isLowerCase()) it.titlecase(langue) else it.toString() }
            holder.duration.text = when {
                row.noteSeule -> getString(R.string.journal_note_only)
                row.minutes > 0 -> Store.formatDuration(holder.itemView.context, row.minutes)
                else -> getString(R.string.today_declared)
            }
            holder.note.visibility = if (row.note.isBlank()) View.GONE else View.VISIBLE
            holder.note.text = row.note
            holder.itemView.setOnClickListener {
                NoteDialog.show(this@JournalActivity, row.date, onSaved = { reload() })
            }
        }
    }
}
