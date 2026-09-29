// Les tests Android des mêmes règles, portés un à un :
// RegleLangueTest, TraductionAffichageTest, JaugeSemaineTest, HolidaysTest,
// DureeSeanceTest, BackupRulesTest (format de sauvegarde).

import { test } from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { DureeSeance, JaugeSemaine, Reference, RegleLangue } from '../src/js/regles.js';
import { estJourChome, israelParDefaut } from '../src/js/calendrier.js';
import { creeStore, memoire } from '../src/js/store.js';
import { creeTraduction, formate, langueInitiale } from '../src/js/i18n.js';
import { litChaines, valeurAndroid } from '../tools/chaines.mjs';

const ICI = dirname(fileURLToPath(import.meta.url));
const RES = join(ICI, '..', '..', 'android', 'app', 'src', 'main', 'res');

// --- RegleLangue -------------------------------------------------------------
test('en hébreu, rien que de l’hébreu', () => {
  assert.deepEqual(RegleLangue.permises('he'), new Set(['he']));
  assert.deepEqual(RegleLangue.permises('iw'), new Set(['he']));
  assert.equal(RegleLangue.permet('he', 'fr'), false);
  assert.equal(RegleLangue.permet('he', 'en'), false);
  assert.equal(RegleLangue.traduit('iw'), false);
});

test('en anglais, l’hébreu et l’anglais, pas le français', () => {
  assert.deepEqual(RegleLangue.permises('en'), new Set(['he', 'en']));
  assert.equal(RegleLangue.permet('en', 'he'), true);
  assert.equal(RegleLangue.permet('en', 'fr'), false);
});

test('en français, l’hébreu et le français, pas l’anglais', () => {
  assert.deepEqual(RegleLangue.permises('fr'), new Set(['he', 'fr']));
  assert.equal(RegleLangue.permet('fr', 'he'), true);
  assert.equal(RegleLangue.permet('fr', 'en'), false);
});

test('aucun repli d’une langue sur une autre', () => {
  const seulementFrancais = { fr: 'une note' };
  assert.equal(RegleLangue.choisit('fr', seulementFrancais), 'une note');
  assert.equal(RegleLangue.choisit('en', seulementFrancais), null);
  assert.equal(RegleLangue.choisit('he', seulementFrancais), null);
  assert.equal(RegleLangue.choisit('en', { fr: 'x', he: '' }), null);
});

// --- Reference -----------------------------------------------------------------
// Les références des données, lues comme ChizukCard, la bibliothèque et les Tfilot les lisent.
function toutesLesRefs() {
  const refs = [];
  const parcours = (v, cle) => {
    if (Array.isArray(v)) v.forEach((x) => parcours(x));
    else if (v && typeof v === 'object') Object.entries(v).forEach(([k, x]) => parcours(x, k));
    else if (cle === 'ref' && typeof v === 'string' && v.trim() !== '') refs.push(v);
  };
  for (const f of ['chizuk.json', 'bibliotheque.json', 'tikkun_haklali.json']) {
    parcours(JSON.parse(readFileSync(join(RES, 'raw', f), 'utf8')));
  }
  return refs;
}

test('en hébreu une référence ne garde aucune lettre latine ni aucun chiffre', () => {
  const refs = toutesLesRefs();
  assert.ok(refs.length > 60, `aucune référence lue : ${refs.length}`);
  for (const ref of refs.filter((r) => Reference.connue(r))) {
    const he = Reference.localise(ref, 'iw');
    assert.doesNotMatch(he, /[A-Za-z0-9]/, `« ${ref} » → « ${he} »`);
  }
  assert.equal(Reference.localise('Likoutei Moharan II, 25', 'he'), 'ליקוטי מוהר״ן תניינא, כ״ה');
  assert.equal(Reference.localise('Michlé 24:16', 'he'), 'משלי כ״ד, ט״ז');
  assert.equal(Reference.localise('Michlé 24:16', 'en'), 'Mishlei 24:16');
});

