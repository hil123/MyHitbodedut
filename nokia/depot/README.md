# MyHitbodedut pour téléphone à touches

L'application MyHitbodedut pour les téléphones à touches qui acceptent les applications Java (Java ME : MIDP 2.0, CLDC 1.1), dont le **Nokia 3310 3G de 2017**. C'est une version à part de l'application Android : hitbodedout minutée, textes de ’hizouk en français, journal, un niggoun d'essai, un rappel quotidien. Sans Internet.

Ce dépôt ne contient que le fichier à installer. Il est reconstruit automatiquement depuis [hil123/MyHitbodedut](https://github.com/hil123/MyHitbodedut) (dossier `nokia/`) à chaque modification ; ne pas le modifier à la main.

## Installer

1. Télécharger [`MyHitbodedut.jar`](MyHitbodedut.jar).
2. L'envoyer au téléphone par Bluetooth, ou le copier sur la carte microSD.
3. L'ouvrir dans le gestionnaire de fichiers du téléphone et accepter l'installation. Le message « application non signée » est normal.

## Téléphones

- Nokia 3310 3G de 2017 (Smart Feature OS) : accepte les applications Java.
- Nokia 3310 de 2017 (2G) : à essayer, les sources se contredisent.
- Nokia 3310 3G de 2019 (Series 30+) : n'accepte pas les applications Java.

## La musique

Un seul niggoun d'essai est inclus, en MP3 et en AMR. L'écran « Musique » indique lequel le téléphone sait jouer, ou pourquoi aucun ne joue.

## Écrans

Le parcours testé à chaque construction, sur un téléphone simulé en 240×320, est dans [`captures/`](captures/).

`version.txt` donne la version du fichier et le commit dont il est construit.
