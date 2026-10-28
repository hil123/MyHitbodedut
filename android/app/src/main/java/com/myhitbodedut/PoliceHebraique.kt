package com.myhitbodedut

import android.graphics.Typeface
import android.text.method.ReplacementTransformationMethod
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat

/**
 * La police des textes saints en hébreu (demande du 04/10) : psaumes,
 * prières, chizuk, sources de la bibliothèque, halakha, hilloulot.
 * L'interface, boutons et menus, garde la police du téléphone.
 *
 * Au choix depuis le 12/10 (bouton « Aa », pour tous les écrans) : la
 * police hébraïque à empattements de l'application, celle du téléphone
 * sans empattements, ou l'arrondie de l'application. Toutes sont dans
 * l'application ou le téléphone : rien ne se télécharge.
 */
object PoliceHebraique {

    enum class Police(val cle: String, val libelle: Int) {
        SERIF("serif", R.string.police_serif),
        SANS("sans", R.string.police_sans),
        ARRONDIE("arrondie", R.string.police_arrondie),
        // Quatre polices hébraïques libres (OFL, Google Fonts), toutes avec le nikkoud (14/10).
        FRANK("frank", R.string.police_frank),
        DAVID("david", R.string.police_david),
        RACHI("rachi", R.string.police_rachi),
        MIRIAM("miriam", R.string.police_miriam),
    }

    private val polices = mutableMapOf<Police, Typeface>()

    /**
     * Pose la police choisie sur [vue], en gardant le gras s'il y en a ; la
     * vue est marquée pour que ReglagesLecture.appliquer la repose quand on
     * change de police.
     */
    fun appliquer(vue: TextView) {
        val choix = Store.lecturePolice(vue.context)
        val base = polices[choix] ?: when (choix) {
            Police.SERIF -> ResourcesCompat.getFont(vue.context, R.font.hebreu_serif)
            Police.ARRONDIE -> ResourcesCompat.getFont(vue.context, R.font.varela_round)
            Police.SANS -> Typeface.SANS_SERIF
            Police.FRANK -> ResourcesCompat.getFont(vue.context, R.font.frank_ruhl)
            Police.DAVID -> ResourcesCompat.getFont(vue.context, R.font.david_libre)
            Police.RACHI -> ResourcesCompat.getFont(vue.context, R.font.noto_rashi)
            Police.MIRIAM -> ResourcesCompat.getFont(vue.context, R.font.miriam_libre)
        }?.also { polices[choix] = it } ?: return
        val gras = vue.typeface?.isBold == true
        vue.setTag(R.id.lecture_police, true)
        vue.typeface = if (gras) Typeface.create(base, Typeface.BOLD) else base
        // Les signes absents de la police, empruntés par Android à une autre,
        // s'y posent sans ancrage et flottent hors des lettres (capture du
        // 28/10, Tikkun Ra'hel en Frank Ruhl). On les masque à l'affichage ;
        // seule la vue change, le texte et ses positions restent les mêmes.
        // Une vue sur une ligne ou en capitales a déjà sa transformation : on n'y touche pas.
        val actuelle = vue.transformationMethod
        if (actuelle == null || actuelle is Masque) {
            val masque = ABSENTS[choix]
            if (masque !== actuelle) vue.transformationMethod = masque
        }
    }

    /**
     * Remplace chaque signe absent par U+034F (combining grapheme joiner) :
     * invisible, sans chasse, sans coupure de ligne ni de mot, et de même
     * longueur, donc les décalages (signets, recherche, liens) ne bougent pas.
     */
    private class Masque(absents: String) : ReplacementTransformationMethod() {
        private val de = absents.toCharArray()
        private val vers = CharArray(de.size) { '\u034F' }
        override fun getOriginal() = de
        override fun getReplacement() = vers
    }

    /** Les טעמים (U+0591–U+05AF), absents des quatre polices ci-dessous. */
    private val TEAMIM = (0x0591..0x05AF).map { it.toChar() }.joinToString("")

    /**
     * Mesuré sur les fichiers de res/font avec fontTools (28/10) : Noto Serif
     * Hebrew et Noto Rashi ont les 31 טעמים et les 24 points ; les autres non.
     * La police du téléphone (SANS) varie selon l'appareil : laissée telle quelle.
     */
    private val ABSENTS: Map<Police, Masque> = mapOf(
        Police.FRANK to Masque(TEAMIM + "\u05C4\u05C5"),
        Police.DAVID to Masque(TEAMIM + "\u05BD"),
        Police.MIRIAM to Masque(TEAMIM + "\u05BD\u05BF\u05C4\u05C5"),
        Police.ARRONDIE to Masque(TEAMIM + "\u05BD\u05BF\u05C4\u05C5"),
    )
}
