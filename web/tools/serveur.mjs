// Un serveur de test pour web/dist, avec les requêtes par tranches (Range)
// que Safari envoie pour la musique.
//
//   node web/tools/serveur.mjs [port]     → sert web/dist sur 127.0.0.1

import { createServer } from 'node:http';
import { readFileSync, statSync } from 'node:fs';
import { dirname, extname, join, normalize } from 'node:path';
import { fileURLToPath } from 'node:url';

const DIST = join(dirname(fileURLToPath(import.meta.url)), '..', 'dist');

const TYPES = {
  '.html': 'text/html; charset=utf-8', '.js': 'text/javascript', '.css': 'text/css', '.json': 'application/json',
  '.webp': 'image/webp', '.png': 'image/png', '.svg': 'image/svg+xml', '.mp3': 'audio/mpeg', '.m4a': 'audio/mp4',
  '.webmanifest': 'application/manifest+json', '.txt': 'text/plain',
};

export function demarreServeur(port = 0, hote = '127.0.0.1') {
  const serveur = createServer((req, res) => {
    const chemin = decodeURIComponent(new URL(req.url, 'http://x').pathname);
    let fichier = normalize(join(DIST, chemin));
    if (!fichier.startsWith(DIST)) { res.writeHead(403).end(); return; }
    try { if (statSync(fichier).isDirectory()) fichier = join(fichier, 'index.html'); } catch { /* 404 plus bas */ }
    let corps;
    try { corps = readFileSync(fichier); } catch { res.writeHead(404).end(); return; }
    const type = TYPES[extname(fichier)] || 'application/octet-stream';
    const plage = /bytes=(\d*)-(\d*)/.exec(req.headers.range || '');
    if (plage) {
      const debut = plage[1] === '' ? Math.max(0, corps.length - Number(plage[2])) : Number(plage[1]);
      const fin = plage[1] !== '' && plage[2] !== '' ? Math.min(Number(plage[2]), corps.length - 1) : corps.length - 1;
      res.writeHead(206, {
        'Content-Type': type, 'Accept-Ranges': 'bytes',
        'Content-Range': `bytes ${debut}-${fin}/${corps.length}`, 'Content-Length': fin - debut + 1,
      });
      res.end(corps.subarray(debut, fin + 1));
      return;
    }
    res.writeHead(200, { 'Content-Type': type, 'Accept-Ranges': 'bytes', 'Content-Length': corps.length });
    res.end(corps);
  });
  return new Promise((r) => serveur.listen(port, hote, () => r(serveur)));
}

if (process.argv[1] === fileURLToPath(import.meta.url)) {
  const s = await demarreServeur(Number(process.argv[2] || 8080), process.argv[3] || '127.0.0.1');
  console.log(`web/dist sur http://127.0.0.1:${s.address().port}/`);
}
