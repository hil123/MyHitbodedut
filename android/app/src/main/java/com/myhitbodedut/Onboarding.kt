package com.myhitbodedut

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes

/** Un écran d'accueil au premier lancement. */
data class OnboardingEcran(
    val cle: String,
    @StringRes val titre: Int,
    @StringRes val corps: Int,
    @DrawableRes val illustration: Int,
    /** L'écran porte les deux boutons צדיק / צדקת. */
    val choixAdresse: Boolean = false
)

/**
 * Les écrans du premier lancement : ce qu'est la hitbodedout, puis ce que
 * fait l'application, une fonction par écran, et enfin sa promesse.
 *
 * Une application de pratique religieuse qui s'ouvre sur un tableau de
 * bord muet demande à celui qui l'installe de deviner ce qu'on attend de
 * lui. Le dernier écran est le seul que personne d'autre ne peut dire à sa
 * place : cette application n'a aucun accès à Internet, et ce qu'on y écrit
 * ne quitte pas le téléphone. Un test tient cette phrase au manifeste — si
 * la permission INTERNET y apparaissait un jour, l'écran deviendrait un
 * mensonge, et le test tombe avant.
 *
 * On peut passer, et on ne les revoit plus, sauf depuis les réglages.
 */
object Onboarding {

    /**
     * À augmenter quand la présentation change assez pour être remontrée à
     * tous. 1 : le booléen de la v1.96 ; 2 : l'adresse n'est plus demandée
     * qu'ici, et la présentation revient aux installations existantes.
     */
    const val VERSION = 2

    val ECRANS: List<OnboardingEcran> = listOf(
        OnboardingEcran(
            "pratique",
            R.string.onboarding_practice,
            R.string.onboarding_practice_body,
            R.drawable.char_open_arms
        ),
        OnboardingEcran(
            "seance",
            R.string.onboarding_session,
            R.string.onboarding_session_body,
            R.drawable.char_praying
        ),
        OnboardingEcran(
            "jours",
            R.string.onboarding_days,
            R.string.onboarding_days_body,
            R.drawable.char_writing
        ),
        OnboardingEcran(
            "textes",
            R.string.onboarding_texts,
            R.string.onboarding_texts_body,
            R.drawable.char_tehilim
        ),
        OnboardingEcran(
            "reglages",
            R.string.onboarding_yours,
            R.string.onboarding_yours_body,
            R.drawable.char_pointing
        ),
        OnboardingEcran(
            "adresse",
            R.string.onboarding_adresse,
            R.string.onboarding_adresse_body,
            R.drawable.char_dancing,
            choixAdresse = true
        ),
        OnboardingEcran(
            "prive",
            R.string.onboarding_private,
            R.string.onboarding_private_body,
            R.drawable.char_thumbs_up
        )
    )

    val DERNIER: Int = ECRANS.lastIndex

    fun ecran(index: Int): OnboardingEcran = ECRANS[index.coerceIn(0, DERNIER)]

    fun suivant(index: Int): Int = (index + 1).coerceIn(0, DERNIER)

    fun precedent(index: Int): Int = (index - 1).coerceIn(0, DERNIER)

    fun estPremier(index: Int): Boolean = index <= 0

    fun estDernier(index: Int): Boolean = index >= DERNIER
}
