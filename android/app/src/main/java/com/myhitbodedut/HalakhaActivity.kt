package com.myhitbodedut

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import java.time.LocalDate

/**
 * La halakha du jour (11/10), sur le modèle de l'écran des hilloulot : le
 * jour, ses halakhot du Ben Ish 'Haï (hors ligne, en hébreu), chacune avec
 * sa source ; le jour précédent ou suivant ; la recherche dans tout le
 * livre ; l'interrupteur de la carte du jour ; le lien vers halachayomit.co.il.
 */
class HalakhaActivity : AppCompatActivity() {

    private var jour: LocalDate = LocalDate.now()
    private var recherche = ""
    private lateinit var liste: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_halakha)
        (savedInstanceState?.getString(ETAT_JOUR) ?: intent.getStringExtra(EXTRA_DATE))?.let { jour = LocalDate.parse(it) }
        val contenu = findViewById<LinearLayout>(R.id.halakha_contenu)

        contenu.addView(VueJour.carte(this).apply {
            addView(LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                addView(VueJour.texte(context, getString(R.string.halakha_afficher), 15f, R.color.ink).apply {
                    layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                })
                addView(SwitchCompat(context).apply {
                    isChecked = Store.halakhaAffichee(context)
                    contentDescription = getString(R.string.halakha_afficher)
                    setOnCheckedChangeListener { _, v -> Store.setHalakhaAffichee(context, v) }
                })
            })
            addView(EditText(context).apply {
                hint = getString(R.string.halakha_recherche)
                setSingleLine()
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                    .apply { topMargin = (8 * resources.displayMetrics.density).toInt() }
                addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) = Unit
                    override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) = Unit
                    override fun afterTextChanged(s: Editable?) { recherche = s?.toString().orEmpty(); afficher() }
                })
            })
        })
        liste = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        contenu.addView(liste)
        afficher()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(ETAT_JOUR, jour.toString())
    }

    private fun afficher() {
        liste.removeAllViews()
        val livre = Halakha.livre(this)
        if (recherche.trim().length >= 2) {
            val trouves = Halakha.recherche(livre, recherche)
            liste.addView(VueJour.texte(this, getString(R.string.hilloulot_resultats, trouves.size), 14f, R.color.muted, gras = true, haut = 14))
            trouves.take(40).forEach { liste.addView(carte(it)) }
            return
        }
        val israel = Store.inIsrael(this)
        val j = CalendrierJuif.jour(jour, israel)
        liste.addView(LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                .apply { topMargin = (14 * resources.displayMetrics.density).toInt() }
            val rtl = resources.configuration.layoutDirection == View.LAYOUT_DIRECTION_RTL
            addView(fleche(if (rtl) "›" else "‹") { jour = jour.minusDays(1); afficher() })
            addView(LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                addView(VueJour.texte(context, j.dateHebraique, 20f, R.color.brand, gras = true).apply {
                    gravity = Gravity.CENTER; PoliceHebraique.appliquer(this)
                })
                addView(VueJour.texte(context, jour.toString(), 13f, R.color.muted).apply { gravity = Gravity.CENTER })
            })
            addView(fleche(if (rtl) "‹" else "›") { jour = jour.plusDays(1); afficher() })
        })
        val du = Halakha.pour(livre, jour, israel)
        if (du.isEmpty()) liste.addView(VueJour.texte(this, getString(R.string.halakha_aucune), 15f, R.color.muted, haut = 14).apply { gravity = Gravity.CENTER })
        du.forEach { liste.addView(carte(it)) }
        liste.addView(VueJour.texte(this, getString(R.string.halakha_note), 12f, R.color.muted, haut = 14).apply { gravity = Gravity.CENTER })
        // Le site de Rav Ovadia Yossef : un lien seulement (tous droits réservés sur ses textes).
        liste.addView(VueJour.texte(this, getString(R.string.halakha_yomit_lien) + " ↗", 15f, R.color.brand, gras = true, haut = 10).apply {
            gravity = Gravity.CENTER
            setPadding(0, 12, 0, 12)
            setOnClickListener { ouvrir(Hilloulot.lienHalakha(ChizukCard.language(context))) }
        })
    }

    private fun carte(h: Halakha.Choisie) = VueJour.carte(this).apply {
        addView(VueJour.texte(context, source(h), 13f, R.color.brand, gras = true).apply {
            textDirection = View.TEXT_DIRECTION_RTL; PoliceHebraique.appliquer(this)
        })
        addView(VueJour.texte(context, h.texte, 18f, R.color.ink, haut = 8).apply {
            textDirection = View.TEXT_DIRECTION_RTL
            PoliceHebraique.appliquer(this)
            setLineSpacing(0f, 1.25f)
            setTextIsSelectable(true)
        })
    }

    private fun fleche(signe: String, action: () -> Unit) = VueJour.texte(this, signe, 26f, R.color.brand).apply {
        textDirection = View.TEXT_DIRECTION_LTR
        gravity = Gravity.CENTER
        val d = (48 * resources.displayMetrics.density).toInt()
        layoutParams = LinearLayout.LayoutParams(d, d)
        setOnClickListener { action() }
    }

    private fun ouvrir(url: String) {
        runCatching { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
    }

    companion object {
        const val EXTRA_DATE = "date"
        private const val ETAT_JOUR = "jour"

        /** « בן איש חי · שנה ראשונה · לך לך · אות א » */
        fun source(h: Halakha.Choisie): String =
            listOf("בן איש חי", if (h.annee == 1) "שנה ראשונה" else "שנה שנייה", h.section, "אות ${Reference.lettres(h.numero)}")
                .joinToString(" · ")
    }
}
