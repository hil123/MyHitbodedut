// Le site dans le vrai Safari d'un Mac, piloté par safaridriver (WebDriver),
// sans dépendance. Lancé par la CI macOS (.github/workflows/web-mac.yml).
//
//   sudo safaridriver --enable && safaridriver -p 4444 &
//   node web/test/safari.mjs [dossier de sortie]
//
// Vérifie sous Safari ce que les captures WebKit de Linux vérifient : la
// page s'affiche dans les trois langues, le sens de lecture suit la langue,
// aucune ressource ne vient d'ailleurs que du site, le calendrier hébraïque
// de Safari donne les bons jours chômés. Puis essaie de lire un morceau.

import { mkdirSync, writeFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { demarreServeur } from '../tools/serveur.mjs';

const ICI = dirname(fileURLToPath(import.meta.url));
const SORTIE = process.argv[2] || join(ICI, '..', 'captures');
mkdirSync(SORTIE, { recursive: true });
const WD = process.env.WEBDRIVER || 'http://127.0.0.1:4444';

const serveur = await demarreServeur();
const base = `http://127.0.0.1:${serveur.address().port}/`;
const erreurs = [];
const rapport = [];

async function wd(methode, chemin, corps) {
  const r = await fetch(WD + chemin, {
    method: methode,
    headers: { 'Content-Type': 'application/json' },
    body: corps === undefined ? undefined : JSON.stringify(corps),
  });
  const json = await r.json();
  if (!r.ok) throw new Error(`${methode} ${chemin} : ${JSON.stringify(json.value)}`);
  return json.value;
}

const session = await wd('POST', '/session', { capabilities: { alwaysMatch: { browserName: 'safari' } } });
const id = session.sessionId;
const S = `/session/${id}`;
rapport.push(`Safari ${session.capabilities.browserVersion} (${session.capabilities.platformName})`);
const executer = (script, args = []) => wd('POST', `${S}/execute/sync`, { script, args });
const executerAsync = (script, args = []) => wd('POST', `${S}/execute/async`, { script, args });
const attendre = (ms) => new Promise((r) => setTimeout(r, ms));

try {
  await wd('POST', `${S}/window/rect`, { width: 430, height: 932 });
  // Deux séances faites : hier et avant-hier.
  const iso = (d) => new Date(Date.now() - d * 86400000).toISOString().slice(0, 10);
  const journal = JSON.stringify({ days: { [iso(1)]: { minutes: 20, note: '' }, [iso(2)]: { minutes: 60, note: '' } } });

  const cas = [
    ['accueil-fr', { langue_app: 'fr' }, ''],
    ['accueil-he', { langue_app: 'he', first_name: 'אברהם' }, ''],
    ['accueil-en-sombre', { langue_app: 'en', apparence_mode: 'sombre' }, ''],
    ['musique-he', { langue_app: 'he' }, '#musique'],
  ];
  for (const [nom, prefs, ecran] of cas) {
    await wd('POST', `${S}/url`, { url: `${base}version.txt` });
    await executer('localStorage.clear(); localStorage.setItem("myhitbodedut", arguments[0]); localStorage.setItem("myhitbodedut-journal", arguments[1]);',
      [JSON.stringify(prefs), journal]);
    await wd('POST', `${S}/url`, { url: base + ecran });
    await attendre(1500);
    const etat = await executer(`return {
      rendu: document.querySelectorAll('#app *').length,
      dir: document.documentElement.dir,
      lang: document.documentElement.lang,
      largeur: document.documentElement.scrollWidth > window.innerWidth,
      horsSite: performance.getEntriesByType('resource').map((e) => e.name).filter((u) => !u.startsWith(location.origin)),
      titre: (document.querySelector('.statut, .barre h1') || {}).textContent,
    }`);
    const png = await wd('GET', `${S}/screenshot`);
    writeFileSync(join(SORTIE, `safari-${nom}.png`), Buffer.from(png, 'base64'));
    rapport.push(`${nom} : ${etat.rendu} éléments, lang=${etat.lang}, dir=${etat.dir}, « ${etat.titre} »`);
    if (etat.rendu < 10) erreurs.push(`${nom} : la page ne s'est pas affichée`);
    if (etat.dir !== (prefs.langue_app === 'he' ? 'rtl' : 'ltr')) erreurs.push(`${nom} : sens de lecture ${etat.dir}`);
    if (etat.largeur) erreurs.push(`${nom} : la page déborde en largeur`);
    if (etat.horsSite.length) erreurs.push(`${nom} : requêtes hors du site ${etat.horsSite.join(', ')}`);
  }

  // Le calendrier hébraïque de Safari lui-même.
  const cal = await executerAsync(`const fin = arguments[arguments.length - 1];
    import(arguments[0]).then(({ estJourChome: c }) => fin({
      'Roch Hachana 2026-09-12': c('2026-09-12', true),
      'Kippour 2026-09-21': c('2026-09-21', true),
      'Souccot 2 diaspora 2026-09-27': c('2026-09-27', false),
      'pas Souccot 2 en Israël': !c('2026-09-27', true),
      'Hol Hamoed ordinaire 2026-09-28': !c('2026-09-28', false),
      'Pessa’h 2027-04-22': c('2027-04-22', true),
      'Chavouot 2027-06-11': c('2027-06-11', true),
    })).catch((e) => fin({ erreur: String(e) }));`, [`${base}js/calendrier.js`]);
  for (const [k, v] of Object.entries(cal)) if (v !== true) erreurs.push(`calendrier Safari : ${k} ${v}`);
  rapport.push(`Calendrier hébraïque de Safari : ${Object.values(cal).every((v) => v === true) ? 'juste' : 'FAUX'}`);

  // La musique : le fichier se charge-t-il, et Safari accepte-t-il de le jouer ?
  const son = await executerAsync(`const fin = arguments[arguments.length - 1];
    const a = new Audio(arguments[0]);
    a.preload = 'auto';
    a.addEventListener('loadedmetadata', () => a.play()
      .then(() => setTimeout(() => fin({ duree: a.duration, joue: !a.paused, position: a.currentTime }), 2000))
      .catch((e) => fin({ duree: a.duration, joue: false, refus: e.name })), { once: true });
    a.addEventListener('error', () => fin({ erreur: a.error && a.error.code }), { once: true });
    setTimeout(() => fin({ delai: true }), 20000);`, [`${base}musique/niggoun_01.mp3`]);
  rapport.push(`Musique (niggoun_01) : ${JSON.stringify(son)}`);
  if (!(son.duree > 0)) erreurs.push(`musique : le morceau ne se charge pas ${JSON.stringify(son)}`);
} finally {
  await wd('DELETE', S).catch(() => {});
  serveur.close();
}

console.log(rapport.join('\n'));
writeFileSync(join(SORTIE, 'safari-rapport.txt'), `${rapport.join('\n')}\n${erreurs.length ? `\nERREURS\n${erreurs.join('\n')}\n` : ''}`);
if (erreurs.length) {
  console.error(erreurs.join('\n'));
  process.exit(1);
}
