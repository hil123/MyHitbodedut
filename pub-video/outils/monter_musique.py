"""Monte un mouvement classique (domaine public) sur 90 s : début du mouvement jusqu'à une fin de phrase
(creux d'énergie entre 68 et 80 s), fondu enchaîné vers la fin réelle du mouvement, fondu de sortie.
Usage : python3 monter_musique.py source.mp3 sortie.wav"""
import sys, subprocess, array, math
src, out = sys.argv[1], sys.argv[2]
raw = subprocess.run(['ffmpeg', '-v', 'error', '-i', src, '-ac', '1', '-ar', '8000', '-f', 's16le', '-'], capture_output=True).stdout
a = array.array('h', raw); sr = 8000; n = len(a) // sr
def db(t0, t1):
    seg = a[int(t0 * sr):int(t1 * sr)][::8]
    return 20 * math.log10(math.sqrt(sum(x * x for x in seg) / max(1, len(seg))) / 32768 + 1e-9)
fin = n
while fin > 10 and db(fin - 1, fin) < -50: fin -= 1          # fin réelle (sans le silence final)
coupe = min((db(t, t + 1), t) for t in [x / 2 for x in range(136, 161)])[1] + 0.5   # creux entre 68 et 80 s
X = 1.5; reste = 90 - coupe + X
# début de la queue : creux le plus proche de (fin - reste), pour entrer sur une respiration
cible = fin - reste
queue = min((db(t, t + 1), t) for t in [cible + x / 2 for x in range(-6, 1)])[1]
print(f'{src}: coupe {coupe:.1f}s, queue {queue:.1f}–{fin}s')
f = (f"[0]atrim=0:{coupe},asetpts=N/SR/TB[a];[0]atrim={queue}:{fin + 2},asetpts=N/SR/TB[b];"
     f"[a][b]acrossfade=d={X}:c1=tri:c2=tri,atrim=0:90,afade=t=in:d=0.3,afade=t=out:st=86.5:d=3.5,"
     "loudnorm=I=-15:TP=-1.5:LRA=14")
subprocess.run(['ffmpeg', '-v', 'error', '-y', '-i', src, '-filter_complex', f, '-ar', '48000', '-ac', '2', out], check=True)
