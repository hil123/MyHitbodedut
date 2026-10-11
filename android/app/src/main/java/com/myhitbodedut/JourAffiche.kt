package com.myhitbodedut

import com.myhitbodedut.CalendrierJuif.C
import java.time.LocalDate
import java.time.YearMonth

/**
 * Ce que l'onglet Zmanim et le calendrier montrent d'un jour (10/10) : les
 * ajouts à nommer, les horaires avec l'entrée et la sortie de Chabbat ou de
 * fête, et la grille d'un mois. Pas d'Android ici : testable sur la machine
 * virtuelle seule. Le même code existe côté web (web/src/js/jour-affiche.js).
 */
object JourAffiche {

    /**
     * L'illustration d'un jour de fête ou de Chabbat (11/10), par son nom de
     * ressource ; null un jour ordinaire. La fête passe avant Chabbat.
     */
    fun illustration(fetes: List<String>, date: LocalDate): String? = when {
        fetes.any { it in setOf("erev_soukkot", "soukkot", "hol_hamoed_soukkot", "hochaana_rabba") } -> "scene_sukkot"
        fetes.any { it in setOf("erev_pessah", "pessah", "hol_hamoed_pessah") } -> "char_matsa"
        "hanouka" in fetes -> "scene_hanukkah"
        fetes.any { it in setOf("pourim", "chouchan_pourim", "pourim_ailleurs", "pourim_jerusalem") } -> "char_purim"
        date.dayOfWeek == java.time.DayOfWeek.SATURDAY -> "scene_shabbat"
        else -> null
    }


    /**
     * Les ajouts du jour, dans l'ordre de l'Amida : des clés de libellé
     * (Libelles). Le nusah change trois choses (ZMANIM.md, « Différences de
     * nusah ») : Morid hatal hors d'Israël chez beaucoup d'Ashkénazes, la
     * bénédiction du Hallel partiel, et l'Anénou du particulier (Ashkenaz et
     * Sfard : à Min'ha seulement, Rema OC 565:3 ; Edot : à Cha'harit et à Min'ha,
     * et à Arvit le soir de Tich'a bèAv).
     */
    fun ajouts(
        c0: Set<String>,
        nusah: String = "edot",
        enIsrael: Boolean = false,
        sansTahanounFinTichri: Boolean = nusah == "edot",
    ): List<String> = buildList {
        val c = SiddurJour.conditionsPour(nusah, enIsrael, c0, sansTahanounFinTichri)
        if (C.ASSERET_YEMEI_TECHOUVA in c) add("ajout_asseret_yemei_techouva")
        if (C.MACHIV_HAROUAH in c) add("ajout_machiv_harouah") else if (C.MORID_HATAL in c) add("ajout_morid_hatal")
        add(if (C.TAL_OUMATAR in c) "ajout_tal_oumatar" else "ajout_berakha")
        if (C.JEUNE in c) {
            add("ajout_anenou_officiant")
            // Edot : à Tich'a bèAv, le jeûne commence la veille au soir, et
            // Arvit en est (Peninei Halakha, yeshiva.co/midrash/48403).
            add(when {
                nusah != "edot" -> "ajout_anenou_particulier_minha"
                C.TICHA_BEAV in c -> "ajout_anenou_particulier_trois"
                else -> "ajout_anenou_particulier_matin_minha"
            })
        }
        if (C.TICHA_BEAV in c) add("ajout_ticha_beav")
        if (C.YAALE_VEYAVO in c) add("ajout_yaale_veyavo")
        if (C.AL_HANISSIM in c) add("ajout_al_hanissim")
        SiddurJour.libelleHallel(nusah, c)?.let { add(it) }
        // Le Chabbat, il n'y a pas d'Amida de semaine où dire ou taire Ta'hanoun.
        if (C.CHABBAT !in c) add(if (C.TAHANOUN_CHAHARIT in c) "ajout_tahanoun" else "ajout_sans_tahanoun")
    }

    /**
     * Les horaires du jour, avec ceux de Chabbat et des fêtes, qui suivent
     * isAssurBemelacha de KosherJava :
     *  - la veille d'un jour chômé : l'allumage, [minutes] avant la chki'a ;
     *  - un jour chômé suivi d'un autre (deuxième jour de fête, Chabbat puis
     *    fête) : l'allumage, pas avant la sortie du jour, à tsét 8,5° ;
     *  - le dernier jour chômé : la sortie, à tsét 8,5° (et Rabbénou Tam reste
     *    dans la liste).
     * Le profil et ses limites : ZMANIM.md.
     */
    fun horaires(lieu: Zmanim.Lieu, date: LocalDate, enIsrael: Boolean, minutes: Int): List<Zmanim.Zman> {
        val aujourdhui = CalendrierJuif.jour(date, enIsrael).chome
        val demain = CalendrierJuif.jour(date.plusDays(1), enIsrael).chome
        val base = Zmanim.jour(lieu, date)
        val tset = base.first { it.cle == "tset" }
        val sortie = mutableListOf<Zmanim.Zman>()
        base.forEach { z ->
            if (z.cle == "chkia" && demain && !aujourdhui) sortie += Zmanim.allumage(lieu, date, minutes)
            sortie += z
            if (z.cle == "tset") {
                if (aujourdhui && !demain) sortie += tset.copy(cle = "sortie")
                if (aujourdhui && demain) sortie += tset.copy(cle = "allumage", methode = Zmanim.Methode("apres_tset", ""))
            }
        }
        return sortie
    }

