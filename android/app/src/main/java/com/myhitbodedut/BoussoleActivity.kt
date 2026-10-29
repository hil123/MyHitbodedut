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
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.text.NumberFormat
import java.util.Locale

/**
 * La boussole de Jérusalem (28/10, design C « le hassid se tourne ») depuis le
 * lieu des zmanim. Le cap vient du capteur d'orientation (nord magnétique),
 * corrigé de la déclinaison du lieu (GeomagneticField) pour le nord vrai, comme
 * les deux directions. Sans capteur, la rose orientée au nord. Rien ne passe par
 * le réseau. « Position exacte » demande la localisation précise (ChoixLieu) ; à
 * l'ouverture, la position du téléphone se reprend sans bruit si elle est déjà permise.
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
        choixLieu.rafraichir()
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
        // Sans lieu, l'état vide propose le choix : pas de seconde pastille dans le bandeau (29/10).
        findViewById<TextView>(R.id.boussole_lieu).apply {
            visibility = if (lieu == null) android.view.View.GONE else android.view.View.VISIBLE
            text = lieu?.nom?.ifEmpty { null } ?: getString(R.string.zmanim_choisir_lieu)
        }
        val contenu = findViewById<LinearLayout>(R.id.boussole_contenu)
        contenu.removeAllViews()
        boussole = BoussoleView(this).apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                .apply { setMargins(dp(16), dp(16), dp(16), 0) }
            setBackgroundResource(R.drawable.bg_stage)
            clipToOutline = true
            if (lieu == null) setOnClickListener { choixLieu.montrer() }
            roseDesVents = Store.boussoleRose(this@BoussoleActivity)
        }
        if (lieu == null) {
            aLieu = false
            boussole.directions(null, null)
            contenu.addView(etatVide())
            contenu.addView(VueJour.texte(this, getString(R.string.zmanim_source_villes), 12f, R.color.muted, haut = 16).apply { setPadding(dp(16), 0, dp(16), 0) })
            return
        }
        contenu.addView(boussole)
        precision = VueJour.texte(this, getString(R.string.boussole_calibrer), 13f, R.color.warning, haut = 8)
            .apply { visibility = android.view.View.GONE; setPadding(dp(16), 0, dp(16), 0) }
        contenu.addView(precision)
        val avecCapteur = aBoussole()
        contenu.addView(VueJour.texte(this, getString(if (avecCapteur) R.string.boussole_a_plat else R.string.boussole_sans_capteur),
            13f, R.color.muted, haut = 8).apply { setPadding(dp(16), 0, dp(16), 0) })
        contenu.addView(MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
            text = getString(R.string.boussole_position_exacte)
            setOnClickListener { choixLieu.positionExacte() }
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                .apply { setMargins(dp(16), dp(8), dp(16), 0) }
        })
        aLieu = true

        declinaison = GeomagneticField(lieu.latitude.toFloat(), lieu.longitude.toFloat(), lieu.altitude.toFloat(),
            System.currentTimeMillis()).declination
        val (principale, autre) = DirectionJerusalem.directions(lieu.latitude, lieu.longitude)
        boussole.directions(principale, autre)
        boussole.cap(lisse)

        val gc = DirectionJerusalem.grandCercle(lieu.latitude, lieu.longitude)
        val rh = DirectionJerusalem.carte(lieu.latitude, lieu.longitude)
        val points = resources.getStringArray(R.array.boussole_points)
        val km = NumberFormat.getIntegerInstance(Locale.forLanguageTag(ChizukCard.language(this)))
            .format(Math.round(DirectionJerusalem.distanceKm(lieu.latitude, lieu.longitude)))
        contenu.addView(VueJour.carte(this).apply {
            addView(VueJour.texte(this@BoussoleActivity, getString(R.string.boussole_grand_cercle, DirectionJerusalem.degres(gc),
                points[DirectionJerusalem.point(gc)]), 15f, R.color.brand, gras = true))
            addView(VueJour.texte(this@BoussoleActivity, getString(R.string.boussole_cap_constant, DirectionJerusalem.degres(rh),
                points[DirectionJerusalem.point(rh)]), 15f, R.color.ink, haut = 6))
            addView(VueJour.texte(this@BoussoleActivity, getString(R.string.boussole_distance, km), 13f, R.color.muted, haut = 6))
            addView(VueJour.texte(this@BoussoleActivity, getString(R.string.boussole_legende), 13f, R.color.muted, haut = 6))
        })

        contenu.addView(VueJour.carte(this).apply {
            addView(VueJour.texte(this@BoussoleActivity, getString(R.string.boussole_affichage), 15f, R.color.ink))
            addView(VueJour.texte(this@BoussoleActivity, getString(if (Store.boussoleRose(this@BoussoleActivity))
                R.string.boussole_affichage_rose else R.string.boussole_affichage_hassid), 13f, R.color.muted, haut = 2))
            foreground = getDrawable(android.R.drawable.list_selector_background)
            setOnClickListener { choisirAffichage() }
        })

        contenu.addView(VueJour.carte(this).apply {
            addView(VueJour.texte(this@BoussoleActivity, getString(R.string.boussole_note_titre), 15f, R.color.ink))
            addView(VueJour.texte(this@BoussoleActivity, getString(R.string.boussole_note), 13f, R.color.muted, haut = 4))
            addView(VueJour.texte(this@BoussoleActivity, HtmlCompat.fromHtml(getString(R.string.boussole_source,
                "<a href=\"$SOURCE\">${android.text.TextUtils.htmlEncode(getString(R.string.boussole_source_titre))}</a>"), HtmlCompat.FROM_HTML_MODE_LEGACY),
                13f, R.color.brand, haut = 6).apply { movementMethod = LinkMovementMethod.getInstance() })
        })
        contenu.addView(VueJour.texte(this, getString(R.string.zmanim_source_villes), 12f, R.color.muted, haut = 16).apply { setPadding(dp(16), 0, dp(16), 0) })
    }

    /** Sans lieu (29/10) : le hassid, une question, la position en un toucher, ou une ville. */
    private fun etatVide() = VueJour.carte(this).apply {
        gravity = android.view.Gravity.CENTER_HORIZONTAL
        addView(android.widget.ImageView(this@BoussoleActivity).apply {
            setImageResource(R.drawable.char_standing_back)
            setBackgroundResource(R.drawable.bg_stage)
            setPadding(dp(6), dp(6), dp(6), dp(6))
            contentDescription = null
            layoutParams = LinearLayout.LayoutParams(dp(96), dp(116)).apply { topMargin = dp(4) }
        })
        addView(VueJour.texte(this@BoussoleActivity, getString(R.string.boussole_vide_titre), 20f, R.color.ink, gras = true, haut = 8).apply { gravity = android.view.Gravity.CENTER })
        addView(VueJour.texte(this@BoussoleActivity, getString(R.string.boussole_carte_sous), 14f, R.color.muted, haut = 4).apply { gravity = android.view.Gravity.CENTER })
        addView(MaterialButton(this@BoussoleActivity).apply {
            text = getString(R.string.boussole_vide_position)
            setOnClickListener { choixLieu.positionExacte() }
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(16) }
        })
        addView(MaterialButton(this@BoussoleActivity, null, com.google.android.material.R.attr.borderlessButtonStyle).apply {
            text = android.text.SpannableString(getString(R.string.boussole_vide_ville)).apply { setSpan(android.text.style.UnderlineSpan(), 0, length, 0) }
            setTextColor(Apparence.couleur(this@BoussoleActivity, R.color.muted))
            setOnClickListener { choixLieu.montrer() }
        })
    }

    /** Le hassid (design C, par défaut) ou la rose des vents. */
    private fun choisirAffichage() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.boussole_affichage)
            .setSingleChoiceItems(arrayOf(getString(R.string.boussole_affichage_hassid), getString(R.string.boussole_affichage_rose)),
                if (Store.boussoleRose(this)) 1 else 0) { d, i ->
                Store.setBoussoleRose(this, i == 1)
                d.dismiss()
                afficher()
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
        /** L'article qui expose les deux calculs (vérifié le 28/10). */
        const val SOURCE = "https://www.chabad.org/library/article_cdo/aid/3502321/jewish/Why-Do-We-Face-East-When-Praying-Or-Do-We.htm"

        /** Un filtre doux sur le cercle : un cinquième du chemin à chaque mesure, par le plus court côté. */
        fun lisser(avant: Double?, mesure: Double): Double =
            avant?.let { DirectionJerusalem.norm(it + DirectionJerusalem.ecart(mesure, it) * 0.2) } ?: mesure
    }
}
