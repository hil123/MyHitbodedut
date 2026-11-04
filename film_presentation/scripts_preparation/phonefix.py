import numpy as np, cv2, json
from PIL import Image
SP='/tmp/claude-0/-home-user/26a79782-a539-52be-8360-a90613406e27/scratchpad/'
OUT='/home/user/film/v1/public/plans/'
res={}
for n in ['M02_telephone','F02_telephone','X03_doigt']:
    I=np.asarray(Image.open(n+'.png').convert('RGB')).astype(int); R,G,B=I[...,0],I[...,1],I[...,2]
    g=((G-np.maximum(R,B)>60)&(G>140)).astype(np.uint8)
    nl,lab,st,_=cv2.connectedComponentsWithStats(g); k=1+np.argmax(st[1:,4]); gm=(lab==k).astype(np.uint8)
    cnt=max(cv2.findContours(gm,cv2.RETR_EXTERNAL,cv2.CHAIN_APPROX_NONE)[0],key=cv2.contourArea)
    (cx,cy),(w,h),a=cv2.minAreaRect(cv2.convexHull(cnt))
    if w>h: w,h=h,w; a+=90
    if a>45: a-=90
    if a<-45: a+=90
    # corps du téléphone = écran + bord (≈ 7 % de la largeur), aspect forcé
    W=w*1.12; H=max(h*1.06, W*2.12)
    res[n]={'cx':round(cx,1),'cy':round(cy,1),'w':round(W,1),'h':round(H,1),'a':round(a,2)}
    # doigts : peau dans la zone du téléphone (agrandie)
    body=np.zeros(gm.shape,np.uint8); cv2.fillConvexPoly(body,cv2.boxPoints(((cx,cy),(W*1.08,H*1.04),a)).astype(np.int32),1)
    skin=((R>140)&(R>G+12)&(G>B-5)&(R-B>35)).astype(np.uint8)
    skin=cv2.morphologyEx(skin,cv2.MORPH_OPEN,np.ones((3,3),np.uint8))
    nl,lab,st,_=cv2.connectedComponentsWithStats(skin*body); keep=[i for i in range(1,nl) if st[i,4]>150]
    fm=np.isin(lab,keep).astype(np.float32)
    fm=cv2.GaussianBlur(cv2.erode(fm,np.ones((2,2),np.uint8)),(3,3),0)
    Image.fromarray(np.dstack([I,fm*255]).astype(np.uint8)).save(OUT+n+'_doigts.png')
    # aperçu
    C=I.copy().astype(np.uint8)
    box=cv2.boxPoints(((cx,cy),(W,H),a)).astype(np.int32)
    cv2.fillConvexPoly(C,box,(20,20,24))
    S=np.asarray(Image.open('/home/user/film/captures/m_barriere.png').convert('RGB').resize((390,844)))
    sw=W*0.9; sh=sw*844/390
    M=cv2.getAffineTransform(np.float32([[0,0],[390,0],[0,844]]),np.float32(cv2.boxPoints(((cx,cy),(sw,sh),a))[[1,2,0]]))
    wp=cv2.warpAffine(S,M,(C.shape[1],C.shape[0])); wm=cv2.warpAffine(np.ones((844,390),np.uint8),M,(C.shape[1],C.shape[0]))>0
    C[wm]=wp[wm]
    F=fm[...,None]; C=(C*(1-F)+I*F).astype(np.uint8)
    Image.fromarray(C).save(SP+f'pf_{n}.png'); print(n,res[n])
json.dump(res,open(SP+'phones.json','w'))
