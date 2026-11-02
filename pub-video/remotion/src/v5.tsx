import React from 'react';
import {AbsoluteFill, Audio, getInputProps, Img, Sequence, Easing, interpolate, spring, staticFile, useCurrentFrame, useVideoConfig} from 'remotion';
import {C, Z, FPS} from './theme';
import {SousTitres, Telephone, TexteEcran, fontFaces, FX, NUIT, LARGEUR_TEL} from './ui';
import {T, EcranOuverture, Titre, ETAPES, AFournir} from './v4';

/*
 * v5 — retour de Hillel (01/11) : « une image par phrase », « les hommes ne suivent pas le texte ».
 * Chaque morceau de phrase de la voix a sa pose du personnage officiel de l'app (mêmes fichiers que l'app).
 * Ouverture : fond blanc, bulles de stress (idée de Hillel). Captures recadrées sur l'élément utile.
 * Mise en page : texte écran en haut, sujet au milieu, sous-titres en dessous ; aucun chevauchement.
 */
const clamp = {extrapolateLeft: 'clamp' as const, extrapolateRight: 'clamp' as const};
const IO = Easing.inOut(Easing.cubic);
const H = Z.sujetBas - Z.sujetHaut;
/* v7 : pause raccourcie avant les fonctions ; le temps gagné va aux trophées (T.avec et la suite inchangés). */
const T5 = {...T, etapes: T.etapes - 75, duree: T.duree - 75};
const L = (f: number, a: number, b: number, x: number, y: number, e = IO) => interpolate(f, [a, b], [x, y], {...clamp, easing: e});
/* v17 : raccords fluides. Fondu enchaîné entre poses (FONDU images de chevauchement), entre captures et entre phases ; plus de flash blanc. */
const FONDU = 10;
const FONDU_PHASE = 16;

/* Voix : mêmes fichiers, nouveau calage. */
const BLOCS: Record<string, {t: number; txt: string}[]> = {
  // v18 : voix naturelle (ElevenLabs « Paul »), coupes calées sur ses pauses réelles (silencedetect)
  s01: [{t: 0.05, txt: 'Chaque jour, nous sommes occupés.'}, {t: 1.97, txt: 'Le travail,'}, {t: 2.83, txt: 'les responsabilités,'}, {t: 4.2, txt: 'les soucis…'}],
  s03: [{t: 0.05, txt: 'La Hitbodedut, c’est s’arrêter…'}, {t: 2.49, txt: 'et prendre un moment rien que pour Hachem.'}],
  s04: [{t: 0.05, txt: 'Lui parler avec tes propres mots.'}, {t: 1.99, txt: 'Lui raconter tes difficultés,'}, {t: 3.65, txt: 'tes rêves,'}, {t: 4.46, txt: 'tes joies…'}, {t: 5.49, txt: 'et tes remerciements.'}],
  s10c: [{t: 0.09, txt: 'Avec le calendrier juif,'}, {t: 1.53, txt: 'retrouve chaque jour la date hébraïque,'}, {t: 3.57, txt: 'les zmanim et les ajouts de la prière.'}],
  s11: [{t: 0.06, txt: 'Ne laisse pas passer une journée'}, {t: 1.4, txt: 'sans prendre un moment pour Hachem.'}, ],
};
/** v22 : fin resserrée — carte à 81 s, film de 87 s qui se termine avec la musique. */
export const FIN5 = 2430;
/** v27 : accroche « tempête de notifications » (0–10 s). --props accroche:'origine' pour l'ancienne. */
const ACCROCHE = ((getInputProps() as {accroche?: string}).accroche ?? (FX ? 'A' : 'origine')) as 'A' | 'origine';
const DEBUT_S01 = ACCROCHE === 'A' ? 6 : 45; // la voix démarre avant 0,5 s
export const TOTAL5 = 2610;
const VOIX: {f: string; at: number; dur: number; txt: string; trim?: number; coupe?: number}[] = [
  {f: 's01', at: DEBUT_S01, dur: 4.95, txt: 'Chaque jour, nous sommes occupés. Le travail, les responsabilités, les soucis…'},
  {f: 's02', at: 300, dur: 3.0, txt: 'Mais prenons-nous vraiment le temps de parler à Hachem ?'},
  // v12 : le bruit de la foule coupe net à 430, silence, puis la voix annonce le nom
  {f: 's05a', at: 442, dur: 1.65, txt: 'MY HITBODEDUT.'},
  {f: 's05b', at: 500, dur: 1.8, txt: 'Pour t’accompagner dans cette démarche.'},
  {f: 's03', at: 600, dur: 4.5, txt: 'La Hitbodedut, c’est s’arrêter… et prendre un moment rien que pour Hachem.'},
  {f: 's04', at: 765, dur: 6.45, txt: 'Lui parler avec tes propres mots. Lui raconter tes difficultés, tes rêves, tes joies… et tes remerciements.'},
  {f: 's06', at: 1065, dur: 4.75, txt: 'Cinq étapes pour t’aider à structurer ta Hitbodedut, inspirées des enseignements de nos Sages.'},
  {f: 's07', at: 1265, dur: 3.55, txt: 'Choisis la durée de ton rendez-vous quotidien et avance à ton rythme.'},
  {f: 's08', at: 1520, dur: 4.7, txt: 'Bloque les distractions. Mets ton téléphone de côté. Ce moment est consacré à Hachem.'},
  {f: 's09', at: 1785, dur: 3.8, txt: 'Personnalise ton expérience selon tes préférences et ton rythme de vie.'},
  {f: 's10c', at: 1975, dur: 5.5, txt: 'Avec le calendrier juif, retrouve chaque jour la date hébraïque, les zmanim et les ajouts de la prière.'},
  {f: 's11', at: 2262, dur: 2.95, txt: 'Ne laisse pas passer une journée sans prendre un moment pour Hachem.', coupe: 92}, // l'audio s'arrête à 3,07 s, avant « Commence » (3,26 s)
  // v22 : « Commence aujourd'hui » (même fichier, coupé à 3,15 s) est dit SUR le bouton de la carte finale, sans sous-titre (le bouton l'écrit déjà)
  {f: 's11', at: 2450, dur: 0.95, txt: '', trim: 95},
];
const volumeMusique = (f: number) => {
  let d = 0;
  for (const v of VOIX) d = Math.max(d, interpolate(f, [v.at - 8, v.at, v.at + v.dur * FPS, v.at + v.dur * FPS + 12], [0, 1, 1, 0], clamp));
  return (0.9 - d * 0.66) * interpolate(f, [TOTAL5 - 50, TOTAL5], [1, 0], clamp);
};

/*
 * Une pose par morceau de phrase. de/a = images ; type 'perso' (détouré) ou 'carte' (scène avec décor).
 * Les bornes suivent la voix (découpage au prorata des caractères, comme les sous-titres).
 */
type Pose = {de: number; a: number; src: string; type: 'perso' | 'carte'; mot: string; bulle?: string; bulles?: string[]; teinte: string; dir: 1 | -1; marche?: boolean; retour?: boolean; fixe?: boolean; lumiere?: boolean};
const POSES_ORIGINE: Pose[] = [
  // s01 « Chaque jour, nous sommes occupés. | Le travail, | les responsabilités, | les soucis… »
  {de: 0, a: 104, src: 'char_walking', type: 'perso', mot: 'occupés', bulle: 'En retard !', teinte: '#e8f1f0', dir: 1, marche: true},
  {de: 104, a: 130, src: 'char_writing', type: 'perso', mot: 'le travail', bulle: 'Encore 3 dossiers…', teinte: '#eef0f5', dir: -1},
  {de: 130, a: 171, src: 'scene_airport', type: 'carte', mot: 'les responsabilités', bulle: 'Tout repose sur moi', teinte: '#eef3f6', dir: 1},
  {de: 171, a: 300, src: 'char_thinking', type: 'perso', mot: 'les soucis', bulle: 'Et demain ?', teinte: '#f1efec', dir: -1},
  // s02 « Mais prenons-nous vraiment le temps de parler à Hachem ? »
  // plan A : il s'arrête, de dos, face à l'horizon ; les bulles de stress s'éteignent une à une
  {de: 300, a: 366, src: 'char_standing_back', type: 'perso', mot: 'prenons-nous le temps', teinte: '#eef3f2', dir: 1, bulles: ['En retard !', 'Encore 3 dossiers…', 'Tout repose sur moi', 'Et demain ?']},
  // plan B : poussée lente, l'horizon s'éclaircit sur « parler à Hachem ? »
  {de: 366, a: 430, src: 'char_standing_back', type: 'perso', mot: 'parler à Hachem', teinte: '#fbf3df', dir: 1, fixe: true, lumiere: true},
  // 430–600 : le téléphone, l'application s'ouvre (s05)
  // s03 « La Hitbodedut, c'est s'arrêter… | et prendre un moment rien que pour Hachem. »
  {de: 600, a: 677, src: 'char_debout', type: 'perso', mot: 's’arrêter', teinte: '#eaf4f1', dir: -1},
  {de: 677, a: 765, src: 'scene_nature', type: 'carte', mot: 'un moment pour Hachem', teinte: '#f6efe2', dir: 1},
  // s04 « Lui parler avec tes propres mots. | Lui raconter tes difficultés, | tes rêves, | tes joies | et tes remerciements. »
  {de: 765, a: 825, src: 'char_praise', type: 'perso', mot: 'parler', teinte: '#f6efe2', dir: -1},
  {de: 825, a: 875, src: 'char_praying', type: 'perso', mot: 'tes difficultés', teinte: '#ecf0f4', dir: 1},
  {de: 875, a: 899, src: 'char_pointing', type: 'perso', mot: 'tes rêves', teinte: '#eef0fa', dir: -1},
  {de: 899, a: 930, src: 'char_dancing_joy', type: 'perso', mot: 'tes joies', teinte: '#fbf3df', dir: 1},
  {de: 930, a: 1028, src: 'char_open_arms', type: 'perso', mot: 'tes remerciements', teinte: '#f8eedb', dir: -1},
  {de: 1028, a: 1055, src: 'char_offering', type: 'perso', mot: 'avec ton cœur', teinte: '#e8f3ef', dir: 1},
  // 1055–2200 : les fonctions (captures réelles) — 1960–2200 : le calendrier juif
  // s11 « Ne laisse pas passer une journée | sans prendre un moment pour Hachem. | Commence aujourd'hui. »
  {de: 2200, a: 2309, src: 'char_walking', type: 'perso', mot: 'une journée', teinte: '#e8f1f0', dir: 1, marche: true, retour: true, bulles: ['En retard !', 'Encore 3 dossiers…', 'Tout repose sur moi', 'Et demain ?']},
  {de: 2309, a: 2360, src: 'scene_nature', type: 'carte', mot: 'un moment pour Hachem', teinte: '#f6efe2', dir: 1},
  // 2360–FIN5 (2430) : l’écran « Bravo, Tsadik ! » dans le téléphone, puis la carte finale
];
// accroche A : les 4 poses d'ouverture sont remplacées par la tempête de notifications ; le personnage pensif revient à 6,7 s
const POSES: Pose[] = ACCROCHE === 'A'
  ? [{de: 200, a: 300, src: 'char_thinking', type: 'perso', mot: 'le monde ne s’arrête jamais', teinte: '#f1efec', dir: -1}, ...POSES_ORIGINE.filter((p) => p.de >= 300)]
  : POSES_ORIGINE;

