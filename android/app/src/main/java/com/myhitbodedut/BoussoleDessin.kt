package com.myhitbodedut

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface

/**
 * La boussole C, « le hassid se tourne » (choisie le 28/10 parmi cinq) : le
 * hassid de dos au centre, Jérusalem qui glisse sur l'horizon ; quand elle
 * arrive au-dessus de sa tête, il est face à elle et se hausse un peu. L'autre
 * direction est le petit rond pointillé bleu. Dessiné dans un carré de 320
 * unités centré sur (0, 0), comme la maquette.
 *
 * [roseVivante] est l'autre affichage au choix (28/10) : une rose des vents
 * qui tourne avec le téléphone, l'aiguille d'or vers Jérusalem.
 *
 * [rose] est la version sans boussole vivante : celle du widget, et de l'écran
 * quand le téléphone n'a pas de capteur. La direction y est donnée par rapport
 * au nord, en haut.
 */
object BoussoleDessin {

    // Les couleurs de la scène, fixes comme une illustration (bg_stage), de jour comme de nuit.
    private const val CIEL = 0xFFDFF5E8.toInt()
    private const val SOL = 0xFFCBEBD8.toInt()
    private const val HORIZON = 0xFF9CC9B0.toInt()
    private const val ENCRE = 0xFF173C32.toInt()
    private const val MARQUE = 0xFF16775A.toInt()
    private const val OR = 0xFFB8780F.toInt()
    private const val BLEU = 0xFF2E7DB3.toInt()
    private const val BLANC = 0xFFFFFFFF.toInt()

    private const val JERUSALEM = "ירושלים"

    /** Combien de pixels d'horizon par degré d'écart ; au-delà de ±150, Jérusalem reste au bord. */
    private const val PAR_DEGRE = 2.2f

    fun xHorizon(ecart: Double): Float = (ecart.toFloat() * PAR_DEGRE).coerceIn(-150f, 150f)

