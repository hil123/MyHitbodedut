package com.myhitbodedut

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

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

    private var danse: AnimatorSet? = null

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
        findViewById<Button>(R.id.celebration_tsadik).setOnClickListener { choisir(Adresse.HOMME) }
        findViewById<Button>(R.id.celebration_tsadeket).setOnClickListener { choisir(Adresse.FEMME) }
        findViewById<Button>(R.id.celebration_continue).setOnClickListener { finish() }
        render()
    }

    override fun onStart() {
        super.onStart()
        danser()
    }

    override fun onStop() {
        danse?.cancel()
        danse = null
        super.onStop()
    }

    private fun choisir(adresse: Adresse) {
        Store.setAdresse(this, adresse)
        render()
    }

    /** Sans choix enregistré, le bravo reste neutre et la question est posée ici. */
    private fun render() {
        val adresse = Store.adresse(this)
        findViewById<TextView>(R.id.celebration_title).setText(
            if (annulee) Adresse.hazak(adresse) else Adresse.bravo(adresse)
        )
        findViewById<View>(R.id.celebration_ask).visibility =
            if (adresse == null) View.VISIBLE else View.GONE
    }

    /**
     * Un balancement autour des pieds et un petit saut. Rien si les
     * animations sont coupées dans le téléphone : c'est souvent un choix
     * d'accessibilité, pas un oubli.
     */
    private fun danser() {
        if (annulee || !ValueAnimator.areAnimatorsEnabled()) return
        val art = findViewById<ImageView>(R.id.celebration_art)
        art.post {
            art.pivotX = art.width / 2f
            art.pivotY = art.height.toFloat()
        }
        val saut = 14f * resources.displayMetrics.density
        val balance = ObjectAnimator.ofFloat(art, View.ROTATION, -7f, 7f).apply {
            duration = 520L
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            interpolator = AccelerateDecelerateInterpolator()
        }
        val rebond = ObjectAnimator.ofFloat(art, View.TRANSLATION_Y, 0f, -saut).apply {
            duration = 260L
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
        }
        danse = AnimatorSet().apply {
            playTogether(balance, rebond)
            start()
        }
    }

    companion object {
        const val EXTRA_MINUTES = "minutes"
        const val EXTRA_ANNULEE = "annulee"

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
            val minutes = Store.takePendingCelebration(activity) ?: return
            activity.startActivity(
                Intent(activity, CelebrationActivity::class.java).putExtra(EXTRA_MINUTES, minutes)
            )
        }
    }
}
