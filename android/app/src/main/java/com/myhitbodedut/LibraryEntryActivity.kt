package com.myhitbodedut

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

/**
 * Une entrée en entier.
 *
 * Une source montre ses paragraphes hébreux, sa référence et un lien vers
 * Sefaria. Un article ne montre qu'une fiche — titre, site, date, résumé —
 * et un bouton vers le site quand l'adresse est connue. Son texte intégral
 * n'apparaît que si une autorisation a été obtenue, ce que LibraryRules
 * décide et que LibraryRulesTest garde.
 */
class LibraryEntryActivity : AppCompatActivity() {

    private lateinit var facts: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_library_entry)

        val library = Data.library(this)
        val entry = library.entry(intent.getStringExtra(EXTRA_ID).orEmpty())
        if (entry == null) {
            finish()
            return
        }
        facts = findViewById(R.id.entry_facts)
        findViewById<ImageView>(R.id.entry_art).setImageResource(Character.ofLibraryEntry(entry))

        when (entry) {
            is SourceEntry -> bindSource(library, entry)
            is ArticleEntry -> bindArticle(entry)
        }
        facts.visibility = if (facts.childCount == 0) View.GONE else View.VISIBLE
    }

    // --- Source ---------------------------------------------------------------

    private fun bindSource(library: Library, source: SourceEntry) {
        findViewById<TextView>(R.id.entry_kind).setText(R.string.library_kind_source)

        val title = findViewById<TextView>(R.id.entry_title)
        title.text = source.titreHe
        title.layoutDirection = View.LAYOUT_DIRECTION_RTL
        title.textAlignment = View.TEXT_ALIGNMENT_VIEW_START

        findViewById<TextView>(R.id.entry_reference).text = source.ref

        val container = findViewById<LinearLayout>(R.id.entry_paragraphs)
        container.visibility = View.VISIBLE
        LibraryRules.paragraphs(library, source).forEachIndexed { index, paragraphe ->
            container.addView(hebrewParagraph(paragraphe, first = index == 0))
        }

        // Ni note ni édition sous le texte : ce sont des détails d'atelier,
        // pas de lecture. Ils restent dans bibliotheque.json, qui est le
        // registre de ce qui a été vérifié pour chaque source ; l'attribution
        // passe par le lien vers Sefaria, juste en dessous.
        linkButton(R.string.library_open_sefaria, source.url)
    }

    /** Un paragraphe hébreu : lu de droite à gauche, quelle que soit l'UI. */
    private fun hebrewParagraph(text: String, first: Boolean): TextView =
        TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply { topMargin = if (first) 0 else dp(14) }
            this.text = text
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            textAlignment = View.TEXT_ALIGNMENT_VIEW_START
            textSize = 17f
            setLineSpacing(dp(4).toFloat(), 1f)
            setTextColor(ContextCompat.getColor(context, R.color.ink))
        }

    // --- Article --------------------------------------------------------------

    private fun bindArticle(article: ArticleEntry) {
        findViewById<TextView>(R.id.entry_kind).setText(R.string.library_kind_article)
        // Le titre est celui de l'article, en hébreu : c'est son vrai nom, pas
        // un texte à traduire — comme le titre d'une source.
        findViewById<TextView>(R.id.entry_title).text = article.titre
        findViewById<TextView>(R.id.entry_reference).text =
            listOfNotNull(article.auteur, article.site, article.date).joinToString(" · ")

        val langue = ChizukCard.language(this)

        LibraryRules.summary(article, langue)?.let { addFact(R.string.library_summary, it) }

        // Le texte intégral n'existe qu'avec une autorisation. Sans elle, on
        // dit pourquoi il manque plutôt que de laisser croire à un oubli.
        // Rien de tout cela ne retombe sur le français : le repli générique
        // (déjà traduit) sert quand la langue en cours manque.
        val integral = LibraryRules.fullText(article)
        if (integral != null) {
            addFact(R.string.library_full_text, integral)
        } else {
            addFact(
                R.string.library_rights,
                LibraryRules.rights(article, langue) ?: getString(R.string.library_full_text_withheld),
            )
        }

        val erreurs = LibraryRules.errors(article, langue)
        if (erreurs.isNotEmpty()) {
            addFact(R.string.library_errors, erreurs.joinToString("\n") { "• $it" })
        }
        linkButton(R.string.library_read_on_site, article.url)
    }

    // --- Briques communes -----------------------------------------------------

    /** Un couple « intitulé / contenu » dans la carte des informations. */
    private fun addFact(labelRes: Int, value: String) {
        if (value.isBlank()) return
        if (facts.childCount > 0) facts.addView(separator())
        facts.addView(
            TextView(this).apply {
                text = getString(labelRes)
                textSize = 12f
                setTextColor(ContextCompat.getColor(context, R.color.brand))
            }
        )
        facts.addView(
            TextView(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                ).apply { topMargin = dp(4) }
                this.text = value
                textSize = 15f
                setLineSpacing(dp(3).toFloat(), 1f)
                setTextColor(ContextCompat.getColor(context, R.color.ink))
            }
        )
    }

    private fun separator(): View = View(this).apply {
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1))
            .apply {
                topMargin = dp(14)
                bottomMargin = dp(14)
            }
        setBackgroundColor(ContextCompat.getColor(context, R.color.line))
    }

    /** Le bouton de sortie vers le web, s'il y a une adresse à ouvrir. */
    private fun linkButton(labelRes: Int, url: String?) {
        val button = findViewById<Button>(R.id.entry_link)
        if (url.isNullOrBlank()) {
            button.visibility = View.GONE
            return
        }
        button.visibility = View.VISIBLE
        button.setText(labelRes)
        button.setOnClickListener {
            // resolveActivity rend null sans déclaration <queries> depuis
            // Android 11 : on tente l'ouverture et on signale si elle échoue.
            runCatching { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
                .onFailure {
                    Toast.makeText(this, R.string.settings_screen_missing, Toast.LENGTH_LONG)
                        .show()
                }
        }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    companion object {
        const val EXTRA_ID = "entry_id"
    }
}
