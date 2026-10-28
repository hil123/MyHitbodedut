// Les zmanim en vol (vol.js, 28/10) : les mêmes cas que VolTest.kt, et la
// parité avec Android par web/test/vecteurs_vol.json (tools/vecteurs_vol),
// produit par Vol.kt sur KosherJava : aucune valeur n'y est tapée à la main.
import { test } from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import * as Vol from '../src/js/vol.js';
import { jour } from '../src/js/zmanim.js';

const liste = Vol.aeroports(JSON.parse(readFileSync(new URL('../../android/app/src/main/res/raw/aeroports.json', import.meta.url), 'utf8')));
const a = (iata) => liste.find((x) => x.iata === iata) ?? assert.fail(`${iata} absent`);
const plan = (de, vers, decollage, atterrissage) => ({
  numero: 'TEST', depart: a(de), arrivee: a(vers), decollage: Vol.instant(decollage, a(de)), atterrissage: Vol.instant(atterrissage, a(vers)),
});
const H = 3600e3;
const MIN = 60e3;
const versOuest = plan('TLV', 'JFK', '2026-11-10T12:30', '2026-11-10T17:45');
const versEst = plan('JFK', 'TLV', '2026-11-10T23:00', '2026-11-11T16:40');
const sol = (iata, iso, cle) => {
  const x = a(iata);
  return jour({ nom: x.ville, latitude: x.latitude, longitude: x.longitude, altitude: 0, fuseau: x.fuseau }, iso).find((z) => z.cle === cle).instant;
};

test('les aéroports sont complets ; la recherche trouve code, ville sans accents, nom', () => {
  assert.ok(liste.length > 3000);
  assert.equal(new Set(liste.map((x) => x.iata)).size, liste.length);
  for (const x of liste) assert.ok(Vol.fuseauConnu(x.fuseau), `${x.iata} ${x.fuseau}`);
  assert.equal(a('OOL').fuseau, 'Australia/Brisbane');
  assert.equal(liste.find((x) => x.iata === 'URC'), undefined);
  assert.equal(Vol.chercher(liste, 'tlv')[0].iata, 'TLV');
  assert.ok(Vol.chercher(liste, 'zurich').some((x) => x.iata === 'ZRH'));
  assert.ok(Vol.chercher(liste, 'Ben Gurion').some((x) => x.iata === 'TLV'));
  assert.deepEqual(Vol.chercher(liste, '  '), []);
});

test('les heures du billet se lisent à l’heure de chaque aéroport, trous d’heure d’été compris', () => {
  assert.equal(Vol.instant('2026-07-01T10:00', a('CDG')).toISOString(), '2026-07-01T08:00:00.000Z');
  assert.equal(Vol.instant('2026-01-01T10:00', a('JFK')).toISOString(), '2026-01-01T15:00:00.000Z');
  // Paris, 29/03/2026 : 02:30 n'existe pas ; comme LocalDateTime.atZone, l'heure avance (03:30 CEST).
  assert.equal(Vol.instant('2026-03-29T02:30', a('CDG')).toISOString(), '2026-03-29T01:30:00.000Z');
  // Paris, 25/10/2026 : 02:30 existe deux fois ; le premier (CEST), comme Java.
  assert.equal(Vol.instant('2026-10-25T02:30', a('CDG')).toISOString(), '2026-10-25T00:30:00.000Z');
  assert.equal(Vol.instant('2026-01-01T10:00', { ...a('JFK'), fuseau: 'Mars/Olympus' }), null);
});

test('une saisie impossible est refusée', () => {
  assert.equal(Vol.verifier(versOuest), null);
  assert.equal(Vol.verifier({ ...versOuest, arrivee: versOuest.depart }), 'meme_aeroport');
  assert.equal(Vol.verifier({ ...versOuest, atterrissage: versOuest.decollage }), 'arrivee_avant_depart');
  assert.equal(Vol.verifier({ ...versOuest, atterrissage: new Date(+versOuest.decollage + 23 * H) }), 'trop_long');
  assert.equal(Vol.verifier({ ...versOuest, arrivee: { ...a('JFK'), fuseau: 'Mars/Olympus' } }), 'fuseau_inconnu');
});

test('le fuseau nautique suit la longitude, signe inversé', () => {
  assert.equal(Vol.fuseauNautique(35), 'Etc/GMT-2');
  assert.equal(Vol.fuseauNautique(-74), 'Etc/GMT+5');
  assert.equal(Vol.fuseauNautique(3), 'Etc/GMT');
  assert.equal(Vol.fuseauNautique(179.9), 'Etc/GMT-12');
  assert.equal(Vol.fuseauNautique(-179.9), 'Etc/GMT+12');
});

