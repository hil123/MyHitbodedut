"""Musique TÉMOIN originale (composée par script, aucun droit tiers).
Piano synthétique, mode Ahava Rabba sur ré, 60 BPM : chaque seconde est un temps,
donc chaque coupe de scène (8,16,25,37,44,51,58,66,73,80 s) tombe sur un temps.
0-16 s : nappe grave + souffle urbain qui s'éteint ; 16 s : entrée du piano (scène 3) ;
80-88 s : montée ; 88-90 s : arrêt doux."""
import numpy as np, wave, sys
SR=44100; DUR=90.0; N=int(SR*DUR)
L=np.zeros(N); R=np.zeros(N)
rng=np.random.default_rng(7)
def hz(m): return 440*2**((m-69)/12)
def piano(m,t,dur,vel,pan=0.0):
    f=hz(m); n=int(SR*min(dur+2.5,DUR-t)); 
    if n<=0: return
    x=np.arange(n)/SR; s=np.zeros(n)
    for k,a in enumerate([1,.55,.32,.2,.12,.07,.04],1):
        fk=f*k*np.sqrt(1+0.0004*k*k)
        if fk>16000: break
        s+=a*np.sin(2*np.pi*fk*x+rng.uniform(0,6))*np.exp(-x*(0.9+0.55*k)*(f/260)**0.35)
    att=np.minimum(1,x/0.004); rel=np.where(x>dur,np.exp(-(x-dur)*5),1)
    s*=att*rel*vel
    i=int(t*SR); e=min(N,i+n); s=s[:e-i]
    L[i:e]+=s*(1-pan)*0.5; R[i:e]+=s*(1+pan)*0.5
def pad(m,t,dur,vel):
    n=int(SR*dur); i=int(t*SR); x=np.arange(n)/SR; f=hz(m)
    s=sum(np.sin(2*np.pi*f*d*x) for d in (1,1.003,0.997,2.0))*vel/4
    env=np.minimum(1,x/2.5)*np.minimum(1,(dur-x)/2.5); s*=env
    e=min(N,i+n); L[i:e]+=s[:e-i]*.5; R[i:e]+=s[:e-i]*.5
# mode Ahava Rabba sur ré : D Eb F# G A Bb C
D=50
# 0-16 s : nappe et souffle urbain décroissant
pad(D-12,0,17,0.10); pad(D-5,0,17,0.05)
noise=np.cumsum(rng.standard_normal(int(SR*16)))*0.0006; noise-=np.convolve(noise,np.ones(4000)/4000,'same')
noise*=np.linspace(0.5,0,len(noise))**1.5
L[:len(noise)]+=noise; R[:len(noise)]+=np.roll(noise,300)
for t in (0,4,8,12): piano(D-12,t,3.5,0.18,-.2)
piano(D+12+2,8,1.5,0.16,.3)  # tintement doux sur la question (scène 2)
# progression (accords en demi-tons depuis D) : i  iv  VII(C)  i(Eb majeur = II bémol)
prog=[[0,6,9],[7,10,14],[ -2,2,5],[1,5,8],[0,6,9],[5,9,12],[ -2,2,5],[0,6,9]]
mel=[ [12,13,16,13,12], [17,16,13,12], [10,12,13,12], [13,12,10,9], [12,16,17,19,17], [21,19,17,16], [14,13,12,10], [12,9,6,7,9] ]
t=16.0; bar=4.0; k=0
while t<88:
    ch=prog[k%len(prog)]; m=mel[k%len(mel)]
    energy=0.55 if t<37 else (0.7 if t<80 else 0.95)
    piano(D-12+ch[0],t,bar,0.30*energy,-.3)
    for j,c in enumerate(ch): piano(D+c,t+j*0.5+1,bar-1,0.14*energy,-.1)   # arpège main gauche
    if t>=25:
        step=bar/len(m)
        for j,n in enumerate(m): piano(D+12+n,t+j*step,step*1.1,0.24*energy,.25)
    if t>=80:
        for c in ch: piano(D+12+c,t,bar,0.10,.0); pad(D+c,t,bar,0.04)
    t+=bar; k+=1
# accents sur les coupes de scène (temps fort)
for c in (25,37,44,51,58,66,73,80): piano(D-24,c,2.5,0.20,-.2)
# accord final et arrêt doux
for c in (0,6,9,12): piano(D+c,88,2,0.22)
# réverbération simple (peignes + passe-tout)
def reverb(x):
    y=np.zeros_like(x)
    for d,g in ((1557,.78),(1617,.77),(1491,.79),(1422,.78)):
        b=np.zeros_like(x); 
        for i in range(0,len(x),d):
            pass
        # implémentation vectorisée par blocs
        b=x.copy()
        for rep in range(1,12): 
            s=d*rep
            if s>=len(x): break
            b[s:]+=x[:-s]*(g**rep)
        y+=b
    return y/4
L=0.65*L+0.35*reverb(L); R=0.65*R+0.35*reverb(np.roll(R,120))
fade=np.ones(N); fe=int(SR*1.6); fade[-fe:]=np.linspace(1,0,fe)**2; fade[:int(SR*.05)]=np.linspace(0,1,int(SR*.05))
L*=fade; R*=fade
pk=max(abs(L).max(),abs(R).max()); L/=pk/0.8; R/=pk/0.8
out=(np.stack([L,R],1)*32767).astype(np.int16)
with wave.open(sys.argv[1],'wb') as w: w.setnchannels(2); w.setsampwidth(2); w.setframerate(SR); w.writeframes(out.tobytes())
