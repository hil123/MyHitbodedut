package com.myhitbodedut

import android.content.Context
import android.content.SharedPreferences
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * Persistance locale : jours de pratique validés, fenêtre de blocage,
 * applications bloquées, report en cours. Rien ne quitte l'appareil.
 */
object Store {

    private const val PREFS = "myhitbodedut"
    private const val KEY_DONE = "done_dates"
    private const val KEY_APPS = "blocked_apps"
    private const val KEY_START = "window_start"
    private const val KEY_END = "window_end"
    private const val KEY_SNOOZE = "snooze_minutes"
    private const val KEY_SNOOZED_UNTIL = "snoozed_until"
    private const val KEY_ENABLED = "enabled"

    const val DEFAULT_START = 6 * 60
    const val DEFAULT_END = 11 * 60
    const val DEFAULT_SNOOZE = 15

    private val FORMAT: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    private fun prefs(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun today(): String = LocalDate.now().format(FORMAT)

    fun doneDates(context: Context): Set<String> =
        prefs(context).getStringSet(KEY_DONE, emptySet()) ?: emptySet()

    fun isDone(context: Context, date: String = today()): Boolean =
        doneDates(context).contains(date)

    fun markDoneToday(context: Context) {
        val dates = doneDates(context).toMutableSet()
        dates.add(today())
        prefs(context).edit().putStringSet(KEY_DONE, dates).apply()
        clearSnooze(context)
    }

    fun unmarkToday(context: Context) {
        val dates = doneDates(context).toMutableSet()
        dates.remove(today())
        prefs(context).edit().putStringSet(KEY_DONE, dates).apply()
    }

    /** Jours consécutifs validés, en remontant depuis aujourd'hui (ou hier si non fait). */
    fun streak(context: Context): Int {
        val dates = doneDates(context)
        var day = LocalDate.now()
        if (!dates.contains(day.format(FORMAT))) day = day.minusDays(1)
        var count = 0
        while (dates.contains(day.format(FORMAT))) {
            count++
            day = day.minusDays(1)
        }
        return count
    }

    /** Les 7 derniers jours, du plus ancien au plus récent. */
    fun lastWeek(context: Context): List<Pair<LocalDate, Boolean>> {
        val dates = doneDates(context)
        return (6 downTo 0).map { offset ->
            val day = LocalDate.now().minusDays(offset.toLong())
            day to dates.contains(day.format(FORMAT))
        }
    }

    fun blockedApps(context: Context): Set<String> =
        prefs(context).getStringSet(KEY_APPS, emptySet()) ?: emptySet()

    fun setBlockedApps(context: Context, apps: Set<String>) {
        prefs(context).edit().putStringSet(KEY_APPS, HashSet(apps)).apply()
    }

    fun windowStart(context: Context): Int = prefs(context).getInt(KEY_START, DEFAULT_START)

    fun windowEnd(context: Context): Int = prefs(context).getInt(KEY_END, DEFAULT_END)

    fun setWindow(context: Context, startMinutes: Int, endMinutes: Int) {
        prefs(context).edit().putInt(KEY_START, startMinutes).putInt(KEY_END, endMinutes).apply()
    }

    /** Vrai si l'heure courante est dans la fenêtre (gère le passage par minuit). */
    fun inWindow(context: Context): Boolean {
        val now = LocalTime.now()
        val minutes = now.hour * 60 + now.minute
        val start = windowStart(context)
        val end = windowEnd(context)
        return if (start <= end) minutes >= start && minutes < end
        else minutes >= start || minutes < end
    }

    fun snoozeMinutes(context: Context): Int = prefs(context).getInt(KEY_SNOOZE, DEFAULT_SNOOZE)

    fun setSnoozeMinutes(context: Context, minutes: Int) {
        prefs(context).edit().putInt(KEY_SNOOZE, minutes).apply()
    }

    fun snooze(context: Context) {
        val until = System.currentTimeMillis() + snoozeMinutes(context) * 60_000L
        prefs(context).edit().putLong(KEY_SNOOZED_UNTIL, until).apply()
    }

    fun clearSnooze(context: Context) {
        prefs(context).edit().putLong(KEY_SNOOZED_UNTIL, 0L).apply()
    }

    fun isSnoozed(context: Context): Boolean =
        prefs(context).getLong(KEY_SNOOZED_UNTIL, 0L) > System.currentTimeMillis()

    fun isEnabled(context: Context): Boolean = prefs(context).getBoolean(KEY_ENABLED, false)

    fun setEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_ENABLED, enabled).apply()
    }

    fun formatMinutes(minutes: Int): String =
        String.format(java.util.Locale.US, "%02d:%02d", minutes / 60, minutes % 60)
}
