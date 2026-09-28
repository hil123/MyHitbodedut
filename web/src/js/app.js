// L'application : l'accueil (MainActivity), la musique (MusicActivity) et
// un premier écran de réglages. Les écrans suivants arrivent par étapes ;
// ce qui n'existe pas encore est montré, mais inerte (classe « a-venir »).

import { chargeTraduction, langueInitiale, LANGUES } from './i18n.js';
import { DureeSeance, JaugeSemaine } from './regles.js';
import { creeStore, memoire } from './store.js';

// --- Stockage ---------------------------------------------------------------
function stockageDuNavigateur() {
  try {
    const s = window.localStorage;
    s.setItem('__essai', '1');
    s.removeItem('__essai');
    return s;
  } catch {
    return memoire();
  }
}
const store = creeStore(stockageDuNavigateur());

// Safari peut effacer ce qu'un site garde ; une application de l'écran
// d'accueil en est exemptée, et persist() le demande explicitement.
navigator.storage?.persist?.().catch(() => {});

// --- Petits outils de construction du DOM ---------------------------------------
function h(tag, attrs = {}, ...enfants) {
  const el = document.createElement(tag);
  for (const [k, v] of Object.entries(attrs)) {
    if (v === null || v === undefined || v === false) continue;
    if (k === 'class') el.className = v;
    else if (k.startsWith('on')) el.addEventListener(k.slice(2), v);
    else el.setAttribute(k, v === true ? '' : v);
  }
  for (const e of enfants.flat()) {
    if (e === null || e === undefined || e === false) continue;
    el.append(e instanceof Node ? e : document.createTextNode(String(e)));
  }
  return el;
}
const SVG = 'http://www.w3.org/2000/svg';
function s(tag, attrs = {}) {
  const el = document.createElementNS(SVG, tag);
  for (const [k, v] of Object.entries(attrs)) el.setAttribute(k, v);
  return el;
}

// --- Les sujets (Theme.kt), dans l'ordre de la liste Android ----------------------
const SUJETS = ['emotions', 'gratitude', 'decisions', 'famille', 'zivoug', 'parnassa', 'avoda', 'difficultes', 'croissance', 'autre'];

// --- Le hassid (Character.kt) : stable sur une journée, change le lendemain -------
const ACCUEIL = ['char_welcome', 'char_offering', 'char_pointing', 'char_praise'];
const JOIE = ['char_dancing_joy', 'char_dancing', 'char_guitar', 'char_thumbs_up', 'char_open_arms'];
function duJour(pool, sel) {
  const [a, m, j] = store.today().split('-').map(Number);
  const jourEpoque = Math.round(Date.UTC(a, m - 1, j) / 86400000) + sel * 7;
  return pool[((jourEpoque % pool.length) + pool.length) % pool.length];
}
const salutation = (fait) => (fait ? duJour(JOIE, 3) : duJour(ACCUEIL, 0));

// --- État de l'application -----------------------------------------------------------
let tr;
let morceaux = [];
let tikkun = null;
let onglet = 'aujourdhui';
const app = document.getElementById('app');

function appliqueApparence() {
  const racine = document.documentElement;
  racine.dataset.mode = store.modeApparence();
  racine.dataset.fond = store.fond();
  racine.dataset.accent = store.accent();
  racine.lang = tr.langue;
  racine.dir = tr.rtl ? 'rtl' : 'ltr';
}

// --- Feuille de choix : ce que sont les AlertDialog d'Android -----------------------
function feuille(titre, construire) {
  const voile = h('div', { class: 'voile', role: 'presentation' });
  const fermer = () => voile.remove();
  const f = h('div', { class: 'feuille', role: 'dialog', 'aria-modal': 'true', 'aria-label': titre },
    h('h2', {}, titre));
  construire(f, fermer);
  voile.addEventListener('click', (e) => { if (e.target === voile) fermer(); });
  voile.append(f);
  document.body.append(voile);
  f.querySelector('button, input')?.focus();
}

function choisirDans(titre, options, courante, onChoisi) {
  feuille(titre, (f, fermer) => {
    options.forEach((o, i) => f.append(h('button', {
      class: 'choix', role: 'menuitemradio', 'aria-checked': String(i === courante),
      onclick: () => { fermer(); onChoisi(i); },
    }, o)));
  });
}

