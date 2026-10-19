// Ouvre chaque page de morceau de Noam Shpayer sur pologi.org dans Chromium,
// clique « télécharger » et garde le MP3 reçu (workflow « Source Noam Shpayer »).
// Le n° 16 (« הלב והמעיין ») n'est pas de lui : écarté.
import { chromium } from 'playwright';
import { writeFileSync, mkdirSync } from 'node:fs';
const TITRES = ['Inward', 'Beyond', 'ניגון חופה', 'Sincerity', 'Be Alive', 'Awakening', 'ניגון קבלת שבת (Live)', 'לעושה נפלאות',
  'קול דודי - בהופעה', 'ארבע בבות', 'Even', 'צמאה', 'Return Of The Spirit', 'Wings', 'Fire', 'אשת חיל', null, 'Let There Be Light',
  'Sky (Live) (feat. Eitan Refua)', 'כי אנו עמך (live)'];
mkdirSync('mp3', { recursive: true });
const nav = await chromium.launch();
const ctx = await nav.newContext({ acceptDownloads: true, userAgent: 'Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124 Safari/537.36' });
const index = [];
for (let n = 0; n < TITRES.length; n++) {
  if (!TITRES[n]) continue;
  const page = await ctx.newPage();
  let audio = null;
  page.on('response', async (r) => {
    const t = r.headers()['content-type'] || '';
    if (!audio && (t.includes('audio') || /\.mp3(\?|$)/.test(r.url())) && r.status() < 300) audio = r.url();
  });
  try {
    await page.goto(`https://pologi.org/poshuky/noam-shpayer/${n}`, { waitUntil: 'networkidle', timeout: 60000 });
    writeFileSync(`mp3/page-${n}.html`, await page.content());
    const attente = page.waitForEvent('download', { timeout: 45000 }).catch(() => null);
    const bouton = page.locator('a[download], a[href*=".mp3"], button:has(svg.lucide-download), a:has(svg.lucide-download)').first();
    if (await bouton.count()) await bouton.click({ timeout: 10000 }).catch(() => {});
    const dl = await attente;
    if (dl) { await dl.saveAs(`mp3/${n}.mp3`); index.push({ n, titre: TITRES[n], via: 'download', url: dl.url() }); }
    else {
      await page.waitForTimeout(5000);
      if (audio) {
        const r = await ctx.request.get(audio, { headers: { referer: page.url() } });
        if (r.ok()) { writeFileSync(`mp3/${n}.mp3`, await r.body()); index.push({ n, titre: TITRES[n], via: 'audio', url: audio }); }
      }
    }
  } catch (e) { index.push({ n, titre: TITRES[n], erreur: String(e).slice(0, 200) }); }
  console.log(n, JSON.stringify(index.at(-1) ?? null));
  await page.close();
}
writeFileSync('mp3/index.json', JSON.stringify(index, null, 1));
await nav.close();
