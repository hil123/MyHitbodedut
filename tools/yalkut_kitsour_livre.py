# -*- coding: utf-8 -*-
"""
Le ילקוט יוסף – קיצור שולחן ערוך en entier, comme livre de la bibliothèque (19/10).

Source : Torat Emet, f_01355_all.html (windows-1255), téléchargé par le workflow
« Source Yalkut Yosef » (branche claude/source-yalkut) : l'environnement de
développement n'atteint pas toratemetfreeware.com.

Droits : « כל הזכויות שמורות למו"ר הרב יצחק יוסף שליט"א – אסור להעתיק קטעים
מהספר ללא ציון המקור » ; saisie Torat Emet, CC BY-NC-SA 2.5, usage non
commercial. Licence reçue par le propriétaire de l'application, avec mention du
crédit « הרב יצחק יוסף שליט״א » (déclaration du 19/10/2026). Le crédit ouvre le
livre, et chaque se'if garde ses renvois entre crochets.

Structure du fichier : <!--$@--> ouvre un groupe de halakhot (« הלכות ציצית »),
<!--$~--> un siman, <!--$!--> un se'if (sa lettre en tête).

  python3 tools/yalkut_kitsour_livre.py <dossier de la branche source> <sortie.json>

Puis le texte sortie.json remplace celui d'id yalkut_yosef_kitsour dans res/raw/livres.json (groupe « halakha »).
"""
import html, json, re, sys

def propre(x):
    x = re.sub(r"<small>\s*<small>", "<small>", x)
    x = re.sub(r"</small>\s*</small>", "</small>", x)
    x = re.sub(r"<(?!/?small\b)[^>]+>", " ", x)
    x = html.unescape(x).replace("\xa0", " ").replace("''", "״")
    x = re.sub(r"\s+", " ", x).strip()
    return x

def lire(dossier):
    s = open(f"{dossier}/f_01355_all.html", encoding="cp1255", errors="replace").read()
    corps = s[s.find("<!--$@-->"):] if "<!--$@-->" in s else s
    # L'avant-propos (avant le premier groupe) : l'introduction de l'auteur.
    morceaux = re.split(r"<!--\$([@~!])-->", s)
    sections, groupe = [], None
    debut = morceaux[0]
    for k in range(1, len(morceaux), 2):
        sorte, contenu = morceaux[k], morceaux[k + 1]
        titre = propre(re.sub(r"<small>.*?</small>", "", contenu.split("</span>")[0] if sorte != "!" else ""))
        if sorte == "@":
            groupe = propre(contenu)
            continue
        if sorte == "~":
            m = re.search(r"<u>(.*?)</u>", contenu, re.S)
            t = propre(m.group(1)) if m else propre(contenu)[:80]
            sections.append({"titre": t, "blocs": []})
            if groupe:
                sections[-1]["blocs"].append({"c": groupe})
                groupe = None
            continue
        # Un se'if : la lettre, puis le texte.
        m = re.search(r"<p></p>\s*([^<]{1,4})</span></B>&nbsp;</span>(.*)", contenu, re.S)
        if not m or not sections:
            continue
        lettre, texte = propre(m.group(1)), propre(m.group(2))
        if texte:
            sections[-1]["blocs"].append({"t": f"<b>{lettre}</b> {texte}"})
    return [s for s in sections if any("t" in b for b in s["blocs"])]

if __name__ == "__main__":
    sections = lire(sys.argv[1])
    # Le crédit, en tête du premier siman.
    sections[0]["blocs"].insert(0, {"c": "מאת הרב יצחק יוסף שליט״א · מהדורת התשס״ז · כל הזכויות שמורות למחבר · הוכן מתוכנת תורת אמת (CC BY-NC-SA 2.5)"})
    texte = {"id": "yalkut_yosef_kitsour", "groupe": "halakha",
             "titre": {"he": "ילקוט יוסף – קיצור שולחן ערוך", "fr": "Yalkout Yossef – Kitsour Choul'han Aroukh", "en": "Yalkut Yosef – Kitzur Shulchan Aruch"},
             "source": "Kitsour Choulhan Aroukh Yalkut Yosef, Rav Yitzhak Yosef chlita — Torat Emet (CC BY-NC-SA 2.5) ; licence reçue par le propriétaire, crédit « הרב יצחק יוסף שליט״א » (19/10/2026)",
             "sections": sections}
    json.dump(texte, open(sys.argv[2], "w", encoding="utf-8"), ensure_ascii=False, separators=(",", ":"))
    n = sum(len([b for b in s["blocs"] if "t" in b]) for s in sections)
    print(len(sections), "simanim,", n, "se'ifim")
