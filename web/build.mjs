// Construit le site dans web/dist à partir de web/src et des ressources
// Android. Aucune dépendance : Node seul.
//
// Ce qui vient d'Android est relu, jamais recopié à la main :
//   - les chaînes (values, values-en, values-iw) ;
//   - les textes vérifiés (res/raw/*.json), copiés octet pour octet ;
//   - les illustrations (drawable-nodpi/char_*.webp) ;
//   - la musique livrée, dans l'ordre de MusicLibrary.kt.
//
// Le service worker reçoit la liste de ce qu'il garde hors connexion et un
// numéro de version tiré du contenu : un site modifié s'installe à neuf, un
// site inchangé ne retélécharge rien.

import { createHash } from 'node:crypto';
import { cpSync, existsSync, mkdirSync, readdirSync, readFileSync, rmSync, statSync, writeFileSync } from 'node:fs';
import { dirname, join, relative } from 'node:path';
import { fileURLToPath } from 'node:url';
import { litChaines } from './tools/chaines.mjs';
import { palettesCss } from './tools/palettes.mjs';

const ICI = dirname(fileURLToPath(import.meta.url));
const SRC = join(ICI, 'src');
const DIST = join(ICI, 'dist');
const ANDROID = join(ICI, '..', 'android', 'app', 'src', 'main');
const RES = join(ANDROID, 'res');

rmSync(DIST, { recursive: true, force: true });
mkdirSync(DIST, { recursive: true });

// --- Le code du site ------------------------------------------------------
cpSync(SRC, DIST, { recursive: true });

// --- Les chaînes ------------------------------------------------------------
// Comme Android : la langue par défaut (le français) sous chaque langue,
// que la traduction recouvre. Seules les chaînes non traduisibles (nom de
// l'application, titres de morceaux) viennent ainsi du défaut.
mkdirSync(join(DIST, 'data'), { recursive: true });
const defaut = litChaines(readFileSync(join(RES, 'values', 'strings.xml'), 'utf8'));
for (const [langue, dossier] of [['fr', 'values'], ['en', 'values-en'], ['he', 'values-iw']]) {
  const propre = litChaines(readFileSync(join(RES, dossier, 'strings.xml'), 'utf8'));
  const table = {
    chaines: { ...defaut.chaines, ...propre.chaines },
    pluriels: { ...defaut.pluriels, ...propre.pluriels },
  };
  writeFileSync(join(DIST, 'data', `strings.${langue}.json`), JSON.stringify(table));
}

// --- Les couleurs ---------------------------------------------------------------
writeFileSync(join(DIST, 'palettes.css'), palettesCss({
  colors: readFileSync(join(RES, 'values', 'colors.xml'), 'utf8'),
  colorsNuit: readFileSync(join(RES, 'values-night', 'colors.xml'), 'utf8'),
  palettes: readFileSync(join(RES, 'values', 'palettes.xml'), 'utf8'),
  palettesNuit: readFileSync(join(RES, 'values-night', 'palettes.xml'), 'utf8'),
}));

// --- Les icônes des onglets -------------------------------------------------
// Les dessins vectoriels d'Android, en SVG : même tracé, de la couleur du
// texte qui les porte (currentColor), comme la teinte des onglets. Chaque
// onglet a deux dessins : au trait au repos (_off), plein quand il est actif (_on).
mkdirSync(join(DIST, 'icones'), { recursive: true });
const onglets = ['ic_nav_today', 'ic_nav_texts', 'ic_nav_journal', 'ic_nav_settings'];
for (const nom of [...onglets.flatMap((n) => [`${n}_off`, `${n}_on`]), 'ic_chevron']) {
  const xml = readFileSync(join(RES, 'drawable', `${nom}.xml`), 'utf8');
  const vue = xml.match(/android:viewportWidth="([\d.]+)"[\s\S]*?android:viewportHeight="([\d.]+)"/);
  const chemins = [...xml.matchAll(/<path\b([\s\S]*?)\/>/g)].map(([, attrs]) => {
    const a = (n) => attrs.match(new RegExp(`android:${n}="([^"]*)"`))?.[1];
    if (a('fillColor')) {
      return `<path d="${a('pathData')}" fill="currentColor" fill-opacity="${a('fillAlpha') ?? 1}"` +
        ` fill-rule="${a('fillType') === 'evenOdd' ? 'evenodd' : 'nonzero'}"/>`;
    }
    return `<path d="${a('pathData')}" fill="none" stroke="currentColor" stroke-width="${a('strokeWidth')}"` +
      ` stroke-opacity="${a('strokeAlpha') ?? 1}"` +
      ` stroke-linecap="${a('strokeLineCap') ?? 'butt'}" stroke-linejoin="${a('strokeLineJoin') ?? 'miter'}"/>`;
  });
  writeFileSync(join(DIST, 'icones', `${nom}.svg`),
    `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 ${vue[1]} ${vue[2]}">${chemins.join('')}</svg>\n`);
}

