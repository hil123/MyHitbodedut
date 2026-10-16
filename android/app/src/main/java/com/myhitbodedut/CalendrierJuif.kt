package com.myhitbodedut

import com.kosherjava.zmanim.hebrewcalendar.HebrewDateFormatter
import com.kosherjava.zmanim.hebrewcalendar.JewishCalendar
import com.kosherjava.zmanim.hebrewcalendar.JewishDate
import com.kosherjava.zmanim.hebrewcalendar.TefilaRules
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

/**
 * Le jour hébraïque et ce qu'il change aux prières (demande du 10/10), hors
 * ligne, par KosherJava 2.5.0 : JewishCalendar pour la date et les fêtes,
 * TefilaRules pour les ajouts. Aucune règle n'est écrite ici : chaque
 * condition renvoie à une méthode de la bibliothèque, lue dans le jar et dans
 * ses sources (zmanim-2.5.0-sources.jar). Les coutumes de Ta'hanoun sont les
 * réglages par défaut de TefilaRules, les mêmes pour les trois nusah : à
 * valider par un Rav (ZMANIM.md).
 *
 * Le même calcul existe côté web (web/src/js/tefila.js), vérifié jour par
 * jour contre ce fichier-ci par des vecteurs que KosherJava a produits
 * (web/test/vecteurs_kosherjava.json, tools/vecteurs_kosherjava).
 *
 * Pas d'Android ici : testable sur la machine virtuelle seule.
 */
object CalendrierJuif {

    /** Les clés de condition, communes à Android, au web et aux annotations du siddour. */
    object C {
        const val YAALE_VEYAVO = "yaale_veyavo"
        const val ROCH_HODECH = "roch_hodech"
        const val PESSAH = "pessah"
        const val SOUKKOT = "soukkot"
        const val CHAVOUOT = "chavouot"
        const val ROCH_HACHANA = "roch_hachana"
        const val CHEMINI_ATSERET = "chemini_atseret"
        const val KIPPOUR = "kippour"
        const val AL_HANISSIM = "al_hanissim"
        const val HANOUKA = "hanouka"
        const val POURIM = "pourim"
        const val MACHIV_HAROUAH = "machiv_harouah"
        const val MORID_HATAL = "morid_hatal"
        const val TAL_OUMATAR = "tal_oumatar"
        const val BERAKHA = "berakha"
        const val HALLEL = "hallel"
        const val HALLEL_COMPLET = "hallel_complet"
        const val TAHANOUN_CHAHARIT = "tahanoun_chaharit"
        const val TAHANOUN_MINHA = "tahanoun_minha"
        const val LUNDI_JEUDI = "lundi_jeudi"
        const val ASSERET_YEMEI_TECHOUVA = "asseret_yemei_techouva"
        const val JEUNE = "jeune"
        const val TICHA_BEAV = "ticha_beav"
        const val CHABBAT = "chabbat"
        const val MOTSAE_CHABBAT = "motsae_chabbat"
        const val OMER = "omer"
        /** Du 22 Tichri à la fin du mois : Ta'hanoun selon la coutume (SiddurJour.conditionsPour). */
        const val FIN_TICHRI = "fin_tichri"

        val TOUTES = listOf(
            YAALE_VEYAVO, ROCH_HODECH, PESSAH, SOUKKOT, CHAVOUOT, ROCH_HACHANA, CHEMINI_ATSERET, KIPPOUR, AL_HANISSIM, HANOUKA, POURIM,
            MACHIV_HAROUAH, MORID_HATAL, TAL_OUMATAR, BERAKHA, HALLEL, HALLEL_COMPLET,
            TAHANOUN_CHAHARIT, TAHANOUN_MINHA, LUNDI_JEUDI, ASSERET_YEMEI_TECHOUVA,
            JEUNE, TICHA_BEAV, CHABBAT, MOTSAE_CHABBAT, OMER, FIN_TICHRI,
        )

        /** Avinou malkénou (SiddurJour.conditionsPour) : déduite des conditions du jour, hors des vecteurs KosherJava. */
        const val AVINOU_MALKENOU = "avinou_malkenou"
        /** Jour de lecture de la Torah (15/10) : déduite, hors des vecteurs KosherJava comme Avinou malkénou. */
        const val KRIAT_HATORAH = "kriat_hatorah"
        /**
         * Le psaume du jour (16/10) : « jour_1 » le dimanche … « jour_6 » le vendredi ; et les
         * jours où l'on en ajoute un (Edot) : le jeûne de Guedalia et le 10 Tévet, le lendemain
         * de Kippour, le jeûne d'Esther et Pourim, le 17 Tamouz. Déduites, hors des vecteurs.
         */
        val JOURS_SEMAINE = (1..6).map { "jour_$it" }
        const val TSOM_GUEDALIA_TEVET = "tsom_guedalia_tevet"
        const val LENDEMAIN_KIPPOUR = "lendemain_kippour"
        const val ESTHER_POURIM = "esther_pourim"
        const val CHIVA_ASSAR_TAMOUZ = "chiva_assar_tamouz"
        val DERIVEES = listOf(AVINOU_MALKENOU, KRIAT_HATORAH) + JOURS_SEMAINE +
            listOf(TSOM_GUEDALIA_TEVET, LENDEMAIN_KIPPOUR, ESTHER_POURIM, CHIVA_ASSAR_TAMOUZ)
    }

