package com.myhitbodedut

import android.animation.ValueAnimator
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlin.random.Random

/**
 * La fin d'une séance : le hassid danse, et l'application dit bravo.
 *
 * Elle s'ouvre une fois par séance close, où qu'on se trouve quand on
 * revient : sur l'écran de séance si on l'avait gardé ouvert, sur l'accueil
 * si la séance s'est achevée téléphone posé.
 *
 * Une séance annulée ouvre le même écran, sans danse : « ’Hazak », et un mot
 * de ’hizouk du moment « relance », ceux qui disent de ne jamais désespérer
 * (Likoutei Moharan II, 78 : אֵין שׁוּם יֵאוּשׁ בָּעוֹלָם כְּלָל). Demande du
 * propriétaire, 28/09.
 */
class CelebrationActivity : AppCompatActivity() {

    private var danse: ValueAnimator? = null

    private val notes = Handler(Looper.getMainLooper())

    private val annulee: Boolean by lazy { intent.getBooleanExtra(EXTRA_ANNULEE, false) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_celebration)

        val minutes = intent.getIntExtra(EXTRA_MINUTES, 0)
        findViewById<TextView>(R.id.celebration_minutes).apply {
            if (annulee) {
                visibility = View.VISIBLE
                setText(R.string.annulation_subtitle)
            } else {
                visibility = if (minutes > 0) View.VISIBLE else View.GONE
                text = resources.getQuantityString(R.plurals.celebration_minutes, minutes, minutes)
            }
        }
        val chizuk = findViewById<View>(R.id.celebration_chizuk)
        if (annulee) {
            findViewById<ImageView>(R.id.celebration_art).setImageResource(R.drawable.char_walking)
            if (ChizukCard.show(chizuk, Moment.RELANCE) == null) chizuk.visibility = View.GONE
        } else {
            chizuk.visibility = View.GONE
        }
        // Sans choix enregistré, le titre reste neutre : la question
        // צדיק / צדקת ne se pose que dans la présentation (demande du 28/09).
        val adresse = Store.adresse(this)
        findViewById<TextView>(R.id.celebration_title).setText(
            if (annulee) Adresse.hazak(adresse) else Adresse.bravo(adresse)
        )
        // Après le bravo, la note de la séance, si elle est proposée : dans
        // cet ordre, et plus l'une sous l'autre (revue du 29/09).
        val dateNote = intent.getStringExtra(EXTRA_NOTE_DATE)
        findViewById<Button>(R.id.celebration_continue).setOnClickListener {
            if (dateNote == null || annulee) {
                finish()
            } else {
                NoteDialog.show(this, dateNote, onSaved = {}, onClosed = { finish() })
            }
        }
    }

    override fun onStart() {
        super.onStart()
        danser()
    }

    override fun onStop() {
        danse?.cancel()
        danse = null
        notes.removeCallbacksAndMessages(null)
        super.onStop()
    }

    /**
     * Une danse de hassid, pas une image qui tremble : à chaque temps il
     * saute, retombe en pliant un peu les genoux, et change de jambe — les
     * deux poses superposées se relaient au moment où il touche le sol. Le
     * corps se balance d'un côté puis de l'autre sur deux temps, et des
     * notes montent de la scène.
     *
     * Rien si les animations sont coupées dans le téléphone : c'est souvent
     * un choix d'accessibilité, pas un oubli. La pose fixe reste alors.
     */
    private fun danser() {
        if (annulee || !ValueAnimator.areAnimatorsEnabled()) return
        val danseur = findViewById<View>(R.id.celebration_dancer)
        val poseA = findViewById<View>(R.id.celebration_art)
        val poseB = findViewById<View>(R.id.celebration_art_b)
        val saut = 16f * resources.displayMetrics.density

        danse = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 2 * DanseMesure.TEMPS_MS
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener {
                val t = it.animatedValue as Float
                val pas = DanseMesure.pas(t)
                danseur.pivotX = danseur.width / 2f
                danseur.pivotY = danseur.height.toFloat()
                danseur.translationY = -saut * pas.hauteur
                danseur.scaleX = 1f + 0.04f * pas.flexion
                danseur.scaleY = 1f - 0.05f * pas.flexion
                danseur.rotation = 6f * pas.balance
                poseA.alpha = 1f - pas.poseB
                poseB.alpha = pas.poseB
            }
            start()
        }
        notes.post(object : Runnable {
            override fun run() {
                lancerNote()
                notes.postDelayed(this, DanseMesure.TEMPS_MS + 180L)
            }
        })
    }

    /** Une note ou une étoile qui monte de la scène et s'efface. */
    private fun lancerNote() {
        val scene = findViewById<FrameLayout>(R.id.celebration_stage)
        if (scene.width == 0) return
        val dp = resources.displayMetrics.density
        val note = TextView(this).apply {
            text = SIGNES[Random.nextInt(SIGNES.size)]
            textSize = 18f + Random.nextInt(8)
            // La scène est une plaque claire de jour comme de nuit.
            setTextColor(Apparence.couleur(this@CelebrationActivity, R.color.on_plate))
        }
        val cote = if (Random.nextBoolean()) 0.18f else 0.72f
        scene.addView(
            note,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )
        )
        note.x = scene.width * (cote + Random.nextFloat() * 0.1f)
        note.y = scene.height * 0.55f
        note.animate()
            .translationYBy(-scene.height * 0.45f)
            .translationXBy((Random.nextFloat() - 0.5f) * 30f * dp)
            .rotation((Random.nextFloat() - 0.5f) * 40f)
            .alpha(0f)
            .setDuration(1700L)
            .setInterpolator(DecelerateInterpolator())
            .withEndAction { scene.removeView(note) }
            .start()
    }

    companion object {
        const val EXTRA_MINUTES = "minutes"
        const val EXTRA_ANNULEE = "annulee"
        const val EXTRA_NOTE_DATE = "note_date"

        private val SIGNES = arrayOf("♪", "♫", "♪", "✦")

        /** Après une séance annulée : pas de bravo, un encouragement. */
        fun annulation(activity: Activity) {
            activity.startActivity(
                Intent(activity, CelebrationActivity::class.java).putExtra(EXTRA_ANNULEE, true)
            )
        }

        /**
         * Ouvre le bravo si une séance vient de se clore. La marque est
         * retirée avant l'ouverture, pas à la fermeture : deux écrans qui
         * reprennent la main l'un après l'autre ne l'ouvriraient pas deux fois.
         */
        fun showIfPending(activity: Activity) {
            val celebration = Store.takePendingCelebration(activity) ?: return
            activity.startActivity(
                Intent(activity, CelebrationActivity::class.java)
                    .putExtra(EXTRA_MINUTES, celebration.minutes)
                    .putExtra(EXTRA_NOTE_DATE, celebration.dateNote)
            )
        }
    }
}
