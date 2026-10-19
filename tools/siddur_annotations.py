# -*- coding: utf-8 -*-
"""
Construit android/app/src/main/res/raw/siddur_annotations.json (10/10/2026) :
ce qui, dans les trois siddourim, dépend du jour. Les textes eux-mêmes ne
sont pas touchés ; ce fichier les désigne par (texte, section, bloc), avec le
début du bloc comme empreinte, pour qu'un siddour reconstruit qui aurait
bougé fasse échouer les tests au lieu de mettre en évidence le mauvais bloc.

Trois sortes d'annotations :
  - « bloc »    : un ajout entier. S'il s'applique ce jour-là, il est mis en
                  évidence ; sinon il est replié, et une flèche le rouvre.
  - « section » : une section entière (Ta'hanoun, omer, motsaé Chabbat…),
                  même traitement.
  - « indice »  : un bloc qui mêle le texte de tous les jours et une
                  variante (« בעשרת ימי תשובה אומרים: המלך הקדוש »). Rien
                  n'est replié : quand la variante s'applique, un repère
                  « Aujourd'hui : המלך הקדוש » s'affiche sous le bloc.

Aucune règle de calendrier n'est écrite ici. Chaque annotation renvoie à une
clé de condition (CalendrierJuif.C), calculée par KosherJava ; l'outil ne
fait que reconnaître, dans le texte, la consigne du siddour qui dit quand
l'ajout se dit (« בקיץ », « בחנוכה ופורים אומרים », « בעשי״ת »…). Une
condition précédée de « ! » s'applique quand la clé est fausse (« בימים שאין
בהם תחנון »). Ce qui ne se laisse pas reconnaître sûrement reste tel quel,
déplié comme avant : Avinou malkénou (coutumes diverses), le Ta'hanoun du
siddour ashkénaze (mêlé au Vidouï sans frontière nette), le Chir chel yom.

  python3 tools/siddur_annotations.py
"""
import json
import os
import re
import sys

RACINE = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
RAW = os.path.join(RACINE, "android", "app", "src", "main", "res", "raw")
TEXTES = ["chaharit", "minha", "arvit", "birkat_hamazon", "meen_chaloch", "moussaf_rosh_hodech", "havdala", "hallel"]
OFFICE = {"chaharit": "chaharit", "minha": "minha"}


def nu(s):
    """Le texte sans balises, sans voyelles ni cantillation, espaces réduits."""
    s = re.sub(r"<[^>]+>", " ", s)
    s = re.sub(r"[֑-ׇ]", "", s)
    return re.sub(r"\s+", " ", s).strip()


def mots(s):
    """Sans ponctuation, pour comparer des débuts de prière."""
    return re.sub(r"[^א-ת ]", "", s.replace("־", " ")).split()


def commence(s, debut):
    return mots(s)[: len(debut.split())] == debut.split()


ETE = ("בקיץ:", "בימות החמה:")
HIVER = ("בחורף:", "בימות הגשמים:")

# Les variantes de Ya'alé véyavo : le préfixe de la ligne, et la fête.
VARIANTES_YAALE = [
    (r"^(ב|ל)(ראש חדש|ראש חודש|ר״ח)\b", "roch_hodech"),
    (r"^(ב|ל)(חוה״מ )?פסח\b", "pessah"),
    (r"^(ב|ל)(חוה״מ )?(סוכות|סכות)\b", "soukkot"),
    (r"^(ב|ל)שבועות\b", "chavouot"),
    (r"^(ב|ל)(ראש השנה|ר״ה)\b", "roch_hachana"),
    (r"^(ב|ל)(שמיני עצרת|שמ״ע|ש״ע)", "chemini_atseret"),
    (r"^ילדים האוכלים ביו״כ", "kippour"),
]

# Les variantes enchâssées dans un bloc, après leur annonce.
INDICES = [
    (r"בעשרת ימי תשובה (?:אומרים|אומר|מסיימים)?:? ?([^:()]+)", "asseret_yemei_techouva"),
    (r"\(בעשי״ת ([^)]+)\)", "asseret_yemei_techouva"),
]


