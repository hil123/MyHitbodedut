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

    /**
     * Les seules mentions que les licences imposent (CC-BY, CC-BY-SA), sans
     * le nom de Sefaria (demande du 11/10) ; domaine public et CC0 n'en
     * demandent aucune.
     */
    val mentions: List<String>
        get() = mentionsObligatoires(sources)

    companion object {
        fun mentionsObligatoires(sources: List<String>): List<String> =
            sources.filter { it.contains("CC-BY") }.map { it.replace("Sefaria / ", "").replace("Sefaria, ", "") }
    }
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

/**
 * La bande de signets en tête de la lecture (demande du 10/10) : une
 * section par signet, et celle où l'on lit d'après ce qui est à l'écran.
 * Les débuts et les bornes sont des positions croissantes, lignes de la
 * liste côté Android, pixels côté web : la même règle sert aux deux.
 */
object SignetsSiddur {

    /**
     * L'indice du signet de la section en cours. En haut de la page : la
     * dernière section commencée à la première ligne visible. Tout en bas, où
     * la page ne peut plus monter, une section courte n'atteindrait jamais
     * le haut : c'est alors la dernière commencée à l'écran qui compte.
     */
    fun courant(debuts: List<Int>, premiere: Int, derniere: Int, enBas: Boolean): Int {
        val borne = if (enBas) derniere else premiere
        return debuts.indexOfLast { it <= borne }.coerceAtLeast(0)
    }
}

/**
 * Le texte que l'heure suggère, en tête de la liste du siddour : une
 * indication d'après l'horloge, non un horaire de zmanim. Les heures sont
 * larges exprès ; la liste entière reste juste en dessous.
 */
object SiddurMoment {

    fun suggestion(heure: java.time.LocalTime): String {
        val minutes = heure.hour * 60 + heure.minute
        return when {
            minutes < 5 * 60 -> "tikoun_hatsot"
            minutes < 6 * 60 + 30 -> "birkot_hachahar"
            minutes < 12 * 60 + 30 -> "chaharit"
            minutes < 18 * 60 + 30 -> "minha"
            minutes < 22 * 60 + 30 -> "arvit"
            else -> "chema_coucher"
        }
    }

    /** Le hassid qui accompagne la suggestion, parmi les illustrations de l'application. */
    fun illustration(id: String): Int = when (id) {
        "tikoun_hatsot" -> R.drawable.char_praying
        "birkot_hachahar" -> R.drawable.char_welcome
        // Les tefillin et le lit (planche du 11/10).
        "chaharit" -> R.drawable.char_tefillin
        "chema_coucher" -> R.drawable.scene_sleeping
        // L'aéroport, décor gardé (demande du 11/10).
        "tefilat_haderekh" -> R.drawable.scene_airport
        "minha" -> R.drawable.char_standing_back
        "arvit" -> R.drawable.char_reading_seated
        else -> R.drawable.char_thinking
    }
}
