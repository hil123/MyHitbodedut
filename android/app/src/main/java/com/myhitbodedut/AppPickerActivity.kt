package com.myhitbodedut

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

/** Choix des applications que l'écran de garde doit interposer. */
class AppPickerActivity : AppCompatActivity() {

    private data class Entry(val packageName: String, val label: String, val icon: Drawable)

    private val entries = mutableListOf<Entry>()
    private val selected = mutableSetOf<String>()
    private lateinit var adapter: Adapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_apps)

        selected.addAll(Store.blockedApps(this))
        adapter = Adapter()

        val list = findViewById<RecyclerView>(R.id.apps_list)
        list.layoutManager = LinearLayoutManager(this)
        list.adapter = adapter

        loadApps()
    }

    override fun onPause() {
        super.onPause()
        Store.setBlockedApps(this, selected)
    }

    private fun loadApps() {
        val empty = findViewById<TextView>(R.id.apps_loading)
        Thread {
            val manager = packageManager
            val launcher = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            val resolved = manager.queryIntentActivities(launcher, 0)
            val found = resolved.asSequence()
                .map { it.activityInfo.applicationInfo }
                .filter { it.packageName != packageName }
                .distinctBy { it.packageName }
                .map { info: ApplicationInfo ->
                    Entry(
                        info.packageName,
                        manager.getApplicationLabel(info).toString(),
                        manager.getApplicationIcon(info)
                    )
                }
                .sortedBy { it.label.lowercase() }
                .toList()
            runOnUiThread {
                entries.clear()
                entries.addAll(found)
                empty.visibility = View.GONE
                adapter.notifyItemRangeInserted(0, entries.size)
            }
        }.start()
    }

    private inner class Adapter : RecyclerView.Adapter<Adapter.Holder>() {

        inner class Holder(view: View) : RecyclerView.ViewHolder(view) {
            val icon: ImageView = view.findViewById(R.id.app_icon)
            val label: TextView = view.findViewById(R.id.app_label)
            val check: CheckBox = view.findViewById(R.id.app_check)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_app, parent, false)
            return Holder(view)
        }

        override fun getItemCount(): Int = entries.size

        override fun onBindViewHolder(holder: Holder, position: Int) {
            val entry = entries[position]
            holder.icon.setImageDrawable(entry.icon)
            holder.label.text = entry.label
            holder.check.setOnCheckedChangeListener(null)
            holder.check.isChecked = selected.contains(entry.packageName)
            val toggle = {
                if (selected.contains(entry.packageName)) {
                    selected.remove(entry.packageName)
                } else {
                    selected.add(entry.packageName)
                }
                holder.check.isChecked = selected.contains(entry.packageName)
            }
            holder.check.setOnClickListener { toggle() }
            holder.itemView.setOnClickListener { toggle() }
        }
    }
}
