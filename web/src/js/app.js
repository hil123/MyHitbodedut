// L'application : l'accueil (MainActivity), la séance (SessionActivity),
// le bravo (CelebrationActivity), la musique (MusicActivity) et un premier
// écran de réglages. Les écrans suivants arrivent par étapes ; ce qui
// n'existe pas encore est montré, mais inerte (classe « a-venir »).

import { chargeTraduction, formate, langueInitiale, LANGUES } from './i18n.js';
import { Anneaux, CycleTehilim, DureeSeance, JaugeSemaine, LibraryRules, Plaque, Reference, Salutation, SiddurMoment, SignetsSiddur } from './regles.js';
import { ART_DU_MOMENT, ChizukBag, ChizukRules } from './chizuk.js';
import { DanseMesure } from './seance.js';
import { creeStore, memoire } from './store.js';
import { isoLocal } from './calendrier.js';
import { JERUSALEM_GEONAMES, jour as jourJuif, jourDeLaPriere } from './calendrier-juif.js';
import { conditionsPour, index as indexAjouts, libelleJour, vrai as ajoutVrai } from './siddur-jour.js';
import * as Paliers from './paliers.js';
import * as Trophees from './trophees.js';
import { FORMES } from './trophees-formes.js';
import { pour as halakhotPour, recherche as rechercheHalakha, source as sourceHalakha } from './halakha.js';
import { ajouts as ajoutsDuJour, dateHilloula, debutMoisHebreu, hilloulotPour, lienArticle, rechercheHilloula, horaires, illustration as illustrationDuJour, lienHalakha, lienHyomi, momentDuSiddour, moisHebreuVoisin, semaines, semainesHebreu } from './jour-affiche.js';
import { ALLUMAGES, heure as heureZman } from './zmanim.js';

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

  // Trois blocs : en-tête, anneaux, actions. Ils s'empilent ; sur un
  // téléphone couché, les anneaux passent à côté (app.css, « Téléphone couché »).
  return h('div', { class: 'contenu accueil' }, h('div', { class: 'accueil-tete' },
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
      record.minutes > 0 ? tr.t('today_minutes', tr.duree(record.minutes)) : tr.t('today_declared')) : null),

    h('div', { class: 'accueil-jauge' }, h('div', { class: 'jauge', role: 'img', 'aria-label': tr.t('rings_description',
      tr.duree(etat.jour.fait), tr.duree(etat.jour.objectif), etat.semaine.fait, etat.semaine.objectif, etat.serie.fait, etat.serie.objectif) },
    anneaux(etat),
    plaqueAccueil()),
    h('div', { class: `jauge-legende${anneauEnAvant ? ` legende-${anneauEnAvant}` : ''}` }, legendeAnneaux(etat, progression)),
    lignePaliers()),

    h('div', { class: 'accueil-actions' }, h('button', { class: 'principal', onclick: () => ouvrir('seance') }, bouton),

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

    fait ? h('button', { class: 'texte-bouton', onclick: () => ecrireNote(store.today(), rendre) }, tr.t('action_write_note')) : null,
    // Les trophées et les paliers, en bas de la page (11/10).
    h('button', { class: 'bouton-trophees', onclick: () => ouvrir('paliers') }, tr.t('trophees_paliers'))));
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
      versets ? h('p', { class: 'indice' }, tr.t('tehilim_count', 150, versets.toLocaleString(tr.langue))) : null,
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
  return ecranLu(libellePortion(p), h('div', { class: 'contenu lecture' },
      h('p', { class: 'indice' }, tr.p('tehilim_verses', total, total)),
      ...blocs), { avecNusah: false, pied: seul ? h('div', { class: 'lecture-nav' },
      h('button', { class: 'secondaire', style: p.debut > 1 ? null : 'visibility:hidden', onclick: () => aller(p.debut - 1) }, tr.t('tehilim_previous')),
      h('button', { class: 'principal', style: p.debut < 150 ? null : 'visibility:hidden', onclick: () => aller(p.debut + 1) }, tr.t('tehilim_next'))) : null });
}

// --- Siddour (SiddurActivity.kt, SiddurLectureActivity.kt) ------------------------
const siddurs = {};
let siddur = null;
let priere = null;
const NUSAH = [['edot', 'siddur_nusah_edot'], ['sefard', 'siddur_nusah_sefard'], ['ashkenaz', 'siddur_nusah_ashkenaz']];
/** Le siddour du nusah choisi, chargé au premier besoin (un fichier par nusah). */
/** Les livres entiers de la bibliothèque (data/livres.json, au format du siddour), chargés au premier besoin. */
let livres = null;
function chargeLivres() {
  if (livres) return true;
  fetch('data/livres.json').then((r) => r.json()).then((d) => { livres = d; rendre(); }).catch(() => {});
  return false;
}

function chargeSiddur() {
  const n = store.siddurNusah();
  if (siddurs[n]) { siddur = siddurs[n]; return true; }
  fetch(`data/siddur_${n}.json`).then((r) => r.json()).then((d) => { siddurs[n] = d; rendre(); });
  return false;
}

// --- Le bouton de lecture (ReglagesLecture.kt) -------------------------------------
// Une petite pastille « Aa » au bas du texte, aux couleurs de l'utilisateur.
// Touchée, elle s'ouvre en barre : taille des lettres, lecture de nuit,
// nusah au siddour, masquer, replier. Masquée, elle disparaît de tous les
// écrans de lecture ; un appui long sur le texte la fait revenir.
let reglagesOuverts = false;
let choixNusahOuvert = false;
const ECRANS_LUS = ['lecture', 'priere', 'tikkun', 'source'];

/** Les chaînes nouvelles, tant qu'Android ne les a pas encore : le français. */
const CHAINES_LECTURE = {
  lecture_reglages: 'Réglages de lecture',
  lecture_nuit: 'Lecture sur fond noir',
  lecture_plus: 'Lettres plus grandes',
  lecture_moins: 'Lettres plus petites',
  lecture_masquer: 'Masquer le bouton',
  lecture_masque_info: 'Bouton masqué. Appui long sur le texte pour le faire revenir.',
  lecture_annuler_masque: 'Annuler',
  lecture_replier: 'Replier',
  lecture_taille: 'Taille du texte : %1$d %%',
};
function tl(cle, ...args) {
  try { return tr.t(cle, ...args); } catch { return formate(CHAINES_LECTURE[cle] ?? cle, args); }
}

// Au trait, bouts arrondis : le dessin des autres icônes de l'application.
const ICONES_LECTURE = {
  lune: ['M19.5,14.6 A7.8,7.8 0 1,1 9.4,4.5 A6.2,6.2 0 0,0 19.5,14.6 Z'],
  masquer: ['M3,12 C5,8 8.2,6 12,6 C15.8,6 19,8 21,12 C19,16 15.8,18 12,18 C8.2,18 5,16 3,12 Z', 'M9.2,12 a2.8,2.8 0 1,0 5.6,0 a2.8,2.8 0 1,0 -5.6,0', 'M4,4 L20,20'],
  replier: ['M6.5,9.5 L12,15 L17.5,9.5'],
  coche: ['M5,12.5 L10,17 L19,7.5'],
};
function icone(nom) {
  const svg = s('svg', { viewBox: '0 0 24 24', 'aria-hidden': 'true' });
  for (const d of ICONES_LECTURE[nom]) {
    svg.append(s('path', { d, fill: 'none', stroke: 'currentColor', 'stroke-width': '1.9', 'stroke-linecap': 'round', 'stroke-linejoin': 'round' }));
  }
  return svg;
}

/** Un nouveau rendu, puis le doigt (ou le clavier) retrouve le bon bouton. */
/** La police choisie des textes hébreux (12/10), posée sur toute la page. */
const POLICES = {
  serif: '"Hebreu Serif", "Noto Serif Hebrew", "Times New Roman", serif',
  sans: '"Noto Sans Hebrew", "Arial Hebrew", system-ui, sans-serif',
  arrondie: '"Varela Round", system-ui, sans-serif',
};
function appliquerPolice() {
  document.documentElement.style.setProperty('--police-texte', POLICES[store.lecturePolice()]);
  // La taille choisie à la lecture vaut aussi pour les cartes de halakha et de hilloulot.
  document.documentElement.style.setProperty('--echelle-texte', String(store.lectureEchelle()));
}

function rendreEtViser(selecteur) {
  rendre();
  app.querySelector(selecteur)?.focus({ preventScroll: true });
}

/** Un message bref avec une action (le Snackbar d'Android). */
function signaleAvecAction(texte, action, onAction, duree = 5000) {
  document.querySelector('.toast')?.remove();
  const t = h('div', { class: 'toast toast-action', role: 'status' },
    h('span', {}, texte),
    h('button', { type: 'button', onclick: () => { t.remove(); onAction(); } }, action));
  document.body.append(t);
  setTimeout(() => t.remove(), duree);
}

function masquerLecture() {
  store.setLectureMasquee(true);
  reglagesOuverts = false;
  choixNusahOuvert = false;
  rendre();
  signaleAvecAction(tl('lecture_masque_info'), tl('lecture_annuler_masque'), () => {
    store.setLectureMasquee(false);
    rendreEtViser('.lecture-pastille');
  });
}

function ramenerLecture() {
  store.setLectureMasquee(false);
  reglagesOuverts = true;
  document.querySelector('.toast')?.remove();
  navigator.vibrate?.(10);
  rendreEtViser('.lecture-barre button');
}

