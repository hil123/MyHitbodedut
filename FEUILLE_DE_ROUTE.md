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

Toutes viennent de **ליקוטי תפילות** de Rabbi Natan de Breslev (m. 1844),
et de lui seul : Likoutei Etsot avait été proposé, le propriétaire a
tranché pour rester dans Likoutei Tefilot. Chaque prière annonce en tête
la leçon de Likoutei Moharan dont elle procède ; **les vingt leçons ont
été ouvertes et vérifiées** une à une, pas crues sur parole.

**Ce que chaque entrée avoue** — Rabbi Natan tire de chaque leçon une prière
très large. La demande peut donc s'y trouver sans que la leçon traite du
sujet. Plutôt que de le laisser croire, chaque entrée porte un champ
`lecon_sur_le_sujet` : **12 oui, 2 partiel, 6 non**. Un test tombe si
toutes les entrées se mettent à dire « oui ».

La mesure elle-même a failli mentir : une première règle, fondée sur la
seule densité, donnait « oui » à guéoula sur **une seule** occurrence,
parce que la leçon ne fait que 319 caractères. Seuil relevé à trois
occurrences.

**Licence** : le texte de Rabbi Natan est du domaine public. La
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
| les envois suivants, sous la même déclaration | Shneor Lerer (Chabad Melodies), *Tzama with orchestra* | rangés dans `musique/`, hors APK |

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

### Correction : le remède que je recommandais ne marche pas

J'ai écrit deux fois qu'il fallait « télécharger les morceaux à la demande ».
**C'est faux, et pour deux raisons que je n'avais pas vérifiées avant de le
conseiller.**

1. **L'application ne déclare aucune permission INTERNET**, et le README en
   fait une propriété revendiquée : « pas d'analytique, pas de réseau ». Pour
   une application de hitbodedout, ce n'est pas un détail de mise en œuvre,
   c'est une promesse. Télécharger obligerait à la rompre.
2. **Il n'y a nulle part où héberger.** Le dépôt est privé — le README dit
   qu'il faut déjà une connexion GitHub pour récupérer l'APK. Il faudrait donc
   un hébergement public, et y poser les enregistrements du Yanuka et d'Ishay
   Ribo les rendrait téléchargeables par n'importe qui. C'est une diffusion
   plus large que de remettre un APK, et donc une question de droits **autre**
   que celle qui a été réglée.

**Le vrai remède ne demande rien de neuf.** `MusicActivity` sait déjà ouvrir un
fichier du téléphone (`OpenDocument` → `addUserTrack`) : la fonction existe et
marche. Il suffit de :

- livrer une **petite sélection** — trois ou quatre morceaux — dans l'APK ;
- mettre les autres sur le téléphone, et les ajouter depuis l'écran Musique.

L'APK retombe à une vingtaine de Mo. Aucune permission nouvelle, aucun
hébergement, aucune question de droits supplémentaire, et rien à construire.

**Espagnol : écrit le 22/09, retiré le 22/09, sur décision du
propriétaire.** L'interface (327 textes), les titres de bibliothèque et les
37 textes de 'hizouk avaient été traduits ; tout est retiré. Il ne reste
que trois langues : français, anglais, hébreu. La traduction est dans
l'historique (`values-es/`, commits `4330398` et `0275d17`) si elle doit
resservir ; rien n'a été perdu, seulement décroché.

**Ce que ce détour a laissé et qu'on garde**, parce que cela ne dépendait
pas de l'espagnol :

- `TraductionTest` vérifie les trois langues d'un coup — même jeu de clés,
  aucune chaîne vide, marqueurs de format concordants, aucun texte écrit en
  dur dans les cinquante layouts ni dans les cinquante fichiers Kotlin.
  C'était vérifié écran par écran, jamais sur l'ensemble.
- Un test garde la trace du piège trouvé : `LocalizedText.forLanguage` et
  `ChizukCard.translationOf` finissent tous deux par un `else`. Poser un
  `values-xx/` sans nommer la langue dans ces deux règles donnerait une
  bibliothèque en hébreu et des textes en français, **sans rien qui le
  signale**. Le test échoue désormais dès qu'un dossier de langue apparaît
  sans sa règle — vérifié en posant un `values-pt/`.

**Ce que la vérification avait établi et qui reste vrai** : l'interface
française, anglaise et hébraïque est complète — 320 clés identiques,
aucun texte en dur, marqueurs concordants. Les 24 résumés d'articles,
eux, ont été traduits le 22/09 — voir plus bas.

**Deux icônes identiques, une seule ouverte (22/09).** Après le correctif
d'identifiant, le propriétaire a continué de tester sans voir le bouton de
langue ni le titre du Tikoun traduit — deux captures d'écran l'ont
confirmé : thème clair, aucune des corrections du jour. L'identifiant de
paquet était pourtant bien distinct ; ce qui manquait, c'est que les deux
applications s'affichaient sous le **même nom**, « MyHitbodedut », sur
l'écran d'accueil. Rien ne permettait de savoir laquelle des deux icônes
rouvrir, et l'ancienne Relecture — jamais mise à jour depuis le début du
problème de versionCode — restait la plus probable par habitude.

