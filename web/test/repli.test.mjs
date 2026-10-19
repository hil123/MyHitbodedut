// Relevé du 19/10 : ce que le siddour annonce pour certains jours se replie les autres jours.
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import test from 'node:test';
import { jour } from '../src/js/calendrier-juif.js';
import { vrai } from '../src/js/siddur-jour.js';

const annotations = JSON.parse(readFileSync(new URL('../../android/app/src/main/res/raw/siddur_annotations.json', import.meta.url), 'utf8'));
const c = (iso, israel = true) => jour(iso, israel).conditions;

test('les conditions déduites du 19/10', () => {
  assert.ok(c('2026-10-19').has('lamnatseah')); // lundi 8 'Hechvan
  assert.ok(!c('2026-10-11').has('lamnatseah')); // Roch 'Hodech
  assert.ok(!c('2027-04-21').has('lamnatseah')); // veille de Pessa'h
  assert.ok(c('2026-08-20').has('ledavid') && c('2026-10-02').has('ledavid')); // Eloul ; Hochaana Rabba
  assert.ok(!c('2026-10-19').has('ledavid'));
  assert.ok(c('2026-10-02').has('techouva_hochaana') && !c('2026-10-19').has('techouva_hochaana'));
  assert.ok(c('2027-04-22').has('yom_tov') && c('2027-04-22').has('moed'));
  assert.ok(c('2027-04-23').has('motsae_chabbat_yt') && !c('2027-04-24').has('motsae_chabbat_yt'));
  assert.ok(!c('2026-10-19').has('beit_avel') && !c('2026-10-19').has('oubli'));
});

test('Barekhi nafchi (Edot, Arvit) replié hors de la veille de Roch \'Hodech', () => {
  const a = annotations.edot.arvit.find((e) => e.empreinte.startsWith('ברכי'));
  assert.equal(a.si, 'roch_hodech');
  assert.ok(!vrai(a.racine, c('2026-10-19')));
  assert.ok(vrai(a.racine, c('2026-10-11')));
  for (const nusah of ['sefard', 'ashkenaz']) {
    const r = annotations[nusah].chaharit.filter((e) => e.si === 'roch_hodech' && e.empreinte.includes('ברכי נפשי'));
    assert.ok(r.length > 0, nusah);
  }
});

test('למנצח, לדוד ה׳ אורי, בית האבל, ברכת כהנים du jeûne, oubli : annotés', () => {
  const si = (nusah, id) => new Set(annotations[nusah][id].map((e) => e.si));
  for (const nusah of ['sefard', 'ashkenaz']) {
    assert.ok(si(nusah, 'chaharit').has('lamnatseah'), nusah);
    assert.ok(si(nusah, 'chaharit').has('ledavid'), nusah);
    assert.ok(si(nusah, 'birkat_hamazon').has('oubli'), nusah);
    assert.ok(si(nusah, 'minha').has('jeune'), nusah);
  }
  assert.ok(si('edot', 'chaharit').has('beit_avel') && si('sefard', 'chaharit').has('beit_avel'));
  assert.ok(si('edot', 'birkat_hamazon').has('oubli') && si('edot', 'birkat_hamazon').has('yom_tov'));
  assert.ok(si('edot', 'arvit').has('motsae_chabbat_yt'));
});
