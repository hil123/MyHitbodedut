// Vérifie Moteur.js hors navigateur et hors Node : un contexte nu, comme
// celui de JavaScriptCore (ni require, ni process, ni window). Rejoue les
// vecteurs KosherJava de web/test. Les mêmes vecteurs sont rejoués sur
// iPhone simulé par MoteurTests.swift.
//
//   node ios/tools/verifie_moteur.mjs chemin/vers/Moteur.js

import { readFileSync } from 'node:fs';
import vm from 'node:vm';

const [, , chemin] = process.argv;
const source = readFileSync(chemin, 'utf8');
const vecteurs = JSON.parse(readFileSync(new URL('../../web/test/vecteurs_kosherjava.json', import.meta.url), 'utf8'));

const contexte = vm.createContext({});
vm.runInContext(source, contexte);
const M = contexte.Moteur;

let jours = 0;
for (const [cle, attendu] of Object.entries(vecteurs.jours)) {
  const [iso, il] = cle.split('/');
  const obtenu = M.calendrier(iso, il === 'il');
  if (obtenu !== JSON.stringify(attendu)) throw new Error(`${cle} : ${obtenu} contre ${JSON.stringify(attendu)}`);
  jours++;
}

let zmanim = 0;
for (const r of vecteurs.zmanim) {
  const lieu = JSON.stringify({ nom: r.lieu, latitude: r.lat, longitude: r.lon, altitude: 0, fuseau: r.fuseau });
  for (const z of JSON.parse(M.zmanim(lieu, r.date, 18))) {
    const attendu = r[z.cle];
    const ou = `${r.lieu} ${r.date} ${z.cle}`;
    if (z.statut === 'erreur') throw new Error(`${ou} : erreur`);
    if (attendu === 'ABSENT') { if (z.statut !== 'absent') throw new Error(`${ou} : présent`); continue; }
    if (z.statut !== 'ok' || Math.abs(z.ms - Date.parse(attendu)) > 1000) throw new Error(`${ou} : ${z.ms} contre ${attendu}`);
    zmanim++;
  }
}
console.log(`Moteur.js : ${jours} jours et ${zmanim} zmanim identiques à KosherJava (${M.version}).`);
