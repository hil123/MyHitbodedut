// Les trophées (Trophees.kt, 11/10) : des moments qui se gagnent une fois
// pour toutes, à côté des paliers. Mêmes règles que l'application Android.

export const TROPHEES = [
  ['premier_pas', '🌱'], ['une_heure', '⏳'], ['deux_heures', '🔥'], ['semaine_pleine', '📅'],
  ['erev_chabbat', '🕯️'], ['retour', '🔄'], ['chai', 'חי'], ['quarante', '⛰️'], ['dix_heures', '⭐'],
  ['cent_heures', '🌟'], ['plume', '✍️'], ['plume_fidele', '📖'], ['taryag', '👑'],
].map(([cle, icone]) => ({ cle, icone }));

const jourDe = (iso) => Math.round(Date.parse(`${iso}T12:00:00Z`) / 86400000);
const semaineDe = (iso) => new Date(`${iso}T12:00:00Z`).getUTCDay(); // 0 dimanche … 6 samedi

/** Les six jours, du dimanche au vendredi, d'une même semaine. */
export function semainePleine(jours) {
  const n = new Set([...jours].map(jourDe));
  return [...jours].some((iso) => semaineDe(iso) === 0 && [0, 1, 2, 3, 4, 5].every((k) => n.has(jourDe(iso) + k)));
}

/** Un jour de pratique après au moins sept jours sans. */
export function retour(jours) {
  const n = [...jours].map(jourDe).sort((a, b) => a - b);
  return n.some((v, i) => i > 0 && v - n[i - 1] > 7);
}

/** { minutes: { iso: min }, notes, meilleureSerie } → l'ensemble des clés gagnées. */
export function gagnes({ minutes, notes, meilleureSerie }) {
  const jours = Object.keys(minutes).filter((k) => minutes[k] > 0);
  const valeurs = Object.values(minutes);
  const total = valeurs.reduce((s, v) => s + v, 0);
  const max = valeurs.length ? Math.max(...valeurs) : 0;
  const g = new Set();
  if (jours.length) g.add('premier_pas');
  if (max >= 60) g.add('une_heure');
  if (max >= 120) g.add('deux_heures');
  if (semainePleine(jours)) g.add('semaine_pleine');
  if (jours.filter((iso) => semaineDe(iso) === 5).length >= 4) g.add('erev_chabbat');
  if (retour(jours)) g.add('retour');
  if (jours.length >= 18) g.add('chai');
  if (meilleureSerie >= 40) g.add('quarante');
  if (total >= 600) g.add('dix_heures');
  if (total >= 6000) g.add('cent_heures');
  if (notes >= 10) g.add('plume');
  if (notes >= 50) g.add('plume_fidele');
  if (jours.length >= 613) g.add('taryag');
  return g;
}

/** Les trophées gagnés depuis la dernière fois ; rien la première fois (vus null). */
export const nouveaux = (g, vus) => (vus ? TROPHEES.filter((t) => g.has(t.cle) && !vus.has(t.cle)) : []);
