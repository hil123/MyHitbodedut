package com.myhitbodedut

import android.content.Context
import org.json.JSONObject

/**
 * Le Tikoun HaKlali : les dix psaumes de Rabbi Na'hman de Breslev, dans
 * l'ordre etabli 16, 32, 41, 42, 59, 77, 90, 105, 137, 150.
 *
 * Le texte vient de l'edition « Tanach with Nikkud » (domaine public), la
 * meme que celle deja citee par chizuk.json. Les noms divins y sont
 * substitues et le qere est retenu, comme le reste de l'application.
 */
data class Verse(val numero: Int, val he: String)

data class Psalm(
    val id: String,
    val numero: Int,
    val ref: String,
    val refSefaria: String?,
    val url: String?,
    val versets: List<Verse>,
)

data class Tikkun(
    val version: String,
    val titre: LocalizedText,
    val edition: Edition,
    val psaumes: List<Psalm>,
) {
    val versets: Int get() = psaumes.sumOf { it.versets.size }

    fun byNumero(numero: Int): Psalm? = psaumes.firstOrNull { it.numero == numero }
}

object TikkunLoader {

    fun parse(json: String): Tikkun {
        val root = JSONObject(json)
        val meta = root.getJSONObject("_meta")
        val ed = meta.getJSONObject("edition")
        return Tikkun(
            version = meta.optString("version", ""),
            titre = LocalizedText.parse(meta.optJSONObject("titre")),
            edition = Edition(
                key = ed.optString("cle", ""),
                titre = ed.optString("titre", ""),
                licence = ed.optString("licence", ""),
                nikkoudOk = ed.optBoolean("nikkoud_ok", false),
            ),
            psaumes = Json.array(root.getJSONArray("psaumes")) { psaume(it) },
        )
    }

    private fun psaume(o: JSONObject) = Psalm(
        id = o.getString("id"),
        numero = o.getInt("numero"),
        ref = o.optString("ref", ""),
        refSefaria = Json.nullable(o, "ref_sefaria"),
        url = Json.nullable(o, "url"),
        versets = Json.array(o.optJSONArray("versets")) {
            Verse(it.getInt("n"), it.optString("he", ""))
        },
    )

    fun load(context: Context): Tikkun = parse(Json.readRaw(context, R.raw.tikkun_haklali))
}
