package com.myhitbodedut

/** Un morceau livré avec l'application : son fichier dans res/raw et son titre. */
internal data class Bundled(val id: String, val rawRes: Int, val titleRes: Int)

/**
 * Les morceaux que la version Play livre (29/10) : ceux dont le droit de
 * diffusion publique est établi — le niggoun du Yanuka (accord du Yanuka),
 * המלך בשדה (déclaré libre de droits) et les morceaux de נעם שפייר (accord
 * écrit, 29/10). PUBLICATION_PLAY_STORE.md § 3.2.
 */
internal object MusiquePlay {
    val MORCEAUX = listOf(
        Bundled("hamelech_basade", R.raw.hamelech_basade, R.string.track_hamelech_basade),
        Bundled("yanuka_nigoun_3", R.raw.yanuka_nigoun_3, R.string.track_yanuka_nigoun_3),
        Bundled("shpayer_beyond", R.raw.shpayer_beyond, R.string.track_shpayer_beyond),
        Bundled("shpayer_sincerity", R.raw.shpayer_sincerity, R.string.track_shpayer_sincerity),
        Bundled("shpayer_awakening", R.raw.shpayer_awakening, R.string.track_shpayer_awakening),
        Bundled("shpayer_inward", R.raw.shpayer_inward, R.string.track_shpayer_inward),
        Bundled("shpayer_even", R.raw.shpayer_even, R.string.track_shpayer_even),
        Bundled("shpayer_fire_kamanche", R.raw.shpayer_fire_kamanche, R.string.track_shpayer_fire_kamanche),
        Bundled("shpayer_tunnels_end", R.raw.shpayer_tunnels_end, R.string.track_shpayer_tunnels_end),
        Bundled("shpayer_eshet_chail", R.raw.shpayer_eshet_chail, R.string.track_shpayer_eshet_chail),
        Bundled("shpayer_arba_bavot", R.raw.shpayer_arba_bavot, R.string.track_shpayer_arba_bavot),
    )
}
