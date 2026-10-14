package com.myhitbodedut

/**
 * La recherche dans toute l'application (14/10) : le siddour, les livres, la
 * halakha (Yalkut Yosef, Ben Ish 'Haï) et les hilloulot, d'un seul champ. Sans
 * voyelles, cantillation, guillemets ni balises, et sans tenir compte de la casse.
 * Le même calcul que rechercheGlobale() du web.
 */
object RechercheGlobale {

    enum class Sorte { SIDDOUR, LIVRE, HALAKHA, HILLOULA }

    data class Resultat(val sorte: Sorte, val id: String, val titre: String, val extrait: String)

    const val PAR_SORTE = 30

    fun nu(t: String): String = t.replace(Regex("<[^>]+>"), " ").replace(Regex("[֑-ׇ\"'״׳]"), "").replace(Regex("\\s+"), " ").trim().lowercase()

    /** Un extrait d'une quarantaine de caractères autour de la trouvaille, dans le texte sans voyelles. */
    fun extrait(texte: String, q: String): String {
        val t = nu(texte)
        val i = t.indexOf(q).coerceAtLeast(0)
        val debut = (i - 30).coerceAtLeast(0)
        return (if (debut > 0) "…" else "") + t.substring(debut, (i + q.length + 50).coerceAtMost(t.length)).trim() + (if (i + q.length + 50 < t.length) "…" else "")
    }

    /** Les textes d'un recueil (siddour ou livres) : titre d'abord, puis le premier paragraphe qui contient la recherche. */
    fun dansRecueil(recueil: Siddur, q: String, sorte: Sorte, langue: String): List<Resultat> = recueil.textes.mapNotNull { t ->
        val titre = t.titre.he
        if (nu(titre).contains(q) || nu(t.titre.forLanguage(langue)).contains(q)) return@mapNotNull Resultat(sorte, t.id, titre, t.titre.forLanguage(langue).takeIf { it != titre }.orEmpty())
        t.sections.asSequence().flatMap { it.blocs.asSequence() }.firstOrNull { !it.consigne && nu(it.texte).contains(q) }
            ?.let { Resultat(sorte, t.id, titre, extrait(it.texte, q)) }
    }.take(PAR_SORTE)

    fun chercher(
        texte: String, langue: String, siddur: Siddur, livres: Siddur?,
        yalkut: YalkutYosef.Livre?, benIshHai: Halakha.Livre?, hilloulot: Map<String, List<Hilloulot.Hilloula>>?,
    ): List<Resultat> {
        val q = nu(texte.trim())
        if (q.length < 2) return emptyList()
        return buildList {
            addAll(dansRecueil(siddur, q, Sorte.SIDDOUR, langue))
            livres?.let { addAll(dansRecueil(it, q, Sorte.LIVRE, langue)) }
            yalkut?.let { y -> addAll(YalkutYosef.recherche(y, texte).take(PAR_SORTE).map { Resultat(Sorte.HALAKHA, texte, YalkutYosef.source(it), extrait(it.texte, q)) }) }
            benIshHai?.let { b -> addAll(Halakha.recherche(b, texte).take(PAR_SORTE).map { Resultat(Sorte.HALAKHA, texte, HalakhaActivity.source(it), extrait(it.texte, q)) }) }
            hilloulot?.let { h -> addAll(Hilloulot.recherche(h, texte).take(PAR_SORTE).map { (_, x) -> Resultat(Sorte.HILLOULA, texte, x.he, x.annee) }) }
        }
    }
}
