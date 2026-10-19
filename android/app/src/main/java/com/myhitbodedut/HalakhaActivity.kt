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
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import java.time.LocalDate

/**
 * La halakha du jour (11/10), sur le modèle de l'écran des hilloulot : le
 * jour, ses halakhot (hors ligne, en hébreu) du livre choisi — Yalkut Yosef,
 * Ben Ish 'Haï ou les deux (14/10) —, chacune avec sa source ; le jour précédent ou suivant ; la recherche dans tout le
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
            // Le livre (14/10, « propose le choix à l'utilisateur ») : Yalkut Yosef, Ben Ish 'Haï, ou les deux.
            addView(VueJour.texte(context, getString(R.string.halakha_source_titre), 13f, R.color.muted, gras = true, haut = 10))
            addView(ChipGroup(context).apply {
                isSingleSelection = true
                isSelectionRequired = true
                Store.SourceHalakha.values().forEach { s ->
                    addView(Chip(context).apply {
                        id = View.generateViewId()
                        text = getString(s.libelle)
                        isCheckable = true
                        isChecked = s == Store.sourceHalakha(context)
                        setOnCheckedChangeListener { _, coche -> if (coche) { Store.setSourceHalakha(context, s); afficher() } }
                    })
                }
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
        // Taille et police des textes, comme dans les écrans de lecture (12/10).
        ReglagesLecture.installer(this, findViewById(R.id.halakha_defile)) { afficher() }
        afficher()
        RechercheActivity.preremplir(this, contenu)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(ETAT_JOUR, jour.toString())
    }

    private fun afficher() {
        remplir()
        ReglagesLecture.retailler(liste)
    }

    private fun remplir() {
        liste.removeAllViews()
        val livre = Halakha.livre(this)
        val source = Store.sourceHalakha(this)
        if (recherche.trim().length >= 2) {
            val yy = if (source.yalkut) YalkutYosef.recherche(YalkutYosef.livre(this), recherche) else emptyList()
            val trouves = if (source.benIshHai) Halakha.recherche(livre, recherche) else emptyList()
            liste.addView(VueJour.texte(this, getString(R.string.hilloulot_resultats, yy.size + trouves.size), 14f, R.color.muted, gras = true, haut = 14))
            yy.take(40).forEach { liste.addView(carte(YalkutYosef.source(it), it.texte)) }
            trouves.take(40).forEach { liste.addView(carte(source(it), it.texte)) }
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
        // Le Yalkut Yosef d'abord (13/10), deux halakhot courtes ; puis le Ben Ish 'Haï de la semaine ; selon le livre choisi.
        val yy = if (source.yalkut) YalkutYosef.pour(YalkutYosef.livre(this), jour, israel) else emptyList()
        if (yy.isNotEmpty()) liste.addView(carte(YalkutYosef.titreCarte(yy), YalkutYosef.texteCarte(yy)))
        val du = if (source.benIshHai) Halakha.pour(livre, jour, israel) else emptyList()
        if (yy.isEmpty() && du.isEmpty()) liste.addView(VueJour.texte(this, getString(R.string.halakha_aucune), 15f, R.color.muted, haut = 14).apply { gravity = Gravity.CENTER })
        if (du.isNotEmpty()) liste.addView(VueJour.texte(this, getString(R.string.halakha_ben_ish_hai), 14f, R.color.muted, gras = true, haut = 18))
        du.forEach { liste.addView(carte(source(it), it.texte)) }
        if (source.yalkut) liste.addView(VueJour.texte(this, getString(R.string.halakha_yalkut_source), 12f, R.color.muted, haut = 14).apply { gravity = Gravity.CENTER })
        liste.addView(VueJour.texte(this, getString(R.string.halakha_note), 12f, R.color.muted, haut = 14).apply { gravity = Gravity.CENTER })
        // Le site de Rav Ovadia Yossef : un lien seulement (tous droits réservés sur ses textes).
        liste.addView(VueJour.texte(this, getString(R.string.halakha_yomit_lien) + " ↗", 15f, R.color.brand, gras = true, haut = 10).apply {
            gravity = Gravity.CENTER
            setPadding(0, VueJour.dp(context, 4), 0, VueJour.dp(context, 4))
            minHeight = VueJour.dp(context, 48)
            setOnClickListener { ouvrir(Hilloulot.lienHalakha(ChizukCard.language(context))) }
        })
    }

    private fun carte(titre: String, texte: String) = VueJour.carte(this).apply {
        addView(VueJour.texte(context, titre, 13f, R.color.brand, gras = true).apply {
            textDirection = View.TEXT_DIRECTION_RTL; PoliceHebraique.appliquer(this)
        })
        addView(VueJour.texte(context, VueJour.paragraphes(texte), 18f, R.color.ink, haut = 8).apply {
            textDirection = View.TEXT_DIRECTION_RTL
            PoliceHebraique.appliquer(this)
            setLineSpacing(0f, 1.4f)
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
