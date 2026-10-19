# Feuillets de prières — transcriptions

Neuf feuillets reçus du propriétaire le 22/09/2026, en PDF. **Vingt-sept
pages**, toutes en image : une seule (`prayer-02`) porte une couche texte,
les huit autres ont dû être lues à l'œil.

## Droits

Le propriétaire déclare avoir **obtenu les autorisations légales des ayants
droit** pour tout ce qui vient du Rav Shalom Arush et du Yanuka. Les
feuillets portent pour leur part, en pied de page, *כל הזכויות שמורות
למכון 'ים שלמה'*. Les deux mentions sont consignées côte à côte, ici et dans
FEUILLE_DE_ROUTE.md.

**Dans l'application depuis le 19/10** (réponse « oui tout » du propriétaire) :
livre « תפילה לשלמה — הינוקא » de la bibliothèque, construit par
`tools/livres_textes_proprietaire.py`. Seul le texte des feuillets y passe,
pas les notes de relecture ; pour 'Habaqouq, les versets seuls (le
commentaire entrelacé n'a pas d'ordre sûr) ; le Nom y est écrit en entier.

## Méthode

Transcrire de l'hébreu **vocalisé** à partir d'une image est faillible, et
l'a été : deux erreurs dans `prayer-06`, une dans `prayer-04`, toutes
invisibles au premier passage. D'où la méthode :

1. page rendue à **200 dpi**, lue, transcrite ;
2. page **re-rendue à 400 dpi**, zones douteuses recadrées et agrandies
   jusqu'à ×6 ;
3. transcription comparée à ces agrandissements, écarts corrigés et
   **consignés** dans chaque fichier ;
4. quand le texte est biblique, il n'est **pas** lu à l'œil : il est repris
   du Tanakh (corpus Sefaria, domaine public) puis comparé à l'image. Plus
   sûr que n'importe quelle lecture.

Ce qui reste incertain est **marqué**, jamais deviné.

## Anomalies trouvées dans les sources

| feuillet | anomalie |
|---|---|
| `prayer-07` | renvoie à **(בראשית כח)** ; les versets sont **Béréchit 21, 1-12**. Établi par comparaison mécanique avec le Tanakh. |
| `prayer-04` | renvoie à *ירמיה ל"א, י"ד-ט"ו* mais cite **trois** versets (31:15-17 en massorétique) pour une plage qui en annonce deux |
| `prayer-04` | « וְתִשְׁמַע אֶת » suivi de « וְאֶת זַעֲקַת לִבִּי » : un mot paraît manquer |
| `prayer-08` | ketiv/qeré traités de façon inconstante : v. 3 imprime les deux, v. 9 et 10 le qeré seul |

Rien n'a été corrigé en silence : tout est reproduit tel qu'imprimé, avec la
remarque à côté.

## État

| feuillet | pages | sujet | transcrit |
|---|---|---|---|
| prayer-01 | 1 | פדיון שבויים — libération des captifs | ✅ |
| prayer-02 | 2 | תפילת חבקוק — 'Habaqouq 3, guéoula | ⚠️ versets exacts, commentaire avec réserve |
| prayer-03 | 15 | **תפילה נוראה לישועת ישראל** — « עקב המצב בארץ הקודש » | ✅ |
| prayer-04 | 2 | זכות רחל אמנו | ✅ |
| prayer-05 | 1 | זיווג (לאישה) | ⚠️ réserve de définition |
| prayer-06 | 1 | זיווג (לאיש) | ✅ |
| prayer-07 | 1 | זרע של קיימא (לאיש) | ✅ |
| prayer-08 | 1 | זרע של קיימא (לאישה) | ✅ |
| prayer-09 | 2 | ישועה ושלוות הנפש — l'angoisse | ✅ |
| prayer-10 | 2 | רפואה שלימה | ⚠️ partiel |


