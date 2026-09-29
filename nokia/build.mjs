// Construit le pack pour téléphone à touches (Nokia 3310 de 2017, 2G ou
// 3G) : des images 240×320 à lire dans la galerie, et la musique de
// l'application, à copier sur la carte microSD ou à envoyer par Bluetooth.
// Aucune connexion n'est nécessaire sur le téléphone.
//
//   node nokia/build.mjs [--sans-musique]
//
// Sortie : nokia/dist/MyHitbodedut/{FR,EN,HE}/*.jpg et musique/*.mp3, plus
// un zip par langue (images seules) et un zip de la musique.
//
// Le texte est dessiné d'avance : le téléphone n'a pas à savoir afficher
// l'hébreu ni ses voyelles. Chaque page est découpée par le navigateur
// (colonnes CSS), donc une ligne n'est jamais coupée en deux.

import { execFileSync } from 'node:child_process';
import { cpSync, mkdirSync, readFileSync, rmSync, statSync, writeFileSync } from 'node:fs';
import { createRequire } from 'node:module';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { litChaines } from '../web/tools/chaines.mjs';
import { HAUTEUR, LANGUES, LARGEUR, nomDePage, planLangue, sens } from './plan.mjs';

const ICI = dirname(fileURLToPath(import.meta.url));
const RES = join(ICI, '..', 'android', 'app', 'src', 'main', 'res');
const JAVA = join(ICI, '..', 'android', 'app', 'src', 'main', 'java', 'com', 'myhitbodedut');
const DIST = join(ICI, 'dist');
const PACK = join(DIST, 'MyHitbodedut');
const avecMusique = !process.argv.includes('--sans-musique');

const lit = (...p) => readFileSync(join(...p), 'utf8');
const chizuk = JSON.parse(lit(RES, 'raw', 'chizuk.json'));
const tikkun = JSON.parse(lit(RES, 'raw', 'tikkun_haklali.json'));
const guideJson = JSON.parse(lit(ICI, 'guide.json'));

// Les chaînes comme Android : le défaut (français) sous chaque langue.
const defaut = litChaines(lit(RES, 'values', 'strings.xml')).chaines;
const dossierValues = { fr: 'values', en: 'values-en', he: 'values-iw' };
const chainesDe = (langue) => ({ ...defaut, ...litChaines(lit(RES, dossierValues[langue], 'strings.xml')).chaines });

// --- La page -----------------------------------------------------------------
// Couleurs de l'application (values/colors.xml) : paper, ink, brand, muted, line.
const ENTETE = 30;
const PIED = 20;
const MARGE = 8;
const ECART = 2 * MARGE;
const CORPS = HAUTEUR - ENTETE - PIED;
const COLONNE = LARGEUR - 2 * MARGE;

const GABARIT = `<!doctype html><html><head><meta charset="utf-8"><style>
  * { box-sizing: border-box; margin: 0; padding: 0; }
  html, body { width: ${LARGEUR}px; height: ${HAUTEUR}px; overflow: hidden; background: #F4F7F2;
    font-family: 'DejaVu Sans', sans-serif; color: #172B27; }
  #entete { height: ${ENTETE}px; display: flex; align-items: center; gap: 6px; padding: 0 ${MARGE}px;
    background: #16775A; color: #fff; font-size: 12px; font-weight: bold; }
  #titre { flex: 1; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
  #numero { font-weight: normal; font-size: 11px; }
  #corps { height: ${CORPS}px; overflow: hidden; column-width: ${COLONNE}px; column-gap: ${ECART}px;
    column-fill: auto; padding: 6px ${MARGE}px 0; direction: ltr; }
  #pied { height: ${PIED}px; display: flex; align-items: center; padding: 0 ${MARGE}px;
    border-top: 1px solid #DCE7DF; color: #6B7D76; font-size: 10px; white-space: nowrap; overflow: hidden; }
  p { margin-bottom: 7px; orphans: 2; widows: 2; }
  .ensemble { break-inside: avoid; }
  .he, .verset, .arret { direction: rtl; text-align: right; }
  .he { font-size: 17px; line-height: 1.5; }
  .verset { font-size: 17px; line-height: 1.5; }
  .verset b { color: #16775A; font-size: 12px; font-weight: bold; margin-left: 4px; }
  .arret { font-size: 12px; color: #6B7D76; font-style: italic; }
  .trad { font-size: 13.5px; line-height: 1.4; }
  .note { font-size: 12px; color: #6B7D76; line-height: 1.35; }
  .ligne { font-size: 13px; line-height: 1.38; }
  .rtl .ligne, .rtl .note, .rtl .trad { direction: rtl; text-align: right; }
</style></head><body><div id="entete"><span id="titre"></span><span id="numero"></span></div>
<div id="corps"></div><div id="pied"></div></body></html>`;

