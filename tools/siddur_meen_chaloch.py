# -*- coding: utf-8 -*-
"""
Méèn chaloch ashkénaze (demande du 29/10, « il y a des choses qui ne sont pas
repliées ») : les ajouts de Chabbat, Roch 'Hodech, Roch haChana et des fêtes
sont dans le même bloc que le texte de tous les jours, séparés par des
« <br> ». On coupe ce bloc à ces sauts, pour que chaque ajout soit un bloc à
lui et que tools/siddur_annotations.py puisse le replier hors du jour.

Le texte n'est pas touché : les morceaux, recollés avec « <br> », redonnent le
bloc d'origine. Idempotent : sans « <br> » dans le bloc, rien ne change.

  python3 tools/siddur_meen_chaloch.py && python3 tools/siddur_annotations.py
"""
import json, os

ICI = os.path.dirname(os.path.abspath(__file__))
CIBLE = os.path.join(ICI, "..", "android", "app", "src", "main", "res", "raw", "siddur_ashkenaz.json")


def main():
    d = json.load(open(CIBLE, encoding="utf-8"))
    t = next(t for t in d["textes"] if t["id"] == "meen_chaloch")
    blocs = t["sections"][0]["blocs"]
    i = next((i for i, b in enumerate(blocs) if "<small>בשבת:</small>" in b.get("t", "") and "<br>" in b["t"]), None)
    if i is None:
        print("déjà coupé")
        return
    morceaux = [m.strip() for m in blocs[i]["t"].split("<br>")]
    assert "<br> ".join(morceaux) == blocs[i]["t"], "le découpage ne redonne pas le bloc"
    blocs[i:i + 1] = [{"t": m} for m in morceaux]
    json.dump(d, open(CIBLE, "w", encoding="utf-8"), ensure_ascii=False, separators=(",", ":"))
    print(f"bloc {i} coupé en {len(morceaux)}")


if __name__ == "__main__":
    main()