    /**
     * Les fêtes par l'index de JewishCalendar.getYomTovIndex() (constantes
     * lues dans JewishCalendar.java, 2.5.0). La clé donne le libellé
     * « fete_<clé> » dans strings.xml.
     */
    val FETES = mapOf(
        JewishCalendar.EREV_PESACH to "erev_pessah",
        JewishCalendar.PESACH to "pessah",
        JewishCalendar.CHOL_HAMOED_PESACH to "hol_hamoed_pessah",
        JewishCalendar.PESACH_SHENI to "pessah_cheni",
        JewishCalendar.EREV_SHAVUOS to "erev_chavouot",
        JewishCalendar.SHAVUOS to "chavouot",
        JewishCalendar.SEVENTEEN_OF_TAMMUZ to "tamouz_17",
        JewishCalendar.TISHA_BEAV to "ticha_beav",
        JewishCalendar.TU_BEAV to "tou_beav",
        JewishCalendar.EREV_ROSH_HASHANA to "erev_roch_hachana",
        JewishCalendar.ROSH_HASHANA to "roch_hachana",
        JewishCalendar.FAST_OF_GEDALYAH to "jeune_guedalia",
        JewishCalendar.EREV_YOM_KIPPUR to "erev_yom_kippour",
        JewishCalendar.YOM_KIPPUR to "yom_kippour",
        JewishCalendar.EREV_SUCCOS to "erev_soukkot",
        JewishCalendar.SUCCOS to "soukkot",
        JewishCalendar.CHOL_HAMOED_SUCCOS to "hol_hamoed_soukkot",
        JewishCalendar.HOSHANA_RABBA to "hochaana_rabba",
        JewishCalendar.SHEMINI_ATZERES to "chemini_atseret",
        JewishCalendar.SIMCHAS_TORAH to "simhat_torah",
        JewishCalendar.CHANUKAH to "hanouka",
        JewishCalendar.TENTH_OF_TEVES to "tevet_10",
        JewishCalendar.TU_BESHVAT to "tou_bichvat",
        JewishCalendar.FAST_OF_ESTHER to "jeune_esther",
        JewishCalendar.PURIM to "pourim",
        JewishCalendar.SHUSHAN_PURIM to "chouchan_pourim",
        JewishCalendar.PURIM_KATAN to "pourim_katan",
        JewishCalendar.ROSH_CHODESH to "roch_hodech",
        JewishCalendar.YOM_HASHOAH to "yom_hachoah",
        JewishCalendar.YOM_HAZIKARON to "yom_hazikaron",
        JewishCalendar.YOM_HAATZMAUT to "yom_haatsmaout",
        JewishCalendar.YOM_YERUSHALAYIM to "yom_yerouchalayim",
        JewishCalendar.LAG_BAOMER to "lag_baomer",
        JewishCalendar.SHUSHAN_PURIM_KATAN to "chouchan_pourim_katan",
        JewishCalendar.ISRU_CHAG to "isrou_hag",
    )

    data class Jour(
        val date: LocalDate,
        val enIsrael: Boolean,
        /** « כ״ט תשרי תשפ״ז », par HebrewDateFormatter. */
        val dateHebraique: String,
        /** Le quantième en lettres, « כ״ט », pour la grille du calendrier. */
        val jourEnLettres: String,
        val jourHebreu: Int,
        /** Le mois hébreu, constante de JewishDate (NISSAN = 1 … ADAR_II = 13). */
        val moisHebreu: Int,
        val anneeHebraique: Int,
        /** « תשרי תשפ״ז » : le titre d'un mois du calendrier. */
        val moisEtAnnee: String,
        /** Clés « fete_<clé> » du jour, sans doublon. */
        val fetes: List<String>,
        /** Le jour de l'omer compté le soir qui ouvre ce jour hébreu, 0 hors omer. */
        val omer: Int,
        /** La paracha lue ce Chabbat, en hébreu ; null en semaine ou un Chabbat de fête. */
        val paracha: String?,
        /** Chabbat ou Yom Tov : jour où le travail est interdit (isAssurBemelacha). */
        val chome: Boolean,
        val conditions: Set<String>,
    )

