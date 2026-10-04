// L'application : l'accueil (MainActivity), la séance (SessionActivity),
// le bravo (CelebrationActivity), la musique (MusicActivity) et un premier
// écran de réglages. Les écrans suivants arrivent par étapes ; ce qui
// n'existe pas encore est montré, mais inerte (classe « a-venir »).

import { chargeTraduction, langueInitiale, LANGUES } from './i18n.js';
import { Anneaux, CycleTehilim, DureeSeance, JaugeSemaine, Plaque, Reference, Salutation } from './regles.js';
import { ART_DU_MOMENT, ChizukBag, ChizukRules } from './chizuk.js';
import { DanseMesure } from './seance.js';
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
const APPEL = ['char_praise', 'char_pointing', 'char_walking', 'char_tehilim', 'char_thinking', 'char_welcome', 'char_open_arms'];
const PENDANT = ['char_arms_raised', 'char_praying', 'char_standing_back', 'char_sitting_low'];
const salutation = (fait) => (fait ? duJour(JOIE, 3) : duJour(ACCUEIL, 0));
const joie = () => duJour(JOIE, 3);
const appel = () => duJour(APPEL, 1);
const pendant = () => duJour(PENDANT, 2);

// --- État de l'application -----------------------------------------------------------
let tr;
let morceaux = [];
let tikkun = null;
let chizuk = null;
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

/**
 * iOS ne laisse pas une page régler le volume : il reste aux boutons du
 * téléphone. La propriété accepte pourtant la valeur et la relit sans rien
 * changer au son (essai sur iPhone, 29/09) : c'est donc l'appareil qu'on
 * reconnaît. Un iPad récent se présente comme un Mac tactile.
 */
const surIOS = /iPad|iPhone|iPod/.test(navigator.userAgent)
  || (navigator.platform === 'MacIntel' && navigator.maxTouchPoints > 1);
const volumeReglable = !surIOS && (() => {
  const a = new Audio();
  a.volume = 0.5;
  return a.volume === 0.5;
})();

const titreMorceau = (m) => tr.t(m.titre);
const morceauChoisi = () => morceaux.find((m) => m.id === store.musicTrackId()) ?? morceaux[0] ?? null;

/**
 * Un message bref en bas de l'écran (le Toast d'Android), qui ne bloque
 * rien : une boîte d'alerte interromprait la séance.
 */
function signale(texte) {
  document.querySelector('.toast')?.remove();
  const t = h('div', { class: 'toast', role: 'status' }, texte);
  document.body.append(t);
  setTimeout(() => t.remove(), 3500);
}

/**
 * Une lecture qui n'a pas pu partir. Une lecture interrompue par un arrêt
 * voulu (séance close, autre morceau) n'en est pas une : Safari la signale
 * pourtant comme un échec (AbortError).
 */
function echecLecture(e) {
  if (e?.name === 'AbortError') return;
  signale(tr.t('music_play_failed'));
}

function metadonnees(m) {
  if (!('mediaSession' in navigator)) return;
  navigator.mediaSession.metadata = new MediaMetadata({
    title: titreMorceau(m), artist: 'MyHitbodedut',
    artwork: [{ src: 'icones/icon-512.png', sizes: '512x512', type: 'image/png' }],
  });
}

function ecouter(m) {
  if (enEcoute === m.id) { lecteur.pause(); enEcoute = null; rendre(); return; }
  lecteur.src = m.fichier;
  lecteur.loop = false;
  if (volumeReglable) lecteur.volume = store.musicVolume() / 100;
  enEcoute = m.id;
  metadonnees(m);
  lecteur.play().catch((e) => { enEcoute = null; rendre(); echecLecture(e); });
  rendre();
}

// --- Rendu : l'accueil ------------------------------------------------------------------
// Les trois anneaux (Anneaux.kt, AnneauxDessin.kt) : jour au centre, puis
// semaine, puis série. Un toucher met un anneau en avant ; la légende suit.
let anneauEnAvant = null;

function anneaux(etat) {
  const EPAISSEUR = 8 / 2.36; // 8 dp sur 236 dp
  const ECART = 3 / 2.36;
  const svg = s('svg', { viewBox: '0 0 100 100', 'aria-hidden': 'true' });
  const ordre = [etat.serie, etat.semaine, etat.jour]; // de l'extérieur vers le centre
  ordre.forEach((a, rang) => {
    const enAvant = anneauEnAvant === null || anneauEnAvant === a.quel;
    const trait = anneauEnAvant === a.quel ? EPAISSEUR * 1.3 : EPAISSEUR;
    const r = 50 - rang * (EPAISSEUR + ECART) - EPAISSEUR / 2;
    const tour = 2 * Math.PI * r;
    const g = s('g', { class: `anneau anneau-${a.quel}${enAvant ? '' : ' efface'}`, 'data-anneau': a.quel });
    g.append(s('circle', { class: 'piste', cx: 50, cy: 50, r, fill: 'none', 'stroke-width': trait }));
    if (a.tours >= 1) g.append(s('circle', { class: 'arc plein', cx: 50, cy: 50, r, fill: 'none', 'stroke-width': trait }));
    if (a.reste > 0) {
      const arc = s('circle', {
        class: `arc${a.tours >= 1 ? ' second-tour' : ''}`, cx: 50, cy: 50, r, fill: 'none', 'stroke-width': trait, 'stroke-linecap': 'round',
        'stroke-dasharray': tour, 'stroke-dashoffset': tour, transform: 'rotate(-90 50 50)',
      });
      g.append(arc);
      requestAnimationFrame(() => requestAnimationFrame(() => arc.setAttribute('stroke-dashoffset', tour * (1 - a.reste))));
    }
    // Une bande transparente plus large que le trait : le doigt n'est pas une pointe.
    g.append(s('circle', { class: 'prise', cx: 50, cy: 50, r, fill: 'none', 'stroke-width': EPAISSEUR + ECART, stroke: 'transparent' }));
    g.addEventListener('click', (e) => {
      e.stopPropagation();
      const quel = anneauEnAvant === a.quel ? null : a.quel;
      mettreEnAvantSurPlace(quel);
      // Toucher un anneau montre sa face au centre ; le relâcher ramène le hassid.
      tournerPlaque(quel ? Plaque.pour(quel) : 'hassid');
    });
    svg.append(g);
  });
  return svg;
}

