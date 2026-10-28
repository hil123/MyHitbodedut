# -*- coding: utf-8 -*-
"""
Construit android/app/src/main/res/raw/aeroports.json (28/10/2026) : les
aéroports proposés pour les zmanim en vol (Vol.kt, web/src/js/vol.js).

Sources, toutes lues hors ligne, rien n'est tapé à la main :
  - OurAirports (ourairports.com/data, airports.csv) : code IATA, nom,
    ville, pays, coordonnées. « All data is released to the Public Domain,
    and comes with no guarantee of accuracy or fitness for use. »
    Retenus : les aéroports « large_airport » et « medium_airport » qui ont
    un code IATA et des vols réguliers (scheduled_service = yes).
  - timezone-boundary-builder 2026e (combined-with-oceans.json, licence
    ODbL) : le fuseau IANA de chaque aéroport, par point dans polygone. Les
    contours servent ici à la construction seulement ; l'application ne les
    embarque pas.
  - GeoNames cities1000 (CC-BY 4.0) : contre-vérification. Le fuseau de la
    localité la plus proche du même pays doit avoir les mêmes décalages UTC
    sur l'année 2026 ; les écarts sont imprimés, et l'outil échoue s'il y en
    a qui ne sont pas listés dans VERIFIES.

Un aéroport que les contours placent en mer (« Etc/… », terre-plein gagné
sur l'eau) prend le fuseau GeoNames, et l'outil l'imprime.

Le fuseau ne sert qu'à convertir en UTC les heures que l'on tape (celles du
billet, à l'heure locale de chaque aéroport) et à les réafficher : aucun
zman n'en dépend, tous sont calculés en UTC au point sous l'avion.

  python3 -I tools/aeroports_construction.py airports.csv combined-with-oceans.json cities1000.txt
"""
import csv
import json
import math
import os
import sys
from collections import defaultdict
from datetime import datetime
from zoneinfo import ZoneInfo

TYPES = ("large_airport", "medium_airport")

# Écarts contours / GeoNames relus un par un (28/10/2026) : les contours
# sont retenus. « Loin » : la localité GeoNames la plus proche du même pays
# est à plus de 100 km, elle ne dit rien du fuseau de l'aéroport.
LOIN = "localité GeoNames à plus de 100 km"
VERIFIES = {
    "NAQ": LOIN, "XKS": LOIN, "YKL": LOIN, "YNA": LOIN, "YPL": LOIN, "YRB": LOIN,
    "YSF": LOIN, "YYL": LOIN, "YYQ": LOIN, "ADK": LOIN,
    "PDS": "Coahuila frontalier : America/Matamoros suit l'heure d'été des États-Unis (tzdb)",
    "PVR": "l'aéroport est dans le Jalisco, à l'heure du Centre ; la localité la plus proche est au Nayarit",
    "USR": "Asia/Ust-Nera est le fuseau propre du district d'Oïmiakon (tzdb)",
}

# L'aéroport à cheval sur une frontière vit à l'heure de son aérogare : le
# fuseau GeoNames est retenu.
GEONAMES = {
    "OOL": "piste sur la frontière Queensland / Nouvelle-Galles du Sud, aérogare au Queensland, "
           "heure du Queensland toute l'année (timeanddate.com, acukwik.com/Airport-Info/YBCG)",
}

# Fuseau disputé entre les sources elles-mêmes : l'aéroport n'est pas proposé
# plutôt que de trancher.
XINJIANG = "Xinjiang : les contours donnent Asia/Shanghai et Asia/Urumqi à la fois"
EXCLUS = {
    "AAT": XINJIANG, "AKU": XINJIANG, "BPL": XINJIANG, "FYN": XINJIANG, "HMI": XINJIANG,
    "HQL": XINJIANG, "HTN": XINJIANG, "IQM": XINJIANG, "KHG": XINJIANG, "KJI": XINJIANG,
    "KRL": XINJIANG, "NLT": XINJIANG, "QSZ": XINJIANG, "RQA": XINJIANG, "TLQ": XINJIANG,
    "URC": XINJIANG, "YIN": XINJIANG,
    "SUI": "Abkhazie : les contours donnent Asia/Tbilisi et Europe/Moscow à la fois",
    "LTI": "Govi-Altaï : Asia/Hovd selon la tzdb, l'heure d'Oulan-Bator selon les horaires de vol "
           "(liste tz de l'IANA, juin 2024)",
}


def signature(fuseau, cache={}):
    """Les décalages UTC au 15 de chaque mois de 2026 : deux fuseaux de même signature s'équivalent ici."""
    if fuseau not in cache:
        z = ZoneInfo(fuseau)
        cache[fuseau] = tuple(datetime(2026, m, 15, 12, tzinfo=z).utcoffset() for m in range(1, 13))
    return cache[fuseau]