def annoter(texte_id, sections, nusah):
    sortie = []

    def note(sorte, si, bi, cond, extra=None):
        extra = dict(extra or {})
        bloc = sections[si]["blocs"][bi] if bi is not None else None
        e = {"sorte": sorte, "s": si, "si": cond}
        if bi is not None:
            e["b"] = bi
            e["empreinte"] = nu(bloc.get("t") or bloc.get("c"))[:40]
        else:
            e["empreinte"] = nu(sections[si]["titre"])
        if extra:
            e.update(extra)
        sortie.append(e)

    office = OFFICE.get(texte_id)
    for si, sec in enumerate(sections):
        titre = nu(sec["titre"])
        # --- Sections entières -------------------------------------------------
        cond = None
        if titre in ("תחנון", "וידוי") and office:
            cond = "tahanoun_" + office
        elif titre == "לשני וחמישי":
            cond = "lundi_jeudi"
        elif "מוצאי שבת" in titre:
            cond = "motsae_chabbat"
        elif titre == "ספירת העומר":
            cond = "omer"
        # La lecture de la Torah (15/10) : un seul groupe, replié les jours sans lecture ;
        # dedans, ce qui ne se dit qu'avec Ta'hanoun, ou que lundi et jeudi, en variante.
        elif titre == "קריאת התורה" and office == "chaharit":
            mode, reste = "kriat_hatorah", None
            for bi, b in enumerate(sec["blocs"]):
                x = nu(b.get("t") or b.get("c"))
                if "c" in b:
                    if "ביום שיש בו תחנון" in x or x.startswith("אין אומרים אא״א"):
                        mode, reste = "tahanoun_chaharit", None
                    elif "ביום שאין בו תחנון" in x:
                        mode, reste = "!tahanoun_chaharit", 1
                    elif "בשני וחמישי כשאומרים תחנון" in x:
                        mode, reste = "lundi_jeudi", None
                    else:
                        mode, reste = "kriat_hatorah", None
                    note("bloc", si, bi, mode, {"force": "kriat_hatorah"})
                    continue
                note("bloc", si, bi, mode, {"force": "kriat_hatorah"})
                if reste is not None:
                    reste -= 1
                    if reste == 0:
                        mode, reste = "kriat_hatorah", None
            continue
        elif titre == "ברכי נפשי" and "בראש חדש" in nu(sec["blocs"][0].get("c", "")):
            cond = "roch_hodech"
        if cond:
            note("section", si, None, cond)
            continue

        blocs = sec["blocs"]
        # Les guillemets doubles ASCII du siddour Edot (« ברה''ש », « ביו''ט ») lus comme gershayim.
        txt = [nu(b.get("t") or b.get("c")).replace("''", "״") for b in blocs]
        cons = ["c" in b for b in blocs]
        pris = set()

        def prend(bi, c, libelle=None):
            if bi not in pris:
                pris.add(bi)
                note("bloc", si, bi, c, {"libelle": libelle} if libelle else None)

        def en_tete(bi, c, *mots_cles, libelle=None):
            """La consigne juste avant un ajout, si elle l'annonce, en suit le sort."""
            if bi > 0 and cons[bi - 1] and all(m in txt[bi - 1] for m in mots_cles):
                prend(bi - 1, c, libelle)

        # Ta'hanoun du siddour ashkénaze (audit du 16/10) : il n'a pas de section à lui, il est
        # dans « אחר העמידה » autour d'Avinou malkénou. Le Vidouï et les treize attributs avant ;
        # après, la consigne « בשני וחמישי יאמר והוא רחום », le long והוא רחום (lundi et jeudi),
        # puis la chute sur la face (« ויאמר דוד ») jusqu'au demi-Kaddich, qui se dit toujours.
        if nusah == "ashkenaz" and office and titre == "אחר העמידה":
            am = next((k for k, y in enumerate(txt) if cons[k] and "אומרים כאן אבינו מלכנו" in y), None)
            lj = next((k for k, y in enumerate(txt) if cons[k] and y.startswith("בשני וחמישי יאמר והוא רחום")), None)
            dd = next((k for k, y in enumerate(txt) if not cons[k] and y.startswith("ויאמר דוד")), None)
            # La fin : la consigne du demi-Kaddich (Cha'harit) ou le Kaddich lui-même (Min'ha).
            kd = next((k for k, y in enumerate(txt) if dd is not None and k > dd
                       and ((cons[k] and "קדיש" in y) or (not cons[k] and commence(y, "יתגדל")))), None)
            if am is not None and txt[0].startswith("טעם נכון למה נתקן הוידוי"):
                for k in range(0, am):
                    prend(k, "tahanoun_" + office)
            if lj is not None and dd is not None and lj < dd:
                prend(lj, "tahanoun_" + office)
                for k in range(lj + 1, dd):
                    prend(k, "lundi_jeudi")
            if dd is not None and kd is not None:
                for k in range(dd, kd):
                    prend(k, "tahanoun_" + office)

        # Anénou : l'officiant le dit dans sa répétition, bénédiction à part
        # entre « גואל ישראל » et « רפאנו » ; le particulier, dans « שומע
        # תפלה ». On les distingue par leur place : avant ou après « רפאנו ».
        rofe = next((k for k, y in enumerate(txt) if not cons[k] and commence(y, "רפאנו")), None)

        def anenou(bi):
            """(condition, libellé) d'un Anénou à la place bi."""
            if rofe is not None and bi < rofe:
                return "jeune", "ajout_anenou_officiant"
            # Le particulier : Edot le dit à Cha'harit et à Min'ha, et le 9 Av à
            # Arvit (« בתשעה באב יש אומרים עננו ») ; Ashkenaz et Sfard, à Min'ha
            # seulement (Rema, OC 565:3), et leur siddour ne le donne qu'à Min'ha.
            return ("ticha_beav" if texte_id == "arvit" else "jeune"), "ajout_anenou_particulier"

        n = len(blocs)
        bi = 0
        while bi < n:
            x = txt[bi]
            # --- Relevé du 19/10 : ce qui restait ouvert tous les jours ---------------------
            # Les bénédictions de qui a oublié רצה ou יעלה ויבא : jamais du jour, toujours repliées.
            # Ashkenaz, Sfard : « דיני שכחה: » jusqu'à la fin de la section ; Edot : « אם שכח לומר
            # "רצה"… » jusqu'à la bénédiction הטוב והמטיב, qui se dit toujours.
            if cons[bi] and x.startswith("דיני שכחה"):
                for j in range(bi, n):
                    prend(j, "oubli")
                bi = n
                continue
            if cons[bi] and x.startswith('אם שכח לומר "רצה"'):
                j = bi
                while j < n and (j == bi or not commence(txt[j], "ברוך אתה יהוה אלהינו מלך העולם לעד")):
                    prend(j, "oubli")
                    j += 1
                bi = j
                continue
            # Barekhi nafchi la veille de Roch 'Hodech (Edot, Arvit : « בליל ראש חודש יש הנוהגים
            # לומר ») : la consigne et le psaume ; Arvit relève déjà du jour hébreu suivant.
            if cons[bi] and x.startswith("בליל ראש חודש") and bi + 1 < n and not cons[bi + 1]:
                prend(bi, "roch_hodech")
                prend(bi + 1, "roch_hodech")
                bi += 2
                continue
            # « אתה חוננתנו » (Edot) : le seul bloc annoncé ; « וחננו מאתך » suit tous les soirs.
            if cons[bi] and x.startswith("במוצאי שבת ויום טוב אומרים") and bi + 1 < n and not cons[bi + 1]:
                prend(bi, "motsae_chabbat_yt")
                prend(bi + 1, "motsae_chabbat_yt")
                bi += 2
                continue
            # La fin d'Arvit du motsaé Chabbat (Edot) : jusqu'au Kaddich titkabal des autres soirs.
            if cons[bi] and x.startswith("בכל לילות החול מלבד מוצאי שבת מדלגים"):
                j = bi
                while j < n and not (cons[j] and "קדיש תתקבל" in txt[j] and j > bi):
                    prend(j, "motsae_chabbat")
                    j += 1
                bi = j
                continue
            # « ה׳ הוא האלהים » des dix jours et de Hochaana Rabba (Edot, Hodou).
            if cons[bi] and x.startswith("בעשרת ימי תשובה והושענא רבה") and bi + 1 < n and not cons[bi + 1]:
                prend(bi, "techouva_hochaana")
                prend(bi + 1, "techouva_hochaana")
                bi += 2
                continue
            # Ce que le siddour fait sauter les jours sans Ta'hanoun : למנצח (Edot), תפלה לדוד
            # (Edot, Sfard ; Sfard ajoute : sans אל ארך אפים, pas de בית יעקב non plus), le
            # « רבונו של עולם » après la Akéda (Sfard).
            if office and cons[bi] and bi + 1 < n and not cons[bi + 1] and (
                    x.startswith("ביום שאין אומרים בו תחנון מדלגים למנצח")
                    or x.startswith("בימים שאין אומרים תחנון אין אומרים תפלה לדוד")
                    or x.startswith("ביום שאין אומרים תחנון אין אומרים זה")):
                prend(bi, "tahanoun_" + office)
                prend(bi + 1, "tahanoun_" + office)
                j = bi + 2
                if "גם בית יעקב אין אומרים" in x and j < n and not cons[j] and commence(txt[j], "בית יעקב"):
                    prend(j, "lamnatseah")
                    j += 1
                bi = j
                continue
            # למנצח (Ashkenaz, Sfard) : pas les jours que le siddour énumère (Sfard, Achré :
            # « ר״ח חנוכה ופורים ערב פסח וערב יו״כ ותשעה באב… י״ד וט״ו באדר ראשון ») ; avec lui,
            # les consignes qui le précèdent.
            if nusah != "edot" and office == "chaharit" and not cons[bi] and commence(x, "למנצח מזמור לדוד יענך"):
                k = bi
                while k > 0 and cons[k - 1] and k - 1 not in pris:
                    k -= 1
                for m in range(k, bi + 1):
                    prend(m, "lamnatseah")
                bi += 1
                continue
            # « לדוד ה׳ אורי » : « ובפרט מר״ח אלול עד הושענא רבא » dit la consigne ; le psaume et
            # son Kaddich, repliés le reste de l'année.
            if cons[bi] and x.startswith("מנהג טוב לומר אחר כל תפלה מזמור לדוד"):
                prend(bi, "ledavid")
                j = bi + 1
                if j < n and not cons[j] and commence(txt[j], "לדוד יהוה אורי"):
                    prend(j, "ledavid")
                    j += 1
                    while j < n and ((cons[j] and txt[j] == "(קדיש יתום)") or
                                     (not cons[j] and any(commence(txt[j], k) for k in KADDICH))):
                        prend(j, "ledavid")
                        j += 1
                bi = j
                continue
            # La maison endeuillée : jamais un jour du calendrier, toujours replié (dépliable).
            if cons[bi] and x.startswith("בבית האבל"):
                j = bi
                while j < n and (j == bi or not cons[j] or txt[j] == "(קדיש יתום)"
                                 or txt[j].startswith("ובימים שאין בהם תחנון")):
                    prend(j, "beit_avel")
                    j += 1
                bi = j
                continue
            # La bénédiction des Cohanim à Min'ha, les jeûnes publics seulement, jusqu'à « שים שלום ».
            if cons[bi] and re.search(r"בתענית ציבור.*ברכת כהנים", x):
                if bi > 0 and cons[bi - 1] and txt[bi - 1] == "ברכת כהנים" and bi - 1 not in pris:
                    prend(bi - 1, "jeune", JEUNE)
                j = bi
                while j < n and (j == bi or not (
                        (not cons[j] and (commence(txt[j], "שים שלום") or commence(txt[j], "שלום רב")
                                          or txt[j].startswith("לתענית ציבור")))
                        or (cons[j] and (txt[j] in ("שלום", "שים שלום") or txt[j].startswith("בעשי״ת"))))):
                    prend(j, "jeune", JEUNE)
                    j += 1
                bi = j
                continue
            # Les versets ajoutés après le Hallel de Roch 'Hodech (Sfard).
            if cons[bi] and x.startswith("יש נוהגין לומר בר״ח אחר הלל"):
                j = bi
                while j < n and (j == bi or not cons[j]):
                    prend(j, "roch_hodech")
                    j += 1
                bi = j
                continue
            # Ya'alé véyavo, ses variantes, jusqu'à « חנון ורחום אתה ».
            if not cons[bi] and commence(x, "אלהינו ואלהי אבותינו יעלה ויבא"):
                en_tete(bi, "yaale_veyavo", "אומרים")
                j = bi
                while j < n:
                    var = next((c for r, c in VARIANTES_YAALE if re.search(r, txt[j])), None)
                    prend(j, var or "yaale_veyavo")
                    if "חנון ורחום אתה" in txt[j] or (j > bi and cons[j] and var is None):
                        break
                    j += 1
                bi = j + 1
                continue
            # Al hanissim, puis ses parties de 'Hanouka et de Pourim.
            if not cons[bi] and (commence(x, "על הנסים ועל הפרקן") or commence(x, "ועל הנסים ועל הפרקן")):
                en_tete(bi, "al_hanissim", "חנוכה")
                prend(bi, "al_hanissim")
                j = bi + 1
                while j < n:
                    y = txt[j]
                    if y.startswith("בחנוכה") or commence(y, "בימי מתתיהו") or commence(y, "בימי מתתיה"):
                        prend(j, "hanouka")
                    elif y.startswith("בפורים") or commence(y, "בימי מרדכי"):
                        prend(j, "pourim")
                    else:
                        break
                    j += 1
                bi = j
                continue
            # Anénou, les jours de jeûne.
            if not cons[bi] and (re.search(r"עננו (אבינו|יהוה) עננו ביום צום", x)):
                c, libelle = anenou(bi)
                en_tete(bi, c, "תענית", libelle=libelle)
                prend(bi, c, libelle)
            elif cons[bi] and re.fullmatch(r"\(?בתענית ציבור אומר כאן הש״ץ עננו ?\)?:?", x):
                prend(bi, "jeune", "ajout_anenou_officiant")
            # Avinou malkénou des jeûnes et des dix jours (Ashkenaz, Sfard) : la consigne
            # « … אומרים כאן אבינו מלכנו » et toute la suite, en un seul groupe ; ses lignes
            # propres à un seul des deux en sont des variantes, estompées l'autre jour
            # (SOUS ; relevé du 14/10).
            elif cons[bi] and "אומרים כאן אבינו מלכנו" in x:
                prend(bi, "avinou_malkenou")
                j = bi + 1
                sous = None
                souche = None
                while j < n:
                    y = txt[j]
                    if cons[j]:
                        if re.fullmatch(r"בתענית ציבור:?", y):
                            sous = "jeune"
                        elif re.fullmatch(r"(בעשי״ת|בעשרת ימי תשובה)( (אומרים|מוסיפים))?:?", y):
                            sous = "asseret_yemei_techouva"
                        else:
                            break
                        souche = None
                        prend(j, sous)
                    elif y.startswith("בתענית ציבור:"):
                        prend(j, "jeune")
                    elif re.match(r"בעשי״ת:", y):
                        prend(j, "asseret_yemei_techouva")
                    elif commence(y, "אבינו מלכנו"):
                        # Une variante ne couvre que ses lignes sœurs (« … כתבנו בספר », « … זכרנו ל ») :
                        # « בטל מעלינו », « הצמח לנו »… se disent dans les deux cas.
                        mots = " ".join(y.split()[:3])
                        if sous and souche is not None and mots != souche:
                            sous = None
                        if sous and souche is None:
                            souche = mots
                        prend(j, sous or "avinou_malkenou")
                    else:
                        break
                    j += 1
                bi = j
                continue
            # Une consigne courte (« בחנוכה אומרים », « בראש חודש: », « בשבת אומר: »…) et ses blocs.
            elif cons[bi] and any(re.fullmatch(m, x) for m, _ in CONSIGNES_COURTES) and bi + 1 < n and not cons[bi + 1]:
                c = next(c for m, c in CONSIGNES_COURTES if re.fullmatch(m, x))
                lib = LIBELLES_CONSIGNE.get(c)
                prend(bi, c, lib)
                j = bi + 1
                # Le Hallel partiel ne saute que le premier bloc annoncé (Ps 115:1-11, 116:1-11) :
                # « יהוה זכרנו » et « מה אשיב » se disent tous les jours de Hallel ; de même,
                # un « הרחמן » de fête n'annonce que sa ligne.
                while j < n and not cons[j] and ((c != "hallel_complet" and c not in UNE_LIGNE) or j == bi + 1):
                    prend(j, c, lib)
                    j += 1
                bi = j
                continue
            # Le psaume du jour (16/10) : seul celui du jour reste ouvert. Ashkenaz et Sfard :
            # « בראשון בשבת: », son en-tête et son psaume, et le renvoi « (הושיענו וכו׳…) » qui
            # suit ; Edot : « מזמור ליום ראשון … » et son psaume.
            elif cons[bi] and re.fullmatch(r"ב(ראשון|שני|שלישי|רביעי|חמישי|שישי|ששי) בשבת:?", x):
                c = JOURS_SEMAINE[re.fullmatch(r"ב(\S+) בשבת:?", x).group(1)]
                prend(bi, c)
                j = bi + 1
                while j < n and not cons[j] and j <= bi + 2:
                    prend(j, c)
                    j += 1
                if j < n and cons[j] and txt[j].startswith("(הושיענו"):
                    prend(j, c)
                    j += 1
                bi = j
                continue
            elif not cons[bi] and re.match(r"מזמור ליום (ראשון|שני|שלישי|רביעי|חמישי|שישי|ששי)\b", x) and bi + 1 < n and not cons[bi + 1]:
                c = JOURS_SEMAINE[re.match(r"מזמור ליום (\S+)", x).group(1)]
                prend(bi, c)
                prend(bi + 1, c)
                bi += 2
                continue
            # Barekhi nafchi à Roch 'Hodech, dans la même section que le psaume du jour (Ashkenaz).
            elif cons[bi] and "ברכי נפשי בראש חדש" in x:
                prend(bi, "roch_hodech")
                j = bi + 1
                while j < n and not cons[j]:
                    prend(j, "roch_hodech")
                    j += 1
                bi = j
                continue
            # Na'hem, le 9 Av.
            elif not cons[bi] and commence(x, "נחם יהוה אלהינו"):
                en_tete(bi, "ticha_beav", "באב")
                prend(bi, "ticha_beav")
            # Na'hem annoncé dans son propre bloc (Edot, Min'ha : « בתשעה באב אומרים: נחם » ; relevé le 14/10).
            elif not cons[bi] and re.match(r"בתשעה באב (יש )?אומרים:? ?נחם", x):
                prend(bi, "ticha_beav")
            # Avinou malkénou des jeûnes (Ashkenaz : « בתענית ציבור: אבינו מלכנו ברך עלינו » ; relevé le 14/10).
            elif not cons[bi] and (x.startswith("בתענית ציבור:") or x.startswith("לתענית ציבור:")):
                prend(bi, "jeune", JEUNE)
            # Les ajouts de Chabbat et de Roch 'Hodech annoncés dans leur bloc (Méèn chaloch, relevé le 14/10).
            elif not cons[bi] and re.match(r"בשבת ורצה", x):
                prend(bi, "chabbat")
            elif not cons[bi] and re.match(r"בראש ?חודש וזכרנו", x):
                prend(bi, "roch_hodech")
            # La prière de Rav Chéchèt, « ביום תענית יאמר רבון העולמים » (Edot, Min'ha ; relevé le 14/10).
            elif not cons[bi] and re.match(r"ביום תענית (יאמר|אומרים)", x):
                prend(bi, "jeune", JEUNE)
            # Été, hiver : la consigne seule, puis son bloc.
            elif cons[bi] and x in ETE + HIVER and bi + 1 < n and not cons[bi + 1]:
                y = txt[bi + 1]
                if x in ETE:
                    c = "morid_hatal" if "מוריד הטל" in y else "berakha"
                else:
                    c = "machiv_harouah" if "משיב הרוח" in y else "tal_oumatar"
                prend(bi, c)
                prend(bi + 1, c)
                bi += 2
                continue
            # Été, hiver : la consigne et sa variante dans le même bloc (Arvit
            # ashkénaze, « בימות החמה: ברכה » ; relevé le 14/10).
            elif not cons[bi] and any(x.startswith(e + " ") for e in ETE + HIVER) \
                    and not (any(e in x for e in ETE) and any(h in x for h in HIVER)):
                if any(x.startswith(e) for e in ETE):
                    c = "morid_hatal" if "מוריד הטל" in x else "berakha"
                else:
                    c = "machiv_harouah" if "משיב הרוח" in x else "tal_oumatar"
                prend(bi, c)
            # Les dix jours : la consigne courte, puis son bloc ; ou le bloc qui
            # s'annonce lui-même (« בעשי״ת: זכרנו לחיים »).
            elif cons[bi] and re.fullmatch(r"(בעשי״ת|בעשרת ימי תשובה)( (מסיים|יסיים|אומרים|מוסיפים|מוסיפין))?:?", x) \
                    and bi + 1 < n and not cons[bi + 1]:
                prend(bi, "asseret_yemei_techouva")
                j = bi + 1
                prend(j, "asseret_yemei_techouva")
                # Avinou malkénou annoncé par la consigne (Edot) : toute la suite.
                while commence(txt[j], "אבינו מלכנו") and j + 1 < n and not cons[j + 1] \
                        and commence(txt[j + 1], "אבינו מלכנו"):
                    j += 1
                    prend(j, "asseret_yemei_techouva")
                bi = j + 1
                continue
            elif not cons[bi] and re.match(r"בעשי״ת( (מסיים|יסיים))?:", x):
                prend(bi, "asseret_yemei_techouva")
            # Retsé, le Chabbat (Birkat hamazone).
            elif not cons[bi] and commence(x, "רצה והחליצנו"):
                en_tete(bi, "chabbat", "בשבת")
                prend(bi, "chabbat")
            # Le Hallel annoncé après l'Amida.
            elif cons[bi] and "אומרים כאן הלל" in x:
                prend(bi, "hallel")
            # « Les jours sans Ta'hanoun » : la consigne et le bloc qui suit.
            elif cons[bi] and x.startswith("בימים שאין בהם תחנון") and office and bi + 1 < n:
                prend(bi, "!tahanoun_" + office)
                prend(bi + 1, "!tahanoun_" + office)
                bi += 2
                continue
            # Les variantes enchâssées : un repère, rien de replié.
            if bi not in pris and not cons[bi]:
                for motif, c in INDICES:
                    for m in re.finditer(motif, x):
                        variante = m.group(1).strip(" ,.")
                        if variante:
                            note("indice", si, bi, c, {"texte": variante})
                if re.fullmatch(r"בקיץ: מוריד הטל\. בחורף: משיב הרוח ומוריד הגשם\.?", x):
                    note("indice", si, bi, "morid_hatal", {"texte": "מוריד הטל"})
                    note("indice", si, bi, "machiv_harouah", {"texte": "משיב הרוח ומוריד הגשם"})
            bi += 1
        if office or texte_id == "arvit":
            minyan(txt, cons, pris, prend)
    # La passe « minyan » ajoute ses blocs après les autres : grouper veut l'ordre du texte.
    sortie.sort(key=lambda e: (e["s"], e.get("b", -1)))
    grouper(sortie)
    for e in sortie:
        e.pop("_force", None)
    return sortie


