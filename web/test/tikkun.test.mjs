// Le Tikoun HaKlali du web (02/11) : les règles de TikkunActivity, PsalmActivity et
// LiturgyActivity, sur le texte même de l'application (res/raw/tikkun_haklali.json).
import { test } from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import * as T from '../src/js/tikkun.js';
import { aCalibrer, SEUIL_IMPRECIS } from '../src/js/boussole.js';

const tikkun = JSON.parse(readFileSync(new URL('../../android/app/src/main/res/raw/tikkun_haklali.json', import.meta.url), 'utf8'));
const ORDRE = [16, 32, 41, 42, 59, 77, 90, 105, 137, 150];

test('la liste suit l’ordre réel : l’ouverture, les dix psaumes rangés, puis clôture, kavana, Rabbi Natan', () => {
  const l = T.lignes(tikkun);
  assert.equal(l.length, 14);
  assert.equal(l[0].bloc.cle, 'ouverture');
  assert.deepEqual(l.slice(1, 11).map((x) => x.psaume.numero), ORDRE);
  assert.deepEqual(l.slice(1, 11).map((x) => x.rang), [1, 2, 3, 4, 5, 6, 7, 8, 9, 10]);
  assert.deepEqual(l.slice(11).map((x) => x.bloc.cle), ['cloture', 'kavana', 'priere_rabbi_nathan']);
  assert.ok(l.every((x) => !(x.bloc && x.rang)));
});

test('le psaume suivant, et rien après le dixième', () => {
  assert.equal(T.suivant(tikkun, 16).numero, 32);
  assert.equal(T.suivant(tikkun, 137).numero, 150);
  assert.equal(T.suivant(tikkun, 150), null);
  assert.equal(T.suivant(tikkun, 1), null);
  assert.equal(T.rang(tikkun, 105), 8);
  assert.equal(T.rang(tikkun, 1), 0);
  assert.equal(T.parNumero(tikkun, 90).versets.length, 17);
  assert.equal(T.parNumero(tikkun, 3), null);
});

test('la progression du jour : rien, en partie, en entier', () => {
  assert.equal(T.versets(tikkun), 164);
  assert.deepEqual(T.progres(tikkun, new Set()), { cle: 'tikkun_progress_none', args: [10, 164] });
  assert.deepEqual(T.progres(tikkun, new Set([16, 32, 41])), { cle: 'tikkun_progress', args: [3, 10] });
  assert.deepEqual(T.progres(tikkun, new Set(ORDRE)), { cle: 'tikkun_progress_all', args: [] });
});

test('une partie : seulement les quatre clés connues', () => {
  assert.equal(T.bloc(tikkun, 'kavana').cle, 'kavana');
  assert.ok(T.bloc(tikkun, 'kavana').formule);
  assert.equal(T.bloc(tikkun, 'psaumes'), null);
  assert.equal(T.bloc(tikkun, '_meta'), null);
  assert.equal(T.bloc(tikkun, undefined), null);
});

test('le détail et le sous-titre d’une partie, selon la langue (LocalizedText.forLanguage)', () => {
  const ouverture = T.bloc(tikkun, 'ouverture');
  const cloture = T.bloc(tikkun, 'cloture');
  assert.deepEqual(T.detailBloc(ouverture, 'fr'), { traduction: 'Avant de commencer' });
  assert.deepEqual(T.detailBloc(ouverture, 'he'), { paragraphes: ouverture.paragraphes.length });
  // La note d'abord ; sans note, le titre traduit ; en hébreu sans note, rien.
  assert.equal(T.sousTitreBloc(ouverture, 'en'), 'Good to say, but not indispensable.');
  assert.equal(T.sousTitreBloc(ouverture, 'he'), ouverture.note.he);
  assert.equal(T.sousTitreBloc(cloture, 'fr'), 'Après les dix psaumes');
  assert.equal(T.sousTitreBloc(cloture, 'he'), null);
  // Une traduction vide retombe sur l'hébreu, comme ifBlank.
  const sansFr = { titre: { he: 'א', fr: ' ', en: 'A' }, note: { he: '', fr: '', en: '' }, paragraphes: ['x'] };
  assert.deepEqual(T.detailBloc(sansFr, 'fr'), { paragraphes: 1 });
  assert.equal(T.sousTitreBloc(sansFr, 'fr'), null);
  assert.equal(T.sousTitreBloc(sansFr, 'en'), 'A');
});

test('boussole : calibrer seulement quand le navigateur dit la précision et qu’elle est basse', () => {
  assert.equal(aCalibrer({ alpha: 10, absolute: true }), null);
  assert.equal(aCalibrer({ webkitCompassHeading: 12, webkitCompassAccuracy: undefined }), null);
  assert.equal(aCalibrer({ webkitCompassHeading: 12, webkitCompassAccuracy: -1 }), true);
  assert.equal(aCalibrer({ webkitCompassHeading: 12, webkitCompassAccuracy: 10 }), false);
  assert.equal(aCalibrer({ webkitCompassHeading: 12, webkitCompassAccuracy: SEUIL_IMPRECIS }), false);
  assert.equal(aCalibrer({ webkitCompassHeading: 12, webkitCompassAccuracy: SEUIL_IMPRECIS + 5 }), true);
  assert.equal(aCalibrer(null), null);
});

test('les marques sont écrites comme Android (tikkun_day, tikkun_done triée) ; « Repartir de zéro » les vide', async () => {
  const { creeStore, memoire } = await import('../src/js/store.js');
  const m = memoire();
  const s = creeStore(m, { maintenant: () => new Date('2026-11-02T10:00:00'), fuseau: 'Europe/Paris' });
  for (const n of [150, 16, 32]) s.setTikkunDone(n, true);
  const lu = () => JSON.parse(m.getItem('myhitbodedut'));
  assert.equal(lu().tikkun_done, '16,32,150');
  assert.equal(lu().tikkun_day, '2026-11-02');
  s.resetTikkun();
  assert.equal(lu().tikkun_done, '');
  assert.equal(lu().tikkun_day, '2026-11-02');
  assert.equal(s.tikkunDone().size, 0);
});

test("l'arrêt de 32:7 coupe le verset entre « אַתָּה סֵתֶר לִי » et « מִצַּר »", () => {
  const v = T.parNumero(tikkun, 32).versets.find((x) => x.n === 7);
  const c = T.coupe(v);
  assert.ok(c, 'pas de coupe en 32:7');
  assert.equal(c[0], 'אַתָּה סֵתֶר לִי');
  assert.ok(c[1].startsWith('מִצַּר'));
  assert.equal(`${c[0]} ${c[1]}`, v.he);
  assert.equal(T.coupe({ he: 'א ב', arret: { avant: 'ג', note: '' } }), null);
  assert.equal(T.coupe({ he: 'א ב' }), null);
});
