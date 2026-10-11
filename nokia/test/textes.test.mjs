// Les textes de l'application Java : qui passe, sous quelle forme.
//
//   node --test nokia/test/

import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { test } from 'node:test';
import { fileURLToPath } from 'node:url';
import { litChaines } from '../../web/tools/chaines.mjs';
import { ChizukRules, MOMENTS } from '../../web/src/js/chizuk.js';
import { CLES_INTERFACE, decode, encode, latin1, momentDe, textesFrancais } from '../textes.mjs';

const ICI = dirname(fileURLToPath(import.meta.url));
const RES = join(ICI, '..', '..', 'android', 'app', 'src', 'main', 'res');
const chizuk = JSON.parse(readFileSync(join(RES, 'raw', 'chizuk.json'), 'utf8'));
const chaines = litChaines(readFileSync(join(RES, 'values', 'strings.xml'), 'utf8')).chaines;
const copie = (o) => JSON.parse(JSON.stringify(o));
const ids = (d) => d.moments.flatMap((m) => m.textes.map((t) => t.id));

test('chaque ’hizouk publiable part une fois, sous son premier moment', () => {
  const d = textesFrancais(chizuk, chaines);
  const attendus = chizuk.items.filter(ChizukRules.publiable).map((it) => it.id).sort();
  assert.ok(attendus.length > 0);
  assert.deepEqual([...ids(d)].sort(), attendus);
  for (const m of d.moments) {
    for (const t of m.textes) assert.equal(momentDe(chizuk.items.find((it) => it.id === t.id)), m.cle);
  }
  assert.deepEqual(d.moments.map((m) => m.cle), MOMENTS.filter((k) => d.moments.some((m) => m.cle === k)));
});

test('un texte non relu ou non vérifié ne part pas', () => {
  const donnees = copie(chizuk);
  donnees.items[0].relu = false;
  donnees.items[1].statut_verif = 'a_verifier';
  const d = textesFrancais(donnees, chaines);
  assert.ok(!ids(d).includes(donnees.items[0].id));
  assert.ok(!ids(d).includes(donnees.items[1].id));
});

test('le texte est la traduction française, jamais l’anglaise', () => {
  const d = textesFrancais(chizuk, chaines);
  const [premier] = d.moments[0].textes;
  const item = chizuk.items.find((it) => it.id === premier.id);
  assert.equal(premier.texte, latin1(item.fr));
  assert.notEqual(premier.texte, latin1(item.en));
});

test('les titres des moments viennent de strings.xml', () => {
  const d = textesFrancais(chizuk, chaines);
  for (const m of d.moments) assert.equal(m.titre, latin1(chaines[`moment_${m.cle}`]));
  assert.equal(d.interface.chizuk_stuck, latin1(chaines.chizuk_stuck));
});

test('tout se dessine avec une police Latin-1', () => {
  assert.equal(latin1('cœur… l’âme — « oui »'), "coeur... l'âme - « oui »");
  assert.throws(() => latin1('שלום'));
  const d = textesFrancais(chizuk, chaines);
  for (const s of JSON.stringify(d)) assert.ok(s.codePointAt(0) <= 0xff, s);
});

test('textes.bin se relit à l’identique, comme DataInputStream', () => {
  const d = textesFrancais(chizuk, chaines);
  const relu = decode(encode(d));
  assert.equal(relu.version, 1);
  assert.deepEqual(relu.interface, d.interface);
  assert.deepEqual(relu.moments, d.moments.map((m) => ({ cle: m.cle, titre: m.titre, textes: m.textes.map((t) => ({ ref: t.ref, texte: t.texte })) })));
});

test('les mots de l’application : hitbodedout, jamais séance', () => {
  const java = ['MyHitbodedut', 'Minuteur', 'Journal', 'Textes']
    .map((n) => readFileSync(join(ICI, '..', 'app', 'src', 'myhitbodedut', `${n}.java`), 'utf8')).join('\n');
  // Les clés de strings.xml (session_done_title…) ne s'affichent pas : leur texte, si.
  const chainesAffichees = [...java.matchAll(/"([^"\n]*)"/g)].map((m) => m[1])
    .filter((c) => !CLES_INTERFACE.includes(c)).join('\n');
  assert.ok(!/s[ée]ance|session/i.test(chainesAffichees));
  assert.ok(chainesAffichees.includes('Hitbodedout'));
});
