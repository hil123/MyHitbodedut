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
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.os.Build
import android.os.Bundle
import android.util.SizeF
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * La série sur l'écran d'accueil, et de quoi commencer d'un geste.
 *
 * Ouvrir l'application pour savoir où l'on en est est déjà un détour, et
 * un détour de plus suffit à ne pas s'y mettre. La vignette dit la série
 * et l'état du jour sans rien ouvrir, et son bouton lance la séance
 * directement — le même chemin que le bouton « Commencer » d'un rappel.
 *
 * Elle suit la place qu'on lui donne (demande du 28/09, WidgetTaille) :
 * une case, le compte et une coche ; la taille habituelle, la jauge de la
 * semaine avec le hassid, et le bouton ; une grande place, la semaine et
 * un texte de ’hizouk du jour en plus.
 */
class StreakWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        manager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val traduit = LangueApp.envelopper(context)
        appWidgetIds.forEach { draw(traduit, manager, it) }
    }

    /** On a agrandi ou réduit la vignette : avant Android 12, c'est à nous de changer de version. */
    override fun onAppWidgetOptionsChanged(
        context: Context,
        manager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        draw(LangueApp.envelopper(context), manager, appWidgetId)
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
                val traduit = LangueApp.envelopper(context)
                ids.forEach { draw(traduit, manager, it) }
            }
        }

        private fun draw(context: Context, manager: AppWidgetManager, id: Int) {
            val etat = Etat.lire(context)
            val vues = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // Le lanceur choisit lui-même la version qui tient dans la place.
                RemoteViews(WidgetTaille.entries.associate { taille ->
                    SizeF(taille.largeurMin, taille.hauteurMin) to vues(context, taille, etat)
                })
            } else {
                val options = manager.getAppWidgetOptions(id)
                val largeur = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 180)
                val hauteur = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 110)
                vues(context, WidgetTaille.pour(largeur.toFloat(), hauteur.toFloat()), etat)
            }
            manager.updateAppWidget(id, vues)
        }

        /** Ce que la vignette montre, lu une fois pour les trois versions. */
        private class Etat(
            val streak: Int,
            val running: Boolean,
            val done: Boolean,
            val semaine: List<WeekDay>,
            val fraction: Float
        ) {
            companion object {
                fun lire(context: Context): Etat {
                    val semaine = Store.currentWeek(context)
                    return Etat(
                        streak = Store.streak(context),
                        running = Store.isSessionRunning(context),
                        done = Store.isDone(context),
                        semaine = semaine,
                        fraction = JaugeSemaine.progression(semaine).fraction
                    )
                }
            }
        }

        private fun vues(context: Context, taille: WidgetTaille, etat: Etat): RemoteViews {
            val gabarit = when (taille) {
                WidgetTaille.PETITE -> R.layout.widget_small
                WidgetTaille.MOYENNE -> R.layout.widget_streak
                WidgetTaille.GRANDE -> R.layout.widget_large
            }
            val views = RemoteViews(context.packageName, gabarit)
            views.setTextViewText(R.id.widget_streak, etat.streak.toString())
            // Le reste de la vignette ouvre l'application, sans rien décider.
            views.setOnClickPendingIntent(
                R.id.widget_body,
                activity(context, 2, Intent(context, MainActivity::class.java))
            )

            if (taille == WidgetTaille.PETITE) {
                views.setTextViewText(R.id.widget_mark, if (etat.done) "✓" else "○")
                return views
            }

            views.setImageViewBitmap(
                R.id.widget_gauge,
                jauge(context, etat.fraction, Character.greeting(etat.done))
            )
            views.setTextViewText(
                R.id.widget_streak_label,
                if (etat.streak == 0) context.getString(R.string.status_streak_none)
                else context.resources.getQuantityString(R.plurals.status_streak, etat.streak, etat.streak)
            )
            views.setTextViewText(
                R.id.widget_today,
                context.getString(
                    when {
                        etat.running -> R.string.widget_running
                        etat.done -> R.string.widget_done
                        else -> R.string.widget_todo
                    }
                )
            )
            views.setTextViewText(
                R.id.widget_start,
                context.getString(if (etat.running) R.string.widget_resume else R.string.widget_start)
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

            if (taille == WidgetTaille.GRANDE) {
                semaine(context, views, etat.semaine)
                texteDuJour(context, views)
            }
            return views
        }

        private val JOURS = intArrayOf(
            R.id.widget_day_0, R.id.widget_day_1, R.id.widget_day_2, R.id.widget_day_3,
            R.id.widget_day_4, R.id.widget_day_5, R.id.widget_day_6
        )

        /** Les sept jours, comme sur l'accueil : fait, Chabbat, pause, à venir. */
        private fun semaine(context: Context, views: RemoteViews, jours: List<WeekDay>) {
            val format = DateTimeFormatter.ofPattern("EEEEE", context.resources.configuration.locales[0])
            val clair = ContextCompat.getColor(context, R.color.mint)
            val fonce = ContextCompat.getColor(context, R.color.brand_dark)
            jours.take(7).forEachIndexed { i, jour ->
                val marque = when {
                    jour.done -> "✓"
                    jour.shabbat && jour.exempt -> context.getString(R.string.week_shabbat_mark)
                    jour.exempt -> "—"
                    jour.future -> " "
                    else -> "·"
                }
                views.setTextViewText(JOURS[i], jour.date.format(format) + "\n" + marque)
                views.setTextColor(JOURS[i], if (jour.done) fonce else clair)
                views.setInt(
                    JOURS[i], "setBackgroundResource",
                    if (jour.done) R.drawable.bg_widget_day_done else R.drawable.bg_widget_day
                )
            }
        }

        /**
         * Un texte de ’hizouk par jour, parmi ceux de l'ouverture de séance.
         * Le même toute la journée : la vignette se redessine souvent, et un
         * texte qui changerait à chaque fois ne se lirait jamais jusqu'au bout.
         */
        private fun texteDuJour(context: Context, views: RemoteViews) {
            val chizuk = runCatching { Data.chizuk(context) }.getOrNull() ?: return
            val textes = ChizukRules.forMoment(chizuk, Moment.OUVERTURE, BuildConfig.DEBUG)
            if (textes.isEmpty()) return
            val item = textes[(LocalDate.now().toEpochDay() % textes.size).toInt()]
            views.setTextViewText(R.id.widget_text, ChizukRules.hebrew(chizuk, item))
            views.setTextViewText(R.id.widget_ref, Reference.localise(context, item.ref))
        }

        /**
         * La jauge de la semaine, le hassid en son creux. Dessinée ici en
         * image : une vignette n'accepte pas de vue sur mesure.
         */
        private fun jauge(context: Context, fraction: Float, personnage: Int): Bitmap {
            val densite = context.resources.displayMetrics.density
            val cote = (76 * densite).toInt().coerceIn(96, 320)
            val image = Bitmap.createBitmap(cote, cote, Bitmap.Config.ARGB_8888)
            val toile = Canvas(image)
            val epaisseur = cote * 0.085f
            val cadre = RectF(epaisseur / 2, epaisseur / 2, cote - epaisseur / 2, cote - epaisseur / 2)

            val trait = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = epaisseur
            }
            trait.color = Color.argb(60, 255, 255, 255)
            toile.drawOval(cadre, trait)
            if (fraction > 0f) {
                trait.color = ContextCompat.getColor(context, R.color.mint)
                trait.strokeCap = Paint.Cap.ROUND
                toile.drawArc(cadre, -90f, 360f * fraction.coerceAtMost(1f), false, trait)
            }

            // La plaque claire, puis le hassid, coupé au bord du cercle.
            val marge = epaisseur * 1.7f
            val plaque = RectF(marge, marge, cote - marge, cote - marge)
            val fond = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = ContextCompat.getColor(context, R.color.stage_soft)
            }
            toile.drawOval(plaque, fond)
            dessin(context, personnage, plaque.height().toInt())?.let { perso ->
                toile.save()
                toile.clipPath(Path().apply { addOval(plaque, Path.Direction.CW) })
                val hauteur = plaque.height() * 0.9f
                val largeur = hauteur * perso.width / perso.height
                val gauche = plaque.centerX() - largeur / 2
                val cible = RectF(gauche, plaque.bottom - hauteur, gauche + largeur, plaque.bottom)
                toile.drawBitmap(perso, null, cible, Paint(Paint.FILTER_BITMAP_FLAG))
                toile.restore()
            }
            return image
        }

        /** L'illustration, réduite à la lecture : les originaux font 900 px de haut. */
        private fun dessin(context: Context, res: Int, hauteurVoulue: Int): Bitmap? = runCatching {
            val bornes = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeResource(context.resources, res, bornes)
            var echelle = 1
            while (bornes.outHeight / (echelle * 2) >= hauteurVoulue) echelle *= 2
            BitmapFactory.decodeResource(
                context.resources, res, BitmapFactory.Options().apply { inSampleSize = echelle }
            )
        }.getOrNull()

        private fun activity(context: Context, code: Int, intent: Intent): PendingIntent =
            PendingIntent.getActivity(
                context,
                code,
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
    }
}
