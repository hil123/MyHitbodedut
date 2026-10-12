// Les trophées (Trophees.kt, 11/10, à niveaux depuis le 12/10) : neuf
// familles qui montent par niveaux, la médaille passant du bronze à
// l'argent, à l'or puis au platine. Mêmes règles que l'application Android.

// La forme de l'objet (12/10) : médaille, coupe, écusson ou gemme — pas que des pièces.
export const FAMILLES = [
  ['premier_pas', '🌱', [1], 'ecusson'],
  ['seance_longue', '⏳', [15, 30, 45, 60, 90, 120, 180], 'coupe'],
  ['jours', '🌳', [7, 18, 40, 70, 100, 180, 365, 613], 'medaille'],
  ['serie', '🔥', [3, 7, 18, 30, 40, 70, 100], 'gemme'],
  ['heures', '⭐', [1, 5, 10, 25, 50, 100, 200, 300], 'coupe'],
  ['semaines', '🏅', [1, 2, 4, 8, 12, 26, 52], 'ecusson'],
  ['erev_chabbat', '🕯️', [1, 4, 12, 26, 52], 'medaille'],
  ['retour', '🔄', [1, 3, 5, 10], 'gemme'],
  ['plume', '✍️', [1, 10, 25, 50, 100, 150], 'ecusson'],
].map(([cle, icone, seuils, forme]) => ({ cle, icone, seuils, forme }));

export const METAUX = ['bronze', 'argent', 'or', 'platine'];

/** Le métal d'un rang : un quart de la famille par métal ; une famille d'un seul niveau est de bronze. */
export function metal(f, rang) {
  if (f.seuils.length === 1) return 'bronze';
  return METAUX[Math.min(3, Math.max(0, Math.floor(((rang - 1) * 4) / f.seuils.length)))];
}

const jourDe = (iso) => Math.round(Date.parse(`${iso}T12:00:00Z`) / 86400000);
const semaineDe = (iso) => new Date(`${iso}T12:00:00Z`).getUTCDay(); // 0 dimanche … 6 samedi

/** Les semaines pleines : six jours faits, du dimanche au vendredi. */
export function semainesPleines(jours) {
  const n = new Set([...jours].map(jourDe));
  return [...jours].filter((iso) => semaineDe(iso) === 0 && [0, 1, 2, 3, 4, 5].every((k) => n.has(jourDe(iso) + k))).length;
}

/** Les retours : un jour de pratique après au moins sept jours sans. */
export function retours(jours) {
  const n = [...jours].map(jourDe).sort((a, b) => a - b);
  return n.filter((v, i) => i > 0 && v - n[i - 1] > 7).length;
}

/** La valeur que compte une famille. pratique = { minutes: { iso: min }, notes, meilleureSerie }. */
export function valeur({ minutes, notes, meilleureSerie }, cle) {
  const jours = Object.keys(minutes).filter((k) => minutes[k] > 0);
  const valeurs = Object.values(minutes);
  switch (cle) {
    case 'premier_pas': return Math.min(jours.length, 1);
    case 'seance_longue': return valeurs.length ? Math.max(...valeurs) : 0;
    case 'jours': return jours.length;
    case 'serie': return meilleureSerie;
    case 'heures': return Math.floor(valeurs.reduce((s, v) => s + v, 0) / 60);
    case 'semaines': return semainesPleines(jours);
    case 'erev_chabbat': return jours.filter((iso) => semaineDe(iso) === 5).length;
    case 'retour': return retours(jours);
    case 'plume': return notes;
    default: return 0;
  }
}

export const rang = (p, f) => { const v = valeur(p, f.cle); return f.seuils.filter((s) => v >= s).length; };

/** Tous les niveaux atteints, en clés « famille:rang ». */
export function gagnes(p) {
  const g = new Set();
  for (const f of FAMILLES) for (let r = 1; r <= rang(p, f); r++) g.add(`${f.cle}:${r}`);
  return g;
}

/** Vers le niveau suivant ; au sommet, le dernier seuil. */
export function progres(p, f) {
  const r = rang(p, f);
  const v = valeur(p, f.cle);
  const cible = f.seuils[Math.min(r, f.seuils.length - 1)];
  return { valeur: v, cible, fraction: cible > 0 ? Math.min(v, cible) / cible : 1 };
}

/**
 * Les niveaux gagnés depuis la dernière fois, le plus haut de chaque famille ;
 * rien la première fois (vus null) ni depuis l'ancien format (clés sans « : »).
 */
export function nouveaux(g, vus) {
  if (!vus || (vus.size && ![...vus].some((k) => k.includes(':')))) return [];
  const parFamille = new Map();
  for (const k of g) {
    if (vus.has(k)) continue;
    const [cle, r] = k.split(':');
    if (!parFamille.has(cle) || parFamille.get(cle) < Number(r)) parFamille.set(cle, Number(r));
  }
  return [...parFamille].map(([cle, r]) => { const famille = FAMILLES.find((f) => f.cle === cle); return { famille, rang: r, metal: metal(famille, r) }; })
    .sort((a, b) => METAUX.indexOf(b.metal) * 100 + b.rang - (METAUX.indexOf(a.metal) * 100 + a.rang));
}