test('les nombres en lettres hébraïques', () => {
  const attendus = {
    1: 'א׳', 7: 'ז׳', 10: 'י׳', 15: 'ט״ו', 16: 'ט״ז', 25: 'כ״ה',
    39: 'ל״ט', 100: 'ק׳', 145: 'קמ״ה', 261: 'רס״א', 437: 'תל״ז', 400: 'ת׳',
  };
  for (const [n, a] of Object.entries(attendus)) assert.equal(Reference.lettres(Number(n)), a, n);
});

test('en anglais une référence ne garde aucune graphie française', () => {
  const francais = /Likoutei|Si'hot|'Hayé|Chiv'hei|Michlé|Hit'hazkout|\bTefila\b| ; /;
  for (const ref of toutesLesRefs().filter((r) => Reference.connue(r))) {
    assert.doesNotMatch(Reference.localise(ref, 'en'), francais, ref);
  }
  assert.equal(Reference.localise('Likoutei Moharan II, 78', 'en'), 'Likutei Moharan II, 78');
  assert.equal(Reference.localise('Likoutei Moharan II, 78', 'fr'), 'Likoutei Moharan II, 78');
});

test('chaque référence des données a sa forme anglaise et hébraïque', () => {
  const inconnues = [...new Set(toutesLesRefs().filter((r) => !Reference.connue(r)))];
  assert.deepEqual(inconnues, []);
});

// --- JaugeSemaine ------------------------------------------------------------------
const semaine = (...etats) => etats.map((e, i) => ({
  done: e.includes('f'), exempt: e.includes('x'), shabbat: i === 6, future: e.includes('a'),
}));

test('le chabbat chômé ne compte pas parmi les jours dus', () => {
  const p = JaugeSemaine.progression(semaine('f', 'f', '', 'a', 'a', 'a', 'x'));
  assert.equal(p.faits, 2);
  assert.equal(p.dus, 6);
  assert.ok(Math.abs(p.fraction - 2 / 6) < 1e-4);
});

test('une séance faite un jour exempté compte quand même', () => {
  const p = JaugeSemaine.progression(semaine('f', 'f', 'f', 'f', 'f', 'f', 'fx'));
  assert.deepEqual([p.faits, p.dus, p.fraction], [7, 7, 1]);
});

test('une semaine sans jour dû reste vide', () => {
  assert.equal(JaugeSemaine.progression(semaine('x', 'x', 'x', 'x', 'x', 'x', 'x')).fraction, 0);
});

// --- Calendrier (HolidaysTest : Tichri 5787) -------------------------------------------
test('diaspora, cinq jours chômés en Tichri 5787', () => {
  for (const d of ['2026-09-21', '2026-09-26', '2026-09-27', '2026-10-03', '2026-10-04']) {
    assert.ok(estJourChome(d, false), d);
  }
});
test('Israël, trois jours chômés', () => {
  for (const d of ['2026-09-21', '2026-09-26', '2026-10-03']) assert.ok(estJourChome(d, true), d);
});
test('le deuxième jour de fête ne vaut qu’en diaspora', () => {
  for (const d of ['2026-09-27', '2026-10-04']) {
    assert.equal(estJourChome(d, true), false, d);
    assert.equal(estJourChome(d, false), true, d);
  }
});
test('Roch Hachana, deux jours partout ; le 28 septembre est ordinaire', () => {
  assert.ok(estJourChome('2026-09-12', true));
  assert.ok(estJourChome('2026-09-13', true));
  assert.equal(estJourChome('2026-09-28', true), false);
  assert.equal(estJourChome('2026-09-28', false), false);
});
test('Pessa’h et Chavouot 5787', () => {
  // 15 Nissan 5787 = 22/04/2027 ; 21 Nissan = 28/04 ; 6 Sivan = 11/06/2027.
  assert.ok(estJourChome('2027-04-22', true));
  assert.ok(estJourChome('2027-04-28', true));
  assert.equal(estJourChome('2027-04-23', true), false);
  assert.ok(estJourChome('2027-04-23', false));
  assert.ok(estJourChome('2027-06-11', true));
  assert.equal(estJourChome('2027-06-12', true), true, 'samedi');
});
test('un samedi ordinaire reste chômé, un jour de semaine ordinaire ne l’est pas', () => {
  assert.ok(estJourChome('2026-09-19', true));
  assert.ok(estJourChome('2026-10-10', false));
  assert.equal(estJourChome('2026-09-22', true), false);
});
test('la région par défaut suit le fuseau', () => {
  assert.equal(israelParDefaut('Asia/Jerusalem'), true);
  assert.equal(israelParDefaut('Europe/Paris'), false);
});

