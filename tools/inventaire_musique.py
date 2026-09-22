# -*- coding: utf-8 -*-
"""Réécrit l'inventaire de `musique/README.md` depuis les fichiers.

Le README affirmait déjà « régénéré depuis les fichiers, pas tenu à la main »
alors que rien ne le régénérait — et il avait dérivé : il annonçait « les six
niggoun_NN » sous un tableau qui en listait seize. Une phrase dérivée d'un
comptage se tient à jour toute seule ou ne se tient pas.

Usage :  python3 tools/inventaire_musique.py
"""
import pathlib, re

RACINE = pathlib.Path(__file__).resolve().parent.parent
HORS = RACINE / 'musique'
RAW = RACINE / 'android/app/src/main/res/raw'
MD = HORS / 'README.md'
MO = 1024 * 1024
MUET = re.compile(r'(niggoun|nigoun_sans_titre)_\d+')


def mo(n):
    return ('%.1f' % (n / MO)).replace('.', ',')


def main():
    fichiers = sorted(HORS.glob('*.mp3'), key=lambda p: p.stem)
    nommes = [p for p in fichiers if not MUET.fullmatch(p.stem)]
    muets = [p for p in fichiers if MUET.fullmatch(p.stem)]

    lignes = ['| fichier | poids |', '|---|---|']
    lignes += ['| `%s` | %s Mo |' % (p.stem, mo(p.stat().st_size)) for p in nommes]
    if muets:
        etendue = ('`%s`' % muets[0].stem if len(muets) == 1
                   else '`%s` … `%s`' % (muets[0].stem, muets[-1].stem))
        lignes.append('| %s *(sans titre)* | %s Mo |'
                      % (etendue, mo(sum(p.stat().st_size for p in muets))))

    embarques = list(RAW.glob('*.mp3'))
    lignes += ['', "**%d morceaux, %s Mo.** L'APK est à %s Mo d'audio sur %d morceaux."
               % (len(fichiers), mo(sum(p.stat().st_size for p in fichiers)),
                  mo(sum(p.stat().st_size for p in embarques)), len(embarques)),
               '', '> Cet inventaire est régénéré depuis les fichiers, pas tenu à la main.']

    texte = MD.read_text()
    bloc = re.compile(r'(?<=## Ce qu.il y a ici\n\n).*?(?=\n## )', re.S)
    if not bloc.search(texte):
        raise SystemExit('section « Ce qu’il y a ici » introuvable')
    texte = bloc.sub('\n'.join(lignes) + '\n', texte, 1)

    compte = re.compile(r'Les \S+ `niggoun_NN` n.en ont aucun')
    if not compte.search(texte):
        raise SystemExit('phrase de comptage introuvable')
    mots = {1: 'Le seul', 2: 'Les deux', 3: 'Les trois'}
    texte = compte.sub('%s `niggoun_NN` n’en ont aucun'
                       % mots.get(len(muets), 'Les %d' % len(muets)), texte, 1)

    MD.write_text(texte)
    print('%d morceaux dont %d sans titre' % (len(fichiers), len(muets)))


if __name__ == '__main__':
    main()
