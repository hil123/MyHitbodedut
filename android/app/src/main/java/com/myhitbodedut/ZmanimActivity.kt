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
            addView(VueJour.texte(this@ZmanimActivity, getString(R.string.reglage_allumage_valeur, Store.allumage(this@ZmanimActivity)),
                13f, R.color.muted, haut = 2))
            foreground = getDrawable(android.R.drawable.list_selector_background)
            setOnClickListener { choisirAllumage() }
        })
        contenu.addView(VueJour.pied(this))
    }

    private fun choisirAllumage() {
        val options = Zmanim.ALLUMAGES
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.reglage_allumage)
            .setSingleChoiceItems(options.map { getString(R.string.reglage_allumage_valeur, it) }.toTypedArray(),
                options.indexOf(Store.allumage(this))) { d, i ->
                Store.setAllumage(this, options[i])
                d.dismiss()
                afficher()
            }
            .show()
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
}