Corrigé par un marqueur de manifeste (`appLabel`), pas par `resValue` :
un `resValue` de même nom qu'une chaîne déjà déclarée dans
`values/strings.xml` (`app_name` l'est) casse la fusion des ressources
pour *toute* variante qui partage ce type de compilation — Relecture
aussi bien que l'essai, donc `android.yml` aussi bien que
`apk-essai.yml`. Le marqueur, lui, ne touche que le texte du manifeste.
L'édition d'essai s'appelle désormais **« MyHitbodedut (essai) »** ;
toutes les autres gardent leur nom. `PackagingTest` en garde la trace,
vérifiée par sabotage.

**« תיקון הכללי » restait figé en hébreu (22/09), signalé sur le
téléphone.** Trois layouts pointaient sur `tikkun_title_he`
(`translatable="false"`) plutôt que sur `tikkun_title` — qui existait
pourtant déjà, correctement traduit dans les trois langues, sans que rien
ne le référence nulle part. Corrigé aux trois endroits
(`activity_main.xml`, `activity_tikkun.xml`, `activity_tikkun_flow.xml`),
avec le sens de lecture forcé au RTL retiré de deux d'entre eux : il n'a
plus lieu d'être puisque le titre suit désormais la langue, et le sens
ambiant de l'écran suffit à l'aligner correctement en hébreu. La chaîne
`tikkun_title_he` a disparu des ressources. `RegleLangueTest` en garde la
trace, vérifiée par sabotage.

**Le bouton de langue n'a jamais atteint le téléphone (22/09) — de ma
faute, pas d'un défaut de l'écran.** Après l'avoir remonté en tête des
réglages, le propriétaire a dit ne toujours rien voir. Vérification faite
sur le binaire compilé lui-même (décodage du XML de mise en page avec
`androguard`, pas seulement une lecture du source) : le bouton **était**
bien la première ligne de l'écran, dans chaque APK envoyé. Le défaut était
ailleurs.

**L'édition d'essai n'a jamais pu s'installer.** Elle portait le même
identifiant que Relecture (`com.myhitbodedut.debug`) — et Relecture est
reconstruite à **chaque envoi sur `main`**, puisque le flux principal se
déclenche sur tout push. Son propre compteur de version n'avançait, lui,
qu'aux déclenchements manuels de l'édition d'essai : à la sixième
édition d'essai envoyée, son `versionCode` valait 7, contre ~80 déjà en
place pour Relecture, reconstruite entre-temps par mes propres poussées.
**Android refuse d'installer un `versionCode` plus bas que celui déjà en
place**, et le refus ne dit rien qui pointe vers la cause — un bouton
« introuvable » était en réalité une version qui n'avait jamais changé sur
le téléphone, ni au premier envoi ni à aucun des suivants.

Corrigé par un identifiant propre à l'édition d'essai
(`com.myhitbodedut.debug.essai`, drapeau `ESSAI_LEGER`) plutôt que par un
simple décalage de compteur : les deux flux ne se comparent plus jamais.
`PackagingTest` garde la trace des deux moitiés du correctif (le drapeau
dans `build.gradle.kts`, son activation dans le flux), vérifié par
sabotage.

**Une seule règle d'affichage, et les fiches d'articles traduites (22/09).**

Après un essai sur le téléphone, trois retours : le bouton de langue restait
introuvable (ligne 201 sur 338, sous la ligne de flottaison — remonté en
tête des réglages, dans sa propre carte), une note s'affichait en français
sur un écran en anglais, et l'écran de relecture montrait le français et
l'anglais côte à côte quelle que soit la langue choisie.

`RegleLangue` tient la règle en un seul endroit : en hébreu rien que
l'hébreu, en anglais l'hébreu et l'anglais, en français l'hébreu et le
français — jamais les trois, et **plus aucun repli d'une langue sur une
autre**. Un texte absent dans la langue en cours n'est plus affiché dans
une autre langue à sa place ; il n'est pas affiché du tout, ce qui se voit
et se répare, plutôt que de passer inaperçu.

**Note et Édition, retirées de sous chaque texte** — demandé ensuite,
séparément : ce sont des détails d'atelier, pas de lecture. Le registre des
26 notes reste dans `bibliotheque.json` (un test en tient le compte à 26) ;
l'attribution passe désormais par le seul lien « Ouvrir sur Sefaria ». Trois
éditions sur huit portent une licence « unknown », mais cela ne concerne
que leur vocalisation, que l'application ne reprend déjà pas — le texte
affiché est le texte consonantique du domaine public, rien n'est dû.

**Les 24 fiches d'articles, traduites.** Elles étaient le seul manque
identifié par la vérification du 22/09, et devenaient invisibles en anglais
et en hébreu depuis que `RegleLangue` interdit le repli. Résumé, droits et
erreurs relevées sont maintenant nommés par langue (`resume_fr/en/he`,
`droits_fr/en/he`, `erreurs_relevees_fr/en/he`) et passent par
`LibraryRules.summary`, `.rights`, `.errors` :

- **24 résumés**, traduits un par un.
- **Droits : trois textes seulement couvrent les 24 fiches** (les dix-huit
  articles הפגישה, les cinq lettres d'Achèr BaNa'hal, et une fiche à part) —
  traduits une fois chacun et déclinés, plutôt que vingt-quatre fois.
- **Six erreurs relevées**, sur le seul article qui en porte
  (`art-bc-salama-2024`) : les citations hébraïques restent telles quelles,
  seul le commentaire autour change de langue.

Le titre d'un article reste en hébreu dans les trois modes — comme le titre
d'une source : c'est le vrai nom de l'article, pas un texte à traduire.

`LibraryRulesTest` vérifie les trois langues pour chaque article, et que le
nombre d'erreurs relevées ne varie pas d'une langue à l'autre. Vérifié par
sabotage : retirer un `resume_en` fait tomber le test.

**Choix de la langue, et relecture texte par texte (22/09).**

Un bouton **עברית · English · Français** dans les réglages : l'application
livrait trois traductions mais suivait la langue du téléphone, sans recours
— un appareil réglé en anglais ne pouvait pas afficher l'hébreu.
`AppCompatDelegate.setApplicationLocales` applique le choix ;
`locales_config.xml` le déclare à Android 13, et le service
`AppLocalesMetadataHolderService` le fait garder en dessous. **Sans ce
service, le choix serait oublié à la fermeture, en silence** : un test le
vérifie, et échoue si `autoStoreLocales` passe à `false`.

**La relecture des 314 textes a trouvé six écarts réels, et l'hébreu est la
source.** Le commit `480e0fe` le dit : les dix conseils ont été fournis en
hébreu, le français et l'anglais sont les traductions. Ce sont donc les
traductions qui avaient dérivé :

1. **Le Nom.** La source dit « השם » dans sept textes ; les traductions
   écrivaient « Dieu » et « God », alors que les textes de 'hizouk disaient
   déjà « Hachem » — le même Nom rendu de deux façons dans la même
   application. Corrigé partout ; « בורא עולם » reste « Créateur », qui est
   un descriptif et non un nom.
2. **`tip_2` : un chiffre inventé.** La source dit « הקדישו חמש-עשרה דקות »
   — quinze minutes. Les traductions disaient « cinq à quinze minutes ».
3. **`tip_3` : « דיבור פנימי » perdu.** La parole intérieure avait disparu
   du titre et du corps, dans les deux langues.
4. **`tip_6` : « חשבון נפש » traduit** en « examen de conscience ».
5. **`tip_7` : « חוסר ביטחון »** rendu par « doute » au lieu de manque
   d'assurance.
6. **`gate_armed_detail` n'était pas un pluriel** : « 1 applications sont
   bloquées », dans les trois langues. Devenu un `plurals` — l'hébreu y
   porte aussi le duel.

**Les dix sujets de prière ont retrouvé leurs termes hébreux** : « Parnassa »
figurait déjà sur l'écran des thèmes tandis que l'écran Tfilot disait
« Gagner sa vie ». Sim'ha, Kedoucha, Guéoula, Zivoug, Refoua chelema,
Chmirat habrit, Zéra chel kayama.

**Ce qui a été vérifié et qui est juste** : les 19 chaînes anglaises
identiques au français le sont légitimement ; les cinq écarts de longueur
sont de la concision anglaise ; le registre hébreu alterne le singulier
pour l'interface et le pluriel pour l'enseignement, ce qui est un système
et non une négligence.

**Renommer depuis l'application (22/09).** La fonction existait déjà et le
propriétaire l'a demandée : elle n'était atteignable que par appui long,
sans bouton ni autre indice que la ligne d'aide. Un bouton crayon sur chaque
ligne, l'appui long conservé, le champ qui ne recopie plus le titre livré
(l'effacer d'abord était une corvée, et enregistrer sans rien changer le
figeait), et un retour au titre d'origine. Ce que cela ne fait pas : ces
noms vivent dans les préférences du téléphone, pas dans le dépôt. Un titre
ne devient commun à tous que s'il m'est transmis.

**Échange du 22/09, demandé par le propriétaire : les sans-titre entrent,
les morceaux nommés sortent.** On ne nomme que ce qu'on entend, et
trente-cinq morceaux attendaient un titre. L'APK en porte maintenant
**trente-trois**, à 123,1 Mo, sous le même plafond ; les morceaux déjà
nommés sont dans `musique/` (34, 199,7 Mo) et leurs titres sont conservés
dans un bloc de réserve de `values/strings.xml`, d'où ils reviendront tels
quels. `tools/echange_musique.py` fait l'aller comme le retour.
`niggoun_09` et `niggoun_16`, les deux plus lourds, n'ont pas pu entrer :
l'ensemble des sans-titre pèse 147 Mo. Priorité au nombre de morceaux
nommables, pas au poids.

**Le seuil avait été franchi avant cet échange.** L'APK était à 129,5 Mo sur 130 pour 31 morceaux ;
depuis, tout nouvel envoi va dans `musique/`, versionné mais hors de
l'application — 36 morceaux, 193,3 Mo à ce jour. Le plafond n'a pas été
relevé une quatrième fois et ne le sera pas sans décision explicite. Le
choix de la sélection reste entier, et il ne presse plus : rien ne se perd
et l'APK ne grossit plus.

**Beaucoup de morceaux sont sans titre** : leurs noms de fichiers étaient
entièrement en hébreu, remplacés par des underscores au transfert, et les
tags ID3 sont vides. Ils portent « Niggoun — titre à préciser » dans
l'application et `niggoun_NN` dans `musique/`, plutôt qu'un titre inventé.
Le compte exact est tenu par `tools/inventaire_musique.py`, qui régénère
`musique/README.md` depuis les fichiers — un chiffre écrit à la main dérive,
et celui-là avait déjà dérivé (« six » sous un tableau qui en listait seize).

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

**Les cheveux du personnage, refaits en peot (22/09).** Signalé sur une
capture d'écran d'accueil (`char_walking`) puis répété sur une deuxième
(`char_open_arms`) : la longue mèche unique ne convient pas, il faut des
peot. Le propriétaire a fourni une nouvelle planche de 16 portraits et
quatre rendus isolés, tous redessinés avec des peot tressées. Les 20 poses
nommées par `tools/decoupe_planche.py` en ont été retirées, plus
`char_open_arms` — sans rendu dédié dans cet envoi, mais de même pose et de
dimensions de découpe quasi identiques à `char_arms_raised` (623×900 contre
626×900) : il s'agit très probablement du même rendu source, exporté deux
fois sous deux noms. Il reçoit donc le même correctif.

`char_walking` portait un défaut que l'outil ne peut pas détecter seul, et
que son propre commentaire documente depuis l'incident précédent avec
`char_torah` : une paire de chaussures du personnage de la cellule
au-dessus (`char_praise`) reste visible, flottant au-dessus de sa tête. Ce
n'est pas un fragment détaché — l'ombre portée sous ces chaussures descend
juste assez sombre pour rester connectée, dans la détection de fond par
connexité, à la tête du marcheur en dessous : pour l'algorithme, c'est une
seule forme. Corrigé en blanchissant la bande contaminée de la cellule
source avant la détection de fond, à la main, pour cette seule image.

`tools/verifie_illustrations.py` confirme les 21 fichiers propres : aucun
bord qui tranche, aucun fragment de voisin visible. `ic_launcher_face.webp`,
hors de ce pipeline, n'a pas été touché.

**Réglages, invisible sans faire défiler (23/09), confirmé corrigé.** Après
la correction du nom d'icône et du titre du Tikoun, le propriétaire a
signalé n'avoir toujours ni le choix de langue ni les autres corrections —
sur l'APK exact envoyé, redécodé pour vérifier : le bouton de langue et la
ligne Réglages existaient bel et bien, mais Réglages était le sixième et
dernier élément de la carte « Tout le reste » sur l'écran d'accueil, après
Tikoun, Tefilot, Bibliothèque, Rappels et Musique — hors champ sans
défilement. La même faute que celle déjà réparée pour le bouton de langue
*à l'intérieur* de Réglages, jamais appliquée à Réglages *lui-même*.
`settings_button` déplacé en tête de la carte, avant `tikkun_button` ;
`HomeTest` ne vérifie que la présence des sections, pas leur ordre — vert
sans changement. **Confirmé fonctionnel par le propriétaire le 23/09.**

## Décisions du 22/09

| | |
|---|---|
| **1** | **faite le 22/09** : le prénom, et la carte de départ (durée, sujet, musique, concentration) |
| **2** | sujet **facultatif** — décidé et construit |
| **10** | texte reçu le 22/09, **treize renvois vérifiés, trois corrigés**. Conservé dans `textes/`. **Droits tranchés le 22/09** — le propriétaire en est l'auteur, rien ne s'oppose plus à l'insertion. **Toujours pas dans l'APK**, pour une raison désormais éditoriale et non juridique : quatre renvois restent invérifiables faute de texte de référence. C'est une décision qui revient au propriétaire, pas un blocage technique. |
| **11** | **partage autorisé** — ce sera le seul endroit où un texte sort de l'appareil |
| **12** | **feu vert**. Reste que cette partie n'a jamais tourné sur un vrai téléphone : l'habiller avant de l'éprouver serait mettre un costume à ce dont on ignore s'il marche |
| **15** | **micro autorisé** → `RECORD_AUDIO`, de l'audio à stocker, et un export qui cesse d'être un seul fichier |

## Ce qui reste vrai quoi qu'on ajoute

L'application ne déclare **aucune permission Internet**. Rien ne peut en
sortir, et l'écran d'accueil le promet — un test tient cette phrase au
manifeste. Toute proposition qui demanderait un serveur, un compte ou une
synchronisation romprait cela, et devrait être décidée comme telle.

## Mise à jour (23/09, suite)

**Tous les personnages remplacés par version avec peot (23/09).** Le
propriétaire a fourni une planche de 4×4 avec tous les rôles redessinnés avec
des sidelocks tressées à la place de la queue longue simple : 16 poses + 4
rendus seuls (dansant, guitare, bras levés, bras levés avec guitare) + closeup
tête. Script `decoupe_planche.py` : découpe, détourage fond blanc par
connexité, export WebP. Tous les 21 fichiers drawable remplacés (20 de la grille
+ `char_open_arms` réutilisé comme avant). Vérification `verifie_illustrations.py`
confirme : aucune tranche de bord, aucun fragment voisin. Commit 41f2c80, prêt à
test.

**Correctif (28/09) : la note ci-dessus était fausse sur un point.** Le
commit 41f2c80 a envoyé quatre images isolées (`char_guitar`,
`char_arms_raised`, `char_arms_raised_guitar`, `char_open_arms`) avec un fond
noir ou blanc plein, et réduit `char_dancing_joy` de 900 à 369 px. Leur source
avait déjà une couche alpha, que `convert("RGB")` a aplatie. Le vérificateur,
lui, les avait bien signalées en intégration continue (comme « sommet plat »,
avec `char_standing_back`) : les runs 87 et 88 ont échoué, aucune version n'a été
publiée, et le téléphone est resté sur v1.86. La note disait l'inverse sans avoir
regardé la CI. Les cinq sont revenues à
leur version de v1.86 (détourées, peyot, pas de queue de cheval),
`decoupe_planche.py` ne les produit plus, et `verifie_illustrations.py` nomme
maintenant ce défaut pour ce qu'il est : moins de 10 % de pixels transparents.

## Mise à jour (28/09)

**Nouvelle planche de 16 poses.** Nouveaux rendus, fournis par le
propriétaire, proches des précédents. Le livre tenu contre la poitrine porte
maintenant « תהלים ». Aucune pose ne montre de queue de cheval. La planche
n'est pas une grille régulière : ses rangées font environ 420, 350, 370 et
395 px. `decoupe_planche.py` coupe donc au milieu des bandes blanches, au
lieu de couper au quart exact, ce qui aurait tranché les pieds de la première
rangée.

**Musique muette (signalée sur v1.86, en anglais, pas l'essai).** Les
fichiers MP3 et la construction sont sains. Trois défauts se cumulaient :
- la musique était **désactivée par défaut** (`Store.musicEnabled`), donc une
  séance restait muette tant qu'on n'avait pas trouvé l'interrupteur. Elle est
  désormais active par défaut, et un choix explicite reste respecté ;
- activer l'interrupteur **pendant** une séance ne lançait rien, et le couper
  n'arrêtait rien. Le service de séance suit maintenant l'interrupteur
  (`SessionService.musicChanged`) ;
- un échec de lecture pendant la séance ne produisait aucun message. Il
  affiche maintenant « Ce morceau n'a pas pu être lu », et `MusicPlayer`
  journalise la cause.
À confirmer sur le téléphone : si le message d'échec apparaît, la cause est
ailleurs, et le journal (`adb logcat -s MusicPlayer`) la donnera.

**Queue de cheval (28/09).** Dans v1.86, les poses « louange » et « marche »
montraient encore un chignon à l'arrière de la tête : les peyot avaient été
ajoutées le 22/09 sans retirer la queue. Elles viennent maintenant de la
nouvelle planche, sans chignon. `char_arms_raised_guitar` (vue de dos, cheveux
ramassés en tresse sur la nuque) est retirée : elle ne servait que dans la
rotation « pendant la séance », qui garde quatre poses.

**Aucune version publiée depuis v1.86, deuxième cause.** Le run 89 a tout
réussi (construction, tests, lint) mais l'envoi de l'artefact APK a échoué :
quota de stockage des artefacts plein. La release, qui dépend de cette étape,
n'est pas partie. L'envoi de l'artefact ne bloque plus, et ne garde ses
fichiers que deux jours. Les anciens artefacts (90 jours par défaut) se
suppriment dans Settings → Actions du dépôt, si le quota gêne encore.

## Mise à jour (28/09, suite) : présentation, bravo, couleurs

Trois demandes du propriétaire.

**Présentation au premier lancement.** Elle existait (3 écrans) mais ne
décrivait les fonctions qu'en une phrase. Elle en compte maintenant 7 : la
hitbodedout, la séance, jour après jour, les textes, les réglages, « comment
vous appeler » (צדיק / צדקת), et la promesse que rien ne sort du téléphone,
toujours en dernier. Une installation qui a déjà servi ne la voit pas
d'elle-même (règle inchangée) ; les réglages ont un bouton « Revoir la
présentation ».

**Bravo de fin de séance.** Quand une séance se clôt (pas quand on
l'abandonne), le hassid danse (`char_dancing`, balancement et petit saut,
rien si les animations sont coupées dans le téléphone) avec « Bravo,
Tsadik ! » ou « Bravo, Tsadeket ! ». Sans choix enregistré, le bravo reste
neutre et l'écran pose la question. Il s'ouvre sur l'écran de séance si on
l'avait gardé ouvert, sinon au retour sur l'accueil. Le choix se change dans
les réglages et part dans l'export.

**Couleurs.** Carte « Apparence » dans les réglages : clair / sombre / comme
le téléphone, 5 fonds, 6 couleurs de boutons. Des palettes et non un
nuancier libre : `PaletteTest` vérifie les contrastes des 30 combinaisons, de
jour et de nuit, aux seuils de la palette d'origine. Techniquement, les
écrans pointent désormais sur des attributs de thème (`?attr/mhBrand`…,
`values/attrs.xml`) au lieu de `@color/…`, et la palette choisie les recouvre
à la création de chaque écran (`Apparence.installer`, appelé par
`MyHitbodedutApp`). Le widget et l'icône gardent leurs couleurs : ils sont
dessinés par l'écran d'accueil du téléphone. L'écran de blocage, posé par un
service, reçoit le thème par `Apparence.contexte`.

**Encouragement après une séance annulée (28/09).** Demande du propriétaire :
une séance annulée ouvre l'écran du bravo sans danse, avec « ’Hazak ! » ou
« ’Hizki ! » (« Courage ! » sans choix), sa phrase « Pour tout ce que vous
avez fait jusqu'ici. La prochaine fois sera meilleure. », et un mot du moment
« relance » (7 textes relus, dont Likoutei Moharan II, 78 : אֵין שׁוּם יֵאוּשׁ
בָּעוֹלָם כְּלָל). Likoutei Moharan II, 112 a été proposé, pas ajouté.

**Édition Relecture retirée (28/09).** Demande du propriétaire. Elle n'est
plus construite ni jointe aux versions : chaque version porte deux fichiers,
MyHitbodedut et Light. Le type de construction debug reste, parce que
l'édition d'essai en dépend ; l'écran de relecture n'existe donc plus que
dans l'édition d'essai. Un texte ajouté plus tard avec relu=false ne
s'affichera nulle part dans l'édition normale tant qu'on ne l'aura pas relu
autrement (par exemple dans la conversation, puis relu=true dans le fichier).

**Neuf textes sur le désespoir (28/09).** Choisis par le propriétaire parmi
22 textes proposés, tous au moment « relance » (après une série rompue, et
maintenant après une séance annulée) : Si'hot HaRan 3, 'Hayé Moharan 431,
Likoutei Moharan II 48 et 112, Michlé 24:16, Likoutei Moharan I 261 et 78
(ce dernier aussi au moment « blocage » : il nomme la hitbodedout), Likoutei
Etsot Hit'hazkout 42, Likoutei Tefilot II 39. Hébreu découpé mécaniquement
dans l'export public de Sefaria, vocalisation comprise ; un second témoin par
texte (pour Likoutei Tefilot II 39 : le livre imprimé du propriétaire, qui a
confirmé le texte). Détail dans chizuk.json,
_meta.verification.ajouts_2026_09_28. Le moment « relance » passe de 7 à 16
textes.

## Mise à jour (10/10) : la bande de signets du siddour

**Demande du propriétaire, d'après la capture d'une autre application** : une
bande de marque-pages en haut de la lecture, pratique mais discrète. Un
signet par section titrée (פתיחת אליהו, ציצית, תפילין…), de droite à gauche
comme le texte ; on touche, on y est ; la section qu'on lit se marque
d'elle-même d'une pastille pâle, et la bande la garde en vue. La bande est du
ton de la page (fond noir en lecture de nuit), en petits caractères, avec un
trait fin dessous : 34 px de haut sur le web. Un texte d'une seule section
(Acher yatsar, Kaddich…) n'en a pas.

Une règle tranche le cas des sections courtes de fin (עלינו) : elles ne
montent jamais jusqu'en haut de l'écran, donc tout en bas de la page c'est
la dernière section commencée à l'écran qui se marque, et un signet touché
reste marqué tant que le doigt ne reprend pas la page. La règle est la même
sur Android et sur le web (`SignetsSiddur`, `Siddur.kt` et `regles.js`),
avec les mêmes cas de test.

Pas de marque-page personnel (« reprendre où j'en étais ») : non demandé. Il
se grefferait sur la même bande si on le veut.

## Mise à jour (10/10, suite) : calendrier hébraïque, zmanim, ajouts du siddour

**Décision du 21/09 revue, à la demande du propriétaire** : « sans ville ni
coucher du soleil » ne tient plus. L'application a désormais un lieu, une
ville de la liste ou la position du téléphone, qui reste sur l'appareil.
L'idée n° 7 (« selon le moment de la journée ») peut donc s'appuyer sur les
vrais zmanim ; ce n'est pas encore fait, `SiddurMoment` garde ses heures
fixes.

**Décisions du propriétaire (10/10)** :

1. Hebcal sert de second avis dans les tests seulement. L'application
   s'appuie sur KosherJava (Android) et kosher-zmanim (web), sous licence
   LGPL : la GPL de Hebcal obligerait à publier toute l'application.
2. Le lieu : une ville choisie à la main, plus un GPS facultatif.
3. Le siddour met en évidence les ajouts du jour et replie les autres
   derrière une flèche.
4. Le profil des zmanim et les coutumes de Ta'hanoun sont marqués « à
   valider par un Rav ».

**Construit** :

- un onglet Zmanim : la date hébraïque, les ajouts du jour, tous les horaires
  avec leur méthode, l'entrée et la sortie de Chabbat et des fêtes ;
- un calendrier, avec le détail du jour touché ;
- les ajouts du jour dans les trois siddourim.

Tout est décrit dans `ZMANIM.md`.

**Un écart consigné** : le 15 Adar I, KosherJava ne dit pas Ta'hanoun,
Hebcal le fait dire partout. Le résultat de KosherJava est gardé ; voir
`ZMANIM.md`.

**Encore deux demandes du même jour** :

- l'accueil, sur un téléphone haut (vidéo du 07/10), tient de nouveau dans
  l'écran ;
- l'onglet Réglages ne porte plus de pastille quand une autorisation
  manque.

**Suite du 10/10** :

- l'algorithme de la NOAA est imposé explicitement (`NOAACalculator`) sur
  Android et sur le web, et vérifié contre les formules publiées par la NOAA ;
- Anénou : deux mentions distinctes, l'officiant (répétition) et le
  particulier (Chomea téfila), pour tous les jeûnes publics, Tich'a bèAv
  compris ;
- le noussa'h compte : bénédiction du Hallel partiel (Edot sans, Sfard et
  Ashkenaz avec), Morid hatal ashkénaze hors d'Israël, Anénou du particulier ;
- Pourim le 15 Adar quand la ville choisie est Jérusalem ;
- l'omer affiché est celui qu'on compte le soir même.
- Ta'hanoun : plus de Ta'hanoun le matin de la veille de Roch Hachana
  (OC 581:3), ni, pour les Edot, du 22 Tichri à la fin du mois.

Détail et sources : `ZMANIM.md`, « Différences de noussa'h ».

## Mise à jour (11–12/10)

**Fait** :

- Moussaf de Roch 'Hodech (trois noussa'h), sans Chabbat Roch 'Hodech ; les
  צירופי הוי״ה et אהי״ה du mois avec leur nikkoud, vérifiés sur deux
  siddourim et sur le texte massorétique des versets ; à penser, jamais à dire.
- Hallel (trois noussa'h) ; dans Cha'harit, « Aujourd'hui · Hallel › » et
  « Aujourd'hui · Moussaf › » les jours où ils se disent.
- Halakha du jour (Ben Ish 'Haï, hors ligne) et hilloulot du jour, avec un
  écran chacun.
- Trophées, paliers, confettis et notification de fête.
- Bibliothèque : Likoutei Etsot (sans nikkoud, licence de la vocalisation
  non déclarée) et Sefer HaMiddot (vocalisé, domaine public).
- Police au choix et taille pour tous les textes ; en-têtes plus bas sur
  petit téléphone ; pas d'image pendant la lecture du siddour.
- Mentions de sources réduites à celles qu'imposent les licences ; plus de
  Sefaria.

**Reste** :

- les propositions 2, 3/15, 5, 6, 7 (sur les vrais zmanim), 9, 10, 11 et 12
  ci-dessus, inchangées ;
- les icônes signalées le 12/10 : lesquelles, à préciser ;
- l'APK : essayer sur un vrai téléphone la barrière du matin (proposition
  12), jamais éprouvée ;
- Kokhvei Or : toujours pas fourni.

## Mise à jour (12/10, suite)

**Fait** :

- Réinitialiser l'app (fin des réglages) ; partager une hitbodedout avec le
  lien de la version web.
- Trophées : étagère d'objets variés en 3D (coupe, écusson, étoile, rosette,
  médaille, pièces, cristal, gemme), avatar, 9 familles et 113 niveaux, avec
  des paliers à chaque dizaine et quinzaine pour les six premières.
- Espacement des textes sur tous les écrans de lecture ; barre de lecture
  (taille, police) sur les écrans halakha et hilloulot du web.
- Hilloulot : 4 049 (au lieu de 2 595), avec leur source à l'écran.
  - Wikipédia hébreu : le début de l'article, jusqu'à six phrases.
  - MyTzadik et המאיר לארץ : biographies mot pour mot, reprises avec
    l'accord reçu par le propriétaire de l'app (12/10), mention et lien.
  - Hidabroot : faits seuls et lien vers ses articles ; 15 ajouts confirmés
    par Wikipédia.
  - תורת אמת (סדר הדורות, CC BY-NC-SA 2.5) : dates des fils de Jacob.
  - Rav Shmouel Birnbaum : 28 Tevet (et non 28 Chevat).
  - Les biographies longues dans hilloulot_bios.json, lues à l'écran des
    hilloulot seulement ; repli « Lire la suite ».

**Reste** :

- breslev.co.il et « ימי הילולא דצדיקיא » (site שטייגן) : pas sans accord
  écrit ;
- « יד מאיר » : site à préciser ;
- quand deux sources donnent des jours différents, le tsaddik paraît aux
  deux dates, chacune avec sa source (ex. Rabbi Akiva, 9 Tichri chez
  המאיר לארץ) ;
- les icônes signalées le 12/10 : lesquelles, à préciser.

## Mise à jour (13/10)

**Droits — déclarations du propriétaire, 13/10, telles quelles** :
« droits reçus si textes non copiés dans leur entièreté », puis « Je te donne
toutes les autorisations arrête de me demander ». Elles portent sur les
textes d'or-breslev.co.il et le Yalkut Yosef. Utile pour la trace : de qui
vient l'accord, quand, et sur quoi il porte.

**Fait** :

- Halakha du jour : le **Yalkut Yosef – Kitsour Choulhan Aroukh** (Rav Yitzhak
  Yosef, texte de Torat Emet, CC BY-NC-SA 2.5 ; « כל הזכויות שמורות … אסור
  להעתיק קטעים מהספר ללא ציון המקור »). Une **sélection** — jamais le livre
  entier — de halakhot **courtes** (420 caractères au plus), deux par jour, la
  source à chaque halakha : Roch 'Hodech ; avant et pendant chaque fête ses
  lois (Pessa'h dès le 16 Adar, Chavouot pendant l'omer, Bein haMetsarim,
  Eloul et Roch Hachana, Kippour, Soukkot, 'Hanouka, Tou biChvat, Pourim) ;
  le vendredi le Chabbat ; sinon la vie de chaque jour. Le Ben Ish 'Haï reste
  dessous. Le fichier d'or-breslev n'est pas joignable d'ici et sa couche
  texte est un OCR bruité : le même texte Torat Emet est lu page à page sur
  itim-latora.org (tools/yalkut_yosef_construction.py).
- Bibliothèque : l'onglet s'appelle « ספרייה » depuis le 12/10 (l'écran vu le
  13/10 était une version antérieure) ; la ligne « ספרייה » devient « מקורות
  התבודדות », et une ligne « ספרים » ouvre les livres entiers :
  - ליקוטי מוהר״ן (חלק א׳ et תנינא, rabenubook, Public Domain, vocalisé) ;
  - השתפכות הנפש et כוכבי אור (or-breslev, domaine public, accord déclaré) ;
  - מגילת אסתר, שיר השירים (tanach.us, Public Domain, comme les Tehilim) ;
  - נשמת כל חי (Siddur Edot HaMizrach, Torat Emet 357, Public Domain) ;
  - פרק שירה (Torat Emet 357, Public Domain).
- Kokhvei Or, demandé depuis le 22/09, est enfin dans l'application.
- Pourim : la Méguila d'Esther s'affiche sous Arvit (la nuit qui ouvre
  Pourim) et sous Cha'harit ; le 15 Adar d'après le lieu, sans réglage
  (demande du 13/10) : la ville de Jérusalem, ou une position à 10 km au plus
  de la Vieille Ville (rayon choisi par l'application) ; Pourim mechoulach :
  le vendredi 14. Les villes au statut douteux (Hébron, Tibériade, Safed…),
  où l'on lit les deux jours, ne sont pas traitées.
- Halakha du jour : les deux halakhot dans une seule carte.
- Birkat haLevana sur la carte du jour (comme CalJ) : la fenêtre du mois,
  sept jours après le molad jusqu'à la mi-mois (Choulhan Aroukh O.H. 426,
  KosherJava), « ce soir » / « dernier soir » / « dans N jours », et une
  jauge du mois lunaire. Vérifié contre CalJ : 'Hechvan 5787, du 18 au 25
  octobre 2026.

**Reste** :

- livres.json pèse 6,9 Mo (Likoutei Moharan : 4,6 Mo) : lu à l'ouverture des
  livres ; à découper par livre si l'ouverture paraît lente sur téléphone ;
- les autres textes d'or-breslev, sur demande.

## Mise à jour (14/10)

**Fait** :

- Été / hiver : l'Arvit ashkénaze ne basculait pas « ברך עלינו » (la consigne
  et la variante étaient dans le même bloc) ; corrigé, et le test couvre
  désormais Cha'harit, Min'ha et Arvit dans les trois nusah. Dates vérifiées
  (KosherJava) : Israël du 7 'Hechvan ; diaspora le 5 décembre 2026, le
  6 décembre 2027 (année civile bissextile qui suit).
- Polices : sept au choix (quatre ajoutées, libres OFL, toutes avec le
  nikkoud : Frank Ruhl, David, Rachi, Miriam) ; sur le web, une feuille de
  choix où chaque police montre un verset vocalisé.
- Siddour : le groupe « בְּרָכוֹת לַזְּמַנִּים וְלַמִּצְווֹת », 21 textes dans les
  trois nusah (tools/siddur_occasions.py, domaine public) : Ilanot, arc-en-
  ciel, éclairs et tonnerre, la mer, bénédictions des plaisirs, HaGomel,
  mezouza, 'hala, tévilat kélim, parapet, téroumot et maasrot, avant un
  remède, 'Hanouka, Méguila, omer, soukka, loulav, 'hamets, érouv tavchilin,
  bougies de Chabbat, Havdala. Ce qu'un nusah n'a pas dans ses sources libres
  est emprunté et marqué (l'Ashkenaz surtout, au Sefard).
- Birkat haIlanot proposée dans le siddour le mois où on la dit.
- « מקורות ההתבודדות ».

## Mise à jour (14/10, suite)

- Écran Zmanim : la carte du jour ne garde que la date et les ajouts ; trois
  boutons (הלכה יומית, הילולות, לוח שנה) ; puis les horaires ; Birkat haLevana,
  la hilloula et la halakha viennent après, chacune masquable (Réglages → Écran
  Zmanim). Le calendrier garde la carte complète.
- Halakha du jour : livre au choix — Yalkut Yosef (défaut), Ben Ish 'Haï, ou
  les deux (écran de la halakha et réglages). Le Ben Ish 'Haï était la première
  halakha hors ligne (11/10, domaine public) ; le Yalkut Yosef est venu le 13/10.
- Hilloulot : police plus petite (noms, textes, sources), Android et web.
- Réglages : police hébraïque, écran d'ouverture (Aujourd'hui, Bibliothèque,
  Zmanim) ; sur le web, le mode clair/sombre, le fond et l'accent (déjà sur
  Android).