test('au sol, le calcul rend les zmanim du moteur ; en vol, le zman du point sous l’avion', () => {
  const c = Vol.calculer(versOuest);
  for (const cle of ['alot', 'netz', 'szt_gra']) {
    const attendu = sol('TLV', '2026-11-10', cle);
    const trouve = c.evenements.find((e) => e.cle === cle && Math.abs(e.instant - attendu) < H);
    assert.ok(Math.abs(trouve.instant - attendu) <= 1000, cle);
  }
  for (const p of [versOuest, versEst]) {
    const v = Vol.enVol(Vol.calculer(p));
    assert.ok(v.length > 0);
    for (const e of v) {
      const lieu = Vol.lieuSous(e);
      const iso = new Intl.DateTimeFormat('en-CA', { timeZone: lieu.fuseau, year: 'numeric', month: '2-digit', day: '2-digit' }).format(e.instant);
      const jours = [-1, 0, 1].map((n) => new Date(Date.UTC(...iso.split('-').map((x, i) => Number(x) - (i === 1 ? 1 : 0)), 12) + n * 24 * H).toISOString().slice(0, 10));
      const ecart = Math.min(...jours.flatMap((d) => jour(lieu, d)).filter((z) => z.cle === e.cle && z.instant).map((z) => Math.abs(z.instant - e.instant)));
      assert.ok(ecart <= 2000, `${e.cle} ${e.instant.toISOString()} : ${ecart} ms`);
    }
  }
});

test('vers l’ouest le jour s’allonge, vers l’est la nuit raccourcit', () => {
  const chkiaVol = Vol.enVol(Vol.calculer(versOuest)).find((e) => e.cle === 'chkia').instant;
  for (const x of ['TLV', 'JFK']) assert.ok(chkiaVol - sol('TLV', '2026-11-10', 'netz') > sol(x, '2026-11-10', 'chkia') - sol(x, '2026-11-10', 'netz') + 15 * MIN);
  const netzVol = Vol.enVol(Vol.calculer(versEst)).find((e) => e.cle === 'netz').instant;
  for (const x of ['TLV', 'JFK']) assert.ok(netzVol - sol('JFK', '2026-11-10', 'chkia') < sol(x, '2026-11-11', 'netz') - sol(x, '2026-11-10', 'chkia') - 2 * H);
});

test('minuit UTC et minuit local, la prière du moment, loin du vol', () => {
  const c = Vol.calculer(plan('TLV', 'CDG', '2026-11-10T22:30', '2026-11-11T02:00'));
  const minuits = Vol.enVol(c).filter((e) => e.cle === 'chatsot_layla');
  assert.equal(minuits.length, 1);
  const m = minuits[0].instant;
  assert.deepEqual(Vol.priere(c, new Date(+m - 10 * MIN)), ['arvit', m]);
  assert.deepEqual(Vol.priere(c, new Date(+m + 10 * MIN)), ['arvit', null]);
  const o = Vol.calculer(versOuest);
  const chkia = Vol.enVol(o).find((e) => e.cle === 'chkia').instant;
  assert.deepEqual(Vol.priere(o, new Date(+chkia - 5 * MIN)), ['minha', chkia]);
  assert.equal(Vol.priere(o, new Date(+versOuest.atterrissage + 7 * H)), null);
});

test('prochaine : la règle de ZmanimWidget, mêmes cas que DixAmeliorationsTest', () => {
  const t = (h, m) => new Date(Date.UTC(2026, 9, 14, h, m));
  const horaires = { alot: t(5, 0), szt_gra: t(10, 30), chkia: t(17, 50), chatsot_layla: t(23, 40) };
  assert.deepEqual(Vol.prochaine(horaires, t(7, 0)), ['chaharit', t(10, 30)]);
  assert.deepEqual(Vol.prochaine(horaires, t(13, 0)), ['minha', t(17, 50)]);
  assert.deepEqual(Vol.prochaine(horaires, t(19, 0)), ['arvit', t(23, 40)]);
  assert.deepEqual(Vol.prochaine(horaires, t(23, 50)), ['arvit', null]);
});

test('un vol court sans zman ; la ligne de date ; une route polaire', () => {
  const court = Vol.calculer(plan('TLV', 'ETM', '2026-11-10T09:50', '2026-11-10T10:40'));
  assert.equal(Vol.enVol(court).length, 0);
  assert.equal(court.reserves.size, 0);
  const ouest = Vol.calculer(plan('SFO', 'HKG', '2026-11-10T11:00', '2026-11-11T19:00'));
  assert.equal(ouest.lignesDate.length, 1);
  assert.ok(ouest.lignesDate[0].apres > ouest.lignesDate[0].avant);
  const est = Vol.calculer(plan('HKG', 'SFO', '2026-11-10T21:00', '2026-11-10T18:00'));
  assert.equal(est.lignesDate.length, 1);
  assert.ok(est.lignesDate[0].apres < est.lignesDate[0].avant);
  const polaire = Vol.calculer(plan('JFK', 'HKG', '2026-06-20T11:00', '2026-06-21T15:00'));
  assert.ok(polaire.reserves.has('haute_latitude') && polaire.reserves.has('absent') && !polaire.reserves.has('erreur'));
});

