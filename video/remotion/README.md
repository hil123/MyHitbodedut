# Vidéo MyHitbodedut — projet Remotion

Teaser d'effets visuels (`TeaserVFX`, 15,7 s, 1080×1920, 30 i/s). Plan du film :
`../PLAN_VIDEO_PRESENTATION.md`.

```
npm install
npx remotion studio                      # aperçu interactif
npx remotion render src/index.ts TeaserVFX out/teaser_vfx_1080.mp4 --crf=18
```

| Fichier | Rôle |
|---|---|
| `src/theme.ts` | couleurs, polices, easings, minutage des actes |
| `src/scenes/Bruit.tsx` | acte I : tempête de notifications 3D, lumière d'écran, aberration chromatique |
| `src/scenes/Barriere.tsx` | acte II : écran « Tu as fait ta hitbodedout ? » qui s'emplit de lumière |
| `src/scenes/Portail.tsx` | la porte de lumière, god rays, notifications → étoiles, traversée |
| `src/scenes/Aube.tsx` | ciel en shader, soleil levant, collines, avatars en contre-jour, logo |
| `src/fx/` | outils : textures, halos, cartes de notification, caméra, titres |

`remotion.config.ts` pointe vers le Chromium préinstallé du conteneur et force le
WebGL logiciel (`swangle`). Sur un PC avec GPU, retirer ces deux lignes : le rendu
sera bien plus rapide.

**Son** : `public/audio/musique_temoin.m4a` (hors git) est המלך בשדה, MUSIQUE TÉMOIN — à recréer par `ffmpeg -i ../../android/app/src/main/res/raw/hamelech_basade.m4a -t 40 -c:a aac -b:a 160k public/audio/musique_temoin.m4a` — licence
non établie, à ne pas diffuser. `montee_acte1.m4a` est générée par ffmpeg (bruit
rose filtré + graves), libre. Polices : Frank Ruhl Libre et Varela Round, OFL
(licences dans `public/fonts/`).
