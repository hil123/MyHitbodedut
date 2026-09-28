package com.myhitbodedut

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

/** Les entrées d'une sous-section : les sources d'abord, puis les articles. */
class LibrarySubSectionActivity : AppCompatActivity() {

    private lateinit var entrees: List<LibraryEntry>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_library)

        val sub = Data.library(this).subSection(intent.getStringExtra(EXTRA_ID).orEmpty())
        if (sub == null) {
            finish()
            return
        }
        entrees = LibraryRules.ordered(sub.entrees)

        findViewById<TextView>(R.id.library_title).text = sub.titre.he
        val subtitle = findViewById<TextView>(R.id.library_subtitle)
        val traduction = sub.titre.forLanguage(ChizukCard.language(this))
        subtitle.text = traduction
        subtitle.visibility = if (traduction == sub.titre.he) View.GONE else View.VISIBLE
        findViewById<ImageView>(R.id.library_art).setImageResource(Character.READING)

        findViewById<RecyclerView>(R.id.library_list).apply {
            layoutManager = LinearLayoutManager(this@LibrarySubSectionActivity)
            adapter = Adapter()
        }
    }

    private inner class Adapter : RecyclerView.Adapter<Adapter.Holder>() {

        inner class Holder(view: View) : RecyclerView.ViewHolder(view) {
            val kind: TextView = view.findViewById(R.id.entry_kind)
            val title: TextView = view.findViewById(R.id.entry_title)
            val detail: TextView = view.findViewById(R.id.entry_detail)
            val art: ImageView = view.findViewById(R.id.entry_art)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = Holder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.item_library_entry, parent, false)
        )

        override fun getItemCount(): Int = entrees.size

        override fun onBindViewHolder(holder: Holder, position: Int) {
            val entry = entrees[position]
            holder.art.setImageResource(Character.ofLibraryEntry(entry))
            when (entry) {
                is SourceEntry -> {
                    holder.kind.setText(R.string.library_kind_source)
                    // Le titre d'une source est son nom hébreu : il se lit à droite.
                    holder.title.text = entry.titreHe
                    holder.title.layoutDirection = View.LAYOUT_DIRECTION_RTL
                    holder.title.textAlignment = View.TEXT_ALIGNMENT_VIEW_START
                    holder.detail.text = Reference.localise(holder.itemView.context, entry.ref)
                }

                is ArticleEntry -> {
                    holder.kind.setText(R.string.library_kind_article)
                    holder.title.text = entry.titre
                    holder.title.layoutDirection = View.LAYOUT_DIRECTION_LOCALE
                    holder.title.textAlignment = View.TEXT_ALIGNMENT_GRAVITY
                    holder.detail.text = listOfNotNull(entry.site, entry.date).joinToString(" · ")
                }
            }
            holder.detail.visibility =
                if (holder.detail.text.isNullOrBlank()) View.GONE else View.VISIBLE
            holder.itemView.setOnClickListener {
                startActivity(
                    Intent(this@LibrarySubSectionActivity, LibraryEntryActivity::class.java)
                        .putExtra(LibraryEntryActivity.EXTRA_ID, entry.id)
                )
            }
        }
    }

    companion object {
        const val EXTRA_ID = "sub_section_id"
    }
}
