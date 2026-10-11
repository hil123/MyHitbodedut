# -*- coding: utf-8 -*-
"""
Le nikkoud des kavanot sur le Nom des 'hatimot de la Amida (demande du
11/10/2026) : le siddour Sfard de Sefaria (Torat Emet, Metsudah) imprime
יְהֹוָה à chaque 'hatima ; le siddour Edot HaMizra'h (Torat Emet 357) donne
le nikkoud propre à chaque bénédiction, selon les kavanot de l'Arizal. On le
reporte, tel quel et lettre pour lettre, sur les 'hatimot du siddour Sfard
(Amidot de semaine, Moussaf de Roch 'Hodech). Règle du propriétaire : un
nikkoud du Nom n'entre qu'après comparaison avec au moins trois sources.

Le nikkoud n'est pas retapé : il est relu dans siddur_edot.json.

  python3 tools/noms_kavanot.py
"""
import json, re, sys

RAW = "android/app/src/main/res/raw/"
SIGNES = "[֑-ׇ]*"
NOM = f"(?:י{SIGNES}ה{SIGNES}ו{SIGNES}ה{SIGNES}|יוּהוּווּהוּ)"
BAROUKH = re.compile(rf"(ב{SIGNES}ר{SIGNES}ו{SIGNES}ך{SIGNES}(?:</b>)?\s+(?:<b>)?א{SIGNES}ת{SIGNES}ה{SIGNES}\s*)({NOM})(?![א-ת])")
nu = lambda s: re.sub(r"[֑-ׇ]", "", re.sub(r"<[^>]+>", "", s))

# Le début de la 'hatima (sans voyelles) → la 'hatima de référence dans Edot.
# Les variantes d'orthographe et les 'hatimot des Dix jours de pénitence
# (même bénédiction, même nom) renvoient à la forme d'Edot.
CHATIMOT = {
    "מגן אברהם": "מגן אברהם", "מחיה המתים": "מחיה המתים",
    "האל הקדוש": "האל הקדוש", "המלך הקדוש": "האל הקדוש",
    "חונן הדעת": "חונן הדעת", "הרוצה בתשובה": "הרוצה בתשובה",
    "חנון המרבה לסלוח": "חנון המרבה לסלח", "חנון המרבה לסלח": "חנון המרבה לסלח",
    "גואל ישראל": "גואל ישראל", "רופא חולי עמו ישראל": "רופא חולי עמו ישראל",
    "מברך השנים": "מברך השנים", "מקבץ נדחי עמו ישראל": "מקבץ נדחי עמו ישראל",
    "מלך אוהב צדקה ומשפט": "מלך אוהב צדקה ומשפט", "מלך אהב צדקה ומשפט": "מלך אוהב צדקה ומשפט",
    "המלך המשפט": "מלך אוהב צדקה ומשפט",
    "שובר אויבים": "שובר אויבים", "שובר איבים": "שובר אויבים",
    "משען ומבטח לצדיקים": "משען ומבטח לצדיקים", "בונה ירושלים": "בונה ירושלים",
    "מצמיח קרן ישועה": "מצמיח קרן ישועה", "שומע תפלה": "שומע תפלה",
    "המחזיר שכינתו לציון": "המחזיר שכינתו לציון", "הטוב שמך": "הטוב שמך",
    "המברך את עמו ישראל בשלום": "המברך את עמו ישראל בשלום",
    "מקדש ישראל וראשי חדשים": "מקדש ישראל וראשי חדשים",
}


def suite(texte, fin):
    """Les mots qui suivent le Nom, sans voyelles ni ponctuation."""
    # Le maqaf (« אֶת־עַמּוֹ ») sépare deux mots.
    return re.sub(r"[^א-ת ]", "", nu(texte[fin:fin + 120].replace("־", " "))).split()


def cle(mots):
    phrase = " ".join(mots)
    return next((CHATIMOT[k] for k in sorted(CHATIMOT, key=len, reverse=True) if phrase.startswith(k)), None)


def reference():
    """Edot : la 'hatima de référence → le Nom tel qu'il y est vocalisé (Amida de Cha'harit et Moussaf)."""
    d = json.load(open(RAW + "siddur_edot.json", encoding="utf-8"))
    table = {}
    for t in d["textes"]:
        if t["id"] not in ("chaharit", "moussaf_rosh_hodech"):
            continue
        for s in t["sections"]:
            if t["id"] == "chaharit" and s["titre"] != "עמידה":
                continue
            for b in s["blocs"]:
                x = b.get("t", "")
                for m in BAROUKH.finditer(x):
                    k = cle(suite(x, m.end()))
                    if k:
                        table.setdefault(k, m.group(2))
    manque = sorted(set(CHATIMOT.values()) - set(table))
    if manque:
        raise SystemExit(f"Edot : 'hatimot sans nikkoud de référence : {manque}")
    return table


def applique(table, fichier, textes):
    d = json.load(open(RAW + fichier, encoding="utf-8"))
    compte = 0
    for t in d["textes"]:
        if t["id"] not in textes:
            continue
        for s in t["sections"]:
            if t["id"] != "moussaf_rosh_hodech" and "עמידה" not in s["titre"]:
                continue
            for b in s["blocs"]:
                if "t" not in b:
                    continue
                def remplace(m):
                    nonlocal compte
                    k = cle(suite(b["t"], m.end()))
                    if not k:
                        return m.group(0)
                    compte += 1
                    return m.group(1) + table[k]
                b["t"] = BAROUKH.sub(remplace, b["t"])
    json.dump(d, open(RAW + fichier, "w", encoding="utf-8"), ensure_ascii=False, separators=(",", ":"))
    return compte


if __name__ == "__main__":
    table = reference()
    for k, v in table.items():
        print(f"  {k:28} {v}")
    n = applique(table, "siddur_sefard.json", ("chaharit", "minha", "arvit", "moussaf_rosh_hodech"))
    print(f"Sfard : {n} 'hatimot vocalisées")