function reglagesLecture({ avecNusah, sansNuit = false }) {
  if (store.lectureMasquee()) return null;
  if (!reglagesOuverts) {
    return h('div', { class: 'lecture-ctrl' },
      h('button', {
        type: 'button', class: 'lecture-pastille', lang: 'en', dir: 'ltr', 'aria-label': tl('lecture_reglages'), 'aria-expanded': 'false',
        onclick: () => { reglagesOuverts = true; rendreEtViser('.lecture-barre button'); },
      }, 'Aa'));
  }
  const e = store.lectureEchelle();
  const pourcent = Math.round(e * 100);
  const echelle = (n) => {
    const v = Math.round(Math.min(2, Math.max(0.8, n)) * 10) / 10;
    if (v !== e) { store.setLectureEchelle(v); rendre(); }
  };
  const nuit = store.lectureSombre();
  const bouton = (attrs, ...enfants) => h('button', { type: 'button', ...attrs }, ...enfants);
  const separateur = () => h('span', { class: 'lecture-sep', 'aria-hidden': 'true' });
  return h('div', { class: 'lecture-ctrl' },
    choixNusahOuvert ? h('div', { class: 'lecture-nusah', role: 'menu', 'aria-label': tl('siddur_choose') }, NUSAH.map(([cle, nom]) => {
      const courant = cle === store.siddurNusah();
      return bouton({
        role: 'menuitemradio', 'aria-checked': String(courant),
        onclick: () => { store.setSiddurNusah(cle); choixNusahOuvert = false; rendreEtViser('.lecture-nusah-bouton'); },
      }, h('span', { class: 'coche' }, courant ? icone('coche') : null), tr.t(nom));
    })) : null,
    h('div', { class: 'lecture-barre', role: 'toolbar', 'aria-label': tl('lecture_reglages') },
      bouton({ class: 'lettres', lang: 'en', dir: 'ltr', 'aria-label': tl('lecture_moins'), disabled: e <= 0.8, onclick: () => echelle(e - 0.1) }, 'A−'),
      bouton({ class: 'taille', 'aria-label': tl('lecture_taille', pourcent), onclick: () => echelle(1) },
        new Intl.NumberFormat(tr.langue, { style: 'percent' }).format(pourcent / 100)),
      bouton({ class: 'lettres', lang: 'en', dir: 'ltr', 'aria-label': tl('lecture_plus'), disabled: e >= 2, onclick: () => echelle(e + 0.1) }, 'A+'),
      separateur(),
      // « אב » : la police suivante (hébraïque à empattements, sans, arrondie), pour tous les textes.
      bouton({ class: 'lecture-police', lang: 'he', dir: 'rtl', 'aria-label': `${tl('lecture_police')} : ${tl(`police_${store.lecturePolice()}`)}`, title: tl(`police_${store.lecturePolice()}`),
        onclick: () => { const l = ['serif', 'sans', 'arrondie']; store.setLecturePolice(l[(l.indexOf(store.lecturePolice()) + 1) % l.length]); appliquerPolice(); rendreEtViser('.lecture-police'); } }, 'אב'),
      sansNuit ? null : bouton({ class: 'icone-bouton', 'aria-label': tl('lecture_nuit'), 'aria-pressed': String(nuit), onclick: () => { store.setLectureSombre(!nuit); rendreEtViser('.lecture-barre [aria-pressed]'); } }, icone('lune')),
      avecNusah ? bouton({
        class: 'lecture-nusah-bouton', 'aria-haspopup': 'menu', 'aria-expanded': String(choixNusahOuvert),
        onclick: () => { choixNusahOuvert = !choixNusahOuvert; rendreEtViser(choixNusahOuvert ? '.lecture-nusah [aria-checked="true"]' : '.lecture-nusah-bouton'); },
      }, tr.t('siddur_choose')) : null,
      separateur(),
      bouton({ class: 'icone-bouton', 'aria-label': tl('lecture_masquer'), onclick: masquerLecture }, icone('masquer')),
      bouton({ class: 'icone-bouton', 'aria-label': tl('lecture_replier'), 'aria-expanded': 'true', onclick: () => { reglagesOuverts = false; choixNusahOuvert = false; rendreEtViser('.lecture-pastille'); } }, icone('replier'))));
}

/**
 * L'appui long sur le texte, quand le bouton est masqué : 550 ms sans
 * bouger de plus de 10 px, ou le menu contextuel. Rien n'est empêché tant
 * qu'il n'a pas abouti : le texte défile comme d'habitude.
 */
function appuiLongPourRamener(zone) {
  let minuteur = null;
  let depart = null;
  let abouti = 0;
  const annule = () => { clearTimeout(minuteur); minuteur = null; depart = null; };
  zone.addEventListener('pointerdown', (e) => {
    if (!store.lectureMasquee() || (e.pointerType === 'mouse' && e.button !== 0)) return;
    depart = { x: e.clientX, y: e.clientY };
    minuteur = setTimeout(() => { minuteur = null; abouti = Date.now(); ramenerLecture(); }, 550);
  });
  zone.addEventListener('pointermove', (e) => {
    if (depart && Math.hypot(e.clientX - depart.x, e.clientY - depart.y) > 10) annule();
  });
  for (const fin of ['pointerup', 'pointercancel', 'pointerleave', 'scroll']) zone.addEventListener(fin, annule, { passive: true });
  zone.addEventListener('contextmenu', (e) => {
    if (Date.now() - abouti < 800) { e.preventDefault(); return; }
    if (!store.lectureMasquee()) return;
    e.preventDefault();
    annule();
    abouti = Date.now();
    ramenerLecture();
  });
  // Le doigt qui se lève après l'appui long ne touche rien d'autre.
  zone.addEventListener('click', (e) => {
    if (Date.now() - abouti < 800) { e.preventDefault(); e.stopPropagation(); }
  }, true);
}

/**
 * Un écran de lecture de texte saint : le texte à la taille choisie, de
 * nuit si demandé, et le bouton de lecture au-dessus du pied d'écran.
 */
function ecranLu(titre, contenu, { avecNusah = false, pied = null, bande = null } = {}) {
  const defile = h('div', { class: 'defile' }, contenu);
  const ctrl = reglagesLecture({ avecNusah });
  let avant = 0;
  defile.addEventListener('scroll', () => {
    const p = ctrl?.querySelector('.lecture-pastille');
    const y = defile.scrollTop;
    if (p) {
      // En bas du texte, la pastille revient : on n'a plus à remonter pour la trouver.
      const enBas = y + defile.clientHeight >= defile.scrollHeight - 4;
      if (enBas || y < avant - 4) p.classList.remove('cachee');
      else if (y > avant + 4) p.classList.add('cachee');
    }
    avant = y;
  }, { passive: true });
  appuiLongPourRamener(defile);
  return h('div', { class: `ecran lu${store.lectureSombre() ? ' nuit' : ''}`, style: `--echelle: ${store.lectureEchelle()}` },
    barre(titre),
    bande,
    h('div', { class: `lu-zone${pied ? '' : ' sans-pied'}` }, defile, ctrl),
    pied);
}

/**
 * Le balisage du siddour, en nœuds : seuls <b>, <small> et <br> existent dans
 * siddur.json (tools/siddur_construction.py) ; tout le reste reste du texte.
 */
function riche(html) {
  const racine = document.createDocumentFragment();
  const pile = [racine];
  for (const morceau of html.split(/(<\/?b>|<\/?small>|<br>)/)) {
    if (!morceau) continue;
    const ouvre = morceau.match(/^<(b|small)>$/);
    if (ouvre) { const el = document.createElement(ouvre[1]); pile.at(-1).append(el); pile.push(el); }
    else if (/^<\/(b|small)>$/.test(morceau)) { if (pile.length > 1) pile.pop(); }
    else if (morceau === '<br>') pile.at(-1).append(document.createElement('br'));
    else pile.at(-1).append(document.createTextNode(morceau));
  }
  return racine;
}

const titreTexte = (titre) => (tr.langue === 'he' ? titre.he : (titre[tr.langue] || titre.he));

/** Une ligne de section : le nom hébreu, sa traduction dessous, le chevron. */
function ligneHebreu(hebreu, traduction, onclick) {
  return h('button', { class: 'ligne ligne-he', onclick },
    h('span', { class: 'corps' },
      h('span', { class: 'nom-he', lang: 'he', dir: 'rtl' }, hebreu),
      traduction && traduction !== hebreu ? h('span', { class: 'etat' }, traduction) : null),
    h('span', { class: 'chevron', 'aria-hidden': 'true' }));
}

/** Le personnage sur son panneau clair, comme sur l'accueil (bg_stage). */
function plateau(dessin, classe) {
  return h('span', { class: `plateau ${classe}`, 'aria-hidden': 'true' }, h('img', { src: `img/${dessin}.webp`, alt: '' }));
}

/**
 * Le bandeau d'un écran à l'identité de l'application (bg_hero) : le
 * retour, le titre, ce qui se règle dessous, et le hassid sur son panneau.
 */
function bandeau(titre, { puce = null, dessin = null } = {}) {
  return h('header', { class: 'bandeau' },
    h('button', { class: 'retour', 'aria-label': tr.t('action_back'), onclick: retour }),
    h('div', { class: 'bandeau-corps' }, h('h1', {}, titre), puce),
    dessin ? plateau(dessin, 'plateau-bandeau') : null);
}

/** Le nom du groupe en hébreu, à la couleur choisie ; sa traduction à côté. */
function enteteGroupe(hebreu, traduction) {
  const enHebreu = /[֐-׿]/.test(hebreu);
  return h('h2', { class: 'entete-groupe' },
    h('span', { class: `entete-he${enHebreu ? ' serif-he' : ''}`, lang: enHebreu ? 'he' : null, dir: enHebreu ? 'rtl' : null }, hebreu),
    traduction && traduction !== hebreu ? h('span', { class: 'entete-trad' }, traduction) : null);
}

/** Les trois nusah ; le choix est gardé, et vaut aussi dans les lectures. */
function choisirNusah() {
  feuille(tr.t('siddur_choose'), (f, fermer) => {
    f.append(h('div', { role: 'radiogroup', 'aria-label': tr.t('siddur_choose') }, NUSAH.map(([cle, nom]) => {
      const courant = cle === store.siddurNusah();
      return h('button', {
        type: 'button', class: 'choix choix-radio', role: 'radio', 'aria-checked': String(courant),
        onclick: () => { fermer(); if (!courant) { store.setSiddurNusah(cle); rendre(); } },
      }, h('span', { class: 'radio', 'aria-hidden': 'true' }), tr.t(nom));
    })));
  });
  document.querySelector('.feuille [aria-checked="true"]')?.focus();
}

/** La suggestion de l'heure : le hassid, l'heure dite en clair, le texte. */
function carteMaintenant(x) {
  const traduction = titreTexte(x.titre) !== x.titre.he ? titreTexte(x.titre) : null;
  return h('button', { class: 'maintenant', onclick: () => { priere = x.id; ouvrir('priere'); } },
    plateau(SiddurMoment.illustration(x.id), 'plateau-maintenant'),
    h('span', { class: 'maintenant-corps' },
      h('span', { class: 'maintenant-libelle' }, tr.t('siddur_maintenant')),
      h('span', { class: 'maintenant-he', lang: 'he', dir: 'rtl' }, x.titre.he),
      traduction ? h('span', { class: 'maintenant-trad' }, traduction) : null),
    h('span', { class: 'chevron', 'aria-hidden': 'true' }));
}

/** Un livre en tuile : son personnage, son nom hébreu, son nom traduit. */
function tuileLivre(hebreu, traduction, dessin, onclick) {
  return h('button', { class: 'livre', onclick },
    plateau(dessin, 'plateau-livre'),
    h('span', { class: 'livre-he', lang: 'he', dir: 'rtl' }, hebreu),
    traduction !== hebreu ? h('span', { class: 'livre-trad' }, traduction) : null);
}

/**
 * Le siddour (SiddurActivity, 04/10), dans la manière de l'application : en
 * tête, le texte que suggère l'heure ; puis Tehilim et le Tikoun côte à
 * côte ; puis les textes par groupe. Le nusah se choisit sur le bandeau.
 */
