package com.myhitbodedut

import android.app.Activity
import androidx.appcompat.app.AlertDialog
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * Ajouter du temps à un jour passé (16/10) : une hitbodedout faite hors du téléphone — dehors,
 * le Chabbat, sans l'appareil — s'inscrit après coup, sur n'importe quel jour jusqu'à
 * aujourd'hui, même un jour déjà fait (les minutes s'ajoutent, Store.addSession). Jamais
 * un jour à venir. Depuis la bande de la semaine et depuis le calendrier.
 */
object AjoutTemps {

    fun possible(date: LocalDate): Boolean = !date.isAfter(LocalDate.now())

    /** [montrer] : l'action « montrer la durée sur la bande », pour un jour fait de la semaine. */
    fun ouvrir(activity: Activity, date: LocalDate, montrer: (() -> Unit)? = null, apres: () -> Unit) {
        if (!possible(date)) return
        val iso = date.toString()
        val minutes = Store.record(activity, iso)?.takeIf { it.faite }?.minutes
        val titre = date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(activity.resources.configuration.locales[0]))
        val message = if (minutes == null) activity.getString(R.string.jour_rien)
            else activity.getString(R.string.jour_fait, Store.formatDuration(activity, minutes))
        AlertDialog.Builder(activity)
            .setTitle(titre)
            .setMessage(message)
            .setPositiveButton(R.string.jour_ajouter_temps) { _, _ ->
                DureeSeance.choisir(activity, R.string.offline_how_long) { m ->
                    Store.addSession(activity, m, theme = null, date = iso)
                    Celebrations.notification(activity)
                    StreakWidget.refresh(activity)
                    android.widget.Toast.makeText(activity,
                        activity.getString(R.string.jour_ajoute, Store.formatDuration(activity, m)), android.widget.Toast.LENGTH_SHORT).show()
                    apres()
                }
            }
            .apply { if (montrer != null && minutes != null) setNeutralButton(R.string.jour_montrer_duree) { _, _ -> montrer() } }
            .setNegativeButton(R.string.action_back, null)
            .show()
    }
}
