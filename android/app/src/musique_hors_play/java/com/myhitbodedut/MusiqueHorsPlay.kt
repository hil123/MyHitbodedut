package com.myhitbodedut

/**
 * Les morceaux que seule la version GitHub livre (29/10) : les niggounim dont
 * la provenance n'est pas établie (Ishay Ribo, sans accord, a quitté
 * l'application et le site le 29/10). Leurs fichiers sont dans
 * src/musique_hors_play/res/raw, que la construction Play (-Pplay=true)
 * n'inclut pas ; elle prend à la place
 * src/musique_play, où cette liste est vide. PUBLICATION_PLAY_STORE.md § 3.2.
 */
internal object MusiqueHorsPlay {
    val MORCEAUX = listOf(
        Bundled("niggoun_01", R.raw.niggoun_01, R.string.track_niggoun_01),
        Bundled("niggoun_02", R.raw.niggoun_02, R.string.track_niggoun_02),
        Bundled("niggoun_03", R.raw.niggoun_03, R.string.track_niggoun_03),
        Bundled("niggoun_04", R.raw.niggoun_04, R.string.track_niggoun_04),
        Bundled("niggoun_05", R.raw.niggoun_05, R.string.track_niggoun_05),
        Bundled("niggoun_06", R.raw.niggoun_06, R.string.track_niggoun_06),
        Bundled("niggoun_07", R.raw.niggoun_07, R.string.track_niggoun_07),
        Bundled("niggoun_10", R.raw.niggoun_10, R.string.track_niggoun_10),
        Bundled("niggoun_11", R.raw.niggoun_11, R.string.track_niggoun_11),
        Bundled("niggoun_12", R.raw.niggoun_12, R.string.track_niggoun_12),
        Bundled("niggoun_13", R.raw.niggoun_13, R.string.track_niggoun_13),
        Bundled("niggoun_14", R.raw.niggoun_14, R.string.track_niggoun_14),
        Bundled("niggoun_15", R.raw.niggoun_15, R.string.track_niggoun_15),
        Bundled("niggoun_17", R.raw.niggoun_17, R.string.track_niggoun_17),
        Bundled("niggoun_18", R.raw.niggoun_18, R.string.track_niggoun_18),
        Bundled("niggoun_19", R.raw.niggoun_19, R.string.track_niggoun_19),
        Bundled("niggoun_20", R.raw.niggoun_20, R.string.track_niggoun_20),
        Bundled("niggoun_21", R.raw.niggoun_21, R.string.track_niggoun_21),
        Bundled("niggoun_22", R.raw.niggoun_22, R.string.track_niggoun_22),
        Bundled("niggoun_23", R.raw.niggoun_23, R.string.track_niggoun_23),
        Bundled("niggoun_24", R.raw.niggoun_24, R.string.track_niggoun_24),
        Bundled("niggoun_25", R.raw.niggoun_25, R.string.track_niggoun_25),
        Bundled("niggoun_26", R.raw.niggoun_26, R.string.track_niggoun_26),
        Bundled("niggoun_27", R.raw.niggoun_27, R.string.track_niggoun_27),
        Bundled("niggoun_28", R.raw.niggoun_28, R.string.track_niggoun_28),
        Bundled("niggoun_29", R.raw.niggoun_29, R.string.track_niggoun_29),
        Bundled("niggoun_30", R.raw.niggoun_30, R.string.track_niggoun_30),
        Bundled("niggoun_31", R.raw.niggoun_31, R.string.track_niggoun_31),
        Bundled("niggoun_32", R.raw.niggoun_32, R.string.track_niggoun_32),
        Bundled("niggoun_33", R.raw.niggoun_33, R.string.track_niggoun_33),
    )
}
