# Pub vidéo MY HITBODEDUT (Remotion)

- `remotion/src/Pub.tsx` : 11 `Sequence` (une par scène du script), zones de mise en page strictes (texte / sujet / sous-titres), `spring()` pour les textes, `interpolate()` pour caméra et zooms.
- `remotion/src/theme.ts` : palette, minutage des scènes, textes voix et écran, zones.
- `captures/` : captures réelles de l'application web (Playwright, 1236×2676, `outils/` non requis).
- `remotion/public/voix` : voix témoin ; `remotion/public/musique` : musique témoin.
- Rendu : `cd remotion && npm i && npx remotion render src/index.ts MyHitbodedutPub out/pub.mp4 --codec=h264 --crf=18`
- Voir `BRIEF_PUB.md`, `FICHE_CREDITS.md`, `REVUE_PUB.md`.