# Ce qui ne se dit qu'avec un minyan (demande du 19/10) : « כל דבר שבקדושה, כגון קדיש וברכו
# וקדושה, אין אומרים אותו בפחות מי׳ » (Choul'han Aroukh OC 55:1 ; Berakhot 21b) ; la
# bénédiction des Cohanim et Modim derabanan n'existent que dans la répétition de l'officiant.
# « minyan » n'est jamais une condition du jour : ces passages restent repliés, dépliables.
CONSIGNES_MINYAN = ("קהל וחזן:", "שליח ציבור:", "קהל:", "חזן:", "קדושה", "ברכת כהנים", "מודים דרבנן")
KEDOUCHA = ["נקדש", "נקדישך", "נעריצך", "כתר יתנו", "קדוש קדוש קדוש", "לעמתם", "ברוך כבוד",
            "ובדברי קדשך", "ימלך יהוה לעולם", "לדור ודור נגיד"]
BAREKHOU = re.compile(r"^(ואומר החזן: (\(רבנן\) )?|חזן: |קהל וחזן: |ועונים הקהל: |אומרים: )?(ברכו את יהוה המברך|ברוך יהוה המברך לעולם ועד)")
FIN_COHANIM = ("שים שלום", "שלום רב")


def colle(s, debut):
    """Comme commence, sans tenir compte des mots soudés du siddour (« כבודיהוה », « אתשמך »)."""
    return "".join(mots(s)).startswith(debut.replace(" ", ""))