const RATIO: Record<string, number> = {char_standing_back: 204 / 640, char_walking: 328 / 640, char_writing: 637 / 640, char_thinking: 544 / 640, char_face: 465 / 640, char_debout: 800 / 1200,
  char_praise: 396 / 640, char_praying: 370 / 640, char_pointing: 383 / 640, char_dancing_joy: 517 / 900, char_open_arms: 626 / 900, char_offering: 353 / 640,
  char_thumbs_up: 244 / 640, scene_airport: 253 / 269, scene_nature: 262 / 259, scene_sleeping: 242 / 303};

const BulleStress: React.FC<{texte: string; f: number; cote: 1 | -1}> = ({texte, f, cote}) => {
  const s = spring({frame: f - 6, fps: FPS, config: {damping: 12, mass: 0.6}});
  const trem = Math.sin(f / 2.2) * 1.8;
  return (
    <div style={{position: 'absolute', top: 50, left: 0, right: 0, display: 'flex', justifyContent: 'center', transform: `translateX(${trem}px) scale(${s})`, transformOrigin: '50% 100%'}}>
      <div style={{fontFamily: 'Inter', fontWeight: 700, fontSize: 34, color: '#7a1f1f', background: '#fff', border: '3px solid #2a2a2a', borderRadius: 30, padding: '12px 22px',
        boxShadow: '0 6px 16px rgba(0,0,0,.12)', whiteSpace: 'nowrap'}}>{texte}</div>
    </div>
  );
};

/** Les quatre bulles de l'ouverture, au-dessus de lui (jamais sur lui) ; elles s'éteignent une à une. */
const BullesQuiSEteignent: React.FC<{textes: string[]; f: number; retour?: boolean}> = ({textes, f, retour}) => (
  <>
    {textes.map((t, i) => {
      // retour : elles réapparaissent puis éclatent une à une (il ne se laisse plus emporter)
      const o = retour ? Math.min(spring({frame: f - 4 - i * 6, fps: FPS, config: {damping: 12}}), L(f, 60 + i * 9, 68 + i * 9, 1, 0)) : L(f, 10 + i * 12, 20 + i * 12, 1, 0);
      const eclat = retour ? L(f, 60 + i * 9, 68 + i * 9, 1, 1.35) : 1;
      return (
        <div key={i} style={{position: 'absolute', top: i < 2 ? 24 : 120, [i % 2 ? 'right' : 'left']: 50, opacity: o, transform: `scale(${(0.85 + 0.15 * o) * eclat}) translateY(${retour ? 0 : (1 - o) * -20}px)`,
          fontFamily: 'Inter', fontWeight: 700, fontSize: 30, color: '#7a1f1f', background: '#fff', border: '3px solid #2a2a2a', borderRadius: 28, padding: '10px 20px',
          boxShadow: '0 6px 16px rgba(0,0,0,.12)', whiteSpace: 'nowrap', filter: `blur(${(1 - o) * 6}px)`}}>{t}</div>
      );
    })}
  </>
);

/** Un plan « pose » : entrée glissée, poussée lente, sortie rapide ; tout reste dans la zone sujet. */
const PlanPose: React.FC<{p: Pose; enchaine: number}> = ({p, enchaine}) => {
  const f = useCurrentFrame(); // local à la séquence
  const dur = p.a - p.de;
  // entrée : fondu + glissé court depuis la droite ; sortie : fondu pendant que le plan suivant entre (même sens, le regard suit)
  const entree = p.fixe ? 1 : spring({frame: f, fps: FPS, config: {damping: 26, mass: 0.8}});
  const apparition = p.fixe ? 1 : L(f, 3, FONDU + 2, 0, 1, Easing.out(Easing.cubic)); // entre quand le plan précédent est presque parti : pas de double exposition
  const sortie = enchaine ? L(f, dur - 2, dur + Math.round(enchaine * 0.6), 0, 1, Easing.in(Easing.sin)) : 0;
  const pousse = p.fixe ? L(f, 0, dur, 1.06, 1.22, Easing.inOut(Easing.sin)) : L(f, 0, dur + enchaine, 1, 1.06, Easing.inOut(Easing.sin));
  const dx = (1 - entree) * 90 - sortie * 90;
  const marche = p.marche ? L(f, 0, dur + enchaine, -200, 200 + (200 * enchaine) / dur, (t) => t) : 0;
  const rebond = p.marche ? Math.abs(Math.sin(f / 4)) * -10 : p.fixe || p.bulles ? 0 : Math.sin(f / 20) * 4;
  const r = RATIO[p.src];
  // avec une bulle : le sujet tient sous la bulle (haut ≥ 220 px dans la zone), jamais dessous ni dessus
  const hMax = p.bulle || p.bulles || p.fixe ? 600 : p.type === 'perso' ? 760 : H - 80;
  let hauteur = hMax, largeur = hauteur * r;
  if (largeur > 900) { largeur = 900; hauteur = largeur / r; }
  if (p.type === 'carte' && largeur > 680) { largeur = 680; hauteur = largeur / r; }
  const decal = 0;
  return (
    <div style={{position: 'absolute', left: 0, right: 0, top: Z.sujetHaut, height: H, overflow: 'hidden', opacity: apparition * (1 - sortie)}}>
      <AbsoluteFill style={{background: NUIT ? 'radial-gradient(ellipse 48% 42% at 50% 55%, rgba(90,150,220,.30) 0, rgba(40,80,140,.10) 45%, rgba(0,0,0,0) 70%)'
        : `radial-gradient(circle at 50% 55%, ${p.teinte} 0, rgba(255,255,255,0) 62%)`}} />
      <div style={{position: 'absolute', left: 540 - largeur / 2 + dx + marche + decal, bottom: p.type === 'perso' || p.bulle || p.bulles ? 14 : (H - hauteur) / 2, width: largeur, height: hauteur,
        transform: `translateY(${rebond}px) scale(${pousse})`, transformOrigin: '50% 100%'}}>
        {p.type === 'perso' && <div style={{position: 'absolute', left: '15%', right: '15%', bottom: -8, height: 20, borderRadius: '50%', background: 'rgba(0,0,0,.10)', filter: 'blur(6px)'}} />}
        <Img src={staticFile(`img/${p.src}.webp`)} style={{width: '100%', height: '100%', position: 'relative',
          // nuit : lumière latérale (une moitié dans l'ombre) + liseré bleu, comme la montre de l'exemple
          ...(NUIT && p.type === 'perso' ? {filter: 'drop-shadow(0 0 1px rgba(150,200,255,.9)) drop-shadow(0 0 18px rgba(70,140,230,.45))'} : {}),
          ...(p.type === 'carte' ? (NUIT ? {borderRadius: 24, border: '2px solid rgba(160,200,255,.35)', boxShadow: '0 0 60px rgba(60,130,220,.35), 0 30px 80px rgba(0,0,0,.6)'}
            : {borderRadius: 28, border: '10px solid #fff', boxShadow: '0 18px 50px rgba(7,40,40,.22)'}) : {})}} />
        {NUIT && <div style={{position: 'absolute', inset: 0, borderRadius: p.type === 'carte' ? 24 : 0, pointerEvents: 'none',
          background: 'linear-gradient(100deg, rgba(3,8,15,.55) 0%, rgba(3,8,15,.15) 40%, rgba(0,0,0,0) 60%)',
          ...(p.type === 'perso' ? {WebkitMaskImage: `url(${staticFile(`img/${p.src}.webp`)})`, WebkitMaskSize: '100% 100%'} : {})}} />}
      </div>
      {p.lumiere && <AbsoluteFill style={{background: 'radial-gradient(circle at 50% 30%, rgba(255,236,190,.9) 0, rgba(255,255,255,0) 55%)', opacity: L(f, 0, dur, 0.15, 1)}} />}
      {/* la bulle s'efface avant le fondu : deux textes ne se superposent jamais */}
      {p.bulle && <div style={{opacity: L(f, dur - 6, dur, 1, 0)}}><BulleStress texte={p.bulle} f={f} cote={p.dir} /></div>}
      {p.bulles && <BullesQuiSEteignent textes={p.bulles} f={f} retour={p.retour} />}
    </div>
  );
};

