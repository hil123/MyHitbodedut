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
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ListView
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
    private val autorisationNotifications = registerForActivityResult(ActivityResultContracts.RequestPermission()) { afficherResultats() }
    private val minuterie = Handler(Looper.getMainLooper())
    private val rafraichir = object : Runnable {
        override fun run() {
            afficherResultats()
            minuterie.postDelayed(this, RAFRAICHISSEMENT_MS)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_vol)
        aeroports = Data.aeroports(this)
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

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(ETAT_NUMERO, numero)
        outState.putString(ETAT_DEPART, depart?.iata)
        outState.putString(ETAT_ARRIVEE, arrivee?.iata)
        outState.putString(ETAT_DECOLLAGE, decollage?.toString())
        outState.putString(ETAT_ATTERRISSAGE, atterrissage?.toString())
    }

    override fun onResume() {
        super.onResume()
        minuterie.post(rafraichir)
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
        contenu.removeAllViews()
        val garde = Store.vol(this, aeroports)
        contenu.addView(if (garde != null && !modifier) carteResume(garde) else carteFormulaire())
        contenu.addView(LinearLayout(this).apply {
            id = R.id.vol_resultats
            orientation = LinearLayout.VERTICAL
        })
        contenu.addView(sources())
        afficherResultats()
    }

    /** « LY81 · TLV → BKK », « Modifier », et les heures du billet. */
    private fun carteResume(plan: Vol.Plan) = VueJour.carte(this).apply {
        addView(LinearLayout(this@VolActivity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            addView(VueJour.texte(this@VolActivity, getString(R.string.vol_carte_vol, plan.numero.ifEmpty { "✈" }, plan.depart.iata, plan.arrivee.iata),
                17f, R.color.ink, gras = true).apply { layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f) })
            addView(MaterialButton(this@VolActivity, null, com.google.android.material.R.attr.borderlessButtonStyle).apply {
                text = getString(R.string.vol_modifier)
                setOnClickListener { modifier = true; afficherFormulaire() }
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
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS
            isSingleLine = true
            filters = arrayOf(android.text.InputFilter.LengthFilter(12))
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) = Unit
                override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) = Unit
                override fun afterTextChanged(s: Editable?) { numero = s?.toString()?.trim().orEmpty() }
            })
        }
        if (BuildConfig.SUIVI_VOL) {
            // La loupe au bout du champ cherche la route du numéro (RouteVol) ; une ligne dit ce qui part.
            champNumero.setPaddingRelative(champNumero.paddingStart, champNumero.paddingTop, dp(52), champNumero.paddingBottom)
            addView(android.widget.FrameLayout(this@VolActivity).apply {
                addView(champNumero, android.widget.FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
                addView(android.widget.ImageButton(this@VolActivity).apply {
                    setImageResource(R.drawable.ic_loupe)
                    imageTintList = android.content.res.ColorStateList.valueOf(Apparence.couleur(this@VolActivity, R.color.brand))
                    background = getDrawable(android.R.drawable.list_selector_background)
                    contentDescription = getString(R.string.vol_route_bouton)
                    isEnabled = !routeEnCours
                    alpha = if (routeEnCours) 0.4f else 1f
                    setOnClickListener { chercherRoute() }
                }, android.widget.FrameLayout.LayoutParams(dp(48), dp(48), android.view.Gravity.END or android.view.Gravity.CENTER_VERTICAL))
            })
            addView(VueJour.texte(this@VolActivity, routeTexte ?: getString(if (routeEnCours) R.string.vol_route_attente else R.string.vol_route_aide),
                12f, R.color.muted, haut = 2).apply { accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE })
        } else addView(champNumero)
        addView(champ(getString(R.string.vol_depart), depart?.let(::nomAeroport) ?: getString(R.string.vol_choisir_aeroport)) {
            choisirAeroport { depart = it; afficherFormulaire() }
        })
        addView(champ(getString(R.string.vol_arrivee), arrivee?.let(::nomAeroport) ?: getString(R.string.vol_choisir_aeroport)) {
            choisirAeroport { arrivee = it; afficherFormulaire() }
        })
        depart?.let { a ->
            addView(champ(getString(R.string.vol_decollage, a.ville.ifEmpty { a.iata }), decollage?.format(formatSaisie()) ?: getString(R.string.vol_choisir_heure)) {
                choisirHeure(decollage) { decollage = it; afficherFormulaire() }
            })
        }
        arrivee?.let { a ->
            addView(champ(getString(R.string.vol_atterrissage, a.ville.ifEmpty { a.iata }), atterrissage?.format(formatSaisie()) ?: getString(R.string.vol_choisir_heure)) {
                choisirHeure(atterrissage ?: decollage) { atterrissage = it; afficherFormulaire() }
            })
        }
        // « Calculer » sur toute la largeur ; « Effacer le vol », rare et destructif, en simple lien.
        addView(MaterialButton(this@VolActivity).apply {
            text = getString(R.string.vol_calculer)
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(12) }
            setOnClickListener { calculer() }
        })
        addView(MaterialButton(this@VolActivity, null, com.google.android.material.R.attr.borderlessButtonStyle).apply {
            text = android.text.SpannableString(getString(R.string.vol_effacer)).apply { setSpan(android.text.style.UnderlineSpan(), 0, length, 0) }
            setTextColor(Apparence.couleur(this@VolActivity, R.color.muted))
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { gravity = android.view.Gravity.CENTER_HORIZONTAL }
            setOnClickListener { effacer() }
        })
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
    private fun depliant(titre: String, detail: View, couleur: Int = R.color.brand) = VueJour.texte(this, "$titre  ⌄", 14f, couleur, gras = true).apply {
        minHeight = dp(44)
        gravity = android.view.Gravity.CENTER_VERTICAL or android.view.Gravity.START
        setOnClickListener {
            val ouvert = detail.visibility != View.VISIBLE
            detail.visibility = if (ouvert) View.VISIBLE else View.GONE
            text = "$titre  ${if (ouvert) "⌃" else "⌄"}"
        }
    }

    private fun champ(etiquette: String, valeur: String, action: () -> Unit) = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(0, dp(10), 0, dp(10))
        foreground = getDrawable(android.R.drawable.list_selector_background)
        setOnClickListener { action() }
        addView(VueJour.texte(this@VolActivity, etiquette, 12f, R.color.muted))
        addView(VueJour.texte(this@VolActivity, valeur, 16f, R.color.brand, gras = true, haut = 2))
    }

    private fun nomAeroport(a: Vol.Aeroport) = "${a.iata} · ${a.ville.ifEmpty { a.nom }}"

    private fun formatSaisie() = DateTimeFormatter.ofPattern("EEE d MMM yyyy · HH:mm", locale)

    /** Une recherche dans les 3 226 aéroports : code, ville ou nom, en lettres latines ou en hébreu. */
    private fun choisirAeroport(choisi: (Vol.Aeroport) -> Unit) {
        val champ = EditText(this).apply {
            hint = getString(R.string.vol_chercher)
            isSingleLine = true
        }
        val liste = ListView(this)
        val adaptateur = ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, mutableListOf())
        liste.adapter = adaptateur
        var trouves: List<Vol.Aeroport> = emptyList()
        val vide = VueJour.texte(this, getString(R.string.vol_aucun_resultat), 14f, R.color.muted, haut = 8).apply { visibility = View.GONE }
        champ.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) = Unit
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                trouves = Aeroports.chercher(aeroports, s?.toString().orEmpty())
                adaptateur.clear()
                adaptateur.addAll(trouves.map { "${it.iata} · ${it.ville} — ${Aeroports.nomLocal(it, ChizukCard.language(this@VolActivity))} (${it.pays})" })
                vide.visibility = if (trouves.isEmpty() && !s.isNullOrBlank()) View.VISIBLE else View.GONE
            }
        })
        val vue = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(8), dp(20), 0)
            addView(champ)
            addView(vide)
            addView(liste, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(320)))
        }
        var dialogue: AlertDialog? = null
        liste.setOnItemClickListener { _, _, i, _ ->
            trouves.getOrNull(i)?.let(choisi)
            dialogue?.dismiss()
        }
        dialogue = MaterialAlertDialogBuilder(this)
            .setTitle(R.string.vol_choisir_aeroport)
            .setView(vue)
            .setNegativeButton(android.R.string.cancel, null)
            .show()
        champ.requestFocus()
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
        afficherFormulaire()
    }

    private fun message(texte: String) {
        MaterialAlertDialogBuilder(this).setMessage(texte).setPositiveButton(android.R.string.ok, null).show()
    }

    private fun afficherResultats() {
        val zone = findViewById<LinearLayout>(R.id.vol_resultats) ?: return
        zone.removeAllViews()
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
        zone.addView(cartePriere(c, maintenant))
        zone.addView(carteFiabilite(c))
        zone.addView(carteRappels(c, maintenant))
        zone.addView(carteZmanim(c))
        carteJalons(c)?.let(zone::addView)
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

    /** La prière du moment mise en avant (29/10) : fond vert pâle, nom en grand, compte à rebours en vert. */
    private fun cartePriere(c: Vol.Calcul, maintenant: Instant) = VueJour.carte(this).apply {
        val marque = Apparence.couleur(this@VolActivity, R.color.brand)
        background = android.graphics.drawable.GradientDrawable().apply {
            cornerRadius = dp(20).toFloat()
            setColor(androidx.core.graphics.ColorUtils.blendARGB(Apparence.couleur(this@VolActivity, R.color.card), marque, 0.09f))
            setStroke(dp(1), androidx.core.graphics.ColorUtils.blendARGB(Apparence.couleur(this@VolActivity, R.color.line), marque, 0.28f))
        }
        addView(VueJour.texte(this@VolActivity, getString(R.string.vol_priere_titre), 13f, R.color.muted))
        val p = VolPriere.priere(c, maintenant)
        if (p == null) {
            addView(VueJour.texte(this@VolActivity, getString(R.string.vol_hors_fenetre), 14f, R.color.ink, haut = 4))
            return@apply
        }
        val (priere, limite) = p
        val nom = when (priere) {
            "chaharit" -> R.string.widget_zmanim_chaharit
            "minha" -> R.string.widget_zmanim_minha
            else -> R.string.widget_zmanim_arvit
        }
        addView(VueJour.texte(this@VolActivity, getString(nom), 30f, R.color.brand, gras = true, haut = 4))
        if (limite == null) {
            addView(VueJour.texte(this@VolActivity, getString(R.string.vol_sans_limite), 14f, R.color.ink, haut = 2))
        } else {
            addView(VueJour.texte(this@VolActivity, getString(R.string.vol_jusqua, deuxHeures(c.plan, limite)), 14f, R.color.ink, haut = 2))
            val minutes = Duration.between(maintenant, limite).toMinutes().toInt().coerceAtLeast(0)
            addView(VueJour.texte(this@VolActivity, getString(R.string.vol_dans, dureeEnMots(minutes)), 22f, R.color.brand, gras = true, haut = 4))
            c.enVol.firstOrNull { it.instant == limite }?.ecartSiRetard?.let {
                addView(VueJour.texte(this@VolActivity, ecart(it), 12f, R.color.muted, haut = 2))
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
        carte.addView(com.google.android.material.materialswitch.MaterialSwitch(this).apply {
            text = getString(R.string.vol_direct_activer)
            isChecked = actif
            setOnCheckedChangeListener { _, v -> Store.setSuiviDirect(this@VolActivity, v); afficherResultats() }
        })
        if (!actif) return
        val champ = EditText(this).apply {
            hint = getString(R.string.vol_indicatif)
            setText(Store.volIndicatif(this@VolActivity))
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS
            isSingleLine = true
            filters = arrayOf(android.text.InputFilter.LengthFilter(9))
        }
        carte.addView(champ)
        carte.addView(MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
            text = getString(if (directEnCours) R.string.vol_direct_attente else R.string.vol_direct_bouton)
            isEnabled = !directEnCours
            setOnClickListener {
                val indicatif = SuiviVol.normaliser(champ.text.toString()) ?: return@setOnClickListener message(getString(R.string.vol_indicatif_invalide))
                Store.setVolIndicatif(this@VolActivity, indicatif)
                demanderDirect(indicatif)
            }
        })
        carte.addView(VueJour.texte(this, getString(R.string.vol_source_opensky), 11f, R.color.muted, haut = 2))
    }

    private var directEnCours = false

    private var routeEnCours = false
    private var routeTexte: String? = null

    /**
     * La route du numéro saisi (RouteVol, adsbdb.com), hors du fil de l'interface : remplit l'arrivée
     * (et le départ s'il est vide) et l'indicatif du suivi en direct ; les heures restent à saisir.
     */
    private fun chercherRoute() {
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
        addView(View(this@VolActivity).apply {
            setBackgroundColor(Apparence.couleur(this@VolActivity, R.color.line))
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1)).apply { topMargin = dp(8) }
        })
        addView(VueJour.texte(this@VolActivity, getString(R.string.vol_preciser_titre), 15f, R.color.ink, gras = true, haut = 12))
        addView(MaterialButton(this@VolActivity, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
            text = getString(if (gpsEnCours) R.string.vol_gps_attente else R.string.vol_gps)
            isEnabled = !gpsEnCours
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(8) }
            setOnClickListener { demanderGps() }
        })
        if (r != null) addView(MaterialButton(this@VolActivity, null, com.google.android.material.R.attr.borderlessButtonStyle).apply {
            text = getString(R.string.vol_gps_oublier)
            setOnClickListener { appliquerReleve(null) }
        })
        if (BuildConfig.SUIVI_VOL) ajouterSuiviDirect(this)
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
