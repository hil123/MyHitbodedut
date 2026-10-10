// Chaque écran du site construit, à chaque taille : petit téléphone,
// téléphone debout et couché, tablette debout et couchée, ordinateur.
// Échoue sur toute erreur de la page, sur tout débordement en largeur (de la
// page ou d'une zone qui défile), et si les onglets ne sont pas à leur place :
// en bas sur téléphone et tablette debout, sur le côté à partir de 900 px.
//
//   node web/test/tailles.mjs [chromium|webkit] [dossier de sortie]
//
// Le site doit être construit (node web/build.mjs).

import { mkdirSync } from 'node:fs';
import { createRequire } from 'node:module';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { demarreServeur } from '../tools/serveur.mjs';

const ICI = dirname(fileURLToPath(import.meta.url));
const moteur = process.argv[2] || 'chromium';
const SORTIE = process.argv[3] || join(ICI, '..', 'captures', 'tailles');
mkdirSync(SORTIE, { recursive: true });

const require = createRequire(import.meta.url);
const pw = require(process.env.PLAYWRIGHT_MODULE || 'playwright');

const serveur = await demarreServeur();
const base = `http://127.0.0.1:${serveur.address().port}/`;
const options = moteur === 'chromium' && process.env.CHROMIUM_PATH ? { executablePath: process.env.CHROMIUM_PATH } : {};
const navigateur = await pw[moteur].launch(options);
const erreurs = [];

const TAILLES = [
  ['petit-telephone', 320, 568],
  ['telephone', 390, 844],
  ['telephone-couche', 844, 390],
  ['tablette', 820, 1180],
  ['tablette-couchee', 1180, 820],
  ['ordinateur', 1440, 900],
];
const COLONNE = 900; // largeur à partir de laquelle les onglets passent sur le côté

const DONNEES = {
  'myhitbodedut-journal': JSON.stringify({ days: { '2026-09-27': { minutes: 20, note: '' }, '2026-09-28': { minutes: 60, note: '' } } }),
};
const MAINTENANT = new Date('2026-09-29T09:00:00').getTime();

const clic = (sel) => (p) => p.locator(sel).first().click();
const finDeSeance = async (p) => {
  await p.click('.duree');
  await p.evaluate(() => {
    const prefs = JSON.parse(localStorage.getItem('myhitbodedut'));
    prefs.session_started_at -= 5 * 60000 + 1000;
    prefs.session_ends_at = Date.now() - 1000;
    localStorage.setItem('myhitbodedut', JSON.stringify(prefs));
  });
  await p.waitForSelector('.bravo', { timeout: 5000 });
};

// [nom, écran d'arrivée (#…), actions, onglets attendus]
const ECRANS = [
  ['accueil', '', null, true],
  ['accueil-feuille', '', clic('.pastille'), true],
  ['textes', '', clic('[data-onglet="textes"]'), true],
  ['musique', 'musique'],
  ['reglages', 'reglages'],
  ['seance', 'seance'],
  ['seance-en-cours', 'seance', clic('.duree')],
  ['bravo', 'seance', finDeSeance],
  ['tehilim', 'tehilim'],
  ['tehilim-lecture', 'tehilim', clic('.carte .ligne')],
  ['tehilim-lecture-barre', 'tehilim', async (p) => { await clic('.carte .ligne')(p); await clic('.lecture-pastille')(p); }],
  ['siddur', 'siddur'],
  ['siddur-nusah', 'siddur', clic('.puce-bandeau')],
  ['priere', 'siddur', clic('.carte-sections .ligne')],
  ['tikkun', 'tikkun'],
  ['bibliotheque', 'bibliotheque'],
  ['source', 'bibliotheque', clic('.carte-sections .ligne')],
];

