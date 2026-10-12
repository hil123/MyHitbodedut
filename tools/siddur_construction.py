# -*- coding: utf-8 -*-
"""
Construit android/app/src/main/res/raw/siddur_{edot,sefard,ashkenaz}.json
(04/10/2026) : les textes demandés par le propriétaire d'après la liste de
l'application CalJ, dans les trois nusah.

Sources (export public de Sefaria, storage.googleapis.com/sefaria-export),
choisies pour leur licence :
  - Edot HaMizrach : version « merged » = Torat Emet 357 (domaine public) et
    Shaliehsaboo Edition (CC0).
  - Sefard : The Metsudah Siddur 1981 (CC-BY) pour les offices de semaine,
    Torat Emet 357 (domaine public) pour le reste.
  - Ashkenaz : The Metsudah Siddur 1981 (CC-BY) et Daat (domaine public).
    Les versions sans licence déclarée (Veshinantam, Torat Emet Ashkenaz)
    sont écartées.
  Ce qui manque à un nusah dans ces sources (Tikoun 'Hatsot, ordre de l'Ari
  z"l) est repris d'Edot HaMizrach et marqué « nusah ». Les Hachkavot,
  coutume séfarade, deviennent l'El malé ra'hamim du Yizkor : Siddur Sefard
  (Torat Emet) en Sefard, la page Yizkor de Sefaria en Ashkenaz, comme le
  Chéva bérakhot ashkénaze (textes de Wikipédia en hébreu, CC-BY-SA).
  - Parashat HaMan : Chemot 16:4-36, « Tanach with Nikkud » (tanach.us,
    domaine public), comme le livre de Tehilim. Le passage est celui que lit
    le siddour Sefard (« Parashat Haman Reading »).

Le Nom est écrit en toutes lettres dans les prières (demande du 04/10) : voir tools/noms_divins.py.

  python3 tools/siddur_construction.py <dossier contenant les deux fichiers>
"""
import html, json, re, sys, unicodedata
import os
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from noms_divins import developpe
from os.path import join

NFC = lambda s: unicodedata.normalize("NFC", s)


def nettoie(s):
    """Ne garde que <b>, <small> et <br> : le texte, ses titres en gras, ses consignes."""
    s = re.sub(r'<i class="footnote">.*?</i>', "", s, flags=re.S)
    s = re.sub(r"<sup[^>]*>.*?</sup>", "", s, flags=re.S)
    s = re.sub(r"<big>", "<b>", s).replace("</big>", "</b>")
    s = re.sub(r"<br\s*/?>", "<br>", s)
    s = re.sub(r"<(?!/?(?:b|small)>|br>)[^>]+>", "", s)
    s = s.replace("&lt;", "‹").replace("&gt;", "›")
    s = html.unescape(s)
    s = re.sub(r"<b>\s*</b>|<small>\s*</small>", "", s)
    s = re.sub(r"[ \t\r\n]+", " ", s).strip()
    # Les balises gras doublées par le passage <big><b> → <b><b>.
    while "<b><b>" in s:
        s = s.replace("<b><b>", "<b>").replace("</b></b>", "</b>")
    return NFC(s)


def bloc(s):
    """
    Un paragraphe : prière (« t ») ou consigne (« c »). La prière est
    vocalisée, la consigne ne l'est pas : c'est ce qui les distingue, plus
    sûrement que les petits caractères, que la source emploie aussi pour
    des prières entières (le Kaddich, les « leshem yihoud »).
    """
    s = nettoie(s)
    nu = re.sub(r"<[^>]+>", "", s)
    if not nu.strip():
        return None
    lettres = len(re.findall("[\u05D0-\u05EA]", nu))
    voyelles = len(re.findall("[\u05B0-\u05BC\u05C1\u05C2\u05C7]", nu))
    if lettres and voyelles / lettres < 0.2:
        return {"c": hebraise(re.sub(r"</?small>", "", s).strip())}
    # Une prière entière en petits caractères se lit en taille normale.
    entier = re.fullmatch(r"(<b>)?<small>(.*)</small>(</b>)?", s)
    if entier and "<small>" not in entier.group(2):
        s = (entier.group(1) or "") + entier.group(2) + (entier.group(3) or "")
    # Le Nom en toutes lettres dans la prière (tools/noms_divins.py).
    s, n = developpe(s)
    COMPTE[0] += n
    return {"t": hebraise(s)}


