package com.myhitbodedut

import android.content.Context
import android.util.AttributeSet

/**
 * Une illustration qui prend la place qu'on lui laisse, sans en réclamer.
 *
 * Dans un ScrollView, la première mesure se fait en hauteur libre. Une
 * ImageView ordinaire y annonce la hauteur de son dessin, mise à l'échelle
 * de la largeur : pour un personnage debout, plus que l'écran entier. La
 * page devenait alors plus haute que l'écran, le fillViewport ne jouait
 * plus, et le titre, le texte et les boutons de la présentation passaient
 * sous le bord (signalé sur un 480×854).
 *
 * Ici, en hauteur libre, l'illustration n'annonce que sa hauteur minimale ;
 * c'est le poids de son cadre qui lui donne ensuite la place restante.
 */
class IllustrationSouple @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : PersonnageImageView(context, attrs) {

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        if (MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.UNSPECIFIED) {
            setMeasuredDimension(
                getDefaultSize(suggestedMinimumWidth, widthMeasureSpec),
                suggestedMinimumHeight,
            )
        } else {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        }
    }
}
