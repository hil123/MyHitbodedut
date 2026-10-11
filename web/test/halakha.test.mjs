import { test } from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { anneeDuLivre, choix, part, pour, recherche, sections } from '../src/js/halakha.js';

const livre = JSON.parse(readFileSync(new URL('../../android/app/src/main/res/raw/ben_ish_hai.json', import.meta.url)));

test('chaque jour de trois années a sa halakha ; la semaine couvre la section (Halakha.kt)', () => {
  for (let t = Date.UTC(2025, 8, 1); t < Date.UTC(2028, 8, 1); t += 86400000) {
    const iso = new Date(t).toISOString().slice(0, 10);
    for (const il of [false, true]) assert.ok(pour(livre, iso, il).length, `${iso} ${il}`);
  }
  const semaine = [18, 19, 20, 21, 22, 23, 24].flatMap((d) => pour(livre, `2026-10-${d}`, false));
  assert.deepEqual(semaine.map((x) => x.numero), Array.from({ length: 21 }, (_, i) => i + 1));
  assert.ok(semaine.every((x) => x.annee === 1 && x.section === 'לך לך'));
});

test('jointes, séparées, Hanouka, part du jour', () => {
  const t = sections(livre.annees['1'], 'TAZRIA')[0];
  const m = sections(livre.annees['1'], 'METZORA')[0];
  assert.equal(m.depart, t.h.length);
  assert.equal(sections(livre.annees['2'], 'TAZRIA_METZORA').length, 2);
  assert.equal(choix(livre, 'VAYESHEV', 0, 5787, 1)[0].section, 'הלכות חנוכה');
  assert.equal(anneeDuLivre(5787), 1);
  assert.deepEqual(part([1, 2, 3], 3, 7), [1]);
  assert.ok(recherche(livre, 'ציצית').length > 10);
});
