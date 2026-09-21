package com.myhitbodedut

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

/** Les dix psaumes du Tikoun HaKlali, et ou l'on en est aujourd'hui. */
class TikkunActivity : AppCompatActivity() {

    /**
     * Une ligne de la liste : une partie liturgique, ou l'un des dix psaumes.
     * La liste suit l'ordre reel du Tikoun, ce qui se dit avant et apres
     * compris — sinon rien ne dirait qu'il y a un avant et un apres.
     */
    private sealed interface Ligne {
        data class Bloc(val bloc: LiturgyBlock) : Ligne
        data class Psaume(val psaume: Psalm, val rang: Int) : Ligne
    }

    private lateinit var tikkun: Tikkun
    private lateinit var lignes: List<Ligne>
    private lateinit var adapter: Adapter
    private lateinit var progress: TextView
    private lateinit var primary: Button
    private lateinit var reset: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tikkun)

        tikkun = Data.tikkun(this)
        lignes = tikkun.avant.map { Ligne.Bloc(it) } +
            tikkun.psaumes.mapIndexed { i, p -> Ligne.Psaume(p, i + 1) } +
            tikkun.apres.map { Ligne.Bloc(it) }
        progress = findViewById(R.id.tikkun_progress)
        primary = findViewById(R.id.tikkun_primary)
        reset = findViewById(R.id.tikkun_reset)
        findViewById<ImageView>(R.id.tikkun_art).setImageResource(Character.TEHILIM)

        adapter = Adapter()
        findViewById<RecyclerView>(R.id.tikkun_list).apply {
            layoutManager = LinearLayoutManager(this@TikkunActivity)
            adapter = this@TikkunActivity.adapter
        }

        findViewById<Button>(R.id.tikkun_flow).setOnClickListener {
            startActivity(Intent(this, TikkunFlowActivity::class.java))
        }

        reset.setOnClickListener {
            Store.resetTikkun(this)
            render()
        }
    }

    override fun onResume() {
        super.onResume()
        render()
    }

    private fun render() {
        val dits = Store.tikkunDone(this)
        val total = tikkun.psaumes.size
        progress.text = when (dits.size) {
            0 -> getString(R.string.tikkun_progress_none, total, tikkun.versets)
            total -> getString(R.string.tikkun_progress_all)
            else -> getString(R.string.tikkun_progress, dits.size, total)
        }

        // Le bouton reprend la ou l'on s'est arrete, pas au premier psaume.
        val suivant = tikkun.psaumes.firstOrNull { it.numero !in dits }
        if (suivant == null) {
            primary.visibility = View.GONE
        } else {
            primary.visibility = View.VISIBLE
            primary.text = getString(
                if (dits.isEmpty()) R.string.tikkun_start else R.string.tikkun_resume,
                suivant.numero,
            )
            primary.setOnClickListener { ouvre(suivant.numero) }
        }
        reset.visibility = if (dits.isEmpty()) View.GONE else View.VISIBLE
        adapter.notifyDataSetChanged()
    }

    private fun ouvre(numero: Int) {
        startActivity(
            Intent(this, PsalmActivity::class.java)
                .putExtra(PsalmActivity.EXTRA_NUMERO, numero)
        )
    }

    private inner class Adapter : RecyclerView.Adapter<Adapter.Holder>() {

        inner class Holder(view: View) : RecyclerView.ViewHolder(view) {
            val rank: TextView = view.findViewById(R.id.psalm_rank)
            val title: TextView = view.findViewById(R.id.psalm_title)
            val detail: TextView = view.findViewById(R.id.psalm_detail)
            val state: TextView = view.findViewById(R.id.psalm_state)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = Holder(
            LayoutInflater.from(parent.context).inflate(R.layout.item_psalm, parent, false)
        )

        override fun getItemCount(): Int = lignes.size

        override fun onBindViewHolder(holder: Holder, position: Int) {
            when (val ligne = lignes[position]) {
                is Ligne.Psaume -> bindPsaume(holder, ligne)
                is Ligne.Bloc -> bindBloc(holder, ligne.bloc)
            }
        }

        private fun bindPsaume(holder: Holder, ligne: Ligne.Psaume) {
            val psaume = ligne.psaume
            holder.rank.text = ligne.rang.toString()
            holder.rank.visibility = View.VISIBLE
            holder.title.text = getString(R.string.tikkun_psalm_he, psaume.numero)
            holder.detail.text = getString(R.string.tikkun_verses, psaume.versets.size)
            val dit = psaume.numero in Store.tikkunDone(this@TikkunActivity)
            holder.state.visibility = if (dit) View.VISIBLE else View.GONE
            holder.state.setText(R.string.tikkun_said)
            holder.itemView.setOnClickListener { ouvre(psaume.numero) }
        }

        /**
         * Une partie liturgique n'a pas de rang : elle ne se compte pas dans
         * les dix, et ne se marque pas comme dite.
         */
        private fun bindBloc(holder: Holder, bloc: LiturgyBlock) {
            holder.rank.visibility = View.INVISIBLE
            holder.title.text = bloc.titre.he
            val langue = ChizukCard.language(this@TikkunActivity)
            val traduction = bloc.titre.forLanguage(langue)
            holder.detail.text =
                if (traduction == bloc.titre.he) {
                    getString(R.string.tikkun_paragraphs, bloc.paragraphes.size)
                } else {
                    traduction
                }
            holder.state.visibility = View.GONE
            holder.itemView.setOnClickListener {
                startActivity(
                    Intent(this@TikkunActivity, LiturgyActivity::class.java)
                        .putExtra(LiturgyActivity.EXTRA_CLE, bloc.cle)
                )
            }
        }
    }
}
