package com.myhitbodedut

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Typeface
import android.hardware.GeomagneticField
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.ColorUtils
import com.google.android.material.button.MaterialButton
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * La boussole de la tefila (28/10) : vers le mont du Temple depuis le lieu des zmanim
 * (DirectionTefila). Le téléphone à plat, le cadran tourne avec lui : le nord reste au
 * nord, la flèche dorée montre Jérusalem. Le cap magnétique du capteur est corrigé de la
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
        cap = TextView(this).apply { textSize = 22f; gravity = Gravity.CENTER; setTypeface(typeface, Typeface.BOLD)
            setTextColor(Apparence.couleur(context, R.color.ink)) }
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
                val cote = min(resources.displayMetrics.widthPixels - dp(48), dp(320))
                addView(cadran, LinearLayout.LayoutParams(cote, cote).apply { topMargin = dp(20) })
                addView(cap, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(16) })
                addView(loxoTexte, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(2) })
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
            cadran.cible = Float.NaN
            cadran.invalidate()
            return
        }
        val ortho = DirectionTefila.orthodromie(lieu.latitude, lieu.longitude)
        val loxo = DirectionTefila.loxodromie(lieu.latitude, lieu.longitude)
        val km = DirectionTefila.distanceKm(lieu.latitude, lieu.longitude)
        lieuTexte.text = getString(R.string.boussole_lieu, lieu.nom, km.roundToInt())
        val points = resources.getStringArray(R.array.boussole_points)
        cap.text = getString(R.string.boussole_cap, ortho.roundToInt(), points[DirectionTefila.octant(ortho)])
        loxoTexte.text = getString(R.string.boussole_loxodromie, loxo.roundToInt(), points[DirectionTefila.octant(loxo)])
        cadran.cible = ortho.toFloat()
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
        cadran.invalidate()
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        if (accuracy <= SensorManager.SENSOR_STATUS_ACCURACY_LOW) etat.text = getString(R.string.boussole_calibrer)
        else etat.text = getString(R.string.boussole_a_plat)
    }

    /** Le cadran : couronne graduée qui tourne avec le téléphone, flèche dorée vers Jérusalem. */
    private class Cadran(context: Context) : View(context) {
        var cap = 0f
        var cible = Float.NaN
        private val d = resources.displayMetrics.density
        private val encre = Apparence.couleur(context, R.color.ink)
        private val marque = Apparence.couleur(context, R.color.brand)
        private val dore = 0xFFC9A227.toInt()
        private val trait = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; color = ColorUtils.setAlphaComponent(encre, 0x55) }
        private val plein = Paint(Paint.ANTI_ALIAS_FLAG)
        private val lettres = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER; typeface = Typeface.DEFAULT_BOLD }
        private val chemin = Path()

        init {
            importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
        }

        override fun onDraw(canvas: Canvas) {
            val cx = width / 2f
            val cy = height / 2f
            val r = min(cx, cy) - 8 * d
            canvas.save()
            canvas.rotate(-cap, cx, cy)
            trait.strokeWidth = 2 * d
            canvas.drawCircle(cx, cy, r, trait)
            for (g in 0 until 360 step 15) {
                val long = if (g % 90 == 0) 14 * d else if (g % 45 == 0) 9 * d else 5 * d
                val a = Math.toRadians(g.toDouble())
                trait.strokeWidth = if (g % 90 == 0) 2.5f * d else 1.2f * d
                canvas.drawLine(cx + (r * sin(a)).toFloat(), cy - (r * cos(a)).toFloat(),
                    cx + ((r - long) * sin(a)).toFloat(), cy - ((r - long) * cos(a)).toFloat(), trait)
            }
            lettres.textSize = 16 * d
            listOf("N" to 0, "E" to 90, "S" to 180, "O" to 270).forEach { (l, g) ->
                lettres.color = if (g == 0) marque else encre
                val a = Math.toRadians(g.toDouble())
                val rr = r - 30 * d
                canvas.drawText(l, cx + (rr * sin(a)).toFloat(), cy - (rr * cos(a)).toFloat() + 6 * d, lettres)
            }
            if (!cible.isNaN()) {
                canvas.rotate(cible, cx, cy)
                chemin.reset()
                chemin.moveTo(cx, cy - r + 18 * d)
                chemin.lineTo(cx - 14 * d, cy + 10 * d)
                chemin.lineTo(cx, cy)
                chemin.lineTo(cx + 14 * d, cy + 10 * d)
                chemin.close()
                plein.color = dore
                canvas.drawPath(chemin, plein)
            }
            canvas.restore()
            // Le repère fixe du haut : la direction où pointe le téléphone.
            plein.color = marque
            chemin.reset()
            chemin.moveTo(cx, 0f)
            chemin.lineTo(cx - 7 * d, 10 * d)
            chemin.lineTo(cx + 7 * d, 10 * d)
            chemin.close()
            canvas.drawPath(chemin, plein)
            plein.color = ColorUtils.setAlphaComponent(encre, 0x99)
            canvas.drawCircle(cx, cy, 4 * d, plein)
        }
    }

    companion object {
        fun ouvrir(c: Context) = c.startActivity(Intent(c, BoussoleActivity::class.java))
    }
}