def hebraise(s):
    """Guershayim et guéresh hébreux au lieu des guillemets ASCII : בא"ח → בא״ח, ר' → ר׳."""
    s = re.sub(r'([\u05D0-\u05EA][\u0591-\u05C7]*)"(?=[\u05D0-\u05EA])', "\\1\u05F4", s)
    return re.sub(r"([\u05D0-\u05EA][\u0591-\u05C7]*)'(?=$|[\s,.:;)\]<])", "\\1\u05F3", s)


COMPTE = [0]


def blocs(segments, sauter_titre=True):
    sortie = []
    for i, s in enumerate(segments):
        if sauter_titre and i == 0 and s.lstrip().startswith("<big>"):
            continue
        b = bloc(s)
        if b:
            sortie.append(b)
    return sortie


def charge(dossier, nom):
    return json.load(open(join(dossier, nom), encoding="utf-8"))


def aplatit(t):
    """Les segments d'un nœud, dans l'ordre, quelle que soit sa profondeur."""
    if isinstance(t, dict):
        for v in t.values():
            yield from aplatit(v)
    elif isinstance(t, list):
        for v in t:
            yield from aplatit(v)
    elif isinstance(t, str):
        yield t


class Source:
    """Une version de Sefaria (texte) et le schéma de son livre (titres hébreux des sections)."""

    def __init__(self, texte, schema, nom):
        self.texte, self.schema, self.nom = texte, schema, nom

    def noeud(self, chemin):
        t = self.texte
        for k in chemin:
            t = t[k]
        return t

    def titre(self, chemin):
        n = self.schema
        for k in chemin:
            n = next(c for c in n["nodes"] if k in (c.get("key"), c.get("enTitle")))
        # « לדוד ה' » : le Nom en toutes lettres jusque dans les titres.
        titre, compte = developpe(n["heTitle"].strip())
        COMPTE[0] += compte
        return hebraise(titre)

    def sections(self, chemin, sauf=()):
        t = self.noeud(chemin)
        if isinstance(t, dict):
            sortie = []
            for k, v in t.items():
                if k in sauf:
                    continue
                b = blocs(list(aplatit(v)))
                if b:
                    sortie.append({"titre": self.titre(chemin + [k]), "blocs": b})
            return sortie
        return [{"titre": self.titre(chemin), "blocs": blocs(list(aplatit(t)))}]

    def plusieurs(self, chemins):
        return [s for c in chemins for s in self.sections(c)]

    def extrait(self, chemin, debut, fin, titre):
        return [{"titre": titre, "blocs": blocs(list(aplatit(self.noeud(chemin)))[debut:fin], sauter_titre=False)}]


def qere(v):
    v = re.sub(r"([^\s\[\]\u05BE]+\u05BE?) ?\[([^\]]+)\]", r"\2", v)
    v = re.sub(r"\[([^\]]+)\]", r"\1", v)
    return re.sub(r"\s{2,}", " ", v.replace("\u05BE ", "\u05BE")).strip()


def T(he, fr, en):
    return {"he": NFC(he), "fr": fr, "en": en}


