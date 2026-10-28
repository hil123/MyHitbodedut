package com.myhitbodedut

import android.app.DatePickerDialog
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
 * téléphone, aucune permission n'est demandée.
 */
class VolActivity : AppCompatActivity() {

    private var aeroports: List<Vol.Aeroport> = emptyList()
    private var numero = ""
    private var depart: Vol.Aeroport? = null
    private var arrivee: Vol.Aeroport? = null
    private var decollage: LocalDateTime? = null
    private var atterrissage: LocalDateTime? = null
    private var calcul: Vol.Calcul? = null
    private var enCours = false
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

    /** Le formulaire, puis une zone de résultats que la minuterie récrit seule (le champ garde le clavier). */
    private fun afficherFormulaire() {
        contenu.removeAllViews()
        contenu.addView(VueJour.carte(this).apply {
            addView(EditText(this@VolActivity).apply {
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
            })
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
            addView(LinearLayout(this@VolActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                    .apply { topMargin = dp(12) }
                addView(MaterialButton(this@VolActivity).apply {
                    text = getString(R.string.vol_calculer)
                    layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                    setOnClickListener { calculer() }
                })
                addView(MaterialButton(this@VolActivity, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
                    text = getString(R.string.vol_effacer)
                    layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = dp(8) }
                    setOnClickListener { effacer() }
                })
            })
        })
        contenu.addView(LinearLayout(this).apply {
            id = R.id.vol_resultats
            orientation = LinearLayout.VERTICAL
        })
        contenu.addView(VueJour.pied(this))
        contenu.addView(VueJour.texte(this, getString(R.string.vol_source_aeroports), 12f, R.color.muted, haut = 6).apply {
            setPadding(dp(16), 0, dp(16), 0)
        })
        afficherResultats()
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

    /** Une recherche dans les 3 226 aéroports : code, ville ou nom. */
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
                adaptateur.addAll(trouves.map { "${it.iata} · ${it.ville} — ${it.nom} (${it.pays})" })
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
        val plan = Vol.Plan(numero, d, a, i1, i2)
        Vol.verifier(plan)?.let { return message(getString(invalide(it))) }
        Store.setVol(this, plan)
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
                if (c == null) message(getString(R.string.vol_reserve_erreur)) else afficherResultats()
            }
        }.start()
    }

    private fun effacer() {
        Store.setVol(this, null)
        numero = ""
        depart = null
        arrivee = null
        decollage = null
        atterrissage = null
        calcul = null
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
        zone.addView(carteZmanim(c))
    }

    /**
     * « mar. 10 · 18:42 TLV · 11:42 JFK » : l'heure du départ et celle de l'arrivée, le jour s'il
     * change. Seuls « heure code » sont isolés de gauche à droite : le jour reste dans le sens de la langue.
     */
    private fun deuxHeures(plan: Vol.Plan, t: Instant): String {
        val jour = DateTimeFormatter.ofPattern("EEE d", locale)
        val heure = DateTimeFormatter.ofPattern("HH:mm", locale)
        val dep = Vol.zone(plan.depart.fuseau)?.let { t.atZone(it) } ?: return t.toString()
        val arr = Vol.zone(plan.arrivee.fuseau)?.let { t.atZone(it) } ?: return t.toString()
        val autreJour = if (arr.toLocalDate() != dep.toLocalDate()) "${arr.format(jour)} " else ""
        return "${dep.format(jour)} \u2066${dep.format(heure)} ${plan.depart.iata}\u2069 · $autreJour\u2066${arr.format(heure)} ${plan.arrivee.iata}\u2069"
    }

    private fun cartePriere(c: Vol.Calcul, maintenant: Instant) = VueJour.carte(this).apply {
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
        addView(VueJour.texte(this@VolActivity, getString(nom), 24f, R.color.brand, gras = true, haut = 4))
        if (limite == null) {
            addView(VueJour.texte(this@VolActivity, getString(R.string.vol_sans_limite), 14f, R.color.ink, haut = 2))
        } else {
            addView(VueJour.texte(this@VolActivity, getString(R.string.vol_jusqua, deuxHeures(c.plan, limite)), 14f, R.color.ink, haut = 2))
            val minutes = Duration.between(maintenant, limite).toMinutes().toInt().coerceAtLeast(0)
            addView(VueJour.texte(this@VolActivity, getString(R.string.vol_dans, JaugeSemaine.dureeCourte(minutes)), 18f, R.color.ink, gras = true, haut = 4))
            c.enVol.firstOrNull { it.instant == limite }?.ecartSiRetard?.let {
                addView(VueJour.texte(this@VolActivity, ecart(it), 12f, R.color.muted, haut = 2))
            }
        }
    }

    private fun ecart(d: Duration) = getString(R.string.vol_ecart, (if (d.isNegative) "−" else "+") + abs(d.toMinutes()))

    private fun carteFiabilite(c: Vol.Calcul) = VueJour.carte(this).apply {
        addView(VueJour.texte(this@VolActivity, getString(R.string.vol_fiabilite_titre), 13f, R.color.muted))
        addView(VueJour.texte(this@VolActivity, getString(R.string.vol_fiabilite), 14f, R.color.ink, haut = 4))
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

    private fun carteZmanim(c: Vol.Calcul) = VueJour.carte(this).apply {
        addView(VueJour.texte(this@VolActivity, getString(R.string.vol_en_vol_titre), 13f, R.color.muted))
        if (c.enVol.isEmpty()) {
            addView(VueJour.texte(this@VolActivity, getString(R.string.vol_aucun_zman), 14f, R.color.ink, haut = 4))
            return@apply
        }
        c.enVol.forEach { e ->
            addView(VueJour.texte(this@VolActivity, Libelles.res("zman_${e.cle}")?.let(::getString) ?: e.cle, 15f, R.color.ink, haut = 12))
            addView(VueJour.texte(this@VolActivity, deuxHeures(c.plan, e.instant), 16f, R.color.ink, gras = true, haut = 2).apply {
                fontFeatureSettings = "tnum"
            })
            val details = listOfNotNull(position(e), e.ecartSiRetard?.let(::ecart)).joinToString(" · ")
            addView(VueJour.texte(this@VolActivity, details, 12f, R.color.muted, haut = 1))
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
        const val RAFRAICHISSEMENT_MS = 30_000L
        const val ETAT_NUMERO = "vol_numero"
        const val ETAT_DEPART = "vol_depart"
        const val ETAT_ARRIVEE = "vol_arrivee"
        const val ETAT_DECOLLAGE = "vol_decollage"
        const val ETAT_ATTERRISSAGE = "vol_atterrissage"
    }
}
