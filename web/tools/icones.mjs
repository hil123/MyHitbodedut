// Les icônes de l'écran d'accueil, en PNG : iOS n'accepte pas d'autre format
// pour apple-touch-icon. Même composition que l'icône adaptative Android
// (mipmap-anydpi/ic_launcher_*.xml) : le visage sur la menthe, jamais sur du
// vert foncé (la frange blanche du détourage s'y verrait, voir ce fichier).
//
// À relancer seulement si l'icône Android change :
//   node web/tools/icones.mjs
// Il faut Playwright et Chromium ; les PNG produits sont versionnés dans
// web/src/icones, la construction du site n'en a pas besoin.

import { readFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { createRequire } from 'node:module';

const ICI = dirname(fileURLToPath(import.meta.url));
const RES = join(ICI, '..', '..', 'android', 'app', 'src', 'main', 'res');
const SORTIE = join(ICI, '..', 'src', 'icones');

const require = createRequire(import.meta.url);
const { chromium } = require(process.env.PLAYWRIGHT_MODULE || 'playwright');

const couleurs = readFileSync(join(RES, 'values', 'colors.xml'), 'utf8');
const fond = (nom) => couleurs.match(new RegExp(`<color name="${nom}">(#[0-9A-Fa-f]{6})<`))[1];
const visages = {
  m: readFileSync(join(RES, 'drawable-nodpi', 'ic_launcher_face.webp')).toString('base64'),
  f: readFileSync(join(RES, 'drawable-nodpi', 'ic_launcher_face_f.webp')).toString('base64'),
};
const ACCENTS = ['vert', 'bleu', 'violet', 'bordeaux', 'ambre', 'turquoise'];

// Le manifeste garde le hassid sur le vert ; apple-touch-icon suit les réglages (29/10) :
// une icône par combinaison צדיק / צדקת × couleur des boutons, comme IconeLanceur.
const icones = [['apple-touch-icon.png', 180, 'm', 'vert'], ['icon-192.png', 192, 'm', 'vert'], ['icon-512.png', 512, 'm', 'vert'],
  ...['m', 'f'].flatMap((g) => ACCENTS.map((a) => [`apple-touch-icon-${g}-${a}.png`, 180, g, a]))];

const navigateur = await chromium.launch({ executablePath: process.env.CHROMIUM_PATH });
for (const [nom, cote, genre, accent] of icones) {
  const menthe = fond(`icone_fond_${accent}`);
  const visage = visages[genre];
  const page = await navigateur.newPage({ viewport: { width: cote, height: cote } });
  await page.setContent(`<html><body style="margin:0;background:${menthe}">
    <img src="data:image/webp;base64,${visage}" style="width:${cote}px;height:${cote}px;display:block;object-fit:contain"></body></html>`);
  await page.waitForFunction(() => document.images[0].complete);
  await page.screenshot({ path: join(SORTIE, nom), omitBackground: false });
  await page.close();
  console.log(`${nom} (${cote}×${cote})`);
}
await navigateur.close();