/** La légende sous les anneaux : la semaine, ou l'anneau mis en avant. */
function legendeAnneaux(etat, progression) {
  if (anneauEnAvant === 'jour') {
    return tr.t(etat.jour.atteint ? 'ring_day_reached' : 'ring_day_detail', tr.duree(etat.jour.fait), tr.duree(etat.jour.objectif));
  }
  if (anneauEnAvant === 'serie') {
    return tr.t(etat.serie.atteint ? 'ring_streak_reached' : 'ring_streak_detail', etat.serie.fait, etat.serie.objectif);
  }
  return tr.p('week_gauge', progression.faits, progression.faits, progression.dus);
}

/**
 * Le saut du hassid, [fois] de suite. Sur l'image du rendu en cours : un
 * nouveau rendu la remplace, et l'ancienne sautait hors de l'écran.
 */
function sauter(fois) {
  const img = app.querySelector('.plaque img');
  if (!img) return;
  img.style.setProperty('--sauts', String(fois));
  img.classList.remove('salut');
  void img.offsetWidth; // relance l'animation CSS
  img.classList.add('salut');
}

// --- Le centre des anneaux (Plaque.kt) : le hassid, puis vos chiffres ---------
let facePlaque = 'hassid';
let minuteurPlaque = null;
const sansMouvement = () => matchMedia('(prefers-reduced-motion: reduce)').matches;

/** Le chiffre d'une face, la façon de l'écrire, et ce qu'il compte. */
function contenuFace(face) {
  const etat = store.anneaux();
  const nombre = (n) => String(n);
  switch (face) {
    case 'jour': return { cible: etat.jour.fait, texte: (n) => tr.duree(n), legende: tr.t('plaque_jour', tr.duree(etat.jour.objectif)) };
    case 'semaine': return { cible: etat.semaine.fait, texte: (n) => tr.t('plaque_semaine_valeur', n, etat.semaine.objectif), legende: tr.t('plaque_semaine') };
    case 'serie': return { cible: etat.serie.fait, texte: nombre, legende: tr.p('plaque_serie', etat.serie.fait, etat.serie.fait, etat.serie.objectif) };
    case 'meilleure': return { cible: store.meilleureSerie(), texte: nombre, legende: tr.t('plaque_meilleure') };
    case 'total': return { cible: store.totalMinutes(), texte: (n) => tr.duree(n), legende: tr.t('plaque_total') };
    case 'jours': {
      const jours = store.joursFaits().size;
      return { cible: jours, texte: nombre, legende: tr.p('plaque_jours', jours, jours) };
    }
    default: return { cible: Plaque.moyenne(store.totalMinutes(), store.joursFaits().size), texte: (n) => tr.duree(n), legende: tr.t('plaque_moyenne') };
  }
}

/** Le chiffre monte depuis zéro. */
function compter(el, cible, texte) {
  if (cible <= 0 || sansMouvement()) return;
  const debut = performance.now();
  const pas = (t) => {
    const f = Math.min(1, (t - debut) / 650);
    el.textContent = texte(Math.round(cible * (1 - (1 - f) ** 2)));
    if (f < 1 && el.isConnected) requestAnimationFrame(pas);
  };
  el.textContent = texte(0);
  requestAnimationFrame(pas);
}

/** Remplit la plaque avec la face courante. */
function remplirPlaque(btn) {
  if (facePlaque === 'hassid') {
    btn.setAttribute('aria-label', tr.t('rings_character_hint'));
    btn.replaceChildren(h('img', { src: `img/${salutation(store.record()?.faite === true)}.webp`, alt: '' }));
    return;
  }
  const c = contenuFace(facePlaque);
  const anneau = Plaque.anneau(facePlaque);
  const valeur = h('div', { class: `face-valeur${anneau ? ` valeur-${anneau}` : ''}` }, c.texte(c.cible));
  btn.setAttribute('aria-label', `${c.texte(c.cible)} ${c.legende}`);
  btn.replaceChildren(h('div', { class: 'face' }, valeur, h('div', { class: 'face-legende' }, c.legende)));
  compter(valeur, c.cible, c.texte);
}

/** Attend la fin d'une animation CSS de cet élément, pas de ses enfants. */
function apresAnimation(el, nom, suite) {
  const ecoute = (e) => {
    if (e.target !== el || e.animationName !== nom) return;
    el.removeEventListener('animationend', ecoute);
    suite();
  };
  el.addEventListener('animationend', ecoute);
}

/** La plaque tourne comme une pièce ; à mi-tour, la face change. */
function tournerPlaque(vers) {
  clearTimeout(minuteurPlaque);
  if (vers !== 'hassid') minuteurPlaque = setTimeout(() => tournerPlaque('hassid'), Plaque.RETOUR_MS);
  if (vers === facePlaque) return;
  const btn = app.querySelector('.plaque');
  if (!btn) { facePlaque = vers; return; }
  const changer = () => {
    facePlaque = vers;
    remplirPlaque(btn);
    mettreEnAvantSurPlace(Plaque.anneau(vers));
    btn.classList.remove('sortie');
    if (sansMouvement()) return;
    btn.classList.add('entree');
    apresAnimation(btn, 'plaque-entree', () => {
      btn.classList.remove('entree');
      if (vers === 'hassid') sauter(1);
    });
  };
  if (sansMouvement()) { changer(); return; }
  btn.classList.remove('entree');
  btn.classList.add('sortie');
  apresAnimation(btn, 'plaque-sortie', changer);
}

