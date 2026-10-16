package com.myhitbodedut

import android.content.Context
import android.graphics.Typeface
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.ColorUtils

/**
 * Les trophées en ligne (15/10, maquette validée ; lot du 16/10) : en tête, le grade
 * (Trophees.grade), ce qu'il reste pour le suivant, les trois trophées les plus proches et le
 * partage du grade ; un filtre par famille ; puis une seule ligne verticale. Au-dessus, les
 * trophées gagnés, chacun du matériau du grade d'alors et daté du jour où il l'a été, une
 * bannière à chaque changement de grade ; au milieu, « Tu es ici » et le prochain objectif
 * (l'écran s'ouvre là) ; en dessous, les suivants, en gris. Chaque trophée s'ouvre en détail.
 * À l'ouverture la ligne se remplit jusqu'à « Tu es ici », les nouveaux se retournent et
 * scintillent ; un nouveau grade se fête une fois. Les records restent en bas.
 */
class PaliersActivity : AppCompatActivity() {

    /** La famille montrée seule (16/10, point 6) ; null : toutes. */
    private var filtre: Trophees.Famille? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_paliers)
        filtre = savedInstanceState?.getString(ETAT_FILTRE)?.let { c -> Trophees.Famille.entries.firstOrNull { it.cle == c } }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(ETAT_FILTRE, filtre?.cle)
    }

    override fun onResume() {
        super.onResume()
        afficher(anime = true)
    }

    private fun afficher(anime: Boolean) {
        val contenu = findViewById<LinearLayout>(R.id.paliers_contenu)
        contenu.removeAllViews()
        val bilan = Store.bilan(this)
        val pratique = Store.pratique(this)
        val dates = Store.datesTrophees(this)
        val faits = Trophees.chemin.filter { Trophees.gagne(pratique, it) }
        val rangDe = faits.withIndex().associate { (i, t) -> t.cle to i + 1 }
        val n = faits.size
        val grade = Trophees.grade(n)
        val suivant = Trophees.SEUILS_GRADES.firstOrNull { it > n }
        val vus = Store.tropheesVus(this)
        val neufs = if (vus == null || !anime) emptySet() else faits.map { it.cle }.filterNot { it in vus }.toSet()
        val f = filtre
        val dans = { t: Trophees.Niveau -> f == null || t.famille == f }
        val prochain = if (f == null) Trophees.prochain(pratique) else Trophees.chemin.firstOrNull { it.famille == f && !Trophees.gagne(pratique, it) }
        val futurs = Trophees.chemin.filter { dans(it) && !Trophees.gagne(pratique, it) && it != prochain }
        val ouvrirDetail = { t: Trophees.Niveau -> detail(t, pratique, dates, rangDe[t.cle] ?: (n + 1)) }

        // Le grade, en tête ; les trois plus proches ; le partage (16/10, points 5 et 9).
        contenu.addView(VueJour.carte(this).apply {
            addView(LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                addView(medaille(grade, "★", 48))
                addView(LinearLayout(context).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = dp(12) }
                    addView(VueJour.texte(context, getString(R.string.grade_titre, TropheesVue.metal(context, grade)), 18f, R.color.ink, gras = true))
                    addView(VueJour.texte(context, if (suivant != null) getString(R.string.grade_sous, n, suivant - n,
                        TropheesVue.metal(context, Trophees.grade(suivant))) else getString(R.string.grade_sous_max, n), 13f, R.color.muted, haut = 2))
                    if (suivant != null) {
                        val de = Trophees.SEUILS_GRADES.last { it <= n }
                        addView(barre((n - de).toFloat() / (suivant - de), dp(6), grade).apply { (layoutParams as LinearLayout.LayoutParams).topMargin = dp(6) })
                    }
                })
            })
            val proches = Trophees.plusProches(pratique, 3)
            if (proches.isNotEmpty()) {
                addView(VueJour.texte(context, getString(R.string.trophees_proches_titre), 12f, R.color.muted, gras = true, haut = 14).apply { letterSpacing = 0.04f })
                proches.forEach { t -> addView(proche(t, pratique) { ouvrirDetail(t) }) }
            }
            if (n > 0) addView(TextView(context).apply {
                text = getString(R.string.grade_partager)
                textSize = 15f
                setTypeface(typeface, Typeface.BOLD)
                setTextColor(Apparence.couleur(context, R.color.brand))
                gravity = Gravity.CENTER
                minHeight = dp(48)
                setPadding(dp(16), 0, dp(16), 0)
                foreground = ondulation()
                setOnClickListener { partager(grade, n) }
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                    gravity = Gravity.CENTER_HORIZONTAL; topMargin = dp(6)
                }
            })
        })

        // Le filtre par famille (16/10, point 6).
        contenu.addView(filtres())

        // La ligne : les gagnés, le prochain, les suivants.
        var ici: View? = null
        val pousses = mutableListOf<Pair<View, View>>()
        val aRetourner = mutableListOf<Pair<View, Trophees.Metal>>()
        var compte: Triple<TextView, Int, Trophees.Niveau>? = null
        var barreIci: View? = null
        contenu.addView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                .apply { setMargins(dp(16), dp(12), dp(16), 0) }
            // Le k-ième trophée (de 1) a le matériau du grade qu'il fait atteindre ; filtrée, la ligne n'a pas de bannières.
            val passage = { k: Int -> f == null && k > 0 && k in Trophees.SEUILS_GRADES }
            faits.filter(dans).forEach { t ->
                val k = rangDe.getValue(t.cle)
                if (passage(k)) addView(banniere(Trophees.grade(k), k, atteint = true))
                val sous = dates[t.cle]?.let { getString(R.string.trophee_gagne_le, dateLongue(it)) } ?: getString(R.string.trophee_gagne)
                val (vue, trait, objet) = etape(t, Trophees.grade(k), sous, gagne = true) { ouvrirDetail(t) }
                addView(vue)
                pousses += trait to objet
                if (t.cle in neufs) aRetourner += objet to Trophees.grade(k)
            }
            if (prochain != null) {
                if (passage(n + 1)) addView(banniere(Trophees.grade(n + 1), n + 1, atteint = false))
                val (carte, cpt, b) = carteIci(prochain, pratique) { ouvrirDetail(prochain) }
                ici = carte
                compte = Triple(cpt, Trophees.valeur(pratique, prochain.famille).coerceAtMost(prochain.seuil), prochain)
                if (Trophees.fraction(pratique, prochain) >= 0.9f) barreIci = b
                addView(carte)
            } else addView(VueJour.texte(context, getString(R.string.trophees_tous), 18f, R.color.ink, gras = true, haut = 12))
            futurs.take(MONTRES).forEachIndexed { k, t ->
                val rang = n + 2 + k
                if (passage(rang)) addView(banniere(Trophees.grade(rang), rang, atteint = false))
                val v = Trophees.valeur(pratique, t.famille)
                addView(etape(t, null, if (v > 0) getString(R.string.trophee_progres, v.coerceAtMost(t.seuil), t.seuil) else null, gagne = false) { ouvrirDetail(t) }.first)
            }
            if (futurs.size > MONTRES) addView(VueJour.texte(context, getString(R.string.trophees_autres, futurs.size - MONTRES), 13f, R.color.muted, haut = 12)
                .apply { gravity = Gravity.CENTER })
        })

        // Les records.
        val moisDe = { d: java.time.LocalDate -> CalendrierJuif.jour(d, false).let { "${it.anneeHebraique}-${it.moisHebreu}" } }
        val r = Paliers.records(bilan.minutes, moisDe)
        contenu.addView(VueJour.carte(this).apply {
            addView(VueJour.texte(context, getString(R.string.paliers_records), 17f, R.color.ink, gras = true))
            listOf(
                getString(R.string.record_serie, PaliersVue.valeur(context, Paliers.Piste.SERIE, Store.meilleureSerie(context))),
                getString(R.string.record_jour, Store.formatDuration(context, r.jour)),
                getString(R.string.record_semaine, Store.formatDuration(context, r.semaine)),
                getString(R.string.record_mois, Store.formatDuration(context, r.mois)),
            ).forEach { addView(VueJour.texte(context, it, 15f, R.color.ink, haut = 6)) }
        })

        // L'écran s'ouvre sur « Tu es ici » ; puis les animations (16/10).
        val defile = findViewById<android.widget.ScrollView>(R.id.paliers_defile)
        defile.post {
            ici?.let { v -> defile.scrollTo(0, (v.top + (v.parent as View).top - defile.height / 3).coerceAtLeast(0)) }
            if (!anime) return@post
            // B : la ligne se remplit, les six dernières étapes une à une.
            pousses.forEachIndexed { i, (trait, objet) ->
                AnimationsTrophees.pousser(trait, objet, ((i - (pousses.size - 6)).coerceAtLeast(0) * 90).toLong())
            }
            // D : le compte monte, la barre presque pleine pulse.
            compte?.let { (t, v, p) -> AnimationsTrophees.monter(t, v) { x -> getString(R.string.trophee_encore, x, p.seuil, p.seuil - x) } }
            barreIci?.let { AnimationsTrophees.pulser(it) }
            // A + E : les nouveaux se retournent et scintillent.
            aRetourner.forEachIndexed { i, (objet, m) ->
                AnimationsTrophees.retourner(objet, 400L + i * 120) { AnimationsTrophees.etincelles(this, objet, m) }
            }
        }

        // Un nouveau grade : la fête, une fois.
        val niveauGrade = if (n == 0) 0 else grade.ordinal + 1 // 0 sans trophée, 1 bronze, 2 argent…
        val vu = Store.gradeVu(this)
        if (anime && vu != null && niveauGrade > vu && grade != Trophees.Metal.BRONZE) fete(grade, n)
        Store.setGradeVu(this, niveauGrade)
        // Ce qui est vu ici est acquis : l'accueil ne le refête pas.
        Store.setPaliersVus(this, Paliers.marques(Paliers.etats(bilan), bilan))
        Store.setTropheesVus(this, Trophees.gagnes(pratique).map { it.cle }.toSet())
    }

    private fun dateLongue(d: java.time.LocalDate): String =
        d.format(java.time.format.DateTimeFormatter.ofLocalizedDate(java.time.format.FormatStyle.LONG).withLocale(resources.configuration.locales[0]))

    /** Les puces du filtre : toutes, puis chaque famille. */
    private fun filtres(): View = HorizontalScrollView(this).apply {
        isHorizontalScrollBarEnabled = false
        clipToPadding = false
        setPadding(dp(16), dp(12), dp(16), 0)
        addView(LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            (listOf<Trophees.Famille?>(null) + Trophees.Famille.entries).forEach { fam ->
                val choisi = fam == filtre
                addView(TextView(context).apply {
                    text = fam?.let { TropheesVue.nom(context, it) } ?: getString(R.string.trophees_filtre_tous)
                    textSize = 14f
                    gravity = Gravity.CENTER
                    minHeight = dp(48)
                    setPadding(dp(16), 0, dp(16), 0)
                    val brand = Apparence.couleur(context, R.color.brand)
                    setTextColor(if (choisi) Apparence.couleur(context, R.color.on_brand) else Apparence.couleur(context, R.color.ink))
                    background = GradientDrawable().apply {
                        cornerRadius = dp(999).toFloat()
                        if (choisi) setColor(brand) else setStroke(dp(1), Apparence.couleur(context, R.color.line))
                    }
                    isSelected = choisi
                    contentDescription = getString(R.string.trophees_filtre, text)
                    setOnClickListener { filtre = fam; afficher(anime = false) }
                    layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(40)).apply { marginEnd = dp(8) }
                })
            }
        })
    }

    /** Un des plus proches : l'objet, ce qu'il demande, où l'on en est. */
    private fun proche(t: Trophees.Niveau, p: Trophees.Pratique, clic: () -> Unit) = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        minimumHeight = dp(48)
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        val v = Trophees.valeur(p, t.famille).coerceAtMost(t.seuil)
        addView(View(context).apply { background = TropheeDessin(context, t.famille.forme, null, t.famille.icone); alpha = 0.6f },
            LinearLayout.LayoutParams(dp(30), dp(30)))
        addView(VueJour.texte(context, TropheesVue.description(context, t.famille, t.seuil), 14f, R.color.ink).apply {
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = dp(10) }
        })
        // VueJour.texte pose MATCH_PARENT : ici le compte doit rester à sa taille, sinon il prend
        // toute la ligne et le titre pondéré tombe à zéro (une lettre par ligne, captures du 16/10).
        addView(VueJour.texte(context, getString(R.string.trophee_progres, v, t.seuil), 13f, R.color.brand, gras = true).apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { marginStart = dp(8) }
        })
        isClickable = true
        isFocusable = true
        foreground = ondulation()
        contentDescription = TropheesVue.description(context, t.famille, t.seuil) + ", " + getString(R.string.trophee_progres, v, t.seuil)
        setOnClickListener { clic() }
    }

    private fun ondulation() = androidx.core.content.ContextCompat.getDrawable(this,
        android.util.TypedValue().also { theme.resolveAttribute(android.R.attr.selectableItemBackground, it, true) }.resourceId)

    /** Une étape de la ligne : le trait, la médaille, le texte ; touchée, le détail. Rend aussi le trait et l'objet (animations). */
    private fun etape(t: Trophees.Niveau, metal: Trophees.Metal?, sous: String?, gagne: Boolean, clic: () -> Unit): Triple<View, View, View> {
        val trait = View(this).apply { background = trait(metal) }
        val objet = View(this).apply {
            background = TropheeDessin(context, t.famille.forme, metal, t.famille.icone)
            if (metal == null) { alpha = 0.5f; tag = 0.5f }
        }
        val vue = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            addView(android.widget.FrameLayout(context).apply {
                layoutParams = LinearLayout.LayoutParams(dp(60), dp(72))
                addView(trait, android.widget.FrameLayout.LayoutParams(dp(6), ViewGroup.LayoutParams.MATCH_PARENT, Gravity.CENTER_HORIZONTAL))
                addView(objet, android.widget.FrameLayout.LayoutParams(dp(if (gagne) 52 else 44), dp(if (gagne) 52 else 44), Gravity.CENTER))
            })
            addView(LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = dp(12) }
                addView(VueJour.texte(context, TropheesVue.description(context, t.famille, t.seuil), 15f,
                    if (gagne) R.color.ink else R.color.muted, gras = gagne))
                sous?.let { addView(VueJour.texte(context, it, 12f, R.color.muted, haut = 2)) }
            })
            isClickable = true
            isFocusable = true
            foreground = ondulation()
            contentDescription = TropheesVue.description(context, t.famille, t.seuil) + (sous?.let { ", $it" } ?: "")
            setOnClickListener { clic() }
        }
        return Triple(vue, trait, objet)
    }

    /** « Tu es ici » : le prochain objectif, sa barre et ce qu'il reste. Rend aussi le compte et la barre (animations). */
    private fun carteIci(t: Trophees.Niveau, p: Trophees.Pratique, clic: () -> Unit): Triple<View, TextView, View> {
        lateinit var compte: TextView
        lateinit var b: View
        val carte = VueJour.carte(this).apply {
            val brand = Apparence.couleur(context, R.color.brand)
            (layoutParams as? ViewGroup.MarginLayoutParams)?.setMargins(0, dp(8), 0, dp(8))
            background = GradientDrawable().apply {
                cornerRadius = dp(18).toFloat()
                setColor(Apparence.couleur(context, R.color.card))
                setStroke(dp(2), brand)
            }
            addView(VueJour.texte(context, getString(R.string.trophees_ici), 11f, R.color.brand, gras = true).apply { letterSpacing = 0.06f })
            addView(LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(8) }
                addView(View(context).apply {
                    background = TropheeDessin(context, t.famille.forme, null, t.famille.icone)
                    alpha = 0.7f
                }, LinearLayout.LayoutParams(dp(64), dp(64)))
                addView(LinearLayout(context).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = dp(12) }
                    addView(VueJour.texte(context, TropheesVue.description(context, t.famille, t.seuil), 19f, R.color.ink, gras = true))
                    b = barre(Trophees.fraction(p, t), dp(10), null).apply { (layoutParams as LinearLayout.LayoutParams).topMargin = dp(10) }
                    addView(b)
                    val v = Trophees.valeur(p, t.famille).coerceAtMost(t.seuil)
                    compte = VueJour.texte(context, getString(R.string.trophee_encore, v, t.seuil, t.seuil - v), 14f, R.color.brand, gras = true, haut = 4)
                    addView(compte)
                })
            })
            isClickable = true
            isFocusable = true
            foreground = ondulation()
            setOnClickListener { clic() }
        }
        return Triple(carte, compte, b)
    }

    /** Le détail d'un trophée (16/10, point 1) : l'objet, ce qu'il demande, le jour gagné ou la progression. */
    private fun detail(t: Trophees.Niveau, p: Trophees.Pratique, dates: Map<String, java.time.LocalDate>, rangGlobal: Int) {
        val gagne = Trophees.gagne(p, t)
        val m = if (gagne) Trophees.grade(rangGlobal) else null
        val v = Trophees.valeur(p, t.famille).coerceAtMost(t.seuil)
        val vue = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(24), dp(24), dp(24), dp(4))
            val objet = View(context).apply {
                background = TropheeDessin(context, t.famille.forme, m, t.famille.icone)
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            }
            addView(objet, LinearLayout.LayoutParams(dp(120), dp(120)))
            if (gagne) AnimationsTrophees.retourner(objet, 150L)
            addView(VueJour.texte(context, TropheesVue.description(context, t.famille, t.seuil), 20f, R.color.ink, gras = true, haut = 12).apply { gravity = Gravity.CENTER })
            addView(VueJour.texte(context, TropheesVue.nom(context, t.famille) + " · " + getString(R.string.trophee_niveau_court, t.rang, t.famille.seuils.size),
                14f, R.color.muted, haut = 2).apply { gravity = Gravity.CENTER })
            if (gagne) {
                addView(VueJour.texte(context, dates[t.cle]?.let { getString(R.string.trophee_gagne_le, dateLongue(it)) } ?: getString(R.string.trophee_gagne),
                    15f, R.color.brand, gras = true, haut = 10).apply { gravity = Gravity.CENTER })
                addView(VueJour.texte(context, getString(R.string.trophee_detail_grade, TropheesVue.metal(context, m!!), rangGlobal), 13f, R.color.muted, haut = 4)
                    .apply { gravity = Gravity.CENTER })
            } else {
                addView(barre(Trophees.fraction(p, t), dp(10), null).apply { (layoutParams as LinearLayout.LayoutParams).topMargin = dp(12) })
                addView(VueJour.texte(context, getString(R.string.trophee_encore, v, t.seuil, t.seuil - v), 14f, R.color.brand, gras = true, haut = 4)
                    .apply { gravity = Gravity.CENTER })
            }
        }
        com.google.android.material.dialog.MaterialAlertDialogBuilder(this).setView(vue)
            .setPositiveButton(R.string.celebration_continue, null).show()
    }

    /** Une image du grade à partager (16/10, point 9) : dessinée ici, dans le cache ; rien d'autre ne sort. */
    private fun partager(g: Trophees.Metal, n: Int) {
        val taille = 1080
        val b = android.graphics.Bitmap.createBitmap(taille, taille, android.graphics.Bitmap.Config.ARGB_8888)
        val cv = android.graphics.Canvas(b)
        cv.drawColor(Apparence.couleur(this, R.color.brand_dark))
        TropheeDessin(this, Trophees.Forme.MEDAILLE, g, "★").apply { setBounds(280, 130, 800, 650); draw(cv) }
        val encre = Apparence.couleur(this, R.color.mint)
        val p = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply { color = encre; textAlign = android.graphics.Paint.Align.CENTER }
        p.textSize = 76f; p.typeface = Typeface.DEFAULT_BOLD
        cv.drawText(getString(R.string.grade_titre, TropheesVue.metal(this, g)), taille / 2f, 790f, p)
        p.textSize = 46f; p.typeface = Typeface.DEFAULT
        cv.drawText(getString(R.string.grade_sous_max, n), taille / 2f, 860f, p)
        p.textSize = 38f; p.alpha = 180; p.typeface = Typeface.DEFAULT_BOLD
        cv.drawText(getString(R.string.app_name), taille / 2f, 990f, p)
        val dossier = java.io.File(cacheDir, "partage").apply { mkdirs() }
        dossier.listFiles()?.forEach { it.delete() }
        val fichier = java.io.File(dossier, "grade.png")
        fichier.outputStream().use { b.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
        val uri = androidx.core.content.FileProvider.getUriForFile(this, "$packageName.partage", fichier)
        val envoi = android.content.Intent(android.content.Intent.ACTION_SEND).setType("image/png")
            .putExtra(android.content.Intent.EXTRA_STREAM, uri)
            .putExtra(android.content.Intent.EXTRA_TEXT, getString(R.string.grade_partage_texte, TropheesVue.metal(this, g), n))
            .addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        startActivity(android.content.Intent.createChooser(envoi, getString(R.string.grade_partager)))
    }

    /** La bannière d'un grade : vive quand il est atteint, grise quand il est à venir. */
    private fun banniere(g: Trophees.Metal, rang: Int, atteint: Boolean) = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(dp(12), dp(8), dp(12), dp(8))
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { setMargins(0, dp(6), 0, dp(6)) }
        val (clair, fonce, _) = TropheeDessin.teintes(g)
        background = GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
            if (atteint) intArrayOf(clair, fonce) else intArrayOf(0x1AFFFFFF, 0x1AFFFFFF)).apply { cornerRadius = dp(14).toFloat() }
        addView(medaille(if (atteint) g else null, "★", 34))
        addView(LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = dp(10) }
            val encre = if (atteint) Color.rgb(0x1A, 0x13, 0x06) else Apparence.couleur(context, R.color.muted)
            addView(TextView(context).apply {
                text = getString(if (atteint) R.string.grade_atteint else R.string.grade_titre, TropheesVue.metal(context, g))
                textSize = 15f; setTypeface(typeface, Typeface.BOLD); setTextColor(encre)
            })
            addView(TextView(context).apply { text = getString(R.string.grade_au, rang); textSize = 12f; setTextColor(encre); alpha = 0.75f })
        })
    }

    /** C : la fête d'un nouveau grade — rayons lents, la médaille passe de l'ancien matériau au nouveau. */
    private fun fete(g: Trophees.Metal, n: Int) {
        val suivant = Trophees.SEUILS_GRADES.firstOrNull { it > n }
        val ancien = Trophees.Metal.entries[(g.ordinal - 1).coerceAtLeast(0)]
        val nouveau = medaille(g, "★", 140)
        val cadre = android.widget.FrameLayout(this)
        val vue = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(24), dp(16), dp(24), dp(8))
            addView(cadre.apply {
                clipChildren = false
                addView(AnimationsTrophees.Rayons(context, TropheeDessin.teintes(g).first),
                    android.widget.FrameLayout.LayoutParams(dp(260), dp(260), Gravity.CENTER))
                val avant = medaille(ancien, "★", 140)
                addView(avant, android.widget.FrameLayout.LayoutParams(dp(140), dp(140), Gravity.CENTER))
                addView(nouveau, android.widget.FrameLayout.LayoutParams(dp(140), dp(140), Gravity.CENTER))
                if (AnimationsTrophees.actives) {
                    nouveau.alpha = 0f; nouveau.scaleX = 1.15f; nouveau.scaleY = 1.15f
                    avant.animate().alpha(0f).scaleX(0.85f).scaleY(0.85f).setStartDelay(300).setDuration(1200).start()
                    nouveau.animate().alpha(1f).scaleX(1f).scaleY(1f).setStartDelay(300).setDuration(1200).start()
                } else avant.alpha = 0f
            }, LinearLayout.LayoutParams(dp(260), dp(220)))
            addView(VueJour.texte(context, getString(R.string.grade_nouveau, TropheesVue.metal(context, g)), 24f, R.color.ink, gras = true, haut = 6)
                .apply { gravity = Gravity.CENTER })
            addView(VueJour.texte(context, getString(R.string.grade_sous_max, n), 15f, R.color.muted, haut = 4).apply { gravity = Gravity.CENTER })
            suivant?.let {
                addView(VueJour.texte(context, getString(R.string.grade_prochain, TropheesVue.metal(context, Trophees.grade(it)), it), 13f, R.color.muted, haut = 6)
                    .apply { gravity = Gravity.CENTER })
            }
        }
        com.google.android.material.dialog.MaterialAlertDialogBuilder(this).setView(vue)
            .setPositiveButton(R.string.celebration_continue, null).show()
        nouveau.postDelayed({ AnimationsTrophees.etincelles(this, nouveau, g, dans = cadre) }, 1500)
    }

    private fun medaille(m: Trophees.Metal?, icone: String, taille: Int) = View(this).apply {
        layoutParams = LinearLayout.LayoutParams(dp(taille), dp(taille))
        background = TropheeDessin(context, Trophees.Forme.MEDAILLE, m, icone)
        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
    }

    /** Le trait de la ligne : du matériau du grade, ou gris à venir. */
    private fun trait(m: Trophees.Metal?) = if (m == null) GradientDrawable().apply {
        setColor(ColorUtils.setAlphaComponent(Apparence.couleur(this@PaliersActivity, R.color.muted), 0x40)); cornerRadius = dp(3).toFloat()
    } else TropheeDessin.teintes(m).let { (clair, fonce, _) ->
        GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(clair, fonce)).apply { cornerRadius = dp(3).toFloat() }
    }

    private fun barre(fraction: Float, hauteur: Int, m: Trophees.Metal?) = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
        max = 1000
        progress = (fraction.coerceIn(0f, 1f) * 1000).toInt()
        val couleur = m?.let { TropheeDessin.teintes(it).second } ?: Apparence.couleur(context, R.color.brand)
        progressTintList = android.content.res.ColorStateList.valueOf(couleur)
        progressBackgroundTintList = android.content.res.ColorStateList.valueOf(ColorUtils.setAlphaComponent(couleur, 0x30))
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, hauteur)
        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        // D : la barre part de zéro.
        if (AnimationsTrophees.actives) {
            val cible = progress
            progress = 0
            post { android.animation.ObjectAnimator.ofInt(this, "progress", 0, cible).setDuration(700).start() }
        }
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private companion object {
        /** Les trophées à venir montrés sous « Tu es ici » ; le reste est annoncé d'une ligne. */
        const val MONTRES = 15
        const val ETAT_FILTRE = "filtre"
    }
}

