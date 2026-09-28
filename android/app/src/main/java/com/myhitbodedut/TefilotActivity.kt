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
 * L'écran Tfilot : les dix sujets, et sous chacun ses prières.
 *
 * L'écran ne contient aucun texte. Il lit la sous-section `tefilot-lt` de la
 * bibliothèque et ouvre l'entrée choisie dans l'écran qui sait déjà afficher
 * une source — celui-là applique la règle de vocalisation, qu'on n'a donc pas
 * à redire ici.
 */
class TefilotActivity : AppCompatActivity() {

    private val lignes: List<TefilotRules.Ligne> by lazy {
        TefilotRules.lignes(Data.library(this))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_library)
        findViewById<TextView>(R.id.library_title).setText(R.string.tefilot_title)
        findViewById<TextView>(R.id.library_subtitle).setText(R.string.tefilot_hint)
        findViewById<ImageView>(R.id.library_art).setImageResource(Character.STUDY)
        findViewById<RecyclerView>(R.id.library_list).apply {
            layoutManager = LinearLayoutManager(this@TefilotActivity)
            adapter = Adapter()
        }
    }

    private inner class Adapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        override fun getItemCount(): Int = lignes.size

        override fun getItemViewType(position: Int): Int =
            if (lignes[position] is TefilotRules.Ligne.Titre) TYPE_SUJET else TYPE_PRIERE

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val layout =
                if (viewType == TYPE_SUJET) R.layout.item_tefila_sujet
                else R.layout.item_library_entry
            val view = LayoutInflater.from(parent.context).inflate(layout, parent, false)
            return object : RecyclerView.ViewHolder(view) {}
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            when (val ligne = lignes[position]) {
                is TefilotRules.Ligne.Titre -> bindSujet(holder.itemView, ligne)
                is TefilotRules.Ligne.Priere -> bindPriere(holder.itemView, ligne)
            }
        }

        private fun bindSujet(view: View, ligne: TefilotRules.Ligne.Titre) {
            view.findViewById<TextView>(R.id.tefila_sujet).setText(ligne.sujet.titre)
            view.findViewById<TextView>(R.id.tefila_sujet_count).text =
                getString(R.string.tefilot_count, ligne.nombre)
        }

        private fun bindPriere(view: View, ligne: TefilotRules.Ligne.Priere) {
            val entree = ligne.entree
            view.findViewById<TextView>(R.id.entry_kind).setText(ligne.sujet.titre)
            view.findViewById<TextView>(R.id.entry_title).text = entree.titreHe
            view.findViewById<TextView>(R.id.entry_detail).text =
                if (TefilotRules.prevenir(entree)) {
                    Reference.localise(view.context, entree.ref) + "\n" + getString(R.string.tefilot_warning)
                } else {
                    Reference.localise(view.context, entree.ref)
                }
            view.findViewById<ImageView>(R.id.entry_art)
                .setImageResource(Character.ofLibraryEntry(entree))
            view.setOnClickListener {
                startActivity(
                    Intent(this@TefilotActivity, LibraryEntryActivity::class.java)
                        .putExtra(LibraryEntryActivity.EXTRA_ID, entree.id)
                )
            }
        }
    }

    private companion object {
        const val TYPE_SUJET = 0
        const val TYPE_PRIERE = 1
    }
}
