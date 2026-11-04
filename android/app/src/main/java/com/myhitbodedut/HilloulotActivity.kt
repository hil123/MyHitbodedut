package com.myhitbodedut

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import java.time.LocalDate

/**
 * Les hilloulot (11/10), réorganisées le 04/11 (« très mal organisé ») : en haut la
 * recherche, puis le jour (flèches, retour à aujourd'hui) et ses tsadikim en liste
 * compacte, les mieux connus d'abord ; un nom touché déplie sa notice, ses biographies
 * et leurs sources. La recherche range les résultats par date, de Tichri à Eloul, et
 * chaque date mène à son jour. L'interrupteur de la carte du jour est en bas : c'est
 * un réglage, pas le contenu.
 */
class HilloulotActivity : AppCompatActivity() {

    private var jour: LocalDate = LocalDate.now()
    private var recherche = ""
    private lateinit var liste: LinearLayout
    private lateinit var champ: EditText
    /** Les noms dépliés (« jour|nom ») ; un seul tsadik ce jour-là est déplié d'office. */
    private val deplies = mutableSetOf<String>()
    private val replies = mutableSetOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_hilloulot)
        (savedInstanceState?.getString(ETAT_JOUR) ?: intent.getStringExtra(EXTRA_DATE))?.let { jour = LocalDate.parse(it) }
        val contenu = findViewById<LinearLayout>(R.id.hilloulot_contenu)

        // La recherche, en tête comme partout : un champ seul, la loupe au bout.
        champ = EditText(this).apply {
            hint = getString(R.string.hilloulot_recherche)
            setSingleLine()
            imeOptions = android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH
            background = null
            minHeight = VueJour.dp(context, 48)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) = Unit
                override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) = Unit
                override fun afterTextChanged(s: Editable?) { recherche = s?.toString().orEmpty(); afficher() }
            })
        }
        contenu.addView(VueJour.carte(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(VueJour.dp(context, 16), VueJour.dp(context, 2), VueJour.dp(context, 8), VueJour.dp(context, 2))
            addView(android.widget.ImageView(context).apply {
                setImageResource(R.drawable.ic_loupe)
                imageTintList = android.content.res.ColorStateList.valueOf(Apparence.couleur(context, R.color.muted))
                layoutParams = LinearLayout.LayoutParams(VueJour.dp(context, 20), VueJour.dp(context, 20)).apply { marginEnd = VueJour.dp(context, 10) }
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            })
            addView(champ)
        })
        liste = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        contenu.addView(liste)
        // Le réglage, en bas : montrer ou cacher la hilloula sur la carte du jour.
        contenu.addView(VueJour.carte(this).apply {
            addView(LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                addView(LinearLayout(context).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                    addView(VueJour.texte(context, getString(R.string.hilloulot_afficher), 15f, R.color.ink))
                    addView(VueJour.texte(context, getString(R.string.hilloulot_afficher_detail), 12f, R.color.muted, haut = 2))
                })
                addView(SwitchCompat(context).apply {
                    isChecked = Store.hilloulotAffichees(context)
                    contentDescription = getString(R.string.hilloulot_afficher)
                    setOnCheckedChangeListener { _, v -> Store.setHilloulotAffichees(context, v) }
                })
            })
        })
        // Taille et police des textes, comme dans les écrans de lecture (12/10).
        ReglagesLecture.installer(this, findViewById(R.id.hilloulot_defile)) { afficher() }
        afficher()
        RechercheActivity.preremplir(this, contenu)
    }

    /** Le jour où l'écran a été quitté (onPause), pour onRestart. */
    private var quitteLe: LocalDate? = null

    override fun onPause() {
        super.onPause()
        quitteLe = LocalDate.now()
    }

    /**
     * Laissé ouvert jusqu'au lendemain (bug du 02/11) : l'écran qui montrait le jour
     * d'alors passe à aujourd'hui ; un autre jour choisi exprès reste affiché.
     */
    override fun onRestart() {
        super.onRestart()
        val auj = LocalDate.now()
        val avant = quitteLe ?: return
        if (avant == auj || jour != avant) return
        jour = auj
        afficher()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(ETAT_JOUR, jour.toString())
    }

    private fun afficher() {
        remplir()
        ReglagesLecture.retailler(liste)
    }

    private fun remplir() {
        liste.removeAllViews()
        // Sans les biographies le temps qu'elles se préparent en arrière-plan : l'écran s'ouvre tout de suite.
        val table = Hilloulot.tableCompleteOuPlusTard(this) { if (!isDestroyed) afficher() } ?: Hilloulot.table(this)
        val israel = Store.inIsrael(this)
        val ashkenaz = Store.siddurNusah(this).cle == "ashkenaz"
        if (recherche.trim().length >= 2) {
            val trouves = Hilloulot.recherche(table, recherche)
            liste.addView(VueJour.texte(this, getString(R.string.hilloulot_resultats, trouves.size), 13f, R.color.muted, gras = true, haut = 16).apply {
                setPadding(VueJour.dp(context, 20), 0, VueJour.dp(context, 20), 0)
            })
            // Une seule carte, par date dans l'ordre de l'année ; chaque date mène à son jour.
            val groupes = trouves.take(60).groupBy({ it.first }, { it.second })
            if (groupes.isNotEmpty()) liste.addView(VueJour.carte(this).apply {
                (layoutParams as LinearLayout.LayoutParams).topMargin = VueJour.dp(context, 6)
                setPadding(VueJour.dp(context, 16), VueJour.dp(context, 4), VueJour.dp(context, 16), VueJour.dp(context, 4))
                groupes.entries.forEachIndexed { g, (cle, noms) ->
                    if (g > 0) addView(View(context).apply {
                        setBackgroundColor(Apparence.couleur(context, R.color.line))
                        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, VueJour.dp(context, 2))
                            .apply { topMargin = VueJour.dp(context, 6) }
                    })
                    addView(enteteDate(cle) {
                        Hilloulot.prochainJour(cle, LocalDate.now(), israel, ashkenaz)?.let { d ->
                            jour = d
                            deplies += noms.map { "$d|${it.he}" }
                            champ.setText("")
                            findViewById<View>(R.id.hilloulot_defile).scrollTo(0, 0)
                        }
                    })
                    noms.forEachIndexed { i, h -> addView(ligne(h, "r|$cle|${h.he}", ouvertParDefaut = false, premier = i == 0)) }
                }
            })
            return
        }
        val j = CalendrierJuif.jour(jour, israel)
        // Le jour : la date hébraïque, la date civile, les flèches ; « Aujourd'hui » quand on s'en est éloigné.
        liste.addView(VueJour.carte(this).apply {
            setPadding(VueJour.dp(context, 4), VueJour.dp(context, 10), VueJour.dp(context, 4), VueJour.dp(context, 10))
            addView(LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                val rtl = resources.configuration.layoutDirection == View.LAYOUT_DIRECTION_RTL
                addView(fleche(if (rtl) "›" else "‹", R.string.jour_precedent) { jour = jour.minusDays(1); afficher() })
                addView(LinearLayout(context).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                    addView(VueJour.texte(context, j.dateHebraique, 22f, R.color.brand, gras = true).apply {
                        gravity = Gravity.CENTER; PoliceHebraique.appliquer(this)
                    })
                    addView(VueJour.texte(context, VueJour.dateCivile(context, jour), 13f, R.color.muted, haut = 2).apply { gravity = Gravity.CENTER })
                })
                addView(fleche(if (rtl) "‹" else "›", R.string.jour_suivant) { jour = jour.plusDays(1); afficher() })
            })
            if (jour != LocalDate.now()) addView(VueJour.texte(context, getString(R.string.calendrier_aujourdhui), 14f, R.color.brand, gras = true, haut = 4).apply {
                gravity = Gravity.CENTER
                minHeight = VueJour.dp(context, 40)
                setOnClickListener { jour = LocalDate.now(); afficher() }
                VueJour.touchable(this)
            })
        })
        val du = Hilloulot.pour(table, jour, israel, ashkenaz)
        if (du.isEmpty()) {
            liste.addView(VueJour.texte(this, getString(R.string.hilloulot_aucune), 15f, R.color.muted, haut = 16).apply { gravity = Gravity.CENTER })
        } else {
            val detaillees = du.any(::aDesDetails)
            liste.addView(VueJour.texte(this, resources.getQuantityString(R.plurals.hilloulot_nombre, du.size, du.size) +
                if (detaillees && du.size > 1) " · " + getString(R.string.hilloulot_toucher) else "", 13f, R.color.muted, haut = 16).apply {
                setPadding(VueJour.dp(context, 20), 0, VueJour.dp(context, 20), 0)
            })
            liste.addView(VueJour.carte(this).apply {
                (layoutParams as LinearLayout.LayoutParams).topMargin = VueJour.dp(context, 6)
                setPadding(VueJour.dp(context, 16), VueJour.dp(context, 4), VueJour.dp(context, 16), VueJour.dp(context, 4))
                du.forEachIndexed { i, h -> addView(ligne(h, "$jour|${h.he}", ouvertParDefaut = du.size == 1, premier = i == 0)) }
            })
        }
        liste.addView(VueJour.texte(this, getString(R.string.hilloula_lien) + " ↗", 14f, R.color.brand, gras = true, haut = 8).apply {
            gravity = Gravity.CENTER
            minHeight = VueJour.dp(context, 48)
            setOnClickListener { ouvrir(Hilloulot.lienHyomi(jour)) }
            VueJour.touchable(this)
        })
    }

    /** La date d'un groupe de résultats, et « Voir ce jour › » au bout. */
    private fun enteteDate(cle: String, voir: () -> Unit) = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            .apply { topMargin = VueJour.dp(context, 4) }
        addView(VueJour.texte(context, Hilloulot.dateDe(cle), 16f, R.color.brand, gras = true).apply {
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            textDirection = View.TEXT_DIRECTION_RTL
            textAlignment = View.TEXT_ALIGNMENT_VIEW_START
            PoliceHebraique.appliquer(this)
        })
        addView(VueJour.texte(context, getString(R.string.hilloulot_ouvrir_jour), 13f, R.color.brand).apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            minHeight = VueJour.dp(context, 40)
            gravity = Gravity.CENTER_VERTICAL
            setPadding(VueJour.dp(context, 8), 0, VueJour.dp(context, 8), 0)
            setOnClickListener { voir() }
            VueJour.touchable(this)
        })
    }

    private fun aDesDetails(h: Hilloulot.Hilloula) =
        h.description != null || h.bios.isNotEmpty() || h.hidabroot != null || h.article != null

    /**
     * Un tsadik : son nom, la traduction et l'année dessous ; le chevron s'il y a de quoi
     * déplier. Écrit de droite à gauche dans toutes les langues : le nom hébreu à droite,
     * le chevron à gauche, sans qu'ils se gênent.
     */
    private fun ligne(h: Hilloulot.Hilloula, id: String, ouvertParDefaut: Boolean, premier: Boolean): View = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        layoutDirection = View.LAYOUT_DIRECTION_RTL
        if (!premier) addView(View(context).apply {
            setBackgroundColor(Apparence.couleur(context, R.color.line))
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, maxOf(1, VueJour.dp(context, 1) / 2))
        })
        val details = aDesDetails(h)
        val ouvert = details && (id in deplies || (ouvertParDefaut && id !in replies))
        val langue = ChizukCard.language(context)
        // Repli FR → EN (audit 1.390) : un nom traduit en anglais vaut mieux que l'hébreu seul.
        val traduit = if (langue.startsWith("en")) h.en else if (langue.startsWith("fr")) h.fr ?: h.en else null
        val tete = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            minimumHeight = VueJour.dp(context, 52)
            setPadding(0, VueJour.dp(context, 10), 0, VueJour.dp(context, 10))
            addView(LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                addView(VueJour.texte(context, h.he, 17f, R.color.ink, gras = true).apply {
                    textDirection = View.TEXT_DIRECTION_RTL
                    textAlignment = View.TEXT_ALIGNMENT_VIEW_START
                    PoliceHebraique.appliquer(this)
                })
                // Isolés (FSI … PDI) : l'année hébraïque ne renverse pas la ligne.
                listOfNotNull(traduit?.takeIf { it.isNotBlank() }, h.annee.takeIf { it.isNotBlank() }?.let { getString(R.string.hilloula_annee, "\u2068$it\u2069") })
                    .joinToString(" · ").takeIf { it.isNotBlank() }?.let { sous ->
                        addView(VueJour.texte(context, sous, 13f, R.color.muted, haut = 2).apply {
                            textDirection = View.TEXT_DIRECTION_FIRST_STRONG_RTL
                            textAlignment = View.TEXT_ALIGNMENT_VIEW_START
                        })
                    }
            })
            if (details) addView(android.widget.ImageView(context).apply {
                setImageResource(R.drawable.ic_chevron)
                // Pas de miroir (le chevron est autoMirrored) : vers le bas replié, vers le haut déplié.
                layoutDirection = View.LAYOUT_DIRECTION_LTR
                imageTintList = android.content.res.ColorStateList.valueOf(Apparence.couleur(context, R.color.muted))
                rotation = if (ouvert) -90f else 90f
                layoutParams = LinearLayout.LayoutParams(VueJour.dp(context, 20), VueJour.dp(context, 20)).apply { marginStart = VueJour.dp(context, 8) }
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            })
        }
        addView(tete)
        if (details) {
            tete.foreground = Apparence.effetToucher(context)
            tete.isClickable = true
            tete.contentDescription = listOfNotNull(h.he, traduit).joinToString(", ") + ", " +
                getString(if (ouvert) R.string.hilloula_reduire else R.string.hilloula_lire_plus)
            tete.setOnClickListener {
                if (ouvert) { deplies -= id; replies += id } else { deplies += id; replies -= id }
                afficher()
            }
        }
        if (ouvert) addView(fiche(h))
    }

    // Police plus petite que le reste de l'application (14/10) : les biographies sont longues.
    /** Ce qui se déplie : la notice de Wikipédia, les biographies, chacune suivie de sa source. */
    private fun fiche(h: Hilloulot.Hilloula) = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(0, 0, 0, VueJour.dp(context, 14))
        h.description?.let { d ->
            addView(VueJour.texte(context, VueJour.paragraphes(d), 14f, R.color.ink).apply {
                textDirection = View.TEXT_DIRECTION_RTL; PoliceHebraique.appliquer(this)
            })
        }
        // La source : Wikipédia (l'article s'il existe), החכם היומי, תורת אמת.
        if ('w' in h.sources) addView(source(getString(R.string.hilloula_source_wiki), h.article?.let(Hilloulot::lienArticle), haut = 4))
        if ('h' in h.sources) addView(source(getString(R.string.hilloula_source_hyomi), null, haut = 2))
        if ('e' in h.sources) addView(source(getString(R.string.hilloula_source_toratemet), null, haut = 2))
        // Les autres biographies (MyTzadik, המאיר לארץ), chacune suivie de sa source, comme le demande l'accord.
        h.bios.forEach { b ->
            val texte = VueJour.texte(context, VueJour.paragraphes(b.texte), 14f, R.color.ink, haut = 12).apply {
                textDirection = View.TEXT_DIRECTION_RTL; PoliceHebraique.appliquer(this)
            }
            addView(texte)
            // Une longue biographie se replie à huit lignes ; « Lire la suite » la déplie.
            if (b.texte.length > 500) {
                texte.maxLines = 8
                texte.ellipsize = android.text.TextUtils.TruncateAt.END
                addView(VueJour.texte(context, getString(R.string.hilloula_lire_plus), 13f, R.color.brand, gras = true, haut = 4).apply {
                    setOnClickListener {
                        val replie = texte.maxLines != Int.MAX_VALUE
                        texte.maxLines = if (replie) Int.MAX_VALUE else 8
                        text = getString(if (replie) R.string.hilloula_reduire else R.string.hilloula_lire_plus)
                    }
                    VueJour.touchable(this)
                })
            }
            val nom = getString(if (b.source == "m") R.string.hilloula_source_mytzadik else R.string.hilloula_source_hameir)
            addView(source(nom, b.lien, haut = 4))
        }
        h.hidabroot?.let { addView(source(getString(R.string.hilloula_source_hidabroot), it, haut = 10)) }
    }

    /** Une source, discrète ; avec ↗ quand elle mène à la page. */
    private fun source(nom: String, lien: String?, haut: Int) =
        VueJour.texte(this, if (lien != null) "$nom ↗" else nom, 12f, if (lien != null) R.color.brand else R.color.muted, haut = haut).apply {
            textAlignment = View.TEXT_ALIGNMENT_VIEW_START
            lien?.let { l -> setOnClickListener { ouvrir(l) }; VueJour.touchable(this) }
        }

    private fun fleche(signe: String, description: Int, action: () -> Unit) = VueJour.texte(this, signe, 28f, R.color.brand).apply {
        contentDescription = getString(description)
        textDirection = View.TEXT_DIRECTION_LTR
        gravity = Gravity.CENTER
        val d = (52 * resources.displayMetrics.density).toInt()
        layoutParams = LinearLayout.LayoutParams(d, d)
        setOnClickListener { action() }
        VueJour.touchable(this)
    }

    private fun ouvrir(url: String) {
        runCatching { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
    }

    companion object {
        const val EXTRA_DATE = "date"
        private const val ETAT_JOUR = "jour"
    }
}