def minyan(txt, cons, pris, prend):
    n = len(txt)

    def debut(bi):
        """Les consignes d'officiant juste avant (« שליח ציבור: », « קדושה »…) suivent le passage."""
        k = bi
        while k > 0 and k - 1 not in pris and cons[k - 1] and (
                txt[k - 1] in CONSIGNES_MINYAN or txt[k - 1].startswith(("בחזרת הש״ץ", "קדושה"))
                or re.fullmatch(r"\(?[^()]{0,30}קדיש[^()]{0,25}\)?:?", txt[k - 1])):
            k -= 1
        return k

    bi = 0
    while bi < n:
        x = txt[bi]
        if bi in pris:
            bi += 1
            continue
        fin = None
        # La Kedoucha de la répétition, de « נקדש / נקדישך » à « ימלך » (et « לדור ודור » ashkénaze).
        if not cons[bi] and any(colle(x, k) for k in KEDOUCHA[:4]):
            fin = bi + 1
            while fin < n and fin not in pris and (
                    (cons[fin] and txt[fin] in CONSIGNES_MINYAN[:4])
                    or (not cons[fin] and any(colle(txt[fin], k) for k in KEDOUCHA))):
                fin += 1
        # Barekhou et sa réponse.
        elif not cons[bi] and BAREKHOU.match(x):
            fin = bi + 1
            while fin < n and fin not in pris and not cons[fin] and BAREKHOU.match(txt[fin]):
                fin += 1
        # Le Kaddich : du « יתגדל » à sa dernière partie.
        elif not cons[bi] and (commence(x, "יתגדל") or commence(x, "קדיש יתום יתגדל")):
            fin = bi + 1
            while fin < n and fin not in pris and not cons[fin] and any(commence(txt[fin], k) for k in KADDICH[1:]):
                fin += 1
        # Modim derabanan, que l'assemblée dit pendant le « מודים » de l'officiant.
        elif (cons[bi] and txt[bi].startswith("בחזרת הש״ץ כשהחזן אומר מודים")) or x.startswith("מודים דרבנן בחזרת"):
            fin = bi + 1
            if cons[bi] and fin < n and not cons[fin] and commence(txt[fin], "מודים אנחנו לך"):
                fin += 1
        # La bénédiction des Cohanim de la répétition, jusqu'à « שים שלום ».
        elif (cons[bi] and (x == "ברכת כהנים" or x.startswith("הטעם שתקנו לברך את ישראל"))) or \
                (not cons[bi] and commence(x, "אלהינו ואלהי אבותינו ברכנו בברכה")):
            fin = bi + 1
            while fin < n and fin not in pris and not (
                    (not cons[fin] and any(commence(txt[fin], k) for k in FIN_COHANIM))
                    or (cons[fin] and txt[fin] in ("שלום", "שים שלום"))):
                fin += 1
        if fin is None:
            bi += 1
            continue
        for j in range(debut(bi), fin):
            prend(j, "minyan")
        bi = fin


