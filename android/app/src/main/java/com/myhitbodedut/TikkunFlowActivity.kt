package com.myhitbodedut

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.myhitbodedut.ReglagesLecture.Role
import androidx.appcompat.app.AppCompatActivity

/**
 * Le Tikoun HaKlali, le seul écran du Tikoun : le texte entier, d'un trait.
 *
 * La source est explicite : « להמשיך מזה לזה ברצף אחד, ללא הפסק ». Il n'y a
 * donc aucun bouton entre les psaumes, rien a toucher, rien qui interrompe —
 * on fait defiler et on continue. Le Tikoun découpé psaume par psaume (liste
 * des dix, un écran par psaume) est retiré à la demande du 04/11.
 *
 * Tout est construit d'avance dans une seule vue defilante : l'ouverture,
 * les dix psaumes a la suite, les versets de cloture ; puis, après un filet,
 * ce qui se dit après le Tikoun sans en faire partie : la kavana du milouy et
 * la prière de Rabbi Natan, chacune avec sa note.
 */
class TikkunFlowActivity : AppCompatActivity() {

    private lateinit var tikkun: Tikkun

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tikkun_flow)

        tikkun = Data.tikkun(this)

        // Pas de « sans interruption » en tête (12/10) : le texte dit lui-même où s'arrêter
        // un peu, après « אַתָּה סֵתֶר לִי », comme il le dit à cet endroit.
        findViewById<TextView>(R.id.flow_subtitle).visibility = View.GONE

        val contenu = findViewById<LinearLayout>(R.id.flow_content)
        var premier = true

        val mode = LanguesTexte.mode(this)
        tikkun.avant.forEach { bloc ->
            contenu.addView(titre(bloc.titre.he, premier))
            premier = false
            paragraphesTraduits(bloc, contenu, mode)
        }

        tikkun.psaumes.forEach { psaume ->
            contenu.addView(titre(getString(R.string.tikkun_psalm_he, Reference.lettres(psaume.numero)), premier))
            premier = false
            psaume.versets.forEach { verset ->
                // Les langues choisies (04/11) : l'hébreu, la traduction, ou les deux.
                val textes = LanguesTexte.textes(mode, verset.he, verset.fr, verset.en)
                val avecHebreu = textes.any { it.first == "he" }
                if (avecHebreu) {
                    // L'arret se marque dans le verset, avant « מִצַּר ».
                    val coupe = verset.coupe()
                    if (coupe == null) {
                        contenu.addView(vueVerset(verset.he))
                        verset.arret?.let { contenu.addView(vueArret(it)) }
                    } else {
                        contenu.addView(vueVerset(coupe.first))
                        contenu.addView(vueArret(verset.arret!!))
                        contenu.addView(vueVerset(coupe.second))
                    }
                }
                textes.filter { it.first != "he" }.forEachIndexed { k, (_, t) ->
                    contenu.addView(LanguesTexte.vueMarquee(this, t, seule = !avecHebreu).apply {
                        layoutParams = ligneParams().apply { topMargin = if (avecHebreu || k > 0) dp(4) else dp(12) }
                    })
                }
            }
        }

        tikkun.bloc(Tikkun.CLOTURE)?.let { bloc ->
            contenu.addView(titre(bloc.titre.he, false))
            paragraphesTraduits(bloc, contenu, mode)
        }

        // Après le Tikoun (04/11, la liste retirée) : la kavana, puis la prière de Rabbi Natan.
        val langue = ChizukCard.language(this)
        listOfNotNull(tikkun.bloc(Tikkun.KAVANA), tikkun.bloc(Tikkun.PRIERE)).forEach { bloc ->
            contenu.addView(titre(bloc.titre.he, false))
            (bloc.note?.forLanguage(langue)?.ifBlank { null }
                ?: bloc.titre.forLanguage(langue).takeIf { it != bloc.titre.he })
                ?.let { contenu.addView(note(it)) }
            // La formule de la kavana est centrée : c'est un tableau de lettres, pas une phrase.
            bloc.formule?.let { contenu.addView(paragraphe(it).apply { (this as TextView).textAlignment = View.TEXT_ALIGNMENT_CENTER }) }
            paragraphesTraduits(bloc, contenu, mode)
        }

        // Le bouton de lecture : taille des lettres et lecture de nuit.
        val defilement = findViewById<ScrollView>(R.id.flow_scroll)
        ReglagesLecture.marquer(defilement, Role.FOND)
        val fin = findViewById<Button>(R.id.flow_done)
        ReglagesLecture.installer(this, defilement, auDessusDe = fin) { ReglagesLecture.appliquer(defilement) }
        ReglagesLecture.appliquer(defilement)

        fin.apply {
            setText(R.string.tikkun_flow_done)
            setOnClickListener {
                // Tout dire d'un trait vaut les dix psaumes.
                tikkun.psaumes.forEach { Store.setTikkunDone(this@TikkunFlowActivity, it.numero, true) }
                finish()
            }
        }
    }

    /** Le nom d'un psaume ou d'une partie, sur un filet, sans rien a toucher. */
    private fun titre(texte: String, premier: Boolean): View = ReglagesLecture.marquer(TextView(this).apply {
        layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
        ).apply {
            topMargin = if (premier) 0 else dp(24)
            bottomMargin = dp(4)
            marginStart = dp(16)
            marginEnd = dp(16)
        }
        text = texte
        layoutDirection = View.LAYOUT_DIRECTION_RTL
        textAlignment = View.TEXT_ALIGNMENT_VIEW_START
        textSize = 20f
        setTypeface(typeface, android.graphics.Typeface.BOLD)
        setTextColor(Apparence.couleur(context, R.color.brand))
        PoliceHebraique.appliquer(this)
    }, Role.TITRE)

    private fun vueVerset(texte: String): View = ReglagesLecture.marquer(TextView(this).apply {
        layoutParams = ligneParams()
        text = texte
        layoutDirection = View.LAYOUT_DIRECTION_RTL
        textAlignment = View.TEXT_ALIGNMENT_VIEW_START
        textSize = 19f
        setLineSpacing(dp(13).toFloat(), 1f)
        setTextColor(Apparence.couleur(context, R.color.ink))
        PoliceHebraique.appliquer(this)
    }, Role.TEXTE)

    /** Les paragraphes d'une partie liturgique, dans les langues choisies (04/11). */
    private fun paragraphesTraduits(bloc: LiturgyBlock, contenu: LinearLayout, mode: LanguesTexte.Mode) {
        bloc.paragraphes.forEachIndexed { i, p ->
            val textes = LanguesTexte.textes(mode, p, bloc.paragraphesFr.getOrNull(i), bloc.paragraphesEn.getOrNull(i))
            val avecHebreu = textes.any { it.first == "he" }
            if (avecHebreu) contenu.addView(paragraphe(p))
            textes.filter { it.first != "he" }.forEachIndexed { k, (_, t) ->
                contenu.addView(LanguesTexte.vueMarquee(this, t, seule = !avecHebreu, base = 18f).apply {
                    layoutParams = ligneParams().apply { topMargin = if (avecHebreu || k > 0) dp(4) else dp(12) }
                })
            }
        }
    }

    private fun paragraphe(texte: String): View = ReglagesLecture.marquer(TextView(this).apply {
        layoutParams = ligneParams()
        text = texte
        layoutDirection = View.LAYOUT_DIRECTION_RTL
        textAlignment = View.TEXT_ALIGNMENT_VIEW_START
        textSize = 18f
        setLineSpacing(dp(13).toFloat(), 1f)
        setTextColor(Apparence.couleur(context, R.color.ink))
        PoliceHebraique.appliquer(this)
    }, Role.TEXTE)

    /** La note d'une partie (« נאמרת אחרי התיקון »…), discrète, sous son titre. */
    private fun note(texte: String): View = ReglagesLecture.marquer(TextView(this).apply {
        layoutParams = ligneParams().apply { topMargin = dp(2) }
        text = texte
        textAlignment = View.TEXT_ALIGNMENT_VIEW_START
        textSize = 14f
        setTextColor(Apparence.couleur(context, R.color.muted))
    }, Role.DISCRET)

    private fun vueArret(arret: Pause): View = ReglagesLecture.marquer(TextView(this).apply {
        layoutParams = ligneParams()
        text = arret.note
        layoutDirection = View.LAYOUT_DIRECTION_RTL
        textAlignment = View.TEXT_ALIGNMENT_VIEW_START
        textSize = 15f
        setLineSpacing(dp(4).toFloat(), 1f)
        setTextColor(Apparence.couleur(context, R.color.muted))
        PoliceHebraique.appliquer(this)
    }, Role.DISCRET)

    private fun ligneParams() = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT,
    ).apply {
        // Des lignes aérées (12/10), comme le siddour.
        topMargin = dp(12)
        marginStart = dp(16)
        marginEnd = dp(16)
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
