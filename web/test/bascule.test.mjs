// Les jours de bascule de la pluie et Avinou malkénou la veille de Kippour un vendredi (audit du 16/10).
import assert from 'node:assert/strict';
import test from 'node:test';
import { jour } from '../src/js/calendrier-juif.js';
import { ajouts } from '../src/js/jour-affiche.js';
import { avinouMalkenou, conditionsPour } from '../src/js/siddur-jour.js';

test('22 Tichri et 15 Nissan : les deux formules (O.H. 114:1)', () => {
  for (const iso of ['2026-10-03', '2027-04-22']) {
    const c = conditionsPour('edot', true, jour(iso, true).conditions);
    assert.ok(c.has('machiv_harouah') && c.has('morid_hatal'), iso);
    const a = ajouts(jour(iso, true).conditions, 'edot', true);
    assert.ok(a.includes('ajout_machiv_harouah') && a.includes('ajout_morid_hatal'), iso);
  }
  // Le lendemain, une seule formule.
  const a = ajouts(jour('2026-10-04', true).conditions, 'edot', true);
  assert.ok(a.includes('ajout_machiv_harouah') && !a.includes('ajout_morid_hatal'));
  // Ashkenaz en diaspora ne dit jamais מוריד הטל.
  assert.ok(!conditionsPour('ashkenaz', false, jour('2026-10-03', false).conditions).has('morid_hatal'));
});

test('Avinou malkénou la veille de Kippour un vendredi (Rema O.H. 604:2)', () => {
  assert.ok(avinouMalkenou(jour('2028-09-29', false).conditions)); // vendredi 9 Tichri 5789
  assert.ok(!avinouMalkenou(jour('2026-09-20', false).conditions)); // dimanche 9 Tichri 5787
});

test('Jérusalem (Pourim le 15) : la ville, pas les localités voisines', async () => {
  const { aJerusalem } = await import('../src/js/calendrier-juif.js');
  const dedans = { vieille_ville: [31.7767, 35.2345], ramot: [31.8466, 35.1975], gilo: [31.735, 35.188], ein_kerem: [31.768, 35.159],
    har_homa: [31.722, 35.226], pisgat_zeev: [31.825, 35.238], isawiya: [31.796, 35.249] };
  const dehors = { maale_adoumim: [31.777, 35.298], mevasseret: [31.8017, 35.1478], bethleem: [31.705, 35.2], abou_dis: [31.762, 35.262],
    beitar: [31.696, 35.115], tel_aviv: [32.08, 34.78] };
  for (const [n, [la, lo]] of Object.entries(dedans)) assert.ok(aJerusalem(la, lo), n);
  for (const [n, [la, lo]] of Object.entries(dehors)) assert.ok(!aJerusalem(la, lo), n);
});

test('Ta\'hanoun du siddour ashkénaze annoté, comme Edot et Sfard (audit du 16/10)', async () => {
  const { readFileSync } = await import('node:fs');
  const a = JSON.parse(readFileSync(new URL('../../android/app/src/main/res/raw/siddur_annotations.json', import.meta.url), 'utf8'));
  for (const office of ['chaharit', 'minha']) {
    assert.ok(a.ashkenaz[office].some((e) => e.si === `tahanoun_${office}` && e.empreinte.startsWith('ויאמר דוד')), office);
  }
  assert.ok(a.ashkenaz.chaharit.some((e) => e.si === 'lundi_jeudi' && e.empreinte.startsWith('והוא רחום')));
});
