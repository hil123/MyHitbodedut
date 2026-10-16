package com.myhitbodedut

import android.animation.ValueAnimator
import android.app.Activity
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout
import android.widget.Toast
import kotlin.random.Random

/**
 * La fête d'un palier ou d'un trophée (demande du 11/10) : des confettis
 * sur l'écran, et une notification qui en porte l'image. Les animations
 * coupées dans les réglages du téléphone, l'écran n'anime rien.
 */
object Confettis {

    private val COULEURS = intArrayOf(
        Color.rgb(0xC9, 0x9A, 0x2E), Color.rgb(0x1F, 0x6F, 0x5C), Color.rgb(0xE0, 0x5A, 0x47),
        Color.rgb(0x3B, 0x6E, 0xC4), Color.rgb(0xF2, 0xC9, 0x4C), Color.rgb(0x8E, 0x4F, 0xB5),
    )

    private class Piece(val x: Float, val vx: Float, val vy: Float, val tour: Float, val taille: Float, val couleur: Int, val rond: Boolean, val retard: Float)

    /** La vue qui tombe : 2,8 s, puis elle se retire d'elle-même. */
    class Vue(context: Context) : View(context) {
        private val pieces = List(140) {
            Piece(Random.nextFloat(), (Random.nextFloat() - 0.5f) * 0.25f, 0.55f + Random.nextFloat() * 0.6f,
                (Random.nextFloat() - 0.5f) * 1440f, 6f + Random.nextFloat() * 8f, COULEURS.random(), Random.nextInt(4) == 0, Random.nextFloat() * 0.35f)
        }
        private val pinceau = Paint(Paint.ANTI_ALIAS_FLAG)
        private var t = 0f

        init {
            isClickable = false
            importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
        }