    private val formatHebreu = HebrewDateFormatter().apply { isHebrewFormat = true }

    /** Jérusalem, ville murée depuis Josué : Pourim y est le 15 Adar (Chouchan Pourim). */
    const val JERUSALEM_GEONAMES = 281184L

    /**
     * Une position dans Jérusalem (13/10) : à 10 km au plus de la Vieille Ville
     * (31,7767 N ; 35,2345 E). Le rayon est un choix de l'application, qui
     * approche l'agglomération ; ce n'est pas une limite halakhique.
     */
    fun aJerusalem(latitude: Double, longitude: Double): Boolean {
        val r = 6371.0
        val dLat = Math.toRadians(latitude - 31.7767)
        val dLon = Math.toRadians(longitude - 35.2345)
        val a = Math.sin(dLat / 2).let { it * it } +
            Math.cos(Math.toRadians(31.7767)) * Math.cos(Math.toRadians(latitude)) * Math.sin(dLon / 2).let { it * it }
        return 2 * r * Math.asin(Math.sqrt(a)) <= 10.0
    }

    /** Les noms de Pourim dans une ville murée (libellés « fete_<clé> »). */
    private val MUREE = mapOf("pourim" to "pourim_ailleurs", "chouchan_pourim" to "pourim_jerusalem")

