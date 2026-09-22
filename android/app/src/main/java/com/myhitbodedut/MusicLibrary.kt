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
        Bundled("niggoun_01", R.raw.niggoun_01, R.string.track_niggoun_01),
        Bundled("niggoun_02", R.raw.niggoun_02, R.string.track_niggoun_02),
        Bundled("niggoun_03", R.raw.niggoun_03, R.string.track_niggoun_03),
        Bundled("niggoun_04", R.raw.niggoun_04, R.string.track_niggoun_04),
        Bundled("niggoun_05", R.raw.niggoun_05, R.string.track_niggoun_05),
        Bundled("niggoun_06", R.raw.niggoun_06, R.string.track_niggoun_06),
        Bundled("niggoun_07", R.raw.niggoun_07, R.string.track_niggoun_07),
        Bundled("niggoun_08", R.raw.niggoun_08, R.string.track_niggoun_08),
        Bundled("niggoun_10", R.raw.niggoun_10, R.string.track_niggoun_10),
        Bundled("niggoun_11", R.raw.niggoun_11, R.string.track_niggoun_11),
        Bundled("niggoun_12", R.raw.niggoun_12, R.string.track_niggoun_12),
        Bundled("niggoun_13", R.raw.niggoun_13, R.string.track_niggoun_13),
        Bundled("niggoun_14", R.raw.niggoun_14, R.string.track_niggoun_14),
        Bundled("niggoun_15", R.raw.niggoun_15, R.string.track_niggoun_15),
        Bundled("niggoun_17", R.raw.niggoun_17, R.string.track_niggoun_17),
        Bundled("niggoun_18", R.raw.niggoun_18, R.string.track_niggoun_18),
        Bundled("niggoun_19", R.raw.niggoun_19, R.string.track_niggoun_19),
        Bundled("niggoun_20", R.raw.niggoun_20, R.string.track_niggoun_20),
        Bundled("niggoun_21", R.raw.niggoun_21, R.string.track_niggoun_21),
        Bundled("niggoun_22", R.raw.niggoun_22, R.string.track_niggoun_22),
        Bundled("niggoun_23", R.raw.niggoun_23, R.string.track_niggoun_23),
        Bundled("niggoun_24", R.raw.niggoun_24, R.string.track_niggoun_24),
        Bundled("niggoun_25", R.raw.niggoun_25, R.string.track_niggoun_25),
        Bundled("niggoun_26", R.raw.niggoun_26, R.string.track_niggoun_26),
        Bundled("niggoun_27", R.raw.niggoun_27, R.string.track_niggoun_27),
        Bundled("niggoun_28", R.raw.niggoun_28, R.string.track_niggoun_28),
        Bundled("niggoun_29", R.raw.niggoun_29, R.string.track_niggoun_29),
        Bundled("niggoun_30", R.raw.niggoun_30, R.string.track_niggoun_30),
        Bundled("niggoun_31", R.raw.niggoun_31, R.string.track_niggoun_31),
        Bundled("niggoun_32", R.raw.niggoun_32, R.string.track_niggoun_32),
        Bundled("niggoun_33", R.raw.niggoun_33, R.string.track_niggoun_33),
        Bundled("ribo_sans_titre", R.raw.ribo_sans_titre, R.string.track_ribo_sans_titre),
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

    /** Le nom donne par l'utilisateur, ou null s'il porte encore son titre livre. */
    fun customName(context: Context, id: String): String? = nameOf(context, id)

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
