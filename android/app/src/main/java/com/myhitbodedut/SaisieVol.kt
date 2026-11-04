package com.myhitbodedut

import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.TemporalAdjusters
import kotlin.math.roundToLong

/**
 * La saisie rapide d'un vol (04/11) : une phrase comme on la dirait — « El Al Lyon TLV demain
 * 11h », « LY 382 de Paris à Tel Aviv le 12/11 à 9h40 », « מחר ב־11 טיסת אל על מליון לתל אביב »
 * — ou le texte d'un billet, d'un courriel, d'une carte d'embarquement. Tout se lit sur le
 * téléphone, sans réseau : ce qui est sûr remplit le formulaire, le reste reste à saisir.
 * Même logique que vol.js (lirePhrase). Pas d'Android ici : testable sur la machine virtuelle.
 */
object SaisieVol {

    data class Proposition(
        val numero: String? = null,
        /** Le code IATA de la compagnie, s'il a été reconnu (numéro ou nom). */
        val compagnie: String? = null,
        val depart: Vol.Aeroport? = null,
        val arrivee: Vol.Aeroport? = null,
        val date: LocalDate? = null,
        val heureDepart: LocalTime? = null,
        val heureArrivee: LocalTime? = null,
        /** La date d'arrivée, si le texte en donne une autre que celle du départ. */
        val dateArrivee: LocalDate? = null,
    ) {
        val vide get() = numero == null && depart == null && arrivee == null && date == null && heureDepart == null
    }

    /** Les compagnies les plus courantes, par leur nom (latin, hébreu) → code IATA. */
    val COMPAGNIES: Map<String, String> = mapOf(
        "el al" to "LY", "elal" to "LY", "אל על" to "LY", "arkia" to "IZ", "ארקיע" to "IZ", "israir" to "6H", "ישראייר" to "6H",
        "air haifa" to "H9", "air france" to "AF", "transavia" to "TO", "easyjet" to "U2", "easy jet" to "U2", "wizz" to "W6",
        "wizz air" to "W6", "ryanair" to "FR", "lufthansa" to "LH", "swiss" to "LX", "british airways" to "BA", "klm" to "KL",
        "delta" to "DL", "united" to "UA", "american airlines" to "AA", "turkish" to "TK", "turkish airlines" to "TK",
        "aegean" to "A3", "ita" to "AZ", "ita airways" to "AZ", "iberia" to "IB", "air europa" to "UX", "emirates" to "EK",
        "etihad" to "EY", "flydubai" to "FZ", "brussels airlines" to "SN", "austrian" to "OS", "lot" to "LO", "air canada" to "AC",
        "ethiopian" to "ET", "vueling" to "VY", "volotea" to "V7", "tap" to "TP", "air portugal" to "TP", "finnair" to "AY",
        "sas" to "SK", "aeroflot" to "SU", "air india" to "AI", "cathay" to "CX", "qatar" to "QR", "qatar airways" to "QR",
        "eurowings" to "EW", "pegasus" to "PC", "bluebird" to "BZ", "smartwings" to "QS", "corsair" to "SS", "air corsica" to "XK",
        "air senegal" to "HC", "royal air maroc" to "AT", "tunisair" to "TU", "egyptair" to "MS", "jet2" to "LS",
    )

