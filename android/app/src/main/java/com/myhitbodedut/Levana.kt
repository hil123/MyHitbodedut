package com.myhitbodedut

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.view.View
import com.kosherjava.zmanim.hebrewcalendar.JewishCalendar
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * Birkat haLevana (13/10, comme CalJ) : la fenêtre du mois, de sept jours après
 * le molad jusqu'au milieu entre deux molad (Choulhan Aroukh, Ora'h 'Haïm 426,
 * KosherJava : getTchilasZmanKidushLevana7Days, getSofZmanKidushLevanaBetweenMoldos).
 * On la bénit la nuit : le premier soir est celui du début, le dernier celui de
 * la nuit où la fenêtre se ferme.
 */
object Levana {

    /** Un mois lunaire moyen : 29 j 12 h 793 halakim. */
    private val MOIS: Duration = Duration.ofDays(29).plusHours(12).plusMinutes(44).plusMillis(3333)

    data class Fenetre(val molad: Instant, val debut: Instant, val fin: Instant) {
        val prochainMolad: Instant get() = molad.plus(MOIS)
        fun premierSoir(zone: ZoneId): LocalDate = debut.atZone(zone).toLocalDate()
        fun dernierSoir(zone: ZoneId): LocalDate = fin.minus(Duration.ofHours(12)).atZone(zone).toLocalDate()
    }

    sealed interface Etat {
        data class Dans(val jours: Int) : Etat
        data object CeSoir : Etat
        data object DernierSoir : Etat
    }

    private fun du(date: LocalDate): Fenetre {
        val c = JewishCalendar(date)
        return Fenetre(c.moladAsDate.toInstant(), c.tchilasZmanKidushLevana7Days.toInstant(), c.sofZmanKidushLevanaBetweenMoldos.toInstant())
    }

    /** La fenêtre qui concerne le soir de [date] : celle du mois, ou celle du mois suivant si elle est passée. */
    fun fenetre(date: LocalDate, zone: ZoneId): Fenetre {
        val ici = du(date)
        if (!date.isAfter(ici.dernierSoir(zone))) return ici
        val jour = JewishCalendar(date).jewishDayOfMonth
        return du(date.plusDays((31 - jour + 2).toLong()))
    }

    fun etat(f: Fenetre, date: LocalDate, zone: ZoneId): Etat {
        val premier = f.premierSoir(zone)
        val dernier = f.dernierSoir(zone)
        return when {
            date.isBefore(premier) -> Etat.Dans(java.time.temporal.ChronoUnit.DAYS.between(date, premier).toInt())
            date == dernier -> Etat.DernierSoir
            else -> Etat.CeSoir
        }
    }

    /** La place d'un instant dans le mois lunaire, de 0 (molad) à 1 (molad suivant). */
    fun part(f: Fenetre, t: Instant): Float =
        (Duration.between(f.molad, t).toMillis().toDouble() / MOIS.toMillis()).coerceIn(0.0, 1.0).toFloat()

    /** Le soir de [date] (20 h) : le point posé sur la jauge. */
    fun soir(date: LocalDate, zone: ZoneId): Instant = date.atTime(LocalTime.of(20, 0)).atZone(zone).toInstant()

    /** La jauge du mois lunaire : un fil, la fenêtre en or, le soir du jour en point. */
    class Jauge(c: Context, private val debut: Float, private val fin: Float, private val point: Float) : View(c) {
        private val d = c.resources.displayMetrics.density
        private val fil = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Apparence.couleur(c, R.color.line) }
        private val or = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Apparence.couleur(c, R.color.ring_streak) }
        private val pt = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Apparence.couleur(c, R.color.brand) }
        private val bord = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Apparence.couleur(c, R.color.card) }

        override fun onMeasure(w: Int, h: Int) = setMeasuredDimension(MeasureSpec.getSize(w), (18 * d).toInt())

        override fun onDraw(canvas: Canvas) {
            val r = 7 * d
            val y = height / 2f
            val g = r
            val dr = width - r
            // Le mois va de droite à gauche en hébreu, comme le texte.
            val rtl = layoutDirection == LAYOUT_DIRECTION_RTL
            fun x(p: Float) = if (rtl) dr - p * (dr - g) else g + p * (dr - g)
            canvas.drawRoundRect(RectF(g, y - 1.5f * d, dr, y + 1.5f * d), 2 * d, 2 * d, fil)
            val a = x(debut); val b = x(fin)
            canvas.drawRoundRect(RectF(minOf(a, b), y - 3 * d, maxOf(a, b), y + 3 * d), 3 * d, 3 * d, or)
            canvas.drawCircle(x(point), y, r, bord)
            canvas.drawCircle(x(point), y, r - 2 * d, pt)
        }
    }
}
