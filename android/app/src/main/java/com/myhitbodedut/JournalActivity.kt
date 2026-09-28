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

        val total = Store.totalMinutes(this)
        findViewById<TextView>(R.id.journal_summary).text = resources.getQuantityString(
            R.plurals.journal_summary,
            rows.size,
            rows.size,
            Store.formatDuration(this, total)
        )
    }

    private inner class Adapter : RecyclerView.Adapter<Adapter.Holder>() {

        private val formatter = DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.getDefault())

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
            holder.date.text = Store.parse(row.date).format(formatter)
                .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
            holder.duration.text = if (row.minutes > 0) {
                Store.formatDuration(holder.itemView.context, row.minutes)
            } else {
                getString(R.string.today_declared)
            }
            holder.note.visibility = if (row.note.isBlank()) View.GONE else View.VISIBLE
            holder.note.text = row.note
            holder.itemView.setOnClickListener {
                NoteDialog.show(this@JournalActivity, row.date) { reload() }
            }
        }
    }
}
