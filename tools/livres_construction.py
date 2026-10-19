# -*- coding: utf-8 -*-
"""
Construit android/app/src/main/res/raw/livres.json (demande du 12/10/2026) :
des livres entiers de Rabbi Na'hman et de Rabbi Natan pour la bibliothèque,
au format des textes du siddour (sections, paragraphes « t »), pour être
lus dans le même lecteur.

Sources (export public de Sefaria, storage.googleapis.com/sefaria-export,
Chasidut/Breslov), relevées le 12/10 avec la licence de chaque version :
  - ספר המדות : « Sefer HaMidot -- rabenubook », licence Public Domain ;
    repris tel quel, vocalisé. Les deux parties d'un chapitre (« Part I »,
    « Part II ») se suivent, la seconde annoncée par « חלק שני ».
  - ליקוטי עצות : « Likutei Etzot - rabenubook.com », licence « unknown ».
    Le texte de Rabbi Natan (m. 1844) est du domaine public ; la
    vocalisation est un travail éditorial dont la licence n'est pas
    déclarée : elle est retirée, comme pour le Likoutei Tefilot de la
    bibliothèque. Le titre du chapitre, répété en tête du premier
    paragraphe, est retiré (il est déjà le titre de la section).

Le Nom, quand il est abrégé (ה׳, יי), reste tel que l'imprime le livre :
ce ne sont pas des prières.

Ajouts du 13/10 (demande du propriétaire) :
  - ליקוטי מוהר״ן (חלק א׳ et תנינא) : « Likutei Moharan - rabenubook.com » et
    « Likutei Moharan Tinyana - rabenubook.com », Public Domain, vocalisés.
  - מגילת אסתר, שיר השירים : « Tanach with Nikkud » (tanach.us), Public Domain,
    la même édition que les Tehilim de l'application ; un paragraphe par verset.
  - פרק שירה : « Torat Emet 357 », Public Domain.
  - נשמת כל חי : Siddur Edot HaMizrach, « Torat Emet 357 », Public Domain
    (Chabbat, Pessouké dezimra, de « נשמת כל חי » jusqu'avant « ישתבח »).
  - השתפכות הנפש (R. Alter de Teplik, m. 1919) et כוכבי אור (R. Avraham b.
    R. Nahman, m. 1917) : or-breslev.co.il, textes du domaine public, repris
    avec l'accord déclaré par le propriétaire de l'application (13/10).

  python3 tools/livres_construction.py <dossier des deux fichiers de version>
"""
import html, json, os, re, sys, unicodedata

NFC = lambda s: unicodedata.normalize("NFC", s)
SORTIE = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "android", "app", "src", "main", "res", "raw", "livres.json")


def nettoie(s):
    s = re.sub(r"<br\s*/?>", "<br>", s)
    s = re.sub(r"<(?!/?b>|br>)[^>]+>", "", s)
    s = re.sub(r"[ \t\r\n]+", " ", s).strip()
    return NFC(s)


def sans_nikkoud(s):
    # Voyelles et signes de cantillation ; le maqaf devient une espace, le geresh et le gershayim restent.
    return re.sub(r"[֑-ֽֿ-ׂׄ-ׇ]", "", s).replace("־", " ")


def titres(schema):
    return {n.get("key", n.get("enTitle")): n["heTitle"] for n in schema["nodes"]}


def segments(x):
    if isinstance(x, dict):
        for v in x.values():
            yield from segments(v)
    elif isinstance(x, list):
        for v in x:
            yield from segments(v)
    elif isinstance(x, str) and x.strip():
        yield x


def midot(v):
    he = titres(v["schema"])
    sections = []
    for cle, chap in v["text"].items():
        blocs = []
        for partie, paras in (chap.items() if isinstance(chap, dict) else [("", chap)]):
            paras = [nettoie(p) for p in segments(paras)]
            if not paras:
                continue
            if partie == "Part II":
                blocs.append({"c": "חלק שני"})
            blocs += [{"t": p} for p in paras if p]
        if blocs:
            sections.append({"titre": NFC(he[cle]), "blocs": blocs})
    return sections


