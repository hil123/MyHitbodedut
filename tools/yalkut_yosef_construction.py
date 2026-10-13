# -*- coding: utf-8 -*-
"""
Construit android/app/src/main/res/raw/yalkut_yosef.json (demande du 13/10/2026) :
la halakha du jour tirée du ילקוט יוסף – קיצור שולחן ערוך (מהדורת התשס"ז) du
Rav Yitzhak Yosef, texte préparé par תורת אמת.

Droits (consignés dans FEUILLE_DE_ROUTE.md) : le livre porte « כל הזכויות שמורות
למו"ר הרב יצחק יוסף – אסור להעתיק קטעים מהספר ללא ציון המקור » et, pour la
saisie, « הוכן מתוכנת תורת אמת, מותר להפיץ בכפוף לתנאי רשיון CC BY-NC-SA 2.5,
אסור בשימוש מסחרי ». Le propriétaire de l'application a déclaré le 13/10 :
« droits reçus si textes non copiés dans leur entièreté », puis « je te donne
toutes les autorisations ». D'où une SÉLECTION, jamais le livre entier : les
pages listées ci-dessous (moins d'un vingtième des 1 048), leurs se'ifim courts seulement, et la source affichée
à chaque halakha.

Le fichier du livre sur or-breslev.co.il (lien donné par le propriétaire) n'est
pas joignable depuis l'environnement de construction et sa couche texte PDF est
un OCR bruité ; le même texte Torat Emet est lu, page par page (un siman par
page, mêmes titres), sur itim-latora.org/כתבים/קיצור-שולחן-ערוך-ילקוט-יוסף/?page=N.
Le texte est repris tel quel : lettre du se'if à part, renvois entre crochets
conservés.

Les périodes (choix de l'application, pas du livre) : la veille et les jours de
fête prennent les lois de la fête ; les vendredis ordinaires, celles du Chabbat ;
les autres jours, la vie de chaque jour (lever, tsitsit, tefillin, bénédictions,
prière, repas).

  python3 tools/yalkut_yosef_construction.py <dossier des pages pN.html>
"""
import html, json, os, re, sys, unicodedata

ICI = os.path.dirname(os.path.abspath(__file__))
SORTIE = os.path.join(ICI, "..", "android", "app", "src", "main", "res", "raw", "yalkut_yosef.json")
TOTAL_PAGES = 1048  # pages du livre sur le site (sommaire relevé le 13/10)
# « Des halakhot courtes chaque jour » (demande du 13/10) : seuls les se'ifim de
# cette longueur au plus (renvois entre crochets compris) sont retenus.
LONGUEUR_MAX = 420

PERIODES = {
    # La vie de chaque jour : lever, tsitsit, tefillin, bénédictions, prière, repas.
    "ordinaire": [1, 3, 5, 8, 10, 19, 28, 33, 41, 52, 54, 67, 84, 88, 95, 100, 123],
    # Les vendredis : veille de Chabbat, bougies, prière du vendredi soir.
    "chabbat": [246, 252, 255, 264],
    "roch_hodech": [432, 434, 440],
    # Du 16 Adar au 22 Nissan.
    "pessah": [449, 466, 478, 482],
    # Du 23 Nissan au 7 Sivan : l'omer et Chavouot.
    "omer": [500, 503, 504],
    # Du 17 Tamouz au 9 Av.
    "bein_hametsarim": [544, 545, 550],
    # Du 1er Eloul au 2 Tichri.
    "eloul": [558, 566, 569],
    # Du 3 au 10 Tichri.
    "kippour": [580, 587],
    # Du 11 au 23 Tichri.
    "soukkot": [606, 617, 629],
    # Du 15 Kislev au 3 Tevet.
    "hanouka": [651, 654, 658],
    # Du 1er au 15 Chevat.
    "tou_bichvat": [666],
    # Du 1er au 15 Adar (Adar II les années embolismiques).
    "pourim": [672, 676, 684],
}

NFC = lambda s: unicodedata.normalize("NFC", s)


def texte(fragment):
    s = re.sub(r"<[^>]+>", "", fragment)
    s = html.unescape(s).replace("\xa0", " ")
    return NFC(re.sub(r"\s+", " ", s).strip())


def page(chemin):
    s = open(chemin, encoding="utf-8").read()
    debut = s.find("book-container")
    fin = s.find("widget text-widget", debut)
    if debut < 0 or fin < 0:
        raise SystemExit(f"{chemin} : pas de corps de livre")
    corps = s[debut:fin]
    corps = corps[:corps.find('<a class="btn ed_btn')] if '<a class="btn ed_btn' in corps else corps
    titre, items = None, []
    for m in re.finditer(r'<h1[^>]*>(.*?)</h1>|<div id="normal-\d+-div" class="normal">(.*?)</div>', corps, flags=re.S):
        if m.group(1) is not None:
            titre = texte(m.group(1))
            continue
        brut = m.group(2)
        lettre = re.match(r'\s*<span class="default-paragraph-font"><strong>([^<]*)</strong>', brut)
        t = texte(brut)
        if not t:
            continue
        l = ""
        if lettre:
            l = lettre.group(1).strip().rstrip(".").strip()
            # Le texte commence par la lettre et son point : on les retire.
            t = re.sub(r"^" + re.escape(l) + r"\s*\.\s*", "", t)
        items.append({"s": titre or "", "l": l, "t": t})
    if not items:
        raise SystemExit(f"{chemin} : aucun se'if")
    return items


def main(dossier):
    sortie = {}
    pages = 0
    for nom, liste in PERIODES.items():
        tous = []
        for n in liste:
            tous += [dict(it, p=n) for it in page(os.path.join(dossier, f"p{n}.html")) if len(it["t"]) <= LONGUEUR_MAX]
            pages += 1
        sortie[nom] = tous
        print(f"{nom:16} {len(liste):3} pages, {len(tous):4} se'ifim")
    assert pages * 10 < TOTAL_PAGES, "la sélection doit rester une partie du livre"
    doc = {
        "_meta": {
            "titre": "ילקוט יוסף – קיצור שולחן ערוך",
            "auteur": "מו\"ר הרב יצחק יוסף",
            "edition": "מהדורת התשס\"ז, הוכן מתוכנת תורת אמת",
            "licence": "כל הזכויות שמורות למו\"ר הרב יצחק יוסף ; saisie תורת אמת CC BY-NC-SA 2.5 (usage non commercial)",
            "droits": "sélection, avec l'accord déclaré par le propriétaire de l'application le 13/10/2026 (textes non copiés dans leur entièreté)",
            "selection": f"{pages} pages sur {TOTAL_PAGES}, se'ifim de {LONGUEUR_MAX} caractères au plus",
            "lu_sur": "https://itim-latora.org/כתבים/קיצור-שולחן-ערוך-ילקוט-יוסף/?page=N",
            "outil": "tools/yalkut_yosef_construction.py",
        },
        "periodes": sortie,
    }
    with open(SORTIE, "w", encoding="utf-8") as f:
        json.dump(doc, f, ensure_ascii=False, separators=(",", ":"))
    print(f"{pages} pages sur {TOTAL_PAGES} -> {os.path.relpath(SORTIE)} ({os.path.getsize(SORTIE) // 1024} Ko)")


if __name__ == "__main__":
    main(sys.argv[1])