/** Les libellés et les valeurs des paliers, partagés par l'écran et l'accueil. */
object PaliersVue {

    fun libelle(c: Context, piste: Paliers.Piste, bilan: Paliers.Bilan): String = when (piste) {
        Paliers.Piste.SERIE -> c.getString(R.string.palier_serie)
        Paliers.Piste.JOUR -> c.getString(R.string.palier_jour)
        Paliers.Piste.SEMAINE -> c.getString(R.string.palier_semaine)
        Paliers.Piste.MOIS -> // Isolé (FSI … PDI) : le nom hébreu ne renverse pas la phrase.
            c.getString(R.string.palier_mois, "\u2068" + CalendrierJuif.jour(bilan.aujourdhui, false).moisEtAnnee + "\u2069")
        Paliers.Piste.CUMUL -> c.getString(R.string.palier_cumul)
        Paliers.Piste.JOURS -> c.getString(R.string.palier_jours)
    }

    /** Des jours pour la série et les jours de pratique, une durée pour le reste. */
    fun valeur(c: Context, piste: Paliers.Piste, v: Int): String = when (piste) {
        Paliers.Piste.SERIE, Paliers.Piste.JOURS -> c.resources.getQuantityString(R.plurals.palier_n_jours, v, v)
        else -> Store.formatDuration(c, v)
    }