# Les variantes qui suivent leur ajout : repliées avec lui quand il est replié.
# « jeune » seul se lit « Anénou » (SiddurJour.libelle) : les autres ajouts du jeûne
# public portent ce libellé-ci.
JEUNE = "ajout_jeune_public"

SOUS = {
    "yaale_veyavo": {"roch_hodech", "pessah", "soukkot", "chavouot", "roch_hachana", "chemini_atseret", "kippour"},
    "al_hanissim": {"hanouka", "pourim"},
    "avinou_malkenou": {"jeune", "asseret_yemei_techouva"},
}

# Une consigne courte annonce les blocs de prière qui la suivent, jusqu'à la
# consigne suivante (relevé du 14/10 : ces passages restaient ouverts tous les jours).
# Le psaume du jour (16/10) : le nom du jour, en hébreu, et sa condition (CalendrierJuif.C.JOURS_SEMAINE).
JOURS_SEMAINE = {"ראשון": "jour_1", "שני": "jour_2", "שלישי": "jour_3", "רביעי": "jour_4", "חמישי": "jour_5",
                 "שישי": "jour_6", "ששי": "jour_6"}

# Les fragments du Kaddich, pour le replier avec le psaume qu'il clôt.
KADDICH = ["יתגדל", "בעלמא", "יהא שמה", "יתברך", "לעלא", "יהא שלמא", "עושה שלום", "על ישראל", "קדיש יתום"]

