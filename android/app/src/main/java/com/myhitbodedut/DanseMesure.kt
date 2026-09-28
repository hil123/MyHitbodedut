package com.myhitbodedut

import kotlin.math.PI
import kotlin.math.floor
import kotlin.math.sin

/**
 * La mesure de la danse du bravo, sans Android : où en est le danseur à
 * l'instant t d'une mesure de deux temps (t de 0 à 1).
 *
 * Premier temps sur la pose A (jambe droite levée), second sur la pose B
 * (jambe gauche). Chaque temps est un saut : il décolle, monte, retombe,
 * et plie les genoux en touchant le sol, moment où la pose change.
 */
object DanseMesure {

    /** Un temps. 480 ms : un peu plus de deux pas par seconde, un niggoun joyeux. */
    const val TEMPS_MS = 480L

    /** La part du temps où les deux poses se relaient, à la réception. */
    private const val RELAIS = 0.1f

    /** La part du temps où les genoux plient après la réception. */
    private const val FLEXION = 0.14f

    data class Pas(
        /** 0 au sol, 1 en haut du saut. */
        val hauteur: Float,
        /** 1 à la réception, 0 jambes tendues. */
        val flexion: Float,
        /** -1 penché d'un côté, 1 de l'autre. */
        val balance: Float,
        /** Visibilité de la pose B ; celle de la pose A est son complément. */
        val poseB: Float
    )

    fun pas(t: Float): Pas {
        val mesure = t - floor(t)
        val temps = mesure * 2f
        val second = temps >= 1f
        val p = temps - floor(temps)
        val relais = (p / RELAIS).coerceAtMost(1f)
        return Pas(
            hauteur = sin(PI * p).toFloat().coerceAtLeast(0f),
            flexion = (1f - p / FLEXION).coerceAtLeast(0f),
            balance = sin(2 * PI * mesure).toFloat(),
            poseB = if (second) relais else 1f - relais
        )
    }
}
