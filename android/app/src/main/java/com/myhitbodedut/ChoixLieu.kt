package com.myhitbodedut

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.util.TimeZone

/**
 * Le choix du lieu des zmanim (demande du 10/10) : la position du téléphone,
 * ou une ville de la liste (GeoNames). La position demande l'autorisation de
 * localisation approximative, et seulement au moment où on la choisit ; elle
 * ne sert qu'au calcul et reste sur l'appareil. L'application ne déclare
 * toujours aucune permission Internet.
 *
 * À créer comme propriété de l'activité, avant onCreate : l'enregistrement de
 * la demande d'autorisation l'exige.
 */
class ChoixLieu(private val activite: AppCompatActivity, private val apres: () -> Unit) {

    private val autorisation = activite.registerForActivityResult(ActivityResultContracts.RequestPermission()) { accordee ->
        if (accordee) localiser() else echec()
    }

    fun montrer() {
        val langue = ChizukCard.language(activite)
        val villes = Data.villes(activite)
        val noms = listOf(activite.getString(R.string.zmanim_position)) + villes.map { it.nom.forLanguage(langue) }
        MaterialAlertDialogBuilder(activite)
            .setTitle(R.string.zmanim_choisir_lieu)
            .setItems(noms.toTypedArray()) { _, i ->
                if (i == 0) demanderPosition()
                else {
                    val ville = villes[i - 1]
                    Store.setLieuZmanim(activite, ville.lieu(langue), ville)
                    apres()
                }
            }
            .show()
    }

    private fun demanderPosition() {
        val accordee = ContextCompat.checkSelfPermission(activite, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        if (accordee) localiser() else autorisation.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
    }

    /** La dernière position connue si elle date de moins d'une heure, sinon une position fraîche. */
    @SuppressLint("MissingPermission") // vérifiée par demanderPosition
    private fun localiser() {
        val lm = activite.getSystemService(LocationManager::class.java) ?: return echec()
        val tous = listOf(LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER, LocationManager.PASSIVE_PROVIDER)
        // Avant Android 12, les fournisseurs GPS et passif exigent la
        // localisation précise : avec la seule approximative, les interroger
        // lève SecurityException. On s'en tient alors au réseau.
        val permis = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) tous else listOf(LocationManager.NETWORK_PROVIDER)
        val connue = tous.mapNotNull { runCatching { lm.getLastKnownLocation(it) }.getOrNull() }.maxByOrNull { it.time }
        if (connue != null && System.currentTimeMillis() - connue.time < UNE_HEURE) return enregistrer(connue)
        val fournisseur = permis.firstOrNull { runCatching { lm.isProviderEnabled(it) }.getOrDefault(false) } ?: return echec()
        Toast.makeText(activite, activite.getString(R.string.zmanim_position_attente), Toast.LENGTH_SHORT).show()
        try {
            demanderUneFois(lm, fournisseur)
        } catch (e: SecurityException) {
            echec()
        } catch (e: IllegalArgumentException) {
            echec()
        }
    }

    @SuppressLint("MissingPermission") // vérifiée par demanderPosition
    private fun demanderUneFois(lm: LocationManager, fournisseur: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            lm.getCurrentLocation(fournisseur, null, activite.mainExecutor) { it?.let(::enregistrer) ?: echec() }
        } else {
            // Avant Android 11, les quatre méthodes de LocationListener sont
            // abstraites sur l'appareil : un lambda n'en fournirait qu'une.
            @Suppress("DEPRECATION")
            lm.requestSingleUpdate(fournisseur, object : LocationListener {
                override fun onLocationChanged(location: Location) = enregistrer(location)
                override fun onProviderEnabled(provider: String) = Unit
                override fun onProviderDisabled(provider: String) = echec()

                @Deprecated("Android 10 et avant seulement")
                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
            }, Looper.getMainLooper())
        }
    }

    private fun enregistrer(position: Location) {
        if (activite.isFinishing || activite.isDestroyed) return
        val lieu = Zmanim.Lieu(activite.getString(R.string.zmanim_position), position.latitude, position.longitude, 0.0,
            TimeZone.getDefault().id)
        Store.setLieuZmanim(activite, lieu, null)
        apres()
    }

    private fun echec() {
        if (activite.isFinishing || activite.isDestroyed) return
        Toast.makeText(activite, activite.getString(R.string.zmanim_position_refusee), Toast.LENGTH_LONG).show()
    }

    private companion object {
        const val UNE_HEURE = 3_600_000L
    }
}