# Les consignes dont un seul bloc suit (les « הרחמן » et les bénédictions d'oubli des fêtes).
UNE_LIGNE = {"yom_tov", "moed", "roch_hachana", "yom_tov_chabbat", "chabbat_roch_hodech", "roch_hachana_chabbat"}

LIBELLES_CONSIGNE = {"jeune": JEUNE, "!chabbat": "ajout_chol"}

CONSIGNES_COURTES = [
    # Le Birkat hamazone (relevé du 19/10) : les psaumes d'avant, les « הרחמן » des fêtes,
    # les bénédictions de qui a oublié רצה ou יעלה ויבא.
    (r"בחול קודם ברכת המזון אומרים:?", "!chabbat"),
    (r"בשבת קודם ברכת המזון אומרים:?", "chabbat"),
    (r"(בראש חודש|בר״ח) שחל בשבת:?", "chabbat_roch_hodech"),
    (r"(ביו״ט|ביום טוב) שחל בשבת:?", "yom_tov_chabbat"),
    (r"(בראש השנה|ברה״ש) שחל בשבת:?", "roch_hachana_chabbat"),
    (r"(בראש השנה|ברה״ש):?", "roch_hachana"),
    (r"(ביו״ט|ביום טוב)( אומר)?:?", "yom_tov"),
    (r"במועדים:?", "moed"),
    (r"לחנוכה:?", "hanouka"),
    # Les psaumes ajoutés certains jours (Edot, 16/10) : seulement le jour dit.
    (r"בצום גדליה ובעשרה בטבת אומרים:?", "tsom_guedalia_tevet"),
    (r"למחרת יום הכיפורים אומרים:?", "lendemain_kippour"),
    (r"בתענית אסתר ובפורים אומרים:?", "esther_pourim"),
    (r"בשבעה עשר בתמוז אומרים:?", "chiva_assar_tamouz"),
    (r"בחנוכה( (אומרים|אומר|מוסיפים))?( זה)?:?", "hanouka"),
    (r"(בראש חודש|בראש חדש|בר״ח)( (אומרים|אומר|מוסיפים|מוסיפין))?:?", "roch_hodech"),
    (r"בשבת( (אומרים|אומר))?:?", "chabbat"),
    (r"בסוכות:?", "soukkot"),
    (r"בתענית ציבור:?", "jeune"),
    # Le Hallel : les passages sautés les jours de Hallel partiel.
    (r"(בימים שאין גומרים את ההלל (מדלגים|אין אומרים)|בראש חודש מדלגים זה|בר״ח ובחוה״מ פסח .*מדלגין זה):?", "hallel_complet"),
]


