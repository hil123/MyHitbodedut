package com.myhitbodedut

/**
 * Israël ou diaspora d'après la position (16/10) : un point dans le territoire — l'État
 * d'Israël avec Jérusalem, la Judée-Samarie et le Golan, de la frontière libanaise à Eilat,
 * du Jourdain et de la Arava à la mer. Avant, seul le fuseau du téléphone en décidait.
 *
 * Contour simplifié (une quarantaine de points relevés sur la carte, précision de l'ordre
 * de 2 à 3 km) : suffisant pour savoir si l'on est en Israël, pas pour arbitrer à quelques
 * centaines de mètres d'une frontière — le réglage « Israël / diaspora » reste là pour ça.
 * La question halakhique des limites de la Terre d'Israël n'est pas tranchée ici.
 *
 * Rien d'Android ici.
 */
object TerritoireIsrael {

    /** (latitude, longitude), dans l'ordre : la côte vers le nord, le Liban, le Golan, le Jourdain, la Arava, l'Égypte. */
    private val CONTOUR = arrayOf(
        doubleArrayOf(31.23, 34.21), doubleArrayOf(31.36, 34.24), doubleArrayOf(31.55, 34.40), doubleArrayOf(31.72, 34.50),
        doubleArrayOf(31.95, 34.66), doubleArrayOf(32.10, 34.72), doubleArrayOf(32.33, 34.80), doubleArrayOf(32.55, 34.86),
        doubleArrayOf(32.75, 34.92), doubleArrayOf(32.84, 34.93), doubleArrayOf(32.93, 35.03), doubleArrayOf(33.09, 35.08),
        // La frontière libanaise, de Roch Hanikra à Metoula, puis l'Hermon.
        doubleArrayOf(33.10, 35.20), doubleArrayOf(33.09, 35.33), doubleArrayOf(33.08, 35.45), doubleArrayOf(33.11, 35.52),
        doubleArrayOf(33.20, 35.55), doubleArrayOf(33.28, 35.57), doubleArrayOf(33.31, 35.63), doubleArrayOf(33.33, 35.79),
        // Le Golan, à l'est, jusqu'à 'Hamat Gader.
        doubleArrayOf(33.22, 35.86), doubleArrayOf(33.08, 35.86), doubleArrayOf(32.93, 35.87), doubleArrayOf(32.78, 35.80),
        doubleArrayOf(32.70, 35.72), doubleArrayOf(32.66, 35.62),
        // Le Jourdain et la mer Morte.
        doubleArrayOf(32.45, 35.57), doubleArrayOf(32.20, 35.56), doubleArrayOf(31.95, 35.55), doubleArrayOf(31.76, 35.56),
        doubleArrayOf(31.45, 35.50), doubleArrayOf(31.15, 35.45),
        // La Arava, jusqu'à Eilat et Taba.
        doubleArrayOf(30.85, 35.36), doubleArrayOf(30.50, 35.22), doubleArrayOf(30.15, 35.14), doubleArrayOf(29.90, 35.09), doubleArrayOf(29.70, 35.03),
        doubleArrayOf(29.55, 34.98), doubleArrayOf(29.49, 34.90),
        // La frontière égyptienne, jusqu'à Kerem Chalom et Rafia'h.
        doubleArrayOf(29.80, 34.76), doubleArrayOf(30.20, 34.60), doubleArrayOf(30.60, 34.50), doubleArrayOf(30.95, 34.37),
        doubleArrayOf(31.22, 34.26),
    )

    /** Le point est-il dans le contour (tracé de rayon) ? */
    fun contient(latitude: Double, longitude: Double): Boolean {
        var dedans = false
        var j = CONTOUR.size - 1
        for (i in CONTOUR.indices) {
            val (yi, xi) = CONTOUR[i].let { it[0] to it[1] }
            val (yj, xj) = CONTOUR[j].let { it[0] to it[1] }
            if ((yi > latitude) != (yj > latitude) &&
                longitude < (xj - xi) * (latitude - yi) / (yj - yi) + xi) dedans = !dedans
            j = i
        }
        return dedans
    }
}
