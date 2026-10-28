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
 * localisation précise (28/10 ; l'approximative suffit si on ne donne qu'elle), et
 * seulement au moment où on la choisit ; elle
 * ne sert qu'au calcul et reste sur l'appareil. L'application ne déclare
 * toujours aucune permission Internet.
 *
 * À créer comme propriété de l'activité, avant onCreate : l'enregistrement de
 * la demande d'autorisation l'exige.
 */
class ChoixLieu(private val activite: AppCompatActivity, private val apres: () -> Unit) {

    // La localisation précise (28/10, « en fonction de la position exacte ») ; l'approximative
    // suffit encore si on ne donne qu'elle.
    private val autorisation = activite.registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { r ->
        if (r.values.any { it }) localiser() else echec()
    }

    /** Silencieux : rafraîchit la position sans message (ouverture de l'écran), seulement si déjà permise. */
    private var silencieux = false

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

    private fun permise(p: String) = ContextCompat.checkSelfPermission(activite, p) == PackageManager.PERMISSION_GRANTED

    private fun demanderPosition() {
        silencieux = false
        if (permise(Manifest.permission.ACCESS_FINE_LOCATION)) localiser()
        else autorisation.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
    }

    /** « Position exacte » (boussole) : la localisation précise, demandée au besoin. */
    fun positionExacte() = demanderPosition()

    /**
     * À l'ouverture de l'écran : si le lieu est la position du téléphone (pas une ville) et que
     * la localisation est déjà permise, la reprendre sans rien dire — les zmanim et la direction
     * suivent l'endroit où l'on est (28/10). Rien n'est demandé.
     */
    fun rafraichir() {
        if (Store.lieuZmanim(activite) == null || Store.lieuVille(activite) != -1L) return
        if (!permise(Manifest.permission.ACCESS_FINE_LOCATION) && !permise(Manifest.permission.ACCESS_COARSE_LOCATION)) return
        silencieux = true
        localiser()
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
        val tous = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER)
        // Avant Android 12, les fournisseurs GPS et passif exigent la
        // localisation précise : avec la seule approximative, les interroger
        // lève SecurityException. On s'en tient alors au réseau.
        val precise = permise(Manifest.permission.ACCESS_FINE_LOCATION)
        val permis = if (precise || Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) tous else listOf(LocationManager.NETWORK_PROVIDER)
        val connue = tous.mapNotNull { runCatching { lm.getLastKnownLocation(it) }.getOrNull() }.maxByOrNull { it.time }
        // Position exacte (28/10) : une position connue ne sert telle quelle que fraîche (2 min) et
        // précise (100 m) ; sinon on demande un point neuf, le GPS d'abord.
        if (connue != null && System.currentTimeMillis() - connue.time < DEUX_MINUTES &&
            (!connue.hasAccuracy() || connue.accuracy <= PRECIS_M)) return enregistrer(connue)
        val actifs = permis.filter { runCatching { lm.isProviderEnabled(it) }.getOrDefault(false) }
        if (actifs.isEmpty()) return connue?.let { ancienne(it) } ?: echec()
        if (!silencieux) Toast.makeText(activite, activite.getString(R.string.zmanim_position_attente), Toast.LENGTH_SHORT).show()
        var fini = false
        var reponses = 0
        var meilleure: Location? = null
        val ecouteurs = mutableListOf<LocationListener>()
        val delai = android.os.Handler(Looper.getMainLooper())
        // Le premier point précis l'emporte ; sinon, au bout du délai ou quand tous ont répondu,
        // le plus précis reçu, puis la dernière position connue.
        fun conclure(position: Location?, finDuDelai: Boolean = false) {
            if (fini) return
            if (position != null && (meilleure == null || (position.hasAccuracy() && position.accuracy < (meilleure?.accuracy ?: Float.MAX_VALUE)))) {
                meilleure = position
            }
            val precis = position != null && (!position.hasAccuracy() || position.accuracy <= PRECIS_M)
            if (!precis && !finDuDelai && ++reponses < actifs.size) return
            fini = true
            delai.removeCallbacksAndMessages(null)
            ecouteurs.forEach { runCatching { lm.removeUpdates(it) } }
            when {
                meilleure != null -> enregistrer(meilleure!!)
                connue != null -> ancienne(connue)
                else -> echec()
            }
        }
        delai.postDelayed({ conclure(null, finDuDelai = true) }, DELAI_MS)
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
        if (!silencieux) Toast.makeText(activite, activite.getString(R.string.zmanim_position_ancienne), Toast.LENGTH_LONG).show()
        enregistrer(position)
    }

    private fun enregistrer(position: Location) {
        if (activite.isFinishing || activite.isDestroyed) return
        // La ville la plus proche ne fait que nommer la position et donner l'altitude. Le fuseau
        // reste celui du téléphone (celui d'une ville voisine fausserait les zmanim près d'une
        // frontière), et Israël ou diaspora se lit sur le territoire, pas sur la ville (16/10).
        val proche = Villes.plusProche(Data.villes(activite), position.latitude, position.longitude)
        val langue = ChizukCard.language(activite)
        val nom = proche?.let { activite.getString(R.string.zmanim_position_pres, it.nom.forLanguage(langue)) }
            ?: activite.getString(R.string.zmanim_position)
        // L'altitude au-dessus du niveau de la mer quand le téléphone la donne (Android 14+) ; sinon
        // celle de la ville la plus proche. L'altitude GPS brute (ellipsoïde) n'est pas celle des cartes.
        val altitude = if (Build.VERSION.SDK_INT >= 34 && position.hasMslAltitude()) position.mslAltitudeMeters.coerceAtLeast(0.0)
        else proche?.altitude ?: 0.0
        val lieu = Zmanim.Lieu(nom, position.latitude, position.longitude, altitude, TimeZone.getDefault().id)
        Store.setLieuZmanim(activite, lieu, null)
        Store.setInIsrael(activite, TerritoireIsrael.contient(position.latitude, position.longitude))
        apres()
    }

    private fun echec() {
        if (activite.isFinishing || activite.isDestroyed || silencieux) return
        Toast.makeText(activite, activite.getString(R.string.zmanim_position_refusee), Toast.LENGTH_LONG).show()
    }

    private companion object {
        const val DEUX_MINUTES = 120_000L
        const val PRECIS_M = 100f
        const val DELAI_MS = 25_000L
    }
}
