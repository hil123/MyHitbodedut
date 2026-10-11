# -*- coding: utf-8 -*-
"""
Construit android/app/src/main/res/raw/villes.json (10/10/2026) : les villes
proposées pour les zmanim, quand on ne prend pas la position du téléphone.

Source : GeoNames (www.geonames.org, licence CC-BY 4.0), lue dans le fichier
cities15000.json du paquet PyPI geonamescache 3.0.2 (licence MIT), parce que
download.geonames.org n'était pas joignable depuis l'environnement de
construction. Chaque ville est désignée par son identifiant GeoNames, jamais
par son nom : « Lakewood » ou « London » existent en plusieurs pays.

Coordonnées et fuseau IANA viennent tels quels du fichier ; rien n'est tapé à
la main. Le nom hébreu est choisi parmi les noms alternatifs que GeoNames
donne pour la ville, et l'outil échoue s'il n'y figure pas. Les noms
français et anglais sont des exonymes courants.

Le fichier ne donne pas l'altitude : elle vaut 0. Le profil de zmanim calcule
au niveau de la mer (voir ZMANIM.md), l'altitude n'y entre pas.

  python3 tools/villes_construction.py <cities15000.json de geonamescache>
"""
import json
import os
import sys

# (identifiant GeoNames, hébreu, français, anglais), dans l'ordre affiché :
# Israël d'abord, puis la diaspora par région.
VILLES = [
    (281184, "ירושלים", "Jérusalem", "Jerusalem"),
    (295514, "בני ברק", "Bné Brak", "Bnei Brak"),
    (293397, "תל אביב", "Tel Aviv", "Tel Aviv"),
    (294801, "חיפה", "Haïfa", "Haifa"),
    (293100, "צפת", "Safed", "Safed"),
    (293322, "טבריה", "Tibériade", "Tiberias"),
    (294071, "נתניה", "Netanya", "Netanya"),
    (295629, "אשדוד", "Ashdod", "Ashdod"),
    (295530, "באר שבע", "Beer-Sheva", "Beersheba"),
    (293918, "פתח תקווה", "Petah Tikva", "Petah Tikva"),
    (282926, "מודיעין", "Modiin", "Modiin"),
    (293788, "רמת גן", "Ramat Gan", "Ramat Gan"),
    (293703, "ראשון לציון", "Rishon LeZion", "Rishon LeZion"),
    (295548, "בת ים", "Bat Yam", "Bat Yam"),
    (294068, "נתיבות", "Netivot", "Netivot"),
    (295277, "אילת", "Eilat", "Eilat"),
    (2988507, "פריז", "Paris", "Paris"),
    (2976043, "סרסל", "Sarcelles", "Sarcelles"),
    (2995469, "מרסיי", "Marseille", "Marseille"),
    (2996944, "ליון", "Lyon", "Lyon"),
    (2990440, "ניס", "Nice", "Nice"),
    (2973783, "שטרסבורג", "Strasbourg", "Strasbourg"),
    (2972315, "טולוז", "Toulouse", "Toulouse"),
    (2800866, "בריסל", "Bruxelles", "Brussels"),
    (2803138, "אנטוורפן", "Anvers", "Antwerp"),
    (2660646, "זנבה", "Genève", "Geneva"),
    (2657896, "ציריך", "Zurich", "Zurich"),
    (2643743, "לונדון", "Londres", "London"),
    (2643123, "מנצ'סטר", "Manchester", "Manchester"),
    (3173435, "מילאנו", "Milan", "Milan"),
    (3169070, "רומא", "Rome", "Rome"),
    (2950159, "ברלין", "Berlin", "Berlin"),
    (690688, "אומן", "Ouman", "Uman"),
    (703448, "קייב", "Kiev", "Kyiv"),
    (524901, "מוסקבה", "Moscou", "Moscow"),
    (2553604, "קזבלנקה", "Casablanca", "Casablanca"),
    (5128581, "ניו יורק", "New York", "New York"),
    (5100280, None, "Lakewood (New Jersey)", "Lakewood (New Jersey)"),
    (5368361, "לוס אנגלס", "Los Angeles", "Los Angeles"),
    (4164138, "מיאמי", "Miami", "Miami"),
    (4887398, "שיקגו", "Chicago", "Chicago"),
    (4347778, "בולטימור", "Baltimore", "Baltimore"),
    (6077243, "מונטריאול", "Montréal", "Montreal"),
    (6167865, "טורונטו", "Toronto", "Toronto"),
    (3530597, "מקסיקו סיטי", "Mexico", "Mexico City"),
    (3703443, "פנמה סיטי", "Panama", "Panama City"),
    (3448439, "סאו פאולו", "São Paulo", "São Paulo"),
    (3435910, "בואנוס איירס", "Buenos Aires", "Buenos Aires"),
    (993800, "יוהנסבורג", "Johannesburg", "Johannesburg"),
    (2158177, "מלבורן", "Melbourne", "Melbourne"),
    (2147714, "סידני", "Sydney", "Sydney"),
]

# Ville dont aucun nom alternatif hébreu ne désigne sans ambiguïté celle-ci :
# GeoNames donne « לייקווד » au Lakewood du Colorado, pas à celui du New
# Jersey. Le nom hébreu est alors la translittération, déclarée ici.
SANS_NOM_HEBREU = {5100280: "לייקווד (ניו ג'רזי)"}


def main(chemin):
    donnees = json.load(open(chemin, encoding="utf-8"))
    sortie = []
    for gid, he, fr, en in VILLES:
        c = donnees[str(gid)]
        if he is None:
            he = SANS_NOM_HEBREU[gid]
        elif he not in c.get("alternatenames", []):
            sys.exit(f"{gid} {c['name']} : « {he} » n'est pas un nom GeoNames de cette ville")
        sortie.append({
            "id": gid,
            "nom": {"he": he, "fr": fr, "en": en},
            "pays": c["countrycode"],
            "latitude": c["latitude"],
            "longitude": c["longitude"],
            "altitude": 0,
            "fuseau": c["timezone"],
        })
    racine = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
    cible = os.path.join(racine, "android", "app", "src", "main", "res", "raw", "villes.json")
    json.dump({
        "_meta": {
            "source": "GeoNames (www.geonames.org), CC-BY 4.0, via geonamescache 3.0.2 (cities15000.json)",
            "licence": "CC-BY 4.0 : attribution « GeoNames » affichée dans les réglages",
            "altitude": "absente de la source : 0, le profil calcule au niveau de la mer",
            "outil": "tools/villes_construction.py",
        },
        "villes": sortie,
    }, open(cible, "w", encoding="utf-8"), ensure_ascii=False, indent=1)
    print(f"{len(sortie)} villes → {cible}")


if __name__ == "__main__":
    main(sys.argv[1])
