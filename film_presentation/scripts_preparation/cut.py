import numpy as np, cv2, os
from PIL import Image
OUT='/home/user/personnages_fond_noir'; os.makedirs(OUT,exist_ok=True)
poses16=['accueil_bras_ouverts','pouce_leve','lecture_tehilim','extase_yeux_fermes','ecrit_au_bureau','reflechit_au_bureau',
 'idee_doigt_leve','marche_sac_a_dos','priere_mains_jointes','victoire_poings_leves','serre_tehilim','tend_la_main',
 'lace_chaussure_assis','lecture_en_tailleur','de_dos','portrait']
sheets=[('A','ae7224f0-image.png','femme_foulard_noir',0,['guitare','louange_bras_leves','danse_bras_ouverts','lecture_a_genoux','livre_contre_soi_profil','de_dos_bras_leves'],3),
        ('B','8eba2b4a-image.png','fille_foulard_blanc',0,poses16,4),
        ('C','02a8f534-image.png','homme_papillotes',255,poses16,4)]
for n,f,prefix,bg,names,ncol in sheets:
    I=np.asarray(Image.open(f).convert('RGB')).astype(np.float32)
    a=np.asarray(Image.open(f'mask_{n}.png').convert('L')).astype(np.float32)/255
    H,W=a.shape
    # props (table, books, stone) dropped by the model: add them back in their cells
    if n in 'BC':
        d=np.abs(I-bg).max(2)
        prop=(d>60).astype(np.uint8)
        prop=cv2.morphologyEx(prop,cv2.MORPH_OPEN,np.ones((3,3),np.uint8))
        cw,ch=W//4,H//4
        keep=np.zeros_like(prop)
        for r,c in [(1,0),(1,1),(3,0)]:
            keep[r*ch:(r+1)*ch, c*cw:(c+1)*cw]=1
        prop=prop*keep
        # only lower half of those cells (table/stone), and cut thin shadow
        pa=cv2.GaussianBlur(prop.astype(np.float32),(3,3),0)
        a=np.maximum(a,pa)
    core=cv2.erode((a>0.8).astype(np.uint8),np.ones((7,7),np.uint8))
    if n=='A': core[680:692,450:850]=0
    nl,lab,st,cen=cv2.connectedComponentsWithStats(core,connectivity=8)
    hard=(a>0.03)
    # give every mask pixel the label of nearest core component
    _,vl=cv2.distanceTransformWithLabels((core==0).astype(np.uint8),cv2.DIST_L2,5,labelType=cv2.DIST_LABEL_PIXEL)
    ys,xs=np.where(core>0); lut=np.zeros(vl.max()+1,np.int32); lut[vl[ys,xs]]=lab[ys,xs]
    lab=np.where(hard,lut[vl],0)
    nrow=len(names)//ncol; cw,ch=W/ncol,H/nrow
    cell=np.full(nl,-1)
    for i in range(1,nl):
        if st[i,4]<400: continue
        cx,cy=cen[i]; cell[i]=int(cy//ch)*ncol+int(cx//cw)
    cellmap=cell[lab]
    a=a*(cellmap>=0)
    a=np.clip((a-0.08)/0.84,0,1)
    if bg==255: a=cv2.erode(a,np.ones((3,3),np.uint8))
    if bg==255:
        A=a[...,None]; F=np.where(A>0.02,(I-(1-A)*255)/np.maximum(A,1e-3),0); F=np.clip(F,0,255)
    else: F=I
    out=(F*a[...,None]).clip(0,255).astype(np.uint8)
    Image.fromarray(out).save(f'{OUT}/{prefix}_00_planche.png')
    # soft alpha pixels near a component belong to its cell: dilate cellmap
    for j,nm in enumerate(names):
        m=(cellmap==j).astype(np.uint8)
        
        sel=out*m[...,None]
        ys,xs=np.where(sel.max(2)>3)
        if len(ys)==0: print("EMPTY",n,j); continue
        y0,y1,x0,x1=ys.min(),ys.max()+1,xs.min(),xs.max()+1
        crop=sel[y0:y1,x0:x1]; h,w=crop.shape[:2]
        mg=int(0.06*max(w,h)); can=np.zeros((h+2*mg,w+2*mg,3),np.uint8); can[mg:mg+h,mg:mg+w]=crop
        Image.fromarray(can).save(f'{OUT}/{prefix}_{j+1:02d}_{nm}.png')
    print(n,'ok')
