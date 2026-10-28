package com.myhitbodedut

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.hardware.GeomagneticField
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.ColorUtils
import com.google.android.material.button.MaterialButton
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * La boussole de la tefila (28/10) : vers le mont du Temple depuis le lieu des zmanim
 * (DirectionTefila). Le téléphone à plat, la rose tourne avec lui : le nord reste au
 * nord, l'aiguille dorée montre Jérusalem. Le guide dit de combien tourner ; face au mont
 * du Temple (±5°, relâché à 8° pour ne pas clignoter), la rose s'allume et le téléphone
 * donne un léger retour haptique. Même dessin que le web (ecranBoussole). Le cap magnétique du capteur est corrigé de la
 * déclinaison du lieu (GeomagneticField, modèle magnétique mondial embarqué dans Android,
 * sans réseau). Sans capteur, le cap seul, en degrés depuis le nord.
 *
 * « Position exacte » relance la localisation précise (ChoixLieu) : la direction et les
 * zmanim suivent le point où l'on est, pas la ville la plus proche.
 */
class BoussoleActivity : AppCompatActivity(), SensorEventListener {

    private val choixLieu = ChoixLieu(this) { afficher() }
    private lateinit var cadran: Cadran
    private lateinit var cap: TextView
    private lateinit var point: TextView
    private lateinit var guide: TextView
    private var cible = Float.NaN
    private var aligne = false
    private lateinit var etat: TextView
    private lateinit var lieuTexte: TextView
    private lateinit var loxoTexte: TextView
    private var capteurs: SensorManager? = null
    private var declinaison = 0f
    private var lisse = Float.NaN
    private val matrice = FloatArray(9)
    private val orientation = FloatArray(3)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val d = resources.displayMetrics.density
        fun dp(v: Int) = (v * d).roundToInt()
        title = getString(R.string.boussole_titre)
        cadran = Cadran(this)
        cap = TextView(this).apply { textSize = 44f; gravity = Gravity.CENTER; setTypeface(typeface, Typeface.BOLD)
            setTextColor(Apparence.couleur(context, R.color.ink)); fontFeatureSettings = "tnum" }
        point = TextView(this).apply { textSize = 15f; gravity = Gravity.CENTER; setTextColor(Apparence.couleur(context, R.color.muted)) }
        guide = TextView(this).apply { textSize = 16f; gravity = Gravity.CENTER; setTypeface(typeface, Typeface.BOLD)
            setTextColor(Apparence.couleur(context, R.color.brand)); accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE }
        etat = TextView(this).apply { textSize = 13f; gravity = Gravity.CENTER; setTextColor(Apparence.couleur(context, R.color.muted)) }
        loxoTexte = TextView(this).apply { textSize = 13f; gravity = Gravity.CENTER; setTextColor(Apparence.couleur(context, R.color.muted)) }
        lieuTexte = TextView(this).apply { textSize = 13f; gravity = Gravity.CENTER; setTextColor(Apparence.couleur(context, R.color.muted)) }
        setContentView(android.widget.ScrollView(this).apply {
            setBackgroundColor(Apparence.couleur(context, R.color.paper))
            addView(LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER_HORIZONTAL
                setPadding(dp(16), dp(24), dp(16), dp(24))
                addView(TextView(context).apply {
                    text = getString(R.string.boussole_titre); textSize = 24f; gravity = Gravity.CENTER
                    setTypeface(typeface, Typeface.BOLD); setTextColor(Apparence.couleur(context, R.color.brand))
                })
                addView(lieuTexte, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(4) })
                val cote = min(resources.displayMetrics.widthPixels - dp(32), dp(340))
                addView(cadran, LinearLayout.LayoutParams(cote, cote).apply { topMargin = dp(12) })
                addView(cap, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(8) })
                addView(point, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
                addView(guide, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(10) })
                addView(loxoTexte, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(10) })
                addView(etat, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(6) })
                addView(MaterialButton(context, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
                    text = getString(R.string.boussole_position_exacte)
                    setOnClickListener { choixLieu.positionExacte() }
                }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(16) })
            })
        })
        capteurs = getSystemService(SensorManager::class.java)
        afficher()
    }

    override fun onResume() {
        super.onResume()
        val c = capteurs?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        if (c == null) {
            etat.text = getString(R.string.boussole_sans_capteur)
        } else {
            capteurs?.registerListener(this, c, SensorManager.SENSOR_DELAY_UI)
            etat.text = getString(R.string.boussole_a_plat)
        }
    }

    override fun onPause() {
        super.onPause()
        capteurs?.unregisterListener(this)
    }

    private fun afficher() {
        val lieu = Store.lieuZmanim(this)
        if (lieu == null) {
            lieuTexte.text = getString(R.string.zmanim_choisir_lieu)
            cap.text = ""
            point.text = ""
            guide.text = ""
            cible = Float.NaN
            cadran.cible = Float.NaN
            cadran.invalidate()
            return
        }
        val ortho = DirectionTefila.orthodromie(lieu.latitude, lieu.longitude)
        val loxo = DirectionTefila.loxodromie(lieu.latitude, lieu.longitude)
        val km = DirectionTefila.distanceKm(lieu.latitude, lieu.longitude)
        lieuTexte.text = getString(R.string.boussole_lieu, lieu.nom, km.roundToInt())
        val points = resources.getStringArray(R.array.boussole_points)
        cap.text = getString(R.string.boussole_degres, ortho.roundToInt())
        point.text = points[DirectionTefila.octant(ortho)].replaceFirstChar { it.uppercase() }
        loxoTexte.text = getString(R.string.boussole_loxodromie, loxo.roundToInt(), points[DirectionTefila.octant(loxo)])
        cible = ortho.toFloat()
        cadran.cible = cible
        cadran.cardinaux = resources.getStringArray(R.array.boussole_cardinaux)
        declinaison = GeomagneticField(lieu.latitude.toFloat(), lieu.longitude.toFloat(), lieu.altitude.toFloat(),
            System.currentTimeMillis()).declination
        cadran.invalidate()
    }

    override fun onSensorChanged(event: SensorEvent) {
        SensorManager.getRotationMatrixFromVector(matrice, event.values)
        SensorManager.getOrientation(matrice, orientation)
        val nord = ((Math.toDegrees(orientation[0].toDouble()).toFloat() + declinaison) + 360f) % 360f
        // Lissage sur le cercle : sans à-coup entre 359° et 0°.
        lisse = if (lisse.isNaN()) nord else {
            var e = nord - lisse
            if (e > 180) e -= 360
            if (e < -180) e += 360
            (lisse + e * 0.15f + 360f) % 360f
        }
        cadran.cap = lisse
        guider()
        cadran.invalidate()
    }

    /** De combien tourner, et l'éclat quand on fait face (hystérésis 5° / 8°). */
    private fun guider() {
        if (cible.isNaN() || lisse.isNaN()) return
        var ecart = (cible - lisse) % 360f
        if (ecart > 180) ecart -= 360
        if (ecart < -180) ecart += 360
        val face = abs(ecart) <= if (aligne) 8f else 5f
        if (face && !aligne) {
            cadran.performHapticFeedback(if (Build.VERSION.SDK_INT >= 30) HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.VIRTUAL_KEY)
        }
        aligne = face
        cadran.aligne = face
        val texte = when {
            face -> getString(R.string.boussole_aligne)
            ecart > 0 -> getString(R.string.boussole_droite, abs(ecart).roundToInt())
            else -> getString(R.string.boussole_gauche, abs(ecart).roundToInt())
        }
        if (guide.text != texte) guide.text = texte
        guide.setTextColor(Apparence.couleur(this, if (face) R.color.boussole_or else R.color.brand))
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        if (accuracy <= SensorManager.SENSOR_STATUS_ACCURACY_LOW) etat.text = getString(R.string.boussole_calibrer)
        else etat.text = getString(R.string.boussole_a_plat)
    }

    /**
     * La rose, dessinée sur une grille de 240 unités comme le SVG du web : graduations aux 5°,
     * degrés aux 30°, points cardinaux, zone dorée de ±5° autour de Jérusalem, aiguille et moyeu.
     */
    private class Cadran(context: Context) : View(context) {
        var cap = 0f
        var cible = Float.NaN
        var aligne = false
        var cardinaux: Array<String> = arrayOf("N", "E", "S", "O")
        private val encre = Apparence.couleur(context, R.color.ink)
        private val pale = Apparence.couleur(context, R.color.muted)
        private val marque = Apparence.couleur(context, R.color.brand)
        private val papier = Apparence.couleur(context, R.color.paper)
        private val or = Apparence.couleur(context, R.color.boussole_or)
        private val trait = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND }
        private val plein = Paint(Paint.ANTI_ALIAS_FLAG)
        private val lettres = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER }
        private val chemin = Path()
        private val arc = RectF()

        init {
            importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
        }

        override fun onDraw(canvas: Canvas) {
            val u = min(width, height) / 240f
            val c = 120 * u
            val r = 104 * u
            fun x(g: Float, rr: Float) = c + rr * sin(Math.toRadians(g.toDouble())).toFloat()
            fun y(g: Float, rr: Float) = c - rr * cos(Math.toRadians(g.toDouble())).toFloat()

            // L'éclat quand on fait face : un anneau d'or et un halo qui s'estompe.
            if (aligne) {
                trait.style = Paint.Style.STROKE
                for (i in 6 downTo 1) {
                    trait.strokeWidth = (4 + i * 3) * u
                    trait.color = ColorUtils.setAlphaComponent(or, 14)
                    canvas.drawCircle(c, c, r + 12 * u, trait)
                }
                trait.strokeWidth = 2.5f * u
                trait.color = or
                canvas.drawCircle(c, c, r + 13 * u, trait)
            }
            plein.color = ColorUtils.blendARGB(papier, if (aligne) or else encre, if (aligne) 0.14f else 0.04f)
            canvas.drawCircle(c, c, r + 10 * u, plein)
            trait.strokeWidth = 1 * u
            trait.color = ColorUtils.setAlphaComponent(encre, 30)
            canvas.drawCircle(c, c, r + 10 * u, trait)

            canvas.save()
            canvas.rotate(-cap, c, c)
            trait.color = ColorUtils.setAlphaComponent(encre, 77)
            canvas.drawCircle(c, c, r, trait)
            if (!cible.isNaN()) {
                trait.strokeWidth = 8 * u
                trait.color = ColorUtils.setAlphaComponent(or, 90)
                val rz = r - 6 * u
                arc.set(c - rz, c - rz, c + rz, c + rz)
                canvas.drawArc(arc, cible - 95f, 10f, false, trait)
            }
            for (g in 0 until 360 step 5) {
                val fort = g % 30 == 0
                val l = (if (fort) 12 else if (g % 10 == 0) 7 else 4) * u
                trait.strokeWidth = (if (fort) 1.6f else 0.8f) * u
                trait.color = if (fort) encre else ColorUtils.setAlphaComponent(encre, 102)
                val gf = g.toFloat()
                canvas.drawLine(x(gf, r), y(gf, r), x(gf, r - l), y(gf, r - l), trait)
            }
            lettres.typeface = Typeface.DEFAULT
            lettres.textSize = 8.5f * u
            lettres.color = pale
            for (g in 30 until 360 step 30) {
                if (g % 90 == 0) continue
                val gf = g.toFloat()
                canvas.save()
                canvas.rotate(gf, x(gf, r - 24 * u), y(gf, r - 24 * u))
                canvas.drawText(g.toString(), x(gf, r - 24 * u), y(gf, r - 24 * u) + 3 * u, lettres)
                canvas.restore()
            }
            lettres.typeface = Typeface.DEFAULT_BOLD
            lettres.textSize = 16 * u
            for (i in 0 until 4) {
                val gf = i * 90f
                lettres.color = if (i == 0) marque else encre
                canvas.save()
                canvas.rotate(gf, x(gf, r - 30 * u), y(gf, r - 30 * u))
                canvas.drawText(cardinaux.getOrElse(i) { "" }, x(gf, r - 30 * u), y(gf, r - 30 * u) + 6 * u, lettres)
                canvas.restore()
            }
            if (!cible.isNaN()) {
                canvas.rotate(cible, c, c)
                trait.strokeWidth = 3 * u
                trait.color = ColorUtils.setAlphaComponent(encre, 90)
                canvas.drawLine(c, c + 8 * u, c, c + 34 * u, trait)
                chemin.reset()
                chemin.moveTo(c, c - r + 16 * u)
                chemin.lineTo(c - 9 * u, c + 2 * u)
                chemin.lineTo(c, c - 6 * u)
                chemin.lineTo(c + 9 * u, c + 2 * u)
                chemin.close()
                plein.color = or
                canvas.drawPath(chemin, plein)
            }
            canvas.restore()

            // Le repère fixe du haut : la direction où pointe le téléphone.
            plein.color = if (aligne) or else marque
            chemin.reset()
            chemin.moveTo(c, 4 * u)
            chemin.lineTo(c - 7 * u, 16 * u)
            chemin.lineTo(c + 7 * u, 16 * u)
            chemin.close()
            canvas.drawPath(chemin, plein)
            plein.color = or
            canvas.drawCircle(c, c, 6 * u, plein)
            plein.color = papier
            canvas.drawCircle(c, c, 2.5f * u, plein)
        }
    }

    companion object {
        fun ouvrir(c: Context) = c.startActivity(Intent(c, BoussoleActivity::class.java))
    }
}
