package com.myhitbodedut

import android.content.Context
import android.hardware.GeomagneticField
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.Surface
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.HtmlCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.text.NumberFormat
import java.util.Locale

/**
 * La boussole de Jérusalem (28/10, design C « le hassid se tourne ») depuis le
 * lieu des zmanim. Le cap vient du capteur d'orientation (nord magnétique),
 * corrigé de la déclinaison du lieu (GeomagneticField) pour le nord vrai, comme
 * les deux directions. Sans capteur, la rose orientée au nord. Rien ne passe par
 * le réseau ; aucune autorisation n'est demandée.
 */
class BoussoleActivity : AppCompatActivity(), SensorEventListener {

    private val choixLieu = ChoixLieu(this) { afficher() }
    private lateinit var boussole: BoussoleView
    private lateinit var precision: TextView
    private var capteurs: SensorManager? = null
    private var declinaison = 0f
    private var lisse: Double? = null
    private val gravite = FloatArray(3)
    private val champ = FloatArray(3)
    private var aGravite = false
    private var aChamp = false
    private var aLieu = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_boussole)
        findViewById<TextView>(R.id.boussole_lieu).setOnClickListener { choixLieu.montrer() }
        capteurs = getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    }

    override fun onResume() {
        super.onResume()
        afficher()
        val sm = capteurs ?: return
        val rotation = sm.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        if (rotation != null) {
            sm.registerListener(this, rotation, SensorManager.SENSOR_DELAY_UI)
        } else {
            sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.let { sm.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
            sm.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)?.let { sm.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
        }
    }

    override fun onPause() {
        super.onPause()
        capteurs?.unregisterListener(this)
        BoussoleWidget.refresh(this)
    }

    /** Le téléphone a-t-il de quoi trouver le nord ? */
    private fun aBoussole(): Boolean {
        val sm = capteurs ?: return false
        return sm.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) != null ||
            (sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null && sm.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD) != null)
    }

    private fun afficher() {
        val lieu = Store.lieuZmanim(this)
        findViewById<TextView>(R.id.boussole_lieu).text = lieu?.nom?.ifEmpty { null } ?: getString(R.string.zmanim_choisir_lieu)
        val contenu = findViewById<LinearLayout>(R.id.boussole_contenu)
        contenu.removeAllViews()
        boussole = BoussoleView(this).apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                .apply { setMargins(dp(16), dp(16), dp(16), 0) }
            setBackgroundResource(R.drawable.bg_stage)
            clipToOutline = true
            if (lieu == null) setOnClickListener { choixLieu.montrer() }
        }
        contenu.addView(boussole)
        precision = VueJour.texte(this, getString(R.string.boussole_calibrer), 13f, R.color.warning, haut = 8)
            .apply { visibility = android.view.View.GONE; setPadding(dp(16), 0, dp(16), 0) }
        contenu.addView(precision)
        val avecCapteur = aBoussole()
        contenu.addView(VueJour.texte(this, getString(if (avecCapteur) R.string.boussole_a_plat else R.string.boussole_sans_capteur),
            13f, R.color.muted, haut = 8).apply { setPadding(dp(16), 0, dp(16), 0) })
        aLieu = lieu != null
        if (lieu == null) {
            boussole.directions(null, null)
            contenu.addView(VueJour.pied(this))
            return
        }

        declinaison = GeomagneticField(lieu.latitude.toFloat(), lieu.longitude.toFloat(), lieu.altitude.toFloat(),
            System.currentTimeMillis()).declination
        val carte = Store.boussoleCarte(this)
        val (principale, autre) = DirectionJerusalem.directions(lieu.latitude, lieu.longitude, carte)
        boussole.directions(principale, autre)
        boussole.cap(lisse)

        val gc = DirectionJerusalem.grandCercle(lieu.latitude, lieu.longitude)
        val rh = DirectionJerusalem.carte(lieu.latitude, lieu.longitude)
        val points = resources.getStringArray(R.array.boussole_points)
        val km = NumberFormat.getIntegerInstance(Locale.forLanguageTag(ChizukCard.language(this)))
            .format(Math.round(DirectionJerusalem.distanceKm(lieu.latitude, lieu.longitude)))
        contenu.addView(VueJour.carte(this).apply {
            addView(VueJour.texte(this@BoussoleActivity, getString(R.string.boussole_grand_cercle, DirectionJerusalem.degres(gc),
                points[DirectionJerusalem.point(gc)]), 15f, if (carte) R.color.ink else R.color.brand, gras = !carte))
            addView(VueJour.texte(this@BoussoleActivity, getString(R.string.boussole_cap_constant, DirectionJerusalem.degres(rh),
                points[DirectionJerusalem.point(rh)]), 15f, if (carte) R.color.brand else R.color.ink, gras = carte, haut = 6))
            addView(VueJour.texte(this@BoussoleActivity, getString(R.string.boussole_distance, km), 13f, R.color.muted, haut = 6))
            addView(VueJour.texte(this@BoussoleActivity, getString(R.string.boussole_legende), 13f, R.color.muted, haut = 6))
        })

        contenu.addView(VueJour.carte(this).apply {
            addView(VueJour.texte(this@BoussoleActivity, getString(R.string.boussole_principale), 15f, R.color.ink))
            addView(VueJour.texte(this@BoussoleActivity, getString(if (carte) R.string.boussole_principale_carte else R.string.boussole_principale_gc),
                13f, R.color.muted, haut = 2))
            foreground = getDrawable(android.R.drawable.list_selector_background)
            setOnClickListener { choisirPrincipale() }
        })

        contenu.addView(VueJour.carte(this).apply {
            addView(VueJour.texte(this@BoussoleActivity, getString(R.string.boussole_note_titre), 15f, R.color.ink))
            addView(VueJour.texte(this@BoussoleActivity, getString(R.string.boussole_note), 13f, R.color.muted, haut = 4))
            addView(VueJour.texte(this@BoussoleActivity, HtmlCompat.fromHtml(getString(R.string.boussole_source), HtmlCompat.FROM_HTML_MODE_LEGACY),
                13f, R.color.brand, haut = 6).apply { movementMethod = LinkMovementMethod.getInstance() })
        })
        contenu.addView(VueJour.pied(this))
    }

    private fun choisirPrincipale() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.boussole_principale)
            .setSingleChoiceItems(arrayOf(getString(R.string.boussole_principale_gc), getString(R.string.boussole_principale_carte)),
                if (Store.boussoleCarte(this)) 1 else 0) { d, i ->
                Store.setBoussoleCarte(this, i == 1)
                d.dismiss()
                afficher()
                BoussoleWidget.refresh(this)
            }
            .show()
    }

    override fun onSensorChanged(event: SensorEvent) {
        val matrice = FloatArray(9)
        when (event.sensor.type) {
            Sensor.TYPE_ROTATION_VECTOR -> SensorManager.getRotationMatrixFromVector(matrice, event.values)
            Sensor.TYPE_ACCELEROMETER, Sensor.TYPE_MAGNETIC_FIELD -> {
                if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) { event.values.copyInto(gravite, 0, 0, 3); aGravite = true }
                else { event.values.copyInto(champ, 0, 0, 3); aChamp = true }
                if (!aGravite || !aChamp || !SensorManager.getRotationMatrix(matrice, null, gravite, champ)) return
            }
            else -> return
        }
        // Le haut de l'écran, quelle que soit la rotation de l'affichage.
        val rotation = if (android.os.Build.VERSION.SDK_INT >= 30) display?.rotation
            else @Suppress("DEPRECATION") windowManager.defaultDisplay.rotation
        val (x, y) = when (rotation ?: Surface.ROTATION_0) {
            Surface.ROTATION_90 -> SensorManager.AXIS_Y to SensorManager.AXIS_MINUS_X
            Surface.ROTATION_180 -> SensorManager.AXIS_MINUS_X to SensorManager.AXIS_MINUS_Y
            Surface.ROTATION_270 -> SensorManager.AXIS_MINUS_Y to SensorManager.AXIS_X
            else -> SensorManager.AXIS_X to SensorManager.AXIS_Y
        }
        val remappee = FloatArray(9)
        SensorManager.remapCoordinateSystem(matrice, x, y, remappee)
        val orientation = FloatArray(3)
        SensorManager.getOrientation(remappee, orientation)
        val vrai = DirectionJerusalem.norm(Math.toDegrees(orientation[0].toDouble()) + declinaison)
        lisse = lisser(lisse, vrai)
        if (::boussole.isInitialized && aLieu) boussole.cap(lisse)
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        if (sensor.type != Sensor.TYPE_ROTATION_VECTOR && sensor.type != Sensor.TYPE_MAGNETIC_FIELD) return
        if (!::precision.isInitialized) return
        precision.visibility = if (accuracy <= SensorManager.SENSOR_STATUS_ACCURACY_LOW) android.view.View.VISIBLE else android.view.View.GONE
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    companion object {
        /** Un filtre doux sur le cercle : un cinquième du chemin à chaque mesure, par le plus court côté. */
        fun lisser(avant: Double?, mesure: Double): Double =
            avant?.let { DirectionJerusalem.norm(it + DirectionJerusalem.ecart(mesure, it) * 0.2) } ?: mesure
    }
}
