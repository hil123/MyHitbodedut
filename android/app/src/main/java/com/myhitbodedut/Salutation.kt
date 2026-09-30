package com.myhitbodedut

/**
 * La salutation de l'accueil selon l'heure.
 *
 * « Bonjour Avraham » à trois heures du matin ou à dix heures du soir
 * sonnait faux (demande du 30/09). Le français dit « bonjour » jusqu'au
 * soir, puis « bonsoir » ; l'anglais et l'hébreu distinguent aussi
 * l'après-midi. Les trois moments sont donc gardés, chaque langue les
 * nomme à sa façon dans ses chaînes.
 *
 * La nuit, jusqu'à 5 h, reste le soir : on ne souhaite pas « bonne nuit » à
 * quelqu'un qu'on accueille.
 */
object Salutation {

    enum class Partie { MATIN, APRES_MIDI, SOIR }

    fun partie(heure: Int): Partie = when (heure) {
        in 5 until 12 -> Partie.MATIN
        in 12 until 18 -> Partie.APRES_MIDI
        else -> Partie.SOIR
    }

    fun chaine(heure: Int): Int = when (partie(heure)) {
        Partie.MATIN -> R.string.hero_greeting_morning
        Partie.APRES_MIDI -> R.string.hero_greeting_afternoon
        Partie.SOIR -> R.string.hero_greeting_evening
    }
}
