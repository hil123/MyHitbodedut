package com.myhitbodedut

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.RadialGradient
import android.graphics.Shader
import android.graphics.drawable.Drawable
import androidx.core.graphics.PathParser
import org.json.JSONObject

/**
 * L'objet d'un trophée (12/10) : coupe, écusson, étoile, rosette, médaille,
 * pièces, cristal ou gemme, dessiné d'après res/raw/trophees_formes.json
 * (tools/trophees_formes.py), le même fichier que la version web. Chaque
 * partie a sa matière : métal (dégradé), métal bombé (radial), métal
 * sombre, verre, ruban, reflet. Sans métal, l'objet est gris.
 */
class TropheeDessin(context: Context, forme: Trophees.Forme, private val metal: Trophees.Metal?, private val icone: String) : Drawable() {

    private class Partie(val matiere: String, val chemin: Path)

    private val parties: List<Partie>
    private val embleme: FloatArray

    init {
        val f = formes(context).getJSONObject(forme.cle)
        val ps = f.getJSONArray("parts")
        parties = (0 until ps.length()).map { i ->
            val p = ps.getJSONArray(i)
            Partie(p.getString(0), PathParser.createPathFromPathData(p.getString(1)))
        }
        val e = f.getJSONArray("embleme")
        embleme = floatArrayOf(e.getDouble(0).toFloat(), e.getDouble(1).toFloat(), e.getDouble(2).toFloat())
    }

    private val pinceau = Paint(Paint.ANTI_ALIAS_FLAG)
    private val texte = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER }
    private val trait = traitIcone()
    private val traces = Companion.icones(context)
    private val densite = context.resources.displayMetrics.density

    override fun draw(c: Canvas) {
        val (clair, fonce, sombre) = teintes(metal)
        val cote = minOf(bounds.width(), bounds.height()).toFloat()
        c.save()
        c.translate(bounds.left + (bounds.width() - cote) / 2, bounds.top + (bounds.height() - cote) / 2)
        c.scale(cote / 100f, cote / 100f)
        val diag = LinearGradient(0f, 0f, 100f, 100f, clair, fonce, Shader.TileMode.CLAMP)
        val bombe = RadialGradient(35f, 30f, 75f, intArrayOf(Color.WHITE, clair, fonce), floatArrayOf(0f, .35f, 1f), Shader.TileMode.CLAMP)
        parties.forEach { p ->
            pinceau.shader = null
            when (p.matiere) {
                "m" -> pinceau.shader = diag
                "mr" -> pinceau.shader = bombe
                "md" -> pinceau.color = sombre
                "v" -> pinceau.color = Color.argb(0xBF, 0xAA, 0xD7, 0xFF)
                "vc" -> pinceau.color = Color.argb(0xE6, 0xEB, 0xF7, 0xFF)
                "r" -> pinceau.color = if (metal != null) Color.rgb(0xD4, 0x3B, 0x3B) else Color.rgb(0xB9, 0xBD, 0xC1)
                else -> pinceau.color = Color.argb(0x80, 0xFF, 0xFF, 0xFF)
            }
            c.drawPath(p.chemin, pinceau)
        }
        val trace = traces[icone]
        if (trace != null) {
            // L'icône de la famille, au trait, du ton sombre du métal (16/10) ; estompée sans métal.
            trait.color = sombre
            trait.alpha = if (metal != null) 0xFF else 0xB3
            // Emblème d'au moins 20 % et trait d'au moins 1,3 dp à l'écran : lisible en liste (audit du 16/10).
            val taille = maxOf(embleme[2] * 1.15f, 20f)
            trait.strokeWidth = maxOf(2f, 1.3f * densite / ((cote / 100f) * (taille / 24f)))
            dessinerIcone(c, trace, embleme[0], embleme[1], taille, trait)
        } else {
            texte.textSize = embleme[2]
            texte.alpha = if (metal != null) 0xFF else 0x73
            val fm = texte.fontMetrics
            c.drawText(icone, embleme[0], embleme[1] - (fm.ascent + fm.descent) / 2, texte)
        }
        c.restore()
    }

    override fun setAlpha(alpha: Int) { pinceau.alpha = alpha }
    override fun setColorFilter(cf: ColorFilter?) { pinceau.colorFilter = cf; texte.colorFilter = cf; trait.colorFilter = cf }
    @Deprecated("Deprecated in Java")
    override fun getOpacity() = PixelFormat.TRANSLUCENT

    companion object {
        @Volatile private var cache: JSONObject? = null
        @Volatile private var cacheIcones: Map<String, Path>? = null

        private fun racine(c: Context): JSONObject = JSONObject(Json.readRaw(c.applicationContext, R.raw.trophees_formes))

        private fun formes(c: Context): JSONObject =
            cache ?: racine(c).getJSONObject("formes").also { cache = it }

        /** Les icônes des familles au trait (boîte 24 × 24), par clé. */
        fun icones(c: Context): Map<String, Path> = cacheIcones ?: racine(c).getJSONObject("icones").let { o ->
            o.keys().asSequence().associateWith { PathParser.createPathFromPathData(o.getString(it)) }
        }.also { cacheIcones = it }

        /** Le pinceau des icônes : trait de 2 (sur 24), bouts et angles ronds. */
        fun traitIcone() = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 2f
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }

        /** Dessine [trace] (boîte 24) centré en ([cx], [cy]), de côté [taille]. */
        fun dessinerIcone(c: Canvas, trace: Path, cx: Float, cy: Float, taille: Float, trait: Paint) {
            val k = taille / 24f
            c.save()
            c.translate(cx - taille / 2, cy - taille / 2)
            c.scale(k, k)
            c.drawPath(trace, trait)
            c.restore()
        }

        /** Clair, foncé, sombre : la face, le bord, la tranche et le socle. Gris sans métal. */
        fun teintes(m: Trophees.Metal?): Triple<Int, Int, Int> = when (m) {
            Trophees.Metal.BRONZE -> Triple(Color.rgb(0xF3, 0xC4, 0x9A), Color.rgb(0xA0, 0x60, 0x2E), Color.rgb(0x6E, 0x3E, 0x1C))
            Trophees.Metal.ARGENT -> Triple(Color.rgb(0xF4, 0xF6, 0xF8), Color.rgb(0x9A, 0xA3, 0xAC), Color.rgb(0x5E, 0x67, 0x70))
            Trophees.Metal.OR -> Triple(Color.rgb(0xFF, 0xE4, 0x8A), Color.rgb(0xC9, 0x96, 0x1A), Color.rgb(0x8A, 0x62, 0x06))
            Trophees.Metal.PLATINE -> Triple(Color.rgb(0xF7, 0xFA, 0xFF), Color.rgb(0x8C, 0xA2, 0xC4), Color.rgb(0x55, 0x68, 0x8A))
            null -> Triple(Color.rgb(0xE3, 0xE5, 0xE7), Color.rgb(0xB6, 0xBA, 0xBE), Color.rgb(0x8E, 0x93, 0x98))
        }
    }
}
