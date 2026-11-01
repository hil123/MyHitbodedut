import React from 'react';
import {AbsoluteFill, Audio, Img, Sequence, Easing, interpolate, spring, staticFile, useCurrentFrame, useVideoConfig, random} from 'remotion';
import {C, Z, FPS} from './theme';
import {SousTitres, Telephone, TexteEcran, fontFaces} from './ui';

/*
 * v4 — idée de Hillel (01/11) :
 * fond blanc, le hassid marche vers l'horizon, tout est flou autour de lui → dézoom : des dizaines d'ombres noires
 * du hassid marchent autour de lui, chacune avec une bulle de stress → rezoom sur lui, sur son téléphone :
 * l'application s'ouvre (logo, présentation) → fonctions réelles → retour dans la foule, apaisé → écran final.
 * Mise en page : texte écran en haut, sujet au milieu, sous-titres en dessous. Rien ne se chevauche.
 */
const clamp = {extrapolateLeft: 'clamp' as const, extrapolateRight: 'clamp' as const};
const IO = Easing.inOut(Easing.cubic);
const H = Z.sujetBas - Z.sujetHaut; // 830
const L = (f: number, a: number, b: number, x: number, y: number, e = IO) => interpolate(f, [a, b], [x, y], {...clamp, easing: e});

/* ---------------- calendrier (images) ---------------- */
export const T = {
  monde: 0, // 0–470 : marche, dézoom, foule
  rezoom: 470, // 470–560 : plongée vers lui
  appli: 560, // 560–730 : téléphone, ouverture de l'app
  parler: 730, // onboarding réel « Parler, simplement »
  etapes: 1130,
  duree: 1330,
  avec: 1510,
  perso: 1770,
  chabbat: 1960,
  retour: 2200,
  fin: 2460,
  total: 2700,
};

/* ---------------- voix (fichiers existants) et sous-titres ---------------- */
const VOIX: {f: string; at: number; dur: number; txt: string}[] = [
  {f: 's01', at: 12, dur: 7.32, txt: 'Chaque jour, nous sommes occupés. Le travail, les responsabilités, les soucis…'},
  {f: 's02', at: 345, dur: 3.5, txt: 'Mais prenons-nous vraiment le temps de parler à Hachem ?'},
  {f: 's05', at: 590, dur: 4.32, txt: 'Pour t’accompagner dans cette démarche, découvre MY HITBODEDUT.'},
  {f: 's03', at: 735, dur: 4.99, txt: 'La Hitbodedut, c’est s’arrêter… et prendre un moment rien que pour Hachem.'},
  {f: 's04', at: 895, dur: 7.8, txt: 'Lui parler avec tes propres mots. Lui raconter tes difficultés, tes rêves, tes joies et tes remerciements.'},
  {f: 's06', at: 1140, dur: 5.81, txt: 'Cinq étapes pour t’aider à structurer ta Hitbodedut, inspirées des enseignements de nos Sages.'},
  {f: 's07', at: 1340, dur: 4.87, txt: 'Choisis la durée de ton rendez-vous quotidien et avance à ton rythme.'},
  {f: 's08', at: 1520, dur: 7.66, txt: 'Bloque les distractions. Mets ton téléphone de côté. Ce moment est consacré à Hachem.'},
  {f: 's09', at: 1785, dur: 4.63, txt: 'Personnalise ton expérience selon tes préférences et ton rythme de vie.'},
  {f: 's10', at: 1975, dur: 4.49, txt: 'Et même pour Shabbat, MY HITBODEDUT s’adapte à ton quotidien.'},
  {f: 's11', at: 2262, dur: 6.02, txt: 'Ne laisse pas passer une journée sans prendre un moment pour Hachem. Commence aujourd’hui.'},
];
const volumeMusique = (f: number) => {
  let d = 0;
  for (const v of VOIX) {
    const a = v.at, z = v.at + v.dur * FPS;
    d = Math.max(d, interpolate(f, [a - 8, a, z, z + 12], [0, 1, 1, 0], clamp));
  }
  return 0.9 - d * 0.66;
};

