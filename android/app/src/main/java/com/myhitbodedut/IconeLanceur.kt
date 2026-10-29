package com.myhitbodedut

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager

/**
 * L'icône de lancement suit le choix צדיק / צדקת et la couleur des boutons
 * (demande du 29/10) : le visage du hassid ou de la tsadeket
 * (ic_launcher_face, ic_launcher_face_f), sur la plaque claire de la palette.
 *
 * Android ne change pas l'icône d'une application en marche : chaque
 * combinaison est une entrée du manifeste (activity-alias « Icone_m_vert »…),
 * et une seule est active. L'entrée voulue s'active avant que les autres ne
 * s'éteignent, pour qu'il y ait toujours une icône. Certains lanceurs mettent
 * quelques secondes à la redessiner, ou retirent le raccourci posé sur le
 * bureau : l'application reste dans la liste des applications.
 */
object IconeLanceur {

    private const val PAQUET = "com.myhitbodedut"

    /** L'entrée par défaut du manifeste (android:enabled="true"). */
    const val PAR_DEFAUT = "$PAQUET.Icone_m_vert"

    fun alias(adresse: Adresse?, accent: Apparence.Accent): String =
        "$PAQUET.Icone_${if (adresse == Adresse.FEMME) "f" else "m"}_${accent.cle}"

    fun tous(): List<String> =
        listOf(Adresse.HOMME, Adresse.FEMME).flatMap { a -> Apparence.Accent.values().map { alias(a, it) } }

    /** Met l'icône à jour d'après les réglages ; sans effet si elle l'est déjà, ni hors d'un appareil. */
    fun appliquer(context: Context) = runCatching {
        val pm = context.packageManager
        val voulu = alias(Store.adresse(context), Store.accent(context))
        fun active(nom: String): Boolean = when (pm.getComponentEnabledSetting(ComponentName(context, nom))) {
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED -> true
            PackageManager.COMPONENT_ENABLED_STATE_DEFAULT -> nom == PAR_DEFAUT
            else -> false
        }
        fun regle(nom: String, oui: Boolean) {
            if (active(nom) == oui) return
            pm.setComponentEnabledSetting(
                ComponentName(context, nom),
                if (oui) PackageManager.COMPONENT_ENABLED_STATE_ENABLED else PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP,
            )
        }
        regle(voulu, true)
        tous().filter { it != voulu }.forEach { regle(it, false) }
    }
}