// --- DureeSeance ----------------------------------------------------------------------------
test('les durées demandées par le propriétaire', () => {
  assert.deepEqual(DureeSeance.PROPOSEES, [5, 10, 15, 30, 60, 90]);
});
test('le temps saisi est borné entre 1 et 240 minutes', () => {
  assert.equal(DureeSeance.lire(' 25 '), 25);
  assert.equal(DureeSeance.lire('0'), 1);
  assert.equal(DureeSeance.lire('600'), 240);
  assert.equal(DureeSeance.lire(''), null);
  assert.equal(DureeSeance.lire('vingt'), null);
});

// --- Chaînes ---------------------------------------------------------------------------------
test('les chaînes Android se lisent comme aapt les lit', () => {
  assert.equal(valeurAndroid("Aujourd\\'hui : %1$s"), "Aujourd'hui : %1$s");
  assert.equal(valeurAndroid('Vendredi &amp; samedi'), 'Vendredi & samedi');
  assert.equal(valeurAndroid('"  deux  espaces "'), '  deux  espaces ');
  assert.equal(valeurAndroid('ligne\\nsuivante'), 'ligne\nsuivante');
});

test('chaque langue a toutes les chaînes et tous les pluriels du français', () => {
  const fr = litChaines(readFileSync(join(RES, 'values', 'strings.xml'), 'utf8'));
  const nonTraduisibles = new Set([...readFileSync(join(RES, 'values', 'strings.xml'), 'utf8')
    .matchAll(/<string name="([^"]+)" translatable="false"/g)].map((m) => m[1]));
  for (const dossier of ['values-en', 'values-iw']) {
    const l = litChaines(readFileSync(join(RES, dossier, 'strings.xml'), 'utf8'));
    const manquantes = Object.keys(fr.chaines).filter((k) => !nonTraduisibles.has(k) && !(k in l.chaines));
    assert.deepEqual(manquantes, [], dossier);
    assert.deepEqual(Object.keys(fr.pluriels).filter((k) => !(k in l.pluriels)), [], dossier);
  }
});

test('le format et les pluriels suivent Android', () => {
  assert.equal(formate('%1$d h %2$02d', [1, 5]), '1 h 05');
  assert.equal(formate('%1$s, volume %2$d %%', ['Niggoun 01', 60]), 'Niggoun 01, volume 60 %');
  const fr = creeTraduction('fr', {
    chaines: { duration_minutes: '%1$d min', duration_hours: '%1$d h', duration_hours_minutes: '%1$d h %2$02d' },
    pluriels: { status_streak: { one: "%d jour d'affilée", other: "%d jours d'affilée" } },
  });
  // En français, zéro et un prennent le singulier (règle CLDR, comme Android).
  assert.equal(fr.p('status_streak', 1), "1 jour d'affilée");
  assert.equal(fr.p('status_streak', 3), "3 jours d'affilée");
  assert.deepEqual([fr.duree(20), fr.duree(60), fr.duree(90)], ['20 min', '1 h', '1 h 30']);
  const he = creeTraduction('he', { chaines: {}, pluriels: { x: { one: 'יום', two: 'יומיים', other: '%d ימים' } } });
  assert.equal(he.p('x', 2), 'יומיים');
  assert.equal(he.p('x', 5), '5 ימים');
});

test('la langue : le choix, sinon le téléphone, sinon le français', () => {
  assert.equal(langueInitiale('en', ['fr-FR']), 'en');
  assert.equal(langueInitiale('', ['iw-IL', 'en']), 'he');
  assert.equal(langueInitiale('', ['de-DE', 'en-GB']), 'en');
  assert.equal(langueInitiale('', ['de-DE']), 'fr');
});

// --- Store : série, semaine, sauvegarde --------------------------------------------------
const store = (journal = {}, prefs = {}, maintenant = '2026-09-29T09:00:00') => {
  const s = memoire();
  s.setItem('myhitbodedut-journal', JSON.stringify({ days: journal }));
  s.setItem('myhitbodedut', JSON.stringify(prefs));
  return creeStore(s, { maintenant: () => new Date(maintenant), fuseau: 'Europe/Paris' });
};

test('la semaine va du dimanche au samedi, le samedi chômé', () => {
  const w = store({ '2026-09-27': { minutes: 20, note: '' } }).currentWeek();
  assert.deepEqual(w.map((j) => j.date), ['2026-09-27', '2026-09-28', '2026-09-29', '2026-09-30', '2026-10-01', '2026-10-02', '2026-10-03']);
  assert.equal(w[0].done, true);
  assert.equal(w[6].exempt, true);
  assert.equal(w[3].future, true);
});

test('la série ne casse ni sur un jour exempté ni sur un aujourd’hui encore à faire', () => {
  // Samedi 26/09 (Souccot, chômé) entre vendredi 25 et dimanche 27.
  const s = store({ '2026-09-25': { minutes: 10, note: '' }, '2026-09-27': { minutes: 10, note: '' }, '2026-09-28': { minutes: 10, note: '' } });
  assert.equal(s.streak(), 3);
});

test('un fichier de sauvegarde Android se relit, et l’export garde le même format', () => {
  const s = store({ '2026-09-28': { minutes: 30, note: 'ici', themes: ['gratitude'] } });
  const android = {
    format: 'myhitbodedut', version: 1, exported_at: '2026-09-28T10:00:00Z',
    days: {
      '2026-09-28': { minutes: 45, note: '', themes: ['famille'] },
      '2026-09-20': { minutes: 60, note: 'là' },
    },
    settings: {
      window_start: 360, window_end: 660, snooze_minutes: 15, target_minutes: 30, pause_shabbat: true,
      in_israel: true, reminder_enabled: true, reminder_minutes: 420, music_enabled: false, music_volume: 40,
      music_track: 'niggoun_05', adresse: 'f', apparence_mode: 'sombre', apparence_fond: 'sable', apparence_accent: 'violet',
    },
    paused_dates: ['2026-09-30'],
    blocked_apps: ['com.exemple'],
  };
  assert.equal(s.importJson(JSON.stringify(android)), 2);
  // Conflit : la séance la plus longue, la note non vide, les sujets réunis.
  assert.deepEqual(s.record('2026-09-28'), { date: '2026-09-28', minutes: 45, note: 'ici', themes: ['gratitude', 'famille'] });
  assert.equal(s.targetMinutes(), 30);
  assert.equal(s.adresse(), 'f');
  assert.equal(s.fond(), 'sable');
  assert.equal(s.isExempt('2026-09-30'), true);

  const exporte = JSON.parse(s.exportJson());
  assert.equal(exporte.format, 'myhitbodedut');
  assert.equal(exporte.version, 1);
  assert.deepEqual(Object.keys(exporte.settings).sort(), Object.keys(android.settings).sort());
  // Ce que l'iPhone ne sait pas régler repart tel qu'il est arrivé.
  assert.deepEqual([exporte.settings.window_start, exporte.settings.reminder_enabled], [360, true]);
  assert.deepEqual(exporte.blocked_apps, ['com.exemple']);
  // Une journée sans sujet n'a pas de clé « themes ».
  assert.equal('themes' in exporte.days['2026-09-20'], false);
});

test('un fichier qui n’est pas une sauvegarde est refusé', () => {
  const s = store();
  assert.equal(s.importJson('pas du json'), null);
  assert.equal(s.importJson('{"format":"autre"}'), null);
});
