// Le calendrier hébraïque, les ajouts de prière et les zmanim du web (10/10).
//
// Trois familles de vérifications, séparées exprès :
//  1. fidélité du portage : le web répond exactement ce que le code Android
//     (KosherJava 2.5.0) répond, jour par jour de 2025 à 2030, en Israël et
//     en diaspora, et à une seconde près pour les zmanim ;
//  2. comparaison avec Hebcal (@hebcal/core 6.14.1, dépendance de test
//     seulement, jamais servie) : là où les méthodes sont réellement les mêmes,
//     les deux moteurs doivent s'accorder ; là où elles diffèrent, l'écart est
//     nommé et borné ici. L'accord de deux moteurs n'est pas une preuve
//     d'exactitude ;
//  3. la règle d'Arvit le soir.
// Les validations halakhiques, elles, reviennent à un Rav (ZMANIM.md).

import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import test from 'node:test';
import { fileURLToPath } from 'node:url';
import { flags, HDate, HebrewCalendar, tachanun, GeoLocation as HGeo, Zmanim as HZmanim } from '@hebcal/core';
import { jour, jourDeLaPriere, TOUTES } from '../src/js/calendrier-juif.js';
import * as Z from '../src/js/zmanim.js';

const ICI = dirname(fileURLToPath(import.meta.url));
const VECTEURS = JSON.parse(readFileSync(join(ICI, 'vecteurs_kosherjava.json'), 'utf8'));

// --- 1. Fidélité au code Android ---------------------------------------------------
test('chaque jour de 2025 à 2030 : la même réponse que KosherJava', () => {
  let n = 0;
  for (const [cle, attendu] of Object.entries(VECTEURS.jours)) {
    const [iso, il] = cle.split('/');
    const j = jour(iso, il === 'il');
    assert.deepEqual({
      h: [j.jourHebreu, j.moisHebreu, j.anneeHebraique], t: j.dateHebraique, f: j.fetes, o: j.omer,
      p: j.paracha, x: j.chome, c: TOUTES.filter((c) => j.conditions.has(c)),
    }, attendu, cle);
    n++;
  }
  assert.equal(n, 3896);
});

test('zmanim : la même heure que KosherJava, à une seconde près, et les mêmes absences', () => {
  for (const r of VECTEURS.zmanim) {
    const lieu = { nom: r.lieu, latitude: r.lat, longitude: r.lon, altitude: 0, fuseau: r.fuseau };
    for (const z of [...Z.jour(lieu, r.date), Z.allumage(lieu, r.date, 18)]) {
      const attendu = r[z.cle];
      const ou = `${r.lieu} ${r.date} ${z.cle}`;
      assert.notEqual(z.statut, 'erreur', ou);
      if (attendu === 'ABSENT') { assert.equal(z.statut, 'absent', ou); continue; }
      assert.equal(z.statut, 'ok', ou);
      assert.ok(Math.abs(+z.instant - Date.parse(attendu)) <= 1000, `${ou} : ${z.instant.toISOString()} contre ${attendu}`);
    }
  }
});

