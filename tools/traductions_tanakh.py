# -*- coding: utf-8 -*-
"""
Les traductions française et anglaise des textes du Tanakh de l'application
(04/11/2026, « traduis Tehilim et les autres livres ») : deux éditions du
domaine public, alignées verset par verset sur l'hébreu (tanach.us) :

  fr : Bible du Rabbinat 1899 (dir. Zadoc Kahn), via Sefaria
       (versionTitle « Bible du Rabbinat 1899 [fr] », licence Public Domain,
       source https://fr.wikisource.org/wiki/Bible_du_Rabbinat_1899)
  en : The Holy Scriptures: A New Translation (JPS 1917), via Sefaria
       (licence Public Domain)

Ce que le script écrit :
  - tehilim.json : « traductions » {fr, en} → {edition, psaumes: [[verset…]×150]},
    à côté de « psaumes » (inchangé : l'hébreu reste une liste de chaînes) ;
  - tikkun_haklali.json : « fr » et « en » dans chaque verset des dix psaumes ;
  - livres.json : « fr » et « en » dans chaque bloc d'Esther et de Chir HaChirim
    (blocs « (א) … », un par verset).

Le nombre de versets est vérifié chapitre par chapitre ; un écart arrête tout.
Usage (racine du dépôt) : python3 tools/traductions_tanakh.py [cache.json]
Sans cache, les textes sont lus sur l'API de Sefaria (v3), un chapitre à la fois.
"""
import json
import os
import re
import sys
import time
import urllib.parse
import urllib.request

RAW = "android/app/src/main/res/raw/"
VERSIONS = {
    "fr": "french|Bible du Rabbinat 1899 [fr]",
    "en": "english|The Holy Scriptures: A New Translation (JPS 1917)",
}
EDITIONS = {
    "fr": {"titre": "Bible du Rabbinat 1899 (dir. Zadoc Kahn) — via Sefaria", "licence": "Public Domain",
           "source": "https://fr.wikisource.org/wiki/Bible_du_Rabbinat_1899"},
    "en": {"titre": "The Holy Scriptures: A New Translation (JPS 1917) — via Sefaria", "licence": "Public Domain",
           "source": "http://opensiddur.org/2010/08/the-holy-scriptures-a-new-translation-jps-1917/"},
}
LIVRES = {"Psalms": 150, "Esther": 10, "Song of Songs": 8}


def chapitre(ref):
    q = "&".join("version=" + urllib.parse.quote(v) for v in VERSIONS.values())
    url = f"https://www.sefaria.org/api/v3/texts/{urllib.parse.quote(ref)}?{q}"
    for essai in range(5):
        try:
            with urllib.request.urlopen(url, timeout=60) as r:
                d = json.load(r)
            out = {}
            for v in d["versions"]:
                langue = "en" if "JPS" in v["versionTitle"] else "fr"
                assert v.get("license") == "Public Domain", (ref, v["versionTitle"], v.get("license"))
                out[langue] = [propre(x) for x in v["text"]]
            return out
        except Exception as e:  # réseau : on réessaie
            print("nouvel essai", ref, e, file=sys.stderr)
            time.sleep(2 ** essai)
    raise SystemExit("échec : " + ref)


def propre(t):
    """Sans balises ni notes, espaces simples."""
    t = re.sub(r"<sup[^>]*>.*?</sup>|<i class=\"footnote\">.*?</i>", "", t)
    t = re.sub(r"<[^>]+>", "", t)
    return re.sub(r"\s+", " ", t).strip()


def typo(langue, t):
    """L'apostrophe française typographique (l’Éternel), comme le reste de l'application."""
    return t.replace("'", "\u2019") if langue == "fr" else t


def charge(cache):
    if cache and os.path.exists(cache):
        brut = json.load(open(cache, encoding="utf-8"))
        tout = {l: [{k: [propre(x) for x in c[k]["texte"]] for k in ("fr", "en")} for c in chs] for l, chs in brut.items()}
    else:
        tout = {l: [chapitre(f"{l}.{i}") for i in range(1, n + 1)] for l, n in LIVRES.items()}
    return {l: [{k: [typo(k, x) for x in c[k]] for k in c} for c in chs] for l, chs in tout.items()}


def main():
    tout = charge(sys.argv[1] if len(sys.argv) > 1 else None)

    teh = json.load(open(RAW + "tehilim.json", encoding="utf-8"))
    for i, ch in enumerate(tout["Psalms"]):
        for l in ("fr", "en"):
            assert len(ch[l]) == len(teh["psaumes"][i]), ("Tehilim", i + 1, l, len(ch[l]), len(teh["psaumes"][i]))
    teh["traductions"] = {l: {"edition": EDITIONS[l], "psaumes": [ch[l] for ch in tout["Psalms"]]} for l in ("fr", "en")}
    json.dump(teh, open(RAW + "tehilim.json", "w", encoding="utf-8"), ensure_ascii=False, indent=1)

    tik = json.load(open(RAW + "tikkun_haklali.json", encoding="utf-8"))
    for p in tik["psaumes"]:
        for v in p["versets"]:
            for l in ("fr", "en"):
                v[l] = tout["Psalms"][p["numero"] - 1][l][v["n"] - 1]
    tik["_meta"]["traductions"] = EDITIONS
    json.dump(tik, open(RAW + "tikkun_haklali.json", "w", encoding="utf-8"), ensure_ascii=False, indent=2)

    livres = json.load(open(RAW + "livres.json", encoding="utf-8"))
    for tid, livre in (("meguilat_esther", "Esther"), ("chir_hachirim", "Song of Songs")):
        t = next(x for x in livres["textes"] if x["id"] == tid)
        for i, s in enumerate(t["sections"]):
            blocs = [b for b in s["blocs"] if "t" in b]
            for l in ("fr", "en"):
                assert len(blocs) == len(tout[livre][i][l]), (tid, i + 1, l)
            for k, b in enumerate(blocs):
                for l in ("fr", "en"):
                    b[l] = tout[livre][i][l][k]
        t["traductions"] = EDITIONS
    json.dump(livres, open(RAW + "livres.json", "w", encoding="utf-8"), ensure_ascii=False, separators=(",", ":"))
    print("traductions écrites : Tehilim 150, Tikoun 10, Esther 10, Chir HaChirim 8 chapitres")


if __name__ == "__main__":
    main()
