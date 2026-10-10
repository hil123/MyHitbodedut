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
     * Les ajouts du jour, dans l'ordre de l'Amida : des clés de libellé
     * (Libelles). Le nusah change trois choses (ZMANIM.md, « Différences de
     * nusah ») : Morid hatal hors d'Israël chez beaucoup d'Ashkénazes, la
     * bénédiction du Hallel partiel, et l'Anénou du particulier (Ashkenaz et
     * Sfard : à Min'ha seulement, Rema OC 565:3 ; Edot : à Cha'harit et à Min'ha,
     * et à Arvit le soir de Tich'a bèAv).
     */
    fun ajouts(c0: Set<String>, nusah: String = "edot", enIsrael: Boolean = false): List<String> = buildList {
        val c = SiddurJour.conditionsPour(nusah, enIsrael, c0)
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
     * Les semaines d'un mois civil, du dimanche au samedi comme le calendrier
     * juif ; null hors du mois.
     */
    fun semaines(mois: YearMonth): List<List<LocalDate?>> {
        val premier = mois.atDay(1)
        val decalage = premier.dayOfWeek.value % 7 // dimanche = 0
        val cases = List(decalage) { null } + (1..mois.lengthOfMonth()).map { mois.atDay(it) }
        return cases.chunked(7).map { it + List(7 - it.size) { null } }
    }
}
