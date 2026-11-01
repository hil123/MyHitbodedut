"""Musique originale n° 2 (01/11) : lumineuse, en ré majeur, clarinette hassidique.
Composition par script, aucun droit tiers. Rendu FluidSynth + FluidR3_GM (licence MIT).
Calage montage v5 : 0–14,3 le monde qui court · 14,3 ouverture de l'app · 20–37,7 parler à Hachem
· 37,7–73,3 fonctions · 73,3–82 montée · 82–90 logo."""
import mido, sys
TPB=480; BPM=84; SPB=60/BPM
ev={}
def add(tr,t,m): ev.setdefault(tr,[]).append((t,m))
def note(tr,ch,t,n,d,v):
    add(tr,t,mido.Message('note_on',channel=ch,note=n,velocity=max(1,min(127,int(v)))))
    add(tr,t+d,mido.Message('note_off',channel=ch,note=n,velocity=0))
def prog(ch,p): add(ch,0,mido.Message('program_change',channel=ch,program=p))
def cc(ch,c,v): add(ch,0,mido.Message('control_change',channel=ch,control=c,value=v))
D=62
MAJ=[0,2,4,5,7,9,11]
def deg(i,base=D): o,r=divmod(i,7); return base+12*o+MAJ[r]
# ré majeur : I, vi, IV, V (ré, si mineur, sol, la)
ACC=[[0,4,7],[-3,0,4],[-7,-3,0],[-5,-1,2]]
#      0 piano, 1 cordes, 2 violoncelle, 3 guitare nylon, 4 harpe, 5 clarinette, 6 timbales, 9 percussions
for ch,p in ((0,0),(1,49),(2,42),(3,24),(4,46),(5,71),(6,47)): prog(ch,p); cc(ch,91,64)
cc(1,7,85); cc(5,7,100); cc(3,7,88); cc(4,7,92)
# 1) 0–14,3 : le monde qui court — piano staccato en doubles croches, guitare étouffée
t=0.0;k=0
while t<14.2:
    a=ACC[k%4]
    for j in range(16):
        tt=t+j*SPB/4
        if tt>=14.2: break
        note(0,0,tt,D+a[j%3]+(12 if j%4==3 else 0),SPB/4*0.5,50+(j%4==0)*14)
    note(2,2,t,D-24+a[0],SPB*0.5,82); note(2,2,t+2*SPB,D-24+a[0],SPB*0.5,74)
    note(9,9,t,42,0.1,40); note(9,9,t+SPB,42,0.1,34); note(9,9,t+2*SPB,42,0.1,40); note(9,9,t+3*SPB,42,0.1,34)
    t+=4*SPB;k+=1
# 2) 14,33 : ouverture de l'app — glissando de harpe montant
for j in range(14): note(4,4,14.33+j*0.045,deg(j,D),1.8,70+j*2)
# 3) 15–37,7 : parler à Hachem — piano et cordes, mélodie de clarinette douce
t=15.0;k=0
while t<37.6:
    a=ACC[k%4]
    for j,i in enumerate([0,1,2,1,0,1,2,1]): note(4,4,t+j*SPB/2,D-12+a[i],SPB,52)
    for n in a: note(1,1,t,D+n-12,4*SPB*0.98,50)
    t+=4*SPB;k+=1
MEL=[(4,1.5),(5,0.5),(4,1),(2,1),(3,2),(1,2),(2,1.5),(3,0.5),(4,1),(5,1),(4,4),
     (7,1.5),(6,0.5),(5,1),(4,1),(5,2),(3,2),(4,1),(3,0.5),(2,0.5),(1,1),(2,1),(0,4)]
t=19.7
for d,du in MEL:
    if t>37.2: break
    note(5,5,t,deg(d),du*SPB*0.95,72);t+=du*SPB
# 4) 37,7–73,3 : fonctions — guitare grattée, pizz, percussions, clarinette vive
t=37.67;k=0
while t<73.2:
    a=ACC[k%4]
    for j in range(8):
        tt=t+j*SPB/2
        if tt>=73.2: break
        if j in (0,3,4,6):
            for q,n in enumerate(sorted(a)): note(3,3,tt+q*0.012,D-12+n,SPB*0.45,62+(j==0)*12)
        note(9,9,tt,42,0.08,36 if j%2 else 28)
        if j in (2,6): note(9,9,tt,39,0.1,46)         # frappe de mains
        if j in (0,4): note(9,9,tt,36,0.15,56)        # grosse caisse
    note(2,2,t,D-24+a[0],2*SPB,70); note(2,2,t+2*SPB,D-24+a[0],2*SPB,64)
    for n in a: note(1,1,t,D+n,4*SPB*0.98,38)
    t+=4*SPB;k+=1
MEL2=[(4,0.5),(5,0.5),(7,1),(6,0.5),(5,0.5),(4,1),(2,0.5),(3,0.5),(4,1),(2,1),(1,1),
      (2,0.5),(3,0.5),(4,1),(5,0.5),(6,0.5),(7,1),(6,0.5),(5,0.5),(4,2),(0,1)]
t=40.5
while t<72.6:
    for d,du in MEL2:
        if t>72.6: break
        note(5,5,t,deg(d,D+12),du*SPB*0.85,82);t+=du*SPB
    t+=2*SPB
for tt in (37.67,44.0,51.0,59.0,65.33): note(4,4,tt,deg(7,D+12),1.4,66)
# 5) 73,3–82 : montée — cordes pleines, timbales, piano en octaves
t=73.33;k=0
while t<82.0:
    a=ACC[k%4];v=62+k*12
    for n in a: note(1,1,t,D+n,4*SPB*0.98,v); note(1,1,t,D+n+12,4*SPB*0.98,v-10)
    note(6,6,t,D-26,0.6,70+k*10); note(6,6,t+2*SPB,D-26,0.6,60+k*10)
    for j in range(4): note(0,0,t+j*SPB,D+a[j%3]+12,SPB*0.9,70+k*8)
    t+=4*SPB;k+=1
t=73.6
for d,du in [(7,1),(9,1),(8,1),(7,1),(6,2),(7,2),(9,1),(11,1),(10,1),(9,1)]:
    if t>81.8: break
    note(5,5,t,deg(d),du*SPB*0.95,96);t+=du*SPB
note(9,9,73.33,49,2,72)
# 6) 82–90 : accord final de ré majeur, harpe
for n in (0,4,7,12): note(1,1,82.0,D+n-12,7.5,70); note(0,0,82.0,D+n,6.5,74)
for j in range(10): note(4,4,82.1+j*0.06,deg(j,D),3,68)
note(6,6,82.0,D-26,1.5,80)
mid=mido.MidiFile(ticks_per_beat=TPB)
for tr in sorted(ev):
    track=mido.MidiTrack(); mid.tracks.append(track)
    track.append(mido.MetaMessage('set_tempo',tempo=mido.bpm2tempo(BPM),time=0)); last=0
    for ts,m in sorted(ev[tr],key=lambda x:(x[0],0 if x[1].type!='note_on' else 1)):
        tick=int(round(ts/SPB*TPB)); m.time=tick-last; last=tick; track.append(m)
mid.save(sys.argv[1]); print('ok',round(mid.length,1))
