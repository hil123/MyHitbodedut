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

    enum class Sorte { BLOC, SECTION, INDICE, RENVOI }

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
        /** Un libellé propre (« ajout_anenou_officiant »), sinon celui de la condition. */
        val libelle: String? = null,
        /** Pour un renvoi : l'identifiant du texte à ouvrir (« hallel »). */
        val cible: String? = null,
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
                        "renvoi" -> Sorte.RENVOI
                        else -> Sorte.BLOC
                    },
                    section = a.getInt("s"),
                    bloc = if (a.has("b")) a.getInt("b") else null,
                    condition = a.getString("si"),
                    empreinte = a.getString("empreinte"),
                    texte = a.optString("texte", "").ifBlank { null },
                    groupe = if (a.has("g")) a.getInt("g") else null,
                    racine = a.optString("racine", "").ifBlank { null },
                    libelle = a.optString("libelle", "").ifBlank { null },
                    cible = a.optString("cible", "").ifBlank { null },
                )
            }
        }
    }

    enum class Style { NORMAL, DU_JOUR, HORS }

    /** Une ligne de la lecture, le jour donné. */
    sealed class Ligne {
        data class Titre(val texte: String) : Ligne()
        data class Bloc(val bloc: SiddurBloc, val style: Style) : Ligne()

        /** « Aujourd'hui · Ya'alé véyavo », au-dessus d'un ajout mis en évidence ; [libelle] : la clé de chaîne. */
        data class Etiquette(val condition: String, val libelle: String) : Ligne()

        /** « Aujourd'hui : המלך הקדוש », sous un bloc qui mêle une variante. */
        data class Indice(val texte: String) : Ligne()

        /** « Aujourd'hui · Hallel › » : le texte à dire ici ce jour-là, ouvert d'un geste ; [cible] : son identifiant. */
        data class Renvoi(val condition: String, val cible: String) : Ligne()

        /** Un ajout hors du jour, replié ; [ouvert] quand on l'a déplié. */
        data class Replie(val cle: String, val condition: String, val ouvert: Boolean, val libelle: String) : Ligne()
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
        conditionsDuJour: Set<String>,
        ouverts: Set<String> = emptySet(),
        nusah: String = "edot",
        enIsrael: Boolean = false,
        sansTahanounFinTichri: Boolean = nusah == "edot",
    ): List<Ligne> {
        val conditions = conditionsPour(nusah, enIsrael, conditionsDuJour, sansTahanounFinTichri)
        fun nom(a: Annotation, condition: String) = libelleJour(a.libelle, condition, nusah, conditions)
        val sections = annotations.filter { it.sorte == Sorte.SECTION }.associateBy { it.section }
        val blocs = annotations.filter { it.sorte == Sorte.BLOC }.associateBy { it.section to it.bloc!! }
        val indices = annotations.filter { it.sorte == Sorte.INDICE }.groupBy { it.section to it.bloc!! }
        // Les renvois du jour seulement : hors du jour, rien (le texte renvoyé n'est pas d'ici).
        val renvois = annotations.filter { it.sorte == Sorte.RENVOI && it.cible != null && vrai(it.condition, conditions) }
            .groupBy { it.section to it.bloc }
        val sortie = mutableListOf<Ligne>()
        fun renvoyer(si: Int, bi: Int?) = renvois[si to bi].orEmpty().forEach { sortie += Ligne.Renvoi(it.condition, it.cible!!) }
        texte.sections.forEachIndexed { si, s ->
            // Une section seule porte déjà le nom du texte : pas de titre répété.
            if (texte.sections.size > 1 && s.titre.isNotBlank()) sortie += Ligne.Titre(s.titre)
            val section = sections[si]
            if (section != null) {
                if (vrai(section.condition, conditions)) {
                    sortie += Ligne.Etiquette(section.condition, nom(section, section.condition))
                    s.blocs.forEach { sortie += Ligne.Bloc(it, Style.DU_JOUR) }
                } else {
                    val cle = "§$si"
                    sortie += Ligne.Replie(cle, section.condition, cle in ouverts, nom(section, section.condition))
                    if (cle in ouverts) s.blocs.forEach { sortie += Ligne.Bloc(it, Style.HORS) }
                }
                renvoyer(si, null)
                return@forEachIndexed
            }
            var i = 0
            while (i < s.blocs.size) {
                val a = blocs[si to i]
                if (a == null) {
                    sortie += Ligne.Bloc(s.blocs[i], Style.NORMAL)
                    indices[si to i].orEmpty().filter { it.texte != null && vrai(it.condition, conditions) }
                        .forEach { sortie += Ligne.Indice(it.texte!!) }
                    renvoyer(si, i)
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
                    sortie += Ligne.Etiquette(racine, nom(a, racine))
                    membres.forEach { (b, m) -> sortie += Ligne.Bloc(b, if (vrai(m.condition, conditions)) Style.DU_JOUR else Style.HORS) }
                } else {
                    val cle = "$si:${a.groupe}"
                    sortie += Ligne.Replie(cle, racine, cle in ouverts, nom(a, racine))
                    if (cle in ouverts) membres.forEach { (b, _) -> sortie += Ligne.Bloc(b, Style.HORS) }
                }
            }
            renvoyer(si, null)
        }
        return sortie
    }

    /**
     * Les conditions du jour, ajustées au nusah (demande du 10/10, « attention
     * aux différences ») : les Ashkénazes ne disent pas Morid hatal hors
     * d'Israël (Rema, OC 114:3), quand le nusah Sfard et les Séfarades le
     * disent ; les Edot ne disent pas Ta'hanoun jusqu'à la fin de Tichri.
     * À valider (ZMANIM.md, « Différences de noussa'h »).
     */
    fun conditionsPour(
        nusah: String,
        enIsrael: Boolean,
        conditions: Set<String>,
        sansTahanounFinTichri: Boolean = nusah == "edot",
    ): Set<String> {
        var c = conditions
        if (nusah == "ashkenaz" && !enIsrael) c = c - CalendrierJuif.C.MORID_HATAL
        // Pas de Ta'hanoun du lendemain de Kippour jusqu'au 2 'Hechvan : la
        // coutume des Edot, au choix en Sfard (Store.sansTahanounFinTichri) ;
        // TefilaRules le fait reprendre après Isrou 'hag.
        if (sansTahanounFinTichri && CalendrierJuif.C.FIN_TICHRI in c) {
            c = c - setOf(CalendrierJuif.C.TAHANOUN_CHAHARIT, CalendrierJuif.C.TAHANOUN_MINHA, CalendrierJuif.C.LUNDI_JEUDI)
        }
        return c
    }

    /**
     * Le libellé d'un ajout, un jour donné : celui de l'annotation s'il en a un ;
     * pour l'annonce du Hallel, complet ou partiel, et la bénédiction du
     * Hallel partiel selon le nusah (Choul'han Aroukh et Rema, OC 422:2).
     */
    fun libelleJour(libelleAnnotation: String?, condition: String, nusah: String, conditions: Set<String>): String = when {
        libelleAnnotation != null -> libelleAnnotation
        condition == CalendrierJuif.C.HALLEL -> libelleHallel(nusah, conditions) ?: libelle(condition)
        else -> libelle(condition)
    }

    /** Hallel complet, ou partiel avec ou sans bénédiction selon le nusah ; null hors Hallel. */
    fun libelleHallel(nusah: String, conditions: Set<String>): String? = when {
        CalendrierJuif.C.HALLEL_COMPLET in conditions -> "ajout_hallel_complet"
        CalendrierJuif.C.HALLEL !in conditions -> null
        nusah == "edot" -> "ajout_hallel_partiel_sans_berakha"
        else -> "ajout_hallel_partiel_avec_berakha"
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
