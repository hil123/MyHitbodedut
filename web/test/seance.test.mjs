// SessionRuleTest, SessionCounterTest, DanseMesureTest, ChizukBagTest et les
// règles d'affichage du ’hizouk, portés depuis Android ; puis la séance
// telle que le site l'enregistre.

import { test } from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { DanseMesure, SessionRule } from '../src/js/seance.js';
import { ChizukBag, ChizukRules, MOMENTS } from '../src/js/chizuk.js';
import { creeStore, memoire } from '../src/js/store.js';

const ICI = dirname(fileURLToPath(import.meta.url));
const chizuk = JSON.parse(readFileSync(join(ICI, '..', '..', 'android', 'app', 'src', 'main', 'res', 'raw', 'chizuk.json'), 'utf8'));

const minute = 60000;
const debut = 1_700_000_000_000;

test('une séance minutée s’arrête à son terme', () => {
  const fin = debut + 20 * minute;
  assert.equal(SessionRule.enCours(debut, fin, 20, fin - 1), true);
  assert.equal(SessionRule.enCours(debut, fin, 20, fin), false);
  assert.equal(SessionRule.enCours(debut, fin, 20, fin + minute), false);
});

test('une séance ouverte ne s’arrête pas d’elle-même', () => {
  assert.equal(SessionRule.estOuverte(debut, SessionRule.OUVERTE), true);
  for (const s of [1, 60, 600, 86400]) {
    assert.equal(SessionRule.enCours(debut, 0, SessionRule.OUVERTE, debut + s * 1000), true, `${s} s`);
  }
  assert.equal(SessionRule.estOuverte(0, SessionRule.OUVERTE), false);
  assert.equal(SessionRule.enCours(0, 0, SessionRule.OUVERTE, debut), false);
});

test('la durée réelle et le reste', () => {
  assert.equal(SessionRule.minutesEcoulees(debut, debut + 59999), 0);
  assert.equal(SessionRule.minutesEcoulees(debut, debut + minute), 1);
  assert.equal(SessionRule.minutesEcoulees(debut, debut + 47 * minute + 30000), 47);
  assert.equal(SessionRule.minutesEcoulees(debut, debut - minute), 0);
  assert.equal(SessionRule.minutesEcoulees(0, debut), 0);
  const fin = debut + 10 * minute;
  assert.equal(SessionRule.minutesRestantes(fin, debut), 10);
  assert.equal(SessionRule.minutesRestantes(fin, fin - 1000), 1);
  assert.equal(SessionRule.minutesRestantes(fin, fin), 0);
  assert.equal(SessionRule.minutesRestantes(fin, fin + minute), 0);
});

test('le compteur masqué ne dit rien', () => {
  assert.equal(SessionRule.compteur(false, true), 'masque');
  assert.equal(SessionRule.compteur(true, true), 'masque');
  assert.equal(SessionRule.compteur(false, false), 'reste');
  assert.equal(SessionRule.compteur(true, false), 'ecoule');
});

// --- DanseMesure ---------------------------------------------------------------------
const instants = Array.from({ length: 401 }, (_, i) => i / 400);
test('chaque valeur de la danse reste dans ses bornes', () => {
  for (const t of instants) {
    const p = DanseMesure.pas(t);
    assert.ok(p.hauteur >= 0 && p.hauteur <= 1);
    assert.ok(p.flexion >= 0 && p.flexion <= 1);
    assert.ok(p.balance >= -1 && p.balance <= 1);
    assert.ok(p.poseB >= 0 && p.poseB <= 1);
  }
});
test('le mouvement est continu, boucle comprise', () => {
  const ts = [...instants, 1];
  for (let i = 0; i < ts.length - 1; i++) {
    const x = DanseMesure.pas(ts[i]);
    const y = DanseMesure.pas(ts[i + 1]);
    assert.ok(Math.abs(x.hauteur - y.hauteur) < 0.05, `hauteur à ${ts[i]}`);
    assert.ok(Math.abs(x.balance - y.balance) < 0.05, `balance à ${ts[i]}`);
    assert.ok(Math.abs(x.poseB - y.poseB) < 0.3, `pose à ${ts[i]}`);
  }
});
test('il change de jambe à chaque temps et touche le sol en changeant', () => {
  const pres = (a, b) => assert.ok(Math.abs(a - b) < 0.001, `${a} ≠ ${b}`);
  pres(DanseMesure.pas(0.2).poseB, 0);
  pres(DanseMesure.pas(0.7).poseB, 1);
  pres(DanseMesure.pas(0).hauteur, 0);
  pres(DanseMesure.pas(0.5).hauteur, 0);
  pres(DanseMesure.pas(0.25).hauteur, 1);
  pres(DanseMesure.pas(0.5).flexion, 1);
});

