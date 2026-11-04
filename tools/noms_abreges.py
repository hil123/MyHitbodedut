# -*- coding: utf-8 -*-
"""
Le Nom abrégé ה' dans les livres, sauf dans les versets (demande du
propriétaire, 04/11/2026). Elle remplace, pour les citations des livres de
Breslev (bibliothèque, 'hizouk) et les livres de prière personnelle (Tefila
LiChlomo), la décision du 04/10 qui les avait mises « en toutes lettres »
(tools/noms_divins.py). Le siddour, Nichmat, les Tehilim, le Tikoun et les
livres du Tanakh restent en toutes lettres : ce sont des prières et des
versets.

Un verset se reconnaît en le cherchant dans le Tanakh : autour de chaque
occurrence du Nom, on compare les mots voisins (sans voyelles, ni lettres de
prolongation ו/י, le Nom et אדני ramenés à un seul signe) aux suites de quatre
mots du Tanakh (tanach.us via l'export Sefaria, domaine public). Si une suite
de quatre mots qui contient le Nom se retrouve dans un verset, c'est une
citation : le Nom reste en toutes lettres. Sinon, il devient ה' :

  לִפְנֵי יְהֹוָה יִתְבָּרַךְ  →  לִפְנֵי ה' יִתְבָּרַךְ
  לַיהֹוָה יִתְבָּרַךְ        →  לַה' יִתְבָּרַךְ
  קָרוֹב יְהֹוָה לְכָל־קֹרְאָיו  (Tehilim 145:18) : inchangé

Usage : python3 tools/noms_abreges.py <dossier de l'export Tanakh>
(un fichier « Livre.json » par livre, version « Tanach with Nikkud »).
"""
import json
import os
import re
import sys
import unicodedata

NFC = lambda s: unicodedata.normalize("NFC", s)
SIGNES = "[֑-ֽֿ-ׇ]"
FINALES = str.maketrans("ךםןףץ", "כמנפצ")
# Le Nom écrit en toutes lettres, vocalisé ou non, avec ses préfixes.
NOM = re.compile(
    "(?<![א-ת֑-ׇ])"
    "((?:[ובלמשכה]" + SIGNES + "*){0,2}?)"
    "י" + SIGNES + "*" + "ה" + SIGNES + "*" + "ו" + SIGNES + "*" + "ה" + SIGNES + "*"
    "(?![א-ת])"
)


def squelette(mot):
    """Un mot sans voyelles, ponctuation ni lettres de prolongation ; le Nom et אדני deviennent « @ »."""
    m = re.sub("[֑-ׇ]", "", mot)
    m = re.sub("[^א-ת]", "", m).translate(FINALES)
    if re.fullmatch("[ובלמשכה]{0,2}(יהוה|אדני)", m):
        return "@"
    if not m:
        return ""
    return m[0] + re.sub("[וי]", "", m[1:])


def mots(texte):
    return [s for s in (squelette(w) for w in re.split(r"[\s־\-]+", texte)) if s]


def index_tanakh(dossier):
    """Les suites de quatre mots contenant le Nom ; et, à part, celles de trois (repli près d'une citation annoncée)."""
    quatre = set()
    for f in sorted(os.listdir(dossier)):
        if not f.endswith(".json"):
            continue
        livre = json.load(open(os.path.join(dossier, f), encoding="utf-8"))["text"]
        for chapitre in livre:
            for verset in chapitre:
                m = mots(re.sub(r"<[^>]+>|\{[פס]\}", " ", verset))
                for n in (2, 3, 4):
                    for i in range(len(m) - n + 1):
                        g = tuple(m[i:i + n])
                        if "@" in g:
                            quatre.add(g)
    return quatre


# Ce qui annonce un verset : « שֶׁכָּתוּב », « שֶׁנֶּאֱמַר », « כְּתִיב », un guillemet ouvrant.
ANNONCE = re.compile("(?:כתוב|כתב|נאמר|כתיב|פסוק|[\"״“])\\W{0,3}(?:[^\\s\"״“]+\\s*){0,2}$")


def abrege_forme(prefixe):
    """לַ / לְ / בַּ / מֵ / שֶׁ / וַ … + ה' : le préfixe garde sa voyelle."""
    p = re.sub("[֑-֯]", "", prefixe)  # sans cantillation
    return p + "ה'"


