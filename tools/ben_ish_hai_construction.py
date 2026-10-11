# -*- coding: utf-8 -*-
"""
La halakha du jour hors ligne (demande du 11/10/2026 : « le même système
que les hilloulot pour la הלכה יומית »).

Le site halachayomit.co.il (Rav Ovadia Yossef) réserve tous les droits sur
ses textes : on n'en reprend rien, l'application garde seulement un lien.
Le texte embarqué est celui du Ben Ish 'Haï (Rabbi Yossef 'Hayim de
Bagdad), Halakhot, première et deuxième année, en hébreu, tel que Sefaria
l'exporte depuis Wikisource (licence : domaine public) :
  https://storage.googleapis.com/sefaria-export/json/Halakhah/Ben%20Ish%20Hai/Hebrew/Ben%20Ish%20Hai%20--%20Wikisource.json
L'édition de Jérusalem 1898 du même export est vide pour les Halakhot ; les
traductions anglaises ne couvrent que 76 halakhot sur 2 022 : on ne les
prend pas, le texte reste en hébreu, verbatim (balises <br> → saut de ligne).

Le livre suit la paracha de la semaine. Clés : les noms de l'énumération
Parsha de KosherJava (les mêmes dans kosher-zmanim) ; une section que
Sefaria donne pour deux parachiot jointes garde la clé jointe
(TAZRIA_METZORA...). Le découpage par jour est fait par l'application
(Halakha.kt, halakha.js).

    python3 tools/ben_ish_hai_construction.py <export.json>
"""
import json, re, sys

CLES = {
    "Bereshit": "BERESHIS", "Noach": "NOACH", "Lech Lecha": "LECH_LECHA", "Vayera": "VAYERA",
    "Chayei Sara": "CHAYEI_SARA", "Toldot": "TOLDOS", "Vayetzei": "VAYETZEI", "Vayishlach": "VAYISHLACH",
    "Vayeshev": "VAYESHEV", "Miketz": "MIKETZ", "Vayigash": "VAYIGASH", "Vayechi": "VAYECHI",
    "Shemot": "SHEMOS", "Vaera": "VAERA", "Bo": "BO", "Beshalach": "BESHALACH", "Yitro": "YISRO",
    "Mishpatim": "MISHPATIM", "Terumah": "TERUMAH", "Tetzaveh": "TETZAVEH", "Ki Tisa": "KI_SISA",
    "Vayakhel": "VAYAKHEL", "Pekudei": "PEKUDEI", "Vayikra": "VAYIKRA", "Tzav": "TZAV", "Shmini": "SHMINI",
    "Tazria": "TAZRIA", "Metzora": "METZORA", "Tazria Metzora": "TAZRIA_METZORA",
    "Achrei Mot": "ACHREI_MOS", "Kedoshim": "KEDOSHIM", "Achrei Mot Kedoshim": "ACHREI_MOS_KEDOSHIM",
    "Emor": "EMOR", "Behar Bechukotai": "BEHAR_BECHUKOSAI", "Bamidbar": "BAMIDBAR", "Nasso": "NASSO",
    "Beha'alotcha": "BEHAALOSCHA", "Sh'lach": "SHLACH", "Korach": "KORACH", "Chukat": "CHUKAS",
    "Balak": "BALAK", "Pinchas": "PINCHAS", "Matot": "MATOS", "Masei": "MASEI", "Devarim": "DEVARIM",
    "Vaetchanan": "VAESCHANAN", "Eikev": "EIKEV", "Re'eh": "REEH", "Shoftim": "SHOFTIM",
    "Ki Teitzei": "KI_SEITZEI", "Ki Tavo": "KI_SAVO", "Nitzavim": "NITZAVIM", "Vayeilech": "VAYEILECH",
    "Ha'Azinu": "HAAZINU", "V'Zot HaBerachah": "VZOS_HABERACHA", "Chanukah": "HANOUKA",
}


def propre(s):
    s = " ".join(s) if isinstance(s, list) else s
    s = re.sub(r"<br\s*/?>", "\n", s or "")
    return re.sub(r"<[^>]+>", "", s).strip()


def construit(export):
    d = json.load(open(export, encoding="utf-8"))
    noms = {}
    for n in d["schema"]["nodes"]:
        if n.get("enTitle", "").startswith("Halachot"):
            noms[n["enTitle"]] = {p["enTitle"]: p["heTitle"] for p in n["nodes"]}
    sortie = {"annees": {}, "hanouka": None}
    total = 0
    for i, annee in enumerate(["Halachot 1st Year", "Halachot 2nd Year"], 1):
        sections = {}
        for p, v in d["text"][annee].items():
            h = [propre(x) for x in v.get("", [])]
            h = [x for x in h if x]
            if not h:
                continue
            total += len(h)
            sec = {"he": noms[annee][p], "h": h}
            if CLES[p] == "HANOUKA":
                sortie["hanouka"] = sec
            else:
                sections[CLES[p]] = sec
        sortie["annees"][str(i)] = sections
    return sortie, total


if __name__ == "__main__":
    s, n = construit(sys.argv[1])
    json.dump(s, open("android/app/src/main/res/raw/ben_ish_hai.json", "w", encoding="utf-8"),
              ensure_ascii=False, separators=(",", ":"))
    print(n, "halakhot ;", {k: len(v) for k, v in s["annees"].items()}, "sections ; Hanouka", len(s["hanouka"]["h"]))
