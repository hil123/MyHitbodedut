# Le relais du suivi de vol en direct

`worker.js` est un Cloudflare Worker. L'application ne contient aucune clé :
elle demande au relais la position d'un vol, et le relais interroge OpenSky
Network.

**Android n'en a pas besoin** (28/10) : l'édition complète interroge OpenSky
directement, en accès anonyme et gratuit (sans compte ni clé, 400 crédits par
jour et par adresse IP, positions à 10 s près), et cherche l'indicatif sur le
téléphone : seule la zone part. **Le web en a besoin** : OpenSky n'autorise
pas les appels d'un autre site depuis un navigateur (vérifié le 28/10 : en-tête
`access-control-allow-origin: https://opensky-network.org` seulement).

## Les conditions d'OpenSky (courriel du 28/10/2026, ticket 837455)

Application gratuite, sans publicité, abonnement ni fonction payante, dons
éventuels reversés à une association : usage **non commercial**, compte OpenSky
gratuit enregistré, sans frais. À condition que :

- les données soient demandées **par ce relais**, avec le client OAuth2 du
  compte (secrets du Worker), et **non par chaque téléphone** ;
- les appareils d'une même zone partagent **une réponse en cache**, temporaire,
  sans archive des données brutes (ici : carré calé sur une grille de 0,5°,
  réponse gardée 15 s en mémoire puis effacée) ;
- les données ne soient ni revendues, ni redistribuées, ni proposées par une
  API (le relais ne rend que l'avion demandé) ;
- l'attribution « Données aéronautiques : OpenSky Network
  (opensky-network.org) » figure dans l'application **et sur la page du
  magasin**, avec un avertissement : ni navigation ni sécurité.

Publicité, abonnement, fonction payante ou gestion par une entreprise :
licence commerciale (Startup, 99 $/mois selon ce courriel).

L'édition Android complète construite **sans** relais interroge encore OpenSky
depuis le téléphone, en anonyme : pour une diffusion publique, la construire
avec `-PrelaisVol=https://…`.

## Ce qui passe par le relais

- **Reçu** : l'indicatif radio du vol (« ELY1 ») et la position estimée sur la
  route, arrondie au dixième de degré. Pas d'identifiant d'appareil, pas de
  compte, pas de nom.
- **Envoyé à OpenSky** : un carré de ±5° autour de cette position (2 crédits,
  4 près du méridien de 180°).
- **Rendu** : la position, l'altitude et l'heure du relevé de l'avion dont
  l'indicatif correspond, ou « introuvable ».
- **Gardé** : la réponse d'OpenSky pour le carré, 15 s en mémoire, puis effacée ; rien d'autre. Cloudflare tient ses propres journaux de
  requêtes ; `observability` est désactivé dans `wrangler.toml`.

## Mettre en ligne (à faire par le propriétaire du compte)

1. Facultatif : un compte OpenSky Network et un client API (identifiant et
   secret), pour 4 000 crédits par jour au lieu de 400. Sans eux, le relais
   utilise l'accès anonyme, dont les 400 crédits de l'adresse du Worker sont
   partagés par tous les visiteurs du web (2 crédits par demande). Usage
   **non commercial** seulement : une publication sur le Play Store ou
   l'App Store est à revoir avec les conditions d'OpenSky.
2. Créer un compte Cloudflare (offre gratuite : 100 000 requêtes par jour).
3. Ici : `npx wrangler login`, puis, si l'on a un client API,
   `npx wrangler secret put OPENSKY_CLIENT_ID` et
   `npx wrangler secret put OPENSKY_CLIENT_SECRET` ; enfin `npx wrangler deploy`.
4. Mettre l'adresse obtenue (`https://myhitbodedut-vol.<compte>.workers.dev`)
   dans la construction :
   - Android, seulement pour passer par le relais plutôt qu'en direct :
     `./gradlew -PrelaisVol=https://… assembleFullRelease` ;
   - web : `RELAIS_VOL=https://… node build.mjs`, et `ORIGINE_WEB` dans
     `wrangler.toml` à l'origine du site.

Sans adresse, l'option reste visible mais inactive sur le web, qui ne fait
aucune requête.

## Limites

OpenSky reçoit les signaux ADS-B par des antennes au sol : au-dessus des
océans, loin des côtes, un avion n'est souvent pas vu. L'application revient
alors à la route prévue, ou au GPS.

Tests : `web/test/relais.test.mjs`, avec un faux OpenSky.
