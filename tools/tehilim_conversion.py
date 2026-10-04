# -*- coding: utf-8 -*-
"""
Construit android/app/src/main/res/raw/tehilim.json (04/10/2026).

Source : « Tanach with Nikkud » (tanach.us, domaine public), via l'export
public de Sefaria :
  https://storage.googleapis.com/sefaria-export/json/Tanakh/Writings/Psalms/Hebrew/Tanach%20with%20Nikkud.json

Le qere est retenu ; le Nom reste en toutes lettres (demande du 04/10), comme
dans le Tikoun, que le contrôle compare verset par verset. Usage :

  python3 tools/tehilim_conversion.py chemin/vers/Tanach_with_Nikkud.json

ne fait qu'afficher le contrôle contre le Tikoun ; la génération du fichier
se fait avec --ecrire.
"""
import json, re, unicodedata
NFC = lambda s: unicodedata.normalize("NFC", s)
N = "[\u0591-\u05BD\u05BF\u05C1\u05C2\u05C4\u05C5\u05C7]*"  # nikud et teamim, sans maqaf (05BE), paseq (05C0) ni sof pasouq (05C3)

def qere(v):
    # « ketiv [qere] » : on garde la forme lue.
    # Le ketiv est le dernier mot avant le crochet, maqaf final compris :
    # « מַה־יגיל [יָּגֶל] » garde מַה־.
    v = re.sub(r"([^\s\[\]\u05BE]+\u05BE?) ?\[([^\]]+)\]", r"\2", v)
    # qere sans ketiv : « [mot] » ; ketiv sans qere : « mot [] »
    v = re.sub(r"\S+ \[\]", "", v)
    v = re.sub(r"\[([^\]]+)\]", r"\1", v)
    # Un qere qui finit par un maqaf se lie au mot suivant.
    v = v.replace("\u05BE ", "\u05BE")
    return re.sub(r"\s{2,}", " ", v).strip()

def noms(v):
    v = v.replace("‍", "")
    # Le Tétragramme, avec ou sans préfixe (לַיהוָה, וַיהוָה, מֵיְהוָה…), lu Adonaï ou Elohim.
    v = re.sub(rf"י{N}ה{N}ו{N}ה{N}", "ה'", v)
    # Elohim et ses formes : le ה de la racine devient ק.
    # Il faut le holam, sur le lamed ou par un vav : אֵלֶּה, אָלָה, אֶל־הַ ne sont pas le Nom.
    v = re.sub(rf"(א{N}ל(?:{N}\u05B9{N}|{N}ו{N}\u05B9{N}|{N}ו\u05B9{N}))ה", r"\1ק", v)
    # Adonaï (avec qamats sous le noun) : coupé d'un trait.
    v = re.sub(rf"(א{N}ד{N})(נ{N}ָ{N}י)", r"\1-\2", v)
    return v

def convertit(v):
    """Le texte du Tikoun : qere retenu, noms divins substitués."""
    return NFC(noms(qere(NFC(v))))


def livre(v):
    """Le texte du livre de Tehilim : qere retenu, Nom en toutes lettres
    (demande du propriétaire, 04/10). Seul le caractère invisible est retiré."""
    return NFC(qere(NFC(v)).replace("\u200d", ""))

if __name__ == "__main__":
    import sys
    src = json.load(open(sys.argv[1], encoding="utf-8"))["text"]
    psaumes = [[livre(v) for v in ch] for ch in src]
    tikkun = json.load(open("android/app/src/main/res/raw/tikkun_haklali.json", encoding="utf-8"))
    # Le Tikoun et le livre écrivent tous deux le Nom en toutes lettres (04/10).
    ecarts = [f'{p["numero"]}:{v["n"]}' for p in tikkun["psaumes"] for v in p["versets"]
              if psaumes[p["numero"] - 1][v["n"] - 1] != NFC(v["he"])]
    print("versets :", sum(map(len, psaumes)), "| écarts avec le Tikoun :", ecarts or "aucun")
    if "--ecrire" in sys.argv:
        chemin = "android/app/src/main/res/raw/tehilim.json"
        donnees = json.load(open(chemin, encoding="utf-8"))
        donnees["psaumes"] = psaumes
        json.dump(donnees, open(chemin, "w", encoding="utf-8"), ensure_ascii=False, indent=1)
        print("écrit :", chemin)
