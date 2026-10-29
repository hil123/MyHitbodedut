"""L'image d'ouverture : python3 tools/ouverture_tziyoun.py tools/sources-images/tombe_rabbi_nahman_uman.jpg android/app/src/main/res/drawable-nodpi/scene_ouverture_tombe.webp"""
import sys
from PIL import Image, ImageDraw, ImageFilter
src = Image.open(sys.argv[1]).convert('RGB')
k = src.width / 816
# La matséva, l'aron et le drap du tsiyoun (coordonnées dans une vignette de 816 px de large).
x0, y0, x1, y1 = 40, 0, 700, 612
crop = src.crop((int(x0*k), int(y0*k), int(x1*k), int(y1*k)))
W, H = 900, 2000
band = crop.resize((W, round(crop.height * W / crop.width)), Image.LANCZOS)
top = 420
fond = Image.new('RGB', (W, H), (0x1E, 0x3A, 0x30))
mask = Image.new('L', band.size, 255)
d = ImageDraw.Draw(mask); f = 140
for i in range(f):
    a = round(255 * i / f)
    d.line([(0, i), (W, i)], fill=a); d.line([(0, band.height-1-i), (W, band.height-1-i)], fill=a)
fond.paste(band, (0, top), mask)
fond.save(sys.argv[2], 'WEBP', quality=82, method=6)
print(band.size, top, top+band.height)
