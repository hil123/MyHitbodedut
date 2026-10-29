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
    )
}