def grouper(annotations):
    """
    Un groupe : des blocs consécutifs d'une même section, de même condition
    ou variantes de la première (« racine »). Replié, un groupe tient en une
    ligne ; la racine dit s'il s'applique.
    """
    g = -1
    precedent = None
    for e in annotations:
        if e["sorte"] != "bloc":
            continue
        force = e.pop("force", None)
        if force:
            # Un groupe imposé (la lecture de la Torah) : une seule racine, ses variantes dedans.
            if precedent and precedent.get("_force") == force and precedent["s"] == e["s"] and precedent["b"] + 1 == e["b"]:
                e["g"], e["racine"] = precedent["g"], force
            else:
                g += 1
                e["g"], e["racine"] = g, force
            e["_force"] = force
            precedent = e
            continue
        if precedent and precedent["s"] == e["s"] and precedent["b"] + 1 == e["b"] \
                and e.get("libelle") == precedent.get("libelle") \
                and (e["si"] == precedent["racine"] or e["si"] in SOUS.get(precedent["racine"], ())):
            e["g"], e["racine"] = precedent["g"], precedent["racine"]
        else:
            g += 1
            e["g"], e["racine"] = g, e["si"]
        precedent = e


# Les renvois de Cha'harit (demande du 12/10) : les jours de Hallel, le Hallel
# après la Amida ; à Roch 'Hodech, le Moussaf à sa place dans l'office. La
# place suit l'ordre de chaque siddour à Roch 'Hodech (export Sefaria) :
# Edot, Hallel · Ouva léTsion · Chir chel yom · Moussaf ; Sfard, Hallel ·
# Chir chel yom · Barkhi nafchi · lecture · Achré-Ouva léTsion · Moussaf ;
# Ashkenaz (Metsudah), Hallel · lecture · Achré · Ouva léTsion · Moussaf,
# avant le Kaddich titkabal. (section, début du bloc après lequel renvoyer ou None, condition, cible)
RENVOIS = {
    "edot": [("עמידה", None, "hallel", "hallel"), ("שיר של יום", None, "roch_hodech", "moussaf_rosh_hodech")],
    "sefard": [("עמידה", None, "hallel", "hallel"), ("אשרי", None, "roch_hodech", "moussaf_rosh_hodech")],
    "ashkenaz": [("עמידה", None, "hallel", "hallel"),
                 ("סיום תפילה", "יהי רצון מלפניך יהוה אלהינו ואלהי אבותינו שנשמר", "roch_hodech", "moussaf_rosh_hodech")],
}