    fun court(c: Context, piste: Paliers.Piste, v: Int): String = valeur(c, piste, v)

    /** La phrase de l'accueil : ce qu'il reste pour le palier le plus proche. */
    fun aLaUne(c: Context, e: Paliers.Etat): String {
        val id = when (e.piste) {
            Paliers.Piste.SERIE -> R.string.palier_une_serie
            Paliers.Piste.JOUR -> R.string.palier_une_jour
            Paliers.Piste.SEMAINE -> R.string.palier_une_semaine
            else -> R.string.palier_une_mois
        }
        return c.getString(id, valeur(c, e.piste, e.reste), valeur(c, e.piste, e.prochain))
    }
}

/** Les noms et les conditions des trophées (écrits ici, pas par getIdentifier : la réduction des ressources les retirerait). */
object TropheesVue {
    fun nom(c: Context, f: Trophees.Famille): String = c.getString(when (f) {
        Trophees.Famille.PREMIER_PAS -> R.string.trophee_premier_pas
        Trophees.Famille.SEANCE_LONGUE -> R.string.trophee_seance_longue
        Trophees.Famille.JOURS -> R.string.trophee_jours
        Trophees.Famille.SERIE -> R.string.trophee_serie
        Trophees.Famille.HEURES -> R.string.trophee_heures
        Trophees.Famille.SEMAINES -> R.string.trophee_semaines
        Trophees.Famille.EREV_CHABBAT -> R.string.trophee_erev_chabbat
        Trophees.Famille.RETOUR -> R.string.trophee_retour
        Trophees.Famille.PLUME -> R.string.trophee_plume
    })