/*
 * Plans du téléphone : quelle capture, et quelle bande verticale montrer (y = % de la hauteur du téléphone).
 * z ≤ 2,2 : l'écran agrandi (402 px × 2,2 = 884 px) tient dans la largeur de l'image, donc aucune ligne n'est coupée.
 */
type PlanTel = {de: number; a: number; src: string; y: number; z0: number; z1: number; aFournir?: string; basseDef?: boolean};
const PLANS_TEL: PlanTel[] = [
  // v18 : captures réelles fournies par Hillel le 02/11 (captures/v2), plus aucune carte « à fournir »
  {de: 505, a: 600, src: 'v2/parler_simplement', y: 35, z0: 1, z1: 1.4},             // A « Pour t’accompagner » : 1er écran de l’app
  {de: T5.etapes, a: T5.duree, src: 'v2/mots_questions', y: 30, z0: 1, z1: 1.04},   // B « Cinq étapes… » : questions guidées
  {de: T5.duree, a: 1262, src: 'v2/duree_15', y: 38, z0: 1.25, z1: 1.55},              // C « Choisis la durée… » : 15 min
  {de: 1262, a: 1343, src: 'v2/duree_15', y: 76, z0: 1.55, z1: 1.8},                   //   les boutons 5 min → 1 h 30
  {de: 1343, a: 1428, src: 'v2/accueil_anneaux', y: 40, z0: 1.3, z1: 1.6},             // D « …avance à ton rythme » : anneaux, prochain trophée
  {de: 1428, a: T.avec, src: 'web_trophees_haut', y: 35, z0: 1.5, z1: 1.95},           //   grade Or, 25 trophées
  {de: T.avec, a: 1575, src: 'v2/je_suis_avec_hachem_basse_def', y: 40, z0: 1, z1: 1.08, basseDef: true}, // E « Bloque les distractions »
  {de: 1575, a: 1630, src: 'v2/choisir_bloquer_basse_def', y: 60, z0: 1, z1: 1.08, basseDef: true},       //   choix des applications
  {de: 1630, a: T.perso, src: 'v2/duree_15', y: 48, z0: 1.2, z1: 1.9},                 //   « posez le téléphone »
  {de: T.perso, a: 1802, src: 'web_langue', y: 108, z0: 1.6, z1: 2.2},                  // F « Personnalise… »
  {de: 1802, a: 1834, src: 'web_onboarding_6', y: 68, z0: 1.6, z1: 2.0},
  {de: 1834, a: 1866, src: 'web_couleur_fond', y: 108, z0: 1.6, z1: 2.2},
  {de: 1866, a: 1898, src: 'web_reglages', y: 48, z0: 1.4, z1: 1.8},
  {de: 1898, a: 1929, src: 'v2/heures_du_mode_basse_def', y: 60, z0: 1, z1: 1.06, basseDef: true},
  {de: 1929, a: T.chabbat, src: 'v2/duree_du_report_basse_def', y: 50, z0: 1, z1: 1.06, basseDef: true},
  {de: T.chabbat, a: 2077, src: 'web_calendrier', y: 33, z0: 1.4, z1: 1.9},             // G le calendrier
  {de: 2077, a: 2150, src: 'v2/zmanim', y: 45, z0: 1.25, z1: 1.55},                     //   « les zmanim et les ajouts de la prière » : date hébraïque + Dans les prières
  ...(FX ? [{de: 2150, a: T.retour, src: 'v2/siddour', y: 50, z0: 1.0, z1: 1.0}] : [            // v23 : éventail de 3 téléphones
  {de: 2150, a: 2167, src: 'v2/siddour', y: 30, z0: 1.15, z1: 1.2},                    // H montage rapide (musique seule)
  {de: 2167, a: 2184, src: 'v2/bibliotheque', y: 30, z0: 1.15, z1: 1.2},
  {de: 2184, a: T.retour, src: 'v2/boussole', y: 35, z0: 1.15, z1: 1.2}]),
  {de: 2360, a: FIN5, src: 'v2/bravo', y: 40, z0: 1.05, z1: 1.3},                      // I « Commence aujourd’hui » : Bravo, Tsadik !
];
const indexTel = (f: number) => PLANS_TEL.findIndex((p) => f >= p.de && f < p.a);
const zPlan = (p: PlanTel, f: number) => interpolate(f, [p.de, p.a], [p.z0, p.z1], {...clamp, easing: Easing.inOut(Easing.sin)});
/* caméra continue : au changement de capture, elle glisse depuis le cadre précédent au lieu de sauter */
const RACCORD_CAM = 14;
const camTel = (f: number) => {
  const i = indexTel(f);
  if (i < 0) return {z: 1, y: 50};
  const p = PLANS_TEL[i], prev = PLANS_TEL[i - 1];
  const z = zPlan(p, f);
  if (!prev || prev.a !== p.de || f >= p.de + RACCORD_CAM) return {z, y: p.y};
  const t = L(f, p.de, p.de + RACCORD_CAM, 0, 1, Easing.inOut(Easing.sin));
  return {z: prev.z1 + (z - prev.z1) * t, y: prev.y + (p.y - prev.y) * t};
};
/** Style nuit : l'application elle-même en mode sombre (captures réelles de la version web, 02/11). Les 4 écrans propres à Android restent tels quels. */
const SOMBRE: Record<string, string> = {
  'v2/parler_simplement': 'sombre/seance_question', 'v2/mots_questions': 'sombre/seance_question', 'v2/duree_15': 'sombre/duree_15',
  'v2/accueil_anneaux': 'sombre/accueil_anneaux', 'v2/zmanim': 'sombre/zmanim', 'v2/siddour': 'sombre/siddour', 'v2/bibliotheque': 'sombre/bibliotheque',
  'v2/boussole': 'sombre/boussole', 'v2/bravo': 'sombre/bravo', web_calendrier: 'sombre/calendrier', web_reglages: 'sombre/reglages',
};
const cap = (src: string) => (NUIT && SOMBRE[src]) || src;
const Capture: React.FC<{p: PlanTel}> = ({p}) =>
  p.src ? <Img src={staticFile(`captures/${cap(p.src)}.png`)} style={{position: 'absolute', width: '100%', height: '100%', objectFit: 'cover', objectPosition: '50% 0%'}} /> : <AFournir titre={p.aFournir!} />;
const EcranPlein: React.FC<{f: number}> = ({f}) => {
  const i = indexTel(f);
  if (i < 0) return null;
  const p = PLANS_TEL[i], prev = PLANS_TEL[i - 1];
  const enchaine = prev && prev.a === p.de;
  if (FX) {
    // v20 : l'écran suivant monte en poussant le précédent (geste d'app), avec un reflet qui balaie la vitre
    const meme = (enchaine && prev.src === p.src) || COUPES_SECTION.includes(p.de); // aux changements de partie, c'est le téléphone entier qui fait la transition
    const t = enchaine && !meme ? L(f, p.de, p.de + 14, 0, 1, Easing.bezier(0.22, 1, 0.36, 1)) : 1;
    const reflet = enchaine && !meme ? L(f, p.de + 2, p.de + 22, -0.6, 1.6, Easing.inOut(Easing.sin)) : -1;
    return (
      <AbsoluteFill style={{overflow: 'hidden'}}>
        {t < 1 && <AbsoluteFill style={{transform: `translateY(${-t * 18}%) scale(${1 - 0.06 * t})`, opacity: 1 - 0.6 * t, filter: `blur(${t * 3}px)`}}><Capture p={prev} /></AbsoluteFill>}
        <AbsoluteFill style={{transform: `translateY(${(1 - t) * 100}%)`, boxShadow: t < 1 ? '0 -20px 40px rgba(0,0,0,.35)' : undefined}}><Capture p={p} /></AbsoluteFill>
        {reflet > -0.6 && reflet < 1.6 && <AbsoluteFill style={{background: `linear-gradient(115deg, transparent ${(reflet - 0.25) * 100}%, rgba(255,255,255,.28) ${reflet * 100}%, transparent ${(reflet + 0.25) * 100}%)`}} />}
      </AbsoluteFill>
    );
  }
  const t = enchaine ? L(f, p.de, p.de + FONDU, 0, 1, Easing.inOut(Easing.sin)) : 1;
  return (
    <AbsoluteFill>
      {t < 1 && <Capture p={prev} />}
      <AbsoluteFill style={{opacity: t}}><Capture p={p} /></AbsoluteFill>
    </AbsoluteFill>
  );
};

