package com.myhitbodedut

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton

/**
 * « J'ai besoin de 'hizouk » (proposition 11 de la feuille de route, 19/10) :
 * un texte à la demande, hors séance. « Un autre » tire dans tout le vivier
 * publiable (Moment.DEMANDE), sans remise comme ailleurs ; « Garder » le met
 * dans les favoris, gardés sur l'appareil ; « Partager » le fait sortir de
 * l'appareil — le seul endroit où un texte de 'hizouk en sort, et c'est
 * l'utilisateur qui le décide, d'un geste (accord du propriétaire, 19/10).
 */
class ChizukActivity : AppCompatActivity() {

    private var courant: ChizukItem? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chizuk)
        val carte = findViewById<View>(R.id.chizuk_carte)
        courant = savedInstanceState?.getString(ETAT_TEXTE)?.let { Data.chizuk(this).byId(it) }
        if (courant == null) courant = ChizukCard.show(carte, Moment.DEMANDE)
        else ChizukCard.bind(carte, Data.chizuk(this), courant, Moment.DEMANDE)

        findViewById<MaterialButton>(R.id.chizuk_autre).setOnClickListener {
            courant = ChizukCard.show(carte, Moment.DEMANDE)
            Effets.fondu(carte)
            rendreGarder()
            findViewById<View>(R.id.chizuk_defilement).scrollTo(0, 0)
        }
        findViewById<MaterialButton>(R.id.chizuk_garder).setOnClickListener {
            courant?.let { Store.basculerChizukFavori(this, it.id) }
            rendreGarder()
            rendreFavoris()
        }
        findViewById<MaterialButton>(R.id.chizuk_partager).setOnClickListener { courant?.let(::partager) }
        findViewById<MaterialButton>(R.id.chizuk_commencer).setOnClickListener {
            startActivity(Intent(this, SessionActivity::class.java))
        }
        rendreGarder()
        rendreFavoris()
        // Les effets (19/10) : la carte, les boutons, les textes gardés entrent en cascade.
        if (savedInstanceState == null) (carte.parent as? android.view.ViewGroup)?.let { Effets.enfants(it) }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        courant?.let { outState.putString(ETAT_TEXTE, it.id) }
    }

    private fun rendreGarder() {
        val garde = courant?.id?.let { it in Store.chizukFavoris(this) } == true
        findViewById<MaterialButton>(R.id.chizuk_garder).apply {
            setText(if (garde) R.string.chizuk_garde else R.string.chizuk_garder)
            isEnabled = courant != null
        }
        findViewById<MaterialButton>(R.id.chizuk_partager).isEnabled = courant != null
    }

    /** Les textes gardés, le plus récent en tête ; toucher l'un le remet sur la carte. */
    private fun rendreFavoris() {
        val chizuk = Data.chizuk(this)
        val gardes = Store.chizukFavoris(this).mapNotNull(chizuk::byId)
        val liste = findViewById<LinearLayout>(R.id.chizuk_favoris)
        liste.removeAllViews()
        val visible = if (gardes.isEmpty()) View.GONE else View.VISIBLE
        liste.visibility = visible
        findViewById<View>(R.id.chizuk_favoris_titre).visibility = visible
        val langue = ChizukCard.language(this)
        gardes.forEachIndexed { i, item ->
            if (i > 0) liste.addView(View(this).apply {
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1)
                setBackgroundColor(Apparence.couleur(this@ChizukActivity, R.color.line))
            })
            liste.addView(TextView(this).apply {
                val traduction = ChizukCard.translationOf(item, langue)
                text = (traduction.ifBlank { ChizukRules.hebrew(chizuk, item) }) + "\n" + Reference.localise(this@ChizukActivity, item.ref)
                maxLines = 3
                ellipsize = android.text.TextUtils.TruncateAt.END
                textSize = 15f
                setTextColor(Apparence.couleur(this@ChizukActivity, R.color.ink))
                setPadding(dp(4), dp(12), dp(4), dp(12))
                foreground = getDrawable(android.R.drawable.list_selector_background)
                Effets.presse(this)
                setOnClickListener {
                    courant = item
                    ChizukCard.bind(findViewById(R.id.chizuk_carte), chizuk, item, Moment.DEMANDE)
                    Effets.fondu(findViewById(R.id.chizuk_carte))
                    rendreGarder()
                    findViewById<View>(R.id.chizuk_defilement).scrollTo(0, 0)
                }
            })
        }
    }

    private fun partager(item: ChizukItem) {
        val texte = ChizukPartage.texte(
            ChizukRules.hebrew(Data.chizuk(this), item),
            ChizukCard.translationOf(item, ChizukCard.language(this)),
            Reference.localise(this, item.ref),
            getString(R.string.partage_lien, CelebrationActivity.LIEN_WEB),
        )
        val envoi = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, texte)
        startActivity(Intent.createChooser(envoi, getString(R.string.chizuk_partager)))
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private companion object {
        const val ETAT_TEXTE = "texte"
    }
}

/** Le texte partagé, vérifiable sans appareil : l'hébreu, la traduction s'il y en a une, la source, le lien. */
object ChizukPartage {
    fun texte(hebreu: String, traduction: String, source: String, lien: String): String =
        listOfNotNull(hebreu.trim(), traduction.trim().ifBlank { null }, "— $source", lien).joinToString("\n\n")
}