**prayer-10** : le la'hash et les douze versets sont faits ; la note en petits
caractères et le commentaire intercalé entre les mots des versets ne le sont
pas. Dit dans le fichier.

**prayer-05** n'a jamais manqué : c'est la prière de zivoug féminine, envoyée
comme image et identifiée par le propriétaire comme « Prière 5 ». **Les neuf
feuillets sont donc au complet.**

Elle reste la moins sûre de la série : 1290 px de large, contre 2339 px pour
les PDF et 4959 px pour le livret. Envoyée deux fois, les deux fois **le même
fichier au bit près** — une seconde copie n'ajoute pas de définition. Seul le
PDF d'origine permettrait de la reprendre au niveau des autres.

**prayer-02** : seul feuillet à couche texte, donc les *caractères* sont ceux
du fichier et non une lecture. Mais les boîtes des fragments se chevauchent
dans le PDF, si bien que l'*ordre* n'est pas récupérable mécaniquement : des
inversions locales subsistent entre le verset et son commentaire. Les versets
de 'Habaqouq sont donc donnés à part, exacts, repris du Tanakh.

**prayer-03** : les quatorze pages de texte sont transcrites. Rendu à
4959 × 7014, plus du double de la définition des feuillets où mes cinq erreurs
précédentes s'étaient glissées — et cette fois les deux passages qui m'ont fait
douter se sont révélés justes au zoom.

Ses citations, elles, ont pu être vérifiées : **Téhilim 142 en entier et les six
versets de Yoël sont exacts**, sans une anomalie. Les litanies liturgiques
(« אַתָּה דָּגוּל וְשִׁמְךָ דָּגוּל… », « רוֹכֵב עֲרָבוֹת ») ne sont dans aucun
des trois siddourim du corpus ni dans aucun ma'hzor : celles-là n'ont pu être
que lues.

**Ce livret donne une adresse différente des autres feuillets** — celle,
formelle, de l'institut : ת.ד 32466 ירושלים, 02-376-3737,
`contact@yamshlomo.co.il`. C'est celle à utiliser pour documenter une
autorisation.

## Cinq erreurs de lecture, toutes attrapées au zoom

Aucune ne se voyait à 200 dpi. C'est l'argument de la méthode.

| feuillet | j'avais lu | l'imprimé dit |
|---|---|---|
| prayer-06 | וְתֶן לִי | **וְתֵן לִי** |
| prayer-06 | מַה טּוֹב | **מַה טוֹב** (sans daguech) |
| prayer-04 | קוֹדֵא | **קוֹרֵא** |
| prayer-09 | וְלִדְרוֹצוֹת *(mot inexistant)* | **וְלִרְצוֹת** |
| prayer-10 | אֶל הַיָּם גּוֹזֵר | **אֶל הַיָּם גְּנוֹסַר** *(le Kinneret)* |

Trois des cinq sont un **ר lu ד**, ou l'inverse : à basse définition les deux
lettres sont les mêmes. C'est la raison d'être du second passage à 400 dpi.

## Citations vérifiées contre le Tanakh

Quand un feuillet cite l'Écriture, la citation n'est pas transcrite à l'œil :
elle est comparée mot à mot au corpus. Bilan :

| feuillet | citation | verdict |
|---|---|---|
| prayer-07 | Béréchit 21:1-12 | exacte, mais **la référence imprimée est fausse** (כח pour כא) |
| prayer-08 | Chmouel I 2:1-10 | exacte, référence juste |
| prayer-04 | Yirmiyahou 31 | texte exact ; **trois versets cités pour une plage de deux** |
| prayer-09 | Téhilim 2:7, 2:8 | exactes, références justes |
| prayer-09 | Bamidbar 6:24-26 | exacte, référence juste |
| prayer-09 | Dévarim 30:19 | exacte, référence juste |
| prayer-09 | Dévarim 28:28 | exacte, **citée sans référence** |
| prayer-10 | Yéchayahou 38:9-20 | exacte |
