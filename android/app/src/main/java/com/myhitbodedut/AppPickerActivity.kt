package com.myhitbodedut

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
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

        findViewById<RecyclerView>(R.id.apps_list).apply {
            layoutManager = LinearLayoutManager(this@AppPickerActivity)
            adapter = this@AppPickerActivity.adapter
        }

        findViewById<Button>(R.id.apps_select_all).setOnClickListener {
            selected.clear()
            entries.forEach { selected.add(it.packageName) }
            adapter.notifyItemRangeChanged(0, entries.size)
            renderCount()
        }
        findViewById<Button>(R.id.apps_clear).setOnClickListener {
            selected.clear()
            adapter.notifyItemRangeChanged(0, entries.size)
            renderCount()
        }

        loadApps()
    }

    override fun onPause() {
        super.onPause()
        Store.setBlockedApps(this, selected)
    }

    private fun renderCount() {
        findViewById<TextView>(R.id.apps_count).text =
            resources.getQuantityString(R.plurals.apps_selected, selected.size, selected.size)
    }

    private fun loadApps() {
        val loading = findViewById<TextView>(R.id.apps_loading)
        Thread {
            val manager = packageManager
            val launcher = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            val found = runCatching {
                manager.queryIntentActivities(launcher, 0)
                    .asSequence()
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
            }.getOrDefault(emptyList())

            runOnUiThread {
                entries.clear()
                entries.addAll(found)
                loading.visibility = View.GONE
                adapter.notifyItemRangeInserted(0, entries.size)
                renderCount()
            }
        }.start()
    }

    private inner class Adapter : RecyclerView.Adapter<Adapter.Holder>() {

        inner class Holder(view: View) : RecyclerView.ViewHolder(view) {
            val icon: ImageView = view.findViewById(R.id.app_icon)
            val label: TextView = view.findViewById(R.id.app_label)
            val check: CheckBox = view.findViewById(R.id.app_check)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder =
            Holder(LayoutInflater.from(parent.context).inflate(R.layout.item_app, parent, false))

        override fun getItemCount(): Int = entries.size

        override fun onBindViewHolder(holder: Holder, position: Int) {
            val entry = entries[position]
            holder.icon.setImageDrawable(entry.icon)
            holder.label.text = entry.label
            holder.check.isChecked = selected.contains(entry.packageName)

            val toggle = {
                if (!selected.remove(entry.packageName)) selected.add(entry.packageName)
                holder.check.isChecked = selected.contains(entry.packageName)
                renderCount()
            }
            holder.check.setOnClickListener { toggle() }
            holder.itemView.setOnClickListener { toggle() }
        }
    }
}