TITRES = {
    "birkot_hachahar": ("jour", T("בִּרְכוֹת הַשַּׁחַר", "Birkot hacha’har", "Morning blessings")),
    "chaharit": ("jour", T("תְּפִלַּת שַׁחֲרִית", "Cha’harit", "Shaharit")),
    "minha": ("jour", T("תְּפִלַּת מִנְחָה", "Min’ha", "Minha")),
    "arvit": ("jour", T("תְּפִלַּת עַרְבִית", "Arvit", "Arvit")),
    "chema_coucher": ("jour", T("קְרִיאַת שְׁמַע שֶׁעַל הַמִּטָּה", "Chema du coucher", "Bedtime Shema")),
    "tikoun_hatsot": ("jour", T("תִּקּוּן חֲצוֹת", "Tikoun ’hatsot", "Tikkun Hatsot")),
    "acher_yatsar": ("brakhot", T("אֲשֶׁר יָצַר", "Acher yatsar", "Asher Yatsar")),
    "birkat_hamazon": ("brakhot", T("בִּרְכַּת הַמָּזוֹן", "Birkat hamazone", "Birkat HaMazon")),
    "meen_chaloch": ("brakhot", T("בְּרָכָה מֵעֵין שָׁלֹשׁ", "Méèn chaloch", "Me’ein Shalosh")),
    "bore_nefachot": ("brakhot", T("בּוֹרֵא נְפָשׁוֹת", "Boré néfachot", "Borei Nefashot")),
    "tefilat_haderekh": ("brakhot", T("תְּפִלַּת הַדֶּרֶךְ", "Téfilat hadérekh", "Traveler’s prayer")),
    "birkat_halevana": ("moments", T("בִּרְכַּת הַלְּבָנָה", "Birkat halévana", "Blessing of the moon")),
    "hallel": ("moments", T("הַלֵּל", "Hallel", "Hallel")),
    "moussaf_rosh_hodech": ("moments", T("מוּסָף לְרֹאשׁ חֹדֶשׁ", "Moussaf de Roch ’Hodech", "Musaf for Rosh Chodesh")),
    "cheva_berakhot": ("moments", T("שֶׁבַע בְּרָכוֹת", "Chéva bérakhot", "Sheva Berakhot")),
    "parachat_haman": ("moments", T("פָּרָשַׁת הַמָּן", "Parachat hamane", "Parashat HaMan")),
    "kaddich": ("moments", T("קַדִּישׁ", "Kaddich", "Kaddish")),
    "hachkavot": ("moments", T("הַשְׁכָּבוֹת", "Hachkavot", "Hashkavot")),
}
EL_MALE = T("אֵל מָלֵא רַחֲמִים", "El malé ra’hamim", "El Malei Rahamim")
NUSAH = {
    "edot": T("נוסח ספרדי – עדות המזרח", "Noussa’h séfarade (Edot HaMizra’h)", "Sephardi nusah (Edot HaMizrah)"),
    "sefard": T("נוסח ספרד (האר״י)", "Noussa’h Sfard (Arizal)", "Nusah Sfard (Arizal)"),
    "ashkenaz": T("נוסח אשכנז", "Noussa’h Ashkenaz", "Nusah Ashkenaz"),
}
GROUPES = [
    {"cle": "jour", "titre": T("תְּפִלּוֹת הַיּוֹם", "Prières du jour", "Daily prayers")},
    {"cle": "brakhot", "titre": T("בְּרָכוֹת", "Bénédictions", "Blessings")},
    {"cle": "moments", "titre": T("לְעִתִּים", "Pour certains moments", "For particular times")},
]


# Les douze צירופי הוי״ה des mois (demande du 11/10), relevés dans le Bnei
# Yissaschar, Piotrkow 1883 (export Sefaria, NLI 001763733), en tête de
# chaque mois : le צירוף et les mots du verset tels que l'auteur les écrit,
# sans vocalisation ajoutée. La phrase citée est au début des maamarim de
# Nisan (maamar 1, 2). Recoupé le 11/10 (export Sefaria) : les Tikounei Zohar
# (hakdama, 9b) donnent les mêmes douze צירופים et versets, sans les mois ;
# le Pardès Rimonim (21:14 et 21:16) range ההי״ו en Eloul et ההו״י en Adar
# (déduit de l'ordre par trimestres, les mois n'y sont pas nommés) et cite
# « הסכת ושמע ישראל היום » pour הוי״ה ; le Pri Etz 'Haïm (Roch 'Hodech 6:20)
# donne הוה״י pour Adar. On suit le Bnei Yissaschar, et on ne prononce pas.
TSEROUFIM = [
    ("ניסן", "יהו״ה", "ר״ת ישמחו השמים ותגל הארץ"),
    ("אייר", "יהה״ו", "ר״ת יתהלל המתהלל השכל וידוע"),
    ("סיון", "יוה״ה", "ר״ת ידות ולצלע המשכן השנית"),
    ("תמוז", "הוה״י", "ס״ת וכל זה איננו שוה לי"),
    ("אב", "הוי״ה", "הנה יד ה׳ הויה"),
    ("אלול", "ההו״י", "ס״ת וצדקה תהיה לנו כי"),
    ("תשרי", "והי״ה", "ס״ת ויראו אתה שרי פרעה"),
    ("מרחשון", "וה״הי", "ר״ת ודבש היום הזה ה׳"),
    ("כסלו", "ויה״ה", "ר״ת וירא יושב הארץ הכנעני"),
    ("טבת", "היה״ו", "ס״ת גדלו לה׳ אתי ונרוממה שמו"),
    ("שבט", "היו״ה", "ר״ת המר ימירנו והיה הוא"),
    ("אדר", "ההי״ו", "ס״ת עירה ולשרקה בני אתונו"),
]


