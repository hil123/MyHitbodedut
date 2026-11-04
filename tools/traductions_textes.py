# -*- coding: utf-8 -*-
"""
Les traductions originales des petits textes (04/11/2026, « s'il manque des traductions,
traduis ») : le guide de la hitbodedout, Nichmat, Perek Chira, les parties liturgiques du
Tikoun (ouverture, clôture, kavana, prière de Rabbi Natan) et les sources de la bibliothèque.
Traduites par Claude (FR et EN originales, comme le 'hizouk le 20/09) ; aucune traduction
protégée reprise. Les versets cités suivent l'usage des éditions du domaine public
(l'Éternel / the LORD).

Entrée : un ou plusieurs fichiers {clé: {fr, en}}, clés
  « <livre>|<section>|<bloc> », « <livre>|<section>|titre » (livres.json),
  « tikkun|<partie>|<i> » (tikkun_haklali.json),
  « biblio|<id de source>|<i> » (bibliotheque.json).
Usage (racine du dépôt) : python3 tools/traductions_textes.py sortie_A.json sortie_B.json …
"""
import json
import os
import sys

RAW = "android/app/src/main/res/raw/"
LIVRES = "android/app/src/main/livres/livres.json"  # la source ; tools/livres_decoupe.py en tire les fichiers par livre
NOTE = "FR et EN originales (Claude, 04/11/2026), relues automatiquement (clés, balises) ; relecture humaine souhaitable."


def main(fichiers):
    trad = {}
    for f in fichiers:
        trad.update(json.load(open(f, encoding="utf-8")))

    livres = json.load(open(LIVRES, encoding="utf-8"))
    par_id = {t["id"]: t for t in livres["textes"]}
    tikkun = json.load(open(RAW + "tikkun_haklali.json", encoding="utf-8"))
    biblio = json.load(open(RAW + "bibliotheque.json", encoding="utf-8"))
    sources = {}
    for s in biblio["sections"]:
        for ss in s["sous_sections"]:
            for e in ss["entrees"]:
                if e.get("type") == "source":
                    sources[e["id"]] = e

    compte = {}
    for cle, v in trad.items():
        quoi, a, b = cle.split("|")
        fr, en = v["fr"].strip(), v["en"].strip()
        if quoi == "tikkun":
            bloc = tikkun[a]
            n = len(bloc["paragraphes"])
            for l, t in (("fr", fr), ("en", en)):
                bloc.setdefault(f"paragraphes_{l}", [""] * n)[int(b)] = t
        elif quoi == "biblio":
            e = sources[a]
            n = len(e["paragraphes"])
            for l, t in (("fr", fr), ("en", en)):
                e.setdefault(f"paragraphes_{l}", [""] * n)[int(b)] = t
        else:
            section = par_id[quoi]["sections"][int(a)]
            if b == "titre":
                section["titre_fr"], section["titre_en"] = fr, en
            else:
                bloc = section["blocs"][int(b)]
                assert "t" in bloc, cle
                bloc["fr"], bloc["en"] = fr, en
            par_id[quoi]["traduction"] = NOTE
        compte[quoi] = compte.get(quoi, 0) + 1

    tikkun["_meta"]["traduction_liturgie"] = NOTE
    biblio["_meta"]["traduction_sources"] = NOTE
    json.dump(livres, open(LIVRES, "w", encoding="utf-8"), ensure_ascii=False, separators=(",", ":"))
    sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
    import livres_decoupe
    livres_decoupe.decoupe(livres)
    json.dump(tikkun, open(RAW + "tikkun_haklali.json", "w", encoding="utf-8"), ensure_ascii=False, indent=2)
    json.dump(biblio, open(RAW + "bibliotheque.json", "w", encoding="utf-8"), ensure_ascii=False, indent=2)
    print(compte)


if __name__ == "__main__":
    main(sys.argv[1:])
