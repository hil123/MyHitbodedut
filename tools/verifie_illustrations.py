#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Cherche les illustrations dont un bord tranche le personnage.

Le defaut est invisible en lisant le code et presque invisible sur un
grand ecran : il ne se voit qu'en vignette, ou une tete coupee net
devient une forme blanche qu'on ne reconnait plus. C'est arrive a
char_standing_back, dont le haut de la kippa etait rase par le bord de
l'image ; en vignette, cela ressemblait a une toque de cuisinier.

La signature d'une coupe n'est pas le simple contact avec le bord — un
dessin bien cadre peut froler le bord sans dommage. C'est la platitude :
plusieurs lignes d'affilee de la meme largeur, alors qu'un crane ou un
couvre-chef s'elargit ligne apres ligne.

Seul le bord du haut est verifie. Sur les cotes et en bas, un decor qui
sort du cadre — une table, un bureau, un bloc de pierre — produit la
meme platitude sans etre un defaut ; en haut, il n'y a que la tete.

Usage :
    python3 tools/verifie_illustrations.py

Rend 1 si une image est tranchee, 0 sinon. Demande Pillow et numpy.
"""
import glob
import os
import sys

import numpy as np
from PIL import Image

DOSSIER = os.path.join(
    os.path.dirname(os.path.abspath(__file__)),
    "..", "android", "app", "src", "main", "res", "drawable-nodpi",
)

# Nombre de lignes examinees, et largeur minimale au-dela de laquelle une
# ligne plate cesse d'etre une pointe de cheveux pour devenir une coupe.
LIGNES = 6
LARGEUR_MINIMALE = 12
TOLERANCE = 3


def largeurs(alpha, depart, lignes):
    return [int((alpha[y] > 20).sum()) for y in range(depart, depart + lignes)]


def tranchee(chemin):
    """Rend la raison si l'image est tranchee par un bord, sinon None."""
    image = np.array(Image.open(chemin).convert("RGBA"))
    alpha = image[:, :, 3]
    lignes_pleines = np.where(alpha.max(axis=1) > 20)[0]
    colonnes = np.where(alpha.max(axis=0) > 20)[0]
    if len(lignes_pleines) == 0:
        return "image entierement transparente"

    # Seul le bord du haut est examine. Un decor qui sort du cadre sur le
    # cote ou en bas — une table, un bureau, un bloc de pierre — est voulu et
    # donne exactement la meme platitude. En haut, il n'y a que la tete.
    if lignes_pleines[0] > 0:
        return None
    mesures = largeurs(alpha, lignes_pleines[0], LIGNES)
    plat = max(mesures) - min(mesures) <= TOLERANCE and mesures[0] >= LARGEUR_MINIMALE
    if plat:
        return f"sommet plat : {mesures} — la tete est rasee par le bord"
    return None


def main():
    fichiers = sorted(glob.glob(os.path.join(DOSSIER, "char_*.webp")))
    if not fichiers:
        print(f"aucune illustration trouvee dans {DOSSIER}")
        return 1
    coupees = []
    for chemin in fichiers:
        raison = tranchee(chemin)
        nom = os.path.basename(chemin)
        if raison:
            coupees.append(nom)
            print(f"  TRANCHEE  {nom:28} {raison}")
        else:
            print(f"            {nom}")
    print()
    if coupees:
        print(f"{len(coupees)} illustration(s) tranchee(s) : {', '.join(coupees)}")
        return 1
    print(f"{len(fichiers)} illustrations, aucune tranchee par un bord.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
