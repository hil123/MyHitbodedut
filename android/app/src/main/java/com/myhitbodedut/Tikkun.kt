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
/**
 * Une indication de lecture a l'interieur d'un verset : la ou il faut
 * marquer un arret, et d'ou cette indication vient.
 */
data class Pause(val avant: String, val note: String)

data class Verse(val numero: Int, val he: String, val arret: Pause? = null, val fr: String? = null, val en: String? = null) {
    /**
     * Le verset coupe la ou l'arret se marque : ce qui precede le mot
     * « avant », puis ce qui commence par lui. Null sans arret, ou si le mot
     * n'est pas dans le verset (l'indication suit alors le verset entier).
     */
    fun coupe(): Pair<String, String>? {
        val mot = arret?.avant ?: return null
        val i = he.indexOf(mot)
        if (i <= 0) return null
        return he.substring(0, i).trimEnd() to he.substring(i)
    }
}

/**
 * Une partie liturgique qui n'est pas un psaume : ce qui se dit avant, ce
 * qui se dit apres, la kavana, la priere de Rabbi Natan.
 */
data class LiturgyBlock(
    val cle: String,
    val titre: LocalizedText,
    val note: LocalizedText?,
    val formule: String?,
    val paragraphes: List<String>,
    /** Les traductions, paragraphe par paragraphe (04/11) ; vides sans traduction. */
    val paragraphesFr: List<String> = emptyList(),
    val paragraphesEn: List<String> = emptyList(),
)

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
    val continuite: LocalizedText,
    val psaumes: List<Psalm>,
    val blocs: Map<String, LiturgyBlock>,
) {
    val versets: Int get() = psaumes.sumOf { it.versets.size }

    fun byNumero(numero: Int): Psalm? = psaumes.firstOrNull { it.numero == numero }

    fun bloc(cle: String): LiturgyBlock? = blocs[cle]

    /** Ce qui se dit avant les psaumes, dans l'ordre. */
    val avant: List<LiturgyBlock> get() = listOfNotNull(blocs[OUVERTURE])

    /** Ce qui se dit apres, dans l'ordre. */
    val apres: List<LiturgyBlock>
        get() = listOfNotNull(blocs[CLOTURE], blocs[KAVANA], blocs[PRIERE])

    companion object {
        const val OUVERTURE = "ouverture"
        const val CLOTURE = "cloture"
        const val KAVANA = "kavana"
        const val PRIERE = "priere_rabbi_nathan"

        /** Les dix psaumes, dans l'ordre établi (le journal du Tikoun, 04/11, sans relire le JSON). */
        val ORDRE = listOf(16, 32, 41, 42, 59, 77, 90, 105, 137, 150)
    }
}

object TikkunLoader {

    fun parse(json: String): Tikkun {
        val root = JSONObject(json)
        val meta = root.getJSONObject("_meta")
        val ed = meta.getJSONObject("edition")
        return Tikkun(
            version = meta.optString("version", ""),
            titre = LocalizedText.parse(meta.optJSONObject("titre")),
            continuite = LocalizedText.parse(meta.optJSONObject("continuite")),
            edition = Edition(
                key = ed.optString("cle", ""),
                titre = ed.optString("titre", ""),
                licence = ed.optString("licence", ""),
                nikkoudOk = ed.optBoolean("nikkoud_ok", false),
            ),
            psaumes = Json.array(root.getJSONArray("psaumes")) { psaume(it) },
            blocs = listOf(
                Tikkun.OUVERTURE, Tikkun.CLOTURE, Tikkun.KAVANA, Tikkun.PRIERE,
            ).mapNotNull { cle ->
                root.optJSONObject(cle)?.let { cle to bloc(cle, it) }
            }.toMap(),
        )
    }

    private fun bloc(cle: String, o: JSONObject) = LiturgyBlock(
        cle = cle,
        titre = LocalizedText.parse(o.optJSONObject("titre")),
        note = o.optJSONObject("note")?.let { LocalizedText.parse(it) },
        formule = Json.nullable(o, "formule"),
        paragraphes = Json.strings(o.optJSONArray("paragraphes")),
        paragraphesFr = Json.strings(o.optJSONArray("paragraphes_fr")),
        paragraphesEn = Json.strings(o.optJSONArray("paragraphes_en")),
    )

    private fun psaume(o: JSONObject) = Psalm(
        id = o.getString("id"),
        numero = o.getInt("numero"),
        ref = o.optString("ref", ""),
        refSefaria = Json.nullable(o, "ref_sefaria"),
        url = Json.nullable(o, "url"),
        versets = Json.array(o.optJSONArray("versets")) { v ->
            Verse(
                numero = v.getInt("n"),
                he = v.optString("he", ""),
                arret = v.optJSONObject("arret")?.let {
                    Pause(it.optString("avant", ""), it.optString("note", ""))
                },
                // Les traductions du domaine public (04/11, tools/traductions_tanakh.py).
                fr = v.optString("fr", "").ifBlank { null },
                en = v.optString("en", "").ifBlank { null },
            )
        },
    )

    fun load(context: Context): Tikkun = parse(Json.readRaw(context, R.raw.tikkun_haklali))
}
