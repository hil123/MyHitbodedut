import numpy as np, cv2, json
from PIL import Image
from rembg import remove, new_session
s=new_session('birefnet-general')
OUT='/home/user/film/v1/public/plans/'
import sys,os
meta=json.load(open(OUT+'meta.json')) if os.path.exists(OUT+'meta.json') else {}
for n in sys.argv[1:]:
    im=Image.open(n+'.png').convert('RGB'); I=np.asarray(im).astype(np.float32)
    a=np.asarray(remove(im,session=s,only_mask=True)).astype(np.float32)/255
    R,G,B=I[...,0],I[...,1],I[...,2]
    green=((G>140)&(R<130)&(B<130)&(G-np.maximum(R,B)>60)).astype(np.uint8)
    q=None
    if green.sum()>2000:
        nl,lab,st,_=cv2.connectedComponentsWithStats(green); k=1+np.argmax(st[1:,4]); gm=(lab==k)
        gm=cv2.morphologyEx(gm.astype(np.uint8),cv2.MORPH_CLOSE,np.ones((7,7),np.uint8))
        ys,xs=np.where(gm>0); P=np.stack([xs,ys],1)
        tl=P[np.argmin(xs+ys)]; br=P[np.argmax(xs+ys)]; tr=P[np.argmax(xs-ys)]; bl=P[np.argmin(xs-ys)]
        q=[tl.tolist(),tr.tolist(),br.tolist(),bl.tolist()]
        hole=cv2.dilate(gm,np.ones((5,5),np.uint8)).astype(bool)
        # garder les doigts posés sur l'écran : un pixel non vert dans l'écran reste opaque
        a=np.where(hole & (green>0),0,a); a=np.where(hole&(green==0)&(G-np.maximum(R,B)>25),0,a)
        a=np.maximum(a, (gm>0)&(green==0)&(a>0)) if False else a
        # despill vert
        sp=np.clip(G-np.maximum(R,B),0,None); I[...,1]=G-sp*0.85
    a=cv2.erode(a,np.ones((3,3),np.uint8)); a=cv2.GaussianBlur(a,(3,3),0)
    A=a[...,None]; F=np.clip(np.where(A>0.02,I/np.maximum(A,1e-3),0),0,255)
    rgba=np.dstack([F,a*255]).astype(np.uint8)
    Image.fromarray(rgba).save(OUT+n+'.png'); meta[n]={'w':im.width,'h':im.height,'quad':q}
    print(n,q)
json.dump(meta,open(OUT+'meta.json','w'))
