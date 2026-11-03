package com.myhitbodedut

/**
 * Les morceaux que seule la version GitHub livre (29/10) : les niggounim dont
 * la provenance n'est pas établie (Ishay Ribo, sans accord, a quitté
 * l'application et le site le 29/10). Leurs fichiers sont dans
 * src/musique_hors_play/res/raw, que la construction Play (-Pplay=true)
 * n'inclut pas ; elle prend à la place
 * src/musique_play, où cette liste est vide. PUBLICATION_PLAY_STORE.md § 3.2.
 *
 * Vide depuis le 03/11 : les trente niggounim sans titre, de provenance
 * inconnue, sont retirés à la demande du propriétaire (historique git).
 */
internal object MusiqueHorsPlay {
    val MORCEAUX = emptyList<Bundled>()
}
