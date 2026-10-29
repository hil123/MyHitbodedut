# -*- coding: utf-8 -*-
"""Ce que les outils de musique partagent depuis le passage à l'AAC (29/10, audit 1.390).

Les morceaux embarqués sont en AAC 96 kb/s (`.m4a`) : 130 Mo de MP3 192 kb/s
devenus 85 Mo, à l'écoute validée par le propriétaire. La réserve de `musique/`
garde ses MP3 d'origine ; un morceau qui entre dans l'APK y est réencodé.
"""
import pathlib, subprocess

EXTENSIONS = ('.mp3', '.m4a')


def morceaux(dossier):
    """Les fichiers audio d'un dossier, MP3 ou AAC."""
    return sorted(p for p in pathlib.Path(dossier).iterdir() if p.suffix in EXTENSIONS)


def est_audio(p):
    """Un MP3 (ID3 ou trame MPEG) ou un MP4 audio (« ftyp » à l'octet 4)."""
    t = pathlib.Path(p).read_bytes()[:8]
    return (t[:3] == b'ID3' or (len(t) > 1 and t[0] == 0xFF and (t[1] & 0xE0) == 0xE0)
            or t[4:8] == b'ftyp')


def vers_aac(source, cible):
    """Réencode en AAC 96 kb/s, sans la pochette, l'index en tête pour la lecture."""
    subprocess.run(['ffmpeg', '-v', 'error', '-y', '-i', str(source), '-map', '0:a',
                    '-map_metadata', '0', '-c:a', 'aac', '-b:a', '96k',
                    '-movflags', '+faststart', str(cible)], check=True)
