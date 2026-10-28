package com.myhitbodedut

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.GestureDetector
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import kotlin.math.roundToInt

/**
 * Le bouton de lecture des textes saints (demandes du 04/10) : la taille
 * des lettres, la lecture de nuit, le nusah au siddour, et un bouton qui
 * disparaît tout à fait de l'écran quand on le veut.
 *
 * Replié, c'est une petite pastille « Aa » au milieu du bas de l'écran,
 * aux couleurs choisies dans les réglages (accent et encre sur l'accent).
 * Elle s'efface quand on descend dans le texte et revient quand on
 * remonte. Déplié, une barre : A−, la taille en pour cent (un toucher la
 * remet à 100 %), A+, la lune, le nusah, masquer, replier. Masqué, plus
 * rien : un appui long sur le texte le fait revenir, ce que dit une barre
 * en bas de l'écran au moment où on le masque.
 *
 * Les réglages valent pour tous les écrans de lecture (Store) ; chaque
 * écran redessine son texte dans surChangement, ou laisse [appliquer]
 * retailler et recolorer les vues qu'il a marquées.
 */
class ReglagesLecture private constructor(
    activite: Activity,
    private val avecNusah: Boolean,
    private val auDessusDe: View?,
    private val surChangement: () -> Unit,
) {
    private val contexte: Context = activite
    private val racine: FrameLayout = activite.findViewById(android.R.id.content)
    private val densite = contexte.resources.displayMetrics.density
    private fun dp(v: Int) = (v * densite).roundToInt()

    /**
     * La taille d'un bouton de la barre : 44 dp, moins sur un écran étroit
     * (320 dp, ou un affichage agrandi) pour que la barre tienne toujours
     * entière, « Replier » compris (audit du 04/10).
     */
    private val bouton: Int = run {
        val largeur = contexte.resources.displayMetrics.widthPixels / densite
        val nombre = if (avecNusah) 8 else 7
        (((largeur - 24 - 48 - 18 - 12) / nombre).toInt()).coerceIn(30, 44)
    }

    private val accent = Apparence.couleur(contexte, R.color.brand)
    private val surAccent = Apparence.couleur(contexte, R.color.on_brand)

    private fun fondArrondi(rayon: Int) = GradientDrawable().apply {
        cornerRadius = dp(rayon).toFloat()
        setColor(ColorUtils.setAlphaComponent(accent, 0xF2))
    }

    private val pastille = TextView(contexte).apply {
        text = "Aa"
        textSize = 15f
        typeface = Typeface.DEFAULT_BOLD
        gravity = Gravity.CENTER
        setTextColor(surAccent)
        background = fondArrondi(18)
        minWidth = dp(56)
        setPadding(dp(12), 0, dp(12), 0)
        elevation = dp(6).toFloat()
        contentDescription = contexte.getString(R.string.lecture_reglages)
        setOnClickListener { deplier(true) }
    }

    private val pourcent = TextView(contexte)
    private val lune = ImageView(contexte)

    private val barre = LinearLayout(contexte).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        background = fondArrondi(24)
        setPadding(dp(4), 0, dp(4), 0)
        elevation = dp(6).toFloat()
        // A− à gauche et A+ à droite, quelle que soit la langue.
        layoutDirection = View.LAYOUT_DIRECTION_LTR
        visibility = View.GONE
    }

    init {
        barre.addView(lettre(plus = false))
        barre.addView(pourcent.apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(44))
            gravity = Gravity.CENTER
            minWidth = dp(48)
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(surAccent)
            touchable(this)
            setOnClickListener { changerTaille(1f) }
        })
        barre.addView(lettre(plus = true))
        barre.addView(filet())
        barre.addView(lune.apply {
            layoutParams = LinearLayout.LayoutParams(dp(bouton), dp(44))
            setImageResource(R.drawable.ic_lecture_lune)
            imageTintList = ColorStateList.valueOf(surAccent)
            scaleType = ImageView.ScaleType.CENTER
            contentDescription = contexte.getString(R.string.lecture_nuit)
            setOnClickListener {
                Store.setLectureSombre(contexte, !Store.lectureSombre(contexte))
                montrerEtat()
                surChangement()
            }
        })
        barre.addView(TextView(contexte).apply {
            // « אב » : le choix de la police (12/10), en lettres hébraïques puisque c'est elles qu'il change.
            layoutParams = LinearLayout.LayoutParams(dp(bouton), dp(44))
            gravity = Gravity.CENTER
            text = "אב"
            textSize = 17f
            setTextColor(surAccent)
            contentDescription = contexte.getString(R.string.lecture_police)
            touchable(this)
            setOnClickListener { choisirPolice(it) }
        })
        if (avecNusah) barre.addView(icone(R.drawable.ic_lecture_nusah, R.string.siddur_choose) { choisirNusah(it) })
        barre.addView(filet())
        barre.addView(icone(R.drawable.ic_lecture_masquer, R.string.lecture_masquer) { masquer() })
        barre.addView(icone(R.drawable.ic_lecture_replier, R.string.lecture_replier) { deplier(false) })

        fun bas(hauteur: Int) = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, dp(hauteur), Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL,
        )
        pastille.layoutParams = bas(36)
        barre.layoutParams = bas(52)
        racine.addView(barre)
        racine.addView(pastille)
        placer()
        // Au-dessus du bouton du bas de l'écran (« Terminé », précédent / suivant).
        // Suivi à chaque mise en page : ce bouton peut aussi disparaître (GONE).
        if (auDessusDe != null) racine.viewTreeObserver.addOnGlobalLayoutListener { placer() }
        montrerEtat()
        if (Store.lectureMasquee(contexte)) {
            pastille.visibility = View.GONE
            barre.visibility = View.GONE
        }
    }

    private fun placer() {
        val pied = auDessusDe?.takeIf { it.visibility == View.VISIBLE }
            ?.let { it.height + ((it.layoutParams as? ViewGroup.MarginLayoutParams)?.bottomMargin ?: 0) } ?: 0
        val marge = pied + dp(16)
        listOf(pastille, barre).forEach { v ->
            val lp = v.layoutParams as FrameLayout.LayoutParams
            if (lp.bottomMargin != marge) {
                lp.bottomMargin = marge
                v.layoutParams = lp
            }
        }
    }

    private fun filet(): View = View(contexte).apply {
        layoutParams = LinearLayout.LayoutParams(dp(1), dp(22)).apply { setMargins(dp(4), 0, dp(4), 0) }
        setBackgroundColor(ColorUtils.setAlphaComponent(surAccent, 0x55))
    }

    /** Le halo au toucher, sans fond : la barre garde sa couleur. */
    private fun touchable(v: View) {
        val tv = TypedValue()
        contexte.theme.resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, tv, true)
        v.background = ContextCompat.getDrawable(contexte, tv.resourceId)
    }

    private fun icone(res: Int, description: Int, action: (View) -> Unit): View = ImageView(contexte).apply {
        layoutParams = LinearLayout.LayoutParams(dp(bouton), dp(44))
        setImageResource(res)
        imageTintList = ColorStateList.valueOf(surAccent)
        scaleType = ImageView.ScaleType.CENTER
        contentDescription = contexte.getString(description)
        touchable(this)
        setOnClickListener { action(it) }
    }

    /** « A− » et « A+ » : la lettre latine, comme partout pour la taille du texte. */
    private fun lettre(plus: Boolean): View = TextView(contexte).apply {
        layoutParams = LinearLayout.LayoutParams(dp(bouton), dp(44))
        gravity = Gravity.CENTER
        text = if (plus) "A+" else "A−"
        textSize = if (plus) 18f else 14f
        typeface = Typeface.DEFAULT_BOLD
        setTextColor(surAccent)
        contentDescription = contexte.getString(if (plus) R.string.lecture_plus else R.string.lecture_moins)
        touchable(this)
        setOnClickListener { changerTaille(Store.lectureEchelle(contexte) + if (plus) PAS else -PAS) }
    }

    private fun changerTaille(voulue: Float) {
        // Au dixième près : dix touchers de +10 % ne doivent pas donner 199,99 %.
        val nouvelle = ((voulue * 10).roundToInt() / 10f).coerceIn(MIN, MAX)
        if (nouvelle == Store.lectureEchelle(contexte)) return
        Store.setLectureEchelle(contexte, nouvelle)
        montrerEtat()
        surChangement()
    }

    private fun montrerEtat() {
        val taille = (Store.lectureEchelle(contexte) * 100).roundToInt()
        pourcent.text = contexte.getString(R.string.lecture_pourcent, taille)
        pourcent.contentDescription = contexte.getString(R.string.lecture_taille, taille)
        val nuit = Store.lectureSombre(contexte)
        lune.background = if (nuit) GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(ColorUtils.setAlphaComponent(surAccent, 0x40))
        } else null
        if (android.os.Build.VERSION.SDK_INT >= 30) {
            lune.stateDescription = contexte.getString(if (nuit) R.string.lecture_nuit_oui else R.string.lecture_nuit_non)
        }
    }

    private fun choisirPolice(ancre: View) {
        PopupMenu(contexte, ancre).apply {
            val actuelle = Store.lecturePolice(contexte)
            PoliceHebraique.Police.entries.forEachIndexed { i, p ->
                menu.add(0, i, i, contexte.getString(p.libelle)).isChecked = p == actuelle
            }
            menu.setGroupCheckable(0, true, true)
            setOnMenuItemClickListener { item ->
                Store.setLecturePolice(contexte, PoliceHebraique.Police.entries[item.itemId])
                surChangement()
                true
            }
            show()
        }
    }

    private fun choisirNusah(ancre: View) {
        PopupMenu(contexte, ancre).apply {
            val actuel = Store.siddurNusah(contexte)
            Nusah.entries.forEachIndexed { i, n ->
                menu.add(0, i, i, contexte.getString(n.libelle)).isChecked = n == actuel
            }
            menu.setGroupCheckable(0, true, true)
            setOnMenuItemClickListener { item ->
                Store.setSiddurNusah(contexte, Nusah.entries[item.itemId])
                surChangement()
                true
            }
            show()
        }
    }

    private fun deplier(ouvert: Boolean) {
        barre.visibility = if (ouvert) View.VISIBLE else View.GONE
        pastille.visibility = if (ouvert) View.GONE else View.VISIBLE
        pastille.animate().cancel()
        pastille.alpha = 1f
        pastille.translationY = 0f
        pastille.isClickable = true
    }

    /** Plus rien à l'écran ; la barre du bas dit comment le retrouver. */
    private fun masquer() {
        Store.setLectureMasquee(contexte, true)
        barre.visibility = View.GONE
        pastille.visibility = View.GONE
        Snackbar.make(racine, R.string.lecture_masque_info, Snackbar.LENGTH_LONG)
            .setAction(R.string.lecture_annuler_masque) { retrouver() }
            .show()
    }

    /** Le double toucher sur le texte : le bouton revient, déplié. */
    fun retrouver() {
        if (!Store.lectureMasquee(contexte)) return
        Store.setLectureMasquee(contexte, false)
        deplier(true)
    }

    /** La pastille s'efface en descendant dans le texte, revient en remontant. */
    private fun montrer(visible: Boolean) {
        if (barre.visibility == View.VISIBLE || pastille.visibility != View.VISIBLE) return
        pastille.animate().cancel()
        pastille.animate().alpha(if (visible) 1f else 0f).translationY(if (visible) 0f else dp(24).toFloat())
            .setDuration(180L).withEndAction { pastille.isClickable = visible }.start()
    }

    /** Le niveau de lecture sur le bord du texte (IndicateurDefilement, 28/10). */
    private var indicateur: IndicateurDefilement? = null

    /** Le chemin parcouru dans un sens : un défilement lent cache aussi la pastille. */
    private var parcouru = 0

    private fun defile(dy: Int) {
        parcouru = if ((parcouru > 0) == (dy > 0)) parcouru + dy else dy
        if (parcouru > dp(24)) { montrer(false); parcouru = 0 } else if (parcouru < -dp(24)) { montrer(true); parcouru = 0 }
    }

    /**
     * Deux touchers sur le texte (28/10, « faire réapparaître le bandeau en appuyant 2 fois ») :
     * masqué, le bouton revient déplié. Un seul toucher fait revenir la pastille effacée par le
     * défilement. L'appui long est laissé aux marque-pages.
     */
    private val toucher = GestureDetector(contexte, object : GestureDetector.SimpleOnGestureListener() {
        override fun onDoubleTap(e: MotionEvent): Boolean {
            retrouver()
            return false
        }
        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
            if (!Store.lectureMasquee(contexte)) montrer(true)
            return false
        }
    })

    /** Pour un texte qui prend lui-même les touchers (les versets des Tehilim) : il les montre ici. */
    fun toucher(e: MotionEvent) {
        toucher.onTouchEvent(e)
    }

    private fun suivreListe(liste: RecyclerView) {
        liste.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                defile(dy)
                if (dy != 0) indicateur?.reveiller()
            }
        })
        liste.addOnItemTouchListener(object : RecyclerView.SimpleOnItemTouchListener() {
            override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
                toucher.onTouchEvent(e)
                return false
            }
        })
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun suivreDefilement(defilement: View) {
        defilement.setOnScrollChangeListener { _, _, y, _, ancien ->
            defile(y - ancien)
            if (y != ancien) indicateur?.reveiller()
        }
        // Le texte ne prend pas les touchers : ils arrivent à la vue qui défile,
        // qui les laisse passer (false) après les avoir montrés au détecteur.
        defilement.setOnTouchListener { _, e ->
            toucher.onTouchEvent(e)
            false
        }
    }

    /** Les couleurs et la taille du moment, pour l'écran qui dessine le texte. */
    data class Rendu(val echelle: Float, val sombre: Boolean, val fond: Int, val texte: Int, val discret: Int, val titre: Int)

    /** Ce qu'une vue marquée est dans la page, pour [appliquer]. */
    enum class Role { TEXTE, TITRE, DISCRET, FOND }

    companion object {
        const val PAS = 0.1f
        const val MIN = 0.8f
        const val MAX = 2.0f

        /**
         * Pose le bouton sur l'écran. [defilement] est la vue qui défile
         * (ScrollView ou RecyclerView) ; [auDessusDe], le bouton du bas que
         * la pastille ne doit pas couvrir.
         */
        fun installer(
            activite: Activity,
            defilement: View,
            avecNusah: Boolean = false,
            auDessusDe: View? = null,
            surChangement: () -> Unit,
        ): ReglagesLecture {
            // Une vue qui défile garde sa place quand les lettres grandissent :
            // la même proportion du texte reste en haut de l'écran.
            val garderPlace: () -> Unit = if (defilement is RecyclerView) surChangement else {
                {
                    val portee = ((defilement as? ViewGroup)?.getChildAt(0)?.height ?: 0) - defilement.height
                    val part = if (portee > 0) defilement.scrollY.toFloat() / portee else 0f
                    surChangement()
                    defilement.post {
                        val nouvelle = ((defilement as? ViewGroup)?.getChildAt(0)?.height ?: 0) - defilement.height
                        if (nouvelle > 0) defilement.scrollTo(0, (part * nouvelle).toInt())
                    }
                }
            }
            val reglages = ReglagesLecture(activite, avecNusah, auDessusDe, garderPlace)
            reglages.indicateur = IndicateurDefilement.poser(activite.findViewById(android.R.id.content), defilement)
            if (defilement is RecyclerView) reglages.suivreListe(defilement) else reglages.suivreDefilement(defilement)
            // De quoi lire la fin du texte au-dessus de la barre ouverte.
            val d = activite.resources.displayMetrics.density
            defilement.setPadding(defilement.paddingLeft, defilement.paddingTop, defilement.paddingRight,
                defilement.paddingBottom + (72 * d).toInt())
            (defilement as? ViewGroup)?.clipToPadding = false
            // Sans double toucher (TalkBack, contacteur) : une action d'accessibilité fait revenir le bouton.
            ViewCompat.addAccessibilityAction(defilement, activite.getString(R.string.lecture_reglages)) { _, _ ->
                reglages.retrouver()
                true
            }
            return reglages
        }

        fun rendu(contexte: Context): Rendu {
            val sombre = Store.lectureSombre(contexte)
            fun c(id: Int) = Apparence.couleur(contexte, id)
            return if (sombre) {
                Rendu(Store.lectureEchelle(contexte), true, c(R.color.lecture_nuit_fond), c(R.color.lecture_nuit_texte),
                    c(R.color.lecture_nuit_discret), c(R.color.lecture_nuit_titre))
            } else {
                Rendu(Store.lectureEchelle(contexte), false, c(R.color.paper), c(R.color.ink), c(R.color.muted), c(R.color.brand))
            }
        }

        /**
         * Les écrans faits de cartes reconstruites (halakha, hilloulot, 12/10) :
         * chaque texte sous [racine] à sa taille de base fois l'échelle, la
         * police choisie déjà posée par PoliceHebraique à la construction.
         */
        fun retailler(racine: View) {
            val echelle = Store.lectureEchelle(racine.context)
            fun parcourir(v: View) {
                if (v is TextView && v !is android.widget.EditText) {
                    val base = (v.getTag(R.id.lecture_taille) as? Float) ?: v.textSize.also { v.setTag(R.id.lecture_taille, it) }
                    v.setTextSize(TypedValue.COMPLEX_UNIT_PX, base * echelle)
                }
                if (v is ViewGroup) for (i in 0 until v.childCount) parcourir(v.getChildAt(i))
            }
            parcourir(racine)
        }

        /** Marque une vue du texte : son rôle pour les couleurs, et sa taille de base en pixels. */
        fun <V : View> marquer(vue: V, role: Role): V {
            vue.setTag(R.id.lecture_role, role)
            if (vue is TextView) {
                vue.setTag(R.id.lecture_taille, vue.textSize)
                vue.setTag(R.id.lecture_interligne, vue.lineSpacingExtra)
            }
            return vue
        }

        /**
         * Retaille et recolore les vues marquées sous [racine], pour les
         * écrans faits de quelques vues plutôt que d'une liste : la taille
         * de base gardée par [marquer], fois l'échelle ; la nuit, le fond
         * noir sur les vues FOND (la vue qui défile, la carte du texte).
         */
        fun appliquer(racine: View) {
            val r = rendu(racine.context)
            fun parcourir(v: View) {
                val role = v.getTag(R.id.lecture_role) as? Role
                if (role == Role.FOND) {
                    // Une vue sans fond (la ScrollView) en reçoit un, la couleur du papier.
                    if (v.background == null) v.setBackgroundColor(Apparence.couleur(v.context, R.color.paper))
                    v.backgroundTintList = if (r.sombre) ColorStateList.valueOf(r.fond) else null
                } else if (v is TextView && v.getTag(R.id.lecture_police) == true) {
                    PoliceHebraique.appliquer(v)
                }
                if (role != null && role != Role.FOND && v is TextView) {
                    (v.getTag(R.id.lecture_taille) as? Float)?.let { v.setTextSize(TypedValue.COMPLEX_UNIT_PX, it * r.echelle) }
                    (v.getTag(R.id.lecture_interligne) as? Float)?.let { v.setLineSpacing(it * r.echelle, 1f) }
                    v.setTextColor(
                        when (role) {
                            Role.TITRE -> r.titre
                            Role.DISCRET -> r.discret
                            else -> r.texte
                        }
                    )
                }
                if (v is ViewGroup) for (i in 0 until v.childCount) parcourir(v.getChildAt(i))
            }
            parcourir(racine)
        }
    }
}
