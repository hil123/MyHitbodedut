# Fiche avant dépense — plans cinéma (AUCUN CRÉDIT DÉPENSÉ, GO requis)

Plans aujourd'hui PROVISOIRES (dessin vectoriel + illustrations de l'app) : scènes 1, 2, 3, 4, 10, 11.

| Plan | Durée | Image de départ | Outil proposé | Critère de réussite |
|---|---|---|---|---|
| S1 rue de Jérusalem, foule pressée, travelling avant | 8 s | photo de rue de Jérusalem (pierre blonde) | Seedance 2.0 (Higgsfield), 720p d'abord | pierre de Jérusalem lisible, passants de dos ou flous, aucun visage net |
| S2 homme seul qui marche, contemplatif | 8 s | image du personnage (chapeau noir, péot, tsitsit) | idem | voir question visage ci-dessous |
| S3 départ vers les collines de Judée, coucher de soleil, plan aérien | 9 s | photo collines de Judée | idem | de dos, silhouette lointaine |
| S4 seul sur la colline, mains levées, paumes ouvertes | 12 s (2 clips) | dernière image de S3 | idem | contre-jour, mains nettes |
| S10 table de Chabbat, bougies, hallot couvertes, coupe de Kiddouch | 3,5 s | image de table | idem | bougies allumées, hallot couvertes |
| S11 colline au lever du soleil, mains ouvertes | 6 s | dernière image de S4 | idem | silhouette en contre-jour |

- Coût : à lire dans Higgsfield avant lancement (solde partagé, relu à chaque session) ; arrêt si +10 %.
- Plan B : photos libres de droits animées en parallaxe 2,5D (0 crédit).
- Voix finale : ElevenLabs sur GO ; aujourd'hui voix témoin edge-tts (fr-FR-HenriNeural).
- Musique finale : aujourd'hui MUSIQUE TÉMOIN composée par script (`outils/musique_temoin.py`, sans droits tiers). Les morceaux du dossier `musique/` de l'app (Shneor Lerer, Yanuka, Karduner…) ne sont PAS utilisés : pas de licence publicitaire documentée.

## Musique (01/11)
`remotion/public/musique/musique_nigoun.wav` : composition originale par script (`outils/musique_nigoun.py`), dans le mode Ahava Rabba sur ré. Rendue avec FluidSynth et la banque de sons FluidR3_GM (Frank Wen, licence MIT). Aucun droit tiers. Elle remplace la musique témoin synthétique.

## Trophées (01/11)
Captures réelles de l'app web : `web_trophees_semaine`, `web_trophees_haut`, `web_accueil_serie`. Pour montrer une progression, l'app a été remplie avec un historique de démonstration : 24 journées de 15 à 30 min sur 4 semaines, Chabbat exclus (script `troph2.js`). Les trophées, le grade et la série affichés sont calculés par l'app elle-même à partir de cet historique.

## Musique n° 2 (01/11)
`musique_lumiere.wav` : composition originale par script (`outils/musique_lumiere.py`), en ré majeur, avec une clarinette hassidique. Rendue avec FluidSynth et FluidR3_GM (licence MIT). Elle remplace `musique_nigoun.wav`.

## Calendrier (01/11)
Scène 5 : « Le calendrier juif » remplace « Respect du Shabbat ». Voix `s10c`. Captures réelles de l'app web (lieu : Jérusalem) : `web_calendrier`, `web_zmanim`.

## Musique v10 (01/11) — enregistrement existant, domaine public
- Œuvre : Johannes Brahms (1833–1897), Symphonie n° 3 en fa majeur, op. 90, III. Poco allegretto. Composition dans le domaine public.
- Enregistrement : Musopen Symphony (projet Kickstarter Musopen 2012), publié dans le domaine public (Creative Commons Public Domain Mark 1.0).
- Source : https://archive.org/details/musopen-dvd (licence affichée : http://creativecommons.org/publicdomain/mark/1.0/). Même enregistrement sur Wikimedia Commons, marqué « Public domain » : File:Brahms - Symphony No. 3 in F major, Op. 90 - III. Poco allegretto (Musopen Symphony).flac
- Montage : 0:00–1:15 du mouvement (thème au violoncelle, puis aux violons), fondu enchaîné vers 5:44–6:02 (accords finaux), fondu de sortie 86–90 s. Aucune crédit obligatoire.
- Alternative téléchargée : Dvořák, Quatuor « Américain » op. 96, II. Lento (Musopen String Quartet, même licence) : sources_musique/dvorak_americain_lento.mp3.

