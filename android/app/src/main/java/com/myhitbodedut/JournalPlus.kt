package com.myhitbodedut

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.view.View
import org.json.JSONObject
import java.time.YearMonth

/**
 * Le journal enrichi (14/10) : l'humeur avant et après la hitbodedout (1 à 5),
 * une gratitude, et le graphique des minutes du mois. Gardé à part des journées
 * (Store.days) : une journée reste ce qu'elle était, ceci s'y ajoute.
 */
object JournalPlus {

    /**
     * [travail] : la chose à travailler (proposition 5, 19/10), seconde question
     * de fin à côté de la gratitude.
     */
    data class Plus(val avant: Int = 0, val apres: Int = 0, val gratitude: String = "", val travail: String = "") {
        val vide: Boolean get() = avant == 0 && apres == 0 && gratitude.isBlank() && travail.isBlank()
    }

    /** Les cinq humeurs, de la plus lourde à la plus légère. */
    val HUMEURS = listOf("😞", "🙁", "😐", "🙂", "😄")

    const val GRATITUDE_MAX = 200

    private fun tout(context: Context): JSONObject =
        runCatching { JSONObject(Store.journalPlusBrut(context) ?: "{}") }.getOrDefault(JSONObject())

    fun lire(context: Context, date: String): Plus {
        val o = tout(context).optJSONObject(date) ?: return Plus()
        return Plus(o.optInt("avant", 0).coerceIn(0, 5), o.optInt("apres", 0).coerceIn(0, 5), o.optString("gratitude", "").take(GRATITUDE_MAX),
            o.optString("travail", "").take(GRATITUDE_MAX))
    }

    fun ecrire(context: Context, date: String, plus: Plus) {
        val t = tout(context)
        if (plus.vide) t.remove(date) else t.put(date, JSONObject().put("avant", plus.avant).put("apres", plus.apres)
            .put("gratitude", plus.gratitude.take(GRATITUDE_MAX)).put("travail", plus.travail.take(GRATITUDE_MAX)))
        Store.setJournalPlusBrut(context, t.toString())
    }

    /** « 🙁 → 🙂 · Gratitude » pour la ligne du journal ; vide s'il n'y a rien. */
    fun resume(plus: Plus, gratitude: String, travail: String = "", sujets: String = ""): String = listOfNotNull(
        sujets.takeIf { it.isNotBlank() },
        if (plus.avant > 0 || plus.apres > 0) "${HUMEURS.getOrNull(plus.avant - 1) ?: "·"} → ${HUMEURS.getOrNull(plus.apres - 1) ?: "·"}" else null,
        plus.gratitude.takeIf { it.isNotBlank() }?.let { "$gratitude : $it" },
        plus.travail.takeIf { it.isNotBlank() }?.let { "$travail : $it" },
    ).joinToString("\n")

    /** Les minutes de chaque jour du mois, du 1er au dernier (0 sans hitbodedout). */
    fun minutesDuMois(jours: Map<String, DayRecord>, mois: YearMonth): List<Int> =
        (1..mois.lengthOfMonth()).map { j -> jours[mois.atDay(j).toString()]?.takeIf { it.faite }?.minutes ?: 0 }

    /** Les barres du mois : une par jour, à l'échelle du plus long ; le jour d'aujourd'hui marqué. */
    class Graphique(c: Context, private val minutes: List<Int>, private val aujourdhui: Int) : View(c) {
        private val d = c.resources.displayMetrics.density
        private val barre = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFFFFF.toInt() }
        private val vide = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x40FFFFFF }

        override fun onMeasure(w: Int, h: Int) = setMeasuredDimension(MeasureSpec.getSize(w), (64 * d).toInt())

        override fun onDraw(canvas: Canvas) {
            if (minutes.isEmpty()) return
            val max = (minutes.maxOrNull() ?: 0).coerceAtLeast(1)
            val pas = width.toFloat() / minutes.size
            val l = (pas * 0.6f).coerceAtLeast(2 * d)
            minutes.forEachIndexed { i, m ->
                val h = if (m > 0) (height - 4 * d) * m / max + 4 * d else 3 * d
                // De droite à gauche quand l'écran l'est.
                val x = if (layoutDirection == LAYOUT_DIRECTION_RTL) width - (i + 1) * pas + (pas - l) / 2 else i * pas + (pas - l) / 2
                val p = if (m > 0) barre else vide
                canvas.drawRoundRect(RectF(x, height - h, x + l, height.toFloat()), l / 2, l / 2, p)
                if (i + 1 == aujourdhui) canvas.drawCircle(x + l / 2, height - h - 4 * d, 1.5f * d, barre)
            }
        }
    }
}
