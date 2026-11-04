package com.myhitbodedut

/**
 * Le fichier de chaque livre (tools/livres_decoupe.py, 04/11), écrit ici plutôt que cherché par
 * son nom (getIdentifier) : la réduction des ressources de la version publiée retirerait un
 * fichier que rien ne nomme. LivresTest vérifie que la table suit l'index.
 */
object LivresBruts {
    val parId: Map<String, Int> = mapOf(
        "guide_hitbodedut" to R.raw.livre_guide_hitbodedut,
        "likoutei_etsot" to R.raw.livre_likoutei_etsot,
        "sefer_hamidot" to R.raw.livre_sefer_hamidot,
        "likoutei_moharan" to R.raw.livre_likoutei_moharan,
        "hichtapkhout" to R.raw.livre_hichtapkhout,
        "kokhvei_or" to R.raw.livre_kokhvei_or,
        "tefila_lishlomo" to R.raw.livre_tefila_lishlomo,
        "meguilat_esther" to R.raw.livre_meguilat_esther,
        "chir_hachirim" to R.raw.livre_chir_hachirim,
        "nichmat" to R.raw.livre_nichmat,
        "perek_chira" to R.raw.livre_perek_chira,
        "yalkut_yosef_kitsour" to R.raw.livre_yalkut_yosef_kitsour,
    )
}
