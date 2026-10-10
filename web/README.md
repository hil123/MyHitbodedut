# MyHitbodedut pour iPhone

Une application web installable : sur l'iPhone, on ouvre le site dans Safari, puis Partager → « Sur l'écran d'accueil ». Une fois installée, elle fonctionne sans connexion.

## D'où vient le contenu

Rien n'est recopié à la main. À chaque construction, `build.mjs` relit les ressources de l'application Android :

| Contenu | Source Android |
| --- | --- |
| Chaînes d'interface, fr / en / he | `res/values*/strings.xml` |
| Textes vérifiés (’hizouk, bibliothèque, Tikoun) | `res/raw/*.json`, copiés à l'octet près |
| Illustrations | `res/drawable-nodpi/*.webp` |
| Couleurs, jour et nuit | `res/values*/colors.xml`, `palettes.xml` |
| Icônes des onglets | `res/drawable/ic_nav_*.xml` |
| Musique et titres | `res/raw/*.mp3`, dans l'ordre de `MusicLibrary.kt` |
| Villes des zmanim, ajouts du siddour | `res/raw/villes.json`, `res/raw/siddur_annotations.json` |

Les règles d'Android (`Langue.kt`, `Reference.kt`, `JaugeSemaine.kt`, `DureeSeance.kt`, les fêtes de `Holidays.kt`, l'export et l'import de `Store.kt`) sont portées dans `src/js/`, avec leurs tests dans `test/`.

## Construire et tester

```sh
node web/build.mjs                       # construit web/dist
(cd web && npm install)                  # Hebcal, moteur de comparaison des tests (jamais servi)
node --test web/test/*.test.mjs          # les règles
node web/test/captures.mjs webkit        # captures d'écran sous WebKit (Playwright requis)
```

La CI (`.github/workflows/web.yml`) fait les trois à chaque push et met le site en ligne si le secret `WEB_DEPLOY_TOKEN` existe.

## Calendrier hébraïque et zmanim

kosher-zmanim 0.9.0 (LGPL 3.0) est servi tel quel, en module séparé, dans
`src/vendor/` (`tools/vendor_kosher_zmanim.sh`, licences dans
`src/vendor/LICENCES.txt`). Les règles de prière portent `TefilaRules` de
KosherJava (`src/js/tefila.js`, LGPL 2.1). Les tests exigent, jour par jour
de 2025 à 2030, la même réponse que l'application Android. Détail : `ZMANIM.md`
à la racine.

## Confidentialité

- Aucune requête ne sort du site : pas de police en ligne, pas de mesure d'audience. La politique de sécurité de la page (`Content-Security-Policy`) l'interdit, et les tests échouent si une requête part ailleurs.
- Les données restent dans le navigateur du téléphone, en `localStorage`, le lieu des zmanim compris. La position, si on la choisit, ne sert qu'au calcul local.
- La musique est gardée hors connexion morceau par morceau, à la première écoute.