    /** Ce que demande un seuil de la famille (« 40 jours de pratique »). */
    fun description(c: Context, f: Trophees.Famille, seuil: Int): String = c.resources.getQuantityString(when (f) {
        Trophees.Famille.PREMIER_PAS -> R.plurals.trophee_premier_pas_desc
        Trophees.Famille.SEANCE_LONGUE -> R.plurals.trophee_seance_longue_desc
        Trophees.Famille.JOURS -> R.plurals.trophee_jours_desc
        Trophees.Famille.SERIE -> R.plurals.trophee_serie_desc
        Trophees.Famille.HEURES -> R.plurals.trophee_heures_desc
        Trophees.Famille.SEMAINES -> R.plurals.trophee_semaines_desc
        Trophees.Famille.EREV_CHABBAT -> R.plurals.trophee_erev_chabbat_desc
        Trophees.Famille.RETOUR -> R.plurals.trophee_retour_desc
        Trophees.Famille.PLUME -> R.plurals.trophee_plume_desc
    }, seuil, seuil)

    fun metal(c: Context, m: Trophees.Metal): String = c.getString(when (m) {
        Trophees.Metal.BRONZE -> R.string.metal_bronze
        Trophees.Metal.ARGENT -> R.string.metal_argent
        Trophees.Metal.OR -> R.string.metal_or
        Trophees.Metal.PLATINE -> R.string.metal_platine
    })
}

