"""Musique originale n° 3 (01/11) : profonde et émouvante, ré mineur, 66 BPM.
Violoncelle solo, cordes, piano, chœur ; résolution en ré majeur sur le logo.
Composition par script, aucun droit tiers. Rendu FluidSynth + FluidR3_GM (licence MIT).
Calage montage v8 : 0–10 le monde qui court · 10–14,3 la question · 14,3 ouverture de l'app
· 15–35,2 parler à Hachem · 35,2–73,3 sa première semaine · 73,3–82 retour du plan, sommet · 82–90 logo."""
import mido, sys
TPB=480; BPM=66; SPB=60/BPM
ev={}
def add(tr,t,m): ev.setdefault(tr,[]).append((t,m))
def note(tr,ch,t,n,d,v):
    add(tr,t,mido.Message('note_on',channel=ch,note=n,velocity=max(1,min(127,int(v)))))
    add(tr,t+d,mido.Message('note_off',channel=ch,note=n,velocity=0))
def prog(ch,p): add(ch,0,mido.Message('program_change',channel=ch,program=p))
def cc(ch,c,v,t=0): add(ch,t,mido.Message('control_change',channel=ch,control=c,value=v))
D=62
MIN=[0,2,3,5,7,8,10]
def deg(i,base=D): o,r=divmod(i,7); return base+12*o+MIN[r]
# ré mineur : i, VI, III, VII (ré m, si♭, fa, do) — puis VI, VII, i avec quinte
ACC=[[0,3,7],[-4,0,3],[-9,-5,-2],[-2,2,5]]
#      0 piano, 1 cordes, 2 violoncelle solo, 3 contrebasse, 4 harpe, 5 violon solo, 6 timbales, 7 chœur
for ch,p in ((0,0),(1,48),(2,42),(3,43),(4,46),(5,40),(6,47),(7,52)): prog(ch,p); cc(ch,91,90)
cc(0,7,92); cc(1,7,80); cc(2,7,110); cc(5,7,100); cc(7,7,70)
# 1) 0–10 : le monde qui court — pulsation sombre de cordes en croches, piano en notes répétées
t=0.0;k=0
while t<10.0:
    a=ACC[k%4]
    for j in range(8):
        tt=t+j*SPB/2
        if tt>=10.0: break
        note(1,1,tt,D-12+a[0],SPB/2*0.7,46+(j%2==0)*10)
        note(0,0,tt,D+a[2]+12 if j%4==3 else D+a[1]+12,SPB/2*0.4,40)
    note(3,3,t,D-24+a[0],4*SPB*0.95,70)
    t+=4*SPB;k+=1
