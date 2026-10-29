# -*- coding: utf-8 -*-
"""
Détourage agrandi (demande du 11/10/2026 : « zoom plus grand chacune des
photos puis applique le process pour enlever l'arrière-plan ; tu fais souvent
l'erreur de laisser entre le visage et les péot »).

1. La case de la planche d'origine (fond blanc vrai) est agrandie ×3
   (Lanczos) : le modèle isnet-anime de rembg voit les fins espaces entre
   péot, barbe et cou trois fois plus larges.
2. Les espaces de fond enfermés par la figure, que le modèle garde opaques,
   sont repérés sur la case d'origine par trous() : composante proche du
   fond qui ne touche pas le bord, d'un gris neutre (le fond de la planche ;
   la chemise et la kippa ont un reflet chaud ou une ombre), bordée de
   cheveux sombres, hors du sommet de la tête. Ils deviennent transparents.
3. Les miettes (moins de 2 % de la plus grande composante) partent ; l'image
   est réduite à 640 px au plus.

Les scènes (scene_*) gardent leur décor, l'aéroport compris.
Les quatre figures sans planche d'origine (arms_raised, dancing_joy,
guitar, open_arms) reçoivent seulement l'étape 2, sur leur version détourée
posée sur la couleur du fond.

    python3 tools/detourage_zoom.py <planche_11_10.png> <dossier drawable>
"""
import os, sys
import numpy as np
from PIL import Image
from scipy import ndimage
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

F = 3
FOND = np.array([253, 254, 253])


def trous(t, haut=0.55, taille=25, neutre=1.6, cheveux=0.2):
    """Fond enfermé (entre péot et cou, entre bras et corps) sur une case à fond blanc."""
    t = t.astype(int)
    bg = np.median(np.concatenate([t[0], t[-1], t[:, 0], t[:, -1]]), axis=0)
    d = np.abs(t - bg).max(-1)
    lab, k = ndimage.label(d <= 6)
    bord = set(np.unique(np.concatenate([lab[0], lab[-1], lab[:, 0], lab[:, -1]])))
    m = np.zeros(d.shape, bool)
    H = t.shape[0]
    haut_fig = np.nonzero((d > 6).any(1))[0][0]
    for i, sl in enumerate(ndimage.find_objects(lab), 1):
        if i in bord or sl is None:
            continue
        c = lab[sl] == i
        if c.sum() < taille or sl[0].start > haut * H:
            continue
        mu = t[sl][c].mean(0)
        if abs(mu[0] - mu[2]) > neutre or mu.min() < 248:
            continue
        cc = lab == i
        # le sommet de la tête (kippa blanche contre les cheveux) n'est jamais un trou
        if ndimage.center_of_mass(cc)[0] < haut_fig + 0.12 * H:
            continue
        # un vrai trou touche des cheveux sombres (péot, barbe)
        ring = ndimage.binary_dilation(cc, iterations=3) & ~cc & (d > 6)
        if (t[ring].mean(-1) < 90).mean() < cheveux:
            continue
        m |= cc
    # la frange anti-crénelée autour
    return m | (ndimage.binary_dilation(m, iterations=1) & (d <= 14))


def miettes(alpha):
    lab, k = ndimage.label(alpha > 40)
    if k:
        sz = np.bincount(lab.ravel()); sz[0] = 0
        alpha = np.where((sz >= 0.02 * sz.max())[lab], alpha, 0)
    return alpha


def detoure(tuile, session, max_cote=640):
    from rembg import remove
    t = np.array(tuile.convert('RGB'))
    m = trous(t)
    grand = tuile.convert('RGB').resize((t.shape[1] * F, t.shape[0] * F), Image.LANCZOS)
    g = np.array(grand).astype(int)
    r = np.array(remove(grand, session=session, post_process_mask=True).convert('RGBA'))
    M = np.kron(m, np.ones((F, F), bool))
    M = ndimage.binary_dilation(M, iterations=F) & (np.abs(g - FOND).max(-1) <= 14) | M
    al = r[..., 3].astype(np.int32)
    al[M] = 0
    r[..., 3] = miettes(al).astype(np.uint8)
    im = Image.fromarray(r); im = im.crop(im.getbbox())
    s = min(1.0, max_cote / max(im.size))
    return im.resize((round(im.width * s), round(im.height * s)), Image.LANCZOS), int(m.sum())


def sans_planche(chemin):
    a = np.array(Image.open(chemin).convert('RGBA'))
    al = a[..., 3:4].astype(float) / 255
    comp = (a[..., :3] * al + FOND * (1 - al)).round().astype(np.uint8)
    m = trous(comp)
    a[m, 3] = 0
    return Image.fromarray(a), int(m.sum())


if __name__ == '__main__':
    from rembg import new_session
    from decoupe_planche import cases as cases16, NAMES
    from decoupe_planche_11_10 import cases as cases20, NOMS
    S = new_session('isnet-anime')
    planche, dest = sys.argv[1], sys.argv[2]
    racine = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    sheet = Image.open(os.path.join(racine, 'images/1.webp')).convert('RGB')
    pl = Image.open(planche).convert('RGB')
    cases = list(zip(NAMES, cases16(sheet), [sheet] * 16)) + list(zip(NOMS, cases20(pl), [pl] * 20))
    for n, box, src in cases:
        chemin = os.path.join(dest, n + '.webp')
        if n.startswith('scene_') or not os.path.exists(chemin):
            continue
        im, k = detoure(src.crop(box), S)
        im.save(chemin, 'WEBP', quality=90, method=6)
        print(n, im.size, 'trous', k)
    for n in ('char_dancing_joy', 'char_guitar', 'char_open_arms'):
        chemin = os.path.join(dest, n + '.webp')
        im, k = sans_planche(chemin)
        im.save(chemin, 'WEBP', quality=90, method=6)
        print(n, im.size, 'trous', k)
