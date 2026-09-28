package com.myhitbodedut

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONObject
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Une case de la bande hebdomadaire. */
data class WeekDay(
    val date: LocalDate,
    val done: Boolean,
    val exempt: Boolean,
    val shabbat: Boolean,
    val future: Boolean
)

/**
 * Une journée de pratique : durée effective, note laissée après la séance,
 * et les sujets des séances de ce jour-là.
 *
 * Les sujets sont une liste et non une valeur : on peut faire deux séances
 * dans la journée et parler d'autre chose la seconde fois. Elle est vide
 * pour toute journée d'avant cette version, et pour toute séance dont on
 * n'a pas voulu dire le sujet — il est facultatif.
 */
data class DayRecord(
    val date: String,
    val minutes: Int,
    val note: String,
    val themes: List<String> = emptyList()
)

/**
 * Toute la persistance de l'application, dans les préférences locales.
 * Rien ne quitte l'appareil : aucune permission Internet n'est déclarée.
 */
object Store {

    private const val PREFS = "myhitbodedut"

    /**
     * Journal et notes vivent dans un fichier séparé, exclu de la sauvegarde
     * automatique d'Android. La sauvegarde raisonne par fichier et non par
     * clé : garder les réglages tout en écartant le journal impose la
     * séparation. L'export JSON manuel reste la voie pour les emporter.
     */
    private const val PREFS_JOURNAL = "myhitbodedut-journal"

    private const val KEY_DAYS = "days"
    private const val KEY_LEGACY_DONE = "done_dates"
    private const val KEY_APPS = "blocked_apps"
    private const val KEY_START = "window_start"
    private const val KEY_END = "window_end"
    private const val KEY_SNOOZE = "snooze_minutes"
    private const val KEY_SNOOZED_UNTIL = "snoozed_until"
    private const val KEY_ENABLED = "enabled"
    private const val KEY_TARGET = "target_minutes"
    private const val KEY_PAUSE_SHABBAT = "pause_shabbat"
    private const val KEY_IN_ISRAEL = "in_israel"
    private const val KEY_PAUSED_DATES = "paused_dates"
    private const val KEY_REMINDER_ON = "reminder_on"
    private const val KEY_REMINDER_MIN = "reminder_minutes"
    private const val KEY_REMINDER_TIMES = "reminder_times"
    private const val KEY_REMINDER_SNOOZE = "reminder_snoozed_until"
    private const val KEY_SESSION_END = "session_ends_at"
    private const val KEY_SESSION_TARGET = "session_target"
    private const val KEY_SESSION_START = "session_started_at"
    private const val KEY_SESSION_THEME = "session_theme"
    private const val KEY_HIDE_COUNTDOWN = "hide_countdown"
    private const val KEY_GUIDED = "guided_hitbodedut"
    private const val KEY_ONBOARDED = "onboarded"
    private const val KEY_NAME = "first_name"
    private const val KEY_MUSIC_ON = "music_enabled"
    private const val KEY_MUSIC_TRACK = "music_track"
    private const val KEY_MUSIC_VOLUME = "music_volume"
    private const val KEY_MUSIC_USER = "music_user_tracks"
    private const val KEY_TIKKUN_DAY = "tikkun_day"
    private const val KEY_TIKKUN_DONE = "tikkun_done"
    private const val KEY_CHIZUK_BAG = "chizuk_bag_"
    private const val KEY_CHIZUK_LAST = "chizuk_last_"
    private const val KEY_MUSIC_NAMES = "music_names"

    const val DEFAULT_START = 6 * 60
    const val DEFAULT_END = 11 * 60
    const val DEFAULT_SNOOZE = 15
    const val DEFAULT_TARGET = 60
    const val DEFAULT_REMINDER = 7 * 60
    const val DEFAULT_VOLUME = 60

