package com.myhitbodedut

import org.json.JSONObject

/**
 * Ce que le jour change à la lecture du siddour (demande du 10/10) : les
 * ajouts du jour mis en évidence, les autres repliés derrière une flèche,
 * jamais retirés. Les annotations viennent de res/raw/siddur_annotations.json
 * (tools/siddur_annotations.py) ; les conditions, de CalendrierJuif.
 *
 * Pas d'Android ici : testable sur la machine virtuelle seule. Le même code
 * existe côté web (web/src/js/siddur-jour.js).
 */
object SiddurJour {

    enum class Sorte { BLOC, SECTION, INDICE }

    data class Annotation(
        val sorte: Sorte,
        val section: Int,
        val bloc: Int?,
        /** Clé de CalendrierJuif.C ; « ! » devant : quand la clé est fausse. */
        val condition: String,
        /** Le début du texte annoté, pour vérifier qu'il n'a pas bougé. */
        val empreinte: String,
        /** Pour un indice : la variante à signaler (« המלך הקדוש »). */
        val texte: String? = null,
        /** Pour un bloc : son groupe, et la condition qui ouvre ou replie le groupe. */
        val groupe: Int? = null,
        val racine: String? = null,
    )

    /** Les annotations d'un nusah, par identifiant de texte. */
    fun parse(json: String, nusah: String): Map<String, List<Annotation>> {
        val racine = JSONObject(json).optJSONObject(nusah) ?: return emptyMap()
        return racine.keys().asSequence().associateWith { texte ->
            Json.array(racine.getJSONArray(texte)) { a ->
                Annotation(
                    sorte = when (a.getString("sorte")) {
                        "section" -> Sorte.SECTION
                        "indice" -> Sorte.INDICE
                        else -> Sorte.BLOC
                    },
                    section = a.getInt("s"),
                    bloc = if (a.has("b")) a.getInt("b") else null,
                    condition = a.getString("si"),
                    empreinte = a.getString("empreinte"),
                    texte = a.optString("texte", "").ifBlank { null },
                    groupe = if (a.has("g")) a.getInt("g") else null,
                    racine = a.optString("racine", "").ifBlank { null },
                )
            }
        }
    }

    enum class Style { NORMAL, DU_JOUR, HORS }

    /** Une ligne de la lecture, le jour donné. */
    sealed class Ligne {
        data class Titre(val texte: String) : Ligne()
        data class Bloc(val bloc: SiddurBloc, val style: Style) : Ligne()

        /** « Aujourd'hui · Ya'alé véyavo », au-dessus d'un ajout mis en évidence. */
        data class Etiquette(val condition: String) : Ligne()

        /** « Aujourd'hui : המלך הקדוש », sous un bloc qui mêle une variante. */
        data class Indice(val texte: String) : Ligne()

        /** Un ajout hors du jour, replié ; [ouvert] quand on l'a déplié. */
        data class Replie(val cle: String, val condition: String, val ouvert: Boolean) : Ligne()
    }

    /**
     * Les lignes d'un texte le jour où valent [conditions]. Un ajout du jour
     * est précédé de son étiquette ; un ajout hors du jour tient en une ligne
     * repliée, et ses blocs suivent si sa clé est dans [ouverts]. Dans un ajout
     * du jour, une variante d'un autre jour (« בחוה״מ פסח » un Roch 'Hodech)
     * reste lisible, estompée.
     */
    fun lignes(
        texte: SiddurTexte,
        annotations: List<Annotation>,
        conditions: Set<String>,
        ouverts: Set<String> = emptySet(),
    ): List<Ligne> {
        val sections = annotations.filter { it.sorte == Sorte.SECTION }.associateBy { it.section }
        val blocs = annotations.filter { it.sorte == Sorte.BLOC }.associateBy { it.section to it.bloc!! }
        val indices = annotations.filter { it.sorte == Sorte.INDICE }.groupBy { it.section to it.bloc!! }
        val sortie = mutableListOf<Ligne>()
        texte.sections.forEachIndexed { si, s ->
            // Une section seule porte déjà le nom du texte : pas de titre répété.
            if (texte.sections.size > 1 && s.titre.isNotBlank()) sortie += Ligne.Titre(s.titre)
            val section = sections[si]
            if (section != null) {
                if (vrai(section.condition, conditions)) {
                    sortie += Ligne.Etiquette(section.condition)
                    s.blocs.forEach { sortie += Ligne.Bloc(it, Style.DU_JOUR) }
                } else {
                    val cle = "§$si"
                    sortie += Ligne.Replie(cle, section.condition, cle in ouverts)
                    if (cle in ouverts) s.blocs.forEach { sortie += Ligne.Bloc(it, Style.HORS) }
                }
                return@forEachIndexed
            }
            var i = 0
            while (i < s.blocs.size) {
                val a = blocs[si to i]
                if (a == null) {
                    sortie += Ligne.Bloc(s.blocs[i], Style.NORMAL)
                    indices[si to i].orEmpty().filter { it.texte != null && vrai(it.condition, conditions) }
                        .forEach { sortie += Ligne.Indice(it.texte!!) }
                    i++
                    continue
                }
                val membres = mutableListOf<Pair<SiddurBloc, Annotation>>()
                while (i < s.blocs.size && blocs[si to i]?.groupe == a.groupe) {
                    membres += s.blocs[i] to blocs.getValue(si to i)
                    i++
                }
                val racine = a.racine ?: a.condition
                if (vrai(racine, conditions)) {
                    sortie += Ligne.Etiquette(racine)
                    membres.forEach { (b, m) -> sortie += Ligne.Bloc(b, if (vrai(m.condition, conditions)) Style.DU_JOUR else Style.HORS) }
                } else {
                    val cle = "$si:${a.groupe}"
                    sortie += Ligne.Replie(cle, racine, cle in ouverts)
                    if (cle in ouverts) membres.forEach { (b, _) -> sortie += Ligne.Bloc(b, Style.HORS) }
                }
            }
        }
        return sortie
    }

    fun vrai(condition: String, conditions: Set<String>): Boolean =
        if (condition.startsWith("!")) condition.drop(1) !in conditions else condition in conditions

    /** Le nom de la chaîne qui nomme une condition, dans les trois langues. */
    fun libelle(condition: String): String {
        val c = condition.removePrefix("!")
        return when {
            condition.startsWith("!tahanoun") -> "ajout_sans_tahanoun"
            c.startsWith("tahanoun") -> "ajout_tahanoun"
            c == "kippour" -> "fete_yom_kippour"
            // « אומרים כאן הלל » : le Hallel, complet ou non selon le jour.
            c == "hallel" -> "ajout_hallel_annonce"
            c in setOf("pessah", "soukkot", "chavouot", "roch_hachana", "chemini_atseret", "hanouka", "pourim") -> "fete_$c"
            else -> "ajout_$c"
        }
    }

    /** Comme tools/siddur_annotations.py : sans balises ni voyelles, quarante caractères. */
    fun empreinte(texte: String): String =
        texte.replace(Regex("<[^>]+>"), " ")
            .replace(Regex("[\\u0591-\\u05C7]"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
            .take(40)
}
