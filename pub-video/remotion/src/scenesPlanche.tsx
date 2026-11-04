import React from 'react';
import {AbsoluteFill, Img, Sequence, Easing, interpolate, staticFile, useCurrentFrame, useVideoConfig, spring} from 'remotion';
import {C, SCENES, Z} from './theme';
import {GradePhoto, Poussiere, Halo} from './photo';
import {TexteEcran} from './ui';

/*
 * v3 — images de Hillel (planche du 01/11) et personnage officiel de l'app.
 * Chaque panneau est séparé en deux calques (décor flou / personnage détouré) : la caméra les déplace
 * à des vitesses différentes (parallaxe 2,5D), donc l'image vit sans déformer le personnage.
 * Tout reste dans la zone sujet : aucun texte ne passe sur le personnage.
 */
const sc = (id: number) => SCENES[id - 1];
const clamp = {extrapolateLeft: 'clamp' as const, extrapolateRight: 'clamp' as const};
const H = Z.sujetBas - Z.sujetHaut;
const DERIVE = Easing.inOut(Easing.sin);

type Cam = {s: [number, number]; x?: [number, number]; y?: [number, number]};
const cam = (f: number, dur: number, c: Cam, e = DERIVE) => {
  const v = (a?: [number, number]) => (a ? interpolate(f, [0, dur], a, {...clamp, easing: e}) : 0);
  return `translate(${v(c.x)}px, ${v(c.y)}px) scale(${interpolate(f, [0, dur], c.s, {...clamp, easing: e})})`;
};

/** Fond plein écran : la même image, très floue et assombrie, derrière la zone sujet. */
const FondFlou: React.FC<{src: string; teinte?: string}> = ({src, teinte = ''}) => {
  const f = useCurrentFrame();
  return (
    <AbsoluteFill style={{transform: `scale(${1.3 + f * 0.0004})`, filter: `blur(40px) brightness(.36) ${teinte}`}}>
      <Img src={staticFile(src)} style={{width: '100%', height: '100%', objectFit: 'cover'}} />
    </AbsoluteFill>
  );
};

/** Zone sujet à bords fondus. */
const Zone: React.FC<{children: React.ReactNode}> = ({children}) => (
  <div style={{position: 'absolute', left: 0, right: 0, top: Z.sujetHaut, height: H, overflow: 'hidden',
    WebkitMaskImage: 'linear-gradient(180deg, transparent 0, #000 40px, #000 calc(100% - 40px), transparent 100%)'}}>
    {children}
  </div>
);

/** Panneau en deux calques avec parallaxe. focus = point d'intérêt (%) ; fond et perso ont la même géométrie. */
const PanneauCalques: React.FC<{nom: string; dur: number; fond: Cam; perso: Cam; focus?: [number, number]}> = ({nom, dur, fond, perso, focus = [50, 40]}) => {
  const f = useCurrentFrame();
  const img = (src: string) => (
    <Img src={staticFile(src)} style={{width: '100%', height: '100%', objectFit: 'cover', objectPosition: `${focus[0]}% ${focus[1]}%`}} />
  );
  return (
    <AbsoluteFill>
      <FondFlou src={`planche/${nom}_fond.jpg`} />
      <Zone>
        <AbsoluteFill style={{transform: cam(f, dur, fond), transformOrigin: `${focus[0]}% ${focus[1]}%`}}>{img(`planche/${nom}_fond.jpg`)}</AbsoluteFill>
        <AbsoluteFill style={{transform: cam(f, dur, perso), transformOrigin: `${focus[0]}% ${focus[1]}%`}}>{img(`planche/${nom}_perso.png`)}</AbsoluteFill>
      </Zone>
    </AbsoluteFill>
  );
};