function plaqueAccueil() {
  const btn = h('button', { class: 'plaque', type: 'button', onclick: () => {
    navigator.vibrate?.(8);
    tournerPlaque(Plaque.suivante(facePlaque));
  } });
  remplirPlaque(btn);
  return btn;
}

/** Met un anneau en avant sans refaire le rendu, qui relancerait le remplissage. */
function mettreEnAvantSurPlace(quel) {
  anneauEnAvant = quel;
  app.querySelectorAll('.jauge .anneau').forEach((g) =>
    g.classList.toggle('efface', quel !== null && g.dataset.anneau !== quel));
  const legende = app.querySelector('.jauge-legende');
  if (legende) {
    legende.className = `jauge-legende${quel ? ` legende-${quel}` : ''}`;
    legende.textContent = legendeAnneaux(store.anneaux(), JaugeSemaine.progression(store.currentWeek()));
  }
}

/**
 * La fin du remplissage, quand l'accueil apparaît (MainActivity.apresRemplissage) :
 * un anneau qui vient de se fermer est fêté une fois, sinon le hassid salue.
 */
let accueilAnime = false;
function animerAccueil() {
  if (!app.querySelector('.jauge')) return;
  const etat = store.anneaux();
  const jour = store.today();
  const nouveaux = Anneaux.aFeter(etat, jour, store.anneauxFetes());
  store.setAnneauxFetes(Anneaux.marques(etat, jour));
  if (nouveaux.length === 0) { if (facePlaque === 'hassid') sauter(1); return; }
  // L'anneau fermé passe devant et sa face s'affiche le temps de la fête.
  const face = Plaque.pour(nouveaux[0]);
  const jauge = app.querySelector('.jauge');
  jauge.classList.remove('fete');
  void jauge.offsetWidth;
  jauge.classList.add('fete');
  mettreEnAvantSurPlace(nouveaux[0]);
  tournerPlaque(face);
  setTimeout(() => { if (facePlaque === face) { mettreEnAvantSurPlace(null); tournerPlaque('hassid'); } }, 2800);
}

