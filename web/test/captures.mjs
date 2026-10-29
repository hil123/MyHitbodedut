// Captures d'écran du site construit, dans un navigateur, à la taille d'un
// iPhone. Échoue sur toute erreur de la page (console, exception, requête
// hors du site) : l'application promet que rien ne quitte le téléphone.
//
//   node web/test/captures.mjs [chromium|webkit] [dossier de sortie]
//
// Le site doit être construit (node web/build.mjs).

import { mkdirSync } from 'node:fs';
import { createRequire } from 'node:module';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { demarreServeur } from '../tools/serveur.mjs';

const ICI = dirname(fileURLToPath(import.meta.url));
const moteur = process.argv[2] || 'chromium';
const SORTIE = process.argv[3] || join(ICI, '..', 'captures');
// LARGEUR=320 : le plus petit téléphone courant (iPhone SE 1re génération), 15/10.
const LARGEUR = Number(process.env.LARGEUR) || 390;
const HAUTEUR = LARGEUR < 390 ? 568 : 844;
mkdirSync(SORTIE, { recursive: true });

const require = createRequire(import.meta.url);
const pw = require(process.env.PLAYWRIGHT_MODULE || 'playwright');

const serveur = await demarreServeur();
const base = `http://127.0.0.1:${serveur.address().port}/`;

const options = moteur === 'chromium' && process.env.CHROMIUM_PATH ? { executablePath: process.env.CHROMIUM_PATH } : {};
const navigateur = await pw[moteur].launch(options);
const erreurs = [];

// Une semaine connue : deux séances faites, dimanche 27 et lundi 28/09/2026.
const DONNEES = {
  'myhitbodedut-journal': JSON.stringify({ days: { '2026-09-27': { minutes: 20, note: '' }, '2026-09-28': { minutes: 60, note: '' } } }),
};
const MAINTENANT = new Date('2026-09-29T09:00:00').getTime();

