"""Musique en deux temps (demande de Hillel, 01/11) :
1) 0–14,0 s : stress et panique — Mendelssohn, Quatuor n° 6 en fa mineur op. 80, I. Allegro vivace assai (Musopen, domaine public).
2) 14,33 s : « déclic » quand l'écran MY HITBODEDUT apparaît (son synthétisé ici : souffle montant + carillon lumineux).
3) 14,33–90 s : joie intense — Mendelssohn, Symphonie n° 4 « Italienne », I. Allegro vivace (Musopen, domaine public),
   début du mouvement puis fondu enchaîné vers ses accords finaux, qui tombent sur le logo.
Usage : python3 musique_deux_temps.py dossier_sources sortie.wav"""
import sys, subprocess, numpy as np
src, out = sys.argv[1], sys.argv[2]
SR = 48000
def lire(f, t0, t1, lufs):
    cmd = ['ffmpeg', '-v', 'error', '-ss', str(t0), '-to', str(t1), '-i', f, '-af', f'loudnorm=I={lufs}:TP=-1.5:LRA=14', '-ar', str(SR), '-ac', '2', '-f', 'f32le', '-']
    return np.frombuffer(subprocess.run(cmd, capture_output=True, check=True).stdout, dtype=np.float32).reshape(-1, 2).copy()
def fondu(x, deb=0.0, fin=0.0):
    n = len(x)
    if deb: k = int(deb * SR); x[:k] *= np.linspace(0, 1, k)[:, None]
    if fin: k = int(fin * SR); x[n - k:] *= np.linspace(1, 0, k)[:, None]
    return x
T_DECLIC = 14.33
total = np.zeros((90 * SR, 2), np.float32)
# 1) panique : coupe nette juste avant le déclic
pan = fondu(lire(f'{src}/mendelssohn_quatuor6_i.mp3', 0, 14.0, -17), 0.2, 0.08)
total[:len(pan)] += pan
# 2) déclic : souffle montant (bruit filtré, 0,4 s) puis carillon (mi 6, si 6, mi 7) qui s'éteint en 2,5 s
t = np.arange(int(0.4 * SR)) / SR
souffle = np.random.default_rng(7).standard_normal(len(t)).astype(np.float32)
souffle = np.convolve(souffle, np.ones(24) / 24, 'same') * (t / 0.4) ** 3 * 0.35
d0 = int((T_DECLIC - 0.4) * SR); total[d0:d0 + len(t)] += souffle[:, None]
t = np.arange(int(2.5 * SR)) / SR
car = sum(a * np.sin(2 * np.pi * f * t) * np.exp(-t * k) for f, a, k in ((1318.5, .5, 2.2), (1975.5, .3, 2.8), (2637.0, .2, 3.5)))
car = (car * np.minimum(1, t / 0.004)).astype(np.float32) * 0.6
d1 = int(T_DECLIC * SR); total[d1:d1 + len(t)] += car[:, None]
# 3) joie : 0–57,3 s du mouvement, puis la fin (accords finaux) jusqu'à 90 s
corps = lire(f'{src}/mendelssohn_italienne_i.mp3', 0, 57.3, -15)
reste = 90 - T_DECLIC - len(corps) / SR + 1.0
queue = lire(f'{src}/mendelssohn_italienne_i.mp3', 461.3 - reste, 461.3, -15)
X = int(1.0 * SR)
joie = np.concatenate([corps[:-X], corps[-X:] * np.linspace(1, 0, X)[:, None] + queue[:X] * np.linspace(0, 1, X)[:, None], queue[X:]])
joie = fondu(joie, 0.05, 1.5)
total[d1:d1 + len(joie)] += joie[:90 * SR - d1]
total /= max(1.0, np.abs(total).max() / 0.95)
subprocess.run(['ffmpeg', '-v', 'error', '-y', '-f', 'f32le', '-ar', str(SR), '-ac', '2', '-i', '-', out], input=total.tobytes(), check=True)
print('ok', round(len(joie) / SR + T_DECLIC, 2))
