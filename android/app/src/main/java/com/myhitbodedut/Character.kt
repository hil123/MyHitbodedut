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
        R.drawable.char_welcome
    )

    /** Pendant la séance : seul, tourné vers le haut. */
    private val DURING = intArrayOf(
        R.drawable.char_arms_raised,
        R.drawable.char_praying,
        R.drawable.char_arms_raised_guitar,
        R.drawable.char_standing_back,
        R.drawable.char_sitting_low
    )

    /** Après la séance, ou journée déjà validée. */
    private val JOY = intArrayOf(
        R.drawable.char_dancing_joy,
        R.drawable.char_dancing,
        R.drawable.char_guitar,
        R.drawable.char_thumbs_up
    )

    val JOURNAL = R.drawable.char_writing
    val STUDY = R.drawable.char_torah
    val READING = R.drawable.char_reading_seated

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
