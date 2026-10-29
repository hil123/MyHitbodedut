# -*- coding: utf-8 -*-
"""Décide quels morceaux sont dans l'APK, et range les autres dans `musique/`.

`ajoute_musique.py` range les *arrivées*. Celui-ci recompose l'ensemble : on
lui donne la liste des morceaux qu'on veut embarquer, il déplace les fichiers
dans les deux sens, réécrit `MusicLibrary.BUNDLED` et le bloc de titres de
`values/strings.xml`, et refuse de dépasser le plafond.

Deux choses qu'il préserve et qu'un déplacement à la main perdrait :

1. **Le titre d'un morceau écarté.** Le nom d'origine de l'envoi n'existe
   plus ; si on supprimait sa ligne de `strings.xml`, le titre serait perdu
   pour de bon. Les titres des écartés restent donc dans un bloc de réserve,
   d'où ils reviennent tels quels si le morceau revient.
2. **Le plafond.** Il est vérifié sur l'ensemble demandé avant tout
   déplacement : soit l'échange tient, soit rien ne bouge.

Usage :  python3 tools/echange_musique.py --embarque <stem> [<stem> ...]
         python3 tools/echange_musique.py --embarque-fichier liste.txt
"""
import argparse, pathlib, re, shutil, sys

from audio import morceaux, vers_aac

RACINE = pathlib.Path(__file__).resolve().parent.parent
RAW = RACINE / 'android/app/src/main/res/raw'
HORS = RACINE / 'musique'
KT = RACINE / 'android/app/src/main/java/com/myhitbodedut/MusicLibrary.kt'
XML = RACINE / 'android/app/src/main/res/values/strings.xml'
MO = 1024 * 1024

DEBUT = '    <!-- Morceaux livres. Titres non traduits : ce sont des noms propres. -->\n'
RESERVE = ('    <!-- En reserve : ces morceaux sont dans musique/, hors de l\'APK.\n'
           '         Leurs titres restent ici pour ne pas etre perdus au retour. -->\n')
FIN = '    <!-- fin des morceaux -->\n'


def titres_existants():
    t = {}
    for m in re.finditer(r'<string name="track_([a-z0-9_]+)" translatable="false">(.*?)</string>',
                         XML.read_text()):
        t[m.group(1)] = m.group(2)
    return t


def titre_par_defaut(stem):
    m = re.fullmatch(r'niggoun_(\d+)', stem)
    if m:
        return 'Niggoun %s — titre à préciser' % m.group(1)
    return 'Niggoun — titre à préciser'


def main():
    # 29/10 : la liste des morceaux livrés est coupée en deux (MusiquePlay.kt
    # pour la version Play, src/musique_hors_play pour la seule version
    # GitHub), selon les droits de diffusion de chaque morceau. Cet outil
    # recomposait une liste unique : il ferait entrer dans la version Play un
    # morceau sans autorisation. Il s'arrête donc, jusqu'à être réécrit pour
    # les deux listes. PUBLICATION_PLAY_STORE.md § 3.2.
    sys.exit('echange_musique.py est suspendu depuis le 29/10 : la version Play ne livre que '
             'les morceaux autorisés (PUBLICATION_PLAY_STORE.md § 3.2). Déplacer un morceau '
             'à la main entre android/app/src/main/res/raw et src/musique_hors_play/res/raw, '
             'avec sa ligne dans MusiquePlay.kt ou MusiqueHorsPlay.kt ; MusicFilesTest vérifie.')
    ap = argparse.ArgumentParser()
    ap.add_argument('--embarque', nargs='*', default=[])
    ap.add_argument('--embarque-fichier')
    ap.add_argument('--plafond', type=float, default=130.0)
    args = ap.parse_args()

    voulus = list(args.embarque)
    if args.embarque_fichier:
        voulus += pathlib.Path(args.embarque_fichier).read_text().split()
    if not voulus:
        sys.exit('rien à embarquer : donnez au moins un morceau')

    ou = {p.stem: p for p in morceaux(RAW) + morceaux(HORS)}
    inconnus = [s for s in voulus if s not in ou]
    if inconnus:
        sys.exit('introuvables : ' + ', '.join(inconnus))
    if len(set(voulus)) != len(voulus):
        sys.exit('doublon dans la liste demandée')

    poids = sum(ou[s].stat().st_size for s in voulus) / MO
    if poids > args.plafond:
        sys.exit('l\'ensemble demandé pèse %.1f Mo, au-dessus du plafond de %.0f : '
                 'rien n\'a bougé.' % (poids, args.plafond))

    garde, titres = set(voulus), titres_existants()
    entrent = [s for s in voulus if ou[s].parent == HORS]
    sortent = [p.stem for p in morceaux(RAW) if p.stem not in garde]

    # Ce qui entre dans l'APK y est en AAC (tools/audio.py) ; ce qui en sort garde son AAC.
    for s in entrent:
        if ou[s].suffix == '.m4a':
            shutil.move(str(ou[s]), str(RAW / (s + '.m4a')))
        else:
            vers_aac(ou[s], RAW / (s + '.m4a'))
            ou[s].unlink()
    for s in sortent:
        p = next(q for q in morceaux(RAW) if q.stem == s)
        shutil.move(str(p), str(HORS / p.name))

    embarques = sorted(p.stem for p in morceaux(RAW))
    reserve = sorted(p.stem for p in morceaux(HORS))

    kt = KT.read_text()
    bloc = re.compile(r'(?<=private val BUNDLED = listOf\(\n).*?(?=\n    \)\n)', re.S)
    if not bloc.search(kt):
        sys.exit('liste BUNDLED introuvable dans MusicLibrary.kt')
    KT.write_text(bloc.sub(
        ''.join('        Bundled("%s", R.raw.%s, R.string.track_%s),\n' % (s, s, s)
                for s in embarques).rstrip('\n'), kt, 1))

    def echappe(t):
        t = t.replace('&', '&amp;').replace('<', '&lt;')
        return re.sub(r"(?<!\\)'", r"\\'", t)

    def ligne(s, defaut):
        t = titres.get(s) or echappe(defaut)
        return '    <string name="track_%s" translatable="false">%s</string>\n' % (s, t)

    xml = XML.read_text()
    # La reserve ne *preserve* que ce qui existait : un morceau de musique/ qui
    # n'a jamais eu de ligne ici n'en recoit pas une inventee. Il en recevra
    # une le jour ou il sera embarque, pas avant.
    corps = (DEBUT + ''.join(ligne(s, titre_par_defaut(s)) for s in embarques)
             + '\n' + RESERVE
             + ''.join(ligne(s, '') for s in reserve if s in titres) + FIN)
    zone = re.compile(re.escape(DEBUT) + r'.*?' + re.escape(FIN), re.S)
    if not zone.search(xml):
        sys.exit('bloc des titres introuvable dans strings.xml — voir les repères')
    XML.write_text(zone.sub(lambda _: corps, xml, 1))

    for s in entrent:
        print('ENTRE     %s' % s)
    for s in sortent:
        print('SORT      %-30s → musique/' % s)
    print('\nAPK      : %d morceaux, %.1f Mo' %
          (len(embarques), sum(p.stat().st_size for p in morceaux(RAW)) / MO))
    print('musique/ : %d morceaux, %.1f Mo' %
          (len(reserve), sum(p.stat().st_size for p in morceaux(HORS)) / MO))
    print('\nÀ FAIRE : régénérer le R.java de contrôle, puis relancer MusicFilesTest.')


if __name__ == '__main__':
    main()
