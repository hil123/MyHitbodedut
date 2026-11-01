import React from 'react';
import {AbsoluteFill, Audio, Img, Sequence, Easing, interpolate, spring, staticFile, useCurrentFrame, useVideoConfig} from 'remotion';
import {C, Z, FPS} from './theme';
import {SousTitres, Telephone, TexteEcran, fontFaces} from './ui';
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
const L = (f: number, a: number, b: number, x: number, y: number, e = IO) => interpolate(f, [a, b], [x, y], {...clamp, easing: e});

/* Voix : mêmes fichiers, nouveau calage. */
const BLOCS: Record<string, {t: number; txt: string}[]> = {
  s01: [{t: 0.1, txt: 'Chaque jour, nous sommes occupés.'}, {t: 3.1, txt: 'Le travail,'}, {t: 4.18, txt: 'les responsabilités,'}, {t: 5.71, txt: 'les soucis…'}],
  s03: [{t: 0.1, txt: 'La Hitbodedut, c’est s’arrêter…'}, {t: 2.22, txt: 'et prendre un moment rien que pour Hachem.'}],
  s04: [{t: 0.1, txt: 'Lui parler avec tes propres mots.'}, {t: 2.7, txt: 'Lui raconter tes difficultés,'}, {t: 4.71, txt: 'tes rêves,'}, {t: 5.54, txt: 'tes joies et tes remerciements.'}],
  s11: [{t: 0.1, txt: 'Ne laisse pas passer une journée'}, {t: 1.45, txt: 'sans prendre un moment pour Hachem.'}, {t: 4.04, txt: 'Commence aujourd’hui.'}],
};
const VOIX = [
  {f: 's01', at: 12, dur: 7.32, txt: 'Chaque jour, nous sommes occupés. Le travail, les responsabilités, les soucis…'},
  {f: 's02', at: 300, dur: 3.5, txt: 'Mais prenons-nous vraiment le temps de parler à Hachem ?'},
  {f: 's05', at: 450, dur: 4.32, txt: 'Pour t’accompagner dans cette démarche, découvre MY HITBODEDUT.'},
  {f: 's03', at: 600, dur: 4.99, txt: 'La Hitbodedut, c’est s’arrêter… et prendre un moment rien que pour Hachem.'},
  {f: 's04', at: 765, dur: 7.8, txt: 'Lui parler avec tes propres mots. Lui raconter tes difficultés, tes rêves, tes joies et tes remerciements.'},
  {f: 's06', at: 1140, dur: 5.81, txt: 'Cinq étapes pour t’aider à structurer ta Hitbodedut, inspirées des enseignements de nos Sages.'},
  {f: 's07', at: 1340, dur: 4.87, txt: 'Choisis la durée de ton rendez-vous quotidien et avance à ton rythme.'},
  {f: 's08', at: 1520, dur: 7.66, txt: 'Bloque les distractions. Mets ton téléphone de côté. Ce moment est consacré à Hachem.'},
  {f: 's09', at: 1785, dur: 4.63, txt: 'Personnalise ton expérience selon tes préférences et ton rythme de vie.'},
  {f: 's10', at: 1975, dur: 4.49, txt: 'Et même pour Shabbat, MY HITBODEDUT s’adapte à ton quotidien.'},
  {f: 's11', at: 2262, dur: 6.02, txt: 'Ne laisse pas passer une journée sans prendre un moment pour Hachem. Commence aujourd’hui.'},
];
const volumeMusique = (f: number) => {
  let d = 0;
  for (const v of VOIX) d = Math.max(d, interpolate(f, [v.at - 8, v.at, v.at + v.dur * FPS, v.at + v.dur * FPS + 12], [0, 1, 1, 0], clamp));
  return 0.9 - d * 0.66;
};

/*
 * Une pose par morceau de phrase. de/a = images ; type 'perso' (détouré) ou 'carte' (scène avec décor).
 * Les bornes suivent la voix (découpage au prorata des caractères, comme les sous-titres).
 */
