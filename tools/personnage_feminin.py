# -*- coding: utf-8 -*-
"""Les poses du personnage au féminin, découpées dans la planche du propriétaire (29/10, audit 1.390).

Usage :  python3 tools/personnage_feminin.py   (Pillow, numpy et scipy requis)

La planche existe deux fois, sur fond blanc et sur fond noir
(tools/sources-images/personnage_feminin_planche{,_noir}.png). Sur le blanc,
la tenue blanche se confond avec le fond : il n'est retiré que là où il est
vraiment blanc (≥ 251) et relié au bord, sinon la chemise part avec lui. Ce
que le blanc mange encore (un pli de manche au coude), le noir le rend : il y
est clair sur noir. Les deux rendus ne se superposent pas au pixel près, donc
le noir ne rend que des morceaux épais, pas le liseré d'un ou deux pixels de
décalage. Les couleurs viennent toutes de la planche blanche.
L'ombre au sol, gris clair, devient une ombre noire translucide : laissée
telle quelle, elle fait un halo blanc sur un fond sombre.

La seconde planche (29/10, six poses : guitare, bras levés, danse, lecture à
genoux, livre contre soi, de dos bras levés) n'existe que sur fond noir, où la
jupe, le foulard et les chaussures sont noirs eux aussi. Le fond n'y est que le
noir pur (≤ 4) relié au bord, ou une poche fermée d'au moins 400 pixels ; la
frange est démélangée du noir ; au bas de la pose, une fermeture rend aux
chaussures leurs reflets noirs, que le fond avait pris.
"""
import pathlib
import numpy as np
from PIL import Image
from scipy import ndimage as nd

RACINE = pathlib.Path(__file__).resolve().parent.parent
PLANCHE = RACINE / 'tools/sources-images/personnage_feminin_planche.png'
PLANCHE_NOIR = RACINE / 'tools/sources-images/personnage_feminin_planche_noir.png'
PLANCHE2 = RACINE / 'tools/sources-images/personnage_feminin_planche2_noir.png'
SEUIL_NOIR = 4
SORTIE = RACINE / 'android/app/src/main/res/drawable-nodpi'
SEUIL = 251
HAUTEUR = 640   # celle des poses du hassid : en wrap_content, les deux tiennent la même place

# nom → (y0, y1, x0, x1) dans la planche, ombre au sol ?
POSES = {
    'welcome':       ((14, 449, 0, 292), True),
    'thumbs_up':     ((14, 449, 293, 532), True),
    'tehilim':       ((20, 415, 534, 748), False),
    'praise':        ((14, 447, 752, 1024), True),
    'writing':       ((460, 737, 0, 270), False),
    'thinking':      ((460, 737, 270, 505), False),
    'pointing':      ((420, 790, 532, 750), False),
    'walking':       ((447, 822, 752, 1024), True),
    'praying':       ((750, 1172, 25, 228), False),
    'joy':           ((740, 1180, 270, 518), True),
    'tehilim_hug':   ((785, 1170, 552, 740), False),
    'offering':      ((818, 1172, 752, 1024), False),
    'reading':       ((1180, 1514, 252, 530), True),
    'standing_back': ((1168, 1530, 560, 720), True),
}
POSES2 = {
    'guitar':        (0, 720, 0, 475),
    'arms_up':       (0, 684, 470, 830),
    'dancing':       (0, 700, 780, 1215),
    'kneel_reading': (720, 1295, 0, 470),
    'book_side':     (686, 1295, 470, 800),
    'back_arms':     (640, 1295, 800, 1215),
}
# La planche a aussi une pose assise qui lace ses chaussures et un portrait :
# aucun écran ne les demande, ils ne sont pas livrés.


