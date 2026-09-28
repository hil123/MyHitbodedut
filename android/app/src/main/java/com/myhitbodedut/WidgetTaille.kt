package com.myhitbodedut

/**
 * Quelle version de la vignette pour quelle place (demande du 28/09) :
 * une case, le compte et une coche ; la taille habituelle, la jauge et le
 * bouton ; une grande place, la semaine et un texte du jour en plus.
 *
 * Sans Android, pour se tester : les tailles sont en dp, comme les
 * donne AppWidgetManager.
 */
enum class WidgetTaille(val largeurMin: Float, val hauteurMin: Float) {
    PETITE(40f, 40f),
    MOYENNE(160f, 110f),
    GRANDE(250f, 250f);

    companion object {
        /** La plus grande version qui tient dans la place donnée. */
        fun pour(largeur: Float, hauteur: Float): WidgetTaille =
            entries.lastOrNull { largeur >= it.largeurMin && hauteur >= it.hauteurMin } ?: PETITE
    }
}