- Birkat haIlanot dans l'hémisphère sud : Eloul et Tichri. Sources recoupées :
  Yeshivat Har Etzion (Laws of Berakhot 81, citant Har Tsvi O.H. 1:118 et
  Min'hat Yits'hak 10:16 : quand les arbres fleurissent) ; Halachipedia
  (« Birkat HaIlanot » : en Australie, Eloul et Tichri) ; Torah Live (vers
  Tichri). Les responsa elles-mêmes n'ont pas été lues.

## Mise à jour (14/10, Birkat haLevana revue sur le texte)

Vérifié sur le texte (export public de Sefaria) : Choulhan Aroukh O.H. 426:2-4 et
Michna Beroura 426:6, 11, 17, 19, 20.
- Début : 7 jours (CA 426:4) ; 3 jours d'heure à heure pour « la plupart des
  A'haronim » (MB 426:20). Fin : 15 jours depuis l'heure du molad (CA 426:3,
  MB 426:17) ou la mi-mois (Rema, MB 426:19).
- Usage par défaut : Edot haMizra'h 7 j → 15 j (Choulhan Aroukh) ; Sefard 7 j →
  mi-mois (l'Ari : Chabad.org, Ohr Somayach ; non lu dans les écrits de l'Ari) ;
  Ashkenaz 3 j → mi-mois. Modifiable : Réglages → Écran Zmanim.
  Avant : 7 j → mi-mois pour tous (ni le Choulhan Aroukh, ni la Michna Beroura).
