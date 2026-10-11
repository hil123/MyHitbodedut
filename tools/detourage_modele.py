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

Reprise du même jour, après relecture du propriétaire (taches blanches entre
les péot et le cou) : les 16 personnages de images/1.webp sont refaits depuis
la planche d'origine (fond blanc vrai, comme la planche du 11/10), plus depuis
leur version déjà détourée, où ce fond était devenu opaque ; pour les quatre
autres (sans planche d'origine) et six des seize, blanc_pur() retire les
plages d'un blanc pur et plat (fond d'origine) collées aux cheveux dans la
zone de la tête, et les miettes blanches collées au contour. Un passage de
tools/nettoie_detourage.py ôte enfin le liseré blanc du bord (sauf
arms_raised, dancing_joy, open_arms : il y détachait les franges du tsitsit).

    python3 -m pip install "rembg[cpu]"   # télécharge isnet-anime.onnx (176 Mo)
    python3 tools/detourage_modele.py <image.webp|png> [...]   (réécrit en place)
"""
import sys
import numpy as np
from PIL import Image
from scipy import ndimage


def blanc_pur(A):
    """Le fond blanc resté opaque : plages blanches pures et plates près des cheveux, miettes au bord."""
    rgb=A[...,:3].astype(np.int32); a=A[...,3]
    lum=rgb.mean(-1); mn=rgb.min(-1); sat=rgb.max(-1)-mn
    std=ndimage.generic_filter(lum,np.std,size=5)
    blanc=(a>20)&(mn>=246)&(sat<12)&(std<2.0)
    sombre=(a>200)&(lum<90)
    ys=np.where(a.max(1)>20)[0]; h=ys[-1]-ys[0]
    zone=np.zeros_like(blanc); zone[int(ys[0]+0.08*h):int(ys[0]+0.35*h)]=True
    lab,k=ndimage.label(blanc&zone)
    ids=np.unique(lab[blanc&zone&ndimage.binary_dilation(sombre,iterations=10)]); ids=ids[ids>0]
    retire=np.isin(lab,ids)
    # partout : petits îlots blancs plats collés au transparent et au sombre
    lab2,k2=ndimage.label(blanc)
    if k2:
        sz=np.bincount(lab2.ravel())
        transp=ndimage.binary_dilation(a<20,iterations=2); pres=ndimage.binary_dilation(sombre,iterations=4)
        ok=np.unique(lab2[blanc&transp&pres]); ok=ok[(ok>0)]
        ok=ok[sz[ok]<300]
        retire|=np.isin(lab2,ok)
    return retire
def retire_blanc_pur(A):
    r=blanc_pur(A); A=A.copy()
    al=A[...,3].astype(np.float32); al[r]=0
    # bord adouci autour des zones retirees
    bord=ndimage.binary_dilation(r,iterations=1)&~r
    al[bord]*=0.55
    A[...,3]=al.astype(np.uint8); return A,r.sum()


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