function ecranSiddur() {
  const pret = chargeSiddur();
  const nomNusah = pret ? titreTexte(siddur._meta.nusah) : tr.t(NUSAH.find(([c]) => c === store.siddurNusah())?.[1] ?? 'siddur_nusah');
  const heure = new Date();
  // Avec un lieu, la suggestion suit ses zmanim ; sans lieu, les heures fixes.
  const moment = store.lieu() ? momentDuSiddour(store.lieu(), heure) : SiddurMoment.suggestion(heure.getHours(), heure.getMinutes());
  const suggere = pret && moment ? siddur.textes.find((x) => x.id === moment) : null;
  const puce = h('button', {
    type: 'button', class: 'puce-bandeau', 'aria-haspopup': 'dialog', 'aria-label': `${tr.t('siddur_choose')} : ${nomNusah}`, onclick: choisirNusah,
  }, h('span', {}, nomNusah), icone('replier'));
  return h('div', { class: 'ecran avec-bandeau' }, bandeau(tr.t('siddur_title'), { puce, dessin: 'char_praying' }),
    h('div', { class: 'defile' }, h('div', { class: 'contenu liste-siddour' },
      suggere ? carteMaintenant(suggere) : null,
      enteteGroupe(tr.t('siddur_books'), null),
      h('div', { class: 'livres' },
        tuileLivre(tr.t('siddur_tehilim_he'), tr.t('tehilim_title'), 'char_tehilim', () => ouvrir('tehilim')),
        tuileLivre(tr.t('siddur_tikkun_he'), tr.t('tikkun_title'), 'char_torah', () => ouvrir('tikkun'))),
      ...(pret ? siddur.groupes.flatMap((g) => [
        enteteGroupe(g.titre.he, titreTexte(g.titre)),
        h('div', { class: 'carte carte-sections' }, siddur.textes.filter((x) => x.groupe === g.cle).map((x) =>
          // Un texte absent des sources de ce nusah, emprunté à un autre, le dit.
          ligneHebreu(x.titre.he, [titreTexte(x.titre) !== x.titre.he ? titreTexte(x.titre) : null, x.nusah ? titreTexte(x.nusah) : null].filter(Boolean).join(' · '),
            () => { priere = x.id; ouvrir('priere'); }))),
      ]) : []),
      // Les seules mentions que les licences demandent (CC-BY du siddour Metsudah, Wikipédia), sans Sefaria.
      pret && mentionsObligatoires(siddur._meta.sources).length
        ? h('p', { class: 'indice sources' }, tr.t('siddur_sources', mentionsObligatoires(siddur._meta.sources).join(' ; '))) : null)));
}

function ecranPriere() {
  // Un livre de la bibliothèque (« livre:likoutei_etsot », 12/10) se lit comme une prière, sans nusah ni ajouts.
  const livre = priere?.startsWith('livre:') ?? false;
  if (livre ? !chargeLivres() : !chargeSiddur()) return h('div', { class: 'ecran' }, barre(tr.t(livre ? 'library_title' : 'siddur_title')), h('div', { class: 'defile' }));
  const recueil = livre ? livres : siddur;
  const x = recueil.textes.find((y) => y.id === (livre ? priere.slice(6) : priere));
  // Rouvert directement sur #priere, sans texte choisi : la liste.
  if (!x) return ecranSiddur();
  const plusieurs = x.sections.length > 1;
  const sousTitre = [titreTexte(x.titre) !== x.titre.he ? titreTexte(x.titre) : null, titreTexte(x.nusah ?? recueil._meta.nusah)].filter(Boolean).join(' · ');
  const titres = [];
  // Les ajouts du jour (10/10) : mis en évidence ; les autres, repliés.
  const annotations = !livre && chargeAnnotations() ? annotationsSiddur[store.siddurNusah()]?.[x.id] : null;
  const idx = indexAjouts(annotations ?? []);
  const maintenant = new Date();
  const jourDuTexte = jourJuif(jourDeLaPriere(x.id, isoLocal(maintenant), maintenant.getHours()), store.inIsrael(), mukafChoma());
  const nusah = store.siddurNusah();
  const c = conditionsPour(nusah, store.inIsrael(), jourDuTexte.conditions, store.sansTahanounFinTichri());
  const nom = (a, condition) => tr.t(libelleJour(a.libelle, condition, nusah, c));
  const paragraphe = (s, i) => {
    const b = s.blocs[i];
    return h('p', { class: b.c !== undefined ? 'consigne' : 'priere', lang: 'he', dir: 'rtl' }, riche(b.c ?? b.t));
  };
  // « Aujourd'hui · הַלֵּל › » : le texte à dire là ce jour-là, ouvert d'un geste ; rien hors du jour.
  const renvois = (si, i) => (idx.renvois.get(`${si}:${i ?? ''}`) ?? []).filter((a) => ajoutVrai(a.si, c)).map((a) => {
    const cible = siddur.textes.find((y) => y.id === a.cible);
    return cible ? h('button', { type: 'button', class: 'renvoi-jour', lang: 'he', dir: 'rtl', onclick: () => ouvrirRenvoi(x.id, cible.id) },
      `${tr.t('siddur_aujourdhui')} · ${cible.titre.he}  ›`) : null;
  });
  const avecIndices = (si, i, p) => {
    const du = (idx.indices.get(`${si}:${i}`) ?? []).filter((a) => ajoutVrai(a.si, c));
    return [p, ...du.map((a) => h('p', { class: 'indice-jour' },
      h('span', {}, `${tr.t('siddur_aujourdhui')} : `), h('span', { lang: 'he', dir: 'rtl' }, a.texte))), ...renvois(si, i)];
  };
  const contenu = h('div', { class: 'contenu lecture siddour' },
    h('p', { class: 'indice' }, sousTitre),
    annotations ? h('p', { class: 'indice note-ajouts' }, tr.t('siddur_jour_ajouts', jourDuTexte.dateHebraique,
      tr.t(store.inIsrael() ? 'region_israel' : 'region_diaspora'))) : null,
    ...x.sections.flatMap((s, si) => {
      const t = plusieurs && s.titre ? h('h2', { class: 'psaume-titre', lang: 'he', dir: 'rtl' }, s.titre) : null;
      if (t) titres.push({ titre: s.titre, el: t });
      const section = idx.sections.get(si);
      if (section) return [t, ajout(nom(section, section.si), ajoutVrai(section.si, c), s.blocs.map((_, i) => paragraphe(s, i))), ...renvois(si)];
      const corps = [];
      for (let i = 0; i < s.blocs.length; i++) {
        const a = idx.blocs.get(`${si}:${i}`);
        if (!a) { corps.push(...avecIndices(si, i, paragraphe(s, i))); continue; }
        // Le groupe : les blocs suivants de même groupe, variantes comprises.
        const membres = [];
        while (i < s.blocs.length && idx.blocs.get(`${si}:${i}`)?.g === a.g) {
          const m = idx.blocs.get(`${si}:${i}`);
          const p = paragraphe(s, i);
          if (!ajoutVrai(m.si, c)) p.classList.add('hors-jour');
          membres.push(p);
          i++;
        }
        i--;
        corps.push(ajout(nom(a, a.racine ?? a.si), ajoutVrai(a.racine ?? a.si, c), membres));
      }
      return [t, ...corps, ...renvois(si)];
    }));
  const bande = titres.length > 1 ? bandeSignets(titres) : null;
  const ecran = ecranLu(x.titre.he, contenu, { avecNusah: !livre, bande: bande?.nav });
  bande?.suivre(ecran.querySelector('.defile'));
  return ecran;
}

// Les annotations des ajouts (tools/siddur_annotations.py), chargées une fois.
let annotationsSiddur = null;
let annotationsDemandees = false;
function chargeAnnotations() {
  if (annotationsSiddur) return true;
  if (!annotationsDemandees) {
    annotationsDemandees = true;
    fetch('data/siddur_annotations.json').then((r) => r.json()).then((d) => { annotationsSiddur = d; rendre(); }).catch(() => {});
  }
  return false;
}

/**
 * Un ajout lié au jour, sous son [nom] déjà traduit. S'il s'applique : mis en
 * évidence. Sinon : replié en une ligne ; la flèche l'ouvre et le referme.
 * Rien n'est jamais retiré : l'annotation n'est qu'indicative.
 */
function ajout(nom, applique, blocs) {
  if (applique) {
    return h('div', { class: 'ajout du-jour' }, h('p', { class: 'ajout-etiquette' }, `${tr.t('siddur_aujourdhui')} · ${nom}`), blocs);
  }
  const corps = h('div', { class: 'ajout-corps', hidden: true }, blocs);
  const bouton = h('button', { type: 'button', class: 'replie-bouton', 'aria-expanded': 'false' },
    h('span', { class: 'replie-nom' }, nom), h('span', { class: 'replie-etat' }, tr.t('siddur_pas_aujourdhui')));
  bouton.addEventListener('click', () => {
    const ouvert = corps.hidden;
    corps.hidden = !ouvert;
    bouton.setAttribute('aria-expanded', String(ouvert));
    bouton.querySelector('.replie-etat').textContent = tr.t(ouvert ? 'siddur_replier' : 'siddur_pas_aujourdhui');
  });
  return h('div', { class: 'ajout replie' }, bouton, corps);
}

/**
 * La bande de signets en tête de la lecture (demande du 10/10, comme
 * SiddurLectureActivity) : une section par signet ; on touche, on y est ; la
 * section qu'on lit se marque d'elle-même (SignetsSiddur). Le signet touché
 * reste marqué tant que le doigt ne reprend pas la page : une section courte
 * de fin ne monte jamais jusqu'en haut.
 */
function bandeSignets(titres) {
  let defile = null;
  let choisi = null;
  let actif = -1;
  // Position d'un titre dans le texte qui défile, quel que soit le défilement.
  const position = (el) => el.getBoundingClientRect().top - defile.getBoundingClientRect().top + defile.scrollTop;
  const signets = titres.map(({ titre, el }, i) => h('button', {
    type: 'button', class: 'signet', lang: 'he',
    onclick: () => { choisi = i; defile.scrollTop = position(el) - 6; marque(i); },
  }, titre));
  const nav = h('nav', { class: 'signets', dir: 'rtl', 'aria-label': tr.t('siddur_signets') }, signets);

  function marque(i) {
    if (i === actif) return;
    actif = i;
    signets.forEach((b, k) => b.setAttribute('aria-current', k === i ? 'true' : 'false'));
    // Le signet en cours reste en vue, au milieu de la bande. offsetLeft et
    // scrollLeft se comptent du même bord, en RTL comme en LTR.
    const b = signets[i];
    nav.scrollTo({ left: b.offsetLeft - (nav.clientWidth - b.offsetWidth) / 2, behavior: 'smooth' });
  }

  function suit() {
    if (!defile || !defile.isConnected) return;
    if (choisi !== null) { marque(choisi); return; }
    const haut = defile.scrollTop;
    const bas = haut + defile.clientHeight;
    marque(SignetsSiddur.courant(titres.map(({ el }) => position(el)), haut + 12, bas - 1, bas >= defile.scrollHeight - 4));
  }

  return {
    nav,
    suivre(d) {
      defile = d;
      defile.addEventListener('scroll', suit, { passive: true });
      for (const geste of ['touchstart', 'wheel', 'keydown']) defile.addEventListener(geste, () => { choisi = null; }, { passive: true });
      requestAnimationFrame(suit);
    },
  };
}

