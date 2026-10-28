// Le relais du suivi de vol (tools/relais_vol/worker.js), contre un faux OpenSky :
// la clé reste dans le relais, seul l'avion de l'indicatif demandé revient.
import { test } from 'node:test';
import assert from 'node:assert/strict';
import { traiter as traiterAvecCache, carres, caler, memeIndicatif, JETON, ETATS, DUREE_CACHE } from '../../tools/relais_vol/worker.js';

// Chaque test a son propre cache, vide : les réponses d'un test ne servent pas au suivant.
const traiter = (requete, env, chercher) => traiterAvecCache(requete, env, chercher, new Map());
const ENV = { OPENSKY_CLIENT_ID: 'id', OPENSKY_CLIENT_SECRET: 'secret', ORIGINE_WEB: 'https://exemple.org' };
const ETAT = (indicatif, lat, lon, t, sol = false) => ['abc123', indicatif, 'Israel', t, t, lon, lat, 11000, sol, 250, 270, 0, null, 11200, '1234', false, 0];

function fauxOpenSky(etats, journal) {
  return async (url, options = {}) => {
    journal.push({ url: String(url), options });
    if (String(url) === JETON) return new Response(JSON.stringify({ access_token: 'jeton', expires_in: 1800 }), { status: 200 });
    if (String(url).startsWith(ETATS)) return new Response(JSON.stringify({ time: 0, states: etats }), { status: 200 });
    return new Response('', { status: 404 });
  };
}

test('le relais rend la position de l’indicatif demandé, et lui seul', async () => {
  const journal = [];
  const t = Math.floor(Date.parse('2026-11-10T18:30:00Z') / 1000);
  const chercher = fauxOpenSky([ETAT('AFR12   ', 51, -20, t), ETAT('ELY001  ', 50.5, -21.2, t)], journal);
  const r = await traiter(new Request('https://relais.test/position?indicatif=ely1&lat=50.1&lon=-20.3', { headers: { origin: 'https://exemple.org' } }), ENV, chercher);
  assert.equal(r.status, 200);
  assert.equal(r.headers.get('access-control-allow-origin'), 'https://exemple.org');
  const d = await r.json();
  assert.deepEqual([d.trouve, d.latitude, d.longitude, d.altitude_geo, d.instant], [true, 50.5, -21.2, 11200, '2026-11-10T18:30:00.000Z']);
  // La clé part vers le serveur de jetons d'OpenSky, jamais ailleurs ; le carré est de ±5°, calé sur la grille de 0,5°.
  assert.ok(journal.every((x) => x.url === JETON || x.url.startsWith(ETATS)));
  assert.match(journal.find((x) => x.url.startsWith(ETATS)).url, /lamin=45&lomin=-25\.5&lamax=55&lomax=-15\.5/);
  assert.equal(journal.find((x) => x.url.startsWith(ETATS)).options.headers.authorization, 'Bearer jeton');
});

test('introuvable, au sol, paramètres invalides, origine inconnue, OpenSky en panne', async () => {
  const t = 1_800_000_000;
  const absent = await traiter(new Request('https://relais.test/position?indicatif=ELY1&lat=50&lon=-20'), ENV, fauxOpenSky([ETAT('ELY1', 50, -20, t, true)], []));
  assert.deepEqual(await absent.json(), { trouve: false });
  assert.equal(absent.headers.get('access-control-allow-origin'), null);
  for (const q of ['indicatif=LY1&lat=1&lon=1', 'indicatif=ELY1&lat=91&lon=1', 'indicatif=ELY1&lat=x&lon=1', 'indicatif=ELY1%3Cscript&lat=1&lon=1']) {
    assert.equal((await traiter(new Request(`https://relais.test/position?${q}`), ENV, fauxOpenSky([], []))).status, 400, q);
  }
  assert.equal((await traiter(new Request('https://relais.test/autre'), ENV, fauxOpenSky([], []))).status, 404);
  const panne = async (url) => (String(url) === JETON ? new Response(JSON.stringify({ access_token: 'j' }), { status: 200 }) : new Response('', { status: 429 }));
  assert.equal((await traiter(new Request('https://relais.test/position?indicatif=ELY1&lat=50&lon=-20'), ENV, panne)).status, 502);
});

test('près du méridien de 180°, deux carrés ; les indicatifs se comparent sans les zéros', () => {
  assert.deepEqual(carres(10, 178), [[5, 173, 15, 180], [5, -180, 15, -177]]);
  assert.deepEqual(carres(10, -178), [[5, 177, 15, 180], [5, -180, 15, -173]]);
  assert.equal(carres(10, 0).length, 1);
  assert.ok(memeIndicatif('ELY001', 'ely1') && memeIndicatif('QFA93 ', 'QFA093'));
  assert.ok(!memeIndicatif('ELY1', 'ELY11') && !memeIndicatif('ELY1A', 'ELY1'));
});

test('sans client API, le relais passe par l’accès anonyme et gratuit, sans jeton', async () => {
  const journal = [];
  const t = 1_800_000_000;
  const r = await traiter(new Request('https://relais.test/position?indicatif=ELY1&lat=50&lon=-20'), { ORIGINE_WEB: 'https://exemple.org' },
    fauxOpenSky([ETAT('ELY1', 50.2, -20.1, t)], journal));
  assert.equal((await r.json()).trouve, true);
  assert.ok(journal.every((x) => x.url.startsWith(ETATS) && !x.options.headers));
});

test('deux appareils de la même zone partagent une seule demande à OpenSky, gardée 15 s au plus', async () => {
  const journal = [];
  const t = 1_800_000_000;
  const cache = new Map();
  const chercher = fauxOpenSky([ETAT('ELY1', 50.2, -20.1, t), ETAT('AFR12', 51, -21, t)], journal);
  const demande = (q, instant) => traiterAvecCache(new Request(`https://relais.test/position?${q}`), ENV, chercher, cache, instant);
  const t0 = 1_000_000;
  assert.equal((await (await demande('indicatif=ELY1&lat=50.1&lon=-20.2', t0)).json()).trouve, true);
  assert.equal((await (await demande('indicatif=AFR12&lat=49.9&lon=-20.1', t0 + 5000)).json()).trouve, true);
  assert.equal(journal.filter((x) => x.url.startsWith(ETATS)).length, 1);
  // Passé le délai, la réponse est effacée et redemandée : pas d'archive.
  await demande('indicatif=ELY1&lat=50.1&lon=-20.2', t0 + DUREE_CACHE + 1);
  assert.equal(journal.filter((x) => x.url.startsWith(ETATS)).length, 2);
  assert.equal(cache.size, 1);
  assert.deepEqual([caler(50.26), caler(-20.24), caler(179.9)], [50.5, -20, 180]);
});
