package com.myhitbodedut

import android.Manifest
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.text.format.DateFormat
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.materialswitch.MaterialSwitch
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * Le premier lancement. Un écran à la fois, et une sortie à tout moment.
 *
 * L'indice est gardé à travers une rotation : reprendre au premier écran
 * parce qu'on a tourné le téléphone serait une punition.
 */
class OnboardingActivity : AppCompatActivity() {

    private var index = 0

    private val demandeNotifications =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { render() }

    private val boutonsDuree by lazy {
        listOf(R.id.onboarding_duree_0, R.id.onboarding_duree_1, R.id.onboarding_duree_2, R.id.onboarding_duree_3)
            .map { findViewById<MaterialButton>(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding)
        index = savedInstanceState?.getInt(ETAT_INDEX, 0) ?: 0

        findViewById<Button>(R.id.onboarding_skip).setOnClickListener { finish() }
        findViewById<MaterialButton>(R.id.onboarding_tsadik).setOnClickListener {
            choisir(Adresse.HOMME)
        }
        findViewById<MaterialButton>(R.id.onboarding_tsadeket).setOnClickListener {
            choisir(Adresse.FEMME)
        }
        // La durée habituelle (16/10) : celle du bouton « Commencer » de l'accueil.
        boutonsDuree.forEachIndexed { i, b -> b.text = Store.formatDuration(this, Onboarding.DUREES[i]) }
        findViewById<MaterialButtonToggleGroup>(R.id.onboarding_durees).addOnButtonCheckedListener { _, id, coche ->
            if (coche) Store.setTargetMinutes(this, Onboarding.DUREES[boutonsDuree.indexOfFirst { it.id == id }])
        }
        // Le rappel quotidien (16/10) : facultatif ; l'activer demande les notifications.
        findViewById<MaterialSwitch>(R.id.onboarding_rappel_switch).setOnCheckedChangeListener { v, coche ->
            if (!v.isPressed && coche == Store.reminderEnabled(this)) return@setOnCheckedChangeListener
            Store.setReminderEnabled(this, coche)
            if (coche) demanderNotifications()
            Reminder.reschedule(this)
            render()
        }
        findViewById<Button>(R.id.onboarding_rappel_heure).setOnClickListener { choisirHeure() }
        findViewById<Button>(R.id.onboarding_back).setOnClickListener {
            index = Onboarding.precedent(index)
            render()
        }
        findViewById<Button>(R.id.onboarding_next).setOnClickListener {
            if (Onboarding.estDernier(index)) finish()
            else {
                index = Onboarding.suivant(index)
                render()
            }
        }
        render()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(ETAT_INDEX, index)
    }

    /**
     * Quelle que soit la sortie — le bouton de fin, « passer », ou le
     * retour arrière — l'accueil a été vu et ne revient plus.
     */
    override fun finish() {
        Store.setOnboarded(this)
        super.finish()
    }

    private fun choisir(adresse: Adresse) {
        Store.setAdresse(this, adresse)
        render()
    }

    private fun demanderNotifications() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED) return
        demandeNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    /** La première heure de rappel ; les autres se règlent dans les réglages. */
    private fun choisirHeure() {
        val heures = Store.reminderTimes(this)
        val depart = heures.firstOrNull() ?: Store.DEFAULT_REMINDER
        TimePickerDialog(this, { _, h, m ->
            Store.setReminderTimes(this, listOf(h * 60 + m) + heures.drop(1))
            Store.setReminderEnabled(this, true)
            demanderNotifications()
            Reminder.reschedule(this)
            render()
        }, (depart / 60) % 24, depart % 60, DateFormat.is24HourFormat(this)).show()
    }

    private fun heure(minutes: Int): String =
        LocalTime.of((minutes / 60) % 24, minutes % 60)
            .format(DateTimeFormatter.ofPattern(if (DateFormat.is24HourFormat(this)) "HH:mm" else "h:mm a"))

    private fun render() {
        val ecran = Onboarding.ecran(index)
        findViewById<View>(R.id.onboarding_durees).visibility =
            if (ecran.choixDuree) View.VISIBLE else View.GONE
        val cible = Store.targetMinutes(this)
        boutonsDuree.forEachIndexed { i, b -> b.isChecked = Onboarding.DUREES[i] == cible }
        findViewById<View>(R.id.onboarding_rappel).visibility =
            if (ecran.choixRappel) View.VISIBLE else View.GONE
        findViewById<MaterialSwitch>(R.id.onboarding_rappel_switch).isChecked = Store.reminderEnabled(this)
        findViewById<Button>(R.id.onboarding_rappel_heure).apply {
            val premiere = Store.reminderTimes(this@OnboardingActivity).firstOrNull() ?: Store.DEFAULT_REMINDER
            text = getString(R.string.onboarding_rappel_heure, heure(premiere))
            contentDescription = getString(R.string.onboarding_rappel_heure_desc, heure(premiere))
        }
        findViewById<View>(R.id.onboarding_adresse).visibility =
            if (ecran.choixAdresse) View.VISIBLE else View.GONE
        val adresse = Store.adresse(this)
        findViewById<MaterialButton>(R.id.onboarding_tsadik).isChecked = adresse == Adresse.HOMME
        findViewById<MaterialButton>(R.id.onboarding_tsadeket).isChecked = adresse == Adresse.FEMME
        findViewById<ImageView>(R.id.onboarding_art).setImageResource(ecran.illustration)
        findViewById<TextView>(R.id.onboarding_title).setText(ecran.titre)
        findViewById<TextView>(R.id.onboarding_body).setText(ecran.corps)
        findViewById<TextView>(R.id.onboarding_step).text =
            getString(R.string.onboarding_step, index + 1, Onboarding.ECRANS.size)

        findViewById<Button>(R.id.onboarding_back).visibility =
            if (Onboarding.estPremier(index)) View.INVISIBLE else View.VISIBLE
        findViewById<Button>(R.id.onboarding_next).setText(
            if (Onboarding.estDernier(index)) R.string.onboarding_start
            else R.string.onboarding_next
        )
        findViewById<Button>(R.id.onboarding_skip).visibility =
            if (Onboarding.estDernier(index)) View.INVISIBLE else View.VISIBLE
    }

    companion object {
        private const val ETAT_INDEX = "index"
    }
}
