// Les tests Android des mêmes règles, portés un à un :
// RegleLangueTest, TraductionAffichageTest, JaugeSemaineTest, HolidaysTest,
// DureeSeanceTest, BackupRulesTest (format de sauvegarde).

import { test } from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { Anneaux, CycleTehilim, DureeSeance, JaugeSemaine, LibraryRules, Plaque, Reference, RegleLangue, Salutation, SiddurMoment, SignetsSiddur } from '../src/js/regles.js';
import { estJourChome, israelParDefaut } from '../src/js/calendrier.js';
import { creeStore, dateValide, memoire } from '../src/js/store.js';
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
      window_start: 360, window_end: 660, snooze_minutes: 15, target_minutes: 30, daily_goal_minutes: 0, pause_shabbat: true,
      in_israel: true, reminder_enabled: true, reminder_minutes: 420, music_enabled: false, music_volume: 40,
      music_track: 'niggoun_05', adresse: 'f', apparence_mode: 'sombre', apparence_fond: 'sable', apparence_accent: 'violet',
    },
    paused_dates: ['2026-09-30'],
    blocked_apps: ['com.exemple'],
  };
  assert.equal(s.importJson(JSON.stringify(android)), 2);
  // Conflit : la séance la plus longue, la note non vide, les sujets réunis.
  assert.deepEqual(s.record('2026-09-28'), { date: '2026-09-28', minutes: 45, note: 'ici', themes: ['gratitude', 'famille'], noteSeule: false, faite: true });
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

test('la salutation suit l\'heure : bonjour le jour, bonsoir le soir et la nuit', () => {
  assert.equal(Salutation.cle(7), 'hero_greeting_morning');
  assert.equal(Salutation.cle(11), 'hero_greeting_morning');
  assert.equal(Salutation.cle(12), 'hero_greeting_afternoon');
  assert.equal(Salutation.cle(17), 'hero_greeting_afternoon');
  assert.equal(Salutation.cle(18), 'hero_greeting_evening');
  assert.equal(Salutation.cle(22), 'hero_greeting_evening');
  assert.equal(Salutation.cle(3), 'hero_greeting_evening');
  assert.equal(Salutation.cle(5), 'hero_greeting_morning');
});

// --- Anneaux (AnneauxTest.kt) ------------------------------------------------------
test('les anneaux : le jour en minutes, le second tour au-delà de l\'objectif', () => {
  const sem = { faits: 4, dus: 6, fraction: 4 / 6 };
  const e = Anneaux.etat(8, 15, sem, 4);
  assert.equal(e.jour.fraction, 8 / 15);
  assert.equal(e.jour.atteint, false);
  assert.deepEqual([e.semaine.fait, e.semaine.objectif], [4, 6]);
  const au_dela = Anneaux.etat(23, 15, sem, 4);
  assert.equal(au_dela.jour.tours, 1);
  assert.ok(Math.abs(au_dela.jour.reste - 8 / 15) < 1e-9);
  assert.equal(au_dela.jour.atteint, true);
});

test('la série vise le premier palier qui l\'atteint', () => {
  for (const [serie, palier] of [[0, 7], [7, 7], [8, 14], [15, 30], [101, 180], [365, 365], [366, 730]]) {
    assert.equal(Anneaux.palier(serie), palier, `série ${serie}`);
  }
  const e = Anneaux.etat(0, 15, { faits: 0, dus: 0, fraction: 0 }, 7);
  assert.equal(e.serie.fraction, 1);
  assert.equal(Anneaux.etat(0, 15, { faits: 0, dus: 0, fraction: 0 }, 8).serie.fraction, 8 / 14);
});

test('sans objectif rien ne se remplit ; l\'objectif retombe sur la durée', () => {
  const e = Anneaux.etat(10, 0, { faits: 0, dus: 0, fraction: 0 }, 0);
  assert.equal(e.jour.fraction, 0);
  assert.equal(e.semaine.fraction, 0);
  assert.equal(Anneaux.objectifEffectif(0, 15), 15);
  assert.equal(Anneaux.objectifEffectif(20, 15), 20);
});

