# MyHitbodedut

Deux applications autour d'une même pratique quotidienne.

| Dossier | Ce que c'est |
| --- | --- |
| `android/` | Application Android native : barrière du matin qui bloque les applications choisies tant que la hitbodedout n'est pas validée |
| `index.html` | Version web autonome : suivi d'habitudes dans le navigateur, sans installation |

## L'application Android

Inspirée du fonctionnement de קודם תפילין, qui verrouille le téléphone le matin
jusqu'à confirmation de la mise des tefillin.

**Le principe.** Pendant une fenêtre horaire que vous définissez, par défaut
06:00 à 11:00, l'ouverture d'une application que vous avez désignée fait
apparaître un écran plein : « Tu as fait ta hitbodedout ? ». Deux issues
seulement, valider la pratique ou reporter de quelques minutes. Une fois la
journée validée, la barrière ne se déclenche plus jusqu'au lendemain.

**Ce qu'elle suit.** Les jours validés, la série en cours, et une bande des
sept derniers jours sur l'écran principal.

### Comment le blocage fonctionne

| Élément | Rôle |
| --- | --- |
| `GateService` | Service au premier plan, interroge `UsageStatsManager` pour connaître l'application affichée |
| `BlockerView` | Écran plein ajouté au `WindowManager` en `TYPE_APPLICATION_OVERLAY`, intercepte le bouton retour |
| `Store` | `SharedPreferences` : jours validés, fenêtre, report, liste des applications bloquées |
| `BootReceiver` | Relance la surveillance après un redémarrage |

Le choix de `UsageStatsManager` plutôt qu'un `AccessibilityService` est
délibéré. L'accessibilité détecterait les changements d'application
instantanément, mais Google Play en restreint l'usage aux applications
d'accessibilité, et s'en servir pour autre chose expose à un retrait.

### Autorisations demandées

| Autorisation | Pourquoi |
| --- | --- |
| Affichage par-dessus les autres applications | Afficher l'écran de garde |
| Accès aux données d'utilisation | Savoir quelle application est au premier plan |
| Notifications | Android impose une notification permanente aux services au premier plan |
| Batterie sans restriction | Éviter que le système suspende la surveillance |
| Démarrage automatique | Reprendre après un redémarrage |

Les deux premières se donnent à la main dans les Réglages, Android ne permet pas
de les accorder par une simple boîte de dialogue. L'écran principal contient la
liste et un bouton vers chaque écran système.

La visibilité des autres applications passe par un élément `<queries>` limité
aux applications lançables, et non par `QUERY_ALL_PACKAGES`, dont Google Play
réserve l'usage à une courte liste de cas approuvés.

### Aucune donnée ne sort de l'appareil

Tout est dans les `SharedPreferences` locales. Pas de compte, pas de serveur,
pas d'analytique, pas de réseau. L'application ne déclare aucune permission
Internet.

### Construire

Le plus simple, sans rien installer : le workflow GitHub Actions `Android`
compile à chaque envoi sur `main` et publie l'APK de débogage en artefact.
Ouvrez l'onglet Actions, la dernière exécution, puis `myhitbodedut-debug-apk`.

En local, avec Android Studio ou le SDK en ligne de commande :

```bash
cd android && ./gradlew assembleDebug
# → app/build/outputs/apk/debug/app-debug.apk
```

Pré-requis : JDK 17 et le SDK Android, plateforme 35.

### Publier sur Google Play

Ce que le dépôt fournit et ce qui reste à faire :

- [x] Code source complet et compilable
- [x] `applicationId`, icône, nom, traductions français, hébreu, anglais
- [ ] Compte développeur Google Play, frais uniques de 25 dollars
- [ ] Clé de signature et build `release` signé, `./gradlew bundleRelease`
- [ ] Politique de confidentialité en ligne, obligatoire dès qu'une
      autorisation sensible est demandée
- [ ] Formulaire Sécurité des données, à remplir « aucune donnée collectée »
- [ ] Justification de l'usage du service au premier plan de type `specialUse`

À savoir avant de soumettre : l'affichage par-dessus les autres applications et
l'accès aux données d'utilisation attirent l'attention de la revue. Décrire
honnêtement la fonction, une barrière volontaire que l'utilisateur installe et
configure lui-même, est la meilleure approche.

### Limites connues

- **Contournable, et c'est voulu.** L'écran de garde est un ralentisseur, pas un
  verrou. Passer par les applications récentes, redémarrer, ou désactiver
  l'application dans les Réglages permet de l'éviter. Comme pour l'app de
  référence, l'objectif est de rompre l'automatisme du matin, pas d'enfermer.
- **Détection à environ une seconde.** La surveillance interroge le système
  toutes les 900 ms. L'application bloquée peut donc rester visible un instant
  avant que l'écran n'apparaisse.
- **Non testée sur un appareil physique** à ce stade. Le code compile en
  intégration continue, mais le comportement de l'overlay varie selon les
  surcouches constructeur, en particulier Xiaomi, Huawei et Samsung, qui
  ajoutent leurs propres restrictions d'arrière-plan.
- **Android uniquement.** iOS n'autorise pas ce type de blocage par une
  application tierce.

## La version web

`index.html` est autonome, à ouvrir dans un navigateur ou à servir :

```bash
python3 -m http.server 8080
```

Suivi d'habitudes générique, stockage dans le `localStorage`, Tailwind et Lucide
chargés depuis un CDN, donc connexion nécessaire au chargement.
