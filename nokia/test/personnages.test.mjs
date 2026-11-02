// Le personnage de l'application Java au féminin (צדקת) : la même table que le
// site et Android, et une image pour chaque pose.
//
//   node --test nokia/test/

import assert from 'node:assert/strict';
import { existsSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { test } from 'node:test';
import { fileURLToPath } from 'node:url';
import { FEMININ as SITE } from '../../web/src/js/personnage.js';
import { FEMININ, PERSONNAGES } from '../app/genere-personnages.mjs';

const P = join(dirname(fileURLToPath(import.meta.url)), '..', 'app', 'res', 'p');

test('chaque pose du téléphone a sa version féminine, celle du site', () => {
  for (const nom of PERSONNAGES) {
    assert.equal(FEMININ[nom], SITE[nom], nom);
    assert.ok(existsSync(join(P, `${nom.slice(5)}.png`)), `${nom}.png`);
    assert.ok(existsSync(join(P, `f_${nom.slice(5)}.png`)), `f_${nom.slice(5)}.png`);
  }
  assert.deepEqual(Object.keys(FEMININ).sort(), [...PERSONNAGES].sort());
});
