# -*- coding: utf-8 -*-
"""Écrit un mp3 silencieux, sans encodeur.

Sert à la construction d'essai : les 123 Mo de musique y sont remplacés par
du silence pour que l'APK tienne sous la limite d'un envoi. On ne dépend pas
de ffmpeg — la première tentative a échoué sur `ffmpeg: command not found`,
parce que le runner ne l'a pas.

Une trame MPEG-1 Layer III dont les données sont nulles se décode en
silence. On en met bout à bout autant qu'il faut.

    en-tête, 4 octets :
      FF FB   synchronisation, MPEG-1, Layer III, sans CRC
      10      débit 32 kb/s, échantillonnage 44,1 kHz, sans bourrage
      C4      mono, original

    longueur d'une trame = 144 x 32000 / 44100 = 104 octets
    une trame porte 1152 échantillons, soit 44100/1152 = 38,28 trames/seconde

Usage :  python3 tools/silence_mp3.py <fichier> [secondes]
"""
import pathlib, sys

ENTETE = bytes((0xFF, 0xFB, 0x10, 0xC4))
LONGUEUR_TRAME = 104
TRAMES_PAR_SECONDE = 44100 / 1152


def silence(secondes=1.0):
    trame = ENTETE + bytes(LONGUEUR_TRAME - len(ENTETE))
    return trame * max(1, round(TRAMES_PAR_SECONDE * secondes))


def main():
    if len(sys.argv) < 2:
        sys.exit(__doc__)
    secondes = float(sys.argv[2]) if len(sys.argv) > 2 else 1.0
    pathlib.Path(sys.argv[1]).write_bytes(silence(secondes))


if __name__ == '__main__':
    main()
