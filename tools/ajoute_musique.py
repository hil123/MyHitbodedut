# -*- coding: utf-8 -*-
"""Range les mp3 reçus : dans l'APK tant qu'il y a la place, sinon à côté.

Trois choses qu'on a apprises à la dure et qui sont codées ici :

1. **L'empreinte tranche, pas le nom.** Sur quarante fichiers reçus, cinq
   étaient des doublons octet pour octet sous des noms différents — et deux
   fichiers portant presque le même nom étaient deux enregistrements
   distincts. Le nom ne décide de rien.

2. **Le poids embarqué a une limite.** L'audio ne se compresse plus, ni par
   R8 ni par le zip : chaque mégaoctet ajouté est un mégaoctet d'APK. Au-delà
   du plafond, les fichiers vont dans `musique/`, versionnés mais hors de
   l'application. Rien n'est perdu ; l'application sait déjà lire un fichier
   du téléphone (MusicActivity → OpenDocument → addUserTrack).

3. **Les titres hébreux ne survivent pas au transfert.** Ils arrivent en
   rangées d'underscores et les tags ID3 sont vides. On nomme avec ce qui
   reste lisible, et à défaut on numérote — on n'invente pas un titre.

Usage :  python3 tools/ajoute_musique.py <dossier des envois> [--plafond 130]
"""
import argparse, hashlib, pathlib, re, shutil, sys

RACINE = pathlib.Path(__file__).resolve().parent.parent
RAW = RACINE / 'android/app/src/main/res/raw'
HORS = RACINE / 'musique'
KT = RACINE / 'android/app/src/main/java/com/myhitbodedut/MusicLibrary.kt'
XML = RACINE / 'android/app/src/main/res/values/strings.xml'
MO = 1024 * 1024


def empreinte(p):
    return hashlib.sha256(pathlib.Path(p).read_bytes()).hexdigest()


def est_mp3(p):
    t = pathlib.Path(p).read_bytes()[:3]
    return t[:3] == b'ID3' or (len(t) > 1 and t[0] == 0xFF and (t[1] & 0xE0) == 0xE0)


def titre_lisible(nom):
    """Ce qui reste du nom d'origine : la partie latine, si elle a survécu."""
    n = re.sub(r'^[0-9a-f]{8}-', '', nom).rsplit('.', 1)[0].replace('_', ' ')
    n = re.sub(r'\s*-\s*', ' - ', n)
    return re.sub(r'\s+', ' ', n).strip(' -')


def cle(titre, pris, prefixe_muet='nigoun_sans_titre_'):
    s = re.sub(r'[^a-z0-9]+', '_', titre.lower()).strip('_')
    s = re.sub(r'_?(the_)?(ha)?yanukas?_melodies_?', '_', s).strip('_')
    for motif, prefixe in ((r'ishay_ribo', 'ribo'), (r'motty_steinmetz', 'steinmetz')):
        if re.search(motif, s):
            s = prefixe + '_' + re.sub(motif, '', s).strip('_')
            break
    else:
        if re.search(r'yanuka|shlomo_yehuda', s):
            s = 'yanuka_' + re.sub(r'yanuka|the_', '', s).strip('_')
    s = re.sub(r'_+', '_', s).strip('_')[:40].strip('_')
    if re.fullmatch(r'\d*', s):
        s = ''   # « 5 », « 2 », « 6 » : ce sont des restes, pas des titres
    if not s:
        # Pas de titre lisible : on numerote dans le schema du dossier vise,
        # pas dans celui de l'autre. Melanger les deux rend l'inventaire
        # illisible, ce qui est exactement ce qui est arrive une fois.
        n = 1
        while ('%s%d' % (prefixe_muet, n)) in pris:
            n += 1
        s = '%s%d' % (prefixe_muet, n)
    base, i = s, 2
    while s in pris:
        s, i = '%s_%d' % (base, i), i + 1
    return s


def poids(dossier):
    return sum(p.stat().st_size for p in dossier.glob('*.mp3'))


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('source')
    ap.add_argument('--plafond', type=float, default=130.0,
                    help='Mo embarqués au-delà desquels on range dans musique/')
    args = ap.parse_args()

    HORS.mkdir(exist_ok=True)
    connus = {empreinte(p): p.stem for p in list(RAW.glob('*.mp3')) + list(HORS.glob('*.mp3'))}
    pris = {p.stem for p in RAW.glob('*.mp3')}
    kt, xml = KT.read_text(), XML.read_text()
    embarques, ecartes, doublons, refuses = [], [], [], []

    for f in sorted(pathlib.Path(args.source).glob('*.mp3')):
        h = empreinte(f)
        if h in connus:
            doublons.append((f.name, connus[h]))
            continue
        if not est_mp3(f):
            refuses.append(f.name)
            continue
        titre = titre_lisible(f.name)
        if (poids(RAW) + f.stat().st_size) / MO <= args.plafond:
            c = cle(titre, pris)
            pris.add(c)
            shutil.copy(f, RAW / (c + '.mp3'))
            embarques.append((c, titre))
        else:
            # Un morceau ecarte garde son nom lisible comme les autres : le
            # numeroter jetterait le peu de titre qui a survecu au transfert.
            c = cle(titre, {q.stem for q in HORS.glob('*.mp3')}, 'niggoun_')
            shutil.copy(f, HORS / (c + '.mp3'))
            ecartes.append(c)
        connus[h] = f.name

    if embarques:
        lignes = ''.join('        Bundled("%s", R.raw.%s, R.string.track_%s),\n' % (c, c, c)
                         for c, _ in embarques)
        anc = '        Bundled("hamelech_basade", R.raw.hamelech_basade, R.string.track_hamelech_basade),\n'
        if anc not in kt:
            sys.exit('ancre introuvable dans MusicLibrary.kt')
        KT.write_text(kt.replace(anc, anc + lignes, 1))

        def echappe(t):
            t = t.replace('&', '&amp;').replace('<', '&lt;')
            return re.sub(r"(?<!\\)'", r"\\'", t)   # une apostrophe nue casse aapt

        t = ''.join('    <string name="track_%s" translatable="false">%s</string>\n'
                    % (c, echappe(titre or 'Niggoun — titre à préciser'))
                    for c, titre in embarques)
        anc = '    <string name="track_hamelech_basade" translatable="false">המלך בשדה</string>\n'
        XML.write_text(xml.replace(anc, anc + t, 1))

    for n, c in doublons:
        print('DOUBLON   %-44s = %s' % (n[:44], c))
    for n in refuses:
        print('REFUSÉ    %s (ce n’est pas un mp3)' % n[:60])
    for c, t in embarques:
        print('EMBARQUÉ  %-30s %s' % (c, t))
    for c in ecartes:
        print('ÉCARTÉ    %-30s → musique/ (plafond atteint)' % c)
    print('\nAPK      : %d morceaux, %.1f Mo' % (len(list(RAW.glob('*.mp3'))), poids(RAW) / MO))
    print('musique/ : %d morceaux, %.1f Mo' % (len(list(HORS.glob('*.mp3'))), poids(HORS) / MO))
    if embarques:
        print('\nÀ FAIRE : régénérer le R.java de contrôle, puis relancer MusicFilesTest.')


if __name__ == '__main__':
    main()
