package com.myhitbodedut

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.app.Activity
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Outline
import android.graphics.drawable.GradientDrawable
import android.media.MediaMetadataRetriever
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import java.util.Locale
import java.util.concurrent.Executors

/**
 * Le mini-lecteur de la pratique (demande du 19/10) : changer de morceau sans
 * quitter la hitbodedout. Déplié, un petit lecteur (la pochette, le titre, la
 * barre de position, précédent, lecture, suivant, aléatoire) ; replié, un disque
 * qui tourne pendant la lecture, avec la photo du morceau, ou à défaut le hassid
 * à la guitare. Il ne commande rien lui-même : il envoie ses ordres au service
 * de la séance (SessionService.lecteur) et lit l'état du lecteur.
 */
class MiniLecteur(private val activite: Activity) {

    private val handler = Handler(Looper.getMainLooper())
    private val fil = Executors.newSingleThreadExecutor()
    private val d = activite.resources.displayMetrics.density
    private fun dp(v: Int) = (v * d).toInt()

    /** Les pochettes déjà lues, par morceau ; null : pas de photo dans le fichier. */
    private val pochettes = HashMap<String, Bitmap?>()
    private var morceau: String? = null
    private var deplie = false
    private var glisse = false

    private val racine = FrameLayout(activite)
    private val disque = rond(64)
    private val petitDisque = rond(48)
    private val panneau = LinearLayout(activite)
    private val titre = TextView(activite)
    private val barre = SeekBar(activite)
    private val ecoule = TextView(activite)
    private val total = TextView(activite)
    private val lecture = ImageButton(activite)
    private val aleatoire = ImageButton(activite)

    private val tours = listOf(disque, petitDisque).map { v ->
        ObjectAnimator.ofFloat(v, View.ROTATION, 0f, 360f).apply {
            duration = 12_000L
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
        }
    }

    /** Pendant que le lecteur est déplié, la barre avance plus souvent que le décompte. */
    private val suivi = object : Runnable {
        override fun run() {
            maj()
            if (deplie) handler.postDelayed(this, 250L)
        }
    }

    init {
        val mint = Apparence.couleur(activite, R.color.mint)
        racine.layoutParams = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM
        )
        racine.visibility = View.GONE

        // Replié : le disque, en bas, du côté de la fin de ligne.
        disque.layoutParams = FrameLayout.LayoutParams(dp(64), dp(64), Gravity.BOTTOM or Gravity.END).apply {
            setMargins(dp(16), dp(16), dp(16), dp(16))
        }
        disque.elevation = dp(8).toFloat()
        disque.contentDescription = activite.getString(R.string.lecteur_ouvrir)
        disque.setOnClickListener { deplier(true) }
        racine.addView(disque)

