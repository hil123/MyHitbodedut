// HitbodedutTest et GuideSujetsTest, portés depuis Android (02/11) : les cinq
// temps, leurs sources, leur parcours, et les invitations selon le sujet.

import { test } from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { Hitbodedut, MOUVEMENTS, SOURCE_LIKOUTEI, SOURCE_RAMBAM, invitationSujet } from '../src/js/hitbodedut.js';
import { Reference } from '../src/js/regles.js';
import { creeStore, memoire } from '../src/js/store.js';

const ICI = dirname(fileURLToPath(import.meta.url));
const RES = join(ICI, '..', '..', 'android', 'app', 'src', 'main', 'res');
const sujets = JSON.parse(readFileSync(join(RES, 'raw', 'guide_sujets.json'), 'utf8')).sujets;
// Les sujets de la séance, comme SUJETS d'app.js (Theme.kt).
const THEMES = ['emotions', 'gratitude', 'decisions', 'famille', 'zivoug', 'parnassa', 'avoda', 'difficultes', 'croissance', 'autre'];

test('les cinq temps sont distincts et complets', () => {
  assert.equal(MOUVEMENTS.length, 5);
  for (const champ of ['cle', 'he', 'titre', 'invite', 'illustration']) {
    assert.equal(new Set(MOUVEMENTS.map((m) => m[champ])).size, 5, champ);
  }
  for (const m of MOUVEMENTS) {
    assert.ok(m.he.trim() && m.source.trim(), m.cle);
    assert.ok(readFileSync(join(RES, 'drawable-nodpi', `${m.illustration}.webp`)).length > 0, m.illustration);
  }
});

test('les intitulés sont ceux de leurs sources, et les sources suivent la langue', () => {
  assert.deepEqual(MOUVEMENTS.map((m) => [m.cle, m.he]), [
    ['sihah', 'לְפָרֵשׁ שִׂיחָתוֹ'],
    ['harata', 'חֲרָטָה וּתְשׁוּבָה עַל הֶעָבָר'],
    ['bakasha', 'בַּקָּשַׁת תַחֲנוּנִים'],
    ['torah', 'לַעֲשׂוֹת מֵהַתּוֹרָה תְּפִלָּה'],
    ['hodaa', 'נוֹתֵן שֶׁבַח וְהוֹדָיָה'],
  ]);
  assert.equal(Reference.localise(SOURCE_LIKOUTEI, 'he'), 'ליקוטי מוהר״ן תניינא, כ״ה');
  assert.equal(Reference.localise(SOURCE_RAMBAM, 'he'), 'רמב״ם, הלכות תפילה א׳, ב׳');
  assert.equal(Reference.localise(SOURCE_LIKOUTEI, 'en'), 'Likutei Moharan II, 25');
  assert.equal(Reference.localise(SOURCE_RAMBAM, 'en'), 'Rambam, Hilchot Tefillah 1:2');
});

test('chaque temps se réclame de la source dont il vient, l’action de grâce après la demande', () => {
  const parCle = Object.fromEntries(MOUVEMENTS.map((m) => [m.cle, m]));
  for (const c of ['sihah', 'harata', 'bakasha', 'torah']) assert.equal(parCle[c].source, SOURCE_LIKOUTEI, c);
  assert.equal(parCle.hodaa.source, SOURCE_RAMBAM);
  const cles = MOUVEMENTS.map((m) => m.cle);
  assert.ok(cles.indexOf('hodaa') > cles.indexOf('bakasha'));
  assert.ok(Hitbodedut.estDernier(cles.indexOf('hodaa')));
});

test('le parcours reste dans ses bornes, et un indice aberrant ne rompt pas', () => {
  assert.equal(Hitbodedut.DERNIER, 4);
  assert.equal(Hitbodedut.suivant(0), 1);
  assert.equal(Hitbodedut.suivant(3), 4);
  assert.equal(Hitbodedut.suivant(4), 4);
  assert.equal(Hitbodedut.suivant(99), 4);
  assert.equal(Hitbodedut.precedent(1), 0);
  assert.equal(Hitbodedut.precedent(0), 0);
  assert.equal(Hitbodedut.precedent(-5), 0);
  assert.ok(Hitbodedut.estPremier(0) && !Hitbodedut.estPremier(1));
  assert.ok(Hitbodedut.estDernier(4) && !Hitbodedut.estDernier(3));
  assert.equal(Hitbodedut.mouvement(-1), MOUVEMENTS[0]);
  assert.equal(Hitbodedut.mouvement(400), MOUVEMENTS[4]);
  assert.equal(Hitbodedut.mouvement(undefined), MOUVEMENTS[0]);
});

test('les libellés existent dans les trois langues', () => {
  const cles = ['settings_hitbodedut', 'settings_hitbodedut_hint', 'hitbodedut_step', 'hitbodedut_previous', 'hitbodedut_next',
    ...MOUVEMENTS.flatMap((m) => [m.titre, m.invite])];
  for (const dossier of ['values', 'values-en', 'values-iw']) {
    const xml = readFileSync(join(RES, dossier, 'strings.xml'), 'utf8');
    for (const c of cles) assert.ok(xml.includes(`name="${c}"`), `${dossier} : ${c}`);
  }
});

test('chaque sujet sauf « autre » a une invitation par temps, dans les trois langues', () => {
  for (const t of THEMES.filter((x) => x !== 'autre')) {
    for (const m of MOUVEMENTS) {
      for (const l of ['fr', 'en', 'he']) {
        const s = invitationSujet(sujets, t, m.cle, l);
        assert.ok(s && !s.includes("'"), `${t} / ${m.cle} / ${l}`);
      }
    }
  }
  assert.equal(Object.keys(sujets).length, THEMES.length - 1);
});

test('sans sujet, ou « autre chose », le guide reste celui de toujours', () => {
  assert.equal(invitationSujet(sujets, null, 'sihah', 'fr'), null);
  assert.equal(invitationSujet(sujets, 'autre', 'sihah', 'fr'), null);
  assert.equal(invitationSujet(null, 'famille', 'sihah', 'fr'), null);
  assert.equal(invitationSujet(sujets, 'famille', 'inconnu', 'fr'), null);
  assert.equal(invitationSujet(sujets, 'famille', 'bakasha', 'iw'), invitationSujet(sujets, 'famille', 'bakasha', 'he'));
});

test('le guide est éteint par défaut et passe par la sauvegarde, sous la clé d’Android', () => {
  const store = creeStore(memoire());
  assert.equal(store.guidedHitbodedut(), false);
  store.setGuidedHitbodedut(true);
  const json = JSON.parse(store.exportJson());
  assert.equal(json.settings.guided_hitbodedut, true);
  const autre = creeStore(memoire());
  autre.importJson(JSON.stringify(json));
  assert.equal(autre.guidedHitbodedut(), true);
  json.settings.guided_hitbodedut = false;
  autre.importJson(JSON.stringify(json));
  assert.equal(autre.guidedHitbodedut(), false);
  // Une sauvegarde sans la clé ne touche pas au réglage (Store.importJson : settings.has).
  delete json.settings.guided_hitbodedut;
  store.importJson(JSON.stringify(json));
  assert.equal(store.guidedHitbodedut(), true);
});
