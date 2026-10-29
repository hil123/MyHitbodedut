# Cahier des charges — publication sur Google Play

Rédigé le 29/10/2026, à partir de l'état du dépôt à `e21d72e` et des pages
officielles de Google lues le même jour. Ce fichier existe pour que la
publication ne vive pas dans une conversation.

Chaque affirmation porte un statut :

- **Vérifié** — lu dans le code du dépôt, ou dans une page Google / OpenSky
  citée en fin de document.
- **Déduit** — conséquence raisonnée de ce qui est vérifié, à confirmer dans la
  Play Console au moment de la saisie.
- **Supposé** — hypothèse de travail, à trancher par le propriétaire.

---

## 1. Objet et périmètre

**Objet.** Publier l'application Android `com.myhitbodedut` sur Google Play,
avec toutes ses fonctions, et remplacer à terme la distribution par GitHub
Releases + Obtainium (README, « Mettre à jour »).

**Dans le périmètre** : l'édition **complète** (`full`), barrière du matin
comprise, et ce qu'elle contient (vérifié, `AndroidManifest.xml` et
`src/full/AndroidManifest.xml`) :

| Domaine | Fonctions |
| --- | --- |
| Hitbodedout | Séance minutée en service de premier plan, sujet facultatif, hitbodedout guidée, « je ne sais pas quoi dire », journal, parcours, paliers et trophées, célébration |
| Barrière du matin | `GateService` + `BlockerView` sur les applications choisies (`AppPickerActivity`), fenêtre horaire, report, Shabbat / fête |
| Musique | 33 morceaux livrés (130 Mo), morceaux ajoutés par le sélecteur de fichiers, lecture écran éteint |
| Textes | Siddour, Tehilim, Tikoun haKlali, Likoutei Tefilot par sujet, 'hizouk, bibliothèque, livres, Halakha (Yalkout Yossef), hilloulot, recherche globale |
| Calendrier | Zmanim (KosherJava), calendrier hébraïque, Birkat haLevana, alerte « Chabbat dans 3 h », boussole vers Jérusalem |
| Vol | Zmanim et prières en vol, rappels (Tefilat haDerekh, Omer), **suivi en direct (OpenSky) et route d'un numéro de vol (adsbdb)** |
| Système | 6 widgets, rappels quotidiens, « Ne pas déranger » pendant la séance, partage d'un jour en image, sauvegarde JSON, 3 langues (fr, en, he) |

**Hors périmètre** :

- **Édition `light`** — sa raison d'être est Play Protect hors boutique (README,
  « Deux éditions »). Sur Play, ce motif disparaît. Elle reste un **plan de
  repli** si la revue refuse la barrière (§ 9). *Déduit.*
- **Version web** (`myhitbodedut-web`) — publiable plus tard comme Trusted Web
  Activity, sujet distinct.
- **Version Nokia** (`MyHitbodedut.jar`, J2ME) — ce n'est pas une application
  Android, Google Play ne la distribue pas. *Déduit.*
- Toute monétisation (achat, publicité, abonnement) — voir § 6.3, la licence
  CC BY-NC-SA du Yalkout Yossef l'interdit en pratique.

---

## 2. État des lieux technique

| Élément | Valeur dans le dépôt | Exigence Play | Statut |
| --- | --- | --- | --- |
| `targetSdk` | **36** depuis le 29/10 (était 35) | **36** pour toute nouvelle app depuis le 31/08/2026 ; prolongation possible jusqu'au 01/11/2026 seulement | **Fait** — compilé et testé ; reste l'essai sur appareil (§ 2.1) |
| `compileSdk` | 36, avec le plugin Android 8.11.1 (était 8.7.3) | ≥ `targetSdk` | **Fait** |
| `minSdk` | 26 | aucune contrainte | OK |
| Format | APK pour GitHub ; **AAB** pour Play (`-Pplay=true bundleFullRelease`, flux `play.yml`) | **AAB** obligatoire pour une nouvelle app | **Fait** — AAB de 28,5 Mo construit le 29/10 |
| Taille | `res/raw` = 160 Mo (130 Mo de MP3, 30 Mo de JSON) | Module de base ≤ 500 Mo compressés ; au-delà de 200 Mo, avertissement non bloquant en données mobiles | OK — vérifié |
| Bibliothèques natives | aucune `.so` | page mémoire 16 Ko exigée pour les `.so` ciblant API 35+ | Sans objet — vérifié |
| Signature | clé de dev pour GitHub ; `play.yml` exige la clé d'envoi (secrets `PLAY_*`) et refuse un AAB signé par la clé de dev | clé d'envoi privée + Play App Signing | **Reste à faire par le propriétaire** : créer la clé et les secrets (§ 5.1, § 5.2) |
| `versionCode` | `1 + github.run_number` | strictement croissant | OK, à garder |
| R8 / réduction | activés en release | — | OK ; garder le fichier de correspondance (§ 5.4) |
| Écran de relecture | masqué hors `BuildConfig.DEBUG` | pas de fonction cachée en production | OK — vérifié |
| `usesCleartextTraffic` | `false` (full) | — | OK |

