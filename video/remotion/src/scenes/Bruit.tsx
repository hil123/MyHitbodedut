import {Bloom, ChromaticAberration, EffectComposer, Noise, Vignette} from '@react-three/postprocessing';
import {ThreeCanvas} from '@remotion/three';
import React from 'react';
import {AbsoluteFill, Easing, interpolate, useCurrentFrame, useVideoConfig} from 'remotion';
import * as THREE from 'three';
import {Avatar} from '../fx/Avatar';
import {Avance, Camera} from '../fx/Camera';
import {r, useCartesNotif, useHalo, useTexture} from '../fx/outils';
import {Titre} from '../fx/Titre';
import {C, POLICE_UI, T} from '../theme';

// ACTE I — le bruit. Six heures, l'écran s'allume, les notifications
// jaillissent de l'écran et finissent par recouvrir les deux visages.
const N = 150;
const DUREE = T.bruit.dur;
const SOURCES: [number, number][] = [
  [0, 1.75],
  [0, -1.75],
];

const Cartes: React.FC<{f: number}> = ({f}) => {
  const cartes = useCartesNotif(6);
  return (
    <>
      {new Array(N).fill(0).map((_, i) => {
        const s = 3 + (DUREE - 10) * Math.pow(i / N, 0.62);
        const age = f - s;
        if (age < 0) return null;
        const [ox, oy] = SOURCES[i % 2];
        const ang = r(`a${i}`) * Math.PI * 2;
        const vit = 0.6 + r(`v${i}`) * 0.9;
        const rayon = 0.35 + age * 0.05 * vit;
        const z = -0.4 + age * age * 0.0016 * vit * (1 + f / 90) + age * 0.03;
        if (z > 8.3) return null;
        const x = ox + Math.cos(ang + age * 0.02) * rayon * 1.6;
        const y = oy - 0.7 + Math.sin(ang + age * 0.02) * rayon;
        const pop = Easing.out(Easing.back(1.8))(Math.min(1, age / 8));
        const taille = (0.75 + r(`t${i}`) * 0.5) * pop;
        const rot = (r(`r${i}`) - 0.5) * 0.6 + Math.sin(age * 0.15 + i) * 0.05;
        const op = Math.min(1, age / 3) * (z > 7.6 ? 1 - (z - 7.6) / 0.7 : 1);
        return (
          <mesh key={i} position={[x, y, z]} rotation={[0, 0, rot]} scale={taille} renderOrder={3}>
            <planeGeometry args={[1.1, 0.344]} />
            <meshBasicMaterial
              map={cartes[i % cartes.length]}
              transparent
              opacity={op}
              depthWrite={false}
              toneMapped={false}
              color={new THREE.Color(1.05, 1.05, 1.08)}
            />
          </mesh>
        );
      })}
    </>
  );
};

const Scene: React.FC<{f: number; h: THREE.Texture | null; fe: THREE.Texture | null}> = ({f, h, fe}) => {
  const halo = useHalo('120,180,255');
  const p = f / DUREE;
  const monte = Easing.in(Easing.cubic)(p);
  // Lumière d'écran : elle vacille, de plus en plus vite.
  const vacille =
    0.78 + 0.22 * Math.sin(f * (0.9 + p * 1.6)) * Math.sin(f * 0.37 + 1.3);
  const b = (0.55 + 0.45 * monte) * vacille;
  const teinte = new THREE.Color(0.1 + 0.3 * b, 0.15 + 0.42 * b, 0.28 + 0.75 * b);
  const tremble = 0.045 * monte;
  const cam: [number, number, number] = [
    Math.sin(f * 2.1) * tremble,
    Math.sin(f * 1.7 + 0.5) * tremble,
    9 - 1.4 * monte,
  ];
  const ca = 0.0007 + 0.006 * Math.pow(p, 3) + (f > DUREE - 7 ? 0.02 : 0);
  return (
    <>
      <color attach="background" args={[C.nuit]} />
      <Camera position={cam} />
      <Avatar tex={h} hauteur={3.05} position={[0, 1.72, 0]} teinte={teinte} />
      <Avatar tex={fe} hauteur={3.05} position={[0, -1.72, 0]} teinte={teinte} />
      {SOURCES.map(([x, y], k) => (
        <mesh key={k} position={[x, y - 1.2, 0.6]} renderOrder={1}>
          <planeGeometry args={[4.2, 2.6]} />
          <meshBasicMaterial
            map={halo}
            transparent
            opacity={0.55 * vacille * (0.6 + 0.4 * monte)}
            blending={THREE.AdditiveBlending}
            depthWrite={false}
            toneMapped={false}
          />
        </mesh>
      ))}
      {/* ligne de partage entre les deux vies */}
      <mesh position={[0, 0, 0.05]}>
        <planeGeometry args={[6, 0.012]} />
        <meshBasicMaterial color={new THREE.Color(0.5, 0.7, 1.4)} transparent opacity={0.5} toneMapped={false} />
      </mesh>
      <Cartes f={f} />
      <EffectComposer multisampling={0} frameBufferType={THREE.UnsignedByteType}>
        <Bloom intensity={0.55 + 1.1 * monte} luminanceThreshold={0.62} luminanceSmoothing={0.2} mipmapBlur />
        <ChromaticAberration offset={new THREE.Vector2(ca, ca * 0.6)} radialModulation={false} modulationOffset={0} />
        <Noise opacity={0.07} />
        <Vignette darkness={0.78} offset={0.22} />
      </EffectComposer>
      <Avance f={f} />
    </>
  );
};

export const Bruit: React.FC = () => {
  const f = useCurrentFrame();
  const h = useTexture('avatars/char_thinking.png');
  const fe = useTexture('avatars/char_f_thinking.png');
  const {width, height} = useVideoConfig();
  const heure = interpolate(f, [0, 6, 34, 42], [0, 1, 1, 0], {
    extrapolateLeft: 'clamp',
    extrapolateRight: 'clamp',
  });
  return (
    <AbsoluteFill style={{background: C.nuit}}>
      <ThreeCanvas
        width={width}
        height={height}
        camera={{fov: 40, position: [0, 0, 9], near: 0.1, far: 100}}
        gl={{antialias: false, preserveDrawingBuffer: true}}
      >
        <Scene f={f} h={h} fe={fe} />
      </ThreeCanvas>
      <div
        style={{
          position: 'absolute',
          top: '50%',
          width: '100%',
          textAlign: 'center',
          fontFamily: POLICE_UI,
          fontSize: 190,
          color: '#EAF3FF',
          letterSpacing: 4,
          opacity: heure,
          transform: `translateY(-50%) scale(${1.08 - 0.08 * heure})`,
          background: 'rgba(3,6,14,0.55)',
          padding: '20px 0',
          textShadow: `0 0 40px ${C.bleuEcran}, 0 0 90px rgba(90,168,255,0.6)`,
        }}
      >
        6:02
      </div>
      <Titre texte="Tout le monde te parle." debut={58} fin={DUREE} taille={98} y={50} lueur="rgba(90,168,255,0.85)" plaque />
    </AbsoluteFill>
  );
};