async function capture(nom, { langue, mode = 'clair', fond = 'vert', accent = 'vert', prenom = '', ecran = '', onglet = null, adresse = null, actions = null, calme = false, extra = {} }) {
  if (process.env.SEUL && !nom.startsWith(process.env.SEUL)) return;
  const contexte = await navigateur.newContext({
    viewport: { width: LARGEUR, height: HAUTEUR }, deviceScaleFactor: 2, colorScheme: mode === 'sombre' ? 'dark' : 'light',
    serviceWorkers: 'block',
    // « calme » : le téléphone demande moins d'animations. La danse du bravo
    // s'arrête alors, comme sur Android ; sous WebKit, la capture d'un écran
    // qui danse n'aboutit pas.
    reducedMotion: calme ? 'reduce' : 'no-preference',
  });
  const page = await contexte.newPage();
  page.on('console', (m) => { if (m.type() === 'error') erreurs.push(`${nom} : ${m.text()}`); });
  page.on('pageerror', (e) => erreurs.push(`${nom} : ${e.message}`));
  page.on('dialog', (d) => { erreurs.push(`${nom} : boîte « ${d.message()} »`); d.dismiss().catch(() => {}); });
  page.on('request', (r) => { if (!r.url().startsWith(base)) erreurs.push(`${nom} : requête hors du site ${r.url()}`); });
  const prefs = JSON.stringify({
    // La présentation du premier lancement (16/10) est capturée à part ; ailleurs, déjà vue.
    onboarded_version: 2,
    langue_app: langue, apparence_mode: mode, apparence_fond: fond, apparence_accent: accent, first_name: prenom,
    ...(adresse ? { adresse } : {}),
    ...extra,
  });
  await page.addInitScript(({ donnees, prefs, maintenant }) => {
    // Sans l'écran d'ouverture (13/10), qui couvrirait la capture trois secondes.
    try { sessionStorage.setItem('ouverture-vue', '1'); } catch { /* rien */ }
    // L'horloge figée : la même semaine à chaque capture.
    const Vrai = Date;
    const decalage = maintenant - Vrai.now();
    globalThis.Date = class extends Vrai {
      constructor(...a) { super(...(a.length ? a : [Vrai.now() + decalage])); }
      static now() { return Vrai.now() + decalage; }
    };
    for (const [k, v] of Object.entries(donnees)) localStorage.setItem(k, v);
    // Le bilan de la semaine (16/10) déjà vu : il s'ouvrirait par-dessus l'accueil.
    const d = new Date(); d.setDate(d.getDate() - d.getDay() - 7);
    const vu = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
    localStorage.setItem('myhitbodedut', JSON.stringify({ bilan_vu: vu, ...JSON.parse(prefs) }));
  }, { donnees: DONNEES, prefs, maintenant: MAINTENANT });
  if (moteur === 'webkit') {
    // WebKit sous Linux lit l'audio par GStreamer, que ni Safari ni l'iPhone
    // n'emploient, et la page s'y fige par moments à l'arrêt d'un morceau.
    // Le son est donc remplacé ici par une doublure qui émet les mêmes
    // événements qu'une lecture (play, timeupdate, pause). Le chargement réel
    // d'un morceau est vérifié dans Safari sur Mac (web-mac.yml) ; la lecture
    // elle-même, sur l'iPhone. Le volume est en lecture seule, comme sur iPhone.
    await page.addInitScript(() => {
      const proto = HTMLMediaElement.prototype;
      Object.defineProperty(proto, 'volume', { get: () => 1, set: () => {}, configurable: true });
      Object.defineProperty(proto, 'paused', { get() { return this.__joue !== true; }, configurable: true });
      proto.play = function play() {
        this.__joue = true;
        clearInterval(this.__horloge);
        this.__horloge = setInterval(() => this.dispatchEvent(new Event('timeupdate')), 250);
        this.dispatchEvent(new Event('play'));
        return Promise.resolve();
      };
      proto.pause = function pause() {
        if (this.__joue !== true) return;
        this.__joue = false;
        clearInterval(this.__horloge);
        this.dispatchEvent(new Event('pause'));
      };
      proto.load = function load() {};
    });
  }
  await page.goto(base + (ecran ? `#${ecran}` : ''));
  await page.waitForSelector('#app > *', { timeout: 8000 }).catch((e) => { console.error(erreurs.join('\n')); throw e; });
  if (onglet) await page.click(`[data-onglet="${onglet}"]`);
  if (actions) await actions(page);
  await page.waitForTimeout(900);
  await page.screenshot({ path: join(SORTIE, `${moteur}-${nom}.png`), animations: 'disabled', timeout: 15000 })
    .catch(async (e) => {
      const etat = await Promise.race([
        page.evaluate(() => ({ hash: location.hash, ecran: document.getElementById('app').dataset.ecran })),
        new Promise((r) => setTimeout(() => r('la page ne répond plus'), 3000)),
      ]);
      console.error(`Capture impossible (${nom}) :`, JSON.stringify(etat), '\n', erreurs.join('\n'));
      throw e;
    });
  // Aucun débordement horizontal, en aucune langue.
  const deborde = await page.evaluate(() => document.documentElement.scrollWidth > window.innerWidth);
  if (deborde) erreurs.push(`${nom} : la page déborde en largeur`);
  // Ni élément qui sort de l'écran, ni texte rogné dans sa boîte (15/10).
  const sortis = await page.evaluate(() => {
    const l = window.innerWidth;
    const fautes = [];
    for (const e of document.querySelectorAll('#app *')) {
      const r = e.getBoundingClientRect();
      if (!r.width || !r.height || getComputedStyle(e).visibility === 'hidden') continue;
      // Une rangée qui défile de côté (les pastilles de l'accueil) déborde par construction.
      let defile = false;
      for (let a = e.parentElement; a && !defile; a = a.parentElement) defile = ['auto', 'scroll'].includes(getComputedStyle(a).overflowX);
      if (defile) continue;
      const style = getComputedStyle(e);
      const texte = [...e.childNodes].some((n) => n.nodeType === 3 && n.textContent.trim());
      if (r.right > l + 1 || r.left < -1) fautes.push(`sort de l'écran : <${e.tagName.toLowerCase()} class="${e.className}"> ${Math.round(r.left)}–${Math.round(r.right)}`);
      else if (texte && style.overflowX !== 'auto' && style.overflowX !== 'scroll' && e.scrollWidth > e.clientWidth + 1 && style.textOverflow !== 'ellipsis')
        fautes.push(`texte rogné : <${e.tagName.toLowerCase()} class="${e.className}"> « ${e.textContent.trim().slice(0, 30)} »`);
    }
    return [...new Set(fautes)].slice(0, 8);
  });
  for (const f of sortis) erreurs.push(`${nom} (${LARGEUR}px) : ${f}`);
  // L'accueil, sans défiler : « Commencer » entier au-dessus des onglets (comme sur Android).
  if (nom.startsWith('accueil') && !actions) {
    const cache = await page.evaluate(() => {
      const b = document.querySelector('.accueil-actions .principal')?.getBoundingClientRect();
      const o = document.querySelector('nav.onglets')?.getBoundingClientRect();
      return !b || b.bottom > (o ? o.top : window.innerHeight) - 2;
    });
    if (cache) erreurs.push(`${nom} (${LARGEUR}px) : « Commencer » caché sous les onglets`);
  }
  await contexte.close();
}

