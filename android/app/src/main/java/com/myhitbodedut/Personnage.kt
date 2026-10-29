package com.myhitbodedut

import android.content.Context
import android.util.AttributeSet
import androidx.annotation.DrawableRes
import androidx.appcompat.widget.AppCompatImageView
import com.google.android.material.theme.MaterialComponentsViewInflater

/**
 * Le personnage au féminin (audit 1.390, point 8) : qui a choisi צדקת voit,
 * à la place de chaque pose du hassid, la plus proche de la planche féminine
 * du 29/10 (tools/personnage_feminin.py). Sans choix, ou צדיק, rien ne change.
 *
 * La planche n'a ni guitare, ni tefillin, ni matsa, ni costume de Pourim :
 * ces poses prennent la plus proche — la joie, la prière, le livre serré
 * contre soi. La même table vit côté site (web/src/js/personnage.js).
 */
object Personnage {

    private val FEMININ: Map<Int, Int> = mapOf(
        R.drawable.char_welcome to R.drawable.char_f_welcome,
        R.drawable.char_open_arms to R.drawable.char_f_welcome,
        R.drawable.char_debout to R.drawable.char_f_welcome,
        R.drawable.char_thumbs_up to R.drawable.char_f_thumbs_up,
        R.drawable.char_tehilim to R.drawable.char_f_tehilim,
        R.drawable.char_praise to R.drawable.char_f_praise,
        R.drawable.char_dancing_joy to R.drawable.char_f_praise,
        R.drawable.char_writing to R.drawable.char_f_writing,
        R.drawable.char_thinking to R.drawable.char_f_thinking,
        R.drawable.char_pointing to R.drawable.char_f_pointing,
        R.drawable.char_walking to R.drawable.char_f_walking,
        R.drawable.char_praying to R.drawable.char_f_praying,
        R.drawable.char_tefillin to R.drawable.char_f_praying,
        R.drawable.char_dancing to R.drawable.char_f_joy,
        R.drawable.char_guitar to R.drawable.char_f_joy,
        R.drawable.char_purim to R.drawable.char_f_joy,
        R.drawable.char_shabbat to R.drawable.char_f_tehilim_hug,
        R.drawable.char_matsa to R.drawable.char_f_tehilim_hug,
        R.drawable.char_offering to R.drawable.char_f_offering,
        R.drawable.char_torah to R.drawable.char_f_reading,
        R.drawable.char_standing_back to R.drawable.char_f_standing_back
    )

    @DrawableRes
    fun feminin(@DrawableRes id: Int): Int = FEMININ[id] ?: id

    @DrawableRes
    fun pour(context: Context, @DrawableRes id: Int): Int =
        if (Store.adresse(context) == Adresse.FEMME) feminin(id) else id
}

/**
 * Une image qui passe par [Personnage] : celle de la mise en page
 * (android:src) comme celles posées ensuite par setImageResource. Toutes les
 * ImageView des mises en page en sont une (PersonnageViewInflater) ; celles
 * construites dans le code le deviennent quand elles montrent le personnage.
 */
open class PersonnageImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : AppCompatImageView(context, attrs) {

    init {
        val src = attrs?.getAttributeResourceValue(ANDROID, "src", 0) ?: 0
        if (src != 0 && !isInEditMode) {
            val adapte = Personnage.pour(context, src)
            if (adapte != src) super.setImageResource(adapte)
        }
    }

    override fun setImageResource(resId: Int) {
        // L'aperçu d'Android Studio n'a pas de préférences à lire.
        super.setImageResource(if (isInEditMode) resId else Personnage.pour(context, resId))
    }

    private companion object {
        const val ANDROID = "http://schemas.android.com/apk/res/android"
    }
}

/** Branché par le thème (viewInflaterClass) : chaque <ImageView> devient une [PersonnageImageView]. */
class PersonnageViewInflater : MaterialComponentsViewInflater() {
    override fun createImageView(context: Context, attrs: AttributeSet?): AppCompatImageView =
        PersonnageImageView(context, attrs)
}
