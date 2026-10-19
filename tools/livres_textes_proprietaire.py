# -*- coding: utf-8 -*-
"""
Deux textes fournis par le propriétaire entrent dans la bibliothèque des
livres (réponse « oui tout » du 19/10/2026) :

  - le guide « המדריך המקיף להתבודדות » (textes/guide_hitbodedut.md), son
    texte à lui (22/09) : un chapitre par « חלק ». Les encadrés en français
    (« Correction du 22/09 »…) sont des notes de vérification, pas du texte :
    ils restent dans le fichier source et dans guide_hitbodedut.verification.md.
    Le guide n'est pas une prière : ה׳ y reste tel qu'il l'a écrit.

  - les dix feuillets « תפילה לשלמה » du Yanuka (textes/tefila_lishlomo/),
    transcrits le 22/09, autorisation des ayants droit déclarée par le
    propriétaire. Un chapitre par feuillet, le crédit imprimé en tête. Seul le
    texte entre les deux premiers « --- » de chaque fichier est repris : le
    reste est la relecture. Dans le feuillet de 'Habaqouq, le commentaire
    entrelacé n'a pas d'ordre sûr (couche texte du PDF) : seuls les versets,
    exacts, sont repris. Ce sont des prières : le Nom y est écrit en entier
    (tools/noms_divins.py), sauf dans les consignes.

Les deux textes sont insérés (ou remplacés, par leur id) dans
android/app/src/main/res/raw/livres.json ; le web le recopie au build.

  python3 tools/livres_textes_proprietaire.py
"""
import json, os, re, sys, unicodedata

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from noms_divins import developpe  # noqa: E402

NFC = lambda s: unicodedata.normalize("NFC", s)
RACINE = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
LIVRES = os.path.join(RACINE, "android", "app", "src", "main", "res", "raw", "livres.json")
GUIDE = os.path.join(RACINE, "textes", "guide_hitbodedut.md")
FEUILLETS = os.path.join(RACINE, "textes", "tefila_lishlomo")

LATIN = re.compile(r"[A-Za-zÀ-ÿ]{3,}")


def hebraise(s):
    """Guillemets et apostrophes des abréviations hébraïques : ״ et ׳."""
    # ל"תנאי כניסה" : une lettre-préfixe seule devant une citation n'est pas un sigle.
    s = re.sub(r'(?<![\u05D0-\u05EA\u0591-\u05C7])([ובלמשכה])"(?=[\u05D0-\u05EA]{2}[^"]{0,40}")', "\\1\x00", s)
    s = re.sub(r'([\u05D0-\u05EA][\u0591-\u05C7]*)"(?=[\u05D0-\u05EA](?![\u0591-\u05C7]))', "\\1\u05F4", s)  # pas ו"וְיַחֵד : une conjonction et un guillemet
    s = re.sub(r"(?<![\u05D0-\u05EA\u0591-\u05C7])([\u05D0-\u05EA][\u0591-\u05C7]*)'(?=[\s,.:;!?)\]\"<]|$)", "\\1\u05F3", s)
    s = re.sub(r"(?<![\u05D0-\u05EA\u0591-\u05C7])([ובלמשכ]ה)'(?=[\s,.:;!?)\]\"<]|$)", "\\1\u05F3", s)
    s = re.sub(r"([זגצ])'(?=[\u05D0-\u05EA])", "\\1\u05F3", s)  # ז׳ de מליז׳נסק
    return s.replace("\x00", '"')


def gras(s):
    s = re.sub(r"\*\*(.+?)\*\*", r"<b>\1</b>", s)
    s = re.sub(r"(?<![\w*])\*(?!\s)(.+?)(?<!\s)\*(?![\w*])", r"<small>\1</small>", s)
    return s


def propre(s):
    return NFC(re.sub(r"\s+", " ", hebraise(gras(s))).strip())


# --- Le guide ---------------------------------------------------------------

def guide():
    t = open(GUIDE, encoding="utf-8").read()
    t = re.sub(r"<!--.*?-->", "", t, flags=re.S)
    sections, para = [], []

    def ferme():
        if para:
            texte = " ".join(para)
            # Une remarque de vérification ajoutée dans le corps (22/09), en français.
            texte = re.sub(r"\s*— et non de Likoutei Moharan.*$", "", texte)
            sections[-1]["blocs"].append({"t": propre(texte)})
            para.clear()

    for ligne in t.splitlines():
        l = ligne.rstrip()
        if l.startswith("# "):
            continue
        if l.startswith("## "):
            if sections:
                ferme()
            sections.append({"titre": propre(l[3:]), "blocs": []})
            continue
        if not sections:
            continue
        if l.startswith(">"):  # note de vérification en français
            ferme()
            continue
        if l.startswith("### "):
            ferme()
            sections[-1]["blocs"].append({"t": "<b>" + propre(l[4:]) + "</b>"})
            continue
        if not l.strip():
            ferme()
            continue
        if l.startswith("|"):  # tableau des sources : une ligne, un paragraphe
            ferme()
            cases = [c.strip() for c in l.strip("|").split("|")]
            if set("".join(cases)) <= set("-: ") or cases == ["מקור", "רעיון מרכזי"]:
                continue
            cases = [re.sub(r"\s*\*\([^)]*[A-Za-z][^)]*\)\*", "", c) for c in cases]
            sections[-1]["blocs"].append({"t": propre("<b>" + cases[0] + "</b> — " + cases[1])})
            continue
        if re.match(r"\d+\. ", l):  # liste numérotée : un paragraphe par point
            ferme()
        para.append(l.strip())
    ferme()
    for s in sections:
        for b in s["blocs"]:
            assert not LATIN.search(re.sub(r"MyHitbodedut|<[^>]+>", "", b["t"])), b["t"][:80]
    return sections