const JOURS = [
  {de: T5.etapes, a: 1343, texte: 'Jour 1'},      // étapes guidées, puis il choisit sa durée
  {de: 1343, a: T.avec, texte: 'Jour 6'},          // trophées : la capture dit « sur 6 jours »
  {de: T.avec, a: T.chabbat, texte: 'Jour 7'},     // « Je suis avec Hachem », personnalisation
  {de: T.chabbat, a: 2150, texte: 'Chaque jour'}, // le calendrier
  {de: 2150, a: T.retour, texte: 'Siddour · Bibliothèque · Boussole'}, // montage rapide
];
const CompteurJour: React.FC<{texte: string; dur: number}> = ({texte, dur}) => {
  const f = useCurrentFrame();
  const s = spring({frame: f - 6, fps: FPS, config: {damping: 12, mass: 0.6}});
  const o = Math.min(s, L(f, dur - 8, dur, 1, 0));
  if (FX) {
    const e = spring({frame: f - 2, fps: FPS, config: {damping: 15, mass: 0.7}});
    const x = L(f, dur - 8, dur, 0, 1, Easing.in(Easing.cubic));
    return (
      <div style={{position: 'absolute', left: 0, right: 0, top: 310, height: Z.texteBas - 310, display: 'flex', alignItems: 'center', justifyContent: 'center', overflow: 'hidden',
        WebkitMaskImage: 'linear-gradient(180deg, transparent, #000 25%, #000 75%, transparent)'}}>
        <div style={{fontFamily: 'Inter', fontWeight: 800, fontSize: texte.length > 14 ? 38 : 52, letterSpacing: 2, color: '#a88233', whiteSpace: 'nowrap',
          transform: `translateY(${(1 - e) * 70 - x * 70}px)`, opacity: Math.min(1, e * 1.5) * (1 - x), filter: `blur(${(1 - Math.min(1, e)) * 6 + x * 6}px)`}}>{texte}</div>
      </div>
    );
  }
  return (
    <div style={{position: 'absolute', left: 0, right: 0, top: 310, height: Z.texteBas - 310, display: 'flex', alignItems: 'center', justifyContent: 'center', opacity: o}}>
      <div style={{fontFamily: 'Inter', fontWeight: 800, fontSize: texte.length > 14 ? 38 : 52, letterSpacing: 2, color: '#a88233', whiteSpace: 'nowrap', transform: `scale(${0.7 + 0.3 * s})`}}>{texte}</div>
    </div>
  );
};

/* Musique choisie au rendu : --props='{"musique":"m_dvorak_americain_lento"}' (domaine public, voir FICHE_CREDITS.md). */
/* v12 : 0–430 bruits de foule (CC0) ; coupe nette ; à 482 la musique de présentation.
   « Beyond » de Noam Shpayer (fichier de l'app, accord écrit du 29/10, voir PUBLICATION_PLAY_STORE.md § 3.2),
   calé pour que le « drop » du morceau tombe à 73,3 s (retour du plan d'ouverture). Autres choix : joie_provisoire, musique_deux_temps. */
const MUSIQUE: string = (getInputProps() as {musique?: string}).musique ?? 'beyond';
const DEBUT_MUSIQUE = 462;
const volumeAmbiance = (f: number) => {
  let d = 0;
  for (const v of VOIX) d = Math.max(d, interpolate(f, [v.at - 8, v.at, v.at + v.dur * FPS, v.at + v.dur * FPS + 12], [0, 1, 1, 0], clamp));
  return 0.9 - d * 0.6;
};

// Applis bloquées (marques citées à titre illustratif)
const ASPIRE = 1550; // début de l'aspiration des logos (voix : « Bloque les distractions »)
const LOGOS = [
  {n: 'whatsapp', bg: '#25D366'}, {n: 'youtube', bg: '#FF0000'},
  {n: 'facebook', bg: '#0866FF'}, {n: 'instagram', bg: 'linear-gradient(45deg, #F58529, #DD2A7B 50%, #8134AF 75%, #515BD4)'},
  {n: 'tiktok', bg: '#000000'}, {n: 'snapchat', bg: '#FFFC00'},
];


/* v21 — une transition DIFFÉRENTE et motivée à chaque changement de partie (au lieu du même fondu partout) */
const COUPES_SECTION = [T5.duree, T.avec, T.perso, T.chabbat];
type Tr = {tx: number; ry: number; sc: number; flou: number; flouX: number; lum: number; voile: number};
/* v24 : mouvements physiques — ressorts avec léger dépassement à l'atterrissage, flou proportionnel à la VITESSE réelle,
   éclairage qui suit l'angle du téléphone (ombre quand il se retourne). */
const ressort = (f: number, b: number, damping: number) => spring({frame: f - b, fps: FPS, config: {damping, mass: 0.7, stiffness: 120}});
const etatSection = (f: number) => {
  const r = {tx: 0, ry: 0, sc: 1, lum: 1, voile: 0};
  for (const b of COUPES_SECTION) {
    if (f < b - 10 || f > b + 40) continue;
    const avant = f < b;
    const k = avant ? L(f, b - 10, b, 0, 1, Easing.in(Easing.cubic)) : 0;
    if (b === T5.duree) { // 1 → 2 : plongée dans l'écran, le suivant arrive de loin et se pose avec un rebond
      r.sc = avant ? 1 + 0.55 * k : 0.8 + 0.2 * ressort(f, b, 13); r.lum = avant ? 1 + 0.25 * k : 1;
    } else if (b === T.avec) { // 2 → 3 : whip ; atterrissage en ressort (dépasse un peu, revient)
      r.tx = avant ? -k * 1150 : 820 * (1 - ressort(f, b, 12)); // repart juste hors champ : pas d'image vide
    } else if (b === T.perso) { // 3 → 4 : retournement 3D, se pose avec un léger balancement
      r.ry = avant ? k * 90 : -90 * (1 - ressort(f, b, 14));
      r.sc = 1 - 0.08 * Math.abs(Math.sin((r.ry * Math.PI) / 180));
      r.lum = 0.55 + 0.45 * Math.cos((r.ry * Math.PI) / 180); // la face qui tourne s'assombrit
    } else if (f < b + 14) { // 4 → 5 : passage par la lumière
      const kk = avant ? k : 1 - L(f, b, b + 14, 0, 1, Easing.out(Easing.cubic));
      r.lum = 1 + 0.45 * kk; r.voile = kk; r.sc = 1 + 0.04 * kk;
    }
  }
  return r;
};
const transitionSection = (f: number): Tr => {
  const r: Tr = {tx: 0, ry: 0, sc: 1, flou: 0, flouX: 0, lum: 1, voile: 0};
  if (!FX) return r;
  const e = etatSection(f), p = etatSection(f - 1);
  const vx = Math.abs(e.tx - p.tx), vs = Math.abs(e.sc - p.sc), vr = Math.abs(e.ry - p.ry);
  return {tx: e.tx, ry: e.ry, sc: e.sc, lum: e.lum, voile: e.voile,
    flouX: Math.min(80, vx * 0.55), flou: Math.min(12, vs * 60 + vr * 0.12)};
};
/** Grain argentique + vignette : texture commune à tout le film (couche du dessus). */
const Texture: React.FC<{f: number}> = ({f}) => (
  <AbsoluteFill style={{pointerEvents: 'none'}}>
    <svg width="100%" height="100%" style={{position: 'absolute', opacity: NUIT ? 0.06 : 0.07, mixBlendMode: NUIT ? 'screen' : 'multiply'}}>
      <filter id="grain"><feTurbulence type="fractalNoise" baseFrequency="0.85" numOctaves="2" seed={f % 37} /><feColorMatrix type="saturate" values="0" /></filter>
      <rect width="100%" height="100%" filter="url(#grain)" />
    </svg>
    <AbsoluteFill style={{background: 'radial-gradient(ellipse at 50% 45%, rgba(0,0,0,0) 55%, rgba(10,30,30,.16) 100%)'}} />
  </AbsoluteFill>
);
/** Fuite de lumière chaude qui traverse l'image (transition 4 → 5). */
const FuiteLumiere: React.FC<{k: number; f: number}> = ({k, f}) => k <= 0 ? null : (<>
  <div style={{position: 'absolute', left: -200, right: -200, top: Z.sujetHaut + H * 0.45 - 3, height: 6, pointerEvents: 'none', opacity: k,
    background: `linear-gradient(90deg, transparent, rgba(255,190,110,.9) ${30 + (f - T.chabbat + 10) * 2.6}%, transparent)`, filter: 'blur(2px)', boxShadow: '0 0 30px 8px rgba(255,200,120,.6)'}} />
  <AbsoluteFill style={{pointerEvents: 'none', mixBlendMode: NUIT ? 'screen' : 'multiply', opacity: NUIT ? 0.7 * k : k,
    background: `radial-gradient(ellipse 80% 55% at ${20 + (f - T.chabbat + 10) * 2.6}% 45%, rgba(255,196,110,1) 0, rgba(255,222,170,.7) 40%, rgba(255,255,255,0) 75%)`}} />
</>);
/** Révélation par la lumière (cf. exemple de Hillel) : le téléphone sort de l'ombre, un faisceau le balaie de gauche à droite. */
const revele = (f: number) => FX ? L(f, 432, 470, 0, 1, Easing.inOut(Easing.cubic)) : 1;
/** Texte qui se resserre (tracking) en sortant du flou — pour la phrase-écho du retour. */
const TexteTracking: React.FC<{texte: string; debut: number; fin: number}> = ({texte, debut, fin}) => {
  const f = useCurrentFrame();
  const e = L(f, debut, debut + 30, 0, 1, Easing.out(Easing.cubic));
  const x = L(f, fin - 9, fin, 0, 1, Easing.in(Easing.sin));
  return (
    <div style={{position: 'absolute', left: 50, right: 50, top: Z.texteHaut, height: Z.texteBas - Z.texteHaut, display: 'flex', alignItems: 'center', justifyContent: 'center', textAlign: 'center',
      opacity: e * (1 - x), filter: `blur(${(1 - e) * 12 + x * 6}px)`}}>
      <div style={{fontFamily: 'FrankRuhl, serif', fontSize: 50, lineHeight: 1.18, color: NUIT ? '#eef3f8' : C.petrole, textShadow: NUIT ? '0 0 24px rgba(120,180,255,.35)' : undefined, letterSpacing: `${0.5 + (1 - e) * 14}px`}}>{texte}</div>
    </div>
  );
};


