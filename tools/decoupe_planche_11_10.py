"""Découpe la planche du 11/10/2026 (4 colonnes, 5 rangées, 20 images).

Les images sur fond blanc sont détourées comme celles de la première
planche (tools/decoupe_planche.py, fond repéré par connexité depuis le
bord) ; les scènes qui ont leur propre décor (le lit, la souka, la
cuisine, le paysage, la synagogue, l'aéroport, la ménora) gardent leur cadre :
retirer le décor les viderait de leur sens.

    python3 tools/decoupe_planche_11_10.py <planche.png> <dossier de sortie>
"""
import os, sys
import numpy as np
from PIL import Image
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from decoupe_planche import cut_out, coupures

NOMS = [
    "scene_sleeping", "scene_sukkot", "char_matsa", "scene_hanukkah",
    "char_purim", "char_tefillin", "char_studying", "char_backpack",
    "scene_cooking", "char_board_game", "scene_nature", "char_child",
    "char_headphones", "scene_tallit_torah", "char_sport", "scene_airport",
    "char_guitar_seated", "scene_shabbat", "char_open_hands", "char_portrait",
]
# La ménora de 'Hanouka a le décor d'une pièce ; à la table de Chabbat, le
# détourage emportait la flamme des bougies avec le fond blanc.
SCENES = {"scene_sleeping", "scene_sukkot", "scene_cooking", "scene_nature", "scene_tallit_torah", "scene_airport",
          "scene_hanukkah", "scene_shabbat"}


def cases(sheet, cols=4, rows=5):
    a = np.asarray(sheet.convert("RGB")).astype(np.int16)
    plein = a.min(axis=2) < 240
    H, W = plein.shape
    boites = [[None] * cols for _ in range(rows)]
    for c in range(cols):
        ys = coupures(plein[:, c * W // cols:(c + 1) * W // cols].sum(axis=1), rows)
        for r in range(rows):
            xs = coupures(plein[ys[r]:ys[r + 1]].sum(axis=0), cols)
            boites[r][c] = (xs[c], ys[r], xs[c + 1], ys[r + 1])
    return [b for rangee in boites for b in rangee]


def scene(im):
    """Le cadre de la scène, sans la marge blanche de la planche."""
    a = np.asarray(im.convert("RGB")).astype(np.int16)
    plein = a.min(axis=2) < 245
    ys, xs = np.where(plein)
    return im.crop((xs.min(), ys.min(), xs.max() + 1, ys.max() + 1)).convert("RGB")


if __name__ == "__main__":
    sheet = Image.open(sys.argv[1]).convert("RGB")
    sortie = sys.argv[2]
    os.makedirs(sortie, exist_ok=True)
    for i, (nom, box) in enumerate(zip(NOMS, cases(sheet))):
        tuile = sheet.crop(box)
        if nom in SCENES:
            im = scene(tuile)
        else:
            im, total, kept = cut_out(tuile)
            im = im.crop(im.getbbox())
        # Une scène garde son décor : « scene_ », hors du contrôle des
        # personnages détourés (tools/verifie_illustrations.py, char_*).
        p = os.path.join(sortie, nom + ".webp")
        im.save(p, "WEBP", quality=92, method=6)
        print(f"{i+1:2}. {nom:20} {box} → {im.size} {'scène' if nom in SCENES else 'détouré'}")
