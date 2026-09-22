package com.myhitbodedut

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

/**
 * L'écran de relecture, réservé aux compilations de test.
 *
 * Les 37 textes de chizuk.json sont tous marqués relu=false : la règle de
 * publication du point 3 les écarte donc d'une release. Cet écran est ce
 * qui permet de lever ce marquage en connaissance de cause — il pose côte
 * à côte l'hébreu affiché, les deux traductions et la référence, sur le
 * téléphone, avec un lien vers la source pour comparer.
 *
 * Rien n'y mène en release : le bouton qui l'ouvre n'apparaît que sous
 * BuildConfig.DEBUG.
 */
class ProofreadActivity : AppCompatActivity() {

    private lateinit var chizuk: Chizuk
    private lateinit var items: List<ChizukItem>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_proofread)

        chizuk = Data.chizuk(this)
        items = chizuk.items

        val relus = items.count { ChizukRules.isPublishable(it) }
        findViewById<TextView>(R.id.proofread_summary).text = getString(
            R.string.proofread_summary,
            relus,
            items.size,
            chizuk.version,
        )

        findViewById<RecyclerView>(R.id.proofread_list).apply {
            layoutManager = LinearLayoutManager(this@ProofreadActivity)
            adapter = Adapter()
        }
    }

    private inner class Adapter : RecyclerView.Adapter<Adapter.Holder>() {

        inner class Holder(view: View) : RecyclerView.ViewHolder(view) {
            val index: TextView = view.findViewById(R.id.proofread_index)
            val state: TextView = view.findViewById(R.id.proofread_state)
            val id: TextView = view.findViewById(R.id.proofread_id)
            val ref: TextView = view.findViewById(R.id.proofread_ref)
            val he: TextView = view.findViewById(R.id.proofread_he)
            val fr: TextView = view.findViewById(R.id.proofread_fr)
            val en: TextView = view.findViewById(R.id.proofread_en)
            val meta: TextView = view.findViewById(R.id.proofread_meta)
            val source: Button = view.findViewById(R.id.proofread_source)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = Holder(
            LayoutInflater.from(parent.context).inflate(R.layout.item_proofread, parent, false)
        )

        override fun getItemCount(): Int = items.size

        override fun onBindViewHolder(holder: Holder, position: Int) {
            val item = items[position]
            holder.index.text = getString(R.string.proofread_index, position + 1, items.size)

            val aRelire = ChizukRules.needsProofreading(item)
            holder.state.setText(
                if (aRelire) R.string.chizuk_badge_to_proofread else R.string.proofread_done
            )
            holder.state.visibility = View.VISIBLE

            holder.id.text = item.id
            holder.ref.text = item.ref

            // Exactement l'hébreu que l'application afficherait, pas l'autre
            // variante : c'est celui-là qu'il s'agit de relire.
            holder.he.text = ChizukRules.hebrew(chizuk, item)
            // L'écran montrait le français ET l'anglais côte à côte, quelle
            // que soit la langue de l'interface. On ne montre plus que celle
            // en cours, et rien du tout en hébreu — l'hébreu est au-dessus.
            val langue = ChizukCard.language(this@ProofreadActivity)
            val traduction = RegleLangue.choisit(langue, mapOf("fr" to item.fr, "en" to item.en))
            holder.fr.text = when (langue) {
                "fr" -> getString(R.string.proofread_line_fr, item.fr)
                "en" -> getString(R.string.proofread_line_en, item.en)
                else -> ""
            }
            holder.fr.visibility = if (traduction.isNullOrBlank()) View.GONE else View.VISIBLE
            holder.en.visibility = View.GONE

            val edition = chizuk.editions[item.edition]
            holder.meta.text = getString(
                R.string.proofread_meta,
                item.moments.joinToString(", ") { it.key },
                edition?.titre ?: item.edition,
                if (edition?.nikkoudOk == true) "he" else "he_sans_nikkoud",
                item.statutVerif,
                item.dureeLectureSec,
            )

            val url = item.url
            holder.source.visibility = if (url.isNullOrBlank()) View.GONE else View.VISIBLE
            holder.source.setOnClickListener {
                runCatching { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
                    .onFailure {
                        Toast.makeText(
                            this@ProofreadActivity,
                            R.string.settings_screen_missing,
                            Toast.LENGTH_LONG,
                        ).show()
                    }
            }
        }
    }
}
