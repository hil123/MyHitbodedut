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

    private lateinit var tikkun: Tikkun
    private lateinit var adapter: Adapter
    private lateinit var progress: TextView
    private lateinit var primary: Button
    private lateinit var reset: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tikkun)

        tikkun = Data.tikkun(this)
        progress = findViewById(R.id.tikkun_progress)
        primary = findViewById(R.id.tikkun_primary)
        reset = findViewById(R.id.tikkun_reset)
        findViewById<ImageView>(R.id.tikkun_art).setImageResource(Character.TEHILIM)

        adapter = Adapter()
        findViewById<RecyclerView>(R.id.tikkun_list).apply {
            layoutManager = LinearLayoutManager(this@TikkunActivity)
            adapter = this@TikkunActivity.adapter
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

        override fun getItemCount(): Int = tikkun.psaumes.size

        override fun onBindViewHolder(holder: Holder, position: Int) {
            val psaume = tikkun.psaumes[position]
            holder.rank.text = (position + 1).toString()
            holder.title.text = getString(R.string.tikkun_psalm_he, psaume.numero)
            holder.detail.text = getString(R.string.tikkun_verses, psaume.versets.size)
            val dit = psaume.numero in Store.tikkunDone(this@TikkunActivity)
            holder.state.visibility = if (dit) View.VISIBLE else View.GONE
            holder.state.setText(R.string.tikkun_said)
            holder.itemView.setOnClickListener { ouvre(psaume.numero) }
        }
    }
}
