# -*- coding: utf-8 -*-
"""
Construit android/app/src/main/res/raw/aeroports_noms.json (28/10/2026) : les
noms en hébreu et en français des aéroports de aeroports.json, pour que la
recherche d'aéroport (Aeroports.chercher, vol.js chercher) trouve « תל אביב »,
« מלבורן » ou « Londres », et que la liste les affiche dans la langue de
l'application.

Source : Wikidata (CC0, domaine public), interrogée à la construction
seulement (query.wikidata.org) ; l'application n'appelle rien.
  - l'élément qui porte le code IATA (P238) ;
  - son libellé hébreu et français ;
  - les libellés hébreu et français des lieux desservis (P931).

Un code IATA peut être porté par plusieurs éléments (aéroport fermé, gare,
base aérienne). On ne garde que l'élément dont les coordonnées (P625) sont à
moins de MAX_KM de celles d'OurAirports ; sans coordonnées, il est écarté.
Le nombre d'écartés est imprimé.

Les lieux desservis peuvent être larges (TLV dessert Tel Aviv et Jérusalem
selon Wikidata) : ils servent à la recherche seulement, l'affichage prend le
libellé de l'aéroport.

  python3 -I tools/aeroports_noms.py
"""
import json
import math
import os
import sys
import time
import urllib.parse
import urllib.request

MAX_KM = 30
LANGUES = ("he", "fr")
PAQUET = 300


def km(la1, lo1, la2, lo2):
    p1, p2 = math.radians(la1), math.radians(la2)
    d = math.sin((p2 - p1) / 2) ** 2 + math.cos(p1) * math.cos(p2) * math.sin(math.radians(lo2 - lo1) / 2) ** 2
    return 2 * 6371 * math.asin(math.sqrt(d))


def requete(codes):
    valeurs = " ".join(f'"{c}"' for c in codes)
    libelles = "".join(
        f' OPTIONAL {{ ?a rdfs:label ?a_{l} FILTER(lang(?a_{l})="{l}") }}'
        f' OPTIONAL {{ ?a wdt:P931 ?v . ?v rdfs:label ?v_{l} FILTER(lang(?v_{l})="{l}") }}' for l in LANGUES)
    sparql = f"SELECT ?iata ?a ?coord {' '.join(f'?a_{l} ?v_{l}' for l in LANGUES)} WHERE {{ VALUES ?iata {{ {valeurs} }} ?a wdt:P238 ?iata . OPTIONAL {{ ?a wdt:P625 ?coord }}{libelles} }}"
    req = urllib.request.Request(
        "https://query.wikidata.org/sparql",
        data=urllib.parse.urlencode({"query": sparql, "format": "json"}).encode(),
        headers={"User-Agent": "MyHitbodedut-build/1.0 (https://github.com/hil123/MyHitbodedut)", "Accept": "application/sparql-results+json"})
    return json.load(urllib.request.urlopen(req, timeout=180))["results"]["bindings"]


def point(wkt):
    # « Point(34.8867 32.0114) » : longitude, puis latitude.
    lon, lat = wkt[wkt.index("(") + 1:wkt.index(")")].split()
    return float(lat), float(lon)


def main():
    racine = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    brut = os.path.join(racine, "android", "app", "src", "main", "res", "raw")
    aeroports = {a[0]: a for a in json.load(open(os.path.join(brut, "aeroports.json"), encoding="utf-8"))["aeroports"]}
    codes = sorted(aeroports)
    lignes = []
    for i in range(0, len(codes), PAQUET):
        for essai in range(5):
            try:
                lignes += requete(codes[i:i + PAQUET])
                break
            except Exception as e:  # 502, 429 : le service public est chargé ; on réessaie.
                print("nouvel essai", i, e, file=sys.stderr)
                time.sleep(10 * (essai + 1))
        else:
            sys.exit(f"Wikidata ne répond pas (paquet {i})")

    noms, ecartes = {}, set()
    for r in lignes:
        iata = r["iata"]["value"]
        a = aeroports[iata]
        if "coord" not in r or km(a[4], a[5], *point(r["coord"]["value"])) > MAX_KM:
            ecartes.add((iata, r["a"]["value"]))
            continue
        n = noms.setdefault(iata, {l: {"nom": None, "villes": []} for l in LANGUES})
        for l in LANGUES:
            if f"a_{l}" in r:
                n[l]["nom"] = n[l]["nom"] or r[f"a_{l}"]["value"]
            v = r.get(f"v_{l}", {}).get("value")
            if v and v not in n[l]["villes"]:
                n[l]["villes"].append(v)
    # Une ligne compacte par aéroport : [nom he, [villes he], nom fr, [villes fr]] ; "" si absent.
    sortie = {}
    for iata in sorted(noms):
        ligne = []
        for l in LANGUES:
            ligne += [noms[iata][l]["nom"] or "", sorted(noms[iata][l]["villes"])]
        if any(ligne):
            sortie[iata] = ligne
    cible = os.path.join(brut, "aeroports_noms.json")
    with open(cible, "w", encoding="utf-8") as f:
        json.dump({
            "_meta": {
                "source": "Wikidata (CC0), libellés de l'élément au code IATA (P238) et des lieux desservis (P931)",
                "controle": f"coordonnées (P625) à moins de {MAX_KM} km de celles d'OurAirports",
                "colonnes": ["nom_he", "villes_he", "nom_fr", "villes_fr"],
                "outil": "tools/aeroports_noms.py",
            },
            "noms": sortie,
        }, f, ensure_ascii=False, separators=(",", ":"))
        f.write("\n")
    avec_he = sum(1 for v in sortie.values() if v[0] or v[1])
    print(f"{len(sortie)} aéroports nommés sur {len(codes)} ; {avec_he} en hébreu ; {len(ecartes)} éléments écartés (coordonnées absentes ou à plus de {MAX_KM} km)")


if __name__ == "__main__":
    main()