/** Place un document et renvoie son nombre de pages. */
function remplit(doc) {
  const echappe = (s) => s.replace(/[&<>]/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;' })[c]);
  // Un mot latin dans une phrase hébraïque (HE_2, MyHitbodedut) ne doit
  // pas emporter la ponctuation voisine : il est isolé.
  const isole = (html) => html.replace(/[A-Za-z][A-Za-z0-9_]*/g, '<bdi>$&</bdi>');
  // Un verset et l'indication d'arrêt qui le suit restent sur la même page.
  const morceaux = [];
  for (const b of doc.blocs) {
    const p = b.type === 'verset'
      ? `<p class="verset"><b>${echappe(b.n)}</b>${echappe(b.texte)}</p>`
      : `<p class="${b.type}">${isole(echappe(b.texte))}</p>`;
    if (b.type === 'arret' && morceaux.length) morceaux[morceaux.length - 1].push(p);
    else morceaux.push([p]);
  }
  const html = morceaux.map((m) => (m.length > 1 ? `<div class="ensemble">${m.join('')}</div>` : m[0])).join('');
  return { html, titre: doc.titre, pied: doc.pied ?? '', dir: sens(doc.langue) };
}

// --- Construction ----------------------------------------------------------------
rmSync(DIST, { recursive: true, force: true });
mkdirSync(PACK, { recursive: true });

const require = createRequire(import.meta.url);
const pw = require(process.env.PLAYWRIGHT_MODULE || 'playwright');
const options = process.env.CHROMIUM_PATH ? { executablePath: process.env.CHROMIUM_PATH } : {};
const navigateur = await pw.chromium.launch(options);
const page = await navigateur.newPage({ viewport: { width: LARGEUR, height: HAUTEUR }, deviceScaleFactor: 1 });
await page.setContent(GABARIT);

const manifeste = { largeur: LARGEUR, hauteur: HAUTEUR, langues: {} };
for (const langue of LANGUES) {
  const dossier = join(PACK, langue.toUpperCase());
  mkdirSync(dossier, { recursive: true });
  const docs = planLangue({ langue, chizuk, tikkun, guideJson, chaines: chainesDe(langue) });
  manifeste.langues[langue] = [];
  for (const doc of docs) {
    const pages = await page.evaluate(({ html, titre, pied, dir }) => {
      document.body.className = dir;
      document.getElementById('entete').dir = dir;
      document.getElementById('pied').dir = dir;
      document.getElementById('titre').textContent = titre;
      document.getElementById('pied').textContent = pied;
      const corps = document.getElementById('corps');
      corps.innerHTML = html;
      corps.scrollLeft = 0;
      // Une colonne et son écart font exactement une largeur de page.
      return Math.max(1, Math.round(corps.scrollWidth / corps.clientWidth));
    }, remplit(doc));
    const fichiers = [];
    for (let k = 1; k <= pages; k++) {
      await page.evaluate(({ k, pages }) => {
        const corps = document.getElementById('corps');
        corps.scrollLeft = (k - 1) * corps.clientWidth;
        document.getElementById('numero').textContent = pages > 1 ? `${k}/${pages}` : '';
      }, { k, pages });
      const nom = nomDePage(doc, k);
      await page.screenshot({ path: join(dossier, nom), type: 'jpeg', quality: 85 });
      fichiers.push(nom);
    }
    manifeste.langues[langue].push({ base: doc.base, id: doc.id ?? null, pages: fichiers });
  }
}
await navigateur.close();

// --- La musique --------------------------------------------------------------------
// L'ordre de MusicLibrary.kt, comme l'application et le site.
const morceaux = [...lit(JAVA, 'MusicLibrary.kt').matchAll(/Bundled\("([a-z0-9_]+)", R\.raw\.\1, R\.string\.track_[a-z0-9_]+\)/g)]
  .map(([, id], i) => ({ id, fichier: `${String(i + 1).padStart(2, '0')}_${id}.mp3` }));
if (morceaux.length === 0) throw new Error('Aucun morceau trouvé dans MusicLibrary.kt');
manifeste.musique = avecMusique ? morceaux.map((m) => m.fichier) : [];
if (avecMusique) {
  mkdirSync(join(PACK, 'musique'), { recursive: true });
  for (const m of morceaux) cpSync(join(RES, 'raw', `${m.id}.mp3`), join(PACK, 'musique', m.fichier));
}
writeFileSync(join(DIST, 'manifeste.json'), `${JSON.stringify(manifeste, null, 1)}\n`);

// --- Les zips : un par langue, et la musique à part ------------------------------------
const zip = (nom, dossier) => execFileSync('zip', ['-q', '-r', '-X', join(DIST, nom), dossier], { cwd: PACK });
for (const langue of LANGUES) zip(`MyHitbodedut-Nokia-${langue.toUpperCase()}.zip`, langue.toUpperCase());
if (avecMusique) zip('MyHitbodedut-Nokia-musique.zip', 'musique');

const mo = (o) => `${(o / 1e6).toFixed(2)} Mo`;
for (const langue of LANGUES) {
  const n = manifeste.langues[langue].reduce((s, d) => s + d.pages.length, 0);
  const z = statSync(join(DIST, `MyHitbodedut-Nokia-${langue.toUpperCase()}.zip`)).size;
  console.log(`${langue.toUpperCase()} : ${manifeste.langues[langue].length} documents, ${n} images, zip ${mo(z)}`);
}
if (avecMusique) console.log(`Musique : ${morceaux.length} morceaux, zip ${mo(statSync(join(DIST, 'MyHitbodedut-Nokia-musique.zip')).size)}`);
