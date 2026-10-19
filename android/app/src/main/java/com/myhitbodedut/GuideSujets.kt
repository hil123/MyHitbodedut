package com.myhitbodedut

import android.content.Context
import org.json.JSONObject

/**
 * La hitbodedout guidée selon le sujet (proposition 2 de la feuille de route,
 * 19/10) : pour chaque sujet, une invitation par temps, sous l'invitation
 * générale du temps (res/raw/guide_sujets.json). « Autre chose » et l'absence
 * de sujet n'en ont pas : le guide reste celui de toujours.
 */
object GuideSujets {

    @Volatile private var donnees: JSONObject? = null

    private fun sujets(context: Context): JSONObject =
        (donnees ?: JSONObject(Json.readRaw(context.applicationContext, R.raw.guide_sujets)).also { donnees = it })
            .getJSONObject("sujets")

    /** L'invitation du temps [cle] pour [theme], dans [langue] ; null sans elle. */
    fun invitation(context: Context, theme: Theme?, cle: String, langue: String): String? =
        invitation(sujets(context), theme, cle, langue)

    /** La même, sur des données déjà lues : vérifiable sans appareil. */
    fun invitation(sujets: JSONObject, theme: Theme?, cle: String, langue: String): String? {
        if (theme == null) return null
        val temps = sujets.optJSONObject(theme.cle)?.optJSONObject(cle) ?: return null
        return temps.optString(if (langue == "he" || langue == "iw") "he" else langue, "").ifBlank { null }
    }
}
