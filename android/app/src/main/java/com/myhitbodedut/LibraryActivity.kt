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

        /** Les livres entiers (12/10) : leur titre de groupe, puis un livre par ligne. */
        data class TitreLivres(val titre: LocalizedText) : Row
        data class Livre(val texte: SiddurTexte) : Row
    }

    /** Les livres entiers (onglet Bibliothèque, 13/10), ou les sources de la hitbodedout. */
    private val livresSeuls: Boolean by lazy { intent.getBooleanExtra(EXTRA_LIVRES, false) }

    private val rows: List<Row> by lazy {
        if (livresSeuls) {
            val livres = Data.livres(this)
            livres.groupes.flatMap { g -> listOf(Row.TitreLivres(g.titre)) + livres.du(g.cle).map { Row.Livre(it) } }
        } else Data.library(this).sections.flatMap { section ->
            listOf(Row.Header(section)) + section.sousSections.map { Row.Sub(it) }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_library)
        findViewById<TextView>(R.id.library_title).setText(if (livresSeuls) R.string.livres_title else R.string.library_title)
        findViewById<TextView>(R.id.library_subtitle).setText(if (livresSeuls) R.string.livres_hint else R.string.library_hint)
        findViewById<ImageView>(R.id.library_art).setImageResource(Character.STUDY)
        findViewById<RecyclerView>(R.id.library_list).apply {
            layoutManager = LinearLayoutManager(this@LibraryActivity)
            adapter = Adapter()
        }
    }

    /** Les marque-pages des livres (28/10), en tête de la liste ; rebâtis au retour d'une lecture. */
    private var signets: View? = null

    override fun onResume() {
        super.onResume()
        if (!livresSeuls) return
        val liste = findViewById<RecyclerView>(R.id.library_list)
        val parent = liste.parent as ViewGroup
        signets?.let { parent.removeView(it) }
        signets = RepriseFavoris.ligneSignets(this, "l")?.also { parent.addView(it, parent.indexOfChild(liste)) }
    }

    private inner class Adapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        override fun getItemCount(): Int = rows.size

        override fun getItemViewType(position: Int): Int =
            if (rows[position] is Row.Header || rows[position] is Row.TitreLivres) TYPE_HEADER else TYPE_SUB

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
                is Row.TitreLivres -> {
                    holder.itemView.findViewById<TextView>(R.id.section_he).text = row.titre.he
                    bindTranslation(holder.itemView.findViewById(R.id.section_translation), row.titre, language)
                }
                is Row.Livre -> bindLivre(holder.itemView, row.texte, language)
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

        /** Un livre entier : son titre, le nombre de chapitres ; il s'ouvre dans le lecteur du siddour. */
        private fun bindLivre(view: View, texte: SiddurTexte, language: String) {
            view.findViewById<TextView>(R.id.subsection_he).text = texte.titre.he
            bindTranslation(view.findViewById(R.id.subsection_translation), texte.titre, language)
            view.findViewById<TextView>(R.id.subsection_count).text =
                resources.getQuantityString(R.plurals.livre_chapitres, texte.sections.size, texte.sections.size)
            view.setOnClickListener { SiddurLectureActivity.ouvrirLivre(this@LibraryActivity, texte.id) }
        }

        /** En hébreu, la traduction répéterait le titre affiché au-dessus. */
        private fun bindTranslation(view: TextView, titre: LocalizedText, language: String) {
            val text = titre.forLanguage(language)
            view.text = text
            view.visibility = if (text == titre.he || text.isBlank()) View.GONE else View.VISIBLE
        }
    }

    companion object {
        /** true : les livres entiers seulement ; sinon les sources de la hitbodedout. */
        const val EXTRA_LIVRES = "livres"
        private const val TYPE_HEADER = 0
        private const val TYPE_SUB = 1
    }
}
