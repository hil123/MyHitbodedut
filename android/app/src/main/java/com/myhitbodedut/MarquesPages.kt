package com.myhitbodedut

/**
 * Les marque-pages (15/10, « possibilité de mettre des marque-pages dans les
 * lectures ») : une place précise dans un texte du siddour ou d'un livre, posée
 * d'un appui long sur un paragraphe. Une place se nomme par le texte (« s:id »
 * ou « l:id », comme les favoris), le noussa'h (les paragraphes diffèrent d'un
 * noussa'h à l'autre ; vide pour un livre), la section et le paragraphe, avec
 * un aperçu des premiers mots pour la liste.
 *
 * Rien d'Android ici.
 */
object MarquesPages {

    data class Marque(val cle: String, val nusah: String, val section: Int, val bloc: Int, val apercu: String) {
        /** Une même place, quel que soit l'aperçu. */
        fun memePlace(m: Marque) = cle == m.cle && nusah == m.nusah && section == m.section && bloc == m.bloc
    }

    /** Au plus tant de marque-pages : les plus anciens tombent. */
    const val MAX = 60

    /** Les premiers mots d'un paragraphe, sans balises ni voyelles. */
    fun apercu(texte: String, mots: Int = 6): String =
        texte.replace(Regex("<[^>]+>"), " ").replace(Regex("[\\u0591-\\u05C7]"), "")
            .replace(Regex("[|\\n\\t]"), " ").trim().split(Regex("\\s+")).take(mots).joinToString(" ")

    fun encoder(l: List<Marque>): String = l.joinToString("\n") { "${it.cle}\t${it.nusah}\t${it.section}\t${it.bloc}\t${it.apercu}" }

    fun decoder(s: String?): List<Marque> = s.orEmpty().split('\n').mapNotNull { ligne ->
        val c = ligne.split('\t')
        if (c.size < 5) null else Marque(c[0], c[1], c[2].toIntOrNull() ?: return@mapNotNull null, c[3].toIntOrNull() ?: return@mapNotNull null, c[4])
    }

    /** Pose la marque, ou la retire si elle y est ; la plus récente en tête. */
    fun basculer(l: List<Marque>, m: Marque): List<Marque> =
        if (l.any { it.memePlace(m) }) l.filterNot { it.memePlace(m) } else (listOf(m) + l).take(MAX)

    /** Celles d'un texte, dans l'ordre du texte. */
    fun duTexte(l: List<Marque>, cle: String, nusah: String): List<Marque> =
        l.filter { it.cle == cle && it.nusah == nusah }.sortedWith(compareBy({ it.section }, { it.bloc }))
}
