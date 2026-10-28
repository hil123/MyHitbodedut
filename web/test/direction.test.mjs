import test from 'node:test';
import assert from 'node:assert/strict';
import { CIBLE_LAT, CIBLE_LON, distanceKm, loxodromie, octant, orthodromie } from '../src/js/direction.js';

const proche = (a, b, e = 0.2) => assert.ok(Math.abs(a - b) <= e, `${a} ≠ ${b}`);

test('New York : nord-est par la route la plus courte, plein est par le cap constant', () => {
  proche(orthodromie(40.7128, -74.0060), 54.1);
  proche(loxodromie(40.7128, -74.0060), 95.8);
});

test('Paris, Tel-Aviv, Melbourne (mêmes vecteurs que DirectionTefilaTest)', () => {
  proche(orthodromie(48.8566, 2.3522), 112.5);
  proche(loxodromie(48.8566, 2.3522), 124.5);
  proche(orthodromie(32.0853, 34.7818), 128.5);
  proche(orthodromie(-37.8136, 144.9631), 286.7);
  proche(loxodromie(-37.8136, 144.9631), 304.2);
});

test('distance et point cardinal', () => {
  const km = distanceKm(48.8566, 2.3522, CIBLE_LAT, CIBLE_LON);
  assert.ok(km > 3280 && km < 3380, String(km));
  assert.deepEqual([359, 54.1, 128.5, 304.2].map(octant), [0, 1, 3, 7]);
});