## Musiques v11 (01/11) — 4 enregistrements existants, domaine public
Tous issus du DVD Musopen (projet Kickstarter 2012), publiés dans le domaine public : Creative Commons Public Domain Mark 1.0,
https://archive.org/details/musopen-dvd. Compositeurs morts depuis plus de 70 ans. Aucun crédit obligatoire.
Montage automatique (outils/monter_musique.py) : début du mouvement jusqu'à un creux de fin de phrase (~70–77 s),
fondu enchaîné de 1,5 s vers la fin réelle du mouvement, fondu de sortie 86,5–90 s.
- m_dvorak_americain_lento : Dvořák (1841–1904), Quatuor à cordes n° 12 « Américain », op. 96, II. Lento. Coupe 73 s ; fin 448,5–468 s.
- m_mendelssohn_quatuor6_adagio : Mendelssohn (1809–1847), Quatuor n° 6 en fa mineur, op. 80, III. Adagio. Coupe 73 s ; fin 465–484 s.
- m_schubert_d958_adagio : Schubert (1797–1828), Sonate pour piano en ut mineur, D. 958, II. Adagio. Coupe 70 s ; fin 438,5–461 s.
- m_brahms1_andante : Brahms (1833–1897), Symphonie n° 1, op. 68, II. Andante sostenuto. Coupe 76,5 s ; fin 498–515 s.
Choix au rendu : --props='{"musique":"m_dvorak_americain_lento"}' (par défaut : Dvořák).

