package com.myhitbodedut

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

/**
 * La série sur l'écran d'accueil, et de quoi commencer d'un geste.
 *
 * Ouvrir l'application pour savoir où l'on en est est déjà un détour, et
 * un détour de plus suffit à ne pas s'y mettre. La vignette dit la série
 * et l'état du jour sans rien ouvrir, et son bouton lance la séance
 * directement — le même chemin que le bouton « Commencer » d'un rappel.
 */
class StreakWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        manager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        appWidgetIds.forEach { draw(context, manager, it) }
    }

    companion object {

        /**
         * Redessine toutes les vignettes posées. Appelé quand la journée
         * change d'état : une série qui vient d'être allongée doit se voir
         * tout de suite, pas à la prochaine mise à jour du système.
         */
        fun refresh(context: Context) {
            runCatching {
                val manager = AppWidgetManager.getInstance(context) ?: return
                val ids = manager.getAppWidgetIds(
                    ComponentName(context.applicationContext, StreakWidget::class.java)
                )
                ids.forEach { draw(context, manager, it) }
            }
        }

        private fun draw(context: Context, manager: AppWidgetManager, id: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_streak)
            val streak = Store.streak(context)
            val running = Store.isSessionRunning(context)

            views.setTextViewText(R.id.widget_streak, streak.toString())
            views.setTextViewText(
                R.id.widget_streak_label,
                if (streak == 0) context.getString(R.string.status_streak_none)
                else context.resources.getQuantityString(R.plurals.status_streak, streak, streak)
            )
            views.setTextViewText(
                R.id.widget_today,
                context.getString(
                    when {
                        running -> R.string.widget_running
                        Store.isDone(context) -> R.string.widget_done
                        else -> R.string.widget_todo
                    }
                )
            )
            views.setTextViewText(
                R.id.widget_start,
                context.getString(if (running) R.string.widget_resume else R.string.widget_start)
            )

            // Le bouton part droit sur la séance et la lance ; une séance
            // déjà en cours n'est pas relancée, l'écran s'ouvre simplement.
            views.setOnClickPendingIntent(
                R.id.widget_start,
                activity(
                    context,
                    1,
                    Intent(context, SessionActivity::class.java)
                        .putExtra(SessionActivity.EXTRA_START_NOW, true)
                )
            )
            // Le reste de la vignette ouvre l'application, sans rien décider.
            views.setOnClickPendingIntent(
                R.id.widget_body,
                activity(context, 2, Intent(context, MainActivity::class.java))
            )

            manager.updateAppWidget(id, views)
        }

        private fun activity(context: Context, code: Int, intent: Intent): PendingIntent =
            PendingIntent.getActivity(
                context,
                code,
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
    }
}