// --- ChizukBag -------------------------------------------------------------------------
const vivier = ['a', 'b', 'c', 'd'];
const identite = (l) => [...l];
test('un cycle sort chaque texte une fois et une seule', () => {
  const sortis = [];
  let reste = [];
  for (let i = 0; i < vivier.length; i++) {
    const t = ChizukBag.suivant(reste, vivier, sortis.at(-1) ?? null, identite);
    sortis.push(t.id);
    reste = t.reste;
  }
  assert.deepEqual(new Set(sortis), new Set(vivier));
  assert.equal(new Set(sortis).size, vivier.length);
  assert.deepEqual(reste, []);
});
test('le cycle suivant ne rouvre pas sur le dernier texte du précédent', () => {
  const t = ChizukBag.suivant([], vivier, 'a', identite);
  assert.equal(t.id, 'b');
  assert.ok(t.reste.includes('a'));
});
test('cas limites du sac', () => {
  assert.equal(ChizukBag.suivant([], ['a'], 'a', identite).id, 'a');
  assert.deepEqual(ChizukBag.suivant(['a'], [], null, identite), { id: null, reste: [] });
  const t = ChizukBag.suivant(['z', 'b'], vivier, null, identite);
  assert.equal(t.id, 'b');
  assert.ok(!t.reste.includes('z'));
});
test('chaque moment tire dans son propre vivier, et n’a que des textes relus', () => {
  for (const moment of MOMENTS) {
    const items = ChizukRules.pourMoment(chizuk, moment);
    assert.ok(items.length > 0, `${moment} : aucun texte`);
    for (const it of items) {
      assert.ok(moment === 'demande' || it.moments.includes(moment));
      assert.equal(it.statut_verif, 'verifie_texte');
      assert.equal(it.relu, true);
    }
  }
});

test('l’hébreu sans nikkoud pour les éditions sans licence de vocalisation', () => {
  for (const it of chizuk.items) {
    const ok = chizuk._meta.editions[it.edition]?.nikkoud_ok ?? false;
    const he = ChizukRules.hebreu(chizuk, it);
    if (ok) assert.equal(he, it.he);
    else if (it.he_sans_nikkoud) assert.equal(he, it.he_sans_nikkoud);
  }
});

test('la traduction suit la règle des langues', () => {
  const it = chizuk.items[0];
  assert.equal(ChizukRules.traduction(it, 'he'), '');
  assert.equal(ChizukRules.traduction(it, 'iw'), '');
  assert.equal(ChizukRules.traduction(it, 'en'), it.en);
  assert.equal(ChizukRules.traduction(it, 'fr'), it.fr);
});

// --- La séance dans le stockage --------------------------------------------------------
function horloge(iso) {
  let t = new Date(iso).getTime();
  return { maintenant: () => new Date(t), avance: (ms) => { t += ms; } };
}

test('une séance minutée inscrit sa durée, même relue longtemps après son terme', () => {
  const h = horloge('2026-09-29T09:00:00');
  const s = creeStore(memoire(), { maintenant: h.maintenant, fuseau: 'Europe/Paris' });
  s.beginSession(20);
  assert.equal(s.isSessionRunning(), true);
  h.avance(3 * 3600 * 1000); // le téléphone a dormi trois heures
  assert.equal(s.isSessionRunning(), false);
  assert.equal(s.isSessionOver(), true);
  assert.equal(s.endSession().minutes, 20);
  assert.equal(s.isSessionOver(), false);
});

