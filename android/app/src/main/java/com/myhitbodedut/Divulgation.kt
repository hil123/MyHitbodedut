package com.myhitbodedut

import android.content.Intent
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * La divulgation bien visible (31/10, politique « Données utilisateur » de
 * Google Play) : avant d'envoyer l'utilisateur dans les réglages d'Android
 * pour l'une des deux autorisations spéciales de la barrière du matin, une
 * fenêtre dit quelle donnée est lue, pourquoi, que rien ne quitte le
 * téléphone, et comment couper la barrière.
 *
 * C'est le seul chemin vers ces deux écrans système : Permissions.overlayIntent
 * et Permissions.usageAccessIntent ne s'appellent qu'ici, et seulement depuis
 * le bouton « Continuer » (DivulgationTest). « Pas maintenant » referme la
 * fenêtre sans rien ouvrir.
 */
object Divulgation {

    /** Le titre et le texte propres à une autorisation spéciale. */
    data class Texte(@StringRes val titre: Int, @StringRes val corps: Int)

    /** Les deux autorisations spéciales ont chacune leur texte ; les autres n'en demandent pas. */
    fun texte(autorisation: Autorisation): Texte? = when (autorisation) {
        Autorisation.OVERLAY -> Texte(R.string.divulgation_overlay_titre, R.string.divulgation_overlay_texte)
        Autorisation.USAGE -> Texte(R.string.divulgation_usage_titre, R.string.divulgation_usage_texte)
        Autorisation.NOTIFICATIONS, Autorisation.BATTERIE -> null
    }

    /**
     * Montre la fenêtre ; « Continuer » ouvre l'écran système par [ouvrir]
     * (qui sait quoi faire si l'écran manque), « Pas maintenant » ne fait rien.
     */
    fun montrer(activite: AppCompatActivity, autorisation: Autorisation, ouvrir: (Intent) -> Unit) {
        val texte = texte(autorisation) ?: return
        MaterialAlertDialogBuilder(activite)
            .setTitle(texte.titre)
            .setMessage(texte.corps)
            .setPositiveButton(R.string.divulgation_continuer) { _, _ ->
                ouvrir(
                    when (autorisation) {
                        Autorisation.OVERLAY -> Permissions.overlayIntent(activite)
                        else -> Permissions.usageAccessIntent()
                    }
                )
            }
            .setNegativeButton(R.string.divulgation_pas_maintenant, null)
            .show()
    }
}