/** Personnage officiel de l'app (fond transparent) posé sur un décor de la planche. */
const PersoSurDecor: React.FC<{decor: string; perso: string; dur: number; h: number; x: [number, number]; bas: number; fond: Cam; pas?: boolean; lueur?: boolean}> = ({
  decor, perso, dur, h, x, bas, fond, pas = false, lueur = false,
}) => {
  const f = useCurrentFrame();
  const px = interpolate(f, [0, dur], x, {...clamp, easing: pas ? (t) => t : DERIVE});
  const rebond = pas ? Math.abs(Math.sin(f / 4.2)) * -6 : Math.sin(f / 30) * 3; // pas de marche ou respiration
  return (
    <AbsoluteFill>
      <FondFlou src={decor} />
      <Zone>
        <AbsoluteFill style={{transform: cam(f, dur, fond)}}>
          <Img src={staticFile(decor)} style={{width: '100%', height: '100%', objectFit: 'cover'}} />
        </AbsoluteFill>
        <Img src={staticFile(perso)} style={{position: 'absolute', height: h, left: px, top: bas - h + rebond,
          filter: lueur ? 'drop-shadow(0 0 30px rgba(255,214,150,.75))' : 'drop-shadow(0 12px 18px rgba(0,0,0,.35))'}} />
      </Zone>
    </AbsoluteFill>
  );
};

/* ---------- Scène 1 : le monde ne s'arrête jamais (p1, 2 plans) ---------- */
export const S1Planche: React.FC = () => (
  <AbsoluteFill>
    <Sequence durationInFrames={120}>
      <PanneauCalques nom="p1_rue" dur={120} focus={[50, 45]} fond={{s: [1.06, 1.12], x: [50, -50]}} perso={{s: [1.0, 1.1], x: [10, -10]}} />
    </Sequence>
    <Sequence from={120} durationInFrames={120}>
      <PanneauCalques nom="p1_rue" dur={120} focus={[55, 38]} fond={{s: [1.35, 1.45], x: [-60, 40]}} perso={{s: [1.4, 1.62]}} />
    </Sequence>
    <GradePhoto chaud={0.08} />
    <TexteEcran texte={sc(1).ecran!} debut={14} fin={232} />
  </AbsoluteFill>
);

/* ---------- Scène 2 : une question (p2, zoom lent sur le visage) ---------- */
export const S2Planche: React.FC = () => (
  <AbsoluteFill>
    <PanneauCalques nom="p2_question" dur={240} focus={[50, 40]} fond={{s: [1.05, 1.15], x: [-20, 20]}} perso={{s: [1.0, 1.32]}} />
    <GradePhoto chaud={0.08} />
    <TexteEcran texte={sc(2).ecran!} debut={70} fin={232} />
  </AbsoluteFill>
);

/* ---------- Scène 3 : quitter le bruit (marche, puis les collines) ---------- */
export const S3Planche: React.FC = () => (
  <AbsoluteFill>
    <Sequence durationInFrames={110}>
      {/* il quitte la ville : il traverse le cadre, la rue défile derrière lui */}
      <PersoSurDecor decor="planche/p1_rue_fond.jpg" perso="img/char_walking.webp" dur={110} h={640} x={[-120, 720]} bas={H - 20}
        fond={{s: [1.15, 1.15], x: [40, -60]}} pas />
    </Sequence>
    <Sequence from={110} durationInFrames={160}>
      {/* les collines au coucher du soleil : travelling arrière puis élévation */}
      <PanneauCalques nom="p3_colline" dur={160} focus={[45, 45]} fond={{s: [1.3, 1.05], y: [0, 40]}} perso={{s: [1.38, 1.0], y: [0, 70]}} />
      <Halo x={900} y={Z.sujetHaut + 300} force={0.35} />
    </Sequence>
    <GradePhoto chaud={0.14} />
    <Poussiere n={16} />
  </AbsoluteFill>
);

