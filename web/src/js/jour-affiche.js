// Ce que l'onglet Zmanim et le calendrier montrent d'un jour (JourAffiche.kt,
// 10/10) : les ajouts à nommer, les horaires avec l'entrée et la sortie de
// Chabbat ou de fête, la grille d'un mois. Mêmes règles, mêmes cas de test.

import { C, jour } from './calendrier-juif.js';
import * as Z from './zmanim.js';

/** Les ajouts du jour, dans l'ordre de l'Amida : des clés de libellé. */
export function ajouts(c) {
  const l = [];
  if (c.has(C.ASSERET_YEMEI_TECHOUVA)) l.push('ajout_asseret_yemei_techouva');
  l.push(c.has(C.MACHIV_HAROUAH) ? 'ajout_machiv_harouah' : 'ajout_morid_hatal');
  l.push(c.has(C.TAL_OUMATAR) ? 'ajout_tal_oumatar' : 'ajout_berakha');
  if (c.has(C.JEUNE)) l.push('ajout_jeune');
  if (c.has(C.TICHA_BEAV)) l.push('ajout_ticha_beav');
  if (c.has(C.YAALE_VEYAVO)) l.push('ajout_yaale_veyavo');
  if (c.has(C.AL_HANISSIM)) l.push('ajout_al_hanissim');
  if (c.has(C.HALLEL_COMPLET)) l.push('ajout_hallel_complet');
  else if (c.has(C.HALLEL)) l.push('ajout_hallel');
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
