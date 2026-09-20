package com.myhitbodedut

/**
 * Ce que la bibliothèque a le droit d'afficher.
 *
 * La règle tient en une phrase, tirée de _meta.regle : une « source » est
 * une œuvre du domaine public, ses paragraphes s'affichent en entier ; un
 * « article » est un texte moderne qui appartient à son auteur, on n'en
 * montre qu'une fiche tant qu'aucune autorisation n'a été obtenue.
 *
 * Reproduire un article sans autorisation n'abîme pas l'application : cela
 * expose celui qui la publie. D'où le test bloquant qui accompagne ce
 * fichier.
 */
object LibraryRules {

    /**
     * Le texte intégral d'un article, ou null.
     *
     * Null tant que `autorisation` est vide, même si `texte_integral` est
     * rempli : c'est l'autorisation qui ouvre, pas la présence du texte.
     */
    fun fullText(article: ArticleEntry): String? {
        if (article.autorisation.isNullOrBlank()) return null
        return article.texteIntegral?.ifBlank { null }
    }

    /**
     * Les paragraphes d'une source, dans la forme que sa licence permet.
     * Même règle que pour le 'hizouk : la vocalisation d'une édition dont la
     * licence n'est pas déclarée est un travail éditorial qu'on ne reprend pas.
     */
    fun paragraphs(library: Library, source: SourceEntry): List<String> {
        val nikkoudOk = library.editions[source.edition]?.nikkoudOk ?: false
        if (nikkoudOk) return source.paragraphes
        return source.paragraphesSansNikkoud.ifEmpty { source.paragraphes }
    }

    /** Le résumé dans la langue de l'interface, à défaut celui du fichier. */
    fun summary(article: ArticleEntry, language: String): String? =
        article.resumes[language] ?: article.resumes["fr"] ?: article.resumes.values.firstOrNull()

    /** Les sources d'abord : ce sont elles qu'on vient lire. */
    fun ordered(entrees: List<LibraryEntry>): List<LibraryEntry> =
        entrees.filterIsInstance<SourceEntry>() + entrees.filterIsInstance<ArticleEntry>()
}
