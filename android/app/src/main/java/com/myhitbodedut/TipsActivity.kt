package com.myhitbodedut

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

/** Les conseils pour la hitbodedout, en lecture seule. */
class TipsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tips)
        findViewById<RecyclerView>(R.id.tips_list).apply {
            layoutManager = LinearLayoutManager(this@TipsActivity)
            adapter = Adapter()
        }
    }

    private inner class Adapter : RecyclerView.Adapter<Adapter.Holder>() {

        inner class Holder(view: View) : RecyclerView.ViewHolder(view) {
            val number: TextView = view.findViewById(R.id.tip_number)
            val title: TextView = view.findViewById(R.id.tip_title)
            val body: TextView = view.findViewById(R.id.tip_body)
            val art: ImageView = view.findViewById(R.id.tip_art)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder =
            Holder(LayoutInflater.from(parent.context).inflate(R.layout.item_tip, parent, false))

        override fun getItemCount(): Int = Tips.ALL.size

        override fun onBindViewHolder(holder: Holder, position: Int) {
            val tip = Tips.ALL[position]
            holder.number.text = tip.number.toString()
            holder.title.setText(tip.titleRes)
            holder.body.setText(tip.bodyRes)
            holder.art.setImageResource(tip.art)
        }
    }
}