def tseroufim():
    """La consigne du Bnei Yissaschar, placée à la fin de la bénédiction du milieu."""
    lignes = [f"{mois}: <b>{t}</b> ({verset})" for mois, t, verset in TSEROUFIM]
    return {"c": NFC("<b>צירוף שם הוי״ה של החודש</b> – בני יששכר, מאמרי חודש ניסן: "
                     "„ידוע מכתבי מרן האריז״ל אשר בכל ראש חדש צריכין לכוין במוסף בסיום הברכה "
                     "האמצעית צירוף הוי״ה השולטת באותו החדש”.<br>"
                     # Précision demandée par le propriétaire (11/10) : en tête de liste, en gras.
                     "<b>אין להוציא צירופים אלו בפה כלל, אלא רק לכוון בהם במחשבה, לאחר סיום הברכה.</b><br>"
                     + "<br>".join(lignes))}


def avec_tseroufim(sections):
    """
    Après la fin de la bénédiction du milieu (« מקדש ישראל וראשי חדשים »),
    qui suit « חדש עלינו » dans le même paragraphe (Edot, Sfard) ou dans un
    paragraphe à part (Ashkenaz, Daat).
    """
    nu = lambda b: re.sub(r"[\u0591-\u05C7]", "", re.sub(r"<[^>]+>", "", b.get("t", "")))
    for s in sections:
        debut = next((i for i, b in enumerate(s["blocs"]) if "חדש עלינו" in nu(b)), None)
        if debut is None:
            continue
        for j in range(debut, len(s["blocs"])):
            if "מקדש ישראל וראשי חדשים" in nu(s["blocs"][j]):
                s["blocs"].insert(j + 1, tseroufim())
                return sections
        if debut + 1 < len(s["blocs"]) or s is sections[-1]:
            break
        # La chatima ouvre la section suivante.
        k = sections.index(s) + 1
        for j, b in enumerate(sections[k]["blocs"]):
            if "מקדש ישראל וראשי חדשים" in nu(b):
                sections[k]["blocs"].insert(j + 1, tseroufim())
                return sections
    raise SystemExit("moussaf de Roch 'Hodech : fin de la bénédiction du milieu introuvable")


def sans_chabbat(sections):
    """
    Le Moussaf de Roch 'Hodech de semaine seulement (demande du 12/10) : les
    consignes qui renvoient au Moussaf de Chabbat Roch 'Hodech sont retirées.
    """
    nu = lambda b: re.sub(r"<[^>]+>", "", b.get("c", ""))
    for s in sections:
        s["blocs"] = [b for b in s["blocs"] if "שבת" not in nu(b)]
    return sections


def sans_renvois(sections):
    """Les renvois « (תהילים קי״ג:א׳-ב׳) » de l'édition Shaliehsaboo ne sont pas du texte de prière."""
    for s in sections:
        for b in s["blocs"]:
            if "t" in b:
                b["t"] = re.sub(r"\s*<small>\([^()<]*\)</small>", "", b["t"])
    return sections


def jusqua(sections, debut_consigne):
    """La section coupée avant la consigne qui commence par [debut_consigne] (le Hallel, sans la lecture de la Torah)."""
    for s in sections:
        i = next((i for i, b in enumerate(s["blocs"]) if b.get("c", "").startswith(debut_consigne)), None)
        if i is not None:
            s["blocs"] = s["blocs"][:i]
    return sections


