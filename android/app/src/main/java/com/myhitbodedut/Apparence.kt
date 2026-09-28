package com.myhitbodedut

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.util.TypedValue
import android.view.ContextThemeWrapper
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import androidx.annotation.StyleRes
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import java.util.WeakHashMap

/**
 * Les couleurs de l'écran, choisies dans les réglages : clair ou sombre, la
 * couleur du fond, celle des boutons.
 *
 * Des palettes et non un nuancier libre : chaque combinaison est vérifiée
 * par PaletteTest, de jour comme de nuit, pour que le texte reste lisible.
 * Un nuancier libre laisserait choisir du jaune pâle sur du blanc.
 *
 * Le vert d'origine n'a pas de style : c'est le thème de base. Une palette
 * choisie recouvre ses attributs (values/attrs.xml) au moment où chaque
 * écran se crée, avant qu'il ne gonfle sa mise en page.
 */
object Apparence {

    enum class Mode(val cle: String, @StringRes val nom: Int, val nuit: Int) {
        AUTO("auto", R.string.mode_auto, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM),
        CLAIR("clair", R.string.mode_clair, AppCompatDelegate.MODE_NIGHT_NO),
        SOMBRE("sombre", R.string.mode_sombre, AppCompatDelegate.MODE_NIGHT_YES);

        companion object {
            fun parse(cle: String?): Mode = values().firstOrNull { it.cle == cle } ?: AUTO
        }
    }

    /** Le fond, les cartes et l'encre. `style` vaut 0 pour la palette d'origine. */
    enum class Fond(val cle: String, @StringRes val nom: Int, @StyleRes val style: Int) {
        VERT("vert", R.string.fond_vert, 0),
        BLANC("blanc", R.string.fond_blanc, R.style.Fond_Blanc),
        SABLE("sable", R.string.fond_sable, R.style.Fond_Sable),
        BLEU("bleu", R.string.fond_bleu, R.style.Fond_Bleu),
        ROSE("rose", R.string.fond_rose, R.style.Fond_Rose);

        companion object {
            fun parse(cle: String?): Fond = values().firstOrNull { it.cle == cle } ?: VERT
        }
    }

    /** Les boutons, les titres et les bandeaux. `style` vaut 0 pour la palette d'origine. */
    enum class Accent(val cle: String, @StringRes val nom: Int, @StyleRes val style: Int) {
        VERT("vert", R.string.accent_vert, 0),
        BLEU("bleu", R.string.accent_bleu, R.style.Accent_Bleu),
        VIOLET("violet", R.string.accent_violet, R.style.Accent_Violet),
        BORDEAUX("bordeaux", R.string.accent_bordeaux, R.style.Accent_Bordeaux),
        AMBRE("ambre", R.string.accent_ambre, R.style.Accent_Ambre),
        TURQUOISE("turquoise", R.string.accent_turquoise, R.style.Accent_Turquoise);

        companion object {
            fun parse(cle: String?): Accent = values().firstOrNull { it.cle == cle } ?: VERT
        }
    }

    /**
     * Les couleurs réglables et l'attribut qui les porte. Une couleur absente
     * d'ici (blanc, orange d'alerte, jour exempté) est la même partout.
     */
    private val ATTRIBUTS: Map<Int, Int> = mapOf(
        R.color.brand to R.attr.mhBrand,
        R.color.brand_dark to R.attr.mhBrandDark,
        R.color.mint to R.attr.mhMint,
        R.color.on_brand to R.attr.mhOnBrand,
        R.color.on_plate to R.attr.mhOnPlate,
        R.color.stage_soft to R.attr.mhStageSoft,
        R.color.day_done to R.attr.mhDayDone,
        R.color.paper to R.attr.mhPaper,
        R.color.card to R.attr.mhCard,
        R.color.line to R.attr.mhLine,
        R.color.ink to R.attr.mhInk,
        R.color.muted to R.attr.mhMuted,
        R.color.day_todo to R.attr.mhDayTodo,
    )

    /** La couleur telle que l'écran la montre, palette choisie comprise. */
    @ColorInt
    fun couleur(context: Context, @ColorRes id: Int): Int {
        val attribut = ATTRIBUTS[id] ?: return ContextCompat.getColor(context, id)
        val valeur = TypedValue()
        if (!context.theme.resolveAttribute(attribut, valeur, true)) {
            return ContextCompat.getColor(context, id)
        }
        return if (valeur.resourceId != 0) ContextCompat.getColor(context, valeur.resourceId)
        else valeur.data
    }

    /** Ce qui distingue deux thèmes appliqués ; le mode sombre, lui, est suivi par AppCompat. */
    private fun signature(context: Context): String =
        "${Store.fond(context).cle}/${Store.accent(context).cle}"

    private fun appliquer(activity: Activity) {
        Store.fond(activity).style.takeIf { it != 0 }?.let { activity.theme.applyStyle(it, true) }
        Store.accent(activity).style.takeIf { it != 0 }?.let { activity.theme.applyStyle(it, true) }
    }

    /**
     * Un contexte aux couleurs choisies, pour ce qui s'affiche hors d'un écran
     * de l'application : l'écran de blocage est posé par un service, qui n'a
     * ni le thème de l'app, ni le mode sombre réglé par AppCompat.
     */
    fun contexte(base: Context): Context {
        val mode = Store.modeApparence(base)
        val configure = if (mode == Mode.AUTO) base else {
            val conf = Configuration(base.resources.configuration)
            val nuit = if (mode == Mode.SOMBRE) Configuration.UI_MODE_NIGHT_YES
            else Configuration.UI_MODE_NIGHT_NO
            conf.uiMode = (conf.uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or nuit
            base.createConfigurationContext(conf)
        }
        return ContextThemeWrapper(configure, R.style.Theme_MyHitbodedut).apply {
            Store.fond(base).style.takeIf { it != 0 }?.let { theme.applyStyle(it, true) }
            Store.accent(base).style.takeIf { it != 0 }?.let { theme.applyStyle(it, true) }
        }
    }

    /** Le mode clair ou sombre vaut pour toute l'application, et recrée les écrans ouverts. */
    fun appliquerMode(context: Context) {
        AppCompatDelegate.setDefaultNightMode(Store.modeApparence(context).nuit)
    }

    /**
     * À l'ouverture de l'application. Chaque écran reçoit la palette en se
     * créant ; un écran resté ouvert sous les réglages pendant qu'on changeait
     * de palette se recrée en reprenant la main.
     */
    fun installer(application: Application) {
        appliquerMode(application)
        val appliquees = WeakHashMap<Activity, String>()
        application.registerActivityLifecycleCallbacks(object :
            Application.ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                appliquer(activity)
                appliquees[activity] = signature(activity)
            }

            override fun onActivityResumed(activity: Activity) {
                val appliquee = appliquees[activity] ?: return
                if (appliquee != signature(activity)) activity.recreate()
            }

            override fun onActivityDestroyed(activity: Activity) {
                appliquees.remove(activity)
            }

            override fun onActivityStarted(activity: Activity) = Unit
            override fun onActivityPaused(activity: Activity) = Unit
            override fun onActivityStopped(activity: Activity) = Unit
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
        })
    }
}
