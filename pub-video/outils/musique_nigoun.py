"""Musique originale de la pub (composée par script, aucun droit tiers).
Rendu avec FluidSynth et la banque FluidR3_GM (licence MIT).
Mode Ahava Rabba sur ré (ré, mi bémol, fa dièse, sol, la, si bémol, do), calée sur le montage v5 :
0–14,3 s le monde qui court · 14,3 s ouverture de l'app · 20–37,7 s nigoun calme · 37,7–73,3 s fonctions (rythme léger)
· 73,3–82 s montée · 82–90 s résolution sur le logo."""
import mido, sys
TPB=480; BPM=72; SPB=60/BPM
mid=mido.MidiFile(ticks_per_beat=TPB)
ev={}  # piste -> liste (temps_s, msg)
def add(tr,t,msg): ev.setdefault(tr,[]).append((t,msg))
def note(tr,ch,t,n,d,v):
    add(tr,t,mido.Message('note_on',channel=ch,note=n,velocity=max(1,min(127,int(v)))))
    add(tr,t+d,mido.Message('note_off',channel=ch,note=n,velocity=0))
def prog(tr,ch,p,t=0): add(tr,t,mido.Message('program_change',channel=ch,program=p))
def cc(tr,ch,c,v,t): add(tr,t,mido.Message('control_change',channel=ch,control=c,value=int(v)))
D=62  # ré4
# gamme Ahava Rabba sur ré, degrés -> demi-tons
G=[0,1,4,5,7,8,10,12,13,16,17,19]
def deg(i,base=D): return base+G[i] if i>=0 else base-12+G[i+7]
beat=lambda b: b*SPB
# canaux : 0 piano, 1 cordes, 2 violoncelle, 3 pizzicato, 4 célesta, 9 percussions
for ch,p in ((0,0),(1,48),(2,42),(3,45),(4,8)): prog(ch,ch,p)
for ch in (0,1,2,3,4): cc(ch,ch,91,70,0)  # réverbération
cc(1,1,7,90,0); cc(2,2,7,95,0); cc(3,3,7,85,0); cc(4,4,7,80,0); cc(0,0,7,110,0)
# accords (racine en demi-tons depuis ré) : i, iv (sol mineur), II bémol (mi bémol majeur), i
ACC=[[0,4,7],[5,8,12],[1,5,8],[0,4,7],[10,14,17],[5,8,12],[1,5,8],[0,4,7]]
# 1) 0–14,3 s : le monde qui court — ostinato de piano en croches, violoncelle staccato
t=0.0; k=0
while t<14.2:
    a=ACC[(k//8)%4]
    for j,i in enumerate([0,2,1,2,0,2,1,2]):
        tt=t+j*SPB/2
        if tt>=14.2: break
        note(0,0,tt,D-12+a[i],SPB/2*0.9,58+ (k%8)*2)
    note(2,2,t,D-24+a[0],SPB*0.4,80); note(2,2,t+SPB*2,D-24+a[0],SPB*0.4,72)
    t+=4*SPB; k+=8
note(1,1,0,D-12,14.0,45); note(1,1,0,D-5,14.0,40)  # tapis sombre
# 2) 14,33 s : ouverture de l'app — arpège de célesta montant
for j,n in enumerate([0,4,7,9,11]): note(4,4,14.33+j*0.11,deg(n,D+12),1.6,82)
# 3) 15–37,7 s : nigoun calme au piano, cordes en tapis
MEL=[ # (degré, durée en temps)
 (4,1),(5,0.5),(4,0.5),(2,1),(1,1),(2,2),(0,2),
 (4,1),(5,0.5),(6,0.5),(7,1.5),(6,0.5),(5,1),(4,1),(2,2),
 (7,1),(8,0.5),(7,0.5),(6,1),(5,1),(4,1.5),(2,0.5),(1,1),(2,1),
 (4,1),(2,0.5),(1,0.5),(0,3)]
t=19.6
for d,du in MEL:
    if t>37.3: break
    note(0,0,t,deg(d),du*SPB*0.95,74); t+=du*SPB
t=15.0; k=0
while t<37.6:
    a=ACC[k%4]
    for n in a: note(1,1,t,D-12+n,4*SPB*0.98,52)
    note(0,0,t,D-24+a[0],4*SPB,46)
    t+=4*SPB; k+=1
# 4) 37,7–73,3 s : fonctions — pizzicato, percussions douces, mélodie plus vive
t=37.67; k=0
while t<73.2:
    a=ACC[k%8]
    for j in range(8):
        tt=t+j*SPB/2
        if tt>=73.2: break
        note(3,3,tt,D-12+a[[0,1,2,1,0,1,2,1][j]],SPB/2*0.6,70)
        note(9,9,tt,42 if j%2 else 70,0.1,38 if j%2 else 30)   # charleston / maracas
        if j in (0,4): note(9,9,tt,36,0.15,52)                 # grosse caisse douce
    for n in a: note(1,1,t,D+n-12,4*SPB*0.98,40)
    t+=4*SPB; k+=1
MEL2=[(4,0.5),(5,0.5),(4,0.5),(2,0.5),(1,1),(2,1),(4,0.5),(5,0.5),(6,1),(4,2),
      (7,0.5),(6,0.5),(5,0.5),(4,0.5),(2,1),(1,1),(0,1),(1,0.5),(2,0.5),(0,2)]
t=41.0
while t<72.5:
    for d,du in MEL2:
        if t>72.5: break
        note(0,0,t,deg(d,D+12),du*SPB*0.9,68); t+=du*SPB
    t+=2*SPB
for tt in (37.67,44.0,51.0,59.0,65.33): note(4,4,tt,deg(7,D+12),1.2,60)   # repères sur les titres
# 5) 73,3–82 s : montée — cordes pleines, piano en octaves
t=73.33; k=0
while t<82.0:
    a=ACC[(k+4)%8]
    v=60+k*12
    for n in a: note(1,1,t,D+n,4*SPB*0.98,v); note(1,1,t,D+n-12,4*SPB*0.98,v-8)
    note(2,2,t,D-24+a[0],4*SPB,v)
    t+=4*SPB; k+=1
t=73.6
for d,du in [(7,1),(8,0.5),(7,0.5),(6,1),(5,1),(4,2),(7,1),(9,1),(8,1),(7,1)]:
    if t>81.8: break
    note(0,0,t,deg(d),du*SPB*0.95,92); note(0,0,t,deg(d)-12,du*SPB*0.95,80); t+=du*SPB
note(9,9,73.33,49,2,70)  # cymbale d'entrée de la montée
# 6) 82–90 s : résolution sur le logo
for n in (0,4,7,12): note(1,1,82.0,D+n-12,7.5,70); note(0,0,82.0,D+n,6.5,78)
for j,n in enumerate([0,4,7,12,16]): note(4,4,82.2+j*0.14,D+12+n,2.5,70)
# pistes -> MIDI
for tr in sorted(ev):
    track=mido.MidiTrack(); mid.tracks.append(track)
    track.append(mido.MetaMessage('set_tempo',tempo=mido.bpm2tempo(BPM),time=0))
    last=0
    for ts,msg in sorted(ev[tr],key=lambda x:(x[0],0 if x[1].type!='note_on' else 1)):
        tick=int(round(ts/SPB*TPB)); msg.time=tick-last; last=tick; track.append(msg)
mid.save(sys.argv[1]); print('ok',mid.length)