    /**
     * Les villes à plusieurs aéroports : celui qu'on prend quand seule la ville est dite. On peut
     * toujours toucher la tuile pour en choisir un autre.
     */
    private val PRINCIPAL = mapOf(
        "paris" to "CDG", "london" to "LHR", "londres" to "LHR", "new york" to "JFK", "milan" to "MXP", "milano" to "MXP",
        "rome" to "FCO", "roma" to "FCO", "moscow" to "SVO", "moscou" to "SVO", "tokyo" to "NRT", "berlin" to "BER",
        "bruxelles" to "BRU", "brussels" to "BRU", "tel aviv" to "TLV", "tel aviv yafo" to "TLV", "chicago" to "ORD",
        "washington" to "IAD", "montreal" to "YUL", "toronto" to "YYZ", "istanbul" to "IST", "stockholm" to "ARN",
        "bangkok" to "BKK", "dubai" to "DXB", "lyon" to "LYS", "nice" to "NCE", "marseille" to "MRS", "geneve" to "GVA",
        "geneva" to "GVA", "bale" to "BSL", "basel" to "BSL", "eilat" to "ETM", "buenos aires" to "EZE", "sao paulo" to "GRU",
        "los angeles" to "LAX", "miami" to "MIA", "venise" to "VCE", "venice" to "VCE", "barcelone" to "BCN", "madrid" to "MAD",
        "athenes" to "ATH", "athens" to "ATH", "kiev" to "KBP", "kyiv" to "KBP", "varsovie" to "WAW", "warsaw" to "WAW",
        "פריז" to "CDG", "לונדון" to "LHR", "ניו יורק" to "JFK", "תל אביב" to "TLV", "אילת" to "ETM", "ליון" to "LYS",
        "מרסיי" to "MRS", "ניס" to "NCE", "רומא" to "FCO", "מילאנו" to "MXP", "ברלין" to "BER",
    )

    /** Les mots de trois lettres qui ne sont pas des codes d'aéroport dans une phrase. */
    private val MOTS = setOf(
        "les", "des", "vol", "par", "sur", "pour", "une", "est", "and", "the", "for", "via", "dep", "arr", "seat", "gate",
        "the", "you", "are", "air", "new", "bus", "car", "jet", "fly", "pax", "ref", "tax", "fee", "eur", "usd", "ils",
        "son", "ses", "mon", "mes", "ton", "nos", "vos", "aux", "pas", "que", "qui", "oui", "non", "lun", "mar", "mer",
        "jeu", "ven", "sam", "dim", "mon", "tue", "wed", "thu", "fri", "sat", "sun", "jan", "fev", "feb", "avr", "apr",
        "mai", "may", "jun", "jul", "aou", "aug", "sep", "oct", "nov", "dec", "bag", "row", "pnr", "age", "end", "all",
    )

    private val MOIS = mapOf(
        "janvier" to 1, "janv" to 1, "jan" to 1, "january" to 1, "ינואר" to 1,
        "fevrier" to 2, "fevr" to 2, "fev" to 2, "february" to 2, "feb" to 2, "פברואר" to 2,
        "mars" to 3, "march" to 3, "מרץ" to 3, "מרס" to 3,
        "avril" to 4, "avr" to 4, "april" to 4, "apr" to 4, "אפריל" to 4,
        "mai" to 5, "may" to 5, "מאי" to 5,
        "juin" to 6, "june" to 6, "jun" to 6, "יוני" to 6,
        "juillet" to 7, "juil" to 7, "july" to 7, "jul" to 7, "יולי" to 7,
        "aout" to 8, "august" to 8, "aug" to 8, "אוגוסט" to 8,
        "septembre" to 9, "sept" to 9, "sep" to 9, "september" to 9, "ספטמבר" to 9,
        "octobre" to 10, "oct" to 10, "october" to 10, "אוקטובר" to 10,
        "novembre" to 11, "nov" to 11, "november" to 11, "נובמבר" to 11,
        "decembre" to 12, "dec" to 12, "december" to 12, "דצמבר" to 12,
    )

    private val JOURS = mapOf(
        "lundi" to DayOfWeek.MONDAY, "monday" to DayOfWeek.MONDAY, "שני" to DayOfWeek.MONDAY,
        "mardi" to DayOfWeek.TUESDAY, "tuesday" to DayOfWeek.TUESDAY, "שלישי" to DayOfWeek.TUESDAY,
        "mercredi" to DayOfWeek.WEDNESDAY, "wednesday" to DayOfWeek.WEDNESDAY, "רביעי" to DayOfWeek.WEDNESDAY,
        "jeudi" to DayOfWeek.THURSDAY, "thursday" to DayOfWeek.THURSDAY, "חמישי" to DayOfWeek.THURSDAY,
        "vendredi" to DayOfWeek.FRIDAY, "friday" to DayOfWeek.FRIDAY, "שישי" to DayOfWeek.FRIDAY,
        "samedi" to DayOfWeek.SATURDAY, "saturday" to DayOfWeek.SATURDAY, "שבת" to DayOfWeek.SATURDAY,
        "dimanche" to DayOfWeek.SUNDAY, "sunday" to DayOfWeek.SUNDAY, "ראשון" to DayOfWeek.SUNDAY,
    )