# --- Les feuillets du Yanuka -----------------------------------------------

def feuillet(chemin):
    lignes = open(chemin, encoding="utf-8").read().splitlines()
    titre = re.match(r"# prayer-\d+ — (.+)", lignes[0]).group(1)
    auteur = next(l.split(":", 1)[1].strip() for l in lignes if l.startswith("- **Auteur**"))
    auteur = re.sub(r"\s*—\s*\*.*\*\s*$", "", auteur)
    traits = [i for i, l in enumerate(lignes) if l.strip() == "---"]
    corps = lignes[traits[0] + 1: traits[1]]
    blocs = [{"c": propre(auteur) + " · כל הזכויות שמורות למכון ׳ים שלמה׳"}]
    premier_titre = True
    for l in corps:
        l = l.strip()
        if not l or l in ('בס"ד', "בס״ד") or re.match(r"#+ page \d+$", l):
            continue
        l = re.sub(r"\s*\*\[voir anomalie \d+\]\*\s*", " ", l)
        if LATIN.search(l):  # titres et remarques de transcription, en français
            continue
        if l.startswith("#"):
            texte = l.lstrip("#").strip()
            if premier_titre and l.startswith("## "):
                premier_titre = False
                if NFC(re.sub("[\u0591-\u05C7]", "", texte)).replace("י", "") == NFC(titre).replace("י", ""):
                    continue
            blocs.append({"t": "<b>" + propre(texte) + "</b>"})
            continue
        entier = re.fullmatch(r"\*\*([^*]+)\*\*|\*([^*]+)\*", l)
        if entier:  # une consigne imprimée en gras ou en italique
            blocs.append({"c": propre(entier.group(1) or entier.group(2))})
            continue
        blocs.append({"t": developpe(propre(l))[0]})
    return {"titre": propre(titre), "blocs": blocs}


def tefila_lishlomo():
    fichiers = sorted(f for f in os.listdir(FEUILLETS) if re.fullmatch(r"prayer-\d+\.md", f))
    assert len(fichiers) == 10, fichiers
    return [feuillet(os.path.join(FEUILLETS, f)) for f in fichiers]


SOURCE_GUIDE = "Texte du propriétaire de l'application, fourni le 22/09/2026"
SOURCE_YANUKA = ("מכון ׳ים שלמה׳, רבנו שלמה יהודה בארי שליט״א (הינוקא) — כל הזכויות שמורות ; "
                 "autorisation des ayants droit déclarée par le propriétaire de l'application (22/09/2026)")


def main():
    d = json.load(open(LIVRES, encoding="utf-8"))
    nouveaux = {
        "guide_hitbodedut": {
            "id": "guide_hitbodedut", "groupe": "livres", "source": SOURCE_GUIDE,
            "titre": {"he": "המדריך המקיף להתבודדות", "fr": "Le guide complet de la hitbodedout",
                      "en": "The complete guide to hitbodedut"},
            "sections": guide()},
        "tefila_lishlomo": {
            "id": "tefila_lishlomo", "groupe": "meguilot", "source": SOURCE_YANUKA,
            "titre": {"he": "תפילה לשלמה — הינוקא", "fr": "Tefila LiChlomo — prières du Yanouka",
                      "en": "Tefila LiShlomo — prayers of the Yanuka"},
            "sections": tefila_lishlomo()},
    }
    textes = [t for t in d["textes"] if t["id"] not in nouveaux]
    # Le guide ouvre les livres ; les prières du Yanuka viennent avant les méguilot.
    textes.insert(0, nouveaux["guide_hitbodedut"])
    i = next(i for i, t in enumerate(textes) if t["groupe"] == "meguilot")
    textes.insert(i, nouveaux["tefila_lishlomo"])
    d["textes"] = textes
    for s in (SOURCE_GUIDE, SOURCE_YANUKA):
        if s not in d["_meta"]["sources"]:
            d["_meta"]["sources"].append(s)
    json.dump(d, open(LIVRES, "w", encoding="utf-8"), ensure_ascii=False, separators=(",", ":"))
    for t in nouveaux.values():
        print(t["id"], len(t["sections"]), "chapitres", sum(len(s["blocs"]) for s in t["sections"]), "paragraphes")


if __name__ == "__main__":
    main()
