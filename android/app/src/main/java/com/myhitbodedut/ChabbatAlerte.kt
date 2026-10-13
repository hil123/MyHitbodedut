package com.myhitbodedut

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import java.time.DayOfWeek
import java.time.Duration
import java.time.ZonedDateTime

/**
 * « Chabbat dans 3 h » (demande du 13/10) : une notification trois heures avant
 * l'allumage des bougies du vendredi, au lieu choisi pour les zmanim et avec les
 * minutes d'allumage des réglages. Sans lieu, pas d'heure d'allumage : rien n'est
 * programmé. Comme le rappel : une seule alarme inexacte, reprogrammée à chaque
 * déclenchement, au démarrage et à l'ouverture de l'application.
 */
object ChabbatAlerte {

    private const val REQUETE = 140
    const val AVANCE_HEURES = 3L

    /**
     * Le prochain moment d'alerte après [maintenant] : l'allumage du vendredi qui
     * vient (ou de la semaine suivante, si celui-ci est passé) moins trois heures.
     * [allumage] donne l'heure d'allumage d'une date, ou null si elle n'existe pas.
     */
    fun prochaine(maintenant: ZonedDateTime, allumage: (java.time.LocalDate) -> ZonedDateTime?): ZonedDateTime? {
        var vendredi = maintenant.toLocalDate()
        while (vendredi.dayOfWeek != DayOfWeek.FRIDAY) vendredi = vendredi.plusDays(1)
        repeat(2) {
            val alerte = allumage(vendredi)?.minus(Duration.ofHours(AVANCE_HEURES))
            if (alerte != null && alerte.isAfter(maintenant)) return alerte
            vendredi = vendredi.plusWeeks(1)
        }
        return null
    }

    private fun intent(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context, REQUETE,
        Intent(context, ChabbatAlerteReceiver::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    fun programmer(context: Context) {
        val alarmes = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pending = intent(context)
        alarmes.cancel(pending)
        val lieu = Store.lieuZmanim(context) ?: return
        val minutes = Store.allumage(context)
        val quand = prochaine(ZonedDateTime.now()) { d -> Zmanim.allumage(lieu, d, minutes).local } ?: return
        runCatching { alarmes.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, quand.toInstant().toEpochMilli(), pending) }
    }
}

/** Affiche « Chabbat dans 3 h », puis programme le vendredi suivant. */
class ChabbatAlerteReceiver : BroadcastReceiver() {
    override fun onReceive(brut: Context, intent: Intent) {
        val context = LangueApp.envelopper(brut)
        Notifications.ensureChannels(context)
        val ouvrir = PendingIntent.getActivity(
            context, 141, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val texte = context.getString(R.string.chabbat_alerte_texte)
        val notification = Notification.Builder(context, Notifications.CHANNEL_REMINDER)
            .setSmallIcon(R.drawable.ic_notification)
            .setLargeIcon(BitmapFactory.decodeResource(context.resources, R.drawable.char_shabbat))
            .setContentTitle(context.getString(R.string.chabbat_alerte_titre))
            .setContentText(texte)
            .setStyle(Notification.BigTextStyle().bigText(texte))
            .setContentIntent(ouvrir)
            .setAutoCancel(true)
            .build()
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        runCatching { manager.notify(Notifications.ID_CHABBAT, notification) }
        ChabbatAlerte.programmer(context)
    }
}