await capture('accueil-fr', { langue: 'fr' });
// La présentation du premier lancement (16/10) : le premier écran, puis celui de la durée.
await capture('presentation-fr', { langue: 'fr', extra: { onboarded_version: 0 } });
await capture('presentation-he-duree', { langue: 'he', extra: { onboarded_version: 0 }, actions: (p) => p.click('.presentation .principal') });
await capture('accueil-he', { langue: 'he', prenom: 'אברהם' });
await capture('accueil-en-sombre', { langue: 'en', mode: 'sombre' });
// Un jour fait touché : sa durée à la place du ✓ (14/10).
// Un jour fait touché ouvre sa feuille (16/10) ; « Montrer la durée » la met sur la bande.
await capture('accueil-fr-durees', { langue: 'fr', actions: async (p) => { const n = await p.locator('button.jour.fait').count(); for (let i = 0; i < n; i++) { await p.locator('button.jour.fait').nth(i).click(); await p.locator('.feuille .actions button').first().click(); } } });
// Un jour passé sans rien : la feuille pour y ajouter du temps fait hors du téléphone (16/10).
await capture('accueil-fr-ajout-temps', { langue: 'fr', actions: (p) => p.locator('button.jour:not(.fait)').first().click() });
await capture('accueil-fr-sable-violet', { langue: 'fr', fond: 'sable', accent: 'violet', prenom: 'Hillel' });
await capture('textes-he', { langue: 'he', onglet: 'textes' });
await capture('musique-fr', { langue: 'fr', ecran: 'musique' });
await capture('reglages-en', { langue: 'en', ecran: 'reglages' });