# 2) 10–14,3 : la question — tout s'efface, une note tenue, deux notes de piano
note(1,1,10.0,D,4.3,44); note(1,1,10.0,D-12,4.3,40)
for tt,n in ((10.4,D+24),(11.8,D+22),(13.0,D+19)): note(0,0,tt,n,1.8,46)
# 3) 14,33 : ouverture de l'app — harpe qui monte, l'harmonie s'ouvre (si♭ majeur)
for j in range(12): note(4,4,14.33+j*0.06,D-4+[0,4,7][j%3]+12*(j//3),2.4,58+j*2)
# 4) 15–35,2 : parler à Hachem — piano arpégé, cordes douces, violoncelle solo
t=15.0;k=0
while t<35.1:
    a=ACC[k%4]
    for j,i in enumerate([0,1,2,1,0,1,2,1]): note(0,0,t+j*SPB/2,D-12+a[i]+(12 if j in (2,6) else 0),SPB*1.2,44)
    for n in a: note(1,1,t,D+n-12,4*SPB*0.98,40)
    note(3,3,t,D-24+a[0],4*SPB*0.98,58)
    t+=4*SPB;k+=1
CELLO=[(4,1.5),(3,0.5),(2,1),(4,1),(5,3),(4,1),(2,1.5),(1,0.5),(0,1),(2,1),(-1,3),(0,1),
       (4,1.5),(5,0.5),(7,1),(6,1),(5,2),(4,2),(3,1),(2,1),(1,1),(2,1),(0,4)]
t=20.0
for d,du in CELLO:
    if t>35.0: break
    note(2,2,t,deg(d,D-12),du*SPB*0.98,82);t+=du*SPB
# 5) 35,2–73,3 : sa première semaine — l'espoir : VI–III–VII–i, croches de piano, violon solo
ESP=[[-4,0,3],[-9,-5,-2],[-2,2,5],[0,3,7]]
t=35.17;k=0
while t<73.2:
    a=ESP[k%4]; v=40+min(k,8)*2
    for j in range(8):
        tt=t+j*SPB/2
        if tt>=73.2: break
        note(0,0,tt,D+a[j%3]+(12 if j>=4 else 0),SPB/2*0.9,v+(j==0)*8)
    for n in a: note(1,1,t,D+n,4*SPB*0.98,36+min(k,8)*2)
    note(3,3,t,D-24+a[0],4*SPB*0.98,62)
    if k>=4: note(2,2,t,D-12+a[0],4*SPB*0.98,54)
    t+=4*SPB;k+=1
VIOLON=[(5,2),(4,1),(2,1),(4,3),(2,1),(1,1.5),(2,0.5),(4,1),(5,1),(4,4),
        (7,2),(6,1),(5,1),(6,2),(4,2),(5,1.5),(4,0.5),(2,1),(1,1),(2,4)]
for debut in (39.0,57.0):
    t=debut
    for d,du in VIOLON:
        if t>debut+17.0 or t>72.8: break
        note(5,5,t,deg(d,D+12)-12,du*SPB*0.97,70 if debut<50 else 80);t+=du*SPB
# 6) 73,3–82 : retour du plan d'ouverture, sommet — cordes en octaves, chœur, timbales, violon aigu
t=73.33;k=0
while t<82.0:
    a=ESP[k%4];v=70+k*10
    for n in a: note(1,1,t,D+n,4*SPB*0.98,v); note(1,1,t,D+n-12,4*SPB*0.98,v-6); note(7,7,t,D+n,4*SPB*0.98,v-14)
    note(3,3,t,D-24+a[0],4*SPB*0.98,80)
    note(6,6,t,D-26,0.9,70+k*12)
    for j in range(8): note(0,0,t+j*SPB/2,D+a[j%3]+12,SPB/2*0.9,58+k*8)
    t+=4*SPB;k+=1
t=73.6
for d,du in [(7,2),(9,1),(8,1),(7,2),(5,2),(6,1.5),(7,0.5),(8,1),(9,1),(11,4)]:
    if t>81.8: break
    note(5,5,t,deg(d,D),du*SPB*0.97,96);t+=du*SPB
# 7) 82–90 : résolution en ré majeur (tierce picarde), harpe, tout s'apaise
for n in (0,4,7,12): note(1,1,82.0,D+n-12,7.6,72); note(7,7,82.0,D+n,7.2,56); note(0,0,82.0,D+n,6.8,66)
note(2,2,82.0,D-24,7.6,70); note(3,3,82.0,D-24,7.6,74)
for j in range(10): note(4,4,82.15+j*0.08,D+[0,4,7][j%3]+12*(j//3),3.5,64)
note(6,6,82.0,D-26,2.0,76)
mid=mido.MidiFile(ticks_per_beat=TPB)
for tr in sorted(ev):
    track=mido.MidiTrack(); mid.tracks.append(track)
    track.append(mido.MetaMessage('set_tempo',tempo=mido.bpm2tempo(BPM),time=0)); last=0
    for ts,m in sorted(ev[tr],key=lambda x:(x[0],0 if x[1].type!='note_on' else 1)):
        tick=int(round(ts/SPB*TPB)); m.time=tick-last; last=tick; track.append(m)
mid.save(sys.argv[1]); print('ok',round(mid.length,1))
