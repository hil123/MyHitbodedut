# MyHitbodedut pour iPhone et iPad (application native)

Application SwiftUI destinée à l'App Store. Cahier des charges : document
« Cahier des charges — publier MyHitbodedut sur l'App Store » (29/10/2026).

## Principe

Rien n'est recopié ni réécrit à la main :

| Ce que l'app utilise | D'où ça vient |
| --- | --- |
| Textes, villes, chaînes fr / en / he | `web/dist/data/`, lui-même construit depuis les ressources Android |
| Illustrations | `web/dist/img/` |
| Calendrier hébraïque, zmanim | `web/src/js/`, empaqueté en `Moteur.js` et exécuté par JavaScriptCore |
| Musique | `המלך בשדה` seulement (libre de droits) ; les autres morceaux seront remplacés par des morceaux autorisés par écrit |

`tools/prepare.sh` fait tout cela dans `MyHitbodedut/Genere/` (non versionné)
et vérifie `Moteur.js` contre les vecteurs KosherJava avant de le copier.

## Construire

Sur un Mac avec Xcode 26 :

```sh
bash ios/tools/prepare.sh
cd ios && brew install xcodegen && xcodegen generate
open MyHitbodedut.xcodeproj
```

Sans Mac : le flux GitHub Actions `iOS` (`.github/workflows/ios.yml`) fait la
même chose sur un Mac de GitHub et lance les tests sur un iPhone simulé.

## Tests

- `tools/verifie_moteur.mjs` : `Moteur.js` dans un contexte JavaScript nu,
  3 896 jours et 531 zmanim identiques à KosherJava.
- `MyHitbodedutTests/MoteurTests.swift` : les mêmes vecteurs, sur iPhone simulé.

## État

Construit : le moteur, les trois langues (hébreu de droite à gauche),
l'accueil avec la date hébraïque, les zmanim du jour, le choix de la langue
et de la ville. Le reste suit l'ordre du cahier des charges.

Non encore fait : signature et envoi vers App Store Connect (il faut le
compte Apple Developer de l'association).

Icône : `MyHitbodedut/Assets.xcassets/AppIcon.appiconset/AppIcon-1024.png`,
fournie par le porteur le 29/10 (1254 px, ramenée à 1024 × 1024, sans transparence).