/* ---------------- le monde blanc et la foule ---------------- */
const BULLES = ['En retard !', 'Les factures…', 'Le travail…', 'Pas le temps', 'Et demain ?', 'La parnassa…', '47 messages', 'Encore une réunion',
  'Je suis épuisé', 'Le loyer…', 'Vite, vite !', 'Les enfants…', 'Trop de choses', 'Débordé…', 'Inquiet…', 'Le stress…'];

// Rangées (coordonnées dans la zone sujet, 1080 × 830). Une rangée = même vitesse, espacement fixe : personne ne se croise.
const RANGEES = [
  {pied: 140, h: 78, n: 12, v: 0.5, dir: 1, bulle: false, gris: 0.55, vue: 'cote'},
  {pied: 330, h: 108, n: 5, v: 1.3, dir: -1, bulle: true, gris: 0, vue: 'cote'},
  {pied: 545, h: 130, n: 5, v: 1.7, dir: 1, bulle: true, gris: 0, vue: 'cote'},
  {pied: 800, h: 165, n: 4, v: 0, dir: 1, bulle: true, gris: 0, vue: 'dos'}, // ils avancent avec lui, vers l'horizon
];
const PRINCIPAL = {x: 540, pied: 812, h: 195}; // le personnage principal, en couleur, de dos
const ANCRE = {x: PRINCIPAL.x, y: PRINCIPAL.pied - PRINCIPAL.h * 0.55};

const Bulle: React.FC<{texte: string; apparition: number; f: number; seed: number; or?: boolean}> = ({texte, apparition, f, seed, or = false}) => {
  const trem = or ? 0 : Math.sin(f / 2.3 + seed) * 1.6; // la bulle tremble : le stress
  return (
    <div style={{position: 'absolute', left: '50%', bottom: '100%', transform: `translate(calc(-50% + ${trem}px), -14px) scale(${apparition})`, transformOrigin: '50% 100%',
      opacity: apparition, whiteSpace: 'nowrap'}}>
      <div style={{fontFamily: 'Inter', fontWeight: 700, fontSize: 24, color: or ? C.noir : '#7a1f1f', background: or ? `linear-gradient(180deg,#f6e3a8,${C.or})` : '#fff',
        border: `2px solid ${or ? C.or : '#2a2a2a'}`, borderRadius: 22, padding: '6px 13px', boxShadow: or ? `0 0 30px ${C.or}` : '0 4px 10px rgba(0,0,0,.15)'}}>{texte}</div>
      <div style={{position: 'absolute', left: '46%', top: '100%', marginTop: 3, width: 10, height: 10, borderRadius: 5, background: or ? C.or : '#fff', border: `2px solid ${or ? C.or : '#2a2a2a'}`}} />
      <div style={{position: 'absolute', left: '43%', top: '100%', marginTop: 16, width: 6, height: 6, borderRadius: 3, background: or ? C.or : '#fff', border: `2px solid ${or ? C.or : '#2a2a2a'}`}} />
    </div>
  );
};

