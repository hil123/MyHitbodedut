# Morceaux non embarqués dans l'APK

Ces fichiers sont **versionnés dans le dépôt mais absents de l'application**.
Ils ne sont pas dans `android/app/src/main/res/raw/`, donc ils n'entrent pas
dans l'APK et n'en augmentent pas le poids.

## Pourquoi

L'audio embarqué a atteint le plafond de **130 Mo** — le chiffre exact est
au tableau plus bas — contre 12 Mo pour tout le reste de l'application après
R8. Le garde-fou du test, déjà relevé trois fois (40 → 80 → 110 → 130), est
au bout.

Depuis, tout nouvel envoi arrive ici automatiquement : `tools/ajoute_musique.py`
compare le poids embarqué au plafond et range de ce côté-ci dès qu'il est
atteint. Le plafond n'a plus bougé.

Plutôt que de le relever une quatrième fois en silence, les fichiers sont
gardés ici : rien n'est perdu, l'APK ne bouge pas, et le choix reste ouvert.

## Comment les écouter quand même, dès aujourd'hui

L'application sait **déjà** lire un fichier du téléphone — `MusicActivity`
ouvre un sélecteur (`OpenDocument`) et `MusicLibrary.addUserTrack` l'ajoute à
la liste, au même titre que les morceaux livrés. Il n'y a donc rien à
construire :

1. copier ces fichiers sur le téléphone ;
2. écran **Musique** → ajouter un morceau ;
3. ils apparaissent à côté des autres.

## Le choix qui reste à faire

**Soit** on continue d'embarquer, et l'APK dépasse 140 Mo pour une
application dont le cœur pèse 12 Mo.

**Soit** on n'embarque qu'une petite sélection — trois ou quatre — et tout le
reste vit ici, à copier sur le téléphone. L'APK retombe à une vingtaine de Mo.

La seconde voie ne demande aucune permission nouvelle, aucun hébergement, et
ne soulève **aucune question de droits supplémentaire** — contrairement à un
téléchargement depuis un serveur public, qui rendrait ces enregistrements
accessibles à n'importe qui et non plus aux seules personnes à qui l'APK est
remis.

## Ce qu'il y a ici

| fichier | poids |
|---|---|
| `ani_maamin` | 2,0 Mo |
| `anim_zemirot` | 2,1 Mo |
| `bratslav_melodies_piano` | 3,3 Mo |
| `breslov_playing_field_sea_waves` | 4,8 Mo |
| `chabad_shabbos_shneor_lerer` | 4,7 Mo |
| `chabad_yamim_noraim_shneor_lerer` | 8,6 Mo |
| `nigun_abaal_shem_tov_for_babies` | 6,7 Mo |
| `nigunim_for_neshama_shneor_lerer` | 27,8 Mo |
| `relaxing_chabad_melodies_shneor_lerer` | 7,5 Mo |
| `tremendous_threat_breslau_piano` | 9,5 Mo |
| `tzama_with_orchestra` | 2,4 Mo |
| `tzomo_lecho_s_lerer` | 2,4 Mo |
| `vehi_sheamda_piano_shneor_lerer` | 2,0 Mo |
| `vehi_sheamda_s_lerer` | 2,1 Mo |
| `yosef_karduner` | 6,6 Mo |
| `niggoun_01` … `niggoun_21` *(sans titre)* | 101,0 Mo |

**36 morceaux, 193,3 Mo.** L'APK est à 129,5 Mo d'audio sur 31 morceaux.

> Cet inventaire est régénéré depuis les fichiers, pas tenu à la main.

## Titres

Ceux qui portent un nom l'ont gardé. Les 21 `niggoun_NN` n'en ont aucun :
leurs noms de fichiers étaient entièrement en hébreu et le transfert les a
remplacés par des underscores, et les tags ID3 sont vides. D'où la
numérotation — on ne devine pas un titre. Les vrais noms sont les bienvenus.

## Droits

Shneor Lerer s'ajoute aux noms déjà couverts par les déclarations du
propriétaire (Rav Arush et le Yanuka ; puis Ishay Ribo, Motty Steinmetz, MBD
et Shmuel). Il est noté ici parce que le registre ne vaut que s'il est tenu à
jour — voir `FEUILLE_DE_ROUTE.md`.
