package com.myhitbodedut

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton

/**
 * Le premier lancement. Un écran à la fois, et une sortie à tout moment.
 *
 * L'indice est gardé à travers une rotation : reprendre au premier écran
 * parce qu'on a tourné le téléphone serait une punition.
 */
class OnboardingActivity : AppCompatActivity() {

    private var index = 0

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
        Store.setOnboarded(this, true)
        super.finish()
    }

    private fun choisir(adresse: Adresse) {
        Store.setAdresse(this, adresse)
        render()
    }

    private fun render() {
        val ecran = Onboarding.ecran(index)
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
