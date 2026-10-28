package com.myhitbodedut

/**
 * L'aération d'un paragraphe de prière (28/10) : « mieux séparer les textes en plusieurs
 * paragraphes, espacer, mettre le premier mot plus grand et plus gras ». Miroir de
 * web/src/js/aeration.js ; mêmes seuils, mêmes vecteurs de test.
 *
 * À l'affichage seulement, sur le balisage du siddour (<b>, <small>, <br>) : les données,
 * les annotations et les marque-pages gardent leurs blocs tels quels.
 * - Un long paragraphe (plus de [LONG] lettres visibles) se coupe après une fin de phrase
 *   (« : » ou « ׃ » suivi d'une espace, hors balise et hors <small>), quand le morceau en
 *   cours a au moins [MORCEAU] lettres et qu'il en reste au moins [RESTE] après.
 * - Le premier mot est mis en gras s'il ne l'est pas déjà (l'écran l'agrandit) ; un
 *   paragraphe qui commence par <small> ou une parenthèse n'est pas touché.
 */
object Aeration {
    const val LONG = 500
    const val MORCEAU = 250
    const val RESTE = 150

    private val BALISE = Regex("<[^>]+>")
    private val MOT = Regex("^([\\u0590-\\u05FF\\uFB1D-\\uFB4F\"'״׳-]+)(?=[\\s,:;.]|$)")

    fun premierMot(html: String): String {
        if (html.startsWith("<b>")) return html
        val m = MOT.find(html) ?: return html
        val mot = m.groupValues[1]
        return "<b>$mot</b>" + html.substring(mot.length)
    }

    fun couper(html: String): String {
        val total = html.replace(BALISE, "").length
        if (total <= LONG) return html
        val sortie = StringBuilder(html.length + 64)
        var balise = false
        var petit = 0
        var depuis = 0
        var vus = 0
        var i = 0
        while (i < html.length) {
            val c = html[i]
            if (c == '<') {
                balise = true
                if (html.startsWith("<small", i)) petit++
                else if (html.startsWith("</small", i)) petit = maxOf(0, petit - 1)
            }
            sortie.append(c)
            if (balise) {
                if (c == '>') balise = false
                i++
                continue
            }
            vus++
            depuis++
            if ((c == ':' || c == '׃') && petit == 0 && html.getOrNull(i + 1) == ' ' && depuis >= MORCEAU && total - vus >= RESTE) {
                sortie.append("<br><br>")
                i++ // l'espace qui suivait
                depuis = 0
            }
            i++
        }
        return sortie.toString()
    }

    fun aerer(html: String): String = premierMot(couper(html))
}
