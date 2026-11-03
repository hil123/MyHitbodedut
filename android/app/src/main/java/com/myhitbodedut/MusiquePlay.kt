package com.myhitbodedut

/** Un morceau livré avec l'application : son fichier dans res/raw et son titre. */
internal data class Bundled(val id: String, val rawRes: Int, val titleRes: Int)

/**
 * Les morceaux que la version Play livre (29/10) : ceux dont le droit de
 * diffusion publique est établi — le niggoun du Yanuka (accord du Yanuka),
 * המלך בשדה (déclaré libre de droits), et les morceaux de נעם שפייר et de
 * יניר קילינסקי (Ianir), accord écrit du 29/10. PUBLICATION_PLAY_STORE.md § 3.2.
 * Les nouveaux vont en fin de liste : le morceau par défaut ne change pas.
 */
internal object MusiquePlay {
    val MORCEAUX = listOf(
        Bundled("hamelech_basade", R.raw.hamelech_basade, R.string.track_hamelech_basade),
        Bundled("yanuka_nigoun_3", R.raw.yanuka_nigoun_3, R.string.track_yanuka_nigoun_3),
        Bundled("shpayer_beyond", R.raw.shpayer_beyond, R.string.track_shpayer_beyond),
        Bundled("shpayer_eshet_hayil", R.raw.shpayer_eshet_hayil, R.string.track_shpayer_eshet_hayil),
        Bundled("shpayer_arba_bavot", R.raw.shpayer_arba_bavot, R.string.track_shpayer_arba_bavot),
        Bundled("shpayer_afilou", R.raw.shpayer_afilou, R.string.track_shpayer_afilou),
        Bundled("shpayer_kenout", R.raw.shpayer_kenout, R.string.track_shpayer_kenout),
        Bundled("shpayer_esh_kamanche", R.raw.shpayer_esh_kamanche, R.string.track_shpayer_esh_kamanche),
        Bundled("shpayer_hithadshout", R.raw.shpayer_hithadshout, R.string.track_shpayer_hithadshout),
        Bundled("shpayer_pnima", R.raw.shpayer_pnima, R.string.track_shpayer_pnima),
        Bundled("shpayer_tunnels_end", R.raw.shpayer_tunnels_end, R.string.track_shpayer_tunnels_end),
        Bundled("shpayer_ki_hine_kahomer", R.raw.shpayer_ki_hine_kahomer, R.string.track_shpayer_ki_hine_kahomer),
        Bundled("ianir_mirage", R.raw.ianir_mirage, R.string.track_ianir_mirage),
        Bundled("ianir_al_harega", R.raw.ianir_al_harega, R.string.track_ianir_al_harega),
        Bundled("ianir_hello", R.raw.ianir_hello, R.string.track_ianir_hello),
        Bundled("ianir_baal_shem_tov", R.raw.ianir_baal_shem_tov, R.string.track_ianir_baal_shem_tov),
        Bundled("ianir_zemer_bishvahin", R.raw.ianir_zemer_bishvahin, R.string.track_ianir_zemer_bishvahin),
        Bundled("ianir_berditchev", R.raw.ianir_berditchev, R.string.track_ianir_berditchev),
        Bundled("ianir_kol_bayaar", R.raw.ianir_kol_bayaar, R.string.track_ianir_kol_bayaar),
        Bundled("ianir_yohanan_hasandlar", R.raw.ianir_yohanan_hasandlar, R.string.track_ianir_yohanan_hasandlar),
        Bundled("ianir_haleluhu", R.raw.ianir_haleluhu, R.string.track_ianir_haleluhu),
        Bundled("ianir_houppa", R.raw.ianir_houppa, R.string.track_ianir_houppa),
        Bundled("ianir_eden_el_salvador", R.raw.ianir_eden_el_salvador, R.string.track_ianir_eden_el_salvador),
        Bundled("ianir_hakol_milfanekha", R.raw.ianir_hakol_milfanekha, R.string.track_ianir_hakol_milfanekha),
        Bundled("ianir_kabbalat_chabbat", R.raw.ianir_kabbalat_chabbat, R.string.track_ianir_kabbalat_chabbat),
        Bundled("ianir_bessarabie", R.raw.ianir_bessarabie, R.string.track_ianir_bessarabie),
    )
}
