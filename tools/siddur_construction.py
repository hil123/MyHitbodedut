# -*- coding: utf-8 -*-
"""
Construit android/app/src/main/res/raw/siddur.json (04/10/2026) : les textes
demandés par le propriétaire d'après la liste de l'application CalJ.

Sources (export public de Sefaria, storage.googleapis.com/sefaria-export) :
  - Siddur Edot HaMizrach, version « merged » : Torat Emet 357 (domaine
    public) et édition Shaliehsaboo (CC0). Nusah séfarade.
  - Parashat HaMan : Chemot 16:4-36, « Tanach with Nikkud » (tanach.us,
    domaine public), comme le livre de Tehilim. Le passage est celui que lit
    le siddour Sefard (« Parashat Haman Reading »).

Le Nom reste en toutes lettres, comme dans les sources (décision du 04/10).

  python3 tools/siddur_construction.py <dossier contenant les deux fichiers>
"""
import html, json, re, sys, unicodedata
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
        return {"c": re.sub(r"</?small>", "", s).strip()}
    # Une prière entière en petits caractères se lit en taille normale.
    entier = re.fullmatch(r"(<b>)?<small>(.*)</small>(</b>)?", s)
    if entier and "<small>" not in entier.group(2):
        s = (entier.group(1) or "") + entier.group(2) + (entier.group(3) or "")
    return {"t": s}


def blocs(segments, sauter_titre=True):
    sortie = []
    for i, s in enumerate(segments):
        if sauter_titre and i == 0 and s.lstrip().startswith("<big>"):
            continue
        b = bloc(s)
        if b:
            sortie.append(b)
    return sortie


