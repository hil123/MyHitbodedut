package com.myhitbodedut

/**
 * Les libellés que le calendrier, les zmanim et le siddour nomment par leur
 * clé (« fete_pessah », « ajout_yaale_veyavo », « zman_alot »). Une table
 * écrite plutôt que getIdentifier : la réduction des ressources de la
 * version publiée ne voit que les références directes, et retirerait le reste.
 *
 * Fichier produit par tools/libelles_construction.py ; LibellesTest vérifie
 * qu'aucune clé n'y manque.
 */
object Libelles {
    fun res(cle: String): Int? = when (cle) {
        "ajout_al_hanissim" -> R.string.ajout_al_hanissim
        "ajout_anenou_officiant" -> R.string.ajout_anenou_officiant
        "ajout_anenou_particulier" -> R.string.ajout_anenou_particulier
        "ajout_anenou_particulier_matin_minha" -> R.string.ajout_anenou_particulier_matin_minha
        "ajout_anenou_particulier_minha" -> R.string.ajout_anenou_particulier_minha
        "ajout_anenou_particulier_trois" -> R.string.ajout_anenou_particulier_trois
        "ajout_asseret_yemei_techouva" -> R.string.ajout_asseret_yemei_techouva
        "ajout_berakha" -> R.string.ajout_berakha
        "ajout_barekh_alenou" -> R.string.ajout_barekh_alenou
        "ajout_barkhenou" -> R.string.ajout_barkhenou
        "ajout_chabbat" -> R.string.ajout_chabbat
        "ajout_hallel" -> R.string.ajout_hallel
        "ajout_hallel_annonce" -> R.string.ajout_hallel_annonce
        "ajout_hallel_complet" -> R.string.ajout_hallel_complet
        "ajout_hallel_partiel_avec_berakha" -> R.string.ajout_hallel_partiel_avec_berakha
        "ajout_hallel_partiel_sans_berakha" -> R.string.ajout_hallel_partiel_sans_berakha
        "ajout_jeune" -> R.string.ajout_jeune
        "ajout_jeune_public" -> R.string.ajout_jeune_public
        "ajout_avinou_malkenou" -> R.string.ajout_avinou_malkenou
        "ajout_kriat_hatorah" -> R.string.ajout_kriat_hatorah
        "ajout_jour_1" -> R.string.ajout_jour_1
        "ajout_jour_2" -> R.string.ajout_jour_2
        "ajout_jour_3" -> R.string.ajout_jour_3
        "ajout_jour_4" -> R.string.ajout_jour_4
        "ajout_jour_5" -> R.string.ajout_jour_5
        "ajout_jour_6" -> R.string.ajout_jour_6
        "ajout_tsom_guedalia_tevet" -> R.string.ajout_tsom_guedalia_tevet
        "ajout_lendemain_kippour" -> R.string.ajout_lendemain_kippour
        "ajout_esther_pourim" -> R.string.ajout_esther_pourim
        "ajout_chiva_assar_tamouz" -> R.string.ajout_chiva_assar_tamouz
        "ajout_lundi_jeudi" -> R.string.ajout_lundi_jeudi
        "ajout_machiv_harouah" -> R.string.ajout_machiv_harouah
        "ajout_morid_hatal" -> R.string.ajout_morid_hatal
        "ajout_motsae_chabbat" -> R.string.ajout_motsae_chabbat
        "ajout_omer" -> R.string.ajout_omer
        "ajout_roch_hodech" -> R.string.ajout_roch_hodech
        "ajout_sans_tahanoun" -> R.string.ajout_sans_tahanoun
        "ajout_tahanoun" -> R.string.ajout_tahanoun
        "ajout_tal_oumatar" -> R.string.ajout_tal_oumatar
        "ajout_ticha_beav" -> R.string.ajout_ticha_beav
        "ajout_yaale_veyavo" -> R.string.ajout_yaale_veyavo
        "fete_chavouot" -> R.string.fete_chavouot
        "fete_chemini_atseret" -> R.string.fete_chemini_atseret
        "fete_chouchan_pourim" -> R.string.fete_chouchan_pourim
        "fete_chouchan_pourim_katan" -> R.string.fete_chouchan_pourim_katan
        "fete_erev_chavouot" -> R.string.fete_erev_chavouot
        "fete_erev_pessah" -> R.string.fete_erev_pessah
        "fete_erev_roch_hachana" -> R.string.fete_erev_roch_hachana
        "fete_erev_soukkot" -> R.string.fete_erev_soukkot
        "fete_erev_yom_kippour" -> R.string.fete_erev_yom_kippour
        "fete_hanouka" -> R.string.fete_hanouka
        "fete_hochaana_rabba" -> R.string.fete_hochaana_rabba
        "fete_hol_hamoed_pessah" -> R.string.fete_hol_hamoed_pessah
        "fete_hol_hamoed_soukkot" -> R.string.fete_hol_hamoed_soukkot
        "fete_isrou_hag" -> R.string.fete_isrou_hag
        "fete_jeune_esther" -> R.string.fete_jeune_esther
        "fete_jeune_guedalia" -> R.string.fete_jeune_guedalia
        "fete_lag_baomer" -> R.string.fete_lag_baomer
        "fete_pessah" -> R.string.fete_pessah
        "fete_pessah_cheni" -> R.string.fete_pessah_cheni
        "fete_pourim" -> R.string.fete_pourim
        "fete_pourim_ailleurs" -> R.string.fete_pourim_ailleurs
        "fete_pourim_jerusalem" -> R.string.fete_pourim_jerusalem
        "fete_pourim_katan" -> R.string.fete_pourim_katan
        "fete_roch_hachana" -> R.string.fete_roch_hachana
        "fete_roch_hodech" -> R.string.fete_roch_hodech
        "fete_simhat_torah" -> R.string.fete_simhat_torah
        "fete_soukkot" -> R.string.fete_soukkot
        "fete_tamouz_17" -> R.string.fete_tamouz_17
        "fete_tevet_10" -> R.string.fete_tevet_10
        "fete_ticha_beav" -> R.string.fete_ticha_beav
        "fete_tou_beav" -> R.string.fete_tou_beav
        "fete_tou_bichvat" -> R.string.fete_tou_bichvat
        "fete_yom_haatsmaout" -> R.string.fete_yom_haatsmaout
        "fete_yom_hachoah" -> R.string.fete_yom_hachoah
        "fete_yom_hazikaron" -> R.string.fete_yom_hazikaron
        "fete_yom_kippour" -> R.string.fete_yom_kippour
        "fete_yom_yerouchalayim" -> R.string.fete_yom_yerouchalayim
        "zman_allumage" -> R.string.zman_allumage
        "zman_alot" -> R.string.zman_alot
        "zman_chatsot" -> R.string.zman_chatsot
        "zman_chatsot_layla" -> R.string.zman_chatsot_layla
        "zman_chkia" -> R.string.zman_chkia
        "zman_methode_apres_tset" -> R.string.zman_methode_apres_tset
        "zman_methode_avant_chkia" -> R.string.zman_methode_avant_chkia
        "zman_methode_degres" -> R.string.zman_methode_degres
        "zman_methode_gra" -> R.string.zman_methode_gra
        "zman_methode_meridien" -> R.string.zman_methode_meridien
        "zman_methode_mga_72" -> R.string.zman_methode_mga_72
        "zman_methode_milieu_chatsot" -> R.string.zman_methode_milieu_chatsot
        "zman_methode_minutes" -> R.string.zman_methode_minutes
        "zman_methode_niveau_mer" -> R.string.zman_methode_niveau_mer
        "zman_minha_gedola" -> R.string.zman_minha_gedola
        "zman_minha_ketana" -> R.string.zman_minha_ketana
        "zman_misheyakir" -> R.string.zman_misheyakir
        "zman_netz" -> R.string.zman_netz
        "zman_plag" -> R.string.zman_plag
        "zman_sortie" -> R.string.zman_sortie
        "zman_szks_gra" -> R.string.zman_szks_gra
        "zman_szks_mga" -> R.string.zman_szks_mga
        "zman_szt_gra" -> R.string.zman_szt_gra
        "zman_szt_mga" -> R.string.zman_szt_mga
        "zman_tset" -> R.string.zman_tset
        "zman_tset_rt" -> R.string.zman_tset_rt
        else -> null
    }
}
