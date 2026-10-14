// Hémisphère sud, Edot (14/10) : Yalkut Yosef, Kitsour Choulhan Aroukh, O.H. 117:20 —
// « מוריד הטל » toute l'année ; ותן טל ומטר comme la diaspora. Les autres noussa'h ne changent pas.
import { test } from 'node:test';
import assert from 'node:assert/strict';
import { conditionsPour } from '../src/js/siddur-jour.js';
import { ajouts } from '../src/js/jour-affiche.js';
import { jour } from '../src/js/calendrier-juif.js';

test('Edot au sud : Morid hatal en hiver, Tal ou-matar inchangé', () => {
  const hiver = jour('2027-01-10', false).conditions;
  assert.ok(hiver.has('machiv_harouah') && hiver.has('tal_oumatar'));
  const sud = conditionsPour('edot', false, hiver, true, true);
  assert.ok(!sud.has('machiv_harouah') && sud.has('morid_hatal') && sud.has('tal_oumatar'));
  assert.ok(ajouts(hiver, 'edot', false, true, true).includes('ajout_morid_hatal'));
  assert.ok(!ajouts(hiver, 'edot', false, true, true).includes('ajout_machiv_harouah'));
  // Au nord, ou dans un autre noussa'h : rien ne change.
  assert.ok(conditionsPour('edot', false, hiver, true, false).has('machiv_harouah'));
  assert.ok(conditionsPour('ashkenaz', false, hiver, false, true).has('machiv_harouah'));
  assert.ok(conditionsPour('sefard', false, hiver, false, true).has('machiv_harouah'));
});