        fun lancer() {
            ValueAnimator.ofFloat(0f, 1f).apply {
                duration = 2800
                interpolator = LinearInterpolator()
                addUpdateListener { t = it.animatedValue as Float; invalidate() }
                addListener(object : android.animation.AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: android.animation.Animator) { (parent as? ViewGroup)?.removeView(this@Vue) }
                })
                start()
            }
        }

        override fun onDraw(canvas: Canvas) {
            val d = resources.displayMetrics.density
            pieces.forEach { p ->
                val u = ((t - p.retard) / (1f - p.retard)).coerceIn(0f, 1f)
                if (u <= 0f) return@forEach
                val x = (p.x + p.vx * u) * width
                val y = (-0.05f + p.vy * u + 0.6f * u * u) * height
                pinceau.color = p.couleur
                pinceau.alpha = ((1f - u) * 2f).coerceAtMost(1f).times(255).toInt()
                canvas.save()
                canvas.rotate(p.tour * u, x, y)
                val s = p.taille * d
                if (p.rond) canvas.drawCircle(x, y, s / 2, pinceau) else canvas.drawRect(x - s / 2, y - s / 4, x + s / 2, y + s / 4, pinceau)
                canvas.restore()
            }
        }
    }

    /** Les confettis par-dessus l'écran, et la phrase de la fête. */
    fun feter(activity: Activity, message: String) {
        Toast.makeText(activity, message, Toast.LENGTH_LONG).show()
        if (!ValueAnimator.areAnimatorsEnabled()) return
        val racine = activity.findViewById<FrameLayout>(android.R.id.content) ?: return
        val vue = Vue(activity)
        racine.addView(vue, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        vue.lancer()
    }

    /** Une image de confettis, pour la notification. */
    fun image(context: Context, icone: String): Bitmap {
        val l = 720; val h = 360
        val b = Bitmap.createBitmap(l, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(b)
        c.drawColor(Apparence.couleur(context, R.color.brand))
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        val r = Random(icone.hashCode())
        repeat(110) {
            p.color = COULEURS[r.nextInt(COULEURS.size)]
            val x = r.nextFloat() * l; val y = r.nextFloat() * h; val s = 8f + r.nextFloat() * 14f
            c.save(); c.rotate(r.nextFloat() * 360f, x, y)
            if (r.nextInt(4) == 0) c.drawCircle(x, y, s / 2, p) else c.drawRect(x - s / 2, y - s / 4, x + s / 2, y + s / 4, p)
            c.restore()
        }
        val trace = TropheeDessin.icones(context)[icone]
        if (trace != null) {
            // L'icône de la famille au trait (16/10), blanche sur les confettis.
            TropheeDessin.dessinerIcone(c, trace, l / 2f, h / 2f, 170f, TropheeDessin.traitIcone().apply { color = Color.WHITE })
        } else {
            p.color = Color.WHITE
            p.textSize = 150f
            p.textAlign = Paint.Align.CENTER
            c.drawText(icone, l / 2f, h / 2f - (p.descent() + p.ascent()) / 2, p)
        }
        return b
    }

    fun notifier(context: Context, titre: String, texte: String, icone: String) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (!manager.areNotificationsEnabled()) return
        Notifications.ensureChannels(context)
        val ouvrir = PendingIntent.getActivity(
            context, 5, Intent(context, PaliersActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val image = image(context, icone)
        val n = Notification.Builder(context, Notifications.CHANNEL_CELEBRATION)
            .setContentTitle(titre)
            .setContentText(texte)
            .setSmallIcon(R.drawable.ic_notification)
            .setLargeIcon(image)
            .setStyle(Notification.BigPictureStyle().bigPicture(image).setSummaryText(texte))
            .setContentIntent(ouvrir)
            .setAutoCancel(true)
            .build()
        runCatching { manager.notify(Notifications.ID_CELEBRATION, n) }
    }
}

/**
 * Ce qui est neuf depuis la dernière fois : paliers et trophées. L'écran et
 * la notification tiennent chacun leur liste de ce qu'ils ont déjà fêté ; le
 * tout premier passage ne fête rien (une installation qui a déjà de
 * l'historique ne reçoit pas une pluie de vieux trophées).
 */
object Celebrations {

    /** [icone] et [message] vides quand rien n'est neuf ; [marques] et [trophees] : l'état à retenir. */
    private class Neuf(val icone: String, val message: String, val marques: Set<String>, val trophees: Set<String>)

    /** Un trophée neuf d'abord, sinon le plus haut palier neuf. */
    private fun neuf(c: Context, paliersVus: Set<String>?, tropheesVus: Set<String>?): Neuf {
        val bilan = Store.bilan(c)
        val etats = Paliers.etats(bilan)
        val gagnes = Trophees.gagnes(Store.pratique(c))
        val trophee = Trophees.nouveaux(gagnes, tropheesVus).firstOrNull()
        val palier = paliersVus?.let { vus -> Paliers.nouveaux(etats, bilan, vus).maxByOrNull { it.rang } }
        val (icone, message) = when {
            trophee != null -> trophee.famille.icone to c.getString(R.string.trophee_niveau_gagne,
                TropheesVue.nom(c, trophee.famille), trophee.rang, TropheesVue.metal(c, trophee.metal))
            palier != null -> "🎉" to c.getString(R.string.palier_nouveau,
                PaliersVue.libelle(c, palier.piste, bilan), PaliersVue.valeur(c, palier.piste, palier.atteint))
            else -> "" to ""
        }
        return Neuf(icone, message, Paliers.marques(etats, bilan), gagnes.map { it.cle }.toSet())
    }

    /** À l'accueil : des étincelles si un palier ou un trophée est neuf pour l'écran. */
    fun ecran(activity: Activity) {
        val r = neuf(activity, Store.paliersVus(activity), Store.tropheesVus(activity))
        // Des étincelles du grade autour de la ligne du trophée, plutôt que des confettis multicolores (16/10).
        if (r.message.isNotEmpty()) {
            android.widget.Toast.makeText(activity, r.message, android.widget.Toast.LENGTH_LONG).show()
            val ligne = activity.findViewById<View>(R.id.paliers_line)
            val grade = Trophees.grade(r.trophees.size)
            ligne?.postDelayed({ AnimationsTrophees.etincelles(activity, ligne, grade) }, 500)
        }
        Store.setPaliersVus(activity, r.marques)
        Store.setTropheesVus(activity, r.trophees)
    }

    /** Ce qui est neuf est su sans notification (ajout de temps, application ouverte ; 16/10). */
    fun marquerNotifies(context: Context) {
        val r = neuf(context, Store.paliersNotifies(context), Store.tropheesNotifies(context))
        Store.setPaliersNotifies(context, r.marques)
        Store.setTropheesNotifies(context, r.trophees)
    }

    /** Après une séance enregistrée : la notification, même l'application fermée. */
    fun notification(context: Context) {
        val r = neuf(context, Store.paliersNotifies(context), Store.tropheesNotifies(context))
        if (r.message.isNotEmpty()) Confettis.notifier(context, context.getString(R.string.celebration_titre), r.message, r.icone)
        Store.setPaliersNotifies(context, r.marques)
        Store.setTropheesNotifies(context, r.trophees)
    }
}