/** DureeSeance.choisir : les durées proposées, puis « Durée réglable… ». */
function choisirDuree(onChoisi) {
  const libelles = [...DureeSeance.PROPOSEES.map((m) => tr.duree(m)), tr.t('duration_custom')];
  const i = DureeSeance.PROPOSEES.indexOf(store.targetMinutes());
  choisirDans(tr.t('settings_target'), libelles, i, (choix) => {
    if (choix < DureeSeance.PROPOSEES.length) onChoisi(DureeSeance.PROPOSEES[choix]);
    else saisirDuree(onChoisi);
  });
}

function saisirDuree(onChoisi) {
  feuille(tr.t('duration_custom_title'), (f, fermer) => {
    const champ = h('input', {
      type: 'number', inputmode: 'numeric', min: DureeSeance.MIN, max: DureeSeance.MAX,
      placeholder: tr.t('duration_custom_hint'), value: store.targetMinutes(),
    });
    const valider = () => {
      const m = DureeSeance.lire(champ.value);
      fermer();
      if (m !== null) onChoisi(m);
    };
    champ.addEventListener('keydown', (e) => { if (e.key === 'Enter') valider(); });
    f.append(champ, h('p', { class: 'indice' }, tr.t('duration_custom_hint')),
      h('div', { class: 'actions' },
        h('button', { onclick: fermer }, tr.t('action_back')),
        h('button', { onclick: valider }, tr.t('action_save'))));
    setTimeout(() => champ.select(), 0);
  });
}

function choisirSujet() {
  const libelles = [tr.t('session_theme_none_short'), ...SUJETS.map((c) => tr.t(`theme_${c}`))];
  const courant = store.sessionTheme();
  choisirDans(tr.t('session_theme_title'), libelles, courant ? SUJETS.indexOf(courant) + 1 : 0, (i) => {
    store.setSessionTheme(i === 0 ? null : SUJETS[i - 1]);
    rendre();
  });
}

// --- La musique -----------------------------------------------------------------------
// Un seul élément <audio>, gardé d'un écran à l'autre. C'est un élément
// média et non Web Audio : iOS coupe Web Audio écran verrouillé, et ne
// laisse continuer que la lecture d'un élément média.
const lecteur = new Audio();
lecteur.preload = 'none';
let enEcoute = null;
lecteur.addEventListener('ended', () => { enEcoute = null; rendre(); });
lecteur.addEventListener('pause', () => { if (!lecteur.ended) { enEcoute = null; rendre(); } });

/** iOS ne laisse pas une page régler le volume : il reste aux boutons du téléphone. */
const volumeReglable = (() => {
  const a = new Audio();
  a.volume = 0.5;
  return a.volume === 0.5;
})();

const titreMorceau = (m) => tr.t(m.titre);
const morceauChoisi = () => morceaux.find((m) => m.id === store.musicTrackId()) ?? morceaux[0] ?? null;

function ecouter(m) {
  if (enEcoute === m.id) { lecteur.pause(); enEcoute = null; rendre(); return; }
  lecteur.src = m.fichier;
  if (volumeReglable) lecteur.volume = store.musicVolume() / 100;
  enEcoute = m.id;
  if ('mediaSession' in navigator) {
    navigator.mediaSession.metadata = new MediaMetadata({
      title: titreMorceau(m), artist: 'MyHitbodedut',
      artwork: [{ src: 'icones/icon-512.png', sizes: '512x512', type: 'image/png' }],
    });
  }
  lecteur.play().catch(() => { enEcoute = null; rendre(); alert(tr.t('music_play_failed')); });
  rendre();
}

