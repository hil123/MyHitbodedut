# -*- coding: utf-8 -*-
"""
Construit android/app/src/main/res/raw/hilloulot.json (demande du 11/10/2026) :
les hilloulot (jours de décès de tsadikim et de 'hakhamim), par jour hébreu.

Sources, lues le 11/10/2026 et gardées dans tools/hilloulot/ (lots 1 à 4) :
  - Wikipédia hébreu, section « נפטרו » des pages de date (CC BY-SA 4.0) ;
  - החכם היומי (hyomi.org.il, Kol Israël 'Haverim), pages par mois.
De החכם היומי, seuls des faits : nom, jour, année. De Wikipédia, en plus,
la première phrase de l'article (CC BY-SA 4.0, attribution à l'écran).
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
import collections, json, os, re

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


G = {c: i for i, c in enumerate("אבגדהוזחט", 1)} | {"י": 10, "כ": 20, "ל": 30}


def jour_hebreu(s):
    """« כ''ח » → 28 ; None si ce n'est pas un jour."""
    s = re.sub("[\"'״׳]", "", s)
    return sum(G[c] for c in s) if s and all(c in G for c in s) and 1 <= sum(G[c] for c in s) <= 30 else None


def complements(jours):
    """Hidabroot et תורת אמת (12/10) : les liens, les ajouts confirmés, une date corrigée."""
    # Hidabroot (hidabroot.org/article/1132443) : des faits seuls (nom, jour), et le
    # lien vers son article. Rapprochement fait à la lecture : tools/hilloulot/hidabroot.json.
    hida = json.load(open(os.path.join(ICI, "hilloulot", "hidabroot.json"), encoding="utf-8"))
    liens = 0
    for c in hida["correspondances"]:
        item = jours[c["notre_cle"]][c["notre_index"]]
        assert item["he"] == c["notre_he"], c
        if c.get("url"):
            item["i"] = c["url"]
            liens += 1
        if "i" not in item["s"]:
            item["s"] += "i"
    # Ajouts : absents de notre table, date confirmée par Wikipédia hébreu (deux sources).
    fichier = os.path.join(ICI, "hilloulot", "extraits_wiki_ajouts.json")
    extraits = json.load(open(fichier, encoding="utf-8")) if os.path.exists(fichier) else {}
    ajouts = 0
    for a in hida["absents"]:
        if a["statut"] != "confirmé":
            continue
        item = {"he": a["hida_nom"], "a": (a.get("annee") or "").replace(" (hewiki)", ""), "s": "iw", "w": a["wiki_article"]}
        if a["wiki_article"] in extraits:
            item["t"] = extraits[a["wiki_article"]]["texte"].strip()
        if a.get("url"):
            item["i"] = a["url"]
        jours.setdefault(a["cle"], []).append(item)
        ajouts += 1
    # Rav Shmouel Birnbaum : 28 Tevet (Wikipédia « כ"ח בטבת תשס"ח, 6 בינואר 2008 » ;
    # Hidabroot 29 Tevet) ; la page de date l'avait rangé au 28 Chevat.
    for i, item in enumerate(jours.get("11-28", [])):
        if item["he"] == "הרב שמואל ברנבוים":
            item["d"] = "date corrigée le 12/10 : 28 Tevet selon son article (la page du 28 Chevat le donnait)"
            jours.setdefault("10-28", []).append(jours["11-28"].pop(i))
            break
    # תורת אמת, « סדר הדורות המקוצר » (CC BY-NC-SA 2.5) : les fils de Jacob, datés.
    # Confirme nos entrées, ou ajoute celles que Hidabroot donne aussi au même jour.
    te = 0
    hida_jour = {(a["hida_nom"], a["cle"]) for a in hida["absents"]}
    for e in json.load(open(os.path.join(ICI, "hilloulot", "toratemet.json"), encoding="utf-8")):
        parties = e["date"].replace("ב", "", 1).split() if e["date"].startswith("ב") else e["date"].split()
        if len(parties) != 2 or parties[1] not in MOIS or jour_hebreu(parties[0]) is None:
            continue
        cle = f"{MOIS[parties[1]]}-{jour_hebreu(parties[0])}"
        nom = e["nom"]
        trouve = [x for x in jours.get(cle, []) if nom.split()[0] in x["he"]]
        for x in trouve:
            if "e" not in x["s"]:
                x["s"] += "e"
                te += 1
        if not trouve and any(n.startswith(nom.split()[0]) and k == cle for n, k in hida_jour):
            jours.setdefault(cle, []).append({"he": nom, "a": "", "s": "ie", "d": "figure biblique ; date de תורת אמת et de Hidabroot"})
            te += 1
    print(f"Hidabroot : {liens} liens, {ajouts} ajouts ; תורת אמת : {te} ;", end=" ")