### 2.1 Passage à `targetSdk 36` — ce qu'il faut éprouver

Changer le chiffre ne suffit pas : Android 16 modifie des comportements que
l'application utilise. À tester sur un appareil Android 16 réel, en plus de la
liste déjà notée dans la feuille de route (proposition 12) :

- l'affichage bord à bord, imposé sans possibilité de s'y soustraire *(déduit
  des notes de version Android 15/16, à vérifier écran par écran)* ;
- les grands écrans : Android 16 ignore les restrictions d'orientation et de
  redimensionnement au-delà de 600 dp ; `CelebrationActivity` déclare des
  `configChanges`, à vérifier en tablette et en fenêtre libre ;
- les services de premier plan `specialUse` et `mediaPlayback` démarrés depuis
  une notification ou une alarme ;
- l'overlay `TYPE_APPLICATION_OVERLAY` et la lecture `UsageStatsManager`
  après redémarrage, économie de batterie, autorisation retirée en cours de
  séance.

**Fait le 29/10** (vérifié par compilation, 570 tests unitaires et lint) :

- **Plein écran** : `Barres.kt`, posé pour tous les écrans par
  `Apparence.installer`. Chaque écran est affiché sous les barres sur toutes
  les versions d'Android, puis reçoit en marge intérieure la place des barres,
  de la découpe et du clavier ; la bande du haut garde la couleur de barre
  d'état de la palette. La dérogation `values-v35/themes.xml` est retirée, et
  `FiabiliteTest` interdit son retour.
- **Geste retour** : `android:enableOnBackInvokedCallback="false"` sur
  `<application>`, pour que l'écran de garde continue de recevoir la touche
  retour. Dérogation temporaire selon Google, à remplacer un jour par
  `OnBackInvokedCallback`.
- **Grands écrans** : rien à faire, l'application ne verrouille aucune
  orientation.

**Pas encore vérifié** : le rendu sur un appareil ou un émulateur Android 16.
Aucun émulateur n'était disponible ; c'est le premier point de la recette du
§ 8, avec un œil sur chaque écran (titres sous la barre d'état ? boutons
sous la barre de gestes ? clavier qui cache un champ ?).

**Critère d'acceptation** : `./gradlew -Pplay=true testFullDebugUnitTest
bundleFullRelease` passe (vrai le 29/10), et la recette du § 8 passe sur
Android 13, 14, 15 et 16.

---

## 3. Points bloquants hors code

Trois sujets empêchent une publication propre tant qu'ils ne sont pas tranchés.
Ils viennent avant tout le reste.

### 3.1 Suivi de vol en direct — licence OpenSky **(bloquant)**

`SuiviVol.kt` interroge `https://opensky-network.org/api/states/all` en accès
anonyme. Les conditions d'utilisation d'OpenSky (vérifié, page citée) :

> « Use of the REST API in any operational capacity — including integration
> into a live product, service, or automated system […] — requires a previous
> written agreement, even for non-profit or governmental entities. »

Une application publiée sur Google Play est un produit en service : il faut
l'accord écrit d'OpenSky.

**Décision du 29/10 : fonction conservée.** Le propriétaire déclare avoir
déjà reçu cet accord. Restent trois vérifications, à faire sur le texte de
l'accord lui-même, qui n'est pas dans le dépôt :

- [ ] l'accord couvre une **application mobile publique et gratuite**
      distribuée sur Google Play, et pas seulement le site ou un usage de test ;
- [ ] il autorise l'**accès anonyme** que fait `SuiviVol.kt`, ou bien il
      impose un compte (identifiants d'API). Dans ce cas, ne pas mettre
      d'identifiants dans l'APK, où n'importe qui les extrairait : passer par
      le relais (`-PrelaisVol=https://…`, `tools/relais_vol`), qui les garde
      côté serveur ;
- [ ] il fixe une **attribution** : l'app affiche déjà « Données
      aéronautiques : OpenSky Network » (`vol_source_opensky`), à aligner sur
      le libellé exigé s'il y en a un.

Conserver l'écrit hors dépôt, avec ceux des ayants droit (§ 3.2) : Google ou
OpenSky peuvent le demander.

**Route d'un numéro de vol (adsbdb) — décision du 29/10 : conservée, après
vérification.** Ce qu'on a trouvé le 29/10 (vérifié, adsbdb.com et dépôt
GitHub `mrjackwills/adsbdb`) :

