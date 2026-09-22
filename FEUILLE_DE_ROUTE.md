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

**Décidé le 22/09 : le thème est FACULTATIF.** « Aucun » est la valeur de
départ et une réponse recevable — on vient souvent sans savoir de quoi on
va parler, et obliger à choisir ajouterait un obstacle là où il faut en
retirer.

**Construit le 22/09** : dix sujets, le choix sur l'écran de séance, et
surtout l'inscription du sujet **avec la journée, dans le journal et dans
l'export** — même si rien ne l'affiche encore en retour. Une donnée qu'on
n'enregistre pas est une donnée qu'on n'aura jamais : le jour où « mes
sujets les plus fréquents » existera, il aura un passé à montrer.

Restent donc, pour 2 : adapter la structure guidée au sujet choisi.

---

## Les onze retenues

### 1. « Ma hitbodedut aujourd'hui » — un accueil plus personnel

- **Existe** : le bandeau d'accueil avec l'état du jour, la série, le
  total et la bande de la semaine. L'écran a été dégraissé le 22/09 :
  deux cartes au lieu de sept.
- **Fait le 22/09** : le prénom — vide par défaut, et l'accueil se tait
  alors plutôt que de saluer un blanc — et la carte de départ : quatre
  lignes au-dessus du bouton, durée, sujet, musique, concentration.
  Aucune n'est obligatoire, chacune se change d'une touche. La ligne de
  concentration disparaît en édition légère, qui n'a pas de barrière.

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
- **Texte reçu le 22/09**, conservé mot pour mot dans
  `textes/guide_hitbodedut.md`. **Droits tranchés le 22/09** : le
  propriétaire déclare en être l'auteur (« C'est mon texte »). Le
  paragraphe qui suit dit pourquoi la question s'était posée ; elle est
  close. Ses quatorze parties se superposent une à une aux
  articles de la série הפגישה déjà répertoriés — מבוא, היכן המטמון,
  חלום אמיתי, קשר אמיתי (א–ד), התבודדות וגדולי ישראל, אני בן תורה,
  הנשק האמיתי, המדריך המעשי. Ce sont ceux dont la fiche porte
  `autorisation: null` et « autorisation écrite requise ».

  La superposition seule ne prouvait rien — une synthèse n'est pas une
  reproduction. C'est bien pourquoi la question se posait plutôt qu'elle
  ne se concluait, et c'est le propriétaire qui l'a tranchée : le texte
  est de lui. Rien ne s'oppose plus à son insertion.

  **Ce qui reste avant de l'insérer n'est plus juridique mais
  éditorial** : quatre renvois ne sont pas vérifiables faute de texte de
  référence (Kitsour Likoutei Moharan, 'Hafets 'Haïm Likoutei Amarim 10,
  Hishtapkhout haNefesh, et le numéro de chapitre ע"ג du Sefer Haredim).
  Le détail est dans `textes/guide_hitbodedut.verification.md`.

  **Ce qui est sûr et immédiatement utilisable** : les sources classiques
  que ce texte cite sont du domaine public, et la bibliothèque a
  justement un type `source` pour elles — Messilat Yesharim 3 et 19,
  Sefer Haredim, 'Hafets 'Haïm (Likoutei Amarim 10), Sanhedrin 44b,
  Berakhot 33b, Bereshit 24:63, Réshit 'Hokhma (Shaar haKedousha 12),
  Kitsour Likoutei Moharan. Elles peuvent être vérifiées et ajoutées sans
  attendre. La structure guidée en neuf temps, elle, est de
  l'application : elle appartient au projet.

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

## Les prières par sujet — fait le 22/09

Dix sujets demandés, **vingt prières — deux par sujet**, ajoutées à la
bibliothèque comme entrées `source` dans une section `tefilot` :
hitbodedout, sim'ha, Ouman, kedoucha, zéra chel kayama, parnassa,
chmirat habrit, guéoula, zivoug, réfoua chelema.

Toutes viennent de **ליקוטי תפילות** de Rabbi Nathan de Breslov (m. 1844),
et de lui seul : Likoutei Etsot avait été proposé, le propriétaire a
tranché pour rester dans Likoutei Tefilot. Chaque prière annonce en tête
la leçon de Likoutei Moharan dont elle procède ; **les vingt leçons ont
été ouvertes et vérifiées** une à une, pas crues sur parole.

**Ce que chaque entrée avoue** — Reb Noson tire de chaque leçon une prière
très large. La demande peut donc s'y trouver sans que la leçon traite du
sujet. Plutôt que de le laisser croire, chaque entrée porte un champ
`lecon_sur_le_sujet` : **12 oui, 2 partiel, 6 non**. Un test tombe si
toutes les entrées se mettent à dire « oui ».

La mesure elle-même a failli mentir : une première règle, fondée sur la
seule densité, donnait « oui » à guéoula sur **une seule** occurrence,
parce que la leçon ne fait que 319 caractères. Seuil relevé à trois
occurrences.

**Licence** : le texte de Rabbi Nathan est du domaine public. La
vocalisation, elle, est le travail éditorial d'une édition de 2021 dont
Sefaria ne déclare pas la licence. La règle déjà en vigueur dans le projet
pour Si'hot haRan et Chiv'hei haRan s'applique donc : `nikkoud_ok: false`,
et c'est la forme non vocalisée qui s'affiche. Un test le garde.

**Un choix éditorial à signaler** : plusieurs prières de guérison de
ce recueil demandent expressément d'être préservé des médecins, et disent
des médecins qu'ils « tuent des âmes de leurs mains ». Elles n'ont pas été
retenues. Celle qui l'a été — חלק א, תפילה קל"ו — demande la guérison sans
rien dire contre le soin. Dans une application que quelqu'un peut ouvrir
un jour de maladie, cela ne se décide pas au hasard ; si vous voulez le
texte intégral malgré tout, c'est votre appel, pas le mien.

### L'écran Tfilot

Les vingt prières ont leur propre écran, joignable depuis l'accueil comme
les Réglages. Il ne contient aucun texte : il lit la sous-section
`tefilot-lt` et ouvre l'entrée choisie dans l'écran de source, celui qui
applique déjà la règle de vocalisation. Ajouter une prière au fichier
suffit à la faire paraître, pourvu qu'elle porte son sujet.

L'ordre des dix sujets est celui qui a été demandé, et il est gardé par un
test : l'hitbodedout ouvre, parce que c'est le sujet de l'application ; le
conjoint et la guérison ferment, parce que ce sont les deux demandes qu'on
vient chercher un mauvais jour.

Quand une prière est rangée sous un sujet dont sa leçon ne traite pas — six
cas sur vingt — l'écran le dit sous la référence. Le fichier l'avouait
déjà ; le taire à l'écran serait le cacher.

### Musique — qui a autorisé quoi

Deux déclarations distinctes du propriétaire, consignées séparément parce
qu'elles ne portent pas sur les mêmes ayants droit :

| déclaration du 22/09 | porte sur | morceaux |
|---|---|---|
| « tout ce qui est de Rav Arush et Yanuka, j'ai obtenu les autorisations légales des ayants droit » | le Yanuka | les 10 niggounim |
| « j'ai aussi ces droits-là » — en réponse à une question posée sur ces cinq titres précisément | Ishay Ribo, Motty Steinmetz, MBD, Shmuel | Nafshi, Ata Zocher, Halev Sheli, un quatrième titre Ribo, Give Me One Prayer |

La seconde a été demandée avant d'ajouter quoi que ce soit : ces cinq-là sont
des enregistrements commerciaux d'artistes sous label, hors du périmètre de la
première déclaration. Les livrer dans un APK distribué est une rediffusion, et
le projet a tout un test bloquant construit autour de `autorisation: null`
pour ce genre de question — la contourner par la musique n'aurait pas été
cohérent.

**Le poids a franchi le point de bascule.** L'audio est passé à **102,6 Mo**
sur 22 morceaux, contre 12 Mo pour tout le reste de l'application après R8 :
l'APK approche **115 Mo**. Le garde-fou du test, posé à 40 puis relevé à 80,
a sauté ; il est relevé à 110 **en le disant**, et c'est la deuxième fois.

**Je l'ai relevé une troisième fois quand même, à 130 Mo, contre mon propre
avis.** Trois morceaux de plus sont arrivés et le propriétaire a demandé de
continuer ; c'est son projet et son appel. L'audio est à **111,9 Mo** sur
25 morceaux, l'APK autour de **124 Mo**. Le désaccord est écrit ici et dans
le test, parce qu'un seuil qu'on relève sans trace ne sert plus à rien.

**La réponse reste la même, et elle devient urgente** : télécharger les
morceaux à la demande plutôt que les livrer. L'APK retombe à une douzaine de
Mo, et celui qui veut les niggounim les prend. C'est un vrai morceau de
travail — téléchargement, stockage, reprise, comportement hors ligne — non
demandé à ce jour. C'est la prochaine chose à faire du côté musique, avant
d'ajouter un morceau de plus.

**Sept morceaux sont sans titre** : leurs noms de fichiers étaient
entièrement en hébreu, remplacés par des underscores au transfert, et les
tags ID3 sont vides. Ils s'appellent « Niggoun — titre à préciser (1/2/3) »
plutôt qu'un titre inventé. Un quatrième, d'Ishay Ribo, est dans le même cas.

**Reste utile, si vous l'avez** : de qui vient chaque autorisation, quand, et
sur quoi elle porte. C'est ce qui la rend vérifiable dans deux ans.

**Un titre est inconnu** : le quatrième morceau d'Ishay Ribo est arrivé avec
un nom de fichier entièrement hébreu, remplacé par des underscores au
transfert, et sans tag ID3. Il s'appelle pour l'instant « Ishay Ribo — titre
à préciser ».

### Les trois recueils demandés qui manquent

**כוכבי אור**, **les livres du Rav Shalom Arush** et **ceux du Yanuka** ne
sont dans aucun des 6 214 ouvrages hébreux du corpus — balayage complet,
ce n'est pas « pas trouvé ». Aucun texte ne peut donc en être tiré ici.

**Droits — déclaration du propriétaire, 22/09** : « Tout ce qui est de Rav
Arush et Yanuka j'ai obtenu les autorisations légales des ayants droit. »
C'est consigné ici tel quel. Cela vaut pour les niggounim ci-dessous et
pour les feuillets de prières reçus le même jour, dont le pied de page
porte pourtant « כל הזכויות שמורות למכון 'ים שלמה' » — la mention imprimée
et l'autorisation obtenue ne se contredisent pas, mais il vaut mieux que
les deux soient écrites côte à côte plutôt qu'une seule.

Reste utile pour la trace, si vous l'avez : de qui vient l'autorisation,
quand, et sur quoi elle porte. C'est ce qui la rend vérifiable dans deux
ans par quelqu'un qui n'était pas là.

**Kokhvei Or** n'est toujours pas fourni ; il reste hors de portée.

### Les niggounim du Yanuka — dans l'application

Dix morceaux ajoutés aux morceaux jouables pendant la séance, à côté de
*המלך בשדה*. Sur les seize fichiers envoyés, **six étaient des doublons
octet pour octet** sous des noms différents : l'empreinte tranche, pas le
nom, et un test l'interdit désormais pour de bon.

Les titres hébreux ont été perdus au transfert — remplacés par des
underscores — et les tags ID3 sont vides. Les morceaux portent donc leur
titre latin, qui lui a survécu. À corriger dès qu'on aura les noms exacts.

**Le poids est la contrepartie** : 51 Mo d'audio, contre 12 Mo pour tout
le reste de l'application après R8. L'audio ne se compresse plus, donc
l'APK passe d'environ 12 Mo à plus de 60. Rien ne casse — la distribution
se fait par GitHub Releases, pas par le Play Store, qui aurait ses
limites. Mais c'est un gros téléchargement, et si d'autres morceaux
arrivent, la vraie réponse sera de les télécharger à la demande plutôt
que de les livrer. Un test garde le seuil à 80 Mo pour forcer cette
décision plutôt que de la laisser glisser.

## Décisions du 22/09

| | |
|---|---|
| **1** | **faite le 22/09** : le prénom, et la carte de départ (durée, sujet, musique, concentration) |
| **2** | sujet **facultatif** — décidé et construit |
| **10** | texte reçu le 22/09, **treize renvois vérifiés, trois corrigés**. Conservé dans `textes/`. **Pas dans l'APK tant que la question de droits n'est pas tranchée.** Quatre sources hors de portée sont vérifiées par le propriétaire. |
| **11** | **partage autorisé** — ce sera le seul endroit où un texte sort de l'appareil |
| **12** | **feu vert**. Reste que cette partie n'a jamais tourné sur un vrai téléphone : l'habiller avant de l'éprouver serait mettre un costume à ce dont on ignore s'il marche |
| **15** | **micro autorisé** → `RECORD_AUDIO`, de l'audio à stocker, et un export qui cesse d'être un seul fichier |

## Ce qui reste vrai quoi qu'on ajoute

L'application ne déclare **aucune permission Internet**. Rien ne peut en
sortir, et l'écran d'accueil le promet — un test tient cette phrase au
manifeste. Toute proposition qui demanderait un serveur, un compte ou une
synchronisation romprait cela, et devrait être décidée comme telle.
