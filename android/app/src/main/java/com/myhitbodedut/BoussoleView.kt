package com.myhitbodedut

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.os.Build
import android.util.AttributeSet
import android.view.HapticFeedbackConstants
import android.view.View

/**
 * La boussole C à l'écran (BoussoleActivity) : un carré. Avec un cap, la scène
 * vivante ; sans cap (pas de capteur), la rose orientée au nord. Une petite
 * vibration quand on arrive face à Jérusalem.
 */
class BoussoleView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {

    private val image: Bitmap? = runCatching { BitmapFactory.decodeResource(resources, R.drawable.char_standing_back) }.getOrNull()

    private var principale: Double? = null
    private var autre: Double? = null
    private var cap: Double? = null
    private var etaitFace = false

    /** La rose des vents plutôt que le hassid (Store.boussoleRose) ; le hassid par défaut. */
    var roseDesVents = false
        set(v) { field = v; invalidate() }
    private val cardinaux: Array<String> = resources.getStringArray(R.array.boussole_cardinaux)

    /** Les deux directions de Jérusalem, depuis le nord vrai ; null sans lieu. */
    fun directions(principale: Double?, autre: Double?) {
        this.principale = principale
        this.autre = autre
        majDescription()
        invalidate()
    }

    /** Où pointe le haut du téléphone, depuis le nord vrai ; null sans capteur. */
    fun cap(valeur: Double?) {
        cap = valeur
        val p = principale
        val face = p != null && valeur != null && DirectionJerusalem.face(DirectionJerusalem.ecart(p, valeur))
        if (face && !etaitFace) {
            performHapticFeedback(if (Build.VERSION.SDK_INT >= 30) HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.VIRTUAL_KEY)
        }
        if (face != etaitFace) {
            etaitFace = face
            majDescription()
        }
        invalidate()
    }

    /** Le texte du haut, et ce que lit TalkBack. */
    fun consigne(): String {
        val p = principale ?: return context.getString(R.string.zmanim_choisir_lieu)
        val c = cap ?: return context.getString(R.string.boussole_par_rapport_nord, DirectionJerusalem.degres(p))
        val e = DirectionJerusalem.ecart(p, c)
        return when {
            DirectionJerusalem.face(e) -> context.getString(R.string.boussole_face)
            e > 0 -> context.getString(R.string.boussole_droite, Math.round(e).toInt())
            else -> context.getString(R.string.boussole_gauche, Math.round(-e).toInt())
        }
    }

    // Pas à chaque mesure du capteur : TalkBack relirait sans fin. À l'arrivée face à Jérusalem, et au départ.
    private fun majDescription() {
        contentDescription = consigne()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val l = MeasureSpec.getSize(widthMeasureSpec)
        val h = if (MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.UNSPECIFIED) l else minOf(l, MeasureSpec.getSize(heightMeasureSpec))
        setMeasuredDimension(l, h)
    }

    override fun onDraw(canvas: Canvas) {
        val p = principale
        val c = cap
        if (p != null && c != null) {
            if (roseDesVents) BoussoleDessin.roseVivante(canvas, width, height, p, autre ?: p, c, consigne(), cardinaux)
            else BoussoleDessin.ecran(canvas, width, height, p, autre ?: p, c, consigne(), image)
        } else {
            BoussoleDessin.rose(canvas, width, height, p, p?.let { "${DirectionJerusalem.degres(it)}°" } ?: "",
                context.getString(R.string.boussole_nord), image)
        }
    }
}