// --- Rendu : l'accueil ------------------------------------------------------------------
function jauge(fraction) {
  const r = 50 - 6 / 2.36; // 12 dp d'épaisseur sur 236 dp
  const tour = 2 * Math.PI * r;
  const svg = s('svg', { viewBox: '0 0 100 100', 'aria-hidden': 'true' });
  svg.append(s('circle', { class: 'piste', cx: 50, cy: 50, r, fill: 'none', 'stroke-width': 12 / 2.36 }));
  if (fraction > 0) {
    // Part d'en haut et tourne dans le sens de la lecture : à gauche en
    // hébreu, où le dessin entier est retourné (app.css).
    const arc = s('circle', {
      class: 'arc', cx: 50, cy: 50, r, fill: 'none', 'stroke-width': 12 / 2.36, 'stroke-linecap': 'round',
      'stroke-dasharray': tour, 'stroke-dashoffset': tour,
      transform: 'rotate(-90 50 50)',
    });
    svg.append(arc);
    requestAnimationFrame(() => requestAnimationFrame(() => arc.setAttribute('stroke-dashoffset', tour * (1 - fraction))));
  }
  return svg;
}

function panneauAujourdhui() {
  const prenom = store.firstName();
  const record = store.record();
  const fait = record !== null;
  const serie = store.streak();
  const semaine = store.currentWeek();
  const progression = JaugeSemaine.progression(semaine);
  const nomJour = new Intl.DateTimeFormat(tr.langue, { weekday: 'short', timeZone: 'UTC' });

  const bouton = store.isSessionRunning() ? tr.t('action_back_to_session')
    : fait ? tr.t('action_another_session')
      : tr.t('action_start_session', tr.duree(store.targetMinutes()));

  const sujet = store.sessionTheme();
  const morceau = store.musicEnabled() ? morceauChoisi() : null;

  return h('div', { class: 'contenu' },
    h('div', { class: 'marque' }, tr.t('app_name')),
    prenom ? h('div', { class: 'salut' }, tr.t('hero_greeting', prenom)) : null,
    // Sans prénom, l'état du jour prend la place du titre.
    h('div', { class: prenom ? 'statut' : 'statut titre' }, tr.t(fait ? 'status_done' : 'status_pending')),
    h('div', { class: 'serie' },
      h('span', {}, serie > 0 ? tr.p('status_streak', serie) : tr.t('status_streak_none')),
      h('span', { class: 'total' }, tr.t('status_total', tr.duree(store.totalMinutes())))),
    fait ? h('div', { class: 'detail' },
      record.minutes > 0 ? tr.t('today_minutes', tr.duree(record.minutes)) : tr.t('today_declared')) : null,

    h('div', { class: 'jauge' }, jauge(progression.fraction),
      h('div', { class: 'plaque' }, h('img', { src: `img/${salutation(fait)}.webp`, alt: '' }))),
    h('div', { class: 'jauge-legende' }, tr.p('week_gauge', progression.faits, progression.faits, progression.dus)),

    // La séance arrive à l'étape 3.
    h('button', { class: 'principal a-venir', 'aria-disabled': 'true' }, bouton),

    h('div', { class: 'pastilles' },
      pastille(tr.t('start_duration'), tr.duree(store.targetMinutes()), () => choisirDuree((m) => {
        store.setTargetMinutes(m); rendre();
      })),
      pastille(tr.t('start_theme'), sujet ? tr.t(`theme_${sujet}`) : tr.t('session_theme_none_short'), choisirSujet),
      pastille(tr.t('start_music'), morceau ? titreMorceau(morceau) : tr.t('start_music_off'), () => ouvrir('musique'))),

    h('div', { class: 'semaine' }, semaine.map((j) => {
      const repos = j.shabbat && j.exempt;
      const marque = j.done ? '✓' : repos ? tr.t('week_shabbat_mark') : j.exempt ? '—' : j.future ? '' : '·';
      const etiquette = j.done ? tr.t('week_done') : repos ? tr.t('settings_shabbat')
        : j.exempt ? tr.t('gate_exempt_title') : tr.t('week_todo');
      const classe = ['jour', j.done ? 'fait' : j.exempt ? 'exempte' : '', repos && !j.done ? 'repos' : ''].join(' ').trim();
      return h('div', { class: classe, role: 'img', 'aria-label': `${nomJour.format(new Date(`${j.date}T12:00:00Z`))} : ${etiquette}` },
        h('span', { class: 'nom', 'aria-hidden': 'true' }, nomJour.format(new Date(`${j.date}T12:00:00Z`))),
        h('span', { class: 'marque-jour', 'aria-hidden': 'true' }, marque));
    })),

    // Le journal arrive à l'étape 4.
    fait ? h('button', { class: 'texte-bouton a-venir', 'aria-disabled': 'true' }, tr.t('action_write_note')) : null);
}