def main(dossier):
    sid = json.load(open(join(dossier, "Siddur_Edot_HaMizrach.json"), encoding="utf-8"))
    texte, schema = sid["text"], sid["schema"]
    exode = json.load(open(join(dossier, "Exodus_TN.json"), encoding="utf-8"))["text"]

    def noeud_schema(chemin):
        n = schema
        for k in chemin:
            n = next(c for c in n["nodes"] if k in (c.get("key"), c.get("enTitle")))
        return n

    def noeud(chemin):
        t = texte
        for k in chemin:
            t = t[k]
        return t

    def sections(chemin):
        t = noeud(chemin)
        if isinstance(t, dict):
            return [{"titre": noeud_schema(chemin + [k])["heTitle"].strip(), "blocs": blocs(v)}
                    for k, v in t.items() if blocs(v)]
        return [{"titre": noeud_schema(chemin)["heTitle"].strip(), "blocs": blocs(t)}]

    def extrait(chemin, debut, fin, titre):
        return [{"titre": titre, "blocs": blocs(noeud(chemin)[debut:fin], sauter_titre=False)}]

    def qere(v):
        v = re.sub(r"([^\s\[\]־]+־?) ?\[([^\]]+)\]", r"\2", v)
        v = re.sub(r"\[([^\]]+)\]", r"\1", v)
        return re.sub(r"\s{2,}", " ", v.replace("־ ", "־")).strip()

    def T(he, fr, en):
        return {"he": NFC(he), "fr": fr, "en": en}

    E = "Siddur Edot HaMizrach"
    textes = [
        ("jour", "birkot_hachahar", T("בִּרְכוֹת הַשַּׁחַר", "Birkot hacha’har", "Morning blessings"), f"{E}, Preparatory Prayers", sections(["Preparatory Prayers"])),
        ("jour", "chaharit", T("תְּפִלַּת שַׁחֲרִית", "Cha’harit", "Shacharit"), f"{E}, Weekday Shacharit", sections(["Weekday Shacharit"])),
        ("jour", "minha", T("תְּפִלַּת מִנְחָה", "Min’ha", "Mincha"), f"{E}, Weekday Mincha", sections(["Weekday Mincha"])),
        ("jour", "arvit", T("תְּפִלַּת עַרְבִית", "Arvit", "Arvit"), f"{E}, Weekday Arvit", sections(["Weekday Arvit"])),
        ("jour", "chema_coucher", T("קְרִיאַת שְׁמַע שֶׁעַל הַמִּטָּה", "Chema du coucher", "Bedtime Shema"), f"{E}, Bedtime Shema", sections(["Bedtime Shema"])),
        ("jour", "tikoun_hatsot", T("תִּקּוּן חֲצוֹת", "Tikoun ’hatsot", "Tikkun Chatzot"), f"{E}, The Midnight Rite", sections(["The Midnight Rite"])),
        ("brakhot", "acher_yatsar", T("אֲשֶׁר יָצַר", "Acher yatsar", "Asher Yatzar"), f"{E}, Morning Blessings", extrait(["Preparatory Prayers", "Morning Blessings"], 3, 5, "אשר יצר")),
        ("brakhot", "birkat_hamazon", T("בִּרְכַּת הַמָּזוֹן", "Birkat hamazone", "Birkat HaMazon"), f"{E}, Post Meal Blessing", sections(["Post Meal Blessing"])),
        ("brakhot", "meen_chaloch", T("בְּרָכָה מֵעֵין שָׁלֹשׁ", "Méèn chaloch", "Me’ein Shalosh"), f"{E}, Al Hamihya", sections(["Al Hamihya"])),
        ("brakhot", "bore_nefachot", T("בּוֹרֵא נְפָשׁוֹת", "Boré néfachot", "Borei Nefashot"), f"{E}, Blessings on Enjoyments", extrait(["Blessings on Enjoyments"], 11, 13, "בורא נפשות")),
        ("brakhot", "tefilat_haderekh", T("תְּפִלַּת הַדֶּרֶךְ", "Téfilat hadérekh", "Traveler’s prayer"), f"{E}, Traveler's Prayer", sections(["Assorted Blessings and Prayers", "Traveler's Prayer"])),
        ("moments", "birkat_halevana", T("בִּרְכַּת הַלְּבָנָה", "Birkat halévana", "Blessing of the moon"), f"{E}, Blessing of the Moon", sections(["Blessing of the Moon"])),
        ("moments", "cheva_berakhot", T("שֶׁבַע בְּרָכוֹת", "Chéva bérakhot", "Sheva Berachot"), f"{E}, Sheva Berachot", sections(["Assorted Blessings and Prayers", "Sheva Berachot"])),
        ("moments", "parachat_haman", T("פָּרָשַׁת הַמָּן", "Parachat hamane", "Parashat HaMan"), "Tanach with Nikkud, Chemot 16:4-36",
         [{"titre": "שמות ט״ז, ד׳–ל״ו", "blocs": [{"t": NFC(qere(v))} for v in exode[15][3:36]]}]),
        ("moments", "kaddich", T("קַדִּישׁ", "Kaddich", "Kaddish"), f"{E}, Kaveh", extrait(["Weekday Shacharit", "Kaveh"], 13, 16, "קדיש על ישראל")),
        ("moments", "hachkavot", T("הַשְׁכָּבוֹת", "Hachkavot", "Hashkavot"), f"{E}, Mourning", extrait(["Fast Days and Mourning", "Mourning"], 19, 31, "השכבות")),
    ]
    sortie = {
        "_meta": {
            "version": "1.0", "date": "2026-10-04",
            "source": {"siddur": "Siddur Edot HaMizrach (Sefaria, version merged : Torat Emet 357, domaine public ; Shaliehsaboo Edition, CC0)",
                       "parachat_haman": "Tanach with Nikkud (tanach.us, domaine public), Chemot 16:4-36"},
            "nusah": "Edot HaMizrach (séfarade), comme l'application CalJ dont vient la liste.",
            "noms_divins": "En toutes lettres, comme dans les sources.",
            "balisage": "Chaque paragraphe est soit {t: texte} avec <b>, <small>, <br> pour seules balises, soit {c: consigne}.",
            "relecture": "Extraction automatique ; une lecture humaine reste souhaitable, en particulier des consignes.",
        },
        "groupes": [
            {"cle": "jour", "titre": T("תְּפִלּוֹת הַיּוֹם", "Prières du jour", "Daily prayers")},
            {"cle": "brakhot", "titre": T("בְּרָכוֹת", "Bénédictions", "Blessings")},
            {"cle": "moments", "titre": T("לְעִתִּים", "Pour certains moments", "For particular times")},
        ],
        "textes": [{"id": i, "groupe": g, "titre": t, "source": s, "sections": sec} for g, i, t, s, sec in textes],
    }
    chemin = "android/app/src/main/res/raw/siddur.json"
    json.dump(sortie, open(chemin, "w", encoding="utf-8"), ensure_ascii=False, separators=(",", ":"))
    for t in sortie["textes"]:
        n = sum(len(s["blocs"]) for s in t["sections"])
        print(f'{t["id"]:18} {len(t["sections"]):3} sections {n:4} paragraphes')


if __name__ == "__main__":
    main(sys.argv[1])
