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
    val themes: List<String> = emptyList(),
    /**
     * Une note laissée un jour sans hitbodedout. Elle est gardée, mais ne
     * valide pas la journée : sans cela, enregistrer une note, même vide,
     * suffisait à faire monter la série (revue du 29/09).
     */
    val noteSeule: Boolean = false
) {
    /** La journée compte : une hitbodedout y a eu lieu ou a été déclarée. */
    val faite: Boolean get() = !noteSeule
}

/** Une hitbodedout close : ce qui s'inscrit, et le jour où elle a commencé. */
data class SeanceClose(val minutes: Int, val date: String)

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
    /** Copie d'un journal illisible, gardée pour qu'il ne soit pas écrasé en silence. */
    private const val KEY_DAYS_CORROMPU = "days_corrompu"

    /** Au-delà, une note n'est plus une note ; et chaque écriture recopie tout le journal. */
    const val NOTE_MAX = 4000
    private const val KEY_LEGACY_DONE = "done_dates"
    private const val KEY_APPS = "blocked_apps"
    private const val KEY_START = "window_start"
    private const val KEY_END = "window_end"
    private const val KEY_SNOOZE = "snooze_minutes"
    private const val KEY_SNOOZED_UNTIL = "snoozed_until"
    private const val KEY_ENABLED = "enabled"
    private const val KEY_TARGET = "target_minutes"
    private const val KEY_DAILY_GOAL = "daily_goal_minutes"
    private const val KEY_ANNEAUX_FETES = "anneaux_fetes"
    private const val KEY_PALIERS_VUS = "paliers_vus"
    private const val KEY_HILLOULOT = "hilloulot_affichees"
    private const val KEY_SIDDUR_NUSAH = "siddur_nusah"
    private const val KEY_TAHANOUN_TICHRI_SFARD = "sans_tahanoun_fin_tichri_sfard"
    private const val KEY_CALENDRIER_CIVIL = "calendrier_civil"
    private const val KEY_LECTURE_ECHELLE = "lecture_echelle"
    private const val KEY_LECTURE_SOMBRE = "lecture_sombre"
    private const val KEY_LECTURE_MASQUEE = "lecture_masquee"
    private const val KEY_PAUSE_SHABBAT = "pause_shabbat"
    private const val KEY_IN_ISRAEL = "in_israel"
    private const val KEY_LIEU_NOM = "lieu_nom"
    private const val KEY_LIEU_LAT = "lieu_latitude"
    private const val KEY_LIEU_LON = "lieu_longitude"
    private const val KEY_LIEU_ALT = "lieu_altitude"
    private const val KEY_LIEU_FUSEAU = "lieu_fuseau"
    private const val KEY_LIEU_VILLE = "lieu_ville"
    private const val KEY_ALLUMAGE = "allumage_minutes"
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
    private const val KEY_ONBOARDED = "onboarded_version"
    /** Le booléen de la v1.96, retiré au premier passage. */
    private const val KEY_ONBOARDED_ANCIEN = "onboarded"
    private const val KEY_NAME = "first_name"
    private const val KEY_ADRESSE = "adresse"
    private const val KEY_CELEBRATION = "celebration_minutes"
    private const val KEY_CELEBRATION_DATE = "celebration_date"
    private const val KEY_MODE = "apparence_mode"
    private const val KEY_FOND = "apparence_fond"
    private const val KEY_ACCENT = "apparence_accent"
    private const val KEY_LANGUE_APP = "langue_app"
    private const val KEY_MUSIC_ON = "music_enabled"
    private const val KEY_MUSIC_TRACK = "music_track"
    private const val KEY_MUSIC_VOLUME = "music_volume"
    private const val KEY_MUSIC_SHUFFLE = "music_shuffle"
    private const val KEY_MUSIC_NOW = "music_now"
    private const val KEY_MUSIC_USER = "music_user_tracks"
    private const val KEY_TIKKUN_DAY = "tikkun_day"
    private const val KEY_TIKKUN_DONE = "tikkun_done"
    private const val KEY_CHIZUK_BAG = "chizuk_bag_"
    private const val KEY_CHIZUK_LAST = "chizuk_last_"
    private const val KEY_MUSIC_NAMES = "music_names"

    const val DEFAULT_START = 6 * 60
    const val DEFAULT_END = 11 * 60
    const val DEFAULT_SNOOZE = 15
    // 15 minutes : c'est ce que le conseil n° 2 recommande pour commencer.
    // Une heure par défaut accueillait un débutant avec une marche trop
    // haute (revue du 29/09). Un choix déjà fait est conservé.
    const val DEFAULT_TARGET = 15
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
                    // Une clé mal formée (fichier importé par une version
                    // d'avant la vérification) ne doit pas faire tomber le
                    // journal : elle est ignorée.
                    if (dateValide(date) == null) continue
                    result[date] = DayRecord(
                        date = date,
                        minutes = entry.optInt("minutes", 0),
                        note = entry.optString("note", ""),
                        themes = readThemes(entry),
                        noteSeule = entry.optBoolean("note_seule", false)
                    )
                }
            }.onFailure {
                // Illisible : on en garde une copie avant que la prochaine
                // écriture ne le remplace par la seule journée du jour.
                val p = journalPrefs(context)
                if (!p.contains(KEY_DAYS_CORROMPU)) p.edit().putString(KEY_DAYS_CORROMPU, raw).apply()
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
        if (record.noteSeule) entry.put("note_seule", true)
        return entry
    }

    /** La date si la clé est une vraie date ISO (2026-09-29), sinon null. */
    fun dateValide(cle: String): LocalDate? =
        runCatching { LocalDate.parse(cle, FORMAT) }.getOrNull()
            ?.takeIf { it.format(FORMAT) == cle }

    /** Le jour civil d'un instant, dans le fuseau du téléphone. */
    fun dateOf(millis: Long): String =
        java.time.Instant.ofEpochMilli(millis).atZone(java.time.ZoneId.systemDefault())
            .toLocalDate().format(FORMAT)

    /** Les journées qui comptent : faites ou déclarées, pas les notes seules. */
    fun joursFaits(context: Context): Set<String> =
        days(context).values.filter { it.faite }.map { it.date }.toSet()

    fun record(context: Context, date: String = today()): DayRecord? = days(context)[date]

    fun isDone(context: Context, date: String = today()): Boolean =
        days(context)[date]?.faite == true

    /**
     * Valide la journée. Les minutes s'ajoutent si plusieurs séances ont lieu.
     *
     * [date] est le jour où la hitbodedout a commencé : celle de 23 h 40 qui
     * finit à 0 h 20 compte pour la veille, sinon la veille restait vide et
     * la série cassait (revue du 29/09).
     */
    fun addSession(
        context: Context,
        minutes: Int,
        note: String? = null,
        theme: Theme? = null,
        date: String = today()
    ) {
        val all = days(context).toMutableMap()
        val existing = all[date]
        all[date] = DayRecord(
            date = date,
            minutes = (existing?.minutes ?: 0) + minutes.coerceAtLeast(0),
            note = note ?: existing?.note ?: "",
            // Le sujet s'ajoute à ceux du jour ; sans sujet, rien n'est ajouté.
            themes = (existing?.themes ?: emptyList()) + listOfNotNull(theme?.cle),
            noteSeule = false
        )
        writeDays(context, all)
        clearSnooze(context)
    }

    /** La note d'un jour. Sur un jour sans hitbodedout, elle ne le valide pas. */
    fun setNote(context: Context, date: String, note: String) {
        val all = days(context).toMutableMap()
        val existing = all[date]
        when {
            // Une note seule qu'on efface : la ligne n'a plus de raison d'être.
            existing?.noteSeule == true && note.isBlank() -> all.remove(date)
            existing != null -> all[date] = existing.copy(note = note.take(NOTE_MAX))
            note.isNotBlank() -> all[date] = DayRecord(date, 0, note.take(NOTE_MAX), emptyList(), noteSeule = true)
            else -> return
        }
        writeDays(context, all)
    }

    fun unmarkToday(context: Context) {
        val all = days(context).toMutableMap()
        all.remove(today())
        writeDays(context, all)
    }

    fun totalMinutes(context: Context): Int = days(context).values.sumOf { it.minutes }

    /** Au moins une journée faite, un jour : il y a une série à perdre. */
    fun aDejaPratique(context: Context): Boolean = days(context).values.any { it.faite }

    // --- Jours sans obligation ---------------------------------------------

    /** Shabbat et Yom Tov : un seul réglage, la clé d'origine est conservée. */
    fun shabbatMode(context: Context): Boolean = prefs(context).getBoolean(KEY_PAUSE_SHABBAT, true)

    fun setShabbatMode(context: Context, value: Boolean) {
        prefs(context).edit().putBoolean(KEY_PAUSE_SHABBAT, value).apply()
    }

    /** Israël ou diaspora : décide du deuxième jour de fête. */
    /**
     * Israël ou diaspora. Le premier accès fige la valeur déduite du fuseau :
     * sans cela, un voyage changeait le deuxième jour de fête après coup, et
     * pouvait casser une série (revue du 01/10).
     */
    fun inIsrael(context: Context): Boolean {
        val p = prefs(context)
        if (!p.contains(KEY_IN_ISRAEL)) {
            p.edit().putBoolean(KEY_IN_ISRAEL, Holidays.defaultInIsrael(java.util.TimeZone.getDefault().id)).apply()
        }
        return p.getBoolean(KEY_IN_ISRAEL, false)
    }

    fun setInIsrael(context: Context, value: Boolean) {
        prefs(context).edit().putBoolean(KEY_IN_ISRAEL, value).apply()
    }

    /**
     * Le lieu des zmanim (10/10) : une ville de la liste ou la position du
     * téléphone ; null tant que rien n'est choisi. Il reste sur l'appareil,
     * comme tout le reste. Les nombres sont gardés en texte :
     * SharedPreferences n'a pas de type décimal double.
     */
    fun lieuZmanim(context: Context): Zmanim.Lieu? {
        val p = prefs(context)
        val lat = p.getString(KEY_LIEU_LAT, null)?.toDoubleOrNull() ?: return null
        val lon = p.getString(KEY_LIEU_LON, null)?.toDoubleOrNull() ?: return null
        val fuseau = p.getString(KEY_LIEU_FUSEAU, null) ?: return null
        return Zmanim.Lieu(p.getString(KEY_LIEU_NOM, "").orEmpty(), lat, lon,
            p.getString(KEY_LIEU_ALT, null)?.toDoubleOrNull() ?: 0.0, fuseau)
    }

    /** La ville choisie dans la liste (identifiant GeoNames), ou -1 pour la position du téléphone. */
    fun lieuVille(context: Context): Long = prefs(context).getLong(KEY_LIEU_VILLE, -1L)

    /** Pourim le 15 Adar : la ville des zmanim est Jérusalem (CalendrierJuif.jour). */
    fun mukafChoma(context: Context): Boolean = lieuVille(context) == CalendrierJuif.JERUSALEM_GEONAMES

    /**
     * Choisir le lieu règle aussi Israël ou diaspora : le deuxième jour de
     * fête suit le lieu des zmanim. Une ville le dit ; la position du
     * téléphone suit son fuseau, comme la valeur par défaut
     * (Holidays.defaultInIsrael).
     */
    fun setLieuZmanim(context: Context, lieu: Zmanim.Lieu, ville: Ville?) {
        prefs(context).edit()
            .putString(KEY_LIEU_NOM, lieu.nom)
            .putString(KEY_LIEU_LAT, lieu.latitude.toString())
            .putString(KEY_LIEU_LON, lieu.longitude.toString())
            .putString(KEY_LIEU_ALT, lieu.altitude.toString())
            .putString(KEY_LIEU_FUSEAU, lieu.fuseau)
            .putLong(KEY_LIEU_VILLE, ville?.id ?: -1L)
            .apply()
        setInIsrael(context, ville?.enIsrael ?: Holidays.defaultInIsrael(lieu.fuseau))
    }

    /** Les minutes d'allumage avant la chki'a ; 18 par défaut (Zmanim.ALLUMAGES). */
    fun allumage(context: Context): Int =
        prefs(context).getInt(KEY_ALLUMAGE, Zmanim.ALLUMAGE_DEFAUT).takeIf { it in Zmanim.ALLUMAGES } ?: Zmanim.ALLUMAGE_DEFAUT

    fun setAllumage(context: Context, minutes: Int) {
        prefs(context).edit().putInt(KEY_ALLUMAGE, minutes).apply()
    }

    /**
     * Pas de Ta'hanoun du 22 Tichri à la fin du mois : toujours en Edot,
     * jamais en Ashkenaz, au choix en Sfard (10/10 ; faux par défaut).
     */
    fun sansTahanounFinTichri(context: Context): Boolean = when (siddurNusah(context).cle) {
        "edot" -> true
        "sefard" -> prefs(context).getBoolean(KEY_TAHANOUN_TICHRI_SFARD, false)
        else -> false
    }

    fun setSansTahanounFinTichriSfard(context: Context, sans: Boolean) {
        prefs(context).edit().putBoolean(KEY_TAHANOUN_TICHRI_SFARD, sans).apply()
    }

    /** Le calendrier s'ouvre sur le mois hébreu ; le mois civil est au choix. */
    fun calendrierCivil(context: Context): Boolean = prefs(context).getBoolean(KEY_CALENDRIER_CIVIL, false)

    fun setCalendrierCivil(context: Context, civil: Boolean) {
        prefs(context).edit().putBoolean(KEY_CALENDRIER_CIVIL, civil).apply()
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
        val done = joursFaits(context)
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

    /** La plus longue série jamais tenue (Plaque.meilleureSerie), au moins la série en cours. */
    fun meilleureSerie(context: Context): Int {
        val faits = joursFaits(context).mapNotNull { dateValide(it) }.toSet()
        return maxOf(Plaque.meilleureSerie(faits) { isExempt(context, it) }, streak(context))
    }

    /**
     * La semaine en cours, du dimanche au samedi : c'est ainsi que se compte la
     * semaine juive, et le samedi doit tomber en fin de bande, pas au milieu.
     */
    fun currentWeek(context: Context): List<WeekDay> {
        val done = joursFaits(context)
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

    /**
     * L'objectif de l'anneau du jour, en minutes. Tant qu'il n'est pas
     * réglé, c'est la durée habituelle d'une hitbodedout (Anneaux).
     */
    fun dailyGoal(context: Context): Int =
        Anneaux.objectifEffectif(prefs(context).getInt(KEY_DAILY_GOAL, 0), targetMinutes(context))

    /** La taille des lettres dans les écrans de lecture, de 0,8 à 2 fois la taille de base. */
    fun lectureEchelle(context: Context): Float =
        prefs(context).getFloat(KEY_LECTURE_ECHELLE, 1f).coerceIn(ReglagesLecture.MIN, ReglagesLecture.MAX)

    fun setLectureEchelle(context: Context, echelle: Float) {
        prefs(context).edit().putFloat(KEY_LECTURE_ECHELLE, echelle).apply()
    }

    /** La lecture sur fond noir, choisie d'un toucher sur la lune. */
    fun lectureSombre(context: Context): Boolean = prefs(context).getBoolean(KEY_LECTURE_SOMBRE, false)

    fun setLectureSombre(context: Context, sombre: Boolean) {
        prefs(context).edit().putBoolean(KEY_LECTURE_SOMBRE, sombre).apply()
    }

    /**
     * Le bouton de lecture masqué tout à fait (demande du 04/10) : il ne
     * revient que par un appui long sur le texte, ou par l'action
     * d'accessibilité « Réglages de lecture » de la vue qui défile.
     */
    fun lectureMasquee(context: Context): Boolean = prefs(context).getBoolean(KEY_LECTURE_MASQUEE, false)

    fun setLectureMasquee(context: Context, masquee: Boolean) {
        prefs(context).edit().putBoolean(KEY_LECTURE_MASQUEE, masquee).apply()
    }

    /** Le nusah du siddour : Edot HaMizra'h par défaut. */
    fun siddurNusah(context: Context): Nusah = Nusah.de(prefs(context).getString(KEY_SIDDUR_NUSAH, null))

    fun setSiddurNusah(context: Context, nusah: Nusah) {
        prefs(context).edit().putString(KEY_SIDDUR_NUSAH, nusah.cle).apply()
    }

    /** Les marques des anneaux déjà fêtés (Anneaux.marque). */
    fun anneauxFetes(context: Context): Set<String> =
        prefs(context).getStringSet(KEY_ANNEAUX_FETES, null)?.toSet() ?: emptySet()

    fun setAnneauxFetes(context: Context, marques: Set<String>) {
        prefs(context).edit().putStringSet(KEY_ANNEAUX_FETES, marques.toSet()).apply()
    }

    /** La hilloula du jour sur la carte du jour (11/10) : affichée par défaut. */
    fun hilloulotAffichees(context: Context): Boolean = prefs(context).getBoolean(KEY_HILLOULOT, true)

    fun setHilloulotAffichees(context: Context, v: Boolean) {
        prefs(context).edit().putBoolean(KEY_HILLOULOT, v).apply()
    }

    // --- Paliers d'objectifs (11/10) -------------------------------------------

    /** La pratique vue par les paliers : minutes des jours faits, série, mois hébreu en cours. */
    fun bilan(context: Context): Paliers.Bilan {
        val aujourdhui = LocalDate.now()
        val minutes = days(context).values.filter { it.faite }
            .mapNotNull { r -> dateValide(r.date)?.let { it to r.minutes } }.toMap()
        return Paliers.Bilan(aujourdhui, minutes, streak(context), JourAffiche.debutMoisHebreu(aujourdhui), dailyGoal(context))
    }

    /** null tant que les paliers n'ont jamais été regardés : le premier passage ne fête rien. */
    fun paliersVus(context: Context): Set<String>? =
        prefs(context).getStringSet(KEY_PALIERS_VUS, null)?.toSet()

    /** Seules restent les marques d'une période encore en cours ou sans période : la liste ne grossit pas. */
    fun setPaliersVus(context: Context, marques: Set<String>) {
        prefs(context).edit().putStringSet(KEY_PALIERS_VUS, marques.toSet()).apply()
    }

    fun setDailyGoal(context: Context, minutes: Int) {
        prefs(context).edit().putInt(KEY_DAILY_GOAL, minutes.coerceAtLeast(0)).apply()
    }

    /** Les trois anneaux de l'accueil, ici et maintenant. */
    fun anneaux(context: Context): Anneaux.Etat = Anneaux.etat(
        minutesJour = record(context)?.minutes ?: 0,
        objectifJour = dailyGoal(context),
        semaine = JaugeSemaine.progression(currentWeek(context)),
        serie = streak(context)
    )

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

    /**
     * Termine la séance : les minutes à inscrire, et le jour où elle a
     * commencé. Une séance minutée ne compte jamais plus que sa durée, même
     * si sa fin n'a été constatée que plus tard (écran éteint, application
     * tuée) ; voir SessionRule.minutesAInscrire.
     */
    fun endSession(context: Context): SeanceClose {
        val started = sessionStartedAt(context)
        val minutes = SessionRule.minutesAInscrire(
            started, sessionEndsAt(context), System.currentTimeMillis()
        )
        // Jamais dans l'avenir : une horloge reculée après le début ne doit
        // pas inscrire une journée à venir.
        val date = if (started > 0L) minOf(dateOf(started), today()) else today()
        prefs(context).edit()
            .remove(KEY_SESSION_START)
            .remove(KEY_SESSION_END)
            .remove(KEY_SESSION_TARGET)
            .apply()
        return SeanceClose(minutes, date)
    }

    /** Une séance est inscrite comme commencée, que le service tourne ou non. */
    fun hasSessionRecord(context: Context): Boolean = sessionStartedAt(context) > 0L

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
     * La présentation, dans sa version actuelle, a déjà été vue.
     *
     * On retient le numéro de version, pas un simple « vu » : quand la
     * présentation change, elle revient une fois, installation existante
     * comprise. La v1.96 la cachait à qui avait déjà servi l'application, et
     * le propriétaire, installant par-dessus, ne l'a jamais vue ; son choix
     * צדיק / צדקת ne se pose que là (demande du 28/09).
     */
    fun hasOnboarded(context: Context): Boolean =
        prefs(context).getInt(KEY_ONBOARDED, 0) >= Onboarding.VERSION

    fun setOnboarded(context: Context) {
        prefs(context).edit()
            .putInt(KEY_ONBOARDED, Onboarding.VERSION)
            .remove(KEY_ONBOARDED_ANCIEN)
            .apply()
    }

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

    /** צדיק ou צדקת ; null tant que rien n'est choisi. */
    fun adresse(context: Context): Adresse? =
        Adresse.parse(prefs(context).getString(KEY_ADRESSE, null))

    fun setAdresse(context: Context, adresse: Adresse?) {
        prefs(context).edit().apply {
            if (adresse == null) remove(KEY_ADRESSE) else putString(KEY_ADRESSE, adresse.cle)
        }.apply()
    }

    /** Copie du choix de langue d'AppCompat, pour les services (LangueApp). */
    fun langueApp(context: Context): String =
        prefs(context).getString(KEY_LANGUE_APP, "").orEmpty()

    fun setLangueApp(context: Context, etiquettes: String) {
        prefs(context).edit().putString(KEY_LANGUE_APP, etiquettes).apply()
    }

    // --- Apparence -----------------------------------------------------------

    fun modeApparence(context: Context): Apparence.Mode =
        Apparence.Mode.parse(prefs(context).getString(KEY_MODE, null))

    fun setModeApparence(context: Context, mode: Apparence.Mode) {
        prefs(context).edit().putString(KEY_MODE, mode.cle).apply()
    }

    fun fond(context: Context): Apparence.Fond =
        Apparence.Fond.parse(prefs(context).getString(KEY_FOND, null))

    fun setFond(context: Context, fond: Apparence.Fond) {
        prefs(context).edit().putString(KEY_FOND, fond.cle).apply()
    }

    fun accent(context: Context): Apparence.Accent =
        Apparence.Accent.parse(prefs(context).getString(KEY_ACCENT, null))

    fun setAccent(context: Context, accent: Apparence.Accent) {
        prefs(context).edit().putString(KEY_ACCENT, accent.cle).apply()
    }

    /** Le bravo en attente, et le jour de la note qu'il proposera ensuite. */
    data class Celebration(val minutes: Int, val dateNote: String?)

    /**
     * Une séance vient de se clore : le bravo attend le prochain écran ouvert.
     * [dateNote] est le jour de la séance ; le bravo propose la note ensuite,
     * pour qu'elle ne s'ouvre plus sous lui (revue du 29/09).
     */
    fun setPendingCelebration(context: Context, minutes: Int, dateNote: String? = null) {
        prefs(context).edit()
            .putInt(KEY_CELEBRATION, minutes.coerceAtLeast(0))
            .apply { if (dateNote == null) remove(KEY_CELEBRATION_DATE) else putString(KEY_CELEBRATION_DATE, dateNote) }
            .apply()
    }

    fun hasPendingCelebration(context: Context): Boolean = prefs(context).contains(KEY_CELEBRATION)

    /** Rend le bravo en attente et le retire, ou null s'il n'y en a pas. */
    fun takePendingCelebration(context: Context): Celebration? {
        val p = prefs(context)
        if (!p.contains(KEY_CELEBRATION)) return null
        val celebration = Celebration(
            p.getInt(KEY_CELEBRATION, 0),
            p.getString(KEY_CELEBRATION_DATE, null)
        )
        p.edit().remove(KEY_CELEBRATION).remove(KEY_CELEBRATION_DATE).apply()
        return celebration
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

    /** Lecture aléatoire : un morceau au hasard, puis un autre (MelangeMusique). */
    fun musicShuffle(context: Context): Boolean = prefs(context).getBoolean(KEY_MUSIC_SHUFFLE, false)

    fun setMusicShuffle(context: Context, on: Boolean) {
        prefs(context).edit().putBoolean(KEY_MUSIC_SHUFFLE, on).apply()
    }

    /** Le morceau qui joue en ce moment, pour que l'écran de séance le nomme. */
    fun musicNow(context: Context): String? = prefs(context).getString(KEY_MUSIC_NOW, null)

    fun setMusicNow(context: Context, id: String?) {
        prefs(context).edit().apply {
            if (id == null) remove(KEY_MUSIC_NOW) else putString(KEY_MUSIC_NOW, id)
        }.apply()
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
            .put("daily_goal_minutes", prefs(context).getInt(KEY_DAILY_GOAL, 0))
            .put("pause_shabbat", shabbatMode(context))
            .put("in_israel", inIsrael(context))
            .put("reminder_enabled", reminderEnabled(context))
            .put("reminder_minutes", reminderMinutes(context))
            // Toutes les heures : l'ancienne clé n'en porte qu'une, et une
            // sauvegarde restaurée perdait les autres (revue du 29/09).
            .put("reminder_times", org.json.JSONArray(reminderTimes(context)))
            .put("first_name", firstName(context))
            .put("hide_countdown", hideCountdown(context))
            .put("guided_hitbodedut", guidedHitbodedut(context))
            .put("music_enabled", musicEnabled(context))
            .put("music_volume", musicVolume(context))
            .put("music_shuffle", musicShuffle(context))
            .put("music_track", musicTrackId(context) ?: "")
            .put("adresse", adresse(context)?.cle ?: "")
            .put("apparence_mode", modeApparence(context).cle)
            .put("apparence_fond", fond(context).cle)
            .put("apparence_accent", accent(context).cle)
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
            val demain = LocalDate.now().plusDays(1)
            for (date in daysJson.keys()) {
                val entry = daysJson.optJSONObject(date) ?: continue
                // Une date mal formée faisait tomber l'écran Journal à chaque
                // ouverture ; une date à venir n'a rien à faire dans un journal.
                val jour = dateValide(date) ?: continue
                if (!jour.isBefore(demain)) continue
                val incoming = DayRecord(
                    date,
                    entry.optInt("minutes", 0).coerceIn(0, 24 * 60),
                    entry.optString("note", "").take(NOTE_MAX),
                    readThemes(entry),
                    entry.optBoolean("note_seule", false)
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
                        (existing.themes + incoming.themes).distinct(),
                        existing.noteSeule && incoming.noteSeule
                    )
                }
                imported++
            }
        }
        writeDays(context, merged)

        root.optJSONObject("settings")?.let { settings ->
            // Chaque valeur est ramenée dans ce que l'écran permet : un
            // fichier modifié à la main ne doit pas laisser une durée nulle
            // ou une heure qui n'existe pas.
            val jour = 0 until 24 * 60
            setWindow(
                context,
                settings.optInt("window_start", windowStart(context)).takeIf { it in jour } ?: windowStart(context),
                settings.optInt("window_end", windowEnd(context)).takeIf { it in jour } ?: windowEnd(context)
            )
            setSnoozeMinutes(context, settings.optInt("snooze_minutes", snoozeMinutes(context)).coerceIn(1, 240))
            setTargetMinutes(
                context,
                settings.optInt("target_minutes", targetMinutes(context)).coerceIn(DureeSeance.MIN, DureeSeance.MAX)
            )
            if (settings.has("daily_goal_minutes")) {
                setDailyGoal(context, settings.optInt("daily_goal_minutes", 0).coerceIn(0, DureeSeance.MAX))
            }
            setShabbatMode(context, settings.optBoolean("pause_shabbat", shabbatMode(context)))
            setInIsrael(context, settings.optBoolean("in_israel", inIsrael(context)))
            setReminderEnabled(context, settings.optBoolean("reminder_enabled", reminderEnabled(context)))
            // Les heures de rappel : la liste si le fichier l'a, sinon l'heure
            // unique des anciennes sauvegardes. Écrite par setReminderTimes,
            // qu'on n'ignore plus quand une liste existait déjà.
            val heures = settings.optJSONArray("reminder_times")?.let { a ->
                (0 until a.length()).mapNotNull { i -> a.optInt(i, -1).takeIf { it in jour } }
            } ?: settings.optInt("reminder_minutes", -1).takeIf { it in jour }?.let { listOf(it) }
            if (!heures.isNullOrEmpty()) setReminderTimes(context, heures)
            if (settings.has("first_name")) setFirstName(context, settings.optString("first_name"))
            if (settings.has("hide_countdown")) setHideCountdown(context, settings.optBoolean("hide_countdown"))
            if (settings.has("guided_hitbodedut")) {
                setGuidedHitbodedut(context, settings.optBoolean("guided_hitbodedut"))
            }
            setMusicEnabled(context, settings.optBoolean("music_enabled", musicEnabled(context)))
            setMusicVolume(context, settings.optInt("music_volume", musicVolume(context)))
            setMusicShuffle(context, settings.optBoolean("music_shuffle", musicShuffle(context)))
            settings.optString("music_track").takeIf { it.isNotBlank() }
                ?.let { setMusicTrackId(context, it) }
            Adresse.parse(settings.optString("adresse"))?.let { setAdresse(context, it) }
            // Une clé absente garde le choix présent ; parse() seul le ramènerait au vert.
            settings.optString("apparence_mode").takeIf { it.isNotBlank() }
                ?.let { setModeApparence(context, Apparence.Mode.parse(it)) }
            settings.optString("apparence_fond").takeIf { it.isNotBlank() }
                ?.let { setFond(context, Apparence.Fond.parse(it)) }
            settings.optString("apparence_accent").takeIf { it.isNotBlank() }
                ?.let { setAccent(context, Apparence.Accent.parse(it)) }
        }
        root.optJSONArray("paused_dates")?.let { array ->
            val dates = pausedDates(context).toMutableSet()
            for (i in 0 until array.length()) dates.add(array.optString(i))
            prefs(context).edit().putStringSet(KEY_PAUSED_DATES, HashSet(dates.filter { dateValide(it) != null })).apply()
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

    /**
     * « 20 min », « 1 h 30 » : les unités viennent des chaînes, pour suivre
     * la langue de l'interface. Écrites en dur, elles restaient françaises en
     * anglais et en hébreu (audit des traductions, 28/09).
     */
    fun formatDuration(context: Context, minutes: Int): String = when {
        minutes < 60 -> context.getString(R.string.duration_minutes, minutes)
        minutes % 60 == 0 -> context.getString(R.string.duration_hours, minutes / 60)
        else -> context.getString(R.string.duration_hours_minutes, minutes / 60, minutes % 60)
    }
}
