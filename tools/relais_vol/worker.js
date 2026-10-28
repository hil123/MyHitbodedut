// Le relais du suivi de vol en direct (28/10/2026), pour Cloudflare Workers.
//
// L'application (SuiviVol.kt, vol.js) envoie l'indicatif radio du vol et la
// position estimée sur sa route ; le relais demande à OpenSky Network les
// avions vus dans un carré de ±5° autour d'elle (2 crédits OpenSky au plus,
// 4 si le carré passe le méridien de 180°) et renvoie la position de celui
// dont l'indicatif correspond. La clé d'OpenSky reste ici, dans les secrets
// du Worker : elle n'est jamais dans l'application. Rien n'est journalisé
// par ce code ; Cloudflare garde ses propres journaux (README.md).
//
// GET /position?indicatif=ELY1&lat=50.1&lon=-20.3
//   200 { trouve: true, latitude, longitude, altitude_baro, altitude_geo, instant, icao24 }
//   200 { trouve: false }
//   400 { erreur }        paramètres invalides
//   502 { erreur }        OpenSky injoignable ou refus (crédits épuisés : 429)
//
// Secrets (wrangler secret put), facultatifs : OPENSKY_CLIENT_ID, OPENSKY_CLIENT_SECRET.
// Sans eux, l'accès anonyme et gratuit d'OpenSky (400 crédits par jour pour
// l'adresse IP du Worker, partagés par tous les utilisateurs du web).
// Variable : ORIGINE_WEB, l'origine de la version web autorisée (CORS).

export const JETON = 'https://auth.opensky-network.org/auth/realms/opensky-network/protocol/openid-connect/token';
export const ETATS = 'https://opensky-network.org/api/states/all';
export const DEMI_COTE = 5;

let jeton = null;

/** Le jeton OAuth2 (client credentials), gardé jusqu'à une minute avant son expiration (30 min). */
async function jetonOpenSky(env, chercher) {
  if (jeton && jeton.expire > Date.now() + 60_000) return jeton.valeur;
  const r = await chercher(JETON, {
    method: 'POST',
    headers: { 'content-type': 'application/x-www-form-urlencoded' },
    body: new URLSearchParams({ grant_type: 'client_credentials', client_id: env.OPENSKY_CLIENT_ID, client_secret: env.OPENSKY_CLIENT_SECRET }),
  });
  if (!r.ok) throw new Error(`jeton ${r.status}`);
  const d = await r.json();
  jeton = { valeur: d.access_token, expire: Date.now() + (d.expires_in ?? 1800) * 1000 };
  return jeton.valeur;
}

/** ELY001 et ELY1 sont le même vol (SuiviVol.memeIndicatif). */
export function memeIndicatif(a, b) {
  const parties = (x) => /^([A-Z]{3})0*([0-9]+)([A-Z]*)$/.exec(String(x).toUpperCase().trim())?.slice(1).join('/');
  const pa = parties(a);
  return pa !== undefined && pa === parties(b);
}

/** Les carrés à demander : un seul, ou deux de part et d'autre du méridien de 180°. */
export function carres(lat, lon, d = DEMI_COTE) {
  const lamin = Math.max(-90, lat - d), lamax = Math.min(90, lat + d);
  const lomin = lon - d, lomax = lon + d;
  if (lomin < -180) return [[lamin, lomin + 360, lamax, 180], [lamin, -180, lamax, lomax]];
  if (lomax > 180) return [[lamin, lomin, lamax, 180], [lamin, -180, lamax, lomax - 360]];
  return [[lamin, lomin, lamax, lomax]];
}

const json = (corps, statut, origine) => new Response(JSON.stringify(corps), {
  status: statut,
  headers: { 'content-type': 'application/json', 'cache-control': 'no-store', ...(origine ? { 'access-control-allow-origin': origine, vary: 'origin' } : {}) },
});

export async function traiter(requete, env, chercher = fetch) {
  const url = new URL(requete.url);
  const origine = env.ORIGINE_WEB && requete.headers.get('origin') === env.ORIGINE_WEB ? env.ORIGINE_WEB : null;
  if (requete.method !== 'GET' || url.pathname !== '/position') return json({ erreur: 'introuvable' }, 404, origine);
  const indicatif = (url.searchParams.get('indicatif') ?? '').toUpperCase().replaceAll(' ', '');
  const lat = Number(url.searchParams.get('lat'));
  const lon = Number(url.searchParams.get('lon'));
  if (!/^[A-Z]{3}[0-9]{1,4}[A-Z]{0,2}$/.test(indicatif) || !Number.isFinite(lat) || !Number.isFinite(lon) || Math.abs(lat) > 90 || Math.abs(lon) > 180) {
    return json({ erreur: 'paramètres invalides' }, 400, origine);
  }
  try {
    const t = env.OPENSKY_CLIENT_ID && env.OPENSKY_CLIENT_SECRET ? await jetonOpenSky(env, chercher) : null;
    for (const [lamin, lomin, lamax, lomax] of carres(lat, lon)) {
      const r = await chercher(`${ETATS}?lamin=${lamin}&lomin=${lomin}&lamax=${lamax}&lomax=${lomax}`, t ? { headers: { authorization: `Bearer ${t}` } } : {});
      if (r.status === 401) jeton = null;
      if (!r.ok) return json({ erreur: `opensky ${r.status}` }, 502, origine);
      const d = await r.json();
      // [icao24, callsign, pays, time_position, last_contact, longitude, latitude, baro_altitude, on_ground, …, geo_altitude (13)]
      const e = (d.states ?? []).find((s) => s[1] && memeIndicatif(s[1], indicatif) && s[5] !== null && s[6] !== null && s[3] !== null && !s[8]);
      if (e) {
        return json({ trouve: true, latitude: e[6], longitude: e[5], altitude_baro: e[7], altitude_geo: e[13], instant: new Date(e[3] * 1000).toISOString(), icao24: e[0] }, 200, origine);
      }
    }
    return json({ trouve: false }, 200, origine);
  } catch {
    return json({ erreur: 'opensky injoignable' }, 502, origine);
  }
}

export default { fetch: (requete, env) => traiter(requete, env) };
