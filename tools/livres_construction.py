# -*- coding: utf-8 -*-
"""
Construit android/app/src/main/res/raw/livres.json (demande du 12/10/2026) :
des livres entiers de Rabbi Na'hman et de Rabbi Natan pour la bibliothèque,
au format des textes du siddour (sections, paragraphes « t »), pour être
lus dans le même lecteur.

Sources (export public de Sefaria, storage.googleapis.com/sefaria-export,
Chasidut/Breslov), relevées le 12/10 avec la licence de chaque version :
  - ספר המדות : « Sefer HaMidot -- rabenubook », licence Public Domain ;
    repris tel quel, vocalisé. Les deux parties d'un chapitre (« Part I »,
    « Part II ») se suivent, la seconde annoncée par « חלק שני ».
  - ליקוטי עצות : « Likutei Etzot - rabenubook.com », licence « unknown ».
    Le texte de Rabbi Natan (m. 1844) est du domaine public ; la
    vocalisation est un travail éditorial dont la licence n'est pas
    déclarée : elle est retirée, comme pour le Likoutei Tefilot de la
    bibliothèque. Le titre du chapitre, répété en tête du premier
    paragraphe, est retiré (il est déjà le titre de la section).

Le Nom, quand il est abrégé (ה׳, יי), reste tel que l'imprime le livre :
ce ne sont pas des prières.

  python3 tools/livres_construction.py <dossier des deux fichiers de version>
"""
import json, os, re, sys, unicodedata

NFC = lambda s: unicodedata.normalize("NFC", s)
SORTIE = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "android", "app", "src", "main", "res", "raw", "livres.json")


def nettoie(s):
    s = re.sub(r"<br\s*/?>", "<br>", s)
    s = re.sub(r"<(?!/?b>|br>)[^>]+>", "", s)
    s = re.sub(r"[ \t\r\n]+", " ", s).strip()
    return NFC(s)


def sans_nikkoud(s):
    # Voyelles et signes de cantillation ; le maqaf devient une espace, le geresh et le gershayim restent.
    return re.sub(r"[֑-ֽֿ-ׂׄ-ׇ]", "", s).replace("־", " ")


def titres(schema):
    return {n.get("key", n.get("enTitle")): n["heTitle"] for n in schema["nodes"]}


def segments(x):
    if isinstance(x, dict):
        for v in x.values():
            yield from segments(v)
    elif isinstance(x, list):
        for v in x:
            yield from segments(v)
    elif isinstance(x, str) and x.strip():
        yield x


def midot(v):
    he = titres(v["schema"])
    sections = []
    for cle, chap in v["text"].items():
        blocs = []
        for partie, paras in (chap.items() if isinstance(chap, dict) else [("", chap)]):
            paras = [nettoie(p) for p in segments(paras)]
            if not paras:
                continue
            if partie == "Part II":
                blocs.append({"c": "חלק שני"})
            blocs += [{"t": p} for p in paras if p]
        if blocs:
            sections.append({"titre": NFC(he[cle]), "blocs": blocs})
    return sections


def etsot(v):
    he = titres(v["schema"])
    sections = []
    for cle, chap in v["text"].items():
        paras = [sans_nikkoud(nettoie(p)) for p in segments(chap)]
        titre = sans_nikkoud(NFC(he[cle]))
        # « <b>אמת ואמונה</b><br>א. … » : le titre du chapitre ouvre le premier paragraphe.
        if paras:
            paras[0] = re.sub(r"^<b>[^<]*</b>\s*<br>\s*", "", paras[0])
        blocs = [{"t": p} for p in paras if re.sub(r"<[^>]+>", "", p).strip()]
        if blocs:
            sections.append({"titre": titre, "blocs": blocs})
    return sections


def main(dossier):
    v_midot = json.load(open(os.path.join(dossier, "v_SeferHaMidotrabenubook.json"), encoding="utf-8"))
    v_etsot = json.load(open(os.path.join(dossier, "v_LikuteiEtzotrabenubookcom.json"), encoding="utf-8"))
    assert v_midot["license"] == "Public Domain", v_midot["license"]
    src_midot = "Sefer HaMidot — rabenubook (via export Sefaria), Public Domain"
    src_etsot = "Likutei Etzot — rabenubook.com (via export Sefaria) ; sans la vocalisation, de licence non déclarée"
    sortie = {
        "_meta": {
            "version": "1.0", "date": "2026-10-12",
            "nusah": {"he": "ספרי ברסלב", "fr": "Livres de Breslev", "en": "Breslov books"},
            "sources": [src_midot],
            "outil": "tools/livres_construction.py",
        },
        "groupes": [{"cle": "livres", "titre": {"he": "סְפָרִים", "fr": "Livres", "en": "Books"}}],
        "textes": [
            {"id": "likoutei_etsot", "groupe": "livres", "source": src_etsot,
             "titre": {"he": "ליקוטי עצות", "fr": "Likoutei Etsot", "en": "Likutei Etzot"}, "sections": etsot(v_etsot)},
            {"id": "sefer_hamidot", "groupe": "livres", "source": src_midot,
             "titre": {"he": "ספר המדות", "fr": "Sefer HaMiddot", "en": "Sefer HaMiddot"}, "sections": midot(v_midot)},
        ],
    }
    json.dump(sortie, open(SORTIE, "w", encoding="utf-8"), ensure_ascii=False, separators=(",", ":"))
    for t in sortie["textes"]:
        print(t["id"], len(t["sections"]), "chapitres", sum(len(s["blocs"]) for s in t["sections"]), "paragraphes")
    print(os.path.getsize(SORTIE) // 1024, "ko")


if __name__ == "__main__":
    main(sys.argv[1])