- pas de conditions d'utilisation publiées : API publique, gratuite, sans
  compte, seulement des limites de débit (512 requêtes par minute avant
  blocage de 60 s). `RouteVol` ne fait qu'une requête par toucher, loin du
  seuil ;
- le logiciel est sous licence MIT ;
- **les routes elles-mêmes** sont l'œuvre de David J. Taylor et Jim Mason, et
  « may not be copied, published, or incorporated into other databases
  without the explicit permission of David J Taylor ».

Afficher à l'utilisateur la route du vol qu'il demande n'est ni une copie de
la base ni une base concurrente. `RouteVol` ne stocke aucune route, et
l'attribution est déjà affichée (`vol_source_adsbdb`). *Déduit : lecture
raisonnable, pas un avis juridique.* Deux règles en découlent :

- ne jamais ajouter de cache ou de liste de routes embarquée construite à
  partir d'adsbdb ;
- facultatif mais prudent : un courriel au mainteneur d'adsbdb décrivant
  l'usage (une requête au toucher, attribution affichée), dont la réponse
  rejoint les autres écrits.

**Conséquence pour Play.** L'édition complète garde la permission
`INTERNET`. Le formulaire Sécurité des données doit déclarer les deux envois
(§ 6.2, ligne « Vol en direct conservé »), et les règles de confidentialité
doivent les décrire (§ 6.1).

### 3.2 Droits sur la musique livrée **(bloquant à la revue)**

33 morceaux sont embarqués, dont des enregistrements du Yanuka, d'Ishay Ribo,
Motty Steinmetz, MBD (feuille de route, lignes 242–243). Les autorisations
sont **déclarées** par le propriétaire (« j'ai obtenu les autorisations
légales des ayants droit »), mais aucun écrit n'est dans le dépôt.

Google Play retire une application sur simple réclamation d'un ayant droit, et
peut suspendre le compte en cas de récidive. *Déduit de la politique de
propriété intellectuelle de Play.* La distribution passe d'un cercle restreint
(dépôt privé) au public mondial : c'est une diffusion plus large que celle
pour laquelle les droits ont été évoqués, la même remarque que la feuille de
route faisait au sujet d'un hébergement public.

**Exigence** : pour chaque morceau livré, un écrit de l'ayant droit couvrant
« distribution publique gratuite dans une application mobile, via Google
Play, monde entier », conservé hors dépôt et produit à la première demande de
Google. À défaut, retirer le morceau de la version Play ; l'utilisateur peut
toujours l'ajouter depuis son téléphone (`MusicActivity` → `OpenDocument`).

**Décision du 29/10.** Le propriétaire a l'accord du Yanuka. La provenance
des 30 niggounim sans titre (`niggoun_01` à `niggoun_33`) est inconnue : leurs
noms hébreux ont été perdus au transfert. *המלך בשדה* est déclaré libre de
droits ; l'autorisation d'Ishay Ribo et de MBD n'a pas été demandée. D'où, **pour la version Play seulement** :

| Morceau livré | Version Play | Version GitHub |
| --- | --- | --- |
| `yanuka_nigoun_3` — ניגון — הינוקא (3) | **Livré** (accord du Yanuka, à conserver par écrit) | Livré |
| `niggoun_01` … `niggoun_33` (30 fichiers) | **Retirés** tant que la provenance n'est pas établie | Livrés |
| `hamelech_basade` — המלך בשדה | **Livré** — déclaré libre de droits par le propriétaire (29/10) ; voir la réserve ci-dessous | Livré |
| `ribo_sans_titre` — Ishay Ribo | **Retiré** — autorisation non demandée (29/10), comme pour MBD | Livré |
| Toute la musique de **נעם שפייר** et de **יניר קילינסקי** (à recevoir) | **À livrer** — accord écrit reçu par le propriétaire (29/10) pour télécharger et utiliser toutes leurs musiques | À livrer, ainsi que dans la version web |

**Réserve sur המלך בשדה.** Une mélodie traditionnelle peut être libre alors
que son **enregistrement** ne l'est pas : l'interprète et le producteur ont
leurs propres droits. Noter d'où vient ce fichier (interprète, source, date)
et ce qui le rend libre ; c'est ce que Google demanderait en cas de
réclamation.

**נעם שפייר et יניר קילינסקי** : accord écrit couvrant toutes leurs musiques,
à conserver hors dépôt avec les autres écrits. Les fichiers sont à fournir
par le propriétaire (dossier `musique/`, avec le titre de chaque morceau) ;
ils entrent dans l'app, version Play comprise, et dans la version web.