- Pas avant la sortie de Kippour ni de Tich'a beAv (Rema 426:2 ; MB 426:11).
- Motsaé Chabbat conseillé quand sa nuit tombe avant le 10 (Rema 426:2, MB 426:6),
  sauf motsaé Chabbat qui est Yom Tov.
- Heures exactes de début et de fin (KosherJava / kosher-zmanim).
- La lune de ce soir dessinée à sa phase réelle (Meeus ch. 48), côté éclairé selon
  l'hémisphère du lieu. Recoupé : nouvelle lune 10/10 15:50 UTC, premier quartier
  18/10 16:12 UTC, pleine lune 26/10 04:12 UTC (timeanddate, Griffith Observatory,
  Royal Observatory Greenwich) ; écart ≤ 1 point avec moongiant.com.
- Retirée : la figure assise (char_reading_seated), remplacée par la Torah (bandeau
  Zmanim), Tehilim (Arvit, bibliothèque), l'écriture (relecture).

## Hémisphère sud, Tal ou-matar (14/10)

- Yalkut Yosef, Kitsour Choulhan Aroukh O.H. 117:20 (texte lu sur
  moreshet-maran.com ; renvoie à Chéérit Yossef III p. 105 et Yalkut Yosef Tefila II,
  117 note 20) : au Brésil, en Australie…, ותן טל ומטר dans ברך עלינו du 60ᵉ jour
  après la tekoufa jusqu'à la veille de Pessa'h, comme la diaspora ; « מוריד הטל »
  toute l'année. Codé pour le noussa'h Edot quand le lieu est au sud.
