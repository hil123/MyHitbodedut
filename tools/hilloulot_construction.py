# -*- coding: utf-8 -*-
"""
Construit android/app/src/main/res/raw/hilloulot.json (demande du 11/10/2026) :
les hilloulot (jours de décès de tsadikim et de 'hakhamim), par jour hébreu.

Sources, lues le 11/10/2026 et gardées dans tools/hilloulot/ (lots 1 à 4) :
  - Wikipédia hébreu, section « נפטרו » des pages de date (CC BY-SA 4.0) ;
  - החכם היומי (hyomi.org.il, Kol Israël 'Haverim), pages par mois.
Seuls des faits sont repris : nom, jour, année, lien. Aucun texte.
Les figures non rabbiniques ont été écartées à la lecture ; les cas
douteux gardent leur raison dans « d ».

Règle du propriétaire (11/10) : quand les deux sources divergent pour une
même personne, החכם היומי l'emporte (lots 2 et 3 dès la lecture ; lots 1 et
4 ci-dessous, cas relevés à la main).

Clé des jours : « <mois>-<jour> », mois de KosherJava (1 Nissan … 6 Eloul,
7 Tichri … 11 Chevat), « adar », « adar1 », « adar2 » pour Adar : un
« adar » sans précision se dit en Adar II les années embolismiques
(Choul'han Aroukh OH 568:7, usage séfarade, celui de החכם היומי).

  python3 tools/hilloulot_construction.py
"""
import json, os, re

ICI = os.path.dirname(os.path.abspath(__file__))
MOIS = {"ניסן": "1", "אייר": "2", "סיוון": "3", "סיון": "3", "תמוז": "4", "אב": "5", "אלול": "6",
        "תשרי": "7", "חשוון": "8", "חשון": "8", "מרחשוון": "8", "כסלו": "9", "טבת": "10", "שבט": "11",
        "אדר": "adar", "אדר א׳": "adar1", "אדר א'": "adar1", "אדר א": "adar1",
        "אדר ב׳": "adar2", "אדר ב'": "adar2", "אדר ב": "adar2"}

# Désaccords des lots 1 et 4 : (mois, jour, début du nom) des entrées
# Wikipédia retirées au profit de החכם היומי, et années corrigées.
RETIRES = [("תשרי", 10, "שמואל דוד לוצאטו"), ("תשרי", 11, "הרב פרץ מימון"),
           ("חשוון", 28, "הרב חיים רחמים יוסף פרנקו"), ("כסלו", 1, "הרב יוסף כנאפו"),
           ("אלול", 4, "הרב משה עידאן"), ("אלול", 19, "רבי חיים בנבנישתי")]
ANNEES = {("תשרי", 20, "רבי אליעזר פאפו"): 'תקפ"ח', ("אלול", 6, "רבי ישועה שמעון חיים עובדיה"): "1952",
          ("אלול", 9, "רבי רפאל טריוויש"): 'תקל"ג', ("אלול", 23, "הרב רחמים אריה אברהם ארייאש"): "1893"}


def main():
    jours, total, retires = {}, 0, 0
    for lot in (1, 2, 3, 4):
        for e in json.load(open(os.path.join(ICI, "hilloulot", f"hilloulot_{lot}.json"), encoding="utf-8")):
            if any(e["mois"] == m and e["jour"] == j and e["he"].startswith(n) for m, j, n in RETIRES):
                retires += 1
                continue
            for (m, j, n), a in ANNEES.items():
                if e["mois"] == m and e["jour"] == j and e["he"].startswith(n):
                    e["doute"] = (e.get("doute") or "") + f" ; année de החכם היומי retenue ({a}) au lieu de {e['annee']}"
                    e["annee"] = a
            cle = f"{MOIS[e['mois'].strip()]}-{int(e['jour'])}"
            sources = sorted({"w" if "wikipedia" in s else "h" for s in e["sources"]})
            item = {"he": e["he"].strip(), "a": e.get("annee") or "", "s": "".join(sources)}
            for k in ("fr", "en"):
                if e.get(k):
                    item[k] = e[k]
            if e.get("doute"):
                item["d"] = e["doute"].strip(" ;")
            jours.setdefault(cle, []).append(item)
            total += 1
    sortie = {
        "_meta": {
            "date": "2026-10-11",
            "sources": ["Wikipédia hébreu, pages de date, section נפטרו (CC BY-SA 4.0)",
                        "החכם היומי, hyomi.org.il (Kol Israël 'Haverim) : faits seuls (nom, date)"],
            "s": "w = Wikipédia, h = החכם היומי",
            "regles": "Désaccord : החכם היומי l'emporte. « adar » : Adar II les années embolismiques (OH 568:7). "
                      "Un 30 absent cette année se dit le 29.",
            "d": "raison d'un doute (personne, date ou année) relevée à la lecture",
        },
        "jours": dict(sorted(jours.items())),
    }
    chemin = os.path.join(ICI, "..", "android", "app", "src", "main", "res", "raw", "hilloulot.json")
    json.dump(sortie, open(chemin, "w", encoding="utf-8"), ensure_ascii=False, separators=(",", ":"))
    print(f"{total} hilloulot, {len(jours)} jours, {retires} entrées Wikipédia retirées ; {os.path.getsize(chemin)//1024} Ko")


if __name__ == "__main__":
    main()
