package com.myhitbodedut

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.materialswitch.MaterialSwitch
import java.time.format.DateTimeFormatter

/** Écran principal : état du jour, diagnostic de la barrière, réglages. */
class MainActivity : AppCompatActivity() {

    /** Le texte de relance a déjà été tiré pour la rupture en cours. */
    private var relanceTiree = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Notifications.ensureChannels(this)
        // Recopie la langue choisie pour les services réveillés seuls (LangueApp).
        LangueApp.retenir(this)
        // Une rotation ou un redimensionnement de fenêtre recrée l'écran :
        // sans cela, le texte de relance serait retiré à chaque fois.
        relanceTiree = savedInstanceState?.getBoolean(ETAT_RELANCE, false) ?: false
        // Une seule fois par création réelle : une recréation (fenêtre
        // redimensionnée) pendant la présentation en ouvrait une seconde.
        if (savedInstanceState == null) showOnboardingOnce()
        wireActions()
        wireTabs(savedInstanceState?.getInt(ETAT_ONGLET, R.id.nav_today) ?: R.id.nav_today)
        // Venu de la notification de fin : la note du jour de la séance, pas
        // du jour où l'on touche la notification. Si le bravo attend encore,
        // c'est lui qui la proposera, après. Lue une seule fois : une
        // rotation ne la rouvre pas.
        if (savedInstanceState == null) {
            intent?.getStringExtra(EXTRA_ASK_NOTE)?.let { date ->
                if (!Store.hasPendingCelebration(this)) NoteDialog.show(this, date, onSaved = { refresh() })
            }
            intent?.removeExtra(EXTRA_ASK_NOTE)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(ETAT_RELANCE, relanceTiree)
        outState.putInt(ETAT_ONGLET, findViewById<BottomNavigationView>(R.id.bottom_nav).selectedItemId)
    }

    // --- Onglets ------------------------------------------------------------