// --- Le Tikoun lu d'un seul trait (TikkunFlowActivity.kt) -------------------------
// « להמשיך מזה לזה ברצף אחד, ללא הפסק » : l'ouverture, les dix psaumes à la
// suite, la clôture ; rien à toucher entre les psaumes. La kavana et la
// prière de Rabbi Natan ne font pas partie du trait.
function ecranTikkun() {
  if (!tikkun) return h('div', { class: 'ecran' }, barre(tr.t('tikkun_title')), h('div', { class: 'defile' }));
  const titre = (texte) => h('h2', { class: 'psaume-titre' }, texte);
  const paragraphe = (texte) => h('p', { class: 'verset paragraphe' }, texte);
  const blocs = [];
  for (const b of [tikkun.ouverture].filter(Boolean)) blocs.push(titre(b.titre.he), ...b.paragraphes.map(paragraphe));
  for (const p of tikkun.psaumes) {
    blocs.push(titre(tr.t('tikkun_psalm_he', Reference.lettres(p.numero))));
    for (const v of p.versets) {
      blocs.push(h('p', { class: 'verset' }, v.he));
      if (v.arret) blocs.push(h('p', { class: 'arret' }, v.arret.note));
    }
  }
  if (tikkun.cloture) blocs.push(titre(tikkun.cloture.titre.he), ...tikkun.cloture.paragraphes.map(paragraphe));
  const fini = () => {
    // Tout dire d'un trait vaut les dix psaumes.
    for (const p of tikkun.psaumes) store.setTikkunDone(p.numero, true);
    retour();
  };
  return ecranLu(tr.t('tikkun_title'), h('div', { class: 'contenu lecture tikkun' },
    h('div', { class: 'psaume', lang: 'he', dir: 'rtl' }, blocs)), {
    pied: h('div', { class: 'lecture-nav' }, h('button', { class: 'principal', onclick: fini }, tr.t('tikkun_flow_done'))),
  });
}

// --- La bibliothèque (LibraryActivity, LibraryEntryActivity) ----------------------
// Sur le web, les sources seulement : des œuvres du domaine public, dont le
// texte se lit en entier. Les articles, dont le texte reste chez leur
// auteur, attendent leur fiche.
let bibliotheque = null;
let entreeBiblio = null;
function chargeBibliotheque() {
  if (bibliotheque) return true;
  fetch('data/bibliotheque.json').then((r) => r.json()).then((d) => { bibliotheque = d; rendre(); });
  return false;
}

function ecranBibliotheque() {
  const pret = chargeBibliotheque();
  const livresPrets = chargeLivres();
  return h('div', { class: 'ecran avec-bandeau' },
    bandeau(tr.t('library_title'), { puce: h('p', { class: 'bandeau-sous-titre' }, tr.t('library_hint')), dessin: 'char_torah' }),
    h('div', { class: 'defile' }, h('div', { class: 'contenu liste-siddour' },
      // Les livres entiers (12/10) : Likoutei Etsot, Sefer HaMiddot.
      ...(livresPrets ? livres.groupes.flatMap((g) => [
        enteteGroupe(g.titre.he, titreTexte(g.titre)),
        h('div', { class: 'carte carte-sections' }, livres.textes.filter((x) => x.groupe === g.cle).map((x) =>
          ligneHebreu(x.titre.he, [titreTexte(x.titre) !== x.titre.he ? titreTexte(x.titre) : null, tr.p('livre_chapitres', x.sections.length)].filter(Boolean).join(' · '),
            () => { priere = `livre:${x.id}`; ouvrir('priere'); }))),
      ]) : []),
      ...(pret ? LibraryRules.sources(bibliotheque).flatMap((section) => [
        enteteGroupe(section.titre.he, titreTexte(section.titre)),
        ...section.sous_sections.flatMap((sous) => [
          sous.titre.he !== section.titre.he ? h('h3', { class: 'sous-entete' },
            h('span', { class: 'serif-he', lang: 'he', dir: 'rtl' }, sous.titre.he),
            titreTexte(sous.titre) !== sous.titre.he ? h('span', { class: 'entete-trad' }, titreTexte(sous.titre)) : null) : null,
          h('div', { class: 'carte carte-sections' }, sous.entrees.map((e) =>
            ligneHebreu(e.titre_he, Reference.localise(e.ref, tr.langue), () => { entreeBiblio = e.id; ouvrir('source'); }))),
        ]),
      ]) : []))));
}

function ecranSource() {
  if (!chargeBibliotheque()) return h('div', { class: 'ecran' }, barre(tr.t('library_title')), h('div', { class: 'defile' }));
  const e = entreeBiblio ? LibraryRules.entree(bibliotheque, entreeBiblio) : null;
  // Rouvert directement sur #source, sans entrée choisie : la liste.
  if (!e || e.type !== 'source') return ecranBibliotheque();
  return ecranLu(tr.t('library_title'), h('div', { class: 'contenu lecture source-biblio' },
    h('div', { class: 'source-tete' },
      h('div', { class: 'source-titres' },
        h('span', { class: 'badge' }, tr.t('library_kind_source')),
        h('h2', { class: 'source-titre', lang: 'he', dir: 'rtl' }, e.titre_he),
        h('p', { class: 'source-ref' }, Reference.localise(e.ref, tr.langue))),
      plateau('char_torah', 'plateau-source')),
    h('div', { class: 'carte carte-texte psaume', lang: 'he', dir: 'rtl' },
      LibraryRules.paragraphes(bibliotheque, e).map((p) => h('p', { class: 'verset paragraphe-source' }, p))),
    // Ni note ni édition sous le texte : l'attribution passe par le lien vers Sefaria.
    e.url ? h('a', { class: 'principal lien-sefaria', href: e.url, target: '_blank', rel: 'noopener noreferrer' }, tr.t('library_open_sefaria')) : null));
}

