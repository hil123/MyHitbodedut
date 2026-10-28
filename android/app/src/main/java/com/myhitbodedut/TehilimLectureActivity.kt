package com.myhitbodedut

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

/**
 * La lecture : un psaume, ou une portion d'un seul trait.
 *
 * Chaque verset porte son numéro en lettres, petit et discret, pour qu'on
 * retrouve sa place sans que la lecture en soit hachée. Un psaume seul a
 * deux boutons, précédent et suivant ; une portion n'en a aucun.
 */
class TehilimLectureActivity : AppCompatActivity() {

    private lateinit var portion: Portion
    private var reglages: ReglagesLecture? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tehilim_lecture)
        portion = Portion(
            debut = intent.getIntExtra(DEBUT, 1).coerceIn(1, Tehilim.PSAUMES),
            fin = intent.getIntExtra(FIN, 1).coerceIn(1, Tehilim.PSAUMES),
            debutVerset = intent.getIntExtra(DEBUT_VERSET, 1),
            finVerset = intent.getIntExtra(FIN_VERSET, 0).takeIf { it > 0 },
        )
        findViewById<Button>(R.id.lecture_previous).setOnClickListener { afficher(Portion(portion.debut - 1, portion.debut - 1)) }
        findViewById<Button>(R.id.lecture_next).setOnClickListener { afficher(Portion(portion.debut + 1, portion.debut + 1)) }
        // Le bouton de lecture : taille des lettres et lecture de nuit (pas de nusah ici).
        reglages = ReglagesLecture.installer(this, findViewById<ScrollView>(R.id.lecture_scroll), auDessusDe = findViewById(R.id.lecture_nav)) {
            afficher(portion, garderPlace = true)
        }
        afficher(portion)
        // Ouvert depuis un marque-page (28/10) : le verset marqué en haut de l'écran.
        intent.getIntExtra(MARQUE_VERSET, 0).takeIf { it > 0 }?.let { v -> allerAuVerset(portion.debut, v) }
    }

    /** Les versets affichés, psaume par psaume : la vue, et pour chaque verset sa place dans le texte. */
    private val places = mutableMapOf<Int, Pair<TextView, List<Triple<Int, Int, Int>>>>()

    private fun allerAuVerset(n: Int, v: Int) {
        val defile = findViewById<ScrollView>(R.id.lecture_scroll)
        defile.post {
            val (vue, versets) = places[n] ?: return@post
            val debut = versets.firstOrNull { it.third == v }?.first ?: return@post
            val ligne = vue.layout?.getLineForOffset(debut) ?: return@post
            var y = vue.top + vue.layout.getLineTop(ligne)
            var parent = vue.parent as? View
            while (parent != null && parent !== defile) { y += parent.top; parent = parent.parent as? View }
            defile.scrollTo(0, (y - dp(8)).coerceAtLeast(0))
        }
    }

    private fun afficher(p: Portion, garderPlace: Boolean = false) {
        portion = p
        rendu = ReglagesLecture.rendu(this)
        val livre = Data.tehilim(this)
        findViewById<TextView>(R.id.lecture_title).text = TehilimTexte.portion(this, p)
        val versets = p.psaumes.sumOf { n -> p.versets(n, livre.versets(n).size).count() }
        findViewById<TextView>(R.id.lecture_subtitle).text =
            resources.getQuantityString(R.plurals.tehilim_verses, versets, versets)

        val contenu = findViewById<LinearLayout>(R.id.lecture_content)
        contenu.removeAllViews()
        places.clear()
        val marques = MarquesPages.duTexte(Store.marques(this), CLE, "").map { it.section to it.bloc }.toSet()
        p.psaumes.forEachIndexed { i, n ->
            val tous = livre.versets(n)
            contenu.addView(titre(getString(R.string.tikkun_psalm_he, Reference.lettres(n)), i == 0))
            contenu.addView(texte(n, tous, p.versets(n, tous.size), marques))
        }

        // Un psaume seul se lit de proche en proche ; une portion d'un trait.
        val seul = p.debut == p.fin && !p.partielle
        findViewById<View>(R.id.lecture_nav).visibility = if (seul) View.VISIBLE else View.GONE
        findViewById<View>(R.id.lecture_previous).visibility = if (p.debut > 1) View.VISIBLE else View.INVISIBLE
        findViewById<View>(R.id.lecture_next).visibility = if (p.debut < Tehilim.PSAUMES) View.VISIBLE else View.INVISIBLE
        findViewById<ScrollView>(R.id.lecture_scroll).apply {
            setBackgroundColor(rendu.fond)
            if (!garderPlace) scrollTo(0, 0)
        }
    }

    private var rendu = ReglagesLecture.Rendu(1f, false, 0, 0, 0, 0)

    private fun titre(texte: String, premier: Boolean): View = TextView(this).apply {
        layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT,
        ).apply {
            topMargin = if (premier) 0 else dp(24)
            bottomMargin = dp(12)
            marginStart = dp(16)
            marginEnd = dp(16)
        }
        text = texte
        layoutDirection = View.LAYOUT_DIRECTION_RTL
        textAlignment = View.TEXT_ALIGNMENT_VIEW_START
        textSize = 20f * rendu.echelle
        setTypeface(typeface, android.graphics.Typeface.BOLD)
        setTextColor(rendu.titre)
        PoliceHebraique.appliquer(this)
    }

    /**
     * Les versets d'un psaume en un seul bloc, chacun précédé de son numéro en lettres.
     * Marque-pages (28/10), comme au siddour : un appui long sur un verset le marque ou le démarque ;
     * un verset marqué porte un trait de la couleur des titres et son numéro dans cette couleur.
     */
    private fun texte(n: Int, versets: List<String>, quels: IntRange, marques: Set<Pair<Int, Int>>): View = TextView(this).apply {
        layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT,
        ).apply {
            topMargin = dp(4)
            marginStart = dp(16)
            marginEnd = dp(16)
        }
        val discret = rendu.discret
        val bloc = SpannableStringBuilder()
        val rangs = mutableListOf<Triple<Int, Int, Int>>()
        quels.forEach { v ->
            val verset = versets.getOrNull(v - 1) ?: return@forEach
            // Entre deux versets, une demi-ligne vide : chaque verset se détache (19/10).
            if (bloc.isNotEmpty()) {
                bloc.append("\n")
                val vide = bloc.length
                bloc.append("\n")
                bloc.setSpan(RelativeSizeSpan(0.4f), vide, bloc.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
            val debut = bloc.length
            bloc.append(Reference.lettres(v)).append(" ")
            bloc.setSpan(ForegroundColorSpan(discret), debut, bloc.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            bloc.setSpan(RelativeSizeSpan(0.7f), debut, bloc.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            bloc.append(verset)
            rangs.add(Triple(debut, bloc.length, v))
            if (n to v in marques) {
                bloc.setSpan(ForegroundColorSpan(rendu.titre), debut, debut + Reference.lettres(v).length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                bloc.setSpan(android.text.style.QuoteSpan(rendu.titre), debut, bloc.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }
        text = bloc
        places[n] = this to rangs
        var x = 0f
        var y = 0f
        setOnTouchListener { _, e -> x = e.x; y = e.y; reglages?.toucher(e); false }
        setOnLongClickListener {
            val o = getOffsetForPosition(x, y)
            val (_, _, v) = rangs.firstOrNull { o in it.first..it.second } ?: return@setOnLongClickListener false
            val pose = Store.basculerMarque(this@TehilimLectureActivity, MarquesPages.Marque(CLE, "", n, v,
                "${Reference.lettres(n)}, ${Reference.lettres(v)} · ${MarquesPages.apercu(versets[v - 1], 5)}"))
            android.widget.Toast.makeText(this@TehilimLectureActivity, if (pose) R.string.marque_posee else R.string.marque_retiree, android.widget.Toast.LENGTH_SHORT).show()
            afficher(portion, garderPlace = true)
            true
        }
        layoutDirection = View.LAYOUT_DIRECTION_RTL
        textAlignment = View.TEXT_ALIGNMENT_VIEW_START
        textSize = 19f * rendu.echelle
        setLineSpacing(dp(8).toFloat() * rendu.echelle, 1f)
        setTextColor(rendu.texte)
        setTextIsSelectable(false)
        PoliceHebraique.appliquer(this)
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    companion object {
        private const val DEBUT = "debut"
        private const val FIN = "fin"
        private const val DEBUT_VERSET = "debut_verset"
        private const val FIN_VERSET = "fin_verset"
        private const val MARQUE_VERSET = "marque_verset"

        /** La clé des marque-pages des Tehilim : le psaume en section, le verset en bloc. */
        const val CLE = "t:tehilim"

        /** Ouvre le psaume d'un marque-page, à son verset. */
        fun ouvrirMarque(context: Context, m: MarquesPages.Marque) {
            context.startActivity(
                Intent(context, TehilimLectureActivity::class.java)
                    .putExtra(DEBUT, m.section).putExtra(FIN, m.section).putExtra(MARQUE_VERSET, m.bloc)
            )
        }

        fun ouvrir(context: Context, p: Portion) {
            context.startActivity(
                Intent(context, TehilimLectureActivity::class.java)
                    .putExtra(DEBUT, p.debut)
                    .putExtra(FIN, p.fin)
                    .putExtra(DEBUT_VERSET, p.debutVerset)
                    .putExtra(FIN_VERSET, p.finVerset ?: 0)
            )
        }
    }
}