test('parité avec Android : chaque zman en vol, à deux secondes près (vecteurs_vol.json)', () => {
  const vecteurs = JSON.parse(readFileSync(new URL('vecteurs_vol.json', import.meta.url), 'utf8'));
  assert.ok(vecteurs.vols.length >= 6);
  for (const v of vecteurs.vols) {
    const p = plan(v.depart, v.arrivee, v.decollage, v.atterrissage);
    if (v.releve) p.releve = { latitude: v.releve.latitude, longitude: v.releve.longitude, instant: new Date(v.releve.instant) };
    const c = Vol.calculer(p);
    assert.equal(c.fiabilite, v.fiabilite);
    const web = Vol.enVol(c);
    assert.deepEqual(web.map((e) => e.cle), v.enVol.map((e) => e.cle), `${v.depart}-${v.arrivee}`);
    web.forEach((e, i) => assert.ok(Math.abs(e.instant - Date.parse(v.enVol[i].instant)) <= 2000, `${v.depart}-${v.arrivee} ${e.cle}`));
    assert.deepEqual([...c.reserves].sort(), v.reserves);
    assert.deepEqual(c.lignesDate.map((l) => [l.avant, l.apres]), v.lignesDate);
    const jalons = Vol.jalons(c, false, 18);
    assert.deepEqual(jalons.map((j) => [j.type, j.date, j.omer]), v.jalons.map((j) => [j.type, j.date, j.omer]), `${v.depart}-${v.arrivee} ${v.decollage}`);
    jalons.forEach((j, i) => assert.ok(Math.abs(j.instant - Date.parse(v.jalons[i].instant)) <= 2000));
  }
});

test('Melbourne – Los Angeles – Montréal, Sydney – Queenstown, Tel Aviv – Buenos Aires : heures d’été et d’hiver', () => {
  // Décalage UTC, en heures, de l'heure du billet : décembre puis juillet (règles officielles, ZMANIM.md).
  const decalage = (iata, local) => (Date.parse(`${local}:00Z`) - Vol.instant(local, a(iata))) / H;
  for (const [iata, dec, juil] of [['MEL', 11, 10], ['SYD', 11, 10], ['LAX', -8, -7], ['YUL', -5, -4], ['ZQN', 13, 12], ['EZE', -3, -3], ['TLV', 2, 3], ['BNE', 10, 10], ['PER', 8, 8], ['DPS', 8, 8], ['CGK', 7, 7], ['DJJ', 9, 9], ['PHX', -7, -7], ['HNL', -10, -10]]) {
    assert.equal(decalage(iata, '2026-12-15T12:00'), dec, `${iata} décembre`);
    assert.equal(decalage(iata, '2026-07-15T12:00'), juil, `${iata} juillet`);
  }
  // Le Pacifique, dans les deux sens : la date locale recule vers l'est, avance vers l'ouest.
  const aller = Vol.calculer(plan('MEL', 'LAX', '2026-12-15T10:30', '2026-12-15T06:45'));
  assert.equal(aller.lignesDate.length, 1);
  assert.ok(aller.lignesDate[0].apres < aller.lignesDate[0].avant);
  const retour = Vol.calculer(plan('LAX', 'MEL', '2026-12-22T22:30', '2026-12-24T08:45'));
  assert.ok(retour.lignesDate[0].apres > retour.lignesDate[0].avant);
  // Queenstown (168° E) : pas de ligne de date ; Tel Aviv – Buenos Aires : l'équateur, pas la ligne.
  assert.equal(Vol.calculer(plan('SYD', 'ZQN', '2026-12-18T09:00', '2026-12-18T14:15')).lignesDate.length, 0);
  assert.equal(Vol.calculer(plan('TLV', 'EZE', '2026-11-10T23:00', '2026-11-11T09:20')).lignesDate.length, 0);
});


test('un relevé GPS en vol recale la route ; hors du vol, il est ignoré', () => {
  const releve = { latitude: 56, longitude: -30, instant: new Date('2026-11-10T18:30:00Z') };
  const avec = { ...versOuest, releve };
  const p = Vol.position(avec, releve.instant);
  assert.ok(Math.abs(p.latitude - 56) < 1e-6 && Math.abs(p.longitude + 30) < 1e-6);
  const c = Vol.calculer(avec);
  assert.equal(c.fiabilite, 'releve_gps');
  const sans = Vol.calculer(versOuest);
  assert.equal(sans.fiabilite, 'route_prevue');
  const chkia = (x) => Vol.enVol(x).find((e) => e.cle === 'chkia').instant;
  assert.notEqual(+chkia(c), +chkia(sans));
  const dehors = Vol.calculer({ ...versOuest, releve: { ...releve, instant: new Date(+versOuest.atterrissage + H) } });
  assert.equal(dehors.fiabilite, 'route_prevue');
  assert.deepEqual(Vol.enVol(dehors).map((e) => +e.instant), Vol.enVol(sans).map((e) => +e.instant));
});