    /** Revenir d'« Textes » ramène à « Aujourd'hui » avant de quitter l'application. */
    private val retourAujourdhui = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            findViewById<BottomNavigationView>(R.id.bottom_nav).selectedItemId = R.id.nav_today
        }
    }

    /**
     * Aujourd'hui et Textes changent de panneau sur place. Journal et
     * Réglages ouvrent leurs écrans sans devenir l'onglet actif : au retour,
     * on retrouve le panneau qu'on avait quitté.
     */
    private fun wireTabs(onglet: Int) {
        onBackPressedDispatcher.addCallback(this, retourAujourdhui)
        val nav = findViewById<BottomNavigationView>(R.id.bottom_nav)
        nav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_today, R.id.nav_texts -> {
                    montrerPanneau(item.itemId)
                    // Revenir sur l'accueil rejoue les anneaux, comme à l'ouverture.
                    if (item.itemId == R.id.nav_today) {
                        findViewById<AnneauxView>(R.id.week_gauge).rejouer()
                    }
                    true
                }
                R.id.nav_zmanim -> {
                    startActivity(Intent(this, ZmanimActivity::class.java))
                    false
                }
                R.id.nav_journal -> {
                    startActivity(Intent(this, JournalActivity::class.java))
                    false
                }
                R.id.nav_settings -> {
                    startActivity(Intent(this, SettingsActivity::class.java))
                    false
                }
                else -> false
            }
        }
        nav.selectedItemId = onglet
        montrerPanneau(onglet)
    }

    private fun montrerPanneau(onglet: Int) {
        val textes = onglet == R.id.nav_texts
        findViewById<View>(R.id.main_scroll).visibility = if (textes) View.GONE else View.VISIBLE
        findViewById<View>(R.id.texts_scroll).visibility = if (textes) View.VISIBLE else View.GONE
        retourAujourdhui.isEnabled = textes
    }

    override fun onResume() {
        super.onResume()
        // Une séance restée inscrite sans service (application tuée) est
        // close ou relancée avant d'afficher quoi que ce soit.
        SessionService.reconcilier(this)
        applyServiceState()
        refresh()
        renderPaliers()
        // Chaque retour sur l'accueil : le hassid au centre, les anneaux se
        // remplissent, puis il salue ou fête l'anneau qui vient de se fermer (04/10).
        findViewById<View>(R.id.hero_plate).apply {
            animate().cancel()
            rotationY = 0f
        }
        montrerFace(Plaque.Face.HASSID)
        findViewById<AnneauxView>(R.id.week_gauge).rejouer()
        // La séance s'est close téléphone posé : le bravo attendait ici.
        CelebrationActivity.showIfPending(this)
        // La vignette d'accueil suit ce que l'écran vient de montrer.
        StreakWidget.refresh(this)
    }

    /**
     * La présentation, une fois par version d'elle-même, installation
     * existante comprise : c'est là seulement qu'on choisit צדיק / צדקת.
     * Elle se passe d'un geste, et on la revoit depuis les réglages.
     */
    private fun showOnboardingOnce() {
        if (Store.hasOnboarded(this)) return
        startActivity(Intent(this, OnboardingActivity::class.java))
    }

    // --- Actions ------------------------------------------------------------

    private fun wireActions() {
        // Un seul geste : les puces au-dessus portent déjà la durée, le sujet
        // et la musique. L'écran de séance redemandait la durée, et il
        // fallait toucher « Commencer » une seconde fois (revue du 29/09).
        findViewById<Button>(R.id.start_session).setOnClickListener {
            startActivity(
                Intent(this, SessionActivity::class.java)
                    .putExtra(SessionActivity.EXTRA_START_NOW, !Store.isSessionRunning(this))
            )
        }
        findViewById<Button>(R.id.today_offline).setOnClickListener { declarerHorsTelephone() }
        findViewById<Button>(R.id.trophees_paliers).setOnClickListener { startActivity(Intent(this, PaliersActivity::class.java)) }
        findViewById<View>(R.id.start_duration).setOnClickListener {
            pickDuration()
        }
        findViewById<View>(R.id.start_theme).setOnClickListener {
            pickTheme()
        }
        findViewById<View>(R.id.start_music).setOnClickListener {
            startActivity(Intent(this, MusicActivity::class.java))
        }
        findViewById<View>(R.id.start_focus).setOnClickListener {
            // La barrière du matin. En édition légère elle n'existe pas,
            // et la ligne est retirée plutôt que rendue inopérante.
            Store.setEnabled(this, !Store.isEnabled(this))
            applyServiceState()
            refresh()
        }
        findViewById<View>(R.id.music_button).setOnClickListener {
            startActivity(Intent(this, MusicActivity::class.java))
        }
        findViewById<View>(R.id.tips_button).setOnClickListener {
            startActivity(Intent(this, TipsActivity::class.java))
        }
        findViewById<View>(R.id.reminders_button).setOnClickListener {
            startActivity(Intent(this, RemindersActivity::class.java))
        }
        findViewById<View>(R.id.tikkun_button).setOnClickListener {
            startActivity(Intent(this, TikkunActivity::class.java))
        }
        findViewById<View>(R.id.siddur_button).setOnClickListener {
            startActivity(Intent(this, SiddurActivity::class.java))
        }
        findViewById<View>(R.id.tehilim_button).setOnClickListener {
            startActivity(Intent(this, TehilimActivity::class.java))
        }
        findViewById<View>(R.id.tefilot_button).setOnClickListener {
            startActivity(Intent(this, TefilotActivity::class.java))
        }
        findViewById<View>(R.id.library_button).setOnClickListener {
            startActivity(Intent(this, LibraryActivity::class.java))
        }

        // L'écran de relecture n'existe que pour lever les relu=false des
        // données : il n'a rien à faire dans une version publiée.
        findViewById<View>(R.id.proofread_button).apply {
            visibility = if (BuildConfig.DEBUG) View.VISIBLE else View.GONE
            setOnClickListener {
                startActivity(Intent(this@MainActivity, ProofreadActivity::class.java))
            }
        }
        // La plaque ronde coupe l'illustration à son bord, au creux des anneaux.
        findViewById<View>(R.id.hero_plate).apply {
            clipToOutline = true
            setOnClickListener { toucherPlaque() }
        }
        findViewById<Button>(R.id.today_note).setOnClickListener {
            NoteDialog.show(this, Store.today(), onSaved = { refresh() })
        }
    }

    /**
     * Une hitbodedout faite sans le téléphone : aujourd'hui, ou hier si hier
     * n'est pas encore fait, puis sa durée. Elle est inscrite comme les
     * autres, et le bravo suit si c'est aujourd'hui.
     */
    private fun declarerHorsTelephone() {
        // Les deux dates sont figées au geste : la fenêtre peut rester
        // ouverte à cheval sur minuit.
        val aujourdhui = Store.today()
        val hier = Store.parse(aujourdhui).minusDays(1).toString()
        val hierOuvert = hierOuvert()
        val aujourdhuiOuvert = !Store.isDone(this, aujourdhui)
        when {
            aujourdhuiOuvert && hierOuvert -> AlertDialog.Builder(this)
                .setTitle(R.string.offline_which_day)
                .setItems(arrayOf(getString(R.string.offline_today), getString(R.string.offline_yesterday))) { _, i ->
                    choisirDureeHorsTelephone(if (i == 0) aujourdhui else hier)
                }
                .show()
            hierOuvert -> choisirDureeHorsTelephone(hier)
            else -> choisirDureeHorsTelephone(aujourdhui)
        }
    }

    /** Hier n'est ni fait ni exempté : on peut encore l'inscrire. */
    private fun hierOuvert(): Boolean {
        val hier = Store.parse(Store.today()).minusDays(1)
        return !Store.isDone(this, hier.toString()) && !Store.isExempt(this, hier)
    }

    private fun choisirDureeHorsTelephone(date: String) {
        DureeSeance.choisir(this, R.string.offline_how_long) { minutes ->
            // Sans sujet : celui des puces est celui de la PROCHAINE séance.
            Store.addSession(this, minutes, theme = null, date = date)
            Celebrations.notification(this)
            if (date == Store.today()) Store.setPendingCelebration(this, minutes, date)
            refresh()
            StreakWidget.refresh(this)
            CelebrationActivity.showIfPending(this)
        }
    }

    /**
     * La durée de la prochaine séance. Le même choix que dans les
     * réglages : c'est la même valeur, atteinte de plus près.
     */
    private fun pickDuration() {
        DureeSeance.choisir(this, R.string.settings_target) {
            Store.setTargetMinutes(this, it)
            refresh()
        }
    }

    /** Le sujet de la prochaine séance. « Aucun » en tête : il est facultatif. */
    private fun pickTheme() {
        val sujets = Theme.values()
        val libelles = (listOf(getString(R.string.session_theme_none_short)) +
            sujets.map { getString(it.titre) }).toTypedArray()
        AlertDialog.Builder(this)
            .setTitle(R.string.session_theme_title)
            .setItems(libelles) { _, index ->
                Store.setSessionTheme(this, if (index == 0) null else sujets[index - 1])
                refresh()
            }
            .show()
    }

    // --- Rendu --------------------------------------------------------------

    private fun refresh() {
        renderToday()
        renderWeek()
        renderGauge()
        renderSwitches()
        renderMusic()
        renderRelance()
        renderTikkun()
        renderTehilim()
        renderReminders()
        renderSettingsEntry()
        renderStartCard()
    }

    /**
     * La carte de départ : ce avec quoi on entre dans la séance. Rien n'y
     * est obligatoire — la durée a sa valeur habituelle, le sujet peut
     * rester « aucun », la musique et la concentration peuvent être
     * éteintes.
     */
    private fun renderStartCard() {
        findViewById<TextView>(R.id.start_duration_value).text =
            Store.formatDuration(this, Store.targetMinutes(this))

        val sujet = Store.sessionTheme(this)
        findViewById<TextView>(R.id.start_theme_value).setText(
            sujet?.titre ?: R.string.session_theme_none_short
        )

        val track = if (Store.musicEnabled(this)) MusicLibrary.selected(this) else null
        findViewById<TextView>(R.id.start_music_value).text = when {
            track == null -> getString(R.string.start_music_off)
            Store.musicShuffle(this) -> getString(R.string.music_shuffle_short)
            else -> track.title
        }

        val focus = findViewById<View>(R.id.start_focus)
        focus.visibility = if (BuildConfig.HAS_GATE) View.VISIBLE else View.GONE
        if (BuildConfig.HAS_GATE) {
            findViewById<TextView>(R.id.start_focus_value).setText(
                if (Store.isEnabled(this)) R.string.start_focus_on else R.string.start_focus_off
            )
        }
    }

    /**
     * « שלום Avraham » : l'accueil se tait s'il n'a personne à nommer, et
     * l'état du jour prend alors la place du titre.
     */
    private fun renderGreeting() {
        val prenom = Store.firstName(this)
        findViewById<TextView>(R.id.hero_greeting).apply {
            visibility = if (prenom.isBlank()) View.GONE else View.VISIBLE
            // Bonjour le matin, bonsoir le soir (Salutation).
            if (prenom.isNotBlank()) {
                text = getString(Salutation.chaine(java.time.LocalTime.now().hour), prenom)
            }
        }
        findViewById<TextView>(R.id.status_title).apply {
            val titre = prenom.isBlank()
            textSize = if (titre) 24f else 17f
            // Titre : la police système en gras ; ligne d'état : Varela Round, comme sous le titre.
            typeface = if (titre) Typeface.DEFAULT_BOLD else androidx.core.content.res.ResourcesCompat.getFont(this@MainActivity, R.font.varela_round)
            setTextColor(Apparence.couleur(this@MainActivity, if (titre) R.color.ink else R.color.muted))
        }
    }

    /**
     * Les trois anneaux autour du hassid : le jour (minutes sur l'objectif),
     * la semaine (jours faits sur jours dus), la série (jours d'affilée sur
     * le prochain palier). Sous eux, la semaine en toutes lettres, ou le
     * détail de l'anneau qu'on a touché.
     */
    private fun renderGauge() {
        val semaine = JaugeSemaine.progression(Store.currentWeek(this))
        val etat = Store.anneaux(this)
        val anneaux = findViewById<AnneauxView>(R.id.week_gauge)
        anneaux.montrer(etat)
        anneaux.contentDescription = getString(
            R.string.rings_description,
            Store.formatDuration(this, etat.jour.fait), Store.formatDuration(this, etat.jour.objectif),
            semaine.faits, semaine.dus, etat.serie.fait, etat.serie.objectif
        )
        anneaux.surChoix = { quel ->
            renderLegende(quel)
            // Toucher un anneau montre sa face au centre ; le relâcher ramène le hassid.
            if (quel != face.anneau) tournerPlaque(if (quel == null) Plaque.Face.HASSID else Plaque.pour(quel))
        }
        anneaux.surRempli = { apresRemplissage() }
        renderLegende(anneaux.misEnAvant)
    }

    /**
     * La fin d'un remplissage. Un anneau qui vient de se fermer est fêté
     * (les anneaux battent, le hassid saute trois fois), une seule fois par
     * jour, par semaine ou par palier ; sinon le hassid salue d'un saut.
     */
    private fun apresRemplissage() {
        val etat = Store.anneaux(this)
        val aujourdhui = java.time.LocalDate.now()
        val nouveaux = Anneaux.aFeter(etat, aujourdhui, Store.anneauxFetes(this))
        Store.setAnneauxFetes(this, Anneaux.marques(etat, aujourdhui))
        if (nouveaux.isEmpty()) {
            if (face == Plaque.Face.HASSID) sautiller(1)
        } else {
            // L'anneau fermé passe devant, et sa face s'affiche au centre le
            // temps de la fête ; le hassid revient ensuite en sautant.
            findViewById<AnneauxView>(R.id.week_gauge).apply {
                performHapticFeedback(
                    if (android.os.Build.VERSION.SDK_INT >= 30) HapticFeedbackConstants.CONFIRM
                    else HapticFeedbackConstants.LONG_PRESS
                )
                feter(nouveaux.first())
            }
        }
    }

    // --- Le centre des anneaux (Plaque) ---------------------------------------

    /** La face montrée au centre des anneaux. */
    private var face = Plaque.Face.HASSID

    private val retourHassid = Runnable { tournerPlaque(Plaque.Face.HASSID) }

    /** Un toucher au centre : la face suivante, en tournant, avec un petit retour au doigt. */
    private fun toucherPlaque() {
        findViewById<View>(R.id.hero_plate).performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
        tournerPlaque(Plaque.suivante(face))
    }

    /**
     * La plaque tourne sur elle-même comme une pièce : à mi-tour, la face
     * change ; le chiffre monte ensuite depuis zéro. Sans animations dans le
     * téléphone, la face change d'un coup.
     */
    private fun tournerPlaque(vers: Plaque.Face) {
        val plaque = findViewById<View>(R.id.hero_plate)
        plaque.removeCallbacks(retourHassid)
        if (vers != Plaque.Face.HASSID) plaque.postDelayed(retourHassid, Plaque.RETOUR_MS)
        if (vers == face) return
        plaque.animate().cancel()
        if (!android.animation.ValueAnimator.areAnimatorsEnabled()) {
            plaque.rotationY = 0f
            montrerFace(vers)
            return
        }
        plaque.cameraDistance = 9000f * resources.displayMetrics.density
        plaque.animate().rotationY(90f).setDuration(140L)
            .setInterpolator(android.view.animation.AccelerateInterpolator())
            .withEndAction {
                montrerFace(vers)
                plaque.rotationY = -90f
                plaque.animate().rotationY(0f).setDuration(200L)
                    .setInterpolator(android.view.animation.DecelerateInterpolator())
                    .withEndAction { if (vers == Plaque.Face.HASSID) sautiller(1) }
                    .start()
            }
            .start()
    }

    /** Remplit la plaque avec [vers], et met en avant l'anneau dont elle parle. */
    private fun montrerFace(vers: Plaque.Face) {
        face = vers
        val hassid = findViewById<View>(R.id.hero_character)
        val chiffres = findViewById<View>(R.id.hero_face)
        val plaque = findViewById<View>(R.id.hero_plate)
        if (vers == Plaque.Face.HASSID) {
            plaque.removeCallbacks(retourHassid)
            hassid.visibility = View.VISIBLE
            chiffres.visibility = View.GONE
            plaque.contentDescription = getString(R.string.rings_character_hint)
        } else {
            hassid.visibility = View.INVISIBLE
            chiffres.visibility = View.VISIBLE
            remplirFace(vers)
        }
        findViewById<AnneauxView>(R.id.week_gauge).mettreEnAvant(vers.anneau)
    }

    /** Le chiffre de la face et ce qu'il compte ; le chiffre monte depuis zéro. */
    private fun remplirFace(vers: Plaque.Face) {
        val etat = Store.anneaux(this)
        val valeur = findViewById<TextView>(R.id.hero_face_value)
        val legende = findViewById<TextView>(R.id.hero_face_caption)
        val duree: (Int) -> String = { Store.formatDuration(this, it) }
        val (cible, texte, legendeTexte) = when (vers) {
            Plaque.Face.JOUR -> Triple(etat.jour.fait, duree,
                getString(R.string.plaque_jour, duree(etat.jour.objectif)))
            Plaque.Face.SEMAINE -> Triple(etat.semaine.fait,
                { n: Int -> getString(R.string.plaque_semaine_valeur, n, etat.semaine.objectif) },
                getString(R.string.plaque_semaine))
            Plaque.Face.SERIE -> Triple(etat.serie.fait, { n: Int -> n.toString() },
                resources.getQuantityString(R.plurals.plaque_serie, etat.serie.fait, etat.serie.fait, etat.serie.objectif))
            Plaque.Face.MEILLEURE -> Triple(Store.meilleureSerie(this), { n: Int -> n.toString() },
                getString(R.string.plaque_meilleure))
            Plaque.Face.TOTAL -> Triple(Store.totalMinutes(this), duree, getString(R.string.plaque_total))
            Plaque.Face.JOURS -> {
                val jours = Store.joursFaits(this).size
                Triple(jours, { n: Int -> n.toString() },
                    resources.getQuantityString(R.plurals.plaque_jours, jours, jours))
            }
            Plaque.Face.MOYENNE -> Triple(
                Plaque.moyenne(Store.totalMinutes(this), Store.joursFaits(this).size), duree,
                getString(R.string.plaque_moyenne))
            Plaque.Face.HASSID -> return
        }
        legende.text = legendeTexte
        valeur.setTextColor(Apparence.couleur(this, when (vers.anneau) {
            Anneaux.Quel.JOUR -> R.color.brand
            Anneaux.Quel.SEMAINE -> R.color.ring_week
            Anneaux.Quel.SERIE -> R.color.ring_streak
            null -> R.color.ink
        }))
        findViewById<View>(R.id.hero_plate).contentDescription = listOf(texte(cible), legendeTexte).joinToString(" ")
        compteurFace?.cancel()
        if (cible <= 0 || !android.animation.ValueAnimator.areAnimatorsEnabled()) {
            valeur.text = texte(cible)
            return
        }
        valeur.text = texte(0)
        compteurFace = android.animation.ValueAnimator.ofInt(0, cible).apply {
            duration = 650L
            interpolator = android.view.animation.DecelerateInterpolator()
            addUpdateListener { valeur.text = texte(it.animatedValue as Int) }
            start()
        }
    }

    /**
     * Le palier le plus proche, sous les anneaux (11/10), et la fête d'un
     * palier atteint depuis la dernière visite : une fois par palier (et par
     * jour, semaine ou mois pour les paliers de durée).
     */
    private fun renderPaliers() {
        val bilan = Store.bilan(this)
        val etats = Paliers.etats(bilan)
        findViewById<TextView>(R.id.paliers_line).apply {
            text = PaliersVue.aLaUne(this@MainActivity, Paliers.aLaUne(etats)) + "  ›"
            setOnClickListener { startActivity(Intent(this@MainActivity, PaliersActivity::class.java)) }
        }
        // Un palier ou un trophée neuf : les confettis (11/10).
        Celebrations.ecran(this)
    }

    /** Le compteur qui fait monter le chiffre de la face ; arrêté quand la face change. */
    private var compteurFace: android.animation.ValueAnimator? = null

    /** La légende sous les anneaux : la semaine, ou l'anneau mis en avant. */
    private fun renderLegende(quel: Anneaux.Quel?) {
        val etat = Store.anneaux(this)
        val semaine = etat.semaine
        val legende = findViewById<TextView>(R.id.week_gauge_label)
        legende.text = when (quel) {
            Anneaux.Quel.JOUR -> getString(
                if (etat.jour.atteint) R.string.ring_day_reached else R.string.ring_day_detail,
                Store.formatDuration(this, etat.jour.fait), Store.formatDuration(this, etat.jour.objectif)
            )
            Anneaux.Quel.SERIE -> getString(
                if (etat.serie.atteint) R.string.ring_streak_reached else R.string.ring_streak_detail,
                etat.serie.fait, etat.serie.objectif
            )
            else -> resources.getQuantityString(R.plurals.week_gauge, semaine.fait, semaine.fait, semaine.objectif)
        }
        legende.setTextColor(
            Apparence.couleur(
                this,
                when (quel) {
                    Anneaux.Quel.JOUR -> R.color.brand
                    Anneaux.Quel.SEMAINE -> R.color.ring_week
                    Anneaux.Quel.SERIE -> R.color.ring_streak
                    null -> R.color.muted
                }
            )
        )
    }

    /**
     * Le petit saut du hassid, [fois] de suite. Rien si les animations sont
     * coupées dans le téléphone (Options pour les développeurs ou
     * Accessibilité → Supprimer les animations).
     */
    private fun sautiller(fois: Int) {
        if (fois <= 0 || !android.animation.ValueAnimator.areAnimatorsEnabled()) return
        val hassid = findViewById<ImageView>(R.id.hero_character)
        val saut = 10f * resources.displayMetrics.density
        hassid.animate().cancel()
        hassid.pivotX = hassid.width / 2f
        hassid.pivotY = hassid.height.toFloat()
        hassid.animate()
            .translationY(-saut).scaleX(1.04f).scaleY(1.06f).rotation(-3f)
            .setDuration(220L)
            .setInterpolator(android.view.animation.DecelerateInterpolator())
            .withEndAction {
                hassid.animate()
                    .translationY(0f).scaleX(1f).scaleY(1f).rotation(3f)
                    .setDuration(260L)
                    .setInterpolator(android.view.animation.BounceInterpolator())
                    .withEndAction {
                        hassid.animate().rotation(0f).setDuration(160L)
                            .withEndAction { sautiller(fois - 1) }
                            .start()
                    }
                    .start()
            }
            .start()
    }

    /**
     * L'onglet « Réglages » ne porte plus de pastille (demande du 10/10) :
     * les autorisations manquantes se lisent dans les Réglages eux-mêmes, et
     * le chiffre rouge sur l'accueil était de trop. removeBadge reste, pour
     * effacer celle qu'une version précédente aurait posée pendant que
     * l'application tournait.
     */
    private fun renderSettingsEntry() {
        findViewById<BottomNavigationView>(R.id.bottom_nav).removeBadge(R.id.nav_settings)
    }

    /** L'état de la musique, sur la carte qui lui est consacrée. */
    private fun renderMusic() {
        val track = MusicLibrary.selected(this)
        findViewById<TextView>(R.id.music_state).text = when {
            !Store.musicEnabled(this) -> getString(R.string.music_off_summary)
            track == null -> getString(R.string.music_none_summary)
            Store.musicShuffle(this) -> getString(R.string.music_shuffle_summary, Store.musicVolume(this))
            else -> getString(R.string.music_on_summary, track.title, Store.musicVolume(this))
        }
    }

    /** L'état des rappels, lisible sans ouvrir leur écran. */
    private fun renderReminders() {
        val heures = Store.reminderTimes(this)
        findViewById<TextView>(R.id.reminders_state).text = when {
            !Store.reminderEnabled(this) -> getString(R.string.reminders_off)
            heures.isEmpty() -> getString(R.string.reminders_none)
            else -> resources.getQuantityString(
                R.plurals.reminders_count, heures.size, heures.size
            )
        }
    }

    /** Ou en est le Tikoun du jour, sans avoir a ouvrir la section. */
    /** La ligne Tehilim dit la portion du jour, selon le mois hébraïque. */
    private fun renderTehilim() {
        val (jour, longueur) = CycleTehilim.jourHebreu(java.time.LocalDate.now())
        findViewById<TextView>(R.id.tehilim_state).text =
            getString(R.string.tehilim_today, TehilimTexte.portion(this, CycleTehilim.duMois(jour, longueur)))
    }

    private fun renderTikkun() {
        val tikkun = Data.tikkun(this)
        val dits = Store.tikkunDone(this).size
        val total = tikkun.psaumes.size
        findViewById<TextView>(R.id.tikkun_state).text = when (dits) {
            0 -> getString(R.string.tikkun_progress_none, total, tikkun.versets)
            total -> getString(R.string.tikkun_progress_all)
            else -> getString(R.string.tikkun_progress, dits, total)
        }
    }

    /**
     * Le moment « relance » : un mot après une rupture de série.
     *
     * La carte n'apparaît qu'à quelqu'un qui a déjà pratiqué et dont la série
     * est retombée à zéro. Elle n'a rien à dire à un premier jour, ni à une
     * série en cours.
     */
    private fun renderRelance() {
        val card = findViewById<View>(R.id.relance_chizuk)
        val brisee = Store.streak(this) == 0 && Store.aDejaPratique(this)
        if (!brisee) {
            card.visibility = View.GONE
            relanceTiree = false
            return
        }
        // refresh() repasse à chaque retour d'écran et après chaque permission
        // accordée : on ne retire pas un texte que l'utilisateur est en train
        // de lire, seulement au premier affichage de la rupture.
        if (!relanceTiree) {
            ChizukCard.show(card, Moment.RELANCE)
            relanceTiree = true
        }
    }

    private fun renderToday() {
        renderGreeting()
        val record = Store.record(this)
        // Une note seule ne fait pas la journée.
        val done = record?.faite == true
        findViewById<ImageView>(R.id.hero_character).setImageResource(Character.greeting(done))
        // L'état dit ce qui est vrai maintenant : une hitbodedout en cours
        // n'est pas « pas encore faite », et un jour de Shabbat, de fête ou de
        // pause n'attend rien (revue du 30/09, sur le modèle de la salutation).
        findViewById<TextView>(R.id.status_title).setText(
            when {
                Store.isSessionRunning(this) -> R.string.status_running
                done -> R.string.status_done
                Store.isExempt(this, java.time.LocalDate.now()) -> R.string.status_rest
                else -> R.string.status_pending
            }
        )

        val streak = Store.streak(this)
        // « Série à relancer » ne se dit qu'à qui en a déjà eu une : le
        // premier jour, ce serait accueillir quelqu'un par un échec.
        findViewById<TextView>(R.id.status_streak).text = when {
            streak > 0 -> resources.getQuantityString(R.plurals.status_streak, streak, streak)
            Store.aDejaPratique(this) -> getString(R.string.status_streak_none)
            else -> getString(R.string.status_streak_first)
        }

        val total = Store.totalMinutes(this)
        // Le premier jour, « 0 min au total » n'apprend rien : la phrase
        // d'accueil reste seule sur la ligne.
        val avecTotal = total > 0
        findViewById<View>(R.id.status_separator).visibility = if (avecTotal) View.VISIBLE else View.GONE
        findViewById<TextView>(R.id.status_total).apply {
            visibility = if (avecTotal) View.VISIBLE else View.GONE
            text = getString(R.string.status_total, Store.formatDuration(this@MainActivity, total))
        }

        val minutesToday = record?.minutes ?: 0
        val detail = findViewById<TextView>(R.id.today_detail)
        detail.visibility = if (done) View.VISIBLE else View.GONE
        if (done) {
            detail.text = if (minutesToday > 0) {
                getString(R.string.today_minutes, Store.formatDuration(this, minutesToday))
            } else {
                getString(R.string.today_declared)
            }
        }
        findViewById<Button>(R.id.today_note).visibility = if (done) View.VISIBLE else View.GONE
        // Tant qu'aujourd'hui ou hier peut encore être inscrit.
        findViewById<Button>(R.id.today_offline).visibility =
            if ((!done || hierOuvert()) && !Store.isSessionRunning(this)) View.VISIBLE else View.GONE

        findViewById<Button>(R.id.start_session).text = when {
            Store.isSessionRunning(this) -> getString(R.string.action_back_to_session)
            done -> getString(R.string.action_another_session)
            else -> getString(
                R.string.action_start_session,
                Store.formatDuration(this, Store.targetMinutes(this))
            )
        }
    }

    private fun renderWeek() {
        val strip = findViewById<LinearLayout>(R.id.week_strip)
        strip.removeAllViews()
        // Les jours dans la langue de l'application, pas celle du téléphone.
        val formatter = DateTimeFormatter.ofPattern("EEE", resources.configuration.locales[0])
        Store.currentWeek(this).forEach { day ->
            val cell = layoutInflater.inflate(R.layout.item_day, strip, false)
            val label = cell.findViewById<TextView>(R.id.day_label)
            val mark = cell.findViewById<TextView>(R.id.day_mark)
            label.text = day.date.format(formatter)

            // Le samedi chômé ne porte pas de point à cocher : il porte sa marque.
            val restDay = day.shabbat && day.exempt
            // Le numéro du jour remplit les cases encore ouvertes : la bande
            // se lit comme un calendrier, pas comme des cases vides.
            mark.text = when {
                day.done -> "✓"
                restDay -> getString(R.string.week_shabbat_mark)
                day.exempt -> "—"
                else -> day.date.dayOfMonth.toString()
            }
            cell.contentDescription = when {
                day.done -> getString(R.string.week_done)
                restDay -> getString(R.string.settings_shabbat)
                day.exempt -> getString(R.string.gate_exempt_title)
                else -> getString(R.string.week_todo)
            }

            // La case du jour fait est vert foncé : un texte sombre y disparaît.
            label.setTextColor(Apparence.couleur(this, if (day.done) R.color.white else R.color.muted))
            mark.setTextColor(
                Apparence.couleur(
                    this,
                    when {
                        day.done -> R.color.white
                        restDay -> R.color.brand
                        day.future -> R.color.muted
                        else -> R.color.ink
                    }
                )
            )
            cell.setBackgroundResource(
                when {
                    day.done -> R.drawable.bg_day_done
                    day.exempt -> R.drawable.bg_day_exempt
                    day.date == java.time.LocalDate.now() -> R.drawable.bg_day_today
                    else -> R.drawable.bg_day_todo
                }
            )
            strip.addView(cell)
        }
    }

    /** Le seul interrupteur restant ici : la musique, sur sa propre carte. */
    private fun renderSwitches() {
        bindSwitch(R.id.music_switch_main, Store.musicEnabled(this)) { checked ->
            Store.setMusicEnabled(this, checked)
            SessionService.musicChanged(this)
            refresh()
        }
    }

    /** Pose la valeur sans déclencher l'écouteur, puis le réarme. */
    private fun bindSwitch(id: Int, checked: Boolean, onChange: (Boolean) -> Unit) {
        val view = findViewById<MaterialSwitch>(id)
        view.setOnCheckedChangeListener(null)
        view.isChecked = checked
        view.setOnCheckedChangeListener { _, value -> onChange(value) }
    }

    // --- Service, permissions, sauvegarde -----------------------------------

    private fun applyServiceState() {
        if (BuildConfig.HAS_GATE) {
            if (GateStatus.shouldRunService(this)) GateService.start(this) else GateService.stop(this)
        }
        Reminder.reschedule(this)
    }

    // --- Sélecteurs ---------------------------------------------------------

    companion object {
        /** Le jour de la séance dont la note est proposée (AAAA-MM-JJ). */
        const val EXTRA_ASK_NOTE = "ask_note"

        /** Survit à une rotation ou au redimensionnement d'une fenêtre. */
        private const val ETAT_RELANCE = "relance_tiree"

        /** L'onglet affiché, pour qu'une rotation ne ramène pas à « Aujourd'hui ». */
        private const val ETAT_ONGLET = "onglet"
    }
}
