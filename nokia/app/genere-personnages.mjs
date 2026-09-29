// Convertit les illustrations Android (drawable-nodpi/char_*.webp) en PNG
// de 80 px pour l'application Java : un téléphone Java ME ne lit que le
// PNG. Les PNG produits sont gardés dans le dépôt (nokia/app/res/p) : la
// construction du .jar n'a pas besoin de navigateur.
//
//   node nokia/app/genere-personnages.mjs   (Playwright et Chromium requis)

import { readFileSync } from 'node:fs';
import { createRequire } from 'node:module';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

export const PERSONNAGES = [
  'char_welcome', 'char_dancing_joy', 'char_praying', 'char_thumbs_up', 'char_writing', 'char_pointing',
  'char_open_arms', 'char_sitting_low', 'char_walking', 'char_praise', 'char_standing_back',
];
export const TAILLE = 80;

const ICI = dirname(fileURLToPath(import.meta.url));
const SRC = join(ICI, '..', '..', 'android', 'app', 'src', 'main', 'res', 'drawable-nodpi');

if (process.argv[1] === fileURLToPath(import.meta.url)) {
  const pw = createRequire(import.meta.url)(process.env.PLAYWRIGHT_MODULE || 'playwright');
  const b = await pw.chromium.launch(process.env.CHROMIUM_PATH ? { executablePath: process.env.CHROMIUM_PATH } : {});
  const p = await b.newPage({ viewport: { width: TAILLE, height: TAILLE } });
  for (const nom of PERSONNAGES) {
    const src = `data:image/webp;base64,${readFileSync(join(SRC, `${nom}.webp`)).toString('base64')}`;
    await p.setContent(`<body style="margin:0;background:transparent"><img src="${src}" style="display:block;width:${TAILLE}px;height:${TAILLE}px;object-fit:contain"></body>`);
    await p.screenshot({ path: join(ICI, 'res', 'p', `${nom.slice(5)}.png`), omitBackground: true });
  }
  await b.close();
  console.log(`${PERSONNAGES.length} personnages en ${TAILLE} px`);
}