// Les écrans du 10 au 14/10 : zmanim (allégé), calendrier, halakha (livre au choix),
// hilloulot, livres, siddour, réglages. Lieu : Jérusalem, sans le dialogue.
const JERUSALEM = { lieu_zmanim: { nom: 'ירושלים', latitude: 31.76904, longitude: 35.21633, altitude: 754, fuseau: 'Asia/Jerusalem', ville: 281184 }, in_israel: true };
const bas = (n) => async (p) => { await p.waitForTimeout(1200); await p.evaluate((y) => { const d = document.querySelector('.defile'); if (d) d.scrollTop = y; }, n); };
await capture('zmanim-he', { langue: 'he', ecran: 'zmanim', extra: JERUSALEM, actions: bas(0) });
await capture('zmanim-he-suite', { langue: 'he', ecran: 'zmanim', extra: JERUSALEM, actions: bas(700) });
await capture('zmanim-fr', { langue: 'fr', ecran: 'zmanim', extra: JERUSALEM, actions: bas(0) });
await capture('calendrier-he', { langue: 'he', ecran: 'calendrier', extra: JERUSALEM, actions: bas(0) });
await capture('halakha-he', { langue: 'he', ecran: 'halakha', extra: JERUSALEM, actions: bas(0) });
await capture('halakha-he-ben-ish-hai', { langue: 'he', ecran: 'halakha', extra: { ...JERUSALEM, halakha_source: 'ben_ish_hai' }, actions: bas(0) });
await capture('hilloulot-he', { langue: 'he', ecran: 'hilloulot', extra: JERUSALEM, actions: bas(0) });
await capture('livres-he', { langue: 'he', ecran: 'livres', actions: bas(0) });
// Le 28/10 : les marque-pages en une ligne, puis la feuille filtrée par recueil et par texte.
const MARQUES = { marques_pages: [
  { cle: 's:chaharit', nusah: 'edot', section: 2, bloc: 3, apercu: 'אביי הוה מסדר סדר המערכה' },
  { cle: 's:minha', nusah: 'edot', section: 0, bloc: 1, apercu: 'אשרי יושבי ביתך עוד יהללוך' },
  { cle: 'l:likoutei_etsot', nusah: '', section: 1, bloc: 0, apercu: 'עיקר ההתבודדות הוא בלילה' },
  { cle: 's:chaharit', nusah: 'edot', section: 5, bloc: 0, apercu: 'ברוך שאמר והיה העולם' },
  { cle: 's:birkat_hamazon', nusah: 'edot', section: 0, bloc: 0, apercu: 'שיר המעלות בשוב ה׳ את שיבת ציון' },
  { cle: 't:tehilim', nusah: '', section: 121, bloc: 1, apercu: 'קכ״א, א׳ · שיר למעלות אשא עיני אל' },
  { cle: 't:tehilim', nusah: '', section: 23, bloc: 4, apercu: 'כ״ג, ד׳ · גם כי אלך בגיא צלמות' },
] };
const feuilleSignets = (filtre) => async (p) => {
  await p.waitForSelector('.ligne .nom-marque');
  await p.click('.ligne:has(.nom-marque)');
  await p.waitForSelector('.puces-signets');
  if (filtre) await p.evaluate((f) => [...document.querySelectorAll('.puces-signets .puce-choix')].find((b) => b.textContent.includes(f)).click(), filtre);
};
await capture('signets-he', { langue: 'he', onglet: 'textes', extra: MARQUES });
await capture('signets-he-feuille', { langue: 'he', onglet: 'textes', extra: MARQUES, actions: feuilleSignets('') });
await capture('signets-he-livres', { langue: 'he', ecran: 'livres', extra: MARQUES, actions: bas(0) });
await capture('signets-he-siddour', { langue: 'he', ecran: 'siddur', extra: MARQUES, actions: bas(0) });
await capture('signets-he-tehilim', { langue: 'he', ecran: 'tehilim', extra: MARQUES });
await capture('signets-he-psaume', { langue: 'he', ecran: 'tehilim', extra: MARQUES, actions: async (p) => {
  await p.waitForSelector('.ligne .nom-marque');
  await p.click('.ligne:has(.nom-marque)');
  await p.waitForSelector('.signet-choix');
  await p.click('.signet-choix >> nth=0');
  await p.waitForSelector('.verset.marque-page');
  await p.waitForTimeout(300);
} });
await capture('signets-he-chaharit', { langue: 'he', onglet: 'textes', extra: MARQUES, actions: feuilleSignets('שַׁחֲרִית') });
await capture('siddour-he', { langue: 'he', ecran: 'siddur', actions: bas(0) });
await capture('siddour-he-occasions', { langue: 'he', ecran: 'siddur', actions: bas(100000) });
// Le 28/10 : paragraphes aérés (premier mot agrandi), Chéva bérakhot repliées en fin de Birkat
// haMazon, boussole de la tefila.
const ouvrirTexte = (titre, y) => async (p) => {
  // Les titres sont vocalisés : comparés sans voyelles.
  await p.evaluate((t) => [...document.querySelectorAll('.ligne-he')]
    .find((b) => b.querySelector('.nom-he').textContent.replace(/[\u0591-\u05C7]/g, '') .includes(t)).click(), titre);
  await p.waitForSelector('.siddour .priere');
  await p.evaluate((y) => { const d = document.querySelector('.defile'); d.scrollTop = y < 0 ? d.scrollHeight : y; }, y);
};
await capture('priere-aeree-he', { langue: 'he', ecran: 'siddur', actions: ouvrirTexte('תפלת שחרית', 1400) });
await capture('birkat-cheva-he', { langue: 'he', ecran: 'siddur', actions: ouvrirTexte('ברכת המזון', -1) });
await capture('birkat-zimun-fr', { langue: 'fr', ecran: 'siddur', actions: ouvrirTexte('ברכת המזון', 560) });
// Le capteur simulé : quarante lectures au même cap, pour que le lissage s'y pose.
const cap = (nord) => async (p) => {
  await p.waitForSelector('.boussole-scene svg');
  // Là où le navigateur demande l'autorisation (iPhone, Chromium récent), le toucher d'abord.
  // Le harnais répond oui à la place de l'utilisateur.
  if (await p.$('.boussole-activer')) {
    await p.evaluate(() => { DeviceOrientationEvent.requestPermission = async () => 'granted'; });
    await p.click('.boussole-activer');
    await p.waitForSelector('.boussole-activer', { state: 'detached' });
  }
  await p.evaluate((n) => { for (let i = 0; i < 40; i++) window.dispatchEvent(Object.assign(new Event('deviceorientationabsolute'), { alpha: (360 - n) % 360, absolute: true })); }, nord);
  await p.waitForTimeout(700);
};
const PARIS = { lieu_zmanim: { nom: 'Paris', latitude: 48.8566, longitude: 2.3522, altitude: 35, fuseau: 'Europe/Paris', ville: 2988507 }, in_israel: false };
await capture('boussole-fr', { langue: 'fr', ecran: 'boussole', extra: PARIS, actions: cap(80) });
await capture('boussole-fr-aligne', { langue: 'fr', ecran: 'boussole', mode: 'sombre', extra: PARIS, actions: cap(112) });
await capture('boussole-he', { langue: 'he', ecran: 'boussole', extra: JERUSALEM, actions: cap(150) });
await capture('boussole-fr-rose', { langue: 'fr', ecran: 'boussole', extra: { ...PARIS, boussole_rose: true }, actions: cap(80) });
await capture('boussole-fr-rose-aligne', { langue: 'fr', ecran: 'boussole', extra: { ...PARIS, boussole_rose: true }, actions: cap(112) });
await capture('bibliotheque-he', { langue: 'he', ecran: 'bibliotheque', actions: bas(0) });
await capture('reglages-fr-personnalisation', { langue: 'fr', ecran: 'reglages', actions: bas(400) });
await capture('reglages-he', { langue: 'he', ecran: 'reglages', mode: 'sombre', actions: bas(400) });