function panneauAujourdhui() {
  const prenom = store.firstName();
  const record = store.record();
  // Une note seule ne fait pas la journée.
  const fait = record?.faite === true;
  const serie = store.streak();
  const semaine = store.currentWeek();
  const progression = JaugeSemaine.progression(semaine);
  const etat = store.anneaux();
  const nomJour = new Intl.DateTimeFormat(tr.langue, { weekday: 'short', timeZone: 'UTC' });

  const bouton = store.isSessionRunning() ? tr.t('action_back_to_session')
    : fait ? tr.t('action_another_session')
      : tr.t('action_start_session', tr.duree(store.targetMinutes()));

  const sujet = store.sessionTheme();
  const morceau = store.musicEnabled() ? morceauChoisi() : null;

  return h('div', { class: 'contenu' },
    h('div', { class: 'marque' }, tr.t('app_name')),
    prenom ? h('div', { class: 'salut' }, tr.t(Salutation.cle(new Date().getHours()), prenom)) : null,
    // Sans prénom, l'état du jour prend la place du titre.
    // Comme Android : en cours, faite, jour sans obligation, ou pas encore faite.
    h('div', { class: prenom ? 'statut' : 'statut titre' }, tr.t(
      store.isSessionRunning() ? 'status_running'
        : fait ? 'status_done'
          : semaine.filter((j) => !j.future).at(-1)?.exempt ? 'status_rest'
            : 'status_pending')),
    // Le premier jour, « 0 min au total » n'apprend rien : la phrase d'accueil reste seule.
    h('div', { class: 'serie' },
      h('span', {}, serie > 0 ? tr.p('status_streak', serie)
        : store.aDejaPratique() ? tr.t('status_streak_none') : tr.t('status_streak_first')),
      store.totalMinutes() > 0 ? h('span', { class: 'separateur', 'aria-hidden': 'true' }, '·') : null,
      store.totalMinutes() > 0 ? h('span', { class: 'total' }, tr.t('status_total', tr.duree(store.totalMinutes()))) : null),
    fait ? h('div', { class: 'detail' },
      record.minutes > 0 ? tr.t('today_minutes', tr.duree(record.minutes)) : tr.t('today_declared')) : null,

    h('div', { class: 'jauge', role: 'img', 'aria-label': tr.t('rings_description',
      tr.duree(etat.jour.fait), tr.duree(etat.jour.objectif), etat.semaine.fait, etat.semaine.objectif, etat.serie.fait, etat.serie.objectif) },
    anneaux(etat),
    plaqueAccueil()),
    h('div', { class: `jauge-legende${anneauEnAvant ? ` legende-${anneauEnAvant}` : ''}` }, legendeAnneaux(etat, progression)),

    h('button', { class: 'principal', onclick: () => ouvrir('seance') }, bouton),

    h('div', { class: 'pastilles' },
      pastille(tr.t('start_duration'), tr.duree(store.targetMinutes()), () => choisirDuree((m) => {
        store.setTargetMinutes(m); rendre();
      })),
      pastille(tr.t('start_theme'), sujet ? tr.t(`theme_${sujet}`) : tr.t('session_theme_none_short'), choisirSujet),
      pastille(tr.t('start_music'), morceau ? titreMorceau(morceau) : tr.t('start_music_off'), () => ouvrir('musique'))),

    h('div', { class: 'semaine' }, semaine.map((j) => {
      const repos = j.shabbat && j.exempt;
      // Le numéro du jour remplit les cases ouvertes : la bande se lit comme un calendrier.
      const marque = j.done ? '✓' : repos ? tr.t('week_shabbat_mark') : j.exempt ? '—' : String(Number(j.date.slice(8)));
      const etiquette = j.done ? tr.t('week_done') : repos ? tr.t('settings_shabbat')
        : j.exempt ? tr.t('gate_exempt_title') : tr.t('week_todo');
      const classe = ['jour', j.done ? 'fait' : j.exempt ? 'exempte' : '', repos && !j.done ? 'repos' : '',
        !j.done && !j.exempt && j.date === store.today() ? 'aujourdhui' : '', j.future ? 'avenir' : ''].join(' ').replace(/\s+/g, ' ').trim();
      return h('div', { class: classe, role: 'img', 'aria-label': `${nomJour.format(new Date(`${j.date}T12:00:00Z`))} : ${etiquette}` },
        h('span', { class: 'nom', 'aria-hidden': 'true' }, nomJour.format(new Date(`${j.date}T12:00:00Z`))),
        h('span', { class: 'marque-jour', 'aria-hidden': 'true' }, marque));
    })),

    fait ? h('button', { class: 'texte-bouton', onclick: () => ecrireNote(store.today(), rendre) }, tr.t('action_write_note')) : null);
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

// --- Tehilim (Tehilim.kt, TehilimActivity.kt) ------------------------------------
// Le livre entier, chargé au premier besoin : 327 ko qu'on ne lit pas à chaque ouverture.
let tehilim = null;
let lecture = null;
function chargeTehilim() {
  if (tehilim) return true;
  fetch('data/tehilim.json').then((r) => r.json()).then((d) => { tehilim = d.psaumes; rendre(); });
  return false;
}

/** Le jour du mois hébraïque et la longueur du mois (29 ou 30), par le calendrier du navigateur. */
function jourHebreu(date = new Date()) {
  const jour = (d) => Number(new Intl.DateTimeFormat('en-u-ca-hebrew', { day: 'numeric' }).format(d));
  const j = jour(date);
  const demain = new Date(date);
  demain.setDate(date.getDate() + 1);
  return { jour: j, longueur: j === 29 && jour(demain) === 1 ? 29 : 30 };
}

const nombreTehilim = (n) => (tr.langue === 'he' ? Reference.lettres(n) : String(n));

function libellePortion(p) {
  if (CycleTehilim.partielle(p)) {
    return tr.t('tehilim_range_verses', nombreTehilim(p.debut), nombreTehilim(p.debutVerset), nombreTehilim(p.finVerset));
  }
  if (p.debut === p.fin) return tr.t('tehilim_single', nombreTehilim(p.debut));
  return tr.t('tehilim_range', nombreTehilim(p.debut), nombreTehilim(p.fin));
}

function portionDuMois() {
  const { jour, longueur } = jourHebreu();
  return { jour, portion: CycleTehilim.duMois(jour, longueur) };
}

function lire(p) {
  lecture = p;
  ouvrir('lecture');
}

function ecranTehilim() {
  const { jour, portion } = portionDuMois();
  const auj = new Date();
  const semaine = CycleTehilim.deLaSemaine(auj.getDay());
  const nomJour = new Intl.DateTimeFormat(tr.langue, { weekday: 'long' }).format(auj);
  const pret = chargeTehilim();
  const versets = pret ? tehilim.reduce((n, ch) => n + ch.length, 0) : null;
  return h('div', { class: 'ecran' }, barre(tr.t('tehilim_title')),
    h('div', { class: 'defile' }, h('div', { class: 'contenu' },
      versets ? h('p', { class: 'indice' }, tr.t('tehilim_count', 150, versets)) : null,
      h('div', { class: 'carte' },
        ligne({ nom: tr.t('tehilim_month'), etat: tr.t('tehilim_month_state', nombreTehilim(jour), libellePortion(portion)), onclick: () => lire(portion) }),
        ligne({ nom: tr.t('tehilim_week'), etat: tr.t('tehilim_week_state', nomJour.charAt(0).toLocaleUpperCase(tr.langue) + nomJour.slice(1), libellePortion(semaine)), onclick: () => lire(semaine) })),
      h('h2', { class: 'section' }, tr.t('tehilim_all')),
      h('div', { class: 'grille-psaumes' }, Array.from({ length: 150 }, (_, i) => i + 1).map((n) =>
        h('button', { class: 'case-psaume', 'aria-label': libellePortion(CycleTehilim.portion(n, n)), onclick: () => lire(CycleTehilim.portion(n, n)) },
          nombreTehilim(n)))))));
}

function ecranLecture() {
  // Rouvert directement sur #lecture, sans passage choisi : le livre.
  if (!lecture) return ecranTehilim();
  const p = lecture;
  if (!chargeTehilim()) return h('div', { class: 'ecran' }, barre(libellePortion(p)), h('div', { class: 'defile' }));
  const seul = p.debut === p.fin && !CycleTehilim.partielle(p);
  const blocs = [];
  let total = 0;
  for (let n = p.debut; n <= p.fin; n++) {
    const tous = tehilim[n - 1];
    const quels = CycleTehilim.versets(p, n, tous.length);
    total += quels.length;
    blocs.push(h('h2', { class: 'psaume-titre', lang: 'he', dir: 'rtl' }, tr.t('tikkun_psalm_he', Reference.lettres(n))));
    blocs.push(h('div', { class: 'psaume', lang: 'he', dir: 'rtl' }, quels.map((v) =>
      h('p', { class: 'verset' }, h('span', { class: 'num', 'aria-hidden': 'true' }, `${Reference.lettres(v)} `), tous[v - 1]))));
  }
  const aller = (n) => { lecture = CycleTehilim.portion(n, n); rendre(); app.querySelector('.defile').scrollTop = 0; };
  return h('div', { class: 'ecran' }, barre(libellePortion(p)),
    h('div', { class: 'defile' }, h('div', { class: 'contenu lecture' },
      h('p', { class: 'indice' }, tr.p('tehilim_verses', total, total)),
      ...blocs)),
    seul ? h('div', { class: 'lecture-nav' },
      h('button', { class: 'secondaire', style: p.debut > 1 ? null : 'visibility:hidden', onclick: () => aller(p.debut - 1) }, tr.t('tehilim_previous')),
      h('button', { class: 'principal', style: p.debut < 150 ? null : 'visibility:hidden', onclick: () => aller(p.debut + 1) }, tr.t('tehilim_next'))) : null);
}

function panneauTextes() {
  return h('div', { class: 'contenu' },
    h('h1', { class: 'titre-ecran' }, tr.t('nav_texts')),
    h('div', { class: 'carte' },
      // Tehilim est le premier texte lisible sur le web (04/10).
      ligne({ nom: tr.t('tehilim_title'), etat: tr.t('tehilim_today', libellePortion(portionDuMois().portion)), onclick: () => ouvrir('tehilim') }),
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
  // Le journal n'existe pas encore sur le web : son onglet est masqué
  // plutôt que montré grisé (finition du 04/10).
  const items = [
    ['aujourdhui', 'nav_today'], ['textes', 'nav_texts'], ['reglages', 'settings_title'],
  ];
  return h('nav', { class: 'onglets' }, items.map(([cle, nom]) => h('button', {
    class: 'onglet',
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
    h('button', { class: 'retour', 'aria-label': tr.t('action_back'), onclick: retour }),
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

// --- Boîtes de dialogue ---------------------------------------------------------------
/** Une confirmation : titre, message, « Retour » et l'action (les AlertDialog de SessionActivity). */
function confirmer(titre, message, action, onConfirme) {
  feuille(titre, (f, fermer) => {
    f.append(h('p', { class: 'message' }, message),
      h('div', { class: 'actions' },
        h('button', { onclick: fermer }, tr.t('action_back')),
        h('button', { onclick: () => { fermer(); onConfirme(); } }, action)));
  });
}

/** NoteDialog : une note libre sur la journée, illustration de joie au-dessus. */
function ecrireNote(date, apres = () => {}) {
  feuille(tr.t('note_title'), (f, fermer) => {
    const champ = h('textarea', { rows: 4, placeholder: tr.t('note_hint'), 'aria-label': tr.t('note_title') });
    champ.value = store.record(date)?.note ?? '';
    f.append(h('img', { class: 'note-art', src: `img/${joie()}.webp`, alt: '' }), champ,
      h('div', { class: 'actions' },
        h('button', { onclick: () => { fermer(); apres(); } }, tr.t('action_later')),
        h('button', { onclick: () => { store.setNote(date, champ.value.trim()); fermer(); apres(); } }, tr.t('action_save'))));
  });
}

// --- La carte de ’hizouk (view_chizuk.xml) -------------------------------------------
// Le texte tiré pour l'écran de séance, et celui du bravo après une annulation.
let chizukSeance = null;
let chizukBravo = null;

function tireChizuk(moment) {
  if (!chizuk) return null;
  const item = ChizukBag.tirer(store, chizuk, moment);
  chizukSeance = item ? { moment, item } : null;
  return chizukSeance;
}

function carteChizuk(tire, { compacte = false } = {}) {
  if (!tire) return null;
  const { moment, item } = tire;
  const traduction = ChizukRules.traduction(item, tr.langue);
  return h('section', { class: `chizuk${compacte ? ' compacte' : ''}` },
    h('div', { class: 'chizuk-tete' },
      h('div', { class: 'chizuk-art' }, h('img', { src: `img/${ART_DU_MOMENT[moment]}.webp`, alt: '' })),
      h('h2', { class: 'chizuk-moment' }, tr.t(`moment_${moment}`))),
    // L'hébreu, de droite à gauche en toute langue.
    h('p', { class: 'chizuk-he ajuste', lang: 'he', dir: 'rtl' }, ChizukRules.hebreu(chizuk, item)),
    traduction ? h('hr', {}) : null,
    traduction ? h('p', { class: 'chizuk-trad ajuste' }, traduction) : null,
    h('p', { class: 'chizuk-ref' }, Reference.localise(item.ref, tr.langue)));
}

/**
 * Le corps qui se réduit jusqu'à tenir (autoSizeTextType d'Android) : la
 * carte garde sa hauteur, le texte ne déborde ni ne fait défiler.
 */
function ajusteLesTextes() {
  for (const el of app.querySelectorAll('.ajuste')) {
    const max = parseFloat(getComputedStyle(el).getPropertyValue('--corps-max')) || 19;
    let taille = max;
    el.style.fontSize = `${taille}px`;
    while (taille > 8 && el.scrollHeight > el.clientHeight + 1) {
      taille -= 1;
      el.style.fontSize = `${taille}px`;
    }
  }
}

// --- La séance (SessionActivity, SessionService) ----------------------------------------
let tic = null;
let etaitEnCours = false;
let verrouEcran = null;
/** Après « Terminer maintenant », la note est proposée au retour du bravo. */
let noteApresBravo = false;
/** { minutes } après une séance close, { annulee: true } après un abandon. */
let bravo = null;

async function gardeEcranAllume() {
  // FLAG_KEEP_SCREEN_ON d'Android. Refusé ou absent : l'écran s'éteint comme d'habitude.
  try { verrouEcran = await navigator.wakeLock?.request('screen'); } catch { verrouEcran = null; }
}
function relacheEcran() {
  verrouEcran?.release?.().catch(() => {});
  verrouEcran = null;
}

function demarreMusique() {
  if (!store.musicEnabled()) return;
  const m = morceauChoisi();
  if (!m) return;
  enEcoute = null;
  lecteur.src = m.fichier;
  lecteur.loop = true;
  if (volumeReglable) lecteur.volume = store.musicVolume() / 100;
  metadonnees(m);
  lecteur.play().catch(echecLecture);
}

/** Fondu court à la fin (MusicPlayer.stop) ; sur iPhone, où le volume ne se règle pas, l'arrêt est net. */
function arreteMusique() {
  if (lecteur.paused) return;
  if (!volumeReglable) { lecteur.pause(); return; }
  const depart = lecteur.volume;
  let pas = 10;
  const fondu = setInterval(() => {
    pas -= 1;
    lecteur.volume = Math.max(0, (depart * pas) / 10);
    if (pas <= 0) { clearInterval(fondu); lecteur.pause(); lecteur.volume = depart; }
  }, 60);
}

/** Lance une séance ; une durée choisie devient aussi la durée habituelle. */
function commencer(minutes) {
  if (minutes > 0) store.setTargetMinutes(minutes);
  store.beginSession(minutes);
  etaitEnCours = true;
  tireChizuk('ouverture');
  demarreMusique();
  gardeEcranAllume();
  // Le décompte s'affiche en haut : l'écran repart du début.
  delete app.dataset.ecran;
  rendre();
}

/**
 * SessionService.finishSession : le sujet se lit avant la clôture, les
 * minutes s'inscrivent, le bravo attend. Une séance arrêtée à zéro minute
 * n'inscrit rien.
 */
// Le jour de la séance qui vient de se clore : la note lui revient, pas au
// jour où l'on touche « Continuer » (une séance de 23 h 40 finit demain).
let dateNote = null;

function cloreSeance({ avantTerme }) {
  const sujet = store.sessionTheme();
  const { minutes, date } = store.endSession({ avantTerme });
  const inscrite = minutes > 0 || !avantTerme;
  if (inscrite) {
    store.addSession(minutes, { theme: sujet, date });
    store.setPendingCelebration(minutes);
    dateNote = date;
  }
  etaitEnCours = false;
  arreteMusique();
  relacheEcran();
  // Le mot de clôture, sur l'écran de séance (moment « cloture »).
  tireChizuk('cloture');
  return inscrite;
}

/** « Terminer maintenant » : la note est proposée après le bravo, ou tout de suite s'il n'y en a pas. */
function terminerMaintenant() {
  if (cloreSeance({ avantTerme: true })) noteApresBravo = true;
  else setTimeout(() => ecrireNote(dateNote ?? store.today(), rendre), 50);
  rendre();
}

function annulerSeance() {
  store.endSession({ avantTerme: true });
  etaitEnCours = false;
  arreteMusique();
  relacheEcran();
  chizukBravo = chizuk ? ChizukBag.tirer(store, chizuk, 'relance') : null;
  bravo = { annulee: true };
  history.pushState({ interne: true }, '', '#bravo');
  rendre();
}

const deuxChiffres = (n) => String(n).padStart(2, '0');

function compteur() {
  const mode = store.sessionCounter();
  const t = Date.now();
  if (mode === 'masque') return { texte: '', legende: tr.t('session_hidden_caption') };
  const ms = mode === 'ecoule'
    ? Math.max(0, t - store.sessionStartedAt())
    : Math.max(0, store.sessionEndsAt() - t);
  return {
    texte: `${deuxChiffres(Math.floor(ms / 60000))}:${deuxChiffres(Math.floor(ms / 1000) % 60)}`,
    legende: mode === 'ecoule' ? tr.t('session_open_caption') : tr.t('session_target', tr.duree(store.sessionTarget())),
  };
}

/** Chaque seconde : le décompte, et la fin quand elle arrive, écran allumé ou au retour. */
function demarreTic() {
  if (tic) return;
  tic = setInterval(() => {
    if (etaitEnCours && !store.isSessionRunning()) {
      cloreSeance({ avantTerme: false });
      rendre();
      return;
    }
    const el = app.querySelector('.decompte');
    if (el && store.isSessionRunning()) el.textContent = compteur().texte;
  }, 1000);
}
function arreteTic() {
  clearInterval(tic);
  tic = null;
}

// La lecture continue parfois écran verrouillé ; ses événements servent
// alors d'horloge pour arrêter la musique et clore la séance à son terme.
lecteur.addEventListener('timeupdate', () => {
  if (etaitEnCours && !store.isSessionRunning()) {
    cloreSeance({ avantTerme: false });
    // Sans ce rendu, la séance était close mais l'écran restait sur le décompte.
    rendre();
  }
});

function ecranSeance() {
  const enCours = store.isSessionRunning();
  etaitEnCours = enCours;
  const fait = store.isDone();
  const sujet = store.sessionTheme();
  const morceau = store.musicEnabled() ? morceauChoisi() : null;
  const aBlocage = chizuk && ChizukRules.pourMoment(chizuk, 'blocage').length > 0;
  const c = enCours ? compteur() : null;

  const corps = [
    h('div', { class: 'seance-titre' }, tr.t('session_screen_title')),
    h('div', { class: 'scene' }, h('img', { src: `img/${enCours ? pendant() : fait ? joie() : appel()}.webp`, alt: '' })),
    enCours
      ? (c.texte ? h('div', { class: 'decompte', role: 'timer' }, c.texte) : null)
      : h('div', { class: 'decompte' }, tr.duree(store.targetMinutes())),
    h('p', { class: 'legende' }, enCours ? c.legende : tr.t(fait ? 'session_idle_done' : 'session_idle_caption')),
    morceau ? h('p', { class: 'legende petite' }, tr.t('session_music_line', titreMorceau(morceau))) : null,
    carteChizuk(chizukSeance),
    enCours && aBlocage ? h('button', { class: 'texte-clair', onclick: () => { tireChizuk('blocage'); rendre(); } }, tr.t('chizuk_stuck')) : null,
    h('button', { class: 'texte-clair', onclick: choisirSujet },
      sujet ? tr.t('session_theme_chosen', tr.t(`theme_${sujet}`)) : tr.t('session_theme_none')),
  ];

  if (enCours) {
    corps.push(
      h('button', {
        class: 'principal', onclick: () => confirmer(tr.t('session_finish_title'), tr.t('session_finish_message'),
          tr.t('session_finish_now'), terminerMaintenant),
      }, tr.t('session_finish_now')),
      h('button', {
        class: 'texte-clair', onclick: () => confirmer(tr.t('session_cancel_title'), tr.t('session_cancel_message'),
          tr.t('session_cancel'), annulerSeance),
      }, tr.t('session_cancel')));
  } else {
    // Les durées proposées, en deux rangées de trois, puis la durée réglable et la séance sans durée.
    corps.push(
      h('div', { class: 'durees' }, DureeSeance.PROPOSEES.map((m) =>
        h('button', { class: 'duree', onclick: () => commencer(m) }, tr.duree(m)))),
      h('button', { class: 'texte-clair', onclick: () => saisirDuree(commencer) }, tr.t('duration_custom')),
      h('button', { class: 'texte-clair', onclick: () => commencer(0) }, tr.t('session_open')),
      h('button', { class: 'principal', onclick: () => commencer(store.targetMinutes()) },
        tr.t('session_start', tr.duree(store.targetMinutes()))));
  }

  return h('div', { class: 'ecran seance' },
    h('header', { class: 'barre' },
      h('button', { class: 'retour', 'aria-label': tr.t('action_back'), onclick: retour })),
    h('div', { class: 'defile' }, h('div', { class: 'contenu' }, corps)));
}

// --- Le bravo (CelebrationActivity) -------------------------------------------------
let danseEnCours = null;

function ecranBravo() {
  const annulee = !!bravo.annulee;
  const adresse = store.adresse();
  const titre = annulee
    ? tr.t(adresse === 'm' ? 'annulation_hazak' : adresse === 'f' ? 'annulation_hizki' : 'annulation_neutral')
    : tr.t(adresse === 'm' ? 'celebration_tsadik' : adresse === 'f' ? 'celebration_tsadeket' : 'celebration_neutral');
  const sousTitre = annulee ? tr.t('annulation_subtitle')
    : bravo.minutes > 0 ? tr.p('celebration_minutes', bravo.minutes) : null;
  const continuer = () => {
    bravo = null;
    chizukBravo = null;
    retour();
    if (noteApresBravo) {
      noteApresBravo = false;
      setTimeout(() => ecrireNote(dateNote ?? store.today(), rendre), 50);
    }
  };
  return h('div', { class: 'ecran bravo' },
    h('div', { class: 'defile' }, h('div', { class: 'contenu' },
      h('div', { class: 'scene danse' },
        h('div', { class: 'danseur' },
          h('img', { class: 'pose-a', src: `img/${annulee ? 'char_walking' : 'char_dancing'}.webp`, alt: '' }),
          annulee ? null : h('img', { class: 'pose-b', src: 'img/char_dancing_joy.webp', alt: '' }))),
      h('h1', { class: 'bravo-titre' }, titre),
      sousTitre ? h('p', { class: 'bravo-sous-titre' }, sousTitre) : null,
      annulee && chizukBravo ? carteChizuk({ moment: 'relance', item: chizukBravo }, { compacte: true }) : null,
      h('button', { class: 'principal', onclick: continuer }, tr.t('celebration_continue')))));
}

/**
 * La danse du hassid : à chaque temps il saute, retombe en pliant les
 * genoux, et change de jambe ; des notes montent de la scène. Rien si le
 * téléphone demande moins d'animations.
 */
function danse() {
  arreteDanse();
  if (bravo?.annulee || matchMedia('(prefers-reduced-motion: reduce)').matches) return;
  const scene = app.querySelector('.scene.danse');
  const danseur = app.querySelector('.danseur');
  const poseA = app.querySelector('.pose-a');
  const poseB = app.querySelector('.pose-b');
  if (!scene || !danseur || !poseB) return;
  const debut = performance.now();
  const periode = 2 * DanseMesure.TEMPS_MS;
  const image = (t) => {
    const p = DanseMesure.pas(((t - debut) % periode) / periode);
    danseur.style.transform = `translateY(${-16 * p.hauteur}px) scale(${1 + 0.04 * p.flexion}, ${1 - 0.05 * p.flexion}) rotate(${6 * p.balance}deg)`;
    poseA.style.opacity = String(1 - p.poseB);
    poseB.style.opacity = String(p.poseB);
    danseEnCours.image = requestAnimationFrame(image);
  };
  const SIGNES = ['♪', '♫', '♪', '✦'];
  const note = () => {
    const l = scene.clientWidth;
    const hauteur = scene.clientHeight;
    if (!l) return;
    const n = h('span', { class: 'note', 'aria-hidden': 'true' }, SIGNES[Math.floor(Math.random() * SIGNES.length)]);
    n.style.fontSize = `${18 + Math.floor(Math.random() * 8)}px`;
    n.style.left = `${l * ((Math.random() < 0.5 ? 0.18 : 0.72) + Math.random() * 0.1)}px`;
    n.style.top = `${hauteur * 0.55}px`;
    scene.append(n);
    const anim = n.animate([
      { transform: 'translate(0, 0) rotate(0deg)', opacity: 1 },
      { transform: `translate(${(Math.random() - 0.5) * 30}px, ${-hauteur * 0.45}px) rotate(${(Math.random() - 0.5) * 40}deg)`, opacity: 0 },
    ], { duration: 1700, easing: 'cubic-bezier(0, 0, 0.2, 1)' });
    anim.onfinish = () => n.remove();
  };
  danseEnCours = { image: requestAnimationFrame(image), notes: setInterval(note, DanseMesure.TEMPS_MS + 180) };
  note();
}
function arreteDanse() {
  if (!danseEnCours) return;
  cancelAnimationFrame(danseEnCours.image);
  clearInterval(danseEnCours.notes);
  danseEnCours = null;
}

// --- Navigation --------------------------------------------------------------------------
// Chaque écran secondaire est une entrée d'historique : le bouton retour
// et le geste de retour ramènent à l'accueil, sur l'onglet quitté.
function ecranCourant() {
  const e = location.hash.slice(1);
  return ['musique', 'reglages', 'seance', 'bravo', 'tehilim', 'lecture'].includes(e) ? e : 'accueil';
}
function ouvrir(ecran) {
  // L'entrée sur l'écran de séance tire le mot d'ouverture (SessionActivity.onCreate).
  if (ecran === 'seance' && ecranCourant() !== 'seance') tireChizuk('ouverture');
  if (ecranCourant() !== ecran) history.pushState({ interne: true }, '', `#${ecran}`);
  rendre();
}

/**
 * Le retour. Ouverte directement sur un écran secondaire (application
 * relancée), la page n'a pas d'historique où revenir : on rentre à l'accueil.
 */
function retour() {
  if (history.state?.interne) history.back();
  else { history.replaceState(null, '', location.pathname); rendre(); }
}
window.addEventListener('popstate', () => rendre());

function rendre() {
  // Une séance arrivée à son terme pendant que le téléphone dormait.
  if (store.isSessionOver()) cloreSeance({ avantTerme: false });
  // Le bravo d'une séance close attend le prochain écran ouvert.
  if (['accueil', 'seance'].includes(ecranCourant())) {
    const minutes = store.takePendingCelebration();
    if (minutes !== null) {
      bravo = { minutes };
      history.pushState({ interne: true }, '', '#bravo');
    }
  }
  if (ecranCourant() === 'bravo' && !bravo) history.replaceState(null, '', location.pathname);
  // La position de défilement survit à un nouveau rendu du même écran.
  const avant = app.querySelector('.defile')?.scrollTop ?? 0;
  const ecran = ecranCourant();
  if (ecran !== 'seance') arreteTic();
  if (ecran !== 'bravo') arreteDanse();
  const memeEcran = app.dataset.ecran === `${ecran}/${onglet}`;
  app.replaceChildren();
  if (ecran === 'musique') app.append(ecranMusique());
  else if (ecran === 'reglages') app.append(ecranReglages());
  else if (ecran === 'seance') app.append(ecranSeance());
  else if (ecran === 'bravo') app.append(ecranBravo());
  else if (ecran === 'tehilim') app.append(ecranTehilim());
  else if (ecran === 'lecture') app.append(ecranLecture());
  else {
    app.append(h('div', { class: 'ecran' },
      h('div', { class: 'defile' }, onglet === 'textes' ? panneauTextes() : panneauAujourdhui()),
      onglets()));
  }
  app.dataset.ecran = `${ecran}/${onglet}`;
  if (memeEcran) app.querySelector('.defile').scrollTop = avant;
  // L'accueil qui apparaît s'anime seul : anneaux, puis saut ou fête (04/10).
  const accueil = ecran === 'accueil' && onglet === 'aujourdhui';
  if (!accueil) { accueilAnime = false; facePlaque = 'hassid'; clearTimeout(minuteurPlaque); }
  else if (!accueilAnime) { accueilAnime = true; setTimeout(animerAccueil, 950); }
  ajusteLesTextes();
  if (ecran === 'seance') demarreTic();
  if (ecran === 'bravo') danse();
}

// --- Démarrage ---------------------------------------------------------------------------
async function demarrer() {
  tr = await chargeTraduction(langueInitiale(store.langueApp(), navigator.languages));
  appliqueApparence();
  const [m, t, c] = await Promise.all([
    fetch('data/musique.json').then((r) => r.json()),
    fetch('data/tikkun_haklali.json').then((r) => r.json()),
    fetch('data/chizuk.json').then((r) => r.json()),
  ]);
  morceaux = m;
  tikkun = t;
  chizuk = c;
  // Rouvert sur l'écran de séance : son mot d'ouverture.
  if (ecranCourant() === 'seance') tireChizuk('ouverture');
  rendre();
  // Revenir à l'application redessine l'écran : un autre jour, une séance
  // arrivée à son terme, le décompte qui a continué pendant le sommeil.
  document.addEventListener('visibilitychange', () => {
    if (document.hidden) return;
    accueilAnime = false;
    facePlaque = 'hassid';
    if (store.isSessionRunning()) gardeEcranAllume();
    rendre();
  });
}

if ('serviceWorker' in navigator) {
  navigator.serviceWorker.register('sw.js').catch(() => {});
}
demarrer();
