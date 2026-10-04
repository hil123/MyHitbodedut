package com.myhitbodedut

/**
 * Quelle version de la vignette pour quelle place (demande du 28/09) :
 * une case, le compte et une coche ; la taille habituelle, la jauge et le
 * bouton ; une grande place, la semaine et un texte du jour en plus.
 *
 * Sans Android, pour se tester : les tailles sont en dp, comme les
 * donne AppWidgetManager.
 *
 * [largeurType] × [hauteurType] : la place qu'elle a d'ordinaire (2 × 2,
 * 4 × 2, 4 × 4 cases). Le fond de la vignette est une image aux
 * proportions de la place : quand le lanceur ne dit pas la sienne, c'est
 * celle-ci qu'on dessine, et c'est aussi celle de la vitrine de debug.
 */
enum class WidgetTaille(
    val largeurMin: Float,
    val hauteurMin: Float,
    val largeurType: Float,
    val hauteurType: Float
) {
    PETITE(40f, 40f, 150f, 150f),
    MOYENNE(160f, 110f, 320f, 150f),
    GRANDE(250f, 250f, 320f, 320f);

    companion object {
        /** La plus grande version qui tient dans la place donnée. */
        fun pour(largeur: Float, hauteur: Float): WidgetTaille =
            entries.lastOrNull { largeur >= it.largeurMin && hauteur >= it.hauteurMin } ?: PETITE
    }
}
