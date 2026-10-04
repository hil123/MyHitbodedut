package com.myhitbodedut

import android.content.Context
import org.json.JSONObject

/**
 * Le siddour (demande du 04/10, d'après la liste de l'application CalJ) :
 * les prières du jour, les bénédictions et les textes de certains moments,
 * nusah Edot HaMizrach. siddur.json dit la source et la licence de chacun.
 */
data class SiddurBloc(val texte: String, val consigne: Boolean)

data class SiddurSection(val titre: String, val blocs: List<SiddurBloc>)

data class SiddurTexte(
    val id: String,
    val groupe: String,
    val titre: LocalizedText,
    val source: String,
    val sections: List<SiddurSection>,
)

data class SiddurGroupe(val cle: String, val titre: LocalizedText)

class Siddur(val groupes: List<SiddurGroupe>, val textes: List<SiddurTexte>) {
    fun texte(id: String): SiddurTexte? = textes.firstOrNull { it.id == id }
    fun du(groupe: String): List<SiddurTexte> = textes.filter { it.groupe == groupe }
}

object SiddurLoader {

    fun parse(json: String): Siddur {
        val racine = JSONObject(json)
        return Siddur(
            groupes = Json.array(racine.getJSONArray("groupes")) {
                SiddurGroupe(it.getString("cle"), LocalizedText.parse(it.getJSONObject("titre")))
            },
            textes = Json.array(racine.getJSONArray("textes")) { t ->
                SiddurTexte(
                    id = t.getString("id"),
                    groupe = t.getString("groupe"),
                    titre = LocalizedText.parse(t.getJSONObject("titre")),
                    source = t.optString("source", ""),
                    sections = Json.array(t.getJSONArray("sections")) { s ->
                        SiddurSection(
                            titre = s.optString("titre", ""),
                            blocs = Json.array(s.getJSONArray("blocs")) { b ->
                                if (b.has("c")) SiddurBloc(b.getString("c"), true) else SiddurBloc(b.getString("t"), false)
                            },
                        )
                    },
                )
            },
        )
    }

    fun load(context: Context): Siddur = parse(Json.readRaw(context, R.raw.siddur))
}
