package com.myhitbodedut

import android.content.Intent
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.time.LocalTime

/**
 * Le siddour (demandes du 04/10), dans la manière de l'application : en
 * tête, le texte que suggère l'heure, avec son hassid ; puis Tehilim et
 * le Tikoun HaKlali côte à côte ; puis les textes par groupe, en lignes de
 * section comme sur l'accueil, le nom hébreu dans la police des textes
 * saints et sa traduction dessous. Le nusah se choisit sur le bandeau.
 */
class SiddurActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_siddur)
        findViewById<TextView>(R.id.siddur_heading_nusah).setOnClickListener { choisirNusah() }
    }

    override fun onResume() {
        super.onResume()
        // Le nusah a pu changer depuis une lecture (bouton de lecture), et l'heure a passé.
        afficher()
    }

    /** Toute la liste, pour le nusah choisi ; rebâtie quand on en change. */
    private fun afficher() {
        val siddur = Data.siddur(this)
        val langue = ChizukCard.language(this)
        findViewById<TextView>(R.id.siddur_heading_nusah).apply {
            text = siddur.nusah.forLanguage(langue)
            contentDescription = getString(R.string.siddur_choose) + " : " + text
        }
        val liste = findViewById<LinearLayout>(R.id.siddur_list)
        liste.removeAllViews()

        siddur.texte(SiddurMoment.suggestion(LocalTime.now()))?.let { liste.addView(maintenant(it, langue)) }

        liste.addView(entete(getString(R.string.siddur_books), null))
        liste.addView(LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                .apply { setMargins(dp(16), dp(8), dp(16), 0) }
            addView(livre(getString(R.string.siddur_tehilim_he), getString(R.string.tehilim_title), R.drawable.char_tehilim, fin = false) {
                startActivity(Intent(this@SiddurActivity, TehilimActivity::class.java))
            })
            addView(livre(getString(R.string.siddur_tikkun_he), getString(R.string.tikkun_title), R.drawable.char_torah, fin = true) {
                startActivity(Intent(this@SiddurActivity, TikkunActivity::class.java))
            })
        })

        siddur.groupes.forEach { groupe ->
            liste.addView(entete(groupe.titre.he, groupe.titre.forLanguage(langue).takeIf { it != groupe.titre.he }))
            liste.addView(carte(siddur.du(groupe.cle).map { texte ->
                val traduction = texte.titre.forLanguage(langue).takeIf { it != texte.titre.he }
                // Un texte absent des sources de ce nusah, emprunté à un autre, le dit.
                val detail = listOfNotNull(traduction, texte.emprunt?.forLanguage(langue)).joinToString(" · ")
                ligne(texte.titre.he, detail) { SiddurLectureActivity.ouvrir(this, texte.id) }
            }))
        }
        // Les sources, comme la licence CC-BY du siddour Metsudah le demande.
        liste.addView(TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                .apply { topMargin = dp(20) }
            setPadding(dp(20), 0, dp(20), 0)
            text = getString(R.string.siddur_sources, siddur.sources.joinToString(" ; "))
            textSize = 12f
            setTextColor(Apparence.couleur(context, R.color.muted))
        })
    }

    /** Les trois nusah ; le choix est gardé, et vaut aussi dans les lectures. */
    private fun choisirNusah() {
        val actuel = Store.siddurNusah(this)
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.siddur_choose)
            .setSingleChoiceItems(Nusah.entries.map { getString(it.libelle) }.toTypedArray(), actuel.ordinal) { dialogue, i ->
                if (Nusah.entries[i] != actuel) {
                    Store.setSiddurNusah(this, Nusah.entries[i])
                    afficher()
                }
                dialogue.dismiss()
            }
            .show()
    }

    private fun ondulation(v: View) {
        val tv = TypedValue()
        theme.resolveAttribute(android.R.attr.selectableItemBackground, tv, true)
        v.foreground = getDrawable(tv.resourceId)
        v.isClickable = true
        v.isFocusable = true
    }

    /** Le personnage sur son panneau clair, comme sur l'accueil. */
    private fun scene(dessin: Int, largeur: Int, hauteur: Int): View = FrameLayout(this).apply {
        layoutParams = LinearLayout.LayoutParams(dp(largeur), dp(hauteur))
        setBackgroundResource(R.drawable.bg_stage)
        setPadding(dp(4), dp(4), dp(4), dp(4))
        addView(ImageView(context).apply {
            layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            scaleType = ImageView.ScaleType.FIT_CENTER
            setImageResource(dessin)
            contentDescription = null
        })
    }

    private fun texteHebreu(texte: String, taille: Float): TextView = TextView(this).apply {
        text = texte
        textSize = taille
        layoutDirection = View.LAYOUT_DIRECTION_RTL
        textDirection = View.TEXT_DIRECTION_RTL
        textAlignment = View.TEXT_ALIGNMENT_VIEW_START
        setTextColor(Apparence.couleur(context, R.color.ink))
        PoliceHebraique.appliquer(this)
    }

    /** La suggestion de l'heure : le hassid, l'heure dite en clair, le texte. */
    private fun maintenant(texte: SiddurTexte, langue: String): View = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setBackgroundResource(R.drawable.bg_card)
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            .apply { setMargins(dp(16), dp(16), dp(16), 0) }
        setPadding(dp(14), dp(14), dp(18), dp(14))
        ondulation(this)
        setOnClickListener { SiddurLectureActivity.ouvrir(this@SiddurActivity, texte.id) }
        addView(scene(SiddurMoment.illustration(texte.id), 72, 86))
        addView(LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = dp(16) }
            addView(TextView(context).apply {
                text = getString(R.string.siddur_maintenant)
                textSize = 12f
                isAllCaps = true
                letterSpacing = 0.06f
                setTextColor(Apparence.couleur(context, R.color.brand))
            })
            addView(texteHebreu(texte.titre.he, 26f).apply { setPadding(0, dp(2), 0, 0) })
            texte.titre.forLanguage(langue).takeIf { it != texte.titre.he }?.let { traduction ->
                addView(TextView(context).apply {
                    text = traduction
                    textSize = 14f
                    setTextColor(Apparence.couleur(context, R.color.muted))
                })
            }
        })
        addView(ImageView(context).apply {
            layoutParams = LinearLayout.LayoutParams(dp(22), dp(22))
            setImageResource(R.drawable.ic_chevron)
            contentDescription = null
        })
    }

    /** Un livre en tuile : son personnage, son nom hébreu, son nom traduit. */
    private fun livre(hebreu: String, traduction: String, dessin: Int, fin: Boolean, ouvrir: () -> Unit): View =
        LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setBackgroundResource(R.drawable.bg_card)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f)
                .apply { if (fin) marginStart = dp(10) }
            setPadding(dp(12), dp(14), dp(12), dp(14))
            ondulation(this)
            setOnClickListener { ouvrir() }
            addView(scene(dessin, 56, 66))
            addView(texteHebreu(hebreu, 19f).apply {
                textAlignment = View.TEXT_ALIGNMENT_CENTER
                setPadding(0, dp(8), 0, 0)
            })
            if (traduction != hebreu) addView(TextView(context).apply {
                text = traduction
                textSize = 13f
                textAlignment = View.TEXT_ALIGNMENT_CENTER
                setTextColor(Apparence.couleur(context, R.color.muted))
            })
        }

    /** Le nom du groupe en hébreu, à la couleur choisie ; sa traduction à côté. */
    private fun entete(hebreu: String, traduction: String?): View = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.BOTTOM
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            .apply { topMargin = dp(22) }
        setPadding(dp(20), 0, dp(20), 0)
        addView(TextView(context).apply {
            text = hebreu
            textSize = 17f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(Apparence.couleur(context, R.color.brand))
            if (hebreu.any { it in '֐'..'׿' }) PoliceHebraique.appliquer(this)
        })
        traduction?.let {
            addView(TextView(context).apply {
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                    .apply { marginStart = dp(10); bottomMargin = dp(2) }
                text = it
                textSize = 13f
                setTextColor(Apparence.couleur(context, R.color.muted))
            })
        }
    }

    private fun carte(lignes: List<View>): View = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setBackgroundResource(R.drawable.bg_card)
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            .apply { setMargins(dp(16), dp(8), dp(16), 0) }
        setPadding(dp(18), dp(2), dp(12), dp(2))
        lignes.forEachIndexed { i, v ->
            if (i > 0) addView(View(context).apply {
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1))
                setBackgroundColor(Apparence.couleur(context, R.color.line))
            })
            addView(v)
        }
    }

    /** Une ligne de section : le nom hébreu, sa traduction dessous, le chevron. */
    private fun ligne(hebreu: String, traduction: String, ouvrir: () -> Unit): View =
        LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            minimumHeight = dp(60)
            setPadding(0, dp(8), 0, dp(8))
            ondulation(this)
            setOnClickListener { ouvrir() }
            addView(LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                addView(texteHebreu(hebreu, 20f))
                if (traduction.isNotBlank()) addView(TextView(context).apply {
                    text = traduction
                    textSize = 13f
                    textAlignment = View.TEXT_ALIGNMENT_VIEW_START
                    setTextColor(Apparence.couleur(context, R.color.muted))
                })
            })
            addView(ImageView(context).apply {
                layoutParams = LinearLayout.LayoutParams(dp(22), dp(22)).apply { marginStart = dp(10) }
                setImageResource(R.drawable.ic_chevron)
                contentDescription = null
            })
        }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
