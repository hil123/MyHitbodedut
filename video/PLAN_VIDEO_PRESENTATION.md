# Plan — vidéo de présentation MyHitbodedut

Rédigé le 29/10/2026. Méthode : skills `publicite-video` (structure, accroche,
formats), `montage-video` (fabrication), `intelligence-montage-video` (goût).
Rien n'est rendu à ce stade : ce fichier fixe le quoi et le comment avant toute
dépense.

Statuts : **Vérifié** (lu dans le dépôt ou dans une source citée), **Déduit**,
**[À VALIDER]** (décision du propriétaire).

---

## 1. L'idée

**Titre de travail : « Avant le monde, Lui. »**

Le monde nous parle toute la journée : notifications, messages, fils
d'actualité. MyHitbodedut renverse l'ordre : **avant d'ouvrir le monde, on
parle au Roi.** C'est exactement le mécanisme de l'application (Vérifié,
README) : la barrière du matin bloque les applications choisies tant que la
hitbodedout n'est pas faite.

**Le message fort, en une phrase (dite ET écrite) :**

> **« Le monde te parle toute la journée. Donne-Lui une heure pour que tu Lui parles. »**

Variante courte pour les coupes de 15 s : **« Une heure. Toi et Lui. »**

**Pourquoi l'homme ET la femme.** Rabbi Na'hman dit de la hitbodedout qu'elle
est « דבר השוה לכל נפש מקטן ועד גדול » — accessible à toute âme, du plus petit
au plus grand (Likoutei Moharan II, 25 — Vérifié sur Wikisource, texte non
encore relu sur une édition imprimée). Le film suit donc **deux vies en
parallèle** — le 'hassid et la femme de l'application — qui ne se croisent
jamais, et qui arrivent au même lieu : seul(e) face au ciel. Le parallèle
dit sans un mot : *c'est pour toi, qui que tu sois.*

## 2. Ce dont on dispose déjà (Vérifié, dépôt)

| Élément | Où | Usage dans le film |
|---|---|---|
| Avatar homme, ~25 poses 3D style animation (debout, prière, marche, écriture, danse, guitare, Tehilim, tefillin…) | `android/app/src/main/res/drawable-nodpi/char_*.webp` | personnage principal 1 |
| Avatar femme, 20 poses **appariées une à une** aux poses homme (`Personnage.kt`, table FEMININ) | `char_f_*.webp` | personnage principal 2, montage en miroir |
| Décors : nature au coucher/lever, tsiyoun d'Ouman (domaine public), Soukkot, 'Hanouka, sommeil, aéroport | `scene_*.webp` | décors du récit |
| L'application elle-même (site web fonctionnel) | `hil123.github.io/myhitbodedut-web/` | vraies captures d'écran animées (Playwright) |
| Textes de 'hizouk, guide de hitbodedout (auteur : le propriétaire) | `textes/` | citations à l'écran |
| Musique : המלך בשדה embarqué, nigounim | `res/raw`, `musique/` | **licences non documentées → [À VALIDER]** |

Les avatars sont détourés sur fond blanc, avec un liseré clair (README,
« Les illustrations ») : en vidéo sur fond sombre, prévoir un léger halo de
lumière derrière eux, qui cache le liseré et sert la direction artistique.

## 3. Brief pub (§1 de `publicite-video`)

1. **Objectif** : notoriété + installation. Appel à l'action unique : ouvrir
   l'application web (aujourd'hui) ; Google Play quand elle y sera
   (PUBLICATION_PLAY_STORE.md : pas encore publiée — Vérifié).
2. **Cible** : hommes et femmes juifs pratiquants ou en chemin, 18–45 ans,
   francophones puis hébréophones. Dans leurs mots : « je prie trois fois par
   jour mais je ne Lui parle jamais vraiment », « mon téléphone me mange le
   matin ».
3. **Niveau de conscience** : connaît le problème (le téléphone, la prière
   devenue routine), connaît vaguement la solution (la hitbodedout), ne
   connaît pas la marque. → Accroche par la **douleur concrète** ou le
   **contraste**.
4. **Promesse** : *une heure par jour seul avec Hachem, sans que le téléphone
   la vole.*
5. **Mécanisme** : la barrière du matin (« Tu as fait ta hitbodedout ? »),
   le minuteur qui valide le temps réel, la musique, le journal, la série.
6. **Preuves** : aucune donnée d'usage documentée → **pas de chiffre, pas de
   témoignage**. La seule « preuve » est la source : Likoutei Moharan II, 25.
7. **Mots interdits / à valider** : pas de promesse de résultat spirituel
   (« tu seras transformé ») ; « gratuit » [À VALIDER] ; nom des artistes
   musicaux interdit sans accord écrit (cf. décision Ishay Ribo, musique/README).