**Ishay Ribo, MBD, Motty Steinmetz, Shmuel** : autorisation non demandée.
Hors de la version Play ; le propriétaire décide de leur place dans la
version GitHub, qui est elle aussi une diffusion.

Audio de la version Play : environ 16 Mo (Yanuka 6,7 + המלך בשדה 9,7) au
lieu de 130 Mo. Un morceau
retiré revient par simple mise à jour dès que son écrit existe ; si un
niggoun s'avère être du Yanuka, il rentre sous le même accord. Pour cela, il
faut retrouver les titres hébreux d'origine (fichiers sources reçus le
22/09).

**Mise en œuvre — faite le 29/10.** `main/res/raw` ne contient plus que les
morceaux livrés par Play, déclarés dans `MusiquePlay.kt`. Les 31 autres sont
dans `src/musique_hors_play/` (fichiers et liste `MusiqueHorsPlay.kt`), que
seule la construction GitHub ajoute ; avec `-Pplay=true`, Gradle prend
`src/musique_play/`, où la même liste est vide. Vérifié sur l'AAB : il ne
contient que `hamelech_basade.mp3` et `yanuka_nigoun_3.mp3`. `MusicFilesTest`
tient la liste ; `play.yml` recontrôle l'AAB lui-même. Le site web garde les
33 morceaux, comme avant.

**Ajouter un morceau autorisé** (נעם שפייר, יניר קילינסקי) : le fichier dans
`main/res/raw`, sa ligne dans `MusiquePlay.kt`, son titre dans
`values/strings.xml` — ce que fait `tools/ajoute_musique.py`. Il entre alors
dans les deux versions et dans le site. `tools/echange_musique.py`, qui
recomposait une liste unique, est suspendu.

### 3.3 Droits sur les textes

| Source (`res/raw/*.json`) | Licence déclarée | Action |
| --- | --- | --- |
| Tanakh, Likoutei Moharan, Sefer haMidot, Siddour Edot haMizra'h, Perek Chira | Domaine public (via Sefaria) | Aucune |
| Kitsour Choul'han Aroukh Yalkout Yossef (Torat Emet) | **CC BY-NC-SA 2.5** + licence reçue | Application **gratuite, sans publicité ni achat** ; crédit affiché ; garder l'écrit de la licence |
| Livres du Yanuka (מכון ים שלמה) | « כל הזכויות שמורות », autorisation déclarée | Écrit à conserver, comme 3.2 |
| or-breslev.co.il | Accord déclaré le 13/10 | Écrit à conserver |
| Villes (GeoNames) | CC BY 4.0, attribution dans les réglages | OK — vérifié |
| Polices (6 × OFL) | `assets/licences/` | OK — vérifié |
| Si'hot haRan, Chiv'hei haRan, Likoutei Etsot (rabenubook), Likoutei Tefilot (Maleh Vigadish 2021) — les 7 anciennes entrées `"unknown"` | Œuvres de Rabbi Natan (m. 1844), domaine public ; licence des éditions non déclarée par Sefaria (vérifié le 29/10) | **Tranché le 29/10** : livrées en texte seul, sans la vocalisation de l'éditeur ; licence réécrite « Public Domain (texte seul) » ; `LicencesEditionsTest` interdit tout retour d'un « unknown » ou de la vocalisation |

---

## 4. Compte développeur

| Étape | Détail | Statut |
| --- | --- | --- |
| Créer le compte | play.google.com/console, compte Google dédié recommandé | — |
| Frais d'inscription | **25 USD, une fois**, carte non prépayée | Vérifié |
| Type de compte | **Personnel** (nom réel affiché) ou **Organisation** (exige un numéro D-U-N-S, mais dispense du test fermé ci-dessous) | **Tranché le 29/10 : personnel, nouveau** |
| Vérification d'identité | pièce d'identité, adresse ; nom public du développeur | Vérifié |
| Vérification d'appareil | accès à un appareil Android via l'appli Play Console | Vérifié |
| **Test fermé obligatoire** (compte personnel créé après le 13/11/2023) | **12 testeurs inscrits sans interruption pendant 14 jours**, puis demande d'accès à la production | Vérifié |
| Vérification développeur Android | enregistrer le nom de paquet `com.myhitbodedut` (obligatoire depuis le 30/09/2026 au Brésil, en Indonésie, à Singapour et en Thaïlande, puis élargi) | Vérifié pour la date ; extension mondiale à suivre |

L'enregistrement du nom de paquet sert aussi la distribution hors boutique :
sans lui, les APK des GitHub Releases finiront par ne plus s'installer sur les
appareils certifiés des régions concernées. *Déduit.*

---

## 5. Construction et signature

### 5.1 Clés

