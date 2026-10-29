// Le moteur de calcul de l'application iOS : les règles de la version web,
// telles quelles, exécutées par JavaScriptCore (Moteur.swift). Aucune règle
// n'est réécrite en Swift : les mêmes fichiers servent au web et à l'iPhone,
// et les tests iOS rejouent les vecteurs KosherJava de web/test.
//
// L'échange avec Swift se fait en texte JSON, dans les deux sens : c'est le
// seul format que JSContext transmet sans ambiguïté (dates, Set, null).

import { jour as jourHebreu, TOUTES } from '../../web/src/js/calendrier-juif.js';
import * as Z from '../../web/src/js/zmanim.js';

function zmanimVersObjet(z) {
  return { cle: z.cle, methode: z.methode, parametres: z.parametres, statut: z.statut, ms: z.instant ? +z.instant : null };
}

globalThis.Moteur = {
  version: Z.MOTEUR,

  /** Le jour hébraïque, sous la forme des vecteurs KosherJava (h, t, f, o, p, x, c). */
  calendrier(iso, enIsrael) {
    const j = jourHebreu(iso, enIsrael);
    return JSON.stringify({
      h: [j.jourHebreu, j.moisHebreu, j.anneeHebraique], t: j.dateHebraique, f: j.fetes, o: j.omer,
      p: j.paracha, x: j.chome, c: TOUTES.filter((c) => j.conditions.has(c)),
    });
  },

  /** Les quinze zmanim du profil, puis l'allumage ; lieuJSON = { nom, latitude, longitude, altitude, fuseau }. */
  zmanim(lieuJSON, iso, allumageMinutes) {
    const lieu = JSON.parse(lieuJSON);
    const liste = Z.jour(lieu, iso).map(zmanimVersObjet);
    liste.push(zmanimVersObjet(Z.allumage(lieu, iso, allumageMinutes)));
    return JSON.stringify(liste);
  },
};
