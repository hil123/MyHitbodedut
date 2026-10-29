import { test } from 'node:test';
import assert from 'node:assert/strict';
import { readdirSync } from 'node:fs';
import { FEMININ, chemin } from '../src/js/personnage.js';

const DESSINS = readdirSync(new URL('../../android/app/src/main/res/drawable-nodpi', import.meta.url)).map((n) => n.replace(/\.webp$/, ''));

test('sans choix ou pour צדיק, le hassid reste', () => {
  assert.equal(chemin('img/char_guitar.webp', null), 'img/char_guitar.webp');
  assert.equal(chemin('img/char_guitar.webp', 'm'), 'img/char_guitar.webp');
});

test('pour צדקת, chaque pose prend la pose feminine', () => {
  assert.equal(chemin('img/char_guitar.webp', 'f'), 'img/char_f_joy.webp');
  assert.equal(chemin('img/char_standing_back.webp', 'f'), 'img/char_f_standing_back.webp');
  // Une pochette ou une autre image ne bouge pas, et la table ne s'applique qu'une fois.
  assert.equal(chemin('img/scene_ouverture_tombe.webp', 'f'), 'img/scene_ouverture_tombe.webp');
  assert.equal(chemin('img/char_f_joy.webp', 'f'), 'img/char_f_joy.webp');
  assert.equal(chemin(undefined, 'f'), undefined);
});

test('la table couvre chaque pose du hassid, et chaque pose feminine existe', () => {
  const hassid = DESSINS.filter((n) => n.startsWith('char_') && !n.startsWith('char_f_') && n !== 'char_face');
  assert.deepEqual(Object.keys(FEMININ).sort(), hassid.sort());
  assert.deepEqual([...new Set(Object.values(FEMININ))].sort(), DESSINS.filter((n) => n.startsWith('char_f_')).sort());
});
