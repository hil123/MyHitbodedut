package com.myhitbodedut

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import java.util.concurrent.Executors

/**
 * La recherche dans toute l'application (14/10) : un champ, les résultats par
 * sorte (siddour, livres, halakha, hilloulot) ; chacun ouvre son écran. La
 * recherche tourne hors du fil de l'interface, un instant après la frappe.
 */
class RechercheActivity : AppCompatActivity() {

    private lateinit var liste: LinearLayout
    private val fil = Executors.newSingleThreadExecutor()
    private val principal = Handler(Looper.getMainLooper())
    private var demande = 0
    private var portee = RechercheGlobale.Portee.TOUT
    private var dernier = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recherche)
        val contenu = findViewById<LinearLayout>(R.id.recherche_contenu)
        contenu.addView(VueJour.carte(this).apply {
            addView(EditText(context).apply {
                hint = getString(R.string.recherche_indice)
                setSingleLine()
                requestFocus()
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) = Unit
                    override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) = Unit
                    override fun afterTextChanged(s: Editable?) { lancer(s?.toString().orEmpty()) }
                })
            })
        })
        // Où chercher (19/10) : tout, les prières, les halakhot ou les livres.
        contenu.addView(com.google.android.material.chip.ChipGroup(this).apply {
            isSingleSelection = true
            isSelectionRequired = true
            RechercheGlobale.Portee.values().forEach { p ->
                addView(com.google.android.material.chip.Chip(context).apply {
                    id = View.generateViewId()
                    text = getString(when (p) {
                        RechercheGlobale.Portee.TOUT -> R.string.recherche_tout
                        RechercheGlobale.Portee.TEFILOT -> R.string.recherche_tefilot
                        RechercheGlobale.Portee.HALAKHOT -> R.string.recherche_halakhot
                        RechercheGlobale.Portee.SEFARIM -> R.string.recherche_sefarim
                    })
                    isCheckable = true
                    isChecked = p == portee
                    setOnCheckedChangeListener { _, coche -> if (coche) { portee = p; lancer(dernier) } }
                })
            }
        })
        liste = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        contenu.addView(liste)
    }

    override fun onDestroy() {
        // L'attente de 300 ms d'abord : lancée après l'arrêt du fil, elle plantait (RejectedExecutionException).
        principal.removeCallbacksAndMessages(null)
        fil.shutdownNow()
        super.onDestroy()
    }

    private fun lancer(texte: String) {
        dernier = texte
        val n = ++demande
        val ou = portee
        principal.postDelayed({
            if (n != demande || fil.isShutdown) return@postDelayed
            val c = applicationContext
            val langue = ChizukCard.language(this)
            fil.execute {
                val r = runCatching {
                    RechercheGlobale.chercher(texte, langue, Data.siddur(c), Data.livres(c), YalkutYosef.livre(c), Halakha.livre(c), Hilloulot.table(c), ou)
                }.getOrDefault(emptyList())
                principal.post { if (n == demande && !isFinishing) afficher(texte, r) }
            }
        }, 300)
    }

    private fun afficher(texte: String, resultats: List<RechercheGlobale.Resultat>) {
        liste.removeAllViews()
        if (texte.trim().length < 2) return
        liste.addView(VueJour.texte(this, getString(R.string.hilloulot_resultats, resultats.size), 14f, R.color.muted, gras = true, haut = 14))
        RechercheGlobale.Sorte.values().forEach { sorte ->
            val de = resultats.filter { it.sorte == sorte }
            if (de.isEmpty()) return@forEach
            liste.addView(VueJour.texte(this, getString(when (sorte) {
                RechercheGlobale.Sorte.SIDDOUR -> R.string.siddur_title
                RechercheGlobale.Sorte.LIVRE -> R.string.recherche_livres
                RechercheGlobale.Sorte.HALAKHA -> R.string.halakha_title
                RechercheGlobale.Sorte.HILLOULA -> R.string.hilloulot_title
            }), 13f, R.color.brand, gras = true, haut = 16))
            de.forEach { r -> liste.addView(ligne(r)) }
        }
    }

    private fun ligne(r: RechercheGlobale.Resultat): View = VueJour.carte(this).apply {
        addView(VueJour.texte(context, r.titre, 16f, R.color.ink, gras = true).apply { textDirection = View.TEXT_DIRECTION_ANY_RTL; PoliceHebraique.appliquer(this) })
        if (r.extrait.isNotBlank()) addView(VueJour.texte(context, r.extrait, 13f, R.color.muted, haut = 4).apply { textDirection = View.TEXT_DIRECTION_ANY_RTL; maxLines = 2 })
        foreground = getDrawable(android.R.drawable.list_selector_background)
        setOnClickListener {
            when (r.sorte) {
                RechercheGlobale.Sorte.SIDDOUR -> SiddurLectureActivity.ouvrir(context, r.id)
                RechercheGlobale.Sorte.LIVRE -> SiddurLectureActivity.ouvrirLivre(context, r.id)
                RechercheGlobale.Sorte.HALAKHA -> startActivity(Intent(context, HalakhaActivity::class.java).putExtra(EXTRA_RECHERCHE, r.id))
                RechercheGlobale.Sorte.HILLOULA -> startActivity(Intent(context, HilloulotActivity::class.java).putExtra(EXTRA_RECHERCHE, r.id))
            }
        }
    }

    companion object {
        /** Le texte recherché, à reprendre dans l'écran ouvert (halakha, hilloulot). */
        const val EXTRA_RECHERCHE = "recherche"

        /** Remplit le champ de recherche de l'écran ouvert depuis la recherche globale. */
        fun preremplir(activite: AppCompatActivity, contenu: ViewGroup) {
            val q = activite.intent.getStringExtra(EXTRA_RECHERCHE) ?: return
            fun champ(v: View): EditText? = when (v) {
                is EditText -> v
                is ViewGroup -> (0 until v.childCount).firstNotNullOfOrNull { champ(v.getChildAt(it)) }
                else -> null
            }
            champ(contenu)?.setText(q)
        }
    }
}
