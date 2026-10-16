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

    /**
     * La position (16/10, « la position ne marche pas très bien ») : une position connue de
     * moins d'une heure sert telle quelle ; sinon tous les fournisseurs permis et allumés
     * (réseau, GPS, passif) sont interrogés ensemble, le premier qui répond l'emporte, et au
     * bout de 25 s sans réponse la dernière position connue, même ancienne, sert encore — pour
     * des zmanim, quelques kilomètres ne changent rien. Avant, seul le premier fournisseur
     * était interrogé, et un réseau sans localisation suffisait à tout faire échouer.
     */
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
        val actifs = permis.filter { runCatching { lm.isProviderEnabled(it) }.getOrDefault(false) }
        if (actifs.isEmpty()) return connue?.let { ancienne(it) } ?: echec()
        Toast.makeText(activite, activite.getString(R.string.zmanim_position_attente), Toast.LENGTH_SHORT).show()
        var fini = false
        var reponses = 0
        val ecouteurs = mutableListOf<LocationListener>()
        val delai = android.os.Handler(Looper.getMainLooper())
        fun conclure(position: Location?) {
            if (fini) return
            if (position == null && ++reponses < actifs.size) return
            fini = true
            delai.removeCallbacksAndMessages(null)
            ecouteurs.forEach { runCatching { lm.removeUpdates(it) } }
            when {
                position != null -> enregistrer(position)
                connue != null -> ancienne(connue)
                else -> echec()
            }
        }
        delai.postDelayed({ reponses = actifs.size - 1; conclure(null) }, DELAI_MS)
        actifs.forEach { f ->
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    lm.getCurrentLocation(f, null, activite.mainExecutor) { conclure(it) }
                } else {
                    // Avant Android 11, les quatre méthodes de LocationListener sont
                    // abstraites sur l'appareil : un lambda n'en fournirait qu'une.
                    val e = object : LocationListener {
                        override fun onLocationChanged(location: Location) = conclure(location)
                        override fun onProviderEnabled(provider: String) = Unit
                        override fun onProviderDisabled(provider: String) = conclure(null)

                        @Deprecated("Android 10 et avant seulement")
                        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
                    }
                    ecouteurs += e
                    @Suppress("DEPRECATION")
                    lm.requestSingleUpdate(f, e, Looper.getMainLooper())
                }
            } catch (e: SecurityException) {
                conclure(null)
            } catch (e: IllegalArgumentException) {
                conclure(null)
            }
        }
    }

    /** Pas de position fraîche : la dernière connue, et on le dit. */
    private fun ancienne(position: Location) {
        Toast.makeText(activite, activite.getString(R.string.zmanim_position_ancienne), Toast.LENGTH_LONG).show()
        enregistrer(position)
    }

    private fun enregistrer(position: Location) {
        if (activite.isFinishing || activite.isDestroyed) return
        // La ville la plus proche nomme la position, donne l'altitude et dit Israël ou diaspora.
        val proche = Villes.plusProche(Data.villes(activite), position.latitude, position.longitude)
        val langue = ChizukCard.language(activite)
        val nom = proche?.let { activite.getString(R.string.zmanim_position_pres, it.nom.forLanguage(langue)) }
            ?: activite.getString(R.string.zmanim_position)
        val lieu = Zmanim.Lieu(nom, position.latitude, position.longitude, proche?.altitude ?: 0.0,
            proche?.fuseau ?: TimeZone.getDefault().id)
        Store.setLieuZmanim(activite, lieu, null)
        if (proche != null) Store.setInIsrael(activite, proche.enIsrael)
        apres()
    }

    private fun echec() {
        if (activite.isFinishing || activite.isDestroyed) return
        Toast.makeText(activite, activite.getString(R.string.zmanim_position_refusee), Toast.LENGTH_LONG).show()
    }

    private companion object {
        const val UNE_HEURE = 3_600_000L
        const val DELAI_MS = 25_000L
    }
}