def etsot(v):
    he = titres(v["schema"])
    sections = []
    for cle, chap in v["text"].items():
        paras = [sans_nikkoud(nettoie(p)) for p in segments(chap)]
        titre = sans_nikkoud(NFC(he[cle]))
        # « <b>אמת ואמונה</b><br>א. … » : le titre du chapitre ouvre le premier paragraphe.
        if paras:
            paras[0] = re.sub(r"^<b>[^<]*</b>\s*<br>\s*", "", paras[0])
        blocs = [{"t": p} for p in paras if re.sub(r"<[^>]+>", "", p).strip()]
        if blocs:
            sections.append({"titre": titre, "blocs": blocs})
    return sections


LETTRES = [(400, "ת"), (300, "ש"), (200, "ר"), (100, "ק"), (90, "צ"), (80, "פ"), (70, "ע"), (60, "ס"), (50, "נ"),
           (40, "מ"), (30, "ל"), (20, "כ"), (10, "י"), (9, "ט"), (8, "ח"), (7, "ז"), (6, "ו"), (5, "ה"), (4, "ד"),
           (3, "ג"), (2, "ב"), (1, "א")]


def hebreu(n):
    r = ""
    for v, l in LETTRES:
        while n >= v:
            r += l
            n -= v
    return r.replace("יה", "טו").replace("יו", "טז")


def moharan(v1, v2):
    sections = []
    for prefixe, torot in (("", v1["text"][""]), ("תנינא ", v2["text"]["Part II"])):
        for i, torah in enumerate(torot, 1):
            blocs = [{"t": nettoie(p)} for p in segments(torah) if nettoie(p)]
            if blocs:
                sections.append({"titre": f"{prefixe}תורה {hebreu(i)}", "blocs": blocs})
    return sections


def megila(v):
    return [{"titre": f"פרק {hebreu(c)}", "blocs": [{"t": f"({hebreu(i)}) {nettoie(p)}"} for i, p in enumerate(chap, 1)]}
            for c, chap in enumerate(v["text"], 1)]


def perek_shira(v):
    sections = []
    for c, chap in enumerate(v["text"][""], 1):
        blocs = [{"t": nettoie(p)} for p in chap if nettoie(p)]
        if blocs:
            sections.append({"titre": f"פרק {hebreu(c)}", "blocs": blocs})
    fin = [nettoie(p) for p in v["text"]["Concluding Prayer"] if nettoie(p)]
    if fin:
        titre = re.sub(r"<[^>]+>", "", fin[0]).strip()
        sections.append({"titre": titre, "blocs": [{"t": p} for p in fin[1:]]})
    return sections


def nishmat(v):
    pz = v["text"]["Shabbat Shacharit"]["Pesukei D'Zimra"]
    debut = next(i for i, p in enumerate(pz) if re.sub(r"[֑-ׇ]", "", re.sub(r"<[^>]+>", "", p)).startswith("נשמת כל חי"))
    fin = next(i for i, p in enumerate(pz) if i > debut and re.sub(r"[֑-ׇ]", "", re.sub(r"<[^>]+>", "", p)).startswith("ישתבח"))
    # La note d'usage qui précède « ישתבח » (petits caractères) n'appartient pas à Nichmat.
    blocs = [{"t": nettoie(p)} for p in pz[debut:fin] if not p.lstrip().startswith("<small>")]
    return [{"titre": "נשמת כל חי", "blocs": blocs}]


def paragraphes_orb(chemin):
    """Les paragraphes d'un livre d'or-breslev.co.il (page HTML), dans l'ordre, sans les menus."""
    s = open(chemin, encoding="utf-8").read()
    s = s[:s.rfind("תפריט")] if "תפריט" in s else s
    out = []
    for tag, b in re.findall(r"<(p|h[1-6])\b[^>]*>(.*?)</\1>", s, flags=re.S):
        t = NFC(html.unescape(re.sub(r"<[^>]+>", "", b)).replace("\xa0", " "))
        t = re.sub(r"\s+", " ", t).strip()
        if t:
            out.append(t)
    return out