1. Générer une **clé d'envoi** neuve, jamais versionnée :
   ```bash
   keytool -genkeypair -v -keystore upload-myhitbodedut.jks \
     -keyalg RSA -keysize 4096 -validity 10000 -alias upload
   ```
2. Activer **Play App Signing** au premier envoi : Google garde la clé de
   signature finale, la clé d'envoi perdue se remplace sur demande.
3. Sauvegarder la clé d'envoi et ses mots de passe à deux endroits hors ligne.
4. La clé de dev (`keystore/myhitbodedut-dev.jks`) reste pour les éditions
   GitHub. Conséquence : **une installation GitHub ne se met pas à jour par
   Play** (signatures différentes). Il faudra exporter le JSON, désinstaller,
   installer depuis Play, réimporter. À écrire dans la note de version et dans
   le README. *Déduit.*

### 5.2 Intégration continue

Flux `.github/workflows/play.yml` **(fait le 29/10)**, lancé à la main
(Actions → Play → Run workflow) ou par une étiquette `play-v*` :

- secrets GitHub à créer : `PLAY_KEYSTORE_BASE64` (la clé d'envoi en base64,
  `base64 -w0 upload-myhitbodedut.jks`), `PLAY_KEYSTORE_PASSWORD`,
  `PLAY_KEY_ALIAS`, `PLAY_KEY_PASSWORD`. Sans eux le flux s'arrête ;
- `./gradlew -Pplay=true testFullDebugUnitTest`, puis
  `-Pplay=true bundleFullRelease` signé avec la clé d'envoi ;
- contrôle de l'AAB : seuls les morceaux de `MusiquePlay.kt`, et pas de
  signature par la clé de développement ;
- artefact `play-N` (30 jours) : `MyHitbodedut-N.aab` et `mapping-N.txt` ;
- envoi vers la piste de test interne : optionnel, par l'API Google Play
  Developer avec un compte de service. À défaut, envoi manuel dans la console.

### 5.3 Numérotation

`versionCode = 1 + run_number` convient tant qu'**un seul** flux produit les
AAB Play. Ne jamais réutiliser un `versionCode` déjà envoyé, même refusé.
`versionName` : garder `1.N`.

### 5.4 Fichier de correspondance R8

Envoyer `mapping.txt` avec chaque AAB (Play Console → App bundle explorer),
sinon les plantages remontés par Android vitals sont illisibles.

---

## 6. Rubrique « Contenu de l'application » (Play Console)

Tout est obligatoire avant le premier envoi en test fermé.

### 6.1 Règles de confidentialité

URL publique obligatoire. **À rédiger et héberger** (GitHub Pages du dépôt
web, par exemple). Contenu minimal, fidèle au code :

- aucune donnée personnelle collectée par le développeur, pas de compte, pas
  d'analytique, pas de publicité ;
- journal, séances, réglages : stockés sur l'appareil seulement ; le journal
  est exclu de la sauvegarde automatique Android ;
- localisation : demandée seulement si l'utilisateur choisit « Ma position »
  ou « Position exacte », utilisée sur l'appareil pour les zmanim et la
  boussole, jamais transmise ;
