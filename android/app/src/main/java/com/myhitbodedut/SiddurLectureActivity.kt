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
 * grand, les consignes plus petites et en gris. Cha'harit compte plus de
 * 350 paragraphes : une liste recyclée plutôt qu'une vue par paragraphe.
 */
class SiddurLectureActivity : AppCompatActivity() {

    private sealed class Ligne {
        data class Titre(val texte: String) : Ligne()
        data class Paragraphe(val bloc: SiddurBloc) : Ligne()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_siddur_lecture)
        val texte = Data.siddur(this).texte(intent.getStringExtra(ID).orEmpty())
        if (texte == null) {
            finish()
            return
        }
        val langue = ChizukCard.language(this)
        findViewById<TextView>(R.id.siddur_lecture_title).apply {
            text = texte.titre.he
            PoliceHebraique.appliquer(this)
        }
        findViewById<TextView>(R.id.siddur_lecture_subtitle).text =
            texte.titre.forLanguage(langue).takeIf { it != texte.titre.he } ?: getString(R.string.siddur_nusah)

        val lignes = texte.sections.flatMap { s ->
            // Une section seule porte déjà le nom du texte : pas de titre répété.
            (if (texte.sections.size > 1 && s.titre.isNotBlank()) listOf(Ligne.Titre(s.titre)) else emptyList()) +
                s.blocs.map { Ligne.Paragraphe(it) }
        }
        findViewById<RecyclerView>(R.id.siddur_lecture_list).apply {
            layoutManager = LinearLayoutManager(this@SiddurLectureActivity)
            adapter = Adaptateur(lignes)
        }
    }

    private inner class Adaptateur(private val lignes: List<Ligne>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        override fun getItemCount() = lignes.size

        override fun getItemViewType(position: Int) = if (lignes[position] is Ligne.Titre) 0 else 1

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
            when (val ligne = lignes[position]) {
                is Ligne.Titre -> vue.apply {
                    setPadding(dp(20), dp(if (position == 0) 6 else 26), dp(20), dp(4))
                    text = ligne.texte
                    textSize = 20f
                    setLineSpacing(0f, 1f)
                    setTypeface(null, android.graphics.Typeface.BOLD)
                    setTextColor(Apparence.couleur(context, R.color.brand))
                    PoliceHebraique.appliquer(this)
                }
                is Ligne.Paragraphe -> vue.apply {
                    val consigne = ligne.bloc.consigne
                    setPadding(dp(20), dp(if (consigne) 10 else 8), dp(20), dp(if (consigne) 2 else 4))
                    text = Html.fromHtml(ligne.bloc.texte, Html.FROM_HTML_MODE_LEGACY)
                    textSize = if (consigne) 14f else 19f
                    setLineSpacing(dp(if (consigne) 3 else 7).toFloat(), 1f)
                    setTypeface(null, android.graphics.Typeface.NORMAL)
                    setTextColor(Apparence.couleur(context, if (consigne) R.color.muted else R.color.ink))
                    PoliceHebraique.appliquer(this)
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
