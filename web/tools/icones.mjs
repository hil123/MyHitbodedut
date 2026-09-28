// Les icônes de l'écran d'accueil, en PNG : iOS n'accepte pas d'autre format
// pour apple-touch-icon. Même composition que l'icône adaptative Android
// (mipmap-anydpi/ic_launcher.xml) : le visage sur la menthe, jamais sur du
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

const menthe = readFileSync(join(RES, 'values', 'colors.xml'), 'utf8').match(/<color name="mint">(#[0-9A-Fa-f]{6})</)[1];
const visage = readFileSync(join(RES, 'drawable-nodpi', 'ic_launcher_face.webp')).toString('base64');

const navigateur = await chromium.launch({ executablePath: process.env.CHROMIUM_PATH });
for (const [nom, cote] of [['apple-touch-icon.png', 180], ['icon-192.png', 192], ['icon-512.png', 512]]) {
  const page = await navigateur.newPage({ viewport: { width: cote, height: cote } });
  await page.setContent(`<html><body style="margin:0;background:${menthe}">
    <img src="data:image/webp;base64,${visage}" style="width:${cote}px;height:${cote}px;display:block;object-fit:contain"></body></html>`);
  await page.waitForFunction(() => document.images[0].complete);
  await page.screenshot({ path: join(SORTIE, nom), omitBackground: false });
  await page.close();
  console.log(`${nom} (${cote}×${cote})`);
}
await navigateur.close();
