import numpy as np, cv2, json
from PIL import Image
SP='/tmp/claude-0/-home-user/26a79782-a539-52be-8360-a90613406e27/scratchpad/'
out={}
def tls(P):  # droite des moindres carrés totaux : point + direction
    c=P.mean(0); u,s,vt=np.linalg.svd(P-c); return c,vt[0]
def inter(l1,l2):
    (p,d),(q,e)=l1,l2; A=np.array([d,-e]).T; t=np.linalg.solve(A,q-p); return p+t[0]*d
for n in ['M02_telephone','F02_telephone','X03_doigt']:
    I=np.asarray(Image.open(n+'.png').convert('RGB')).astype(int); R,G,B=I[...,0],I[...,1],I[...,2]
    g=((G-np.maximum(R,B)>60)&(G>140)).astype(np.uint8)
    nl,lab,st,_=cv2.connectedComponentsWithStats(g); k=1+np.argmax(st[1:,4]); gm=(lab==k).astype(np.uint8)
    cnt=cv2.findContours(gm,cv2.RETR_EXTERNAL,cv2.CHAIN_APPROX_NONE)[0]; cnt=max(cnt,key=cv2.contourArea)
    hull=cv2.convexHull(cnt)[:,0,:].astype(float)
    # densifier le contour du hull
    pts=[]
    for i in range(len(hull)):
        a,b=hull[i],hull[(i+1)%len(hull)]; m=max(2,int(np.linalg.norm(b-a)))
        pts+= [a+(b-a)*t for t in np.linspace(0,1,m,endpoint=False)]
    pts=np.array(pts)
    box=cv2.boxPoints(cv2.minAreaRect(hull.astype(np.float32)))
    # ordonner box : tl,tr,br,bl
    c=box.mean(0); ang=np.arctan2(box[:,1]-c[1],box[:,0]-c[0]); box=box[np.argsort(ang)]; i=np.argmin(box.sum(1)); box=np.roll(box,-i,0)
    lines=[]
    for s in range(4):
        a,b=box[s],box[(s+1)%4]; d=(b-a)/np.linalg.norm(b-a); L=np.linalg.norm(b-a)
        t=(pts-a)@d; dist=np.abs((pts-a)@np.array([-d[1],d[0]]))
        m=(t>0.2*L)&(t<0.8*L)&(dist<0.06*L+6)
        lines.append(tls(pts[m]))
    q=[inter(lines[3],lines[0]),inter(lines[0],lines[1]),inter(lines[1],lines[2]),inter(lines[2],lines[3])]
    q=[[round(float(v),1) for v in p] for p in q]; out[n]=q; print(n,q)
    S=np.asarray(Image.open('/home/user/film/captures/m_barriere.png').convert('RGB').resize((390,844)))
    H=cv2.getPerspectiveTransform(np.float32([[0,0],[390,0],[390,844],[0,844]]),np.float32(q))
    wp=cv2.warpPerspective(S,H,(I.shape[1],I.shape[0]))
    hole=((G-np.maximum(R,B)>25)&(G>90))&(cv2.dilate(gm,np.ones((25,25),np.uint8))>0)
    C=I.copy(); C[hole]=wp[hole]
    Image.fromarray(C.astype(np.uint8)).save(SP+f'fit_{n}.png')
json.dump(out,open(SP+'quads.json','w'))
