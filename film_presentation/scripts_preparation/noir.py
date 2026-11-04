import numpy as np, cv2, json, sys
from PIL import Image
OUT='/home/user/film/v1/public/plans/'
meta=json.load(open(OUT+'meta.json'))
for n in sys.argv[1:]:
    I=np.asarray(Image.open(n+'.png').convert('RGB')).astype(np.float32)
    mx=I.max(2)
    fg=(mx>22).astype(np.uint8)
    fg=cv2.morphologyEx(fg,cv2.MORPH_OPEN,np.ones((3,3),np.uint8))
    # fond = noir connecté au bord
    bg=(1-fg).astype(np.uint8); h,w=bg.shape
    ff=bg.copy(); m=np.zeros((h+2,w+2),np.uint8)
    for x,y in [(0,0),(w-1,0),(0,h-1),(w-1,h-1),(w//2,0)]:
        if ff[y,x]==1: cv2.floodFill(ff,m,(x,y),2)
    a=(ff!=2).astype(np.float32)
    dk=(mx<14).astype(np.uint8); nl2,lab2,st2,_=cv2.connectedComponentsWithStats(dk)
    for i in range(1,nl2):
        if st2[i,4]>6000: a[lab2==i]=0
    nl,lab,st,_=cv2.connectedComponentsWithStats((a>0).astype(np.uint8)); keep=[i for i in range(1,nl) if st[i,4]>3000]
    a=a*np.isin(lab,keep)
    soft=np.clip((mx-6)/30,0,1); edge=cv2.dilate(a,np.ones((5,5),np.uint8))-cv2.erode(a,np.ones((5,5),np.uint8))
    a=np.where(edge>0,np.minimum(a+0,soft)*1.0,a)
    R,G,B=I[...,0],I[...,1],I[...,2]
    green=(G-np.maximum(R,B)>60)&(G>140)
    q=meta.get(n,{}).get('quad')
    if green.sum()>2000:
        nl,lab,st,_=cv2.connectedComponentsWithStats(green.astype(np.uint8)); k=1+np.argmax(st[1:,4]); gm=(lab==k).astype(np.uint8)
        gm=cv2.morphologyEx(gm,cv2.MORPH_CLOSE,np.ones((7,7),np.uint8)); ys,xs=np.where(gm>0); P=np.stack([xs,ys],1)
        q=[P[np.argmin(xs+ys)].tolist(),P[np.argmax(xs-ys)].tolist(),P[np.argmax(xs+ys)].tolist(),P[np.argmin(xs-ys)].tolist()]
        a=np.where((G-np.maximum(R,B)>25)&(cv2.dilate(gm,np.ones((5,5),np.uint8))>0),0,a)
        sp=np.clip(G-np.maximum(R,B),0,None); I[...,1]=G-sp*0.85
    a=cv2.GaussianBlur(a.astype(np.float32),(3,3),0)
    A=a[...,None]; F=np.clip(np.where(A>0.02,I/np.maximum(A,1e-3),0),0,255)
    Image.fromarray(np.dstack([F,a*255]).astype(np.uint8)).save(OUT+n+'.png')
    meta[n]={'w':I.shape[1],'h':I.shape[0],'quad':q}; print(n,q)
json.dump(meta,open(OUT+'meta.json','w'))
