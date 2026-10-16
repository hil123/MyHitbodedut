package com.myhitbodedut

import android.content.Context
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/**
 * L'horaire du jour d'une prière, pour sa bannière (16/10) : aux bornes du widget
 * (ZmanimWidget.prochaine) — Cha'harit jusqu'à la fin du temps de la tefila (GRA), Min'ha de
 * min'ha guedola au coucher du soleil, Arvit dès la sortie des étoiles. Null sans lieu choisi.
 */
object HorairePriere {
    fun texte(c: Context, id: String): String? {
        if (id !in setOf("chaharit", "minha", "arvit")) return null
        val lieu = Store.lieuZmanim(c) ?: return null
        val zone = runCatching { ZoneId.of(lieu.fuseau) }.getOrDefault(ZoneId.systemDefault())
        val z = Zmanim.jour(lieu, ZonedDateTime.now(zone).toLocalDate()).associate { it.cle to it.local }
        val f = DateTimeFormatter.ofPattern("HH:mm")
        val h = { cle: String -> z[cle]?.format(f) }
        return when (id) {
            "chaharit" -> h("szt_gra")?.let { c.getString(R.string.banniere_chaharit, it) }
            "minha" -> { val a = h("minha_gedola"); val b = h("chkia"); if (a != null && b != null) c.getString(R.string.banniere_minha, a, b) else null }
            else -> h("tset")?.let { c.getString(R.string.banniere_arvit, it) }
        }
    }
}
