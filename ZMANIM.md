# Calendrier hébraïque, zmanim et ajouts de prière

Demande du 10/10/2026, sur Android et sur le web. Ce fichier dit ce que
l'application calcule, avec quoi, selon quelle méthode, et où elle s'arrête.
**Le profil halakhique est à valider par un Rav** : rien ici n'est une
décision halakhique, chaque méthode est nommée à l'écran sous son horaire.

## Les moteurs

| Rôle | Android | Web | Licence |
| --- | --- | --- | --- |
| Moteur principal | KosherJava 2.5.0 (`com.kosherjava:zmanim`, déjà utilisé par `Holidays.kt`) | kosher-zmanim 0.9.0, son portage JavaScript, servi tel quel (`web/src/vendor/`) | LGPL 2.1 / LGPL 3.0 |
| Règles de prière | `TefilaRules` de KosherJava | `web/src/js/tefila.js`, portage ligne à ligne de `TefilaRules.java` (absent de kosher-zmanim) | LGPL 2.1 |
| Comparaison | — | Hebcal (`@hebcal/core` 6.14.1), **dans les tests seulement**, jamais servi | GPL 2.0+ |

Tout se calcule sur l'appareil, sans réseau. Le site garde sa politique de
sécurité (`connect-src 'self'`) ; l'application Android ne déclare toujours
aucune permission Internet.

Hebcal n'est pas embarqué : sa licence GPL obligerait à publier toute
l'application sous GPL. Il sert de second avis dans les tests.

## Le profil des zmanim

