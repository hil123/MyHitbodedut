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

// --- Le relais du suivi de vol en direct (28/10) -----------------------------
// Sans RELAIS_VOL, le suivi reste inactif et la CSP n'autorise que le site et
// api.adsbdb.com (la route d'un numéro de vol, au toucher). Avec, son origine
// seule s'ajoute à connect-src, en HTTPS.
const relais = process.env.RELAIS_VOL ?? '';
if (relais) {
  if (!relais.startsWith('https://')) throw new Error(`RELAIS_VOL doit être en https:// : ${relais}`);
  const origine = new URL(relais).origin;
  writeFileSync(join(DIST, 'js', 'relais.js'), `export const RELAIS_VOL = ${JSON.stringify(relais)};\n`);
  const page = readFileSync(join(DIST, 'index.html'), 'utf8');
  if (!page.includes("connect-src 'self' ")) throw new Error('CSP : connect-src introuvable');
  writeFileSync(join(DIST, 'index.html'), page.replace("connect-src 'self' ", `connect-src 'self' ${origine} `));
}

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
    tableaux: { ...defaut.tableaux, ...propre.tableaux },
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
const onglets = ['ic_nav_today', 'ic_nav_texts', 'ic_nav_zmanim', 'ic_nav_journal', 'ic_nav_settings'];
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
for (const nom of ['chizuk.json', 'bibliotheque.json', 'tikkun_haklali.json', 'tehilim.json', 'siddur_edot.json', 'siddur_sefard.json', 'siddur_ashkenaz.json', 'villes.json', 'aeroports.json', 'aeroports_noms.json', 'siddur_annotations.json', 'hilloulot.json', 'ben_ish_hai.json', 'yalkut_yosef.json', 'guide_sujets.json']) {
  cpSync(join(RES, 'raw', nom), join(DIST, 'data', nom));
}

// --- Les biographies des hilloulot, un fichier par mois (04/11) -----------------
// hilloulot_bios.json pèse 6,9 Mo : l'écran des hilloulot le téléchargeait et l'analysait en entier
// pour n'afficher qu'un jour (300 ms bloquées sur un téléphone moyen, sans compter le réseau).
// Ses clés sont « mois-jour » (mois de KosherJava, adar, adar1, adar2) : un fichier par mois.
{
  const bios = JSON.parse(readFileSync(join(RES, 'raw', 'hilloulot_bios.json'), 'utf8'));
  mkdirSync(join(DIST, 'data', 'hilloulot_bios'), { recursive: true });
  const parMois = {};
  for (const [cle, ligne] of Object.entries(bios)) {
    const mois = cle.split('-')[0];
    if (!/^[a-z0-9]+$/.test(mois)) throw new Error(`Clé de hilloula inattendue : ${cle}`);
    (parMois[mois] ??= {})[cle] = ligne;
  }
  for (const [mois, table] of Object.entries(parMois)) writeFileSync(join(DIST, 'data', 'hilloulot_bios', `${mois}.json`), JSON.stringify(table));
}

// --- Les livres, un fichier chacun (02/11) --------------------------------------
// livres.json pèse 16 Mo : le lire en entier pour ouvrir Likoutei Etsot (400 Ko) faisait
// tomber Safari sur iPhone. Le site lit la table (titres, groupes, nombre de chapitres), puis
// le seul livre ouvert. Les textes restent ceux d'Android, octet pour octet dans leur contenu.
{
  // La source des livres (hors des ressources Android depuis le 04/11 : l'application en reçoit la découpe).
  const livres = JSON.parse(readFileSync(join(ANDROID, 'livres', 'livres.json'), 'utf8'));
  mkdirSync(join(DIST, 'data', 'livres'), { recursive: true });
  const table = { _meta: livres._meta, groupes: livres.groupes, textes: [] };
  for (const t of livres.textes) {
    if (!/^[a-z0-9_]+$/.test(t.id)) throw new Error(`Identifiant de livre inattendu : ${t.id}`);
    const { sections, ...entete } = t;
    table.textes.push({ ...entete, chapitres: sections.length });
    writeFileSync(join(DIST, 'data', 'livres', `${t.id}.json`), JSON.stringify({ id: t.id, sections }));
  }
  writeFileSync(join(DIST, 'data', 'livres', 'table.json'), JSON.stringify(table));
}

// --- Les illustrations -------------------------------------------------------
mkdirSync(join(DIST, 'img'), { recursive: true });
for (const nom of readdirSync(join(RES, 'drawable-nodpi'))) {
  if (nom.endsWith('.webp')) cpSync(join(RES, 'drawable-nodpi', nom), join(DIST, 'img', nom));
}

// --- La musique ---------------------------------------------------------------
// L'ordre et les titres sont ceux de l'application Android, lus dans son
// source. Depuis le 29/10 la liste y est en deux (PUBLICATION_PLAY_STORE.md
// § 3.2) : MusiquePlay.kt, fichiers dans main/res/raw, puis MusiqueHorsPlay.kt
// de src/musique_hors_play, fichiers dans son propre res/raw. Le site garde
// les deux, comme avant.
const HORS_PLAY = join(ANDROID, '..', 'musique_hors_play');
const listes = [
  { kt: join(ANDROID, 'java', 'com', 'myhitbodedut', 'MusiquePlay.kt'), raw: join(RES, 'raw') },
  { kt: join(HORS_PLAY, 'java', 'com', 'myhitbodedut', 'MusiqueHorsPlay.kt'), raw: join(HORS_PLAY, 'res', 'raw') },
];
const morceaux = listes.flatMap(({ kt, raw }) =>
  [...readFileSync(kt, 'utf8').matchAll(/Bundled\("([a-z0-9_]+)", R\.raw\.\1, R\.string\.(track_[a-z0-9_]+)\)/g)]
    .map(([, id, titre]) => ({ id, titre, fichier: `musique/${id}.m4a`, raw })));
if (morceaux.length === 0) throw new Error('Aucun morceau trouvé dans MusiquePlay.kt ni MusiqueHorsPlay.kt');
mkdirSync(join(DIST, 'musique'), { recursive: true });
for (const m of morceaux) {
  const source = join(m.raw, `${m.id}.m4a`);
  delete m.raw;
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
