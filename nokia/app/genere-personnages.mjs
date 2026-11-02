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
  'char_open_arms', 'char_walking', 'char_praise', 'char_standing_back', 'char_guitar',
];
// Le personnage au féminin, pour qui a choisi צדקת : la même table que
// Personnage.kt et web/src/js/personnage.js, gardée en res/p/f_<pose du hassid>.png.
export const FEMININ = {
  char_welcome: 'char_f_welcome', char_dancing_joy: 'char_f_joy', char_praying: 'char_f_praying',
  char_thumbs_up: 'char_f_thumbs_up', char_writing: 'char_f_writing', char_pointing: 'char_f_pointing',
  char_open_arms: 'char_f_arms_up', char_walking: 'char_f_walking', char_praise: 'char_f_praise',
  char_standing_back: 'char_f_standing_back', char_guitar: 'char_f_guitar',
};
export const TAILLE = 80;

const ICI = dirname(fileURLToPath(import.meta.url));
const SRC = join(ICI, '..', '..', 'android', 'app', 'src', 'main', 'res', 'drawable-nodpi');

if (process.argv[1] === fileURLToPath(import.meta.url)) {
  const pw = createRequire(import.meta.url)(process.env.PLAYWRIGHT_MODULE || 'playwright');
  const b = await pw.chromium.launch(process.env.CHROMIUM_PATH ? { executablePath: process.env.CHROMIUM_PATH } : {});
  const p = await b.newPage({ viewport: { width: TAILLE, height: TAILLE } });
  const travaux = [
    ...PERSONNAGES.map((nom) => [nom, nom.slice(5)]),
    ...PERSONNAGES.map((nom) => [FEMININ[nom], `f_${nom.slice(5)}`]),
  ];
  for (const [source, sortie] of travaux) {
    const src = `data:image/webp;base64,${readFileSync(join(SRC, `${source}.webp`)).toString('base64')}`;
    await p.setContent(`<body style="margin:0;background:transparent"><img src="${src}" style="display:block;width:${TAILLE}px;height:${TAILLE}px;object-fit:contain"></body>`);
    await p.screenshot({ path: join(ICI, 'res', 'p', `${sortie}.png`), omitBackground: true });
  }
  await b.close();
  console.log(`${travaux.length} personnages en ${TAILLE} px (dont ${PERSONNAGES.length} au féminin)`);
}
