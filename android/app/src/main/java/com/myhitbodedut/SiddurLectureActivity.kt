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

    /** Le pied d'un ajout déplié (19/10) : « Replier », pour ne pas remonter le chercher. */
    private data class Pied(val cle: String)

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

    /** Le gras qui ouvre le paragraphe, un quart plus grand. */
    private fun premierMotAgrandi(texte: android.text.Spanned): CharSequence {
        val s = android.text.SpannableStringBuilder(texte)
        s.getSpans(0, minOf(1, s.length), android.text.style.StyleSpan::class.java)
            .firstOrNull { s.getSpanStart(it) == 0 && it.style == android.graphics.Typeface.BOLD }
            ?.let { s.setSpan(android.text.style.RelativeSizeSpan(1.25f), 0, s.getSpanEnd(it), android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE) }
        // Une ligne vide dans un paragraphe (aération) : réduite, plus courte que l'écart entre deux paragraphes.
        var i = s.indexOf("\n\n")
        while (i >= 0) {
            s.setSpan(android.text.style.RelativeSizeSpan(0.3f), i + 1, i + 2, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            i = s.indexOf("\n\n", i + 2)
        }
        return s
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
                // Une pastille simple, pas une Chip : la Chip Material pose sa couleur de surface
                // (opaque) sous le fond translucide — pastilles blanches, texte blanc invisible (captures du 16/10).
                val d = resources.displayMetrics.density
                addView(TextView(context).apply {
                    text = e
                    textSize = 13f
                    maxLines = 1
                    ellipsize = android.text.TextUtils.TruncateAt.END
                    setTextColor(android.graphics.Color.WHITE)
                    gravity = android.view.Gravity.CENTER_VERTICAL
                    minHeight = (28 * d).toInt()
                    setPadding((12 * d).toInt(), (4 * d).toInt(), (12 * d).toInt(), (4 * d).toInt())
                    background = android.graphics.drawable.GradientDrawable().apply {
                        cornerRadius = 14 * d
                        setColor(0x26FFFFFF)
                        setStroke(d.toInt().coerceAtLeast(1), 0x55FFFFFF)
                    }
                    importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                })
            }
        }
        findViewById<TextView>(R.id.siddur_lecture_horaire).apply {
            val h = if (livre) null else HorairePriere.texte(this@SiddurLectureActivity, this@SiddurLectureActivity.id)
            text = h?.let { "◷ $it" }
            visibility = if (h == null) View.GONE else View.VISIBLE
        }

        // Le jour hébreu de cette prière : Arvit, l'après-midi, est déjà celui de demain.
        val israel = Store.inIsrael(this)
        // Après la sortie des étoiles du lieu, les autres textes relèvent aussi de demain (28/10).
        val apresSortie = Store.lieuZmanim(this)?.let { lieu ->
            Zmanim.jour(lieu, LocalDate.now()).firstOrNull { it.cle == "tset" }?.instant?.let { !java.time.Instant.now().isBefore(it) }
        } ?: false
        val jour = CalendrierJuif.jour(CalendrierJuif.jourDeLaPriere(id, LocalDate.now(), LocalTime.now(), apresSortie), israel,
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
        // Un ajout déplié se lit dans un panneau, fermé par un pied « Replier » (19/10).
        val avecPieds = ArrayList<Any>(lignes.size + 4)
        val panneau = HashSet<Int>()
        var ouvert: String? = null
        for (l in lignes) {
            val dedans = l is SiddurJour.Ligne.Bloc && l.style == SiddurJour.Style.HORS
            if (ouvert != null && !dedans) { avecPieds += Pied(ouvert); ouvert = null }
            if (ouvert != null) panneau += avecPieds.size
            avecPieds += l
            if (l is SiddurJour.Ligne.Replie && l.ouvert) ouvert = l.cle
        }
        ouvert?.let { avecPieds += Pied(it) }
        lignesAffichees = avecPieds
        liste.setBackgroundColor(rendu.fond)
        // Déplier, replier : les paragraphes glissent en place au lieu d'apparaître d'un coup.
        liste.itemAnimator = if (AnimationsTrophees.actives) androidx.recyclerview.widget.DefaultItemAnimator().apply {
            supportsChangeAnimations = false
            addDuration = 260
            removeDuration = 180
            moveDuration = 260
        } else null
        val ancien = liste.adapter as? Adaptateur
        if (garderPlace && ancien != null && ancien.rendu == rendu) {
            ancien.remplacer(avecPieds, panneau)
        } else {
            liste.adapter = Adaptateur(avecPieds, panneau, rendu)
            if (garderPlace && place in avecPieds.indices) gestion.scrollToPositionWithOffset(place, decalage)
        }
        debuts = avecPieds.indices.filter { avecPieds[it] is SiddurJour.Ligne.Titre }
        bande(avecPieds, rendu)
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
                setPadding(dp(8), dp(4), dp(8), dp(4))
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

    /** Les ajouts qui ne sont jamais « du jour » : on dit « toucher pour lire », pas « pas aujourd'hui ». */
    private val jamaisDuJour = setOf(CalendrierJuif.C.MINYAN, CalendrierJuif.C.BEIT_AVEL, CalendrierJuif.C.OUBLI, CalendrierJuif.C.CHEVA_BERAKHOT)

    /** Le chevron d'un repli, tourné d'un quart de tour quand l'ajout est ouvert ; [haut] : vers le haut. */
    private fun chevron(couleur: Int, ouvert: Boolean, haut: Boolean = false): android.graphics.drawable.RotateDrawable {
        val rtl = resources.configuration.layoutDirection == View.LAYOUT_DIRECTION_RTL
        val sens = if (rtl) -1f else 1f
        return android.graphics.drawable.RotateDrawable().apply {
            drawable = ContextCompat.getDrawable(this@SiddurLectureActivity, R.drawable.ic_chevron)?.mutate()?.apply { setTint(couleur) }
            fromDegrees = if (haut) -90f * sens else 0f
            toDegrees = if (haut) -90f * sens else 90f * sens
            level = if (ouvert || haut) 10000 else 0
            setBounds(0, 0, dp(22), dp(22))
        }
    }

    /** Un fond en panneau : [haut], [bas] arrondis ou non, en retrait des bords. */
    private fun panneau(couleur: Int, haut: Boolean, bas: Boolean, trait: Int? = null, marge: Int = 0): android.graphics.drawable.Drawable {
        val r = dp(16).toFloat()
        val forme = GradientDrawable().apply {
            setColor(couleur)
            cornerRadii = floatArrayOf(if (haut) r else 0f, if (haut) r else 0f, if (haut) r else 0f, if (haut) r else 0f,
                if (bas) r else 0f, if (bas) r else 0f, if (bas) r else 0f, if (bas) r else 0f)
            if (trait != null) setStroke(dp(1), trait)
        }
        // Un LayerDrawable plutôt qu'un InsetDrawable : ses retraits ne deviennent pas le padding de la vue.
        return android.graphics.drawable.LayerDrawable(arrayOf(forme)).apply {
            setLayerInset(0, dp(8), if (haut) marge else 0, dp(8), if (bas) marge else 0)
        }
    }

    private fun basculer(cle: String) {
        if (!ouverts.remove(cle)) ouverts.add(cle)
        afficher(garderPlace = true)
    }

    private inner class Adaptateur(
        private var lignes: List<Any>,
        private var dansPanneau: Set<Int>,
        val rendu: ReglagesLecture.Rendu,
    ) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        override fun getItemCount() = lignes.size

        /** Le nouveau texte, à la place de l'ancien : seules les lignes changées bougent, animées. */
        fun remplacer(nouvelles: List<Any>, panneau: Set<Int>) {
            val anciennes = lignes
            val diff = androidx.recyclerview.widget.DiffUtil.calculateDiff(object : androidx.recyclerview.widget.DiffUtil.Callback() {
                override fun getOldListSize() = anciennes.size
                override fun getNewListSize() = nouvelles.size
                override fun areItemsTheSame(a: Int, b: Int): Boolean {
                    val x = anciennes[a]
                    val y = nouvelles[b]
                    return when {
                        x is SiddurJour.Ligne.Bloc && y is SiddurJour.Ligne.Bloc -> x.bloc === y.bloc
                        x is SiddurJour.Ligne.Replie && y is SiddurJour.Ligne.Replie -> x.cle == y.cle
                        else -> x == y
                    }
                }
                override fun areContentsTheSame(a: Int, b: Int) = anciennes[a] == nouvelles[b] && (a in dansPanneau) == (b in panneau)
            })
            lignes = nouvelles
            dansPanneau = panneau
            diff.dispatchUpdatesTo(this)
        }

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
                minHeight = 0
                gravity = android.view.Gravity.TOP or android.view.Gravity.START
                compoundDrawablePadding = 0
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
                    setPadding(dp(16), dp(8), dp(16), dp(0))
                    text = ligne.texte
                    textSize = 12f
                    setTextColor(rendu.discret)
                }
                is SiddurJour.Ligne.Titre -> vue.apply {
                    PoliceHebraique.appliquer(this)
                    setPadding(dp(16), dp(if (position == 0) 8 else 32), dp(16), dp(8))
                    text = ligne.texte
                    textSize = 20f * e
                    setTypeface(typeface, android.graphics.Typeface.BOLD)
                    setTextColor(rendu.titre)
                }
                is SiddurJour.Ligne.Bloc -> vue.apply {
                    PoliceHebraique.appliquer(this)
                    val consigne = ligne.bloc.consigne
                    // Des paragraphes aérés ; les consignes, petites, collées au texte qu'elles annoncent (12/10).
                    setPadding(dp(16), dp(if (consigne) 24 else 14), dp(16), dp(if (consigne) 4 else 26))
                    // Aéré (28/10) : les longs paragraphes coupés aux fins de phrase, le premier mot en gras, agrandi.
                    text = if (consigne) Html.fromHtml(ligne.bloc.texte, Html.FROM_HTML_MODE_LEGACY)
                    else premierMotAgrandi(Html.fromHtml(Aeration.aerer(ligne.bloc.texte), Html.FROM_HTML_MODE_LEGACY))
                    textSize = (if (consigne) 12f else 19f) * e
                    setLineSpacing(dp(if (consigne) 3 else 8).toFloat() * e, 1f)
                    setTextColor(if (consigne) rendu.discret else rendu.texte)
                    if (ligne.style == SiddurJour.Style.DU_JOUR) setBackgroundColor(teinte)
                    // Déplié : le texte entier, lisible, dans le panneau de son repli ; une variante
                    // hors du jour au milieu d'un ajout du jour reste estompée.
                    if (position in dansPanneau) background = panneau(voile, haut = false, bas = false)
                    else if (ligne.style == SiddurJour.Style.HORS) alpha = 0.55f
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
                    setPadding(dp(16), dp(8), dp(16), dp(0))
                    text = getString(R.string.siddur_aujourdhui) + " · " + nom(ligne.libelle)
                    textSize = 12f
                    setTypeface(typeface, android.graphics.Typeface.BOLD)
                    setTextColor(rendu.titre)
                    setBackgroundColor(teinte)
                }
                is SiddurJour.Ligne.Indice -> vue.apply {
                    PoliceHebraique.appliquer(this)
                    setPadding(dp(16), dp(0), dp(16), dp(8))
                    text = getString(R.string.siddur_aujourdhui) + " : " + ligne.texte
                    textSize = 12f * e
                    setTextColor(rendu.titre)
                }
                is SiddurJour.Ligne.Renvoi -> vue.apply {
                    // « Aujourd'hui · הַלֵּל › » : le texte de ce jour, à cet endroit, ouvert d'un geste.
                    val cible = recueil().texte(ligne.cible)
                    PoliceHebraique.appliquer(this)
                    setPadding(dp(16), dp(12), dp(16), dp(12))
                    text = getString(R.string.siddur_aujourdhui) + " · " + (cible?.titre?.he ?: ligne.cible) + "  ›"
                    textSize = 15f * e
                    setTypeface(typeface, android.graphics.Typeface.BOLD)
                    setTextColor(rendu.titre)
                    setBackgroundColor(teinte)
                    contentDescription = text
                    if (cible != null) setOnClickListener { ouvrir(context, cible.id) }
                }
                is SiddurJour.Ligne.Replie -> vue.apply {
                    // Le bandeau d'un ajout replié (19/10) : son nom, et dessous ce qu'un toucher fait.
                    layoutDirection = View.LAYOUT_DIRECTION_LOCALE
                    textDirection = View.TEXT_DIRECTION_LOCALE
                    val etat = getString(when {
                        ligne.ouvert -> R.string.siddur_replier
                        ligne.condition in jamaisDuJour -> R.string.siddur_toucher_lire
                        else -> R.string.siddur_pas_aujourdhui
                    })
                    val titre = nom(ligne.libelle)
                    text = android.text.SpannableStringBuilder(titre).apply {
                        setSpan(android.text.style.StyleSpan(android.graphics.Typeface.BOLD), 0, length, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                        append("\n")
                        val d = length
                        append(etat)
                        setSpan(android.text.style.RelativeSizeSpan(0.82f), d, length, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                        setSpan(android.text.style.ForegroundColorSpan(rendu.discret), d, length, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    }
                    textSize = 15f
                    setTextColor(rendu.texte)
                    setLineSpacing(dp(2).toFloat(), 1f)
                    gravity = android.view.Gravity.CENTER_VERTICAL or android.view.Gravity.START
                    background = if (ligne.ouvert) panneau(fondOuvert, haut = true, bas = false, marge = dp(6))
                    else panneau(fondFerme, haut = true, bas = true, trait = traitFerme, marge = dp(6))
                    setPadding(dp(24), dp(18), dp(24), dp(if (ligne.ouvert) 12 else 18))
                    minHeight = dp(64)
                    val fleche = chevron(rendu.titre, ligne.ouvert)
                    setCompoundDrawablesRelative(fleche, null, null, null)
                    compoundDrawablePadding = dp(14)
                    contentDescription = "$titre, $etat"
                    setOnClickListener {
                        // Le chevron tourne d'abord, puis le texte se déplie (ou se replie).
                        if (!AnimationsTrophees.actives) return@setOnClickListener basculer(ligne.cle)
                        isClickable = false
                        android.animation.ObjectAnimator.ofInt(fleche, "level", fleche.level, if (ligne.ouvert) 0 else 10000).apply {
                            duration = 160
                            interpolator = android.view.animation.DecelerateInterpolator()
                            addListener(object : android.animation.AnimatorListenerAdapter() {
                                override fun onAnimationEnd(animation: android.animation.Animator) = basculer(ligne.cle)
                            })
                        }.start()
                    }
                }
                is SiddurJour.Ligne.Seul -> vue.apply {
                    // Qui prie seul (19/10) : une carte à part, jamais repliée ; la consigne petite, la braïta en grand.
                    PoliceHebraique.appliquer(this)
                    val etiquette = getString(R.string.siddur_seul)
                    text = android.text.SpannableStringBuilder(etiquette).apply {
                        setSpan(android.text.style.StyleSpan(android.graphics.Typeface.BOLD), 0, length, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                        setSpan(android.text.style.ForegroundColorSpan(rendu.titre), 0, length, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                        setSpan(android.text.style.RelativeSizeSpan(0.68f), 0, length, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                        ligne.consigne?.let { c ->
                            append("\n")
                            val d = length
                            append(c)
                            setSpan(android.text.style.RelativeSizeSpan(0.68f), d, length, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                            setSpan(android.text.style.ForegroundColorSpan(rendu.discret), d, length, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                        }
                        append("\n")
                        val d = length
                        append(ligne.texte)
                        if (ligne.consigne == null) {
                            setSpan(android.text.style.RelativeSizeSpan(0.75f), d, length, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                            setSpan(android.text.style.ForegroundColorSpan(rendu.discret), d, length, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                        }
                    }
                    textSize = 19f * e
                    setTextColor(rendu.texte)
                    setLineSpacing(dp(6).toFloat() * e, 1f)
                    background = panneau(voile, haut = true, bas = true, trait = traitFerme, marge = dp(8))
                    setPadding(dp(24), dp(20), dp(24), dp(20))
                }
                is Pied -> vue.apply {
                    layoutDirection = View.LAYOUT_DIRECTION_LOCALE
                    textDirection = View.TEXT_DIRECTION_LOCALE
                    text = getString(R.string.siddur_replier)
                    textSize = 14f
                    setTypeface(typeface, android.graphics.Typeface.BOLD)
                    setTextColor(rendu.titre)
                    gravity = android.view.Gravity.CENTER
                    background = panneau(voile, haut = false, bas = true, marge = dp(10))
                    setPadding(dp(24), dp(8), dp(24), dp(18))
                    minHeight = dp(56)
                    setCompoundDrawablesRelative(chevron(rendu.titre, ouvert = false, haut = true), null, null, null)
                    compoundDrawablePadding = dp(6)
                    contentDescription = getString(R.string.siddur_replier)
                    setOnClickListener {
                        basculer(ligne.cle)
                        // Replié depuis le bas : on revient à son bandeau s'il est passé au-dessus de l'écran.
                        val i = lignesAffichees.indexOfFirst { it is SiddurJour.Ligne.Replie && it.cle == ligne.cle }
                        val gestion = liste.layoutManager as LinearLayoutManager
                        if (i >= 0 && i < gestion.findFirstVisibleItemPosition()) liste.post { liste.smoothScrollToPosition(i) }
                    }
                }
            }
        }

        private val voile = ColorUtils.setAlphaComponent(rendu.titre, 0x10)
        private val fondOuvert = ColorUtils.setAlphaComponent(rendu.titre, 0x1E)
        private val fondFerme = ColorUtils.setAlphaComponent(rendu.titre, 0x12)
        private val traitFerme = ColorUtils.setAlphaComponent(rendu.titre, 0x38)
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
            if (m.cle == TehilimLectureActivity.CLE) return TehilimLectureActivity.ouvrirMarque(context, m)
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
