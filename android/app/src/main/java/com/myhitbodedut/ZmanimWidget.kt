package com.myhitbodedut

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import java.time.ZonedDateTime

/**
 * La vignette moyenne des zmanim (14/10, refaite le 28/10 avec une petite et
 * une grande : ZmanimVignette). Mise à jour toutes les demi-heures et à
 * chaque ouverture.
 */
class ZmanimWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { manager.updateAppWidget(it, ZmanimVignette.vues(LangueApp.envelopper(context), ZmanimVignette.Taille.MOYENNE)) }
    }

    companion object {
        /** La prière du moment et sa limite : Cha'harit jusqu'au sof zman tefila (GRA), Min'ha jusqu'à la chki'a, Arvit jusqu'à minuit. */
        fun prochaine(horaires: Map<String, ZonedDateTime?>, maintenant: ZonedDateTime): Pair<String, ZonedDateTime?> {
            // Après minuit et avant l'aube, c'est encore la nuit d'Arvit (audit du 16/10) : jusqu'au
            // milieu de la nuit d'hier s'il n'est pas passé, sans limite affichée ensuite.
            val alot = horaires["alot"]
            if (alot != null && maintenant.isBefore(alot)) {
                return "arvit" to horaires["chatsot_layla"]?.minusDays(1)?.takeIf { maintenant.isBefore(it) }
            }
            val limites = listOf("chaharit" to horaires["szt_gra"], "minha" to horaires["chkia"], "arvit" to horaires["chatsot_layla"])
            for ((priere, limite) in limites) if (limite != null && maintenant.isBefore(limite)) return priere to limite
            return "arvit" to null
        }

        /** Les trois tailles (28/10) : ZmanimVignette ; et la boussole, qui suit le même lieu. */
        fun refresh(context: Context) {
            ZmanimVignette.rafraichir(context)
            BoussoleWidget.refresh(context)
        }
    }
}