## Musique v11 (01/11) — deux musiques et un déclic (demande de Hillel)
Script : outils/musique_deux_temps.py. Fichier : remotion/public/musique/musique_deux_temps.wav (choix par défaut du rendu).
- 0–14,0 s, stress et panique : Mendelssohn (1809–1847), Quatuor à cordes n° 6 en fa mineur, op. 80, I. Allegro vivace assai.
  Enregistrement Musopen String Quartet, Public Domain Mark 1.0 (https://archive.org/details/musopen-dvd).
- 14,33 s, déclic sur l'écran MY HITBODEDUT : son synthétisé par le script (souffle montant + carillon), aucun droit.
- 14,33–90 s, joie intense : Mendelssohn, Symphonie n° 4 « Italienne », op. 90, I. Allegro vivace.
  Enregistrement Musopen Symphony, Public Domain Mark 1.0 (même source). 0–57,3 s du mouvement, fondu enchaîné d'1 s vers
  ses 19,4 dernières secondes (accords finaux sur le logo).
Aucun crédit obligatoire.

## Son v12 (01/11) — foule, coupe nette, annonce, « Beyond »
- 0–14,33 s : bruits de rue et brouhaha de foule — Red Library « Crowds Outdoor » (USC Cinema / Sunset Editorial),
  licence CC0 1.0 (https://archive.org/details/Red_Library_Crowds_Outdoor) : R07-52 Busy City Ambience, R08-02 Outdoor Crowd,
  R08-06 Outdoor Voices. La pression monte jusqu'à la coupe nette à 14,33 s. Fichier : musique/ambiance_foule.wav.
  v14 (« trop agressif ») : Outdoor Voices retiré, filtre 150 Hz–3,8 kHz, niveau −8 dB, baisse plus forte sous la voix.
- 14,33–14,96 s : silence. 14,73 s : la voix annonce « MY HITBODEDUT. » (s05a), puis 16,7 s « Pour t'accompagner dans cette démarche. » (s05b).
- 16,07–90 s : « Beyond », Noam Shpayer (נעם שפייר), fichier de l'app (musique/shpayer_beyond.m4a).
  Droits : accord écrit du 29/10 noté dans MyHitbodedut/PUBLICATION_PLAY_STORE.md § 3.2 (« Livré (accord écrit) »).
  Preuve : preuves/accord_noam_shpayer_whatsapp.png (capture WhatsApp fournie par Hillel le 01/11).
  Demande (Avraham, 12:15) : remerciement pour l'autorisation d'utiliser ses niggounim, demande d'un dossier Drive
  des fichiers MP3/WAV des créations autorisées, engagement d'un crédit « נעם שפייר » dans l'application.
  Réponse de Noam Shpayer (16:20) : « שלום, אין לי תיקיה ששם הם מרוכזים. אתה יכול להוריד מהרשת ולהשתמש »
  (« Je n'ai pas de dossier où elles sont rassemblées. Tu peux les télécharger sur le net et les utiliser. »).
  Portée : autorisation d'utiliser, sans restriction écrite ; la demande portait sur l'application, la publicité
  n'y est pas nommée explicitement. Le crédit « נעם שפייר » a été promis dans l'application.
  Crédit affiché dans la pub (v13) : carte finale, « Musique : « Beyond » — Noam Shpayer · נעם שפייר ».
  Montage : morceau pris à 8,8 s pour que le « drop » (66 s) tombe à 73,3 s, sur le retour du plan d'ouverture ;
  à 83,5 s fondu enchaîné vers la fin du morceau (150 s →), fondu final. Fichier : musique/beyond.wav.

## Ambiance v15 (01/11) — « trouve quelque chose d'autre qui ressemble »
Remplace la Red Library. Enregistrements réels, Radio Aporee, Creative Commons Public Domain Mark 1.0 :
- Mahane Yehuda, Jérusalem — promenade sonore dans le shouk couvert (https://archive.org/details/aporee_27456_31642), extrait 272–286,3 s.
- Tel-Aviv, rue principale vers midi — murmure, voitures, oiseaux (https://archive.org/details/aporee_31564_36259), extrait 132–146,3 s, à 45 %.
Extraits choisis pour leur régularité (pas de pic ni de cri), filtre 120 Hz–6 kHz, −26 LUFS, coupe nette à 14,33 s. Aucun crédit obligatoire.

## v16 (01/11) — voix plus douce, transition foule → musique adoucie
- Voix : edge-tts fr-FR-RemyMultilingualNeural (débit −10 %, hauteur −2 Hz), aigus adoucis (−4 dB au-dessus de 5,5 kHz), −16 LUFS.
  Toutes les phrases regénérées (outils/voix.py) ; poses, sous-titres et plans du calendrier recalés sur les nouveaux mots (mots_voix.json, clés v16_*).
- Transition : la foule s'étouffe (passe-bas) et s'éteint de 12,6 à 15,0 s au lieu d'une coupe nette ; « MY HITBODEDUT » à 14,73 s ;
  « Beyond » entre en fondu de 2,5 s à 15,4 s (le drop reste sur 73,3 s).

## v18 (02/11)
- Voix off : ElevenLabs, voix « Paul – Calm, Confident Narrator » (bibliothèque ElevenLabs), modèle eleven_v4, générée sur le compte de Hillel (usage commercial selon l'abonnement ElevenLabs du compte). Sources : voix_eleven/*.mp3.
- Captures d'écran : fournies par Hillel le 02/11 (remotion/public/captures/v2). Les 4 fichiers *_basse_def sont découpés dans la vue d'ensemble, à remplacer par les originaux.

## v19 — Logos des applis bloquées
- Segment « Je suis avec Hachem » (frames 1510–1770) : logos WhatsApp, YouTube, Facebook, Instagram, TikTok, Snapchat, aspirés sous le téléphone (v20).
- Source des tracés : simple-icons 13.21.0 (CC0), via cdn.jsdelivr.net — fichiers dans remotion/public/logos/.
- Ces logos sont des marques déposées de leurs propriétaires ; utilisés uniquement pour illustrer les applis que l'app peut bloquer (usage nominatif), sans affiliation.

## v23 — Accent de « Hitbodedut » adouci (sans nouvelle génération)
- Retour de Hillel : trop d'accent sur « Hit ». Fichiers retouchés : voix/s05a.wav, s03.wav, s06.wav (originaux dans voix/avant_accent/).
- s05a : souffle du H −8 dB et raccourci ; voyelle « i » abaissée (206 → ~135 Hz, rubberband, formants préservés) et −3 dB.
- s03 : « Hi » raccourci (0,26 → 0,20 s), −4 dB, hauteur ×0,9. s06 : « Hit » −3 dB, hauteur ×0,88.
- Solution définitive possible avec crédits ElevenLabs : régénérer ces 3 phrases.

## v25 — « My Hitbodedut » reconstruit, coupure de « Commence » corrigée
- Une seule prise existait pour « My Hitbodédoute » (vérifié dans le flow ElevenLabs, sans dépense). s05a.wav = « My » de la prise d'origine + le mot « Hitbodédoute » repris de s06 (« …ta Hitbodédoute », accent jugé bon par Hillel). Même voix, même niveau (−17,2 LUFS).
- s11 : la 1re partie s'arrêtait 0,4 s trop tard et laissait entendre le début de « Commence ». Audio coupé à 3,07 s.

## v26 — Style « nuit » + application en mode sombre
- Style inspiré de l'exemple envoyé par Hillel (fond bleu nuit, sujet éclairé, néon). Activé par --props='{"effets":true,"style":"nuit"}'.
- Captures en mode sombre : produites le 02/11 avec le script officiel du dépôt (web/test/captures.mjs, mode « sombre », langue fr, Chromium 780×1688) sur la version web de l'app construite depuis les ressources Android. Fichiers : remotion/public/captures/sombre/.
- Les 4 écrans propres à Android (Je suis avec Hachem, choix des applis, heures du mode, durée du report) n'existent pas en version web : ils restent en captures Android (clair).
