package com.myhitbodedut

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.RectF
import android.view.View
import androidx.core.content.ContextCompat

/** Les trois anneaux en image, pour la grande icône des notifications. */
object AnneauxImage {

    fun notification(context: Context): Bitmap {
        val cote = (64 * context.resources.displayMetrics.density).toInt().coerceIn(96, 256)
        val image = Bitmap.createBitmap(cote, cote, Bitmap.Config.ARGB_8888)
        val geometrie = AnneauxDessin.Geometrie(epaisseur = cote * 0.09f, ecart = cote * 0.035f)
        AnneauxDessin.dessiner(
            Canvas(image), RectF(0f, 0f, cote.toFloat(), cote.toFloat()), Store.anneaux(context),
            AnneauxDessin.Couleurs(
                jour = ContextCompat.getColor(context, R.color.brand),
                semaine = ContextCompat.getColor(context, R.color.ring_week),
                serie = ContextCompat.getColor(context, R.color.ring_streak),
                piste = ContextCompat.getColor(context, R.color.day_todo)
            ),
            geometrie,
            rtl = context.resources.configuration.layoutDirection == View.LAYOUT_DIRECTION_RTL
        )
        return image
    }
}
