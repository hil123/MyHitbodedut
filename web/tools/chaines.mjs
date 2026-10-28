// Lecture des strings.xml d'Android.
//
// Les chaînes de l'interface restent écrites à un seul endroit, dans
// android/app/src/main/res/values*/strings.xml. Le site les relit à chaque
// construction : une traduction corrigée côté Android l'est aussi ici, sans
// copie à la main qui finirait par diverger.
//
// Le format est volontairement réduit à ce que ces fichiers contiennent :
// <string> et <plurals>, sans CDATA ni balise interne (vérifié le 28/09).
// Une balise inattendue fait échouer la construction plutôt que d'être
// avalée en silence.

const ENTITES = { amp: '&', lt: '<', gt: '>', quot: '"', apos: "'" };

function decodeEntites(s) {
  return s.replace(/&(#x[0-9a-fA-F]+|#\d+|\w+);/g, (m, e) => {
    if (e[0] === '#') {
      const code = e[1] === 'x' ? parseInt(e.slice(2), 16) : parseInt(e.slice(1), 10);
      return String.fromCodePoint(code);
    }
    if (!(e in ENTITES)) throw new Error(`Entité inconnue : ${m}`);
    return ENTITES[e];
  });
}

/** Ce qu'aapt fait d'une valeur : guillemets, échappements, espaces. */
export function valeurAndroid(brute) {
  let s = decodeEntites(brute);
  if (/<[a-zA-Z/]/.test(s)) throw new Error(`Balise dans une chaîne : ${brute}`);
  // Chaque caractère retient s'il vient d'un espace hors guillemets : seuls
  // ceux-là tombent en tête et en fin de chaîne.
  const sortie = [];
  let entreGuillemets = false;
  for (let i = 0; i < s.length; i++) {
    const c = s[i];
    if (c === '\\' && i + 1 < s.length) {
      const n = s[++i];
      sortie.push({ c: n === 'n' ? '\n' : n === 't' ? '\t' : n, libre: false });
    } else if (c === '"') {
      entreGuillemets = !entreGuillemets;
    } else if (/\s/.test(c) && !entreGuillemets) {
      // Hors guillemets, aapt ramène toute suite d'espaces à une seule.
      if (!(sortie.length && sortie[sortie.length - 1].libre)) sortie.push({ c: ' ', libre: true });
    } else {
      sortie.push({ c, libre: false });
    }
  }
  while (sortie.length && sortie[0].libre) sortie.shift();
  while (sortie.length && sortie[sortie.length - 1].libre) sortie.pop();
  return sortie.map((x) => x.c).join('');
}

function attribut(balise, nom) {
  const m = balise.match(new RegExp(`${nom}="([^"]*)"`));
  return m ? m[1] : null;
}

/** { chaines: {nom: texte}, pluriels: {nom: {one, two, other}} } */
export function litChaines(xml) {
  const sansCommentaires = xml.replace(/<!--[\s\S]*?-->/g, '');
  const chaines = {};
  const pluriels = {};
  for (const m of sansCommentaires.matchAll(/<string\b([^>]*)>([\s\S]*?)<\/string>/g)) {
    chaines[attribut(m[1], 'name')] = valeurAndroid(m[2]);
  }
  for (const m of sansCommentaires.matchAll(/<string\b([^>]*)\/>/g)) {
    chaines[attribut(m[1], 'name')] = '';
  }
  // Les tableaux (<string-array>, ex. boussole_points) : « nom_0 », « nom_1 »…
  for (const m of sansCommentaires.matchAll(/<string-array\b([^>]*)>([\s\S]*?)<\/string-array>/g)) {
    [...m[2].matchAll(/<item>([\s\S]*?)<\/item>/g)].forEach((item, i) => { chaines[`${attribut(m[1], 'name')}_${i}`] = valeurAndroid(item[1]); });
  }
  for (const m of sansCommentaires.matchAll(/<plurals\b([^>]*)>([\s\S]*?)<\/plurals>/g)) {
    const formes = {};
    for (const item of m[2].matchAll(/<item\b([^>]*)>([\s\S]*?)<\/item>/g)) {
      formes[attribut(item[1], 'quantity')] = valeurAndroid(item[2]);
    }
    pluriels[attribut(m[1], 'name')] = formes;
  }
  return { chaines, pluriels };
}
