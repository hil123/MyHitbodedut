package com.myhitbodedut

import java.time.LocalDate

/**
 * Les illustrations du personnage, réparties par moment de la journée.
 * Le choix est stable sur une journée entière et change le lendemain,
 * pour que l'app paraisse vivante sans jamais clignoter sous les yeux.
 */
object Character {

    /** Accueil, quand la pratique du jour reste à faire. */
    private val GREETING = intArrayOf(
        R.drawable.char_welcome,
        R.drawable.char_offering,
        R.drawable.char_pointing,
        R.drawable.char_praise
    )

    /** Appel à la pratique, sur l'écran de garde. */
    private val CALL = intArrayOf(
        R.drawable.char_praise,
        R.drawable.char_pointing,
        R.drawable.char_walking,
        R.drawable.char_tehilim,
        R.drawable.char_thinking,
        R.drawable.char_welcome,
        R.drawable.char_open_arms
    )

    /** Pendant la séance : seul, tourné vers le haut. */
    private val DURING = intArrayOf(
        R.drawable.char_arms_raised,
        R.drawable.char_praying,
        R.drawable.char_standing_back,
        R.drawable.char_sitting_low
    )

    /** Après la séance, ou journée déjà validée. */
    private val JOY = intArrayOf(
        R.drawable.char_dancing_joy,
        R.drawable.char_dancing,
        R.drawable.char_guitar,
        R.drawable.char_thumbs_up,
        R.drawable.char_open_arms
    )

    val JOURNAL = R.drawable.char_writing
    val STUDY = R.drawable.char_torah
    val READING = R.drawable.char_reading_seated

    /** Le Tikoun HaKlali : dix psaumes, livre en main. */
    val TEHILIM = R.drawable.char_tehilim

    /**
     * Les deux natures d'entrée de la bibliothèque. Une source se tient
     * devant le livre ; un article est un texte qu'on a écrit.
     */
    fun ofLibraryEntry(entry: LibraryEntry): Int = when (entry) {
        is SourceEntry -> R.drawable.char_torah
        is ArticleEntry -> R.drawable.char_writing
    }

    /**
     * L'illustration d'une carte de 'hizouk, choisie par le moment plutôt
     * que par le jour : ici l'image dit ce qu'on est en train de vivre, elle
     * ne doit donc pas tourner comme les autres.
     */
    fun ofMoment(moment: Moment): Int = when (moment) {
        // On se présente, les mains ouvertes.
        Moment.OUVERTURE -> R.drawable.char_open_arms
        // Les mots ne viennent pas : assis, en attente.
        Moment.BLOCAGE -> R.drawable.char_sitting_low
        // Sur le pas de la porte, le téléphone à la main.
        Moment.PORTE -> R.drawable.char_pointing
        // Reprendre la route après une rupture.
        Moment.RELANCE -> R.drawable.char_walking
        // La séance s'achève : on remercie.
        Moment.CLOTURE -> R.drawable.char_praise
        // Le lieu et le moment : seul, assis face au paysage (planche du 11/10).
        Moment.LIEU -> R.drawable.scene_nature
    }

    fun greeting(done: Boolean): Int = if (done) ofDay(JOY, 3) else ofDay(GREETING, 0)

    fun call(): Int = ofDay(CALL, 1)

    fun during(): Int = ofDay(DURING, 2)

    fun joy(): Int = ofDay(JOY, 3)

    private fun ofDay(pool: IntArray, salt: Int): Int {
        val day = LocalDate.now().toEpochDay() + salt * 7L
        val index = ((day % pool.size) + pool.size) % pool.size
        return pool[index.toInt()]
    }
}
