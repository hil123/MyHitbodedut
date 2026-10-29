"""Le visage de l'icône au masculin (ic_launcher_face), depuis le portrait fourni le
29/10 (tools/sources-images/icone_hassid.png, fond menthe uni).

Le fond menthe touchant le bord est retiré, avec un bord doux dé-mélangé (la teinte
menthe ne reste pas sur les cheveux) ; cadrage identique à ic_launcher_face_f : le haut
de la kippa à 102 px, le buste jusqu'au bord bas, 432 × 432. Écrit aussi l'icône
Nokia (32 × 32 sur icone_fond_vert).

    python3 tools/icone_hassid.py && node web/tools/icones.mjs
"""
from pathlib import Path

import numpy as np
from PIL import Image
from scipy import ndimage as nd

RACINE = Path(__file__).resolve().parent.parent
SOURCE = RACINE / 'tools/sources-images/icone_hassid.png'
SORTIE = RACINE / 'android/app/src/main/res/drawable-nodpi/ic_launcher_face.webp'
NOKIA = RACINE / 'nokia/app/res/icone.png'
FOND_ICONE = (0xDF, 0xF5, 0xE8)  # icone_fond_vert


def detoure(u):
    coins = np.concatenate([u[:40, :40].reshape(-1, 3), u[:40, -40:].reshape(-1, 3), u[400:800, :30].reshape(-1, 3)])
    fond = np.median(coins, axis=0)
    d = np.sqrt(((u - fond) ** 2).sum(-1))
    # Le fond : ce qui est proche de la menthe et relié au bord (la chemise blanche, à
    # une trentaine de niveaux de la menthe, n'y entre pas).
    lab, _ = nd.label(d < 22)
    bord = set(np.unique(np.concatenate([lab[0], lab[-1], lab[:, 0], lab[:, -1]]))) - {0}
    alpha = np.where(np.isin(lab, list(bord)), np.clip((d - 8) / 14, 0, 1), 1.0)
    a = alpha[..., None]
    rgb = np.clip(np.where(a > 0.02, (u - (1 - a) * fond) / np.maximum(a, 1e-3), u), 0, 255)
    return Image.fromarray(np.dstack([rgb, alpha * 255]).astype(np.uint8), 'RGBA'), alpha


def main():
    u = np.asarray(Image.open(SOURCE).convert('RGB')).astype(float)
    im, alpha = detoure(u)
    haut = int(np.where(alpha.max(1) > 0.5)[0][0])
    im = im.crop((0, haut, im.width, im.height))
    h = 432 - 102
    im = im.resize((round(im.width * h / im.height), h), Image.LANCZOS)
    toile = Image.new('RGBA', (432, 432), (0, 0, 0, 0))
    toile.alpha_composite(im, ((432 - im.width) // 2, 102))
    toile.save(SORTIE, 'WEBP', quality=90, method=6)
    print(f'{SORTIE.name} 432×432 (visage {im.width}×{im.height})')
    nokia = Image.new('RGBA', (432, 432), FOND_ICONE + (255,))
    nokia.alpha_composite(toile)
    nokia.convert('RGB').resize((32, 32), Image.LANCZOS).save(NOKIA)
    print(f'{NOKIA.name} 32×32')


if __name__ == '__main__':
    main()
