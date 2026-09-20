package com.myhitbodedut

/**
 * Faut-il sonner le rappel maintenant ?
 *
 * Le calendrier hébraïque raisonne en date civile, alors que la fête entre au
 * coucher du soleil la veille. Un rappel du soir tomberait donc en pleine
 * entrée de Shabbat ou de Yom Tov. Avec une ville réglée on utiliserait le
 * coucher du soleil ; l'application n'en demande aucune, pour ne pas réclamer
 * la localisation, et retient donc midi comme limite sûre.
 *
 * Fonction pure, sans Android, pour rester testable.
 */
object ReminderRule {

    /** Limite retenue faute de coucher du soleil calculable. */
    const val NO_SUNSET_CUTOFF_MINUTES = 12 * 60

    fun shouldNotify(
        nowMinutes: Int,
        doneToday: Boolean,
        todayIsRestDay: Boolean,
        tomorrowIsRestDay: Boolean,
        sunsetMinutes: Int?
    ): Boolean {
        if (doneToday) return false
        if (todayIsRestDay) return false
        if (!tomorrowIsRestDay) return true
        val cutoff = sunsetMinutes ?: NO_SUNSET_CUTOFF_MINUTES
        return nowMinutes < cutoff
    }
}
