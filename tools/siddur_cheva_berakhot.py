# -*- coding: utf-8 -*-
"""
Chéva bérakhot dans l'ordre de la table (demande du 28/10/2026) : « בורא פרי
הגפן » en dernier, sur le kos de Birkat haMazon ; sous la 'houppa elle vient
en premier, et une consigne le dit.

Sources (vérifiées le 28/10) :
- Tour, Even haEzer 62, citant Tossafot : « ואומר עליו שבע ברכות ומניחו ונוטל
  הכוס שבירך עליו ברכת המזון ואומר עליו בורא פרי הגפן »
  (https://he.wikisource.org/wiki/טור_מנוקד_אבן_העזר_סב) ;
- Chabad.org, « Sheva Brachot Text and Guide » : sous la 'houppa le vin vient
  avant les six autres, après le repas il vient en septième ;
- Wikipédia (en), « Sheva Brachot », note 7 : même distinction.

Les sources libres du siddur suivent l'ordre de la 'houppa (Edot, Sefard) ;
la version Ashkenaz (Wikipédia) est déjà dans l'ordre de la table. Ne
réordonne que les paragraphes existants, mot pour mot ; idempotent.

  python3 tools/siddur_cheva_berakhot.py
"""
import json, os, re

ICI = os.path.dirname(os.path.abspath(__file__))
RAW = os.path.join(ICI, "..", "android", "app", "src", "main", "res", "raw")
NUSAH = ("edot", "sefard", "ashkenaz")

C_HOUPPA = "תחת החופה מברכים תחילה בורא פרי הגפן. בשבע ברכות שלאחר ברכת המזון אומרים שש הברכות, והמברך ברכת המזון מסיים בבורא פרי הגפן."
C_KOS = "המברך ברכת המזון נוטל הכוס שבירך עליו ומברך:"
C_HOUPPA_SEULE = "בחופה:"
# Les consignes Sefard qui renvoyaient d'un ordre à l'autre : remplacées par les deux ci-dessus.
SEFARD_ANCIENNES = ("בשבע ברכות שלאחר ברכת המזון מתחילים כאן:", "בברכת המזון מברך עתה על הכוס:")


def sans_balises(t):
    return re.sub(r"<[^>]+>", "", t)


def est_gefen(b):
    return "t" in b and "בּוֹרֵא פְּרִי הַג" in sans_balises(b["t"])


def est_savri(b):
    return "t" in b and sans_balises(b["t"]).startswith("סַבְרֵי")


def reordonne(blocs):
    if blocs and blocs[0].get("c") == C_HOUPPA:
        return blocs  # déjà fait
    vin = [b for b in blocs if est_gefen(b) or est_savri(b)]
    reste = [b for b in blocs if b not in vin and b.get("c") not in SEFARD_ANCIENNES]
    # Le reste se coupe en deux : les six bérakhot, puis ce qui suit le vin
    # (goûter le kos, et pour la 'houppa seule, le verre brisé).
    i = next((k for k, b in enumerate(reste) if "c" in b), len(reste))
    six, apres = reste[:i], reste[i:]
    if apres and any("שובר" in b.get("c", "") for b in apres):
        k = next(k for k, b in enumerate(apres) if "שובר" in b.get("c", ""))
        apres = apres[:k] + [{"c": C_HOUPPA_SEULE}] + apres[k:]
    return [{"c": C_HOUPPA}] + six + [{"c": C_KOS}] + vin + apres


def main():
    for n in NUSAH:
        nom = os.path.join(RAW, f"siddur_{n}.json")
        d = json.load(open(nom, encoding="utf-8"))
        t = next(x for x in d["textes"] if x["id"] == "cheva_berakhot")
        for s in t["sections"]:
            if isinstance(s, dict) and any(est_gefen(b) for b in s["blocs"]):
                s["blocs"] = reordonne(s["blocs"])
                print(n, [sans_balises(b.get("t", b.get("c", "")))[:28] for b in s["blocs"]])
        json.dump(d, open(nom, "w", encoding="utf-8"), ensure_ascii=False, separators=(",", ":"))


if __name__ == "__main__":
    main()