// --- Les textes vérifiés, tels quels ---------------------------------------
for (const nom of ['chizuk.json', 'bibliotheque.json', 'tikkun_haklali.json', 'tehilim.json']) {
  cpSync(join(RES, 'raw', nom), join(DIST, 'data', nom));
}

// --- Les illustrations -------------------------------------------------------
mkdirSync(join(DIST, 'img'), { recursive: true });
for (const nom of readdirSync(join(RES, 'drawable-nodpi'))) {
  if (nom.endsWith('.webp')) cpSync(join(RES, 'drawable-nodpi', nom), join(DIST, 'img', nom));
}

// --- La musique ---------------------------------------------------------------
// L'ordre et les titres sont ceux de MusicLibrary.kt : la même liste que
// l'application Android, lue dans son source.
const musicLibrary = readFileSync(join(ANDROID, 'java', 'com', 'myhitbodedut', 'MusicLibrary.kt'), 'utf8');
const morceaux = [...musicLibrary.matchAll(/Bundled\("([a-z0-9_]+)", R\.raw\.\1, R\.string\.(track_[a-z0-9_]+)\)/g)]
  .map(([, id, titre]) => ({ id, titre, fichier: `musique/${id}.mp3` }));
if (morceaux.length === 0) throw new Error('Aucun morceau trouvé dans MusicLibrary.kt');
mkdirSync(join(DIST, 'musique'), { recursive: true });
for (const m of morceaux) {
  const source = join(RES, 'raw', `${m.id}.mp3`);
  if (!existsSync(source)) throw new Error(`Morceau manquant : ${source}`);
  cpSync(source, join(DIST, m.fichier));
  m.octets = statSync(source).size;
}
writeFileSync(join(DIST, 'data', 'musique.json'), JSON.stringify(morceaux));

// --- Le service worker ----------------------------------------------------
// La musique n'est pas dans la liste : ~120 Mo téléchargés d'office à
// l'installation, c'est trop. Chaque morceau est gardé la première fois
// qu'on l'écoute (voir sw.js).
function fichiers(dossier) {
  return readdirSync(dossier).flatMap((nom) => {
    const chemin = join(dossier, nom);
    return statSync(chemin).isDirectory() ? fichiers(chemin) : [chemin];
  });
}
const coquille = fichiers(DIST)
  .map((f) => relative(DIST, f).split('\\').join('/'))
  .filter((f) => f !== 'sw.js' && !f.startsWith('musique/'))
  .sort();
const empreinte = createHash('sha256');
for (const f of coquille) empreinte.update(f).update(readFileSync(join(DIST, f)));
const version = empreinte.digest('hex').slice(0, 12);

const sw = readFileSync(join(DIST, 'sw.js'), 'utf8')
  .replace('__VERSION__', version)
  .replace('__COQUILLE__', JSON.stringify(['./', ...coquille.filter((f) => f !== 'index.html')]));
writeFileSync(join(DIST, 'sw.js'), sw);
writeFileSync(join(DIST, 'version.txt'), `${version}\n`);

const taille = (liste) => liste.reduce((s, f) => s + statSync(join(DIST, f)).size, 0);
const mo = (o) => `${(o / 1e6).toFixed(2)} Mo`;
console.log(`Version ${version}`);
console.log(`Coquille hors connexion : ${coquille.length} fichiers, ${mo(taille(coquille))}`);
console.log(`Musique : ${morceaux.length} morceaux, ${mo(morceaux.reduce((s, m) => s + m.octets, 0))}`);