test('terminer avant le terme inscrit le temps réellement passé', () => {
  const h = horloge('2026-09-29T09:00:00');
  const s = creeStore(memoire(), { maintenant: h.maintenant, fuseau: 'Europe/Paris' });
  s.beginSession(60);
  h.avance(12 * minute + 30000);
  assert.equal(s.endSession({ avantTerme: true }).minutes, 12);
});

test('une séance ouverte court jusqu’à ce qu’on la close', () => {
  const h = horloge('2026-09-29T09:00:00');
  const s = creeStore(memoire(), { maintenant: h.maintenant, fuseau: 'Europe/Paris' });
  s.beginSession(0);
  h.avance(5 * 3600 * 1000);
  assert.equal(s.isSessionRunning(), true);
  assert.equal(s.isSessionOpen(), true);
  assert.equal(s.sessionCounter(), 'ecoule');
  assert.equal(s.endSession({ avantTerme: true }).minutes, 240);
});

test('les séances d’un jour s’additionnent, sujets compris, et le bravo attend une fois', () => {
  const h = horloge('2026-09-29T09:00:00');
  const s = creeStore(memoire(), { maintenant: h.maintenant, fuseau: 'Europe/Paris' });
  s.addSession(10, { theme: 'gratitude' });
  s.addSession(5);
  assert.deepEqual(s.record(), { date: '2026-09-29', minutes: 15, note: '', themes: ['gratitude'], noteSeule: false, faite: true });
  s.setPendingCelebration(15);
  assert.equal(s.takePendingCelebration(), 15);
  assert.equal(s.takePendingCelebration(), null);
});

test('le sac de ’hizouk survit d’un tirage à l’autre', () => {
  const s = creeStore(memoire(), { fuseau: 'Europe/Paris' });
  const vus = new Set();
  const n = ChizukRules.pourMoment(chizuk, 'ouverture').length;
  for (let i = 0; i < n; i++) vus.add(ChizukBag.tirer(s, chizuk, 'ouverture').id);
  assert.equal(vus.size, n, 'un texte est revenu avant la fin du cycle');
});

test('Ta’hanoun en fin de Tichri : toujours en Edot, au choix en Sfard, jamais en Ashkenaz', () => {
  const s = creeStore(memoire());
  assert.equal(s.sansTahanounFinTichri(), true); // Edot par défaut
  s.setSiddurNusah('sefard');
  assert.equal(s.sansTahanounFinTichri(), false);
  s.setSansTahanounFinTichriSfard(true);
  assert.equal(s.sansTahanounFinTichri(), true);
  s.setSiddurNusah('ashkenaz');
  assert.equal(s.sansTahanounFinTichri(), false);
});

test('à la demande : tout le publiable, et le partage garde la source (19/10)', () => {
  const publiables = chizuk.items.filter(ChizukRules.publiable).map((x) => x.id);
  assert.deepEqual(ChizukRules.pourMoment(chizuk, 'demande').map((x) => x.id), publiables);
  assert.equal(ChizukRules.partage('עִקַּר', '', 'Likoutei Moharan II, 25', 'https://x'), 'עִקַּר\n\n— Likoutei Moharan II, 25\n\nhttps://x');
});

test('les ambiances calculées : audibles, douces, jamais saturées, le silence se tait (19/10)', async () => {
  const { AMBIANCES, generer } = await import('../src/js/ambiance.js');
  for (const a of AMBIANCES) {
    const s = generer(a, 8000, 24);
    let crete = 0; let r = 0;
    for (const x of s) { crete = Math.max(crete, Math.abs(x)); r += x * x; }
    if (a === 'silence') assert.equal(crete, 0);
    else { assert.ok(Math.sqrt(r / s.length) > 0.02, a); assert.ok(crete <= 0.61, a); assert.equal(Math.abs(s[0]), 0, a); }
  }
  assert.deepEqual(generer('mer', 8000, 2), generer('mer', 8000, 2));
});
