package com.myhitbodedut

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Les notifications des rappels en vol (VolRappels). Comme l'alerte de Chabbat :
 * une seule alarme inexacte, celle du prochain rappel, reprogrammée à chaque
 * déclenchement, au démarrage et à chaque calcul du vol. La liste est calculée
 * par VolActivity et gardée avec le vol (Store) : le récepteur ne refait pas le
 * calcul, il lit la liste. En mode avion aussi : rien ne passe par le réseau.
 */
object VolAlerte {

    private const val REQUETE = 160

    /** Un rappel déclenché avec un peu de retard (Android regroupe les alarmes) s'affiche encore ; au-delà, il est passé. */
    private const val TOLERANCE_MS = 20 * 60_000L

    private fun intent(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context, REQUETE, Intent(context, VolAlerteReceiver::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    /** Garde les rappels d'un vol calculé et programme le prochain ; une liste vide les efface. */
    fun enregistrer(context: Context, rappels: List<VolRappels.Rappel>) {
        Store.setVolRappels(context, rappels)
        programmer(context)
    }

    fun programmer(context: Context) {
        val alarmes = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pending = intent(context)
        alarmes.cancel(pending)
        if (!Store.volRappelsActifs(context)) return
        val maintenant = System.currentTimeMillis()
        val vu = Store.volRappelVu(context)
        val prochain = Store.volRappels(context).map { it.instant.toEpochMilli() }.filter { it > vu && it > maintenant - TOLERANCE_MS }.minOrNull() ?: return
        runCatching { alarmes.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, prochain.coerceAtLeast(maintenant), pending) }
    }

    /** Les rappels dus maintenant, pas encore montrés (plusieurs peuvent tomber à la même minute). */
    fun dus(rappels: List<VolRappels.Rappel>, vu: Long, maintenant: Long): List<VolRappels.Rappel> =
        rappels.filter { val t = it.instant.toEpochMilli(); t > vu && t <= maintenant + 60_000L && t > maintenant - TOLERANCE_MS }

    /** « mar. 10 · 18:42 TLV · 11:42 JFK » (VolActivity) : l'heure du départ et celle de l'arrivée. */
    fun deuxHeures(plan: Vol.Plan, t: Instant, locale: Locale): String {
        val jour = DateTimeFormatter.ofPattern("EEE d", locale)
        val heure = DateTimeFormatter.ofPattern("HH:mm", locale)
        val dep = Vol.zone(plan.depart.fuseau)?.let { t.atZone(it) } ?: return t.toString()
        val arr = Vol.zone(plan.arrivee.fuseau)?.let { t.atZone(it) } ?: return t.toString()
        val autreJour = if (arr.toLocalDate() != dep.toLocalDate()) "${arr.format(jour)} " else ""
        return "${dep.format(jour)} \u2066${dep.format(heure)} ${plan.depart.iata}\u2069 · $autreJour\u2066${arr.format(heure)} ${plan.arrivee.iata}\u2069"
    }

    fun nomPriere(c: Context, priere: String?) = c.getString(when (priere) {
        "chaharit" -> R.string.widget_zmanim_chaharit
        "minha" -> R.string.widget_zmanim_minha
        else -> R.string.widget_zmanim_arvit
    })

    /** Le titre et le texte d'un rappel ; [plan] donne les heures des deux aéroports. */
    fun textes(c: Context, r: VolRappels.Rappel, plan: Vol.Plan?): Pair<String, String> {
        val locale = Locale.forLanguageTag(ChizukCard.language(c))
        val heure = { t: Instant -> plan?.let { deuxHeures(it, t, locale) } ?: t.toString() }
        return when (r.type) {
            VolRappels.Type.TEFILAT_HADEREKH -> c.getString(R.string.vol_rappel_derekh_titre) to
                (c.getString(R.string.vol_rappel_derekh_texte) +
                    (r.limite?.let { "\n" + c.getString(R.string.vol_rappel_derekh_priere, nomPriere(c, r.priere), heure(it)) } ?: ""))
            VolRappels.Type.PRIERE -> c.getString(R.string.vol_rappel_priere_titre, nomPriere(c, r.priere)) to
                (r.limite?.let { c.getString(R.string.vol_rappel_priere_texte, nomPriere(c, r.priere), heure(it)) }
                    ?: c.getString(R.string.vol_rappel_priere_sans_limite, nomPriere(c, r.priere)))
            VolRappels.Type.PRIERE_FIN -> c.getString(R.string.vol_rappel_fin_titre, nomPriere(c, r.priere)) to
                c.getString(R.string.vol_rappel_fin_texte, heure(r.limite ?: r.instant))
            VolRappels.Type.OMER -> c.getString(R.string.vol_rappel_omer_titre) to c.getString(R.string.vol_rappel_omer_texte, r.omer.toString())
        }
    }

    /** Le texte du siddur qu'ouvre le rappel, et le hassid qui l'accompagne. */
    fun texteSiddur(r: VolRappels.Rappel): String = when (r.type) {
        VolRappels.Type.TEFILAT_HADEREKH -> "tefilat_haderekh"
        VolRappels.Type.OMER -> "sefirat_haomer"
        else -> r.priere ?: "arvit"
    }

    /** L'illustration en carré : une image haute (le hassid debout) garde le haut, le visage. */
    fun icone(c: Context, r: VolRappels.Rappel): android.graphics.Bitmap {
        val b = BitmapFactory.decodeResource(c.resources, illustration(r))
        return if (b.height > b.width) android.graphics.Bitmap.createBitmap(b, 0, 0, b.width, b.width) else b
    }

    fun illustration(r: VolRappels.Rappel): Int = when (r.type) {
        VolRappels.Type.TEFILAT_HADEREKH -> R.drawable.scene_airport
        VolRappels.Type.OMER -> R.drawable.char_tehilim
        else -> SiddurMoment.illustration(r.priere ?: "arvit")
    }
}

/** Montre les rappels dus, puis programme le suivant. */
class VolAlerteReceiver : BroadcastReceiver() {
    override fun onReceive(brut: Context, intent: Intent) {
        val context = LangueApp.envelopper(brut)
        Notifications.ensureChannels(context)
        val maintenant = System.currentTimeMillis()
        val dus = if (Store.volRappelsActifs(context)) VolAlerte.dus(Store.volRappels(context), Store.volRappelVu(context), maintenant) else emptyList()
        val plan = Store.vol(context, Data.aeroports(context))
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        dus.forEach { r ->
            val (titre, texte) = VolAlerte.textes(context, r, plan)
            val ouvrir = PendingIntent.getActivity(
                context, 161 + r.type.ordinal, SiddurLectureActivity.intent(context, VolAlerte.texteSiddur(r)),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            val n = Notification.Builder(context, Notifications.CHANNEL_REMINDER)
                .setSmallIcon(R.drawable.ic_notification)
                .setLargeIcon(VolAlerte.icone(context, r))
                .setContentTitle(titre)
                .setContentText(texte)
                .setStyle(Notification.BigTextStyle().bigText(texte))
                .setContentIntent(ouvrir)
                .setAutoCancel(true)
                .build()
            // Le Omer garde sa notification à part : il tombe à la même minute qu'Arvit.
            runCatching { manager.notify(if (r.type == VolRappels.Type.OMER) Notifications.ID_VOL_OMER else Notifications.ID_VOL, n) }
        }
        dus.maxOfOrNull { it.instant.toEpochMilli() }?.let { Store.setVolRappelVu(context, it) }
        VolAlerte.programmer(context)
    }
}
