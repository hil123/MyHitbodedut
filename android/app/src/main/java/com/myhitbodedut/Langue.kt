package com.myhitbodedut

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import androidx.appcompat.app.AppCompatDelegate

/**
 * Quelles langues un écran a le droit d'afficher.
 *
 * La règle, donnée par le propriétaire le 22/09 : en hébreu, **rien que de
 * l'hébreu** ; en anglais, l'hébreu et l'anglais ; en français, l'hébreu et
 * le français. Jamais les trois ensemble, et jamais une langue qui n'est ni
 * l'hébreu ni celle de l'interface.
 *
 * Ce que cela change de l'ancien comportement : il n'y a **plus de repli
 * d'une langue sur une autre**. Une note écrite en français ne s'affichait
 * pas en français faute de mieux — elle s'affichait parce que rien
 * n'interdisait de le faire. Ici, un texte dont la langue n'est pas permise
 * n'est pas montré. C'est visible, et c'est réparable en le traduisant ;
 * l'afficher dans la mauvaise langue ne se voyait pas.
 */
object RegleLangue {

    /** L'hébreu est toujours permis : c'est la langue des textes eux-mêmes. */
    fun permises(interfaceLangue: String): Set<String> = when (normalise(interfaceLangue)) {
        "he" -> setOf("he")
        else -> setOf("he", normalise(interfaceLangue))
    }

    fun permet(interfaceLangue: String, langueDuTexte: String): Boolean =
        normalise(langueDuTexte) in permises(interfaceLangue)

    /** Montre-t-on une traduction à côté de l'hébreu ? Non, en hébreu. */
    fun traduit(interfaceLangue: String): Boolean = normalise(interfaceLangue) != "he"

    /**
     * Le texte à montrer parmi ceux qu'on a, ou null.
     *
     * `textes` est indexé par langue. On ne rend que celui de la langue de
     * l'interface — pas de repli, c'est tout l'objet de la règle.
     */
    fun choisit(interfaceLangue: String, textes: Map<String, String?>): String? {
        val langue = normalise(interfaceLangue)
        return textes[langue]?.takeIf { it.isNotBlank() }
    }

    /** « iw » est l'ancien code d'Android pour l'hébreu, encore rendu. */
    fun normalise(etiquette: String?): String {
        val brute = etiquette.orEmpty().substringBefore('-').substringBefore('_').lowercase()
        return if (brute == "iw") "he" else brute
    }
}

/**
 * La langue choisie dans l'application, appliquée aussi hors des écrans.
 *
 * Avant Android 13, AppCompat n'applique ce choix qu'à ses activités : les
 * services, les receveurs et la vignette gardaient la langue du téléphone.
 * Un téléphone en français avec l'application en anglais montrait donc
 * l'écran de blocage, les notifications et la vignette en français (audit
 * des traductions, 28/09). Depuis Android 13 le système s'en charge seul.
 *
 * AppCompat ne relit son choix enregistré qu'à l'ouverture d'une activité :
 * un service réveillé seul le trouverait vide. D'où la copie dans Store.
 */
object LangueApp {

    fun envelopper(base: Context): Context {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) return base
        val etiquettes = AppCompatDelegate.getApplicationLocales().toLanguageTags()
            .ifBlank { Store.langueApp(base) }
        if (etiquettes.isBlank()) return base
        val config = Configuration(base.resources.configuration)
        config.setLocales(LocaleList.forLanguageTags(etiquettes))
        return base.createConfigurationContext(config)
    }

    /** Recopie le choix d'AppCompat, lisible dans une activité. */
    fun retenir(context: Context) {
        Store.setLangueApp(context, AppCompatDelegate.getApplicationLocales().toLanguageTags())
    }
}
