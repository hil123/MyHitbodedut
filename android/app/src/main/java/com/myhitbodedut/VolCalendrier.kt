package com.myhitbodedut

import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Chabbat, les fêtes et le compte du Omer en vol (28/10) : la règle de
 * JourAffiche.horaires (isAssurBemelacha de KosherJava), appliquée aux
 * chki'a et aux tsét de la route au lieu de celles d'un lieu fixe.
 *
 *  - chki'a la veille d'un jour chômé : entrée (et l'heure d'allumage,
 *    [minutes] avant cette chki'a) ;
 *  - tsét d'un jour chômé suivi d'un jour ouvré : sortie ;
 *  - tsét d'un jour chômé suivi d'un autre : le jour suivant commence ;
 *  - tsét pendant le Omer : le jour qui se compte ce soir.
 *
 * Le jour civil est celui du point sous l'avion (fuseau nautique, comme
 * Vol) ; Israël ou diaspora, le réglage de l'application. Ce sont deux choix
 * de calcul, pas des règles : ZMANIM.md, « En vol ». Aucun conseil n'est
 * donné (allumer à bord, compter avant ou après une escale…).
 * Pas d'Android ici : testable sur la machine virtuelle seule.
 */
object VolCalendrier {

    enum class Type { ENTREE, SORTIE, ENCHAINEMENT, OMER }

    data class Jalon(
        val type: Type,
        val instant: Instant,
        /** Le jour qui commence (entrée, enchaînement, Omer) ou qui finit (sortie), au point sous l'avion. */
        val date: LocalDate,
        val chabbat: Boolean,
        /** Les clés « fete_<clé> » de ce jour. */
        val fetes: List<String>,
        val omer: Int = 0,
        /** L'allumage, [minutes] avant la chki'a : à l'entrée seulement. */
        val allumage: Instant? = null,
        val latitude: Double,
        val longitude: Double,
    )

    fun jalons(calcul: Vol.Calcul, enIsrael: Boolean, minutes: Int): List<Jalon> = calcul.enVol.flatMap { e ->
        if (e.cle != "chkia" && e.cle != "tset") return@flatMap emptyList()
        val jour = LocalDate.ofInstant(e.instant, ZoneId.of(Vol.fuseauNautique(e.longitude)))
        val aujourdhui = CalendrierJuif.jour(jour, enIsrael)
        val demain = CalendrierJuif.jour(jour.plusDays(1), enIsrael)
        fun jalon(type: Type, j: CalendrierJuif.Jour, allumage: Instant? = null, omer: Int = 0) =
            Jalon(type, e.instant, j.date, j.date.dayOfWeek == DayOfWeek.SATURDAY, j.fetes, omer, allumage, e.latitude, e.longitude)
        buildList {
            if (e.cle == "chkia" && demain.chome && !aujourdhui.chome) add(jalon(Type.ENTREE, demain, e.instant - Duration.ofMinutes(minutes.toLong())))
            if (e.cle == "tset") {
                if (aujourdhui.chome && !demain.chome) add(jalon(Type.SORTIE, aujourdhui))
                if (aujourdhui.chome && demain.chome) add(jalon(Type.ENCHAINEMENT, demain))
                if (demain.omer > 0) add(jalon(Type.OMER, demain, omer = demain.omer))
            }
        }
    }
}