// --- Fiabilité 2 (01/10) : parité avec Store.kt ---------------------------------------
test('une note seule ne valide pas la journée et survit à un aller-retour', () => {
  const s = store({});
  s.setNote(s.today(), 'une pensée');
  assert.equal(s.isDone(), false);
  assert.equal(s.streak(), 0);
  assert.equal(s.record().noteSeule, true);
  // L'export la marque, et l'import la relit telle quelle.
  const exporte = JSON.parse(s.exportJson());
  assert.equal(exporte.days[s.today()].note_seule, true);
  const t = store({});
  t.importJson(JSON.stringify(exporte));
  assert.equal(t.isDone(), false);
  // Une hitbodedout le même jour la transforme en journée faite, note gardée.
  s.addSession(10);
  assert.equal(s.isDone(), true);
  assert.equal(s.record().note, 'une pensée');
  // Une note seule effacée disparaît.
  const u = store({});
  u.setNote(u.today(), 'x');
  u.setNote(u.today(), '');
  assert.equal(u.record(), null);
});

test('une séance compte pour le jour où elle a commencé', () => {
  // 23 h 40 à Paris, 20 minutes : finit le lendemain.
  const horloge = { t: new Date('2026-09-29T21:40:00Z').getTime() };
  const s = creeStore(memoire(), { maintenant: () => new Date(horloge.t), fuseau: 'Europe/Paris' });
  s.beginSession(20);
  horloge.t += 40 * 60000;
  const close = s.endSession();
  assert.equal(close.minutes, 20);
  assert.equal(close.date, '2026-09-29');
  s.addSession(close.minutes, { date: close.date });
  assert.equal(s.isDone('2026-09-29'), true);
  assert.equal(s.isDone('2026-09-30'), false);
});

test('une séance ouverte oubliée est plafonnée', () => {
  const horloge = { t: new Date('2026-09-29T10:00:00Z').getTime() };
  const s = creeStore(memoire(), { maintenant: () => new Date(horloge.t), fuseau: 'Europe/Paris' });
  s.beginSession(0);
  horloge.t += 20 * 60 * 60000;
  assert.equal(s.endSession({ avantTerme: true }).minutes, 240);
});

test('l\'import rejette les dates fausses ou à venir et borne les valeurs', () => {
  const s = store({});
  const n = s.importJson(JSON.stringify({
    format: 'myhitbodedut', version: 1,
    days: { '2026-13-45': { minutes: 10 }, 'x': { minutes: 10 }, '2099-01-01': { minutes: 10 }, '2026-09-20': { minutes: -5, note: 'a'.repeat(5000) } },
    settings: { target_minutes: 0, music_volume: 500, snooze_minutes: 15 },
    paused_dates: ['2026-02-30', '2026-09-21'],
  }));
  assert.equal(n, 1);
  assert.equal(s.record('2026-09-20').minutes, 0);
  assert.equal(s.record('2026-09-20').note.length, 4000);
  assert.equal(s.targetMinutes(), 15);
  assert.equal(s.musicVolume(), 60);
  assert.equal(s.isExempt('2026-09-21'), true);
  assert.equal(s.isExempt('2026-02-30'), false);
  assert.equal(dateValide('2026-02-30'), false);
  assert.equal(dateValide('2026-09-29'), true);
});

// --- La fête des anneaux (Anneaux.aFeter), comme AnneauxTest -------------------
test('un anneau fermé se fête une fois, puis le lendemain', () => {
  const plein = Anneaux.etat(15, 15, { faits: 4, dus: 6 }, 4);
  assert.deepEqual(Anneaux.aFeter(plein, '2026-10-07', new Set()), ['jour']);
  const deja = Anneaux.marques(plein, '2026-10-07');
  assert.deepEqual(Anneaux.aFeter(plein, '2026-10-07', deja), []);
  assert.deepEqual(Anneaux.aFeter(plein, '2026-10-08', deja), ['jour']);
});

test('la semaine se fête par semaine, la série par palier', () => {
  const e = Anneaux.etat(0, 15, { faits: 6, dus: 6 }, 7);
  assert.deepEqual(Anneaux.aFeter(e, '2026-10-07', new Set()), ['semaine', 'serie']);
  const deja = Anneaux.marques(e, '2026-10-07');
  assert.ok(!Anneaux.aFeter(e, '2026-10-08', deja).includes('semaine'));
  assert.ok(Anneaux.aFeter(e, '2026-10-11', deja).includes('semaine'));
  assert.deepEqual(Anneaux.aFeter(Anneaux.etat(0, 15, { faits: 0, dus: 0 }, 14), '2026-10-07', deja), ['serie']);
});