- Recoupé pour « מוריד הטל toute l'année » : R. Y. Kaganoff (yeshiva.co,
  « Davening for Rain in the Southern Hemisphere »), shofar.tv. Les communautés
  ashkénazes suivent les dates habituelles (R. A. A. Hirschowitz, Beit Avraham, avec
  R. Yits'hak El'hanan Spektor et R. Chmouel Salant, d'après torahb.co.il) :
  Ashkenaz et Sefard inchangés.
- Halakha Beroura (R. David Yosef), siman 117 : texte non trouvé en ligne, non
  vérifié.

## Ce qui manque — liste complète (14/10)

**Textes et prières**
- Les offices de Chabbat et des fêtes (Kabbalat Chabbat, Amida et Moussaf de
  Chabbat, Amida des fêtes, Hallel des fêtes, Kiddouch), le Ma'hzor (Roch
  Hachana, Kippour), les Seli'hot, la Haggada, les Hochaanot : les sources
  libres existent, rien n'est encore dans l'application.
- Birkat ha'Hama (tous les 28 ans, prochaine en 2037) : pas de texte.
- Brit mila, pidyon haben : les textes sont dans les sources, non repris.
- Nusah Ashkenaz : la plupart des bénédictions des occasions sont empruntées
  au Sefard (pas de version ashkénaze vocalisée et libre trouvée).
