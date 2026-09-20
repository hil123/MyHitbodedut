package com.myhitbodedut

import com.kosherjava.zmanim.hebrewcalendar.JewishCalendar
import java.time.LocalDate

/**
 * Calendrier hébraïque, hors ligne et sans réseau, via KosherJava 2.5.0,
 * dernière version publiée sous com.kosherjava:zmanim au 20/09/2026.
 *
 * Aucune méthode n'est devinée : isAssurBemelacha, isYomTovAssurBemelacha et
 * setInIsrael ont été lues dans le jar de cette version, et les jours de
 * Tichri 5787 vérifiés par exécution avant d'écrire ce fichier.
 *
 * Cette classe ne dépend pas d'Android, pour rester testable sur la machine
 * virtuelle seule.
 */
object Holidays {

    /** Shabbat ou Yom Tov : le jour où la pratique n'est pas attendue. */
    fun isRestDay(date: LocalDate, inIsrael: Boolean): Boolean =
        calendar(date, inIsrael).isAssurBemelacha

    /** Yom Tov seul, samedis ordinaires exclus. */
    fun isYomTov(date: LocalDate, inIsrael: Boolean): Boolean =
        calendar(date, inIsrael).isYomTovAssurBemelacha

    /** Vrai si le lendemain est chômé : la fête entre au coucher du soleil. */
    fun isEveOfRestDay(date: LocalDate, inIsrael: Boolean): Boolean =
        isRestDay(date.plusDays(1), inIsrael)

    /**
     * Israël par défaut si le téléphone est à l'heure de Jérusalem,
     * diaspora partout ailleurs. Réglable ensuite à la main.
     */
    fun defaultInIsrael(timeZoneId: String): Boolean = timeZoneId == "Asia/Jerusalem"

    private fun calendar(date: LocalDate, inIsrael: Boolean): JewishCalendar =
        JewishCalendar(date).apply { setInIsrael(inIsrael) }
}