    /**
     * [mukafChoma] : une ville murée depuis Josué, où Pourim et Al hanissim
     * sont le 15 Adar (JewishCalendar.setIsMukafChoma). Seule Jérusalem l'est
     * avec certitude parmi les villes proposées ; les villes en doute (Tibériade,
     * Safed…) gardent le 14, comme le calendrier commun.
     */
    fun jour(date: LocalDate, enIsrael: Boolean, mukafChoma: Boolean = false): Jour {
        // Deux calendriers : les fêtes modernes s'affichent, mais n'entrent pas
        // dans les règles de prière, où leur coutume varie (TefilaRules les
        // compte si isUseModernHolidays est vrai).
        val regles = calendrier(date, enIsrael, modernes = false).apply { setIsMukafChoma(mukafChoma) }
        val affiche = calendrier(date, enIsrael, modernes = true).apply { setIsMukafChoma(mukafChoma) }
        val tefila = TefilaRules()
        val semaine = date.dayOfWeek

        val conditions = buildSet {
            if (tefila.isYaalehVeyavoRecited(regles)) add(C.YAALE_VEYAVO)
            if (regles.isRoshChodesh) add(C.ROCH_HODECH)
            if (regles.isPesach) add(C.PESSAH)
            if (regles.isSuccos) add(C.SOUKKOT)
            // Les variantes de Ya'alé véyavo du Birkat hamazone, une par fête.
            if (regles.isShavuos) add(C.CHAVOUOT)
            if (regles.isRoshHashana) add(C.ROCH_HACHANA)
            if (regles.isShminiAtzeres || regles.isSimchasTorah) add(C.CHEMINI_ATSERET)
            if (regles.isYomKippur) add(C.KIPPOUR)
            if (tefila.isAlHanissimRecited(regles)) add(C.AL_HANISSIM)
            if (regles.isChanukah) add(C.HANOUKA)
            if (regles.isPurim) add(C.POURIM)
            if (tefila.isMashivHaruachRecited(regles)) add(C.MACHIV_HAROUAH)
            if (tefila.isMoridHatalRecited(regles)) add(C.MORID_HATAL)
            if (tefila.isVeseinTalUmatarRecited(regles)) add(C.TAL_OUMATAR)
            if (tefila.isVeseinBerachaRecited(regles)) add(C.BERAKHA)
            if (tefila.isHallelRecited(regles)) add(C.HALLEL)
            if (tefila.isHallelShalemRecited(regles)) add(C.HALLEL_COMPLET)
            // La veille de Roch Hachana, pas de Ta'hanoun à la prière (Choul'han
            // Aroukh, OC 581:3), que TefilaRules 2.5.0 fait dire.
            val tahanoun = tefila.isTachanunRecitedShacharis(regles) &&
                regles.yomTovIndex != JewishCalendar.EREV_ROSH_HASHANA
            if (tahanoun) add(C.TAHANOUN_CHAHARIT)
            if (tefila.isTachanunRecitedMincha(regles)) add(C.TAHANOUN_MINHA)
            // « לשני וחמישי » : le titre de la section du siddour le dit ; les
            // jours sans Ta'hanoun, la section ne se dit pas non plus.
            if (tahanoun && (semaine == DayOfWeek.MONDAY || semaine == DayOfWeek.THURSDAY)) add(C.LUNDI_JEUDI)
            if (regles.isAseresYemeiTeshuva) add(C.ASSERET_YEMEI_TECHOUVA)
            if (regles.isTaanis && !regles.isYomKippur) add(C.JEUNE)
            if (regles.isTishaBav) add(C.TICHA_BEAV)
            if (semaine == DayOfWeek.SATURDAY) add(C.CHABBAT)
            if (semaine == DayOfWeek.SUNDAY) add(C.MOTSAE_CHABBAT)
            if (regles.dayOfOmer > 0) add(C.OMER)
            if (regles.jewishMonth == JewishDate.TISHREI && regles.jewishDayOfMonth >= 22) add(C.FIN_TICHRI)
            // La lecture de la Torah à Cha'harit (15/10) : lundi et jeudi, Chabbat et fêtes,
            // Roch 'Hodech, 'Hol hamoed, 'Hanouka, Pourim, jeûnes publics (9 Av compris).
            if (semaine == DayOfWeek.MONDAY || semaine == DayOfWeek.THURSDAY ||
                listOf(C.CHABBAT, C.ROCH_HODECH, C.PESSAH, C.SOUKKOT, C.CHAVOUOT, C.ROCH_HACHANA, C.KIPPOUR,
                    C.CHEMINI_ATSERET, C.HANOUKA, C.POURIM, C.JEUNE, C.TICHA_BEAV).any { it in this }) add(C.KRIAT_HATORAH)
            // Le psaume du jour et les jours qui en ajoutent un (16/10).
            if (semaine != DayOfWeek.SATURDAY) add("jour_${semaine.value % 7 + 1}")
            val index = regles.yomTovIndex
            if (index == JewishCalendar.FAST_OF_GEDALYAH || index == JewishCalendar.TENTH_OF_TEVES) add(C.TSOM_GUEDALIA_TEVET)
            if (regles.jewishMonth == JewishDate.TISHREI && regles.jewishDayOfMonth == 11) add(C.LENDEMAIN_KIPPOUR)
            if (index == JewishCalendar.FAST_OF_ESTHER || C.POURIM in this) add(C.ESTHER_POURIM)
            if (index == JewishCalendar.SEVENTEEN_OF_TAMMUZ) add(C.CHIVA_ASSAR_TAMOUZ)
        }

        val fetes = buildList {
            // Dans une ville murée, le 15 Adar est Pourim ; le 14 ne l'est qu'ailleurs.
            FETES[affiche.yomTovIndex]?.let { add(if (mukafChoma) MUREE[it] ?: it else it) }
            if (affiche.isRoshChodesh && "roch_hodech" !in this) add("roch_hodech")
        }
        val paracha = if (semaine == DayOfWeek.SATURDAY && regles.parshah != JewishCalendar.Parsha.NONE)
            formatHebreu.formatParsha(regles) else null

        return Jour(
            date = date,
            enIsrael = enIsrael,
            dateHebraique = formatHebreu.format(regles),
            jourEnLettres = formatHebreu.formatHebrewNumber(regles.jewishDayOfMonth),
            jourHebreu = regles.jewishDayOfMonth,
            moisHebreu = regles.jewishMonth,
            anneeHebraique = regles.jewishYear,
            moisEtAnnee = formatHebreu.formatMonth(regles) + " " + formatHebreu.formatHebrewNumber(regles.jewishYear),
            fetes = fetes,
            omer = regles.dayOfOmer.coerceAtLeast(0),
            paracha = paracha?.takeIf { it.isNotBlank() },
            chome = regles.isAssurBemelacha,
            conditions = conditions,
        )
    }

    /**
     * Le jour hébreu dont relève une prière dite à [heure] le jour civil
     * [date]. Arvit et le Chema du coucher se disent le soir, qui ouvre déjà
     * le jour hébreu suivant : l'après-midi, c'est le lendemain ; après
     * minuit, c'est encore le jour civil, que la nuit a ouvert la veille.
     * Midi sert de borne parce qu'aucun Arvit ne se dit le matin ; la
     * chki'a, elle, demanderait une ville. Toutes les autres prières (et
     * les bénédictions du repas) relèvent du jour civil : les ajouts y
     * restent ouvrables à la main, ce n'est qu'une mise en évidence.
     */
    fun jourDeLaPriere(texte: String, date: LocalDate, heure: LocalTime): LocalDate = when (texte) {
        "arvit", "chema_coucher" -> if (heure >= LocalTime.NOON) date.plusDays(1) else date
        else -> date
    }

    private fun calendrier(date: LocalDate, enIsrael: Boolean, modernes: Boolean) =
        JewishCalendar(date).apply {
            setInIsrael(enIsrael)
            isUseModernHolidays = modernes
        }
}
