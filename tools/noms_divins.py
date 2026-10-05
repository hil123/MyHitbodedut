# -*- coding: utf-8 -*-
"""
Le Nom en toutes lettres (demande du propriétaire, 04/10/2026) : les
abréviations des sources sont développées dans les textes de prière.

  יְיָ, יי, ה' / ה׳ (avec ou sans préfixe ו ב ל מ ש כ ה) → יְהֹוָה, ou יהוה
      dans un texte non vocalisé ;
  אלקים, אֱלֹקִים et leurs formes → אלהים, אֱלֹהִים…

On ne l'applique qu'au texte des prières : dans une consigne, ה' peut être
un chiffre (« סימן ה' »).
"""
import re
import unicodedata

NFC = lambda s: unicodedata.normalize("NFC", s)
SIGNES = "[֑-ׇֽֿׁׂׅׄ]*"
PREFIXE = f"((?:[ובלמשכה]{SIGNES}){{0,2}})"
FIN = r"(?=$|[\s,.:;!?)\]־׃׃<]|&)"
DEBUT = r"(?:(?<=^)|(?<=[\s(\[>־]))"


def vocalise(texte):
    nu = re.sub(r"<[^>]+>", "", texte)
    lettres = len(re.findall("[א-ת]", nu))
    voyelles = len(re.findall("[ְ-ׇּׁׂ]", nu))
    return lettres > 0 and voyelles / lettres >= 0.2


def _nom(prefixe, voyelles):
    if not voyelles:
        return prefixe + "יהוה"
    # Après un préfixe vocalisé patah ou qamats (לַ, בַּ, וַ), le yod perd son shva.
    if prefixe and re.search("[ַָ]$", re.sub("[֑-ּ֯]", "", prefixe)):
        return prefixe + "יהֹוָה"
    # Préfixe sans voyelle (« לה' » abrégé) : on lui rend la sienne, comme dans
    # le Tanakh : לַיהֹוָה, וַיהֹוָה, מֵיְהֹוָה, שֶׁיְּהֹוָה.
    if prefixe and prefixe[-1] in "לבכו":
        return prefixe + "ַיהֹוָה"
    if prefixe and prefixe[-1] == "מ":
        return prefixe + "ֵיְהֹוָה"
    if prefixe and prefixe[-1] == "ש":
        return prefixe + "ֶׁיְּהֹוָה"
    return prefixe + "יְהֹוָה"


def developpe(texte):
    """Rend [texte] avec le Nom en toutes lettres, et le nombre de formes développées."""
    t = NFC(texte)
    v = vocalise(t)
    compte = 0

    def remplace(m):
        nonlocal compte
        compte += 1
        return _nom(m.group(1), v)

    # יְיָ (vocalisé) ou יי (nu), mot entier, préfixes permis.
    t = re.sub(DEBUT + PREFIXE + "יְ?" + "י" + "ָ?" + SIGNES + FIN, remplace, t)
    # ה' / ה׳ : un hé seul suivi d'un geresh, qui n'est pas un sigle (ה״ / ה'' / ה"…).
    t = re.sub(DEBUT + PREFIXE + "ה['׳](?![\"'״])", remplace, t)

    def elohim(m):
        nonlocal compte
        compte += 1
        return m.group(1) + "ה"
    # Elohim vocalisé : holam sur le lamed (ou par un vav), puis ק.
    t = re.sub(rf"(א{SIGNES}ל(?:{SIGNES}ֹ{SIGNES}|{SIGNES}ו{SIGNES}ֹ{SIGNES}|{SIGNES}וֹ{SIGNES}))ק", elohim, t)
    # Elohim nu : אלק suivi d'une terminaison du nom.
    t = re.sub(r"(אל)ק(?=(?:ים|י|ינו|יך|יכם|יהם|יו|ה)(?:$|[\s,.:;!?)]))", elohim, t)
    # אֲדֹ-נָי / אד-ני : le tiret de substitution est retiré.
    def adonai(m):
        nonlocal compte
        compte += 1
        return m.group(1) + m.group(2)
    t = re.sub(f"(א{SIGNES}ד{SIGNES})[-־](נ{SIGNES}י)", adonai, t)
    return NFC(t), compte


if __name__ == "__main__":
    # Les citations hébraïques du chizuk et de la bibliothèque (04/10) : texte
    # vocalisé et non vocalisé. Le résumé hébreu, texte de l'application,
    # dit « השם » comme toute l'interface.
    import json
    racine = "android/app/src/main/res/raw/"
    total = 0

    def champ(o, cle):
        global total
        v = o.get(cle)
        if isinstance(v, str):
            o[cle], n = developpe(v)
            total += n
        elif isinstance(v, list):
            nouveaux = []
            for x in v:
                y, n = developpe(x) if isinstance(x, str) else (x, 0)
                nouveaux.append(y)
                total += n
            o[cle] = nouveaux

    chizuk = json.load(open(racine + "chizuk.json", encoding="utf-8"))
    for item in chizuk["items"]:
        champ(item, "he")
        champ(item, "he_sans_nikkoud")
        if item.get("note") == "Nom divin écrit אלקים.":
            item["note"] = "Nom divin en toutes lettres."
        if isinstance(item.get("note"), str):
            item["note"] = item["note"].replace("Nom divin écrit אֲדֹ-נָי.", "Nom divin en toutes lettres.")
    chizuk["_meta"]["noms_divins"] = "en toutes lettres, comme dans les sources (décision du propriétaire, 04/10/2026)"
    json.dump(chizuk, open(racine + "chizuk.json", "w", encoding="utf-8"), ensure_ascii=False, indent=2)

    biblio = json.load(open(racine + "bibliotheque.json", encoding="utf-8"))
    def parcourt(o):
        global total
        if isinstance(o, dict):
            for cle in ("paragraphes", "paragraphes_sans_nikkoud", "texte_integral"):
                champ(o, cle)
            if isinstance(o.get("resume_he"), str) and re.search("ה['׳](?![\"'״])", o["resume_he"]):
                o["resume_he"] = re.sub("ה['׳](?![\"'״])", "השם", o["resume_he"])
                total += 1
            if isinstance(o.get("note"), str):
                o["note"] = o["note"].replace("Noms divins substitués (ה', אלקים).", "Nom divin en toutes lettres.")
            for v in o.values():
                parcourt(v)
        elif isinstance(o, list):
            for v in o:
                parcourt(v)
    parcourt(biblio)
    json.dump(biblio, open(racine + "bibliotheque.json", "w", encoding="utf-8"), ensure_ascii=False, indent=2)
    print("formes développées :", total)