- Kokhvei Or est là ; les livres du Rav Arush et du Yanuka : aucun texte fourni.

**Calendrier**
- Pourim : les villes au statut douteux (Hébron, Tibériade, Safed…) où l'on
  lit deux jours ne sont pas traitées.
- Hémisphère sud : Birkat haIlanot en Eloul et Tichri (fait, voir 14/10 suite).
  Tal ou-matar : dates de la diaspora pour tous (inchangé). Morid hatal : voir
  « Hémisphère sud, Tal ou-matar (14/10) ».

**Fonctions (les onze propositions)**
- 2 : choix du thème au début et structure adaptée au thème.
- 3 / 15 : « Je ne sais pas quoi dire » en questions, une à la fois.
- 5 : journal — humeur avant/après, thème, gratitude et chose à travailler.
- 6 : page « Mon parcours » et thèmes fréquents.
- 7 : accueil et texte selon le moment de la journée.
- 9 : ambiances (pluie, mer, forêt, feu, nuit, silence) et leurs fichiers.
- 10 : « source du jour » distincte du 'hizouk.
- 11 : 'hizouk à la demande hors séance, favoris, partage.
- 12 : présentation « Je suis avec Hachem » de la barrière.

**Versions**
- Web : le journal, les Tefilot de Rabbi Natan et les conseils n'y sont pas
  encore (« à venir »).
- Nokia : minuteur, journal, textes et musique seulement ; ni zmanim, ni
  siddour, ni halakha, ni livres.
- Android : la barrière du matin jamais éprouvée sur un vrai téléphone ;
  livres.json (6,9 Mo) à découper si l'ouverture est lente.

**Droits et sources en attente**
- breslev.co.il : accord écrit nécessaire ; « יד מאיר » : site à préciser.
- Pour la trace : de qui vient l'accord du 13/10 (or-breslev, Yalkut Yosef),
  quand, et sur quoi il porte.
- Les icônes signalées le 12/10 : lesquelles.
