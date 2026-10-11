package com.myhitbodedut

import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.time.LocalDate

/**
 * L'onglet Zmanim (demande du 10/10) : la date hébraïque du jour, ce qu'elle
 * change aux prières, le bouton du calendrier, puis tous les horaires de la
 * journée, chacun avec sa méthode (ZMANIM.md). Le lieu se choisit sur le
 * bandeau ; l'allumage des bougies, en bas de page.
 */
class ZmanimActivity : AppCompatActivity() {

    private val choixLieu = ChoixLieu(this) { afficher() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_zmanim)
        findViewById<TextView>(R.id.zmanim_lieu).setOnClickListener { choixLieu.montrer() }
    }

    override fun onResume() {
        super.onResume()
        // Le jour a pu changer, et le lieu avec les réglages.
        afficher()
    }

    private fun afficher() {
        val israel = Store.inIsrael(this)
        val lieu = Store.lieuZmanim(this)
        findViewById<TextView>(R.id.zmanim_lieu).text = if (lieu == null) getString(R.string.zmanim_choisir_lieu)
        else getString(R.string.zmanim_lieu_detail, lieu.nom, getString(if (israel) R.string.region_israel else R.string.region_diaspora))
        val aujourdhui = LocalDate.now()
        val contenu = findViewById<LinearLayout>(R.id.zmanim_contenu)
        contenu.removeAllViews()
        contenu.addView(VueJour.carteJour(this, aujourdhui, israel))
        contenu.addView(MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
            text = getString(R.string.zmanim_calendrier)
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                .apply { setMargins(dp(16), dp(12), dp(16), 0) }
            setOnClickListener { startActivity(Intent(this@ZmanimActivity, CalendrierActivity::class.java)) }
        })
        contenu.addView(VueJour.horaires(this, aujourdhui, israel) { choixLieu.montrer() })
        contenu.addView(VueJour.carte(this).apply {
            addView(VueJour.texte(this@ZmanimActivity, getString(R.string.reglage_allumage), 15f, R.color.ink))
            addView(VueJour.texte(this@ZmanimActivity, libelleAllumage(Store.allumage(this@ZmanimActivity)),
                13f, R.color.muted, haut = 2))
            // À Jérusalem, l'usage est d'allumer 40 minutes avant : proposé, jamais imposé.
            if (Store.mukafChoma(this@ZmanimActivity) && Store.allumage(this@ZmanimActivity) != ALLUMAGE_JERUSALEM) {
                addView(VueJour.texte(this@ZmanimActivity, getString(R.string.reglage_allumage_proposition), 13f, R.color.brand, haut = 2))
            }
            foreground = getDrawable(android.R.drawable.list_selector_background)
            setOnClickListener { choisirAllumage() }
        })
        // Ta'hanoun en fin de Tichri : au choix en Sfard seulement.
        if (Store.siddurNusah(this).cle == "sefard") {
            contenu.addView(VueJour.carte(this).apply {
                addView(VueJour.texte(this@ZmanimActivity, getString(R.string.reglage_tahanoun_tichri), 15f, R.color.ink))
                addView(VueJour.texte(this@ZmanimActivity, getString(if (Store.sansTahanounFinTichri(this@ZmanimActivity))
                    R.string.reglage_tahanoun_tichri_sans else R.string.reglage_tahanoun_tichri_dit), 13f, R.color.muted, haut = 2))
                foreground = getDrawable(android.R.drawable.list_selector_background)
                setOnClickListener { choisirTahanounTichri() }
            })
        }
        contenu.addView(VueJour.pied(this))
    }

    private fun choisirAllumage() {
        val options = Zmanim.ALLUMAGES
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.reglage_allumage)
            .setSingleChoiceItems(options.map { libelleAllumage(it) }.toTypedArray(),
                options.indexOf(Store.allumage(this))) { d, i ->
                Store.setAllumage(this, options[i])
                d.dismiss()
                afficher()
            }
            .show()
    }

    /** « 40 min avant la chki'a · coutume de Jérusalem », quand Jérusalem est choisie. */
    private fun libelleAllumage(minutes: Int): String =
        if (minutes == ALLUMAGE_JERUSALEM && Store.mukafChoma(this)) getString(R.string.reglage_allumage_jerusalem, minutes)
        else getString(R.string.reglage_allumage_valeur, minutes)

    private fun choisirTahanounTichri() {
        val sans = Store.sansTahanounFinTichri(this)
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.reglage_tahanoun_tichri)
            .setSingleChoiceItems(arrayOf(getString(R.string.reglage_tahanoun_tichri_dit), getString(R.string.reglage_tahanoun_tichri_sans)),
                if (sans) 1 else 0) { d, i ->
                Store.setSansTahanounFinTichriSfard(this, i == 1)
                d.dismiss()
                afficher()
            }
            .show()
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private companion object {
        const val ALLUMAGE_JERUSALEM = 40
    }
}