TITRES = set('רבי הרב ר רב חכם האדמו"ר אדמו"ר האדמור אדמור המקובל מקובל התנא רבנו רבינו מרן הגאון הצדיק '
             'הרה"ק הרבנית רבנית בעל ה בן בר זצ"ל זיע"א זצוק"ל מוהר"ר הקדוש החכם השר אבינו אמנו הנביא'.split())


def mots(nom):
    """Les mots qui distinguent un nom : sans niqqoud, ponctuation ni titres ; les
    sigles gardent leurs lettres (הגר"א → הגרא)."""
    nom = re.sub("[\u0591-\u05C7]", "", nom)
    nom = re.sub("[\"'׳״`]", "", nom)
    nom = re.sub("[,.()\\[\\]\\-–:;]", " ", nom)
    return [w for w in nom.split() if w not in TITRES and len(w) > 1]


def cle_de(date):
    """« כ"ו אייר », « ט' בסיוון », « אדר ב' » … → la clé du jour, ou None."""
    p = re.sub("מנחם[ -]אב", "אב", date).replace("מרחשון", "מרחשוון").replace("־", " ").split()
    if len(p) < 2 or jour_hebreu(p[0]) is None:
        return None
    m = " ".join(p[1:])
    m = m[1:] if m.startswith("ב") and m[1:].split()[0] in MOIS else m
    m = {"אדר א'": "adar1", "אדר ב'": "adar2", "אדר א׳": "adar1", "אדר ב׳": "adar2"}.get(m) or MOIS.get(m.split()[0])
    return f"{m}-{jour_hebreu(p[0])}" if m else None


def sans_balisage(texte):
    """Ôte la mise en forme Markdown laissée par l'extraction (« ## », « ** », liens),
    sans toucher aux mots ; trois lignes vides au plus deviennent une seule."""
    texte = re.sub(r"(?m)^\s*#+\s*", "", texte)
    texte = texte.replace("**", "").replace("__", "")
    texte = re.sub(r"!?\[([^\]]*)\]\([^)]*\)", r"\1", texte)
    return re.sub(r"\n{3,}", "\n\n", texte).strip()


