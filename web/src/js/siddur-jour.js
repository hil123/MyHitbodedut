// Ce que le jour change à la lecture du siddour (SiddurJour.kt, 10/10) : les
// ajouts du jour mis en évidence, les autres repliés derrière une flèche,
// jamais retirés. Annotations : data/siddur_annotations.json
// (tools/siddur_annotations.py) ; conditions : calendrier-juif.js.

/** Une condition, « ! » devant : quand la clé est fausse. */
export function vrai(condition, conditions) {
  return condition.startsWith('!') ? !conditions.has(condition.slice(1)) : conditions.has(condition);
}

/** Le nom de la chaîne qui nomme une condition (le même qu'Android). */
export function libelle(condition) {
  const c = condition.replace(/^!/, '');
  if (condition.startsWith('!tahanoun')) return 'ajout_sans_tahanoun';
  if (c.startsWith('tahanoun')) return 'ajout_tahanoun';
  if (c === 'kippour') return 'fete_yom_kippour';
  if (c === 'hallel') return 'ajout_hallel_annonce';
  if (['pessah', 'soukkot', 'chavouot', 'roch_hachana', 'chemini_atseret', 'hanouka', 'pourim'].includes(c)) return `fete_${c}`;
  return `ajout_${c}`;
}

/** Comme tools/siddur_annotations.py : sans balises ni voyelles, quarante caractères. */
export function empreinte(texte) {
  return texte.replace(/<[^>]+>/g, ' ').replace(/[֑-ׇ]/g, '').replace(/\s+/g, ' ').trim().slice(0, 40);
}

/**
 * Les annotations d'un texte, rangées pour la lecture : par section, la
 * condition de la section entière ; par bloc, son annotation de bloc ; et
 * les indices de chaque bloc.
 */
export function index(annotations = []) {
  const sections = new Map();
  const blocs = new Map();
  const indices = new Map();
  for (const a of annotations) {
    if (a.sorte === 'section') sections.set(a.s, a);
    else if (a.sorte === 'bloc') blocs.set(`${a.s}:${a.b}`, a);
    else indices.set(`${a.s}:${a.b}`, [...(indices.get(`${a.s}:${a.b}`) ?? []), a]);
  }
  return { sections, blocs, indices };
}
