# -*- coding: utf-8 -*-
"""
Reprend le détourage des illustrations avec un modèle de segmentation
(demande du 11/10/2026 : « plusieurs petits bouts blancs ou de l'arrière-plan »).

Le seuil sur le blanc (tools/decoupe_planche.py) laissait des liserés et des
îlots de fond collés au contour, la chemise blanche interdisant un seuil plus
bas. Le modèle isnet-anime de rembg, entraîné sur des illustrations, sépare
le personnage du fond sans regarder la couleur seule.

Deux usages :
  - une case de planche (fond blanc d'origine) : le modèle sur la case ;
  - une illustration déjà détourée : posée sur un fond bleu franc (sur du
    blanc, le modèle confondait la chemise et le fond et effaçait des bras),
    puis le masque du modèle est croisé avec l'alpha existant (minimum) :
    il ne peut que retirer, jamais rendre opaque ce qui ne l'était pas.
Dans les deux cas, seules les composantes d'au moins 2 % de la plus grande
sont gardées (les miettes isolées partent). char_walking garde l'alpha
d'origine sur ses 25 premières lignes : le masque rognait le haut de la
kippa (tools/verifie_illustrations.py le signalait).

    python3 -m pip install "rembg[cpu]"   # télécharge isnet-anime.onnx (176 Mo)
    python3 tools/detourage_modele.py <image.webp|png> [...]   (réécrit en place)
"""
import sys
import numpy as np
from PIL import Image
from scipy import ndimage


def detoure(im, session):
    from rembg import remove
    src = im.convert("RGBA")
    a = np.array(src)
    deja = (a[..., 3] < 250).mean() > 0.01
    if deja:
        fond = Image.new("RGBA", src.size, (46, 110, 150, 255))
        fond.alpha_composite(src)
        masque = np.array(remove(fond.convert("RGB"), session=session, post_process_mask=True).convert("RGBA"))[..., 3]
        alpha = np.minimum(masque, a[..., 3]).astype(np.int32)
    else:
        sortie = np.array(remove(src.convert("RGB"), session=session, post_process_mask=True).convert("RGBA"))
        a, alpha = sortie, sortie[..., 3].astype(np.int32)
    lab, n = ndimage.label(alpha > 40)
    if n:
        tailles = np.bincount(lab.ravel())
        tailles[0] = 0
        alpha = np.where((tailles >= 0.02 * tailles.max())[lab], alpha, 0)
    a = a.copy()
    a[..., 3] = alpha.astype(np.uint8)
    out = Image.fromarray(a)
    return out if deja else out.crop(out.getbbox())


if __name__ == "__main__":
    from rembg import new_session
    session = new_session("isnet-anime")
    for chemin in sys.argv[1:]:
        detoure(Image.open(chemin), session).save(chemin, "WEBP", quality=90, method=6)
        print(chemin)
