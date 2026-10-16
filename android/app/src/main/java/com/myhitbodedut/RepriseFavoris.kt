package com.myhitbodedut

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView

/**
 * En tête de la Bibliothèque (14/10) : « Reprendre » le dernier texte lu, à sa
 * place, puis les favoris (l'étoile de l'écran de lecture). Chaque ligne ouvre
 * son texte ; un texte qui n'existe plus est passé sous silence.
 */
object RepriseFavoris {

    private fun dp(c: Context, v: Int) = (v * c.resources.displayMetrics.density).toInt()

    /** Le titre hébreu d'une clé « s:id » ou « l:id », ou null. */
    fun titre(c: Context, cle: String): String? {
        val id = cle.substringAfter(':')
        val recueil = if (cle.startsWith("l:")) Data.livres(c) else Data.siddur(c)
        return recueil.texte(id)?.titre?.he
    }

    fun ouvrir(c: Context, cle: String) {
        val id = cle.substringAfter(':')
        if (cle.startsWith("l:")) SiddurLectureActivity.ouvrirLivre(c, id) else SiddurLectureActivity.ouvrir(c, id)
    }

    fun lignes(c: Context): List<View> = buildList {
        Store.derniereLecture(c)?.let { cle -> titre(c, cle)?.let { add(ligne(c, c.getString(R.string.reprise_titre), it, cle)) } }
        val favoris = Store.favoris(c).mapNotNull { cle -> titre(c, cle)?.let { cle to it } }
        favoris.forEachIndexed { i, (cle, t) -> add(ligne(c, if (i == 0) c.getString(R.string.favoris_titre) else null, t, cle)) }
        // Les marque-pages (15/10) : le texte et les premiers mots du paragraphe marqué.
        Store.marques(c).mapNotNull { m -> titre(c, m.cle)?.let { m to it } }.take(12).forEachIndexed { i, (m, t) ->
            add(ligne(c, if (i == 0) c.getString(R.string.marques_titre) else null, "$t · ${m.apercu}…", m.cle,
                icone = SiddurLectureActivity.icone(m.cle.startsWith("l:"))) { SiddurLectureActivity.ouvrirMarque(c, m) })
        }
    }

    private fun ligne(c: Context, entete: String?, titre: String, cle: String, icone: Int? = null, action: () -> Unit = { ouvrir(c, cle) }): View = LinearLayout(c).apply {
        orientation = LinearLayout.VERTICAL
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            .apply { setMargins(dp(c, 16), dp(c, if (entete != null) 14 else 4), dp(c, 16), 0) }
        entete?.let {
            addView(TextView(c).apply {
                text = it
                textSize = 12f
                isAllCaps = true
                letterSpacing = 0.06f
                setTextColor(Apparence.couleur(c, R.color.brand))
            })
        }
        addView(TextView(c).apply {
            text = "$titre  ›"
            textSize = 19f
            textDirection = View.TEXT_DIRECTION_RTL
            textAlignment = View.TEXT_ALIGNMENT_VIEW_START
            setTextColor(Apparence.couleur(c, R.color.ink))
            setPadding(0, dp(c, 4), 0, dp(c, 4))
            PoliceHebraique.appliquer(this)
            foreground = c.getDrawable(android.R.drawable.list_selector_background)
            setOnClickListener { action() }
            icone?.let { id ->
                c.getDrawable(id)?.mutate()?.apply { setTint(Apparence.couleur(c, R.color.brand)); setBounds(0, 0, dp(c, 18), dp(c, 18)) }
                    ?.let { setCompoundDrawablesRelative(null, null, it, null); compoundDrawablePadding = dp(c, 8) }
            }
        })
    }
}