/** Le monde : sol blanc, horizon, foule d'ombres, personnage principal. zoom/flou/bulles pilotés de l'extérieur. */
const Monde: React.FC<{zoom: number; flou: number; bulles: number; apaise: number; marche: number}> = ({zoom, flou, bulles, apaise, marche}) => {
  const f = useCurrentFrame();
  return (
    <div style={{position: 'absolute', left: 0, right: 0, top: Z.sujetHaut, height: H, overflow: 'hidden'}}>
      <div style={{position: 'absolute', inset: 0, transform: `scale(${zoom})`, transformOrigin: `${ANCRE.x}px ${ANCRE.y}px`}}>
        {/* horizon : lumière douce qui grandit quand il s'apaise */}
        <AbsoluteFill style={{background: `radial-gradient(ellipse at 50% 18%, rgba(216,183,106,${0.12 + apaise * 0.4}) 0, rgba(255,255,255,0) 55%)`}} />
        <div style={{position: 'absolute', left: -200, right: -200, top: 112, height: 2, background: 'linear-gradient(90deg, transparent, #d9dede 20%, #d9dede 80%, transparent)'}} />
        <div style={{filter: `blur(${flou}px)`, opacity: 1 - apaise * 0.55}}>
          {RANGEES.map((r, ri) => Array.from({length: r.n}, (_, i) => {
            const periode = 1080 + 260;
            const pas = periode / r.n;
            let x = ((i * pas + ri * 70 + r.dir * r.v * f * marche) % periode + periode) % periode - 130;
            if (r.vue === 'dos') x = [150, 330, 750, 930][i]; // de part et d'autre du personnage principal
            const src = r.vue === 'dos' ? 'img/char_standing_back.webp' : 'img/char_walking.webp';
            const ratio = r.vue === 'dos' ? 204 / 640 : 328 / 640;
            const rebond = Math.abs(Math.sin(f / 5 + i * 1.7 + ri)) * -r.h * 0.03 * marche;
            const k = ri * 20 + i;
            const pop = r.bulle ? spring({frame: f - bulles - (k % 13) * 7, fps: FPS, config: {damping: 14, mass: 0.6}}) : 0;
            const disparait = interpolate(apaise, [0, 1], [1, 0], clamp);
            return (
              <div key={k} style={{position: 'absolute', left: x - (r.h * ratio) / 2, top: r.pied - r.h + rebond, width: r.h * ratio, height: r.h}}>
                <Img src={staticFile(src)} style={{width: '100%', height: '100%', transform: r.dir < 0 && r.vue === 'cote' ? 'scaleX(-1)' : undefined,
                  filter: `brightness(0) ${r.gris ? `invert(${r.gris})` : ''}`, opacity: r.gris ? 0.7 : 0.92}} />
                {r.bulle && <Bulle texte={BULLES[k % BULLES.length]} apparition={pop * disparait} f={f} seed={k} />}
              </div>
            );
          }))}
        </div>
        {/* le personnage principal, en couleur, de dos, qui marche vers l'horizon */}
        <div style={{position: 'absolute', left: PRINCIPAL.x - (PRINCIPAL.h * 204 / 640) / 2, top: PRINCIPAL.pied - PRINCIPAL.h + Math.abs(Math.sin(f / 5)) * -5 * marche,
          width: PRINCIPAL.h * 204 / 640, height: PRINCIPAL.h}}>
          <div style={{position: 'absolute', left: '-40%', right: '-40%', bottom: -6, height: 14, borderRadius: '50%', background: 'rgba(0,0,0,.12)', filter: 'blur(4px)'}} />
          <Img src={staticFile('img/char_standing_back.webp')} style={{width: '100%', height: '100%', position: 'relative'}} />
          {apaise > 0 && <Bulle texte="Merci, Hachem" apparition={spring({frame: f - (T.retour + 70), fps: FPS, config: {damping: 200}})} f={f} seed={0} or />}
        </div>
      </div>
    </div>
  );
};

/* ---------------- le téléphone vu par-dessus son épaule ---------------- */
const EcranOuverture: React.FC<{f: number}> = ({f}) => {
  const s = spring({frame: f - 12, fps: FPS, config: {damping: 12}});
  return (
    <AbsoluteFill style={{background: '#dff5ec', alignItems: 'center', justifyContent: 'center'}}>
      <Img src={staticFile('planche/logo.png')} style={{width: 200, height: 200, borderRadius: 44, transform: `scale(${s})`, boxShadow: '0 10px 40px rgba(7,91,91,.25)'}} />
    </AbsoluteFill>
  );
};

const AFournir: React.FC<{titre: string}> = ({titre}) => (
  <AbsoluteFill style={{background: '#10302c', alignItems: 'center', justifyContent: 'center', padding: 26, textAlign: 'center',
    fontFamily: 'Inter', color: '#ffdf8a', fontSize: 24, fontWeight: 700, lineHeight: 1.4}}>CAPTURE ANDROID À FOURNIR<br />{titre}</AbsoluteFill>
);