def pose(a, noir, ombre):
    mn, mx = a.min(2), a.max(2)
    blanc = (mn >= SEUIL) & ((mx - mn) <= 12)
    lab, _ = nd.label(blanc)
    bords = set(np.unique(np.r_[lab[0], lab[-1], lab[:, 0], lab[:, -1]])) - {0}
    fond = np.isin(lab, list(bords))
    fond &= ~nd.binary_opening((noir.max(2) > 60) & fond, iterations=2)
    # Le personnage : la plus grande tache, et ce qui lui est comparable (un
    # livre, une table). Les bouts de la pose voisine restent dehors.
    lab2, n = nd.label(~fond)
    tailles = nd.sum(~fond, lab2, range(1, n + 1))
    garde = np.isin(lab2, [k + 1 for k, t in enumerate(tailles) if t >= 0.15 * tailles.max()])
    alpha = np.where(garde, 255.0, 0.0)
    frange = garde & ~nd.binary_erosion(garde)
    alpha[frange] = np.clip((255 - mn[frange]) / (255 - SEUIL) * 127.5 + 128, 0, 255)
    # Sur la frange, la couleur est un mélange avec le blanc du fond : on
    # retire ce blanc, sinon il reste un liseré clair sur un fond sombre.
    k = (alpha[frange] / 255.0)[:, None]
    rgb = a.astype(float)
    rgb[frange] = np.clip((rgb[frange] - (1 - k) * 255) / k, 0, 255)
    rgb = rgb.astype(np.uint8)
    if ombre:
        h = garde.shape[0]
        lum = a.mean(2)
        bas = np.zeros_like(garde)
        bas[int(h * 0.86):] = True
        claire = garde & bas & ((mx - mn) <= 14) & (lum > 150)
        lab3, _ = nd.label(claire | fond)
        o = claire & np.isin(lab3, list(set(np.unique(lab3[fond])) - {0}))
        rgb[o] = 0
        alpha[o] = np.clip((255 - lum[o]) * 1.6, 0, 255)
    im = Image.fromarray(np.dstack([rgb, alpha.astype(np.uint8)]), 'RGBA')
    im = im.crop(im.getbbox())
    return im.resize((round(im.width * HAUTEUR / im.height), HAUTEUR), Image.LANCZOS)


def personnage(garde):
    """La plus grande tache et ce qui lui est comparable, sans trous."""
    lab, n = nd.label(garde)
    tailles = nd.sum(garde, lab, range(1, n + 1))
    return nd.binary_fill_holes(np.isin(lab, [k + 1 for k, t in enumerate(tailles) if t >= 0.15 * tailles.max()]))


def pose_noir(a):
    mx = a.max(2)
    noir = mx <= SEUIL_NOIR
    lab, n = nd.label(noir)
    tailles = nd.sum(noir, lab, range(1, n + 1))
    bords = set(np.unique(np.r_[lab[0], lab[-1], lab[:, 0], lab[:, -1]])) - {0}
    fond = np.isin(lab, [k for k in range(1, n + 1) if k in bords or tailles[k - 1] >= 400])
    garde = personnage(~fond)
    # Les chaussures : leur noir pur touche le sol, le fond l'a pris.
    ys = np.where(garde.any(1))[0]
    bas = np.zeros_like(garde)
    bas[ys[-1] - int((ys[-1] - ys[0]) * 0.13):] = True
    ferme = nd.binary_closing(np.pad(garde & bas, 20), structure=np.ones((17, 17)))[20:-20, 20:-20]
    garde |= nd.binary_fill_holes(ferme) & bas
    # La frange : son opacité est sa clarté rapportée à celle de l'intérieur
    # voisin, et sa couleur est démélangée du noir.
    interieur = nd.binary_erosion(garde, iterations=3)
    frange = garde & ~interieur
    ref = nd.grey_dilation(np.where(interieur, mx, 0), size=9).astype(float)
    k = np.clip(mx / np.where(ref > 0, ref, 255.0), 0, 1)
    alpha = np.where(garde, 255.0, 0.0)
    alpha[frange] = k[frange] * 255
    rgb = a.astype(float)
    rgb[frange] = np.clip(rgb[frange] / np.maximum(k[frange], 1e-3)[:, None], 0, 255)
    im = Image.fromarray(np.dstack([rgb.astype(np.uint8), alpha.astype(np.uint8)]), 'RGBA')
    im = im.crop(im.getbbox())
    return im.resize((round(im.width * HAUTEUR / im.height), HAUTEUR), Image.LANCZOS)


def enregistre(im, nom):
    cible = SORTIE / f'char_f_{nom}.webp'
    im.save(cible, 'WEBP', quality=85, method=6)
    print(f'{cible.name:28s} {im.width}×{im.height}  {cible.stat().st_size // 1024} ko')


def main():
    planche = np.asarray(Image.open(PLANCHE).convert('RGB')).astype(np.int16)
    noir = np.asarray(Image.open(PLANCHE_NOIR).convert('RGB')).astype(np.int16)
    for nom, ((y0, y1, x0, x1), ombre) in POSES.items():
        enregistre(pose(planche[y0:y1, x0:x1], noir[y0:y1, x0:x1], ombre), nom)
    planche2 = np.asarray(Image.open(PLANCHE2).convert('RGB')).astype(np.int16)
    for nom, (y0, y1, x0, x1) in POSES2.items():
        enregistre(pose_noir(planche2[y0:y1, x0:x1]), nom)


if __name__ == '__main__':
    main()
