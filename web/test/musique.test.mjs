// MelangeMusiqueTest, porté ; le titre d'un fichier ajouté, les noms donnés, la sauvegarde.

import { test } from 'node:test';
import assert from 'node:assert/strict';
import { MelangeMusique, estQuota, nomDonne, renomme, titreDeFichier } from '../src/js/musique.js';
import { creeStore, memoire } from '../src/js/store.js';

/** Un hasard rejouable (LCG), comme Random(seed) des tests Android. */
const graine = (n) => {
  let x = n >>> 0;
  return () => { x = (Math.imul(x, 1664525) + 1013904223) >>> 0; return x / 2 ** 32; };
};

test('aucun morceau ne revient avant que tous aient été joués', () => {
  const ids = ['a', 'b', 'c', 'd', 'e'];
  const m = new MelangeMusique(graine(7));
  for (let tour = 0; tour < 20; tour++) {
    const joues = ids.map(() => m.suivant(ids));
    assert.deepEqual([...joues].sort(), ids);
  }
});

test('le dernier d’un tour n’ouvre jamais le suivant', () => {
  for (let g = 0; g < 200; g++) {
    const m = new MelangeMusique(graine(g));
    const ids = ['a', 'b', 'c'];
    let avant = null;
    for (let i = 0; i < 30; i++) {
      const id = m.suivant(ids);
      assert.notEqual(id, avant);
      avant = id;
    }
  }
});

test('l’ordre change d’un tour à l’autre', () => {
  const ids = ['a', 'b', 'c', 'd', 'e', 'f', 'g', 'h'];
  const m = new MelangeMusique(graine(3));
  const premier = ids.map(() => m.suivant(ids));
  const second = ids.map(() => m.suivant(ids));
  assert.notDeepEqual(premier, second);
});

test('un seul morceau revient, aucun rend null', () => {
  const m = new MelangeMusique(graine(1));
  assert.equal(m.suivant([]), null);
  assert.deepEqual([m.suivant(['seul']), m.suivant(['seul']), m.suivant(['seul'])], ['seul', 'seul', 'seul']);
  assert.equal(m.dernier, 'seul');
});

test('un morceau retiré entre-temps ne sort plus', () => {
  const m = new MelangeMusique(graine(3));
  const premier = m.suivant(['a', 'b', 'c', 'd']);
  const reste = ['a', 'b', 'c', 'd'].filter((x) => x !== premier);
  const retire = reste[0];
  const sans = ['a', 'b', 'c', 'd'].filter((x) => x !== retire);
  for (let i = 0; i < 12; i++) assert.notEqual(m.suivant(sans), retire);
});

test('les doublons ne comptent qu’une fois par tour', () => {
  const m = new MelangeMusique(graine(5));
  const tour = [m.suivant(['a', 'a', 'b']), m.suivant(['a', 'a', 'b'])];
  assert.deepEqual(tour.sort(), ['a', 'b']);
});

test('le titre d’un fichier : son nom sans l’extension', () => {
  assert.equal(titreDeFichier('Niggoun de Breslev.m4a'), 'Niggoun de Breslev');
  assert.equal(titreDeFichier('chant.v2.mp3'), 'chant.v2');
  assert.equal(titreDeFichier('.cache'), '.cache');
  assert.equal(titreDeFichier('C:\\musique\\ניגון.ogg'), 'ניגון');
  assert.equal(titreDeFichier(''), '');
});

test('un nom vide rend le titre d’origine', () => {
  let noms = renomme({}, 'niggoun_01', '  Le mien  ');
  assert.equal(nomDonne(noms, 'niggoun_01'), 'Le mien');
  noms = renomme(noms, 'niggoun_01', ' ');
  assert.equal(nomDonne(noms, 'niggoun_01'), null);
  assert.deepEqual(noms, {});
  assert.equal(nomDonne(null, 'x'), null);
});

test('le quota se reconnaît d’un navigateur à l’autre', () => {
  assert.equal(estQuota({ name: 'QuotaExceededError' }), true);
  assert.equal(estQuota({ name: 'NS_ERROR_DOM_QUOTA_REACHED' }), true);
  assert.equal(estQuota({ code: 22 }), true);
  assert.equal(estQuota({ name: 'NotReadableError' }), false);
  assert.equal(estQuota(null), false);
});

test('la lecture aléatoire : désactivée d’abord, gardée et rendue par la sauvegarde (Store.musicShuffle)', () => {
  const s = creeStore(memoire());
  assert.equal(s.musicShuffle(), false);
  s.setMusicShuffle(true);
  assert.equal(JSON.parse(s.exportJson()).settings.music_shuffle, true);
  const autre = creeStore(memoire());
  autre.importJson(s.exportJson());
  assert.equal(autre.musicShuffle(), true);
  // Une valeur qui n'est pas un booléen ne change rien.
  autre.importJson(JSON.stringify({ format: 'myhitbodedut', days: {}, settings: { music_shuffle: 'oui' } }));
  assert.equal(autre.musicShuffle(), true);
});
