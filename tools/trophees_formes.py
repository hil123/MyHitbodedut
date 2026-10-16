# -*- coding: utf-8 -*-
"""
Les objets des trophées (12/10, d'après les planches d'inspiration
envoyées par le propriétaire) : coupe à anses, écusson aux lauriers, étoile
sur pied, rosette crantée, médaille à ruban, pile de pièces, trophée de
cristal, diamant. Une seule description, en tracés SVG dans une boîte de
100 × 100, avec la matière de chaque partie :

  m  métal (dégradé en diagonale, clair → foncé)
  mr métal bombé (dégradé radial, éclairé en haut à gauche)
  md métal sombre (tranches, anses, bords)
  v  verre ; vc verre clair (facette éclairée)
  r  ruban rouge
  w  reflet blanc translucide

Le même fichier sert à Android (res/raw/trophees_formes.json, dessiné par
TropheeDessin) et au web (web/src/js/trophees-formes.js).

  python3 tools/trophees_formes.py
"""
import json, math, os

RACINE = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")


def cercle(cx, cy, r):
    return f"M{cx - r:.2f} {cy:.2f} A{r} {r} 0 1 0 {cx + r:.2f} {cy:.2f} A{r} {r} 0 1 0 {cx - r:.2f} {cy:.2f} Z"


def polygone(points):
    return "M" + " L".join(f"{x:.2f} {y:.2f}" for x, y in points) + " Z"


def etoile():
    cx, cy, R, r, n = 50, 38, 34, 15, 8
    pts = []
    for i in range(2 * n):
        a = -math.pi / 2 + i * math.pi / n
        rr = R if i % 2 == 0 else r
        pts.append((cx + rr * math.cos(a), cy + rr * math.sin(a)))
    parts = []
    for i in range(2 * n):
        a, b = pts[i], pts[(i + 1) % (2 * n)]
        parts.append(["m" if i % 2 == 0 else "md", polygone([(cx, cy), a, b])])
    return parts


def rosette():
    cx, cy = 50, 46
    pts = []
    for i in range(48):
        a = i * 2 * math.pi / 48
        rr = 40 if i % 2 == 0 else 36
        pts.append((cx + rr * math.cos(a), cy + rr * math.sin(a)))
    return [
        ["r", "M38 72 L28 97 L37 91 L44 98 L49 76 Z"],
        ["r", "M62 72 L72 97 L63 91 L56 98 L51 76 Z"],
        ["md", polygone(pts)],
        ["m", cercle(cx, cy, 31)],
        ["mr", cercle(cx, cy, 25)],
        ["w", "M27 40 C30 28 40 20 52 19 C42 24 34 32 31 44 Z"],
    ]


def pieces():
    parts = []
    for i, y in enumerate([84, 75, 66, 57, 48]):
        parts.append(["md", f"M20 {y} H80 V{y + 8} A30 7 0 0 1 20 {y + 8} Z"])
        parts.append(["m" if i < 4 else "mr", f"M20 {y} A30 7 0 0 1 80 {y} A30 7 0 0 1 20 {y} Z"])
    parts.append(["md", cercle(70, 30, 15)])
    parts.append(["mr", cercle(70, 30, 12)])
    return parts


FORMES = {
    "coupe": {"embleme": [50, 34, 20], "parts": [
        ["md", "M26 20 C6 20 6 52 32 56 L34 50 C16 46 15 28 27 27 Z"],
        ["md", "M74 20 C94 20 94 52 68 56 L66 50 C84 46 85 28 73 27 Z"],
        ["m", "M21 13 H79 C79 44 67 60 50 63 C33 60 21 44 21 13 Z"],
        ["md", "M19 10 H81 V17 H19 Z"],
        ["m", "M46 63 H54 L56 76 H44 Z"],
        ["md", "M39 76 H61 V81 H39 Z"],
        ["mr", "M31 81 H69 L73 93 H27 Z"],
        ["w", "M27 18 C29 36 35 49 44 55 C35 47 32 34 33 18 Z"],
    ]},
    "ecusson": {"embleme": [50, 46, 22], "parts": [
        ["md", "M31 86 C14 74 8 52 13 26 C19 30 21 38 21 44 C17 40 15 36 15 32 C15 50 21 64 33 76 Z"],
        ["md", "M69 86 C86 74 92 52 87 26 C81 30 79 38 79 44 C83 40 85 36 85 32 C85 50 79 64 67 76 Z"],
        ["m", "M50 9 L81 19 V47 C81 66 67 80 50 89 C33 80 19 66 19 47 V19 Z"],
        ["mr", "M50 16 L74 24 V47 C74 62 63 74 50 81 C37 74 26 62 26 47 V24 Z"],
        ["w", "M26 24 L50 16 V38 C40 38 32 43 26 50 Z"],
    ]},
    "etoile": {"embleme": [50, 38, 14], "parts": etoile() + [
        ["md", "M47 62 H53 L55 86 H45 Z"],
        ["m", "M34 86 H66 L68 93 H32 Z"],
    ]},
    "rosette": {"embleme": [50, 46, 22], "parts": rosette()},
    "medaille": {"embleme": [50, 62, 20], "parts": [
        ["r", "M28 3 H44 L57 40 H41 Z"],
        ["r", "M72 3 H56 L43 40 H59 Z"],
        ["md", cercle(50, 62, 31)],
        ["m", cercle(50, 62, 27)],
        ["mr", cercle(50, 62, 21)],
        ["w", "M30 56 C33 44 42 37 54 36 C44 41 37 48 34 60 Z"],
    ]},
    "pieces": {"embleme": [70, 30, 12], "parts": pieces()},
    "cristal": {"embleme": [50, 46, 20], "parts": [
        ["v", "M33 7 L68 19 L61 88 H39 Z"],
        ["vc", "M33 7 L50 13 L50 88 H39 Z"],
        ["w", "M37 14 L44 16 L42 70 L39 72 Z"],
    ]},
    "gemme": {"embleme": [50, 40, 14], "parts": [
        ["m", "M22 34 H78 L50 84 Z"],
        ["md", "M22 34 L50 84 L39 34 Z"],
        ["mr", "M61 34 L50 84 L78 34 Z"],
        ["mr", "M22 34 L35 17 H65 L78 34 Z"],
        ["w", "M35 17 L42 34 H22 Z"],
        ["md", "M50 34 L65 17 L57 34 Z"],
    ]},
}


