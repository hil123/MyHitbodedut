package com.myhitbodedut

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.widget.RemoteViews

/**
 * Le widget boussole 2 × 2 (28/10, design C). Un widget ne lit pas la boussole
 * du téléphone : il montre la direction principale de Jérusalem par rapport au
 * nord, en haut, pour le lieu des zmanim (BoussoleDessin.rose), et ouvre la
 * boussole vivante au toucher. Redessiné quand le lieu ou le réglage change
 * (ZmanimWidget.refresh, BoussoleActivity).
 */
class BoussoleWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val c = LangueApp.envelopper(context)
        ids.forEach { manager.updateAppWidget(it, vues(c)) }
    }

    companion object {
        /** Le côté de l'image, en pixels : net sur une case de 2 × 2 sans peser sur le lanceur. */
        private const val COTE = 420

        fun image(context: Context): Bitmap {
            val b = Bitmap.createBitmap(COTE, COTE, Bitmap.Config.ARGB_8888)
            val lieu = Store.lieuZmanim(context)
            val principale = lieu?.let { DirectionJerusalem.directions(it.latitude, it.longitude, Store.boussoleCarte(context)).first }
            val hassid = runCatching { BitmapFactory.decodeResource(context.resources, R.drawable.char_standing_back) }.getOrNull()
            BoussoleDessin.rose(Canvas(b), COTE, COTE, principale, principale?.let { "${DirectionJerusalem.degres(it)}°" } ?: "",
                context.getString(R.string.boussole_nord), hassid)
            return b
        }

        fun vues(context: Context): RemoteViews {
            val v = RemoteViews(context.packageName, R.layout.widget_boussole)
            v.setImageViewBitmap(R.id.wb_image, image(context))
            val lieu = Store.lieuZmanim(context)
            val principale = lieu?.let { DirectionJerusalem.directions(it.latitude, it.longitude, Store.boussoleCarte(context)).first }
            v.setContentDescription(R.id.wb_image, if (principale == null) context.getString(R.string.zmanim_choisir_lieu)
                else context.getString(R.string.boussole_widget_lecture, lieu.nom, DirectionJerusalem.degres(principale)))
            v.setOnClickPendingIntent(R.id.wb_image, PendingIntent.getActivity(context, 170,
                Intent(context, BoussoleActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
            return v
        }

        fun refresh(context: Context) {
            val manager = AppWidgetManager.getInstance(context) ?: return
            val ids = manager.getAppWidgetIds(ComponentName(context, BoussoleWidget::class.java))
            if (ids.isEmpty()) return
            val c = LangueApp.envelopper(context)
            ids.forEach { manager.updateAppWidget(it, vues(c)) }
        }
    }
}