    private fun pinceau(couleur: Int, style: Paint.Style = Paint.Style.FILL) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = couleur
        this.style = style
    }

    private fun texte(couleur: Int, taille: Float, gras: Boolean = false, serif: Boolean = false) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = couleur
        textSize = taille
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(if (serif) Typeface.SERIF else Typeface.DEFAULT, if (gras) Typeface.BOLD else Typeface.NORMAL)
    }

    /** Ramène le canevas au carré de 320 unités centré, au milieu de la place donnée. */
    private fun cadrer(c: Canvas, largeur: Int, hauteur: Int, cote: Float = 320f) {
        val echelle = minOf(largeur, hauteur) / cote
        c.translate(largeur / 2f, hauteur / 2f)
        c.scale(echelle, echelle)
    }

    /** Le hassid de dos, dans le rectangle donné. */
    private fun hassid(c: Canvas, image: Bitmap?, x: Float, y: Float, l: Float, h: Float) {
        image ?: return
        c.drawBitmap(image, null, RectF(x, y, x + l, y + h), Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG))
    }

    /**
     * L'écran vivant. [principale] et [autre] sont les deux directions de Jérusalem,
     * [cap] celle où pointe le haut du téléphone, toutes depuis le nord vrai ;
     * [consigne] le texte du haut (« Tournez à droite de 12° »).
     */
    fun ecran(c: Canvas, largeur: Int, hauteur: Int, principale: Double, autre: Double, cap: Double, consigne: String, image: Bitmap?) {
        c.save()
        cadrer(c, largeur, hauteur)
        val e = DirectionJerusalem.ecart(principale, cap)
        val face = DirectionJerusalem.face(e)
        c.drawRect(-160f, -160f, 160f, 160f, pinceau(CIEL))
        c.drawPath(Path().apply {
            moveTo(-160f, 40f); quadTo(0f, 10f, 160f, 40f); lineTo(160f, 160f); lineTo(-160f, 160f); close()
        }, pinceau(SOL))
        c.drawLine(-150f, -90f, 150f, -90f, pinceau(HORIZON, Paint.Style.STROKE).apply { strokeWidth = 2f })

        // L'autre direction : un petit rond pointillé.
        val xa = xHorizon(DirectionJerusalem.ecart(autre, cap))
        c.drawCircle(xa, -90f, 7f, pinceau(BLEU, Paint.Style.STROKE).apply {
            strokeWidth = 2.5f
            pathEffect = DashPathEffect(floatArrayOf(3f, 3f), 0f)
        })

        // Jérusalem : l'étiquette et son point.
        val xg = xHorizon(e)
        c.drawRoundRect(RectF(xg - 34f, -124f, xg + 34f, -102f), 11f, 11f, pinceau(if (face) MARQUE else BLANC))
        c.drawText(JERUSALEM, xg, -108f, texte(if (face) BLANC else MARQUE, 15f, gras = true, serif = true))
        c.drawCircle(xg, -90f, 8f, pinceau(if (face) MARQUE else OR))

        // Face à Jérusalem, il se hausse un peu.
        hassid(c, image, -28f, if (face) -68f else -62f, 56f, 176f)
        c.drawText(consigne, 0f, -132f, texte(ENCRE, 14f))
        c.restore()
    }

    /**
     * La rose sans capteur, pour le widget et l'écran sans boussole : le nord en
     * haut, un triangle d'or à la direction principale, le hassid au centre.
     */
    fun rose(c: Canvas, largeur: Int, hauteur: Int, principale: Double?, degres: String, nord: String, image: Bitmap?) {
        c.save()
        cadrer(c, largeur, hauteur, 112f)
        c.drawRoundRect(RectF(-56f, -56f, 56f, 56f), 12f, 12f, pinceau(CIEL))
        c.drawCircle(0f, 0f, 44f, pinceau(HORIZON, Paint.Style.STROKE).apply { strokeWidth = 1.2f })
        hassid(c, image, -12f, -33f, 26f, 81f)
        if (principale != null) {
            c.save()
            c.rotate(principale.toFloat())
            c.drawPath(Path().apply { moveTo(0f, -53f); lineTo(5f, -37f); lineTo(0f, -40f); lineTo(-5f, -37f); close() }, pinceau(OR))
            c.restore()
        }
        val petit = texte(ENCRE, 11f, gras = true)
        c.drawText(nord, 0f, -42f, petit.apply { textSize = 9f })
        c.drawText(degres, 38f, 51f, texte(ENCRE, 10f))
        c.drawText(JERUSALEM, -36f, 51f, texte(MARQUE, 9f, gras = true, serif = true))
        c.restore()
    }

    /**
     * La rose des vents vivante : le cadran tourne avec le téléphone, le repère
     * du haut est le cap ; l'aiguille d'or va vers la direction principale, le
     * trait bleu pointillé vers l'autre. [cardinaux] : N, E, S, O dans la langue.
     */
    fun roseVivante(c: Canvas, largeur: Int, hauteur: Int, principale: Double, autre: Double, cap: Double,
                    consigne: String, cardinaux: Array<String>) {
        c.save()
        cadrer(c, largeur, hauteur)
        val face = DirectionJerusalem.face(DirectionJerusalem.ecart(principale, cap))
        c.drawRect(-160f, -160f, 160f, 160f, pinceau(CIEL))
        val trait = pinceau(HORIZON, Paint.Style.STROKE).apply { strokeWidth = 2f }
        c.drawCircle(0f, 0f, 112f, trait)

        // Le cadran, tourné du cap : le nord du cadran reste au nord.
        c.save()
        c.rotate(-cap.toFloat())
        val grad = pinceau(ENCRE, Paint.Style.STROKE)
        for (d in 0 until 360 step 10) {
            c.save()
            c.rotate(d.toFloat())
            grad.strokeWidth = if (d % 90 == 0) 2.5f else 1.2f
            c.drawLine(0f, -112f, 0f, -112f + if (d % 90 == 0) 14f else if (d % 30 == 0) 9f else 5f, grad)
            c.restore()
        }
        val lettre = texte(ENCRE, 17f, gras = true)
        cardinaux.take(4).forEachIndexed { i, n ->
            val a = Math.toRadians(i * 90.0)
            c.save()
            c.translate((86 * Math.sin(a)).toFloat(), (-86 * Math.cos(a)).toFloat())
            c.rotate(cap.toFloat())
            lettre.color = if (i == 0) OR else ENCRE
            c.drawText(n, 0f, 6f, lettre)
            c.restore()
        }
        c.restore()

        // L'autre direction, puis la principale.
        c.save()
        c.rotate((autre - cap).toFloat())
        c.drawLine(0f, 0f, 0f, -100f, pinceau(BLEU, Paint.Style.STROKE).apply {
            strokeWidth = 3f
            pathEffect = DashPathEffect(floatArrayOf(7f, 7f), 0f)
        })
        c.restore()
        val couleur = if (face) MARQUE else OR
        c.save()
        c.rotate((principale - cap).toFloat())
        c.drawLine(0f, 18f, 0f, -100f, pinceau(couleur, Paint.Style.STROKE).apply { strokeWidth = 5f; strokeCap = Paint.Cap.ROUND })
        c.drawCircle(0f, -100f, 7f, pinceau(couleur))
        c.restore()
        c.drawCircle(0f, 0f, 6f, pinceau(ENCRE))

        // L'étiquette au bout de l'aiguille, droite.
        val a = Math.toRadians(principale - cap)
        var x = (128 * Math.sin(a)).toFloat().coerceIn(-120f, 120f)
        val y = (-128 * Math.cos(a)).toFloat().coerceIn(-118f, 140f)
        // Près du haut, à côté du repère du cap plutôt que dessus.
        if (y < -100f && kotlin.math.abs(x) < 46f) x = if (x < 0) -46f else 46f
        c.drawRoundRect(RectF(x - 34f, y - 11f, x + 34f, y + 11f), 11f, 11f, pinceau(if (face) MARQUE else BLANC))
        c.drawText(JERUSALEM, x, y + 5f, texte(if (face) BLANC else MARQUE, 15f, gras = true, serif = true))

        // Le repère du haut : où pointe le téléphone.
        c.drawPath(Path().apply { moveTo(0f, -118f); lineTo(-7f, -130f); lineTo(7f, -130f); close() }, pinceau(ENCRE))
        c.drawText(consigne, 0f, -140f, texte(ENCRE, 14f))
        c.restore()
    }
}