Une seule liste, la même sur les deux plateformes (`Zmanim.PROFIL`,
`web/src/js/zmanim.js`). Calcul **au niveau de la mer** (`isUseElevation`
faux), dans le fuseau IANA du lieu, par **l'algorithme de la NOAA**
(`NOAACalculator`, imposé explicitement sur les deux plateformes : c'est le
défaut de KosherJava, mais l'application ne dépend plus de ce défaut). Les descriptions entre guillemets sont
celles de la documentation de KosherJava 2.5.0 (`zmanim-2.5.0-sources.jar`).

| Zman | Méthode KosherJava | Formule |
| --- | --- | --- |
| עלות השחר | `getAlos16Point1Degrees` | soleil à 16,1° sous l'horizon géométrique ; « la position du soleil 72 minutes avant le lever à Jérusalem vers l'équinoxe » |
| משיכיר | `getMisheyakir11Point5Degrees` | soleil à 11,5° ; « 52 minutes avant le lever à Jérusalem vers l'équinoxe », « selon certaines opinions » |
| הנץ החמה | `getSeaLevelSunrise` | lever au niveau de la mer (réfraction et demi-diamètre du soleil compris) |
| סוף זמן ק״ש, מג״א | `getSofZmanShmaMGA` | 3 heures saisonnières, journée de 72 min avant le lever à 72 min après le coucher |
| סוף זמן ק״ש, גר״א | `getSofZmanShmaGRA` | 3 heures saisonnières du lever au coucher |
| סוף זמן תפילה, מג״א / גר״א | `getSofZmanTfilaMGA` / `getSofZmanTfilaGRA` | 4 heures saisonnières, mêmes journées |
| חצות היום | `getChatzos` | en 2.5.0, le passage du soleil au méridien (`getSunTransit`) |
| מנחה גדולה / קטנה, פלג המנחה | `getMinchaGedola` / `getMinchaKetana` / `getPlagHamincha` | 6,5 / 9,5 / 10,75 heures saisonnières du lever au coucher |
| שקיעה | `getSeaLevelSunset` | coucher au niveau de la mer |
| צאת הכוכבים | `getTzaisGeonim8Point5Degrees` | soleil à 8,5° sous l'horizon, « selon les Gueonim » |
| צאת הכוכבים, ר״ת | `getTzais72` | 72 minutes fixes après le coucher, « selon Rabbénou Tam » |
| חצות הלילה | `getSolarMidnight` | le milieu entre chatsot d'aujourd'hui et de demain |

**Chabbat et fêtes** (`JourAffiche.horaires`) :

- la veille d'un jour chômé, l'allumage : `getCandleLighting`, coucher moins
  18 minutes par défaut, réglable à 18, 20, 22, 30 ou 40 ;
- un jour chômé suivi d'un autre (deuxième jour de fête, Chabbat puis fête) :
  allumage pas avant tsét à 8,5° ;
- le dernier jour chômé, la sortie : tsét à 8,5°. Rabbénou Tam reste dans la
  liste.

Un jour est « chômé » selon `JewishCalendar.isAssurBemelacha`.

## Les ajouts de prière

`CalendrierJuif.kt` et `web/src/js/calendrier-juif.js` calculent pour chaque
jour un jeu de conditions. Chacune renvoie à une méthode de KosherJava, et
aucune règle n'est écrite à la main :

- Ya'alé véyavo, Al hanissim, Machiv haroua'h, Morid hatal, Tal oumatar,
  Hallel complet ou partiel, Ta'hanoun du matin et de Min'ha : `TefilaRules` ;
- jeûne (Anénou), Tich'a bèAv, dix jours de pénitence, omer, fêtes :
  `JewishCalendar` (`isTaanis`, hors Kippour : les quatre jeûnes publics,
  Tich'a bèAv compris, et le jeûne d'Esther) ;
- Pourim à Jérusalem le 15 Adar : `JewishCalendar.setIsMukafChoma`, quand la
  ville choisie est Jérusalem (GeoNames 281184). Les villes dont le statut
  est douteux gardent le 14, comme le calendrier commun. Le 14 s'y affiche
  alors « Pourim (hors Jérusalem) », le 15 « Pourim à Jérusalem ».

### Différences de noussa'h

Demande du 10/10 (« attention aux différences »). Ce que le noussa'h choisi
change, sur la carte du jour et dans le siddour :

| Point | Edot hamizra'h | Sfard | Ashkenaz | Sources |
| --- | --- | --- | --- | --- |
| Bénédiction du Hallel partiel (Roch 'Hodech, fin de Pessa'h) | sans bénédiction, même en communauté | avec | avec | OC 422:2 : « שאף הצבור אין מברך עליו … וכן נוהגי' בכל מלכות ארץ ישראל » ; Rema : « דגם יחיד מברך עליו … וכן נוהגין במדינות אלו » |
| Morid hatal l'été | dit | dit | **hors d'Israël, pas dit** (dit en Israël) | Rema, OC 114:3 : « ואנו בני אשכנז לא מזכירין טל » ; en Israël : [yeshiva.co, question 6293](https://www.yeshiva.co/ask/6293) |
| Anénou de l'officiant (répétition) | Cha'harit et Min'ha | idem | idem | OC 565:3 : « שליח ציבור אומרו גם בתפלת שחרית » |
| Anénou du particulier (Chomea téfila) | Cha'harit et Min'ha, jeûne d'Esther compris ; à Tich'a bèAv, Arvit aussi | Min'ha seulement | Min'ha seulement | OC 565:3 : « ובארבע צומות גם היחיד אומרו בכל תפלותיו » ; Rema : « ונהגו בכל הצומות שלא לאומרו כי אם במנחה » ; Tich'a bèAv : Peninei Halakha ([yeshiva.co 48403](https://www.yeshiva.co/midrash/48403)) |
| Ta'hanoun du 22 Tichri à la fin du mois | pas dit (du 11 Tichri au 2 'Hechvan) | dit après Isrou 'hag | dit après Isrou 'hag | coutume séfarade ; ailleurs, réglage par défaut de `TefilaRules` (`isTachanunRecitedEndOfTishrei`) |
| Ta'hanoun la veille de Roch Hachana, le matin | pas dit | pas dit | pas dit | OC 581:3 : « אין נופלים על פניהם בערב ראש השנה בתפלה » (`TefilaRules` 2.5.0 le fait dire : corrigé dans `CalendrierJuif`) |

Limites déclarées :

- les Séfarades marocains (et, selon une source, turcs) disent la
  bénédiction du Hallel partiel ; les sources ne s'accordent pas sur la
  liste exacte ([Kol Torah](https://www.koltorah.org/halachah/chatzi-hallel-by-rabbi-chaim-jachter),
  [JewishLink](https://jewishlink.news/a-bracha-on-chatzi-hallel-sephardic-and-ashkenazic-practices/)).
  L'application suit la règle du Choul'han Aroukh pour Edot ;
- Morid hatal ashkénaze : c'est une coutume répandue, pas une règle
  unanime ; les sources ne sont que secondaires ;
- les blocs d'Anénou sont reconnus à leur texte et à la consigne du
  siddour (`tools/siddur_annotations.py`) : l'officiant est le bloc placé
  entre « גואל ישראל » et « רפאנו », le particulier celui de « שומע תפלה ».
  Le siddour Edot porte lui-même un Anénou d'Arvit « בתשעה באב », annoté
  pour ce seul jour.

Le texte du Choul'han Aroukh (OC 114:3, 422:2, 565:3, 581:3, 697:1) a été
relu dans l'édition de Lemberg (1893). Les coutumes actuelles des Edot
(Anénou du particulier à Cha'harit et Min'ha, jeûne d'Esther compris ; pas
de Ta'hanoun jusqu'au 2 'Hechvan ; Hallel partiel sans bénédiction) ont été
vérifiées auprès d'abrégés de halakha séfarade contemporains.

**Les coutumes de Ta'hanoun** sont les réglages par défaut de `TefilaRules`,
avec deux corrections (tableau ci-dessus) : la veille de Roch Hachana pour
tous, la fin de Tichri pour les Edot. Les jours sans Ta'hanoun de la liste
séfarade concordent avec ces réglages : tout Nissan, Pessa'h chéni,
Lag baOmer, de Roch 'Hodech Sivan au 12 inclus, Tich'a bèAv, Tou bèAv, veilles
de Roch Hachana et de Kippour, 'Hanouka, Tou biChvat, 14 et 15 des deux Adar,
Roch 'Hodech ; à Min'ha, la veille d'un jour sans Ta'hanoun, sauf veilles de
Roch Hachana, de Kippour et de Pessa'h chéni. Restent des coutumes qui
divergent entre communautés, Sfard et Ashkenaz surtout (fin de Tichri, 13
Sivan hors d'Israël).

**Le siddour, « Pour cette heure »** (`JourAffiche.momentDuSiddour`) : avec
un lieu, la prière proposée suit ses zmanim. Tikoun 'hatsot de 'hatsot de la
nuit à l'aube ; Birkot hacha'har de l'aube à michéyakir ; Cha'harit jusqu'à
'hatsot ; Min'ha de min'ha guedola à la chki'a ; Arvit dès tsét, puis le
Chema du coucher après 22 h 30 (repère d'usage, pas un zman). Rien n'est
proposé de 'hatsot à min'ha guedola ni de la chki'a à tsét. Sans lieu, ou
sans zmanim (soleil de minuit), les heures fixes d'avant.

**Réglages** : en noussa'h Sfard, Ta'hanoun de fin de Tichri au choix (dit
après Isrou 'hag par défaut) ; à Jérusalem, l'allumage 40 minutes avant la
chki'a est proposé, sans être imposé.

**Le soir** : Arvit et le Chema du coucher relèvent du jour hébreu suivant à
partir de midi (`jourDeLaPriere`). Midi plutôt que la chki'a, qui demanderait
un lieu ; aucun Arvit ne se dit le matin. Les bénédictions du repas suivent
le jour civil : après la chki'a, la règle dépend de l'heure du début du
repas, et les ajouts restent ouvrables.

### Dans le siddour

`tools/siddur_annotations.py` désigne, dans les trois siddourim, les blocs
qui dépendent du jour. Il les reconnaît à la consigne du siddour lui-même
(« בקיץ », « בחנוכה ופורים אומרים », « בעשי״ת »…). Le résultat va dans
`res/raw/siddur_annotations.json`, où chaque bloc est désigné par son
empreinte : si un siddour reconstruit bouge, les tests échouent. Les textes
eux-mêmes ne sont jamais modifiés.

- **Ajout du jour** : mis en évidence, avec son nom.
- **Ajout hors du jour** : replié en une ligne ; une flèche l'ouvre et le
  referme. Rien n'est retiré.
- **Variante enchâssée dans un bloc** (« בעשרת ימי תשובה אומרים: המלך
  הקדוש ») : un repère « Aujourd'hui : … » sous le bloc.

Volontairement non annotés, donc toujours dépliés : Avinou malkénou dans les
noussa'him Sfard et Ashkenaz (coutumes diverses), le Ta'hanoun du siddour
ashkénaze (mêlé au Vidouï sans frontière nette), le Chir chel yom. Dans le
siddour Edot, Avinou malkénou suit la consigne du texte (« בעשרת ימי תשובה
אומרים »).

## Les lieux

- **Une ville** : 51 villes de GeoNames (CC-BY 4.0), tirées du fichier
  `cities15000.json` du paquet `geonamescache` 3.0.2
  (`tools/villes_construction.py`). Chacune est désignée par son identifiant
  GeoNames ; ses coordonnées et son fuseau sont repris tels quels. Le nom
  hébreu est vérifié contre les noms alternatifs de GeoNames, sauf Lakewood
  (New Jersey), déclaré dans l'outil. Choisir une ville règle aussi Israël ou
  diaspora.
- **La position du téléphone** : sur Android, l'autorisation de localisation
  approximative, demandée seulement à ce moment-là ; sur le web,
  `navigator.geolocation`. Depuis le 16/10, la région suit la position
  (contour du territoire, `TerritoireIsrael`) ; le fuseau reste celui du téléphone.

Le lieu reste sur l'appareil. Choisir une ville revient sur la décision du
21/09 (« sans ville ni coucher du soleil »), à la demande du propriétaire.

## Les tests

**NOAA** (`ZmanimTest`, `web/test/calendrier.test.mjs`). Le moteur rapporte
lui-même son algorithme (« US National Oceanic and Atmospheric
Administration Algorithm ») ; les deux tests l'exigent. Les heures sont
comparées aux formules publiées par la NOAA
([calcdetails](https://gml.noaa.gov/grad/solcalc/calcdetails.html)),
recalculées sans KosherJava par `tools/zmanim_reference_noaa.py` (traduction
Python du calculateur de la NOAA, paquet `hamsci_psws` 0.1.3, servant
seulement à produire les valeurs) : 2 s de tolérance pour le lever, le
coucher et les angles. `'hatsot` est toléré à 20 s : KosherJava évalue
l'équation du temps une demi-journée plus tôt que le tableur, comme l'ancien
calculateur JavaScript de la NOAA, d'où un écart mesuré jusqu'à 15 s.

**Astronomie** (`ZmanimTest`). Le lever, le coucher, les angles et le passage
au méridien sont comparés à PyEphem 4.2.1 (libastro, VSOP87), qui ne partage
rien avec l'algorithme NOAA. Quatre villes, cinq dates, dont les deux
changements d'heure et un solstice. Tolérance de 60 s ; écart mesuré :
moins de 15 s. Script : `tools/zmanim_reference_pyephem.py`. Au-delà du
cercle polaire (Tromsø), les événements qui n'ont pas lieu sont « absents »,
jamais « en erreur ».

**Calendrier** (`CalendrierJuifTest`). Dates de 5787 relevées sur Hebcal et
ICU (`Intl`), qui concordent ; cas de Roch 'Hodech, 'Hanouka, des deux dates
de Tal oumatar (Israël, diaspora, année avant une bissextile civile), des
jeûnes, du lundi et du jeudi, de l'omer, de Pourim.

**Grille du mois hébreu** (`JourAfficheTest`, `web/test/calendrier.test.mjs`).
L'écran Calendrier s'ouvre sur le mois hébreu (demande du 10/10/2026) ; le
mois civil est un choix mémorisé, et chaque case porte toujours les deux
quantièmes, le principal en haut. Le 1er et la longueur de chaque mois hébreu
de 2026 à 2028 concordent avec Hebcal, jour par jour ; Tichri 5787 et la
suite Chevat, Adar I, Adar II, Nissan de 5787 sont relevés aussi dans ICU.

**Fidélité du web à Android** (`web/test/calendrier.test.mjs`). Chaque jour
du 01/09/2025 au 31/12/2030, en Israël et en diaspora, le web rend
exactement la réponse de KosherJava : date, fêtes, omer, paracha,
conditions. Vecteurs dans `web/test/vecteurs_kosherjava.json`, produits par
`tools/vecteurs_kosherjava/`. Les zmanim concordent à une seconde près.

**Hebcal, second avis** (`web/test/calendrier.test.mjs`) :

- dates hébraïques, Roch 'Hodech et jeûnes publics : accord total sur
  2025-2030 ;
- zmanim de même méthode : accord à 5 s près ;
- `'hatsot` n'est **pas** la même méthode (Hebcal : milieu entre lever et
  coucher ; KosherJava : passage au méridien). L'écart, quelques dizaines de
  secondes, est seulement borné. Aucune moyenne n'est jamais faite ;
- Ta'hanoun : environ 75 jours d'écart sur 2025-2030, tous des jours que
  Hebcal marque lui-même « selon les communautés » ;
- **un vrai désaccord : le 15 Adar I** (Chouchan Pourim katan).
  KosherJava ne dit pas Ta'hanoun, Hebcal le fait dire partout. Le
  Choul'han Aroukh l'exclut
  (OC 697:1, « יום י״ד וט״ו שבאדר ראשון אין נופלין על פניהם »). Le
  résultat de KosherJava, moteur retenu, est gardé.

L'accord de deux moteurs ne prouve pas l'exactitude : ils partagent
l'algorithme solaire de la NOAA. C'est pourquoi l'astronomie est vérifiée
contre PyEphem, et les règles de prière restent à valider par un Rav.

## Maintenance

- **Changer de version de KosherJava** : relancer les tests Android, puis
  régénérer les vecteurs (`tools/vecteurs_kosherjava/README.md`) et relancer
  les tests web.
- **Changer de version de kosher-zmanim** : `tools/vendor_kosher_zmanim.sh`,
  puis les tests web ; tout écart avec les vecteurs fait échouer le test.
- **Reconstruire un siddour** : relancer `tools/siddur_annotations.py`. Si
  une empreinte ne correspond plus, `SiddurJourTest` et le test web
  l'indiquent.
- **Ajouter une ville** : son identifiant GeoNames dans
  `tools/villes_construction.py`, puis relancer l'outil.
