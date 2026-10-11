// Chaque écran du site construit, à chaque taille : petit téléphone,
// téléphone debout et couché, tablette debout et couchée, ordinateur.
// Échoue sur toute erreur de la page, sur tout débordement en largeur (de la
// page ou d'une zone qui défile), et si les onglets ne sont pas à leur place :
// en bas sur téléphone et tablette debout, sur le côté à partir de 900 px
// et sur le téléphone couché. Le bouton principal de l'accueil et du bravo
// doit se voir sans faire défiler (sauf sur le plus petit des téléphones),
// et celui de la séance partout, le plus petit compris. Le téléphone couché est
// vérifié dans tous les modes : trois langues, clair et sombre, avec prénom,
// séance en cours, journée faite.
//
//   node web/test/tailles.mjs [chromium|webkit] [dossier de sortie]
//   FILTRE=couche-bravo node web/test/tailles.mjs   → seulement les cas dont le nom contient ce motif
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
  ['iphone-se', 375, 667],
  ['telephone', 390, 844],
  ['telephone-couche', 844, 390],
  ['tablette', 820, 1180],
  ['tablette-couchee', 1180, 820],
  ['ordinateur', 1440, 900],
];
// Le téléphone couché, dans tous les modes : trois tailles, trois langues, clair et sombre.
const COUCHES = [
  ['se-couche', 667, 375],
  ['telephone-couche', 844, 390],
  ['grand-telephone-couche', 932, 430],
];
// Les onglets passent sur le côté à partir de 900 px, ou sur un téléphone couché.
const couche = (l, h) => l > h && h <= 500;
const ontLaColonne = (l, h) => l >= 900 || couche(l, h);

const JOURNAL = { '2026-09-27': { minutes: 20, note: '' }, '2026-09-28': { minutes: 60, note: '' } };
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

// nom, écran d'arrivée (#…), actions, onglets attendus, réglages et journal
// propres, et le bouton principal qui doit se voir sans faire défiler.
const ECRANS = [
  { nom: 'accueil', onglets: true, bouton: '.accueil .principal' },
  { nom: 'accueil-prenom', onglets: true, bouton: '.accueil .principal', prefs: { first_name: 'Hillel' } },
  { nom: 'accueil-faite', onglets: true, bouton: '.accueil .principal', journal: { '2026-09-29': { minutes: 30, note: '' } } },
  { nom: 'accueil-en-cours', ecran: 'seance', onglets: true, bouton: '.accueil .principal',
    actions: async (p) => { await p.click('.duree'); await p.click('.retour'); await p.waitForSelector('.accueil'); } },
  { nom: 'accueil-feuille', onglets: true, actions: clic('.pastille') },
  { nom: 'textes', onglets: true, actions: clic('[data-onglet="textes"]') },
  { nom: 'musique', ecran: 'musique' },
  { nom: 'reglages', ecran: 'reglages' },
  { nom: 'seance', ecran: 'seance', bouton: '.seance .principal', petitAussi: true },
  { nom: 'seance-en-cours', ecran: 'seance', actions: clic('.duree'), bouton: '.seance .principal', petitAussi: true },
  { nom: 'bravo', ecran: 'seance', actions: finDeSeance, bouton: '.bravo .principal' },
  { nom: 'tehilim', ecran: 'tehilim' },
  { nom: 'tehilim-lecture', ecran: 'tehilim', actions: clic('.carte .ligne') },
  { nom: 'tehilim-lecture-barre', ecran: 'tehilim', actions: async (p) => { await clic('.carte .ligne')(p); await clic('.lecture-pastille')(p); } },
  { nom: 'siddur', ecran: 'siddur' },
  { nom: 'siddur-nusah', ecran: 'siddur', actions: clic('.puce-bandeau') },
  { nom: 'priere', ecran: 'siddur', actions: clic('.carte-sections .ligne') },
  { nom: 'tikkun', ecran: 'tikkun' },
  { nom: 'bibliotheque', ecran: 'bibliotheque' },
  { nom: 'source', ecran: 'bibliotheque', actions: clic('.carte-sections .ligne') },
  { nom: 'zmanim', ecran: 'zmanim' },
  { nom: 'calendrier', ecran: 'calendrier' },
  { nom: 'calendrier-civil', ecran: 'calendrier', actions: clic('.vue-calendrier button:nth-child(2)') },
];

