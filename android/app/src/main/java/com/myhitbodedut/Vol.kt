package com.myhitbodedut

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Les zmanim en vol (28/10), phase 1 : hors ligne, sur la route prévue. Rien
 * ne quitte le téléphone, aucune permission n'est demandée.
 *
 * L'avion est placé sur l'arc de grand cercle entre les deux aéroports, à
 * vitesse constante entre les heures prévues de décollage et d'atterrissage ;
 * avant le décollage, il est à l'aéroport de départ, après l'atterrissage, à
 * celui d'arrivée. Un zman a lieu en vol à l'instant t où le zman du point
 * sous l'avion vaut t : t = zman(position(t)). Le zman du point est celui du
 * moteur existant (Zmanim.jour, au niveau de la mer, même profil), rien
 * n'est recalculé autrement. Choix et limites : ZMANIM.md, « En vol ».
 *
 * Tout est en UTC ; les fuseaux ne servent qu'à lire les heures saisies et à
 * les réafficher. Pas d'Android ici, ni d'autre fichier que Zmanim.kt : testable
 * et compilable sur la machine virtuelle seule (tools/vecteurs_vol). La prière
 * du moment, qui passe par ZmanimWidget, est dans VolPriere.kt.
 */
object Vol {

    /** Un aéroport de res/raw/aeroports.json (tools/aeroports_construction.py). */
    data class Aeroport(
        val iata: String,
        val nom: String,
        val ville: String,
        val pays: String,
        val latitude: Double,
        val longitude: Double,
        /** Fuseau IANA de l'aéroport : celui des heures du billet. */
        val fuseau: String,
    )

    /** Le vol tel qu'on le saisit : numéro (affiché seulement), aéroports et heures prévues. */
    data class Plan(
        val numero: String,
        val depart: Aeroport,
        val arrivee: Aeroport,
        val decollage: Instant,
        val atterrissage: Instant,
        /** Un relevé pris en vol (GPS ou suivi en direct) : la route passe par lui (deux grands cercles). */
        val releve: Point? = null,
        val sourceReleve: SourceReleve = SourceReleve.GPS,
    )

    /** Le GPS du téléphone, ou la position reçue du suivi en direct (SuiviVol). */
    enum class SourceReleve { GPS, DIRECT }

    /** Le relevé compte s'il tombe entre le décollage et l'atterrissage prévus. */
    fun releveUtilisable(plan: Plan): Boolean = plan.releve?.let { it.instant.isAfter(plan.decollage) && it.instant.isBefore(plan.atterrissage) } == true

    enum class Invalide { MEME_AEROPORT, ARRIVEE_AVANT_DEPART, TROP_LONG, FUSEAU_INCONNU }

    /** Plus long qu'aucun vol régulier : une erreur de saisie (date d'arrivée, AM/PM). */
    val DUREE_MAX: Duration = Duration.ofHours(22)

    fun verifier(plan: Plan): Invalide? = when {
        plan.depart.iata == plan.arrivee.iata -> Invalide.MEME_AEROPORT
        !plan.atterrissage.isAfter(plan.decollage) -> Invalide.ARRIVEE_AVANT_DEPART
        Duration.between(plan.decollage, plan.atterrissage) > DUREE_MAX -> Invalide.TROP_LONG
        zone(plan.depart.fuseau) == null || zone(plan.arrivee.fuseau) == null -> Invalide.FUSEAU_INCONNU
        else -> null
    }

    /** Un fuseau que l'appareil ne connaît pas (base tz trop ancienne) rend null plutôt que d'échouer. */
    fun zone(fuseau: String): ZoneId? = try { ZoneId.of(fuseau) } catch (e: RuntimeException) { null }

    /** L'heure du billet (heure locale de l'aéroport) en instant UTC ; null si le fuseau est inconnu. */
    fun instant(local: LocalDateTime, aeroport: Aeroport): Instant? = zone(aeroport.fuseau)?.let { local.atZone(it).toInstant() }

    data class Point(val instant: Instant, val latitude: Double, val longitude: Double)

    /**
     * La position prévue à [t] : grand cercle, vitesse constante ; à l'aéroport hors du vol.
     * Avec un relevé GPS en vol : du départ au relevé, puis du relevé à l'arrivée, chaque
     * tronçon à vitesse constante.
     */
    fun position(plan: Plan, t: Instant): Point {
        val r = plan.releve
        if (r != null && releveUtilisable(plan)) {
            return if (t.isAfter(r.instant)) arc(r.latitude, r.longitude, plan.arrivee.latitude, plan.arrivee.longitude, r.instant, plan.atterrissage, t)
            else arc(plan.depart.latitude, plan.depart.longitude, r.latitude, r.longitude, plan.decollage, r.instant, t)
        }
        return arc(plan.depart.latitude, plan.depart.longitude, plan.arrivee.latitude, plan.arrivee.longitude, plan.decollage, plan.atterrissage, t)
    }

