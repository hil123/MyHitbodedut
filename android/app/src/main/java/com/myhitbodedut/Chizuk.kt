package com.myhitbodedut

import android.content.Context
import org.json.JSONObject

/**
 * Les textes de 'hizouk, lus depuis res/raw/chizuk.json.
 *
 * L'analyse est une fonction pure d'une chaîne vers des objets typés : elle
 * tourne telle quelle dans les tests JVM, sans appareil ni émulateur.
 */

/** Édition numérique d'où vient un texte, avec sa licence. */
data class Edition(
    val key: String,
    val titre: String,
    val licence: String,
    val nikkoudOk: Boolean,
)

/** Le moment de la journée, ou de la séance, où un texte est proposé. */
enum class Moment(val key: String) {
    /** Début de séance. */
    OUVERTURE("ouverture"),

    /** Quand les mots ne viennent pas, pendant la séance. */
    BLOCAGE("blocage"),

    /** Écran de porte. */
    PORTE("porte"),

    /** Après une rupture de série. */
    RELANCE("relance"),

    /** Fin de séance. */
    CLOTURE("cloture"),

    /** Réglages lieu et moment. */
    LIEU("lieu");

    /** Le nom affiché en tête de la carte. */
    val labelRes: Int
        get() = when (this) {
            OUVERTURE -> R.string.moment_ouverture
            BLOCAGE -> R.string.moment_blocage
            PORTE -> R.string.moment_porte
            RELANCE -> R.string.moment_relance
            CLOTURE -> R.string.moment_cloture
            LIEU -> R.string.moment_lieu
        }

    companion object {
        fun of(key: String): Moment? = entries.firstOrNull { it.key == key }
    }
}

/** Un texte, avec sa référence, ses traductions et son état de vérification. */
data class ChizukItem(
    val id: String,
    val moments: List<Moment>,
    val theme: String?,
    val edition: String,
    val ref: String,
    val refSefaria: String?,
    val url: String?,
    val he: String,
    val heSansNikkoud: String?,
    val fr: String,
    val en: String,
    val es: String,
    val note: String?,
    val dureeLectureSec: Int,
    val statutVerif: String,
    val relu: Boolean,
)

/** Le fichier entier. */
data class Chizuk(
    val version: String,
    val editions: Map<String, Edition>,
    val items: List<ChizukItem>,
) {
    fun byId(id: String): ChizukItem? = items.firstOrNull { it.id == id }
}

object ChizukLoader {

    fun parse(json: String): Chizuk {
        val root = JSONObject(json)
        val meta = root.getJSONObject("_meta")
        val editions = Json.editions(meta.optJSONObject("editions"))
        val items = Json.array(root.getJSONArray("items")) { item(it) }
        return Chizuk(meta.optString("version", ""), editions, items)
    }

    private fun item(o: JSONObject): ChizukItem = ChizukItem(
        id = o.getString("id"),
        moments = Json.strings(o.optJSONArray("moments")).mapNotNull { Moment.of(it) },
        theme = Json.nullable(o, "theme"),
        edition = o.optString("edition", ""),
        ref = o.optString("ref", ""),
        refSefaria = Json.nullable(o, "ref_sefaria"),
        url = Json.nullable(o, "url"),
        he = o.optString("he", ""),
        heSansNikkoud = Json.nullable(o, "he_sans_nikkoud"),
        fr = o.optString("fr", ""),
        en = o.optString("en", ""),
        es = o.optString("es", ""),
        note = Json.nullable(o, "note"),
        dureeLectureSec = o.optInt("duree_lecture_sec", 0),
        statutVerif = o.optString("statut_verif", ""),
        relu = o.optBoolean("relu", false),
    )

    /** Lit et analyse la ressource embarquée. */
    fun load(context: Context): Chizuk =
        parse(Json.readRaw(context, R.raw.chizuk))
}