// --- Le centre des anneaux (PlaqueTest.kt) ---------------------------------------
test('les faces se suivent et reviennent au hassid', () => {
  let face = 'hassid';
  const vues = [];
  for (let i = 0; i < Plaque.FACES.length; i++) { face = Plaque.suivante(face); vues.push(face); }
  assert.equal(vues[0], 'jour');
  assert.equal(vues.at(-1), 'hassid');
  assert.equal(new Set(vues).size, Plaque.FACES.length);
  assert.equal(Plaque.pour('serie'), 'serie');
  assert.equal(Plaque.anneau('meilleure'), 'serie');
  assert.equal(Plaque.anneau('total'), null);
});

test('la meilleure série ne casse pas sur un jour exempté', () => {
  const faits = new Set(['2026-09-28', '2026-09-29', '2026-09-30', '2026-10-02', '2026-10-04']);
  assert.equal(Plaque.meilleureSerie(faits, (d) => d === '2026-10-01'), 4);
  assert.equal(Plaque.meilleureSerie(faits, () => false), 3);
  assert.equal(Plaque.meilleureSerie(new Set(), () => false), 0);
  assert.equal(Plaque.moyenne(50, 3), 17);
  assert.equal(Plaque.moyenne(0, 0), 0);
});

// --- Tehilim (TehilimTest.kt) ---------------------------------------------------------
test('le cycle du mois couvre tout le livre, 119 coupé en deux', () => {
  assert.equal(CycleTehilim.MOIS.length, 30);
  assert.deepEqual(CycleTehilim.duMois(1, 30), CycleTehilim.portion(1, 9));
  assert.deepEqual(CycleTehilim.duMois(25, 30), CycleTehilim.portion(119, 119, 1, 96));
  assert.deepEqual(CycleTehilim.duMois(30, 30), CycleTehilim.portion(145, 150));
  assert.deepEqual(CycleTehilim.duMois(29, 29), CycleTehilim.portion(140, 150));
  assert.deepEqual(CycleTehilim.duMois(29, 30), CycleTehilim.portion(140, 144));
  const entiers = CycleTehilim.MOIS.filter((p) => !CycleTehilim.partielle(p));
  const psaumes = entiers.flatMap((p) => Array.from({ length: p.fin - p.debut + 1 }, (_, i) => p.debut + i));
  assert.deepEqual(psaumes, Array.from({ length: 150 }, (_, i) => i + 1).filter((n) => n !== 119));
});

test('le cycle de la semaine va du dimanche au Shabbat', () => {
  assert.deepEqual(CycleTehilim.deLaSemaine(0), CycleTehilim.portion(1, 29));
  assert.deepEqual(CycleTehilim.deLaSemaine(6), CycleTehilim.portion(120, 150));
  CycleTehilim.SEMAINE.slice(1).forEach((p, i) => assert.equal(p.debut, CycleTehilim.SEMAINE[i].fin + 1));
  assert.deepEqual(CycleTehilim.versets(CycleTehilim.portion(119, 119, 97, 176), 119, 176).length, 80);
  assert.deepEqual(CycleTehilim.versets(CycleTehilim.portion(1, 9), 1, 6), [1, 2, 3, 4, 5, 6]);
});

// --- SignetsSiddur (SiddurTest) ------------------------------------------------------
test('le signet suit la section qu’on lit, comme sur Android', () => {
  const debuts = [0, 11, 14, 40];
  assert.equal(SignetsSiddur.courant(debuts, 0, 6, false), 0);
  assert.equal(SignetsSiddur.courant(debuts, 10, 13, false), 0);
  assert.equal(SignetsSiddur.courant(debuts, 11, 16, false), 1);
  assert.equal(SignetsSiddur.courant(debuts, 39, 45, false), 2);
  assert.equal(SignetsSiddur.courant(debuts, 40, 45, false), 3);
  assert.equal(SignetsSiddur.courant(debuts, 35, 45, true), 3);
  assert.equal(SignetsSiddur.courant([3, 9], 1, 4, false), 0);
});