LETTRE_SEULE = re.compile(r"^(?:אות-)?([א-ת]{1,3})\.?$")


def hishtapchut(chemin):
    ps = paragraphes_orb(chemin)
    ps = ps[ps.index("הקדמה"):]
    sections, courante, partie = [], None, "debut"
    def ouvre(titre):
        nonlocal courante
        courante = {"titre": titre, "blocs": []}
        sections.append(courante)
    for t in ps:
        m = LETTRE_SEULE.match(t)
        if t in ("הקדמה", "פתיחה"):
            ouvre(t)
        elif t.startswith("הִשְׁתַּפְּכוּת הַנֶּפֶשׁ"):
            partie = "corps"
        elif m and partie == "corps":
            if m.group(1) == "א" and sections[-1]["titre"] == "אות קב":
                partie = "fin"
                ouvre("בסוף הספר")
                courante["blocs"].append({"c": "אות א"})
            else:
                ouvre(f"אות {m.group(1)}")
        elif m:
            courante["blocs"].append({"c": f"אות {m.group(1)}"})
        elif t != ".":
            courante["blocs"].append({"t": t})
    return [x for x in sections if x["blocs"]]


def kokhvei_or(chemin):
    ps = paragraphes_orb(chemin)
    ps = ps[ps.index("הקדמה"):]
    sections, courante, partie = [], None, ""
    def ouvre(titre):
        nonlocal courante
        courante = {"titre": titre, "blocs": []}
        sections.append(courante)
    for t in ps:
        m = LETTRE_SEULE.match(t)
        if t.startswith("כוכבי אור חלק"):
            partie = t.replace("כוכבי אור ", "")
            courante = None
        elif t in ("הקדמה", "פתיחה", "ראשית דבר", "***"):
            ouvre(" · ".join(x for x in (partie, t) if x))
        elif re.match(r"^תפלה [א-ת]+'?:$", t):
            ouvre(f"{partie} · {t.rstrip(':')}")
        elif m:
            # Après les prières de la quatrième partie, les lettres reprennent le propos.
            if courante is None or " · תפלה " in courante["titre"]:
                ouvre(partie)
            courante["blocs"].append({"c": m.group(1)})
        else:
            if courante is None:
                ouvre(partie)
            courante["blocs"].append({"t": t})
    return [x for x in sections if x["blocs"]]


