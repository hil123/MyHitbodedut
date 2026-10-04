package com.myhitbodedut

import android.content.Context
import org.json.JSONObject

/**
 * Le siddour (demande du 04/10, d'après la liste de l'application CalJ) :
 * les prières du jour, les bénédictions et les textes de certains moments,
 * en trois nusah au choix. Chaque siddur_<nusah>.json dit la source et la
 * licence de chaque texte (tools/siddur_construction.py).
 */
data class SiddurBloc(val texte: String, val consigne: Boolean)

data class SiddurSection(val titre: String, val blocs: List<SiddurBloc>)

data class SiddurTexte(
    val id: String,
    val groupe: String,
    val titre: LocalizedText,
    val source: String,
    val sections: List<SiddurSection>,
    /** Le nusah d'où vient le texte, quand il manque à celui-ci et a été emprunté. */
    val emprunt: LocalizedText? = null,
)

data class SiddurGroupe(val cle: String, val titre: LocalizedText)

/** Les trois nusah proposés, dans l'ordre du sélecteur. */
enum class Nusah(val cle: String, val libelle: Int) {
    EDOT("edot", R.string.siddur_nusah_edot),
    SEFARD("sefard", R.string.siddur_nusah_sefard),
    ASHKENAZ("ashkenaz", R.string.siddur_nusah_ashkenaz);

    val brut: Int
        get() = when (this) {
            EDOT -> R.raw.siddur_edot
            SEFARD -> R.raw.siddur_sefard
            ASHKENAZ -> R.raw.siddur_ashkenaz
        }

    companion object {
        fun de(cle: String?): Nusah = entries.firstOrNull { it.cle == cle } ?: EDOT
    }
}

class Siddur(
    val groupes: List<SiddurGroupe>,
    val textes: List<SiddurTexte>,
    val nusah: LocalizedText = LocalizedText("", "", ""),
    val sources: List<String> = emptyList(),
) {
    fun texte(id: String): SiddurTexte? = textes.firstOrNull { it.id == id }
    fun du(groupe: String): List<SiddurTexte> = textes.filter { it.groupe == groupe }
}

object SiddurLoader {

    fun parse(json: String): Siddur {
        val racine = JSONObject(json)
        val meta = racine.optJSONObject("_meta")
        return Siddur(
            nusah = LocalizedText.parse(meta?.optJSONObject("nusah")),
            sources = Json.strings(meta?.optJSONArray("sources")),
            groupes = Json.array(racine.getJSONArray("groupes")) {
                SiddurGroupe(it.getString("cle"), LocalizedText.parse(it.getJSONObject("titre")))
            },
            textes = Json.array(racine.getJSONArray("textes")) { t ->
                SiddurTexte(
                    id = t.getString("id"),
                    groupe = t.getString("groupe"),
                    titre = LocalizedText.parse(t.getJSONObject("titre")),
                    source = t.optString("source", ""),
                    emprunt = t.optJSONObject("nusah")?.let { LocalizedText.parse(it) },
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

    fun load(context: Context, nusah: Nusah): Siddur = parse(Json.readRaw(context, nusah.brut))
}