// --- SiddurMoment (SiddurMomentTest) -----------------------------------------------
test('le siddour suggère le texte de l’heure, aux bornes d’Android', () => {
  const cas = [[0, 0, 'tikoun_hatsot'], [4, 59, 'tikoun_hatsot'], [5, 0, 'birkot_hachahar'], [6, 29, 'birkot_hachahar'],
    [6, 30, 'chaharit'], [12, 29, 'chaharit'], [12, 30, 'minha'], [18, 29, 'minha'], [18, 30, 'arvit'],
    [22, 29, 'arvit'], [22, 30, 'chema_coucher'], [23, 59, 'chema_coucher']];
  for (const [h, m, id] of cas) assert.equal(SiddurMoment.suggestion(h, m), id, `${h}:${m}`);
  assert.equal(SiddurMoment.illustration('chaharit'), 'char_tefillin');
  assert.equal(SiddurMoment.illustration('chema_coucher'), 'scene_sleeping');
  // Chaque suggestion existe dans chacun des trois siddourim, et son hassid aussi.
  for (const n of ['edot', 'sefard', 'ashkenaz']) {
    const ids = new Set(JSON.parse(readFileSync(join(RES, 'raw', `siddur_${n}.json`), 'utf8')).textes.map((t) => t.id));
    for (const [h, m] of cas) assert.ok(ids.has(SiddurMoment.suggestion(h, m)), `${n} : ${SiddurMoment.suggestion(h, m)}`);
  }
  for (const [h, m] of cas) {
    const img = SiddurMoment.illustration(SiddurMoment.suggestion(h, m));
    assert.ok(readFileSync(join(RES, 'drawable-nodpi', `${img}.webp`)).length > 0);
  }
});

// --- LibraryRules (LibraryRulesTest) ------------------------------------------------
test('une source sans licence de vocalisation se lit sans nikkoud', () => {
  const lib = { _meta: { editions: { ok: { nikkoud_ok: true }, non: { nikkoud_ok: false } } } };
  const s = { paragraphes: ['בְּ'], paragraphes_sans_nikkoud: ['ב'] };
  assert.deepEqual(LibraryRules.paragraphes(lib, { ...s, edition: 'ok' }), ['בְּ']);
  assert.deepEqual(LibraryRules.paragraphes(lib, { ...s, edition: 'non' }), ['ב']);
  assert.deepEqual(LibraryRules.paragraphes(lib, { ...s, edition: 'inconnue' }), ['ב']);
  // Faute de texte sans nikkoud, le texte tel quel.
  assert.deepEqual(LibraryRules.paragraphes(lib, { paragraphes: ['בְּ'], edition: 'non' }), ['בְּ']);
});

test('la bibliothèque du web ne montre que des sources, chacune avec du texte', () => {
  const lib = JSON.parse(readFileSync(join(RES, 'raw', 'bibliotheque.json'), 'utf8'));
  const sources = LibraryRules.sources(lib).flatMap((s) => s.sous_sections.flatMap((ss) => ss.entrees));
  assert.ok(sources.length > 10);
  for (const e of sources) {
    assert.equal(e.type, 'source');
    assert.ok(LibraryRules.paragraphes(lib, e).length > 0, e.id);
    assert.equal(LibraryRules.entree(lib, e.id), e);
  }
  assert.equal(LibraryRules.entree(lib, 'inexistante'), null);
});

test('le Tikoun dit d’un trait marque les dix psaumes, pour le jour seulement', () => {
  const horloge = { t: new Date('2026-10-04T10:00:00').getTime() };
  const s = creeStore(memoire(), { maintenant: () => new Date(horloge.t), fuseau: 'Europe/Paris' });
  assert.equal(s.tikkunDone().size, 0);
  for (const n of [150, 16, 32]) s.setTikkunDone(n, true);
  assert.deepEqual([...s.tikkunDone()].sort((a, b) => a - b), [16, 32, 150]);
  s.setTikkunDone(32, false);
  assert.deepEqual([...s.tikkunDone()].sort((a, b) => a - b), [16, 150]);
  horloge.t += 86400000;
  assert.equal(s.tikkunDone().size, 0);
});

