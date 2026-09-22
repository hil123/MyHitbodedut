# Feuille de route

Onze propositions retenues le 22/09/2026, sur une liste de quinze. Ce
fichier existe pour qu'elles ne vivent pas dans une conversation.

Pour chacune : ce qu'elle demande, **ce qui existe déjà** dans
l'application, et ce qui reste à faire. La colonne du milieu est la plus
utile — plusieurs de ces idées sont à moitié construites, et il serait
absurde de les refaire.

Non retenues : **4** (sessions express), **8** (programme 30 jours),
**13** (question profonde pendant la séance), **14** (assistant IA). Ce
sont des décisions, pas des oublis.

---

## La clé de voûte : un thème de séance

**Trois des propositions retenues — 2, 5 et 6 — reposent sur la même
chose qui n'existe pas encore : un thème attaché à une séance**
(parnassa, zivoug, émouna, relations, difficultés…). Sans lui, ni le
choix d'objectif, ni le journal enrichi, ni la page de parcours ne
peuvent exister.

C'est donc par là qu'il faut commencer, et c'est une seule décision :
quelle liste de thèmes, et le thème est-il obligatoire ou facultatif au
démarrage d'une séance. Le reste en découle.

---

## Les onze retenues

### 1. « Ma hitbodedut aujourd'hui » — un accueil plus personnel

- **Existe** : le bandeau d'accueil avec l'état du jour, la série, le
  total et la bande de la semaine. L'écran a été dégraissé le 22/09 :
  deux cartes au lieu de sept.
- **Reste** : le prénom, et une carte de départ qui rassemble durée,
  thème, musique et mode concentration avant de commencer.
- **Dépend de** : le thème de séance.

### 2. Hitbodedut guidée par objectif

- **Existe** : la hitbodedout guidée en cinq temps (Likoutei Moharan II,
  25 pour quatre d'entre eux, Rambam Hilkhot Tefila 1,2 pour l'action de
  grâce). Éteinte par défaut, facultative, un temps à la fois.
- **Reste** : le choix du thème au début, et une structure qui s'adapte
  au thème choisi.
- **Dépend de** : le thème de séance.

### 3. Mode « Je ne sais pas quoi dire »

- **Existe** : le bouton pendant la séance, adossé au moment `blocage`
  des textes de 'hizouk — c'est exactement ce que Likoutei Moharan II, 25
  répond à celui dont les mots ne viennent pas.
- **Reste** : l'enchaînement de questions, une à la fois, au lieu d'un
  texte unique.
- **Remarque** : c'est la proposition 15 sous un autre nom. Les traiter
  ensemble.

### 5. Le journal en carnet spirituel

- **Existe** : `JournalActivity`, la note du jour, la durée réelle
  enregistrée, l'export JSON. Le journal est dans un fichier séparé,
  exclu de la sauvegarde automatique d'Android.
- **Reste** : humeur avant / après, le thème, les deux questions de fin
  (une gratitude, une chose à travailler).
- **Dépend de** : le thème de séance.
- **Attention** : ce sont des données intimes. Tout ce qui s'y ajoute
  doit rester sur l'appareil, comme le reste.

### 6. « Mon parcours »

- **Existe** : série, total, bande de la semaine, progression du Tikoun.
- **Reste** : une page dédiée, et surtout les **thèmes les plus
  fréquents** — qui n'existent pas encore.
- **Dépend de** : le thème de séance.

### 7. Selon le moment de la journée

- **Existe** : les rappels à plusieurs heures, qui se taisent un jour de
  repos, la veille après midi, et dès que la séance est faite.
- **Reste** : adapter le mot d'accueil et le texte proposé au matin, à la
  journée, au soir, à la nuit.
- **Remarque** : sans ville ni coucher du soleil (idée écartée le
  21/09), les bornes restent des heures fixes.

### 9. Ambiances audio

- **Existe** : `MusicActivity`, `MusicPlayer`, le choix d'un morceau, le
  volume, **et le démarrage automatique au début de la séance** — c'est
  déjà fait, `SessionService.ensureMusic()`.
- **Reste** : des catégories (pluie, mer, forêt, feu, nuit, silence) et
  les fichiers qui vont avec.
- **Attention** : l'APK fait déjà 12 Mo, dont 9,7 pour un seul morceau.
  Chaque ambiance ajoutée se paie en taille. À arbitrer.

### 10. « Source du jour »

- **Existe** : la carte de 'hizouk sur l'accueil, la bibliothèque, ses
  métadonnées.
- **Reste** : une pensée du jour tirée de la bibliothèque, distincte du
  'hizouk, avec deux sorties : commencer, ou lire davantage.
- **Attention** : les 18 articles de la série הפגישה n'ont **aucun texte
  intégral** et aucune autorisation. Un test bloquant l'empêche. La
  pensée du jour devra venir des sources libres, pas de ceux-là.

### 11. Chizuk à la demande

- **Existe** : les 37 textes, le sac mélangé sans répétition, les six
  moments, la carte qui ne défile jamais.
- **Reste** : une entrée « j'ai besoin de 'hizouk » hors séance, les
  favoris, le partage.
- **Remarque** : le partage fait sortir un texte de l'appareil. C'est le
  seul endroit de l'application où cela arriverait — à décider
  explicitement.

### 12. Mode « Je suis avec Hachem »

- **Existe** : toute la mécanique — `GateService`, `BlockerView`,
  `AppPickerActivity`, les autorisations, l'écran de barrage.
- **Reste** : la présentation. Le nommer ainsi plutôt que « blocage »,
  et clore la séance par un mot plutôt que par un décompte à zéro.
- **Attention** : cette partie n'a **jamais été essayée sur un vrai
  téléphone**. Avant de l'habiller, il faut l'éprouver : Android 13 à 16,
  redémarrage, économie de batterie, autorisation retirée en cours de
  séance.

### 15. « Commence simplement » *(en option)*

- **Reste** : un écran unique — parler, écrire, prier, recevoir une
  question — qui démarre immédiatement.
- **Dépend de** : la proposition 3, dont c'est la même idée.
- **Attention** : « parler » suppose le micro, donc la permission
  `RECORD_AUDIO`, de l'audio à stocker, et un export qui cesse d'être un
  seul fichier. C'est la question des notes vocales, toujours ouverte.

---

## Ce qui reste vrai quoi qu'on ajoute

L'application ne déclare **aucune permission Internet**. Rien ne peut en
sortir, et l'écran d'accueil le promet — un test tient cette phrase au
manifeste. Toute proposition qui demanderait un serveur, un compte ou une
synchronisation romprait cela, et devrait être décidée comme telle.
