package com.myhitbodedut

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Html
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

/**
 * La lecture d'un texte du siddour : les titres de sections, la prière en
 * grand, les consignes plus petites et en gris, et la source à la fin.
 * Cha'harit compte plus de 500 paragraphes : une liste recyclée plutôt
 * qu'une vue par paragraphe. Le bouton de lecture (ReglagesLecture) règle
 * la taille, la lecture de nuit et le nusah ; le texte se redessine sur
 * place, au paragraphe où l'on était.
 */
class SiddurLectureActivity : AppCompatActivity() {

    private sealed class Ligne {
        data class Titre(val texte: String) : Ligne()
        data class Paragraphe(val bloc: SiddurBloc) : Ligne()
        data class Source(val texte: String) : Ligne()
    }

    private lateinit var id: String
    private lateinit var liste: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_siddur_lecture)
        id = intent.getStringExtra(ID).orEmpty()
        liste = findViewById<RecyclerView>(R.id.siddur_lecture_list).apply {
            layoutManager = LinearLayoutManager(this@SiddurLectureActivity)
        }
        ReglagesLecture.installer(this, liste, avecNusah = true) {
            // Même nusah : on garde sa place ; un autre nusah n'a pas les mêmes paragraphes.
            afficher(garderPlace = Data.siddur(this).nusah == nusahAffiche)
        }
        afficher(garderPlace = false)
    }

    private var nusahAffiche: LocalizedText? = null

    private fun afficher(garderPlace: Boolean) {
        val siddur = Data.siddur(this)
        val texte = siddur.texte(id)
        if (texte == null) {
            finish()
            return
        }
        val langue = ChizukCard.language(this)
        findViewById<TextView>(R.id.siddur_lecture_title).apply {
            text = texte.titre.he
            PoliceHebraique.appliquer(this)
        }
        findViewById<TextView>(R.id.siddur_lecture_subtitle).text = listOfNotNull(
            texte.titre.forLanguage(langue).takeIf { it != texte.titre.he },
            (texte.emprunt ?: siddur.nusah).forLanguage(langue),
        ).joinToString(" · ")

        val lignes = texte.sections.flatMap { s ->
            // Une section seule porte déjà le nom du texte : pas de titre répété.
            (if (texte.sections.size > 1 && s.titre.isNotBlank()) listOf(Ligne.Titre(s.titre)) else emptyList()) +
                s.blocs.map { Ligne.Paragraphe(it) }
        } + Ligne.Source(getString(R.string.siddur_source, texte.source))
        val rendu = ReglagesLecture.rendu(this)
        val gestion = liste.layoutManager as LinearLayoutManager
        val place = gestion.findFirstVisibleItemPosition()
        val decalage = gestion.findViewByPosition(place)?.top ?: 0
        nusahAffiche = siddur.nusah
        liste.setBackgroundColor(rendu.fond)
        liste.adapter = Adaptateur(lignes, rendu)
        if (garderPlace && place in lignes.indices) gestion.scrollToPositionWithOffset(place, decalage)
    }

    private inner class Adaptateur(
        private val lignes: List<Ligne>,
        private val rendu: ReglagesLecture.Rendu,
    ) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        override fun getItemCount() = lignes.size

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val vue = TextView(parent.context).apply {
                layoutParams = RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                layoutDirection = View.LAYOUT_DIRECTION_RTL
                textDirection = View.TEXT_DIRECTION_RTL
                textAlignment = View.TEXT_ALIGNMENT_VIEW_START
            }
            return object : RecyclerView.ViewHolder(vue) {}
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val vue = holder.itemView as TextView
            val e = rendu.echelle
            when (val ligne = lignes[position]) {
                is Ligne.Titre -> vue.apply {
                    setPadding(dp(20), dp(if (position == 0) 6 else 26), dp(20), dp(4))
                    text = ligne.texte
                    textSize = 20f * e
                    setLineSpacing(0f, 1f)
                    setTypeface(null, android.graphics.Typeface.BOLD)
                    setTextColor(rendu.titre)
                    PoliceHebraique.appliquer(this)
                }
                is Ligne.Paragraphe -> vue.apply {
                    val consigne = ligne.bloc.consigne
                    setPadding(dp(20), dp(if (consigne) 10 else 8), dp(20), dp(if (consigne) 2 else 4))
                    text = Html.fromHtml(ligne.bloc.texte, Html.FROM_HTML_MODE_LEGACY)
                    textSize = (if (consigne) 14f else 19f) * e
                    setLineSpacing(dp(if (consigne) 3 else 7).toFloat() * e, 1f)
                    setTypeface(null, android.graphics.Typeface.NORMAL)
                    setTextColor(if (consigne) rendu.discret else rendu.texte)
                    PoliceHebraique.appliquer(this)
                }
                is Ligne.Source -> vue.apply {
                    // La source, et sa licence : ce que demande la CC-BY du Metsudah.
                    setPadding(dp(20), dp(28), dp(20), dp(88))
                    text = ligne.texte
                    textSize = 12f
                    textDirection = View.TEXT_DIRECTION_LOCALE
                    textAlignment = View.TEXT_ALIGNMENT_VIEW_START
                    setTypeface(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.NORMAL)
                    setTextColor(rendu.discret)
                }
            }
        }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    companion object {
        private const val ID = "id"

        fun ouvrir(context: Context, id: String) {
            context.startActivity(Intent(context, SiddurLectureActivity::class.java).putExtra(ID, id))
        }
    }
}
