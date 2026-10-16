package com.myhitbodedut

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/**
 * La vignette des zmanim (14/10) : la date hébraïque, la prière du moment et
 * l'heure où elle se ferme, la chki'a, la série. Sans lieu, elle invite à en
 * choisir un. Mise à jour toutes les demi-heures et à chaque ouverture.
 */
class ZmanimWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { manager.updateAppWidget(it, vues(LangueApp.envelopper(context))) }
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

        fun refresh(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, ZmanimWidget::class.java))
            if (ids.isNotEmpty()) ids.forEach { manager.updateAppWidget(it, vues(LangueApp.envelopper(context))) }
        }

        private fun vues(context: Context): RemoteViews {
            val v = RemoteViews(context.packageName, R.layout.widget_zmanim)
            val ouvrir = PendingIntent.getActivity(context, 160, Intent(context, ZmanimActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            v.setOnClickPendingIntent(R.id.wz_corps, ouvrir)
            val israel = Store.inIsrael(context)
            v.setTextViewText(R.id.wz_date, CalendrierJuif.jour(LocalDate.now(), israel).dateHebraique)
            v.setTextViewText(R.id.wz_serie, context.resources.getQuantityString(R.plurals.widget_zmanim_serie, Store.streak(context), Store.streak(context)))
            val lieu = Store.lieuZmanim(context)
            if (lieu == null) {
                v.setTextViewText(R.id.wz_priere, context.getString(R.string.zmanim_choisir_lieu))
                v.setTextViewText(R.id.wz_chkia, "")
                return v
            }
            val zone = runCatching { ZoneId.of(lieu.fuseau) }.getOrDefault(ZoneId.systemDefault())
            val maintenant = ZonedDateTime.now(zone)
            val horaires = Zmanim.jour(lieu, maintenant.toLocalDate()).associate { it.cle to it.local }
            val heure = DateTimeFormatter.ofPattern("HH:mm")
            val (priere, limite) = prochaine(horaires, maintenant)
            val nom = context.getString(when (priere) { "chaharit" -> R.string.widget_zmanim_chaharit; "minha" -> R.string.widget_zmanim_minha; else -> R.string.widget_zmanim_arvit })
            v.setTextViewText(R.id.wz_priere, if (limite != null) context.getString(R.string.widget_zmanim_jusqua, nom, limite.format(heure)) else nom)
            v.setTextViewText(R.id.wz_chkia, horaires["chkia"]?.let { context.getString(R.string.widget_zmanim_chkia, it.format(heure)) } ?: "")
            return v
        }
    }
}