const Titre: React.FC<{n: number; texte: string; f: number}> = ({n, texte, f}) => {
  const s = spring({frame: f, fps: FPS, config: {damping: 200}});
  return (
    <div style={{position: 'absolute', top: Z.texteHaut - 10, left: 0, right: 0, display: 'flex', justifyContent: 'center', opacity: s, transform: `translateY(${(1 - s) * -14}px)`}}>
      <div style={{fontFamily: 'Inter', fontWeight: 700, fontSize: 34, color: C.blanc, background: C.petrole, borderRadius: 40, padding: '8px 26px', boxShadow: `0 0 0 3px ${C.or}`}}>
        <span style={{color: C.or}}>{n}.</span> {texte}
      </div>
    </div>
  );
};

const ETAPES = ['Dire ce qu’on a sur le cœur', 'Le regret du passé', 'La demande', 'Faire de l’étude une prière', 'L’action de grâce'];
const MONTAGE = [
  {src: 'captures/web_langue.png', leg: 'Langue', z: 1.5, o: '50% 92%'},
  {src: 'captures/web_onboarding_6.png', leg: 'Comment t’appeler', z: 1.5, o: '50% 62%'},
  {src: 'captures/web_couleur_fond.png', leg: 'Couleurs', z: 1.5, o: '50% 88%'},
  {src: 'captures/web_accueil_sombre.png', leg: 'Clair ou sombre', z: 1.2, o: '50% 30%'},
  {src: '', leg: 'Horaires', z: 1, o: '50% 50%'},
  {src: '', leg: 'Durée du report', z: 1, o: '50% 50%'},
];

/** Contenu de l'écran du téléphone selon le moment (captures réelles). */
const Ecran: React.FC<{f: number}> = ({f}) => {
  if (f < T.parler) return null;
  if (f < T.etapes) return <Img src={staticFile(f < 900 ? 'captures/web_onboarding_1.png' : 'captures/web_accueil.png')} style={{width: '100%', height: '100%'}} />;
  if (f < T.duree) return <AFournir titre="« Hitbodedout guidée »" />;
  if (f < T.avec) {
    const a = f < 1420;
    return <Img src={staticFile(a ? 'captures/web_choix_duree.png' : 'captures/web_seance.png')} style={{width: '100%', height: '100%',
      transform: `scale(${a ? L(f, 1340, 1420, 1, 1.6) : L(f, 1420, 1510, 1.5, 1.1)})`, transformOrigin: a ? '50% 82%' : '50% 38%'}} />;
  }
  if (f < T.perso) return f < 1630 ? <AFournir titre="« Je suis avec Hachem » : choix des applications" /> : <Img src={staticFile('captures/web_seance.png')} style={{width: '100%', height: '100%'}} />;
  if (f < T.chabbat) {
    const per = (T.chabbat - T.perso) / MONTAGE.length;
    const i = Math.min(MONTAGE.length - 1, Math.floor((f - T.perso) / per));
    const m = MONTAGE[i];
    const loc = f - T.perso - i * per;
    return m.src ? <Img src={staticFile(m.src)} style={{width: '100%', height: '100%', transform: `scale(${L(loc, 0, per, 1, m.z)})`, transformOrigin: m.o}} /> : <AFournir titre={`« ${m.leg} »`} />;
  }
  return <Img src={staticFile('captures/web_bougies.png')} style={{width: '100%', height: '100%', transform: `scale(${L(f, 2080, 2200, 1, 1.45)})`, transformOrigin: '50% 75%'}} />;
};