// La carte du jour (11/10) : l'illustration de fête, la hilloula, les liens.
test('carte du jour : illustration, hilloula et liens', async () => {
  const J = await import('../src/js/jour-affiche.js');
  assert.equal(J.illustration(['soukkot'], '2026-09-26'), 'scene_sukkot');
  assert.equal(J.illustration(['hanouka'], '2026-12-05'), 'scene_hanukkah');
  assert.equal(J.illustration([], '2026-10-10'), 'char_shabbat');
  assert.equal(J.illustration([], '2026-10-11'), null);
  // 5784 et 5787 sont embolismiques, 5786 ne l'est pas.
  assert.deepEqual([5784, 5786, 5787].map(J.embolismique), [true, false, true]);
  assert.deepEqual(J.clesHilloula(12, 7, false, false), ['adar-7', 'adar1-7', 'adar2-7']);
  assert.deepEqual(J.clesHilloula(12, 7, true, false), ['adar1-7']);
  assert.deepEqual(J.clesHilloula(13, 7, true, false), ['adar-7', 'adar2-7']);
  assert.deepEqual(J.clesHilloula(8, 29, false, true), ['8-29', '8-30']);
  const table = JSON.parse(readFileSync(join(RES, 'raw', 'hilloulot.json'), 'utf8')).jours;
  // Chaque jour d'une année simple (5786) a au moins une hilloula ; en année
  // embolismique, Adar I n'a que les hilloulot dites « Adar I ».
  let iso = '2025-09-23';
  while (iso < '2026-09-12') {
    assert.ok(J.hilloulotPour(table, iso, false).length > 0, iso);
    const [a, m, d] = iso.split('-').map(Number);
    iso = new Date(Date.UTC(a, m - 1, d + 1, 12)).toISOString().slice(0, 10);
  }
  // 7 Adar II 5787 (16/03/2027) : les hilloulot d'Adar sans précision.
  assert.ok(J.hilloulotPour(table, '2027-03-16', false).length > 10);
  // 30 Tichri 5787 (11/10/2026) : Rabbi Tsvi Hirsch Chajes, donné par Wikipédia.
  assert.ok(J.hilloulotPour(table, '2026-10-11', false).some((x) => x.he.includes('צבי הירש חיות')));
  assert.equal(J.lienHyomi('2026-10-11'), 'https://hyomi.org.il/day.php?month1=0&dd=11&mm=10&yy=2026');
  assert.equal(J.lienHalakha('iw'), 'https://halachayomit.co.il/he/default.aspx');
  assert.equal(J.lienHalakha('fr'), 'https://halachayomit.co.il/fr/default.aspx');
  for (const img of ['scene_sukkot', 'char_matsa', 'scene_hanukkah', 'char_purim', 'char_shabbat', 'scene_nature', 'scene_sleeping', 'char_tefillin']) {
    assert.ok(readFileSync(join(RES, 'drawable-nodpi', `${img}.webp`)).length > 0, img);
  }
});

// Les paliers d'objectifs (11/10), comme PaliersTest.
test('paliers : après chaque palier, le suivant', async () => {
  const P = await import('../src/js/paliers.js');
  const e = P.etat('serie', 7);
  assert.deepEqual([e.atteint, e.prochain, e.reste, e.rang], [7, 14, 7, 2]);
  assert.equal(P.etat('serie', 400).prochain, 730);
  assert.equal(P.etat('cumul', 61000).prochain, 72000);
  assert.equal(P.etat('jour', 21, 25).prochain, 25);
  const bilan = { aujourdhui: '2026-10-14', minutes: { '2026-10-10': 20, '2026-10-12': 30, '2026-10-14': 15 }, serie: 2, debutMoisHebreu: '2026-10-12', objectifJour: 20 };
  const l = Object.fromEntries(P.etats(bilan).map((x) => [x.piste, x.valeur]));
  assert.deepEqual(l, { serie: 2, jour: 15, semaine: 45, mois: 45, cumul: 65, jours: 3 });
  const m = P.marques(P.etats(bilan), bilan);
  assert.ok(m.has('jour:2026-10-14:15'));
  assert.deepEqual(P.nouveaux(P.etats(bilan), bilan, new Set()).map((x) => x.piste).sort(), ['cumul', 'jour', 'jours', 'semaine']);
  assert.deepEqual(P.nouveaux(P.etats(bilan), bilan, m), []);
  const r = P.records({ '2026-10-04': 30, '2026-10-05': 40, '2026-10-12': 20 }, (d) => (d < '2026-10-12' ? 't' : 'h'));
  assert.deepEqual(r, { jour: 40, semaine: 70, mois: 70 });
  assert.equal(P.aLaUne([P.etat('serie', 6), P.etat('jour', 5)]).piste, 'serie');
});