    private fun arc(lat1: Double, lon1: Double, lat2: Double, lon2: Double, debut: Instant, fin: Instant, t: Instant): Point {
        val total = Duration.between(debut, fin).toMillis().toDouble()
        val f = (Duration.between(debut, t).toMillis() / total).coerceIn(0.0, 1.0)
        val a = vecteur(lat1, lon1)
        val b = vecteur(lat2, lon2)
        val omega = acos((a[0] * b[0] + a[1] * b[1] + a[2] * b[2]).coerceIn(-1.0, 1.0))
        val p = if (omega < 1e-9) a else DoubleArray(3) { i ->
            (sin((1 - f) * omega) * a[i] + sin(f * omega) * b[i]) / sin(omega)
        }
        val lat = Math.toDegrees(asin(p[2].coerceIn(-1.0, 1.0)))
        val lon = Math.toDegrees(atan2(p[1], p[0]))
        return Point(t, lat, lon)
    }

    private fun vecteur(lat: Double, lon: Double): DoubleArray {
        val la = Math.toRadians(lat)
        val lo = Math.toRadians(lon)
        return doubleArrayOf(cos(la) * cos(lo), cos(la) * sin(lo), sin(la))
    }

    /** La distance du grand cercle, en kilomètres (haversine, rayon moyen : comme Villes.distanceKm). */
    fun distanceKm(plan: Plan): Double = distanceKm(plan.depart.latitude, plan.depart.longitude, plan.arrivee.latitude, plan.arrivee.longitude)