        // Déplié : une carte sombre, la largeur de l'écran moins les marges.
        panneau.orientation = LinearLayout.VERTICAL
        panneau.layoutDirection = View.LAYOUT_DIRECTION_LTR
        panneau.setPadding(dp(16), dp(12), dp(16), dp(12))
        panneau.background = GradientDrawable().apply {
            cornerRadius = dp(20).toFloat()
            setColor(0xF2121A17.toInt())
            setStroke(dp(1), 0x33FFFFFF)
        }
        panneau.elevation = dp(8).toFloat()
        panneau.layoutParams = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM
        ).apply { setMargins(dp(16), dp(16), dp(16), dp(16)) }
        panneau.isClickable = true
        panneau.visibility = View.GONE

        val tete = LinearLayout(activite).apply { gravity = Gravity.CENTER_VERTICAL }
        petitDisque.layoutParams = LinearLayout.LayoutParams(dp(48), dp(48))
        petitDisque.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        tete.addView(petitDisque)
        titre.apply {
            setTextColor(0xFFFFFFFF.toInt())
            textSize = 15f
            isSingleLine = true
            ellipsize = TextUtils.TruncateAt.MARQUEE
            marqueeRepeatLimit = -1
            isSelected = true
            textDirection = View.TEXT_DIRECTION_FIRST_STRONG
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = dp(12) }
        }
        tete.addView(titre)
        tete.addView(bouton(R.drawable.ic_lecteur_replier, R.string.lecteur_replier, 0xB3FFFFFF.toInt()) { deplier(false) })
        panneau.addView(tete)

        barre.apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(32)).apply { topMargin = dp(8) }
            progressTintList = android.content.res.ColorStateList.valueOf(mint)
            thumbTintList = android.content.res.ColorStateList.valueOf(mint)
            progressBackgroundTintList = android.content.res.ColorStateList.valueOf(0x55FFFFFF)
            contentDescription = activite.getString(R.string.lecteur_position)
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(s: SeekBar, p: Int, parUtilisateur: Boolean) {
                    if (parUtilisateur) ecoule.text = temps(p)
                }
                override fun onStartTrackingTouch(s: SeekBar) { glisse = true }
                override fun onStopTrackingTouch(s: SeekBar) {
                    glisse = false
                    SessionService.lecteur(activite, SessionService.CMD_POSITION, s.progress)
                }
            })
        }
        panneau.addView(barre)

        val temps = LinearLayout(activite)
        listOf(ecoule, total).forEach {
            it.setTextColor(0xB3FFFFFF.toInt())
            it.textSize = 12f
            it.fontFeatureSettings = "tnum"
        }
        temps.addView(ecoule, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        total.gravity = Gravity.END
        temps.addView(total, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        panneau.addView(temps)

        val commandes = LinearLayout(activite).apply {
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                .apply { topMargin = dp(4) }
        }
        aleatoire.apply {
            setImageResource(R.drawable.ic_lecteur_aleatoire)
            background = null
            contentDescription = activite.getString(R.string.lecteur_aleatoire)
            setOnClickListener {
                Store.setMusicShuffle(activite, !Store.musicShuffle(activite))
                SessionService.musicChanged(activite)
                maj()
            }
        }
        commandes.addView(aleatoire, LinearLayout.LayoutParams(dp(48), dp(48)))
        commandes.addView(espace())
        commandes.addView(bouton(R.drawable.ic_lecteur_precedent, R.string.lecteur_precedent, 0xFFFFFFFF.toInt()) {
            SessionService.lecteur(activite, SessionService.CMD_PRECEDENT); bientot()
        })
        commandes.addView(espace())
        lecture.apply {
            background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(0xFFFFFFFF.toInt()) }
            setColorFilter(0xFF121A17.toInt())
            setOnClickListener {
                val joue = SessionService.lecteur?.isPlaying == true
                SessionService.lecteur(activite, if (joue) SessionService.CMD_PAUSE else SessionService.CMD_REPRENDRE)
                bientot()
            }
        }
        commandes.addView(lecture, LinearLayout.LayoutParams(dp(64), dp(64)))
        commandes.addView(espace())
        commandes.addView(bouton(R.drawable.ic_lecteur_suivant, R.string.lecteur_suivant, 0xFFFFFFFF.toInt()) {
            SessionService.lecteur(activite, SessionService.CMD_SUIVANT); bientot()
        })
        commandes.addView(espace())
        // Le pendant de l'aléatoire, pour que la lecture reste au centre.
        commandes.addView(View(activite), LinearLayout.LayoutParams(dp(48), dp(48)))
        panneau.addView(commandes)
        racine.addView(panneau)

        activite.findViewById<ViewGroup>(android.R.id.content).addView(racine)
    }

    /** Chaque seconde (SessionActivity.render) et, déplié, quatre fois par seconde. */
    fun maj() {
        val enCours = Store.isSessionRunning(activite)
        racine.visibility = if (enCours) View.VISIBLE else View.GONE
        if (!enCours) {
            tours.forEach { it.pause() }
            return
        }
        val lecteur = SessionService.lecteur
        val joue = lecteur?.isPlaying == true
        val track = if (Store.musicEnabled(activite)) MusicLibrary.byId(activite, Store.musicNow(activite)) ?: MusicLibrary.selected(activite) else null
        titre.text = track?.title ?: activite.getString(R.string.music_off_short)
        if (track?.id != morceau || morceau == null) {
            morceau = track?.id
            pochette(track)
        }
        lecture.setImageResource(if (joue) R.drawable.ic_lecteur_pause else R.drawable.ic_lecteur_lecture)
        lecture.contentDescription = activite.getString(if (joue) R.string.lecteur_pause else R.string.lecteur_lecture)
        aleatoire.alpha = if (Store.musicShuffle(activite)) 1f else 0.45f
        aleatoire.isSelected = Store.musicShuffle(activite)
        val duree = lecteur?.duree ?: 0
        if (!glisse) {
            barre.max = duree.coerceAtLeast(1)
            barre.progress = lecteur?.position ?: 0
            ecoule.text = temps(barre.progress)
        }
        total.text = temps(duree)
        tours.forEach { a ->
            when {
                joue && !a.isStarted -> a.start()
                joue && a.isPaused -> a.resume()
                !joue && a.isRunning -> a.pause()
            }
        }
    }

    fun fermer() {
        handler.removeCallbacksAndMessages(null)
        tours.forEach { it.cancel() }
        fil.shutdownNow()
    }

    private fun deplier(oui: Boolean) {
        deplie = oui
        panneau.visibility = if (oui) View.VISIBLE else View.GONE
        disque.visibility = if (oui) View.GONE else View.VISIBLE
        handler.removeCallbacks(suivi)
        if (oui) handler.post(suivi) else maj()
        (if (oui) panneau else disque).sendAccessibilityEvent(android.view.accessibility.AccessibilityEvent.TYPE_VIEW_FOCUSED)
    }

    /** Le service change de morceau un peu après l'ordre : on relit l'état juste après. */
    private fun bientot() {
        handler.postDelayed({ maj() }, 300L)
    }

    /** La photo du morceau (pochette du fichier), sinon le hassid à la guitare. */
    private fun pochette(track: Track?) {
        val id = track?.id
        if (id == null || track.uri == null) {
            montrer(null)
            return
        }
        if (pochettes.containsKey(id)) {
            montrer(pochettes[id])
            return
        }
        montrer(null)
        if (fil.isShutdown) return
        fil.execute {
            val image = runCatching {
                MediaMetadataRetriever().run {
                    try {
                        setDataSource(activite, track.uri)
                        embeddedPicture?.let { BitmapFactory.decodeByteArray(it, 0, it.size) }
                    } finally {
                        release()
                    }
                }
            }.getOrNull()
            handler.post {
                pochettes[id] = image
                if (morceau == id) montrer(image)
            }
        }
    }

    private fun montrer(image: Bitmap?) {
        listOf(disque, petitDisque).forEach { v ->
            if (image != null) {
                v.setImageBitmap(image)
                v.scaleType = ImageView.ScaleType.CENTER_CROP
                v.setPadding(0, 0, 0, 0)
            } else {
                v.setImageResource(R.drawable.char_guitar)
                v.scaleType = ImageView.ScaleType.FIT_CENTER
                v.setPadding(dp(4), dp(4), dp(4), dp(2))
            }
        }
    }

    private fun rond(taille: Int) = ImageView(activite).apply {
        background = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(Apparence.couleur(activite, R.color.stage_soft))
            setStroke(dp(2), Apparence.couleur(activite, R.color.mint))
        }
        outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(v: View, o: Outline) = o.setOval(0, 0, v.width, v.height)
        }
        clipToOutline = true
        minimumWidth = dp(taille)
        minimumHeight = dp(taille)
    }

    private fun bouton(icone: Int, libelle: Int, teinte: Int, action: () -> Unit) = ImageButton(activite).apply {
        setImageResource(icone)
        setColorFilter(teinte)
        background = null
        contentDescription = activite.getString(libelle)
        layoutParams = LinearLayout.LayoutParams(dp(48), dp(48))
        setOnClickListener { action() }
    }

    private fun espace() = View(activite).apply { layoutParams = LinearLayout.LayoutParams(0, 1, 1f) }

    private fun temps(ms: Int): String {
        val s = (ms / 1000).coerceAtLeast(0)
        return String.format(Locale.US, "%d:%02d", s / 60, s % 60)
    }
}