# Les icônes des familles (16/10, variante B choisie sur planche) : au trait, boîte 24 × 24,
# trait de 2, bouts et angles arrondis — le style des marque-pages. Cercles et rectangles
# écrits en tracés, pour que PathParser (Android) et SVG (web) lisent la même chaîne.
ICONES = {
    "premier_pas": "M8 4.5c1.7 0 2.5 1.8 2.5 4s-.8 4.5-2.5 4.5S5.5 10.7 5.5 8.5 6.3 4.5 8 4.5z"
                   "M6.5 15.5h3v2a1.5 1.5 0 0 1-3 0z"
                   "M16 8.5c1.7 0 2.5 1.8 2.5 4s-.8 4.5-2.5 4.5-2.5-2.3-2.5-4.5.8-4 2.5-4z"
                   "M14.5 19.5h3",
    "seance_longue": "M4 13a8 8 0 1 0 16 0a8 8 0 1 0-16 0z" "M12 9v4l2.5 2.5" "M10 2.5h4",
    "jours": "M5.5 5h13a2 2 0 0 1 2 2v11.5a2 2 0 0 1-2 2h-13a2 2 0 0 1-2-2V7a2 2 0 0 1 2-2z"
             "M3.5 10h17" "M8 3v4" "M16 3v4" "M8.5 15l2.2 2.2 4.8-4.7",
    "serie": "M5.5 9h2a3 3 0 0 1 0 6h-2a3 3 0 0 1 0-6z"
             "M11 9h2a3 3 0 0 1 0 6h-2a3 3 0 0 1 0-6z"
             "M16.5 9h2a3 3 0 0 1 0 6h-2a3 3 0 0 1 0-6z",
    "heures": "M5 3.5h14" "M5 20.5h14" "M6.5 12a5.5 5.5 0 1 0 11 0a5.5 5.5 0 1 0-11 0z" "M12 9.5V12l1.8 1.2",
    "semaines": "M12 3l7.5 3v5.5c0 4.5-3.2 8-7.5 9.5-4.3-1.5-7.5-5-7.5-9.5V6z" "M8.5 12l2.5 2.5 4.5-5",
    "erev_chabbat": "M10.5 10h3a1 1 0 0 1 1 1v8.5a1 1 0 0 1-1 1h-3a1 1 0 0 1-1-1V11a1 1 0 0 1 1-1z"
                    "M12 7.5c-1.2 0-2-.9-2-2C10 4 12 2.5 12 2.5s2 1.5 2 3c0 1.1-.8 2-2 2z" "M6.5 20.5h11",
    "retour": "M14 5l-6 7 6 7" "M8 12h8.5a4 4 0 0 1 4 4v3",
    "plume": "M12 3l5 6-5 12-5-12z" "M10.6 11a1.4 1.4 0 1 0 2.8 0a1.4 1.4 0 1 0-2.8 0z" "M12 12.4V21",
}


def main():
    donnees = {"_meta": {"outil": "tools/trophees_formes.py", "boite": 100,
                         "matieres": "m métal, mr métal bombé, md métal sombre, v verre, vc verre clair, r ruban, w reflet"},
               "formes": FORMES, "icones": ICONES}
    json.dump(donnees, open(os.path.join(RACINE, "android/app/src/main/res/raw/trophees_formes.json"), "w", encoding="utf-8"),
              ensure_ascii=False, separators=(",", ":"))
    with open(os.path.join(RACINE, "web/src/js/trophees-formes.js"), "w", encoding="utf-8") as f:
        f.write("// Généré par tools/trophees_formes.py : les objets des trophées, en tracés SVG (boîte 100 × 100).\n")
        f.write("export const FORMES = " + json.dumps(FORMES, ensure_ascii=False) + ";\n")
        f.write("// Les icônes des familles, au trait (boîte 24 × 24).\n")
        f.write("export const ICONES = " + json.dumps(ICONES, ensure_ascii=False) + ";\n")
    print({k: len(v["parts"]) for k, v in FORMES.items()})


if __name__ == "__main__":
    main()
