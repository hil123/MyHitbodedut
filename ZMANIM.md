# Calendrier hébraïque, zmanim et ajouts de prière

Demande du 10/10/2026, sur Android et sur le web. Ce fichier dit ce que
l'application calcule, avec quoi, selon quelle méthode, et où elle s'arrête.
Rien ici n'est une décision halakhique : chaque méthode est nommée à l'écran
sous son horaire. *29/10 : le propriétaire indique que les zmanim ont été
vérifiés ; la mention « profil à valider par un Rav » est retirée de l'écran
(audit 1.390).*

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

## En vol (phase 1, hors ligne)

`Vol.kt` (Android) et `vol.js` (web), ligne pour ligne, ajoutés le 28/10.
Rien ne quitte le téléphone, sauf si l'on active le suivi en direct (plus
bas). La localisation précise n'est demandée qu'au toucher de « position
réelle (GPS) ».

**Le calcul.** On saisit les deux aéroports et les heures du billet, à
l'heure locale de chaque aéroport. L'avion est placé sur l'arc de grand
cercle entre les deux, à vitesse constante entre décollage et atterrissage ;
avant le décollage il est à l'aéroport de départ, après l'atterrissage à
celui d'arrivée. Un zman a lieu en vol à l'instant t où le zman du point
sous l'avion vaut t : t = zman(position(t)). Le zman du point est celui du
moteur, sans changement (`Zmanim.jour`, même profil, au niveau de la mer) ;
on échantillonne toutes les 5 minutes en vol, toutes les heures au sol, et
chaque racine est affinée (fausse position) à la demi-seconde. Tout est en
UTC. Le fuseau « nautique » du point (UTC + longitude / 15, arrondi) sert
seulement à dire quel jour civil demander au moteur ; on demande toujours la
veille, le jour et le lendemain. La prière du moment est
`ZmanimWidget.prochaine`, sans règle nouvelle, nourrie des zmanim de la
route (`VolPriere`).

