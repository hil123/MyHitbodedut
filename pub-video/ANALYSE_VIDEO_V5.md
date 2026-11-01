# Analyse vidéo Higgsfield — v5 (01/11), 0 crédit

Outil : Higgsfield `video_analysis` (scène par scène). 24 scènes détectées sur 90 s.

## Ce qu'elle confirme
- Même personnage dans tous les plans (« the man from scene 1 »).
- Structure lisible : accroche (0:00), problème (0:06), produit (0:13), usages (0:19–0:37), arguments (0:37–1:12), appel à l'action (1:19).

## Défauts relevés, puis corrigés dans la v5b
- Poses en retard d'un morceau de phrase : « les soucis » sur l'aéroport, « tes joies » sur le doigt levé, « tes remerciements » sur la danse.
  - Cause : découpage au prorata des lettres.
  - Correction : calage sur l'horodatage réel des mots de la voix (`outils/mots_voix.json`), pour les poses comme pour les sous-titres.

## Défauts relevés, encore ouverts
- La bulle « Et demain ? » est lue comme une réplique : les bulles se confondent avec la voix.
- Le personnage sourit partout, même sur « les soucis » : le stress ne se voit pas sur le visage.
- 0:34–0:37 et 1:15–1:19 : la musique seule, sans voix, sur des plans fixes.
- Écrans Android (étapes guidées, blocage, horaires, report) : cartes « à fournir ».