function panneauTextes() {
  return h('div', { class: 'contenu' },
    h('h1', { class: 'titre-ecran' }, tr.t('nav_texts')),
    h('div', { class: 'carte' },
      ligne({ nom: tr.t('siddur_title'), etat: tr.t('siddur_hint'), onclick: () => ouvrir('siddur') }),
      // Tehilim est le premier texte lisible sur le web (04/10).
      ligne({ nom: tr.t('tehilim_title'), etat: tr.t('tehilim_today', libellePortion(portionDuMois().portion)), onclick: () => ouvrir('tehilim') }),
      // Le Tikoun, lu d'un trait (04/10).
      ligne({ nom: tr.t('tikkun_title'), etat: etatTikkun(), onclick: () => ouvrir('tikkun') }),
      // Les textes arrivent à l'étape 4.
      ligne({ nom: tr.t('tefilot_title'), etat: tr.t('tefilot_hint'), aVenir: true }),
      // La bibliothèque : ses sources, lisibles (04/10) ; les articles attendent.
      ligne({ nom: tr.t('library_title'), etat: tr.t('library_hint'), onclick: () => ouvrir('bibliotheque') }),
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
    ['aujourdhui', 'nav_today'], ['textes', 'nav_texts'], ['zmanim', 'nav_zmanim'], ['reglages', 'settings_title'],
  ];
  return h('nav', { class: 'onglets' }, items.map(([cle, nom]) => h('button', {
    class: 'onglet',
    'data-onglet': cle,
    'aria-current': cle === onglet ? 'page' : null,
    onclick: () => {
      // Journal et Réglages ouvrent leur écran ; l'onglet actif ne bouge pas (MainActivity.wireTabs).
      if (cle === 'reglages' || cle === 'zmanim') ouvrir(cle);
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

// --- Zmanim et calendrier (ZmanimActivity, CalendrierActivity, 10/10) ----------------
let villes = null;
let villesDemandees = false;
function chargeVilles() {
  if (villes) return true;
  if (!villesDemandees) {
    villesDemandees = true;
    fetch('data/villes.json').then((r) => r.json()).then((d) => { villes = d.villes; rendre(); }).catch(() => {});
  }
  return false;
}
let jourCalendrier = null;
let moisCalendrier = null;
/** Le 1er du mois hébreu affiché, en vue hébraïque ; null : celui du jour choisi. */
let moisHebreuCalendrier = null;

/** « 16.1 » → « 16,1 » en français : les paramètres des méthodes, à la locale. */
const parametre = (p) => (p !== '' && !Number.isNaN(Number(p)) ? Number(p).toLocaleString(tr.langue) : p);

/** Choisir le lieu : la position du téléphone, ou une ville de la liste (GeoNames). */
function choisirLieu() {
  chargeVilles();
  feuille(tr.t('zmanim_choisir_lieu'), (f, fermer) => {
    const etat = h('p', { class: 'message', 'aria-live': 'polite' });
    f.append(h('button', {
      class: 'choix',
      onclick: () => {
        if (!navigator.geolocation) { etat.textContent = tr.t('zmanim_position_refusee'); return; }
        etat.textContent = tr.t('zmanim_position_attente');
        navigator.geolocation.getCurrentPosition((p) => {
          store.setLieu({
            nom: tr.t('zmanim_position'), latitude: p.coords.latitude, longitude: p.coords.longitude,
            altitude: 0, fuseau: Intl.DateTimeFormat().resolvedOptions().timeZone,
          });
          fermer();
          rendre();
        }, () => { etat.textContent = tr.t('zmanim_position_refusee'); }, { maximumAge: 3_600_000, timeout: 20_000 });
      },
    }, tr.t('zmanim_position')), etat);
    const courant = store.lieu()?.ville;
    for (const v of villes ?? []) {
      f.append(h('button', {
        class: 'choix', role: 'menuitemradio', 'aria-checked': String(v.id === courant),
        onclick: () => {
          store.setLieu({ nom: titreTexte(v.nom), latitude: v.latitude, longitude: v.longitude, altitude: v.altitude, fuseau: v.fuseau }, v);
          fermer();
          rendre();
        },
      }, titreTexte(v.nom)));
    }
    f.append(h('p', { class: 'indice sources' }, tr.t('zmanim_source_villes')));
  });
}

/** « 40 min avant la chki'a · coutume de Jérusalem », quand Jérusalem est choisie. */
const libelleAllumage = (m) => tr.t(m === 40 && mukafChoma() ? 'reglage_allumage_jerusalem' : 'reglage_allumage_valeur', m);

function choisirTahanounTichri() {
  choisirDans(tr.t('reglage_tahanoun_tichri'), [tr.t('reglage_tahanoun_tichri_dit'), tr.t('reglage_tahanoun_tichri_sans')],
    store.sansTahanounFinTichri() ? 1 : 0, (i) => { store.setSansTahanounFinTichriSfard(i === 1); rendre(); });
}

function choisirAllumage() {
  choisirDans(tr.t('reglage_allumage'), ALLUMAGES.map(libelleAllumage),
    ALLUMAGES.indexOf(store.allumage()), (i) => { store.setAllumage(ALLUMAGES[i]); rendre(); });
}

/** Jérusalem choisie : Pourim le 15 Adar (CalendrierJuif.jour, Store.mukafChoma). */
const mukafChoma = () => store.lieu()?.ville === JERUSALEM_GEONAMES;

/** La date hébraïque d'un jour, ses fêtes, et ce qu'il change aux prières. */
// La hilloula du jour (11/10) : hilloulot.json, chargé à la première carte.
let hilloulots = null;
let hilloulotsDemandees = false;
function tableHilloulot() {
  if (!hilloulotsDemandees) {
    hilloulotsDemandees = true;
    fetch('data/hilloulot.json').then((r) => r.json()).then((d) => { hilloulots = hilloulots ?? d.jours; rendre(); }).catch(() => {});
  }
  return hilloulots;
}

// Les biographies d'autres sources (hilloulot_bios.json, 12/10), à part pour que la carte du
// jour reste légère : lues à l'écran des hilloulot ; par jour, une entrée par hilloula, dans le même ordre.
let biosDemandees = false;
function chargeBiosHilloulot() {
  if (biosDemandees) return;
  biosDemandees = true;
  Promise.all([fetch('data/hilloulot.json').then((r) => r.json()), fetch('data/hilloulot_bios.json').then((r) => r.json())])
    .then(([d, bios]) => {
      hilloulots = hilloulots ?? d.jours;
      hilloulotsDemandees = true;
      for (const [cle, ligne] of Object.entries(bios)) ligne.forEach((b, i) => { if (b && hilloulots[cle]?.[i]) hilloulots[cle][i].b = b; });
      rendre();
    }).catch(() => { biosDemandees = false; });
}

// La halakha du jour (11/10) : ben_ish_hai.json, chargé à la première carte.
let livreHalakha = null;
let livreHalakhaDemande = false;
function livreDuJour() {
  if (!livreHalakhaDemande) {
    livreHalakhaDemande = true;
    fetch('data/ben_ish_hai.json').then((r) => r.json()).then((d) => { livreHalakha = d; rendre(); }).catch(() => {});
  }
  return livreHalakha;
}

function lienExterne(libelle, url) {
  return h('a', { class: 'jour-lien', href: url, target: '_blank', rel: 'noopener' }, `${libelle} ↗`);
}

function carteJour(iso) {
  const j = jourJuif(iso, store.inIsrael(), mukafChoma());
  // L'omer se compte le soir, pour le jour hébreu qui commence : celui de demain.
  const [a, m, d] = iso.split('-').map(Number);
  const omerCeSoir = jourJuif(new Date(Date.UTC(a, m - 1, d + 1, 12)).toISOString().slice(0, 10), store.inIsrael()).omer;
  const civil = new Intl.DateTimeFormat(tr.langue, { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric', timeZone: 'UTC' })
    .format(new Date(`${iso}T12:00:00Z`));
  const marques = [
    ...j.fetes.map((f) => tr.t(`fete_${f}`)),
    j.paracha ? tr.t('zmanim_paracha', j.paracha) : null,
    omerCeSoir ? tr.t('zmanim_omer_ce_soir', omerCeSoir) : null,
  ].filter(Boolean);
  const dessin = illustrationDuJour(j.fetes, iso);
  return h('div', { class: 'carte carte-jour' },
    dessin ? h('img', { class: 'jour-illustration', src: `img/${dessin}.webp`, alt: '' }) : null,
    h('p', { class: 'jour-hebreu', lang: 'he', dir: 'rtl' }, j.dateHebraique),
    h('p', { class: 'jour-civil' }, civil),
    marques.length ? h('div', { class: 'jour-marques' }, marques.map((m) => h('span', { class: 'marque' }, m))) : null,
    h('h3', { class: 'jour-sous-titre' }, tr.t('zmanim_ajouts_titre')),
    h('p', { class: 'jour-ajouts' }, ajoutsDuJour(j.conditions, store.siddurNusah(), store.inIsrael(), store.sansTahanounFinTichri()).map((k) => tr.t(k)).join(' · ')),
    ...blocHilloula(iso),
    ...blocHalakha(iso));
}

/** La halakha du jour, hors ligne (Ben Ish 'Haï) : la première, puis l'écran de la halakha. */
function blocHalakha(iso) {
  const livre = livreDuJour();
  if (!livre || !store.halakhaAffichee()) return [];
  const liste = halakhotPour(livre, iso, store.inIsrael());
  if (!liste.length) return [];
  return [
    h('h3', { class: 'jour-sous-titre' }, `${tr.t('halakha_yomit_titre')} · ${tr.t('halakha_livre')}`),
    h('p', { class: 'jour-halakha', lang: 'he', dir: 'rtl' }, liste[0].texte),
    h('button', { class: 'jour-lien lien-bouton', onclick: () => { jourHalakha = iso; rechercheHalakhaTexte = ''; ouvrir('halakha'); } }, tr.t('halakha_tout')),
  ];
}

// --- L'écran de la halakha du jour (HalakhaActivity, 11/10) -----------------------
let jourHalakha = null;
let rechercheHalakhaTexte = '';

/**
 * Un écran à bandeau dont le texte se règle (halakha, hilloulot) : la même
 * barre de lecture que les textes saints, taille et police ; la nuit reste aux textes saints.
 */
function zoneReglable(contenu) {
  const defile = h('div', { class: 'defile' }, contenu);
  appuiLongPourRamener(defile);
  return h('div', { class: 'lu-zone sans-pied reglable' }, defile, reglagesLecture({ avecNusah: false, sansNuit: true }));
}

function ecranHalakha() {
  const livre = livreDuJour() ?? { annees: {} };
  const iso = jourHalakha ?? isoLocal(new Date());
  const carte = (x) => h('div', { class: 'carte carte-hilloula' },
    h('p', { class: 'halakha-source', lang: 'he', dir: 'rtl' }, sourceHalakha(x, (n) => Reference.lettres(n))),
    h('p', { class: 'halakha-texte', lang: 'he', dir: 'rtl' }, x.texte));
  const champ = h('input', { type: 'search', class: 'champ-recherche', placeholder: tr.t('halakha_recherche'), value: rechercheHalakhaTexte,
    oninput: (e) => { rechercheHalakhaTexte = e.target.value; rendre(); setTimeout(() => { const c = app.querySelector('.champ-recherche'); c?.focus(); c?.setSelectionRange(c.value.length, c.value.length); }, 0); } });
  let corps;
  if (rechercheHalakhaTexte.trim().length >= 2) {
    const trouves = rechercheHalakha(livre, rechercheHalakhaTexte);
    corps = [h('p', { class: 'jour-sous-titre' }, tr.t('hilloulot_resultats', trouves.length)), ...trouves.slice(0, 40).map(carte)];
  } else {
    const j = jourJuif(iso, store.inIsrael());
    const decale = (n) => { const [a, m, d] = iso.split('-').map(Number); jourHalakha = new Date(Date.UTC(a, m - 1, d + n, 12)).toISOString().slice(0, 10); rendre(); };
    const liste = livreHalakha ? halakhotPour(livre, iso, store.inIsrael()) : [];
    corps = [
      h('div', { class: 'mois-tete' },
        h('button', { class: 'icone-bouton mois-precedent', dir: 'ltr', 'aria-label': tr.t('calendrier_precedent'), onclick: () => decale(-1) }, tr.rtl ? '›' : '‹'),
        h('div', { class: 'mois-titres' }, h('p', { class: 'mois-principal mois-hebreu', lang: 'he', dir: 'rtl' }, j.dateHebraique), h('p', { class: 'mois-second' }, iso)),
        h('button', { class: 'icone-bouton mois-suivant', dir: 'ltr', 'aria-label': tr.t('calendrier_suivant'), onclick: () => decale(1) }, tr.rtl ? '‹' : '›')),
      ...(liste.length ? liste.map(carte) : [h('p', { class: 'message' }, tr.t('halakha_aucune'))]),
      h('p', { class: 'note-halakha' }, tr.t('halakha_note')),
      // Le site de Rav Ovadia Yossef : un lien seulement (tous droits réservés sur ses textes).
      lienExterne(tr.t('halakha_yomit_lien'), lienHalakha(tr.langue)),
    ];
  }
  return h('div', { class: 'ecran avec-bandeau' }, bandeau(tr.t('halakha_title'), { dessin: 'char_reading_seated' }),
    zoneReglable(h('div', { class: 'contenu' },
      h('div', { class: 'carte' },
        h('label', { class: 'ligne' }, h('span', { class: 'corps' }, h('div', { class: 'nom' }, tr.t('halakha_afficher'))),
          h('input', { type: 'checkbox', role: 'switch', class: 'interrupteur', checked: store.halakhaAffichee(), 'aria-label': tr.t('halakha_afficher'),
            onchange: (e) => { store.setHalakhaAffichee(e.target.checked); rendre(); } })),
        champ),
      ...corps)));
}

/** La hilloula du jour (comme CalJ) : les noms, cinq au plus, puis l'écran des hilloulot. */
/** Domaine public et CC0 ne demandent aucune mention ; CC-BY et CC-BY-SA, si (sans le nom de Sefaria). */
function mentionsObligatoires(sources) {
  return (sources || []).filter((x) => x.includes('CC-BY')).map((x) => x.replace('Sefaria / ', '').replace('Sefaria, ', ''));
}

function blocHilloula(iso) {
  const table = tableHilloulot();
  if (!table || !store.hilloulotAffichees()) return [];
  const liste = hilloulotPour(table, iso, store.inIsrael());
  if (!liste.length) return [];
  const reste = liste.length - 5;
  const versHilloulot = () => { jourHilloulot = iso; rechercheHilloulot = ''; ouvrir('hilloulot'); };
  return [
    h('h3', { class: 'jour-sous-titre' }, tr.t('hilloula_titre')),
    h('div', { class: 'jour-hilloula', lang: 'he', dir: 'rtl' }, ...liste.slice(0, 5).map((x) => h('div', {}, x.he)),
      reste > 0 ? h('button', { class: 'jour-hilloula-reste lien-bouton', onclick: versHilloulot }, `${tr.p('hilloula_autres', reste)} ›`) : null),
    h('button', { class: 'jour-lien lien-bouton', onclick: versHilloulot }, tr.t('hilloulot_tout')),
  ];
}

// --- L'écran des hilloulot (HilloulotActivity, 11/10) --------------------------------
let jourHilloulot = null;
let rechercheHilloulot = '';

function ecranHilloulot() {
  chargeBiosHilloulot();
  const table = tableHilloulot() ?? {};
  const iso = jourHilloulot ?? isoLocal(new Date());
  const carte = (x, date) => h('div', { class: 'carte carte-hilloula' },
    h('p', { class: 'hilloula-nom', lang: 'he', dir: 'rtl' }, x.he),
    (tr.langue === 'fr' ? x.fr : tr.langue === 'en' ? x.en : null) ? h('p', { class: 'hilloula-traduit' }, tr.langue === 'fr' ? x.fr : x.en) : null,
    // Isolés (FSI … PDI) : la date et l'année hébraïques ne renversent pas la ligne.
    [date, x.a].some(Boolean)
      ? h('p', { class: 'hilloula-annee' }, [date ? `\u2068${date}\u2069` : null, x.a ? tr.t('hilloula_annee', `\u2068${x.a}\u2069`) : null].filter(Boolean).join(' · ')) : null,
    x.t ? h('p', { class: 'hilloula-description', lang: 'he', dir: 'rtl' }, x.t) : null,
    x.s.includes('w') ? (x.w ? h('a', { class: 'hilloula-source', href: lienArticle(x.w), target: '_blank', rel: 'noopener' }, tr.t('hilloula_source_wiki'))
      : h('p', { class: 'hilloula-source' }, tr.t('hilloula_source_wiki'))) : null,
    x.s.includes('h') ? h('p', { class: 'hilloula-source' }, tr.t('hilloula_source_hyomi')) : null,
    x.s.includes('e') ? h('p', { class: 'hilloula-source' }, tr.t('hilloula_source_toratemet')) : null,
    // Les autres biographies (MyTzadik, המאיר לארץ), chacune suivie de sa source, comme le demande l'accord (12/10).
    ...(x.b ?? []).flatMap(([src, texte, lien]) => {
      const nom = tr.t(src === 'm' ? 'hilloula_source_mytzadik' : 'hilloula_source_hameir');
      // Une longue biographie se replie à huit lignes ; « Lire la suite » la déplie.
      const long = texte.length > 500;
      const p = h('p', { class: `hilloula-description${long ? ' replie' : ''}`, lang: 'he', dir: 'rtl' }, texte);
      const bascule = long ? h('button', { type: 'button', class: 'lien-bouton hilloula-plus', 'aria-expanded': 'false', onclick: (e) => {
        const ouvert = p.classList.toggle('replie') === false;
        e.currentTarget.setAttribute('aria-expanded', String(ouvert));
        e.currentTarget.textContent = tr.t(ouvert ? 'hilloula_reduire' : 'hilloula_lire_plus');
      } }, tr.t('hilloula_lire_plus')) : null;
      return [p, bascule,
        lien ? h('a', { class: 'hilloula-source', href: lien, target: '_blank', rel: 'noopener' }, `${nom} ↗`) : h('p', { class: 'hilloula-source' }, nom)];
    }),
    x.i ? h('a', { class: 'hilloula-source', href: x.i, target: '_blank', rel: 'noopener' }, `${tr.t('hilloula_source_hidabroot')} ↗`) : null);
  const champ = h('input', { type: 'search', class: 'champ-recherche', placeholder: tr.t('hilloulot_recherche'), value: rechercheHilloulot,
    oninput: (e) => { rechercheHilloulot = e.target.value; rendre(); setTimeout(() => { const c = app.querySelector('.champ-recherche'); c?.focus(); c?.setSelectionRange(c.value.length, c.value.length); }, 0); } });
  let corps;
  if (rechercheHilloulot.trim().length >= 2) {
    const trouves = rechercheHilloula(table, rechercheHilloulot);
    corps = [h('p', { class: 'jour-sous-titre' }, tr.t('hilloulot_resultats', trouves.length)), ...trouves.slice(0, 60).map(([cle, x]) => carte(x, dateHilloula(cle, (n) => Reference.lettres(n))))];
  } else {
    const j = jourJuif(iso, store.inIsrael());
    const decale = (n) => { const [a, m, d] = iso.split('-').map(Number); jourHilloulot = new Date(Date.UTC(a, m - 1, d + n, 12)).toISOString().slice(0, 10); rendre(); };
    const liste = hilloulotPour(table, iso, store.inIsrael());
    corps = [
      h('div', { class: 'mois-tete' },
        h('button', { class: 'icone-bouton mois-precedent', dir: 'ltr', 'aria-label': tr.t('calendrier_precedent'), onclick: () => decale(-1) }, tr.rtl ? '›' : '‹'),
        h('div', { class: 'mois-titres' }, h('p', { class: 'mois-principal mois-hebreu', lang: 'he', dir: 'rtl' }, j.dateHebraique), h('p', { class: 'mois-second' }, iso)),
        h('button', { class: 'icone-bouton mois-suivant', dir: 'ltr', 'aria-label': tr.t('calendrier_suivant'), onclick: () => decale(1) }, tr.rtl ? '‹' : '›')),
      ...(liste.length ? liste.map((x) => carte(x, null)) : [h('p', { class: 'message' }, tr.t('hilloulot_aucune'))]),
      lienExterne(tr.t('hilloula_lien'), lienHyomi(iso)),
    ];
  }
  return h('div', { class: 'ecran avec-bandeau' }, bandeau(tr.t('hilloulot_title'), { dessin: 'char_torah' }),
    zoneReglable(h('div', { class: 'contenu' },
      h('div', { class: 'carte' },
        h('label', { class: 'ligne' }, h('span', { class: 'corps' }, h('div', { class: 'nom' }, tr.t('hilloulot_afficher'))),
          h('input', { type: 'checkbox', role: 'switch', class: 'interrupteur', checked: store.hilloulotAffichees(), 'aria-label': tr.t('hilloulot_afficher'),
            onchange: (e) => { store.setHilloulotAffichees(e.target.checked); rendre(); } })),
        champ),
      ...corps)));
}

/** Les horaires d'un jour, chacun avec sa méthode ; ou l'invitation à choisir un lieu. */
function listeZmanim(iso) {
  const lieu = store.lieu();
  if (!lieu) {
    return h('div', { class: 'carte carte-texte' }, h('p', { class: 'message' }, tr.t('zmanim_sans_lieu')),
      h('button', { class: 'principal', onclick: choisirLieu }, tr.t('zmanim_choisir_lieu')));
  }
  return h('div', { class: 'carte carte-zmanim' }, horaires(lieu, iso, store.inIsrael(), store.allumage()).map((z) =>
    h('div', { class: `zman${z.cle === 'allumage' || z.cle === 'sortie' ? ' zman-chabbat' : ''}` },
      h('div', { class: 'zman-tete' },
        h('span', { class: 'zman-nom' }, tr.t(`zman_${z.cle}`)),
        h('span', { class: 'zman-heure', dir: 'ltr' }, z.statut === 'ok' ? heureZman(z.instant, lieu.fuseau) : '—')),
      h('div', { class: 'zman-methode' }, z.statut === 'ok'
        ? tr.t(`zman_methode_${z.methode}`, parametre(z.parametres))
        : tr.t(z.statut === 'absent' ? 'zmanim_absent' : 'zmanim_erreur')))));
}

const piedZmanim = () => [h('p', { class: 'indice sources' }, tr.t('zmanim_a_valider')),
  h('p', { class: 'indice sources' }, tr.t('zmanim_source_villes'))];

function ecranZmanim() {
  chargeVilles();
  const iso = isoLocal(new Date());
  const lieu = store.lieu();
  const puce = h('button', {
    type: 'button', class: 'puce-bandeau', 'aria-haspopup': 'dialog', onclick: choisirLieu,
    'aria-label': `${tr.t('reglage_lieu')} : ${lieu ? lieu.nom : tr.t('reglage_lieu_aucun')}`,
  }, h('span', {}, lieu ? tr.t('zmanim_lieu_detail', lieu.nom, tr.t(store.inIsrael() ? 'region_israel' : 'region_diaspora')) : tr.t('zmanim_choisir_lieu')), icone('replier'));
  return h('div', { class: 'ecran avec-bandeau' }, bandeau(tr.t('zmanim_title'), { puce, dessin: 'char_reading_seated' }),
    h('div', { class: 'defile' }, h('div', { class: 'contenu' },
      carteJour(iso),
      h('button', { class: 'secondaire bouton-calendrier', onclick: () => { jourCalendrier = iso; moisCalendrier = null; ouvrir('calendrier'); } },
        tr.t('zmanim_calendrier')),
      listeZmanim(iso),
      ...piedZmanim())));
}

/**
 * Le calendrier : un mois, du dimanche au samedi ; un jour touché montre son détail.
 * Le mois hébreu est la vue principale, le mois civil au choix ; chaque case porte
 * toujours les deux quantièmes, le principal en haut.
 */
// --- Les paliers d'objectifs (PaliersActivity, 11/10) --------------------------------
function bilanPaliers() {
  const aujourdhui = isoLocal(new Date());
  return { aujourdhui, minutes: store.minutesParJour(), serie: store.streak(), debutMoisHebreu: debutMoisHebreu(aujourdhui), objectifJour: store.dailyGoal() };
}

function libellePalier(piste, bilan) {
  return piste === 'mois' ? tr.t('palier_mois', `\u2068${jourJuif(bilan.aujourdhui, false).moisEtAnnee}\u2069`) : tr.t(`palier_${piste}`);
}

/** Des jours pour la série et les jours de pratique, une durée pour le reste. */
const valeurPalier = (piste, v) => (piste === 'serie' || piste === 'jours' ? tr.p('palier_n_jours', v) : tr.duree(v));

/** Le palier le plus proche, sous les anneaux, et la fête d'un palier atteint depuis la dernière visite. */
function lignePaliers() {
  const bilan = bilanPaliers();
  const etats = Paliers.etats(bilan);
  const une = Paliers.aLaUne(etats);
  const vus = store.paliersVus();
  // Un trophée neuf d'abord, sinon le plus haut palier neuf : les confettis (11/10).
  const gagnes = Trophees.gagnes(pratique());
  const trophee = Trophees.nouveaux(gagnes, store.tropheesVus())[0];
  const neuf = vus ? Paliers.nouveaux(etats, bilan, vus).sort((a, b) => b.rang - a.rang)[0] : null;
  const message = trophee ? tr.t('trophee_niveau_gagne', tr.t(`trophee_${trophee.famille.cle}`), trophee.rang, tr.t(`metal_${trophee.metal}`))
    : neuf ? tr.t('palier_nouveau', libellePalier(neuf.piste, bilan), valeurPalier(neuf.piste, neuf.atteint)) : null;
  if (message) setTimeout(() => { signale(message); confettis(); }, 600);
  store.setPaliersVus(Paliers.marques(etats, bilan));
  store.setTropheesVus(new Set(gagnes));
  const cle = { serie: 'palier_une_serie', jour: 'palier_une_jour', semaine: 'palier_une_semaine' }[une.piste] ?? 'palier_une_mois';
  return h('button', { class: 'ligne-paliers', onclick: () => ouvrir('paliers') },
    `${tr.t(cle, valeurPalier(une.piste, une.reste), valeurPalier(une.piste, une.prochain))}  ›`);
}

const pratique = () => ({ minutes: store.minutesParJour(), notes: store.nombreNotes(), meilleureSerie: store.meilleureSerie() });

/** Des confettis par-dessus l'écran, 2,8 s ; rien si l'on a demandé moins d'animations. */
function confettis() {
  if (window.matchMedia?.('(prefers-reduced-motion: reduce)').matches) return;
  const c = h('canvas', { class: 'confettis', 'aria-hidden': 'true' });
  document.body.append(c);
  const ctx = c.getContext('2d');
  if (!ctx) { c.remove(); return; }
  const dpr = window.devicePixelRatio || 1;
  c.width = innerWidth * dpr; c.height = innerHeight * dpr;
  const couleurs = ['#C99A2E', '#1F6F5C', '#E05A47', '#3B6EC4', '#F2C94C', '#8E4FB5'];
  const pieces = Array.from({ length: 140 }, () => ({ x: Math.random(), vx: (Math.random() - 0.5) * 0.25, vy: 0.55 + Math.random() * 0.6,
    tour: (Math.random() - 0.5) * 8 * Math.PI, s: (6 + Math.random() * 8) * dpr, c: couleurs[Math.floor(Math.random() * couleurs.length)],
    rond: Math.random() < 0.25, retard: Math.random() * 0.35 }));
  const debut = performance.now();
  const image = (t) => {
    const u0 = Math.min(1, (t - debut) / 2800);
    ctx.clearRect(0, 0, c.width, c.height);
    for (const p of pieces) {
      const u = Math.max(0, Math.min(1, (u0 - p.retard) / (1 - p.retard)));
      if (u <= 0) continue;
      const x = (p.x + p.vx * u) * c.width; const y = (-0.05 + p.vy * u + 0.6 * u * u) * c.height;
      ctx.save(); ctx.globalAlpha = Math.min(1, (1 - u) * 2); ctx.fillStyle = p.c; ctx.translate(x, y); ctx.rotate(p.tour * u);
      if (p.rond) { ctx.beginPath(); ctx.arc(0, 0, p.s / 2, 0, 2 * Math.PI); ctx.fill(); } else ctx.fillRect(-p.s / 2, -p.s / 4, p.s, p.s / 2);
      ctx.restore();
    }
    if (u0 < 1) requestAnimationFrame(image); else c.remove();
  };
  requestAnimationFrame(image);
}

/** La vitrine : les trophées gagnés en couleur, les autres en attente, avec ce qu'il faut pour les gagner. */
// Les couleurs des métaux : clair, foncé, sombre (la tranche, le socle). Gris pour ce qui reste à gagner.
const TEINTES = {
  bronze: ['#F3C49A', '#A0602E', '#6E3E1C'], argent: ['#F4F6F8', '#9AA3AC', '#5E6770'],
  or: ['#FFE48A', '#C9961A', '#8A6206'], platine: ['#F7FAFF', '#8CA2C4', '#55688A'], aucun: ['#E3E5E7', '#B6BABE', '#8E9398'],
};
let numeroSvg = 0;
/** Un objet de trophée (tools/trophees_formes.py), au métal donné, avec l'icône de la famille en emblème. */
function objetTrophee(forme, metal, icone, taille) {
  const [clair, fonce, sombre] = TEINTES[metal ?? 'aucun'];
  const id = `t${++numeroSvg}`;
  const f = FORMES[forme];
  const remplis = { m: `url(#${id}m)`, mr: `url(#${id}r)`, md: sombre, v: 'rgba(170,215,255,.75)', vc: 'rgba(235,247,255,.9)', r: metal ? '#D43B3B' : '#B9BDC1', w: 'rgba(255,255,255,.5)' };
  const NS = 'http://www.w3.org/2000/svg';
  const el = (nom, attrs, ...enfants) => { const e = document.createElementNS(NS, nom); for (const [k, v] of Object.entries(attrs)) e.setAttribute(k, v); e.append(...enfants); return e; };
  const [ex, ey, et] = f.embleme;
  return el('svg', { viewBox: '0 0 100 100', width: taille, height: taille, class: 'objet-svg', 'aria-hidden': 'true' },
    el('defs', {},
      el('linearGradient', { id: `${id}m`, x1: '0', y1: '0', x2: '1', y2: '1' }, el('stop', { offset: '0', 'stop-color': clair }), el('stop', { offset: '1', 'stop-color': fonce })),
      el('radialGradient', { id: `${id}r`, cx: '.35', cy: '.3', r: '.75' }, el('stop', { offset: '0', 'stop-color': '#FFFFFF' }), el('stop', { offset: '.35', 'stop-color': clair }), el('stop', { offset: '1', 'stop-color': fonce }))),
    ...f.parts.map(([matiere, d]) => el('path', { d, fill: remplis[matiere] })),
    el('text', { x: ex, y: ey, 'font-size': et, 'text-anchor': 'middle', 'dominant-baseline': 'central', opacity: metal ? '1' : '.45' }, icone));
}

function vitrineTrophees() {
  const p = pratique();
  store.setTropheesVus(Trophees.gagnes(p));
  const atteints = Trophees.FAMILLES.reduce((n, f) => n + Trophees.rang(p, f), 0);
  const total = Trophees.FAMILLES.reduce((n, f) => n + f.seuils.length, 0);
  // Les plus hauts métaux d'abord ; à égalité, les plus proches du niveau suivant (12/10).
  const ordre = (f) => { const r = Trophees.rang(p, f); return (r ? Trophees.METAUX.indexOf(Trophees.metal(f, r)) + 1 : 0) * 10 + Trophees.progres(p, f).fraction; };
  const tries = [...Trophees.FAMILLES].sort((a, b) => ordre(b) - ordre(a));
  const barre = (fraction, classe) => h('div', { class: `trophee-barre ${classe}`, 'aria-hidden': 'true' },
    h('span', { style: `width: ${Math.round(Math.min(1, Math.max(0, fraction)) * 100)}%` }));
  const desc = (f, seuil) => tr.p(`trophee_${f.cle}_desc`, seuil);
  // La scène : l'avatar, de plus en plus joyeux à mesure que le métal monte, et le plus beau trophée sur son socle.
  const meilleure = tries[0] && Trophees.rang(p, tries[0]) ? tries[0] : null;
  const mMeilleure = meilleure ? Trophees.metal(meilleure, Trophees.rang(p, meilleure)) : null;
  const avatar = { bronze: 'char_thumbs_up', argent: 'char_open_arms', or: 'char_arms_raised', platine: 'char_dancing_joy' }[mMeilleure] ?? 'char_thinking';
  const socle = (metal, nom, grand) => h('div', { class: `trophee-socle ${metal ?? 'aucun'}${grand ? ' grand' : ''}` }, h('span', { class: 'socle-plaque' }, nom));
  const scene = h('div', { class: `trophees-scene ${mMeilleure ?? 'aucune'}` },
    h('img', { class: 'scene-avatar', src: `img/${avatar}.webp`, alt: '' }),
    h('div', { class: 'scene-podium' },
      h('span', { class: `bulle ${mMeilleure ?? 'aucun'}` }, meilleure ? tr.t(`metal_${mMeilleure}`) : tr.t('trophee_a_venir')),
      h('div', { class: `objet vedette${meilleure ? ' gagne' : ''}` }, objetTrophee(meilleure?.forme ?? 'coupe', mMeilleure, meilleure?.icone ?? '🏆', 116)),
      socle(mMeilleure, meilleure ? tr.t(`trophee_${meilleure.cle}`) : tr.t('trophees_titre'), true),
      h('p', { class: 'scene-titre' }, meilleure ? tr.t('trophee_vedette') : tr.t('trophee_vedette_aucune'))));
  const carte = (f) => {
    const r = Trophees.rang(p, f);
    const m = r ? Trophees.metal(f, r) : null;
    const pr = Trophees.progres(p, f);
    const sommet = r >= f.seuils.length;
    const niveau = m ? tr.t('trophee_niveau', r, f.seuils.length, tr.t(`metal_${m}`)) : tr.t('trophee_a_venir');
    return h('div', { class: `trophee ${m ?? ''}${m ? ' gagne' : ''}`,
      'aria-label': [tr.t(`trophee_${f.cle}`), niveau, sommet ? tr.t('trophee_sommet') : tr.t('trophee_prochain', desc(f, pr.cible))].join(', ') },
      h('span', { class: `bulle ${m ?? 'aucun'}`, 'aria-hidden': 'true' }, m ? `${tr.t(`metal_${m}`)} · ${r}/${f.seuils.length}` : tr.t('trophee_a_venir')),
      h('div', { class: `objet${m ? ' gagne' : ''}`, 'aria-hidden': 'true' }, objetTrophee(f.forme, m, f.icone, 78), m ? h('span', { class: 'eclat' }) : null),
      socle(m, tr.t(`trophee_${f.cle}`)));
  };
  const details = (f) => {
    const r = Trophees.rang(p, f);
    const pr = Trophees.progres(p, f);
    const sommet = r >= f.seuils.length;
    return h('div', { class: 'trophee-details' },
      h('span', { class: 'trophee-desc' }, desc(f, f.seuils[Math.max(0, r - 1)])),
      sommet ? h('span', { class: 'trophee-progres' }, tr.t('trophee_sommet')) : barre(pr.fraction, 'petite'),
      sommet ? null : h('span', { class: 'trophee-progres' }, tr.t('trophee_progres', Math.min(pr.valeur, pr.cible), pr.cible)));
  };
  // Les étagères : trois trophées par planche de verre, leurs détails dessous.
  const rangees = [];
  for (let k = 0; k < tries.length; k += 3) rangees.push(tries.slice(k, k + 3));
  return h('div', { class: 'carte carte-palier' },
    h('div', { class: 'trophees-tete' }, h('p', { class: 'palier-nom' }, tr.t('trophees_titre')),
      h('p', { class: 'trophees-compte' }, tr.t('trophees_compte', atteints, total))),
    scene,
    barre(atteints / total, 'globale'),
    ...rangees.map((rangee) => h('div', { class: 'etagere' },
      h('div', { class: 'etagere-objets' }, ...rangee.map(carte)),
      h('div', { class: 'planche', 'aria-hidden': 'true' }),
      h('div', { class: 'etagere-details' }, ...rangee.map(details)))));
}

function ecranPaliers() {
  const bilan = bilanPaliers();
  const etats = Paliers.etats(bilan);
  store.setPaliersVus(Paliers.marques(etats, bilan));
  const r = Paliers.records(bilan.minutes, (d) => { const j = jourJuif(d, false); return `${j.anneeHebraique}-${j.moisHebreu}`; });
  const carte = (e) => h('div', { class: 'carte carte-palier' },
    h('p', { class: 'palier-nom' }, libellePalier(e.piste, bilan)),
    h('p', { class: 'palier-valeur' }, valeurPalier(e.piste, e.valeur)),
    h('div', { class: 'palier-barre', role: 'progressbar', 'aria-valuemin': '0', 'aria-valuemax': '100', 'aria-valuenow': String(Math.round(e.avancee * 100)) },
      h('span', { style: `width:${Math.round(e.avancee * 100)}%` })),
    h('p', { class: 'palier-prochain' }, tr.t('palier_prochain', valeurPalier(e.piste, e.prochain), valeurPalier(e.piste, e.reste))),
    e.rang > 0 ? h('p', { class: 'palier-atteints' }, tr.t('palier_atteints', e.rang)) : null,
    e.rang > 0 ? h('div', { class: 'palier-pastilles' }, Paliers.echelle(e.piste, e.valeur, bilan.objectifJour).filter((x) => x <= e.valeur)
      .reverse().slice(0, 8).map((x) => h('span', { class: 'palier-pastille' }, valeurPalier(e.piste, x)))) : null);
  return h('div', { class: 'ecran avec-bandeau' }, bandeau(tr.t('trophees_paliers_titre'), { dessin: 'char_arms_raised' }),
    h('div', { class: 'defile' }, h('div', { class: 'contenu' },
      vitrineTrophees(),
      h('h2', { class: 'paliers-titre' }, tr.t('paliers_title')),
      h('p', { class: 'paliers-sous-titre' }, tr.t('paliers_sous_titre')),
      ...etats.map(carte),
      h('div', { class: 'carte carte-palier' }, h('p', { class: 'palier-nom' }, tr.t('paliers_records')),
        ...[tr.t('record_serie', valeurPalier('serie', store.meilleureSerie())), tr.t('record_jour', tr.duree(r.jour)),
          tr.t('record_semaine', tr.duree(r.semaine)), tr.t('record_mois', tr.duree(r.mois))].map((x) => h('p', { class: 'palier-record' }, x))))));
}

function ecranCalendrier() {
  const aujourdhui = isoLocal(new Date());
  const choisi = jourCalendrier ?? aujourdhui;
  const civil = store.calendrierCivil();
  const israel = store.inIsrael();
  const moisCourt = new Intl.DateTimeFormat(tr.langue, { month: 'short', timeZone: 'UTC' });
  const date = (iso) => new Date(`${iso}T12:00:00Z`);
  let grille;
  let titre;
  let sousTitre;
  let decale;
  if (civil) {
    const [a, m] = moisCalendrier ?? choisi.split('-').map(Number);
    grille = semaines(a, m);
    const jours = grille.flat().filter(Boolean);
    const premier = jourJuif(jours[0], israel);
    const dernier = jourJuif(jours.at(-1), israel);
    titre = h('p', { class: 'mois-principal' }, new Intl.DateTimeFormat(tr.langue, { month: 'long', year: 'numeric', timeZone: 'UTC' }).format(new Date(Date.UTC(a, m - 1, 15))));
    sousTitre = h('p', { class: 'mois-second mois-hebreu', lang: 'he', dir: 'rtl' },
      premier.moisEtAnnee === dernier.moisEtAnnee ? premier.moisEtAnnee : `${premier.moisEtAnnee} – ${dernier.moisEtAnnee}`);
    decale = (n) => { const d = new Date(Date.UTC(a, m - 1 + n, 1)); moisCalendrier = [d.getUTCFullYear(), d.getUTCMonth() + 1]; rendre(); };
  } else {
    const debut = moisHebreuCalendrier ?? debutMoisHebreu(choisi);
    grille = semainesHebreu(debut);
    const jours = grille.flat().filter(Boolean);
    const [d0, d1] = [date(jours[0]), date(jours.at(-1))];
    const memeAnnee = d0.getUTCFullYear() === d1.getUTCFullYear();
    const mois = (d, an) => new Intl.DateTimeFormat(tr.langue, { month: 'long', ...(an ? { year: 'numeric' } : {}), timeZone: 'UTC' }).format(d);
    titre = h('p', { class: 'mois-principal mois-hebreu', lang: 'he', dir: 'rtl' }, jourJuif(debut, israel).moisEtAnnee);
    // Seule l'initiale de la plage prend la majuscule (« Septembre – octobre 2026 »).
    const plage = `${mois(d0, !memeAnnee)} – ${mois(d1, true)}`;
    sousTitre = h('p', { class: 'mois-second' }, plage.charAt(0).toLocaleUpperCase(tr.langue) + plage.slice(1));
    decale = (n) => { moisHebreuCalendrier = moisHebreuVoisin(debut, n); rendre(); };
  }
  const bascule = (versCivil) => { store.setCalendrierCivil(versCivil); moisCalendrier = null; moisHebreuCalendrier = null; rendre(); };
  // Les initiales des jours, du dimanche (4/10/2026) au samedi.
  const initiales = Array.from({ length: 7 }, (_, i) => new Intl.DateTimeFormat(tr.langue, { weekday: 'narrow', timeZone: 'UTC' })
    .format(new Date(Date.UTC(2026, 9, 4 + i, 12))));
  return h('div', { class: 'ecran' }, barre(tr.t('calendrier_title')),
    h('div', { class: 'defile' }, h('div', { class: 'contenu' },
      h('div', { class: 'carte carte-calendrier' },
        h('div', { class: 'vue-calendrier', role: 'group', 'aria-label': tr.t('calendrier_vue') },
          h('button', { class: civil ? '' : 'actif', 'aria-pressed': String(!civil), onclick: () => bascule(false) }, tr.t('calendrier_vue_hebreu')),
          h('button', { class: civil ? 'actif' : '', 'aria-pressed': String(civil), onclick: () => bascule(true) }, tr.t('calendrier_vue_civil'))),
        h('div', { class: 'mois-tete' },
          h('button', { class: 'icone-bouton mois-precedent', 'aria-label': tr.t('calendrier_precedent'), dir: 'ltr', onclick: () => decale(-1) }, tr.rtl ? '›' : '‹'),
          h('div', { class: 'mois-titres' }, titre, sousTitre),
          h('button', { class: 'icone-bouton mois-suivant', 'aria-label': tr.t('calendrier_suivant'), dir: 'ltr', onclick: () => decale(1) }, tr.rtl ? '‹' : '›')),
        h('div', { class: 'grille-jours', role: 'grid' },
          initiales.map((x) => h('span', { class: 'jour-initiale', 'aria-hidden': 'true' }, x)),
          grille.flat().map((d) => {
            if (!d) return h('span', { class: 'case vide' });
            const j = jourJuif(d, israel, mukafChoma());
            const quantieme = Number(d.slice(8));
            // Le 1er d'un mois civil porte le nom du mois, pour qu'on lise où il bascule.
            const civilTexte = !civil && quantieme === 1 ? `1 ${moisCourt.format(date(d))}` : String(quantieme);
            const hebreu = h('span', { class: civil ? 'case-bas case-hebreu' : 'case-haut case-hebreu', lang: 'he', dir: 'rtl' }, j.jourEnLettres);
            const civ = h('span', { class: civil ? 'case-haut case-civil' : 'case-bas case-civil' }, civilTexte);
            return h('button', {
              class: ['case', d === aujourdhui ? 'aujourdhui' : '', d === choisi ? 'choisi' : '', j.chome ? 'chome' : '', j.fetes.length ? 'fete' : ''].filter(Boolean).join(' '),
              'aria-pressed': String(d === choisi),
              'aria-label': `${d} · ${j.dateHebraique}${j.fetes.length ? ` · ${j.fetes.map((f) => tr.t(`fete_${f}`)).join(', ')}` : ''}`,
              onclick: () => { jourCalendrier = d; rendre(); },
            }, ...(civil ? [civ, hebreu] : [hebreu, civ]));
          })),
        h('button', { class: 'lien-aujourdhui', onclick: () => { jourCalendrier = aujourdhui; moisCalendrier = null; moisHebreuCalendrier = null; rendre(); } }, tr.t('calendrier_aujourdhui'))),
      carteJour(choisi),
      listeZmanim(choisi),
      ...piedZmanim())));
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
        }),
        ligne({ nom: tr.t('reglage_lieu'), etat: store.lieu()?.nom ?? tr.t('reglage_lieu_aucun'), onclick: choisirLieu }),
        // À Jérusalem, l'usage des 40 minutes est proposé, jamais imposé.
        ligne({ nom: tr.t('reglage_allumage'), onclick: choisirAllumage,
          etat: [libelleAllumage(store.allumage()), mukafChoma() && store.allumage() !== 40 ? tr.t('reglage_allumage_proposition') : null].filter(Boolean).join(' · ') }),
        store.siddurNusah() === 'sefard' ? ligne({ nom: tr.t('reglage_tahanoun_tichri'), onclick: choisirTahanounTichri,
          etat: tr.t(store.sansTahanounFinTichri() ? 'reglage_tahanoun_tichri_sans' : 'reglage_tahanoun_tichri_dit') }) : null),
      // Réinitialiser l'application (12/10) : en dernier, avec une confirmation.
      h('div', { class: 'carte carte-reinit' },
        h('h3', {}, tr.t('reset_title')),
        h('p', { class: 'indice' }, tr.t('reset_hint')),
        h('button', { class: 'bouton-danger', onclick: () => confirmer(tr.t('reset_confirm_title'), tr.t('reset_confirm_text'), tr.t('reset_confirm_yes'), () => {
          store.reinitialiser();
          location.replace(location.pathname);
        }) }, tr.t('reset_action'))))));
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
    enCours && aBlocage ? h('button', { class: 'texte-clair blocage', onclick: () => { tireChizuk('blocage'); rendre(); } }, tr.t('chizuk_stuck')) : null,
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
    // La scène et ses chiffres, puis le reste : l'un sous l'autre, ou côte à côte
    // sur un téléphone couché (app.css, « Téléphone couché »).
    h('div', { class: 'defile' }, h('div', { class: 'contenu' },
      h('div', { class: 'seance-scene' }, corps.slice(0, 5)),
      h('div', { class: 'seance-actions' }, corps.slice(5)))));
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
      h('button', { class: 'principal', onclick: continuer }, tr.t('celebration_continue')),
      // Partager la séance faite (12/10), avec le lien de la version web.
      !annulee && bravo.minutes > 0 ? h('button', { class: 'lien-bouton bravo-partage', onclick: () => partagerSeance(bravo.minutes) }, tr.t('partage_action')) : null)));
}

