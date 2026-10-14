package com.myhitbodedut

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Le rappel de Birkat haLevana (14/10) : à la sortie des étoiles du premier soir
 * possible, du motsaé Chabbat conseillé et du dernier soir, selon l'usage suivi
 * (VueJour.usageLevana). Comme l'alerte de Chabbat : une seule alarme inexacte,
 * reprogrammée à chaque déclenchement, au démarrage et à l'ouverture.
 */
object LevanaAlerte {

    private const val REQUETE = 150

    enum class Soir(val titre: Int) { PREMIER(R.string.levana_rappel_premier), MOTSAE(R.string.levana_rappel_motsae), DERNIER(R.string.levana_rappel_dernier) }

    /** Les soirs à rappeler d'une fenêtre, à partir de [depuis]. */
    fun soirs(f: Levana.Fenetre, depuis: LocalDate, zone: ZoneId): List<Pair<LocalDate, Soir>> = buildList {
        add(f.premierSoir(zone) to Soir.PREMIER)
        Levana.motsaeChabbat(f, f.premierSoir(zone), zone)?.let { if (it != f.premierSoir(zone) && it != f.dernierSoir(zone)) add(it to Soir.MOTSAE) }
        if (f.dernierSoir(zone) != f.premierSoir(zone)) add(f.dernierSoir(zone) to Soir.DERNIER)
    }.filter { !it.first.isBefore(depuis) }.sortedBy { it.first }

    /**
     * Le prochain rappel après [maintenant] : le soir, à [heure] de ce jour-là
     * (la sortie des étoiles au lieu choisi, sinon 20 h).
     */
    fun prochain(maintenant: ZonedDateTime, usage: Levana.Usage, heure: (LocalDate) -> ZonedDateTime): Pair<ZonedDateTime, Soir>? {
        val zone = maintenant.zone
        var date = maintenant.toLocalDate()
        repeat(3) {
            val f = Levana.fenetre(date, zone, usage)
            for ((d, soir) in soirs(f, maintenant.toLocalDate(), zone)) {
                val quand = heure(d)
                if (quand.isAfter(maintenant) && !quand.toInstant().isAfter(f.fin)) return quand to soir
            }
            date = f.dernierSoir(zone).plusDays(1)
        }
        return null
    }

    private fun intent(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context, REQUETE, Intent(context, LevanaAlerteReceiver::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    /** L'heure du rappel un soir donné : la sortie des étoiles au lieu choisi, sinon 20 h. */
    fun heure(context: Context, zone: ZoneId): (LocalDate) -> ZonedDateTime = { d ->
        Store.lieuZmanim(context)?.let { lieu -> Zmanim.jour(lieu, d).firstOrNull { it.cle == "tset" }?.local?.withZoneSameInstant(zone) }
            ?: d.atTime(20, 0).atZone(zone)
    }

    fun programmer(context: Context) {
        val alarmes = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pending = intent(context)
        alarmes.cancel(pending)
        if (!Store.levanaRappel(context)) return
        val zone = Store.lieuZmanim(context)?.fuseau?.let { runCatching { ZoneId.of(it) }.getOrNull() } ?: ZoneId.systemDefault()
        val (quand, soir) = prochain(ZonedDateTime.now(zone), VueJour.usageLevana(context), heure(context, zone)) ?: return
        Store.setLevanaRappelSoir(context, soir.name)
        runCatching { alarmes.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, quand.toInstant().toEpochMilli(), pending) }
    }
}

/** Affiche le rappel de Birkat haLevana ; le toucher ouvre le texte dans le noussa'h choisi. */
class LevanaAlerteReceiver : BroadcastReceiver() {
    override fun onReceive(brut: Context, intent: Intent) {
        val context = LangueApp.envelopper(brut)
        Notifications.ensureChannels(context)
        val soir = runCatching { LevanaAlerte.Soir.valueOf(Store.levanaRappelSoir(context) ?: "") }.getOrDefault(LevanaAlerte.Soir.PREMIER)
        val ouvrir = PendingIntent.getActivity(
            context, 151,
            SiddurLectureActivity.intent(context, "birkat_halevana"),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val texte = context.getString(R.string.levana_rappel_texte)
        val notification = Notification.Builder(context, Notifications.CHANNEL_REMINDER)
            .setSmallIcon(R.drawable.ic_notification)
            .setLargeIcon(BitmapFactory.decodeResource(context.resources, R.drawable.char_praise))
            .setContentTitle(context.getString(soir.titre))
            .setContentText(texte)
            .setStyle(Notification.BigTextStyle().bigText(texte))
            .setContentIntent(ouvrir)
            .setAutoCancel(true)
            .build()
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        runCatching { manager.notify(Notifications.ID_LEVANA, notification) }
        LevanaAlerte.programmer(context)
    }
}
