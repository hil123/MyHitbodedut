import React from 'react';
import {AbsoluteFill, Img, Sequence, Easing, staticFile, useCurrentFrame, spring, useVideoConfig} from 'remotion';
import {C, SCENES, Z} from './theme';
import {Plan, GradePhoto, Poussiere, Halo} from './photo';
import {TexteEcran, lerp} from './ui';

/* Scènes « réelles » : photos libres (Wikimedia Commons) animées, un mouvement par plan. Voir DECOUPAGE.md. */
const sc = (id: number) => SCENES[id - 1];
const P = (n: string) => `photos/${n}.jpg`;
const EASE_RAPIDE = Easing.out(Easing.cubic);
const RALENTI = Easing.out(Easing.quad);

/** Silhouette en contre-jour (forme noire, aucun visage) posée sur une photo réelle. */
const SilhouetteReelle: React.FC<{x: number; bas: number; h: number; lueur?: number}> = ({x, bas, h, lueur = 0.6}) => (
  <Img src={staticFile('img/char_open_arms.webp')} style={{position: 'absolute', left: x, top: bas - h, height: h,
    filter: `brightness(0) blur(0.6px) drop-shadow(0 0 ${h * 0.05}px rgba(255,215,150,${lueur}))`}} />
);

/* ---------- Scène 1 : le monde qui court (3 plans, filés) ---------- */
export const Scene1R: React.FC = () => (
  <AbsoluteFill>
    <Sequence durationInFrames={80}>
      <Plan src={P('haredim_jlm_5')} dur={80} fileSortie={1} m={{s: [1.0, 1.2], x: [0, -40], focus: [70, 75], easing: EASE_RAPIDE}} />
    </Sequence>
    <Sequence from={80} durationInFrames={80}>
      <Plan src={P('mea_shearim_6')} dur={80} fileEntree={1} fileSortie={1} m={{s: [1.12, 1.12], x: [-60, 60], focus: [55, 60], easing: (t) => t}} />
    </Sequence>
    <Sequence from={160} durationInFrames={80}>
      <Plan src={P('geula_3')} dur={80} fileEntree={1} m={{s: [1.05, 1.16], focus: [40, 58], easing: RALENTI}} />
    </Sequence>
    <GradePhoto />
    <TexteEcran texte={sc(1).ecran!} debut={14} fin={232} />
  </AbsoluteFill>
);

/* ---------- Scène 2 : une question (plan large puis serré, même homme) ---------- */
export const Scene2R: React.FC<{photo: string; visage: [number, number]}> = ({photo, visage}) => (
  <AbsoluteFill>
    <Sequence durationInFrames={120}>
      <Plan src={P(photo)} dur={120} m={{s: [1.15, 1.0], focus: [52, 35]}} />
    </Sequence>
    <Sequence from={120} durationInFrames={120}>
      <Plan src={P(photo)} dur={120} m={{s: [1.6, 1.9], focus: visage}} />
    </Sequence>
    <GradePhoto />
    <TexteEcran texte={sc(2).ecran!} debut={70} fin={232} />
  </AbsoluteFill>
);

/* ---------- Scène 3 : quitter le bruit (3 plans) ---------- */
export const Scene3R: React.FC<{a: string; b: string; c: string}> = ({a, b, c}) => (
  <AbsoluteFill>
    <Sequence durationInFrames={90}>
      <Plan src={P(a)} dur={90} m={{s: [1.15, 1.15], x: [-70, 70], focus: [50, 50], easing: (t) => t}} />
      <Halo x={700} y={760} force={0.35} />
    </Sequence>
    <Sequence from={90} durationInFrames={90}>
      <Plan src={P(b)} dur={90} m={{s: [1.0, 1.25], focus: [50, 62]}} />
    </Sequence>
    <Sequence from={180} durationInFrames={90}>
      <Plan src={P(c)} dur={90} m={{s: [1.3, 1.08], y: [120, -60], focus: [50, 45]}} />
    </Sequence>
    <GradePhoto chaud={0.18} />
    <Poussiere n={18} />
  </AbsoluteFill>
);

/* ---------- Scène 4 : parler à Hachem (2 plans) ---------- */
export const Scene4R: React.FC<{a: string; horizon: number}> = ({a, horizon}) => {
  const f = useCurrentFrame();
  return (
    <AbsoluteFill>
      <Sequence durationInFrames={180}>
        <Plan src={P(a)} dur={180} m={{s: [1.05, 1.14], focus: [50, 55]}}>
          <SilhouetteReelle x={430} bas={horizon} h={300} />
        </Plan>
        <Halo x={540} y={700} force={0.4} />
      </Sequence>
      <Sequence from={180} durationInFrames={180}>
        <Plan src={P('silhouette_2')} dur={180} m={{s: [1.15, 1.32], focus: [50, 30]}} />
      </Sequence>
      <GradePhoto chaud={0.15} />
      <Poussiere n={22} />
      <TexteEcran texte={sc(4).ecran!} debut={200} fin={352} />
    </AbsoluteFill>
  );
};

/* ---------- Scène 10 (plan A) : bougies de Chabbat ---------- */
export const ShabbatReel: React.FC = () => {
  const f = useCurrentFrame();
  const fl = 0.9 + 0.1 * Math.sin(f / 2.7) * Math.sin(f / 6.3);
  return (
    <AbsoluteFill>
      <Plan src={P('candles_10')} dur={105} m={{s: [1.05, 1.2], focus: [62, 45]}} filtre={`brightness(${fl})`} />
      <GradePhoto chaud={0.1} />
    </AbsoluteFill>
  );
};

/* ---------- Scène 11 (plans A et B) : lever du soleil ---------- */
export const LeverReel: React.FC<{horizon: number}> = ({horizon}) => (
  <AbsoluteFill>
    <Sequence durationInFrames={105}>
      <Plan src={P('sunrise_desert_5')} dur={105} m={{s: [1.25, 1.08], focus: [50, 45]}}>
        <SilhouetteReelle x={430} bas={horizon} h={300} lueur={0.8} />
      </Plan>
    </Sequence>
    <Sequence from={105} durationInFrames={95}>
      <Plan src={P('silhouette_2')} dur={95} m={{s: [1.02, 1.12], focus: [50, 40]}} />
    </Sequence>
    <GradePhoto chaud={0.16} />
    <Poussiere n={16} />
  </AbsoluteFill>
);
