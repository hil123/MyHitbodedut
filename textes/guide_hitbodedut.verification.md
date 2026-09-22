# Vérification des renvois du guide

Contrôle du 22/09/2026 sur `guide_hitbodedut.md`, avant toute insertion
dans l'application. Chaque renvoi a été confronté au **texte primaire**,
pas à une citation de seconde main : corpus public `sefaria-export`,
comparaison mécanique après retrait des points-voyelles et de la
ponctuation des deux côtés.

Le texte lui-même n'a pas été modifié : il reste tel qu'il a été transmis.

## Exacts — vérifiés mot pour mot (6)

| Renvoi | Vérifié |
|---|---|
| **סנהדרין מ"ד ע"ב** — « לעולם יקדים אדם תפלה לצרה » | Sanhedrin 44b, au nom de רבי אלעזר, avec l'exemple d'Abraham entre Beth-El et Aï |
| **ברכות ל"ג ע"ב** — « הכל בידי שמים חוץ מיראת שמים » | Berakhot 33b, au nom de רבי חנינא |
| **בראשית כ"ד** — « ויצא יצחק לשוח בשדה » | Bereshit **24:63** |
| **תהלים קמ"ה** — la « קריאה באמת » | Tehilim **145:18** : « קרוב ה' לכל קֹרְאָיו, לכל אשר יקראהו באמת » |
| **« ובו תדבק »** | Devarim **10:20** (et 13:5) |
| **מסילת ישרים פרק ג'** — « שיהיה מדקדק ושוקל דרכיו דבר יום ביומו » | Messilat Yesharim **ch. 3**, mot pour mot, avec l'image des grands négociants |

## À corriger (2)

### « כדבר איש אל רעהו »

Le verset — Shemot **33:11** — dit **« כַּאֲשֶׁר יְדַבֵּר אִישׁ אֶל־רֵעֵהוּ »**.
La forme du guide n'apparaît ni dans le verset, ni dans Likoutei Moharan,
ni dans Likoutei Etsot, Si'hot haRan ou 'Hayé Moharan. C'est une
paraphrase, présentée entre guillemets comme une citation.

### « עיקר הכלי זיין של איש הישראלי הוא התפילה »

Cette phrase **n'est pas dans Likoutei Moharan**. Le passage parallèle,
Likoutei Moharan **I, 2**, dit :

> עִקַּר כְּלֵי זֵינוֹ שֶׁל **מָשִׁיחַ** הוּא הַתְּפִלָּה

Le sujet diffère : *le Machia'h* dans l'original, *l'homme d'Israël* dans
le guide. Vérifié absent aussi de Likoutei Etsot, Si'hot haRan et 'Hayé
Moharan.

Le guide, lui, cite **קיצור ליקוטי מוהר"ן** — un autre ouvrage, qui n'est
pas dans le corpus Sefaria accessible ici. La citation est donc
**plausible pour l'ouvrage qu'elle nomme** et je ne peux ni la confirmer
ni l'infirmer ; elle serait fausse si on l'attribuait à Likoutei Moharan.

## Imprécis (1)

### מסילת ישרים פרק י"ט

Le guide écrit que le Ram'hal y « décrit un état où l'homme supplie
devant le Créateur et Lui demande, depuis un lien personnel ».

Le chapitre 19 traite bien de prière et de dvékout — il cite d'ailleurs
« ובו תדבק » — mais ce qu'il demande de prier, c'est **la délivrance,
Jérusalem et sa génération** :

> וְיִתְפַּלֵּל תָּמִיד עַל גְּאֻלַּת בְּנֵי יִשְׂרָאֵל

> שֶׁיִּתְפַּלֵּל עַל דּוֹרוֹ

C'est une prière **pour autrui et pour la Shekhina**, pas une
conversation personnelle. Le renvoi n'est pas faux, il est déplacé : ce
que le guide y cherche se trouve plutôt au chapitre 3, qu'il cite
d'ailleurs correctement ailleurs.

## Hors de portée d'ici (4 + 1) — **pris en charge par le propriétaire**

Absents du corpus `sefaria-export` : **ספר החרדים**, **ראשית חכמה** (שער
הקדושה פרק י"ב), **חפץ חיים — ליקוטי אמרים פרק י'**, **קיצור ליקוטי
מוהר"ן**. Ni confirmés ni infirmés ici.

**Le propriétaire de l'application les vérifie lui-même** (22/09). Tant
que ce retour n'est pas là, ces quatre renvois restent au même statut que
les autres non vérifiés : ils n'ont pas été contrôlés, et rien dans ce
dépôt ne doit laisser croire le contraire.

Le plus utile des quatre est le Kitsour : c'est lui qui décide si la
phrase sur l'arme est correctement attribuée — voir plus haut.

« ותן בלבנו בינה » et « ויחד לבבנו ליראה ולאהבה את שמך » viennent de la
bénédiction *Ahava Rabba* du siddour du matin — usuel, non vérifié ici
faute de texte de référence dans ce corpus.

La partie ד énumère par ailleurs רבינו בחיי, האר"י, החיד"א, בעל התניא,
רבי אלימלך מליז'נסק, רבי אהרן מקרלין — sans citation précise. Rien à
vérifier : ce sont des noms, pas des renvois.

## Corrections portées le 22/09

Les trois ont été appliquées dans `guide_hitbodedut.md`, **chacune
signalée sur place par un encadré** plutôt que faite en silence : un texte
transmis ne se réécrit pas sans le dire. La version d'origine reste dans
l'historique git.

1. « כדבר איש אל רעהו » → **כַּאֲשֶׁר יְדַבֵּר אִישׁ אֶל־רֵעֵהוּ** (שמות ל"ג, י"א).
2. מסילת ישרים **י"ט** → **פרק ג'**, avec la phrase exacte, et ce que le
   chapitre 19 demande réellement de prier.
3. L'arme : attribution maintenue à **קיצור ליקוטי מוהר"ן** seul, avec
   l'original de Likoutei Moharan **I, 2** cité à côté — où le sujet est
   le Machia'h.

## Ce que cela change pour l'insertion

Rien n'empêche d'insérer le texte une fois la **question de droits**
tranchée (voir `FEUILLE_DE_ROUTE.md`, proposition 10). Mais deux
corrections devraient l'accompagner, et la troisième mérite d'être
signalée :

1. ~~rétablir « כאשר ידבר איש אל רעהו »~~ — **fait** ;
2. ~~attribuer la phrase sur l'arme au seul Kitsour~~ — **fait**, avec
   l'original cité à côté ;
3. ~~déplacer le renvoi de מסילת ישרים י"ט~~ — **fait**.

Reste donc la seule **question de droits**, et le retour du propriétaire
sur les quatre sources hors de portée.

Les six sources exactes, elles, sont du **domaine public** et peuvent
entrer dans la bibliothèque comme entrées de type `source`, sans attendre
la question de droits — qui ne porte que sur la prose du guide.