/* ---------- v23 : effets « stimulants » ---------- */
/** Temps forts de « Beyond » (détection d'attaques sur le fichier, frames vidéo) : légers coups de zoom calés sur la musique après le drop. */
const TEMPS_FORTS = [2230, 2248, 2259, 2294, 2354, 2377];
const punch = (f: number) => !FX ? 1 : 1 + TEMPS_FORTS.reduce((a, b) => a + (f >= b ? 0.03 * Math.exp(-(f - b) / 5) : 0), 0);
/** Secousse d'atterrissage après le whip (2 → 3). */
const secousse = (_f: number) => 0; // v24 : remplacée par l'atterrissage en ressort du whip
/** Confettis dorés sur « Bravo, Tsadik ! ». */
const CONFETTIS = Array.from({length: 70}, (_, i) => {
  const a = -Math.PI / 2 + ((i * 97) % 100 - 50) / 50 * 1.25;
  const v = 16 + ((i * 53) % 13);
  return {vx: Math.cos(a) * v, vy: Math.sin(a) * v, c: ['#d4af5f', '#f3dd9c', '#14968c', '#ffffff', '#e8c372'][i % 5], w: 10 + (i % 4) * 4, h: 6 + (i % 3) * 5, rot: (i * 37) % 360, vr: ((i * 29) % 21) - 10};
});
const Confettis: React.FC<{f: number; de: number}> = ({f, de}) => {
  const t = f - de;
  if (!FX || t < 0 || t > 75) return null;
  return (
    <div style={{position: 'absolute', left: 540, top: Z.sujetHaut + H * 0.45, pointerEvents: 'none'}}>
      {CONFETTIS.map((c, i) => {
        const z = 0.6 + ((i * 41) % 9) / 10; // profondeur : 0,6 (loin) → 1,4 (près)
        const frein = 1 - Math.exp(-t / 14); // la résistance de l'air freine l'éclat initial
        const x = c.vx * 14 * frein * z + Math.sin(t / 5 + i) * 10 * frein, y = c.vy * 14 * frein * z + 0.09 * t * t * z;
        return <div key={i} style={{position: 'absolute', left: x, top: y, width: c.w * z, height: c.h * z, background: c.c, borderRadius: 2, boxShadow: '0 1px 3px rgba(0,0,0,.2)',
          filter: Math.abs(z - 1) > 0.3 ? `blur(${Math.abs(z - 1) * 3}px)` : undefined,
          transform: `rotate(${c.rot + c.vr * t}deg) rotateX(${t * 14 + i * 20}deg)`, opacity: L(t, 50, 75, 1, 0)}} />;
      })}
    </div>
  );
};
/** Onde qui part du téléphone à chaque appli avalée. */
const Ondes: React.FC<{f: number}> = ({f}) => !FX ? null : (
  <>{LOGOS.map((_, i) => {
    const t = f - (ASPIRE + 17 + i * 9);
    if (t < 0 || t > 24) return null;
    const e = L(t, 0, 24, 0, 1, Easing.out(Easing.cubic));
    return <div key={i} style={{position: 'absolute', left: 540 - 140 - 260 * e, top: Z.sujetHaut + H / 2 - 140 - 260 * e, width: 280 + 520 * e, height: 280 + 520 * e,
      borderRadius: '50%', border: `${4 * (1 - e) + 1}px solid ${C.turquoise}`, opacity: 0.6 * (1 - e)}} />;
  })}</>
);
/** Rayons de lumière sur la scène de nature (moment avec Hachem). */
const Rayons: React.FC<{f: number; de: number; a: number}> = ({f, de, a}) => {
  if (!FX || f < de || f >= a + 10) return null;
  const o = Math.min(L(f, de, de + 14, 0, 1), L(f, a - 4, a + 10, 1, 0));
  return <div style={{position: 'absolute', left: 0, right: 0, top: Z.sujetHaut, height: H, pointerEvents: 'none', mixBlendMode: NUIT ? 'screen' : 'soft-light', opacity: (NUIT ? 0.35 : 0.9) * o,
    background: `repeating-conic-gradient(from ${200 + (f - de) * 0.25}deg at 70% 8%, rgba(255,236,190,.95) 0deg 4deg, rgba(255,236,190,0) 4deg 11deg)`,
    WebkitMaskImage: 'radial-gradient(ellipse 80% 70% at 70% 10%, #000 0, transparent 75%)'}} />;
};
/** Éventail de 3 téléphones : Siddour au centre, Bibliothèque et Boussole sortent de derrière. */
const Eventail: React.FC<{f: number}> = ({f}) => {
  if (!FX || f < 2150 || f >= T.retour + FONDU) return null;
  const e = spring({frame: f - 2152, fps: FPS, config: {damping: 16, mass: 0.8}});
  return (<>{[{src: 'v2/bibliotheque', s: -1}, {src: 'v2/boussole', s: 1}].map((q) => (
    <div key={q.src} style={{position: 'absolute', left: '50%', top: '50%', width: LARGEUR_TEL + 36, marginLeft: -(LARGEUR_TEL + 36) / 2, marginTop: -((LARGEUR_TEL * 2676) / 1236 + 36) / 2,
      transform: `perspective(1600px) translateX(${q.s * 310 * e}px) rotateY(${-q.s * 28 * e}deg) scale(${0.72 + 0.0 * e})`, opacity: Math.min(1, e * 1.4), filter: `brightness(${0.85 + 0.15 * e})`}}>
      <Telephone src=""><Img src={staticFile(`captures/${cap(q.src)}.png`)} style={{position: 'absolute', width: '100%', height: '100%', objectFit: 'cover', objectPosition: '50% 0%'}} /></Telephone>
    </div>
  ))}</>);
};
const eventailCentre = (f: number) => !FX || f < 2150 ? 1 : 1 - 0.2 * spring({frame: f - 2150, fps: FPS, config: {damping: 16, mass: 0.8}});

/** Carte finale : poussière dorée qui monte lentement (luxe, cf. exemple de Hillel). */
const POUSSIERE = Array.from({length: 40}, (_, i) => ({x: (i * 73) % 1080, y0: 1500 + ((i * 131) % 500), v: 1.2 + ((i * 17) % 10) / 6, r: 3 + (i % 4) * 2, ph: i}));
const PoussiereDoree: React.FC<{f: number}> = ({f}) => f < 0 ? null : (
  <AbsoluteFill style={{pointerEvents: 'none'}}>
    {POUSSIERE.map((p, i) => (
      <div key={i} style={{position: 'absolute', left: p.x + Math.sin((f + p.ph * 7) / 22) * 18, top: p.y0 - f * p.v * 2.2, width: p.r, height: p.r, borderRadius: '50%',
        background: '#e3c27a', boxShadow: `0 0 ${p.r * 3}px rgba(227,194,122,.9)`, opacity: L(f, 0, 20, 0, 0.75) * (0.5 + 0.5 * Math.sin((f + p.ph * 11) / 9))}} />
    ))}
  </AbsoluteFill>
);