/* ---------------- composition ---------------- */
export const PubV4: React.FC = () => {
  const f = useCurrentFrame();
  const {fps} = useVideoConfig();

  // caméra du monde
  const zoom = f < 150 ? L(f, 0, 150, 4.2, 3.7, Easing.inOut(Easing.sin))
    : f < T.rezoom ? L(f, 150, 330, 3.7, 1.12)
    : f < T.appli ? L(f, T.rezoom, T.appli, 1.12, 7, Easing.in(Easing.cubic))
    : f < T.retour ? 7 : L(f, T.retour, T.retour + 200, 3.0, 1.6);
  const flou = f < T.retour ? L(f, 120, 300, 9, 0) : 0;
  const apaise = f < T.retour ? 0 : L(f, T.retour + 40, T.retour + 160, 0, 1);
  const marche = f < T.retour ? 1 : L(f, T.retour, T.retour + 120, 1, 0.45); // le monde ralentit
  const mondeVisible = f < T.appli + 6 || (f >= T.retour && f < T.fin + 10);
  const flash = Math.max(interpolate(f, [T.appli - 14, T.appli, T.appli + 12], [0, 1, 0], clamp), interpolate(f, [T.retour - 10, T.retour, T.retour + 12], [0, 1, 0], clamp));

  // téléphone
  const telVisible = f >= T.appli && f < T.retour;
  const entreeTel = spring({frame: f - T.appli, fps, config: {damping: 18}});
  const icones = f >= T.avec && f < T.perso;

  // écran final
  const carte = spring({frame: f - T.fin - 10, fps, config: {damping: 200}});
  const cta = spring({frame: f - T.fin - 30, fps, config: {damping: 200}});
  const sec = spring({frame: f - T.fin - 48, fps, config: {damping: 200}});

  return (
    <AbsoluteFill style={{background: '#ffffff'}}>
      <style>{fontFaces}</style>
      <AbsoluteFill style={{background: 'radial-gradient(ellipse at 50% 40%, #ffffff 40%, #eef3f2 100%)'}} />

      {mondeVisible && <Monde zoom={zoom} flou={flou} bulles={210} apaise={apaise} marche={marche} />}

      {telVisible && (
        <>
          {/* son épaule, au premier plan, floue : on regarde par-dessus */}
                    <div style={{position: 'absolute', left: 0, right: 0, top: Z.sujetHaut, height: H, display: 'flex', alignItems: 'center', justifyContent: 'center',
            transform: `translateY(${(1 - entreeTel) * 500}px)`}}>
            <Telephone src="">
              {f < T.parler ? <EcranOuverture f={f - T.appli} /> : <Ecran f={f} />}
            </Telephone>
          </div>
          {/* 5 temps (textes réels de l'app), à droite du téléphone pendant l'étape guidée */}
          {f >= T.etapes && f < T.duree && (
            <div style={{position: 'absolute', left: 0, right: 0, top: Z.sujetHaut, height: H}}>
              {ETAPES.map((e, i) => {
                const s = spring({frame: f - T.etapes - 20 - i * 16, fps, config: {damping: 200}});
                const gauche = i % 2 === 0;
                return (
                  <div key={i} style={{position: 'absolute', top: 70 + i * 145, [gauche ? 'left' : 'right']: 24, width: 300, opacity: s,
                    transform: `translateX(${(1 - s) * (gauche ? -40 : 40)}px)`, background: '#fff', border: `2px solid ${C.turquoise}`, borderRadius: 18, padding: '10px 14px',
                    fontFamily: 'Inter', fontWeight: 600, fontSize: 23, color: C.noir, boxShadow: '0 6px 16px rgba(7,91,91,.12)', display: 'flex', gap: 10, alignItems: 'center'}}>
                    <span style={{minWidth: 34, height: 34, borderRadius: 17, background: C.or, display: 'inline-flex', alignItems: 'center', justifyContent: 'center', fontWeight: 700}}>{i + 1}</span>{e}
                  </div>
                );
              })}
            </div>
          )}
          {/* icônes génériques (aucune marque) qui s'effacent pendant « Je suis avec Hachem » */}
          {icones && ['#e85d75', '#4f8cf7', '#f2b233', '#37c27a', '#9b6cf0', '#f07a3c'].map((c, i) => {
            const e = L(f, 1640 + i * 8, 1670 + i * 8, 1, 0);
            return <div key={i} style={{position: 'absolute', left: i % 2 ? 880 : 90, top: Z.sujetHaut + 150 + Math.floor(i / 2) * 220, width: 110, height: 110, borderRadius: 28,
              background: c, opacity: e, transform: `scale(${0.7 + 0.3 * e})`, filter: `blur(${(1 - e) * 10}px)`}} />;
          })}
          {/* légendes du montage des réglages */}
          {f >= T.perso && f < T.chabbat && (() => {
            const per = (T.chabbat - T.perso) / MONTAGE.length;
            const i = Math.min(MONTAGE.length - 1, Math.floor((f - T.perso) / per));
            return <div style={{position: 'absolute', top: Z.sujetHaut, height: H, left: 24, width: 300, display: 'flex', flexDirection: 'column', justifyContent: 'center', gap: 18}}>{MONTAGE.map((m, k) => <div key={k} style={{fontFamily: 'Inter', fontWeight: k === i ? 700 : 500, fontSize: k === i ? 34 : 26, color: k === i ? C.petrole : '#9fb3b1'}}>{m.leg}</div>)}</div>;
          })()}
        </>
      )}

      {/* table de Chabbat : le personnage officiel, à côté du téléphone */}
      {f >= T.chabbat && f < T.chabbat + 120 && (
        <div style={{position: 'absolute', left: 0, right: 0, top: Z.sujetHaut, height: H, background: '#fff', overflow: 'hidden'}}>
          <AbsoluteFill style={{background: `radial-gradient(circle at 70% 50%, rgba(255,190,100,${0.35 + 0.08 * Math.sin(f / 3)}) 0, rgba(255,255,255,0) 60%)`}} />
          <Img src={staticFile('img/char_shabbat.webp')} style={{position: 'absolute', width: 740, left: L(f, T.chabbat, T.chabbat + 120, 190, 150), bottom: -10}} />
        </div>
      )}

      {/* textes écran (zone haute), sombres sur fond blanc */}
      <Sequence from={0} durationInFrames={330}><TexteEcran texte="Le monde ne s’arrête jamais…" debut={20} fin={320} sombre /></Sequence>
      <Sequence from={340} durationInFrames={130}><TexteEcran texte="Et toi, prends-tu ce moment ?" debut={6} fin={125} sombre /></Sequence>
      {f >= T.appli + 30 && f < T.parler && (
        <div style={{position: 'absolute', top: Z.texteHaut, height: Z.texteBas - Z.texteHaut, left: 0, right: 0, display: 'flex', alignItems: 'center', justifyContent: 'center',
          fontFamily: 'Inter', fontWeight: 700, fontSize: 66, letterSpacing: 6, color: C.petrole, opacity: spring({frame: f - T.appli - 30, fps, config: {damping: 200}})}}>MY HITBODEDUT</div>
      )}
      <Sequence from={T.parler + 170} durationInFrames={T.etapes - T.parler - 170}><TexteEcran texte="Parle-Lui avec ton cœur." debut={4} fin={T.etapes - T.parler - 172} sombre /></Sequence>
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

      {/* écran final */}
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

      {/* flash blanc sur les deux plongées (vers le téléphone, retour dans le monde) */}
      {flash > 0 && <AbsoluteFill style={{background: '#fff', opacity: flash}} />}

      {/* sous-titres et sons */}
      {VOIX.map((v) => (
        <Sequence key={v.f} from={v.at} durationInFrames={Math.ceil(v.dur * FPS) + 14}>
          <Audio src={staticFile(`voix/${v.f}.wav`)} />
          <SousTitres texte={v.txt} debut={0} duree={v.dur} />
        </Sequence>
      ))}
      <Audio src={staticFile('musique/musique_temoin.wav')} volume={volumeMusique} />
    </AbsoluteFill>
  );
};