type Pose = {de: number; a: number; src: string; type: 'perso' | 'carte'; mot: string; bulle?: string; teinte: string; dir: 1 | -1; marche?: boolean};
const POSES: Pose[] = [
  // s01 « Chaque jour, nous sommes occupés. | Le travail, | les responsabilités, | les soucis… »
  {de: 0, a: 102, src: 'char_walking', type: 'perso', mot: 'occupés', bulle: 'En retard !', teinte: '#e8f1f0', dir: 1, marche: true},
  {de: 102, a: 134, src: 'char_writing', type: 'perso', mot: 'le travail', bulle: 'Encore 3 dossiers…', teinte: '#eef0f5', dir: -1},
  {de: 134, a: 180, src: 'scene_airport', type: 'carte', mot: 'les responsabilités', bulle: 'Tout repose sur moi', teinte: '#eef3f6', dir: 1},
  {de: 180, a: 300, src: 'char_thinking', type: 'perso', mot: 'les soucis', bulle: 'Et demain ?', teinte: '#f1efec', dir: -1},
  // s02 « Mais prenons-nous vraiment le temps de parler à Hachem ? »
  {de: 300, a: 430, src: 'char_face', type: 'perso', mot: 'la question', teinte: '#e6f2ef', dir: 1},
  // 430–600 : le téléphone, l'application s'ouvre (s05)
  // s03 « La Hitbodedut, c'est s'arrêter… | et prendre un moment rien que pour Hachem. »
  {de: 600, a: 664, src: 'char_debout', type: 'perso', mot: 's’arrêter', teinte: '#eaf4f1', dir: -1},
  {de: 664, a: 765, src: 'scene_nature', type: 'carte', mot: 'un moment pour Hachem', teinte: '#f6efe2', dir: 1},
  // s04 « Lui parler avec tes propres mots. | Lui raconter tes difficultés, | tes rêves, | tes joies | et tes remerciements. »
  {de: 765, a: 843, src: 'char_praise', type: 'perso', mot: 'parler', teinte: '#f6efe2', dir: -1},
  {de: 843, a: 903, src: 'char_praying', type: 'perso', mot: 'tes difficultés', teinte: '#ecf0f4', dir: 1},
  {de: 903, a: 928, src: 'char_pointing', type: 'perso', mot: 'tes rêves', teinte: '#eef0fa', dir: -1},
  {de: 928, a: 946, src: 'char_dancing_joy', type: 'perso', mot: 'tes joies', teinte: '#fbf3df', dir: 1},
  {de: 946, a: 1050, src: 'char_open_arms', type: 'perso', mot: 'tes remerciements', teinte: '#f8eedb', dir: -1},
  {de: 1050, a: 1130, src: 'char_offering', type: 'perso', mot: 'avec ton cœur', teinte: '#e8f3ef', dir: 1},
  // 1130–2200 : les fonctions (captures réelles) — 1960–2080 : table de Chabbat
  // s11 « Ne laisse pas passer une journée | sans prendre un moment pour Hachem. | Commence aujourd'hui. »
  {de: 2200, a: 2302, src: 'scene_sleeping', type: 'carte', mot: 'une journée', teinte: '#ebeef6', dir: -1},
  {de: 2302, a: 2380, src: 'scene_nature', type: 'carte', mot: 'un moment pour Hachem', teinte: '#f6efe2', dir: 1},
  {de: 2380, a: 2462, src: 'char_thumbs_up', type: 'perso', mot: 'commence', teinte: '#e8f3ef', dir: -1},
];

