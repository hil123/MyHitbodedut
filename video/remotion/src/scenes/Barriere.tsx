import React from 'react';
import {AbsoluteFill, Img, interpolate, spring, staticFile, useCurrentFrame, useVideoConfig} from 'remotion';
import {C, EASE, POLICE_TITRE, POLICE_UI, T} from '../theme';

// ACTE II — l'arrêt. Coupe nette : le silence, l'écran de barrière.
// Ses dernières images s'emplissent de lumière : le rectangle devient la porte.
export const CARTE = {w: 760, h: 1260, rayon: 64};

export const Barriere: React.FC = () => {
  const f = useCurrentFrame();
  const {fps} = useVideoConfig();
  const d = T.barriere.dur;
  const pose = spring({frame: f, fps, config: {damping: 200}, durationInFrames: 12});
  const lumiere = interpolate(f, [d - 14, d], [0, 1], {
    easing: EASE.depart,
    extrapolateLeft: 'clamp',
    extrapolateRight: 'clamp',
  });
  const respire = 1 + 0.004 * Math.sin(f / 6);
  const bouton = (texte: string, plein: boolean, i: number) => {
    const p = spring({frame: f - 6 - i * 3, fps, config: {damping: 200}});
    return (
      <div
        style={{
          marginTop: 26,
          padding: '30px 0',
          borderRadius: 999,
          fontFamily: POLICE_UI,
          fontSize: 44,
          textAlign: 'center',
          color: plein ? C.vert : C.creme,
          background: plein ? C.or : 'rgba(255,246,230,0.08)',
          border: plein ? 'none' : '2px solid rgba(255,246,230,0.35)',
          opacity: p,
          transform: `translateY(${(1 - p) * 30}px)`,
        }}
      >
        {texte}
      </div>
    );
  };
  return (
    <AbsoluteFill
      style={{
        background: `radial-gradient(ellipse at 50% 45%, ${C.vertClair} 0%, ${C.vert} 45%, #0B1813 100%)`,
        alignItems: 'center',
        justifyContent: 'center',
      }}
    >
      <div
        style={{
          width: CARTE.w,
          height: CARTE.h,
          borderRadius: CARTE.rayon,
          background: 'linear-gradient(180deg, #24493B 0%, #1A3329 100%)',
          boxShadow: `0 0 ${80 + 160 * lumiere}px rgba(244,199,122,${0.25 + 0.7 * lumiere}), inset 0 0 0 2px rgba(255,246,230,0.12)`,
          transform: `scale(${(1.05 - 0.05 * pose) * respire})`,
          position: 'relative',
          overflow: 'hidden',
          padding: '120px 70px',
          boxSizing: 'border-box',
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'stretch',
        }}
      >
        <Img
          src={staticFile('avatars/ic_launcher_face.png')}
          style={{width: 230, height: 230, borderRadius: '50%', alignSelf: 'center', background: C.creme}}
        />
        <div
          style={{
            marginTop: 70,
            fontFamily: POLICE_TITRE,
            fontSize: 88,
            lineHeight: 1.12,
            textAlign: 'center',
            color: C.creme,
          }}
        >
          Tu as fait ta hitbodedout&nbsp;?
        </div>
        <div style={{flex: 1}} />
        {bouton('Commencer maintenant', true, 0)}
        {bouton("C'est fait", false, 1)}
        {bouton('Dans 5 minutes', false, 2)}
        {/* la lumière qui monte du fond de l'écran */}
        <div
          style={{
            position: 'absolute',
            inset: 0,
            background: `radial-gradient(ellipse at 50% 55%, rgba(255,250,235,1) 0%, rgba(255,222,160,0.95) 45%, rgba(244,199,122,0.85) 100%)`,
            opacity: lumiere,
          }}
        />
      </div>
    </AbsoluteFill>
  );
};
