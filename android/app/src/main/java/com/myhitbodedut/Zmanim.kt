package com.myhitbodedut

import com.kosherjava.zmanim.ComplexZmanimCalendar
import com.kosherjava.zmanim.util.GeoLocation
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Date
import java.util.GregorianCalendar
import java.util.TimeZone

/**
 * Les zmanim du jour (demande du 10/10), hors ligne, par KosherJava 2.5.0
 * (ComplexZmanimCalendar). Le profil est fixe et chaque ligne nomme sa
 * méthode : aucune appellation de communauté (« séfarade »…) ne choisit à
 * la place de l'utilisateur. Formules, sources et limites : ZMANIM.md. Les
 * méthodes ont été lues dans zmanim-2.5.0-sources.jar ; le profil est à
 * valider par un Rav.
 *
 * Le même profil existe côté web (web/src/js/zmanim.js, sur kosher-zmanim),
 * comparé à celui-ci par les vecteurs de web/test/vecteurs_kosherjava.json.
 *
 * Pas d'Android ici : testable sur la machine virtuelle seule.
 */
object Zmanim {

    const val MOTEUR = "KosherJava 2.5.0"

    data class Lieu(
        val nom: String,
        val latitude: Double,
        val longitude: Double,
        /** En mètres. Le profil calcule au niveau de la mer : elle n'entre dans aucun zman. */
        val altitude: Double,
        /** Fuseau IANA, « Asia/Jerusalem ». */
        val fuseau: String,
    )

    enum class Statut {
        OK,

        /** L'événement n'a pas lieu ce jour-là (soleil de minuit, nuit polaire) : ce n'est pas une erreur. */
        ABSENT,

        /** Le calcul a échoué : une erreur technique, distincte d'un événement absent. */
        ERREUR,
    }

    /** Une méthode : sa clé (libellé « zman_methode_<clé> ») et ses paramètres. */
    data class Methode(val cle: String, val parametres: String)

    /** Le format commun : le même côté web (zmanim.js). */
    data class Zman(
        val cle: String,
        val methode: Methode,
        val statut: Statut,
        val instant: Instant?,
        /** L'heure dans le fuseau du lieu ; sa date peut être le lendemain (chatsot de la nuit). */
        val local: ZonedDateTime?,
        val moteur: String = MOTEUR,
    )

    /**
     * Le profil, dans l'ordre de la journée. Pour chaque zman : la méthode
     * de ComplexZmanimCalendar qui le calcule. Rien d'autre ne décide.
     */
    val PROFIL: List<Pair<String, Methode>> = listOf(
        "alot" to Methode("degres", "16.1"),                 // getAlos16Point1Degrees
        "misheyakir" to Methode("degres", "11.5"),           // getMisheyakir11Point5Degrees
        "netz" to Methode("niveau_mer", ""),                 // getSeaLevelSunrise
        "szks_mga" to Methode("mga_72", "72"),               // getSofZmanShmaMGA
        "szks_gra" to Methode("gra", ""),                    // getSofZmanShmaGRA
        "szt_mga" to Methode("mga_72", "72"),                // getSofZmanTfilaMGA
        "szt_gra" to Methode("gra", ""),                     // getSofZmanTfilaGRA
        "chatsot" to Methode("meridien", ""),                // getChatzos (= getSunTransit en 2.5.0)
        "minha_gedola" to Methode("gra", ""),                // getMinchaGedola
        "minha_ketana" to Methode("gra", ""),                // getMinchaKetana
        "plag" to Methode("gra", ""),                        // getPlagHamincha
        "chkia" to Methode("niveau_mer", ""),                // getSeaLevelSunset
        "tset" to Methode("degres", "8.5"),                  // getTzaisGeonim8Point5Degrees
        "tset_rt" to Methode("minutes", "72"),               // getTzais72
        "chatsot_layla" to Methode("milieu_chatsot", ""),    // getSolarMidnight
    )

