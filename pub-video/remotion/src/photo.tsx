import React from 'react';
import {AbsoluteFill, Img, interpolate, staticFile, useCurrentFrame, Easing} from 'remotion';
import {Z} from './theme';

const clamp = {extrapolateLeft: 'clamp' as const, extrapolateRight: 'clamp' as const};

export type Mvt = {
  /** échelle de départ et d'arrivée (1 = l'image couvre le cadre 1080×1920) */
  s: [number, number];
  /** décalage horizontal et vertical en px, départ et arrivée */
  x?: [number, number];
  y?: [number, number];
  /** point d'ancrage de l'image dans le cadre, en % (sujet) */
  focus?: [number, number];
  easing?: (t: number) => number;
};

/** Plan photo réel : couvre tout le cadre, un seul mouvement de caméra continu, flou de filé en entrée/sortie si demandé. */
export const Plan: React.FC<{
  src: string; dur: number; m: Mvt; fileEntree?: number; fileSortie?: number; filtre?: string; children?: React.ReactNode;
}> = ({src, dur, m, fileEntree = 0, fileSortie = 0, filtre = '', children}) => {
  const f = useCurrentFrame();
  const e = m.easing ?? Easing.inOut(Easing.sin);
  const t = (a: [number, number] | undefined) => (a ? interpolate(f, [0, dur], a, {...clamp, easing: e}) : 0);
  const s = interpolate(f, [0, dur], m.s, {...clamp, easing: e});
  // filé : flou horizontal fort sur les 5 premières / dernières images, avec un glissement dans le sens du mouvement
  const fe = fileEntree ? interpolate(f, [0, 6], [1, 0], clamp) : 0;
  const fs = fileSortie ? interpolate(f, [dur - 6, dur], [0, 1], clamp) : 0;
  const glisse = fe * -fileEntree * 260 + fs * fileSortie * 260;
  const flou = (fe + fs) * 22;
  const [fx, fy] = m.focus ?? [50, 50];
  const H = Z.sujetBas - Z.sujetHaut;
  const derive = interpolate(f, [0, dur], [1.25, 1.32], clamp);
  return (
    <AbsoluteFill style={{overflow: 'hidden', background: '#000'}}>
      {/* fond : la même photo, très floue et assombrie (aucun sujet lisible derrière le texte) */}
      <AbsoluteFill style={{transform: `scale(${derive})`, filter: `blur(38px) brightness(.38) saturate(1.1) ${filtre}`}}>
        <Img src={staticFile(src)} style={{width: '100%', height: '100%', objectFit: 'cover'}} />
      </AbsoluteFill>
      {/* le plan lui-même, confiné à la zone sujet, bords fondus */}
      <div style={{position: 'absolute', left: 0, right: 0, top: Z.sujetHaut, height: H, overflow: 'hidden',
        WebkitMaskImage: 'linear-gradient(180deg, transparent 0, #000 36px, #000 calc(100% - 36px), transparent 100%)'}}>
        <div style={{position: 'absolute', inset: 0, transform: `translate(${t(m.x) + glisse}px, ${t(m.y)}px) scale(${s})`, transformOrigin: `${fx}% ${fy}%`,
          filter: `${flou ? `blur(${flou}px)` : ''} ${filtre}`, willChange: 'transform'}}>
          <Img src={staticFile(src)} style={{width: '100%', height: '100%', objectFit: 'cover', objectPosition: `${fx}% ${fy}%`}} />
          {children}
        </div>
      </div>
    </AbsoluteFill>
  );
};

/** Étalonnage commun des photos (sources mêlées) + bandes sombres derrière le texte et les sous-titres. */
export const GradePhoto: React.FC<{chaud?: number}> = ({chaud = 0.12}) => (
  <AbsoluteFill style={{pointerEvents: 'none'}}>
    <AbsoluteFill style={{background: `linear-gradient(180deg, rgba(255,170,90,${chaud}), rgba(8,124,128,${chaud * 0.6}))`, mixBlendMode: 'soft-light'}} />
    <AbsoluteFill style={{background: 'radial-gradient(ellipse at 50% 50%, rgba(0,0,0,0) 60%, rgba(7,17,17,.35) 100%)'}} />
  </AbsoluteFill>
);

/** Poussières lumineuses qui dérivent (lumière vivante sur une photo). */
export const Poussiere: React.FC<{n?: number; couleur?: string}> = ({n = 26, couleur = '255,225,170'}) => {
  const f = useCurrentFrame();
  return (
    <AbsoluteFill style={{pointerEvents: 'none', mixBlendMode: 'screen'}}>
      {Array.from({length: n}, (_, i) => {
        const r = (k: number) => (Math.sin(i * 127.1 + k * 311.7) * 43758.5453) % 1;
        const a = Math.abs(r(1)), b = Math.abs(r(2)), c = Math.abs(r(3));
        const x = (a * 1080 + f * (0.3 + c) * 0.8 + Math.sin(f / 40 + i) * 20) % 1080;
        const y = Z.sujetHaut + b * (Z.sujetBas - Z.sujetHaut) - f * (0.2 + c * 0.4);
        const sz = 3 + c * 7;
        return <div key={i} style={{position: 'absolute', left: x, top: y, width: sz, height: sz, borderRadius: sz, background: `rgba(${couleur},${0.25 + c * 0.45})`, filter: 'blur(1.5px)'}} />;
      })}
    </AbsoluteFill>
  );
};

/** Halo de soleil vivant (pas de disque dessiné : la photo porte le soleil réel). */
export const Halo: React.FC<{x: number; y: number; force?: number}> = ({x, y, force = 0.5}) => {
  const f = useCurrentFrame();
  const p = force * (0.85 + 0.15 * Math.sin(f / 18));
  return (
    <AbsoluteFill style={{pointerEvents: 'none', mixBlendMode: 'screen',
      background: `radial-gradient(circle at ${x}px ${y}px, rgba(255,235,190,${p}) 0, rgba(255,190,110,${p * 0.45}) 220px, rgba(0,0,0,0) 620px)`}} />
  );
};