let verifies = 0;
async function verifie(langue, mode, [taille, largeur, hauteur], e) {
  const id = `${langue}-${mode}-${taille}-${e.nom}`;
  if (process.env.FILTRE && !new RegExp(process.env.FILTRE).test(id)) return;
  verifies++;
  const contexte = await navigateur.newContext({
    viewport: { width: largeur, height: hauteur }, serviceWorkers: 'block', reducedMotion: 'reduce',
    colorScheme: mode === 'sombre' ? 'dark' : 'light',
  });
  const page = await contexte.newPage();
  page.on('console', (m) => { if (m.type() === 'error') erreurs.push(`${id} : ${m.text()}`); });
  page.on('pageerror', (x) => erreurs.push(`${id} : ${x.message}`));
  page.on('dialog', (d) => { erreurs.push(`${id} : boîte « ${d.message()} »`); d.dismiss().catch(() => {}); });
  page.on('request', (r) => { if (!r.url().startsWith(base)) erreurs.push(`${id} : requête hors du site ${r.url()}`); });
  // Pas de musique pendant la séance : la mesure porte sur la mise en page.
  const prefs = JSON.stringify({ langue_app: langue, apparence_mode: mode, music_enabled: false, ...e.prefs });
  const journal = JSON.stringify({ days: { ...JOURNAL, ...e.journal } });
  await page.addInitScript(({ journal, prefs, maintenant }) => {
    const Vrai = Date;
    const decalage = maintenant - Vrai.now();
    globalThis.Date = class extends Vrai {
      constructor(...a) { super(...(a.length ? a : [Vrai.now() + decalage])); }
      static now() { return Vrai.now() + decalage; }
    };
    // Une fois seulement : un rechargement garde ce que la page a écrit.
    if (!sessionStorage.getItem('pret')) {
      localStorage.setItem('myhitbodedut-journal', journal);
      localStorage.setItem('myhitbodedut', prefs);
      sessionStorage.setItem('pret', '1');
    }
    HTMLMediaElement.prototype.play = function play() { return Promise.resolve(); };
  }, { journal, prefs, maintenant: MAINTENANT });
  try {
    await page.goto(base + (e.ecran ? `#${e.ecran}` : ''));
    await page.waitForSelector('#app > *', { timeout: 8000 });
    if (e.actions) await e.actions(page);
    await page.waitForTimeout(500);
    await page.screenshot({ path: join(SORTIE, `${moteur}-${id}.png`), animations: 'disabled', timeout: 15000 });
    const mesure = await page.evaluate((sel) => {
      const doc = document.documentElement;
      const zones = [...document.querySelectorAll('.defile, .lecture, .feuille')]
        .filter((z) => z.scrollWidth > z.clientWidth + 1)
        .map((z) => `${z.className} ${z.clientWidth}/${z.scrollWidth}`);
      const nav = document.querySelector('.onglets')?.getBoundingClientRect();
      const bouton = sel ? document.querySelector(sel)?.getBoundingClientRect() : null;
      return {
        deborde: doc.scrollWidth > window.innerWidth, zones,
        bouton: bouton ? bouton.bottom : null,
        nav: nav && { haut: nav.top, bas: nav.bottom, gauche: nav.left, droite: nav.right, largeur: nav.width, hauteur: nav.height },
      };
    }, e.bouton ?? null);
    if (mesure.deborde) erreurs.push(`${id} : la page déborde en largeur`);
    for (const z of mesure.zones) erreurs.push(`${id} : zone qui déborde en largeur (${z})`);
    const n = mesure.nav;
    if (e.onglets) {
      if (!n) erreurs.push(`${id} : onglets absents`);
      else if (ontLaColonne(largeur, hauteur)) {
        const cote = langue === 'he' ? Math.abs(n.droite - largeur) < 1 : n.gauche < 1;
        if (n.hauteur < n.largeur || !cote) erreurs.push(`${id} : onglets attendus en colonne sur le côté ${JSON.stringify(n)}`);
      } else if (Math.abs(n.bas - hauteur) > 1 || n.largeur < largeur - 1) {
        erreurs.push(`${id} : onglets attendus en bas ${JSON.stringify(n)}`);
      }
    }
    // Le bouton principal se voit sans faire défiler : partout pour l'accueil
    // et le bravo (sauf le plus petit des téléphones), et partout, le plus
    // petit compris, pour la séance.
    const exige = e.bouton && (taille !== 'petit-telephone' || e.petitAussi);
    if (exige) {
      const limite = n && !ontLaColonne(largeur, hauteur) ? n.haut : hauteur;
      if (!(mesure.bouton !== null && mesure.bouton <= limite + 0.5)) {
        erreurs.push(`${id} : bouton principal sous le pli (bas ${mesure.bouton}, limite ${limite})`);
      }
    }
  } catch (x) {
    erreurs.push(`${id} : ${x.message.split('\n')[0]}`);
  }
  await contexte.close();
}

for (const langue of ['fr', 'he']) {
  for (const taille of TAILLES) {
    for (const e of ECRANS) await verifie(langue, 'clair', taille, e);
  }
}
for (const langue of ['fr', 'en', 'he']) {
  for (const mode of ['clair', 'sombre']) {
    for (const taille of COUCHES) {
      for (const e of ECRANS) await verifie(langue, mode, taille, e);
    }
  }
}

await navigateur.close();
serveur.close();
if (erreurs.length) {
  console.error(erreurs.join('\n'));
  process.exit(1);
}
console.log(`${verifies} écrans vérifiés (${moteur}), captures dans ${SORTIE}`);
