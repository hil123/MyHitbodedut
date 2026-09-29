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

    /**
     * Dans combien de minutes tombe le prochain rappel ?
     *
     * Les heures sont celles de la journee, en minutes depuis minuit. Quand
     * toutes sont deja passees, on repart sur la premiere du lendemain —
     * d'ou le retour a travers minuit plutot qu'un simple `first { it > now }`.
     *
     * Rend null si aucune heure n'est reglee : il n'y a alors rien a
     * programmer, et l'appelant doit annuler l'alarme au lieu d'en poser une.
     */
    fun minutesUntilNext(nowMinutes: Int, heures: List<Int>): Int? {
        val propres = heures.filter { it in 0 until MINUTES_PAR_JOUR }.distinct().sorted()
        if (propres.isEmpty()) return null
        val suivante = propres.firstOrNull { it > nowMinutes }
        if (suivante != null) return suivante - nowMinutes
        return (MINUTES_PAR_JOUR - nowMinutes) + propres.first()
    }

    const val MINUTES_PAR_JOUR = 24 * 60

    /**
     * L'instant du prochain rappel, dans le fuseau de [maintenant].
     *
     * Calcule en heure murale, jour par jour : ajouter des minutes a
     * l'instant present decalait le rappel d'une heure les jours de
     * changement d'heure (revue du 29/09). Une heure qui n'existe pas ce
     * jour-la (2 h 30 au passage a l'heure d'ete) est repoussee d'autant,
     * comme le fait ZonedDateTime.
     */
    fun prochainDeclenchement(
        maintenant: java.time.ZonedDateTime,
        heures: List<Int>
    ): java.time.ZonedDateTime? {
        val propres = heures.filter { it in 0 until MINUTES_PAR_JOUR }.distinct().sorted()
        if (propres.isEmpty()) return null
        val jour = maintenant.toLocalDate()
        return (0L..1L).asSequence()
            .flatMap { decalage ->
                propres.asSequence().map { h ->
                    java.time.ZonedDateTime.of(
                        jour.plusDays(decalage).atTime(h / 60, h % 60),
                        maintenant.zone
                    )
                }
            }
            .firstOrNull { it.isAfter(maintenant) }
    }

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