8. **Formats** : master **9:16** (Reels, Stories, WhatsApp Status, TikTok) ;
   **4:5** recomposé (Feed) ; **16:9** pour le site et YouTube.
   Durées : 60 s (version film), 3 × 15 s (acquisition), 90 s en 16:9 (site).
9. **Langues** : français d'abord, puis hébreu (même montage, voix et textes
   changés — la grille de texte est pensée RTL dès le départ).

## 4. Direction artistique

**Le fil visuel : du bruit à la lumière.** Le film passe d'un monde saturé,
froid et fragmenté à un espace ouvert, chaud et silencieux.

| Acte | Palette | Lumière | Texture | Rythme des plans |
|---|---|---|---|---|
| I. Le bruit | bleu écran, rouge notification | dure, venue d'en bas (l'écran) | bulles, pastilles, flou de mouvement | 0,5–1,2 s, coupes serrées |
| II. L'arrêt | noir velouté + vert profond de l'app (#1E3A30) | une seule source | grain fin | coupe nette puis tenue |
| III. La rencontre | or du lever, ciel ambré | rasante, chaude | poussière dorée, brume | 3–6 s, dérives lentes |
| IV. L'envoi | blanc chaud + vert de marque | pleine | — | logo, appel |

**Trois idées visuelles signatures** (ce qui rend le film « artistique » et
pas seulement explicatif) :

1. **Les bulles deviennent des étoiles.** Les pastilles de notification qui
   assaillent les personnages s'éteignent une à une, montent, et deviennent le
   ciel étoilé d'avant l'aube. Un seul objet change de sens : le bruit devient
   l'immensité.
2. **Le rectangle de l'écran devient une porte.** Raccord de forme : le
   téléphone, plein cadre, s'éclaire en écran de barrière « Tu as fait ta
   hitbodedout ? », puis ce rectangle s'agrandit et devient l'encadrement
   d'une porte ouverte sur le champ. On « entre » dans la hitbodedout par
   l'écran même qui nous en éloignait.
3. **Le miroir homme / femme.** Écran partagé vertical (moitié haute / moitié
   basse en 9:16) : mêmes gestes, mêmes heures, deux maisons. Au moment de la
   prière, la ligne de partage **se dissout dans l'horizon** — même ciel,
   même Roi. Ils ne se rencontrent jamais à l'image : chacun est seul, et
   c'est le sens même du mot *hitbodedout*.

**Typographie** : un titrage serif élégant pour les citations (hébreu
vocalisé + français), une sans-serif claire pour l'interface. Au plus 2 lignes
à la fois, zone sûre 9:16 (haut ≈ 14 %, bas ≈ 35 % libres).

## 5. Script — version 60 s (9:16)

Voix : deux voix alternées, une masculine et une féminine, qui finissent par
dire la même phrase ensemble. Musique : une seule pièce qui naît de
l'acte II et s'ouvre à l'acte III.

| Temps | Image | Voix | Texte à l'écran | Intention |
|---|---|---|---|---|
| 0,0–1,5 | Gros plan : le visage de l'homme éclairé en bleu par l'écran, réveil 6:02, la première pastille rouge « explose » dans le cadre | **H** : « Six heures. » | `6:02` | Arrêter le pouce : mouvement dès la 1re image |
| 1,5–3,0 | Même cadre, côté femme (écran partagé qui s'ouvre) : même lumière, mêmes pastilles | **F** : « Le monde est déjà là. » | — | Nommer la cible : lui ET elle |
| 3–8 | Accélération : messages, actualités, groupes, vidéos ; les bulles s'empilent et recouvrent les deux visages ; le logo discret apparaît en haut à 0,8 s | **H** : « Les messages. » **F** : « Les nouvelles. » **H+F** : « Tout le monde te parle… » | `Tout le monde te parle.` | Problème concret, tension montante |
| 8–10 | Un doigt va toucher une application → **COUPE NETTE** : l'écran de barrière plein cadre, vert profond | (silence de musique, 0,6 s) **voix calme** : « …sauf toi, à Lui. » | `Tu as fait ta hitbodedout ?` | Retournement, sommet du haut de l'échelle d'emphase |
| 10–15 | Le rectangle de l'écran devient une porte qui s'ouvre sur le champ avant l'aube ; les bulles montent et deviennent des étoiles | **F** : « Avant d'ouvrir le monde… » **H** : « …ouvre ton cœur. » | **MyHitbodedut** | Promesse + marque nommée avant la moitié |
| 15–25 | L'homme marche vers le champ (`char_walking`), la femme marche vers la forêt (`char_f_walking`) ; écran partagé, la ligne de partage devient l'horizon | **H** : « Une heure. » **F** : « Seul avec Lui. » **H** : « Dans tes mots. » | `Une heure. Toi et Lui.` | Situer, respirer |
| 25–40 | **Mécanisme** en vraies captures de l'app, posées dans un téléphone qui flotte dans la lumière du matin : le minuteur qui tourne · la musique choisie · le sujet du jour · le journal où il/elle écrit (`char_writing` / `char_f_writing`) · la série de jours | **F** : « L'application garde ta porte fermée au bruit. » **H** : « Elle compte ton heure, pas tes promesses. » | `Barrière du matin` · `Minuteur` · `Journal` | Comprendre, une idée par plan |
| 40–50 | Prière : l'homme (`char_praying`), la femme (`char_f_praying`), puis bras levés (`char_open_arms` / `char_f_arms_up`) dans le soleil qui se lève ; la ligne de partage a disparu | (musique au premier plan, pas de voix 3 s) puis **H+F ensemble** : « Même si les mots ne viennent pas, être là, c'est déjà beaucoup. » | `ואפילו אם... אינו יכול לפתוח פיו... זה בעצמו טוב מאוד` — *Likoutei Moharan II, 25* | Émerveillement, sommet émotionnel |
| 50–56 | Joie : danse (`char_dancing_joy` / `char_f_joy`), puis plan large : deux silhouettes minuscules, deux collines, un seul ciel | **H+F** : « Le monde te parle toute la journée. Donne-Lui une heure. » | `Le monde te parle toute la journée.` → `Donne-Lui une heure.` | Message fort |
| 56–60 | Logo MyHitbodedut, icône de l'app, adresse | **voix** : « MyHitbodedut. Commence demain matin. » | `MyHitbodedut` · `hil123.github.io/myhitbodedut-web` [À VALIDER] | Appel à l'action dit et écrit |

**Note sur la citation (40–50).** La phrase hébraïque est extraite du texte
vérifié (Likoutei Moharan II, 25 : « ואפילו אם לפעמים נסתתמין דבריו ואינו
יכול לפתוח פיו לדבר לפניו יתברך כלל, אף על פי כן זה בעצמו טוב מאד »). La
voix française en est une **paraphrase**, à présenter comme telle, jamais
comme une traduction littérale. La formule souvent citée « dire un seul mot,
'Ribono chel Olam', encore et encore » n'a **pas** été retrouvée dans ce
siman lors de la vérification : elle n'est pas utilisée tant que sa source
exacte n'est pas établie.

## 6. Les 3 accroches (même suite à partir de 3 s)

| # | Type | 0–3 s | Phrase (≤ 10 mots) |
|---|---|---|---|
| A | Douleur concrète | Réveil 6:02, la première notification explose sur les deux visages | « Six heures. Le monde est déjà là. » |
| B | Question à la cible | Les deux avatars regardent la caméra, téléphone à la main | « À qui tu parles en premier, le matin ? » |
| C | Contraste | Écran partagé : à gauche 247 notifications, à droite un champ silencieux ; le pouce hésite | « Eux te parlent. Lui t'attend. » |

Coupes de 15 s : accroche (3 s) → barrière + porte (3 s) → mécanisme
accéléré (5 s) → message + logo + appel (4 s).

## 7. Son

- **Musique** : une seule pièce, sans paroles. Acte I : pulsation
  électronique sèche (ou silence + tic de l'horloge, sans bruitage littéral) ;
  rupture nette à 8 s ; puis nigoun lent qui s'ouvre (cordes, piano) jusqu'à
  la danse.
  - Option 1 : **המלך בשדה** (déjà dans l'app ; le titre, « le Roi dans le
    champ », colle au film) — **licence et interprète à établir [À VALIDER]**.
  - Option 2 : musique libre de droits sous licence notée (Pixabay / FMA,
    licence copiée dans `video/LICENCES.md`).
  - Option 3 : composition générée (ElevenLabs Music / Higgsfield audio) —
    crédits, fiche + GO.
- **Voix** : témoin gratuite edge-tts (`fr-FR-HenriNeural` et
  `fr-FR-DeniseNeural`), puis voix finales ElevenLabs avec GO. Voix
  ≈ −16 LUFS, musique 10–18 dB dessous, remontée dans les respirations.
- **Sous-titres** complets, calés au mot (Whisper), dans la zone sûre :
  le film se comprend son coupé.

## 8. Fabrication — outils et pipeline

| Étape | Outil | Coût |
|---|---|---|
| Projet vidéo | **Remotion 4** (React/TypeScript), dossier `video/remotion/` ; compositions `Master916`, `Feed45`, `Site169`, `Cut15_A/B/C` | 0 |
| Avatars animés | Remotion : parallaxe 2,5D (avatar / halo / décor / particules sur calques), respiration ±1 %, entrées en `spring()` ; **jamais** de Ken Burns par défaut | 0 |
| Gestes réels des avatars (marche, bras qui se lèvent, danse) | image → vidéo (Higgsfield / Seedance 2.0 ou ElevenLabs Video) à partir des poses existantes, **un clip = un mouvement**, chaînés par dernière image | **crédits : fiche + GO par plan** |
| Décors manquants (chambre au réveil, porte sur le champ, forêt à l'aube) | génération d'image dans le style 3D des avatars, puis parallaxe | crédits : fiche + GO |
| Captures de l'application | Playwright (Chromium déjà installé) sur le site web, en 1080×1920, enregistrement vidéo des gestes réels (lancer une séance, choisir la musique, écrire au journal) | 0 |
| Bulles → étoiles, porte, ligne d'horizon | composants Remotion (SVG + particules), easing `inOut(sin)` / `out(cubic)` | 0 |
| Voix témoin | edge-tts | 0 |
| Sous-titres | Whisper → `@remotion/captions` | 0 |
| Mixage | ffmpeg `loudnorm` + ducking | 0 |
| Contrôles | `remotion still` + planche contact, `ffprobe`, `silencedetect`, détection de scènes | 0 |

Arborescence prévue :

```
video/
  PLAN_VIDEO_PRESENTATION.md   ← ce fichier
  BRIEF_PUB.md                 (§3 extrait)
  DECOUPAGE_master.md          (plan par plan : intention, mouvement, easing, transition)
  LICENCES.md                  (musique, images, polices)
  remotion/                    (projet Remotion)
    public/avatars/ public/scenes/ public/captures/ public/audio/
    src/theme.ts  src/compositions/  src/components/
  _RENDU_SESSION/              (rendus livrés + INDEX.md, hors git pour les MP4)
```

## 9. Étapes et livraison

1. **Brief + découpage** (`BRIEF_PUB.md`, `DECOUPAGE_master.md`) — 0 crédit.
2. **Aperçu complet en moins de 45 min** : Remotion avec les poses fixes
   animées en 2,5D, captures Playwright, voix témoin, musique témoin marquée
   « MUSIQUE TÉMOIN », plans manquants marqués « PROVISOIRE ». Rendu 720p
   9:16. → **première vidéo à regarder.**
3. Revue de Hillel (≤ 5 questions bloquantes).
4. **Fiches crédits** pour les 4 à 6 plans qui gagnent vraiment à être animés
   par IA (marche, bras levés, danse, porte qui s'ouvre). GO plan par plan,
   720p d'abord.
5. Voix finales (GO), musique finale (licence établie).
6. Rendus : master 9:16 60 s → 3 coupes de 15 s → 4:5 recomposé → 16:9 90 s.
   Puis version hébraïque.
7. Contrôles : revue pub (§6 de `publicite-video`) et §8 de `montage-video`
   dans `REVUE_PUB.md`. Livraison : MP4 + copie légère 720p + planche des 3
   premières secondes de chaque accroche.

## 10. Décisions à prendre (bloquantes)

1. **Musique** : המלך בשדה (si la licence est obtenue), musique libre, ou
   composition générée ?
2. **Femme à l'image** : une partie du public 'harédi n'accepte pas d'images
   de femmes. Faut-il, en plus du film homme + femme, une **coupe « homme
   seul »** pour certains canaux ? (et inversement une coupe « femme seule »
   pour des groupes féminins)
3. **Appel à l'action** : site web aujourd'hui, ou attendre Google Play ?
   Peut-on écrire « gratuit » ?
4. **Langue de la première version** : français, hébreu, ou les deux d'emblée ?
5. **Budget crédits** pour les plans animés par IA (estimation à chiffrer sur
   fiche avant toute dépense).

## Sources

- Likoutei Moharan II, 25 — texte hébreu, Wikisource :
  https://he.wikisource.org/wiki/ליקוטי_מוהר"ן_תניינא_כה (lu le 29/10/2026 ;
  la page signale un texte non entièrement relu ; à recouper sur une édition
  imprimée ou Sefaria avant diffusion).
- Breslov Research Institute, présentation de II, 25 comme leçon centrale sur
  la hitbodedout : https://breslov.org/hitbodedut-i-cant-find-the-words/
- Règles de format et de structure pub : sources listées au §0 du skill
  `publicite-video` (Meta for Business, Google Ads ABCD).
