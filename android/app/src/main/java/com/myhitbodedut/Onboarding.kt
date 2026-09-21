package com.myhitbodedut

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes

/** Un écran d'accueil au premier lancement. */
data class OnboardingEcran(
    val cle: String,
    @StringRes val titre: Int,
    @StringRes val corps: Int,
    @DrawableRes val illustration: Int
)

/**
 * Les trois écrans du premier lancement.
 *
 * Une application de pratique religieuse qui s'ouvre sur un tableau de
 * bord muet demande à celui qui l'installe de deviner ce qu'on attend de
 * lui. Trois écrans suffisent, et le troisième est le seul que personne
 * d'autre ne peut dire à sa place : cette application n'a aucun accès à
 * Internet, et ce qu'on y écrit ne quitte pas le téléphone. Un test tient
 * cette phrase au manifeste — si la permission INTERNET y apparaissait un
 * jour, l'écran deviendrait un mensonge, et le test tombe avant.
 *
 * On peut passer, et on ne les revoit plus.
 */
object Onboarding {

    val ECRANS: List<OnboardingEcran> = listOf(
        OnboardingEcran(
            "pratique",
            R.string.onboarding_practice,
            R.string.onboarding_practice_body,
            R.drawable.char_open_arms
        ),
        OnboardingEcran(
            "app",
            R.string.onboarding_app,
            R.string.onboarding_app_body,
            R.drawable.char_walking
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
