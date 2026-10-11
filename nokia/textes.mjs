// Les textes de l'application Java, en français seulement, pour un
// téléphone à touches. Relus dans les ressources Android, jamais recopiés :
//   - le ’hizouk publiable (relevé de la source ET relu), traduction
//     française seule, rangé sous son premier moment ;
//   - les titres des moments et quelques mots de l'interface, dans
//     values/strings.xml.
//
// Le fichier produit (textes.bin) se lit avec DataInputStream :
// readUTF pour les chaînes, readShort pour les nombres. Les caractères
// sont ramenés à Latin-1 : une police de téléphone à touches n'a pas
// toujours « œ » ni « … ».

import { ChizukRules, MOMENTS } from '../web/src/js/chizuk.js';
import { Reference } from '../web/src/js/regles.js';

/** Les mots de l'application reprises telles quelles (values/strings.xml). */
export const CLES_INTERFACE = ['chizuk_stuck', 'journal_empty', 'session_done_title', 'reminder_title'];

const REMPLACEMENTS = [
  [/[’‘]/g, "'"], [/[“”]/g, '"'], [/…/g, '...'], [/œ/g, 'oe'], [/Œ/g, 'Oe'],
  [/[–—]/g, '-'], [/[   ]/g, ' '],
];

/** Ramène un texte à ce qu'une police Latin-1 sait dessiner. */
export function latin1(texte) {
  let s = String(texte);
  for (const [motif, par] of REMPLACEMENTS) s = s.replace(motif, par);
  const reste = [...s].find((c) => c.codePointAt(0) > 0xff);
  if (reste) throw new Error(`Caractère hors Latin-1 : ${reste} (U+${reste.codePointAt(0).toString(16)}) dans « ${s.slice(0, 60)} »`);
  return s;
}

/** Le premier moment d'un texte, dans l'ordre de l'application. */
export function momentDe(item) {
  return MOMENTS.find((m) => (item.moments || []).includes(m)) ?? null;
}

/**
 * { interface: {cle: texte}, moments: [{ cle, titre, textes: [{ id, ref, texte }] }] }
 * Un moment sans texte publiable n'apparaît pas.
 */
export function textesFrancais(chizuk, chaines) {
  const moments = MOMENTS.map((cle) => ({
    cle,
    titre: latin1(chaines[`moment_${cle}`]),
    textes: chizuk.items
      .filter((it) => ChizukRules.publiable(it) && momentDe(it) === cle)
      .map((it) => ({ id: it.id, ref: latin1(Reference.localise(it.ref, 'fr')), texte: latin1(ChizukRules.traduction(it, 'fr')) }))
      .filter((t) => t.texte !== ''),
  })).filter((m) => m.textes.length > 0);
  const iface = Object.fromEntries(CLES_INTERFACE.map((k) => {
    if (!(k in chaines)) throw new Error(`Chaîne manquante : ${k}`);
    return [k, latin1(chaines[k])];
  }));
  return { interface: iface, moments };
}

// --- Encodage DataOutputStream ----------------------------------------------------
function utf(s) {
  // En Latin-1, l'UTF-8 modifié de Java est l'UTF-8 ordinaire (pas de NUL ici).
  if (s.includes('\0')) throw new Error('NUL interdit');
  const octets = Buffer.from(s, 'utf8');
  if (octets.length > 0xffff) throw new Error('Chaîne trop longue pour readUTF');
  const tete = Buffer.alloc(2);
  tete.writeUInt16BE(octets.length);
  return Buffer.concat([tete, octets]);
}
function court(n) {
  const b = Buffer.alloc(2);
  b.writeUInt16BE(n);
  return b;
}

/** Format : version, n clés (clé, texte)…, n moments (clé, titre, n textes (ref, texte)…)… */
export function encode(donnees) {
  const parts = [court(1)];
  const cles = Object.keys(donnees.interface);
  parts.push(court(cles.length));
  for (const k of cles) parts.push(utf(k), utf(donnees.interface[k]));
  parts.push(court(donnees.moments.length));
  for (const m of donnees.moments) {
    parts.push(utf(m.cle), utf(m.titre), court(m.textes.length));
    for (const t of m.textes) parts.push(utf(t.ref), utf(t.texte));
  }
  return Buffer.concat(parts);
}

/** Relit textes.bin comme le fera le téléphone (pour les tests). */
export function decode(octets) {
  let i = 0;
  const lireCourt = () => { const n = octets.readUInt16BE(i); i += 2; return n; };
  const lireUtf = () => { const n = lireCourt(); const s = octets.subarray(i, i + n).toString('utf8'); i += n; return s; };
  const version = lireCourt();
  const iface = {};
  for (let n = lireCourt(); n > 0; n--) { const k = lireUtf(); iface[k] = lireUtf(); }
  const moments = [];
  for (let n = lireCourt(); n > 0; n--) {
    const cle = lireUtf();
    const titre = lireUtf();
    const textes = [];
    for (let t = lireCourt(); t > 0; t--) textes.push({ ref: lireUtf(), texte: lireUtf() });
    moments.push({ cle, titre, textes });
  }
  if (i !== octets.length) throw new Error('Octets en trop');
  return { version, interface: iface, moments };
}
