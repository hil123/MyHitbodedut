#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Cherche deux defauts dans les illustrations : un bord qui tranche le
personnage, et un morceau d'un personnage voisin reste dans le cadre.

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

Le second defaut vient du decoupage. Les images ont ete taillees dans une
planche qui en contenait plusieurs, et le decoupage a parfois garde un
bout du voisin : char_walking portait les CHAUSSURES du personnage du
dessus, flottant au-dessus de sa tete, et char_torah le haut du crane de
celui du dessous. La signature est un morceau visible qui ne touche pas
le personnage.

« Visible » compte : deux autres morceaux detaches sont restes en place,
dans char_reading_seated et char_standing_back, parce qu'ils sont a une
luminance de 253 et 248 sur 255 — du blanc sur du blanc. Les signaler
aurait appris a ignorer l'outil.

Ce que cette verification NE rattrape PAS, et il faut le dire : les
chaussures de char_walking etaient reliees au personnage par l'ombre
douce qui tombait sous elles. Pour le programme, c'etait une seule et
meme forme. Ce defaut-la n'a ete trouve par aucun outil : quelqu'un a
regarde l'ecran et a demande ce qui flottait au-dessus de la tete.

Usage :
    python3 tools/verifie_illustrations.py

Rend 1 si une image est fautive, 0 sinon. Demande Pillow, numpy et scipy.
"""
import glob
import os
import sys

import numpy as np
from PIL import Image
from scipy import ndimage

DOSSIER = os.path.join(
    os.path.dirname(os.path.abspath(__file__)),
    "..", "android", "app", "src", "main", "res", "drawable-nodpi",
)

# Nombre de lignes examinees, et largeur minimale au-dela de laquelle une
# ligne plate cesse d'etre une pointe de cheveux pour devenir une coupe.
LIGNES = 6
LARGEUR_MINIMALE = 12
TOLERANCE = 3

# Un morceau detache est signale s'il est assez gros pour se voir et assez
# sombre pour se distinguer du fond. Les deux seuils sont necessaires :
# l'aire seule signalerait les ombres pales, la couleur seule les poussieres.
#
# Ce qui est mesure est le POINT LE PLUS SOMBRE du morceau, pas sa moyenne.
# Le crane qui trainait au bas de char_torah avait une moyenne de 236 sur
# 255 — plus pale que le seuil, donc invisible pour un critere en moyenne —
# alors que son coeur etait a 198 et se voyait parfaitement. Les deux
# morceaux laisses en place, eux, ne descendent pas sous 239.
AIRE_MINIMALE = 150
LUMINANCE_VISIBLE = 225

PART_TRANSPARENTE_MINIMALE = 0.10


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


def fond_opaque(chemin):
    """Rend la raison si le fond n'a pas ete detoure.

    Le 23/09, quatre images isolees sont parties avec un fond noir ou blanc
    plein : leur source avait deja une couche alpha, que la conversion en RVB
    a aplatie. Aucune des deux autres verifications ne regarde le fond.
    Un examen des coins en laissait passer une, dont le bras leve touchait
    un coin : c'est la part de transparence qui est mesuree. Les images
    detourees en ont au moins 30 % (le buste de char_face), les fautives
    0,1 %.
    """
    alpha = np.array(Image.open(chemin).convert("RGBA"))[:, :, 3]
    part = (alpha < 20).mean()
    if part < PART_TRANSPARENTE_MINIMALE:
        return f"{part:.1%} de pixels transparents : fond non detoure"
    return None


def detache(chemin):
    """Rend la raison si un morceau visible ne touche pas le personnage."""
    image = np.array(Image.open(chemin).convert("RGBA")).astype(int)
    visible = image[:, :, 3] > 40
    if not visible.any():
        return None
    etiquettes, nombre = ndimage.label(visible, structure=np.ones((3, 3)))
    if nombre <= 1:
        return None
    aires = ndimage.sum(visible, etiquettes, range(1, nombre + 1))
    personnage = int(np.argmax(aires)) + 1

    luminance = image[:, :, :3].mean(axis=2)
    for numero in range(1, nombre + 1):
        if numero == personnage or aires[numero - 1] < AIRE_MINIMALE:
            continue
        morceau = etiquettes == numero
        # Le deuxieme centile plutot que le minimum : un seul pixel sombre
        # est du bruit de compression, pas une forme.
        sombre = np.percentile(luminance[morceau], 2)
        if sombre >= LUMINANCE_VISIBLE:
            continue  # du blanc sur du blanc : invisible, on le laisse
        lignes = np.where(morceau.any(axis=1))[0]
        return ("morceau detache de %d px, lignes %d-%d, point le plus "
                "sombre %d" % (int(aires[numero - 1]), lignes[0], lignes[-1],
                               int(sombre)))
    return None


def main():
    fichiers = sorted(glob.glob(os.path.join(DOSSIER, "char_*.webp")))
    if not fichiers:
        print(f"aucune illustration trouvee dans {DOSSIER}")
        return 1
    fautives = []
    for chemin in fichiers:
        nom = os.path.basename(chemin)
        raison = fond_opaque(chemin)
        if raison:
            fautives.append(nom)
            print(f"  FOND      {nom:28} {raison}")
            continue
        raison = tranchee(chemin)
        if raison:
            fautives.append(nom)
            print(f"  TRANCHEE  {nom:28} {raison}")
            continue
        raison = detache(chemin)
        if raison:
            fautives.append(nom)
            print(f"  INTRUS    {nom:28} {raison}")
        else:
            print(f"            {nom}")
    print()
    if fautives:
        print(f"{len(fautives)} illustration(s) fautive(s) : {', '.join(fautives)}")
        return 1
    print(f"{len(fichiers)} illustrations : toutes detourees, aucune tranchee "
          f"par un bord, aucun morceau de voisin visible.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
