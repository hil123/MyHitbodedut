# Film de présentation MyHitbodedut — projet de montage

Deux films verticaux (9:16, 1080×1920, environ 50 s) : version **homme** et version **femme** (foulard blanc).
La dernière version livrée est la **v3**, du 3 novembre 2026. Les vidéos elles-mêmes ne sont pas dans le dépôt, car elles sont trop lourdes.

## Contenu

| Dossier | Ce qu'il contient |
|---|---|
| `montage/` | Projet Remotion : `src/Film.jsx` (le film), `src/timing.json` (durées des scènes, calées sur les voix), `public/` (tous les médias) |
| `montage/public/chars/` | Les 38 poses des planches, détourées en PNG transparent |
| `montage/public/plans/` | Plans ChatGPT détourés : réveil, marche à l'aube, gros plan des mains redressé (`X03_droit`), doigts à poser par-dessus le téléphone |
| `montage/public/ecrans/` | Captures des écrans de l'app : version web, plus l'écran de barrière Android reconstitué |
| `montage/public/audio/` | Voix finales ElevenLabs (`m_L1..9` = Paul, `f_L1..9` = Caroline) et musique |
| `voix_elevenlabs/` | Voix ElevenLabs brutes (tests et répliques du film) |
| `sources_chatgpt/` | Images ChatGPT d'origine, avec l'écran vert |
| `captures_html/` | Pages HTML de l'écran de barrière |
| `scripts_preparation/` | Scripts Python et Node de détourage, d'insertion des écrans, de captures et de transcription. Leurs chemins absolus pointent vers l'ancienne session et sont à adapter. |
| `BRIEF_ET_NOTE_v1.md` | Brief, note d'intention, scénario |
| `PROMPTS_CHATGPT_plans_manquants.md` | Prompts des plans à générer, dont le plan 09 « débordé » |

## Reprendre le montage

```bash
cd film_presentation/montage
npm install                      # remotion 4.0.300, react 18.3.1
python3 make_timing.py           # durées des scènes d'après les voix
./mix.sh m && ./mix.sh f         # voix + musique (ducking, -16 LUFS) -> out/mix_*.wav
node render.mjs FilmHomme FilmFemme   # vidéo muette -> out/*_muet.mp4
# puis ajouter le son (voir run_all.sh)
```

`render.mjs` et `stills.mjs` utilisent le Chromium de l'ancien conteneur (`/opt/pw-browsers/...`) : adapte `exe`, ou supprime `browserExecutable` pour laisser Remotion télécharger le sien.

## Décisions prises

- **Voix** : ElevenLabs, modèle `eleven_v4`. Homme : **Paul** (`RAFNgKBJ8SavtpwF6uUV`). Femme : **Caroline** (`kwhMCf63M8O3rCfnQ3oQ`).
- **Prononciation** : le texte envoyé aux voix écrit le nom **« Maï itbodédout »**, et la pratique « itbodédout ». Les sous-titres gardent l'orthographe normale.
- **Crédits ElevenLabs** : 1 661 consommés sur un plafond de 2 600 autorisé par Hillel.
- **Musique** : « Heartwarming », Kevin MacLeod (incompetech.com), licence CC BY 4.0, avec crédit à l'écran. Un accord d'artiste a été demandé et pourra la remplacer.
- **Appel à l'action** : « Essayez-la dès maintenant : hil123.github.io/myhitbodedut-web », puis « Bientôt sur Google Play ».
- **Chabbat** : aucune mention, retirée à la demande de Hillel.
- **Écrans** : toujours droits. Sur le gros plan des mains, l'image est pivotée et le téléphone ChatGPT est remplacé par un téléphone propre, avec les doigts reposés par-dessus.

## Reste à faire

1. **Version femme, environ 22 à 28 s** : la pose « PROVISOIRE » est à remplacer par la marche à l'aube au féminin (`F05_marche_aube`, prompt 05).
2. **Plan 09 « débordé par les notifications »** pour la scène du problème : à générer, prompt section E.
3. Plans « téléphone en main » : il faut des images avec le téléphone **tenu de face**.
4. Option : décors D1 à D3 pour remplacer le ciel d'aube dessiné en CSS.