    /** Les mots qui annoncent l'arrivée (« vers Lyon », « לתל אביב » se lit par la lettre ל). */
    private val VERS = setOf("vers", "pour", "a", "to", "destination", "arrivee", "arrival", "arr", "→", "->")
    private val DE = setOf("de", "depuis", "from", "depart", "departure", "dep")

    fun lire(texte: String, aeroports: List<Vol.Aeroport>, aujourdhui: LocalDate): Proposition {
        val plie = Aeroports.plier(texte.replace("→", " → ").replace("->", " → ").replace("✈", " "))
        val mots = plie.split(' ').filter { it.isNotEmpty() }

        // Le numéro de vol : deux caractères (ou trois lettres) puis 1 à 4 chiffres, collés ou non.
        val num = Regex("(?<![a-z0-9])([a-z]{2,3}|[a-z][0-9]|[0-9][a-z])\\s?([0-9]{1,4})([a-z]?)(?![a-z0-9:h/])(?!\\s?(?:jan|fe|mar|av|ap|mai|may|juin|jun|juil|jul|ao|aug|sep|oct|nov|dec))")
            .findAll(plie).mapNotNull { m ->
                val code = m.groupValues[1]
                // « à 11 », « le 12 » ne sont pas des numéros de vol.
                if (code in setOf("a", "le", "la", "de", "du", "au", "et", "on", "at", "le", "ב", "the") || code in MOTS) null
                else RouteVol.normaliser(code + m.groupValues[2] + m.groupValues[3])?.let { (code + m.groupValues[2] + m.groupValues[3]).uppercase() }
            }.firstOrNull()

        val compagnie = num?.let { RouteVol.normaliser(it)?.code?.takeIf { c -> c.length == 2 } }
            ?: COMPAGNIES.entries.sortedByDescending { it.key.length }.firstOrNull { (nom, _) -> contientMot(plie, Aeroports.plier(nom)) }?.value

        val (depart, arrivee) = aeroports(mots, aeroports, num)
        val leger = java.text.Normalizer.normalize(texte, java.text.Normalizer.Form.NFD).replace(Regex("\\p{M}"), "").lowercase()
        val (date, dateArrivee) = dates(plie, leger, mots, aujourdhui)
        val heures = heures(leger)
        return Proposition(num, compagnie, depart, arrivee, date, heures.getOrNull(0), heures.getOrNull(1), dateArrivee)
    }

    private fun contientMot(texte: String, mot: String) = Regex("(^|\\s)${Regex.escape(mot)}(\\s|$)").containsMatchIn(texte)

    /**
     * Les aéroports dans l'ordre où ils sont dits ; « de X », « vers Y », « ל־Y », « מ־X »
     * l'emportent sur l'ordre. On essaie les groupes de trois, deux puis un mot.
     */
    private fun aeroports(mots: List<String>, aeroports: List<Vol.Aeroport>, numero: String?): Pair<Vol.Aeroport?, Vol.Aeroport?> {
        data class Trouve(val a: Vol.Aeroport, val pos: Int, val role: Int) // role : -1 départ, 1 arrivée, 0 inconnu
        val trouves = mutableListOf<Trouve>()
        val pris = BooleanArray(mots.size)
        for (n in 3 downTo 1) {
            for (i in 0..mots.size - n) {
                if ((i until i + n).any { pris[it] }) continue
                var gram = mots.subList(i, i + n).joinToString(" ")
                var role = 0
                // L'hébreu colle « מ » (de) et « ל » (vers) au nom : on essaie sans.
                var a = trouver(gram, aeroports, n)
                if (a == null && n >= 1 && (gram.startsWith("מ") || gram.startsWith("ל"))) {
                    a = trouver(gram.drop(1), aeroports, n)
                    if (a != null) role = if (gram.startsWith("ל")) 1 else -1
                    gram = gram.drop(1)
                }
                if (a == null) continue
                if (numero != null && gram.equals(numero, ignoreCase = true)) continue
                val avant = mots.getOrNull(i - 1)
                if (role == 0) role = when (avant) { in VERS -> 1; in DE -> -1; else -> 0 }
                (i until i + n).forEach { pris[it] = true }
                if (trouves.none { it.a == a }) trouves += Trouve(a, i, role)
            }
        }
        trouves.sortBy { it.pos }
        val dep = trouves.firstOrNull { it.role == -1 } ?: trouves.firstOrNull { it.role == 0 }
        val arr = trouves.firstOrNull { it.role == 1 && it != dep } ?: trouves.firstOrNull { it != dep && it.role == 0 }
        return dep?.a to arr?.a
    }