function pastille(libelle, valeur, onclick) {
  return h('button', { class: 'pastille', onclick },
    h('span', { class: 'libelle' }, libelle), h('span', { class: 'valeur' }, valeur));
}

function ligne({ nom, etat, onclick, aVenir = false, fin = null }) {
  return h('button', { class: `ligne${aVenir ? ' a-venir' : ''}`, onclick, 'aria-disabled': aVenir ? 'true' : null },
    h('span', { class: 'corps' }, h('div', { class: 'nom' }, nom), etat ? h('div', { class: 'etat' }, etat) : null),
    fin, h('span', { class: 'chevron', 'aria-hidden': 'true' }));
}

function etatTikkun() {
  if (!tikkun) return '';
  const dits = store.tikkunDone().size;
  const total = tikkun.psaumes.length;
  const versets = tikkun.psaumes.reduce((n, p) => n + p.versets.length, 0);
  if (dits === 0) return tr.t('tikkun_progress_none', total, versets);
  if (dits === total) return tr.t('tikkun_progress_all');
  return tr.t('tikkun_progress', dits, total);
}

function etatMusique() {
  const m = morceauChoisi();
  if (!store.musicEnabled()) return tr.t('music_off_summary');
  if (!m) return tr.t('music_none_summary');
  return volumeReglable ? tr.t('music_on_summary', titreMorceau(m), store.musicVolume()) : titreMorceau(m);
}

function panneauTextes() {
  return h('div', { class: 'contenu' },
    h('h1', { class: 'titre-ecran' }, tr.t('nav_texts')),
    h('div', { class: 'carte' },
      // Les textes arrivent à l'étape 4.
      ligne({ nom: tr.t('tikkun_title'), etat: etatTikkun(), aVenir: true }),
      ligne({ nom: tr.t('tefilot_title'), etat: tr.t('tefilot_hint'), aVenir: true }),
      ligne({ nom: tr.t('library_title'), etat: tr.t('library_hint'), aVenir: true }),
      ligne({ nom: tr.t('tips_title'), etat: tr.t('tips_hint'), aVenir: true })),
    h('h2', { class: 'section' }, tr.t('section_around')),
    h('div', { class: 'carte' },
      ligne({ nom: tr.t('music_title'), etat: etatMusique(), onclick: () => ouvrir('musique') }),
      ligne({ nom: tr.t('settings_title'), onclick: () => ouvrir('reglages') })));
}

function onglets() {
  const items = [
    ['aujourdhui', 'nav_today'], ['textes', 'nav_texts'], ['journal', 'nav_journal'], ['reglages', 'settings_title'],
  ];
  return h('nav', { class: 'onglets' }, items.map(([cle, nom]) => h('button', {
    class: `onglet${cle === 'journal' ? ' a-venir' : ''}`,
    'data-onglet': cle,
    'aria-current': cle === onglet ? 'page' : null,
    onclick: () => {
      // Journal et Réglages ouvrent leur écran ; l'onglet actif ne bouge pas (MainActivity.wireTabs).
      if (cle === 'reglages') ouvrir('reglages');
      else { onglet = cle; rendre(); }
    },
  }, h('span', { class: 'icone', 'aria-hidden': 'true' }), tr.t(nom))));
}

// --- Rendu : écrans secondaires ---------------------------------------------------------
function barre(titre) {
  return h('header', { class: 'barre' },
    h('button', { class: 'retour', 'aria-label': tr.t('action_back'), onclick: () => history.back() }),
    h('h1', {}, titre));
}

