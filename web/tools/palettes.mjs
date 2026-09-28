// Les couleurs d'Android (colors.xml, palettes.xml, jour et nuit) en
// variables CSS. Les combinaisons ont été vérifiées par PaletteTest côté
// Android : le site reprend exactement les mêmes valeurs.
//
// Ordre des règles, du plus faible au plus fort :
//   vert de jour < vert de nuit < fond de jour < fond de nuit
//   < accent de jour < accent de nuit.
// Un fond ou un accent ne recouvre que ses propres jetons ; sa version de
// nuit porte un sélecteur plus précis que sa version de jour.

const JETON = {
  mhBrand: 'brand', mhBrandDark: 'brand-dark', mhMint: 'mint', mhOnBrand: 'on-brand',
  mhOnPlate: 'on-plate', mhStageSoft: 'stage-soft', mhDayDone: 'day-done',
  mhPaper: 'paper', mhCard: 'card', mhLine: 'line', mhInk: 'ink', mhMuted: 'muted', mhDayTodo: 'day-todo',
};

function couleurs(xml) {
  const out = {};
  for (const m of xml.replace(/<!--[\s\S]*?-->/g, '').matchAll(/<color name="([a-z_]+)">(#[0-9A-Fa-f]{6,8})<\/color>/g)) {
    out[m[1].replace(/_/g, '-')] = m[2];
  }
  return out;
}

function styles(xml) {
  const out = {};
  for (const m of xml.replace(/<!--[\s\S]*?-->/g, '').matchAll(/<style name="(Fond|Accent)\.(\w+)"[^>]*>([\s\S]*?)<\/style>/g)) {
    const jetons = {};
    for (const it of m[3].matchAll(/<item name="(mh\w+)">(#[0-9A-Fa-f]{6})<\/item>/g)) {
      jetons[JETON[it[1]]] = it[2];
    }
    out[`${m[1].toLowerCase()}:${m[2].toLowerCase()}`] = jetons;
  }
  return out;
}

/** #AARRGGBB d'Android → rgba() ; #RRGGBB tel quel. */
function css(valeur) {
  if (valeur.length !== 9) return valeur;
  const a = parseInt(valeur.slice(1, 3), 16) / 255;
  const [r, g, b] = [3, 5, 7].map((i) => parseInt(valeur.slice(i, i + 2), 16));
  return `rgba(${r}, ${g}, ${b}, ${a.toFixed(3)})`;
}

const bloc = (selecteur, jetons) =>
  `${selecteur} {\n${Object.entries(jetons).map(([k, v]) => `  --${k}: ${css(v)};`).join('\n')}\n}\n`;

export function palettesCss({ colors, colorsNuit, palettes, palettesNuit }) {
  const jour = couleurs(colors);
  const nuit = couleurs(colorsNuit);
  const pj = styles(palettes);
  const pn = styles(palettesNuit);
  let sortie = '/* Construit par web/tools/palettes.mjs depuis les ressources Android. Ne pas modifier. */\n';
  sortie += bloc(':root', jour);
  // La nuit selon le téléphone (mode « auto ») ou forcée (mode « sombre »).
  const deNuit = (sel, jetons) =>
    `@media (prefers-color-scheme: dark) {\n${bloc(`:root[data-mode="auto"]${sel}`, jetons)}}\n` +
    bloc(`:root[data-mode="sombre"]${sel}`, jetons);
  sortie += deNuit('', nuit);
  for (const genre of ['fond', 'accent']) {
    for (const cle of Object.keys(pj).filter((k) => k.startsWith(`${genre}:`))) {
      const nom = cle.split(':')[1];
      const sel = `[data-${genre}="${nom}"]`;
      sortie += bloc(`:root${sel}`, pj[cle]);
      if (!pn[cle]) throw new Error(`Palette de nuit absente : ${cle}`);
      sortie += deNuit(sel, pn[cle]);
    }
  }
  return sortie;
}