    /**
     * Le texte du siddour que suggère l'instant [maintenant], d'après les
     * zmanim du [lieu] (demande du 10/10) : Tikoun 'hatsot de 'hatsot de la
     * nuit à l'aube ; Birkot hacha'har de l'aube à michéyakir ; Cha'harit
     * jusqu'à 'hatsot ; Min'ha de min'ha guedola à la chki'a ; Arvit dès tsét,
     * puis le Chema du coucher après 22 h 30 (repère d'usage, pas un zman).
     * null dans les deux creux, de 'hatsot à min'ha guedola et de la chki'a
     * à tsét : aucune de ces prières n'y a sa place à priori. Sans les
     * zmanim du jour (soleil de minuit), les heures fixes de SiddurMoment.
     */
    fun momentDuSiddour(lieu: Zmanim.Lieu, maintenant: java.time.Instant): String? {
        val zone = java.time.ZoneId.of(lieu.fuseau)
        val local = maintenant.atZone(zone)
        val date = local.toLocalDate()
        fun instants(d: LocalDate) = Zmanim.jour(lieu, d).associate { it.cle to it.instant }
        val z = instants(date)
        val hier = instants(date.minusDays(1))
        val cles = listOf("alot", "misheyakir", "chatsot", "minha_gedola", "chkia", "tset", "chatsot_layla")
        if (cles.any { z[it] == null } || hier["chatsot_layla"] == null) return SiddurMoment.suggestion(local.toLocalTime())
        fun avant(cle: String) = maintenant < z.getValue(cle)!!
        val soir = if (local.toLocalTime() < java.time.LocalTime.of(22, 30) && local.toLocalTime() > java.time.LocalTime.NOON) "arvit" else "chema_coucher"
        return when {
            avant("alot") -> if (maintenant >= hier.getValue("chatsot_layla")!!) "tikoun_hatsot" else "chema_coucher"
            avant("misheyakir") -> "birkot_hachahar"
            avant("chatsot") -> "chaharit"
            avant("minha_gedola") -> null
            avant("chkia") -> "minha"
            avant("tset") -> null
            avant("chatsot_layla") -> soir
            else -> "tikoun_hatsot"
        }
    }

    /**
     * Les semaines d'un mois civil, du dimanche au samedi comme le calendrier
     * juif ; null hors du mois.
     */
    fun semaines(mois: YearMonth): List<List<LocalDate?>> = grille((1..mois.lengthOfMonth()).map { mois.atDay(it) })

    /** Le premier jour (1er) du mois hébreu qui contient [date]. Le mois ne dépend pas du lieu. */
    fun debutMoisHebreu(date: LocalDate): LocalDate =
        date.minusDays((CalendrierJuif.jour(date, false).jourHebreu - 1).toLong())

    /** Les 29 ou 30 jours du mois hébreu qui contient [date]. */
    fun joursMoisHebreu(date: LocalDate): List<LocalDate> {
        val debut = debutMoisHebreu(date)
        val mois = CalendrierJuif.jour(debut, false).moisHebreu
        return generateSequence(debut) { it.plusDays(1) }
            .takeWhile { CalendrierJuif.jour(it, false).moisHebreu == mois }.toList()
    }

    /** Les semaines du mois hébreu qui contient [date], du dimanche au samedi ; null hors du mois. */
    fun semainesHebreu(date: LocalDate): List<List<LocalDate?>> = grille(joursMoisHebreu(date))

    /** Le 1er du mois hébreu suivant ([sens] = 1) ou précédent ([sens] = -1). */
    fun moisHebreuVoisin(date: LocalDate, sens: Int): LocalDate =
        if (sens > 0) joursMoisHebreu(date).last().plusDays(1)
        else debutMoisHebreu(debutMoisHebreu(date).minusDays(1))

    private fun grille(jours: List<LocalDate>): List<List<LocalDate?>> {
        val decalage = jours.first().dayOfWeek.value % 7 // dimanche = 0
        val cases = List(decalage) { null } + jours
        return cases.chunked(7).map { it + List(7 - it.size) { null } }
    }
}
