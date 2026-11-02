// Le jour du lieu (bug D du 02/11) : la date et l'heure de la ville choisie, pas celles du téléphone.
import { test } from 'node:test';
import assert from 'node:assert/strict';
import { heureDans, isoDans, isoLocal } from '../src/js/calendrier.js';

// 2 novembre 2026, 23 h 30 à Paris (UTC+1) = 22 h 30 UTC = 17 h 30 à New York (UTC-5).
const t = new Date(Date.UTC(2026, 10, 2, 22, 30));

test('le jour civil suit le fuseau de la ville', () => {
  assert.equal(isoDans(t, 'Europe/Paris'), '2026-11-02');
  assert.equal(isoDans(t, 'America/New_York'), '2026-11-02');
  assert.equal(isoDans(t, 'Asia/Jerusalem'), '2026-11-03');
  assert.equal(isoDans(t, 'Pacific/Kiritimati'), '2026-11-03');
});

test("l'heure suit le fuseau de la ville", () => {
  assert.deepEqual(heureDans(t, 'Europe/Paris'), { heures: 23, minutes: 30 });
  assert.deepEqual(heureDans(t, 'America/New_York'), { heures: 17, minutes: 30 });
  assert.deepEqual(heureDans(t, 'Asia/Jerusalem'), { heures: 0, minutes: 30 });
});

test('minuit à la ville : 00, pas 24', () => {
  const minuit = new Date(Date.UTC(2026, 10, 2, 22, 0)); // 00:00 à Jérusalem (UTC+2)
  assert.deepEqual(heureDans(minuit, 'Asia/Jerusalem'), { heures: 0, minutes: 0 });
  assert.equal(isoDans(minuit, 'Asia/Jerusalem'), '2026-11-03');
});

test('sans fuseau, ou fuseau inconnu : le jour du téléphone', () => {
  assert.equal(isoDans(t, undefined), isoLocal(t));
  assert.equal(isoDans(t, 'Pas/Un_Fuseau'), isoLocal(t));
  assert.deepEqual(heureDans(t, null), { heures: t.getHours(), minutes: t.getMinutes() });
});