// La séance : avant, pendant, le bravo, l'abandon.
const finDeSeance = async (page) => {
  // Cinq minutes passent d'un coup : le décompte doit clore la séance seul.
  await page.evaluate(() => {
    const p = JSON.parse(localStorage.getItem('myhitbodedut'));
    p.session_started_at -= 5 * 60000 + 1000;
    p.session_ends_at = Date.now() - 1000;
    localStorage.setItem('myhitbodedut', JSON.stringify(p));
  });
  await page.waitForSelector('.bravo', { timeout: 5000 }).catch(async (e) => {
    const etat = await page.evaluate(() => ({
      hash: location.hash, ecran: document.getElementById('app').dataset.ecran,
      prefs: localStorage.getItem('myhitbodedut'), maintenant: Date.now(),
    }));
    console.error('Bravo absent :', JSON.stringify(etat), '\n', erreurs.join('\n'));
    throw e;
  });
};
await capture('seance-fr', { langue: 'fr', ecran: 'seance' });
await capture('seance-en-cours-he', { langue: 'he', ecran: 'seance', actions: (p) => p.click('.duree') });
// « המילים לא באות לי » et la carte question (28/10).
await capture('seance-question-he', {
  langue: 'he', ecran: 'seance', calme: true,
  actions: async (p) => { await p.click('.duree'); await p.click('.blocage'); await p.click('.question-suivante');
    await p.evaluate(() => document.querySelector('.question-seance')?.scrollIntoView({ block: 'center' })); },
});
await capture('seance-en-cours-en-sombre', { langue: 'en', mode: 'sombre', ecran: 'seance', actions: (p) => p.click('.duree') });
await capture('bravo-fr', {
  langue: 'fr', ecran: 'seance', adresse: 'm', calme: true,
  actions: async (p) => { await p.click('.duree'); await finDeSeance(p); },
});
// La danse elle-même : le danseur bouge, et des notes montent.
await capture('bravo-danse-fr', {
  langue: 'fr', ecran: 'seance',
  actions: async (p) => {
    await p.click('.duree');
    await finDeSeance(p);
    // La danse démarre à la première image d'animation : attendre qu'elle ait posé un mouvement,
    // sinon le premier relevé est vide et le contrôle échoue au hasard (CI du 16/10).
    if (moteur === 'chromium') await p.waitForFunction(() => document.querySelector('.danseur')?.style.transform, null, { timeout: 3000 }).catch(() => {});
    const a = await p.evaluate(() => document.querySelector('.danseur').style.transform);
    await p.waitForTimeout(250);
    const b = await p.evaluate(() => document.querySelector('.danseur').style.transform);
    const notes = await p.evaluate(() => document.querySelectorAll('.note').length);
    // Sous WebKit sans écran, les images d'animation ne sont pas rafraîchies :
    // le mouvement ne se vérifie que sous Chromium.
    if (moteur === 'chromium' && (!a || a === b)) erreurs.push(`la danse ne bouge pas : « ${a} » puis « ${b} »`);
    if (notes === 0) erreurs.push('aucune note ne monte de la scène');
    // Pour la capture, la danse s'arrête sur une pose.
    await p.emulateMedia({ reducedMotion: 'reduce' });
    await p.evaluate(() => window.dispatchEvent(new Event('popstate')));
  },
});
await capture('hazak-he', {
  langue: 'he', ecran: 'seance', adresse: 'm', calme: true,
  actions: async (p) => {
    await p.click('.duree');
    await p.click('.seance .principal + .texte-clair');
    await p.click('.feuille .actions button:last-child');
    await p.waitForSelector('.bravo');
  },
});
// Le journal a reçu la séance, et l'accueil la montre.
await capture('accueil-apres-seance-fr', {
  langue: 'fr', ecran: 'seance', calme: true,
  actions: async (p) => {
    await p.click('.duree');
    await finDeSeance(p);
    await p.click('.bravo .principal');
    await p.click('.retour');
    const jour = await p.evaluate(() => JSON.parse(localStorage.getItem('myhitbodedut-journal')).days['2026-09-29']);
    if (!jour || jour.minutes !== 5) erreurs.push(`séance non inscrite : ${JSON.stringify(jour)}`);
  },
});

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
