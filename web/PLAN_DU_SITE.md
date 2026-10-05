# MyHitbodedut pour iPhone : plan du site

État au 28/09/2026, commit `5fcc114` de la branche `claude/version-web`.

Les libellés entre guillemets sont ceux que l'application affiche. Ils sont lus dans les fichiers `strings.xml` d'Android (fr, en, he) et n'ont pas été retraduits. Le texte qui les relie est une rédaction, pas une traduction vérifiée : en hébreu, il reste à faire relire.

- ✅ : construit et testé (tests Node, WebKit, Chromium, Safari sur Mac).
- ⏳ : visible mais inactif (grisé), prévu à l'étape indiquée.

---

## Version française

### Au premier lancement
- ⏳ Présentation : 7 écrans, avec le choix Tsadik / Tsadeket (étape 2).

### Onglet « Aujourd’hui » (accueil) ✅
- « MyHitbodedut », puis « Bonjour [prénom] » si un prénom est enregistré.
- État du jour : « Hitbodedout faite aujourd'hui » ou « Hitbodedout pas encore faite ».
- Série : « … jours d'affilée » ou « Série à relancer ». Total : « … au total ».
- Jauge de la semaine, avec le hassid en son centre : « … jours sur … cette semaine ».
- Bouton « Commencer une séance de … » → **Séance**.
- Pastilles :
  - « Durée » : 5, 10, 15, 30 min, 1 h, 1 h 30, ou « Durée réglable… ».
  - « Sujet » : « Sur quoi voulez-vous parler ? », 10 sujets, ou « Aucun ».
  - « Musique » → **Musique**.
- Semaine du dimanche au samedi : ✓ séance faite, ש Chabbat, — fête ou pause.
- « Écrire une note » (après une séance) → **Note de la séance**.

### Séance ✅
- Avant de commencer :
  - le hassid ;
  - les six durées, « Durée réglable… » et « Sans durée » ;
  - le sujet ;
  - le bouton « Commencer, … ».
- Pendant la séance :
  - le décompte, ou le temps écoulé pour une séance « Sans durée » ;
  - le niggoun choisi ;
  - la carte de ’hizouk « Pour commencer » ;
  - « Je n'arrive pas à parler », qui affiche un texte « Quand les mots ne viennent pas » ;
  - « Terminer maintenant » et « Abandonner », chacun avec une confirmation.
- À la fin : la carte « Pour finir », puis → **Bravo**.

### Bravo ✅
- « Bravo, Tsadik ! », « Bravo, Tsadeket ! » ou « Bravo ! », selon le choix fait dans la présentation.
- La danse du hassid, les notes qui montent, « … minutes de hitbodedout ».
- « Continuer » → **Note de la séance**.
- Après un abandon : « ’Hazak ! », « ’Hizki ! » ou « Courage ! », avec une carte « Reprendre la route ».

### Note de la séance ✅
- Texte libre, « Plus tard » ou « Enregistrer ».

### Onglet « Textes »
- ⏳ « Tikoun HaKlali », « Tfilot », « Bibliothèque », « Conseils pour la hitbodedout » (étape 4).
- « Autour de la séance » : « Musique » ✅, « Réglages » ✅.

### Musique ✅
- « Jouer pendant la séance » (interrupteur), « Volume ».
- « Bibliothèque » : 33 niggounim, avec « Écouter » et le choix du morceau.

### Onglet « Journal »
- ⏳ Liste des jours et des notes (étape 4).

### Onglet « Réglages »
- ✅ « Langue » : עברית, English, Français.
- ⏳ Apparence (clair ou sombre, 5 fonds, 6 accents), prénom, « Shabbat et Yom Tov », export et import de la sauvegarde (étape 2).

---

## English version

### First launch
- ⏳ Onboarding: 7 screens, with the Tsadik / Tsadeket choice (step 2).

### "Today" tab (home) ✅
- "MyHitbodedut", then "Hello [first name]" if a first name is saved.
- Today's status: "Hitbodedut done today" or "Hitbodedut not done yet".
- Streak: "… days in a row" or "Streak to restart". Total: "… in total".
- Weekly gauge with the hassid in the middle: "… of … days this week".
- "Start a … session" button → **Session**.
- Chips:
  - "Length": 5, 10, 15, 30 min, 1 h, 1 h 30, or "Custom duration…".
  - "Subject": "What do you want to talk about?", 10 subjects, or "None".
  - "Music" → **Music**.
- Week from Sunday to Saturday: ✓ session done, ש Shabbat, — holiday or pause.
- "Write a note" (after a session) → **Session note**.

### Session ✅
- Before starting:
  - the hassid;
  - the six lengths, "Custom duration…" and "No set length";
  - the subject;
  - the "Start, …" button.
- During the session:
  - the countdown, or the elapsed time for a session with "No set length";
  - the chosen niggun;
  - the "To begin" hizuk card;
  - "I cannot find the words", which shows a "When the words do not come" text;
  - "Finish now" and "Abandon", each with a confirmation.
