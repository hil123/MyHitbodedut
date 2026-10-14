package com.myhitbodedut

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.view.View
import com.kosherjava.zmanim.hebrewcalendar.JewishCalendar
import com.kosherjava.zmanim.hebrewcalendar.JewishDate
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * Birkat haLevana (13/10 ; revue le 14/10 sur le texte, export Sefaria) :
 *  - début : pas avant sept jours (Choulhan Aroukh O.H. 426:4) ; « la plupart des
 *    A'haronim » permettent après trois jours d'heure à heure (Michna Beroura 426:20) ;
 *    l'Ari attend sept jours (Chabad.org, Ohr Somayach) ;
 *  - fin : jusqu'au seizième jour, non compris (426:3), soit quinze jours depuis
 *    l'heure du molad (MB 426:17) ; le Rema : la mi-mois, 14 j 18 h 22 min (MB 426:19) ;
 *  - pas avant Kippour ni avant Tich'a beAv (Rema 426:2) ; à leur sortie, oui
 *    (Rema ; MB 426:11 pour Tich'a beAv) ;
 *  - motsaé Chabbat de préférence, si cette nuit tombe avant le 10 du mois (Rema 426:2,
 *    MB 426:6), et pas un motsaé Chabbat qui est Yom Tov.
 * Les heures viennent de KosherJava (getTchilasZmanKidushLevana3Days/7Days,
 * getSofZmanKidushLevana15Days/BetweenMoldos). Le premier soir est celui du début ;
 * le dernier, celui de la nuit où la fenêtre se ferme. Même calcul : levana() du web.
 */
object Levana {

    /** Un mois lunaire moyen : 29 j 12 h 793 halakim. */
    private val MOIS: Duration = Duration.ofDays(29).plusHours(12).plusMinutes(44).plusMillis(3333)

    /** Les trois usages : début (jours depuis le molad), fin (quinze jours ou mi-mois). */
    enum class Usage(val cle: String, val libelle: Int, val troisJours: Boolean, val quinzeJours: Boolean) {
        TROIS_MI_MOIS("trois_mi_mois", R.string.levana_usage_trois_mi_mois, true, false),
        SEPT_MI_MOIS("sept_mi_mois", R.string.levana_usage_sept_mi_mois, false, false),
        SEPT_QUINZE("sept_quinze", R.string.levana_usage_sept_quinze, false, true);

        companion object {
            /** L'usage par défaut d'un noussa'h : Edot haMizra'h le Choulhan Aroukh, Sefard l'Ari, Ashkenaz la Michna Beroura et le Rema. */
            fun duNusah(nusah: String): Usage = when (nusah) {
                "edot" -> SEPT_QUINZE
                "sefard" -> SEPT_MI_MOIS
                else -> TROIS_MI_MOIS
            }
        }
    }

    enum class Restriction { KIPPOUR, TISHA_BEAV }

    data class Fenetre(val molad: Instant, val debut: Instant, val fin: Instant, val restriction: Restriction? = null) {
        val prochainMolad: Instant get() = molad.plus(MOIS)
        fun premierSoir(zone: ZoneId): LocalDate = debut.atZone(zone).toLocalDate()
        fun dernierSoir(zone: ZoneId): LocalDate = fin.minus(Duration.ofHours(12)).atZone(zone).toLocalDate()
    }

    sealed interface Etat {
        data class Dans(val jours: Int) : Etat
        data object CeSoir : Etat
        data object DernierSoir : Etat
    }

    /** La lune : la part éclairée du disque (0 à 1), si elle croît. */
    data class Lune(val eclairage: Double, val croissante: Boolean)

    /**
     * La lune à un instant : Meeus, Astronomical Algorithms, ch. 48 (angle de phase
     * approché) ; à un ou deux pour cent près, assez pour un dessin. Même formule que phaseLune() du web.
     */
    fun phase(t: Instant): Lune {
        val jj = t.toEpochMilli() / 86400000.0 + 2440587.5
        val T = (jj - 2451545) / 36525
        val r = Math.PI / 180
        val d = 297.8501921 + 445267.1114034 * T
        val m = 357.5291092 + 35999.0502909 * T
        val mp = 134.9633964 + 477198.8675055 * T
        val i = 180 - d - 6.289 * Math.sin(mp * r) + 2.1 * Math.sin(m * r) - 1.274 * Math.sin((2 * d - mp) * r) -
            0.658 * Math.sin(2 * d * r) - 0.214 * Math.sin(2 * mp * r) - 0.11 * Math.sin(d * r)
        return Lune((1 + Math.cos(i * r)) / 2, ((d % 360) + 360) % 360 < 180)
    }

    private fun du(date: LocalDate, usage: Usage, zone: ZoneId): Fenetre {
        val c = JewishCalendar(date)
        val molad = c.moladAsDate.toInstant()
        var debut = (if (usage.troisJours) c.tchilasZmanKidushLevana3Days else c.tchilasZmanKidushLevana7Days).toInstant()
        val fin = (if (usage.quinzeJours) c.sofZmanKidushLevana15Days else c.sofZmanKidushLevanaBetweenMoldos).toInstant()
        var restriction: Restriction? = null
        // Tichri et Av : pas avant la sortie de Kippour, de Tich'a beAv (le jeûne, même repoussé au 10).
        val mois = JewishCalendar(debut.atZone(zone).toLocalDate()).jewishMonth
        if (mois == JewishDate.TISHREI || mois == JewishDate.AV) {
            val cible = if (mois == JewishDate.TISHREI) JewishCalendar.YOM_KIPPUR else JewishCalendar.TISHA_BEAV
            val base = molad.atZone(zone).toLocalDate()
            for (k in -9L..12L) {
                val j = base.plusDays(k)
                if (JewishCalendar(j).yomTovIndex == cible) {
                    val sortie = soir(j, zone)
                    if (sortie.isAfter(debut)) {
                        debut = sortie
                        restriction = if (mois == JewishDate.TISHREI) Restriction.KIPPOUR else Restriction.TISHA_BEAV
                    }
                    break
                }
            }
        }
        return Fenetre(molad, debut, fin, restriction)
    }

    /** La fenêtre qui concerne le soir de [date] : celle du mois, ou celle du mois suivant si elle est passée. */
    fun fenetre(date: LocalDate, zone: ZoneId, usage: Usage = Usage.SEPT_MI_MOIS): Fenetre {
        val ici = du(date, usage, zone)
        if (!date.isAfter(ici.dernierSoir(zone))) return ici
        val jour = JewishCalendar(date).jewishDayOfMonth
        return du(date.plusDays((31 - jour + 2).toLong()), usage, zone)
    }

    /** Le motsaé Chabbat conseillé de la fenêtre, à partir de [date] : sa nuit avant le 10, et pas Yom Tov. */
    fun motsaeChabbat(f: Fenetre, date: LocalDate, zone: ZoneId): LocalDate? {
        var d = f.premierSoir(zone)
        while (!d.isAfter(f.dernierSoir(zone))) {
            if (d.dayOfWeek == java.time.DayOfWeek.SATURDAY) {
                val nuit = JewishCalendar(d.plusDays(1))
                if (nuit.jewishDayOfMonth <= 10 && !nuit.isYomTovAssurBemelacha) return d.takeIf { !it.isBefore(date) }
            }
            d = d.plusDays(1)
        }
        return null
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

    /** Le soir de [date] (20 h) : le point posé sur la jauge, l'heure de la lune dessinée. */
    fun soir(date: LocalDate, zone: ZoneId): Instant = date.atTime(LocalTime.of(20, 0)).atZone(zone).toInstant()

    /**
     * La lune de ce soir (14/10, « plus réaliste ») : le disque sombre, la part
     * éclairée à sa vraie mesure, du côté où la voit l'hémisphère du lieu
     * (à droite quand elle croît au nord, à gauche au sud), quelques mers.
     */
    class Dessin(c: Context, private val lune: Lune, private val sud: Boolean) : View(c) {
        private val d = c.resources.displayMetrics.density
        private val ombre = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF3A3F4A.toInt() }
        private val clair = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFF4EFD8.toInt() }
        private val mer = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x2E6B6450 }
        private val bord = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = d; color = 0x33FFFFFF }

        override fun onMeasure(w: Int, h: Int) = setMeasuredDimension((56 * d).toInt(), (56 * d).toInt())

        override fun onDraw(canvas: Canvas) {
            val r = width / 2f - d
            val cx = width / 2f
            val cy = height / 2f
            canvas.drawCircle(cx, cy, r, ombre)
            // Le côté éclairé : à droite quand elle croît (nord), en miroir au sud.
            val droite = lune.croissante != sud
            val k = lune.eclairage.toFloat()
            val ellipse = r * Math.abs(1 - 2 * k)
            val chemin = android.graphics.Path().apply {
                // Le demi-disque du côté éclairé…
                arcTo(RectF(cx - r, cy - r, cx + r, cy + r), -90f, if (droite) 180f else -180f, true)
                // … refermé par le terminateur, une demi-ellipse : bombée vers l'ombre au-delà de la moitié.
                val bombe = if (k > 0.5f) droite else !droite
                arcTo(RectF(cx - ellipse, cy - r, cx + ellipse, cy + r), 90f, if (bombe) 180f else -180f, false)
                close()
            }
            canvas.save()
            canvas.clipPath(chemin)
            canvas.drawCircle(cx, cy, r, clair)
            // Quelques mers, à peine : la lune, pas un disque.
            listOf(Triple(-0.25f, -0.3f, 0.22f), Triple(0.2f, -0.1f, 0.18f), Triple(-0.05f, 0.25f, 0.2f), Triple(0.35f, 0.35f, 0.1f))
                .forEach { (x, y, t) -> canvas.drawCircle(cx + x * r, cy + y * r, t * r, mer) }
            canvas.restore()
            canvas.drawCircle(cx, cy, r, bord)
        }
    }

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
