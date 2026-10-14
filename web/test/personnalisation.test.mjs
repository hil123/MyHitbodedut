// Les réglages de personnalisation (14/10) : valeurs par défaut, choix gardés, valeur inconnue ignorée.
import { test } from 'node:test';
import assert from 'node:assert/strict';
import { creeStore, memoire } from '../src/js/store.js';

test('livre de la halakha : Yalkut Yosef par défaut, puis le choix', () => {
  const s = creeStore(memoire());
  assert.equal(s.sourceHalakha(), 'yalkut');
  s.setSourceHalakha('ben_ish_hai');
  assert.equal(s.sourceHalakha(), 'ben_ish_hai');
  s.setSourceHalakha('autre');
  assert.equal(s.sourceHalakha(), 'yalkut');
});

test('écran d\'ouverture et Levana', () => {
  const s = creeStore(memoire());
  assert.equal(s.ouverture(), 'aujourdhui');
  s.setOuverture('zmanim');
  assert.equal(s.ouverture(), 'zmanim');
  assert.equal(s.levanaAffichee(), true);
  s.setLevanaAffichee(false);
  assert.equal(s.levanaAffichee(), false);
});
