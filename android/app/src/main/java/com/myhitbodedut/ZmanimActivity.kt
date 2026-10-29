package com.myhitbodedut

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.time.LocalDate

/**
 * L'onglet Zmanim (demande du 10/10) : la date hébraïque du jour, ce qu'elle
 * change aux prières, cinq tuiles (calendrier, halakha, hilloulot, zmanim en
 * vol — VolActivity —, boussole de Jérusalem — BoussoleActivity), puis tous les horaires de la
 * journée, chacun avec sa méthode (ZMANIM.md) ; ensuite
 * Birkat haLevana, la hilloula et la halakha du jour (14/10). Le lieu se choisit sur le
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
        // Le jour a pu changer, et le lieu avec les réglages ; la position du téléphone se reprend
        // sans bruit (28/10, « les zmanim en fonction de la position exacte »).
        afficher()
        choixLieu.rafraichir()
    }

    private fun afficher() {
        val israel = Store.inIsrael(this)
        val lieu = Store.lieuZmanim(this)
        // Sans lieu, la grande carte « Choisir le lieu » suffit : pas de seconde pastille dans le bandeau (29/10).
        findViewById<TextView>(R.id.zmanim_lieu).apply {
            visibility = if (lieu == null) View.GONE else View.VISIBLE
            if (lieu != null) text = getString(R.string.zmanim_lieu_detail, lieu.nom, getString(if (israel) R.string.region_israel else R.string.region_diaspora))
        }
        val aujourdhui = LocalDate.now()
        val contenu = findViewById<LinearLayout>(R.id.zmanim_contenu)
        contenu.removeAllViews()
        // Le jour seul, les accès en tuiles, puis les horaires (29/10, option C : une seule forme de
        // bouton au lieu de trois). Levana, hilloula et halakha viennent après, masquables dans les réglages.
        contenu.addView(VueJour.carteJour(this, aujourdhui, israel, complet = false))
        contenu.addView(tuiles(listOf(
            Acces(R.string.zmanim_calendrier, R.drawable.char_writing) { startActivity(Intent(this, CalendrierActivity::class.java)) },
            Acces(R.string.zmanim_bouton_halakha, R.drawable.char_torah) { VueJour.ouvrirHalakha(this, aujourdhui) },
            Acces(R.string.zmanim_bouton_hilloulot, R.drawable.char_tehilim) {
                startActivity(Intent(this, HilloulotActivity::class.java).putExtra(HilloulotActivity.EXTRA_DATE, aujourdhui.toString()))
            },
            Acces(R.string.zmanim_bouton_vol, R.drawable.char_walking) { startActivity(Intent(this, VolActivity::class.java)) },
            Acces(R.string.zmanim_bouton_boussole, R.drawable.char_standing_back) { startActivity(Intent(this, BoussoleActivity::class.java)) },
        )))
        contenu.addView(VueJour.horaires(this, aujourdhui, israel) { choixLieu.montrer() })
        VueJour.sousHoraires(this, aujourdhui, israel).forEach { contenu.addView(it) }
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

    /**
     * Les accès du jour en tuiles, trois par rangée (29/10) : une illustration du personnage
     * (au féminin pour צדקת, PersonnageImageView) sur la plaque claire, et le nom dessous.
     */
    private fun tuiles(acces: List<Acces>) = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            .apply { setMargins(dp(16), dp(4), dp(16), 0) }
        acces.chunked(PAR_RANGEE).forEach { rangee ->
            addView(LinearLayout(this@ZmanimActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                    .apply { topMargin = dp(8) }
                rangee.forEachIndexed { i, (nom, dessin, action) -> addView(tuile(nom, dessin, if (i == 0) 0 else 8, action)) }
                // Une rangée incomplète garde la largeur des tuiles du dessus.
                repeat(PAR_RANGEE - rangee.size) {
                    addView(android.view.View(this@ZmanimActivity).apply {
                        layoutParams = LinearLayout.LayoutParams(0, 0, 1f).apply { marginStart = dp(8) }
                    })
                }
            })
        }
    }

    /** Une tuile : son nom, son dessin (char_…), ce qu'elle ouvre. */
    private data class Acces(val nom: Int, val dessin: Int, val action: () -> Unit)

    private fun tuile(nom: Int, dessin: Int, marge: Int, action: () -> Unit) = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        gravity = android.view.Gravity.CENTER_HORIZONTAL
        setBackgroundResource(R.drawable.bg_card)
        setPadding(dp(6), dp(12), dp(6), dp(10))
        layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f).apply { marginStart = dp(marge) }
        foreground = getDrawable(android.R.drawable.list_selector_background)
        Effets.presse(this)
        setOnClickListener { action() }
        contentDescription = getString(nom)
        addView(PersonnageImageView(this@ZmanimActivity).apply {
            setImageResource(dessin)
            scaleType = android.widget.ImageView.ScaleType.FIT_CENTER
            setPadding(dp(4), dp(4), dp(4), dp(4))
            background = android.graphics.drawable.GradientDrawable().apply {
                cornerRadius = dp(14).toFloat()
                setColor(Apparence.couleur(this@ZmanimActivity, R.color.mint))
            }
            contentDescription = null
            importantForAccessibility = android.view.View.IMPORTANT_FOR_ACCESSIBILITY_NO
            layoutParams = LinearLayout.LayoutParams(dp(56), dp(56))
        })
        addView(TextView(this@ZmanimActivity).apply {
            text = getString(nom)
            textSize = 13f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            gravity = android.view.Gravity.CENTER
            setTextColor(Apparence.couleur(this@ZmanimActivity, R.color.ink))
            importantForAccessibility = android.view.View.IMPORTANT_FOR_ACCESSIBILITY_NO
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                .apply { topMargin = dp(6) }
        })
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
        const val PAR_RANGEE = 3
    }
}