    /** Un nom de ville exact, ou un code IATA (pas un mot courant de trois lettres). */
    private fun trouver(gram: String, aeroports: List<Vol.Aeroport>, n: Int): Vol.Aeroport? {
        if (gram.length < 3) return null
        if (n == 1 && (gram in MOTS || gram in VERS || gram in DE || gram in MOIS || gram in JOURS)) return null
        PRINCIPAL[gram]?.let { iata -> Aeroports.parIata(aeroports, iata)?.let { return it } }
        if (n == 1 && gram.length == 3 && gram !in MOTS && gram.all { it in 'a'..'z' }) {
            Aeroports.parIata(aeroports, gram.uppercase())?.let { return it }
        }
        return Aeroports.exact(aeroports, gram)
    }

    /** La date du vol, et celle de l'arrivée si une seconde est donnée. */
    private fun dates(plie: String, leger: String, mots: List<String>, aujourdhui: LocalDate): Pair<LocalDate?, LocalDate?> {
        val trouvees = mutableListOf<Pair<Int, LocalDate>>()
        fun ajoute(pos: Int, d: LocalDate?) { if (d != null) trouvees += pos to d }
        // Relatifs. « après demain » avant « demain ».
        Regex("apres demain|after tomorrow|מחרתיים").find(plie)?.let { ajoute(it.range.first, aujourdhui.plusDays(2)) }
        if (trouvees.isEmpty()) Regex("(^|\\s)(demain|tomorrow|מחר)(\\s|$)").find(plie)?.let { ajoute(it.range.first, aujourdhui.plusDays(1)) }
        Regex("(^|\\s)(aujourd hui|aujourdhui|today|ce soir|tonight|היום|הערב)(\\s|$)").find(plie)?.let { ajoute(it.range.first, aujourdhui) }
        // 12/11, 12.11.2026, 2026-11-12 : sur le texte où la ponctuation reste. Les positions de ce
        // texte et du texte plié diffèrent un peu ; elles ne servent qu'à l'ordre.
        Regex("(?<![0-9])([0-9]{4})-([0-9]{1,2})-([0-9]{1,2})(?![0-9])").findAll(leger).forEach { m ->
            ajoute(m.range.first, date(m.groupValues[1].toInt(), m.groupValues[2].toInt(), m.groupValues[3].toInt()))
        }
        Regex("(?<![0-9:./])([0-9]{1,2})[/.]([0-9]{1,2})(?:[/.]([0-9]{2,4}))?(?![0-9:])").findAll(leger).forEach { m ->
            val j = m.groupValues[1].toInt()
            val mo = m.groupValues[2].toInt()
            val an = m.groupValues[3].takeIf { it.isNotEmpty() }?.toInt()?.let { if (it < 100) 2000 + it else it }
            ajoute(m.range.first, if (an != null) date(an, mo, j) else prochaine(aujourdhui, mo, j))
        }
        // 12 novembre, 12 nov 2026, nov 12
        Regex("(?<![0-9])([0-9]{1,2})(?:er)?\\s+([a-z\\u05D0-\\u05EA]+)(?:\\s+([0-9]{4}))?").findAll(plie).forEach { m ->
            val mo = MOIS[m.groupValues[2]] ?: MOIS[m.groupValues[2].removePrefix("ב")] ?: return@forEach
            val an = m.groupValues[3].takeIf { it.isNotEmpty() }?.toInt()
            ajoute(m.range.first, if (an != null) date(an, mo, m.groupValues[1].toInt()) else prochaine(aujourdhui, mo, m.groupValues[1].toInt()))
        }
        Regex("([a-z]+)\\s+([0-9]{1,2})(?![0-9:h])(?:\\s+([0-9]{4}))?").findAll(plie).forEach { m ->
            val mo = MOIS[m.groupValues[1]] ?: return@forEach
            val an = m.groupValues[3].takeIf { it.isNotEmpty() }?.toInt()
            ajoute(m.range.first, if (an != null) date(an, mo, m.groupValues[2].toInt()) else prochaine(aujourdhui, mo, m.groupValues[2].toInt()))
        }
        // Un jour de la semaine seul : le prochain (aujourd'hui compris).
        if (trouvees.isEmpty()) mots.forEachIndexed { i, m ->
            val jour = JOURS[m] ?: JOURS[m.removePrefix("ב")] ?: return@forEachIndexed
            // En hébreu, le nom du jour suit « יום » (« שני » est aussi « deuxième »), sauf שבת.
            if (m.any { it in '\u05D0'..'\u05EA' } && m != "שבת" && m != "בשבת" && mots.getOrNull(i - 1) !in setOf("יום", "ביום")) return@forEachIndexed
            trouvees += i to aujourdhui.with(TemporalAdjusters.nextOrSame(jour))
        }
        val tri = trouvees.sortedBy { it.first }.map { it.second }.distinct()
        return tri.getOrNull(0) to tri.getOrNull(1)
    }

