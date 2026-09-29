// Le plan du pack, sans navigateur : qui passe, sous quelle forme, dans
// quel ordre, sous quel nom.
//
//   node --test nokia/test/

import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { test } from 'node:test';
import { fileURLToPath } from 'node:url';
import { litChaines } from '../../web/tools/chaines.mjs';
import { ChizukRules, MOMENTS } from '../../web/src/js/chizuk.js';
import { LANGUES, formate, momentDe, nomDePage, planLangue, sectionDuMoment } from '../plan.mjs';

const ICI = dirname(fileURLToPath(import.meta.url));
const RES = join(ICI, '..', '..', 'android', 'app', 'src', 'main', 'res');
const lit = (...p) => readFileSync(join(...p), 'utf8');
const chizuk = JSON.parse(lit(RES, 'raw', 'chizuk.json'));
const tikkun = JSON.parse(lit(RES, 'raw', 'tikkun_haklali.json'));
const guideJson = JSON.parse(lit(ICI, '..', 'guide.json'));
const defaut = litChaines(lit(RES, 'values', 'strings.xml')).chaines;
const dossier = { fr: 'values', en: 'values-en', he: 'values-iw' };
const chainesDe = (l) => ({ ...defaut, ...litChaines(lit(RES, dossier[l], 'strings.xml')).chaines });
const plan = (langue, donnees = chizuk) =>
  planLangue({ langue, chizuk: donnees, tikkun, guideJson, chaines: chainesDe(langue) });

const copie = (o) => JSON.parse(JSON.stringify(o));

test('chaque ’hizouk publiable est envoyé une fois, dans chaque langue', () => {
  const publiables = chizuk.items.filter(ChizukRules.publiable).map((it) => it.id).sort();
  assert.ok(publiables.length > 0);
  for (const langue of LANGUES) {
    const ids = plan(langue).filter((d) => d.id).map((d) => d.id).sort();
    assert.deepEqual(ids, publiables, langue);
  }
});

test('un texte non relu ou non vérifié ne part pas', () => {
  const donnees = copie(chizuk);
  const [a, b] = donnees.items;
  a.relu = false;
  b.statut_verif = 'a_verifier';
  for (const langue of LANGUES) {
    const ids = plan(langue, donnees).map((d) => d.id);
    assert.ok(!ids.includes(a.id) && !ids.includes(b.id), langue);
  }
});

test("l'hébreu perd son nikkoud quand la licence de l'édition l'exige", () => {
  const donnees = copie(chizuk);
  const item = donnees.items.find(ChizukRules.publiable);
  donnees._meta.editions[item.edition].nikkoud_ok = false;
  item.he_sans_nikkoud = 'SANS NIKKOUD';
  const doc = plan('fr', donnees).find((d) => d.id === item.id);
  assert.equal(doc.blocs[0].texte, 'SANS NIKKOUD');
});

test("en hébreu, pas de traduction ; en français et en anglais, celle de la langue seulement", () => {
  const item = chizuk.items.find(ChizukRules.publiable);
  const blocs = (l) => plan(l).find((d) => d.id === item.id).blocs;
  assert.deepEqual(blocs('he').map((b) => b.type), ['he']);
  assert.deepEqual(blocs('fr').map((b) => b.type), ['he', 'trad']);
  assert.equal(blocs('fr')[1].texte, item.fr);
  assert.equal(blocs('en')[1].texte, item.en);
});

test('un texte est rangé sous son premier moment, dans l’ordre de l’application', () => {
  assert.equal(momentDe({ moments: ['cloture', 'ouverture'] }), 'ouverture');
  assert.equal(momentDe({ moments: [] }), null);
  const docs = plan('fr').filter((d) => d.id);
  const sections = docs.map((d) => Number(d.base.split('_')[1]));
  assert.deepEqual(sections, [...sections].sort((x, y) => x - y));
  assert.equal(sectionDuMoment(MOMENTS[0]), 2);
  assert.equal(sectionDuMoment(MOMENTS.at(-1)), 7);
});

test('le Tikoun : ouverture, les dix psaumes dans l’ordre, puis clôture, kavana et prière', () => {
  for (const langue of LANGUES) {
    const docs = plan(langue).filter((d) => d.base.includes('_8_tikoun_'));
    assert.equal(docs.length, 14, langue);
    const psaumes = docs.slice(1, 11);
    psaumes.forEach((d, i) => {
      const p = tikkun.psaumes[i];
      assert.ok(d.titre.endsWith(formate(chainesDe(langue).tikkun_psalm_he, p.numero)));
      assert.deepEqual(d.blocs.filter((b) => b.type === 'verset').map((b) => b.texte), p.versets.map((v) => v.he));
    });
    // L'indication d'arrêt suit son verset.
    const ps32 = psaumes[tikkun.psaumes.findIndex((p) => p.numero === 32)].blocs;
    const i = ps32.findIndex((b) => b.type === 'arret');
    assert.ok(i > 0 && ps32[i - 1].type === 'verset');
    assert.equal(docs.at(-1).titre.split(' · ')[1], tikkun.priere_rabbi_nathan.titre[langue]);
  }
});

test('le mode d’emploi ouvre chaque langue', () => {
  for (const langue of LANGUES) {
    const [premier] = plan(langue);
    assert.equal(premier.base, `${langue.toUpperCase()}_1_guide`);
    assert.ok(premier.blocs.length > 0);
  }
});

test('les noms de fichier : ASCII, uniques, et triés dans l’ordre de lecture', () => {
  for (const langue of LANGUES) {
    const docs = plan(langue);
    // Jusqu'à 12 pages par document : -10 doit suivre -09.
    const noms = docs.flatMap((d) => Array.from({ length: 12 }, (_, k) => nomDePage(d, k + 1)));
    for (const n of noms) assert.match(n, /^[A-Z]{2}_\d_[a-z]+(_\d{2})?-\d{2}\.jpg$/);
    assert.equal(new Set(noms).size, noms.length);
    assert.deepEqual([...noms].sort(), noms, langue);
  }
});

test('les mots de l’application : hitbodedout, hitbodedut, התבודדות', () => {
  const tout = JSON.stringify(guideJson);
  assert.ok(!/s[ée]ance|session/i.test(tout));
  assert.ok(guideJson.fr.lignes.join(' ').includes('hitbodedout'));
  assert.ok(guideJson.en.lignes.join(' ').includes('hitbodedut'));
  assert.ok(guideJson.he.lignes.join(' ').includes('התבודדות'));
});