def km(la1, lo1, la2, lo2):
    p1, p2 = math.radians(la1), math.radians(la2)
    h = math.sin((p2 - p1) / 2) ** 2 + math.cos(p1) * math.cos(p2) * math.sin(math.radians(lo2 - lo1) / 2) ** 2
    return 12742 * math.asin(min(1.0, math.sqrt(h)))


def dans_anneau(x, y, anneau):
    """Lancer de rayon : le point (x = longitude, y = latitude) est-il dans l'anneau ?"""
    dedans = False
    j = len(anneau) - 1
    for i in range(len(anneau)):
        xi, yi = anneau[i][0], anneau[i][1]
        xj, yj = anneau[j][0], anneau[j][1]
        if (yi > y) != (yj > y) and x < (xj - xi) * (y - yi) / (yj - yi) + xi:
            dedans = not dedans
        j = i
    return dedans


def polygones(geometrie):
    if geometrie["type"] == "Polygon":
        return [geometrie["coordinates"]]
    return geometrie["coordinates"]


def charger_contours(chemin):
    zones = []
    for f in json.load(open(chemin, encoding="utf-8"))["features"]:
        for poly in polygones(f["geometry"]):
            xs = [p[0] for p in poly[0]]
            ys = [p[1] for p in poly[0]]
            zones.append((min(xs), max(xs), min(ys), max(ys), poly, f["properties"]["tzid"]))
    return zones


def fuseau_contours(zones, lat, lon):
    trouves = set()
    for x0, x1, y0, y1, poly, tzid in zones:
        if x0 <= lon <= x1 and y0 <= lat <= y1 and dans_anneau(lon, lat, poly[0]) \
                and not any(dans_anneau(lon, lat, trou) for trou in poly[1:]):
            trouves.add(tzid)
    return trouves


def main(aeroports_csv, contours_json, villes_txt):
    par_pays = defaultdict(list)
    for ligne in open(villes_txt, encoding="utf-8"):
        c = ligne.rstrip("\n").split("\t")
        par_pays[c[8]].append((float(c[4]), float(c[5]), c[17]))
    zones = charger_contours(contours_json)

    sortie, ecarts, en_mer = [], [], []
    for a in csv.DictReader(open(aeroports_csv, encoding="utf-8")):
        if a["type"] not in TYPES or not a["iata_code"] or a["scheduled_service"] != "yes":
            continue
        if a["iata_code"] in EXCLUS:
            continue
        lat, lon = float(a["latitude_deg"]), float(a["longitude_deg"])
        proches = sorted((km(lat, lon, la, lo), tz) for la, lo, tz in par_pays[a["iso_country"]])
        geonames = proches[0][1] if proches else None
        trouves = sorted(t for t in fuseau_contours(zones, lat, lon) if not t.startswith("Etc/"))
        if len(trouves) > 1:
            sys.exit(f"{a['iata_code']} : plusieurs fuseaux {trouves}")
        if a["iata_code"] in GEONAMES:
            fuseau = geonames
        elif trouves:
            fuseau = trouves[0]
        elif geonames:
            fuseau = geonames
            en_mer.append(f"{a['iata_code']} {a['name']} → {geonames} (GeoNames)")
        else:
            sys.exit(f"{a['iata_code']} : aucun fuseau")
        if geonames and signature(geonames) != signature(fuseau) and a["iata_code"] not in GEONAMES:
            ecarts.append((a["iata_code"], f"{a['iata_code']} {a['name']} ({a['iso_country']}) : contours {fuseau}, GeoNames {geonames} à {proches[0][0]:.0f} km"))
        sortie.append([a["iata_code"], a["name"], a["municipality"], a["iso_country"],
                       round(lat, 4), round(lon, 4), fuseau])

    for ligne in en_mer:
        print("en mer :", ligne)
    for _, ligne in ecarts:
        print("écart :", ligne)
    non_relus = [code for code, _ in ecarts if code not in VERIFIES]
    if non_relus:
        sys.exit(f"{len(non_relus)} écarts à relire : {' '.join(non_relus)}")

    codes = [a[0] for a in sortie]
    if len(codes) != len(set(codes)):
        sys.exit("code IATA en double")
    sortie.sort(key=lambda a: a[0])
    racine = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
    cible = os.path.join(racine, "android", "app", "src", "main", "res", "raw", "aeroports.json")
    with open(cible, "w", encoding="utf-8") as f:
        json.dump({
            "_meta": {
                "source": "OurAirports (ourairports.com/data), domaine public",
                "fuseaux": "timezone-boundary-builder 2026e (ODbL, © contributeurs OpenStreetMap), contre-vérifiés par GeoNames cities1000 (CC-BY 4.0)",
                "exclus": EXCLUS,
                "colonnes": ["iata", "nom", "ville", "pays", "latitude", "longitude", "fuseau"],
                "outil": "tools/aeroports_construction.py",
            },
            "aeroports": sortie,
        }, f, ensure_ascii=False, separators=(",", ":"))
        f.write("\n")
    print(f"{len(sortie)} aéroports → {cible}")


if __name__ == "__main__":
    main(*sys.argv[1:4])
