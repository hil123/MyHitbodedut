package com.myhitbodedut

import android.content.Context
import android.graphics.Typeface
import android.view.View
import android.widget.TextView

/**
 * Les langues d'un texte à lire (04/11, « propose des versions he-fr, fr-en, he-en, en deux
 * langues ou séparément ») : hébreu seul, français seul, anglais seul, ou deux langues l'une
 * sous l'autre. C'est un choix de lecteur, fait dans la barre de lecture ; il vaut pour les
 * textes qui ont une traduction (Tehilim, le Tikoun, Esther, Chir HaChirim, le 'hizouk).
 * Un texte sans traduction reste en hébreu, et le dit.
 *
 * Par défaut, celui de la langue de l'application : l'hébreu seul en hébreu, l'hébreu et la
 * traduction en français et en anglais — la règle d'avant (RegleLangue, 22/09), inchangée tant
 * qu'on ne choisit rien.
 */
object LanguesTexte {

    enum class Mode(val cle: String, val langues: List<String>, val libelle: Int) {
        HE("he", listOf("he"), R.string.langues_he),
        FR("fr", listOf("fr"), R.string.langues_fr),
        EN("en", listOf("en"), R.string.langues_en),
        HE_FR("he+fr", listOf("he", "fr"), R.string.langues_he_fr),
        HE_EN("he+en", listOf("he", "en"), R.string.langues_he_en),
        FR_EN("fr+en", listOf("fr", "en"), R.string.langues_fr_en),
    }

    fun defaut(langueApp: String): Mode = when (langueApp) {
        "he", "iw" -> Mode.HE
        "en" -> Mode.HE_EN
        else -> Mode.HE_FR
    }

    fun mode(c: Context): Mode = Store.lectureLangues(c)?.let { k -> Mode.entries.firstOrNull { it.cle == k } }
        ?: defaut(ChizukCard.language(c))

    /**
     * Les textes à montrer, dans l'ordre du mode : (langue, texte). Une traduction absente est
     * sautée ; s'il ne reste rien, l'hébreu seul.
     */
    fun textes(mode: Mode, he: String, fr: String?, en: String?): List<Pair<String, String>> {
        val tous = mode.langues.mapNotNull { l ->
            when (l) {
                "he" -> "he" to he
                "fr" -> fr?.takeIf { it.isNotBlank() }?.let { "fr" to it }
                else -> en?.takeIf { it.isNotBlank() }?.let { "en" to it }
            }
        }
        return tous.ifEmpty { listOf("he" to he) }
    }

    /** Le mode veut une traduction que ce texte n'a pas. */
    fun manque(mode: Mode, aTraduction: Boolean): Boolean = !aTraduction && mode.langues.any { it != "he" }

    /**
     * Une traduction affichée : de gauche à droite, police du système, un peu plus petite que
     * l'hébreu quand elle l'accompagne ; [rendu] pour la taille et les couleurs de la lecture.
     */
    fun vueTraduction(c: Context, texte: CharSequence, rendu: ReglagesLecture.Rendu, seule: Boolean, base: Float = 19f): TextView =
        TextView(c).apply {
            text = texte
            layoutDirection = View.LAYOUT_DIRECTION_LTR
            textDirection = View.TEXT_DIRECTION_LTR
            textAlignment = View.TEXT_ALIGNMENT_VIEW_START
            typeface = Typeface.create("serif", Typeface.NORMAL)
            textSize = (if (seule) base - 1f else base - 3f) * rendu.echelle
            setLineSpacing(4f * c.resources.displayMetrics.density * rendu.echelle, 1f)
            setTextColor(if (seule) rendu.texte else rendu.discret)
        }

    /**
     * La même, pour les écrans que ReglagesLecture.appliquer retaille et recolore (Tikoun,
     * psaume, liturgie) : posée à sa taille de base et marquée, l'échelle vient ensuite.
     */
    fun vueMarquee(c: Context, texte: CharSequence, seule: Boolean, base: Float = 19f): TextView =
        ReglagesLecture.marquer(TextView(c).apply {
            text = texte
            layoutDirection = View.LAYOUT_DIRECTION_LTR
            textDirection = View.TEXT_DIRECTION_LTR
            textAlignment = View.TEXT_ALIGNMENT_VIEW_START
            typeface = Typeface.create("serif", Typeface.NORMAL)
            textSize = if (seule) base - 1f else base - 3f
            setLineSpacing(4f * c.resources.displayMetrics.density, 1f)
        }, if (seule) ReglagesLecture.Role.TEXTE else ReglagesLecture.Role.DISCRET)
}