def main(dossier):
    v_midot = json.load(open(os.path.join(dossier, "v_SeferHaMidotrabenubook.json"), encoding="utf-8"))
    v_etsot = json.load(open(os.path.join(dossier, "v_LikuteiEtzotrabenubookcom.json"), encoding="utf-8"))
    assert v_midot["license"] == "Public Domain", v_midot["license"]
    src_midot = "Sefer HaMidot — rabenubook (via export Sefaria), Public Domain"
    src_etsot = "Likutei Etzot — rabenubook.com (via export Sefaria) ; sans la vocalisation, de licence non déclarée"
    fichiers = {"lm1": "lm_Likutei_Moharan_-_rabenubook.com.json", "lm2": "lm_Likutei_Moharan_Tinyana_-_rabenubook.com.json",
                "esther": "esther_nikkud.json", "shir": "shir_nikkud.json", "ps": "ps2.json", "edot": "edot_te.json"}
    v = {k: json.load(open(os.path.join(dossier, f), encoding="utf-8")) for k, f in fichiers.items()}
    for k, d in v.items():
        assert d["license"] == "Public Domain", (k, d["license"])
    src = {
        "lm": "Likutei Moharan — rabenubook.com (via export Sefaria), Public Domain",
        "tanakh": "Tanach with Nikkud — tanach.us (via export Sefaria), Public Domain",
        "ps": "Perek Shirah — Torat Emet 357 (via export Sefaria), Public Domain",
        "nishmat": "Siddur Edot HaMizrach — Torat Emet 357 (via export Sefaria), Public Domain",
        "orb": "or-breslev.co.il — domaine public, repris avec l'accord déclaré par le propriétaire de l'application (13/10/2026)",
    }
    sortie = {
        "_meta": {
            "version": "1.0", "date": "2026-10-12",
            "nusah": {"he": "ספרים", "fr": "Livres", "en": "Books"},
            "sources": [src_midot, src["lm"], src["tanakh"], src["ps"], src["nishmat"], src["orb"]],
            "outil": "tools/livres_construction.py",
        },
        "groupes": [{"cle": "livres", "titre": {"he": "סְפָרִים", "fr": "Livres", "en": "Books"}},
                    {"cle": "meguilot", "titre": {"he": "מְגִלּוֹת וּתְפִלּוֹת", "fr": "Méguilot et prières", "en": "Megillot and prayers"}}],
        "textes": [
            {"id": "likoutei_etsot", "groupe": "livres", "source": src_etsot,
             "titre": {"he": "ליקוטי עצות", "fr": "Likoutei Etsot", "en": "Likutei Etzot"}, "sections": etsot(v_etsot)},
            {"id": "sefer_hamidot", "groupe": "livres", "source": src_midot,
             "titre": {"he": "ספר המדות", "fr": "Sefer HaMiddot", "en": "Sefer HaMiddot"}, "sections": midot(v_midot)},
            {"id": "likoutei_moharan", "groupe": "livres", "source": src["lm"],
             "titre": {"he": "ליקוטי מוהר״ן", "fr": "Likoutei Moharan", "en": "Likutei Moharan"}, "sections": moharan(v["lm1"], v["lm2"])},
            {"id": "hichtapkhout", "groupe": "livres", "source": src["orb"],
             "titre": {"he": "השתפכות הנפש", "fr": "Hichtapkhout HaNefech", "en": "Hishtapchut HaNefesh"},
             "sections": hishtapchut(os.path.join(dossier, "hishtapchut.html"))},
            {"id": "kokhvei_or", "groupe": "livres", "source": src["orb"],
             "titre": {"he": "כוכבי אור", "fr": "Kokhvei Or", "en": "Kochvei Or"}, "sections": kokhvei_or(os.path.join(dossier, "kokhvei.html"))},
            {"id": "meguilat_esther", "groupe": "meguilot", "source": src["tanakh"],
             "titre": {"he": "מגילת אסתר", "fr": "Méguilat Esther", "en": "Megillat Esther"}, "sections": megila(v["esther"])},
            {"id": "chir_hachirim", "groupe": "meguilot", "source": src["tanakh"],
             "titre": {"he": "שיר השירים", "fr": "Chir HaChirim", "en": "Shir HaShirim"}, "sections": megila(v["shir"])},
            {"id": "nichmat", "groupe": "meguilot", "source": src["nishmat"],
             "titre": {"he": "נשמת כל חי", "fr": "Nichmat kol 'haï", "en": "Nishmat Kol Chai"}, "sections": nishmat(v["edot"])},
            {"id": "perek_chira", "groupe": "meguilot", "source": src["ps"],
             "titre": {"he": "פרק שירה", "fr": "Perek Chira", "en": "Perek Shira"}, "sections": perek_shira(v["ps"])},
        ],
    }
    json.dump(sortie, open(SORTIE, "w", encoding="utf-8"), ensure_ascii=False, separators=(",", ":"))
    for t in sortie["textes"]:
        print(t["id"], len(t["sections"]), "chapitres", sum(len(s["blocs"]) for s in t["sections"]), "paragraphes")
    print(os.path.getsize(SORTIE) // 1024, "ko")


if __name__ == "__main__":
    main(sys.argv[1])
