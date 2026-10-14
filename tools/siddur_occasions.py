# -*- coding: utf-8 -*-
"""
Ajoute aux trois siddourim (android/app/src/main/res/raw/siddur_*.json) le
groupe « בְּרָכוֹת לַזְּמַנִּים וְלַמִּצְווֹת » (demande du 14/10 : « toutes les
prières style ברכת האילנות ») : Birkat haIlanot, les bénédictions de la vue et
de l'ouïe, celles des mitsvot, 'Hanouka, la Méguila, l'omer, Chabbat…

Outil additif : les textes déjà construits par tools/siddur_construction.py
(et retouchés par les outils qui le suivent) ne sont pas touchés ; ce groupe-ci
est retiré puis remis à chaque passage. Mêmes fonctions d'extraction, donc même
traitement : prière vocalisée ou consigne, le Nom en toutes lettres.

Sources (export public de Sefaria), du domaine public :
  - Edot HaMizrach : version « merged » (Torat Emet 357 ; Shaliehsaboo, CC0) ;
  - Sefard : Torat Emet 357 ;
  - Ashkenaz : Daat (pour 'Hanouka, le loulav, la soukka) ; le reste n'y est
    pas, ou seulement en listes non vocalisées : repris du Sefard (Torat Emet),
    et dit (« nusah »), comme le fait déjà siddur_construction.py.
Ce qu'un nusah n'a pas dans ses sources libres est emprunté à un autre et marqué.

  python3 tools/siddur_occasions.py <dossier des exports Sefaria>
"""
import json
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import siddur_construction as sc  # noqa: E402

RAW = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "android", "app", "src", "main", "res", "raw")
GROUPE = {"cle": "occasions", "titre": sc.T("בְּרָכוֹת לַזְּמַנִּים וְלַמִּצְווֹת", "Bénédictions des occasions", "Blessings for occasions")}

# id : titre, chemin Edot, chemin Sefard, chemin Ashkenaz (Daat) ; None = emprunté.
TEXTES = [
    ("birkat_hailanot", sc.T("בִּרְכַּת הָאִילָנוֹת", "Birkat haIlanot", "Blessing of the trees"),
     ["Nissan", "Blessing of the Trees"], ["Blessings", "Blossoming Fruit Tree"], None),
    ("keshet", sc.T("בִּרְכַּת הַקֶּשֶׁת", "L’arc-en-ciel", "The rainbow"),
     ["Assorted Blessings and Prayers", "Rainbow"], ["Blessings", "Seeing Rainbow"], None),
    ("barakim", sc.T("בְּרָקִים וּרְעָמִים", "Éclairs et tonnerre", "Lightning and thunder"),
     ["Assorted Blessings and Prayers", "Blessings on Lighting and Thunder"], ["Blessings", "Lightning & Thunder"], None),
    ("yam", sc.T("הָרוֹאֶה אֶת הַיָּם", "La mer", "Seeing the sea"),
     None, ["Blessings", "Seeing Ocean"], None),
    ("nehenin", sc.T("בִּרְכוֹת הַנֶּהֱנִין", "Bénédictions des plaisirs", "Blessings on enjoyments"),
     ["Blessings on Enjoyments"],
     [["Blessings", "Ha'etz"], ["Blessings", "Ha'adamah"], ["Blessings", "Shehakol"], ["Blessings", "Shehecheyanu"],
      ["Blessings", "Fragrant Spices"], ["Blessings", "Fragrant Herbs"], ["Blessings", "Fragrant Shrubs"],
      ["Blessings", "Fragrant Fruit"], ["Blessings", "Fragrant Oils"]], None),
    ("hagomel", sc.T("בִּרְכַּת הַגּוֹמֵל", "HaGomel", "HaGomel"),
     ["Shabbat Shacharit", "HaGomel"], ["Shabbat Morning Services", "Thanksgiving Blessing"], None),
    ("mezouza", sc.T("בִּרְכַּת הַמְּזוּזָה", "La mezouza", "Mezuzah"),
     ["Assorted Blessings and Prayers", "Mezuza"], ["Blessings", "Mezuzah"], None),
    ("hala", sc.T("הַפְרָשַׁת חַלָּה", "Prélever la ’hala", "Separating challah"),
     ["Assorted Blessings and Prayers", "Separating Hallah"], ["Blessings", "Separating Challah"], None),
    ("tevilat_kelim", sc.T("טְבִילַת כֵּלִים", "Tévilat kélim", "Immersing utensils"),
     ["Assorted Blessings and Prayers", "Tevillat Kelim"], ["Blessings", "Immersing Utensils"], None),
    ("maakeh", sc.T("בִּרְכַּת הַמַּעֲקֶה", "Le parapet", "Building a parapet"),
     ["Assorted Blessings and Prayers", "Building a Fence"], None, None),
    ("maasrot", sc.T("תְּרוּמוֹת וּמַעַשְׂרוֹת", "Téroumot et maasrot", "Terumot and maasrot"),
     ["Assorted Blessings and Prayers", "Separating Tithes"], ["Blessings", "Separating Terumot & Maasrot"], None),
    ("refoua", sc.T("תְּפִלַּת הַנּוֹטֵל תְּרוּפָה", "Avant un remède", "Before taking medicine"),
     ["Assorted Blessings and Prayers", "Prayer for Taking Medicine"], None, None),
    ("nerot_hanouka", sc.T("הַדְלָקַת נֵרוֹת חֲנֻכָּה", "Allumage de ’Hanouka", "Hanukkah candles"),
     ["Hanukkah", "Menorah Lighting"], ["Chanukah", "Menorah Lighting"],
     ["Festivals", "Chanukah", "Service for Lighting Chanukah Candles"]),
    ("megila", sc.T("קְרִיאַת הַמְּגִלָּה", "Lecture de la Méguila", "Megillah reading"),
     ["Purim", "Megillah Reading"], ["Purim", "Megillah Reading"], None),
    ("sefirat_haomer", sc.T("סְפִירַת הָעֹמֶר", "Compte de l’omer", "Counting the Omer"),
     ["Counting of the Omer"], ["Weekday Maariv", "Sefirat HaOmer"], None),
    ("soukka", sc.T("הַכְּנִיסָה לַסֻּכָּה", "Entrée dans la soukka", "Entering the sukkah"),
     None, ["Holidays", "Prayer Upon Entering Sukkah"], ["Festivals", "Sukkot", "Prayers in the Sukkah"]),
    ("loulav", sc.T("נְטִילַת לוּלָב", "Le loulav", "Lulav"),
     None, ["Shaking Lulav"], ["Festivals", "Sukkot", "Blessing on Lulav"]),
    ("bedikat_hamets", sc.T("בְּדִיקַת וּבִעוּר חָמֵץ", "Recherche du ’hamets", "Searching for chametz"),
     None, [["Nissan", "Search for Hametz"], ["Nissan", "Burning Hametz"]], None),
    ("erouv_tavchilin", sc.T("עֵרוּב תַּבְשִׁילִין", "Érouv tavchilin", "Eruv tavshilin"),
     None, ["Eruv Tavshilin"], None),
    ("nerot_chabbat", sc.T("הַדְלָקַת נֵרוֹת שַׁבָּת", "Bougies de Chabbat", "Shabbat candles"),
     ["Shabbat Candle Lighting"], ["Shabbat Candle Lighting"], None),
    ("havdala", sc.T("הַבְדָּלָה", "Havdala", "Havdalah"),
     ["Havdalah", "Havdala"], ["Motzaei Shabbat ", "Havdala"], None),
]


