package com.myhitbodedut

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.tan

/**
 * La direction de Jérusalem (Har haBayit) depuis un lieu (28/10, boussole C).
 * Elle se calcule de deux façons, et l'application ne tranche pas :
 * - par le plus court chemin sur le globe (grand cercle, cap initial) ;
 * - selon la direction sur la carte (loxodromie, cap constant).
 * De New York : environ 54° et 96°. Les deux restent affichées ; le réglage
 * « direction principale » (Store.boussoleCarte) dit seulement laquelle le
 * hassid suit. Calcul sur une sphère : à moins d'un demi-degré du calcul sur
 * l'ellipsoïde (DirectionJerusalemTest).
 */
object DirectionJerusalem {

    /** Har haBayit, au Dôme : 31,7781° N, 35,2354° E. */
    const val LATITUDE = 31.7781
    const val LONGITUDE = 35.2354

    /** À ±4°, on est face à Jérusalem. */
    const val TOLERANCE = 4f

    private const val RAYON_KM = 6371.0

    private fun rad(d: Double) = d * PI / 180
    private fun deg(r: Double) = r * 180 / PI

    /** Un angle ramené entre 0 et 360. */
    fun norm(d: Double): Double = ((d % 360) + 360) % 360

    /** Le cap initial du grand cercle, en degrés depuis le nord vrai. */
    fun grandCercle(lat: Double, lon: Double): Double {
        val f1 = rad(lat)
        val f2 = rad(LATITUDE)
        val dl = rad(LONGITUDE - lon)
        return norm(deg(atan2(sin(dl) * cos(f2), cos(f1) * sin(f2) - sin(f1) * cos(f2) * cos(dl))))
    }

    /** Le cap constant de la loxodromie (la droite sur une carte de Mercator), en degrés depuis le nord vrai. */
    fun carte(lat: Double, lon: Double): Double {
        var dl = rad(LONGITUDE - lon)
        // Par le plus court côté du méridien de 180°.
        if (abs(dl) > PI) dl = if (dl > 0) dl - 2 * PI else dl + 2 * PI
        val q = ln(tan(PI / 4 + rad(LATITUDE) / 2) / tan(PI / 4 + rad(lat) / 2))
        return norm(deg(atan2(dl, q)))
    }

    /** La distance par le grand cercle, en kilomètres. */
    fun distanceKm(lat: Double, lon: Double): Double {
        val f1 = rad(lat)
        val f2 = rad(LATITUDE)
        val c = sin(f1) * sin(f2) + cos(f1) * cos(f2) * cos(rad(LONGITUDE - lon))
        return RAYON_KM * acos(c.coerceIn(-1.0, 1.0))
    }

    /** De combien tourner, de -180 (à gauche) à +180 (à droite), pour passer du cap [cap] à [cible]. */
    fun ecart(cible: Double, cap: Double): Double {
        val e = norm(cible - cap)
        return if (e > 180) e - 360 else e
    }

    /** En degrés entiers, de 0 à 359 (359,6° s'écrit 0°). */
    fun degres(d: Double): Int = Math.round(norm(d)).toInt() % 360

    fun face(ecart: Double) = abs(ecart) <= TOLERANCE

    /** Le point cardinal le plus proche, de 0 (N) à 7 (NO), dans le sens des aiguilles. */
    fun point(d: Double): Int = (norm(d) / 45).roundToInt() % 8

    /** La direction principale et l'autre, selon le réglage. */
    fun directions(lat: Double, lon: Double, carteEnAvant: Boolean): Pair<Double, Double> {
        val gc = grandCercle(lat, lon)
        val rh = carte(lat, lon)
        return if (carteEnAvant) rh to gc else gc to rh
    }
}
