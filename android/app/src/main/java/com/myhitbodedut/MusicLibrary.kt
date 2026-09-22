package com.myhitbodedut

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject

/**
 * Un morceau jouable pendant la séance : soit livré avec l'application,
 * soit choisi par l'utilisateur dans ses fichiers.
 */
data class Track(
    val id: String,
    val title: String,
    val rawRes: Int = 0,
    val uri: Uri? = null
) {
    val isBundled: Boolean get() = rawRes != 0
}

/** La bibliothèque : les morceaux livrés, plus ceux ajoutés depuis le téléphone. */
object MusicLibrary {

    private data class Bundled(val id: String, val rawRes: Int, val titleRes: Int)

    private val BUNDLED = listOf(
        Bundled("hamelech_basade", R.raw.hamelech_basade, R.string.track_hamelech_basade),
        Bundled("yanuka_the_truth_will_rise", R.raw.yanuka_the_truth_will_rise, R.string.track_yanuka_the_truth_will_rise),
        Bundled("yanuka_listening_to_the_heart", R.raw.yanuka_listening_to_the_heart, R.string.track_yanuka_listening_to_the_heart),
        Bundled("yanuka_the_king_will_return", R.raw.yanuka_the_king_will_return, R.string.track_yanuka_the_king_will_return),
        Bundled("yanuka_a_part_of_me", R.raw.yanuka_a_part_of_me, R.string.track_yanuka_a_part_of_me),
        Bundled("yanuka_harping_his_praise", R.raw.yanuka_harping_his_praise, R.string.track_yanuka_harping_his_praise),
        Bundled("yanuka_yearning", R.raw.yanuka_yearning, R.string.track_yanuka_yearning),
        Bundled("yanuka_anticipation", R.raw.yanuka_anticipation, R.string.track_yanuka_anticipation),
        Bundled("yanuka_shlomo_yehuda", R.raw.yanuka_shlomo_yehuda, R.string.track_yanuka_shlomo_yehuda),
        Bundled("yanuka_shlomo_yehuda_2", R.raw.yanuka_shlomo_yehuda_2, R.string.track_yanuka_shlomo_yehuda_2),
        Bundled("yanuka_nigoun_3", R.raw.yanuka_nigoun_3, R.string.track_yanuka_nigoun_3),
    )

    fun all(context: Context): List<Track> = BUNDLED.map { b ->
        Track(b.id, nameOf(context, b.id) ?: context.getString(b.titleRes), rawRes = b.rawRes)
    } + userTracks(context)

    fun byId(context: Context, id: String?): Track? =
        if (id == null) null else all(context).firstOrNull { it.id == id }

    /** Le morceau retenu pour les séances, ou le premier disponible. */
    fun selected(context: Context): Track? =
        byId(context, Store.musicTrackId(context)) ?: all(context).firstOrNull()

    // --- Morceaux ajoutés par l'utilisateur --------------------------------

    private fun userTracks(context: Context): List<Track> {
        val raw = Store.musicUserTracks(context)
        val array = runCatching { JSONArray(raw) }.getOrNull() ?: return emptyList()
        val out = mutableListOf<Track>()
        for (i in 0 until array.length()) {
            val entry = array.optJSONObject(i) ?: continue
            val id = entry.optString("uri")
            if (id.isBlank()) continue
            val stored = entry.optString("title")
            val title = nameOf(context, id) ?: stored.ifBlank { id }
            out.add(Track(id, title, uri = Uri.parse(id)))
        }
        return out
    }

    fun addUserTrack(context: Context, uri: Uri, title: String) {
        val array = runCatching { JSONArray(Store.musicUserTracks(context)) }.getOrNull() ?: JSONArray()
        for (i in 0 until array.length()) {
            if (array.optJSONObject(i)?.optString("uri") == uri.toString()) return
        }
        array.put(JSONObject().put("uri", uri.toString()).put("title", title))
        Store.setMusicUserTracks(context, array.toString())
    }

    fun removeUserTrack(context: Context, id: String) {
        val array = runCatching { JSONArray(Store.musicUserTracks(context)) }.getOrNull() ?: return
        val kept = JSONArray()
        for (i in 0 until array.length()) {
            val entry = array.optJSONObject(i) ?: continue
            if (entry.optString("uri") != id) kept.put(entry)
        }
        Store.setMusicUserTracks(context, kept.toString())
        if (Store.musicTrackId(context) == id) Store.setMusicTrackId(context, null)
    }

    // --- Renommage ----------------------------------------------------------

    private fun nameOf(context: Context, id: String): String? {
        val names = runCatching { JSONObject(Store.musicNames(context)) }.getOrNull() ?: return null
        val value = names.optString(id)
        return value.ifBlank { null }
    }

    fun rename(context: Context, id: String, title: String) {
        val names = runCatching { JSONObject(Store.musicNames(context)) }.getOrNull() ?: JSONObject()
        if (title.isBlank()) names.remove(id) else names.put(id, title)
        Store.setMusicNames(context, names.toString())
    }
}
