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

Les règles d'Android (`Langue.kt`, `Reference.kt`, `JaugeSemaine.kt`, `DureeSeance.kt`, les fêtes de `Holidays.kt`, l'export et l'import de `Store.kt`) sont portées dans `src/js/`, avec leurs tests dans `test/`.

## Construire et tester

```sh
node web/build.mjs                       # construit web/dist
node --test web/test/*.test.mjs          # les règles
node web/test/captures.mjs webkit        # captures d'écran sous WebKit (Playwright requis)
```

La CI (`.github/workflows/web.yml`) fait les trois à chaque push et met le site en ligne si le secret `WEB_DEPLOY_TOKEN` existe.

## Confidentialité

- Aucune requête ne sort du site : pas de police en ligne, pas de mesure d'audience. La politique de sécurité de la page (`Content-Security-Policy`) l'interdit, et les tests échouent si une requête part ailleurs.
- Les données restent dans le navigateur du téléphone, en `localStorage`.
- La musique est gardée hors connexion morceau par morceau, à la première écoute.
