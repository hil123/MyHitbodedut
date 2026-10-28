package com.myhitbodedut

import java.time.Duration
import java.time.Instant

/**
 * Les rappels pendant un vol (28/10) : Tefilat haDerekh cinq minutes après le
 * décollage prévu (délai choisi par l'utilisateur ; les sources la placent après
 * le décollage, ZMANIM.md « En vol »), le début de chaque prière dans l'avion et
 * l'approche de sa limite, le compte du Omer du soir.
 *
 * Les bornes des prières sont celles de l'application (HorairePriere,
 * ZmanimWidget.prochaine) : Min'ha de min'ha guedola à la chki'a, Arvit de la
 * sortie des étoiles au milieu de la nuit, Cha'harit jusqu'à la fin du temps de
 * la tefila (GRA). Seul le début de Cha'harit n'y était pas : le lever du soleil
 * (netz), le temps de la prière au départ (Choul'han Aroukh O.H. 89, 1).
 * Le Omer : les jalons de VolCalendrier. Pas d'Android ici.
 */
object VolRappels {

    enum class Type { TEFILAT_HADEREKH, PRIERE, PRIERE_FIN, OMER }

    data class Rappel(
        val type: Type,
        val instant: Instant,
        /** « chaharit », « minha », « arvit » : la prière qui commence ou finit ; au décollage, celle du moment. */
        val priere: String? = null,
        val limite: Instant? = null,
        val omer: Int = 0,
    )

    val APRES_DECOLLAGE: Duration = Duration.ofMinutes(5)
    val AVANT_LIMITE: Duration = Duration.ofMinutes(30)

    /** Chaque prière : le zman qui l'ouvre, celui qui la ferme. */
    val BORNES = listOf(
        Triple("chaharit", "netz", "szt_gra"),
        Triple("minha", "minha_gedola", "chkia"),
        Triple("arvit", "tset", "chatsot_layla"),
    )

    /** La prière ouverte à [t] et sa limite, aux bornes ci-dessus ; null entre deux prières. */
    fun ouverte(calcul: Vol.Calcul, t: Instant): Pair<String, Instant>? {
        val ev = calcul.evenements
        for ((priere, debut, fin) in BORNES) {
            val d = ev.lastOrNull { it.cle == debut && !it.instant.isAfter(t) } ?: continue
            val f = ev.firstOrNull { it.cle == fin && it.instant.isAfter(d.instant) } ?: continue
            if (t.isBefore(f.instant)) return priere to f.instant
        }
        return null
    }

    fun rappels(calcul: Vol.Calcul, enIsrael: Boolean): List<Rappel> {
        val plan = calcul.plan
        val dansLAvion = { t: Instant -> !t.isBefore(plan.decollage) && !t.isAfter(plan.atterrissage) }
        val res = mutableListOf<Rappel>()
        val t0 = plan.decollage + APRES_DECOLLAGE
        if (dansLAvion(t0)) {
            val o = ouverte(calcul, t0)
            res += Rappel(Type.TEFILAT_HADEREKH, t0, o?.first, o?.second)
        }
        for ((priere, debut, fin) in BORNES) {
            calcul.evenements.filter { it.cle == debut && dansLAvion(it.instant) && it.instant.isAfter(t0) }.forEach { d ->
                val limite = calcul.evenements.firstOrNull { it.cle == fin && it.instant.isAfter(d.instant) }?.instant
                res += Rappel(Type.PRIERE, d.instant, priere, limite)
            }
            // L'approche de la limite (sauf Arvit, qui se dit encore après le milieu de la nuit) :
            // seulement si la prière est déjà ouverte à ce moment-là.
            if (priere == "arvit") continue
            calcul.evenements.filter { it.cle == fin }.forEach { f ->
                val t = f.instant - AVANT_LIMITE
                if (dansLAvion(t) && t.isAfter(t0) && ouverte(calcul, t)?.first == priere) res += Rappel(Type.PRIERE_FIN, t, priere, f.instant)
            }
        }
        VolCalendrier.jalons(calcul, enIsrael, 0).filter { it.type == VolCalendrier.Type.OMER }
            .forEach { res += Rappel(Type.OMER, it.instant, omer = it.omer) }
        return res.sortedWith(compareBy({ it.instant }, { it.type.ordinal }))
    }
}