    private val FORMAT: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    private fun prefs(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun journalPrefs(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_JOURNAL, Context.MODE_PRIVATE)

    fun today(): String = LocalDate.now().format(FORMAT)

    fun parse(date: String): LocalDate = LocalDate.parse(date, FORMAT)

    // --- Journées -----------------------------------------------------------

    /** Lit la carte des journées, en reprenant l'ancien format s'il existe encore. */
    fun days(context: Context): Map<String, DayRecord> {
        val result = linkedMapOf<String, DayRecord>()
        // Fichier du journal d'abord, puis reprise de l'ancien emplacement.
        val raw = journalPrefs(context).getString(KEY_DAYS, null)
            ?: prefs(context).getString(KEY_DAYS, null)
        if (raw != null) {
            runCatching {
                val root = JSONObject(raw)
                for (date in root.keys()) {
                    val entry = root.optJSONObject(date) ?: continue
                    result[date] = DayRecord(
                        date = date,
                        minutes = entry.optInt("minutes", 0),
                        note = entry.optString("note", ""),
                        themes = readThemes(entry)
                    )
                }
            }
        }
        // Reprise des jours validés par les versions précédentes.
        val legacy = prefs(context).getStringSet(KEY_LEGACY_DONE, emptySet()) ?: emptySet()
        legacy.forEach { date ->
            if (!result.containsKey(date)) result[date] = DayRecord(date, 0, "")
        }
        return result
    }

    private fun writeDays(context: Context, days: Map<String, DayRecord>) {
        val root = JSONObject()
        days.values.forEach { record ->
            root.put(record.date, toJson(record))
        }
        journalPrefs(context).edit().putString(KEY_DAYS, root.toString()).apply()
        // L'ancien emplacement, dans le fichier des réglages, est vidé.
        prefs(context).edit().remove(KEY_DAYS).remove(KEY_LEGACY_DONE).apply()
    }

    /** Les sujets d'une journée, absents des journaux d'avant cette version. */
    private fun readThemes(entry: JSONObject): List<String> {
        val array = entry.optJSONArray("themes") ?: return emptyList()
        return (0 until array.length()).mapNotNull { array.optString(it).ifBlank { null } }
    }

    private fun toJson(record: DayRecord): JSONObject {
        val entry = JSONObject().put("minutes", record.minutes).put("note", record.note)
        // Une clé absente vaut mieux qu'un tableau vide : le journal des
        // journées sans sujet reste exactement ce qu'il était.
        if (record.themes.isNotEmpty()) {
            entry.put("themes", org.json.JSONArray(record.themes))
        }
        return entry
    }

    fun record(context: Context, date: String = today()): DayRecord? = days(context)[date]

    fun isDone(context: Context, date: String = today()): Boolean = days(context).containsKey(date)

    /** Valide la journée. Les minutes s'ajoutent si plusieurs séances ont lieu. */
    fun addSession(
        context: Context,
        minutes: Int,
        note: String? = null,
        theme: Theme? = null
    ) {
        val all = days(context).toMutableMap()
        val existing = all[today()]
        all[today()] = DayRecord(
            date = today(),
            minutes = (existing?.minutes ?: 0) + minutes.coerceAtLeast(0),
            note = note ?: existing?.note ?: "",
            // Le sujet s'ajoute à ceux du jour ; sans sujet, rien n'est ajouté.
            themes = (existing?.themes ?: emptyList()) + listOfNotNull(theme?.cle)
        )
        writeDays(context, all)
        clearSnooze(context)
    }

    fun setNote(context: Context, date: String, note: String) {
        val all = days(context).toMutableMap()
        val existing = all[date] ?: DayRecord(date, 0, "", emptyList())
        all[date] = existing.copy(note = note)
        writeDays(context, all)
    }

    fun unmarkToday(context: Context) {
        val all = days(context).toMutableMap()
        all.remove(today())
        writeDays(context, all)
    }

    fun totalMinutes(context: Context): Int = days(context).values.sumOf { it.minutes }

    // --- Jours sans obligation ---------------------------------------------

    /** Shabbat et Yom Tov : un seul réglage, la clé d'origine est conservée. */
    fun shabbatMode(context: Context): Boolean = prefs(context).getBoolean(KEY_PAUSE_SHABBAT, true)

    fun setShabbatMode(context: Context, value: Boolean) {
        prefs(context).edit().putBoolean(KEY_PAUSE_SHABBAT, value).apply()
    }

    /** Israël ou diaspora : décide du deuxième jour de fête. */
    fun inIsrael(context: Context): Boolean = prefs(context).getBoolean(
        KEY_IN_ISRAEL,
        Holidays.defaultInIsrael(java.util.TimeZone.getDefault().id)
    )

    fun setInIsrael(context: Context, value: Boolean) {
        prefs(context).edit().putBoolean(KEY_IN_ISRAEL, value).apply()
    }

    fun pausedDates(context: Context): Set<String> =
        prefs(context).getStringSet(KEY_PAUSED_DATES, emptySet()) ?: emptySet()

    fun togglePausedDate(context: Context, date: String) {
        val dates = pausedDates(context).toMutableSet()
        if (!dates.remove(date)) dates.add(date)
        prefs(context).edit().putStringSet(KEY_PAUSED_DATES, HashSet(dates)).apply()
    }

    /**
     * Jour où la pratique n'est pas attendue : Shabbat ou Yom Tov si l'option
     * est active, ou une date mise en pause à la main pour un voyage.
     */
    fun isExempt(context: Context, date: LocalDate): Boolean {
        if (pausedDates(context).contains(date.format(FORMAT))) return true
        return shabbatMode(context) && Holidays.isRestDay(date, inIsrael(context))
    }

    /** Veille d'un jour chômé, pour taire le rappel du soir. */
    fun isEveOfRestDay(context: Context, date: LocalDate): Boolean =
        shabbatMode(context) && Holidays.isEveOfRestDay(date, inIsrael(context))

    // --- Série --------------------------------------------------------------

    /** Jours consécutifs validés. Les jours exemptés ne comptent pas et ne cassent pas. */
    fun streak(context: Context): Int {
        val done = days(context).keys
        var day = LocalDate.now()
        if (!done.contains(day.format(FORMAT)) && !isExempt(context, day)) day = day.minusDays(1)
        var count = 0
        var guard = 0
        while (guard++ < 3650) {
            val key = day.format(FORMAT)
            when {
                done.contains(key) -> count++
                isExempt(context, day) -> Unit
                else -> return count
            }
            day = day.minusDays(1)
        }
        return count
    }

    /**
     * La semaine en cours, du dimanche au samedi : c'est ainsi que se compte la
     * semaine juive, et le samedi doit tomber en fin de bande, pas au milieu.
     */
    fun currentWeek(context: Context): List<WeekDay> {
        val done = days(context).keys
        val today = LocalDate.now()
        // DayOfWeek va de lundi=1 à dimanche=7 ; le reste modulo 7 ramène au dimanche.
        val sunday = today.minusDays((today.dayOfWeek.value % 7).toLong())
        return (0..6).map { offset ->
            val day = sunday.plusDays(offset.toLong())
            WeekDay(
                date = day,
                done = done.contains(day.format(FORMAT)),
                exempt = isExempt(context, day),
                shabbat = day.dayOfWeek == DayOfWeek.SATURDAY,
                future = day.isAfter(today)
            )
        }
    }

    // --- Applications bloquées ---------------------------------------------

    fun blockedApps(context: Context): Set<String> =
        prefs(context).getStringSet(KEY_APPS, emptySet()) ?: emptySet()

    fun setBlockedApps(context: Context, apps: Set<String>) {
        prefs(context).edit().putStringSet(KEY_APPS, HashSet(apps)).apply()
    }

    // --- Fenêtre, report, durée --------------------------------------------

    fun windowStart(context: Context): Int = prefs(context).getInt(KEY_START, DEFAULT_START)

    fun windowEnd(context: Context): Int = prefs(context).getInt(KEY_END, DEFAULT_END)

    fun setWindow(context: Context, startMinutes: Int, endMinutes: Int) {
        prefs(context).edit().putInt(KEY_START, startMinutes).putInt(KEY_END, endMinutes).apply()
    }

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

    fun snoozedUntil(context: Context): Long = prefs(context).getLong(KEY_SNOOZED_UNTIL, 0L)

    fun snooze(context: Context) {
        val until = System.currentTimeMillis() + snoozeMinutes(context) * 60_000L
        prefs(context).edit().putLong(KEY_SNOOZED_UNTIL, until).apply()
    }

    fun clearSnooze(context: Context) {
        prefs(context).edit().putLong(KEY_SNOOZED_UNTIL, 0L).apply()
    }

    fun isSnoozed(context: Context): Boolean = snoozedUntil(context) > System.currentTimeMillis()

    fun targetMinutes(context: Context): Int = prefs(context).getInt(KEY_TARGET, DEFAULT_TARGET)

    fun setTargetMinutes(context: Context, minutes: Int) {
        prefs(context).edit().putInt(KEY_TARGET, minutes).apply()
    }

    fun isEnabled(context: Context): Boolean = prefs(context).getBoolean(KEY_ENABLED, false)

    fun setEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_ENABLED, enabled).apply()
    }

    // --- Rappel quotidien ---------------------------------------------------

    fun reminderEnabled(context: Context): Boolean = prefs(context).getBoolean(KEY_REMINDER_ON, false)

    fun setReminderEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_REMINDER_ON, enabled).apply()
    }

    fun reminderMinutes(context: Context): Int = prefs(context).getInt(KEY_REMINDER_MIN, DEFAULT_REMINDER)

    fun setReminderMinutes(context: Context, minutes: Int) {
        prefs(context).edit().putInt(KEY_REMINDER_MIN, minutes).apply()
    }

    /**
     * Les heures de rappel de la journee, en minutes depuis minuit, triees.
     *
     * Un seul rappel se rate facilement — on est au volant, en reunion, le
     * telephone est dans une poche. Plusieurs heures donnent plusieurs
     * chances sans rien changer a la regle : le rappel se tait des que la
     * seance est faite.
     *
     * L'ancien reglage a une seule heure est repris au premier acces, pour
     * que personne ne perde l'heure qu'il avait choisie.
     */
    fun reminderTimes(context: Context): List<Int> {
        val brut = prefs(context).getString(KEY_REMINDER_TIMES, null)
        if (brut == null) return listOf(reminderMinutes(context))
        return normaliseHeures(brut.split(',').mapNotNull { it.trim().toIntOrNull() })
    }

    fun setReminderTimes(context: Context, heures: List<Int>) {
        val propres = normaliseHeures(heures)
        prefs(context).edit()
            .putString(KEY_REMINDER_TIMES, propres.joinToString(","))
            // L'ancienne cle reste a jour : l'export et une version
            // precedente de l'application y lisent encore la premiere heure.
            .putInt(KEY_REMINDER_MIN, propres.firstOrNull() ?: DEFAULT_REMINDER)
            .apply()
    }

    /** Tri, deduplication, et rejet de ce qui ne tombe pas dans la journee. */
    private fun normaliseHeures(heures: List<Int>): List<Int> =
        heures.filter { it in 0 until 24 * 60 }.distinct().sorted()

    /** Un rappel reporte, qui doit sonner une fois a cette heure-la. */
    fun reminderSnoozedUntil(context: Context): Long =
        prefs(context).getLong(KEY_REMINDER_SNOOZE, 0L)

    fun setReminderSnoozedUntil(context: Context, millis: Long) {
        prefs(context).edit().putLong(KEY_REMINDER_SNOOZE, millis).apply()
    }

    // --- Séance en cours ----------------------------------------------------

    fun sessionEndsAt(context: Context): Long = prefs(context).getLong(KEY_SESSION_END, 0L)

    fun sessionStartedAt(context: Context): Long = prefs(context).getLong(KEY_SESSION_START, 0L)

    fun sessionTarget(context: Context): Int = prefs(context).getInt(KEY_SESSION_TARGET, 0)

    fun isSessionRunning(context: Context): Boolean = SessionRule.enCours(
        sessionStartedAt(context),
        sessionEndsAt(context),
        sessionTarget(context),
        System.currentTimeMillis()
    )

    /** Une séance sans durée, que seul le geste de l'arrêter termine. */
    fun isSessionOpen(context: Context): Boolean =
        SessionRule.estOuverte(sessionStartedAt(context), sessionTarget(context))

    fun sessionElapsedMinutes(context: Context): Int =
        SessionRule.minutesEcoulees(sessionStartedAt(context), System.currentTimeMillis())

    /**
     * Le sujet de la séance en cours, ou null — il est facultatif, et
     * l'absence de valeur est la façon dont « aucun » s'écrit.
     */
    fun sessionTheme(context: Context): Theme? =
        ThemeRules.parse(prefs(context).getString(KEY_SESSION_THEME, null))

    /**
     * Le sujet choisi pour la prochaine séance, ou pour celle en cours.
     * Il se change pendant la séance : on découvre parfois en parlant de
     * quoi on était venu parler.
     */
    fun setSessionTheme(context: Context, theme: Theme?) {
        prefs(context).edit().putString(KEY_SESSION_THEME, theme?.cle).apply()
    }

    /** Une durée nulle ouvre la séance : rien n'est inscrit comme fin. */
    fun beginSession(context: Context, minutes: Int) {
        val now = System.currentTimeMillis()
        val ouverte = minutes <= SessionRule.OUVERTE
        prefs(context).edit()
            .putLong(KEY_SESSION_START, now)
            .putLong(KEY_SESSION_END, if (ouverte) 0L else now + minutes * 60_000L)
            .putInt(KEY_SESSION_TARGET, if (ouverte) SessionRule.OUVERTE else minutes)
            .apply()
    }

    /** Termine la séance et renvoie les minutes réellement écoulées. */
    fun endSession(context: Context): Int {
        val started = sessionStartedAt(context)
        val elapsed = if (started == 0L) 0
        else ((System.currentTimeMillis() - started) / 60_000L).toInt()
        prefs(context).edit()
            .remove(KEY_SESSION_START)
            .remove(KEY_SESSION_END)
            .remove(KEY_SESSION_TARGET)
            .apply()
        return elapsed.coerceAtLeast(0)
    }

    /**
     * Masquer le décompte pendant la séance.
     *
     * Regarder le temps qui reste est une façon de ne pas être là. L'option
     * retire le chiffre de partout — écran, notification, barrage — et ne
     * laisse qu'un son discret à la fin. Elle est éteinte par défaut : c'est
     * un choix, pas une opinion imposée.
     */
    fun hideCountdown(context: Context): Boolean =
        prefs(context).getBoolean(KEY_HIDE_COUNTDOWN, false)

    fun setHideCountdown(context: Context, hidden: Boolean) {
        prefs(context).edit().putBoolean(KEY_HIDE_COUNTDOWN, hidden).apply()
    }

    /**
     * La hitbodedout guidée : les quatre temps de Likoutei Moharan II, 25
     * affichés pendant la séance.
     *
     * Éteinte par défaut. Celui qui sait quoi dire n'a pas besoin qu'on le
     * lui souffle, et une séance encadrée sans qu'on l'ait demandé serait
     * le contraire de ce que le mot veut dire.
     */
    fun guidedHitbodedut(context: Context): Boolean =
        prefs(context).getBoolean(KEY_GUIDED, false)

    fun setGuidedHitbodedut(context: Context, guided: Boolean) {
        prefs(context).edit().putBoolean(KEY_GUIDED, guided).apply()
    }

    /**
     * L'accueil du premier lancement a déjà été vu.
     *
     * Une installation existante ne doit pas le voir apparaître : la clé
     * est posée au premier passage par l'écran principal si l'application
     * a déjà servi, ce que la présence d'un réglage ou d'une journée
     * suffit à établir.
     */
    fun hasOnboarded(context: Context): Boolean =
        prefs(context).getBoolean(KEY_ONBOARDED, false)

    fun setOnboarded(context: Context, seen: Boolean) {
        prefs(context).edit().putBoolean(KEY_ONBOARDED, seen).apply()
    }

    /** L'application a déjà servi : une journée notée, ou un réglage touché. */
    fun hasHistory(context: Context): Boolean =
        days(context).isNotEmpty() || prefs(context).contains(KEY_TARGET) ||
            prefs(context).contains(KEY_REMINDER_TIMES) || prefs(context).contains(KEY_APPS)

    /**
     * Le prénom, pour que l'accueil s'adresse à quelqu'un.
     *
     * Vide par défaut, et vide est une réponse : l'accueil se tait alors
     * plutôt que de saluer un blanc. Il ne sort pas de l'appareil, comme
     * le reste.
     */
    fun firstName(context: Context): String =
        prefs(context).getString(KEY_NAME, "").orEmpty().trim()

    fun setFirstName(context: Context, name: String) {
        prefs(context).edit().putString(KEY_NAME, name.trim().take(40)).apply()
    }

    /** Ce que le compteur doit dire, ici et maintenant. */
    fun sessionCounter(context: Context): SessionRule.Compteur =
        SessionRule.compteur(isSessionOpen(context), hideCountdown(context))

    // --- Musique ------------------------------------------------------------

    // Active par défaut : éteinte, une séance restait muette sans que rien ne
    // dise pourquoi, et « la musique ne marche pas » était le seul constat
    // possible. Un choix explicite de l'utilisateur, lui, est conservé.
    fun musicEnabled(context: Context): Boolean = prefs(context).getBoolean(KEY_MUSIC_ON, true)

    fun setMusicEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_MUSIC_ON, enabled).apply()
    }

    fun musicTrackId(context: Context): String? = prefs(context).getString(KEY_MUSIC_TRACK, null)

    fun setMusicTrackId(context: Context, id: String?) {
        prefs(context).edit().putString(KEY_MUSIC_TRACK, id).apply()
    }

    fun musicVolume(context: Context): Int = prefs(context).getInt(KEY_MUSIC_VOLUME, DEFAULT_VOLUME)

    fun setMusicVolume(context: Context, percent: Int) {
        prefs(context).edit().putInt(KEY_MUSIC_VOLUME, percent.coerceIn(0, 100)).apply()
    }

    fun musicUserTracks(context: Context): String =
        prefs(context).getString(KEY_MUSIC_USER, "[]") ?: "[]"

    fun setMusicUserTracks(context: Context, json: String) {
        prefs(context).edit().putString(KEY_MUSIC_USER, json).apply()
    }

    fun musicNames(context: Context): String =
        prefs(context).getString(KEY_MUSIC_NAMES, "{}") ?: "{}"

    fun setMusicNames(context: Context, json: String) {
        prefs(context).edit().putString(KEY_MUSIC_NAMES, json).apply()
    }

    // --- Tikoun HaKlali -------------------------------------------------------

    /**
     * Les psaumes deja dits aujourd'hui.
     *
     * Le Tikoun se dit d'un trait, mais rarement sans interruption : la
     * progression est gardee pour qu'un appel ou un enfant ne renvoie pas au
     * premier psaume. Elle repart a zero chaque jour — c'est une pratique
     * quotidienne, pas un compteur qui s'accumule.
     */
    fun tikkunDone(context: Context): Set<Int> {
        val prefs = prefs(context)
        if (prefs.getString(KEY_TIKKUN_DAY, "") != today()) return emptySet()
        return prefs.getString(KEY_TIKKUN_DONE, "").orEmpty()
            .split(',')
            .mapNotNull { it.trim().toIntOrNull() }
            .toSet()
    }

    fun setTikkunDone(context: Context, numero: Int, done: Boolean) {
        val courant = tikkunDone(context).toMutableSet()
        if (done) courant.add(numero) else courant.remove(numero)
        prefs(context).edit()
            .putString(KEY_TIKKUN_DAY, today())
            .putString(KEY_TIKKUN_DONE, courant.sorted().joinToString(","))
            .apply()
    }

    fun resetTikkun(context: Context) {
        prefs(context).edit()
            .putString(KEY_TIKKUN_DAY, today())
            .putString(KEY_TIKKUN_DONE, "")
            .apply()
    }

    // --- Rotation des textes de 'hizouk --------------------------------------

    /**
     * Le sac mélangé d'un moment : les identifiants qui n'ont pas encore
     * servi dans le cycle en cours. Un identifiant ne contient ni espace ni
     * saut de ligne, la liste tient donc dans une chaîne.
     */
    fun chizukBag(context: Context, moment: Moment): List<String> =
        prefs(context).getString(KEY_CHIZUK_BAG + moment.key, "")
            .orEmpty()
            .split('\n')
            .filter { it.isNotBlank() }

    /** Le dernier texte sorti pour ce moment, pour ne pas le redonner aussitôt. */
    fun chizukLast(context: Context, moment: Moment): String? =
        prefs(context).getString(KEY_CHIZUK_LAST + moment.key, null)?.ifBlank { null }

    fun setChizukBag(context: Context, moment: Moment, bag: List<String>, last: String?) {
        prefs(context).edit()
            .putString(KEY_CHIZUK_BAG + moment.key, bag.joinToString("\n"))
            .putString(KEY_CHIZUK_LAST + moment.key, last)
            .apply()
    }

    // --- Sauvegarde ---------------------------------------------------------

    fun exportJson(context: Context): String {
        val root = JSONObject()
        root.put("format", "myhitbodedut")
        root.put("version", 1)
        root.put("exported_at", java.time.Instant.now().toString())

        val daysJson = JSONObject()
        days(context).values.forEach { record -> daysJson.put(record.date, toJson(record)) }
        root.put("days", daysJson)

        val settings = JSONObject()
            .put("window_start", windowStart(context))
            .put("window_end", windowEnd(context))
            .put("snooze_minutes", snoozeMinutes(context))
            .put("target_minutes", targetMinutes(context))
            .put("pause_shabbat", shabbatMode(context))
            .put("in_israel", inIsrael(context))
            .put("reminder_enabled", reminderEnabled(context))
            .put("reminder_minutes", reminderMinutes(context))
            .put("music_enabled", musicEnabled(context))
            .put("music_volume", musicVolume(context))
            .put("music_track", musicTrackId(context) ?: "")
        root.put("settings", settings)
        root.put("paused_dates", org.json.JSONArray(pausedDates(context).toList()))
        root.put("blocked_apps", org.json.JSONArray(blockedApps(context).toList()))
        return root.toString(2)
    }

    /** Fusionne une sauvegarde. Renvoie le nombre de journées importées, ou null si le fichier est invalide. */
    fun importJson(context: Context, text: String): Int? {
        val root = runCatching { JSONObject(text) }.getOrNull() ?: return null
        if (root.optString("format") != "myhitbodedut") return null

        val merged = days(context).toMutableMap()
        var imported = 0
        root.optJSONObject("days")?.let { daysJson ->
            for (date in daysJson.keys()) {
                val entry = daysJson.optJSONObject(date) ?: continue
                val incoming = DayRecord(
                    date,
                    entry.optInt("minutes", 0),
                    entry.optString("note", ""),
                    readThemes(entry)
                )
                val existing = merged[date]
                merged[date] = when {
                    existing == null -> incoming
                    // En cas de conflit on garde la séance la plus longue et la
                    // note non vide. Les sujets, eux, se réunissent : deux
                    // sauvegardes du même jour ont pu voir deux séances
                    // différentes, et en perdre une serait perdre le passé.
                    else -> DayRecord(
                        date,
                        maxOf(existing.minutes, incoming.minutes),
                        existing.note.ifBlank { incoming.note },
                        (existing.themes + incoming.themes).distinct()
                    )
                }
                imported++
            }
        }
        writeDays(context, merged)

        root.optJSONObject("settings")?.let { settings ->
            setWindow(
                context,
                settings.optInt("window_start", windowStart(context)),
                settings.optInt("window_end", windowEnd(context))
            )
            setSnoozeMinutes(context, settings.optInt("snooze_minutes", snoozeMinutes(context)))
            setTargetMinutes(context, settings.optInt("target_minutes", targetMinutes(context)))
            setShabbatMode(context, settings.optBoolean("pause_shabbat", shabbatMode(context)))
            setInIsrael(context, settings.optBoolean("in_israel", inIsrael(context)))
            setReminderEnabled(context, settings.optBoolean("reminder_enabled", reminderEnabled(context)))
            setReminderMinutes(context, settings.optInt("reminder_minutes", reminderMinutes(context)))
            setMusicEnabled(context, settings.optBoolean("music_enabled", musicEnabled(context)))
            setMusicVolume(context, settings.optInt("music_volume", musicVolume(context)))
            settings.optString("music_track").takeIf { it.isNotBlank() }
                ?.let { setMusicTrackId(context, it) }
        }
        root.optJSONArray("paused_dates")?.let { array ->
            val dates = pausedDates(context).toMutableSet()
            for (i in 0 until array.length()) dates.add(array.optString(i))
            prefs(context).edit().putStringSet(KEY_PAUSED_DATES, HashSet(dates.filter { it.isNotBlank() })).apply()
        }
        root.optJSONArray("blocked_apps")?.let { array ->
            val apps = blockedApps(context).toMutableSet()
            for (i in 0 until array.length()) apps.add(array.optString(i))
            setBlockedApps(context, apps.filter { it.isNotBlank() }.toSet())
        }
        return imported
    }

    // --- Format -------------------------------------------------------------

    fun formatMinutes(minutes: Int): String =
        String.format(Locale.US, "%02d:%02d", (minutes / 60) % 24, minutes % 60)

    fun formatDuration(minutes: Int): String = when {
        minutes < 60 -> "$minutes min"
        minutes % 60 == 0 -> "${minutes / 60} h"
        else -> "${minutes / 60} h ${minutes % 60}"
    }
}
