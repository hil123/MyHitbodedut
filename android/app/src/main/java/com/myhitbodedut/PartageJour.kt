package com.myhitbodedut

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.FileProvider
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Partager un jour (14/10) : une image de 1080 × 1350 — la date hébraïque et
 * civile, les fêtes et la paracha, la hilloula, le début de la halakha du jour
 * avec sa source — envoyée par le choix d'Android (WhatsApp…). Rien ne part
 * sans ce geste ; l'image reste dans le cache de l'application.
 */
object PartageJour {

    private const val L = 1080
    private const val H = 1350
    private const val MARGE = 84f

    /** Le texte de l'image, ligne par ligne (testé sans dessin). */
    data class Contenu(val dateHebraique: String, val civil: String, val marques: String, val hilloula: String, val halakha: String, val source: String)

    fun contenu(c: Context, date: LocalDate): Contenu {
        val israel = Store.inIsrael(c)
        val j = CalendrierJuif.jour(date, israel, Store.mukafChoma(c))
        val langue = ChizukCard.language(c)
        val marques = j.fetes.mapNotNull { Libelles.res("fete_$it")?.let(c::getString) } +
            listOfNotNull(j.paracha?.let { c.getString(R.string.zmanim_paracha, it) })
        val hilloulot = Hilloulot.pour(Hilloulot.table(c), date, israel, Store.siddurNusah(c).cle == "ashkenaz").take(3).joinToString(" · ") { it.he }
        val (livre, debut) = VueJour.apercuHalakha(c, date, israel) ?: ("" to "")
        return Contenu(
            j.dateHebraique,
            date.format(DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.forLanguageTag(langue))),
            marques.joinToString("  ·  "),
            hilloulot,
            debut.take(420).let { if (debut.length > 420) "$it…" else it },
            livre,
        )
    }

    fun image(c: Context, x: Contenu): Bitmap {
        val b = Bitmap.createBitmap(L, H, Bitmap.Config.ARGB_8888)
        val cv = Canvas(b)
        cv.drawColor(Apparence.couleur(c, R.color.brand_dark))
        val encre = 0xFFFFFFFF.toInt()
        val douce = 0xCCFFFFFF.toInt()
        val hebreu = runCatching { androidx.core.content.res.ResourcesCompat.getFont(c, R.font.frank_ruhl) }.getOrNull() ?: Typeface.SERIF
        var y = MARGE + 40
        fun bloc(texte: String, taille: Float, couleur: Int, police: Typeface, gras: Boolean = false, rtl: Boolean = true, apres: Float = 28f) {
            if (texte.isBlank()) return
            val p = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { textSize = taille; color = couleur; typeface = Typeface.create(police, if (gras) Typeface.BOLD else Typeface.NORMAL) }
            val l = StaticLayout.Builder.obtain(texte, 0, texte.length, p, (L - 2 * MARGE).toInt())
                .setAlignment(if (rtl) Layout.Alignment.ALIGN_OPPOSITE else Layout.Alignment.ALIGN_NORMAL)
                .setTextDirection(if (rtl) android.text.TextDirectionHeuristics.RTL else android.text.TextDirectionHeuristics.FIRSTSTRONG_LTR)
                .setLineSpacing(0f, 1.25f).build()
            cv.save(); cv.translate(MARGE, y); l.draw(cv); cv.restore()
            y += l.height + apres
        }
        bloc(x.dateHebraique, 92f, encre, hebreu, gras = true, apres = 8f)
        bloc(x.civil, 40f, douce, Typeface.DEFAULT, rtl = false, apres = 22f)
        bloc(x.marques, 40f, encre, Typeface.DEFAULT, gras = true, rtl = false, apres = 40f)
        if (x.hilloula.isNotBlank()) {
            bloc(c.getString(R.string.hilloula_titre), 30f, douce, Typeface.DEFAULT, gras = true, rtl = false, apres = 6f)
            bloc(x.hilloula, 44f, encre, hebreu, apres = 40f)
        }
        if (x.halakha.isNotBlank()) {
            bloc(c.getString(R.string.halakha_yomit_titre) + " · " + x.source, 30f, douce, Typeface.DEFAULT, gras = true, rtl = false, apres = 6f)
            bloc(x.halakha, 42f, encre, hebreu, apres = 20f)
        }
        // La signature, en bas.
        val p = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 30f; color = douce; typeface = Typeface.DEFAULT_BOLD }
        cv.drawText(c.getString(R.string.app_name), MARGE, H - MARGE / 2, p)
        return b
    }

    fun partager(c: Context, date: LocalDate) {
        val dossier = File(c.cacheDir, "partage").apply { mkdirs() }
        dossier.listFiles()?.forEach { it.delete() }
        val fichier = File(dossier, "jour-$date.png")
        fichier.outputStream().use { image(c, contenu(c, date)).compress(Bitmap.CompressFormat.PNG, 100, it) }
        val uri = FileProvider.getUriForFile(c, c.packageName + ".partage", fichier)
        val envoi = Intent(Intent.ACTION_SEND).setType("image/png").putExtra(Intent.EXTRA_STREAM, uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        c.startActivity(Intent.createChooser(envoi, c.getString(R.string.partage_jour)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