def sources_autorisees(jours):
    """MyTzadik et המאיר לארץ (12/10) : reprise autorisée avec mention, accord reçu par le
    propriétaire de l'app. Leur biographie, mot pour mot, s'ajoute au tsaddik du même jour
    quand les noms concordent ; sinon le tsaddik est ajouté, avec cette seule source."""
    communs = collections.Counter(w for v in jours.values() for x in v for w in mots(x["he"]))

    def candidats(cle):
        if cle.startswith("adar"):
            return [x for k in ("adar", "adar1", "adar2") for x in jours.get(f"{k}-{cle.split('-')[1]}", [])]
        return jours.get(cle, [])

    def score(a, x):
        m = mots(x["he"])
        c = a & set(m)
        rares = [w for w in c if communs[w] < 15]
        # Un seul mot commun ne suffit que s'il est rare et que c'est le dernier de notre nom
        # (un nom de famille, un lieu, un sigle) : « דניאל » seul ne fait pas la même personne.
        return len(c) + len(rares) if len(c) >= 2 or (rares and m and m[-1] in rares) else 0

    def apparier(nom, cle):
        """Le même jour seulement. Un rapprochement d'un jour à l'autre confondait des
        homonymes d'une même dynastie (12/10) : un tsaddik dont la date diffère de la
        nôtre est ajouté à sa date, avec sa source."""
        a = set(mots(nom))
        meilleur = max(((score(a, x), x) for x in candidats(cle)), key=lambda t: t[0], default=(0, None))
        return (meilleur[1], False) if meilleur[0] else (None, False)

    for code, fichier in (("l", "hameir.json"), ("m", "mytzadik.json")):
        chemin = os.path.join(ICI, "hilloulot", fichier)
        if not os.path.exists(chemin):
            continue
        lies = ajoutes = 0
        for e in json.load(open(chemin, encoding="utf-8")):
            cle = cle_de(e.get("date") or "")
            if not cle:
                continue
            bio = sans_balisage(e.get("bio") or "")
            x, _ = apparier(e["nom"], cle)
            if x is None:
                x = {"he": e["nom"].strip(), "a": "", "s": ""}
                jours.setdefault(cle, []).append(x)
                ajoutes += 1
            else:
                lies += 1
            if code not in x["s"]:
                x["s"] += code
            if bio and not any(b[0] == code for b in x.get("b", [])):
                x.setdefault("b", []).append([code, bio, e.get("url") or ""])
        print(f"{fichier} : {lies} rapprochés, {ajoutes} ajoutés ;", end=" ")


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
    # La première phrase de l'article de Wikipédia hébreu (CC BY-SA 4.0), relevée
    # mot pour mot le 11/10/2026 (tools/hilloulot/descriptions.json : la raison
    # de chaque absence, de chaque coupe ou retouche est dans « note »).
    decrits = 0
    for e in json.load(open(os.path.join(ICI, "hilloulot", "descriptions.json"), encoding="utf-8")):
        item = jours[e["cle"]][e["index"]]
        assert item["he"] == e["he"], (e["cle"], e["index"], e["he"])
        if e.get("article") and e.get("description"):
            item["t"], item["w"] = e["description"], e["article"]
            decrits += 1
    # Plus de détails (12/10) : le début de l'article, jusqu'à six phrases de son
    # introduction, mot pour mot (API de Wikipédia hébreu, prop=extracts, exintro,
    # exsentences=6, relevé le 12/10/2026 : tools/hilloulot/extraits_wiki.json).
    # Il remplace la première phrase quand il en dit plus.
    fichier = os.path.join(ICI, "hilloulot", "extraits_wiki.json")
    extraits = json.load(open(fichier, encoding="utf-8")) if os.path.exists(fichier) else {}
    allonges = 0
    for liste in jours.values():
        for item in liste:
            x = extraits.get(item.get("w") or "")
            if x and len(x["texte"].strip()) > len(item.get("t", "")):
                item["t"] = x["texte"].strip()
                allonges += 1
    print(f"{allonges} descriptions allongées ;", end=" ")
    complements(jours)
    sources_autorisees(jours)
    sortie = {
        "_meta": {
            "date": "2026-10-12",
            "sources": ["Wikipédia hébreu, pages de date, section נפטרו (CC BY-SA 4.0)",
                        "החכם היומי, hyomi.org.il (Kol Israël 'Haverim) : faits seuls (nom, date)",
                        "Hidabroot, hidabroot.org/article/1132443 : faits seuls (nom, date) et lien vers ses articles",
                        "תורת אמת, סדר הדורות המקוצר (CC BY-NC-SA 2.5)",
                        "MyTzadik, mytzadik.com (YETEV 4 LTD) : reprise autorisée avec mention, accord reçu par le propriétaire de l'app le 12/10/2026",
                        "המאיר לארץ, hameir-laarets.org.il : reprise autorisée avec mention, accord reçu par le propriétaire de l'app le 12/10/2026"],
            "s": "w = Wikipédia, h = החכם היומי, i = Hidabroot, e = תורת אמת, m = MyTzadik, l = המאיר לארץ",
            "i": "lien vers l'article de Hidabroot",
            "b": "hilloulot_bios.json : par jour et dans le même ordre, les biographies d'autres sources, mot pour mot : [source, texte, lien], ou 0",
            "regles": "Désaccord : החכם היומי l'emporte. « adar » : Adar II les années embolismiques (OH 568:7). "
                      "Un 30 absent cette année se dit le 29.",
            "d": "raison d'un doute (personne, date ou année) relevée à la lecture",
            "t": "début de l'article de Wikipédia hébreu (CC BY-SA 4.0), jusqu'à six phrases ; w : son titre",
        },
        "jours": dict(sorted(jours.items())),
    }
    # Les biographies longues (MyTzadik, המאיר לארץ) à part : hilloulot.json reste léger pour la
    # carte du jour ; hilloulot_bios.json n'est lu qu'à l'écran des hilloulot. Par jour, une
    # entrée par hilloula, dans le même ordre : ses biographies, ou 0.
    bios = {}
    for cle, liste in sortie["jours"].items():
        ligne = [x.pop("b", 0) for x in liste]
        if any(ligne):
            bios[cle] = ligne
    raw = os.path.join(ICI, "..", "android", "app", "src", "main", "res", "raw")
    json.dump(bios, open(os.path.join(raw, "hilloulot_bios.json"), "w", encoding="utf-8"), ensure_ascii=False, separators=(",", ":"))
    chemin = os.path.join(raw, "hilloulot.json")
    json.dump(sortie, open(chemin, "w", encoding="utf-8"), ensure_ascii=False, separators=(",", ":"))
    print(f"{decrits} descriptions ;", end=" ")
    total = sum(len(v) for v in jours.values())
    print(f"{total} hilloulot, {len(jours)} jours, {retires} entrées Wikipédia retirées ; {os.path.getsize(chemin)//1024} Ko")


if __name__ == "__main__":
    main()
