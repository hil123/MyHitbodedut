// Le Tikoun HaKlali (Tikkun.kt, TikkunActivity.kt, PsalmActivity.kt, LiturgyActivity.kt, 02/11),
// en règles pures : l'ordre de la liste, la progression du jour, le psaume suivant, le
// sous-titre d'une partie liturgique. Ni DOM ni stockage : elles se testent avec Node seul.
// Le texte est data/tikkun_haklali.json (res/raw), lu tel quel.

export const OUVERTURE = 'ouverture';
export const CLOTURE = 'cloture';
export const KAVANA = 'kavana';
export const PRIERE = 'priere_rabbi_nathan';
const BLOCS = [OUVERTURE, CLOTURE, KAVANA, PRIERE];

/** Tikkun.bloc : une partie liturgique par sa clé ; null pour toute autre clé. */
export const bloc = (t, cle) => (BLOCS.includes(cle) && t?.[cle] ? { cle, ...t[cle] } : null);

/** Tikkun.avant : ce qui se dit avant les psaumes, dans l'ordre. */
export const avant = (t) => [OUVERTURE].map((c) => bloc(t, c)).filter(Boolean);

/** Tikkun.apres : ce qui se dit après, dans l'ordre. */
export const apres = (t) => [CLOTURE, KAVANA, PRIERE].map((c) => bloc(t, c)).filter(Boolean);

/** Tikkun.versets : tous les versets des dix psaumes. */
export const versets = (t) => t.psaumes.reduce((n, p) => n + p.versets.length, 0);

/** Tikkun.byNumero. */
export const parNumero = (t, numero) => t.psaumes.find((p) => p.numero === numero) ?? null;

/**
 * Les lignes de TikkunActivity : l'ordre réel du Tikoun, ce qui se dit avant et après
 * compris. Un psaume porte son rang (1 à 10) ; une partie liturgique n'en a pas.
 */
export const lignes = (t) => [
  ...avant(t).map((b) => ({ bloc: b })),
  ...t.psaumes.map((p, i) => ({ psaume: p, rang: i + 1 })),
  ...apres(t).map((b) => ({ bloc: b })),
];

/** Le rang d'un psaume dans les dix (PsalmActivity : indexOf + 1) ; 0 s'il n'en est pas. */
export const rang = (t, numero) => t.psaumes.findIndex((p) => p.numero === numero) + 1;

/** PsalmActivity, « Tehilim suivant » : psaumes.getOrNull(rang) ; null après le dixième. */
export const suivant = (t, numero) => {
  const r = rang(t, numero);
  return r === 0 ? null : t.psaumes[r] ?? null;
};

/**
 * TikkunActivity.render : la chaîne de la progression du jour et ses arguments.
 * [dits] : les numéros dits aujourd'hui (Store.tikkunDone).
 */
export function progres(t, dits) {
  const total = t.psaumes.length;
  const n = dits.size;
  if (n === 0) return { cle: 'tikkun_progress_none', args: [total, versets(t)] };
  if (n === total) return { cle: 'tikkun_progress_all', args: [] };
  return { cle: 'tikkun_progress', args: [n, total] };
}

/** LocalizedText.forLanguage : le français ou l'anglais s'ils existent, l'hébreu sinon. */
export function pourLangue(texte, langue) {
  if (!texte) return '';
  const he = texte.he ?? '';
  if (langue === 'fr') return (texte.fr ?? '').trim() ? texte.fr : he;
  if (langue === 'en') return (texte.en ?? '').trim() ? texte.en : he;
  return he;
}

/**
 * TikkunActivity.bindBloc : sous le titre hébreu d'une partie, sa traduction ; à défaut
 * (en hébreu), le nombre de ses paragraphes — rendu { traduction } ou { paragraphes }.
 */
export function detailBloc(b, langue) {
  const traduction = pourLangue(b.titre, langue);
  return traduction === b.titre.he ? { paragraphes: b.paragraphes.length } : { traduction };
}

/**
 * LiturgyActivity : sous le titre, la note de la partie dans la langue ; sans note,
 * le titre traduit s'il diffère de l'hébreu ; sinon rien (null).
 */
export function sousTitreBloc(b, langue) {
  const note = b.note ? pourLangue(b.note, langue).trim() : '';
  if (note) return pourLangue(b.note, langue);
  const titre = pourLangue(b.titre, langue);
  return titre !== b.titre.he ? titre : null;
}
