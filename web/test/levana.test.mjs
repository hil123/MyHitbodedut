import { test } from 'node:test';
import assert from 'node:assert/strict';
import { levana } from '../src/js/calendrier-juif.js';

// 'Hechvan 5787 : du 18 au 25 octobre 2026 au soir, comme CalJ (LevanaTest.kt).
test('Birkat haLevana, Hechvan 5787', () => {
  const l = levana('2026-10-14', 'Asia/Jerusalem');
  assert.equal(l.premier, '2026-10-18');
  assert.equal(l.dernier, '2026-10-25');
  assert.deepEqual(l.etat, { dans: 4 });
  assert.ok(l.debut < l.point === false && l.debut < l.fin);
  assert.equal(levana('2026-10-18', 'Asia/Jerusalem').etat, 'ce_soir');
  assert.equal(levana('2026-10-25', 'Asia/Jerusalem').etat, 'dernier');
  assert.ok(levana('2026-10-26', 'Asia/Jerusalem').premier > '2026-11-01');
});
