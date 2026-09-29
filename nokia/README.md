# MyHitbodedut pour téléphone à touches

Pour le Nokia 3310 de 2017 (2G ou 3G) et les téléphones du même genre : pas d'application à installer, pas d'Internet. Le pack est fait de fichiers que le téléphone sait déjà lire.

| Dans l'application | Dans le pack | Sur le téléphone |
| --- | --- | --- |
| ’Hizouk, moment par moment | images 240×320, `FR_2` à `FR_7` | Galerie |
| Tikoun HaKlali | images 240×320, `FR_8` | Galerie |
| Hitbodedout minutée | mode d'emploi, `FR_1` | Minuteur (compte à rebours) |
| Rappels | mode d'emploi, `FR_1` | Alarme |

Le journal n'a pas d'équivalent sans application. La musique n'est pas dans le pack : 128 Mo, trop lourd pour le Bluetooth.

Le texte est dessiné dans les images : le téléphone n'a pas besoin de savoir afficher l'hébreu vocalisé. Les noms de fichier sont en ASCII et se trient dans l'ordre de lecture, parce qu'un fichier reçu par Bluetooth perd son dossier mais garde son nom.

## D'où vient le contenu

Rien n'est recopié : `build.mjs` relit les ressources Android (`res/raw/chizuk.json`, `tikkun_haklali.json`, `strings.xml`) et applique les règles de l'application, reprises de `web/src/js` : seul passe un ’hizouk relevé de la source et relu, l'hébreu perd son nikkoud quand la licence de l'édition l'exige, et l'hébreu n'a pas de traduction.

`guide.json` contient le mode d'emploi en trois langues. Il n'est pas encore relu (`_meta.relu`).

## Construire et tester

```sh
node nokia/build.mjs                  # les images, dans nokia/dist
node --test nokia/test/*.test.mjs     # le plan, puis le pack s'il est construit
```

`build.mjs` a besoin de Playwright et de Chromium (`PLAYWRIGHT_MODULE`, `CHROMIUM_PATH` si besoin), et de la commande `zip`. Il produit `MyHitbodedut-Nokia.zip` (les trois langues, le fichier à partager) et un zip par langue (2 à 3 Mo).

## Envoyer par Bluetooth

Depuis un téléphone Android : décompresser le zip, ouvrir le dossier de la langue (FR, EN ou HE), tout sélectionner, Partager › Bluetooth. Le Nokia ne sait pas ouvrir un zip : il reçoit les images une à une.
