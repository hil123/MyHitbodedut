"""Découpe la planche de 16 portraits, détoure le fond blanc, exporte en WebP.

Sert à régénérer les illustrations de res/drawable-nodpi à partir des rendus
d'origine. Le fond de ces rendus est mesuré à 254-255, et la chemise du
personnage monte elle aussi à 255 : un simple seuil trouerait le vêtement.
On repère donc le fond par connexité depuis le bord de l'image.

    python3 -m pip install Pillow numpy scipy
    python3 tools/decoupe_planche.py      # lit images/, écrit cut/
"""
from PIL import Image, ImageFilter
from scipy import ndimage
import numpy as np
import os

SRC, OUT = "images", "cut"
WHITE = 253          # le fond mesuré est à 254-255
MIN_PART = 0.006     # part minimale d'une composante conservée

def cut_out(im):
    a = np.asarray(im.convert("RGB")).astype(np.int16)
    candidate = a.min(axis=2) >= WHITE

    # Le fond est la partie quasi blanche reliée au bord de l'image.
    lab, _ = ndimage.label(candidate)
    edge = set(lab[0, :]) | set(lab[-1, :]) | set(lab[:, 0]) | set(lab[:, -1])
    edge.discard(0)
    bg = np.isin(lab, sorted(edge))
    solid = ~bg

    # On ne garde que les composantes assez grandes ET passant par le centre :
    # les bouts du voisin happés par la découpe restent collés aux bords.
    lab2, n2 = ndimage.label(solid)
    kept = 0
    if n2:
        h, w = solid.shape
        centre = np.zeros_like(solid)
        centre[int(0.15 * h):int(0.85 * h), int(0.20 * w):int(0.80 * w)] = True
        sizes = np.bincount(lab2.ravel())
        central = set(np.unique(lab2[centre])) - {0}
        big = [i for i in central if sizes[i] >= MIN_PART * solid.size]
        if big:
            solid = np.isin(lab2, big)
            kept = len(big)

    alpha = Image.fromarray(np.where(solid, 255, 0).astype(np.uint8), "L")
    # Le fond étant blanc, le contour reste cerné d'un liseré clair : deux pixels
    # d'érosion l'emportent, le flou rattrape ensuite l'escalier du bord.
    alpha = alpha.filter(ImageFilter.MinFilter(5)).filter(ImageFilter.GaussianBlur(1.0))
    out = im.convert("RGBA")
    out.putalpha(alpha)
    return out, n2, kept

def finish(im, name, max_side):
    box = im.getbbox()
    if box:
        im = im.crop(box)
    w, h = im.size
    s = min(max_side / max(w, h), 1.0)
    if s < 1.0:
        im = im.resize((round(w * s), round(h * s)), Image.LANCZOS)
    p = os.path.join(OUT, name + ".webp")
    im.save(p, "WEBP", quality=90, method=6)
    return im.size, os.path.getsize(p)

NAMES = [
    "char_welcome", "char_thumbs_up", "char_tehilim", "char_praise",
    "char_writing", "char_thinking", "char_pointing", "char_walking",
    "char_praying", "char_dancing", "char_torah", "char_offering",
    "char_sitting_low", "char_reading_seated", "char_standing_back", "char_face",
]
STANDALONE = {
    "2.webp": "char_dancing_joy", "3.webp": "char_guitar",
    "4.webp": "char_arms_raised", "5.webp": "char_arms_raised_guitar",
}

if __name__ == "__main__":
    sheet = Image.open(os.path.join(SRC, "1.webp"))
    W, H = sheet.size
    cw, ch = W // 4, H // 4
    for i, name in enumerate(NAMES):
        r, c = divmod(i, 4)
        cell = sheet.crop((c * cw, r * ch, (c + 1) * cw, (r + 1) * ch))
        cut, total, kept = cut_out(cell)
        size, weight = finish(cut, name, 640)
        print(f"{i+1:2}. {name:24} {str(size):12} {weight//1024:3} Ko  gardé {kept}/{total}")
    for src, name in STANDALONE.items():
        cut, total, kept = cut_out(Image.open(os.path.join(SRC, src)))
        size, weight = finish(cut, name, 900)
        print(f"    {name:24} {str(size):12} {weight//1024:3} Ko  gardé {kept}/{total}")
