package com.myhitbodedut

import android.content.Context
import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.Html
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.time.LocalDate
import java.time.LocalTime

/**
 * La lecture d'un texte du siddour : les titres de sections, la prière en
 * grand, les consignes plus petites et en gris. Pas de note de source sous
 * le texte (demande du 22/09) : les sources et leurs licences sont au pied
 * de la liste du siddour. Cha'harit compte plus de 500 paragraphes : une
 * liste recyclée plutôt qu'une vue par paragraphe. Le bouton de lecture
 * (ReglagesLecture) règle la taille, la lecture de nuit et le nusah ; le
 * texte se redessine sur place, au paragraphe où l'on était.
 *
 * En tête, une bande de signets (demande du 10/10) : une section par
 * signet ; on touche, on y est ; la section en cours se marque d'elle-même
 * pendant la lecture (SignetsSiddur).
 *
 * Les marque-pages (15/10) : un appui long sur un paragraphe le marque (un
 * trait à son début) ou le démarque ; le bouton 🔖 liste ceux du texte et y
 * conduit ; la Bibliothèque les reprend tous (MarquesPages, RepriseFavoris).
 *
 * Les ajouts du jour (10/10) : mis en évidence, avec leur nom ; les autres
 * repliés en une ligne, qu'un toucher ouvre et referme (SiddurJour, qui
 * construit les lignes et que les tests vérifient sur la JVM).
 */
class SiddurLectureActivity : AppCompatActivity() {

    /** La note du jour en tête : la date hébraïque qui décide des ajouts. */
    private data class Note(val texte: String)

    /** Les ajouts repliés que l'on a ouverts, pour ce texte. */
    private val ouverts = mutableSetOf<String>()

    private lateinit var id: String
    private lateinit var liste: RecyclerView

    /** La place (section, paragraphe) de chaque paragraphe affiché, par identité. */
    private var places = java.util.IdentityHashMap<SiddurBloc, Pair<Int, Int>>()
    private var lignesAffichees: List<Any> = emptyList()

    /** Les lignes où commencent les sections titrées : un signet chacune. */
    private var debuts: List<Int> = emptyList()

