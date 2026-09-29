// Le pack construit (node nokia/build.mjs) : chaque image fait 240×320,
// rien ne manque, rien ne traîne. Ignoré si le pack n'est pas construit.

import assert from 'node:assert/strict';
import { existsSync, readFileSync, readdirSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { test } from 'node:test';
import { fileURLToPath } from 'node:url';
import { HAUTEUR, LANGUES, LARGEUR } from '../plan.mjs';

const ICI = dirname(fileURLToPath(import.meta.url));
const DIST = join(ICI, '..', 'dist');
const PACK = join(DIST, 'MyHitbodedut');
const construit = existsSync(join(DIST, 'manifeste.json'));

/** Largeur et hauteur lues dans l'en-tête JPEG (marqueur SOF). */
export function tailleJpeg(octets) {
  assert.equal(octets.readUInt16BE(0), 0xffd8, 'pas un JPEG');
  let i = 2;
  while (i < octets.length) {
    const marqueur = octets.readUInt16BE(i);
    const longueur = octets.readUInt16BE(i + 2);
    if (marqueur >= 0xffc0 && marqueur <= 0xffcf && ![0xffc4, 0xffc8, 0xffcc].includes(marqueur)) {
      return { hauteur: octets.readUInt16BE(i + 5), largeur: octets.readUInt16BE(i + 7) };
    }
    i += 2 + longueur;
  }
  throw new Error('marqueur SOF introuvable');
}

test('chaque image fait exactement 240×320', { skip: !construit && 'pack non construit' }, () => {
  for (const langue of LANGUES) {
    const dossier = join(PACK, langue.toUpperCase());
    for (const nom of readdirSync(dossier)) {
      const t = tailleJpeg(readFileSync(join(dossier, nom)));
      assert.deepEqual(t, { largeur: LARGEUR, hauteur: HAUTEUR }, nom);
    }
  }
});

test('le dossier de chaque langue contient exactement les pages du manifeste', { skip: !construit && 'pack non construit' }, () => {
  const manifeste = JSON.parse(readFileSync(join(DIST, 'manifeste.json'), 'utf8'));
  for (const langue of LANGUES) {
    const attendues = manifeste.langues[langue].flatMap((d) => d.pages).sort();
    assert.deepEqual(readdirSync(join(PACK, langue.toUpperCase())).sort(), attendues, langue);
    assert.ok(existsSync(join(DIST, `MyHitbodedut-Nokia-${langue.toUpperCase()}.zip`)));
  }
  if (manifeste.musique.length) {
    assert.deepEqual(readdirSync(join(PACK, 'musique')).sort(), [...manifeste.musique].sort());
  }
});
