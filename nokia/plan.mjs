// Ce que contient le pack pour téléphone à touches, page par page, sans
// navigateur : la liste des documents, leur ordre et leurs noms de fichier.
// build.mjs ne fait que les dessiner en 240×320.
//
// Les règles sont celles de l'application, reprises de web/src/js :
//   - seul passe un ’hizouk publiable (relevé de la source ET relu) ;
//   - l'hébreu sans nikkoud quand la licence de l'édition l'exige ;
//   - en hébreu, pas de traduction ; en français ou en anglais, l'hébreu
//     et la traduction de cette langue, jamais une autre.
//
// Les noms de fichier sont en ASCII et se trient dans l'ordre de lecture :
// un fichier reçu par Bluetooth perd son dossier, pas son nom.

import { ChizukRules, MOMENTS } from '../web/src/js/chizuk.js';
import { Reference } from '../web/src/js/regles.js';

export const LANGUES = ['fr', 'en', 'he'];
export const LARGEUR = 240;
export const HAUTEUR = 320;

/** Section 1 : le mode d'emploi ; 2 à 7 : les moments ; 8 : le Tikoun. */
export const SECTION_GUIDE = 1;
export const SECTION_TIKKUN = 8;
export const sectionDuMoment = (moment) => MOMENTS.indexOf(moment) + 2;

const deux = (n) => String(n).padStart(2, '0');

/** %1$d, %2$s… comme Android. */
export function formate(modele, ...valeurs) {
  return modele.replace(/%(\d+)\$[ds]/g, (_, i) => String(valeurs[Number(i) - 1]));
}

/**
 * Le moment sous lequel ranger un texte : le premier des siens dans l'ordre
 * de l'application. Un texte n'est envoyé qu'une fois.
 */
export function momentDe(item) {
  return MOMENTS.find((m) => (item.moments || []).includes(m)) ?? null;
}

function guide(guideJson, langue) {
  const g = guideJson[langue];
  return {
    langue,
    base: `${langue.toUpperCase()}_${SECTION_GUIDE}_guide`,
    titre: g.titre,
    blocs: g.lignes.map((texte) => ({ type: 'ligne', texte })),
    pied: 'MyHitbodedut',
  };
}

function chizukDocs(chizuk, chaines, langue) {
  const docs = [];
  for (const moment of MOMENTS) {
    const items = chizuk.items.filter((it) => ChizukRules.publiable(it) && momentDe(it) === moment);
    items.forEach((item, i) => {
      const blocs = [{ type: 'he', texte: ChizukRules.hebreu(chizuk, item) }];
      const trad = ChizukRules.traduction(item, langue);
      if (trad) blocs.push({ type: 'trad', texte: trad });
      docs.push({
        langue,
        id: item.id,
        base: `${langue.toUpperCase()}_${sectionDuMoment(moment)}_${moment}_${deux(i + 1)}`,
        titre: chaines[`moment_${moment}`],
        blocs,
        pied: Reference.localise(item.ref, langue),
      });
    });
  }
  return docs;
}

function blocTikkun(bloc, langue) {
  const blocs = [];
  const note = bloc.note?.[langue];
  if (note) blocs.push({ type: 'note', texte: note });
  if (bloc.formule) blocs.push({ type: 'he', texte: bloc.formule });
  for (const p of bloc.paragraphes || []) blocs.push({ type: 'he', texte: p });
  return blocs;
}

function tikkunDocs(tikkun, chaines, langue) {
  const L = langue.toUpperCase();
  const titreTikkun = chaines.tikkun_title;
  const doc = (n, titre, blocs, pied = '') => ({
    langue,
    base: `${L}_${SECTION_TIKKUN}_tikoun_${deux(n)}`,
    titre: `${titreTikkun} · ${titre}`,
    blocs,
    pied,
  });
  const docs = [];
  // L'ordre de TikkunActivity : avant, les dix psaumes, après.
  const avant = ['ouverture'];
  const apres = ['cloture', 'kavana', 'priere_rabbi_nathan'];
  let n = 0;
  for (const cle of avant) {
    if (tikkun[cle]) docs.push(doc(n++, tikkun[cle].titre[langue], blocTikkun(tikkun[cle], langue)));
  }
  for (const p of tikkun.psaumes) {
    const blocs = [];
    for (const v of p.versets) {
      blocs.push({ type: 'verset', n: Reference.lettres(v.n), texte: v.he });
      // L'indication d'arrêt suit le verset qu'elle concerne (PsalmActivity).
      if (v.arret?.note) blocs.push({ type: 'arret', texte: v.arret.note });
    }
    docs.push(doc(n++, formate(chaines.tikkun_psalm_he, p.numero), blocs,
      formate(chaines.tikkun_verses, p.versets.length)));
  }
  for (const cle of apres) {
    if (tikkun[cle]) docs.push(doc(n++, tikkun[cle].titre[langue], blocTikkun(tikkun[cle], langue)));
  }
  return docs;
}

/**
 * Tous les documents d'une langue, dans l'ordre de lecture.
 * chaines : les chaînes Android de cette langue, défaut compris.
 */
export function planLangue({ langue, chizuk, tikkun, guideJson, chaines }) {
  return [
    guide(guideJson, langue),
    ...chizukDocs(chizuk, chaines, langue),
    ...tikkunDocs(tikkun, chaines, langue),
  ];
}

/** Le nom de la page k (1, 2…) d'un document : -01, -02… pour que -10 suive -09. */
export const nomDePage = (doc, k) => `${doc.base}-${deux(k)}.jpg`;

/** Le sens d'écriture d'une langue d'interface. */
export const sens = (langue) => (langue === 'he' ? 'rtl' : 'ltr');