test('le profil web est celui d’Android, dans le même ordre', () => {
  const kotlin = readFileSync(join(ICI, '..', '..', 'android', 'app', 'src', 'main', 'java', 'com', 'myhitbodedut', 'Zmanim.kt'), 'utf8');
  const cles = [...kotlin.matchAll(/^\s+"([a-z_]+)" to Methode\(/gm)].map(([, c]) => c);
  assert.deepEqual(Z.CLES, cles);
});

// --- 2. Hebcal, moteur de comparaison ------------------------------------------------
function chaqueJour(f) {
  for (let t = Date.UTC(2025, 8, 1); t <= Date.UTC(2030, 11, 31); t += 864e5) {
    const iso = new Date(t).toISOString().slice(0, 10);
    const [a, m, d] = iso.split('-').map(Number);
    for (const il of [false, true]) f(iso, il, new HDate(new Date(a, m - 1, d)), new Date(t).getUTCDay());
  }
}

test('Hebcal : mêmes dates hébraïques, mêmes Roch ’Hodech, mêmes jeûnes publics', () => {
  chaqueJour((iso, il, hd) => {
    const j = jour(iso, il);
    assert.deepEqual([j.jourHebreu, j.anneeHebraique], [hd.getDate(), hd.getFullYear()], iso);
    const ev = HebrewCalendar.getHolidaysOnDate(hd, il) ?? [];
    assert.equal(j.fetes.includes('roch_hodech'), ev.some((e) => e.getFlags() & flags.ROSH_CHODESH), `${iso} ${il}`);
    // Hebcal range aussi parmi les jeûnes mineurs les jeûnes volontaires
    // (BaHaB, Yom Kippour katan, jeûne des premiers-nés), et la veille de Tich'a bèAv dont le jeûne ne commence que le soir : ils n'appellent pas Anénou dans le siddour.
    const publics = ev.filter((e) => (e.getFlags() & (flags.MAJOR_FAST | flags.MINOR_FAST))
      && !/BeHaB|Katan|Bechorot|^Erev/.test(e.getDesc()));
    assert.equal(j.conditions.has('jeune') || j.fetes.includes('yom_kippour'), publics.length > 0, `${iso} ${il}`);
  });
});

test('Hebcal : Ta’hanoun, les écarts sont des coutumes, sauf le 15 Adar I, consigné', () => {
  const coutumes = [];
  const tranches = [];
  chaqueJour((iso, il, hd, semaine) => {
    if (semaine === 6) return;
    const h = tachanun(hd, il);
    const nous = jour(iso, il).conditions.has('tahanoun_chaharit');
    if (h.shacharit === nous) return;
    if (!h.allCongs) coutumes.push(iso);
    else tranches.push(`${iso} ${hd.getMonthName()} ${hd.getDate()}`);
  });
  // Hebcal dit lui-même « certaines communautés » pour chacun de ces jours.
  assert.ok(coutumes.length > 0 && coutumes.length < 100, `${coutumes.length}`);
  // Un seul vrai désaccord : Chouchan Pourim katan. KosherJava, moteur
  // retenu, suit le Choul'han Aroukh (OC 697:1 : ni Ta'hanoun le 14 ni le 15
  // Adar I) ; Hebcal le fait dire partout. Le résultat de KosherJava est
  // gardé, l'écart est écrit dans ZMANIM.md.
  assert.deepEqual([...new Set(tranches.map((t) => t.split(' ').slice(1).join(' ')))], ['Adar I 15']);
});

const LIEUX = [
  { nom: 'Jérusalem', latitude: 31.76904, longitude: 35.21633, altitude: 0, fuseau: 'Asia/Jerusalem' },
  { nom: 'Paris', latitude: 48.85341, longitude: 2.3488, altitude: 0, fuseau: 'Europe/Paris' },
  { nom: 'New York', latitude: 40.71427, longitude: -74.00597, altitude: 0, fuseau: 'America/New_York' },
  { nom: 'Melbourne', latitude: -37.814, longitude: 144.96332, altitude: 0, fuseau: 'Australia/Melbourne' },
];

test('Hebcal : les zmanim de même méthode s’accordent à 5 secondes près', () => {
  // [clé du profil, méthode Hebcal réellement équivalente]
  const paires = [
    ['netz', (h) => h.seaLevelSunrise()], ['chkia', (h) => h.seaLevelSunset()],
    ['alot', (h) => h.alotHaShachar()], ['misheyakir', (h) => h.misheyakir()],
    ['szks_gra', (h) => h.sofZmanShma()], ['szks_mga', (h) => h.sofZmanShmaMGA()],
    ['szt_gra', (h) => h.sofZmanTfilla()], ['szt_mga', (h) => h.sofZmanTfillaMGA()],
    ['minha_gedola', (h) => h.minchaGedola()], ['minha_ketana', (h) => h.minchaKetana()],
    ['plag', (h) => h.plagHaMincha()], ['tset', (h) => h.tzeit(8.5)], ['tset_rt', (h) => h.tzeit72()],
  ];
  for (const lieu of LIEUX) {
    for (const iso of ['2026-01-15', '2026-03-29', '2026-06-21', '2026-10-25', '2026-12-21']) {
      const [a, m, d] = iso.split('-').map(Number);
      const h = new HZmanim(new HGeo(lieu.nom, lieu.latitude, lieu.longitude, 0, lieu.fuseau), new Date(a, m - 1, d), false);
      const z = Object.fromEntries(Z.jour(lieu, iso).map((x) => [x.cle, x]));
      for (const [cle, f] of paires) {
        const v = f(h);
        // tzeit72 rend un Temporal.ZonedDateTime, les autres une Date.
        const ecart = Math.abs(+z[cle].instant - (v instanceof Date ? +v : v.epochMilliseconds)) / 1000;
        assert.ok(ecart <= 5, `${lieu.nom} ${iso} ${cle} : ${ecart} s`);
      }
      // ’Hatsot n'est PAS la même méthode : Hebcal prend le milieu du lever
      // et du coucher, KosherJava 2.5.0 le passage au méridien. Jamais de
      // moyenne : on vérifie seulement que l'écart reste celui de la
      // définition (moins de deux minutes à ces latitudes).
      const milieu = (+z.netz.instant + +z.chkia.instant) / 2;
      assert.ok(Math.abs(milieu - +h.chatzot()) <= 2000, `${lieu.nom} ${iso} milieu du jour`);
      assert.ok(Math.abs(+z.chatsot.instant - +h.chatzot()) < 120_000, `${lieu.nom} ${iso} chatsot`);
    }
  }
});

// --- 3. Arvit le soir ------------------------------------------------------------------
test('Arvit et le Chema du coucher relèvent du jour hébreu suivant dès l’après-midi', () => {
  assert.equal(jourDeLaPriere('arvit', '2026-10-10', 19), '2026-10-11');
  assert.equal(jourDeLaPriere('chema_coucher', '2026-12-31', 23), '2027-01-01');
  assert.equal(jourDeLaPriere('arvit', '2026-10-10', 0), '2026-10-10');
  assert.equal(jourDeLaPriere('minha', '2026-10-10', 17), '2026-10-10');
  // Samedi soir : le dimanche, d'où la section de motsaé Chabbat.
  assert.ok(jour(jourDeLaPriere('arvit', '2026-10-10', 20), false).conditions.has('motsae_chabbat'));
});

test('soleil de minuit à Tromsø : absent, jamais en erreur', () => {
  const tromso = { nom: 'Tromsø', latitude: 69.6492, longitude: 18.9553, altitude: 0, fuseau: 'Europe/Oslo' };
  const z = Object.fromEntries(Z.jour(tromso, '2026-06-21').map((x) => [x.cle, x]));
  assert.equal(z.netz.statut, 'absent');
  assert.equal(z.chkia.statut, 'absent');
  assert.ok(Object.values(z).every((x) => x.statut !== 'erreur'));
});

// --- 4. Les annotations du siddour ---------------------------------------------------
test('siddour : chaque annotation désigne encore son bloc, et chaque libellé existe', async () => {
  const { empreinte, libelle, vrai, DERIVEES } = await import('../src/js/siddur-jour.js');
  const raw = join(ICI, '..', '..', 'android', 'app', 'src', 'main', 'res');
  const annotations = JSON.parse(readFileSync(join(raw, 'raw', 'siddur_annotations.json'), 'utf8'));
  const chaines = readFileSync(join(raw, 'values', 'strings.xml'), 'utf8');
  for (const nusah of ['edot', 'sefard', 'ashkenaz']) {
    const siddur = JSON.parse(readFileSync(join(raw, 'raw', `siddur_${nusah}.json`), 'utf8'));
    for (const [id, liste] of Object.entries(annotations[nusah])) {
      const texte = siddur.textes.find((t) => t.id === id);
      for (const a of liste) {
        const section = texte.sections[a.s];
        const vu = a.b === undefined ? empreinte(section.titre) : empreinte(section.blocs[a.b].t ?? section.blocs[a.b].c);
        assert.equal(vu, a.empreinte, `${nusah}/${id} §${a.s}/${a.b}`);
        assert.ok([...TOUTES, ...DERIVEES].includes(a.si.replace(/^!/, '')), a.si);
        assert.ok(chaines.includes(`name="${libelle(a.si)}"`), libelle(a.si));
        if (a.libelle) assert.ok(chaines.includes(`name="${a.libelle}"`), a.libelle);
      }
    }
  }
  assert.equal(vrai('!tahanoun_minha', new Set(['chabbat'])), true);
  assert.equal(vrai('!tahanoun_minha', new Set(['tahanoun_minha'])), false);
  // Anénou (Rema OC 565:3) : l'officiant dans les trois noussa'him ; le
  // particulier à Cha'harit seulement dans le siddour Edot.
  const anenou = (nusah, id) => annotations[nusah][id].map((a) => a.libelle).filter((l) => l?.startsWith('ajout_anenou'));
  for (const nusah of ['edot', 'sefard', 'ashkenaz']) {
    assert.ok(anenou(nusah, 'chaharit').includes('ajout_anenou_officiant'), nusah);
    assert.ok(anenou(nusah, 'minha').includes('ajout_anenou_officiant') && anenou(nusah, 'minha').includes('ajout_anenou_particulier'), nusah);
    assert.equal(anenou(nusah, 'chaharit').includes('ajout_anenou_particulier'), nusah === 'edot', nusah);
  }
  // Roch 'Hodech (12/10) : Cha'harit renvoie au Hallel puis au Moussaf, textes présents dans chaque siddour.
  const { index } = await import('../src/js/siddur-jour.js');
  for (const nusah of ['edot', 'sefard', 'ashkenaz']) {
    const siddur = JSON.parse(readFileSync(join(raw, 'raw', `siddur_${nusah}.json`), 'utf8'));
    const renvois = [...index(annotations[nusah].chaharit).renvois.values()].flat();
    assert.deepEqual(renvois.map((a) => a.cible), ['hallel', 'moussaf_rosh_hodech'], nusah);
    for (const a of renvois) assert.ok(siddur.textes.some((t) => t.id === a.cible), `${nusah} ${a.cible}`);
  }
});

// --- 5. L'onglet Zmanim et le calendrier (JourAfficheTest) ---------------------------
test('JourAffiche : allumage, sortie, ajouts nommés et grille, comme Android', async () => {
  const A = await import('../src/js/jour-affiche.js');
  const parIndex = (l) => Object.fromEntries(l.map((z) => [z.cle, z]));
  const jer = LIEUX[0];
  const paris = LIEUX[1];
  const vendredi = parIndex(A.horaires(jer, '2026-10-16', true, 18));
  assert.equal((+vendredi.chkia.instant - +vendredi.allumage.instant) / 1000, 18 * 60);
  assert.ok(!('sortie' in vendredi));
  const samedi = parIndex(A.horaires(jer, '2026-10-17', true, 18));
  assert.equal(+samedi.sortie.instant, +samedi.tset.instant);
  const rh1 = parIndex(A.horaires(paris, '2026-09-12', false, 18));
  assert.equal(rh1.allumage.methode, 'apres_tset');
  assert.ok('sortie' in parIndex(A.horaires(paris, '2026-09-13', false, 18)));
  assert.deepEqual(A.ajouts(jour('2026-10-12', false).conditions),
    ['ajout_machiv_harouah', 'ajout_barkhenou', 'ajout_yaale_veyavo', 'ajout_hallel_partiel_sans_berakha', 'ajout_sans_tahanoun']);
  // Les Edot disent ברכנו / ברך עלינו ; les autres noussa'h ותן ברכה / ותן טל ומטר.
  assert.ok(A.ajouts(jour('2026-10-12', false).conditions, 'ashkenaz').includes('ajout_berakha'));
  assert.ok(A.ajouts(jour('2026-12-07', false).conditions).includes('ajout_barekh_alenou'));
  assert.ok(!A.ajouts(jour('2026-10-10', false).conditions).some((k) => ['ajout_barkhenou', 'ajout_berakha'].includes(k)));
  // Le nusah (JourAfficheTest) : bénédiction du Hallel partiel (OC 422:2),
  // Morid hatal ashkénaze hors d'Israël, Anénou du particulier (OC 565:3).
  const rc = jour('2026-10-12', false).conditions;
  assert.ok(A.ajouts(rc, 'sefard').includes('ajout_hallel_partiel_avec_berakha'));
  assert.ok(A.ajouts(rc, 'ashkenaz').includes('ajout_hallel_partiel_avec_berakha'));
  const ete = jour('2027-06-01', false).conditions;
  assert.ok(A.ajouts(ete, 'edot').includes('ajout_morid_hatal'));
  assert.ok(A.ajouts(ete, 'sefard').includes('ajout_morid_hatal'));
  assert.ok(!A.ajouts(ete, 'ashkenaz').includes('ajout_morid_hatal'));
  assert.ok(A.ajouts(ete, 'ashkenaz', true).includes('ajout_morid_hatal'));
  const jeune = jour('2026-12-20', false).conditions;
  assert.deepEqual(A.ajouts(jeune, 'edot').filter((k) => k.startsWith('ajout_anenou')), ['ajout_anenou_officiant', 'ajout_anenou_particulier_matin_minha']);
  assert.deepEqual(A.ajouts(jeune, 'ashkenaz').filter((k) => k.startsWith('ajout_anenou')), ['ajout_anenou_officiant', 'ajout_anenou_particulier_minha']);
  const av = jour('2027-08-12', false).conditions; // Tich'a bèAv 5787
  assert.ok(av.has('ticha_beav'));
  assert.deepEqual(A.ajouts(av, 'edot').filter((k) => k.startsWith('ajout_anenou')), ['ajout_anenou_officiant', 'ajout_anenou_particulier_trois']);
  assert.deepEqual(A.ajouts(av, 'sefard').filter((k) => k.startsWith('ajout_anenou')), ['ajout_anenou_officiant', 'ajout_anenou_particulier_minha']);
  // Veille de Roch Hachana (OC 581:3) ; fin de Tichri, Edot sans Ta'hanoun.
  assert.ok(!jour('2026-09-11', true).conditions.has('tahanoun_chaharit'));
  assert.ok(jour('2026-09-10', true).conditions.has('tahanoun_minha'));
  const { conditionsPour } = await import('../src/js/siddur-jour.js');
  const fin = jour('2026-10-05', true).conditions;
  assert.ok(fin.has('fin_tichri') && fin.has('tahanoun_chaharit'));
  assert.ok(!conditionsPour('edot', true, fin).has('tahanoun_chaharit'));
  assert.ok(!A.ajouts(fin, 'edot').includes('ajout_tahanoun'));
  assert.ok(A.ajouts(fin, 'ashkenaz').includes('ajout_tahanoun'));
  assert.ok(A.ajouts(fin, 'sefard', true).includes('ajout_tahanoun'));
  assert.ok(!A.ajouts(fin, 'sefard', true, true).includes('ajout_tahanoun'));
  assert.ok(conditionsPour('edot', true, jour('2026-10-13', true).conditions).has('tahanoun_chaharit'));
  // Avinou malkénou (14/10) : 10 Tévèt, jour des dix jours de pénitence, Roch Hachana un dimanche ;
  // jamais un jour ordinaire, Roch Hachana un Chabbat, le Chabbat Chouva, la veille de Kippour ni le 9 Av.
  const am = (d) => conditionsPour('ashkenaz', true, jour(d, true).conditions).has('avinou_malkenou');
  for (const d of ['2026-12-20', '2026-09-15', '2026-09-13', '2026-09-14']) assert.ok(am(d), d);
  for (const d of ['2026-12-21', '2026-09-12', '2026-09-19', '2026-09-20', '2027-08-12']) assert.ok(!am(d), d);
  assert.ok(jour('2027-08-12', true).conditions.has('ticha_beav'));
  // Lecture de la Torah (15/10) : lundi, jeudi, Chabbat, Roch 'Hodech, jeûne, 'Hol hamoed ; pas un mardi ordinaire.
  for (const d of ['2026-10-19', '2026-10-15', '2026-10-17', '2026-11-11', '2026-12-20', '2026-09-29', '2026-04-16']) assert.ok(jour(d, true).conditions.has('kriat_hatorah'), d);
  for (const d of ['2026-10-20', '2026-10-21', '2026-10-23']) assert.ok(!jour(d, true).conditions.has('kriat_hatorah'), d);
  // Jérusalem : Pourim et Al hanissim le 15 Adar II 5787 (24/03/2027).
  assert.ok(!jour('2027-03-23', true, true).conditions.has('al_hanissim'));
  assert.ok(jour('2027-03-24', true, true).conditions.has('pourim'));
  assert.deepEqual(jour('2027-03-23', true, true).fetes, ['pourim_ailleurs']);
  assert.deepEqual(jour('2027-03-24', true, true).fetes, ['pourim_jerusalem']);
  assert.deepEqual(jour('2027-03-23', true).fetes, ['pourim']);
  assert.ok(jour('2027-03-23', true).conditions.has('al_hanissim'));
  assert.ok(!jour('2027-03-24', true).conditions.has('al_hanissim'));
  const chaines = readFileSync(join(ICI, '..', '..', 'android', 'app', 'src', 'main', 'res', 'values', 'strings.xml'), 'utf8');
  for (const n of ['edot', 'sefard', 'ashkenaz']) {
    for (const k of [...A.ajouts(rc, n), ...A.ajouts(jeune, n), ...A.ajouts(ete, n), ...A.ajouts(av, n)]) assert.ok(chaines.includes(`name="${k}"`), k);
  }
  const s = A.semaines(2026, 10);
  assert.equal(s.length, 5);
  assert.equal(s[0][4], '2026-10-01');
  assert.equal(s.flat().filter(Boolean).length, 31);
});

// Le calendrier hébraïque en vue principale (10/10) : chaque mois, contre Hebcal.
test('calendrier : les mois hébreux de 2026 à 2028, bornes et longueurs comme Hebcal', async () => {
  const A = await import('../src/js/jour-affiche.js');
  const iso = (d) => d.toISOString().slice(0, 10);
  for (let d = new Date(Date.UTC(2026, 0, 1, 12)); d < new Date(Date.UTC(2029, 0, 1)); d.setUTCDate(d.getUTCDate() + 1)) {
    const hd = new HDate(new Date(d.getUTCFullYear(), d.getUTCMonth(), d.getUTCDate()));
    const debut = new HDate(1, hd.getMonth(), hd.getFullYear()).greg();
    const attendu = `${debut.getFullYear()}-${String(debut.getMonth() + 1).padStart(2, '0')}-${String(debut.getDate()).padStart(2, '0')}`;
    assert.equal(A.debutMoisHebreu(iso(d)), attendu, iso(d));
    if (hd.getDate() === 1) assert.equal(A.joursMoisHebreu(iso(d)).length, hd.daysInMonth(), iso(d));
  }
  const t = A.semainesHebreu('2026-10-10');
  assert.deepEqual(t[0], [null, null, null, null, null, null, '2026-09-12']);
  assert.equal(t.flat().filter(Boolean).length, 30);
  assert.equal(A.moisHebreuVoisin('2026-10-10', 1), '2026-10-12');
  assert.equal(A.moisHebreuVoisin('2026-10-10', -1), '2026-08-14');
  // 5787 est embolismique : Chevat, Adar I, Adar II, Nissan.
  assert.equal(A.moisHebreuVoisin('2027-01-15', 1), '2027-02-08');
  assert.equal(A.moisHebreuVoisin('2027-02-08', 1), '2027-03-10');
  assert.equal(A.moisHebreuVoisin('2027-03-10', 1), '2027-04-08');
});

// --- 6. L'algorithme de la NOAA ---------------------------------------------------------
test('zmanim : l’algorithme de la NOAA, à deux secondes près de ses formules', () => {
  assert.equal(Z.calculateur(LIEUX[1], '2026-10-10'), 'US National Oceanic and Atmospheric Administration Algorithm');
  const ref = JSON.parse(readFileSync(join(ICI, '..', '..', 'android', 'app', 'src', 'test', 'resources', 'zmanim_reference_noaa.json'), 'utf8'));
  assert.ok(ref.length >= 28);
  for (const r of ref) {
    const lieu = LIEUX.find((l) => l.nom === r.lieu);
    const z = Object.fromEntries(Z.jour(lieu, r.date).map((x) => [x.cle, x]));
    for (const cle of ['netz', 'chkia', 'alot', 'misheyakir', 'tset', 'chatsot']) {
      const local = Z.heure(z[cle].instant, lieu.fuseau);
      const secondes = (t) => t.split(':').map(Number).reduce((a, b) => a * 60 + b, 0);
      const vu = new Intl.DateTimeFormat('fr-FR', { hour: '2-digit', minute: '2-digit', second: '2-digit', hourCycle: 'h23', timeZone: lieu.fuseau }).format(z[cle].instant);
      // ’Hatsot : l'équation du temps évaluée à JD − 0,5 par l'ancien calculateur
      // JavaScript de la NOAA, que KosherJava reprend (voir ZmanimTest).
      const tolerance = cle === 'chatsot' ? 20 : 2;
      assert.ok(Math.abs(secondes(vu) - secondes(r[cle])) <= tolerance, `${r.lieu} ${r.date} ${cle} : ${vu} contre ${r[cle]} (NOAA, ${local})`);
    }
  }
});

// --- 7. Le moment du siddour selon les zmanim (JourAfficheTest) ----------------------
test('siddour : la suggestion suit les zmanim du lieu, rien dans les creux', async () => {
  const A = await import('../src/js/jour-affiche.js');
  const { SiddurMoment } = await import('../src/js/regles.js');
  const jer = { nom: 'J', latitude: 31.76904, longitude: 35.21633, altitude: 0, fuseau: 'Asia/Jerusalem' };
  const z = Object.fromEntries(Z.jour(jer, '2026-10-16').map((x) => [x.cle, x.instant]));
  const d = (cle, s) => new Date(+z[cle] + s * 1000);
  assert.equal(A.momentDuSiddour(jer, d('alot', 60)), 'birkot_hachahar');
  assert.equal(A.momentDuSiddour(jer, d('misheyakir', 60)), 'chaharit');
  assert.equal(A.momentDuSiddour(jer, d('chatsot', -60)), 'chaharit');
  assert.equal(A.momentDuSiddour(jer, d('chatsot', 60)), null);
  assert.equal(A.momentDuSiddour(jer, d('minha_gedola', 60)), 'minha');
  assert.equal(A.momentDuSiddour(jer, d('chkia', -60)), 'minha');
  assert.equal(A.momentDuSiddour(jer, d('chkia', 60)), null);
  assert.equal(A.momentDuSiddour(jer, d('tset', 60)), 'arvit');
  assert.equal(A.momentDuSiddour(jer, new Date('2026-10-16T22:45:00+03:00')), 'chema_coucher');
  assert.equal(A.momentDuSiddour(jer, new Date('2026-10-16T02:00:00+03:00')), 'tikoun_hatsot');
  const tromso = { nom: 'T', latitude: 69.6492, longitude: 18.9553, altitude: 0, fuseau: 'Europe/Oslo' };
  assert.equal(A.momentDuSiddour(tromso, new Date('2026-06-21T14:00:00+02:00')), SiddurMoment.suggestion(14, 0));
});

// --- Rappels de tefila (RappelsTefilaTest, 16/10) ---
import { rappelsTefila } from '../src/js/calendrier-juif.js';
test('rappels de tefila : mêmes cas que RappelsTefilaTest', () => {
  assert.deepEqual(rappelsTefila('2026-10-16', true), [{ type: 'pluie_debut', date: '2026-10-18' }]);
  assert.ok(!rappelsTefila('2026-10-16', false).some((r) => r.type === 'pluie_debut'));
  assert.deepEqual(rappelsTefila('2026-12-02', false, 6).filter((r) => r.type === 'pluie_debut').map((r) => r.date), ['2026-12-05']);
  const p = rappelsTefila('2027-04-20', true).map((r) => `${r.type}@${r.date}`);
  for (const x of ['pluie_fin@2027-04-22', 'guechem_fin@2027-04-22', 'omer@2027-04-23']) assert.ok(p.includes(x), x);
  assert.ok(rappelsTefila('2027-09-30', false).some((r) => r.type === 'dix_jours' && r.date === '2027-10-02'));
  assert.ok(rappelsTefila('2027-10-21', true).some((r) => r.type === 'guechem_debut' && r.date === '2027-10-23'));
});

// --- Israël ou diaspora d'après la position (TerritoireIsraelTest, 16/10) ---
import { dansIsrael } from '../src/js/calendrier-juif.js';
test('territoire d’Israël : mêmes villes que TerritoireIsraelTest', () => {
  for (const [lat, lon] of [[31.7769, 35.2224], [32.0853, 34.7818], [29.5577, 34.9519], [33.2073, 35.57], [32.993, 35.69], [32.106, 35.171], [29.895, 35.06], [33.106, 35.806], [33.27, 35.77]]) assert.ok(dansIsrael(lat, lon), `${lat},${lon}`);
  for (const [lat, lon] of [[31.9539, 35.9106], [29.5321, 35.0063], [33.2705, 35.2038], [31.1316, 33.7984], [29.49, 34.876], [48.8566, 2.3522], [33.1256, 35.8244]]) assert.ok(!dansIsrael(lat, lon), `${lat},${lon}`);
});

test('rappels de tefila : nord et sud, Israël et diaspora', () => {
  const t = (israel, sud) => new Set(rappelsTefila('2027-10-21', israel, 3, undefined, sud).map((r) => r.type));
  assert.ok(t(true, false).has('guechem_debut'));
  assert.ok(t(false, false).has('guechem_debut'));
  assert.ok(!t(false, true).has('guechem_debut'));
  assert.ok(!rappelsTefila('2026-10-16', false, 3, undefined, true).some((r) => r.type === 'pluie_debut'));
});