    private fun date(an: Int, mois: Int, jour: Int): LocalDate? = runCatching { LocalDate.of(an, mois, jour) }.getOrNull()

    /** Le prochain jour/mois à venir (ou d'il y a moins d'une semaine : un billet relu après coup). */
    private fun prochaine(aujourdhui: LocalDate, mois: Int, jour: Int): LocalDate? {
        val d = date(aujourdhui.year, mois, jour) ?: return null
        return if (d.isBefore(aujourdhui.minusDays(7))) date(aujourdhui.year + 1, mois, jour) else d
    }

    /**
     * Les heures, dans l'ordre : « 11h », « 11h30 », « 11:30 », « 11 am », « 11pm », « à 11 »,
     * « בשעה 11 ». Celles de l'embarquement ou de la fermeture de la porte, qu'un billet donne
     * avant le départ, sont écartées.
     */
    fun heures(leger: String): List<LocalTime> {
        val r = Regex("(?<![0-9])([01]?[0-9]|2[0-3])(?:\\s?h\\s?([0-5][0-9])?|:([0-5][0-9])(?:\\s?(am|pm))?|\\s?(am|pm))(?![0-9])" +
            "|(?<![a-z\\u05D0-\\u05EA])(?:בשעה|a|à|at|vers)\\s([01]?[0-9]|2[0-3])(?![0-9/:.]|\\s?h|\\s?am|\\s?pm)")
        return r.findAll(leger).mapNotNull { m ->
            val avant = leger.substring(maxOf(0, m.range.first - 24), m.range.first)
            if (Regex("boarding|embarquement|gate|porte|עלייה|שער|fermeture|closes").containsMatchIn(avant)) return@mapNotNull null
            val g = m.groupValues
            if (g[6].isNotEmpty()) return@mapNotNull LocalTime.of(g[6].toInt(), 0)
            var h = g[1].toInt()
            val min = (g[2].ifEmpty { g[3] }).ifEmpty { "0" }.toInt()
            val apm = g[4].ifEmpty { g[5] }
            if (apm == "pm" && h < 12) h += 12
            if (apm == "am" && h == 12) h = 0
            LocalTime.of(h, min)
        }.toList()
    }

    /**
     * La durée de vol estimée quand le billet ne donne pas l'arrivée : la distance à 820 km/h,
     * plus 30 minutes de roulage et de montée. Une estimation, dite comme telle à l'écran.
     */
    fun dureeEstimee(depart: Vol.Aeroport, arrivee: Vol.Aeroport): Duration {
        val km = Vol.distanceKm(depart.latitude, depart.longitude, arrivee.latitude, arrivee.longitude)
        val minutes = (km / 820.0 * 60.0 + 30.0).roundToLong()
        // Au quart d'heure le plus proche : une estimation ne doit pas avoir l'air précise.
        return Duration.ofMinutes(((minutes + 7) / 15) * 15)
    }
}