/* ---------- v27 : accroche « tempête de notifications » ---------- */
type Notif = {app: string; fond: string; lettre?: string; titre: string; txt: string};
const NOTIFS: Notif[] = [
  {app: 'whatsapp', fond: '#25D366', titre: 'Maman', txt: 'Tu rappelles quand ?'},
  {app: '', fond: '#2f7cf6', lettre: '@', titre: 'Bureau', txt: 'Réunion avancée à 9 h'},
  {app: 'youtube', fond: '#FF0000', titre: 'YouTube', txt: 'Nouvelle vidéo pour toi'},
  {app: '', fond: '#f2543d', lettre: '31', titre: 'Agenda', txt: 'Encore 3 dossiers à rendre'},
  {app: 'whatsapp', fond: '#25D366', titre: 'Groupe famille', txt: '47 nouveaux messages'},
  {app: 'instagram', fond: 'linear-gradient(45deg,#F58529,#DD2A7B,#8134AF)', titre: 'Instagram', txt: 'Nouvelle story'},
  {app: '', fond: '#2f7cf6', lettre: '@', titre: 'Banque', txt: 'Échéance demain'},
  {app: 'tiktok', fond: '#000', titre: 'TikTok', txt: 'Tendances du jour'},
  {app: 'whatsapp', fond: '#25D366', titre: 'David', txt: 'Tu es en retard ?'},
  {app: 'facebook', fond: '#0866FF', titre: 'Facebook', txt: '12 notifications'},
  {app: '', fond: '#f2543d', lettre: '31', titre: 'Rappel', txt: 'Rendez-vous à 18 h'},
  {app: 'snapchat', fond: '#FFFC00', titre: 'Snapchat', txt: 'Nouveau snap'},
  {app: '', fond: '#2f7cf6', lettre: '@', titre: 'Bureau', txt: 'URGENT : à valider'},
  {app: 'whatsapp', fond: '#25D366', titre: 'Groupe travail', txt: 'Tu as vu ?'},
];
// arrivées de plus en plus rapprochées (la pression monte), la dernière juste avant « les soucis »
const ARRIVEES = NOTIFS.map((_, i) => Math.round(-14 + 142 * Math.pow(i / (NOTIFS.length - 1), 0.72))); // 1re image déjà animée : deux notifications sont en train d'arriver
const STOP = 158; // fin de « les soucis… » : tout s'arrête net
const MOTS = [{de: -10, a: 40, t: 'CHAQUE JOUR.'}, {de: 40, a: 64, t: 'OCCUPÉS.'}, {de: 65, a: 90, t: 'TRAVAIL.'}, {de: 91, a: 131, t: 'RESPONSABILITÉS.'}, {de: 132, a: 170, t: 'SOUCIS…'}];
const Tempete: React.FC<{f: number}> = ({f}) => {
  const g = Math.min(f, STOP); // l'instant se fige à STOP
  const balaye = L(f, STOP + 4, STOP + 18, 0, 1, Easing.in(Easing.cubic)); // « tout effacer »
  const eteint = L(f, STOP + 14, STOP + 26, 0, 1);
  const sortie = L(f, 182, 206, 0, 1, Easing.in(Easing.cubic));
  // vibration à chaque arrivée
  const vib = ARRIVEES.reduce((a, t) => a + (g >= t && g < t + 7 ? Math.sin((g - t) * 3.1) * (1 - (g - t) / 7) : 0), 0);
  const pousse = 0.9 + 0.1 * L(g, 0, STOP, 0, 1, Easing.inOut(Easing.sin)); // reste dans la zone du sujet : ne touche ni le texte ni les sous-titres
  const tension = L(g, 60, STOP, 0, 1);
  const largeur = LARGEUR_TEL;
  return (
    <AbsoluteFill>
      {/* projecteur sur le téléphone */}
      <AbsoluteFill style={{background: `radial-gradient(ellipse 50% 38% at 50% 50%, rgba(${Math.round(90 + 120 * tension)},${Math.round(150 - 60 * tension)},${Math.round(220 - 120 * tension)},.32) 0, rgba(0,0,0,0) 70%)`, opacity: 1 - eteint * 0.7}} />
      <div style={{position: 'absolute', left: 0, right: 0, top: Z.sujetHaut, height: H, display: 'flex', alignItems: 'center', justifyContent: 'center',
        transform: `translateY(${sortie * 700}px) scale(${pousse}) rotate(${vib * 1.4}deg) translateX(${vib * 5}px)`, opacity: 1 - sortie}}>
        <Telephone src="">
          <AbsoluteFill style={{background: 'linear-gradient(180deg,#14304a 0%,#0a1726 60%,#060d16 100%)'}}>
            <div style={{position: 'absolute', top: 70, left: 0, right: 0, textAlign: 'center', color: '#e8eef5', fontFamily: 'Inter', fontWeight: 600, fontSize: 92, letterSpacing: -2}}>08:57</div>
            <div style={{position: 'absolute', top: 178, left: 0, right: 0, textAlign: 'center', color: '#b9c7d6', fontFamily: 'Inter', fontWeight: 500, fontSize: 22}}>mardi</div>
            {NOTIFS.map((n, i) => {
              const t = ARRIVEES[i];
              if (g < t) return null;
              const rang = ARRIVEES.filter((u) => u <= g && u > t).length; // combien sont arrivées après celle-ci
              if (rang > 6) return null;
              const e = spring({frame: g - t, fps: FPS, config: {damping: 14, mass: 0.6}});
              const y = 228 + rang * 76 - (1 - e) * 60;
              const fuite = Math.max(0, balaye * 900 - (6 - rang) * 40);
              return (
                <div key={i} style={{position: 'absolute', left: 14, right: 14, top: y - fuite, height: 68, borderRadius: 18, background: 'rgba(235,242,250,.16)',
                  border: '1px solid rgba(255,255,255,.12)', display: 'flex', alignItems: 'center', gap: 10, padding: '0 12px', opacity: e * (1 - rang / 8) * (1 - balaye * 0.6),
                  transform: `scale(${0.9 + 0.1 * e})`, filter: balaye > 0 ? `blur(${balaye * 6}px)` : undefined}}>
                  <div style={{width: 40, height: 40, borderRadius: 10, background: n.fond, display: 'flex', alignItems: 'center', justifyContent: 'center', flex: 'none',
                    color: '#fff', fontFamily: 'Inter', fontWeight: 800, fontSize: 18}}>
                    {n.app ? <Img src={staticFile(`logos/${n.app}.svg`)} style={{width: 24, height: 24}} /> : n.lettre}
                  </div>
                  <div style={{minWidth: 0, fontFamily: 'Inter', color: '#f1f5fa', lineHeight: 1.2}}>
                    <div style={{fontWeight: 700, fontSize: 17}}>{n.titre}</div>
                    <div style={{fontWeight: 500, fontSize: 16, opacity: 0.85, whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis'}}>{n.txt}</div>
                  </div>
                </div>
              );
            })}
            <AbsoluteFill style={{background: '#000', opacity: eteint}} />
          </AbsoluteFill>
        </Telephone>
      </div>
      {/* les mots de la voix frappent en grand, un à la fois (zone texte du haut) */}
      {MOTS.map((m) => {
        if (f < m.de || f >= m.a) return null;
        const e = spring({frame: f - m.de, fps: FPS, config: {damping: 13, mass: 0.5}});
        const x = L(f, m.a - 5, m.a, 0, 1, Easing.in(Easing.cubic));
        return (
          <div key={m.t} style={{position: 'absolute', top: Z.texteHaut, height: Z.texteBas - Z.texteHaut, left: 30, right: 30, display: 'flex', alignItems: 'center', justifyContent: 'center'}}>
            <div style={{fontFamily: 'Inter', fontWeight: 800, fontSize: m.t.length > 10 ? 72 : 104, letterSpacing: 2, color: m.t.startsWith('SOUCIS') ? '#e3c27a' : '#f2f6fa',
              opacity: Math.min(1, e * 1.5) * (1 - x), transform: `scale(${1.35 - 0.35 * e + 0.06 * x}) translateY(${-x * 20}px)`, filter: `blur(${(1 - Math.min(1, e)) * 8 + x * 8}px)`,
              textShadow: '0 0 30px rgba(120,170,255,.35)'}}>{m.t}</div>
          </div>
        );
      })}
    </AbsoluteFill>
  );
};
/** Signature de marque dès 0,5 s (sous les sous-titres, hors du sujet). */
const Signature: React.FC<{f: number}> = ({f}) => {
  if (ACCROCHE !== 'A' || f > 430) return null;
  const o = Math.min(L(f, 12, 24, 0, 1), L(f, 410, 430, 1, 0));
  return (
    <div style={{position: 'absolute', top: 1420, left: 0, right: 0, display: 'flex', justifyContent: 'center', alignItems: 'center', gap: 12, opacity: 0.85 * o}}>
      <Img src={staticFile('planche/logo.png')} style={{width: 44, height: 44, borderRadius: 11}} />
      <div style={{fontFamily: 'Inter', fontWeight: 700, fontSize: 26, letterSpacing: 4, color: NUIT ? '#e3c27a' : C.petrole}}>MY HITBODEDUT</div>
    </div>
  );
};

const SECTIONS = [
  {n: 1, texte: 'Une Hitbodedut guidée', de: T5.etapes, a: T5.duree},
  {n: 2, texte: 'Ton objectif quotidien', de: T5.duree, a: T.avec},
  {n: 3, texte: 'Je suis avec Hachem', de: T.avec, a: T.perso},
  {n: 4, texte: 'Une application à ton rythme', de: T.perso, a: T.chabbat},
  {n: 5, texte: 'Le calendrier juif', de: T.chabbat, a: T.retour},
];
/** v20 : titre de section qui s'ouvre depuis le centre, reflet doré, et s'efface en douceur sous le suivant. */
const TitreFX: React.FC<{n: number; texte: string; f: number; dur: number}> = ({n, texte, f, dur}) => {
  const s = spring({frame: f - 2, fps: FPS, config: {damping: 200}, durationInFrames: 16});
  const x = L(f, dur - 9, dur, 0, 1, Easing.in(Easing.sin)); // sort AVANT l'arrivée du suivant : jamais deux titres superposés
  const balaye = L(f, 8, 30, -30, 130, Easing.inOut(Easing.sin));
  return (
    <div style={{position: 'absolute', top: Z.texteHaut - 10, left: 0, right: 0, display: 'flex', justifyContent: 'center', opacity: (1 - x) * Math.max(0, Math.min(1, (s - 0.2) * 3)), transform: `translateY(${-x * 12}px)`, filter: `blur(${x * 5}px)`}}>
      <div style={{position: 'relative', fontFamily: 'Inter', fontWeight: 700, fontSize: 34, color: C.blanc, background: C.petrole, borderRadius: 40, padding: '8px 26px', boxShadow: `0 0 0 3px ${C.or}`,
        clipPath: `inset(-6px ${(1 - s) * 50}% -6px ${(1 - s) * 50}% round 40px)`, overflow: 'hidden'}}>
        <span style={{color: C.or}}>{n}.</span> <span style={{opacity: L(f, 4, 14, 0, 1)}}>{texte}</span>
        <div style={{position: 'absolute', inset: 0, background: `linear-gradient(110deg, transparent ${balaye - 15}%, rgba(242,210,140,.55) ${balaye}%, transparent ${balaye + 15}%)`}} />
      </div>
    </div>
  );
};
/** v20 : fond vivant — lumière chaude qui dérive lentement et quelques halos flous (profondeur, jamais devant le sujet). */
const HALOS = Array.from({length: 9}, (_, i) => ({x: (i * 137) % 100, y: (i * 61 + 13) % 100, r: 60 + ((i * 47) % 90), v: 0.4 + ((i * 29) % 10) / 12}));
const FondVivant: React.FC<{f: number}> = ({f}) => (
  <AbsoluteFill>
    <AbsoluteFill style={{background: `radial-gradient(circle at ${50 + Math.sin(f / 140) * 22}% ${35 + Math.cos(f / 170) * 12}%, rgba(255,226,170,.35) 0, rgba(255,255,255,0) 45%)`}} />
    {HALOS.map((h, i) => (
      <div key={i} style={{position: 'absolute', left: `${h.x}%`, top: `${(h.y - (f * h.v) / 30 + 200) % 110 - 5}%`, width: h.r, height: h.r, borderRadius: '50%',
        background: i % 2 ? 'rgba(20,150,140,.10)' : 'rgba(212,175,95,.13)', filter: 'blur(14px)'}} />
    ))}
  </AbsoluteFill>
);

export const PubV5: React.FC = () => {
  const f = useCurrentFrame();
  const {fps} = useVideoConfig();
  // v17 : le téléphone entre et sort en fondu enchaîné avec les poses (chevauchement FONDU_PHASE), sans flash
  const PHASES_TEL = [{de: 430, a: 600}, {de: T5.etapes, a: T.retour}, {de: 2360, a: FIN5}];
  const phase = PHASES_TEL.find((q) => f >= q.de && f < q.a + FONDU);
  const telA = phase?.de === 430;
  const telB = phase?.de === T5.etapes;
  const fe = phase ? Math.max(phase.de, Math.min(f, phase.a - 1)) : f; // l'écran se fige sur sa dernière image pendant la sortie
  const telIn = phase ? L(f, phase.de + 4, phase.de + 18, 0, 1, Easing.out(Easing.cubic)) : 0;
  const telOut = phase ? L(f, phase.a - 2, phase.a + 6, 0, 1, Easing.in(Easing.sin)) : 0;
  const zCam = camTel(fe).z;
  const fondu = Math.min(18, Math.max(0, (zCam - 1) * 200)); // bords fondus qui apparaissent progressivement avec le zoom
  const icones = f >= T.avec && f < T.perso;
  const tr = transitionSection(f);
  const rv = revele(f);
  const avale = LOGOS.reduce((acc, _, i) => acc + Math.sin(Math.PI * L(f, ASPIRE + 15 + i * 9, ASPIRE + 25 + i * 9, 0, 1, (t) => t)) * 0.012, 0);
  // v22 : la carte se construit pendant que le téléphone part (aucun écran vide), le bouton arrive sur « Commence aujourd'hui »
  const carte = spring({frame: f - FIN5 - 4, fps, config: {damping: 200}});
  const cta = spring({frame: f - FIN5 - 18, fps, config: {damping: 200}});
  const sec = spring({frame: f - FIN5 - 40, fps, config: {damping: 200}});

  return (
    <AbsoluteFill style={{background: NUIT ? '#040a12' : '#fff'}}>
      <style>{fontFaces}</style>
      <AbsoluteFill style={{background: NUIT
        ? 'radial-gradient(ellipse 90% 60% at 50% 45%, #10263b 0%, #07121f 55%, #03070d 100%), linear-gradient(180deg, transparent 70%, rgba(40,90,160,.25) 100%)'
        : 'radial-gradient(ellipse at 50% 40%, #ffffff 40%, #eef3f2 100%)'}} />
      {NUIT && <AbsoluteFill style={{background: 'linear-gradient(180deg, rgba(0,0,0,0) 72%, rgba(30,80,150,.28) 100%)'}} />}
      {FX && <FondVivant f={f} />}

      <AbsoluteFill style={{transform: `translateX(${secousse(f)}px) scale(${punch(f)})`}}>
      {POSES.map((p, i) => (
        // fondu enchaîné avec le plan suivant, sauf quand le suivant continue le même cadre (plan fixe)
        <Sequence key={i} from={p.de} durationInFrames={p.a - p.de + (POSES[i + 1]?.fixe ? 0 : POSES[i + 1]?.de === p.a ? FONDU : FONDU_PHASE)}>
          <PlanPose p={p} enchaine={POSES[i + 1]?.fixe ? 0 : POSES[i + 1]?.de === p.a ? FONDU : FONDU_PHASE} />
        </Sequence>
      ))}

      {/* v20 (demande de Hillel) : les applis ne sont plus barrées, elles sont aspirées SOUS le téléphone, l'une après l'autre */}
      {icones && LOGOS.map((l, i) => {
        const pop = spring({frame: f - T.avec - 4 - i * 3, fps, config: {damping: 11, mass: 0.6}});
        const a = L(f, ASPIRE + i * 9, ASPIRE + 18 + i * 9, 0, 1, Easing.in(Easing.cubic));
        if (a >= 1) return null;
        const x0 = i % 2 ? 880 : 90, y0 = Z.sujetHaut + 150 + Math.floor(i / 2) * 220;
        const x = x0 + (540 - 55 - x0) * a, y = y0 + (Z.sujetHaut + H / 2 - 55 - y0) * a;
        const a1 = L(f - 1, ASPIRE + i * 9, ASPIRE + 18 + i * 9, 0, 1, Easing.in(Easing.cubic));
        const vit = FX ? Math.hypot((540 - 55 - x0) * (a - a1), (Z.sujetHaut + H / 2 - 55 - y0) * (a - a1)) : 0;
        return <div key={l.n} style={{position: 'absolute', left: x, top: y, width: 110, height: 110, borderRadius: 28,
          background: l.bg, boxShadow: '0 8px 24px rgba(0,0,0,0.35)', display: 'flex', alignItems: 'center', justifyContent: 'center',
          opacity: Math.min(1, pop * 2), filter: vit > 1 ? `blur(${Math.min(6, vit * 0.08)}px)` : undefined,
          transform: `scale(${pop * (1 - 0.45 * a)}) rotate(${(i % 2 ? -1 : 1) * 25 * a}deg) scaleX(${1 + Math.min(0.35, vit * 0.004)})`}}>
          <Img src={staticFile(`logos/${l.n}.svg`)} style={{width: 64, height: 64}} />
        </div>;
      })}

      <Ondes f={f} />
      {phase && (
        <div style={{position: 'absolute', left: 0, right: 0, top: Z.sujetHaut, height: H, display: 'flex', alignItems: 'center', justifyContent: 'center', overflow: 'hidden',
          // bords haut et bas fondus : le téléphone agrandi n'est jamais coupé par une ligne dure
          WebkitMaskImage: `linear-gradient(180deg, transparent 0, #000 ${fondu * 5}px, #000 calc(100% - ${fondu * 5}px), transparent 100%)`,
          opacity: telIn * (1 - telOut), transform: `perspective(1800px) translateX(${tr.tx}px) rotateY(${tr.ry}deg) translateY(${(1 - telIn) * 90 - telOut * 40}px) scale(${(0.94 + 0.06 * telIn) * (1 - 0.04 * telOut) * (1 + avale) * tr.sc})`,
          filter: FX ? `${tr.flouX > 0.5 ? `url(#flouX) ` : ''}blur(${tr.flou}px) brightness(${tr.lum * (0.6 + 0.4 * rv)}) saturate(${0.5 + 0.5 * rv})` : undefined}}>
          {FX && <svg width="0" height="0" style={{position: 'absolute'}}><filter id="flouX" x="-50%" width="200%"><feGaussianBlur stdDeviation={`${tr.flouX} 0`} /></filter></svg>}
          {/* zoom sur le TÉLÉPHONE entier (jamais dans l'écran) : la largeur de l'écran reste toujours visible, aucun texte coupé */}
          <Eventail f={f} />
          <div style={{transform: `scale(${zCam * eventailCentre(f)})`, transformOrigin: `50% ${camTel(fe).y}%`}}>
            <Telephone src="">
              {telA && fe < 505 + FONDU && <EcranOuverture f={Math.max(0, fe - 430)} />}
              {/* logo d'ouverture → accueil : fondu, pas de coupe */}
              {FX && rv < 1 && f >= 430 && <AbsoluteFill style={{zIndex: 5, pointerEvents: 'none',
                background: `linear-gradient(100deg, rgba(255,240,215,0) ${rv * 140 - 45}%, rgba(255,240,215,.8) ${rv * 140 - 20}%, rgba(255,240,215,0) ${rv * 140 + 5}%)`}} />}
              {!(telA && fe < 505) && <AbsoluteFill style={{opacity: telA ? L(fe, 505, 505 + FONDU, 0, 1, Easing.inOut(Easing.sin)) : 1}}><EcranPlein f={fe} /></AbsoluteFill>}
            </Telephone>
          </div>
        </div>
      )}
      <Rayons f={f} de={677} a={765} />
      <Rayons f={f} de={2309} a={2360} />
      <Confettis f={f} de={2368} />
      </AbsoluteFill>
      {f >= T5.etapes && f < T5.duree && (
        <div style={{position: 'absolute', left: 0, right: 0, top: Z.sujetHaut, height: H, opacity: L(f, T5.duree - 8, T5.duree, 1, 0)}}>
          {ETAPES.map((e, i) => {
            const s = spring({frame: f - T5.etapes - 20 - i * 16, fps, config: {damping: 200}});
            const g = i % 2 === 0;
            return (
              <div key={i} style={{position: 'absolute', top: 70 + i * 145, [g ? 'left' : 'right']: 18, width: 262, opacity: s, transform: `translateX(${(1 - s) * (g ? -40 : 40)}px)`,
                background: NUIT ? 'rgba(20,40,60,.72)' : '#fff', border: `2px solid ${NUIT ? 'rgba(120,180,255,.45)' : C.turquoise}`, borderRadius: 18, padding: '10px 14px', fontFamily: 'Inter', fontWeight: 600, fontSize: 23, color: NUIT ? '#eef3f8' : C.noir,
                boxShadow: '0 6px 16px rgba(7,91,91,.12)', display: 'flex', gap: 10, alignItems: 'center'}}>
                <span style={{minWidth: 34, height: 34, borderRadius: 17, background: C.or, display: 'inline-flex', alignItems: 'center', justifyContent: 'center', fontWeight: 700}}>{i + 1}</span>{e}
              </div>
            );
          })}
        </div>
      )}

      {FX && f >= T.chabbat - 10 && f < T.chabbat + 14 && <FuiteLumiere k={tr.voile} f={f} />}

      {/* textes écran */}
      {ACCROCHE === 'A' && f < 210 && <Tempete f={f} />}
      <Sequence from={ACCROCHE === 'A' ? 196 : 0} durationInFrames={ACCROCHE === 'A' ? 104 : 300}><TexteEcran texte="Le monde ne s’arrête jamais…" debut={ACCROCHE === 'A' ? 4 : 14} fin={ACCROCHE === 'A' ? 100 : 296} sombre={!NUIT} /></Sequence>
      <Sequence from={300} durationInFrames={130}><TexteEcran texte="Et toi, prends-tu ce moment ?" debut={6} fin={126} sombre={!NUIT} /></Sequence>
      {f >= 442 && f < 600 && (
        <div style={{position: 'absolute', top: Z.texteHaut, height: Z.texteBas - Z.texteHaut, left: 0, right: 0, display: 'flex', alignItems: 'center', justifyContent: 'center',
          fontFamily: 'Inter', fontWeight: 700, fontSize: 66, letterSpacing: 6, color: NUIT ? '#e3c27a' : C.petrole, textShadow: NUIT ? '0 0 30px rgba(227,194,122,.35)' : undefined, opacity: spring({frame: f - 442, fps, config: {damping: 200}})}}>MY HITBODEDUT</div>
      )}
      <Sequence from={962} durationInFrames={93}><TexteEcran texte="Parle-Lui avec ton cœur." debut={4} fin={89} sombre={!NUIT} /></Sequence>
      {SECTIONS.map((q) => FX
        ? (f >= q.de && f < q.a && <TitreFX key={q.n} n={q.n} texte={q.texte} f={f - q.de} dur={q.a - q.de} />)
        : (f >= q.de && f < q.a && <Titre key={q.n} n={q.n} texte={q.texte} f={f - q.de} />))}
      {/* v8 : les fonctions racontées comme sa première semaine (compteur doré à la place de la 2e ligne) */}
      {JOURS.map((j) => (
        <Sequence key={j.de} from={j.de} durationInFrames={j.a - j.de}><CompteurJour texte={j.texte} dur={j.a - j.de} /></Sequence>
      ))}
      {/* v8 : écho de l'ouverture quand le plan revient */}
      <Sequence from={2206} durationInFrames={170}>{FX ? <TexteTracking texte="Le monde ne s’arrête pas. Toi, si." debut={8} fin={166} /> : <TexteEcran texte="Le monde ne s’arrête pas. Toi, si." debut={8} fin={166} taille={50} sombre />}</Sequence>

      {f >= FIN5 - 6 && (
        <AbsoluteFill style={{background: NUIT ? 'radial-gradient(ellipse 80% 55% at 50% 38%, #10263b 0%, #050c16 70%), #040a12' : '#fff', opacity: L(f, FIN5 - 6, FIN5 + 6, 0, 1)}}>
          {FX && <PoussiereDoree f={f - FIN5} />}{/* derrière le texte de la carte */}
          <div style={{position: 'absolute', top: 380, left: 70, right: 70, display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 36, transform: `scale(${L(f, FIN5, TOTAL5, 1, 1.08, (t) => t)})`}}>
            <div style={{position: 'relative', width: 280, height: 280, borderRadius: 62, overflow: 'hidden', opacity: carte, transform: `scale(${0.9 + 0.1 * carte})`, boxShadow: '0 16px 50px rgba(7,91,91,.25)'}}>
              <Img src={staticFile('planche/logo.png')} style={{width: 280, height: 280}} />
              {FX && <AbsoluteFill style={{background: `linear-gradient(115deg, transparent ${L(f, FIN5 + 22, FIN5 + 52, -60, 140) - 20}%, rgba(255,255,255,.55) ${L(f, FIN5 + 22, FIN5 + 52, -60, 140)}%, transparent ${L(f, FIN5 + 22, FIN5 + 52, -60, 140) + 20}%)`}} />}
            </div>
            <div style={{overflow: 'hidden', padding: '0 10px'}}><div style={{fontFamily: 'Inter', fontWeight: 700, fontSize: 72, letterSpacing: 6, color: NUIT ? '#e3c27a' : C.petrole, opacity: FX ? 1 : carte,
              transform: FX ? `translateY(${(1 - carte) * 100}%)` : undefined, textShadow: FX ? `0 0 ${24 * sec}px rgba(212,175,95,.45)` : undefined}}>MY HITBODEDUT</div></div>
            <div style={{fontFamily: 'Inter', fontWeight: 700, fontSize: 30, letterSpacing: 3, color: NUIT ? '#9fb8cf' : C.turquoise, textAlign: 'center', lineHeight: 1.5, opacity: carte}}>YOUR PERSONAL CONVERSATION<br />WITH HASHEM</div>
            <div style={NUIT ? {fontFamily: 'Inter', fontWeight: 700, fontStyle: 'italic', fontSize: 54, color: '#dceeff', padding: '10px 20px', opacity: cta,
              textShadow: `0 0 6px #9fd0ff, 0 0 ${18 + 6 * Math.sin(f / 7)}px #3d8bff, 0 0 42px #2a6fe0`, transform: `scale(${0.9 + 0.1 * cta})`}
              : {fontFamily: 'Inter', fontWeight: 700, fontSize: 40, color: C.noir, background: `linear-gradient(180deg,#f3dd9c,${C.or})`, borderRadius: 50, padding: '16px 46px',
              opacity: cta, transform: `scale(${(0.9 + 0.1 * cta) * (FX ? 1 + 0.025 * Math.sin(Math.max(0, f - FIN5 - 50) / 9) : 1)})`, boxShadow: `0 8px 30px ${C.or}88`}}>Commence aujourd’hui.</div>
            <div style={{fontFamily: 'FrankRuhl, serif', fontSize: 40, color: NUIT ? '#c9d6e3' : C.petrole, textAlign: 'center', lineHeight: 1.4, opacity: sec}}>MY HITBODEDUT<br />Parle à Hachem. Chaque jour.</div>
          </div>
          {/* crédit musical promis à Noam Shpayer (accord du 29/10, preuves/accord_noam_shpayer_whatsapp.png) ; hors du zoom de la carte */}
          <div style={{position: 'absolute', top: 1640, left: 0, right: 0, textAlign: 'center', fontFamily: 'Inter', fontWeight: 500, fontSize: 28, color: NUIT ? '#9fb8cf' : C.petrole,
            opacity: 0.75 * sec, letterSpacing: 0.5}}>Musique : « Beyond » — Noam Shpayer · <span style={{fontFamily: 'FrankRuhl, serif'}}>נעם שפייר</span></div>
        </AbsoluteFill>
      )}

      <Signature f={f} />
      {FX && <Texture f={f} />}
      {/* nuit : בס״ד discret en haut, comme sur l'exemple */}
      {NUIT && <div style={{position: 'absolute', top: 34, left: 0, right: 0, textAlign: 'center', fontFamily: 'FrankRuhl, serif', fontSize: 26, color: '#c9d6e3', opacity: 0.55}}>בס״ד</div>}
      {VOIX.map((v) => (
        <Sequence key={v.at} from={v.at} durationInFrames={Math.ceil(v.dur * FPS) + (v.trim !== undefined ? 8 : 14)}>
          <Sequence durationInFrames={v.coupe ?? Infinity}><Audio src={staticFile(`voix/${v.f}.wav`)} trimBefore={v.trim} /></Sequence>
          {v.txt && <SousTitres texte={v.txt} debut={0} duree={v.dur} blocs={BLOCS[v.f]} />}
        </Sequence>
      ))}
      <Sequence durationInFrames={456}><Audio src={staticFile('musique/ambiance_foule.wav')} volume={volumeAmbiance} /></Sequence>
      {/* v27 : un son par appli (synthétisé, libre de droits, évoque le style sans imiter le son de la marque) + vibration ; rien après l'arrêt net */}
      {ACCROCHE === 'A' && ARRIVEES.map((t, i) => (
        <Sequence key={`bip${i}`} from={Math.max(0, t)} durationInFrames={14}>
          <Audio src={staticFile(`sons/${NOTIFS[i].app || (NOTIFS[i].lettre === '@' ? 'mail' : 'agenda')}.wav`)} volume={0.3 + 0.12 * (i / ARRIVEES.length)} />
          <Audio src={staticFile('sons/vibre.wav')} volume={0.28} />
        </Sequence>
      ))}
      <Sequence from={DEBUT_MUSIQUE}><Audio src={staticFile(`musique/${MUSIQUE}.wav`)} volume={(f) => volumeMusique(f + DEBUT_MUSIQUE)} /></Sequence>
    </AbsoluteFill>
  );
};