    /** L'allumage : coucher au niveau de la mer moins [minutes] (getCandleLighting). */
    fun methodeAllumage(minutes: Int) = Methode("avant_chkia", minutes.toString())

    /** Les minutes d'allumage proposées dans les réglages ; 18 par défaut. */
    val ALLUMAGES = listOf(18, 20, 22, 30, 40)
    const val ALLUMAGE_DEFAUT = 18

    fun calendrier(lieu: Lieu, date: LocalDate): ComplexZmanimCalendar {
        val fuseau = TimeZone.getTimeZone(lieu.fuseau)
        val geo = GeoLocation(lieu.nom, lieu.latitude, lieu.longitude, lieu.altitude, fuseau)
        return ComplexZmanimCalendar(geo).apply {
            calendar = GregorianCalendar(fuseau).apply {
                clear()
                set(date.year, date.monthValue - 1, date.dayOfMonth, 12, 0)
            }
            // Explicite, même si c'est le défaut : le profil calcule au niveau de la mer.
            isUseElevation = false
        }
    }

    /** Les quinze zmanim du profil pour [date] au [lieu]. */
    fun jour(lieu: Lieu, date: LocalDate): List<Zman> {
        val c = calendrier(lieu, date)
        val zone = ZoneId.of(lieu.fuseau)
        // getSolarMidnight prend le milieu entre chatsot d'aujourd'hui et de
        // demain, sans vérifier qu'ils existent : au-delà du cercle polaire,
        // l'un des deux manque et la bibliothèque lèverait une exception là où
        // l'événement est simplement absent.
        val nuitCalculable = c.chatzos != null && calendrier(lieu, date.plusDays(1)).chatzos != null
        return PROFIL.map { (cle, methode) ->
            zman(cle, methode, zone) { if (cle == "chatsot_layla" && !nuitCalculable) null else valeur(c, cle) }
        }
    }

    /** L'allumage des bougies ce jour-là au [lieu], [minutes] avant la chki'a. */
    fun allumage(lieu: Lieu, date: LocalDate, minutes: Int): Zman {
        val c = calendrier(lieu, date).apply { candleLightingOffset = minutes.toDouble() }
        return zman("allumage", methodeAllumage(minutes), ZoneId.of(lieu.fuseau)) { c.candleLighting }
    }

    private fun valeur(c: ComplexZmanimCalendar, cle: String): Date? = when (cle) {
        "alot" -> c.alos16Point1Degrees
        "misheyakir" -> c.misheyakir11Point5Degrees
        "netz" -> c.seaLevelSunrise
        "szks_mga" -> c.sofZmanShmaMGA
        "szks_gra" -> c.sofZmanShmaGRA
        "szt_mga" -> c.sofZmanTfilaMGA
        "szt_gra" -> c.sofZmanTfilaGRA
        "chatsot" -> c.chatzos
        "minha_gedola" -> c.minchaGedola
        "minha_ketana" -> c.minchaKetana
        "plag" -> c.plagHamincha
        "chkia" -> c.seaLevelSunset
        "tset" -> c.tzaisGeonim8Point5Degrees
        "tset_rt" -> c.tzais72
        "chatsot_layla" -> c.solarMidnight
        else -> error("zman inconnu : $cle")
    }

    /**
     * KosherJava rend null quand l'événement n'a pas lieu (au-delà du cercle
     * polaire) ; une exception est une erreur. Les deux ne se confondent pas.
     */
    private fun zman(cle: String, methode: Methode, zone: ZoneId, calcul: () -> Date?): Zman = try {
        val d = calcul()
        if (d == null) Zman(cle, methode, Statut.ABSENT, null, null)
        else Zman(cle, methode, Statut.OK, d.toInstant(), d.toInstant().atZone(zone))
    } catch (e: RuntimeException) {
        Zman(cle, methode, Statut.ERREUR, null, null)
    }
}
