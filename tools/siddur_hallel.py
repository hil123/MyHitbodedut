# -*- coding: utf-8 -*-
"""
Ajoute le Hallel (demande du 12/10/2026) aux trois siddur_{edot,sefard,ashkenaz}.json
existants, juste avant le Moussaf de Roch 'Hodech, et retire de ce Moussaf
la consigne qui renvoie au Moussaf de Chabbat Roch 'Hodech. Les autres
textes ne sont pas reconstruits (voir tools/siddur_moussaf_rosh_hodech.py).
Sources et découpe : tools/siddur_construction.py (TITRES, sans_renvois,
jusqua, hallel_daat, sans_chabbat), qui les produit aussi.

  python3 tools/siddur_hallel.py <dossier de l'export Sefaria>
"""
import json, os, sys
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import siddur_construction as sc

ID = "hallel"


def main(dossier):
    em = sc.charge(dossier, "Siddur_Edot_HaMizrach.json")
    sf = sc.charge(dossier, "Siddur_Sefard.json")
    asch = sc.charge(dossier, "Siddur_Ashkenaz.json")
    EM = sc.Source(em["text"], em["schema"], "Siddur Edot HaMizrach (Torat Emet 357, Public Domain; Shaliehsaboo Edition, CC0)")
    ST = sc.Source(sc.charge(dossier, "Siddur_Sefard__Hebrew__Torat_Emet_357.json")["text"], sf["schema"], "Siddur Sefard, Torat Emet 357 (Public Domain)")
    AD = sc.Source(sc.charge(dossier, "Ashk__Daat_Siddur_Ashkenaz.json")["text"], asch["schema"], "Siddur Ashkenaz, Daat (Public Domain)")
    tables = {
        "edot": (EM, sc.sans_renvois(sc.jusqua(EM.sections(["Rosh Hodesh", "Hallel"]), "קודם הוצאת ספר תורה"))),
        "sefard": (ST, ST.sections(["Rosh Chodesh", "Hallel"])),
        "ashkenaz": (AD, sc.hallel_daat(AD.sections(["Festivals", "Rosh Chodesh", "Hallel"]))),
    }
    groupe, titre = sc.TITRES[ID]
    for cle, (source, sections) in tables.items():
        nom = f"android/app/src/main/res/raw/siddur_{cle}.json"
        d = json.load(open(nom, encoding="utf-8"))
        textes = [t for t in d["textes"] if t["id"] != ID]
        for t in textes:
            if t["id"] == "moussaf_rosh_hodech":
                sc.sans_chabbat(t["sections"])
        avant = next(i for i, t in enumerate(textes) if t["id"] == "moussaf_rosh_hodech")
        textes.insert(avant, {"id": ID, "groupe": groupe, "titre": titre, "source": source.nom, "sections": sections})
        d["textes"] = textes
        d["_meta"]["sources"] = sorted(set(d["_meta"]["sources"]) | {source.nom})
        json.dump(d, open(nom, "w", encoding="utf-8"), ensure_ascii=False, separators=(",", ":"))
        n = sum(len(s["blocs"]) for s in sections)
        print(f"{cle}: {len(sections)} sections, {n} paragraphes ; noms développés : {sc.COMPTE[0]}")


if __name__ == "__main__":
    main(sys.argv[1])
