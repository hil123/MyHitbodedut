# -*- coding: utf-8 -*-
"""
Construit android/app/src/main/res/raw/siddur_annotations.json (10/10/2026) :
ce qui, dans les trois siddourim, dépend du jour. Les textes eux-mêmes ne
sont pas touchés ; ce fichier les désigne par (texte, section, bloc), avec le
début du bloc comme empreinte, pour qu'un siddour reconstruit qui aurait
bougé fasse échouer les tests au lieu de mettre en évidence le mauvais bloc.

Trois sortes d'annotations :
  - « bloc »    : un ajout entier. S'il s'applique ce jour-là, il est mis en
                  évidence ; sinon il est replié, et une flèche le rouvre.
  - « section » : une section entière (Ta'hanoun, omer, motsaé Chabbat…),
                  même traitement.
  - « indice »  : un bloc qui mêle le texte de tous les jours et une
                  variante (« בעשרת ימי תשובה אומרים: המלך הקדוש »). Rien
                  n'est replié : quand la variante s'applique, un repère
                  « Aujourd'hui : המלך הקדוש » s'affiche sous le bloc.

Aucune règle de calendrier n'est écrite ici. Chaque annotation renvoie à une
clé de condition (CalendrierJuif.C), calculée par KosherJava ; l'outil ne
fait que reconnaître, dans le texte, la consigne du siddour qui dit quand
l'ajout se dit (« בקיץ », « בחנוכה ופורים אומרים », « בעשי״ת »…). Une
condition précédée de « ! » s'applique quand la clé est fausse (« בימים שאין
בהם תחנון »). Ce qui ne se laisse pas reconnaître sûrement reste tel quel,
déplié comme avant : Avinou malkénou (coutumes diverses), le Ta'hanoun du
siddour ashkénaze (mêlé au Vidouï sans frontière nette), le Chir chel yom.

  python3 tools/siddur_annotations.py
"""
import json
import os
import re
import sys

RACINE = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
RAW = os.path.join(RACINE, "android", "app", "src", "main", "res", "raw")
TEXTES = ["chaharit", "minha", "arvit", "birkat_hamazon", "meen_chaloch"]
OFFICE = {"chaharit": "chaharit", "minha": "minha"}


def nu(s):
    """Le texte sans balises, sans voyelles ni cantillation, espaces réduits."""
    s = re.sub(r"<[^>]+>", " ", s)
    s = re.sub(r"[֑-ׇ]", "", s)
    return re.sub(r"\s+", " ", s).strip()


def mots(s):
    """Sans ponctuation, pour comparer des débuts de prière."""
    return re.sub(r"[^א-ת ]", "", s.replace("־", " ")).split()


def commence(s, debut):
    return mots(s)[: len(debut.split())] == debut.split()


ETE = ("בקיץ:", "בימות החמה:")
HIVER = ("בחורף:", "בימות הגשמים:")

# Les variantes de Ya'alé véyavo : le préfixe de la ligne, et la fête.
VARIANTES_YAALE = [
    (r"^(ב|ל)(ראש חדש|ראש חודש|ר״ח)\b", "roch_hodech"),
    (r"^(ב|ל)(חוה״מ )?פסח\b", "pessah"),
    (r"^(ב|ל)(חוה״מ )?(סוכות|סכות)\b", "soukkot"),
    (r"^(ב|ל)שבועות\b", "chavouot"),
    (r"^(ב|ל)(ראש השנה|ר״ה)\b", "roch_hachana"),
    (r"^(ב|ל)(שמיני עצרת|שמ״ע|ש״ע)", "chemini_atseret"),
    (r"^ילדים האוכלים ביו״כ", "kippour"),
]

# Les variantes enchâssées dans un bloc, après leur annonce.
INDICES = [
    (r"בעשרת ימי תשובה (?:אומרים|אומר|מסיימים)?:? ?([^:()]+)", "asseret_yemei_techouva"),
    (r"\(בעשי״ת ([^)]+)\)", "asseret_yemei_techouva"),
]


