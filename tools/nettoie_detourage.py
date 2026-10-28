#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Nettoie le détourage des illustrations (demande du propriétaire, 29/09).

Les personnages ont été détourés sur fond blanc. Sur un fond sombre
(mode sombre, fenêtre de note du journal), trois restes se voyaient :
  1. une tache blanche au sol, sous les pieds : l'ombre portée d'origine,
     gris très clair ; elle est remplacée par une ombre douce et sombre,
     qui se lit sur fond clair comme sur fond sombre ;
  2. des trous restés blancs, entre les jambes ou sous un bras : du fond
     blanc bordé de sombre (le pantalon), à la différence des reflets de
     la chemise, bordés de gris ;
  3. un liseré blanc tout autour : les pixels du bord sont « décontaminés »
     du blanc, puis le bord est rogné d'un pixel (deux sur les grandes
     images) et adouci.

Le sol n'est traité que pour les personnages dont on voit les pieds : sur
un buste coupé à la taille, le « bas de l'image » est la chemise.

Usage : python3 tools/nettoie_detourage.py   (réécrit les fichiers en place)
"""
import numpy as np, sys
from PIL import Image, ImageFilter, ImageDraw

def nettoie(im, sol_present=True):
    im=im.convert('RGBA'); A=np.array(im).astype(np.float32)
    h,w=A.shape[:2]; rgb=A[...,:3]; a=A[...,3]/255.0
    lum=rgb.mean(-1); sat=rgb.max(-1)-rgb.min(-1)
    # 1. l'ombre blanche au sol : pixels clairs et gris dans le bas, sous le haut des chaussures
    fonce=(a>0.8)&(lum<90)
    rows=np.where(fonce.any(1))[0]
    bas=rows.max() if len(rows) else h-1
    haut_sol=int(bas-0.10*h)
    sol=np.zeros_like(a,bool); sol[haut_sol:,:]=True
    clair=sol&(a>0)&(lum>110)&(sat<45)
    if not sol_present:
        clair[:]=False
    retire=clair.sum()
    ombre=None
    if retire>0:
        ys,xs=np.where(clair)
        x0,x1=np.percentile(xs,[3,97]); yc=np.percentile(ys,50)
        ombre=(x0,x1,yc)
        a=np.where(clair,0,a)
    # 1b. les trous : le fond blanc resté entre les jambes ou sous un bras,
    # reconnaissable à ce qu'il est bordé de sombre (le pantalon) et non
    # d'ombres grises comme les reflets de la chemise.
    from scipy import ndimage
    blanc=(a>0.5)&(lum>246)&(sat<10)
    lab,k=ndimage.label(blanc)
    for i,sl in enumerate(ndimage.find_objects(lab)):
        comp=lab==i+1
        if comp.sum()<25: continue
        anneau=ndimage.binary_dilation(comp,iterations=3)&~comp
        sombre=((lum<110)|(a<0.3))[anneau].mean()
        if sombre>0.55:
            # on étend aux pixels clairs voisins (le fondu du bord)
            zone=ndimage.binary_dilation(comp,iterations=2)&(lum>185)&(sat<25)
            a=np.where(comp|zone,0,a)
    # 2. décontamination du blanc sur les bords semi-transparents
    semi=(a>0)&(a<1)
    aa=np.clip(a,1e-3,1)[...,None]
    F=np.clip((rgb-(1-aa)*255)/aa,0,255)
    rgb=np.where(semi[...,None],F,rgb)
    # 3. érosion d'un pixel (deux sur les grandes images) du liseré, puis adoucie
    r=1 if h<600 else 2
    al=Image.fromarray((a*255).astype(np.uint8))
    er=al.filter(ImageFilter.MinFilter(2*r+1)).filter(ImageFilter.GaussianBlur(0.6*r))
    a=np.minimum(a,np.array(er).astype(np.float32)/255)
    a=np.where(a<0.06,0,a)
    out=np.dstack([rgb,a*255]).clip(0,255).astype(np.uint8)
    fig=Image.fromarray(out,'RGBA')
    # 4. une ombre douce et sombre à la place, lisible sur fond clair comme sombre
    if ombre:
        x0,x1,yc=ombre
        calque=Image.new('L',(w,h),0); d=ImageDraw.Draw(calque)
        rx=(x1-x0)/2*0.85; cx=(x0+x1)/2; ry=max(3,0.018*h)
        d.ellipse([cx-rx,yc-ry,cx+rx,yc+ry],fill=70)
        calque=calque.filter(ImageFilter.GaussianBlur(ry*0.9))
        fond=Image.new('RGBA',(w,h),(0,0,0,0)); fond.putalpha(calque)
        fond=Image.alpha_composite(fond,fig); fig=fond
    return fig,retire


# Les personnages dont on voit les pieds posés au sol.
AVEC_SOL = {
    "char_dancing", "char_dancing_joy", "char_guitar",
    "char_open_arms", "char_praise", "char_standing_back", "char_thumbs_up",
    "char_walking", "char_welcome", "char_reading_seated",
}

if __name__ == '__main__':
    import glob, os
    dossier = os.path.join(os.path.dirname(__file__), '..', 'android', 'app', 'src', 'main', 'res', 'drawable-nodpi')
    for chemin in sorted(glob.glob(os.path.join(dossier, 'char_*.webp'))):
        nom = os.path.basename(chemin)[:-5]
        propre, retire = nettoie(Image.open(chemin), sol_present=nom in AVEC_SOL)
        propre.save(chemin, 'WEBP', quality=90, method=6)
        print(nom, 'sol retiré :', int(retire))
