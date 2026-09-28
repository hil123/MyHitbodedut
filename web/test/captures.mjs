// Captures d'écran du site construit, dans un navigateur, à la taille d'un
// iPhone. Échoue sur toute erreur de la page (console, exception, requête
// hors du site) : l'application promet que rien ne quitte le téléphone.
//
//   node web/test/captures.mjs [chromium|webkit] [dossier de sortie]
//
// Le site doit être construit (node web/build.mjs).

import { createServer } from 'node:http';
import { mkdirSync, readFileSync, statSync } from 'node:fs';
import { createRequire } from 'node:module';
import { dirname, extname, join, normalize } from 'node:path';
import { fileURLToPath } from 'node:url';

const ICI = dirname(fileURLToPath(import.meta.url));
const DIST = join(ICI, '..', 'dist');
const moteur = process.argv[2] || 'chromium';
const SORTIE = process.argv[3] || join(ICI, '..', 'captures');
mkdirSync(SORTIE, { recursive: true });

const require = createRequire(import.meta.url);
const pw = require(process.env.PLAYWRIGHT_MODULE || 'playwright');

const TYPES = {
  '.html': 'text/html; charset=utf-8', '.js': 'text/javascript', '.css': 'text/css', '.json': 'application/json',
  '.webp': 'image/webp', '.png': 'image/png', '.svg': 'image/svg+xml', '.mp3': 'audio/mpeg',
  '.webmanifest': 'application/manifest+json', '.txt': 'text/plain',
};
const serveur = createServer((req, res) => {
  const chemin = decodeURIComponent(new URL(req.url, 'http://x').pathname);
  let fichier = normalize(join(DIST, chemin));
  if (!fichier.startsWith(DIST)) { res.writeHead(403).end(); return; }
  try { if (statSync(fichier).isDirectory()) fichier = join(fichier, 'index.html'); } catch { /* 404 plus bas */ }
  let corps;
  try { corps = readFileSync(fichier); } catch { res.writeHead(404).end(); return; }
  res.writeHead(200, { 'Content-Type': TYPES[extname(fichier)] || 'application/octet-stream' });
  res.end(corps);
});
await new Promise((r) => serveur.listen(0, '127.0.0.1', r));
const base = `http://127.0.0.1:${serveur.address().port}/`;

const options = moteur === 'chromium' && process.env.CHROMIUM_PATH ? { executablePath: process.env.CHROMIUM_PATH } : {};
const navigateur = await pw[moteur].launch(options);
const erreurs = [];

// Une semaine connue : deux séances faites, dimanche 27 et lundi 28/09/2026.
const DONNEES = {
  'myhitbodedut-journal': JSON.stringify({ days: { '2026-09-27': { minutes: 20, note: '' }, '2026-09-28': { minutes: 60, note: '' } } }),
};
const MAINTENANT = new Date('2026-09-29T09:00:00').getTime();

async function capture(nom, { langue, mode = 'clair', fond = 'vert', accent = 'vert', prenom = '', ecran = '', onglet = null }) {
  const contexte = await navigateur.newContext({
    viewport: { width: 390, height: 844 }, deviceScaleFactor: 2, colorScheme: mode === 'sombre' ? 'dark' : 'light',
    serviceWorkers: 'block',
  });
  const page = await contexte.newPage();
  page.on('console', (m) => { if (m.type() === 'error') erreurs.push(`${nom} : ${m.text()}`); });
  page.on('pageerror', (e) => erreurs.push(`${nom} : ${e.message}`));
  page.on('request', (r) => { if (!r.url().startsWith(base)) erreurs.push(`${nom} : requête hors du site ${r.url()}`); });
  const prefs = JSON.stringify({
    langue_app: langue, apparence_mode: mode, apparence_fond: fond, apparence_accent: accent, first_name: prenom,
  });
  await page.addInitScript(({ donnees, prefs, maintenant }) => {
    // L'horloge figée : la même semaine à chaque capture.
    const Vrai = Date;
    const decalage = maintenant - Vrai.now();
    globalThis.Date = class extends Vrai {
      constructor(...a) { super(...(a.length ? a : [Vrai.now() + decalage])); }
      static now() { return Vrai.now() + decalage; }
    };
    for (const [k, v] of Object.entries(donnees)) localStorage.setItem(k, v);
    localStorage.setItem('myhitbodedut', prefs);
  }, { donnees: DONNEES, prefs, maintenant: MAINTENANT });
  await page.goto(base + (ecran ? `#${ecran}` : ''));
  await page.waitForSelector('#app > *', { timeout: 8000 }).catch((e) => { console.error(erreurs.join('\n')); throw e; });
  if (onglet) await page.click(`[data-onglet="${onglet}"]`);
  await page.waitForTimeout(900);
  await page.screenshot({ path: join(SORTIE, `${moteur}-${nom}.png`) });
  // Aucun débordement horizontal, en aucune langue.
  const deborde = await page.evaluate(() => document.documentElement.scrollWidth > window.innerWidth);
  if (deborde) erreurs.push(`${nom} : la page déborde en largeur`);
  await contexte.close();
}

await capture('accueil-fr', { langue: 'fr' });
await capture('accueil-he', { langue: 'he', prenom: 'אברהם' });
await capture('accueil-en-sombre', { langue: 'en', mode: 'sombre' });
await capture('accueil-fr-sable-violet', { langue: 'fr', fond: 'sable', accent: 'violet', prenom: 'Hillel' });
await capture('textes-he', { langue: 'he', onglet: 'textes' });
await capture('musique-fr', { langue: 'fr', ecran: 'musique' });
await capture('reglages-en', { langue: 'en', ecran: 'reglages' });

// Le calendrier hébraïque du navigateur lui-même : sous WebKit, c'est celui
// de Safari. Les mêmes jours que HolidaysTest.kt.
{
  const page = await navigateur.newPage();
  await page.goto(`${base}version.txt`);
  const verdicts = await page.evaluate(async (url) => {
    const { estJourChome } = await import(url);
    return {
      '2026-09-12 Roch Hachana': estJourChome('2026-09-12', true),
      '2026-09-21 Kippour': estJourChome('2026-09-21', true),
      '2026-09-27 Souccot 2 diaspora': estJourChome('2026-09-27', false),
      '2026-09-27 Souccot 2 en Israël (faux)': !estJourChome('2026-09-27', true),
      '2026-09-28 Hol Hamoed (faux)': !estJourChome('2026-09-28', false),
      '2026-10-03 Chemini Atseret': estJourChome('2026-10-03', true),
      '2027-04-22 Pessa’h': estJourChome('2027-04-22', true),
      '2027-06-11 Chavouot': estJourChome('2027-06-11', true),
    };
  }, `${base}js/calendrier.js`);
  for (const [cas, ok] of Object.entries(verdicts)) if (!ok) erreurs.push(`calendrier ${moteur} : ${cas}`);
  await page.close();
}

await navigateur.close();
serveur.close();
if (erreurs.length) {
  console.error(erreurs.join('\n'));
  process.exit(1);
}
console.log(`Captures ${moteur} dans ${SORTIE}`);