**Ce que l'écran dit, sans conseil** : la fiabilité (« route prévue », jamais
une position réelle) ; de combien chaque horaire bouge si le départ a
30 minutes de retard ; la haute latitude (au-delà de 60°) ; un zman absent
sur une partie de la route ; un zman qui repasse (l'avion plus rapide que le
soleil vers l'ouest) ; le passage du méridien de 180°, avec la date locale de
part et d'autre. La vraie ligne de changement de date s'écarte de ce
méridien (Kiribati, Samoa…) : seul le méridien est calculé.

**Ce qui est vérifié, estimé, supposé.**

- Vérifié par les tests (`VolTest`, `vol.test.mjs`) : chaque zman trouvé en
  vol est le zman du moteur au point où est l'avion à cet instant, à 2 s
  près ; au sol, avant le décollage, le calcul rend exactement les zmanim de
  l'aéroport ; Android et web s'accordent à 2 s sur vingt-six vols, jalons
  de Chabbat, des fêtes et du Omer compris (`tools/vecteurs_vol`). Ce sont des vérifications de calcul, pas de
  halakha.
- Estimé : la position. Une route réelle s'écarte du grand cercle de
  plusieurs centaines de kilomètres (vents, couloirs de l'Atlantique Nord),
  le roulage et les retards déplacent tout. Vers l'ouest, quand l'avion suit
  le soleil, un horaire peut bouger de beaucoup plus que le retard lui-même
  (exemple calculé, Tel Aviv–New York : chki'a avancée de 86 min pour 30 min
  de retard). L'écart affiché le montre ; il ne le corrige pas.
- Supposé, et **à valider par un Rav** (aucune règle n'est tranchée ici) :
  1. le zman du sol sous l'avion, au niveau de la mer, plutôt que l'horizon
     vu de l'altitude. Les sources secondaires lues pendant l'audit (28/10)
     présentent le sol sous l'avion comme l'avis majoritaire ; les sources
     premières n'ont pas été vérifiées. À 11 000 m, l'horizon s'abaisse
     d'environ 3,4° (géométrie seule, sans réfraction) : le soleil s'y lève
     plus tôt et s'y couche plus tard, de l'ordre d'un quart d'heure ou plus
     aux latitudes moyennes. Ce lever vu de l'avion n'est pas calculé ;
  2. les heures saisonnières (fin du Chema, de la Téfila, Min'ha…) prennent
     la journée du point sous l'avion à cet instant, pas la journée vécue à
     bord, qui peut être bien plus longue ou plus courte. Laquelle compte
     n'est pas tranché ;
  3. la date (jour de fête, jeûne, Chabbat) quand on franchit la ligne de
     changement de date : l'écran donne seulement le fait ;
  4. au-delà de 60° de latitude, les méthodes en degrés et en minutes
     divergent fortement ; certaines n'ont pas lieu.

**Le GPS** (au choix, en plus de la route prévue). Au toucher de « position
réelle », l'application demande la localisation précise et un relevé du GPS
seul, 90 s au plus ; le GPS marche en mode avion, sans réseau. Un relevé pris
entre le décollage et l'atterrissage prévus recale la route : grand cercle du
départ au relevé, puis du relevé à l'arrivée, chaque tronçon à vitesse
constante. L'écart affiché devient alors celui d'une arrivée retardée de
30 min. Le relevé reste sur l'appareil ; un autre vol l'efface.

**Le suivi en direct par Internet** (édition complète et web, désactivé par
défaut ; `SuiviVol.kt`, `positionEnDirect` dans `vol.js`, `tools/relais_vol`).
L'utilisateur l'active et donne l'indicatif radio OACI du vol (« ELY1 »).
Sur Android, l'accès anonyme et gratuit d'OpenSky Network (sans compte ni clé,
400 crédits par jour et par adresse IP, positions à 10 s près ; documentation
de l'API REST, lue le 28/10) : le téléphone demande les avions d'un carré de
±5° autour de la position estimée (2 crédits) et y cherche l'indicatif
lui-même, qui ne part pas. Essai réel le 28/10 : un avion en vol retrouvé
depuis une position estimée décalée de 1,3°. Le web ne peut pas appeler
OpenSky (le navigateur le refuse : OpenSky n'autorise que son propre site) ;
il passe par le relais (Cloudflare Workers), qui reçoit l'indicatif et la
position estimée arrondie au dixième de degré, et interroge OpenSky en
anonyme, ou avec un client API s'il en a un. Une position de plus de 10 minutes est écartée. La position reçue
devient un relevé, comme celui du GPS, et la fiabilité le dit. OpenSky ne voit
les avions que près de ses antennes au sol : au-dessus des océans, souvent
rien. Conditions d'OpenSky (courriel du 28/10, ticket 837455) : application
gratuite sans publicité ni fonction payante = usage non commercial, compte
gratuit ; données demandées par le relais avec le client OAuth2 du compte, pas
par chaque téléphone ; une réponse partagée en cache temporaire par zone (relais :
grille de 0,5°, 15 s) ; ni revente, ni redistribution, ni API ; attribution
« Données aéronautiques : OpenSky Network (opensky-network.org) » dans
l'application et sur la page du magasin, avec un avertissement « ni navigation
ni sécurité » (affichés sous le suivi en direct). Détail :
`tools/relais_vol/README.md`. L'accès direct depuis le téléphone (Android sans
relais) ne suit pas la deuxième condition : pour une diffusion publique,
construire avec le relais (`SuiviVol.FournisseurPosition` est interchangeable). Sans adresse de relais à
la construction (`RELAIS_VOL=`), l'option est inactive sur le web et sa CSP
n'autorise que le site.

**Les rappels pendant le vol** (Android, `VolRappels.kt`, `VolAlerte.kt` ;
actifs par défaut, un interrupteur sur l'écran du vol). Des notifications,
même en mode avion, aux heures calculées sur la route :

- **Tefilat haDerekh**, cinq minutes après le décollage prévu. Le délai de cinq
  minutes est la demande de l'utilisateur, pas une règle. Ce que disent les
  sources consultées (recoupées, non lues dans les responsa) : si l'aéroport
  est hors de la ville, on la dit en route vers l'aéroport et elle couvre le
  vol ; sinon on la dit dans l'avion, au mieux après le décollage, plusieurs
  Rabbanim la disant déjà sur la piste ; oubliée, on peut la dire pendant le
  vol jusqu'à la descente, avis rapporté dans les sources consultées sans
  être unanime (shulchanaruchharav.com, « When traveling on an airplane, when
  is Tefilas Haderech to be recited? » ; OU Torah, « Airplane Travel »).
  L'application ne tranche pas entre ces cas.
- **Le début de chaque prière dans l'avion**, aux bornes de l'application
  (HorairePriere, ZmanimWidget) : Min'ha à min'ha guedola, Arvit à la sortie
  des étoiles. Cha'harit n'avait pas de début dans l'application : le rappel
  est au lever du soleil (netz), temps de la prière au départ (Choul'han
  Aroukh O.H. 89, 1). Au décollage, la notification de Tefilat haDerekh dit
  la prière ouverte, s'il y en a une.
- **30 minutes avant la limite** de Cha'harit (fin du temps de la tefila,
  GRA) et de Min'ha (chki'a), si elle tombe dans l'avion ; pas pour Arvit.
- **Le compte du Omer**, à la sortie des étoiles sous l'avion (les jalons de
  `VolCalendrier`).

Les rappels suivent les heures du billet : un décollage en retard ne les
décale pas, et Android peut les retarder de quelques minutes (alarme
inexacte, comme les autres rappels de l'application). Le web n'a pas ces
notifications.

**Chabbat, fêtes et Omer** (`VolCalendrier.kt`, `jalons` dans `vol.js`) : la
règle de `JourAffiche.horaires` (isAssurBemelacha de KosherJava), appliquée aux
chki'a et aux tsét de la route. Chki'a la veille d'un jour chômé : entrée, et
l'allumage du réglage (18 min par défaut) avant cette chki'a ; tsét d'un jour
chômé suivi d'un jour ouvré : sortie ; suivi d'un autre jour chômé : le jour
suivant commence ; tsét pendant le Omer : le jour compté ce soir. Le jour civil
est celui du point sous l'avion (fuseau nautique), le calendrier d'Israël ou
de diaspora celui du réglage de l'application. Ce sont deux choix de calcul,
**à valider par un Rav**, comme la question de la date après la ligne de
changement de date ; l'écran ne donne aucun conseil (allumer à bord, compter
avant ou après une escale…).

**Vérifications des fuseaux et de l'heure d'été** (28/10, `VolTest`,
`vol.test.mjs`) : Melbourne, Sydney +11 en décembre et +10 en juillet ;
Brisbane +10, Perth +8, Adélaïde +10,5 / +9,5, Darwin +9,5 ; Jakarta +7,
Bali +8, Jayapura +9 ; Los Angeles −8 / −7, New York et Montréal −5 / −4,
Phoenix −7 et Honolulu −10 toute l'année ; Buenos Aires −3 toute l'année ;
Queenstown +13 / +12 ; Tel Aviv +2 / +3. Règles recoupées : heure d'été en
Nouvelle-Galles du Sud, Victoria, Tasmanie, Australie-Méridionale et ACT, pas
au Queensland, en Australie-Occidentale ni dans le Territoire du Nord ; trois
fuseaux indonésiens par province ; pas d'heure d'été en Argentine (un projet
UTC−4 voté par les députés en 2025 n'a pas été trouvé adopté par le Sénat) ;
ni à Hawaï ni en Arizona hors Nation navajo. Les 729 aéroports d'Australie,
d'Indonésie, des États-Unis et d'Argentine ont été comparés à ces règles,
région par région, en janvier et en juillet : aucun écart. L'ouest de
l'Alaska (Nome, Pribilof, Unalaska) est à l'heure de l'Alaska malgré sa
longitude, comme le dit la tzdb. Trajets vérifiés : Melbourne – Los Angeles –
Montréal et retour (été et hiver austral, ligne de date dans les deux sens),
Sydney – Queenstown, Tel Aviv – Buenos Aires (direct hypothétique, aucun
vol régulier connu).

**La route d'un numéro de vol** (28/10 ; édition complète et web ; `RouteVol.kt`,
`chercherRoute` dans `vol.js`). Au toucher de « Trouver l'arrivée par le numéro »,
le numéro seul (« LY81 » ou « ELY81 ») part vers api.adsbdb.com, gratuit, sans
compte ni clé. La réponse donne la compagnie, l'indicatif OACI (repris pour le
suivi en direct) et les escales connues ; le tronçon retenu est celui qui part
de l'aéroport de départ choisi, sinon le premier. adsbdb refuse certains
numéros IATA courts (« QF9 ») : on reprend avec l'indicatif OACI de chaque
compagnie qui porte ce code. **Pas les heures** : aucune source gratuite et sans
clé n'a été trouvée (28/10 : Aviationstack, Aviation Edge, Cirium, OAG
demandent tous une clé) ; elles restent à saisir, celles du billet. Les routes
d'adsbdb peuvent être anciennes (28/10 : QF9 y passe par Singapour), et l'écran
le dit. Données de route de David J. Taylor et Jim Mason, qui n'en permettent ni
la copie ni l'intégration à une autre base sans accord : l'application les lit
à la demande et n'en garde rien que les deux aéroports du vol saisi ; une
publication commerciale serait à revoir avec eux, comme OpenSky.

**Les noms en hébreu et en français** (`res/raw/aeroports_noms.json`,
`tools/aeroports_noms.py`, 28/10) : Wikidata (CC0), libellé de l'élément au code
IATA et des lieux desservis, lus à la construction ; l'élément n'est gardé que
si ses coordonnées sont à moins de 30 km de celles d'OurAirports (24 écartés).
3 144 aéroports nommés, 2 462 en hébreu. La recherche ignore les points-voyelles
et la ponctuation (« תל-אביב » = « תל אביב ») ; les lieux desservis servent à la
recherche seulement (Wikidata fait desservir Jérusalem par TLV).

**Les aéroports** (`res/raw/aeroports.json`, `tools/aeroports_construction.py`) :
OurAirports (domaine public, « sans garantie d'exactitude »), aéroports
moyens et grands à vols réguliers ayant un code IATA. Le fuseau de chacun
vient des contours de timezone-boundary-builder 2026e (ODbL : attribution
affichée), par point dans polygone, à la construction seulement ; il est
contre-vérifié par la localité GeoNames la plus proche. Chaque écart a été
relu (liste dans l'outil) ; Gold Coast prend l'heure du Queensland, celle
de son aérogare. Les aéroports dont le fuseau est disputé par les sources
elles-mêmes (Xinjiang, Abkhazie, Govi-Altaï) ne sont pas proposés.

## La direction de Jérusalem (boussole, 28/10)

`DirectionJerusalem.kt`, écran `BoussoleActivity` (design C, « le hassid se
tourne »), widget 2 × 2 `BoussoleWidget`. Vers Har haBayit, 31,7781° N,
35,2354° E, depuis le lieu des zmanim.

- **Le hassid suit la direction réelle** (décision du 28/10) : le plus court
  chemin sur le globe (grand cercle, cap initial). La direction sur la carte
  (loxodromie, cap constant) reste affichée pour information, en pointillé
  bleu. Selon Chabad.org (« Why Do We Face East When Praying? Or Do We? »), le
  Choul'han Aroukh haRav et beaucoup d'autres tiennent le grand cercle pour
  préférable ; la page donne, de New York, 54° et environ 95°.
- **Deux affichages au choix** (réglage « Affichage de la boussole ») : le
  hassid qui se tourne (design C, par défaut) ou une rose des vents qui tourne
  avec le téléphone : la rose graduée venue de `main` à la fusion du 28/10
  (graduations aux 5°, degrés aux 30°, zone d'or de ±4°, aiguille d'or vers
  Jérusalem, anneau d'or face à elle), avec le pointillé bleu de la carte.
- **Position exacte.** Le bouton relance la localisation précise
  (`ChoixLieu.positionExacte`, web `localiserPosition`) ; à l'ouverture, la
  position du téléphone se reprend sans bruit si elle est déjà permise.
  La boussole parallèle de `main` (`DirectionTefila`, même calcul) est
  remplacée par celle-ci ; `web/src/js/direction.js` reste pour `distanceKm`.
- **Nord vrai.** Le capteur d'orientation donne le nord magnétique ; la
  déclinaison du lieu (`GeomagneticField`, modèle intégré à Android) est
  ajoutée (Android) ; sur le web, le modèle WMM2025 de la NOAA
  (`web/src/js/magnetisme.js`). Sans capteur, la rose orientée au nord.
- **Widget.** Il ne lit pas le capteur : la direction par rapport au nord, en
  haut ; redessiné quand le lieu change.
- **Tests.** `DirectionJerusalemTest` : six villes contre geographiclib
  (WGS84) à 0,5° près pour le grand cercle (calcul sur sphère), la loxodromie
  contre un calcul séparé, le méridien de 180°, l'écart, le filtre du cap.
  Non vérifié : le comportement du capteur sur un vrai téléphone (rendu
  Robolectric seulement).

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
- **Changer le calcul en vol** : `VolTest` échoue tant que
  `web/test/vecteurs_vol.json` n'est pas régénéré (`tools/vecteurs_vol/README.md`).
- **Mettre à jour les noms des aéroports** : `python3 -I tools/aeroports_noms.py`,
  après tout changement d'`aeroports.json`.
- **Mettre à jour les aéroports** : relancer `tools/aeroports_construction.py`
  sur un `airports.csv` récent ; tout nouvel écart de fuseau l'arrête jusqu'à
  ce qu'il soit relu et ajouté à l'une de ses listes.
- **Mettre le relais en ligne ou changer de fournisseur** : `tools/relais_vol/README.md` ;
  `web/test/relais.test.mjs` le teste contre un faux OpenSky.
