# -*- coding: utf-8 -*-
"""
Ajoute le Moussaf de Roch 'Hodech (demande du 11/10/2026) aux trois
siddur_{edot,sefard,ashkenaz}.json existants, sans reconstruire les autres
textes : les pages Yizkor et Chéva bérakhot de Wikipédia, lues le 04/10, ne
sont pas dans l'export de Sefaria. Les sources et le placement sont ceux de
tools/siddur_construction.py (TITRES, TSEROUFIM), qui les produit aussi.

  python3 tools/siddur_moussaf_rosh_hodech.py <dossier de l'export Sefaria>
"""
import json, os, sys
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import siddur_construction as sc

ID = "moussaf_rosh_hodech"


def main(dossier):
    em = sc.charge(dossier, "Siddur_Edot_HaMizrach.json")
    sf = sc.charge(dossier, "Siddur_Sefard.json")
    asch = sc.charge(dossier, "Siddur_Ashkenaz.json")
    EM = sc.Source(em["text"], em["schema"], "Siddur Edot HaMizrach (Torat Emet 357, Public Domain; Shaliehsaboo Edition, CC0)")
    ST = sc.Source(sc.charge(dossier, "Siddur_Sefard__Hebrew__Torat_Emet_357.json")["text"], sf["schema"], "Siddur Sefard, Torat Emet 357 (Public Domain)")
    AD = sc.Source(sc.charge(dossier, "Ashk__Daat_Siddur_Ashkenaz.json")["text"], asch["schema"], "Siddur Ashkenaz, Daat (Public Domain)")
    tables = {
        "edot": (EM, ["Rosh Hodesh", "Mussaf"]),
        "sefard": (ST, ["Rosh Chodesh", "Mussaf"]),
        "ashkenaz": (AD, ["Festivals", "Rosh Chodesh", "Musaf Amidah for Rosh Chodesh"]),
    }
    groupe, titre = sc.TITRES[ID]
    by = "Bnei Yissaschar, Piotrkow 1883 (Sefaria / NLI, Public Domain)"
    for cle, (source, chemin) in tables.items():
        sections = sc.avec_tseroufim(source.sections(chemin))
        nom = f"android/app/src/main/res/raw/siddur_{cle}.json"
        d = json.load(open(nom, encoding="utf-8"))
        textes = [t for t in d["textes"] if t["id"] != ID]
        apres = next(i for i, t in enumerate(textes) if t["id"] == "birkat_halevana") + 1
        textes.insert(apres, {"id": ID, "groupe": groupe, "titre": titre, "source": source.nom, "sections": sections})
        d["textes"] = textes
        d["_meta"]["sources"] = sorted(set(d["_meta"]["sources"]) | {source.nom, by})
        json.dump(d, open(nom, "w", encoding="utf-8"), ensure_ascii=False, separators=(",", ":"))
        n = sum(len(s["blocs"]) for s in sections)
        print(f"{cle}: {len(sections)} sections, {n} paragraphes ; noms développés : {sc.COMPTE[0]}")


if __name__ == "__main__":
    main(sys.argv[1])