const RATIO: Record<string, number> = {char_walking: 328 / 640, char_writing: 637 / 640, char_thinking: 544 / 640, char_face: 465 / 640, char_debout: 800 / 1200,
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

/** Un plan « pose » : entrée glissée, poussée lente, sortie rapide ; tout reste dans la zone sujet. */
const PlanPose: React.FC<{p: Pose}> = ({p}) => {
  const f = useCurrentFrame(); // local à la séquence
  const dur = p.a - p.de;
  const entree = spring({frame: f, fps: FPS, config: {damping: 18, mass: 0.7}});
  const sortie = L(f, dur - 5, dur, 0, 1, Easing.in(Easing.cubic));
  const pousse = L(f, 0, dur, 1, 1.06, (t) => t);
  const dx = (1 - entree) * 220 * p.dir - sortie * 260 * p.dir;
  const marche = p.marche ? L(f, 0, dur, -200, 200, (t) => t) : 0;
  const rebond = p.marche ? Math.abs(Math.sin(f / 4)) * -10 : Math.sin(f / 20) * 4;
  const r = RATIO[p.src];
  // avec une bulle : le sujet tient sous la bulle (haut ≥ 220 px dans la zone), jamais dessous ni dessus
  const hMax = p.bulle ? 600 : p.type === 'perso' ? 760 : H - 80;
  let hauteur = hMax, largeur = hauteur * r;
  if (largeur > 900) { largeur = 900; hauteur = largeur / r; }
  if (p.type === 'carte' && largeur > 680) { largeur = 680; hauteur = largeur / r; }
  const decal = 0;
  return (
    <div style={{position: 'absolute', left: 0, right: 0, top: Z.sujetHaut, height: H, overflow: 'hidden', opacity: 1 - sortie * 0.6}}>
      <AbsoluteFill style={{background: `radial-gradient(circle at 50% 55%, ${p.teinte} 0, rgba(255,255,255,0) 62%)`}} />
      <div style={{position: 'absolute', left: 540 - largeur / 2 + dx + marche + decal, bottom: p.type === 'perso' || p.bulle ? 14 : (H - hauteur) / 2, width: largeur, height: hauteur,
        transform: `translateY(${rebond}px) scale(${pousse})`, transformOrigin: '50% 100%'}}>
        {p.type === 'perso' && <div style={{position: 'absolute', left: '15%', right: '15%', bottom: -8, height: 20, borderRadius: '50%', background: 'rgba(0,0,0,.10)', filter: 'blur(6px)'}} />}
        <Img src={staticFile(`img/${p.src}.webp`)} style={{width: '100%', height: '100%', position: 'relative',
          ...(p.type === 'carte' ? {borderRadius: 28, border: '10px solid #fff', boxShadow: '0 18px 50px rgba(7,40,40,.22)'} : {})}} />
      </div>
      {p.bulle && <BulleStress texte={p.bulle} f={f} cote={p.dir} />}
    </div>
  );
};

/*
 * Plans du téléphone : quelle capture, et quelle bande verticale montrer (y = % de la hauteur du téléphone).
 * z ≤ 2,2 : l'écran agrandi (402 px × 2,2 = 884 px) tient dans la largeur de l'image, donc aucune ligne n'est coupée.
 */
type PlanTel = {de: number; a: number; src: string; y: number; z0: number; z1: number; aFournir?: string};
const PLANS_TEL: PlanTel[] = [
  {de: T.etapes, a: T.duree, src: '', y: 50, z0: 1, z1: 1.15, aFournir: '« Hitbodedout guidée »'},
  {de: T.duree, a: 1420, src: 'web_choix_duree', y: 95, z0: 1.2, z1: 2.2},            // la liste des durées
  {de: 1420, a: T.avec, src: 'web_seance', y: 52, z0: 2.2, z1: 2.0},                    // « 15 min », Commencer, durées
  {de: T.avec, a: 1630, src: '', y: 50, z0: 1, z1: 1.15, aFournir: '« Je suis avec Hachem » : choix des applications'},
  {de: 1630, a: T.perso, src: 'web_seance', y: 50, z0: 1, z1: 2.2},                     // « posez le téléphone »
  {de: T.perso, a: 1802, src: 'web_langue', y: 95, z0: 1.6, z1: 2.2},                   // Langue : עברית / English / Français
  {de: 1802, a: 1834, src: 'web_onboarding_6', y: 68, z0: 1.6, z1: 2.0},                // Comment vous appeler ? Tsadik / Tsadeket
  {de: 1834, a: 1866, src: 'web_couleur_fond', y: 95, z0: 1.6, z1: 2.2},                // Couleur du fond
  {de: 1866, a: 1898, src: 'web_reglages', y: 48, z0: 1.4, z1: 1.8},                    // Apparence : clair ou sombre
  {de: 1898, a: 1929, src: '', y: 50, z0: 1, z1: 1.15, aFournir: '« Heures du mode »'},
  {de: 1929, a: T.chabbat, src: '', y: 50, z0: 1, z1: 1.15, aFournir: '« Durée du report »'},
  {de: T.chabbat, a: T.retour, src: 'web_bougies', y: 95, z0: 1.4, z1: 2.2},           // Allumage des bougies
];
const planTel = (f: number) => PLANS_TEL.find((p) => f >= p.de && f < p.a);
const camTel = (f: number) => {
  const p = planTel(f);
  if (!p) return {z: 1, y: 50};
  return {z: interpolate(f, [p.de, p.a], [p.z0, p.z1], {...clamp, easing: Easing.inOut(Easing.sin)}), y: p.y};
};
const EcranPlein: React.FC<{f: number}> = ({f}) => {
  const p = planTel(f);
  if (!p) return null;
  if (!p.src) return <AFournir titre={p.aFournir!} />;
  return <Img src={staticFile(`captures/${p.src}.png`)} style={{width: '100%', height: '100%'}} />;
};

export const PubV5: React.FC = () => {
  const f = useCurrentFrame();
  const {fps} = useVideoConfig();
  const telA = f >= 430 && f < 600; // ouverture de l'application
  const telB = f >= T.etapes && f < T.retour; // fonctions
  const telEntree = spring({frame: f - (telA ? 430 : T.etapes), fps, config: {damping: 18}});
  const icones = f >= T.avec && f < T.perso;
  const carte = spring({frame: f - T.fin - 10, fps, config: {damping: 200}});
  const cta = spring({frame: f - T.fin - 30, fps, config: {damping: 200}});
  const sec = spring({frame: f - T.fin - 48, fps, config: {damping: 200}});
  const flash = Math.max(...[430, 600, T.etapes, T.retour].map((c) => interpolate(f, [c - 8, c, c + 10], [0, 0.9, 0], clamp)));

  return (
    <AbsoluteFill style={{background: '#fff'}}>
      <style>{fontFaces}</style>
      <AbsoluteFill style={{background: 'radial-gradient(ellipse at 50% 40%, #ffffff 40%, #eef3f2 100%)'}} />

      {POSES.map((p, i) => (
        <Sequence key={i} from={p.de} durationInFrames={p.a - p.de}><PlanPose p={p} /></Sequence>
      ))}

      {(telA || telB) && (
        <div style={{position: 'absolute', left: 0, right: 0, top: Z.sujetHaut, height: H, display: 'flex', alignItems: 'center', justifyContent: 'center', overflow: 'hidden',
          transform: `translateY(${(1 - telEntree) * 500}px)`}}>
          {/* zoom sur le TÉLÉPHONE entier (jamais dans l'écran) : la largeur de l'écran reste toujours visible, aucun texte coupé */}
          <div style={{transform: `scale(${camTel(f).z})`, transformOrigin: `50% ${camTel(f).y}%`}}>
            <Telephone src="">{telA ? <EcranOuverture f={f - 430} /> : <EcranPlein f={f} />}</Telephone>
          </div>
        </div>
      )}
      {f >= T.etapes && f < T.duree && (
        <div style={{position: 'absolute', left: 0, right: 0, top: Z.sujetHaut, height: H}}>
          {ETAPES.map((e, i) => {
            const s = spring({frame: f - T.etapes - 20 - i * 16, fps, config: {damping: 200}});
            const g = i % 2 === 0;
            return (
              <div key={i} style={{position: 'absolute', top: 70 + i * 145, [g ? 'left' : 'right']: 24, width: 300, opacity: s, transform: `translateX(${(1 - s) * (g ? -40 : 40)}px)`,
                background: '#fff', border: `2px solid ${C.turquoise}`, borderRadius: 18, padding: '10px 14px', fontFamily: 'Inter', fontWeight: 600, fontSize: 23, color: C.noir,
                boxShadow: '0 6px 16px rgba(7,91,91,.12)', display: 'flex', gap: 10, alignItems: 'center'}}>
                <span style={{minWidth: 34, height: 34, borderRadius: 17, background: C.or, display: 'inline-flex', alignItems: 'center', justifyContent: 'center', fontWeight: 700}}>{i + 1}</span>{e}
              </div>
            );
          })}
        </div>
      )}
      {icones && ['#e85d75', '#4f8cf7', '#f2b233', '#37c27a', '#9b6cf0', '#f07a3c'].map((c, i) => {
        const e = L(f, 1640 + i * 8, 1670 + i * 8, 1, 0);
        return <div key={i} style={{position: 'absolute', left: i % 2 ? 880 : 90, top: Z.sujetHaut + 150 + Math.floor(i / 2) * 220, width: 110, height: 110, borderRadius: 28,
          background: c, opacity: e, transform: `scale(${0.7 + 0.3 * e})`, filter: `blur(${(1 - e) * 10}px)`}} />;
      })}
      {f >= T.chabbat && f < T.chabbat + 120 && (
        <div style={{position: 'absolute', left: 0, right: 0, top: Z.sujetHaut, height: H, background: '#fff', overflow: 'hidden'}}>
          <AbsoluteFill style={{background: `radial-gradient(circle at 70% 50%, rgba(255,190,100,${0.35 + 0.08 * Math.sin(f / 3)}) 0, rgba(255,255,255,0) 60%)`}} />
          <Img src={staticFile('img/char_shabbat.webp')} style={{position: 'absolute', width: 740, left: L(f, T.chabbat, T.chabbat + 120, 190, 150), bottom: -10}} />
        </div>
      )}

      {/* textes écran */}
      <Sequence from={0} durationInFrames={300}><TexteEcran texte="Le monde ne s’arrête jamais…" debut={14} fin={296} sombre /></Sequence>
      <Sequence from={300} durationInFrames={130}><TexteEcran texte="Et toi, prends-tu ce moment ?" debut={6} fin={126} sombre /></Sequence>
      {f >= 460 && f < 600 && (
        <div style={{position: 'absolute', top: Z.texteHaut, height: Z.texteBas - Z.texteHaut, left: 0, right: 0, display: 'flex', alignItems: 'center', justifyContent: 'center',
          fontFamily: 'Inter', fontWeight: 700, fontSize: 66, letterSpacing: 6, color: C.petrole, opacity: spring({frame: f - 460, fps, config: {damping: 200}})}}>MY HITBODEDUT</div>
      )}
      <Sequence from={962} durationInFrames={168}><TexteEcran texte="Parle-Lui avec ton cœur." debut={4} fin={164} sombre /></Sequence>
      {f >= T.etapes && f < T.duree && <Titre n={1} texte="Une Hitbodedut guidée" f={f - T.etapes} />}
      {f >= T.duree && f < T.avec && <Titre n={2} texte="Ton objectif quotidien" f={f - T.duree} />}
      {f >= T.avec && f < T.perso && <Titre n={3} texte="Je suis avec Hachem" f={f - T.avec} />}
      {f >= T.perso && f < T.chabbat && <Titre n={4} texte="Une application à ton rythme" f={f - T.perso} />}
      {f >= T.chabbat && f < T.retour && <Titre n={5} texte="Respect du Shabbat" f={f - T.chabbat} />}
      <Sequence from={T.etapes} durationInFrames={T.duree - T.etapes}><TexteEcran texte="5 étapes pour avancer." debut={10} fin={T.duree - T.etapes - 4} y={310} taille={44} sombre /></Sequence>
      <Sequence from={T.duree} durationInFrames={T.avec - T.duree}><TexteEcran texte="Chaque jour, un pas de plus." debut={10} fin={T.avec - T.duree - 4} y={310} taille={44} sombre /></Sequence>
      <Sequence from={T.avec} durationInFrames={T.perso - T.avec}><TexteEcran texte="Moins de distractions. Plus de concentration." debut={10} fin={T.perso - T.avec - 4} y={310} taille={38} sombre /></Sequence>
      <Sequence from={T.perso} durationInFrames={T.chabbat - T.perso}><TexteEcran texte="Ton application. Tes habitudes." debut={10} fin={T.chabbat - T.perso - 4} y={310} taille={44} sombre /></Sequence>
      <Sequence from={T.chabbat} durationInFrames={T.retour - T.chabbat}><TexteEcran texte="Un rythme qui respecte Shabbat." debut={10} fin={T.retour - T.chabbat - 4} y={310} taille={44} sombre /></Sequence>

      {f >= T.fin && (
        <AbsoluteFill style={{background: '#fff', opacity: L(f, T.fin, T.fin + 14, 0, 1)}}>
          <div style={{position: 'absolute', top: 380, left: 70, right: 70, display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 36, transform: `scale(${L(f, T.fin, T.total, 1, 1.12, (t) => t)})`}}>
            <Img src={staticFile('planche/logo.png')} style={{width: 280, height: 280, borderRadius: 62, opacity: carte, transform: `scale(${0.9 + 0.1 * carte})`, boxShadow: '0 16px 50px rgba(7,91,91,.25)'}} />
            <div style={{fontFamily: 'Inter', fontWeight: 700, fontSize: 72, letterSpacing: 6, color: C.petrole, opacity: carte}}>MY HITBODEDUT</div>
            <div style={{fontFamily: 'Inter', fontWeight: 700, fontSize: 30, letterSpacing: 3, color: C.turquoise, textAlign: 'center', lineHeight: 1.5, opacity: carte}}>YOUR PERSONAL CONVERSATION<br />WITH HASHEM</div>
            <div style={{fontFamily: 'Inter', fontWeight: 700, fontSize: 40, color: C.noir, background: `linear-gradient(180deg,#f3dd9c,${C.or})`, borderRadius: 50, padding: '16px 46px',
              opacity: cta, transform: `scale(${0.9 + 0.1 * cta})`, boxShadow: `0 8px 30px ${C.or}88`}}>Commence aujourd’hui.</div>
            <div style={{fontFamily: 'FrankRuhl, serif', fontSize: 40, color: C.petrole, textAlign: 'center', lineHeight: 1.4, opacity: sec}}>MY HITBODEDUT<br />Parle à Hachem. Chaque jour.</div>
          </div>
        </AbsoluteFill>
      )}

      {flash > 0 && <AbsoluteFill style={{background: '#fff', opacity: flash}} />}
      {VOIX.map((v) => (
        <Sequence key={v.f} from={v.at} durationInFrames={Math.ceil(v.dur * FPS) + 14}>
          <Audio src={staticFile(`voix/${v.f}.wav`)} />
          <SousTitres texte={v.txt} debut={0} duree={v.dur} blocs={BLOCS[v.f]} />
        </Sequence>
      ))}
      <Audio src={staticFile('musique/musique_temoin.wav')} volume={volumeMusique} />
    </AbsoluteFill>
  );
};