function ecranMusique() {
  const choisi = morceauChoisi();
  const interrupteur = h('input', {
    type: 'checkbox', role: 'switch', class: 'interrupteur', checked: store.musicEnabled(),
    'aria-label': tr.t('music_enable'),
    onchange: (e) => { store.setMusicEnabled(e.target.checked); rendre(); },
  });
  return h('div', { class: 'ecran' }, barre(tr.t('music_title')),
    h('div', { class: 'defile' }, h('div', { class: 'contenu' },
      h('div', { class: 'carte' },
        h('label', { class: 'ligne' },
          h('span', { class: 'corps' }, h('div', { class: 'nom' }, tr.t('music_enable')),
            h('div', { class: 'etat' }, tr.t('music_enable_hint'))),
          interrupteur),
        volumeReglable ? h('div', { class: 'ligne' },
          h('span', { class: 'corps' }, h('div', { class: 'nom' }, tr.t('music_volume')),
            h('input', {
              type: 'range', min: 0, max: 100, value: store.musicVolume(), 'aria-label': tr.t('music_volume'),
              oninput: (e) => { store.setMusicVolume(Number(e.target.value)); lecteur.volume = store.musicVolume() / 100; },
            }))) : null),
      h('h2', { class: 'section' }, tr.t('music_library')),
      h('div', { class: 'carte' }, morceaux.map((m) => h('div', { class: `ligne morceau${m === choisi ? ' choisi' : ''}` },
        h('button', {
          class: 'corps', onclick: () => { store.setMusicTrackId(m.id); rendre(); },
          'aria-pressed': String(m === choisi),
        }, h('div', { class: 'nom' }, titreMorceau(m)), h('div', { class: 'etat' }, tr.t('music_origin_bundled'))),
        h('button', {
          class: 'ecouter', 'aria-pressed': String(enEcoute === m.id), onclick: () => ecouter(m),
        }, tr.t('music_preview'))))))));
}

function ecranReglages() {
  const courante = LANGUES.find((l) => l.code === tr.langue);
  return h('div', { class: 'ecran' }, barre(tr.t('settings_title')),
    h('div', { class: 'defile' }, h('div', { class: 'contenu' },
      h('div', { class: 'carte' },
        ligne({
          nom: tr.t('settings_language'), etat: courante.nom,
          onclick: () => choisirDans(tr.t('settings_language'), LANGUES.map((l) => l.nom),
            LANGUES.indexOf(courante), async (i) => {
              store.setLangueApp(LANGUES[i].code);
              tr = await chargeTraduction(LANGUES[i].code);
              appliqueApparence();
              rendre();
            }),
        })))));
}

// --- Navigation --------------------------------------------------------------------------
// Chaque écran secondaire est une entrée d'historique : le bouton retour
// et le geste de retour ramènent à l'accueil, sur l'onglet quitté.
function ecranCourant() {
  const e = location.hash.slice(1);
  return ['musique', 'reglages'].includes(e) ? e : 'accueil';
}
function ouvrir(ecran) {
  if (ecranCourant() !== ecran) history.pushState(null, '', `#${ecran}`);
  rendre();
}
window.addEventListener('popstate', () => rendre());

function rendre() {
  // La position de défilement survit à un nouveau rendu du même écran.
  const avant = app.querySelector('.defile')?.scrollTop ?? 0;
  const ecran = ecranCourant();
  const memeEcran = app.dataset.ecran === `${ecran}/${onglet}`;
  app.replaceChildren();
  if (ecran === 'musique') app.append(ecranMusique());
  else if (ecran === 'reglages') app.append(ecranReglages());
  else {
    app.append(h('div', { class: 'ecran' },
      h('div', { class: 'defile' }, onglet === 'textes' ? panneauTextes() : panneauAujourdhui()),
      onglets()));
  }
  app.dataset.ecran = `${ecran}/${onglet}`;
  if (memeEcran) app.querySelector('.defile').scrollTop = avant;
}

// --- Démarrage ---------------------------------------------------------------------------
async function demarrer() {
  tr = await chargeTraduction(langueInitiale(store.langueApp(), navigator.languages));
  appliqueApparence();
  const [m, t] = await Promise.all([
    fetch('data/musique.json').then((r) => r.json()),
    fetch('data/tikkun_haklali.json').then((r) => r.json()),
  ]);
  morceaux = m;
  tikkun = t;
  rendre();
  // Revenir à l'application un autre jour redessine l'accueil du jour.
  document.addEventListener('visibilitychange', () => { if (!document.hidden) rendre(); });
}

if ('serviceWorker' in navigator) {
  navigator.serviceWorker.register('sw.js').catch(() => {});
}
demarrer();
