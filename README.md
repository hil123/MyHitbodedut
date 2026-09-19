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
apparaître un écran plein : « Tu as fait ta hitbodedout ? ». Trois issues :
commencer la séance tout de suite, déclarer qu'elle est déjà faite, ou reporter
de quelques minutes. Une fois la journée validée, la barrière se tait jusqu'au
lendemain.

**La séance.** Un minuteur tourne dans un service d'arrière-plan, pour que le
téléphone puisse être posé. La durée est réglable, par défaut soixante minutes.
La journée est validée par le temps réellement passé, pas par une simple
déclaration. Si une application bloquée est ouverte pendant la séance, l'écran
rappelle le temps restant et ne propose que d'y retourner.

**Le journal.** Chaque séance peut recevoir une note libre. L'historique garde
la durée et la note, jour par jour.

**Pourquoi la barrière n'agit pas.** L'écran principal affiche en permanence
l'état de la barrière et, si elle n'agit pas, la raison exacte : autorisation
manquante, blocage éteint, aucune application choisie, journée déjà validée,
jour sans obligation, report en cours, ou heure hors de la fenêtre. Un bouton
affiche l'écran de blocage à la demande, à n'importe quelle heure et sans
aucun effet, pour vérifier qu'il fonctionne.

**Shabbat et fêtes.** Le samedi, la barrière se met en pause et la série n'est
pas cassée. Un interrupteur permet de déclarer le jour courant comme jour de
fête. La détection se fait par le jour de la semaine, ce qui est exact pour une
fenêtre située le matin ; une fenêtre qui traverserait le coucher du soleil
demanderait un calcul de zmanim, donc la localisation.

**Rappel quotidien.** Une notification à l'heure choisie, tant que la pratique
du jour n'est pas faite. L'alarme est volontairement inexacte : elle se
déclenche même en veille profonde sans exiger la permission d'alarme exacte,
que Google Play restreint.

**Sauvegarde.** Export et import d'un fichier JSON par le sélecteur de fichiers
du système, sans aucune permission de stockage. À faire de temps en temps :
sinon un téléphone perdu emporte l'historique.

### Comment le blocage fonctionne

| Élément | Rôle |
| --- | --- |
| `GateStatus` | Source unique de vérité : la barrière est-elle armée, et sinon pourquoi |
| `GateService` | Service au premier plan, interroge `UsageStatsManager` pour connaître l'application affichée |
| `BlockerView` | Écran plein en `TYPE_APPLICATION_OVERLAY`, intercepte le bouton retour |
| `SessionService` | Décompte de la séance, notification avec le temps restant |
| `Store` | `SharedPreferences` : journées, durées, notes, réglages, sauvegarde JSON |
| `Reminder` | Rappel quotidien par alarme inexacte |
| `BootReceiver` | Relance la surveillance et le rappel après un redémarrage |

Le choix de `UsageStatsManager` plutôt qu'un `AccessibilityService` est
délibéré. L'accessibilité détecterait les changements d'application
instantanément, mais Google Play en restreint l'usage aux applications
d'accessibilité, et s'en servir pour autre chose expose à un retrait.

### Les illustrations

Le personnage vient d'une planche de seize portraits et de quatre rendus pleins.
La planche est découpée en une grille de 4 par 4, chaque vignette est détourée,
puis exportée en WebP dans `res/drawable-nodpi`. Vingt images, 492 Ko au total.

Le détourage mérite une note. Le fond des rendus est mesuré entre 254 et 255,
et la chemise blanche du personnage atteint elle aussi 255 : un seuil de
luminosité trouerait le vêtement. `tools/decoupe_planche.py` identifie donc le
fond par connexité depuis le bord de l'image, ce qui laisse intacte toute zone
claire enfermée dans la silhouette.

Il subsiste un liseré clair le long des contours, inévitable sur un fond blanc.
Les écrans sombres posent donc les personnages sur un panneau clair, où ce
liseré ne se voit pas.

`Character.kt` répartit les images par moment : accueil, appel à la pratique,
pendant la séance, après la séance. Le choix est stable sur une journée entière
et change le lendemain.

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
compile à chaque envoi sur `main` et publie l'APK dans la release `latest`.

Lien de téléchargement direct, dépôt privé donc connexion GitHub nécessaire :
<https://github.com/hil123/MyHitbodedut/releases/latest/download/MyHitbodedut.apk>

L'APK est signé par la clé stable de `android/keystore`, et n'est pas déboguable.
Android demandera d'autoriser l'installation depuis une source inconnue pour le
navigateur utilisé.

En local, avec Android Studio ou le SDK en ligne de commande :

```bash
cd android && ./gradlew assembleDebug
# → app/build/outputs/apk/debug/app-debug.apk
```

Pré-requis : JDK 17 et le SDK Android, plateforme 35.

### Deux éditions

| Édition | Contenu | Permissions sensibles |
| --- | --- | --- |
| `MyHitbodedut.apk` | Tout, barrière du matin comprise | Affichage par-dessus les autres applications, accès aux données d'utilisation |
| `MyHitbodedut-Light.apk` | Minuteur, journal, série, rappel, Shabbat, sauvegarde | Aucune |

L'édition légère existe pour une raison précise. Le couple affichage par-dessus
les autres applications et accès aux données d'utilisation est exactement celui
des logiciels de surveillance, et Play Protect refuse volontiers une application
hors boutique qui le demande. Sans la barrière, ce motif disparaît, et
l'installation passe sans discussion.

Elle sert aussi de test : si la légère s'installe et l'autre non, le blocage
vient bien des permissions, et pas du fichier ni du téléphone.

Les deux portent un identifiant différent et peuvent coexister sur le même
téléphone. Leurs données ne sont pas partagées ; l'export JSON permet de passer
de l'une à l'autre.

### Si l'installation échoue

Deux causes distinctes, souvent confondues.

**Une version antérieure est déjà installée.** Jusqu'au 19 septembre 2026, chaque
compilation était signée par une clé de débogage régénérée à neuf, donc une
identité différente à chaque fois. Android refuse d'installer une application
par-dessus une autre signée différemment. Le dépôt utilise désormais une clé
fixe, mais il faut désinstaller une fois les versions posées avant ce changement.

**Play Protect bloque l'installation.** Une application installée hors du Play
Store, qui demande l'affichage par-dessus les autres applications et l'accès aux
données d'utilisation, présente exactement le profil d'un logiciel de
surveillance. Le blocage est attendu. Sur l'avertissement, ouvrir « Plus de
détails » puis « Installer quand même ». Le blocage disparaît avec une
publication signée sur le Play Store, pas avant.

**Après l'installation, sur Android 13 et au-delà**, le système interdit
d'accorder l'accès aux données d'utilisation à une application installée hors
boutique tant que l'on n'a pas levé la restriction : informations sur
l'application, menu à trois points, « Autoriser les paramètres restreints ».

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
