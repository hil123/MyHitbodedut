package com.myhitbodedut

import java.time.LocalDate

/**
 * Le code-barres d'une carte d'embarquement (04/11) : le format BCBP de l'IATA (résolution 792),
 * le même pour toutes les compagnies, dans le PDF417, l'Aztec ou le QR de la carte, sur papier,
 * en PDF ou dans le téléphone. On n'en lit que le premier tronçon : départ, arrivée, compagnie,
 * numéro, jour de l'année. Le nom du passager et le numéro de réservation ne sont ni lus ni gardés.
 * Pas les heures : le code ne les porte pas ; elles viennent du texte du billet, ou de la saisie.
 */
object CarteEmbarquement {

    data class Troncon(val depart: String, val arrivee: String, val compagnie: String, val numero: String, val date: LocalDate?)

    /**
     * « M1DUPONT/JEAN        EABC123 LYSTLVLY 0382 316Y012A0001 100 » → LYS, TLV, LY, LY382, le
     * 316e jour, l'année la plus proche d'aujourd'hui.
     */
    fun lire(code: String, aujourdhui: LocalDate): Troncon? {
        if (code.length < 47 || code[0] != 'M' || !code[1].isDigit()) return null
        val de = code.substring(30, 33).trim()
        val vers = code.substring(33, 36).trim()
        val compagnie = code.substring(36, 39).trim()
        val vol = code.substring(39, 44).trim().trimStart('0')
        val jour = code.substring(44, 47).trim().toIntOrNull()
        if (!Regex("[A-Z]{3}").matches(de) || !Regex("[A-Z]{3}").matches(vers)) return null
        if (!Regex("[A-Z0-9]{2,3}").matches(compagnie) || !Regex("[0-9]{1,4}[A-Z]?").matches(vol)) return null
        val date = jour?.takeIf { it in 1..366 }?.let { quantieme(it, aujourdhui) }
        return Troncon(de, vers, compagnie, compagnie + vol, date)
    }

    /** Le jour de l'année le plus proche d'aujourd'hui : l'an dernier, cette année ou l'an prochain. */
    private fun quantieme(n: Int, aujourdhui: LocalDate): LocalDate? =
        listOf(aujourdhui.year - 1, aujourdhui.year, aujourdhui.year + 1)
            .mapNotNull { an -> runCatching { LocalDate.ofYearDay(an, n) }.getOrNull() }
            .minByOrNull { kotlin.math.abs(it.toEpochDay() - aujourdhui.toEpochDay()) }
}
