package com.myhitbodedut

import android.content.Context
import org.json.JSONObject

/**
 * La bibliothèque, lue depuis res/raw/bibliotheque.json.
 *
 * Deux natures d'entrée cohabitent et n'obéissent pas aux mêmes règles :
 * une « source » est une œuvre dont le texte intégral est affichable, un
 * « article » est une fiche dont le texte reste chez son auteur.
 */
sealed interface LibraryEntry {
    val id: String
}

/** Œuvre du domaine public : les paragraphes sont affichés en entier. */
data class SourceEntry(
    override val id: String,
    val titreHe: String,
    val ref: String,
    val refSefaria: String?,
    val url: String?,
    val edition: String,
    val paragraphes: List<String>,
    val paragraphesSansNikkoud: List<String>,
    val liensChizuk: List<String>,
    val statutVerif: String,
    /** Le sujet demandé auquel cette entrée répond, quand elle en sert un. */
    val sujet: String?,
    /**
     * La leçon annoncée porte-t-elle sur ce sujet — « oui », « partiel »,
     * « non » ? Reb Noson tire de chaque leçon une prière très large : la
     * demande peut s'y trouver sans que la leçon traite du sujet. Le dire
     * vaut mieux que le laisser croire.
     */
    val leconSurLeSujet: String?,
) : LibraryEntry

/** Article moderne : fiche seulement, sauf autorisation écrite. */
data class ArticleEntry(
    override val id: String,
    val titre: String,
    val auteur: String?,
    val site: String?,
    val date: String?,
    val url: String?,
    /** Résumé, droits et erreurs relevées sont nommés par langue (_fr, _en, _he). */
    val resumes: Map<String, String>,
    val droits: Map<String, String>,
    val autorisation: String?,
    val texteIntegral: String?,
    val sourcesCitees: List<String>,
    val erreursRelevees: Map<String, List<String>>,
) : LibraryEntry

data class SubSection(
    val id: String,
    val titre: LocalizedText,
    val entrees: List<LibraryEntry>,
)

data class Section(
    val id: String,
    val titre: LocalizedText,
    val sousSections: List<SubSection>,
)

data class Library(
    val version: String,
    val editions: Map<String, Edition>,
    val sections: List<Section>,
) {
    val entrees: List<LibraryEntry>
        get() = sections.flatMap { section -> section.sousSections.flatMap { it.entrees } }

    fun subSection(id: String): SubSection? =
        sections.firstNotNullOfOrNull { s -> s.sousSections.firstOrNull { it.id == id } }

    fun entry(id: String): LibraryEntry? = entrees.firstOrNull { it.id == id }
}

object LibraryLoader {

    fun parse(json: String): Library {
        val root = JSONObject(json)
        val meta = root.getJSONObject("_meta")
        return Library(
            version = meta.optString("version", ""),
            editions = Json.editions(meta.optJSONObject("editions")),
            sections = Json.array(root.getJSONArray("sections")) { section(it) },
        )
    }

    private fun section(o: JSONObject) = Section(
        id = o.getString("id"),
        titre = LocalizedText.parse(o.optJSONObject("titre")),
        sousSections = Json.array(o.optJSONArray("sous_sections")) { subSection(it) },
    )

    private fun subSection(o: JSONObject) = SubSection(
        id = o.getString("id"),
        titre = LocalizedText.parse(o.optJSONObject("titre")),
        entrees = Json.array(o.optJSONArray("entrees")) { entry(it) },
    )

    private fun entry(o: JSONObject): LibraryEntry =
        if (o.optString("type") == "source") source(o) else article(o)

    private fun source(o: JSONObject) = SourceEntry(
        id = o.getString("id"),
        titreHe = o.optString("titre_he", ""),
        ref = o.optString("ref", ""),
        refSefaria = Json.nullable(o, "ref_sefaria"),
        url = Json.nullable(o, "url"),
        edition = o.optString("edition", ""),
        paragraphes = Json.strings(o.optJSONArray("paragraphes")),
        paragraphesSansNikkoud = Json.strings(o.optJSONArray("paragraphes_sans_nikkoud")),
        liensChizuk = Json.strings(o.optJSONArray("liens_chizuk")),
        statutVerif = o.optString("statut_verif", ""),
        sujet = Json.nullable(o, "sujet"),
        leconSurLeSujet = Json.nullable(o, "lecon_sur_le_sujet"),
    )

    private fun article(o: JSONObject) = ArticleEntry(
        id = o.getString("id"),
        titre = o.optString("titre", ""),
        auteur = Json.nullable(o, "auteur"),
        site = Json.nullable(o, "site"),
        date = Json.nullable(o, "date"),
        url = Json.nullable(o, "url"),
        resumes = parLangue(o, "resume_"),
        droits = parLangue(o, "droits_"),
        autorisation = Json.nullable(o, "autorisation"),
        texteIntegral = Json.nullable(o, "texte_integral"),
        sourcesCitees = Json.strings(o.optJSONArray("sources_citees")),
        erreursRelevees = listesParLangue(o, "erreurs_relevees_"),
    )

    /**
     * Un champ nommé par langue (resume_fr, droits_en…) : le préfixe fixe,
     * la langue en suffixe. Ajouter une langue n'exige pas de toucher au code.
     */
    private fun parLangue(o: JSONObject, prefixe: String): Map<String, String> =
        o.keys().asSequence()
            .filter { it.startsWith(prefixe) }
            .mapNotNull { key -> Json.nullable(o, key)?.let { key.removePrefix(prefixe) to it } }
            .toMap()

    /** Comme parLangue, mais pour une liste — les erreurs relevées. */
    private fun listesParLangue(o: JSONObject, prefixe: String): Map<String, List<String>> =
        o.keys().asSequence()
            .filter { it.startsWith(prefixe) }
            .associate { key -> key.removePrefix(prefixe) to Json.strings(o.optJSONArray(key)) }
            .filterValues { it.isNotEmpty() }

    fun load(context: Context): Library =
        parse(Json.readRaw(context, R.raw.bibliotheque))
}
