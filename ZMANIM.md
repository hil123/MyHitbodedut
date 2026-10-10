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
faux), dans le fuseau IANA du lieu. Les descriptions entre guillemets sont
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
  `JewishCalendar`.

**Les coutumes de Ta'hanoun** sont les réglages par défaut de `TefilaRules`,
les mêmes pour les trois noussa'him. Par exemple : Ta'hanoun le vendredi et
le dimanche ; pas de Ta'hanoun du 9 au 21 Tichri ; pas de Ta'hanoun à Pessa'h
chéni.
Ce sont précisément les points où les communautés divergent.

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
  `navigator.geolocation`. La région suit alors le fuseau du téléphone.

Le lieu reste sur l'appareil. Choisir une ville revient sur la décision du
21/09 (« sans ville ni coucher du soleil »), à la demande du propriétaire.

## Les tests

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
  Choul'han Aroukh, OC 697:1, l'exclut les 14 et 15 Adar I, d'après deux
  sources secondaires concordantes ([Etzion](https://etzion.org.il/en/holidays/purim/adar-alef-and-holiday-purim)
  cite le Me'haber ; le Michna Beroura est résumé sur [OU Torah](https://outorah.org/p/229903)).
  Le résultat de
  KosherJava, moteur retenu, est gardé.

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