# Consigne de l'application, sans source à reproduire : le demi-Hallel de Roch
# 'Hodech omet Tehilim 115, 1-11 et 116, 1-11 (Choul'han Aroukh, Ora'h 'Haïm
# 422, 2 ; c'est aussi ce que disent les siddourim Edot et Sfard ci-dessus).
GUEMATRIA = {i: ("י" * (i // 10) if i < 20 else "כ") + ("" if i % 10 == 0 else "אבגדהוזחט"[i % 10 - 1]) for i in range(1, 30)}
GUEMATRIA.update({15: "טו", 16: "טז", 20: "כ"})
SAUT_ROSH_HODECH = {"c": NFC("בראש חודש מדלגים זה:")}


def hallel_daat(sections):
    """
    Le Hallel du siddour Daat porte les numéros de versets (« א », « ב »…) :
    retirés ; Tehilim 115 et 116 coupés au verset 12, avec la consigne du saut.
    """
    for s in sections:
        sortie = []
        for b in s["blocs"]:
            if "t" not in b:
                sortie.append(b)
                continue
            # Un numéro de verset : lettres sans voyelle, en tête ou après la fin d'un verset,
            # suivies d'une espace ou collées au mot vocalisé qui ouvre le verset (« יבמָה »).
            parts = re.split(r"(?:^|(?<=[.:׃] ))([\u05D0-\u05EA]{1,3})(?: |(?=[\u05D0-\u05EA][\u0591-\u05C7]))", b["t"])
            versets = [(parts[i], parts[i + 1].strip()) for i in range(1, len(parts) - 1, 2)]
            if not versets:
                sortie.append(b)
                continue
            numeros = [n for n, _ in versets]
            # Garde-fou : les numéros se suivent (א, ב, ג…), sinon la découpe a mordu dans le texte.
            if numeros != [GUEMATRIA[i] for i in range(1, len(numeros) + 1)]:
                raise SystemExit(f"Hallel (Daat), {s['titre']} : numéros de versets inattendus {numeros}")
            texte = lambda vs: " ".join(v for _, v in vs)
            if s["titre"] in ("תהילים קטו", "תהילים קטז") and "יב" in numeros:
                k = numeros.index("יב")
                sortie += [SAUT_ROSH_HODECH, {"t": texte(versets[:k])}, {"t": texte(versets[k:])}]
            else:
                sortie.append({"t": texte(versets)})
        s["blocs"] = sortie
    return sections


def main(dossier):
    em_merged = charge(dossier, "Siddur_Edot_HaMizrach.json")
    sf_merged = charge(dossier, "Siddur_Sefard.json")
    as_merged = charge(dossier, "Siddur_Ashkenaz.json")
    EM = Source(em_merged["text"], em_merged["schema"], "Siddur Edot HaMizrach (Torat Emet 357, Public Domain; Shaliehsaboo Edition, CC0)")
    SM = Source(charge(dossier, "Siddur_Sefard__Hebrew__The_Metsudah_siddur,_1981.json")["text"], sf_merged["schema"], "Siddur Sefard, The Metsudah Siddur 1981 (CC-BY)")
    ST = Source(charge(dossier, "Siddur_Sefard__Hebrew__Torat_Emet_357.json")["text"], sf_merged["schema"], "Siddur Sefard, Torat Emet 357 (Public Domain)")
    AM = Source(charge(dossier, "Ashk__The_Metsudah_siddur,_1981.json")["text"], as_merged["schema"], "Siddur Ashkenaz, The Metsudah Siddur 1981 (CC-BY)")
    AD = Source(charge(dossier, "Ashk__Daat_Siddur_Ashkenaz.json")["text"], as_merged["schema"], "Siddur Ashkenaz, Daat (Public Domain)")
    exode = charge(dossier, "Exodus_TN.json")["text"]
    haman = [{"titre": "שמות ט״ז, ד׳–ל״ו", "blocs": [{"t": NFC(qere(v)).replace("\u200d", "")} for v in exode[15][3:36]]}]
    TN = "Tanach with Nikkud (tanach.us, Public Domain), Exodus 16:4-36"

    edot = {
        "birkot_hachahar": (EM, EM.sections(["Preparatory Prayers"])),
        "chaharit": (EM, EM.sections(["Weekday Shacharit"])),
        "minha": (EM, EM.sections(["Weekday Mincha"])),
        "arvit": (EM, EM.sections(["Weekday Arvit"])),
        "chema_coucher": (EM, EM.sections(["Bedtime Shema"])),
        "tikoun_hatsot": (EM, EM.sections(["The Midnight Rite"])),
        "acher_yatsar": (EM, EM.extrait(["Preparatory Prayers", "Morning Blessings"], 3, 5, "אשר יצר")),
        "birkat_hamazon": (EM, EM.sections(["Post Meal Blessing"])),
        "meen_chaloch": (EM, EM.sections(["Al Hamihya"])),
        "bore_nefachot": (EM, EM.extrait(["Blessings on Enjoyments"], 11, 13, "בורא נפשות")),
        "tefilat_haderekh": (EM, EM.sections(["Assorted Blessings and Prayers", "Traveler's Prayer"])),
        "birkat_halevana": (EM, EM.sections(["Blessing of the Moon"])),
        "hallel": (EM, sans_renvois(jusqua(EM.sections(["Rosh Hodesh", "Hallel"]), "קודם הוצאת ספר תורה"))),
        "moussaf_rosh_hodech": (EM, sans_chabbat(avec_tseroufim(EM.sections(["Rosh Hodesh", "Mussaf"])))),
        "cheva_berakhot": (EM, EM.sections(["Assorted Blessings and Prayers", "Sheva Berachot"])),
        "parachat_haman": (TN, haman),
        "kaddich": (EM, EM.extrait(["Weekday Shacharit", "Kaveh"], 13, 16, "קדיש על ישראל")),
        "hachkavot": (EM, EM.extrait(["Fast Days and Mourning", "Mourning"], 19, 31, "השכבות")),
    }
    # L'El malé ra'hamim de la prière du souvenir (Yizkor), pour un homme et
    # pour une femme. Pas celui des jeûnes (« של כ' סיון »), qui est l'élégie
    # de 5408 pour Rabbi Ye'hiel Mikhal de Nemirov : une erreur corrigée le 04/10.
    el_male_sefard = ST.extrait(["Holidays", "Yizkor"], 11, 15, "אל מלא רחמים")
    yizkor = charge(dossier, "Yizkor_PD.json")["text"]
    el_male_ashkenaz = [{"titre": "אל מלא רחמים", "blocs": blocs(["לזכר:", yizkor[9], "לנקבה:", yizkor[11]], sauter_titre=False)}]
    YZ = "Yizkor, Sefaria / Wikipedia (he), CC-BY-SA"
    sheva = charge(dossier, "Sheva_Brachot_WP.json")["text"]["Sheva Brachot"]
    sheva_ashkenaz = [{"titre": "שבע ברכות", "blocs": blocs(list(sheva), sauter_titre=False)}]
    SB = "Sheva Brachot, Sefaria / Wikipedia (he), CC-BY-SA"
    sefard = {
        "birkot_hachahar": (SM, SM.plusieurs([["Upon Arising", "Modeh Ani"], ["Weekday Shacharit", "Morning Blessings"], ["Weekday Shacharit", "Blessings on Torah"]])),
        "chaharit": (SM, SM.sections(["Weekday Shacharit"])),
        "minha": (SM, SM.sections(["Weekday Mincha"])),
        "arvit": (SM, SM.sections(["Weekday Maariv"])),
        "chema_coucher": (ST, ST.sections(["Bedtime Shema"])),
        "tikoun_hatsot": ("edot",) + edot["tikoun_hatsot"],
        "acher_yatsar": (SM, SM.extrait(["Weekday Shacharit", "Morning Blessings"], 1, 2, "אשר יצר")),
        "birkat_hamazon": (ST, ST.sections(["Birchat HaMazon", "Birchat HaMazon"])),
        "meen_chaloch": (ST, ST.sections(["Blessings", "Me'ein Shalosh"])),
        "bore_nefachot": (ST, ST.sections(["Blessings", "Borei Nefashot"])),
        "tefilat_haderekh": (ST, ST.sections(["Blessings", "Traveler's Prayer"])),
        "birkat_halevana": (ST, ST.sections(["Kiddush Levanah"])),
        "hallel": (ST, ST.sections(["Rosh Chodesh", "Hallel"])),
        "moussaf_rosh_hodech": (ST, sans_chabbat(avec_tseroufim(ST.sections(["Rosh Chodesh", "Mussaf"])))),
        "cheva_berakhot": (ST, ST.sections(["Various Blessings", "Sheva Berachot"])),
        "parachat_haman": (TN, haman),
        "kaddich": (SM, SM.extrait(["Weekday Shacharit", "Aleinu"], 5, 9, "קדיש יתום")),
        "hachkavot": (ST, el_male_sefard, EL_MALE),
    }
    ashkenaz = {
        "birkot_hachahar": (AM, [s for k in ["Modeh Ani", "Netilat Yadayim", "Asher Yatzar", "Elokai Neshama", "Torah Blessings", "Morning Blessings"]
                                 for s in AM.sections(["Weekday", "Shacharit", "Preparatory Prayers", k])]),
        "chaharit": (AM, AM.sections(["Weekday", "Shacharit"])),
        "minha": (AM, AM.sections(["Weekday", "Minchah"])),
        "arvit": (AM, AM.sections(["Weekday", "Maariv"], sauf=("Birkat HaLevana", "Keri'at Shema al Hamita"))),
        "chema_coucher": (AM, AM.sections(["Weekday", "Maariv", "Keri'at Shema al Hamita"])),
        "tikoun_hatsot": ("edot",) + edot["tikoun_hatsot"],
        "acher_yatsar": (AM, AM.sections(["Weekday", "Shacharit", "Preparatory Prayers", "Asher Yatzar"])),
        "birkat_hamazon": (AM, AM.sections(["Berachot", "Birkat HaMazon"])),
        "meen_chaloch": (AD, AD.sections(["Berachot", "Birkat Hanehenin", "Eating", "Brachot Achronot", "Al Hamichyah"])),
        "bore_nefachot": (AD, AD.sections(["Berachot", "Birkat Hanehenin", "Eating", "Brachot Achronot", "Borei Nefashot"])),
        "tefilat_haderekh": (AD, AD.sections(["Berachot", "Tefillat HaDerech"])),
        "birkat_halevana": (AM, AM.sections(["Weekday", "Maariv", "Birkat HaLevana"])),
        "hallel": (AD, hallel_daat(AD.sections(["Festivals", "Rosh Chodesh", "Hallel"]))),
        "moussaf_rosh_hodech": (AD, sans_chabbat(avec_tseroufim(AD.sections(["Festivals", "Rosh Chodesh", "Musaf Amidah for Rosh Chodesh"])))),
        "cheva_berakhot": (SB, sheva_ashkenaz),
        "parachat_haman": (TN, haman),
        "kaddich": (AD, AD.sections(["Kaddish", "Mourner's Kaddish"])),
        "hachkavot": (YZ, el_male_ashkenaz, EL_MALE),
    }

    for cle, table in [("edot", edot), ("sefard", sefard), ("ashkenaz", ashkenaz)]:
        textes, sources = [], set()
        for id_, (groupe, titre) in TITRES.items():
            entree = table[id_]
            emprunt = entree[0] if isinstance(entree[0], str) and entree[0] in NUSAH else None
            if emprunt:
                entree = entree[1:]
            source, sections = entree[0], entree[1]
            if len(entree) > 2:
                titre = entree[2]
            nom_source = source if isinstance(source, str) else source.nom
            sources.add(nom_source)
            t = {"id": id_, "groupe": groupe, "titre": titre, "source": nom_source, "sections": sections}
            if emprunt:
                # Absent des sources libres de ce nusah : repris d'un autre, et dit.
                t["nusah"] = NUSAH[emprunt]
            textes.append(t)
        sortie = {
            "_meta": {
                "version": "1.0", "date": "2026-10-04", "nusah": NUSAH[cle],
                "sources": sorted(sources),
                "noms_divins": "En toutes lettres : les abréviations des sources (יְיָ, יי, ה', אלקים) sont développées dans les prières, pas dans les consignes (tools/noms_divins.py).",
                "balisage": "Chaque paragraphe est soit {t: texte} avec <b>, <small>, <br> pour seules balises, soit {c: consigne}.",
                "relecture": "Extraction automatique ; une lecture humaine reste souhaitable, en particulier des consignes.",
            },
            "groupes": GROUPES,
            "textes": textes,
        }
        chemin = f"android/app/src/main/res/raw/siddur_{cle}.json"
        json.dump(sortie, open(chemin, "w", encoding="utf-8"), ensure_ascii=False, separators=(",", ":"))
        print(f"   noms développés jusqu'ici : {COMPTE[0]}")
        print(f"== {cle} : {len(textes)} textes, {sum(len(s['blocs']) for x in textes for s in x['sections'])} paragraphes")
        for x in textes:
            n = sum(len(s["blocs"]) for s in x["sections"])
            print(f'   {x["id"]:18} {len(x["sections"]):3} sections {n:4} paragraphes {"(emprunt)" if "nusah" in x else ""}')


if __name__ == "__main__":
    main(sys.argv[1])