- autorisations spéciales (overlay, données d'utilisation) : servent
  uniquement à la barrière configurée par l'utilisateur ; aucune liste
  d'applications ni historique d'usage ne quitte l'appareil ;
- si le suivi de vol est conservé : ce qui part chez OpenSky (zone
  géographique approchée) et chez adsbdb (numéro de vol), quand, et seulement
  si l'utilisateur active la fonction ;
- contact du développeur, date de mise à jour.

### 6.2 Sécurité des données

| Cas | Réponse |
| --- | --- |
| Vol en direct retiré (option A, § 3.1) | « Aucune donnée collectée ni partagée ». Aucune donnée ne quitte l'appareil. |
| Vol en direct conservé | Déclarer **Position approximative** (zone envoyée à OpenSky) et **Autres identifiants / infos de l'app** pour le numéro de vol (adsbdb), *facultatif pour l'utilisateur*, chiffré en transit (HTTPS). *Déduit — la qualification exacte d'une boîte géographique se confirme dans le formulaire.* |

Dans tous les cas : chiffrement en transit = oui (HTTPS seulement) ;
suppression des données = l'utilisateur les efface en désinstallant ou par
les réglages (aucune donnée côté serveur).

### 6.3 Publicités, achats

Aucune publicité, aucun achat intégré. Prix : **gratuit**. Une application
gratuite ne peut plus devenir payante sur Play. *Règle Play connue, non relue
ce jour.*

### 6.4 Public cible et contenu

- **Tranche d'âge : 18 ans et plus** *(tranché le 29/10)*. Cocher une tranche
  inférieure à 13 ans fait entrer l'app dans le programme Familles, avec des
  exigences supplémentaires sans bénéfice ici.
- Classification du contenu (questionnaire IARC) : contenu religieux, aucune
  violence, aucun contenu généré par d'autres utilisateurs, aucun partage de
  position avec d'autres utilisateurs. Classement attendu : tout public.
  *Déduit.*
- Application d'actualités : non. Application gouvernementale : non.
  Fonctionnalités financières, santé : non.

### 6.5 Accès à l'application

Aucune connexion requise : cocher « Toutes les fonctionnalités sont
disponibles sans restriction ». Ajouter une note aux examinateurs (§ 7.3) sur
la manière de voir la barrière à n'importe quelle heure : le bouton qui
affiche l'écran de blocage à la demande (README, « Pourquoi la barrière
n'agit pas »).

### 6.6 Services de premier plan — déclaration

Requise pour toute app ciblant Android 14+ (vérifié). Deux services, trois
types :

| Service | Type | Description à saisir | Vidéo |
| --- | --- | --- | --- |
| `SessionService` | `specialUse` | Compte à rebours d'une séance de méditation lancée par l'utilisateur, avec le temps restant affiché, pour que le téléphone puisse être posé. | Oui : lancer une séance, verrouiller l'écran, montrer la notification |
| `SessionService` | `mediaPlayback` | Lecture du morceau choisi par l'utilisateur pendant sa séance, écran éteint. | Même vidéo |
| `GateService` | `specialUse` | Affiche un écran de confirmation par-dessus les applications choisies par l'utilisateur, pendant la fenêtre horaire qu'il a définie, jusqu'à ce qu'il confirme sa pratique du jour. | Oui : choisir une app, ouvrir l'app, voir l'écran, valider |

Les textes reprennent mot pour mot les `PROPERTY_SPECIAL_USE_FOREGROUND_SERVICE_SUBTYPE`
des manifestes : la cohérence entre manifeste et déclaration compte. Vidéos :
YouTube non répertoriée, moins de 2 minutes. *Format supposé.*

### 6.7 Autorisations sensibles — argumentaire

| Autorisation | Déclaration Play | Argument |
| --- | --- | --- |
| `SYSTEM_ALERT_WINDOW` | pas de formulaire, examinée en revue | Écran de garde volontaire, configuré par l'utilisateur |
| `PACKAGE_USAGE_STATS` | pas de formulaire, examinée en revue (politique « stalkerware ») | Ne surveille que **l'utilisateur lui-même**, à sa demande ; rien n'est transmis ; icône et notification permanentes, jamais cachée |
| `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION` | pas de déclaration (premier plan seulement) | Demandées au toucher de « Ma position » ; ville de la liste sans autorisation |
| `ACCESS_NOTIFICATION_POLICY` | aucune | « Ne pas déranger » pendant la séance, accordé dans Android |
| `POST_NOTIFICATIONS`, `RECEIVE_BOOT_COMPLETED`, `WAKE_LOCK` | aucune | Rappels, reprise après redémarrage, séance minutée |
| `QUERY_ALL_PACKAGES` | **non demandée** — `<queries>` limité aux apps lançables | Évite le formulaire le plus restrictif — vérifié |
| `SCHEDULE_EXACT_ALARM` / `USE_EXACT_ALARM` | **non demandée** — alarmes inexactes | Évite une déclaration — vérifié (README) |
| `AccessibilityService` | **non utilisé** | Évite le motif de retrait le plus fréquent — vérifié (README) |

Le couple overlay + données d'utilisation est celui des logiciels espions : la
revue s'y attardera. L'argument tient en une phrase, à répéter partout (fiche,
déclaration, note aux examinateurs, écran d'accueil de l'app) : **une barrière
que l'utilisateur installe sur son propre téléphone, contre ses propres
habitudes, qu'il peut couper à tout instant.**

Recommandé en plus : un écran d'**explication préalable** (« divulgation bien
visible ») avant d'envoyer l'utilisateur dans les réglages Android pour
chacune des deux autorisations spéciales. L'écran principal liste déjà les
autorisations ; vérifier que le texte dit ce qui est fait de la donnée.

---

## 7. Fiche Play Store

### 7.1 Textes (× 2 langues : en-US par défaut, iw-IL)

**Tranché le 29/10 : pas de fiche en français.** La fiche Play existe en
anglais et en hébreu seulement ; un téléphone en français (ou toute autre
langue) voit la fiche anglaise, langue par défaut. L'interface de
l'application garde ses trois langues.

