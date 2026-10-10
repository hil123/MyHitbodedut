// Ce que l'onglet Zmanim et le calendrier montrent d'un jour (JourAffiche.kt,
// 10/10) : les ajouts à nommer, les horaires avec l'entrée et la sortie de
// Chabbat ou de fête, la grille d'un mois. Mêmes règles, mêmes cas de test.

import { C, jour } from './calendrier-juif.js';
import { conditionsPour, libelleHallel } from './siddur-jour.js';
import * as Z from './zmanim.js';
import { SiddurMoment } from './regles.js';

/**
 * Les ajouts du jour, dans l'ordre de l'Amida : des clés de libellé. Le nusah
 * change Morid hatal hors d'Israël, la bénédiction du Hallel partiel et
 * l'Anénou du particulier (JourAffiche.ajouts, ZMANIM.md).
 */
export function ajouts(c0, nusah = 'edot', enIsrael = false, sansTahanounFinTichri = nusah === 'edot') {
  const c = conditionsPour(nusah, enIsrael, c0, sansTahanounFinTichri);
  const l = [];
  if (c.has(C.ASSERET_YEMEI_TECHOUVA)) l.push('ajout_asseret_yemei_techouva');
  if (c.has(C.MACHIV_HAROUAH)) l.push('ajout_machiv_harouah');
  else if (c.has(C.MORID_HATAL)) l.push('ajout_morid_hatal');
  l.push(c.has(C.TAL_OUMATAR) ? 'ajout_tal_oumatar' : 'ajout_berakha');
  if (c.has(C.JEUNE)) {
    l.push('ajout_anenou_officiant');
    // Edot : à Tich'a bèAv, le jeûne commence la veille au soir, et Arvit en est.
    if (nusah !== 'edot') l.push('ajout_anenou_particulier_minha');
    else l.push(c.has(C.TICHA_BEAV) ? 'ajout_anenou_particulier_trois' : 'ajout_anenou_particulier_matin_minha');
  }
  if (c.has(C.TICHA_BEAV)) l.push('ajout_ticha_beav');
  if (c.has(C.YAALE_VEYAVO)) l.push('ajout_yaale_veyavo');
  if (c.has(C.AL_HANISSIM)) l.push('ajout_al_hanissim');
  const hallel = libelleHallel(nusah, c);
  if (hallel) l.push(hallel);
  // Le Chabbat, il n'y a pas d'Amida de semaine où dire ou taire Ta'hanoun.
  if (!c.has(C.CHABBAT)) l.push(c.has(C.TAHANOUN_CHAHARIT) ? 'ajout_tahanoun' : 'ajout_sans_tahanoun');
  return l;
}

const lendemain = (iso) => {
  const [a, m, j] = iso.split('-').map(Number);
  return new Date(Date.UTC(a, m - 1, j + 1, 12)).toISOString().slice(0, 10);
};

/** Les horaires du jour, avec l'allumage et la sortie (règles de JourAffiche.horaires). */
export function horaires(lieu, iso, enIsrael, minutes) {
  const aujourdhui = jour(iso, enIsrael).chome;
  const demain = jour(lendemain(iso), enIsrael).chome;
  const base = Z.jour(lieu, iso);
  const tset = base.find((z) => z.cle === 'tset');
  const sortie = [];
  for (const z of base) {
    if (z.cle === 'chkia' && demain && !aujourdhui) sortie.push(Z.allumage(lieu, iso, minutes));
    sortie.push(z);
    if (z.cle === 'tset') {
      if (aujourdhui && !demain) sortie.push({ ...tset, cle: 'sortie' });
      if (aujourdhui && demain) sortie.push({ ...tset, cle: 'allumage', methode: 'apres_tset', parametres: '' });
    }
  }
  return sortie;
}

/**
 * Le texte du siddour que suggère l'instant [maintenant] (Date) d'après les
 * zmanim du [lieu] : mêmes bornes que JourAffiche.momentDuSiddour ; null
 * dans les creux ('hatsot – min'ha guedola, chki'a – tsét) ; sans zmanim,
 * les heures fixes de SiddurMoment.
 */
export function momentDuSiddour(lieu, maintenant) {
  const parties = Object.fromEntries(new Intl.DateTimeFormat('en-CA', { timeZone: lieu.fuseau, year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit', hourCycle: 'h23' })
    .formatToParts(maintenant).map((p) => [p.type, p.value]));
  const iso = `${parties.year}-${parties.month}-${parties.day}`;
  const minutes = Number(parties.hour) * 60 + Number(parties.minute);
  const instants = (d) => Object.fromEntries(Z.jour(lieu, d).map((x) => [x.cle, x.instant]));
  const z = instants(iso);
  const [a, m, j] = iso.split('-').map(Number);
  const hier = instants(new Date(Date.UTC(a, m - 1, j - 1, 12)).toISOString().slice(0, 10));
  const cles = ['alot', 'misheyakir', 'chatsot', 'minha_gedola', 'chkia', 'tset', 'chatsot_layla'];
  if (cles.some((c) => !z[c]) || !hier.chatsot_layla) return SiddurMoment.suggestion(Math.floor(minutes / 60), minutes % 60);
  const t = +maintenant;
  const avant = (c) => t < +z[c];
  if (avant('alot')) return t >= +hier.chatsot_layla ? 'tikoun_hatsot' : 'chema_coucher';
  if (avant('misheyakir')) return 'birkot_hachahar';
  if (avant('chatsot')) return 'chaharit';
  if (avant('minha_gedola')) return null;
  if (avant('chkia')) return 'minha';
  if (avant('tset')) return null;
  if (avant('chatsot_layla')) return minutes > 12 * 60 && minutes < 22 * 60 + 30 ? 'arvit' : 'chema_coucher';
  return 'tikoun_hatsot';
}

/** Les semaines d'un mois civil (mois 1-12), du dimanche au samedi ; null hors du mois. */
export function semaines(annee, mois) {
  const p = (n) => String(n).padStart(2, '0');
  const decalage = new Date(Date.UTC(annee, mois - 1, 1)).getUTCDay();
  const longueur = new Date(Date.UTC(annee, mois, 0)).getUTCDate();
  const cases = [...Array(decalage).fill(null), ...Array.from({ length: longueur }, (_, i) => `${annee}-${p(mois)}-${p(i + 1)}`)];
  const s = [];
  for (let i = 0; i < cases.length; i += 7) {
    const l = cases.slice(i, i + 7);
    s.push([...l, ...Array(7 - l.length).fill(null)]);
  }
  return s;
}