async function verifie(langue, [taille, largeur, hauteur], [nom, ecran, actions, avecOnglets]) {
  const id = `${langue}-${taille}-${nom}`;
  const contexte = await navigateur.newContext({
    viewport: { width: largeur, height: hauteur }, serviceWorkers: 'block', reducedMotion: 'reduce',
  });
  const page = await contexte.newPage();
  page.on('console', (m) => { if (m.type() === 'error') erreurs.push(`${id} : ${m.text()}`); });
  page.on('pageerror', (e) => erreurs.push(`${id} : ${e.message}`));
  page.on('dialog', (d) => { erreurs.push(`${id} : boîte « ${d.message()} »`); d.dismiss().catch(() => {}); });
  page.on('request', (r) => { if (!r.url().startsWith(base)) erreurs.push(`${id} : requête hors du site ${r.url()}`); });
  // Pas de musique pendant la séance : la mesure porte sur la mise en page.
  const prefs = JSON.stringify({ langue_app: langue, music_enabled: false });
  await page.addInitScript(({ donnees, prefs, maintenant }) => {
    const Vrai = Date;
    const decalage = maintenant - Vrai.now();
    globalThis.Date = class extends Vrai {
      constructor(...a) { super(...(a.length ? a : [Vrai.now() + decalage])); }
      static now() { return Vrai.now() + decalage; }
    };
    for (const [k, v] of Object.entries(donnees)) localStorage.setItem(k, v);
    localStorage.setItem('myhitbodedut', prefs);
    HTMLMediaElement.prototype.play = function play() { return Promise.resolve(); };
  }, { donnees: DONNEES, prefs, maintenant: MAINTENANT });
  try {
    await page.goto(base + (ecran ? `#${ecran}` : ''));
    await page.waitForSelector('#app > *', { timeout: 8000 });
    if (actions) await actions(page);
    await page.waitForTimeout(500);
    await page.screenshot({ path: join(SORTIE, `${moteur}-${id}.png`), animations: 'disabled', timeout: 15000 });
    const mesure = await page.evaluate(() => {
      const doc = document.documentElement;
      const zones = [...document.querySelectorAll('.defile, .lecture, .feuille')]
        .filter((e) => e.scrollWidth > e.clientWidth + 1)
        .map((e) => `${e.className} ${e.clientWidth}/${e.scrollWidth}`);
      const nav = document.querySelector('.onglets')?.getBoundingClientRect();
      return {
        deborde: doc.scrollWidth > window.innerWidth, zones,
        nav: nav && { haut: nav.top, bas: nav.bottom, gauche: nav.left, droite: nav.right, largeur: nav.width, hauteur: nav.height },
      };
    });
    if (mesure.deborde) erreurs.push(`${id} : la page déborde en largeur`);
    for (const z of mesure.zones) erreurs.push(`${id} : zone qui déborde en largeur (${z})`);
    if (avecOnglets) {
      const n = mesure.nav;
      if (!n) erreurs.push(`${id} : onglets absents`);
      else if (largeur >= COLONNE) {
        const cote = langue === 'he' ? Math.abs(n.droite - largeur) < 1 : n.gauche < 1;
        if (n.hauteur < n.largeur || !cote) erreurs.push(`${id} : onglets attendus en colonne sur le côté ${JSON.stringify(n)}`);
      } else if (Math.abs(n.bas - hauteur) > 1 || n.largeur < largeur - 1) {
        erreurs.push(`${id} : onglets attendus en bas ${JSON.stringify(n)}`);
      }
    }
  } catch (e) {
    erreurs.push(`${id} : ${e.message.split('\n')[0]}`);
  }
  await contexte.close();
}

for (const langue of ['fr', 'he']) {
  for (const taille of TAILLES) {
    for (const ecran of ECRANS) await verifie(langue, taille, ecran);
  }
}

await navigateur.close();
serveur.close();
if (erreurs.length) {
  console.error(erreurs.join('\n'));
  process.exit(1);
}
console.log(`${2 * TAILLES.length * ECRANS.length} écrans vérifiés (${moteur}), captures dans ${SORTIE}`);
