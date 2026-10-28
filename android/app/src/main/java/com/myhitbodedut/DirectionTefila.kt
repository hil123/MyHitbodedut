package com.myhitbodedut

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.sign
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

/**
 * La direction de la tefila (28/10, « une boussole pour la direction de la tefila en fonction
 * de la position exacte »). Kotlin pur, testé (DirectionTefilaTest) ; miroir de
 * web/src/js/direction.js.
 *
 * Vérifié : on se tourne vers la terre d'Israël, Jérusalem, le Temple et le Saint des saints
 * (Berakhot 30a ; Choul'han Aroukh, O.H. 94:1 ; recoupé sur Peninei Halakha et Chabad.org).
 * Discuté : sur la sphère, la route la plus courte (orthodromie, « grand cercle ») et le cap
 * constant d'une carte (loxodromie) diffèrent loin d'Israël — depuis New York, nord-est contre
 * plein est. Les deux opinions existent (Torah Musings, 2012 ; Chabad.org, « Why Do We Face
 * East ») : l'écran montre la première et nomme la seconde. Jamais tranché ici.
 *
 * Cible : le mont du Temple, au Dôme du Rocher (31,7780 N ; 35,2354 E), arrondi au mètre près
 * de ce que donnent les cartes ; l'emplacement exact du Saint des saints est lui-même discuté.
 */
object DirectionTefila {
    const val CIBLE_LAT = 31.7780
    const val CIBLE_LON = 35.2354
    private const val RAYON_KM = 6371.0

    private fun rad(d: Double) = d * PI / 180
    private fun deg(r: Double) = r * 180 / PI
    private fun cap(r: Double) = (deg(r) + 360) % 360

    /** Le cap initial de la route la plus courte, en degrés depuis le nord géographique. */
    fun orthodromie(lat: Double, lon: Double): Double {
        val p1 = rad(lat)
        val p2 = rad(CIBLE_LAT)
        val dl = rad(CIBLE_LON - lon)
        return cap(atan2(sin(dl) * cos(p2), cos(p1) * sin(p2) - sin(p1) * cos(p2) * cos(dl)))
    }

    /** Le cap constant (ligne droite sur une carte de Mercator), en degrés depuis le nord géographique. */
    fun loxodromie(lat: Double, lon: Double): Double {
        var dl = rad(CIBLE_LON - lon)
        if (abs(dl) > PI) dl -= sign(dl) * 2 * PI
        val dp = ln(tan(PI / 4 + rad(CIBLE_LAT) / 2) / tan(PI / 4 + rad(lat) / 2))
        return cap(atan2(dl, dp))
    }

    /** La distance au mont du Temple par la route la plus courte, en kilomètres (haversine). */
    fun distanceKm(lat: Double, lon: Double): Double {
        val dp = rad(CIBLE_LAT - lat)
        val dl = rad(CIBLE_LON - lon)
        val a = sin(dp / 2) * sin(dp / 2) + cos(rad(lat)) * cos(rad(CIBLE_LAT)) * sin(dl / 2) * sin(dl / 2)
        return 2 * RAYON_KM * atan2(sqrt(a), sqrt(1 - a))
    }

    /** Le point cardinal le plus proche d'un cap (0 = nord, 1 = nord-est… 7 = nord-ouest). */
    fun octant(cap: Double): Int = (((cap % 360 + 360) % 360 + 22.5) / 45).toInt() % 8
}
