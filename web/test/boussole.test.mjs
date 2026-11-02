// La boussole du web (28/10) : les mêmes villes que DirectionJerusalemTest
// (Android), contre geographiclib (WGS84) ; le modèle magnétique contre les
// 100 valeurs de test officielles de WMM2025 (NOAA, WMM2025_TestValues.txt).
import { test } from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import * as B from '../src/js/boussole.js';
import { champ, declinaison, anneeDecimale } from '../src/js/magnetisme.js';

const VILLES = [
  ['Paris', 48.8566, 2.3522, 112.39, 124.51, 3337],
  ['New York', 40.7128, -74.0060, 54.01, 95.80, 9189],
  ['Los Angeles', 34.0522, -118.2437, 23.71, 91.01, 12208],
  ['Melbourne', -37.8136, 144.9631, 286.52, 304.16, 13709],
  ['Buenos Aires', -34.6037, -58.3816, 64.76, 53.03, 12231],
  ['Montréal', 45.5017, -73.5673, 54.90, 99.22, 8851],
];

test('les deux directions et la distance, comme Android', () => {
  for (const [nom, lat, lon, gc, rh, km] of VILLES) {
    assert.ok(Math.abs(B.ecart(gc, B.grandCercle(lat, lon))) <= 0.5, `${nom} grand cercle`);
    assert.ok(Math.abs(B.ecart(rh, B.carte(lat, lon))) <= 0.05, `${nom} carte`);
    assert.ok(Math.abs(B.distanceKm(lat, lon) - km) / km < 0.005, `${nom} distance`);
  }
  const honolulu = B.carte(21.3069, -157.8583);
  assert.ok(honolulu > 270 && honolulu < 300);
  assert.equal(B.distanceKm(B.LATITUDE, B.LONGITUDE), 0);
});

test('écart, tolérance, degrés, points, filtre', () => {
  assert.equal(B.ecart(350, 10), -20);
  assert.equal(B.ecart(10, 350), 20);
  assert.ok(B.face(4) && B.face(-4) && !B.face(4.1));
  assert.equal(B.degres(359.6), 0);
  assert.equal(B.point(54), 1);
  assert.equal(B.lisser(350, 10), 354);
  assert.equal(B.xHorizon(-90), -150);
});

test("le cap d'un événement d'orientation", () => {
  assert.equal(B.capMagnetique({ webkitCompassHeading: 30 }), 30);
  assert.equal(B.capMagnetique({ webkitCompassHeading: 30 }, 90), 120);
  assert.equal(B.capMagnetique({ absolute: true, alpha: 90 }), 270);
  assert.equal(B.capMagnetique({ absolute: false, alpha: 90 }), null);
  assert.equal(B.capMagnetique({ alpha: null }), null);
});

// Bug du 02/11 : téléphone droit, le cap ne doit plus dépendre du haut de l'écran (qui pointe le ciel).
const proche = (a, b, tol = 1e-6) => assert.ok(Math.abs(B.ecart(a, b)) < tol, `${a} ≠ ${b}`);
test('capDevant : à plat, le cap du haut de l\'écran (360 − alpha)', () => {
  for (const a of [0, 30, 90, 200, 359]) proche(B.capDevant(a, 0, 0), 360 - a);
  proche(B.capDevant(90, 0, 0, 90), 0);
});
test('capDevant : droit devant soi, le cap du dos du téléphone', () => {
  proche(B.capDevant(0, 90, 0), 0);
  proche(B.capDevant(270, 90, 0), 90);
  proche(B.capDevant(180, 90, 0), 180);
  // Incliné à 45° ou presque vertical : même cap, sans saut.
  for (const b of [30, 45, 60, 80, 89, 89.9, 90]) proche(B.capDevant(270, b, 0), 90);
  proche(B.capMagnetique({ absolute: true, alpha: 270, beta: 90, gamma: 0 }), 90);
});
test('capDevant : droit, en paysage, le dos du téléphone donne toujours le cap', () => {
  // Paysage (écran tourné de 90°), dressé : X vers le ciel, Y vers l'ouest, Z vers le sud → le dos regarde le nord.
  proche(B.capDevant(90, 0, -90, 90), 0);
  // Même pose, tournée de 90° vers la droite : face à l'est.
  proche(B.capDevant(0, 0, -90, 90), 90);
});

test('WMM2025 : les 100 valeurs de test de la NOAA, à 0,01° près', () => {
  const lignes = readFileSync(new URL('./vecteurs_wmm2025.txt', import.meta.url), 'utf8').split('\n').filter((l) => l.trim() && !l.startsWith('#'));
  assert.equal(lignes.length, 100);
  for (const l of lignes) {
    const [annee, alt, lat, lon, D, I, , X, Y, Z] = l.trim().split(/\s+/).map(Number);
    const c = champ(lat, lon, alt, annee);
    assert.ok(Math.abs(c.declinaison - D) <= 0.01, `D ${l}`);
    assert.ok(Math.abs(c.inclinaison - I) <= 0.01, `I ${l}`);
    for (const [a, b] of [[c.X, X], [c.Y, Y], [c.Z, Z]]) assert.ok(Math.abs(a - b) <= 0.1, `XYZ ${l}`);
  }
  assert.equal(anneeDecimale(new Date(Date.UTC(2026, 0, 1))), 2026);
  // Le cap vrai : le magnétique plus la déclinaison du lieu.
  const ny = { latitude: 40.7128, longitude: -74.0060 };
  const date = new Date(Date.UTC(2026, 9, 28));
  assert.equal(B.capVrai(100, ny, date), B.norm(100 + declinaison(ny.latitude, ny.longitude, date)));
});
