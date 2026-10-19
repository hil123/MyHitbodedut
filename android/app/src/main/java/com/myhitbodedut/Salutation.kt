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

    /**
     * Le mot du moment, sous la prière de l'heure (proposition 7 de la feuille
     * de route, 19/10) : quatre moments, la nuit comprise cette fois — ce
     * n'est plus une salutation, c'est une invitation.
     */
    fun mot(heure: Int): Int = when (heure) {
        in 5 until 12 -> R.string.moment_mot_matin
        in 12 until 18 -> R.string.moment_mot_journee
        in 18 until 23 -> R.string.moment_mot_soir
        else -> R.string.moment_mot_nuit
    }

    fun chaine(heure: Int): Int = when (partie(heure)) {
        Partie.MATIN -> R.string.hero_greeting_morning
        Partie.APRES_MIDI -> R.string.hero_greeting_afternoon
        Partie.SOIR -> R.string.hero_greeting_evening
    }
}
