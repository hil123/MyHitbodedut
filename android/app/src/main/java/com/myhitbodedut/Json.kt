package com.myhitbodedut

import android.content.Context
import androidx.annotation.RawRes
import org.json.JSONArray
import org.json.JSONObject

/** Les quelques gestes d'analyse partagés par les deux fichiers de données. */
object Json {

    /** JSON écrit `null` là où une valeur manque ; org.json rend la chaîne "null". */
    fun nullable(o: JSONObject, key: String): String? {
        if (!o.has(key) || o.isNull(key)) return null
        val value = o.optString(key, "")
        return value.ifBlank { null }
    }

    fun strings(array: JSONArray?): List<String> {
        if (array == null) return emptyList()
        return (0 until array.length()).map { array.getString(it) }
    }

    fun <T> array(array: JSONArray?, map: (JSONObject) -> T): List<T> {
        if (array == null) return emptyList()
        return (0 until array.length()).map { map(array.getJSONObject(it)) }
    }

    /** Les deux fichiers décrivent leurs éditions avec la même forme. */
    fun editions(o: JSONObject?): Map<String, Edition> {
        if (o == null) return emptyMap()
        return o.keys().asSequence().associateWith { key ->
            val edition = o.getJSONObject(key)
            Edition(
                key = key,
                titre = edition.optString("titre", ""),
                licence = edition.optString("licence", ""),
                nikkoudOk = edition.optBoolean("nikkoud_ok", false),
            )
        }
    }

    fun readRaw(context: Context, @RawRes id: Int): String =
        context.resources.openRawResource(id).bufferedReader().use { it.readText() }
}

/**
 * Un titre donné en hébreu, français et anglais dans les données.
 *
 * L'hébreu sert de repli : c'est la langue dans laquelle les sections sont
 * nommées, et le seul champ dont on est sûr qu'il est rempli.
 */
data class LocalizedText(val he: String, val fr: String, val en: String) {

    /**
     * Trois langues, et l'hebreu en dernier recours parce que c'est le seul
     * champ dont on est sur qu'il est rempli.
     *
     * Une quatrieme langue ne s'ajoute pas ici seulement : l'espagnol a ete
     * ecrit puis retire le 22/09, et ce qu'il a montre au passage vaut d'etre
     * garde — une langue que ce `when` ne nomme pas est servie en hebreu sans
     * que rien ne le signale.
     */
    fun forLanguage(language: String): String = when (language) {
        "fr" -> fr.ifBlank { he }
        "en" -> en.ifBlank { he }
        else -> he
    }

    companion object {
        fun parse(o: JSONObject?): LocalizedText {
            if (o == null) return LocalizedText("", "", "")
            return LocalizedText(
                he = o.optString("he", ""),
                fr = o.optString("fr", ""),
                en = o.optString("en", ""),
            )
        }
    }
}