    fun distanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val h = sin(dLat / 2) * sin(dLat / 2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2) * sin(dLon / 2)
        return 2 * 6371.0 * asin(sqrt(h.coerceIn(0.0, 1.0)))
    }

    /**
     * Le décalage de l'heure solaire moyenne au point, arrondi à l'heure : le
     * fuseau nautique (UTC + n, n = longitude / 15). Il choisit seulement quel
     * jour civil demander au moteur ; les zmanim n'en dépendent pas.
     */
    fun decalageNautique(longitude: Double): Int = (longitude / 15.0).roundToInt().coerceIn(-12, 12)

    /** « Etc/GMT-3 » pour UTC+3 : le signe des fuseaux Etc est inversé (tzdb). */
    fun fuseauNautique(longitude: Double): String = decalageNautique(longitude).let {
        when {
            it == 0 -> "Etc/GMT"
            it > 0 -> "Etc/GMT-$it"
            else -> "Etc/GMT+${-it}"
        }
    }

    /** Le lieu du moteur pour le point sous l'avion, au niveau de la mer comme tout le profil. */
    fun lieuSous(p: Point): Zmanim.Lieu = Zmanim.Lieu("", p.latitude, p.longitude, 0.0, fuseauNautique(p.longitude))

    /** Un zman sur la route : l'instant, et où est l'avion à cet instant. */
    data class Evenement(
        val cle: String,
        val instant: Instant,
        val latitude: Double,
        val longitude: Double,
        /**
         * De combien cet horaire bouge si le vol part 30 min plus tard (même durée) ; avec un relevé
         * GPS, si l'arrivée a 30 min de retard. null : sans correspondant.
         */
        val ecartSiRetard: Duration? = null,
    )

    /** Ce que l'écran doit dire de la donnée, sans conseil : des faits. */
    enum class Reserve {
        /** La route passe au-delà de 60° de latitude : les méthodes en degrés et en minutes s'écartent, certains zmanim manquent. */
        HAUTE_LATITUDE,

        /** Un zman n'a pas lieu sur une partie de la route (soleil de minuit, nuit polaire). */
        ABSENT,

        /** Le moteur a échoué en un point : une erreur, distincte d'un événement absent. */
        ERREUR,

        /** L'avion va plus vite que le soleil vers l'ouest : un zman repasse à rebours. */
        A_REBOURS,
    }

    /** Le passage du méridien de 180° : la date locale (fuseau nautique) passe de [avant] à [apres]. */
    data class LigneDate(val instant: Instant, val avant: LocalDate, val apres: LocalDate)

    /** La fiabilité : la route prévue seule, ou recalée sur un relevé pris en vol (GPS ou suivi en direct). */
    enum class Fiabilite { ROUTE_PREVUE, RELEVE_GPS, RELEVE_DIRECT }

    data class Calcul(
        val plan: Plan,
        /** Tous les zmanim de la fenêtre (30 h avant le décollage à 30 h après l'atterrissage), dans l'ordre. */
        val evenements: List<Evenement>,
        val reserves: Set<Reserve>,
        val lignesDate: List<LigneDate>,
        val fiabilite: Fiabilite = when {
            !releveUtilisable(plan) -> Fiabilite.ROUTE_PREVUE
            plan.sourceReleve == SourceReleve.DIRECT -> Fiabilite.RELEVE_DIRECT
            else -> Fiabilite.RELEVE_GPS
        },
    ) {
        /** Les zmanim entre le décollage et l'atterrissage. */
        val enVol: List<Evenement>
            get() = evenements.filter { !it.instant.isBefore(plan.decollage) && !it.instant.isAfter(plan.atterrissage) }
    }

    val MARGE: Duration = Duration.ofHours(30)
    private val PAS_VOL: Duration = Duration.ofMinutes(5)
    private val PAS_SOL: Duration = Duration.ofMinutes(60)
    private const val APPARIEMENT_S = 3 * 3600.0
    private const val TOLERANCE_S = 0.5
    const val LATITUDE_HAUTE = 60.0
    val RETARD: Duration = Duration.ofMinutes(30)

    /** Le calcul complet, avec l'écart de chaque horaire en vol si le départ glisse de 30 minutes. */
    fun calculer(plan: Plan): Calcul {
        val base = calculerSeul(plan)
        // Avec un relevé GPS, le départ est passé : l'incertitude est l'arrivée, 30 min plus tard.
        val retard = calculerSeul(if (releveUtilisable(plan)) plan.copy(atterrissage = plan.atterrissage + RETARD)
            else plan.copy(decollage = plan.decollage + RETARD, atterrissage = plan.atterrissage + RETARD))
        val evenements = base.evenements.map { e ->
            if (e.instant.isBefore(plan.decollage) || e.instant.isAfter(plan.atterrissage)) return@map e
            val proche = retard.evenements.filter { it.cle == e.cle }
                .minByOrNull { abs(Duration.between(e.instant, it.instant).seconds) }
                ?.takeIf { abs(Duration.between(e.instant, it.instant).seconds) <= APPARIEMENT_S }
            e.copy(ecartSiRetard = proche?.let { Duration.between(e.instant, it.instant) })
        }
        return base.copy(evenements = evenements)
    }

    private class Echantillon(val t: Instant, val p: Point, val occurrences: Map<String, List<Instant>>)

    private fun calculerSeul(plan: Plan): Calcul {
        val reserves = mutableSetOf<Reserve>()
        val instants = buildList {
            var t = plan.decollage - MARGE
            while (t.isBefore(plan.decollage)) { add(t); t += PAS_SOL }
            t = plan.decollage
            while (t.isBefore(plan.atterrissage)) { add(t); t += PAS_VOL }
            t = plan.atterrissage
            val fin = plan.atterrissage + MARGE
            while (!t.isAfter(fin)) { add(t); t += PAS_SOL }
        }
        val echantillons = instants.map { t ->
            val p = position(plan, t)
            val enVol = !t.isBefore(plan.decollage) && !t.isAfter(plan.atterrissage)
            if (enVol && abs(p.latitude) > LATITUDE_HAUTE) reserves += Reserve.HAUTE_LATITUDE
            Echantillon(t, p, occurrences(p) { if (enVol) reserves += it })
        }
        val trouves = mutableListOf<Evenement>()
        for (i in 0 until echantillons.size - 1) {
            val a = echantillons[i]
            val b = echantillons[i + 1]
            val enVol = a.t.isBefore(plan.atterrissage) && b.t.isAfter(plan.decollage)
            for (cle in Zmanim.PROFIL.map { it.first }) {
                val oa = a.occurrences[cle].orEmpty()
                val ob = b.occurrences[cle].orEmpty()
                for (o in oa) {
                    // Sans vis-à-vis : la fenêtre de trois jours a glissé avec la date locale. Un zman
                    // vraiment absent est signalé par le moteur (Statut.ABSENT), pas ici.
                    val apparie = ob.minByOrNull { abs(secondes(o, it)) }?.takeIf { abs(secondes(o, it)) <= APPARIEMENT_S }
                        ?: continue
                    val fa = secondes(a.t, o)
                    val fb = secondes(b.t, apparie)
                    // Racine dans ]a, b] : le zman du point rejoint l'heure.
                    val descend = fa > 0 && fb <= 0
                    val remonte = fa < 0 && fb >= 0
                    if (!descend && !remonte) continue
                    if (remonte && enVol) reserves += Reserve.A_REBOURS
                    val t = racine(plan, cle, a.t, o, b.t, apparie)
                    val p = position(plan, t)
                    trouves += Evenement(cle, t, p.latitude, p.longitude)
                }
            }
        }
        // Un même zman trouvé deux fois à la seconde près (aux bornes d'un intervalle) ne compte qu'une fois.
        val evenements = trouves.sortedBy { it.instant }.fold(mutableListOf<Evenement>()) { acc, e ->
            if (acc.none { it.cle == e.cle && abs(secondes(it.instant, e.instant)) < 60 }) acc += e
            acc
        }
        return Calcul(plan, evenements, reserves, lignesDate(plan, echantillons))
    }

    /** Les zmanim du point pour la veille, le jour et le lendemain (jour du fuseau nautique). */
    private fun occurrences(p: Point, signaler: (Reserve) -> Unit = {}): Map<String, List<Instant>> {
        val lieu = lieuSous(p)
        val jour = LocalDate.ofInstant(p.instant, ZoneId.of(lieu.fuseau))
        val parCle = mutableMapOf<String, MutableList<Instant>>()
        for (d in listOf(jour.minusDays(1), jour, jour.plusDays(1))) {
            for (z in Zmanim.jour(lieu, d)) {
                when (z.statut) {
                    Zmanim.Statut.OK -> parCle.getOrPut(z.cle) { mutableListOf() } += z.instant!!
                    Zmanim.Statut.ABSENT -> signaler(Reserve.ABSENT)
                    Zmanim.Statut.ERREUR -> signaler(Reserve.ERREUR)
                }
            }
        }
        return parCle
    }

    /** Fausse position (Illinois) sur ]a, b] : t tel que zman(position(t)) = t, à la demi-seconde. */
    private fun racine(plan: Plan, cle: String, a: Instant, oa: Instant, b: Instant, ob: Instant): Instant {
        var ta = 0.0
        var tb = secondes(a, b)
        var fa = secondes(a, oa)
        var fb = secondes(b, ob)
        var cote = 0
        repeat(60) {
            val t = if (fb == fa) (ta + tb) / 2 else tb - fb * (tb - ta) / (fb - fa)
            val instant = a.plusMillis((t * 1000).toLong())
            val attendu = oa.plusMillis(((secondes(oa, ob)) * (t / secondes(a, b)) * 1000).toLong())
            val o = occurrences(position(plan, instant))[cle].orEmpty().minByOrNull { abs(secondes(attendu, it)) }
                ?: return instant
            val f = secondes(instant, o)
            if (abs(f) <= TOLERANCE_S) return o
            if ((f > 0) == (fa > 0)) {
                ta = t; fa = f
                if (cote == -1) fb /= 2
                cote = -1
            } else {
                tb = t; fb = f
                if (cote == 1) fa /= 2
                cote = 1
            }
        }
        return a.plusMillis((((ta + tb) / 2) * 1000).toLong())
    }

    private fun secondes(de: Instant, a: Instant): Double = Duration.between(de, a).toMillis() / 1000.0

    /** Le passage du méridien de 180° en vol, et la date locale de part et d'autre. */
    private fun lignesDate(plan: Plan, echantillons: List<Echantillon>): List<LigneDate> {
        val lignes = mutableListOf<LigneDate>()
        for (i in 0 until echantillons.size - 1) {
            val a = echantillons[i].p
            val b = echantillons[i + 1].p
            if (a.instant.isBefore(plan.decollage) || b.instant.isAfter(plan.atterrissage)) continue
            if (abs(b.longitude - a.longitude) <= 180) continue
            // Vers l'est, la longitude passe de +180 à -180 ; la part du pas avant la ligne.
            val avantLigne = 180 - abs(a.longitude)
            val part = avantLigne / (avantLigne + 180 - abs(b.longitude))
            val t = a.instant.plusMillis((Duration.between(a.instant, b.instant).toMillis() * part).toLong())
            lignes += LigneDate(t, LocalDate.ofInstant(t, ZoneId.of(fuseauNautique(a.longitude))),
                LocalDate.ofInstant(t, ZoneId.of(fuseauNautique(b.longitude))))
        }
        return lignes
    }

    /**
     * Les horaires que ZmanimWidget.prochaine attend, pris sur la route : l'aube
     * passée la plus récente, puis la première fin de Cha'harit, chki'a et
     * minuit qui la suivent. Sans aube passée (route polaire), les prochains
     * de chaque zman. La règle de prochaine n'est pas réécrite.
     */
    fun horaires(calcul: Calcul, maintenant: Instant): Map<String, ZonedDateTime?> {
        val ev = calcul.evenements
        val alot = ev.lastOrNull { it.cle == "alot" && !it.instant.isAfter(maintenant) }?.instant
        val apres = alot ?: maintenant
        fun suivant(cle: String) = ev.firstOrNull { it.cle == cle && it.instant.isAfter(apres) }?.instant
        return mapOf(
            "alot" to alot,
            "szt_gra" to suivant("szt_gra"),
            "chkia" to suivant("chkia"),
            "chatsot_layla" to suivant("chatsot_layla"),
        ).mapValues { (_, v) -> v?.atZone(ZoneOffset.UTC) }
    }
}
