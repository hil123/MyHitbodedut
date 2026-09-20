package com.myhitbodedut

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.RadioButton
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.materialswitch.MaterialSwitch

/** La bibliothèque de musique, et ce qui sera joué pendant la séance. */
class MusicActivity : AppCompatActivity() {

    private val tracks = mutableListOf<Track>()
    private lateinit var adapter: Adapter
    private var preview: MusicPlayer? = null
    private var previewingId: String? = null

    private val pickAudio =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) addTrack(uri)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_music)

        adapter = Adapter()
        findViewById<RecyclerView>(R.id.music_list).apply {
            layoutManager = LinearLayoutManager(this@MusicActivity)
            adapter = this@MusicActivity.adapter
        }

        findViewById<Button>(R.id.music_add).setOnClickListener {
            pickAudio.launch(arrayOf("audio/*"))
        }

        findViewById<MaterialSwitch>(R.id.music_switch).apply {
            isChecked = Store.musicEnabled(this@MusicActivity)
            setOnCheckedChangeListener { _, checked ->
                Store.setMusicEnabled(this@MusicActivity, checked)
                renderSummary()
            }
        }

        findViewById<SeekBar>(R.id.music_volume).apply {
            max = 100
            progress = Store.musicVolume(this@MusicActivity)
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(bar: SeekBar?, value: Int, fromUser: Boolean) {
                    Store.setMusicVolume(this@MusicActivity, value)
                    preview?.setVolume(value)
                    renderSummary()
                }

                override fun onStartTrackingTouch(bar: SeekBar?) = Unit
                override fun onStopTrackingTouch(bar: SeekBar?) = Unit
            })
        }
    }

    override fun onResume() {
        super.onResume()
        reload()
    }

    override fun onPause() {
        stopPreview()
        super.onPause()
    }

    // --- Données ------------------------------------------------------------

    private fun reload() {
        tracks.clear()
        tracks.addAll(MusicLibrary.all(this))
        adapter.notifyDataSetChanged()
        renderSummary()
    }

    private fun renderSummary() {
        val selected = MusicLibrary.selected(this)
        findViewById<TextView>(R.id.music_summary).text = when {
            !Store.musicEnabled(this) -> getString(R.string.music_off_summary)
            selected == null -> getString(R.string.music_none_summary)
            else -> getString(
                R.string.music_on_summary,
                selected.title,
                Store.musicVolume(this)
            )
        }
    }

    private fun addTrack(uri: Uri) {
        // Sans permission persistante, l'accès serait perdu au prochain démarrage.
        val granted = runCatching {
            contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            true
        }.getOrDefault(false)
        if (!granted) {
            Toast.makeText(this, R.string.music_add_failed, Toast.LENGTH_LONG).show()
            return
        }
        MusicLibrary.addUserTrack(this, uri, displayName(uri))
        Store.setMusicTrackId(this, uri.toString())
        reload()
    }

    private fun displayName(uri: Uri): String {
        val fallback = uri.lastPathSegment?.substringAfterLast('/') ?: getString(R.string.music_unnamed)
        return runCatching {
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0 && cursor.moveToFirst()) {
                    cursor.getString(index)?.substringBeforeLast('.')
                } else {
                    null
                }
            }
        }.getOrNull() ?: fallback
    }

    // --- Écoute d'essai -----------------------------------------------------

    private fun togglePreview(track: Track) {
        if (previewingId == track.id) {
            stopPreview()
            return
        }
        stopPreview()
        val player = MusicPlayer(this)
        val started = player.start(track, Store.musicVolume(this), loop = false)
        if (!started) {
            Toast.makeText(this, R.string.music_play_failed, Toast.LENGTH_LONG).show()
            return
        }
        preview = player
        previewingId = track.id
        adapter.notifyDataSetChanged()
    }

    private fun stopPreview() {
        preview?.stop(fade = false)
        preview = null
        previewingId = null
        adapter.notifyDataSetChanged()
    }

    private fun askRename(track: Track) {
        val input = EditText(this).apply {
            setText(track.title)
            val pad = (16 * resources.displayMetrics.density).toInt()
            setPadding(pad, pad, pad, pad)
        }
        AlertDialog.Builder(this)
            .setTitle(R.string.music_rename)
            .setView(input)
            .setPositiveButton(R.string.action_save) { _, _ ->
                MusicLibrary.rename(this, track.id, input.text.toString().trim())
                reload()
            }
            .setNegativeButton(R.string.action_back, null)
            .apply {
                if (!track.isBundled) {
                    setNeutralButton(R.string.music_remove) { _, _ ->
                        MusicLibrary.removeUserTrack(this@MusicActivity, track.id)
                        reload()
                    }
                }
            }
            .show()
    }

    // --- Liste --------------------------------------------------------------

    private inner class Adapter : RecyclerView.Adapter<Adapter.Holder>() {

        inner class Holder(view: View) : RecyclerView.ViewHolder(view) {
            val radio: RadioButton = view.findViewById(R.id.track_radio)
            val title: TextView = view.findViewById(R.id.track_title)
            val origin: TextView = view.findViewById(R.id.track_origin)
            val play: ImageButton = view.findViewById(R.id.track_play)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder =
            Holder(LayoutInflater.from(parent.context).inflate(R.layout.item_track, parent, false))

        override fun getItemCount(): Int = tracks.size

        override fun onBindViewHolder(holder: Holder, position: Int) {
            val track = tracks[position]
            holder.title.text = track.title
            holder.origin.setText(
                if (track.isBundled) R.string.music_origin_bundled else R.string.music_origin_device
            )
            holder.radio.isChecked = MusicLibrary.selected(this@MusicActivity)?.id == track.id
            holder.play.setImageResource(
                if (previewingId == track.id) android.R.drawable.ic_media_pause
                else android.R.drawable.ic_media_play
            )

            val select = {
                Store.setMusicTrackId(this@MusicActivity, track.id)
                notifyItemRangeChanged(0, tracks.size)
                renderSummary()
            }
            holder.radio.setOnClickListener { select() }
            holder.itemView.setOnClickListener { select() }
            holder.itemView.setOnLongClickListener { askRename(track); true }
            holder.play.setOnClickListener { togglePreview(track) }
        }
    }
}
