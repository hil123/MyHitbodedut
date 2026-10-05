package com.myhitbodedut

import kotlin.math.roundToInt

/**
 * Le décor de la vignette, en nombres : son dégradé, ses voiles et la
 * taille de son image (demande du 04/10 : « trop banale »).
 *
 * La vignette n'est plus une carte d'une seule couleur. Elle porte la
 * couleur des boutons choisie, en dégradé du coin de départ vers le foncé
 * (comme la plaque de l'écran de séance), avec deux grands arcs très pâles
 * qui reprennent les anneaux de l'accueil, et un halo derrière le hassid.
 *
 * Le dégradé ne part pas de « brand », qui s'éclaircit la nuit pour servir
 * d'encre sur une carte sombre, mais de « day_done » : la même couleur le
 * jour, fixe la nuit, et dont PaletteTest garantit déjà qu'elle porte du
 * blanc. Il arrive sur « brand_dark », fixe lui aussi. Le soir, les deux
 * bouts foncent encore.
 *
 * Tout ce qui est écrit sur la vignette l'est en blanc ou en « mint » (fixe
 * lui aussi). WidgetTest vérifie, pour chaque palette, de jour comme de
 * nuit, que les deux restent lisibles sur le pire point du fond : le coin
 * clair sous l'arc le plus marqué. D'où les voiles plafonnés ici.
 *
 * Sans Android, pour se tester : les couleurs sont des entiers ARGB.
 */
object VignetteDecor {

    /** La part de brand_dark dans le coin clair, le jour : le coin reste vif sans pâlir le texte. */
    const val DEPART_JOUR = 0.2f

    /** La nuit, le coin clair se rapproche du foncé… */
    const val DEPART_NUIT = 0.45f

    /** … et le foncé descend vers le noir. */
    const val NOIR_NUIT = 0.35f

    /** Le blanc des deux arcs décoratifs : à peine visibles, ils ne doivent rien coûter au texte. */
    const val ARC_EXTERIEUR = 0.05f
    const val ARC_INTERIEUR = 0.035f

    /** Le halo derrière les anneaux : il ne passe jamais sous le texte. */
    const val HALO = 0.18f

    /** Le liseré clair du haut, sur le bord seulement. */
    const val LISERE = 0.16f

    /** Le voile sombre sous le texte du jour et sous le compte : il ne fait que gagner du contraste. */
    const val VOILE_PANNEAU = 0.18f
    const val VOILE_BADGE = 0.24f

    /** Le disque sombre sous les anneaux, qui les détache du dégradé. */
    const val VOILE_ANNEAUX = 0.16f

    /** La plus grande image de fond, en pixels : un dégradé n'a pas besoin de plus, et RemoteViews compte sa mémoire. */
    const val COTE_MAX = 640

    const val BLANC = 0xFFFFFFFF.toInt()
    const val NOIR = 0xFF000000.toInt()

    /** Le mélange de deux couleurs, canal par canal ; [t] est la part de [b]. */
    fun melange(a: Int, b: Int, t: Float): Int {
        val u = t.coerceIn(0f, 1f)
        fun canal(decalage: Int): Int {
            val x = (a ushr decalage) and 0xFF
            val y = (b ushr decalage) and 0xFF
            return (x + (y - x) * u).roundToInt().coerceIn(0, 255)
        }
        return (canal(24) shl 24) or (canal(16) shl 16) or (canal(8) shl 8) or canal(0)
    }

    /** La couleur avec une autre opacité, de 0 à 1. */
    fun opacite(couleur: Int, alpha: Float): Int =
        ((alpha.coerceIn(0f, 1f) * 255).roundToInt() shl 24) or (couleur and 0x00FFFFFF)

    /** Les deux bouts du dégradé : le coin de départ, puis le coin foncé. */
    fun degrade(jourFait: Int, brandDark: Int, nuit: Boolean): Pair<Int, Int> =
        if (nuit) {
            melange(jourFait, brandDark, DEPART_NUIT) to melange(brandDark, NOIR, NOIR_NUIT)
        } else {
            melange(jourFait, brandDark, DEPART_JOUR) to brandDark
        }

    /**
     * Le fond le plus clair sur lequel un texte peut tomber : le coin de
     * départ, sous l'arc le plus marqué. Le halo et le liseré n'y sont pas :
     * ils restent derrière le hassid et sur le bord.
     */
    fun pireFond(jourFait: Int, brandDark: Int, nuit: Boolean): Int =
        melange(degrade(jourFait, brandDark, nuit).first, BLANC, maxOf(ARC_EXTERIEUR, ARC_INTERIEUR))

    /**
     * La taille de l'image de fond, en pixels, pour une vignette de
     * [largeurDp] × [hauteurDp] : la densité de l'écran, ramenée sous
     * [COTE_MAX] sans changer les proportions.
     */
    fun pixels(largeurDp: Float, hauteurDp: Float, densite: Float, max: Int = COTE_MAX): Pair<Int, Int> {
        val l = (largeurDp.coerceAtLeast(1f) * densite).coerceAtLeast(1f)
        val h = (hauteurDp.coerceAtLeast(1f) * densite).coerceAtLeast(1f)
        val echelle = minOf(1f, max / maxOf(l, h))
        return (l * echelle).roundToInt().coerceAtLeast(1) to (h * echelle).roundToInt().coerceAtLeast(1)
    }
}
