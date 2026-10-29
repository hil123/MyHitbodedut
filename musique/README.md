# Morceaux non embarqués dans l'APK

Ces fichiers sont **versionnés dans le dépôt mais absents de l'application**.
Ils ne sont pas dans `android/app/src/main/res/raw/`, donc ils n'entrent pas
dans l'APK et n'en augmentent pas le poids.

Depuis le 29/10 (audit 1.390), les morceaux embarqués sont en AAC 96 kb/s
(`.m4a`) : l'audio de l'APK est passé de 130 à 85 Mo. Ceux d'ici gardent leurs
MP3 d'origine ; `tools/echange_musique.py` et `tools/ajoute_musique.py`
réencodent en AAC ce qui entre dans l'APK (`tools/audio.py`, ffmpeg requis).

Le 29/10 (audit 1.390), `ribo_sans_titre` est sorti de l'APK et du site, sur
décision du propriétaire : aucun morceau d'Ishay Ribo n'est diffusé tant qu'il
n'y a pas d'accord écrit de l'artiste ou de son label. Il revient ici en MP3
d'origine.

## Pourquoi

L'audio embarqué a atteint le plafond de **130 Mo** — le chiffre exact est
au tableau plus bas — contre 12 Mo pour tout le reste de l'application après
R8. Le garde-fou du test, déjà relevé trois fois (40 → 80 → 110 → 130), est
au bout. Il n'a pas bougé depuis, et ne bougera pas sans décision explicite.

## Ce que contient ce dossier aujourd'hui : un échange en cours

**Le contenu s'est inversé le 22/09, à la demande du propriétaire.** Les
morceaux sans titre sont maintenant **dans** l'APK, et les morceaux qui
portent déjà un nom sont ici.

La raison : on ne peut nommer que ce qu'on entend. **Trente-cinq** morceaux
attendaient d'être identifiés, dont vingt-et-un hors de l'application ; les
écouter aurait demandé de copier les fichiers sur le téléphone. L'échange en
met **trente-trois** sous la main, le temps d'un aller-retour.

Trente-et-un d'entre eux portent désormais **« Niggoun NN — titre à
préciser »**, le NN étant le numéro du fichier : c'est ce qui permet de dire
« le 07 est untel » sans ambiguïté. Avant l'échange ils s'appelaient tous
« Niggoun — titre à préciser », indistinguables dans une liste. La suite va
de 01 à 33 **sans le 09 ni le 16**, restés ici. Les deux derniers du lot
gardent l'attribution qu'ils avaient déjà : « Ishay Ribo — titre à préciser »
et « ניגון — הינוקא (3) ».

**L'échange se défait de la même façon** : `tools/echange_musique.py` prend
la liste des morceaux à embarquer et remet tout le reste ici, en une fois.
Les titres des morceaux écartés ne sont pas perdus — ils restent dans un
bloc de réserve de `values/strings.xml` et reviennent tels quels.

**Échange du 19/10, à la demande du propriétaire** : *המלך בשדה* (9,2 Mo)
revient dans l'APK ; pour rester sous le plafond, `niggoun_08` (2,4 Mo, sans
titre) vient ici. L'APK passe à 129,8 Mo.

**Deux morceaux sans titre n'ont pas pu entrer** : `niggoun_09` (15,3 Mo) et
`niggoun_16` (9,6 Mo). L'ensemble des sans-titre pèse 147 Mo, le plafond est
à 130 ; le choix a été de garder le plus grand nombre de morceaux plutôt que
le plus grand poids, donc d'écarter les deux plus lourds. Ils entreront au
prochain échange, quand les autres auront un nom.

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
| `ribo_ata_zocher` | 5,6 Mo |
| `ribo_halev_sheli` | 5,6 Mo |
| `ribo_nafshi` | 6,1 Mo |
| `ribo_ochila` | 5,3 Mo |
| `ribo_sans_titre` | 4,6 Mo |
| `ribo_seder_haavoda` | 8,1 Mo |
| `ribo_zusha` | 4,4 Mo |
| `shmuel_one_prayer` | 2,6 Mo |
| `tremendous_threat_breslau_piano` | 9,5 Mo |
| `tzama_with_orchestra` | 2,4 Mo |
| `tzomo_lecho_s_lerer` | 2,4 Mo |
| `vehi_sheamda_piano_shneor_lerer` | 2,0 Mo |
| `vehi_sheamda_s_lerer` | 2,1 Mo |
| `yanuka_a_part_of_me` | 4,0 Mo |
| `yanuka_anticipation` | 1,4 Mo |
| `yanuka_harping_his_praise` | 7,1 Mo |
| `yanuka_listening_to_the_heart` | 2,3 Mo |
| `yanuka_shlomo_yehuda` | 5,8 Mo |
| `yanuka_shlomo_yehuda_2` | 4,2 Mo |
| `yanuka_the_king_will_return` | 2,7 Mo |
| `yanuka_the_truth_will_rise` | 2,4 Mo |
| `yanuka_yearning` | 5,6 Mo |
| `yosef_karduner` | 6,6 Mo |
| `niggoun_08`, `niggoun_09`, `niggoun_16` *(3 sans titre)* | 27,4 Mo |

**35 morceaux, 197,6 Mo.** L'APK est à 82,1 Mo d'audio sur 32 morceaux.

> Cet inventaire est régénéré depuis les fichiers, pas tenu à la main.

## Titres

Ceux qui portent un nom l'ont gardé. Les trois `niggoun_NN` n'en ont aucun :
leurs noms de fichiers étaient entièrement en hébreu et le transfert les a
remplacés par des underscores, et les tags ID3 sont vides. D'où la
numérotation — on ne devine pas un titre. Les vrais noms sont les bienvenus.

**Depuis le 22/09 on renomme depuis l'application** : écran Musique, le
crayon sur la ligne du morceau (l'appui long marche aussi). Le renommage
valait déjà pour les morceaux livrés, mais il n'était accessible que par
appui long, sans bouton — le propriétaire a demandé la fonction alors
qu'elle existait, ce qui tranche la question de l'affordance.

**Attention : ces noms restent sur le téléphone.** Ils sont écrits dans les
préférences de l'application, pas dans le dépôt ; une réinstallation les
perd, et ils ne remontent pas dans l'APK. Pour qu'un titre devienne celui
que tout le monde voit, il faut me le dire — il entre alors dans
`values/strings.xml`.

## Droits

Shneor Lerer s'ajoute aux noms déjà couverts par les déclarations du
propriétaire (Rav Arush et le Yanuka ; puis Ishay Ribo, Motty Steinmetz, MBD
et Shmuel). Il est noté ici parce que le registre ne vaut que s'il est tenu à
jour — voir `FEUILLE_DE_ROUTE.md`.