def annoter(texte_id, sections, nusah):
    sortie = []

    def note(sorte, si, bi, cond, extra=None):
        bloc = sections[si]["blocs"][bi] if bi is not None else None
        e = {"sorte": sorte, "s": si, "si": cond}
        if bi is not None:
            e["b"] = bi
            e["empreinte"] = nu(bloc.get("t") or bloc.get("c"))[:40]
        else:
            e["empreinte"] = nu(sections[si]["titre"])
        if extra:
            e.update(extra)
        sortie.append(e)

    office = OFFICE.get(texte_id)
    for si, sec in enumerate(sections):
        titre = nu(sec["titre"])
        # --- Sections entières -------------------------------------------------
        cond = None
        if titre in ("תחנון", "וידוי") and office:
            cond = "tahanoun_" + office
        elif titre == "לשני וחמישי":
            cond = "lundi_jeudi"
        elif "מוצאי שבת" in titre:
            cond = "motsae_chabbat"
        elif titre == "ספירת העומר":
            cond = "omer"
        elif titre == "ברכי נפשי" and "בראש חדש" in nu(sec["blocs"][0].get("c", "")):
            cond = "roch_hodech"
        if cond:
            note("section", si, None, cond)
            continue

        blocs = sec["blocs"]
        txt = [nu(b.get("t") or b.get("c")) for b in blocs]
        cons = ["c" in b for b in blocs]
        pris = set()

        def prend(bi, c):
            if bi not in pris:
                pris.add(bi)
                note("bloc", si, bi, c)

        def en_tete(bi, c, *mots_cles):
            """La consigne juste avant un ajout, si elle l'annonce, en suit le sort."""
            if bi > 0 and cons[bi - 1] and all(m in txt[bi - 1] for m in mots_cles):
                prend(bi - 1, c)

        n = len(blocs)
        bi = 0
        while bi < n:
            x = txt[bi]
            # Ya'alé véyavo, ses variantes, jusqu'à « חנון ורחום אתה ».
            if not cons[bi] and commence(x, "אלהינו ואלהי אבותינו יעלה ויבא"):
                en_tete(bi, "yaale_veyavo", "אומרים")
                j = bi
                while j < n:
                    var = next((c for r, c in VARIANTES_YAALE if re.search(r, txt[j])), None)
                    prend(j, var or "yaale_veyavo")
                    if "חנון ורחום אתה" in txt[j] or (j > bi and cons[j] and var is None):
                        break
                    j += 1
                bi = j + 1
                continue
            # Al hanissim, puis ses parties de 'Hanouka et de Pourim.
            if not cons[bi] and (commence(x, "על הנסים ועל הפרקן") or commence(x, "ועל הנסים ועל הפרקן")):
                en_tete(bi, "al_hanissim", "חנוכה")
                prend(bi, "al_hanissim")
                j = bi + 1
                while j < n:
                    y = txt[j]
                    if y.startswith("בחנוכה") or commence(y, "בימי מתתיהו") or commence(y, "בימי מתתיה"):
                        prend(j, "hanouka")
                    elif y.startswith("בפורים") or commence(y, "בימי מרדכי"):
                        prend(j, "pourim")
                    else:
                        break
                    j += 1
                bi = j
                continue
            # Anénou, les jours de jeûne.
            if not cons[bi] and ("עננו אבינו עננו ביום צום" in x):
                # À Arvit, le siddour ne le dit que le 9 Av (« בתשעה באב יש
                # אומרים עננו ») : un jeûne mineur ne commence qu'à l'aube.
                c = "ticha_beav" if texte_id == "arvit" else "jeune"
                en_tete(bi, c, "תענית")
                prend(bi, c)
            elif cons[bi] and re.fullmatch(r"\(?בתענית ציבור אומר כאן הש״ץ עננו\)?", x):
                prend(bi, "jeune")
            # Na'hem, le 9 Av.
            elif not cons[bi] and commence(x, "נחם יהוה אלהינו"):
                en_tete(bi, "ticha_beav", "באב")
                prend(bi, "ticha_beav")
            # Été, hiver : la consigne seule, puis son bloc.
            elif cons[bi] and x in ETE + HIVER and bi + 1 < n and not cons[bi + 1]:
                y = txt[bi + 1]
                if x in ETE:
                    c = "morid_hatal" if "מוריד הטל" in y else "berakha"
                else:
                    c = "machiv_harouah" if "משיב הרוח" in y else "tal_oumatar"
                prend(bi, c)
                prend(bi + 1, c)
                bi += 2
                continue
            # Les dix jours : la consigne courte, puis son bloc ; ou le bloc qui
            # s'annonce lui-même (« בעשי״ת: זכרנו לחיים »).
            elif cons[bi] and re.fullmatch(r"(בעשי״ת|בעשרת ימי תשובה)( (מסיים|יסיים|אומרים|מוסיפים))?:?", x) \
                    and bi + 1 < n and not cons[bi + 1]:
                prend(bi, "asseret_yemei_techouva")
                j = bi + 1
                prend(j, "asseret_yemei_techouva")
                # Avinou malkénou annoncé par la consigne (Edot) : toute la suite.
                while commence(txt[j], "אבינו מלכנו") and j + 1 < n and not cons[j + 1] \
                        and commence(txt[j + 1], "אבינו מלכנו"):
                    j += 1
                    prend(j, "asseret_yemei_techouva")
                bi = j + 1
                continue
            elif not cons[bi] and re.match(r"בעשי״ת( (מסיים|יסיים))?:", x):
                prend(bi, "asseret_yemei_techouva")
            # Retsé, le Chabbat (Birkat hamazone).
            elif not cons[bi] and commence(x, "רצה והחליצנו"):
                en_tete(bi, "chabbat", "בשבת")
                prend(bi, "chabbat")
            # Le Hallel annoncé après l'Amida.
            elif cons[bi] and "אומרים כאן הלל" in x:
                prend(bi, "hallel")
            # « Les jours sans Ta'hanoun » : la consigne et le bloc qui suit.
            elif cons[bi] and x.startswith("בימים שאין בהם תחנון") and office and bi + 1 < n:
                prend(bi, "!tahanoun_" + office)
                prend(bi + 1, "!tahanoun_" + office)
                bi += 2
                continue
            # Les variantes enchâssées : un repère, rien de replié.
            if bi not in pris and not cons[bi]:
                for motif, c in INDICES:
                    for m in re.finditer(motif, x):
                        variante = m.group(1).strip(" ,.")
                        if variante:
                            note("indice", si, bi, c, {"texte": variante})
                if re.fullmatch(r"בקיץ: מוריד הטל\. בחורף: משיב הרוח ומוריד הגשם\.?", x):
                    note("indice", si, bi, "morid_hatal", {"texte": "מוריד הטל"})
                    note("indice", si, bi, "machiv_harouah", {"texte": "משיב הרוח ומוריד הגשם"})
            bi += 1
    grouper(sortie)
    return sortie