| Champ | Limite | Proposition |
| --- | --- | --- |
| Titre | 30 caractères, **un par langue** | he : `MyHitbodedut – התבודדות` (23) · en : `MyHitbodedut – Hitbodedut` (25) *(tranché le 29/10)* |
| Description courte | 80 caractères | en : à rédiger, avec « התבודדות » · he : à rédiger |
| Description complète | 4 000 caractères | Plan : la pratique · la séance · la barrière du matin (avec l'argument de 6.7) · textes · zmanim et calendrier · widgets · vie privée (aucune donnée ne sort) · crédits et licences |

**Recherche en hébreu.** Play affiche la fiche dans la langue du téléphone
et indexe titre, description courte et description longue ; une traduction
automatique n'est pas indexée. Google ne dit pas si un mot hébreu tapé sur un
téléphone en français trouve la fiche hébraïque. D'où : « התבודדות » dans la
description courte anglaise, et les graphies latines (hitbodedut,
hisbodedus) dans une phrase de chaque description longue, y compris
l'hébraïque. *Vérifié pour l'indexation des champs (aide Play
« Get discovered on Google Play search ») ; déduit pour le reste.*

Interdits : classement ou récompense (« n° 1 », « meilleure »), emojis
répétés, mots-clés empilés. *Vérifié (bonnes pratiques Play).*

### 7.2 Visuels

| Élément | Spécification | Source dans le dépôt |
| --- | --- | --- |
| Icône | 512 × 512, PNG 32 bits avec alpha, ≤ 1 Mo | À exporter depuis `mipmap` |
| Image de présentation | 1024 × 500, JPEG ou PNG 24 bits sans alpha | À créer : personnage + היכל הציון (`scene_ouverture_tombe.webp`) |
| Captures téléphone | 2 minimum, 8 maximum, 320–3 840 px, 16:9 ou 9:16 | `captures-calendrier/`, à compléter : accueil, séance, barrière, siddour, zmanim, widgets |
| Captures tablette 7" et 10" | recommandées (l'app gère les grands écrans) | À produire |
| Vidéo | facultative, YouTube | Peut réutiliser la vidéo de 6.6 |

Les captures doivent montrer l'interface réelle de la version envoyée. Pas de
capture du suivi de vol si la fonction est retirée.

### 7.3 Note aux examinateurs (Play Console → Accès à l'application)

> L'application ne demande aucun compte. Pour voir la fonction de barrière :
> Accueil → autorisations (affichage par-dessus les autres applications,
> accès aux données d'utilisation) → choisir une application → bouton
> « Voir l’écran « Je suis avec Hachem » maintenant » (s'affiche à toute
> heure, sans effet).
> La barrière est un outil d'autodiscipline que l'utilisateur configure et
> peut désactiver à tout moment. Aucune donnée d'usage ne quitte l'appareil.

*Libellé repris de `action_preview_gate` (`values/strings.xml`) ; le traduire
pour la note en anglais.*

### 7.4 Catégorie et coordonnées

- Catégorie : **Style de vie** *(tranché le 29/10)*.
- Tags : méditation, prière, judaïsme, calendrier.
- Adresse e-mail de contact : obligatoire et publique. Site web : facultatif.

---

## 8. Recette avant soumission

À passer sur l'AAB signé, installé via la piste de test interne (pas en
local) :

- [ ] Installation propre depuis Play, sur Android 13, 14, 15, 16 dont au
      moins un Samsung et un Xiaomi (surcouches constructeur, README
      « Limites connues »)
- [ ] Barrière : armée, déclenchée sur une app choisie, validée, report,
      Shabbat, après redémarrage, après retrait d'autorisation en cours
- [ ] Séance de 60 min écran éteint, musique jusqu'au bout, fondu final
- [ ] Rappels, alerte Chabbat, Birkat haLevana : déclenchés en veille profonde
- [ ] Zmanim : ville de la liste sans autorisation ; « Ma position » ; mode
      avion
- [ ] Les 6 widgets posés et mis à jour
- [ ] Export / import JSON entre l'édition GitHub et l'édition Play
- [ ] Trois langues dans l'app, droite-à-gauche en hébreu
- [ ] Tablette, fenêtre libre, Chromebook (écran tactile non requis)
- [ ] Pré-lancement Play Console (rapport automatique) : aucun plantage,
      aucun avertissement d'accessibilité bloquant
- [ ] Aucun appel réseau si le suivi de vol est retiré (vérifier avec
      un proxy ou les statistiques de données d'Android)

---

## 9. Déroulé et calendrier

| # | Étape | Durée | Dépend de |
| --- | --- | --- | --- |
| 1 | Décisions § 3 : vol en direct, écrits des ayants droit, 7 licences inconnues | propriétaire | — |
| 2 | Compte développeur, paiement, vérifications | 1–3 jours | — |
| 3 | `targetSdk 36`, flux `play.yml`, musique de la version Play — **fait le 29/10** ; clé d'envoi et secrets — propriétaire | 1 jour | 1 |
| 4 | Règles de confidentialité en ligne | 1 jour | 1 |
| 5 | Contenu de l'application (§ 6), fiche (§ 7), vidéos | 2–3 jours | 3, 4 |
| 6 | Piste de test interne, recette § 8 | 3–5 jours | 3 |
| 7 | **Test fermé : 12 testeurs, 14 jours sans interruption** | **14 jours minimum** | 5, 6 |
| 8 | Demande d'accès à la production | quelques jours *(supposé)* | 7 |
| 9 | Revue de la version de production | de quelques jours à plus d'une semaine *(supposé)* | 8 |
| 10 | Déploiement progressif : 20 % → 50 % → 100 % | 1–2 semaines | 9 |

**Délai réaliste : 5 à 7 semaines**, dont deux incompressibles pour le test
fermé. Recruter les 12 testeurs dès l'étape 2 : il faut leurs adresses Gmail,
et chacun doit rester inscrit les 14 jours.

### Plan de repli si la revue refuse la barrière

1. Lire le motif exact. Répondre par le formulaire de recours avec la vidéo
   et l'argument de 6.7.
2. En cas de refus maintenu : publier l'édition **`light`**
   (`com.myhitbodedut.light`) — sans overlay ni données d'utilisation —
   comme application Play, et garder l'édition complète en distribution
   GitHub. La `light` est une **nouvelle application** : nouveau test fermé
   de 14 jours. *Déduit — l'exigence s'applique par application.*

---

## 10. Après la publication

- Chaque mise à jour : `targetSdk` au niveau exigé par Google chaque 31 août.
- Android vitals : taux de plantage < 1,09 %, ANR < 0,47 % *(seuils de
  mauvais comportement connus de Play, à reconfirmer)*.
- Répondre aux avis, surtout ceux qui concernent la barrière.
- README : remplacer la section « Publier sur Google Play » par le lien de la
  fiche, et documenter la migration GitHub → Play (§ 5.1, point 4).
- Garder les écrits des ayants droit et des licences accessibles : une
  réclamation peut arriver des mois après.

---

## 11. Décisions attendues du propriétaire

1. ~~**Suivi de vol en direct**~~ — **tranché le 29/10** : conservé (accord
   OpenSky reçu, à vérifier selon § 3.1) ; route adsbdb conservée.
2. ~~**Musique**~~ — **tranché le 29/10** : la version Play livre le niggoun
   du Yanuka et המלך בשדה ; les 30 niggounim sans titre et Ishay Ribo
   restent hors de Play (§ 3.2).
3. ~~**7 textes de licence inconnue**~~ — **tranché le 29/10** : domaine
   public, texte seul sans vocalisation (§ 3.3).
4. ~~**Compte personnel ou organisation**~~ — **tranché le 29/10** : compte
   **personnel, nouveau**. Test fermé obligatoire : 12 testeurs, 14 jours
   (§ 4, § 9 étape 7). Recruter les testeurs dès la création du compte.
5. ~~**Titre, catégorie, tranche d'âge**~~ — **tranchés le
   29/10** : Style de vie, 18 ans et plus, un titre par langue (§ 7.1).
6. ~~**Édition `light` sur Play**~~ — **tranché le 29/10** : non, gardée
   en réserve (plan de repli du § 9) ; elle reste distribuée par GitHub.

---

## Sources lues le 29/10/2026

- Exigences de niveau d'API cible — support.google.com/googleplay/android-developer/answer/11926878
- Test obligatoire des nouveaux comptes personnels — support.google.com/googleplay/android-developer/answer/14151465
- Limites de taille Play — support.google.com/googleplay/android-developer/answer/9859372
- Android App Bundle — developer.android.com/guide/app-bundle
- Types de services de premier plan et déclaration Play — developer.android.com/develop/background-work/services/fgs/service-types ; support.google.com/googleplay/android-developer/answer/13392821
- Bonnes pratiques de fiche (titre 30, description courte 80, longue 4 000) — support.google.com/googleplay/android-developer/answer/13393723
- Frais d'inscription et étapes du compte — support.google.com/googleplay/android-developer/answer/6112435 ; answer/14659200
- Vérification développeur Android (30/09/2026, quatre pays) — developer.android.com/developer-verification, cité par un article du 29/08/2026 ; la page Google elle-même n'a pas été relue
- Conditions d'utilisation OpenSky — opensky-network.org/about/terms-of-use
- Spécifications des visuels (icône, image de présentation, captures) — recoupées entre deux guides tiers datés de 2026 ; la page Google correspondante (answer/9866151) ne les détaille pas dans l'extrait lu
