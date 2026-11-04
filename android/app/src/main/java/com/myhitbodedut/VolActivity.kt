package com.myhitbodedut

import android.Manifest
import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.CancellationSignal
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import android.app.TimePickerDialog
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

/**
 * Les zmanim en vol (28/10), phase 1 : on saisit le vol (aéroports, heures
 * du billet, à l'heure de chaque aéroport) ; l'écran montre la prière du
 * moment avec son compte à rebours, la fiabilité de l'estimation, puis les
 * zmanim de la route. Le calcul est Vol.kt, hors ligne ; rien ne sort du
 * téléphone. La seule permission, la localisation précise, n'est demandée
 * qu'au toucher de « position réelle » : le GPS marche en mode avion.
 */
class VolActivity : AppCompatActivity() {

    private var aeroports: List<Vol.Aeroport> = emptyList()
    private var numero = ""
    private var depart: Vol.Aeroport? = null
    private var arrivee: Vol.Aeroport? = null
    private var decollage: LocalDateTime? = null
    private var atterrissage: LocalDateTime? = null
    private var calcul: Vol.Calcul? = null
    private var gpsEnCours = false
    private val autorisationGps = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { r ->
        if (r[Manifest.permission.ACCESS_FINE_LOCATION] == true) releverGps() else message(getString(R.string.vol_gps_refuse))
    }
    private var enCours = false
    private val choixBillet = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> if (uri != null) lireBillet(uri) }
    private val autorisationNotifications = registerForActivityResult(ActivityResultContracts.RequestPermission()) { afficherResultats() }
    private val minuterie = Handler(Looper.getMainLooper())
    private val rafraichir = object : Runnable {
        override fun run() {
            battre()
            minuterie.postDelayed(this, RAFRAICHISSEMENT_MS)
        }
    }

    /**
     * Toutes les 30 s (04/11) : seul le compte à rebours change de texte. L'écran entier n'est
     * redessiné que si la prière du moment change ou si « maintenant » passe un zman de la
     * frise — avant, il l'était à chaque fois, et la page sautait sous le doigt.
     */
    private fun battre() {
        val c = calcul ?: return
        if (enCours) return
        val maintenant = Instant.now()
        if (signature(c, maintenant) != signatureAffichee) return afficherResultats()
        val p = VolPriere.priere(c, maintenant) ?: return
        val limite = p.second ?: return
        compteur?.text = getString(R.string.vol_dans, dureeEnMots(Duration.between(maintenant, limite).toMinutes().toInt().coerceAtLeast(0)))
    }

    /** Ce qui, en changeant, demande de redessiner : la prière, sa limite, la place de « maintenant ». */
    private fun signature(c: Vol.Calcul, maintenant: Instant): String {
        val p = VolPriere.priere(c, maintenant)
        val place = c.enVol.indexOfFirst { maintenant.isBefore(it.instant) }
        val rappels = VolRappels.rappels(c, Store.inIsrael(this)).count { it.instant.isAfter(maintenant) }
        return "${p?.first}|${p?.second}|$place|$rappels|${maintenant.isAfter(c.plan.atterrissage)}"
    }

    private var signatureAffichee: String? = null
    private var compteur: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_vol)
        // Les 3 226 aéroports (550 Ko de JSON) se lisent hors du fil de l'interface (04/11) :
        // l'écran s'ouvrait avec un temps mort, puis la première recherche ramait.
        contenu.addView(VueJour.carte(this).apply { addView(VueJour.texte(this@VolActivity, getString(R.string.vol_calcul_en_cours), 15f, R.color.muted)) })
        Thread {
            val liste = Data.aeroports(this)
            Aeroports.preparer(liste)
            runOnUiThread { if (!isFinishing && !isDestroyed) demarrer(liste, savedInstanceState) }
        }.start()
    }

    private fun demarrer(liste: List<Vol.Aeroport>, savedInstanceState: Bundle?) {
        aeroports = liste
        pret = true
        val enregistre = Store.vol(this, aeroports)
        if (savedInstanceState != null) {
            numero = savedInstanceState.getString(ETAT_NUMERO).orEmpty()
            depart = savedInstanceState.getString(ETAT_DEPART)?.let { Aeroports.parIata(aeroports, it) }
            arrivee = savedInstanceState.getString(ETAT_ARRIVEE)?.let { Aeroports.parIata(aeroports, it) }
            decollage = savedInstanceState.getString(ETAT_DECOLLAGE)?.let(LocalDateTime::parse)
            atterrissage = savedInstanceState.getString(ETAT_ATTERRISSAGE)?.let(LocalDateTime::parse)
        } else if (enregistre != null) {
            numero = enregistre.numero
            depart = enregistre.depart
            arrivee = enregistre.arrivee
            decollage = Vol.zone(enregistre.depart.fuseau)?.let { LocalDateTime.ofInstant(enregistre.decollage, it) }
            atterrissage = Vol.zone(enregistre.arrivee.fuseau)?.let { LocalDateTime.ofInstant(enregistre.atterrissage, it) }
        }
        afficherFormulaire()
        // Après une rotation aussi : le calcul n'est pas gardé, il se refait en un instant.
        if (enregistre != null) lancer(enregistre)
    }

    /** Les aéroports sont lus : le formulaire peut s'afficher. */
    private var pret = false

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (!pret) return
        outState.putString(ETAT_NUMERO, numero)
        outState.putString(ETAT_DEPART, depart?.iata)
        outState.putString(ETAT_ARRIVEE, arrivee?.iata)
        outState.putString(ETAT_DECOLLAGE, decollage?.toString())
        outState.putString(ETAT_ATTERRISSAGE, atterrissage?.toString())
    }

    override fun onResume() {
        super.onResume()
        minuterie.postDelayed(rafraichir, RAFRAICHISSEMENT_MS)
        battre()
    }

    override fun onPause() {
        super.onPause()
        minuterie.removeCallbacks(rafraichir)
    }

    private val contenu get() = findViewById<LinearLayout>(R.id.vol_contenu)
    private val locale get() = Locale.forLanguageTag(ChizukCard.language(this))

    /** Le formulaire rouvert pour modifier le vol calculé ; replié en résumé sinon (29/10). */
    private var modifier = false

    /**
     * Le formulaire, ou son résumé une fois le vol calculé, puis une zone de résultats que la
     * minuterie récrit seule (le champ garde le clavier).
     */
    private fun afficherFormulaire() {
        if (!pret) return
        val y = defile.scrollY
        contenu.removeAllViews()
        val garde = Store.vol(this, aeroports)
        formulaireOuvert = garde == null || modifier
        if (formulaireOuvert) contenu.addView(carteRapide())
        contenu.addView(if (!formulaireOuvert) carteResume(garde!!) else carteFormulaire())
        contenu.addView(LinearLayout(this).apply {
            id = R.id.vol_resultats
            orientation = LinearLayout.VERTICAL
        })
        contenu.addView(sources())
        afficherResultats()
        defile.post { defile.scrollTo(0, y) }
    }

    private val defile get() = findViewById<android.widget.ScrollView>(R.id.vol_defile)

    /** « LY81 · TLV → BKK », « Modifier », et les heures du billet. */
    private fun carteResume(plan: Vol.Plan) = VueJour.carte(this).apply {
        addView(LinearLayout(this@VolActivity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            addView(VueJour.texte(this@VolActivity, getString(R.string.vol_carte_vol, plan.numero.ifEmpty { "✈" }, plan.depart.iata, plan.arrivee.iata),
                17f, R.color.ink, gras = true).apply { layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f) })
            addView(bouton(getString(R.string.vol_modifier), Genre.LIEN) { modifier = true; afficherFormulaire() }.apply {
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            })
        })
        val fleche = if (resources.configuration.layoutDirection == View.LAYOUT_DIRECTION_RTL) "←" else "→"
        addView(VueJour.texte(this@VolActivity, "\u2068${heureAeroport(plan.depart, plan.decollage)}\u2069 $fleche " +
            "\u2068${heureAeroport(plan.arrivee, plan.atterrissage)}\u2069", 14f, R.color.muted, haut = 2).apply { fontFeatureSettings = "tnum" })
    }

    /** « mer. 28 19:20 TLV » : l'heure locale d'un aéroport (celle du billet). */
    private fun heureAeroport(a: Vol.Aeroport, t: Instant): String {
        val z = Vol.zone(a.fuseau) ?: return a.iata
        return "${DateTimeFormatter.ofPattern("EEE d HH:mm", locale).format(t.atZone(z))} \u2066${a.iata}\u2069"
    }

    private fun carteFormulaire() = VueJour.carte(this).apply {
        val champNumero = EditText(this@VolActivity).apply {
            hint = getString(R.string.vol_numero)
            setText(numero)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
            isSingleLine = true
            textSize = 17f
            background = null
            setPaddingRelative(dp(16), dp(14), dp(16), dp(14))
            filters = arrayOf(android.text.InputFilter.LengthFilter(12))
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) = Unit
                override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) = Unit
                override fun afterTextChanged(s: Editable?) { numero = s?.toString()?.trim().orEmpty() }
            })
        }
        if (BuildConfig.SUIVI_VOL) {
            // La loupe est toujours là (04/11) ; services en ligne éteints, elle demande d'abord
            // l'accord (chercherRoute ne part jamais sans l'interrupteur). « Entrée » cherche aussi.
            champNumero.imeOptions = android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH
            champNumero.setOnEditorActionListener { _, action, _ ->
                if (action == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) { loupe(); true } else false
            }
            champNumero.setPaddingRelative(dp(16), dp(14), dp(56), dp(14))
            addView(android.widget.FrameLayout(this@VolActivity).apply {
                background = cadreSaisie()
                addView(champNumero, android.widget.FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
                addView(android.widget.ImageButton(this@VolActivity).apply {
                    setImageResource(R.drawable.ic_loupe)
                    imageTintList = android.content.res.ColorStateList.valueOf(Apparence.couleur(this@VolActivity, R.color.brand))
                    background = Apparence.effetToucher(this@VolActivity)
                    contentDescription = getString(R.string.vol_route_bouton)
                    isEnabled = !routeEnCours
                    alpha = if (routeEnCours) 0.4f else 1f
                    setOnClickListener { loupe() }
                }, android.widget.FrameLayout.LayoutParams(dp(48), dp(48), android.view.Gravity.END or android.view.Gravity.CENTER_VERTICAL).apply { marginEnd = dp(4) })
            })
            val aide = routeTexte ?: if (routeEnCours) getString(R.string.vol_route_attente) else null
            if (aide != null) addView(VueJour.texte(this@VolActivity, aide, 13f, R.color.muted, haut = 8).apply {
                accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE
            })
        } else addView(android.widget.FrameLayout(this@VolActivity).apply {
            background = cadreSaisie()
            addView(champNumero, android.widget.FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        })
        // Départ et arrivée, puis leurs heures : des tuiles qui se touchent, toutes pareilles.
        addView(champ(getString(R.string.vol_depart), depart?.let(::nomAeroport), getString(R.string.vol_choisir_aeroport), haut = 12) {
            choisirAeroport { depart = it; afficherFormulaire() }
        })
        addView(champ(getString(R.string.vol_arrivee), arrivee?.let(::nomAeroport), getString(R.string.vol_choisir_aeroport)) {
            choisirAeroport { arrivee = it; afficherFormulaire() }
        })
        depart?.let { a ->
            addView(champ(getString(R.string.vol_decollage, a.ville.ifEmpty { a.iata }), decollage?.format(formatSaisie()), getString(R.string.vol_choisir_heure)) {
                choisirHeure(decollage ?: dateSeule?.atTime(12, 0)) { decollage = it; afficherFormulaire() }
            })
        }
        arrivee?.let { a ->
            addView(champ(getString(R.string.vol_atterrissage, a.ville.ifEmpty { a.iata }), atterrissage?.format(formatSaisie()), getString(R.string.vol_choisir_heure)) {
                choisirHeure(atterrissage ?: decollage) { atterrissage = it; arriveeEstimee = false; afficherFormulaire() }
            })
            if (arriveeEstimee) addView(VueJour.texte(this@VolActivity, getString(R.string.vol_rapide_estimee_court), 12f, R.color.muted, haut = 4))
        }
        // « Calculer » en pilule pleine ; « Effacer le vol », rare et destructif, en simple lien.
        addView(bouton(getString(R.string.vol_calculer), Genre.PRINCIPAL) { calculer() }.apply {
            (layoutParams as LinearLayout.LayoutParams).topMargin = dp(16)
        })
        if (Store.vol(this@VolActivity, aeroports) != null || depart != null || arrivee != null || numero.isNotEmpty()) {
            addView(bouton(getString(R.string.vol_effacer), Genre.LIEN) { effacer() }.apply {
                setTextColor(Apparence.couleur(this@VolActivity, R.color.muted))
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                    gravity = android.view.Gravity.CENTER_HORIZONTAL
                    topMargin = dp(4)
                }
            })
        }
    }

    /** Le cadre d'une saisie : arrondi, filet fin, fond à peine teinté. */
    private fun cadreSaisie() = android.graphics.drawable.GradientDrawable().apply {
        cornerRadius = dp(14).toFloat()
        setColor(androidx.core.graphics.ColorUtils.blendARGB(Apparence.couleur(this@VolActivity, R.color.card), Apparence.couleur(this@VolActivity, R.color.line), 0.25f))
        setStroke(dp(1), Apparence.couleur(this@VolActivity, R.color.line))
    }

    private enum class Genre { PRINCIPAL, SECONDAIRE, LIEN }

    /**
     * Un seul système de boutons (04/11) : PRINCIPAL, pilule pleine de 52 dp ; SECONDAIRE, pilule
     * cerclée ; LIEN, texte seul. Pas de majuscules, des coins entièrement ronds.
     */
    private fun bouton(texte: String, genre: Genre, action: () -> Unit): MaterialButton {
        val style = when (genre) {
            Genre.PRINCIPAL -> com.google.android.material.R.attr.materialButtonStyle
            Genre.SECONDAIRE -> com.google.android.material.R.attr.materialButtonOutlinedStyle
            Genre.LIEN -> com.google.android.material.R.attr.borderlessButtonStyle
        }
        return MaterialButton(this, null, style).apply {
            text = texte
            isAllCaps = false
            textSize = if (genre == Genre.LIEN) 14f else 16f
            if (genre != Genre.LIEN) {
                minHeight = dp(52)
                insetTop = 0
                insetBottom = 0
                cornerRadius = dp(26)
                letterSpacing = 0.01f
                setTypeface(android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL))
            }
            if (genre == Genre.SECONDAIRE) strokeColor = android.content.res.ColorStateList.valueOf(Apparence.couleur(this@VolActivity, R.color.line))
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            setOnClickListener { action() }
        }
    }

    /** Le texte de la saisie rapide, gardé quand l'écran se redessine. */
    private var phrase = ""

    /** L'arrivée a été estimée d'après la distance : on le dit sous la tuile. */
    private var arriveeEstimee = false

    /**
     * La saisie rapide (04/11) : on écrit le vol comme on le dirait — « El Al Lyon TLV demain
     * 11h » — ou on colle le texte du billet ; ou on importe le billet (PDF, photo), dont le
     * code-barres donne la route et la date. Tout est lu sur le téléphone (SaisieVol).
     */
    private fun carteRapide() = VueJour.carte(this).apply {
        val marque = Apparence.couleur(this@VolActivity, R.color.brand)
        background = android.graphics.drawable.GradientDrawable(android.graphics.drawable.GradientDrawable.Orientation.TL_BR, intArrayOf(
            androidx.core.graphics.ColorUtils.blendARGB(Apparence.couleur(this@VolActivity, R.color.card), marque, 0.10f),
            Apparence.couleur(this@VolActivity, R.color.card),
        )).apply {
            cornerRadius = dp(20).toFloat()
            setStroke(dp(1), androidx.core.graphics.ColorUtils.blendARGB(Apparence.couleur(this@VolActivity, R.color.line), marque, 0.25f))
        }
        addView(VueJour.texte(this@VolActivity, getString(R.string.vol_rapide_titre), 17f, R.color.ink, gras = true))
        addView(VueJour.texte(this@VolActivity, getString(R.string.vol_rapide_aide), 13f, R.color.muted, haut = 4))
        val champ = EditText(this@VolActivity).apply {
            hint = getString(R.string.vol_rapide_exemple)
            setText(phrase)
            textSize = 16f
            minLines = 2
            maxLines = 6
            gravity = android.view.Gravity.TOP or android.view.Gravity.START
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            background = cadreSaisie()
            setPaddingRelative(dp(14), dp(12), dp(14), dp(12))
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(12) }
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) = Unit
                override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) = Unit
                override fun afterTextChanged(s: Editable?) { phrase = s?.toString().orEmpty() }
            })
        }
        addView(champ)
        addView(bouton(getString(R.string.vol_rapide_remplir), Genre.PRINCIPAL) {
            (getSystemService(INPUT_METHOD_SERVICE) as? android.view.inputmethod.InputMethodManager)?.hideSoftInputFromWindow(champ.windowToken, 0)
            appliquer(SaisieVol.lire(phrase, aeroports, LocalDate.now()), source = null)
        }.apply { (layoutParams as LinearLayout.LayoutParams).topMargin = dp(12) })
        addView(bouton(getString(R.string.vol_rapide_billet), Genre.SECONDAIRE) {
            choixBillet.launch(arrayOf("application/pdf", "image/*"))
        }.apply {
            (layoutParams as LinearLayout.LayoutParams).topMargin = dp(8)
            setIconResource(R.drawable.ic_billet)
            iconGravity = MaterialButton.ICON_GRAVITY_TEXT_START
            iconPadding = dp(8)
        })
        rapideTexte?.let { t ->
            addView(VueJour.texte(this@VolActivity, t, 14f, R.color.ink, haut = 12).apply {
                accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE
                setLineSpacing(dp(3).toFloat(), 1f)
            })
        }
    }

    /** Ce que la saisie rapide a compris, ou ce qui manque. */
    private var rapideTexte: String? = null

    /**
     * Remplit le formulaire avec ce qui a été compris ; l'arrivée manquante est estimée d'après
     * la distance. Complet, le vol se calcule aussitôt ; sinon on dit ce qui manque, et l'heure
     * de départ manquante (un code-barres n'en porte pas) s'ouvre d'elle-même.
     */
    private fun appliquer(p: SaisieVol.Proposition, source: String?) {
        if (p.vide) {
            rapideTexte = getString(R.string.vol_rapide_rien)
            return afficherFormulaire()
        }
        p.numero?.let { numero = it }
        p.depart?.let { depart = it }
        p.arrivee?.let { arrivee = it }
        if (p.depart != null || p.arrivee != null) routeTexte = null
        val jour = p.date ?: decollage?.toLocalDate()
        if (jour != null && p.heureDepart != null) decollage = LocalDateTime.of(jour, p.heureDepart)
        else if (jour != null) dateSeule = jour
        arriveeEstimee = false
        val d = depart
        val a = arrivee
        val t1 = decollage
        if (d != null && a != null && t1 != null) {
            val i1 = Vol.instant(t1, d)
            val za = Vol.zone(a.fuseau)
            if (p.heureArrivee != null && i1 != null && za != null) {
                // L'arrivée donnée : le premier jour où elle tombe après le départ (vol de nuit).
                var t2 = LocalDateTime.of(p.dateArrivee ?: jour ?: t1.toLocalDate(), p.heureArrivee)
                var n = 0
                while ((Vol.instant(t2, a)?.isAfter(i1) != true) && n++ < 2) t2 = t2.plusDays(1)
                atterrissage = t2
            } else if (i1 != null && za != null) {
                atterrissage = LocalDateTime.ofInstant(i1.plus(SaisieVol.dureeEstimee(d, a)), za)
                arriveeEstimee = true
            }
        }
        val compris = listOfNotNull(
            p.numero ?: p.compagnie,
            if (depart != null || arrivee != null) "${depart?.iata ?: "?"} → ${arrivee?.iata ?: "?"}" else null,
            decollage?.format(formatSaisie()) ?: (dateSeule)?.format(DateTimeFormatter.ofPattern("EEE d MMM", locale)),
        ).joinToString(" · ")
        val manque = listOfNotNull(
            if (depart == null) getString(R.string.vol_depart) else null,
            if (arrivee == null) getString(R.string.vol_arrivee) else null,
            if (decollage == null) getString(R.string.vol_rapide_heure) else null,
        )
        rapideTexte = listOfNotNull(
            source,
            getString(R.string.vol_rapide_compris, compris),
            if (arriveeEstimee) getString(R.string.vol_rapide_estimee) else null,
            if (manque.isNotEmpty()) getString(R.string.vol_rapide_manque, manque.joinToString(", ")) else null,
        ).joinToString("\n")
        afficherFormulaire()
        when {
            // Le numéro sans la route : la loupe s'en charge (avec l'accord si besoin).
            BuildConfig.SUIVI_VOL && numero.isNotEmpty() && (depart == null || arrivee == null) -> loupe()
            depart != null && decollage == null -> choisirHeure(dateSeule?.atTime(12, 0)) {
                appliquer(SaisieVol.Proposition(date = it.toLocalDate(), heureDepart = it.toLocalTime()), null)
            }
            depart != null && arrivee != null && decollage != null && atterrissage != null -> calculer()
        }
    }

    /** Une date sans heure (code-barres, « demain ») : le sélecteur d'heure s'ouvre sur elle. */
    private var dateSeule: LocalDate? = null

    /** Le billet choisi : son code-barres, lu hors du fil de l'interface. */
    private fun lireBillet(uri: android.net.Uri) {
        rapideTexte = getString(R.string.vol_rapide_lecture)
        afficherFormulaire()
        Thread {
            val code = runCatching { LecteurBillet.code(this, uri) }.getOrNull()
            val troncon = code?.let { CarteEmbarquement.lire(it, LocalDate.now()) }
            runOnUiThread {
                if (isFinishing || isDestroyed) return@runOnUiThread
                if (troncon == null) {
                    rapideTexte = getString(if (code == null) R.string.vol_rapide_billet_sans_code else R.string.vol_rapide_billet_illisible)
                    return@runOnUiThread afficherFormulaire()
                }
                appliquer(SaisieVol.Proposition(
                    numero = troncon.numero,
                    compagnie = troncon.compagnie,
                    depart = Aeroports.parIata(aeroports, troncon.depart),
                    arrivee = Aeroports.parIata(aeroports, troncon.arrivee),
                    date = troncon.date,
                ), source = getString(R.string.vol_rapide_billet_lu))
            }
        }.start()
    }

    /** La loupe : services en ligne éteints, on demande d'abord, une fois, ce qui partira. */
    private fun loupe() {
        if (Store.suiviDirect(this)) return chercherRoute()
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.vol_en_ligne_titre)
            .setMessage(R.string.vol_en_ligne_aide)
            .setPositiveButton(R.string.vol_en_ligne_oui) { _, _ ->
                Store.setSuiviDirect(this, true)
                chercherRoute()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    /** La méthode, les aéroports, les routes : repliés sous « Méthode et sources ». */
    private fun sources() = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(16), dp(8), dp(16), 0)
        val detail = LinearLayout(this@VolActivity).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
            addView(VueJour.texte(this@VolActivity, getString(R.string.zmanim_a_valider), 12f, R.color.muted))
            addView(VueJour.texte(this@VolActivity, getString(R.string.vol_source_aeroports), 12f, R.color.muted, haut = 6))
            if (BuildConfig.SUIVI_VOL) addView(VueJour.texte(this@VolActivity, getString(R.string.vol_source_adsbdb), 12f, R.color.muted, haut = 4))
        }
        addView(depliant(getString(R.string.vol_sources), detail, R.color.muted))
        addView(detail)
    }

    /** « Titre ⌄ » : un toucher montre ou cache `detail`. */
    private fun depliant(titre: String, detail: View, couleur: Int = R.color.brand): View = LinearLayout(this).apply {
        // Ouvert, il le reste quand l'écran se redessine (04/11) : il se refermait seul.
        var ouvert = titre in ouverts
        detail.visibility = if (ouvert) View.VISIBLE else View.GONE
        orientation = LinearLayout.HORIZONTAL
        gravity = android.view.Gravity.CENTER_VERTICAL
        minimumHeight = dp(44)
        background = Apparence.effetToucher(this@VolActivity)
        addView(VueJour.texte(this@VolActivity, titre, 14f, couleur, gras = true).apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        })
        // Le chevron (autoMirrored) pointe vers l'avant ; un quart de tour le tourne vers le bas.
        val bas = if (layoutDirection == View.LAYOUT_DIRECTION_RTL || resources.configuration.layoutDirection == View.LAYOUT_DIRECTION_RTL) -90f else 90f
        val chevron = android.widget.ImageView(this@VolActivity).apply {
            setImageResource(R.drawable.ic_chevron)
            imageTintList = android.content.res.ColorStateList.valueOf(Apparence.couleur(this@VolActivity, couleur))
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            rotation = if (ouvert) -bas else bas
        }
        addView(chevron, LinearLayout.LayoutParams(dp(18), dp(18)).apply { marginStart = dp(6) })
        isClickable = true
        contentDescription = titre
        setOnClickListener {
            ouvert = !ouvert
            if (ouvert) ouverts += titre else ouverts -= titre
            detail.visibility = if (ouvert) View.VISIBLE else View.GONE
            chevron.animate().rotation(if (ouvert) -bas else bas).setDuration(180).start()
        }
    }

    /** Les volets ouverts, par titre. */
    private val ouverts = mutableSetOf<String>()

    /** Une tuile à toucher : l'étiquette en petit, la valeur (ou l'invite, plus pâle), un chevron. */
    private fun champ(etiquette: String, valeur: String?, invite: String, haut: Int = 8, action: () -> Unit) = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = android.view.Gravity.CENTER_VERTICAL
        background = cadreSaisie()
        foreground = Apparence.effetToucher(this@VolActivity)
        setPaddingRelative(dp(16), dp(10), dp(12), dp(10))
        minimumHeight = dp(60)
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(haut) }
        contentDescription = "$etiquette, ${valeur ?: invite}"
        setOnClickListener { action() }
        addView(LinearLayout(this@VolActivity).apply {
            orientation = LinearLayout.VERTICAL
            addView(VueJour.texte(this@VolActivity, etiquette, 12f, R.color.muted))
            addView(VueJour.texte(this@VolActivity, valeur ?: invite, 16f, if (valeur == null) R.color.muted else R.color.ink, gras = valeur != null, haut = 2).apply {
                fontFeatureSettings = "tnum"
            })
        }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        addView(android.widget.ImageView(this@VolActivity).apply {
            setImageResource(R.drawable.ic_chevron)
            imageTintList = android.content.res.ColorStateList.valueOf(Apparence.couleur(this@VolActivity, R.color.muted))
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }, LinearLayout.LayoutParams(dp(20), dp(20)))
    }

    private fun nomAeroport(a: Vol.Aeroport) = "${a.iata} · ${a.ville.ifEmpty { a.nom }}"

    private fun formatSaisie() = DateTimeFormatter.ofPattern("EEE d MMM yyyy · HH:mm", locale)

    /**
     * Une recherche dans les 3 226 aéroports : code, ville ou nom, en lettres latines ou en hébreu.
     * Refaite (04/11) : l'index est déjà plié, la recherche part 120 ms après la dernière lettre,
     * et chaque résultat tient sur deux lignes — le code et la ville, puis l'aéroport et le pays.
     */
    private fun choisirAeroport(choisi: (Vol.Aeroport) -> Unit) {
        val langue = ChizukCard.language(this)
        var trouves: List<Vol.Aeroport> = emptyList()
        var dialogue: AlertDialog? = null
        val liste = androidx.recyclerview.widget.RecyclerView(this).apply {
            layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this@VolActivity)
            overScrollMode = View.OVER_SCROLL_NEVER
        }
        val adaptateur = object : androidx.recyclerview.widget.RecyclerView.Adapter<androidx.recyclerview.widget.RecyclerView.ViewHolder>() {
            override fun getItemCount() = trouves.size
            override fun onCreateViewHolder(parent: ViewGroup, type: Int): androidx.recyclerview.widget.RecyclerView.ViewHolder =
                object : androidx.recyclerview.widget.RecyclerView.ViewHolder(ligneAeroport()) {}
            override fun onBindViewHolder(h: androidx.recyclerview.widget.RecyclerView.ViewHolder, i: Int) {
                val a = trouves[i]
                val v = h.itemView as LinearLayout
                (v.getChildAt(0) as TextView).text = a.iata
                val textes = v.getChildAt(1) as LinearLayout
                (textes.getChildAt(0) as TextView).text = a.ville.ifEmpty { Aeroports.nomLocal(a, langue) }
                // Des noms propres et un code pays : rien à traduire.
                val detail = listOf(Aeroports.nomLocal(a, langue), a.pays).joinToString(" · ")
                (textes.getChildAt(1) as TextView).text = detail
                v.setOnClickListener { choisi(a); dialogue?.dismiss() }
            }
        }
        liste.adapter = adaptateur
        val vide = VueJour.texte(this, getString(R.string.vol_aucun_resultat), 14f, R.color.muted, haut = 12).apply { visibility = View.GONE }
        val attente = Handler(Looper.getMainLooper())
        val champ = EditText(this).apply {
            hint = getString(R.string.vol_chercher)
            isSingleLine = true
            textSize = 17f
            background = cadreSaisie()
            setPaddingRelative(dp(16), dp(14), dp(16), dp(14))
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
            imeOptions = android.view.inputmethod.EditorInfo.IME_ACTION_DONE
            setOnEditorActionListener { _, _, _ ->
                // « Entrée » prend le premier résultat.
                trouves.firstOrNull()?.let { choisi(it); dialogue?.dismiss() }
                true
            }
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) = Unit
                override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) = Unit
                override fun afterTextChanged(s: Editable?) {
                    val q = s?.toString().orEmpty()
                    attente.removeCallbacksAndMessages(null)
                    attente.postDelayed({
                        trouves = Aeroports.chercher(aeroports, q)
                        @Suppress("NotifyDataSetChanged") adaptateur.notifyDataSetChanged()
                        liste.scrollToPosition(0)
                        vide.visibility = if (trouves.isEmpty() && q.isNotBlank()) View.VISIBLE else View.GONE
                    }, 120)
                }
            })
        }
        val vue = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(8), dp(20), 0)
            addView(champ)
            addView(vide)
            addView(liste, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (resources.displayMetrics.heightPixels * 0.45).toInt()).apply { topMargin = dp(8) })
        }
        val d = MaterialAlertDialogBuilder(this)
            .setTitle(R.string.vol_choisir_aeroport)
            .setView(vue)
            .setNegativeButton(android.R.string.cancel, null)
            .setOnDismissListener { attente.removeCallbacksAndMessages(null) }
            .create()
        dialogue = d
        // Le clavier s'ouvre avec la fenêtre : on tape aussitôt.
        d.window?.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE or android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        d.show()
        champ.requestFocus()
    }

    /** Une ligne de résultat : le code IATA dans une pastille, la ville, l'aéroport et le pays. */
    private fun ligneAeroport() = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = android.view.Gravity.CENTER_VERTICAL
        setPaddingRelative(dp(4), dp(10), dp(4), dp(10))
        background = Apparence.effetToucher(this@VolActivity)
        layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        val marque = Apparence.couleur(this@VolActivity, R.color.brand)
        addView(TextView(this@VolActivity).apply {
            textSize = 13f
            setTypeface(android.graphics.Typeface.MONOSPACE, android.graphics.Typeface.BOLD)
            setTextColor(marque)
            gravity = android.view.Gravity.CENTER
            textDirection = View.TEXT_DIRECTION_LTR
            background = android.graphics.drawable.GradientDrawable().apply {
                cornerRadius = dp(8).toFloat()
                setColor(androidx.core.graphics.ColorUtils.setAlphaComponent(marque, 24))
            }
        }, LinearLayout.LayoutParams(dp(52), dp(32)))
        addView(LinearLayout(this@VolActivity).apply {
            orientation = LinearLayout.VERTICAL
            addView(VueJour.texte(this@VolActivity, "", 16f, R.color.ink, gras = true).apply { isSingleLine = true; ellipsize = android.text.TextUtils.TruncateAt.END })
            addView(VueJour.texte(this@VolActivity, "", 13f, R.color.muted, haut = 2).apply { isSingleLine = true; ellipsize = android.text.TextUtils.TruncateAt.END })
        }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = dp(12) })
    }

    /** La date puis l'heure, à l'heure locale de l'aéroport (celle du billet). */
    private fun choisirHeure(initiale: LocalDateTime?, choisi: (LocalDateTime) -> Unit) {
        val d = initiale ?: LocalDateTime.now().withSecond(0).withNano(0)
        DatePickerDialog(this, { _, annee, mois, jour ->
            TimePickerDialog(this, { _, h, m ->
                choisi(LocalDateTime.of(LocalDate.of(annee, mois + 1, jour), LocalTime.of(h, m)))
            }, d.hour, d.minute, android.text.format.DateFormat.is24HourFormat(this)).show()
        }, d.year, d.monthValue - 1, d.dayOfMonth).show()
    }

    private fun calculer() {
        val d = depart
        val a = arrivee
        val t1 = decollage
        val t2 = atterrissage
        if (d == null || a == null || t1 == null || t2 == null) return message(getString(R.string.vol_incomplet))
        val i1 = Vol.instant(t1, d)
        val i2 = Vol.instant(t2, a)
        if (i1 == null || i2 == null) return message(getString(R.string.vol_invalide_fuseau_inconnu))
        // Le même vol, calculé à nouveau, garde son relevé GPS ; un autre vol l'efface.
        val garde = Store.vol(this, aeroports)?.takeIf { it.depart == d && it.arrivee == a && it.decollage == i1 && it.atterrissage == i2 }
        val plan = Vol.Plan(numero, d, a, i1, i2, garde?.releve, garde?.sourceReleve ?: Vol.SourceReleve.GPS)
        Vol.verifier(plan)?.let { return message(getString(invalide(it))) }
        Store.setVol(this, plan)
        modifier = false
        afficherFormulaire()
        lancer(plan)
    }

    private fun invalide(i: Vol.Invalide) = when (i) {
        Vol.Invalide.MEME_AEROPORT -> R.string.vol_invalide_meme_aeroport
        Vol.Invalide.ARRIVEE_AVANT_DEPART -> R.string.vol_invalide_arrivee_avant_depart
        Vol.Invalide.TROP_LONG -> R.string.vol_invalide_trop_long
        Vol.Invalide.FUSEAU_INCONNU -> R.string.vol_invalide_fuseau_inconnu
    }

    /** Le calcul hors du fil de l'interface : deux passes de Vol (le vol, puis 30 min de retard). */
    private fun lancer(plan: Vol.Plan) {
        if (Vol.verifier(plan) != null) return
        enCours = true
        calcul = null
        afficherResultats()
        Thread {
            val c = runCatching { Vol.calculer(plan) }.getOrNull()
            runOnUiThread {
                if (isFinishing || isDestroyed) return@runOnUiThread
                enCours = false
                calcul = c
                // Les rappels suivent le dernier calcul (un relevé GPS les recale aussi).
                if (c != null) VolAlerte.enregistrer(this, VolRappels.rappels(c, Store.inIsrael(this)))
                if (c == null) message(getString(R.string.vol_reserve_erreur)) else afficherResultats()
            }
        }.start()
    }

    private fun effacer() {
        Store.setVol(this, null)
        VolAlerte.enregistrer(this, emptyList())
        numero = ""
        depart = null
        arrivee = null
        decollage = null
        atterrissage = null
        calcul = null
        modifier = false
        phrase = ""
        rapideTexte = null
        dateSeule = null
        arriveeEstimee = false
        afficherFormulaire()
    }

    private fun message(texte: String) {
        MaterialAlertDialogBuilder(this).setMessage(texte).setPositiveButton(android.R.string.ok, null).show()
    }

    private fun afficherResultats() {
        val zone = findViewById<LinearLayout>(R.id.vol_resultats) ?: return
        // La hauteur gardée le temps de reconstruire : la page ne saute pas sous le doigt.
        zone.minimumHeight = zone.height
        val y = defile.scrollY
        zone.removeAllViews()
        compteur = null
        defile.post { defile.scrollTo(0, y); zone.minimumHeight = 0 }
        val c = calcul
        findViewById<TextView>(R.id.vol_resume).text = c?.plan?.let {
            getString(R.string.vol_carte_vol, it.numero.ifEmpty { "✈" }, it.depart.iata, it.arrivee.iata)
        } ?: getString(R.string.vol_carte_sous)
        if (enCours) {
            zone.addView(VueJour.carte(this).apply { addView(VueJour.texte(this@VolActivity, getString(R.string.vol_calcul_en_cours), 15f, R.color.muted)) })
            return
        }
        if (c == null) return
        val maintenant = Instant.now()
        signatureAffichee = signature(c, maintenant)
        // L'essentiel d'abord (04/11) : la prière du moment, la frise, Chabbat et fêtes ; puis les
        // rappels ; la position et sa précision, repliées, en dernier.
        zone.addView(cartePriere(c, maintenant))
        zone.addView(carteZmanim(c))
        carteJalons(c)?.let(zone::addView)
        zone.addView(carteRappels(c, maintenant))
        zone.addView(carteFiabilite(c))
    }

    /** Chabbat, fêtes et Omer sur la route (VolCalendrier) : des heures, sans conseil. */
    private fun carteJalons(c: Vol.Calcul): View? {
        val israel = Store.inIsrael(this)
        val minutes = Store.allumage(this)
        val jalons = VolCalendrier.jalons(c, israel, minutes)
        if (jalons.isEmpty()) return null
        return VueJour.carte(this).apply {
            addView(VueJour.texte(this@VolActivity, getString(R.string.vol_jalons_titre), 13f, R.color.muted))
            jalons.forEach { j ->
                val nom = (listOfNotNull(if (j.chabbat) getString(R.string.vol_chabbat) else null) +
                    j.fetes.mapNotNull { Libelles.res("fete_$it")?.let(::getString) }).joinToString(" · ")
                val heure = deuxHeures(c.plan, j.instant)
                val texte = when (j.type) {
                    VolCalendrier.Type.ENTREE -> getString(R.string.vol_jalon_entree, nom, heure)
                    VolCalendrier.Type.SORTIE -> getString(R.string.vol_jalon_sortie, nom, heure)
                    VolCalendrier.Type.ENCHAINEMENT -> getString(R.string.vol_jalon_enchainement, nom, heure)
                    VolCalendrier.Type.OMER -> getString(R.string.vol_jalon_omer, j.omer.toString(), heure)
                }
                addView(VueJour.texte(this@VolActivity, texte, 15f, R.color.ink, gras = true, haut = 10))
                j.allumage?.let { addView(VueJour.texte(this@VolActivity, getString(R.string.vol_jalon_allumage, minutes.toString(), deuxHeures(c.plan, it)), 13f, R.color.ink, haut = 2)) }
                addView(VueJour.texte(this@VolActivity, position(Vol.Evenement("", j.instant, j.latitude, j.longitude)), 12f, R.color.muted, haut = 1))
            }
            addView(VueJour.texte(this@VolActivity, getString(R.string.vol_jalons_note,
                getString(if (israel) R.string.vol_jalons_israel else R.string.vol_jalons_diaspora)), 12f, R.color.muted, haut = 10))
        }
    }

    /**
     * « mar. 10 · 18:42 TLV · 11:42 JFK » : l'heure du départ et celle de l'arrivée, le jour s'il
     * change. Seuls « heure code » sont isolés de gauche à droite : le jour reste dans le sens de la langue.
     */
    private fun deuxHeures(plan: Vol.Plan, t: Instant): String = VolAlerte.deuxHeures(plan, t, locale)

    /**
     * Les rappels pendant le vol (VolRappels) : l'interrupteur, puis chaque rappel à venir
     * avec son heure. Les notifications partent de VolAlerte, même téléphone fermé.
     */
    private fun carteRappels(c: Vol.Calcul, maintenant: Instant) = VueJour.carte(this).apply {
        addView(VueJour.texte(this@VolActivity, getString(R.string.vol_rappels_titre), 13f, R.color.muted))
        val actifs = Store.volRappelsActifs(this@VolActivity)
        addView(com.google.android.material.materialswitch.MaterialSwitch(this@VolActivity).apply {
            text = getString(R.string.vol_rappels_activer)
            isChecked = actifs
            setOnCheckedChangeListener { _, v ->
                Store.setVolRappelsActifs(this@VolActivity, v)
                VolAlerte.programmer(this@VolActivity)
                if (v && Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(this@VolActivity, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    autorisationNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                afficherResultats()
            }
        })
        if (!actifs) return@apply
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(this@VolActivity, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            addView(VueJour.texte(this@VolActivity, getString(R.string.vol_rappels_sans_permission), 13f, R.color.ink, haut = 4))
        }
        val aVenir = VolRappels.rappels(c, Store.inIsrael(this@VolActivity)).filter { it.instant.isAfter(maintenant) }
        if (aVenir.isEmpty()) addView(VueJour.texte(this@VolActivity, getString(R.string.vol_rappels_aucun), 14f, R.color.ink, haut = 4))
        aVenir.forEach { r ->
            addView(VueJour.texte(this@VolActivity, VolAlerte.textes(this@VolActivity, r, c.plan).first, 15f, R.color.ink, gras = true, haut = 10))
            addView(VueJour.texte(this@VolActivity, deuxHeures(c.plan, r.instant), 13f, R.color.ink, haut = 2))
        }
        addView(VueJour.texte(this@VolActivity, getString(R.string.vol_rappels_note), 12f, R.color.muted, haut = 10))
    }

    /**
     * La prière du moment mise en avant : la carte prend le ciel de son heure (04/11) — l'aube
     * pour Cha'harit, l'or de l'après-midi pour Min'ha, la nuit pour Arvit —, texte en blanc,
     * le compte à rebours en grand.
     */
    private fun cartePriere(c: Vol.Calcul, maintenant: Instant) = VueJour.carte(this).apply {
        val p = VolPriere.priere(c, maintenant)
        val ciel = when (p?.first) {
            "chaharit" -> intArrayOf(0xFF2B3F6E.toInt(), 0xFF8C5A7A.toInt(), 0xFFE0906B.toInt())
            "minha" -> intArrayOf(0xFF17525A.toInt(), 0xFF6E7A3E.toInt(), 0xFFC9962E.toInt())
            null -> intArrayOf(Apparence.couleur(this@VolActivity, R.color.brand), androidx.core.graphics.ColorUtils.blendARGB(Apparence.couleur(this@VolActivity, R.color.brand), 0xFF000000.toInt(), 0.35f))
            else -> intArrayOf(0xFF0E1530.toInt(), 0xFF1E2A55.toInt(), 0xFF3E4F86.toInt())
        }
        background = android.graphics.drawable.GradientDrawable(android.graphics.drawable.GradientDrawable.Orientation.TL_BR, ciel).apply {
            cornerRadius = dp(24).toFloat()
        }
        setPadding(dp(20), dp(20), dp(20), dp(20))
        elevation = dp(2).toFloat()
        val blanc = { a: Int -> androidx.core.graphics.ColorUtils.setAlphaComponent(0xFFFFFFFF.toInt(), a) }
        addView(VueJour.texte(this@VolActivity, getString(R.string.vol_priere_titre).uppercase(locale), 12f, R.color.muted).apply {
            setTextColor(blanc(200)); letterSpacing = 0.08f
        })
        if (p == null) {
            addView(VueJour.texte(this@VolActivity, getString(R.string.vol_hors_fenetre), 15f, R.color.ink, haut = 8).apply { setTextColor(blanc(240)) })
            return@apply
        }
        val (priere, limite) = p
        val nom = when (priere) {
            "chaharit" -> R.string.widget_zmanim_chaharit
            "minha" -> R.string.widget_zmanim_minha
            else -> R.string.widget_zmanim_arvit
        }
        addView(VueJour.texte(this@VolActivity, getString(nom), 34f, R.color.ink, gras = true, haut = 4).apply { setTextColor(blanc(255)) })
        if (limite == null) {
            addView(VueJour.texte(this@VolActivity, getString(R.string.vol_sans_limite), 14f, R.color.ink, haut = 4).apply { setTextColor(blanc(225)) })
        } else {
            val minutes = Duration.between(maintenant, limite).toMinutes().toInt().coerceAtLeast(0)
            addView(VueJour.texte(this@VolActivity, getString(R.string.vol_dans, dureeEnMots(minutes)), 22f, R.color.ink, gras = true, haut = 8).apply {
                setTextColor(blanc(255)); fontFeatureSettings = "tnum"
            }.also { compteur = it })
            addView(VueJour.texte(this@VolActivity, getString(R.string.vol_jusqua, deuxHeures(c.plan, limite)), 14f, R.color.ink, haut = 4).apply {
                setTextColor(blanc(215)); fontFeatureSettings = "tnum"
            })
            c.enVol.firstOrNull { it.instant == limite }?.ecartSiRetard?.let {
                addView(VueJour.texte(this@VolActivity, ecart(it), 12f, R.color.muted, haut = 4).apply { setTextColor(blanc(170)) })
            }
        }
    }

    /** « 10 minutes », « 1 minute », « 1 h 11 min », « 2 h » : le compte à rebours en toutes lettres. */
    private fun dureeEnMots(minutes: Int): String = when {
        minutes == 1 -> getString(R.string.vol_duree_une_minute)
        minutes < 60 -> getString(R.string.vol_duree_minutes, minutes.toString())
        minutes % 60 == 0 -> getString(R.string.vol_duree_heures, (minutes / 60).toString())
        else -> getString(R.string.vol_duree_heures_minutes, (minutes / 60).toString(), (minutes % 60).toString())
    }

    private fun ecart(d: Duration) = getString(if (calcul?.fiabilite != null && calcul?.fiabilite != Vol.Fiabilite.ROUTE_PREVUE) R.string.vol_ecart_gps else R.string.vol_ecart,
        (if (d.isNegative) "−" else "+") + abs(d.toMinutes()))

    private fun demanderGps() {
        val accordee = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (accordee) releverGps()
        else autorisationGps.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
    }

    /**
     * Un relevé du GPS seul (pas le réseau : en vol, il n'y en a pas), 90 s au plus. Il recale la
     * route s'il tombe pendant le vol prévu ; sinon on le dit et la route prévue reste.
     */
    @SuppressLint("MissingPermission") // vérifiée par demanderGps
    private fun releverGps() {
        val lm = getSystemService(LocationManager::class.java) ?: return message(getString(R.string.vol_gps_echec))
        if (!runCatching { lm.isProviderEnabled(LocationManager.GPS_PROVIDER) }.getOrDefault(false)) return message(getString(R.string.vol_gps_echec))
        gpsEnCours = true
        afficherResultats()
        val delai = Handler(Looper.getMainLooper())
        var fini = false
        val annulation = CancellationSignal()
        var ecouteur: LocationListener? = null
        fun conclure(l: Location?) {
            if (fini || isFinishing || isDestroyed) return
            fini = true
            delai.removeCallbacksAndMessages(null)
            annulation.cancel()
            ecouteur?.let { runCatching { lm.removeUpdates(it) } }
            gpsEnCours = false
            if (l == null) {
                afficherResultats()
                return message(getString(R.string.vol_gps_echec))
            }
            appliquerReleve(Vol.Point(Instant.ofEpochMilli(l.time), l.latitude, l.longitude))
        }
        delai.postDelayed({ conclure(null) }, GPS_MS)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                lm.getCurrentLocation(LocationManager.GPS_PROVIDER, annulation, mainExecutor) { conclure(it) }
            } else {
                // Avant Android 11, les quatre méthodes de LocationListener sont abstraites (ChoixLieu).
                val e = object : LocationListener {
                    override fun onLocationChanged(location: Location) = conclure(location)
                    override fun onProviderEnabled(provider: String) = Unit
                    override fun onProviderDisabled(provider: String) = conclure(null)

                    @Deprecated("Android 10 et avant seulement")
                    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
                }
                ecouteur = e
                @Suppress("DEPRECATION")
                lm.requestSingleUpdate(LocationManager.GPS_PROVIDER, e, Looper.getMainLooper())
            }
        } catch (e: SecurityException) {
            conclure(null)
        } catch (e: IllegalArgumentException) {
            conclure(null)
        }
    }

    /**
     * Le suivi en direct (SuiviVol), édition complète seulement : un interrupteur désactivé par
     * défaut, qui dit ce qui part ; l'indicatif ; le bouton. Sans relais configuré à la
     * construction, OpenSky en accès anonyme et gratuit.
     */
    private fun ajouterSuiviDirect(carte: LinearLayout) {
        val actif = Store.suiviDirect(this)
        ajouterInterrupteurEnLigne(carte)
        if (!actif) return
        val champ = EditText(this).apply {
            hint = getString(R.string.vol_indicatif)
            setText(Store.volIndicatif(this@VolActivity))
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS
            isSingleLine = true
            filters = arrayOf(android.text.InputFilter.LengthFilter(9))
        }
        carte.addView(champ)
        carte.addView(bouton(getString(if (directEnCours) R.string.vol_direct_attente else R.string.vol_direct_bouton), Genre.SECONDAIRE) {
            val indicatif = SuiviVol.normaliser(champ.text.toString()) ?: return@bouton message(getString(R.string.vol_indicatif_invalide))
            Store.setVolIndicatif(this@VolActivity, indicatif)
            demanderDirect(indicatif)
        }.apply {
            isEnabled = !directEnCours
            (layoutParams as LinearLayout.LayoutParams).topMargin = dp(8)
        })
        carte.addView(VueJour.texte(this, getString(R.string.vol_source_opensky), 11f, R.color.muted, haut = 2))
    }

    /**
     * L'interrupteur des services en ligne (02/11) : un seul, désactivé par défaut, pour la route
     * d'un numéro (adsbdb) et le suivi en direct (OpenSky). Il figure dans le formulaire et sous
     * les résultats ; le basculer redessine les deux, pour que la loupe suive.
     */
    private fun ajouterInterrupteurEnLigne(carte: LinearLayout) {
        val actif = Store.suiviDirect(this)
        carte.addView(com.google.android.material.materialswitch.MaterialSwitch(this).apply {
            text = getString(R.string.vol_direct_activer)
            isChecked = actif
            setOnCheckedChangeListener { _, v -> Store.setSuiviDirect(this@VolActivity, v); afficherFormulaire() }
        })
        if (!actif) carte.addView(VueJour.texte(this, getString(R.string.vol_en_ligne_aide), 12f, R.color.muted, haut = 2))
    }

    /** Le formulaire est affiché (et non son résumé) : il porte alors l'interrupteur des services en ligne. */
    private var formulaireOuvert = true

    private var directEnCours = false

    private var routeEnCours = false
    private var routeTexte: String? = null

    /**
     * La route du numéro saisi (RouteVol, adsbdb.com), hors du fil de l'interface : remplit l'arrivée
     * (et le départ s'il est vide) et l'indicatif du suivi en direct ; les heures restent à saisir.
     */
    private fun chercherRoute() {
        // La loupe n'existe que services en ligne activés ; la garde tient même si elle réapparaissait.
        if (!BuildConfig.SUIVI_VOL || !Store.suiviDirect(this)) return
        if (RouteVol.normaliser(numero) == null) {
            routeTexte = getString(R.string.vol_route_invalide)
            return afficherFormulaire()
        }
        routeEnCours = true
        routeTexte = null
        afficherFormulaire()
        val n = numero
        val d = depart?.iata
        Thread {
            val r = RouteVol.chercher(n, d)
            runOnUiThread {
                if (isFinishing || isDestroyed) return@runOnUiThread
                routeEnCours = false
                routeTexte = when (r) {
                    is RouteVol.Resultat.Trouve -> {
                        r.indicatif?.let { Store.setVolIndicatif(this, it) }
                        val de = Aeroports.parIata(aeroports, r.de)
                        val vers = Aeroports.parIata(aeroports, r.vers)
                        if (de == null || vers == null) getString(R.string.vol_route_inconnue, if (de == null) r.de else r.vers)
                        else {
                            depart = de
                            arrivee = vers
                            // La route trouvée complète la saisie rapide : arrivée estimée, heure, calcul.
                            if (rapideTexte != null) {
                                routeTexte = getString(R.string.vol_route_trouvee, n.uppercase(), r.compagnie, r.de, r.vers)
                                defile.post { appliquer(SaisieVol.Proposition(depart = de, arrivee = vers, date = decollage?.toLocalDate() ?: dateSeule,
                                    heureDepart = decollage?.toLocalTime()), source = routeTexte) }
                            }
                            getString(R.string.vol_route_trouvee, n.uppercase(), r.compagnie, r.de, r.vers)
                        }
                    }
                    is RouteVol.Resultat.AutreDepart -> getString(R.string.vol_route_autre_depart, n.uppercase(), r.escales.joinToString(" → "), d)
                    RouteVol.Resultat.Introuvable -> getString(R.string.vol_route_introuvable)
                    RouteVol.Resultat.Invalide -> getString(R.string.vol_route_invalide)
                    is RouteVol.Resultat.Erreur -> getString(R.string.vol_route_erreur, raisonLisible(r.raison))
                }
                afficherFormulaire()
            }
        }.start()
    }

    /** Une raison de SuiviVol.Raison, dans la langue de l'interface. */
    private fun raisonLisible(raison: String): String = SuiviVol.Raison.codeServeur(raison)?.let { getString(R.string.vol_erreur_serveur, it) }
        ?: getString(when (raison) {
            SuiviVol.Raison.HORS_LIGNE -> R.string.vol_erreur_hors_ligne
            SuiviVol.Raison.QUOTA -> R.string.vol_erreur_quota
            SuiviVol.Raison.ILLISIBLE -> R.string.vol_erreur_illisible
            SuiviVol.Raison.POSITION_INVALIDE -> R.string.vol_erreur_position
            SuiviVol.Raison.RELAIS_ABSENT -> R.string.vol_erreur_relais_absent
            else -> R.string.vol_erreur_service
        })

    /** La position en direct, hors du fil de l'interface : seulement pendant le vol prévu. */
    private fun demanderDirect(indicatif: String) {
        val plan = Store.vol(this, aeroports) ?: return
        val maintenant = Instant.now()
        if (maintenant.isBefore(plan.decollage) || maintenant.isAfter(plan.atterrissage)) return message(getString(R.string.vol_gps_hors_vol))
        val estime = Vol.position(plan, maintenant)
        directEnCours = true
        afficherResultats()
        val fournisseur: SuiviVol.FournisseurPosition = SuiviVol.fournisseur(BuildConfig.RELAIS_VOL)
        Thread {
            val r = fournisseur.position(indicatif, estime)
            runOnUiThread {
                if (isFinishing || isDestroyed) return@runOnUiThread
                directEnCours = false
                when (r) {
                    is SuiviVol.Resultat.Trouve -> appliquerReleve(r.point, Vol.SourceReleve.DIRECT)
                    SuiviVol.Resultat.Introuvable -> { afficherResultats(); message(getString(R.string.vol_direct_introuvable)) }
                    is SuiviVol.Resultat.Erreur -> { afficherResultats(); message(getString(R.string.vol_direct_erreur, raisonLisible(r.raison))) }
                }
            }
        }.start()
    }

    /** Garde le relevé avec le vol et recalcule ; null : retour à la route prévue. */
    private fun appliquerReleve(releve: Vol.Point?, source: Vol.SourceReleve = Vol.SourceReleve.GPS) {
        val plan = Store.vol(this, aeroports) ?: return
        val nouveau = plan.copy(releve = releve, sourceReleve = source)
        if (releve != null && !Vol.releveUtilisable(nouveau)) {
            afficherResultats()
            return message(getString(R.string.vol_gps_hors_vol))
        }
        Store.setVol(this, nouveau)
        lancer(nouveau)
    }

    /**
     * La position (29/10) : une étiquette pour la route prévue, le détail du calcul à déplier,
     * puis « Préciser la position » (GPS, suivi en direct).
     */
    private fun carteFiabilite(c: Vol.Calcul) = VueJour.carte(this).apply {
        val r = c.plan.releve
        if (r == null || c.fiabilite == Vol.Fiabilite.ROUTE_PREVUE) {
            addView(VueJour.texte(this@VolActivity, getString(R.string.vol_fiabilite_court), 14f, R.color.ink, gras = true).apply {
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                setPadding(dp(12), dp(6), dp(12), dp(6))
                background = android.graphics.drawable.GradientDrawable().apply {
                    cornerRadius = dp(999).toFloat()
                    setColor(androidx.core.graphics.ColorUtils.setAlphaComponent(Apparence.couleur(this@VolActivity, R.color.brand), 26))
                }
            })
        } else addView(VueJour.texte(this@VolActivity, getString(if (c.fiabilite == Vol.Fiabilite.RELEVE_DIRECT) R.string.vol_fiabilite_direct else R.string.vol_fiabilite_gps,
            deuxHeures(c.plan, r.instant), position(Vol.Evenement("", r.instant, r.latitude, r.longitude))), 14f, R.color.ink))
        val detail = VueJour.texte(this@VolActivity, getString(R.string.vol_fiabilite), 14f, R.color.muted).apply { visibility = View.GONE }
        addView(depliant(getString(R.string.vol_comment_calcule), detail))
        addView(detail)
        // GPS et suivi en direct, rarement utiles : sous « Préciser la position », replié (04/11).
        val preciser = LinearLayout(this@VolActivity).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
        }
        addView(depliant(getString(R.string.vol_preciser_titre), preciser))
        addView(preciser)
        preciser.addView(bouton(getString(if (gpsEnCours) R.string.vol_gps_attente else R.string.vol_gps), Genre.SECONDAIRE) { demanderGps() }.apply {
            isEnabled = !gpsEnCours
            (layoutParams as LinearLayout.LayoutParams).topMargin = dp(4)
        })
        if (r != null) preciser.addView(bouton(getString(R.string.vol_gps_oublier), Genre.LIEN) { appliquerReleve(null) })
        if (BuildConfig.SUIVI_VOL) ajouterSuiviDirect(preciser)
        Vol.Reserve.entries.filter { it in c.reserves }.forEach { r ->
            val texte = when (r) {
                Vol.Reserve.HAUTE_LATITUDE -> R.string.vol_reserve_haute_latitude
                Vol.Reserve.ABSENT -> R.string.vol_reserve_absent
                Vol.Reserve.ERREUR -> R.string.vol_reserve_erreur
                Vol.Reserve.A_REBOURS -> R.string.vol_reserve_a_rebours
            }
            addView(VueJour.texte(this@VolActivity, getString(texte), 14f, R.color.ink, haut = 8))
        }
        val date = DateTimeFormatter.ofPattern("EEE d MMM", locale)
        c.lignesDate.forEach { l ->
            addView(VueJour.texte(this@VolActivity, getString(R.string.vol_ligne_date, deuxHeures(c.plan, l.instant),
                l.avant.format(date), l.apres.format(date)), 14f, R.color.ink, haut = 8))
        }
    }

    /**
     * La frise du vol (29/10) : du décollage à l'atterrissage, une icône par zman dans la couleur de
     * son ciel (nuit, aube, jour, coucher), et « maintenant » à sa place. La phrase du retard est
     * dite une fois en tête ; l'écart d'une ligne n'y figure que s'il s'éloigne de 5 min ou plus.
     */
    private fun carteZmanim(c: Vol.Calcul) = VueJour.carte(this).apply {
        addView(VueJour.texte(this@VolActivity, getString(R.string.vol_en_vol_titre), 13f, R.color.muted))
        if (c.enVol.isEmpty()) {
            addView(VueJour.texte(this@VolActivity, getString(R.string.vol_aucun_zman), 14f, R.color.ink, haut = 4))
            return@apply
        }
        val ecarts = c.enVol.mapNotNull { it.ecartSiRetard }.sorted()
        val median = ecarts.getOrNull(ecarts.size / 2)
        if (median != null) addView(VueJour.texte(this@VolActivity, getString(R.string.vol_frise_retard), 13f, R.color.muted, haut = 2))
        val frise = LinearLayout(this@VolActivity).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(12) }
        }
        addView(frise)
        val lignes = mutableListOf<View>()
        lignes += pointFrise(Ciel.BOUT, getString(R.string.vol_frise_decollage, c.plan.depart.iata), c.plan, c.plan.decollage, null)
        val maintenant = Instant.now()
        var place = maintenant.isBefore(c.plan.decollage) || maintenant.isAfter(c.plan.atterrissage)
        c.enVol.forEach { e ->
            if (!place && maintenant.isBefore(e.instant)) { lignes += pointFrise(Ciel.MAINTENANT, getString(R.string.vol_frise_maintenant), c.plan, null, null); place = true }
            val ecartLigne = e.ecartSiRetard?.takeIf { median != null && it.minus(median).abs() >= Duration.ofMinutes(5) }?.let(::ecart)
            lignes += pointFrise(CIEL[e.cle] ?: Ciel.JOUR, Libelles.res("zman_${e.cle}")?.let(::getString) ?: e.cle, c.plan, e.instant,
                listOfNotNull(position(e), ecartLigne).joinToString(" · "))
        }
        lignes += pointFrise(Ciel.BOUT, getString(R.string.vol_frise_atterrissage, c.plan.arrivee.iata), c.plan, c.plan.atterrissage, null)
        lignes.forEachIndexed { i, v ->
            if (i == lignes.lastIndex) v.findViewWithTag<View>(TRAIT)?.visibility = View.INVISIBLE
            frise.addView(v)
        }
    }

    private enum class Ciel(val icone: Int, val couleur: Int) {
        NUIT(R.drawable.ic_frise_nuit, 0xFF3E4F86.toInt()), AUBE(R.drawable.ic_frise_aube, 0xFFD4856B.toInt()),
        JOUR(R.drawable.ic_frise_jour, 0xFFC99A2E.toInt()), CREPUSCULE(R.drawable.ic_frise_crepuscule, 0xFFB5543A.toInt()),
        BOUT(R.drawable.ic_frise_avion, 0), MAINTENANT(0, 0),
    }

    /** Une ligne de la frise : l'icône sur son trait, le nom, l'heure du départ en gras, celle de l'arrivée plus légère. */
    private fun pointFrise(ciel: Ciel, nom: String, plan: Vol.Plan, t: Instant?, detail: String?): View {
        val marque = Apparence.couleur(this, R.color.brand)
        val teinte = if (ciel.couleur == 0) marque else ciel.couleur
        val gauche = android.widget.FrameLayout(this).apply {
            addView(View(this@VolActivity).apply {
                tag = TRAIT
                setBackgroundColor(Apparence.couleur(this@VolActivity, R.color.line))
            }, android.widget.FrameLayout.LayoutParams(dp(2), ViewGroup.LayoutParams.MATCH_PARENT, android.view.Gravity.CENTER_HORIZONTAL).apply { topMargin = dp(30) })
            addView(if (ciel == Ciel.MAINTENANT) View(this@VolActivity).apply {
                background = android.graphics.drawable.GradientDrawable().apply {
                    shape = android.graphics.drawable.GradientDrawable.OVAL
                    setColor(Apparence.couleur(this@VolActivity, R.color.card))
                    setStroke(dp(3), marque)
                }
            } else android.widget.ImageView(this@VolActivity).apply {
                setImageResource(ciel.icone)
                imageTintList = android.content.res.ColorStateList.valueOf(teinte)
                scaleType = android.widget.ImageView.ScaleType.CENTER
                background = android.graphics.drawable.GradientDrawable().apply {
                    shape = android.graphics.drawable.GradientDrawable.OVAL
                    setColor(androidx.core.graphics.ColorUtils.blendARGB(Apparence.couleur(this@VolActivity, R.color.card), teinte, 0.14f))
                }
            }, if (ciel == Ciel.MAINTENANT) android.widget.FrameLayout.LayoutParams(dp(16), dp(16), android.view.Gravity.CENTER_HORIZONTAL).apply { topMargin = dp(7) }
            else android.widget.FrameLayout.LayoutParams(dp(30), dp(30), android.view.Gravity.CENTER_HORIZONTAL))
        }
        val corps = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPaddingRelative(dp(14), 0, 0, dp(14))
            if (ciel == Ciel.MAINTENANT) {
                addView(VueJour.texte(this@VolActivity, nom.uppercase(locale), 14f, R.color.brand, gras = true, haut = 4).apply { letterSpacing = 0.06f })
            } else {
                addView(VueJour.texte(this@VolActivity, nom, 15f, R.color.ink))
                if (t != null) {
                    val parties = deuxHeures(plan, t).split(" · ", limit = 2)
                    val heure = android.text.SpannableStringBuilder(parties[0]).apply {
                        setSpan(android.text.style.StyleSpan(android.graphics.Typeface.BOLD), 0, length, 0)
                        setSpan(android.text.style.ForegroundColorSpan(Apparence.couleur(this@VolActivity, R.color.ink)), 0, length, 0)
                        parties.getOrNull(1)?.let { append(" · $it") }
                    }
                    addView(VueJour.texte(this@VolActivity, heure, 16f, R.color.muted, haut = 2).apply { fontFeatureSettings = "tnum" })
                }
                if (!detail.isNullOrEmpty()) addView(VueJour.texte(this@VolActivity, detail, 12f, R.color.muted, haut = 1))
            }
        }
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(gauche, LinearLayout.LayoutParams(dp(30), ViewGroup.LayoutParams.MATCH_PARENT))
            addView(corps, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        }
    }

    private fun position(e: Vol.Evenement): String {
        val lat = String.format(locale, "%.1f", abs(e.latitude))
        val lon = String.format(locale, "%.1f", abs(e.longitude))
        return getString(R.string.vol_position,
            getString(if (e.latitude >= 0) R.string.vol_lat_nord else R.string.vol_lat_sud, lat),
            getString(if (e.longitude >= 0) R.string.vol_lon_est else R.string.vol_lon_ouest, lon))
    }

    private fun dp(v: Int) = VueJour.dp(this, v)

    private companion object {
        const val TRAIT = "trait"
        val CIEL = mapOf("chatsot_layla" to Ciel.NUIT, "tset" to Ciel.NUIT, "tset_rt" to Ciel.NUIT,
            "alot" to Ciel.AUBE, "misheyakir" to Ciel.AUBE, "netz" to Ciel.AUBE, "chkia" to Ciel.CREPUSCULE)
        const val RAFRAICHISSEMENT_MS = 30_000L
        const val GPS_MS = 90_000L
        const val ETAT_NUMERO = "vol_numero"
        const val ETAT_DEPART = "vol_depart"
        const val ETAT_ARRIVEE = "vol_arrivee"
        const val ETAT_DECOLLAGE = "vol_decollage"
        const val ETAT_ATTERRISSAGE = "vol_atterrissage"
    }
}
