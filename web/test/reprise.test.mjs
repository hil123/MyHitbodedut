// Bug du 02/11 : la prière du matin reprenait le lendemain là où on s'était
// arrêté la veille. Une prière ne reprend qu'après une courte interruption ;
// un livre reprend toujours (Store.positionLecture, Android).
import { test } from 'node:test';
import assert from 'node:assert/strict';
import { creeStore, memoire, PRIERE_REPRISE_MS, repriseValable } from '../src/js/store.js';

const H = 60 * 60 * 1000;
const T0 = Date.UTC(2026, 10, 1, 6, 30);

test('prière : reprise juste après une interruption', () => {
  const s = creeStore(memoire());
  s.setPositionLecture('s:chaharit', 1200, T0);
  assert.equal(s.positionLecture('s:chaharit', T0 + 20 * 60 * 1000), 1200);
  assert.equal(s.positionLecture('s:chaharit', T0 + PRIERE_REPRISE_MS - 1), 1200);
});

test('prière : le lendemain, au début', () => {
  const s = creeStore(memoire());
  s.setPositionLecture('s:chaharit', 1200, T0);
  assert.equal(s.positionLecture('s:chaharit', T0 + PRIERE_REPRISE_MS), 0);
  assert.equal(s.positionLecture('s:chaharit', T0 + 24 * H), 0);
  // « Reprendre » dans la Bibliothèque garde le titre ; seule la place est oubliée.
  assert.equal(s.derniereLecture(), 's:chaharit');
});

test('prière enregistrée avant la correction (sans heure) : au début', () => {
  assert.equal(repriseValable('s:chaharit', 0, T0), false);
  assert.equal(repriseValable('l:likoutei_etsot', 0, T0), true);
  // Horloge reculée : pas de reprise.
  assert.equal(repriseValable('s:chaharit', T0 + H, T0), false);
});

test('livre : reprise même des jours plus tard', () => {
  const s = creeStore(memoire());
  s.setPositionLecture('l:likoutei_etsot', 800, T0);
  assert.equal(s.positionLecture('l:likoutei_etsot', T0 + 10 * 24 * H), 800);
});