def extrait(source, chemin):
    if chemin is None:
        return None
    if isinstance(chemin[0], list):
        return source.plusieurs(chemin)
    return source.sections(chemin)


def main(dossier):
    em = sc.charge(dossier, "Siddur_Edot_HaMizrach.json")
    sf = sc.charge(dossier, "Siddur_Sefard.json")
    asm = sc.charge(dossier, "Siddur_Ashkenaz.json")
    EM = sc.Source(em["text"], em["schema"], "Siddur Edot HaMizrach (Torat Emet 357, Public Domain; Shaliehsaboo Edition, CC0)")
    ST = sc.Source(sc.charge(dossier, "Siddur_Sefard__Hebrew__Torat_Emet_357.json")["text"], sf["schema"], "Siddur Sefard, Torat Emet 357 (Public Domain)")
    AD = sc.Source(sc.charge(dossier, "Ashk__Daat_Siddur_Ashkenaz.json")["text"], asm["schema"], "Siddur Ashkenaz, Daat (Public Domain)")
    sources = {"edot": EM, "sefard": ST, "ashkenaz": AD}

    tables = {"edot": {}, "sefard": {}, "ashkenaz": {}}
    for id_, titre, ce, cs, ca in TEXTES:
        propres = {"edot": extrait(EM, ce), "sefard": extrait(ST, cs), "ashkenaz": extrait(AD, ca)}
        for n in tables:
            if propres[n]:
                tables[n][id_] = (titre, sources[n].nom, propres[n], None)
            else:
                # Emprunté : au Sefard pour l'Ashkenaz, à l'Edot pour le Sefard, au Sefard pour l'Edot.
                ordre = {"ashkenaz": ["sefard", "edot"], "sefard": ["edot"], "edot": ["sefard"]}[n]
                autre = next(o for o in ordre if propres[o])
                tables[n][id_] = (titre, sources[autre].nom, propres[autre], autre)

    for n, table in tables.items():
        chemin = os.path.join(RAW, f"siddur_{n}.json")
        d = json.load(open(chemin, encoding="utf-8"))
        d["textes"] = [t for t in d["textes"] if t.get("groupe") != GROUPE["cle"]]
        d["groupes"] = [g for g in d["groupes"] if g["cle"] != GROUPE["cle"]] + [GROUPE]
        for id_, (titre, nom_source, sections, emprunt) in table.items():
            t = {"id": id_, "groupe": GROUPE["cle"], "titre": titre, "source": nom_source, "sections": sections}
            if emprunt:
                t["nusah"] = sc.NUSAH[emprunt]
            d["textes"].append(t)
            if nom_source not in d["_meta"]["sources"]:
                d["_meta"]["sources"] = sorted(d["_meta"]["sources"] + [nom_source])
        json.dump(d, open(chemin, "w", encoding="utf-8"), ensure_ascii=False, separators=(",", ":"))
        empruntes = [i for i, v in table.items() if v[3]]
        print(f"{n}: {len(table)} textes ajoutés, {sum(len(s['blocs']) for v in table.values() for s in v[2])} paragraphes ; empruntés : {empruntes}")


if __name__ == "__main__":
    main(sys.argv[1])