- At the end: the "To close" card, then → **Bravo**.

### Bravo ✅
- "Well done, Tsadik!", "Well done, Tsadeket!" or "Well done!", depending on the choice made in the onboarding.
- The hassid's dance, rising notes, "… minutes of hitbodedut".
- "Continue" → **Session note**.
- After abandoning: "Hazak!", "Hizki!" or "Take heart!", with a "Back on the road" card.

### Session note ✅
- Free text, then "Later" or "Save".

### "Texts" tab
- ⏳ "Tikkun HaKlali", "Tfilot", "Library", "Tips for Hitbodedut" (step 4).
- "Around the session": "Music" ✅, "Settings" ✅.

### Music ✅
- "Play during the session" (switch), "Volume".
- "Library": 33 niggunim, with "Listen" and track choice.

### "Journal" tab
- ⏳ List of days and notes (step 4).

### "Settings" tab
- ✅ "Language": עברית, English, Français.
- ⏳ Appearance (light or dark, 5 backgrounds, 6 accents), first name, "Shabbat and Yom Tov", backup export and import (step 2).

---

<div dir="rtl">

## גרסה עברית

### בפתיחה הראשונה
- ⏳ היכרות: 7 מסכים, עם הבחירה צדיק / צדקת (שלב 2).

### לשונית « היום » (מסך הבית) ✅
- « MyHitbodedut », ואחריו « שלום [שם] » אם נשמר שם.
- מצב היום: « התבודדת היום » או « עדיין לא התבודדת היום ».
- הרצף: « … ימים ברצף » או « הרצף מחכה להתחלה ». הסך: « … בסך הכול ».
- מד השבוע עם החסיד במרכזו: « … ימים מתוך … השבוע ».
- כפתור « להתחיל התבודדות של … » ← **התבודדות**.
- כפתורים קטנים:
  - « משך »: 5, 10, 15, 30 דק׳, 1 שע׳, 1 שע׳ 30 דק׳, או « משך לבחירה… ».
  - « נושא »: « על מה תרצו לדבר? », 10 נושאים או « ללא ».
  - « מוזיקה » ← **מוזיקה**.
- השבוע מיום א׳ עד שבת: ✓ התבודדות שבוצעה, ש שבת, — חג או הפסקה.
- « לכתוב הערה » (אחרי התבודדות) ← **« הערה על ההתבודדות »**.

### התבודדות ✅
- לפני ההתחלה:
  - החסיד;
  - שש אפשרויות משך, « משך לבחירה… » ו« בלי זמן קצוב »;
  - הנושא;
  - הכפתור « להתחיל, … ».
- בזמן ההתבודדות:
  - ספירה לאחור, או הזמן שעבר כשאין זמן קצוב;
  - הניגון שנבחר;
  - כרטיס חיזוק « לפתיחה »;
  - « אני לא מצליח לדבר », שמביא טקסט « כשהמילים לא באות »;
  - « לסיים עכשיו » ו« לוותר », כל אחד עם אישור.
- בסוף: כרטיס « לסיום », ואחריו ← **כל הכבוד**.

### כל הכבוד ✅
- « כל הכבוד, צדיק! », « כל הכבוד, צדקת! » או « כל הכבוד! », לפי הבחירה בהיכרות.
- ריקוד החסיד, תווים שעולים, « … דקות של התבודדות ».
- « להמשיך » ← **« הערה על ההתבודדות »**.
- אחרי ויתור: « חזק! », « חזקי! » או « לא מוותרים! », עם כרטיס « חוזרים לדרך ».

### « הערה על ההתבודדות » ✅
- טקסט חופשי, ואחר כך שמירה או דחייה.

### לשונית « טקסטים »
- ⏳ « תיקון הכללי », « תְּפִלּוֹת », « ספרייה », « עצות להתבודדות » (שלב 4).
- « סביב ההתבודדות »: « מוזיקה » ✅, « הגדרות » ✅.

### מוזיקה ✅
- « לנגן בזמן ההתבודדות » (מתג), « עוצמה ».
- « ספרייה »: 33 ניגונים, עם « האזנה » ובחירת ניגון.

### לשונית « יומן »
- ⏳ רשימת הימים וההערות (שלב 4).

### לשונית « הגדרות »
- ✅ « שפה »: עברית, English, Français.
- ⏳ מראה (בהיר או כהה, 5 רקעים, 6 צבעים), שם, « שבת ויום טוב », ייצוא וייבוא של הגיבוי (שלב 2).

</div>

---

## Notes

- En hébreu, l'application s'affiche de droite à gauche, et les cartes de ’hizouk ne portent que le texte hébreu, sans traduction (règle des langues d'Android).
- Hors de cette version : la barrière du matin et les rappels à heure fixe, impossibles sur le web sans serveur.
