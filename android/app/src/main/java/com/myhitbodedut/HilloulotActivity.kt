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
 * Les hilloulot (11/10), à la manière de CalJ : le jour, ses tsadikim avec
 * l'année, une phrase de Wikipédia quand l'article existe et la source ; le
 * jour précédent ou suivant ; la recherche par nom dans toute l'année ; et
 * l'interrupteur qui montre ou cache la hilloula sur la carte du jour.
 */
class HilloulotActivity : AppCompatActivity() {

    private var jour: LocalDate = LocalDate.now()
    private var recherche = ""
    private lateinit var liste: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_hilloulot)
        (savedInstanceState?.getString(ETAT_JOUR) ?: intent.getStringExtra(EXTRA_DATE))?.let { jour = LocalDate.parse(it) }
        val contenu = findViewById<LinearLayout>(R.id.hilloulot_contenu)

        contenu.addView(VueJour.carte(this).apply {
            addView(LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                addView(VueJour.texte(context, getString(R.string.hilloulot_afficher), 15f, R.color.ink).apply {
                    layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                })
                addView(SwitchCompat(context).apply {
                    isChecked = Store.hilloulotAffichees(context)
                    contentDescription = getString(R.string.hilloulot_afficher)
                    setOnCheckedChangeListener { _, v -> Store.setHilloulotAffichees(context, v) }
                })
            })
            addView(EditText(context).apply {
                hint = getString(R.string.hilloulot_recherche)
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
        ReglagesLecture.installer(this, findViewById(R.id.hilloulot_defile)) { afficher() }
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
        val table = Hilloulot.tableComplete(this)
        if (recherche.trim().length >= 2) {
            val trouves = Hilloulot.recherche(table, recherche)
            liste.addView(VueJour.texte(this, getString(R.string.hilloulot_resultats, trouves.size), 14f, R.color.muted, gras = true, haut = 14))
            trouves.take(60).forEach { (cle, h) -> liste.addView(carte(h, Hilloulot.dateDe(cle))) }
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
        val du = Hilloulot.pour(table, jour, israel, Store.siddurNusah(this).cle == "ashkenaz")
        if (du.isEmpty()) liste.addView(VueJour.texte(this, getString(R.string.hilloulot_aucune), 15f, R.color.muted, haut = 14).apply { gravity = Gravity.CENTER })
        du.forEach { liste.addView(carte(it, null)) }
        liste.addView(VueJour.texte(this, getString(R.string.hilloula_lien) + " ↗", 15f, R.color.brand, gras = true, haut = 14).apply {
            gravity = Gravity.CENTER
            setPadding(0, VueJour.dp(context, 4), 0, VueJour.dp(context, 4))
            minHeight = VueJour.dp(context, 48)
            setOnClickListener { ouvrir(Hilloulot.lienHyomi(jour)) }
        })
    }

    // Police plus petite que le reste de l'application (14/10) : les biographies sont longues.
    private fun carte(h: Hilloulot.Hilloula, date: String?) = VueJour.carte(this).apply {
        addView(VueJour.texte(context, h.he, 18f, R.color.ink, gras = true).apply {
            textDirection = View.TEXT_DIRECTION_RTL; PoliceHebraique.appliquer(this)
        })
        val langue = ChizukCard.language(context)
        val traduit = if (langue.startsWith("en")) h.en else if (langue.startsWith("fr")) h.fr else null
        if (!traduit.isNullOrBlank()) addView(VueJour.texte(context, traduit, 13f, R.color.muted, haut = 2))
        // Isolés (FSI … PDI) : la date et l'année hébraïques ne renversent pas la ligne.
        listOfNotNull(date?.let { "\u2068$it\u2069" }, h.annee.takeIf { it.isNotBlank() }?.let { getString(R.string.hilloula_annee, "\u2068$it\u2069") })
            .joinToString(" · ").takeIf { it.isNotBlank() }?.let { addView(VueJour.texte(context, it, 12f, R.color.muted, haut = 4)) }
        h.description?.let { d ->
            addView(VueJour.texte(context, d, 14f, R.color.ink, haut = 8).apply {
                textDirection = View.TEXT_DIRECTION_RTL; PoliceHebraique.appliquer(this)
            })
        }
        // La source : Wikipédia (l'article s'il existe), החכם היומי.
        if ('w' in h.sources) addView(VueJour.texte(context, getString(R.string.hilloula_source_wiki), 12f, R.color.brand, haut = 8).apply {
            h.article?.let { a -> setOnClickListener { ouvrir(Hilloulot.lienArticle(a)) }; VueJour.touchable(this) }
        })
        if ('h' in h.sources) addView(VueJour.texte(context, getString(R.string.hilloula_source_hyomi), 12f, R.color.brand, haut = 2))
        if ('e' in h.sources) addView(VueJour.texte(context, getString(R.string.hilloula_source_toratemet), 12f, R.color.brand, haut = 2))
        // Les autres biographies (MyTzadik, המאיר לארץ), chacune suivie de sa source, comme le demande l'accord.
        h.bios.forEach { b ->
            val texte = VueJour.texte(context, b.texte, 14f, R.color.ink, haut = 14).apply {
                textDirection = View.TEXT_DIRECTION_RTL; PoliceHebraique.appliquer(this)
            }
            addView(texte)
            // Une longue biographie se replie à huit lignes ; « Lire la suite » la déplie.
            if (b.texte.length > 500) {
                texte.maxLines = 8
                texte.ellipsize = android.text.TextUtils.TruncateAt.END
                addView(VueJour.texte(context, getString(R.string.hilloula_lire_plus), 13f, R.color.brand, gras = true, haut = 4).apply {
                    setOnClickListener {
                        val replie = texte.maxLines != Int.MAX_VALUE
                        texte.maxLines = if (replie) Int.MAX_VALUE else 8
                        text = getString(if (replie) R.string.hilloula_reduire else R.string.hilloula_lire_plus)
                    }
                    VueJour.touchable(this)
                })
            }
            val nom = getString(if (b.source == "m") R.string.hilloula_source_mytzadik else R.string.hilloula_source_hameir)
            addView(VueJour.texte(context, if (b.lien != null) "$nom ↗" else nom, 12f, R.color.brand, haut = 6).apply {
                b.lien?.let { l -> setOnClickListener { ouvrir(l) }; VueJour.touchable(this) }
            })
        }
        h.hidabroot?.let { l ->
            addView(VueJour.texte(context, getString(R.string.hilloula_source_hidabroot) + " ↗", 12f, R.color.brand, haut = 8).apply { setOnClickListener { ouvrir(l) }; VueJour.touchable(this) })
        }
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
    }
}
