# Le relais du suivi de vol en direct

`worker.js` est un Cloudflare Worker. L'application ne contient aucune clé :
elle demande au relais la position d'un vol, et le relais interroge OpenSky
Network avec la sienne.

## Ce qui passe par le relais

- **Reçu** : l'indicatif radio du vol (« ELY1 ») et la position estimée sur la
  route, arrondie au dixième de degré. Pas d'identifiant d'appareil, pas de
  compte, pas de nom.
- **Envoyé à OpenSky** : un carré de ±5° autour de cette position (2 crédits,
  4 près du méridien de 180°).
- **Rendu** : la position, l'altitude et l'heure du relevé de l'avion dont
  l'indicatif correspond, ou « introuvable ».
- **Gardé** : rien par ce code. Cloudflare tient ses propres journaux de
  requêtes ; `observability` est désactivé dans `wrangler.toml`.

## Mettre en ligne (à faire par le propriétaire du compte)

1. Créer un compte OpenSky Network, puis un client API (identifiant et secret)
   dans la page du compte. Usage **non commercial** seulement : une
   publication sur le Play Store ou l'App Store est à revoir avec les
   conditions d'OpenSky, ou avec un autre fournisseur.
2. Créer un compte Cloudflare (offre gratuite : 100 000 requêtes par jour).
3. Ici : `npx wrangler login`, puis
   `npx wrangler secret put OPENSKY_CLIENT_ID` et
   `npx wrangler secret put OPENSKY_CLIENT_SECRET`, puis `npx wrangler deploy`.
4. Mettre l'adresse obtenue (`https://myhitbodedut-vol.<compte>.workers.dev`)
   dans la construction :
   - Android : `./gradlew -PrelaisVol=https://… assembleFullRelease` ;
   - web : `RELAIS_VOL=https://… node build.mjs`, et `ORIGINE_WEB` dans
     `wrangler.toml` à l'origine du site.

Sans adresse, l'option reste visible mais inactive, et l'application ne fait
aucune requête.

## Limites

OpenSky reçoit les signaux ADS-B par des antennes au sol : au-dessus des
océans, loin des côtes, un avion n'est souvent pas vu. L'application revient
alors à la route prévue, ou au GPS.

Tests : `web/test/relais.test.mjs`, avec un faux OpenSky.
