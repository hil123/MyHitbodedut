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
        // Les marque-pages (15/10) : une seule ligne (28/10), qui ouvre la liste filtrée.
        ligneSignets(c)?.let { add(it) }
    }

    /** La ligne « סימניות · n » (28/10) : tous les marque-pages, ou ceux d'un recueil (« s », « l »). */
    fun ligneSignets(c: Context, recueil: String? = null): View? {
        val marques = Store.marques(c).filter { (recueil == null || it.cle.startsWith("$recueil:")) && titre(c, it.cle) != null }
        if (marques.isEmpty()) return null
        return ligne(c, null, "${c.getString(R.string.marques_titre)} · ${marques.size}", "",
            icone = SiddurLectureActivity.icone(recueil == "l")) { listeSignets(c, marques) }
    }

    /** L'ordre du recueil : le siddour d'abord, puis les livres, chacun dans son ordre. */
    private fun ordre(c: Context, cle: String): Int {
        val livre = cle.startsWith("l:")
        val i = (if (livre) Data.livres(c) else Data.siddur(c)).textes.indexOfFirst { it.id == cle.substringAfter(':') }
        return (if (livre) 10_000 else 0) + i
    }

    /**
     * Tous les marque-pages (28/10) : d'abord tous, puis filtrés par recueil (סידור, ספרים)
     * ou par texte (שחרית, מנחה…). Même feuille que le web (listeSignets).
     */
    private fun listeSignets(c: Context, marques: List<MarquesPages.Marque>) {
        val textes = marques.map { it.cle }.distinct().sortedBy { ordre(c, it) }
        val filtres = buildList {
            add("*" to c.getString(R.string.recherche_tout))
            if (marques.any { it.cle.startsWith("s:") }) add("s" to c.getString(R.string.siddur_title))
            if (marques.any { it.cle.startsWith("l:") }) add("l" to c.getString(R.string.livres_title))
            if (textes.size > 1) textes.forEach { add(it to (titre(c, it) ?: it)) }
        }
        val liste = LinearLayout(c).apply { orientation = LinearLayout.VERTICAL }
        var dialogue: androidx.appcompat.app.AlertDialog? = null
        fun remplir(filtre: String) {
            liste.removeAllViews()
            marques.filter { filtre == "*" || (if (':' in filtre) it.cle == filtre else it.cle.startsWith("$filtre:")) }
                .sortedWith(compareBy({ ordre(c, it.cle) }, { it.section }, { it.bloc }))
                .forEach { m -> liste.addView(signet(c, m) { dialogue?.dismiss(); SiddurLectureActivity.ouvrirMarque(c, m) }) }
        }
        val puces = com.google.android.material.chip.ChipGroup(c).apply {
            isSingleSelection = true
            isSelectionRequired = true
            filtres.forEachIndexed { i, (cle, nom) ->
                addView(com.google.android.material.chip.Chip(c).apply {
                    id = View.generateViewId()
                    text = nom
                    if (':' in cle) PoliceHebraique.appliquer(this)
                    isCheckable = true
                    isChecked = i == 0
                    setOnCheckedChangeListener { _, coche -> if (coche) remplir(cle) }
                })
            }
        }
        remplir("*")
        val corps = LinearLayout(c).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(c, 20), dp(c, 8), dp(c, 20), dp(c, 8))
            addView(puces)
            addView(liste)
        }
        dialogue = com.google.android.material.dialog.MaterialAlertDialogBuilder(c)
            .setTitle(R.string.marques_titre)
            .setView(android.widget.ScrollView(c).apply { addView(corps) })
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    /** Une ligne de la feuille : le texte en petit, les premiers mots du paragraphe dessous. */
    private fun signet(c: Context, m: MarquesPages.Marque, action: () -> Unit): View = LinearLayout(c).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(0, dp(c, 10), 0, dp(c, 10))
        minimumHeight = dp(c, 48)
        foreground = c.getDrawable(android.R.drawable.list_selector_background)
        setOnClickListener { action() }
        addView(TextView(c).apply {
            text = titre(c, m.cle)
            textSize = 13f
            textDirection = View.TEXT_DIRECTION_RTL
            textAlignment = View.TEXT_ALIGNMENT_VIEW_START
            setTextColor(Apparence.couleur(c, R.color.muted))
            PoliceHebraique.appliquer(this)
            c.getDrawable(SiddurLectureActivity.icone(m.cle.startsWith("l:")))?.mutate()?.apply {
                setTint(Apparence.couleur(c, R.color.brand)); setBounds(0, 0, dp(c, 14), dp(c, 14))
            }?.let { setCompoundDrawablesRelative(null, null, it, null); compoundDrawablePadding = dp(c, 6) }
        })
        addView(TextView(c).apply {
            text = "${m.apercu}…"
            textSize = 17f
            textDirection = View.TEXT_DIRECTION_RTL
            textAlignment = View.TEXT_ALIGNMENT_VIEW_START
            setTextColor(Apparence.couleur(c, R.color.ink))
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
            PoliceHebraique.appliquer(this)
        })
    }

    private fun ligne(c: Context, entete: String?, titre: String, cle: String, icone: Int? = null, discret: Boolean = false, action: () -> Unit = { ouvrir(c, cle) }): View = LinearLayout(c).apply {
        orientation = LinearLayout.VERTICAL
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            .apply { setMargins(dp(c, 16), dp(c, if (entete != null) 14 else if (discret) 0 else 4), dp(c, 16), 0) }
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
            textSize = if (discret) 14f else 19f
            textDirection = View.TEXT_DIRECTION_RTL
            textAlignment = View.TEXT_ALIGNMENT_VIEW_START
            setTextColor(Apparence.couleur(c, if (discret) R.color.muted else R.color.ink))
            if (discret) { maxLines = 1; ellipsize = android.text.TextUtils.TruncateAt.END }
            setPadding(0, dp(c, 4), 0, dp(c, 4))
            PoliceHebraique.appliquer(this)
            foreground = c.getDrawable(android.R.drawable.list_selector_background)
            setOnClickListener { action() }
            icone?.let { id ->
                c.getDrawable(id)?.mutate()?.apply { setTint(Apparence.couleur(c, if (discret) R.color.muted else R.color.brand)); setBounds(0, 0, dp(c, if (discret) 14 else 18), dp(c, if (discret) 14 else 18)) }
                    ?.let { setCompoundDrawablesRelative(null, null, it, null); compoundDrawablePadding = dp(c, 8) }
            }
        })
    }
}