# Les variantes qui suivent leur ajout : repliées avec lui quand il est replié.
SOUS = {
    "yaale_veyavo": {"roch_hodech", "pessah", "soukkot", "chavouot", "roch_hachana", "chemini_atseret", "kippour"},
    "al_hanissim": {"hanouka", "pourim"},
}


def grouper(annotations):
    """
    Un groupe : des blocs consécutifs d'une même section, de même condition
    ou variantes de la première (« racine »). Replié, un groupe tient en une
    ligne ; la racine dit s'il s'applique.
    """
    g = -1
    precedent = None
    for e in annotations:
        if e["sorte"] != "bloc":
            continue
        if precedent and precedent["s"] == e["s"] and precedent["b"] + 1 == e["b"] \
                and (e["si"] == precedent["racine"] or e["si"] in SOUS.get(precedent["racine"], ())):
            e["g"], e["racine"] = precedent["g"], precedent["racine"]
        else:
            g += 1
            e["g"], e["racine"] = g, e["si"]
        precedent = e


def main():
    racine = {"_meta": {
        "outil": "tools/siddur_annotations.py",
        "conditions": "clés de CalendrierJuif.C (KosherJava) ; « ! » : la clé fausse",
        "regle": "les textes ne sont pas modifiés ; un ajout hors du jour est replié, jamais retiré",
        "a_valider": "indicatif : le texte et ses consignes font foi ; relecture par un Rav souhaitée",
    }}
    for nusah in ("edot", "sefard", "ashkenaz"):
        siddur = json.load(open(os.path.join(RAW, f"siddur_{nusah}.json"), encoding="utf-8"))
        racine[nusah] = {}
        for t in siddur["textes"]:
            if t["id"] in TEXTES:
                a = annoter(t["id"], t["sections"], nusah)
                if a:
                    racine[nusah][t["id"]] = a
    cible = os.path.join(RAW, "siddur_annotations.json")
    json.dump(racine, open(cible, "w", encoding="utf-8"), ensure_ascii=False, indent=1)
    for nusah in ("edot", "sefard", "ashkenaz"):
        print(nusah, {k: len(v) for k, v in racine[nusah].items()})


if __name__ == "__main__":
    main()
