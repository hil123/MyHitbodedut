package com.myhitbodedut

import androidx.annotation.StringRes

/**
 * Comment l'application s'adresse à qui la tient : צדיק ou צדקת.
 *
 * Tant que rien n'est choisi, la valeur est null et le « bravo » de fin de
 * séance reste neutre : deviner le genre à partir d'un prénom se tromperait
 * pour quelqu'un, et se tromper sur ce mot-là gâcherait le compliment.
 */
enum class Adresse(
    val cle: String,
    @StringRes val bravo: Int,
    @StringRes val nom: Int,
    /** Après une séance annulée : חזק ou חזקי. */
    @StringRes val hazak: Int
) {
    HOMME("m", R.string.celebration_tsadik, R.string.adresse_tsadik, R.string.annulation_hazak),
    FEMME("f", R.string.celebration_tsadeket, R.string.adresse_tsadeket, R.string.annulation_hizki);

    companion object {
        /** Une clé inconnue ou vide rend null : la sauvegarde d'une autre version ne rompt rien. */
        fun parse(cle: String?): Adresse? = values().firstOrNull { it.cle == cle }

        @StringRes
        fun bravo(adresse: Adresse?): Int = adresse?.bravo ?: R.string.celebration_neutral

        @StringRes
        fun hazak(adresse: Adresse?): Int = adresse?.hazak ?: R.string.annulation_neutral
    }
}
