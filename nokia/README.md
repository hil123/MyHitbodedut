# MyHitbodedut pour téléphone à touches

Une vraie application, en un seul fichier : `MyHitbodedut.jar` (environ 107 Ko), en français, sans Internet, dans les couleurs de l'application et avec son personnage. Elle s'installe sur les téléphones à touches qui acceptent les applications Java (Java ME : MIDP 2.0, CLDC 1.1), dont le Nokia 3310 3G de 2017.

| Dans l'application Android | Dans MyHitbodedut.jar |
| --- | --- |
| Hitbodedout minutée : 5, 10, 15, 30 min, 1 h, 1 h 30, au choix | pareil ; pause à la touche centrale, sonnerie et vibration à la fin |
| Texte « Pour commencer », « Je n'arrive pas à parler », texte « Pour finir » | pareil |
| ’Hizouk par moment | les 46 textes publiés, en français |
| Accueil | le personnage du jour, « À faire » ou « Faite : 20 min », la série et la semaine en carrés |
| Journal | minutes du jour, série, 7 derniers jours en barres, total |
| Rappels | un rappel quotidien, si le téléphone l'autorise (PushRegistry) ; sinon, l'alarme du téléphone |

Les écrans sont dessinés (couleurs de `values/colors.xml`) ; le personnage change avec le moment, comme `Character.kt`. Les illustrations viennent de `drawable-nodpi/char_*.webp`, converties en PNG de 80 px par `app/genere-personnages.mjs` (un téléphone Java ne lit que le PNG) et gardées dans `app/res/p`.

Absents : l'hébreu (un téléphone à touches n'a pas toujours la police), le Tikoun HaKlali (en hébreu seulement), la musique, la bibliothèque.

## Téléphones

- **Nokia 3310 3G de 2017** (Smart Feature OS) : accepte les applications Java (Wikipédia, et un développeur l'a testé sur Stack Overflow).
- **Nokia 3310 de 2017 (2G)** : sources contradictoires.
- **Nokia 3310 3G de 2019** (Series 30+ sur Mocor) : pas d'applications Java.

## Installer

Envoyer `MyHitbodedut.jar` au téléphone par Bluetooth (ou le copier sur la carte microSD), l'ouvrir dans le gestionnaire de fichiers, accepter l'installation (« application non signée » : c'est normal).

## D'où viennent les textes

Rien n'est recopié : `app/genere-textes.mjs` relit `res/raw/chizuk.json` et `values/strings.xml` et applique les règles de l'application (`web/src/js`) : seul passe un ’hizouk relevé de la source et relu, dans sa traduction française. Les caractères sont ramenés à Latin-1 (« œ » devient « oe », « … » devient « ... »).

## Construire et tester

```sh
nokia/app/build.sh   # nokia/dist/MyHitbodedut.jar
nokia/app/test.sh    # règles (Java), textes (Node), parcours à l'écran 240×320
```

Il faut Node, un JDK 8 (le dernier dont `javac` vise encore Java 1.3, ce qu'exige CLDC) et Java 11 ou plus pour ProGuard, qui fait la pré-vérification sans laquelle le téléphone refuse le fichier. Les API MIDP/CLDC, ProGuard et MicroEmulator sont téléchargés dans `nokia/app/outils`.

Le parcours à l'écran joue l'application dans MicroEmulator, sur un appareil 240×320 (`app/test/appareil240`), touche par touche, et garde une capture de chaque étape dans `nokia/app/build/captures`.
