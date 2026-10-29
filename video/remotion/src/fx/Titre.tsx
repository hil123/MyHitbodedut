import React from 'react';
import {interpolate, useCurrentFrame} from 'remotion';
import {EASE, POLICE_TITRE} from '../theme';

// Une ligne qui apparaît mot à mot (opacité, montée, netteté), et sort plus vite qu'elle n'entre.
export const Titre: React.FC<{
  texte: string;
  debut: number;
  fin: number;
  taille?: number;
  y?: number;
  couleur?: string;
  lueur?: string;
  police?: string;
  italique?: boolean;
  dir?: 'ltr' | 'rtl';
  plaque?: boolean;
}> = ({
  texte,
  debut,
  fin,
  taille = 92,
  y = 50,
  couleur = '#fff',
  lueur = 'rgba(0,0,0,0.6)',
  police = POLICE_TITRE,
  italique = false,
  dir = 'ltr',
  plaque = false,
}) => {
  const f = useCurrentFrame();
  if (f < debut - 1 || f > fin) return null;
  const sortie = interpolate(f, [fin - 9, fin], [1, 0], {
    extrapolateLeft: 'clamp',
    extrapolateRight: 'clamp',
  });
  const mots = texte.split(' ');
  return (
    <div
      dir={dir}
      style={{
        position: 'absolute',
        left: '7%',
        right: '7%',
        top: `${y}%`,
        transform: 'translateY(-50%)',
        textAlign: 'center',
        fontFamily: police,
        fontSize: taille,
        fontStyle: italique ? 'italic' : 'normal',
        fontWeight: 500,
        lineHeight: 1.15,
        color: couleur,
        opacity: sortie,
        textShadow: `0 0 28px ${lueur}, 0 4px 18px rgba(0,0,0,0.55)`,
        ...(plaque
          ? {
              padding: '36px 30px',
              borderRadius: 40,
              background: 'rgba(3,6,14,0.62)',
              backdropFilter: 'blur(14px)',
            }
          : {}),
      }}
    >
      {mots.map((m, i) => {
        const a = debut + i * 4;
        const p = interpolate(f, [a, a + 16], [0, 1], {
          easing: EASE.arrivee,
          extrapolateLeft: 'clamp',
          extrapolateRight: 'clamp',
        });
        return (
          <span
            key={i}
            style={{
              display: 'inline-block',
              opacity: p,
              transform: `translateY(${(1 - p) * 34}px) scale(${0.96 + 0.04 * p})`,
              filter: `blur(${(1 - p) * 8}px)`,
              marginInline: '0.14em',
            }}
          >
            {m}
          </span>
        );
      })}
    </div>
  );
};