def renvois(sections, nusah):
    sortie = []
    for titre, debut, cond, cible in RENVOIS[nusah]:
        si = next(i for i, s in enumerate(sections) if nu(s["titre"]) == titre)
        e = {"sorte": "renvoi", "s": si, "si": cond, "cible": cible}
        if debut is None:
            e["empreinte"] = nu(sections[si]["titre"])
        else:
            bi = next(i for i, b in enumerate(sections[si]["blocs"]) if commence(nu(b.get("t") or b.get("c")), debut))
            e["b"], e["empreinte"] = bi, nu(sections[si]["blocs"][bi].get("t"))[:40]
        sortie.append(e)
    return sortie


def main():
    racine = {"_meta": {
        "outil": "tools/siddur_annotations.py",
        "conditions": "clés de CalendrierJuif.C (KosherJava) ; « ! » : la clé fausse",
        "regle": "les textes ne sont pas modifiés ; un ajout hors du jour est replié, jamais retiré",
        "a_valider": "indicatif : le texte et ses consignes font foi ; relecture par un Rav souhaitée",
    }}
    for nusah in ("edot", "sefard", "ashkenaz"):
        siddur = json.load(open(os.path.join(RAW, f"siddur_{nusah}.json"), encoding="utf-8"))
        racine[nusah] = {}
        for t in siddur["textes"]:
            if t["id"] in TEXTES:
                a = annoter(t["id"], t["sections"], nusah)
                if t["id"] == "chaharit":
                    a += renvois(t["sections"], nusah)
                if a:
                    racine[nusah][t["id"]] = a
    cible = os.path.join(RAW, "siddur_annotations.json")
    json.dump(racine, open(cible, "w", encoding="utf-8"), ensure_ascii=False, indent=1)
    for nusah in ("edot", "sefard", "ashkenaz"):
        print(nusah, {k: len(v) for k, v in racine[nusah].items()})


if __name__ == "__main__":
    main()