const LIEN_WEB = 'https://hil123.github.io/myhitbodedut-web/';
/** Le texte d'une séance faite et le lien de la version web : le partage du téléphone, sinon le presse-papiers. */
async function partagerSeance(minutes) {
  const serie = store.streak?.() ?? 0;
  const texte = [tr.t('partage_texte', tr.p('partage_minutes', minutes)), serie >= 2 ? tr.t('partage_serie', serie) : null,
    tr.t('partage_lien', LIEN_WEB)].filter(Boolean).join('\n');
  try {
    if (navigator.share) { await navigator.share({ text: texte }); return; }
  } catch (e) { if (e?.name === 'AbortError') return; }
  try { await navigator.clipboard.writeText(texte); signale(tr.t('partage_copie')); } catch { /* rien d'autre à faire */ }
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
  return ['musique', 'reglages', 'seance', 'bravo', 'tehilim', 'lecture', 'siddur', 'priere', 'tikkun', 'bibliotheque', 'source', 'zmanim', 'calendrier', 'paliers', 'hilloulot', 'halakha'].includes(e) ? e : 'accueil';
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
window.addEventListener('popstate', () => {
  // Le retour d'un renvoi (« Hallel › ») rouvre la prière d'où l'on venait.
  if (history.state?.priere) priere = history.state.priere;
  rendre();
});

/** Un renvoi du jour : la prière [cible] en haut de page ; le retour ramène à [depuis]. */
function ouvrirRenvoi(depuis, cible) {
  history.replaceState({ ...(history.state ?? {}), priere: depuis }, '', location.hash);
  history.pushState({ interne: true, priere: cible }, '', '#priere');
  priere = cible;
  rendre();
  const defile = app.querySelector('.defile');
  if (defile) defile.scrollTop = 0;
}

function rendre() {
  appliquerPolice();
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
  else if (ecran === 'zmanim') app.append(ecranZmanim());
  else if (ecran === 'calendrier') app.append(ecranCalendrier());
  else if (ecran === 'paliers') app.append(ecranPaliers());
  else if (ecran === 'hilloulot') app.append(ecranHilloulot());
  else if (ecran === 'halakha') app.append(ecranHalakha());
  else if (ecran === 'seance') app.append(ecranSeance());
  else if (ecran === 'bravo') app.append(ecranBravo());
  else if (ecran === 'tehilim') app.append(ecranTehilim());
  else if (ecran === 'lecture') app.append(ecranLecture());
  else if (ecran === 'siddur') app.append(ecranSiddur());
  else if (ecran === 'priere') app.append(ecranPriere());
  else if (ecran === 'tikkun') app.append(ecranTikkun());
  else if (ecran === 'bibliotheque') app.append(ecranBibliotheque());
  else if (ecran === 'source') app.append(ecranSource());
  else {
    app.append(h('div', { class: 'ecran avec-onglets' },
      h('div', { class: 'defile' }, onglet === 'textes' ? panneauTextes() : panneauAujourdhui()),
      onglets()));
  }
  app.dataset.ecran = `${ecran}/${onglet}`;
  if (memeEcran) app.querySelector('.defile').scrollTop = avant;
  // L'accueil qui apparaît s'anime seul : anneaux, puis saut ou fête (04/10).
  const accueil = ecran === 'accueil' && onglet === 'aujourdhui';
  if (!accueil) { accueilAnime = false; facePlaque = 'hassid'; clearTimeout(minuteurPlaque); }
  else if (!accueilAnime) { accueilAnime = true; setTimeout(animerAccueil, 950); }
  // En quittant la lecture, le bouton se replie ; l'écran suivant le retrouve en pastille.
  if (!ECRANS_LUS.includes(ecran)) { reglagesOuverts = false; choixNusahOuvert = false; }
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
