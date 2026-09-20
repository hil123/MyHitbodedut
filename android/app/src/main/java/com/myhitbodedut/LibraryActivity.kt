package com.myhitbodedut

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

/**
 * La bibliothèque : ses sections, et les sous-sections où l'on entre.
 *
 * Les données n'en contiennent aujourd'hui qu'une seule de chaque —
 * « שיחת חברים » et « התבודדות מה ולמה » — mais l'écran lit ce qu'il trouve :
 * ajouter une section au fichier suffira.
 */
class LibraryActivity : AppCompatActivity() {

    /** Une ligne de la liste : un titre de section, ou une sous-section. */
    private sealed interface Row {
        data class Header(val section: Section) : Row
        data class Sub(val sub: SubSection) : Row
    }

    private val rows: List<Row> by lazy {
        Data.library(this).sections.flatMap { section ->
            listOf(Row.Header(section)) + section.sousSections.map { Row.Sub(it) }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_library)
        findViewById<TextView>(R.id.library_title).setText(R.string.library_title)
        findViewById<TextView>(R.id.library_subtitle).setText(R.string.library_hint)
        findViewById<RecyclerView>(R.id.library_list).apply {
            layoutManager = LinearLayoutManager(this@LibraryActivity)
            adapter = Adapter()
        }
    }

    private inner class Adapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        override fun getItemCount(): Int = rows.size

        override fun getItemViewType(position: Int): Int =
            if (rows[position] is Row.Header) TYPE_HEADER else TYPE_SUB

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val layout =
                if (viewType == TYPE_HEADER) R.layout.item_library_section
                else R.layout.item_library_subsection
            val view = LayoutInflater.from(parent.context).inflate(layout, parent, false)
            return object : RecyclerView.ViewHolder(view) {}
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val language = ChizukCard.language(this@LibraryActivity)
            when (val row = rows[position]) {
                is Row.Header -> bindHeader(holder.itemView, row.section, language)
                is Row.Sub -> bindSub(holder.itemView, row.sub, language)
            }
        }

        private fun bindHeader(view: View, section: Section, language: String) {
            view.findViewById<TextView>(R.id.section_he).text = section.titre.he
            bindTranslation(
                view.findViewById(R.id.section_translation),
                section.titre,
                language,
            )
        }

        private fun bindSub(view: View, sub: SubSection, language: String) {
            view.findViewById<TextView>(R.id.subsection_he).text = sub.titre.he
            bindTranslation(
                view.findViewById(R.id.subsection_translation),
                sub.titre,
                language,
            )
            view.findViewById<TextView>(R.id.subsection_count).text =
                getString(R.string.library_entries_count, sub.entrees.size)
            view.setOnClickListener {
                startActivity(
                    Intent(this@LibraryActivity, LibrarySubSectionActivity::class.java)
                        .putExtra(LibrarySubSectionActivity.EXTRA_ID, sub.id)
                )
            }
        }

        /** En hébreu, la traduction répéterait le titre affiché au-dessus. */
        private fun bindTranslation(view: TextView, titre: LocalizedText, language: String) {
            val text = titre.forLanguage(language)
            view.text = text
            view.visibility = if (text == titre.he || text.isBlank()) View.GONE else View.VISIBLE
        }
    }

    private companion object {
        const val TYPE_HEADER = 0
        const val TYPE_SUB = 1
    }
}
