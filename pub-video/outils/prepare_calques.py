"""Prépare les calques des panneaux de la planche de Hillel : personnage détouré (avant-plan) et décor
sans personnage (fond, trou comblé par inpainting puis flou de profondeur). Textes incrustés retirés
par recadrage ou inpainting. Agrandissement x3 (Lanczos + accentuation)."""
import cv2, numpy as np
from PIL import Image, ImageFilter
SRC='/home/user/pub-video/sources_hillel/'; OUT='/home/user/pub-video/remotion/public/planche/'
# nom : (recadrage x0,y0,x1,y1 en coordonnées du panneau, rectangles de texte à effacer dans le fond)
P={'p1_rue':((0,0,396,250),[]),
   'p2_question':((0,0,379,248),[]),
   'p3_colline':((0,0,428,266),[(196,14,410,104)]),
   'p10_livre':((0,0,296,397),[]),
   'p11_final':((420,0,693,397),[])}
for n,(box,txt) in P.items():
    img=np.array(Image.open(SRC+n+'.png').convert('RGB'))
    cut=np.array(Image.open(SRC+n+'_perso.png').convert('RGBA'))
    x0,y0,x1,y1=box
    img=img[y0:y1,x0:x1].copy(); cut=cut[y0:y1,x0:x1].copy()
    a=cut[...,3]
    m=(a>16).astype(np.uint8)*255
    for (tx0,ty0,tx1,ty1) in txt:
        m[max(0,ty0-y0):ty1-y0, max(0,tx0-x0):tx1-x0]=255
    m=cv2.dilate(m,np.ones((15,15),np.uint8))
    bg=cv2.inpaint(cv2.cvtColor(img,cv2.COLOR_RGB2BGR),m,9,cv2.INPAINT_TELEA)
    bg=cv2.cvtColor(bg,cv2.COLOR_BGR2RGB)
    H,W=bg.shape[:2]; k=3
    bgI=Image.fromarray(bg).resize((W*k,H*k),Image.LANCZOS).filter(ImageFilter.GaussianBlur(9))
    bgI.save(OUT+n+'_fond.jpg',quality=92)
    fg=Image.fromarray(cut).resize((W*k,H*k),Image.LANCZOS)
    rgb=fg.convert('RGB').filter(ImageFilter.UnsharpMask(radius=2,percent=90,threshold=2)); rgb.putalpha(fg.getchannel('A'))
    rgb.save(OUT+n+'_perso.png')
    # version complète nette (sans calques) pour les plans fixes
    Image.fromarray(img).resize((W*k,H*k),Image.LANCZOS).filter(ImageFilter.UnsharpMask(2,80,2)).save(OUT+n+'_plein.jpg',quality=92)
    print(n,(W*k,H*k))
# logo officiel (hassid sur fond menthe), fourni par Hillel
Image.open(SRC+'logo_hassid_menthe.png').convert('RGB').resize((600,600),Image.LANCZOS).save(OUT+'logo.png')