def abrege(texte, quatre):
    """[texte] avec le Nom abrégé hors des versets ; et (abrégés, gardés)."""
    t = NFC(texte)
    occurrences = list(NOM.finditer(t))
    if not occurrences:
        return t, 0, 0
    # La position (en mots) de chaque occurrence, pour comparer ses voisins.
    morceaux = [(m.start(), m.end()) for m in re.finditer(r"[^\s־\-]+", t)]
    sq = [squelette(t[a:b]) for a, b in morceaux]
    garde = abrege_n = 0
    sortie = []
    dernier = 0
    for m in occurrences:
        k = next((i for i, (a, b) in enumerate(morceaux) if a <= m.start() < b), None)
        verset = False
        if k is not None:
            # Les voisins non vides, de part et d'autre.
            avant = [s for s in sq[:k] if s][-3:]
            apres = [s for s in sq[k + 1:] if s][:3]
            suite = avant + ["@"] + apres
            j = len(avant)
            for d in range(4):
                debut = j - d
                if debut < 0 or debut + 4 > len(suite):
                    continue
                if tuple(suite[debut:debut + 4]) in quatre:
                    verset = True
                    break
            # Une citation annoncée (« כמו שכתוב », un guillemet) peut être courte, ou finir le
            # texte : trois mots suffisent alors, à partir du Nom ou juste avant lui.
            # Au bord du paragraphe (une citation qui le finit ou l'ouvre), trois mots suffisent.
            if not verset and (len(apres) < 3 or len(avant) < 3):
                for debut in (j - 2, j - 1, j):
                    if 0 <= debut and debut + 3 <= len(suite) and (debut + 3 == len(suite) or debut == 0) \
                            and tuple(suite[debut:debut + 3]) in quatre:
                        verset = True
                        break
            # Une courte citation entre guillemets (« "ה' הוֹשִׁיעָה" ») : toute la citation est dans un verset.
            if not verset:
                avant_brut = t[max(0, m.start() - 3):m.start()]
                fin = re.search("[\"”״]", t[m.end():m.end() + 60])
                if re.search("[\"“״]\\s*$", avant_brut) and fin:
                    cite = mots(t[m.start():m.end() + fin.start()])
                    if 2 <= len(cite) <= 4 and tuple(cite) in quatre:
                        verset = True
            if not verset:
                annonce = ANNONCE.search(re.sub("[\u0591-\u05C7]", "", t[max(0, m.start() - 40):m.start()]).strip()) or \
                    ANNONCE.search(re.sub("[\u0591-\u05C7]", "", " ".join(t[a:b] for a, b in morceaux[max(0, k - 2):k])))
                if annonce:
                    for debut in (j, j - 1):
                        if 0 <= debut and debut + 3 <= len(suite) and tuple(suite[debut:debut + 3]) in quatre:
                            verset = True
                            break
        sortie.append(t[dernier:m.start()])
        if verset:
            sortie.append(m.group(0))
            garde += 1
        else:
            sortie.append(abrege_forme(m.group(1)))
            abrege_n += 1
        dernier = m.end()
    sortie.append(t[dernier:])
    return NFC("".join(sortie)), abrege_n, garde


def parcourt(o, cles, quatre, journal, chemin=""):
    """Applique [abrege] aux chaînes des clés [cles] (et aux listes de chaînes), partout dans [o]."""
    total = [0, 0]
    if isinstance(o, dict):
        for k, v in o.items():
            if k in cles and isinstance(v, str):
                o[k], a, g = abrege(v, quatre)
                total[0] += a; total[1] += g
                if a or g:
                    journal.append((f"{chemin}.{k}", a, g))
            elif k in cles and isinstance(v, list) and all(isinstance(x, str) for x in v):
                nouveaux = []
                for i, x in enumerate(v):
                    y, a, g = abrege(x, quatre)
                    nouveaux.append(y)
                    total[0] += a; total[1] += g
                    if a or g:
                        journal.append((f"{chemin}.{k}[{i}]", a, g))
                o[k] = nouveaux
            elif k != "_meta":
                r = parcourt(v, cles, quatre, journal, f"{chemin}.{k}")
                total[0] += r[0]; total[1] += r[1]
    elif isinstance(o, list):
        for i, v in enumerate(o):
            r = parcourt(v, cles, quatre, journal, f"{chemin}[{i}]")
            total[0] += r[0]; total[1] += r[1]
    return total


REGLE = ("abrégé ה' hors des versets ; en toutes lettres dans les versets cités "
         "(décision du propriétaire, 04/11/2026 ; tools/noms_abreges.py)")

if __name__ == "__main__":
    racine = "android/app/src/main/res/raw/"
    quatre = index_tanakh(sys.argv[1])
    print("suites de quatre mots avec le Nom dans le Tanakh :", len(quatre))
    journal = []

    chizuk = json.load(open(racine + "chizuk.json", encoding="utf-8"))
    r = parcourt(chizuk["items"], {"he", "he_sans_nikkoud"}, quatre, journal, "chizuk.items")
    chizuk["_meta"]["noms_divins"] = REGLE
    json.dump(chizuk, open(racine + "chizuk.json", "w", encoding="utf-8"), ensure_ascii=False, indent=2)
    print("chizuk : abrégés", r[0], "gardés (versets)", r[1])

    biblio = json.load(open(racine + "bibliotheque.json", encoding="utf-8"))
    r = parcourt(biblio["sections"], {"paragraphes", "paragraphes_sans_nikkoud", "texte_integral"}, quatre, journal, "bibliotheque.sections")
    biblio["_meta"]["noms_divins"] = REGLE
    json.dump(biblio, open(racine + "bibliotheque.json", "w", encoding="utf-8"), ensure_ascii=False, indent=2)
    print("bibliothèque : abrégés", r[0], "gardés (versets)", r[1])

    livres_src = os.path.join(racine, "..", "..", "livres", "livres.json")  # la source, hors des ressources (04/11)
    livres = json.load(open(livres_src, encoding="utf-8"))
    for t in livres["textes"]:
        # Les livres d'étude et de prière personnelle ; pas le Tanakh ni Nichmat (liturgie, comme le siddour).
        if t["id"] in ("meguilat_esther", "chir_hachirim", "nichmat"):
            continue
        r = parcourt(t["sections"], {"t"}, quatre, journal, f"livres.{t['id']}")
        if r[0] or r[1]:
            print(f"livres/{t['id']} : abrégés", r[0], "gardés (versets)", r[1])
    livres["_meta"]["noms_divins"] = REGLE + " ; le Tanakh (Esther, Chir HaChirim) et Nichmat restent en toutes lettres"
    json.dump(livres, open(livres_src, "w", encoding="utf-8"), ensure_ascii=False, separators=(",", ":"))
    sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
    import livres_decoupe
    livres_decoupe.decoupe(livres)

    json.dump(journal, open("/tmp/noms_abreges_journal.json", "w", encoding="utf-8"), ensure_ascii=False, indent=1)