/* ---------- Scène 4 : parler à Hachem (3 plans) ---------- */
export const S4Planche: React.FC = () => (
  <AbsoluteFill>
    <Sequence durationInFrames={140}>
      {/* la main levée vers le ciel */}
      <PanneauCalques nom="p3_colline" dur={140} focus={[72, 35]} fond={{s: [1.15, 1.25]}} perso={{s: [1.2, 1.42], x: [0, -20]}} />
      <Halo x={880} y={Z.sujetHaut + 260} force={0.4} />
    </Sequence>
    <Sequence from={140} durationInFrames={120}>
      {/* seul sur la colline, les deux mains ouvertes */}
      <PersoSurDecor decor="planche/p3_colline_fond.jpg" perso="img/char_open_arms.webp" dur={120} h={720} x={[205, 205]} bas={H + 40}
        fond={{s: [1.1, 1.2], x: [30, -30]}} lueur />
    </Sequence>
    <Sequence from={260} durationInFrames={100}>
      {/* plus près : les mains jointes, le remerciement */}
      <PersoSurDecor decor="planche/p3_colline_fond.jpg" perso="img/char_praying.webp" dur={100} h={980} x={[200, 200]} bas={H + 220}
        fond={{s: [1.3, 1.38]}} lueur />
    </Sequence>
    <GradePhoto chaud={0.14} />
    <Poussiere n={20} />
    <TexteEcran texte={sc(4).ecran!} debut={200} fin={352} />
  </AbsoluteFill>
);

/* ---------- Scène 10 (plan A) : la table de Chabbat (personnage officiel) ---------- */
export const ShabbatPlanche: React.FC = () => {
  const f = useCurrentFrame();
  const fl = 0.88 + 0.12 * Math.sin(f / 2.7) * Math.sin(f / 6.3);
  return (
    <AbsoluteFill style={{background: '#1a0d06'}}>
      <AbsoluteFill style={{background: 'radial-gradient(circle at 60% 50%, #7a4520 0%, #2a160b 55%, #0d0705 100%)'}} />
      <Zone>
        <AbsoluteFill style={{background: `radial-gradient(circle at 72% 45%, rgba(255,190,100,${0.5 * fl}) 0, rgba(255,150,60,.15) 360px, rgba(0,0,0,0) 640px)`}} />
        <Img src={staticFile('img/char_shabbat.webp')} style={{position: 'absolute', width: 1000, left: interpolate(f, [0, 105], [60, 20], clamp), top: H - 1000 * 640 / 565 + 40,
          filter: `brightness(${0.82 + 0.12 * fl})`}} />
      </Zone>
    </AbsoluteFill>
  );
};

/* ---------- Scène 11 (plans A et B) : le message final ---------- */
export const FinalPlanche: React.FC = () => (
  <AbsoluteFill>
    <Sequence durationInFrames={110}>
      <PanneauCalques nom="p10_livre" dur={110} focus={[45, 30]} fond={{s: [1.05, 1.12]}} perso={{s: [1.0, 1.12], y: [0, 10]}} />
    </Sequence>
    <Sequence from={110} durationInFrames={80}>
      <PanneauCalques nom="p11_final" dur={80} focus={[50, 30]} fond={{s: [1.1, 1.15]}} perso={{s: [1.05, 1.18]}} />
    </Sequence>
    <GradePhoto chaud={0.1} />
    <Poussiere n={14} />
  </AbsoluteFill>
);

/** Titre numéroté doré, dans le style de la planche (scènes application). */
export const TitreNumero: React.FC<{n: number; texte: string}> = ({n, texte}) => {
  const f = useCurrentFrame();
  const {fps} = useVideoConfig();
  const s = spring({frame: f - 4, fps, config: {damping: 200}});
  return (
    <div style={{position: 'absolute', top: Z.texteHaut, left: 0, right: 0, display: 'flex', justifyContent: 'center', opacity: s, transform: `translateY(${(1 - s) * -16}px)`}}>
      <div style={{fontFamily: 'FrankRuhl, serif', fontSize: 40, color: C.blanc, background: 'rgba(7,17,17,.75)', border: `2px solid ${C.or}`,
        borderRadius: 40, padding: '6px 26px'}}>
        <span style={{color: C.or}}>{n}.</span> {texte}
      </div>
    </div>
  );
};