    /** Le signet touché : il reste marqué tant que le doigt ne reprend pas la page. */
    private var choisi: Int? = null
    private var actif = -1
    private var renduBande: ReglagesLecture.Rendu? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_siddur_lecture)
        id = intent.getStringExtra(ID).orEmpty()
        livre = intent.getBooleanExtra(LIVRE, false)
        liste = findViewById<RecyclerView>(R.id.siddur_lecture_list).apply {
            layoutManager = LinearLayoutManager(this@SiddurLectureActivity)
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    if (newState == RecyclerView.SCROLL_STATE_DRAGGING) choisi = null
                }

                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) = suivre()
            })
        }
        ReglagesLecture.installer(this, liste, avecNusah = !livre) {
            // Même nusah : on garde sa place ; un autre nusah n'a pas les mêmes paragraphes.
            afficher(garderPlace = recueil().nusah == nusahAffiche)
        }
        afficher(garderPlace = false)
        // Ouvert depuis un marque-page (15/10) : à sa place ; sinon, reprendre là où on s'était arrêté (14/10).
        val marque = intent.getStringExtra(MARQUE)?.split(':')?.mapNotNull { it.toIntOrNull() }?.takeIf { it.size == 2 }
        if (marque != null) liste.post { allerA(marque[0], marque[1]) }
        else Store.positionLecture(this, cle())?.let { p -> liste.post { (liste.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(p, 0) } }
        findViewById<android.widget.ImageView>(R.id.siddur_lecture_marques).apply {
            // Ruban en contour pour une prière, livre ouvert et ruban pour une étude (15/10).
            setImageResource(icone(livre))
            setOnClickListener { listeMarques() }
        }
        findViewById<TextView>(R.id.siddur_lecture_favori).apply {
            fun etoile() {
                val fav = cle() in Store.favoris(this@SiddurLectureActivity)
                text = if (fav) "★" else "☆"
                contentDescription = getString(if (fav) R.string.favori_retirer else R.string.favori_ajouter)
            }
            etoile()
            setOnClickListener { Store.basculerFavori(this@SiddurLectureActivity, cle()); etoile() }
        }
    }

    /** « s:chaharit » pour le siddour, « l:likoutei_etsot » pour un livre : la clé des favoris et de la reprise. */
    private fun cle(): String = (if (livre) "l:" else "s:") + id

    /** Le noussa'h des marque-pages : celui du siddour affiché ; aucun pour un livre. */
    private fun nusahMarques(): String = if (livre) "" else Store.siddurNusah(this).cle

    private fun marquee(bloc: SiddurBloc): Boolean {
        val (si, bi) = places[bloc] ?: return false
        return MarquesPages.duTexte(Store.marques(this), cle(), nusahMarques()).any { it.section == si && it.bloc == bi }
    }

    private fun basculerMarque(bloc: SiddurBloc) {
        val (si, bi) = places[bloc] ?: return
        val pose = Store.basculerMarque(this, MarquesPages.Marque(cle(), nusahMarques(), si, bi, MarquesPages.apercu(bloc.texte)))
        android.widget.Toast.makeText(this, if (pose) R.string.marque_posee else R.string.marque_retiree, android.widget.Toast.LENGTH_SHORT).show()
        liste.adapter?.notifyDataSetChanged()
    }

    /** Va au paragraphe ; replié aujourd'hui, au titre de sa section. */
    private fun allerA(si: Int, bi: Int) {
        val gestion = liste.layoutManager as LinearLayoutManager
        val i = lignesAffichees.indexOfFirst { it is SiddurJour.Ligne.Bloc && places[it.bloc] == (si to bi) }
        val repli = if (i >= 0) i else lignesAffichees.indexOfFirst { it is SiddurJour.Ligne.Bloc && (places[it.bloc]?.first ?: -1) >= si }
        if (repli >= 0) { choisi = null; liste.stopScroll(); gestion.scrollToPositionWithOffset(repli, 0) }
    }

    /** La liste des marque-pages du texte ; vide, comment en poser un. */
    private fun listeMarques() {
        val marques = MarquesPages.duTexte(Store.marques(this), cle(), nusahMarques())
        val dialogue = com.google.android.material.dialog.MaterialAlertDialogBuilder(this).setTitle(R.string.marques_titre)
        if (marques.isEmpty()) dialogue.setMessage(R.string.marques_aucun)
        else dialogue.setItems(marques.map { "🔖  ${it.apercu}…" }.toTypedArray()) { _, k -> allerA(marques[k].section, marques[k].bloc) }
        dialogue.setPositiveButton(android.R.string.ok, null).show()
    }

    override fun onPause() {
        super.onPause()
        val p = (liste.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()
        if (p >= 0) Store.setPositionLecture(this, cle(), p)
    }

    private var nusahAffiche: LocalizedText? = null

    /** Un livre de la bibliothèque (Likoutei Etsot, Sefer HaMiddot, 12/10), lu comme une prière, sans nusah ni ajouts du jour. */
    private var livre = false

    private fun recueil(): Siddur = if (livre) Data.livres(this) else Data.siddur(this)

    private fun afficher(garderPlace: Boolean) {
        val siddur = recueil()
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
        val etiquettes = listOfNotNull(
            texte.titre.forLanguage(langue).takeIf { it != texte.titre.he },
            (texte.emprunt ?: siddur.nusah).forLanguage(langue),
        )
        findViewById<TextView>(R.id.siddur_lecture_subtitle).text = etiquettes.joinToString(" · ")
        // La bannière (16/10) : des étiquettes courtes plutôt qu'une phrase coupée, et l'horaire du jour.
        findViewById<com.google.android.material.chip.ChipGroup>(R.id.siddur_lecture_etiquettes).apply {
            removeAllViews()
            contentDescription = etiquettes.joinToString(", ")
            etiquettes.forEach { e ->
                addView(com.google.android.material.chip.Chip(context).apply {
                    text = e
                    isClickable = false
                    isCheckable = false
                    isFocusable = false
                    setEnsureMinTouchTargetSize(false)
                    chipMinHeight = 28 * resources.displayMetrics.density
                    textSize = 13f
                    setTextColor(android.graphics.Color.WHITE)
                    chipBackgroundColor = android.content.res.ColorStateList.valueOf(0x22FFFFFF)
                    chipStrokeColor = android.content.res.ColorStateList.valueOf(0x40FFFFFF)
                    chipStrokeWidth = resources.displayMetrics.density
                    importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                })
            }
        }
        findViewById<TextView>(R.id.siddur_lecture_horaire).apply {
            val h = if (livre) null else HorairePriere.texte(this@SiddurLectureActivity, id)
            text = h?.let { "◷ $it" }
            visibility = if (h == null) View.GONE else View.VISIBLE
        }

        // Le jour hébreu de cette prière : Arvit, l'après-midi, est déjà celui de demain.
        val israel = Store.inIsrael(this)
        val jour = CalendrierJuif.jour(CalendrierJuif.jourDeLaPriere(id, LocalDate.now(), LocalTime.now()), israel,
            Store.mukafChoma(this))
        val annotations = if (livre) emptyList() else Data.ajouts(this)[id].orEmpty()
        val note = if (annotations.isEmpty()) emptyList() else listOf(Note(getString(R.string.siddur_jour_ajouts,
            jour.dateHebraique, getString(if (israel) R.string.region_israel else R.string.region_diaspora))))
        val lignes: List<Any> = note + SiddurJour.lignes(texte, annotations, jour.conditions, ouverts,
            Store.siddurNusah(this).cle, israel, Store.sansTahanounFinTichri(this), Store.hemisphereSud(this))
        val rendu = ReglagesLecture.rendu(this)
        val gestion = liste.layoutManager as LinearLayoutManager
        val place = gestion.findFirstVisibleItemPosition()
        val decalage = gestion.findViewByPosition(place)?.top ?: 0
        nusahAffiche = siddur.nusah
        places = java.util.IdentityHashMap<SiddurBloc, Pair<Int, Int>>().apply {
            texte.sections.forEachIndexed { si, s -> s.blocs.forEachIndexed { bi, b -> put(b, si to bi) } }
        }
        lignesAffichees = lignes
        liste.setBackgroundColor(rendu.fond)
        liste.adapter = Adaptateur(lignes, rendu)
        if (garderPlace && place in lignes.indices) gestion.scrollToPositionWithOffset(place, decalage)
        debuts = lignes.indices.filter { lignes[it] is SiddurJour.Ligne.Titre }
        bande(lignes, rendu)
        liste.post { suivre() }
    }

    /** La bande de signets, rebâtie avec le texte : un autre nusah, la nuit, une autre taille. */
    private fun bande(lignes: List<Any>, rendu: ReglagesLecture.Rendu) {
        val defile = findViewById<HorizontalScrollView>(R.id.siddur_lecture_signets_defile)
        val trait = findViewById<View>(R.id.siddur_lecture_signets_trait)
        val rang = findViewById<LinearLayout>(R.id.siddur_lecture_signets)
        rang.removeAllViews()
        actif = -1
        choisi = null
        renduBande = rendu
        val presente = debuts.size > 1
        defile.visibility = if (presente) View.VISIBLE else View.GONE
        trait.visibility = defile.visibility
        if (!presente) return
        defile.setBackgroundColor(rendu.fond)
        defile.contentDescription = getString(R.string.siddur_signets)
        trait.setBackgroundColor(ColorUtils.setAlphaComponent(rendu.discret, 0x40))
        val ondulation = TypedValue().also {
            theme.resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, it, true)
        }.resourceId
        debuts.forEachIndexed { i, position ->
            rang.addView(TextView(this).apply {
                text = (lignes[position] as SiddurJour.Ligne.Titre).texte
                textSize = 13f
                setPadding(dp(10), dp(4), dp(10), dp(4))
                // 48 dp de haut : une cible qu'on touche sans viser (audit du 16/10).
                minHeight = dp(48)
                gravity = android.view.Gravity.CENTER
                setTextColor(rendu.discret)
                PoliceHebraique.appliquer(this)
                foreground = ContextCompat.getDrawable(context, ondulation)
                setOnClickListener {
                    choisi = i
                    liste.stopScroll()
                    (liste.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(position, 0)
                    marquer(i)
                }
            })
        }
    }

    /** Marque le signet de la section qu'on lit. */
    private fun suivre() {
        if (debuts.size < 2) return
        val gestion = liste.layoutManager as LinearLayoutManager
        val premiere = gestion.findFirstVisibleItemPosition()
        if (premiere == RecyclerView.NO_POSITION) return
        marquer(choisi ?: SignetsSiddur.courant(
            debuts, premiere, gestion.findLastVisibleItemPosition(), !liste.canScrollVertically(1),
        ))
    }

    /** Une pastille pâle sous le signet en cours, rien sous les autres ; la bande le garde en vue. */
    private fun marquer(i: Int) {
        val rendu = renduBande ?: return
        if (i == actif) return
        actif = i
        val rang = findViewById<LinearLayout>(R.id.siddur_lecture_signets)
        for (k in 0 until rang.childCount) (rang.getChildAt(k) as TextView).apply {
            val oui = k == i
            isSelected = oui
            setTextColor(if (oui) rendu.titre else rendu.discret)
            background = if (oui) GradientDrawable().apply {
                cornerRadius = dp(999).toFloat()
                setColor(ColorUtils.setAlphaComponent(rendu.titre, 0x26))
            } else null
        }
        val defile = findViewById<HorizontalScrollView>(R.id.siddur_lecture_signets_defile)
        val vue = rang.getChildAt(i) ?: return
        defile.post { defile.smoothScrollTo(vue.left - (defile.width - vue.width) / 2, 0) }
    }

    /** Un libellé (clé de chaîne) dans la langue de l'application. */
    private fun nom(libelle: String): String = Libelles.res(libelle)?.let { getString(it) } ?: libelle

    private inner class Adaptateur(
        private val lignes: List<Any>,
        private val rendu: ReglagesLecture.Rendu,
    ) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        override fun getItemCount() = lignes.size

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val vue = TextView(parent.context).apply {
                layoutParams = RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            }
            return object : RecyclerView.ViewHolder(vue) {}
        }

        private val teinte = ColorUtils.setAlphaComponent(rendu.titre, 0x1C)

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val vue = holder.itemView as TextView
            val e = rendu.echelle
            // Tout est remis à chaque fois : une vue recyclée garde les réglages d'avant.
            vue.apply {
                background = null
                setCompoundDrawablesRelative(null, null, null, null)
                alpha = 1f
                contentDescription = null
                setOnClickListener(null)
                setOnLongClickListener(null)
                isClickable = false
                isLongClickable = false
                layoutDirection = View.LAYOUT_DIRECTION_RTL
                textDirection = View.TEXT_DIRECTION_RTL
                textAlignment = View.TEXT_ALIGNMENT_VIEW_START
                setLineSpacing(0f, 1f)
                typeface = android.graphics.Typeface.DEFAULT
            }
            when (val ligne = lignes[position]) {
                is Note -> vue.apply {
                    layoutDirection = View.LAYOUT_DIRECTION_LOCALE
                    textDirection = View.TEXT_DIRECTION_LOCALE
                    setPadding(dp(20), dp(10), dp(20), dp(2))
                    text = ligne.texte
                    textSize = 12f
                    setTextColor(rendu.discret)
                }
                is SiddurJour.Ligne.Titre -> vue.apply {
                    PoliceHebraique.appliquer(this)
                    setPadding(dp(20), dp(if (position == 0) 6 else 26), dp(20), dp(4))
                    text = ligne.texte
                    textSize = 20f * e
                    setTypeface(typeface, android.graphics.Typeface.BOLD)
                    setTextColor(rendu.titre)
                }
                is SiddurJour.Ligne.Bloc -> vue.apply {
                    PoliceHebraique.appliquer(this)
                    val consigne = ligne.bloc.consigne
                    // Des paragraphes aérés ; les consignes, petites, collées au texte qu'elles annoncent (12/10).
                    setPadding(dp(20), dp(if (consigne) 18 else 14), dp(20), dp(if (consigne) 2 else 12))
                    text = Html.fromHtml(ligne.bloc.texte, Html.FROM_HTML_MODE_LEGACY)
                    textSize = (if (consigne) 12f else 19f) * e
                    setLineSpacing(dp(if (consigne) 2 else 10).toFloat() * e, 1f)
                    setTextColor(if (consigne) rendu.discret else rendu.texte)
                    if (ligne.style == SiddurJour.Style.DU_JOUR) setBackgroundColor(teinte)
                    if (ligne.style == SiddurJour.Style.HORS) alpha = 0.55f
                    // Le marque-page : un trait de la couleur des titres au début du paragraphe.
                    if (marquee(ligne.bloc)) background = android.graphics.drawable.LayerDrawable(arrayOf(
                        background ?: android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT),
                        android.graphics.drawable.ColorDrawable(rendu.titre))).apply {
                        setLayerWidth(1, dp(4)); setLayerGravity(1, android.view.Gravity.START or android.view.Gravity.FILL_VERTICAL)
                    }
                    if (marquee(ligne.bloc)) setCompoundDrawablesRelative(null, null,
                        ContextCompat.getDrawable(context, icone(livre))?.mutate()?.apply {
                            setTint(rendu.titre); setBounds(0, 0, dp(16), dp(16))
                        }, null)
                    compoundDrawablePadding = dp(6)
                    setOnLongClickListener { basculerMarque(ligne.bloc); true }
                }
                is SiddurJour.Ligne.Etiquette -> vue.apply {
                    layoutDirection = View.LAYOUT_DIRECTION_LOCALE
                    textDirection = View.TEXT_DIRECTION_LOCALE
                    setPadding(dp(20), dp(10), dp(20), dp(2))
                    text = getString(R.string.siddur_aujourdhui) + " · " + nom(ligne.libelle)
                    textSize = 12f
                    setTypeface(typeface, android.graphics.Typeface.BOLD)
                    setTextColor(rendu.titre)
                    setBackgroundColor(teinte)
                }
                is SiddurJour.Ligne.Indice -> vue.apply {
                    PoliceHebraique.appliquer(this)
                    setPadding(dp(20), dp(0), dp(20), dp(8))
                    text = getString(R.string.siddur_aujourdhui) + " : " + ligne.texte
                    textSize = 12f * e
                    setTextColor(rendu.titre)
                }
                is SiddurJour.Ligne.Renvoi -> vue.apply {
                    // « Aujourd'hui · הַלֵּל › » : le texte de ce jour, à cet endroit, ouvert d'un geste.
                    val cible = recueil().texte(ligne.cible)
                    PoliceHebraique.appliquer(this)
                    setPadding(dp(20), dp(12), dp(20), dp(12))
                    text = getString(R.string.siddur_aujourdhui) + " · " + (cible?.titre?.he ?: ligne.cible) + "  ›"
                    textSize = 15f * e
                    setTypeface(typeface, android.graphics.Typeface.BOLD)
                    setTextColor(rendu.titre)
                    setBackgroundColor(teinte)
                    contentDescription = text
                    if (cible != null) setOnClickListener { ouvrir(context, cible.id) }
                }
                is SiddurJour.Ligne.Replie -> vue.apply {
                    layoutDirection = View.LAYOUT_DIRECTION_LOCALE
                    textDirection = View.TEXT_DIRECTION_LOCALE
                    setPadding(dp(20), dp(10), dp(20), dp(10))
                    val rtl = resources.configuration.layoutDirection == View.LAYOUT_DIRECTION_RTL
                    val fleche = if (ligne.ouvert) "▾" else if (rtl) "◂" else "▸"
                    val etat = getString(if (ligne.ouvert) R.string.siddur_replier else R.string.siddur_pas_aujourdhui)
                    text = "$fleche  ${nom(ligne.libelle)}  ·  $etat"
                    textSize = 14f
                    setTextColor(rendu.discret)
                    background = GradientDrawable().apply {
                        cornerRadius = dp(12).toFloat()
                        setStroke(dp(1), ColorUtils.setAlphaComponent(rendu.discret, 0x73), dp(5).toFloat(), dp(4).toFloat())
                    }
                    contentDescription = "${nom(ligne.libelle)}, $etat"
                    setOnClickListener {
                        if (!ouverts.remove(ligne.cle)) ouverts.add(ligne.cle)
                        afficher(garderPlace = true)
                    }
                }
            }
        }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    companion object {
        private const val ID = "id"
        private const val LIVRE = "livre"
        private const val MARQUE = "marque"

        /** L'icône des marque-pages : prière (siddour) ou étude (livre). */
        fun icone(etude: Boolean) = if (etude) R.drawable.ic_marque_etude else R.drawable.ic_marque_priere

        /** Ouvre le texte d'un marque-page, à sa place. */
        fun ouvrirMarque(context: Context, m: MarquesPages.Marque) {
            val i = Intent(context, SiddurLectureActivity::class.java).putExtra(ID, m.cle.substringAfter(':'))
                .putExtra(MARQUE, "${m.section}:${m.bloc}")
            if (m.cle.startsWith("l:")) i.putExtra(LIVRE, true)
            context.startActivity(i)
        }

        fun intent(context: Context, id: String): Intent = Intent(context, SiddurLectureActivity::class.java).putExtra(ID, id)

        fun ouvrir(context: Context, id: String) {
            context.startActivity(intent(context, id))
        }

        fun ouvrirLivre(context: Context, id: String) {
            context.startActivity(Intent(context, SiddurLectureActivity::class.java).putExtra(ID, id).putExtra(LIVRE, true))
        }
    }
}
