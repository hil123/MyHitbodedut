import {Bloom, EffectComposer, GodRays, Noise, Vignette} from '@react-three/postprocessing';
import {ThreeCanvas} from '@remotion/three';
import React, {useMemo, useState} from 'react';
import {AbsoluteFill, interpolate, useCurrentFrame, useVideoConfig} from 'remotion';
import * as THREE from 'three';
import {Avance, Camera} from '../fx/Camera';
import {lisse, r, useCartesNotif, useHalo} from '../fx/outils';
import {Titre} from '../fx/Titre';
import {EASE, T} from '../theme';

// ACTE II → III — la porte. L'écran de barrière est devenu une porte de lumière.
// Les notifications qui flottaient dans le noir montent et deviennent des étoiles ;
// puis la caméra traverse la porte.
const PORTE = {w: 2.2, h: 3.65};
const Z_DEPART = 7.64; // la porte remplit le cadre comme la carte de la barrière
const NB = 46;
const NB_POUSSIERE = 420;

const camZ = (f: number) => {
  if (f < 36) return interpolate(f, [0, 36], [Z_DEPART, 11], {easing: EASE.arrivee});
  if (f < 84) return interpolate(f, [36, 84], [11, 10.2], {easing: EASE.derive});
  return interpolate(f, [84, 120], [10.2, 0.25], {easing: EASE.depart, extrapolateRight: 'clamp'});
};

const Poussiere: React.FC<{f: number; halo: THREE.Texture}> = ({f, halo}) => {
  const geo = useMemo(() => {
    const pos = new Float32Array(NB_POUSSIERE * 3);
    const col = new Float32Array(NB_POUSSIERE * 3);
    for (let i = 0; i < NB_POUSSIERE; i++) {
      const x = (r(`px${i}`) - 0.5) * 6 + Math.sin(f * 0.02 + i) * 0.08;
      const y = ((r(`py${i}`) * 5 + f * 0.004 * (0.5 + r(`pv${i}`))) % 5) - 1.8;
      const z = r(`pz${i}`) * 10;
      pos.set([x, y, z], i * 3);
      // éclairée seulement dans le faisceau de la porte
      const dans = Math.max(0, 1 - Math.max(Math.abs(x) - PORTE.w / 2, 0) * 1.5) *
        Math.max(0, 1 - Math.max(Math.abs(y - 0.0) - PORTE.h / 2, 0) * 1.5);
      const sc = 0.35 + 0.65 * Math.abs(Math.sin(f * 0.07 + i * 1.7));
      const v = (0.08 + 1.6 * dans) * sc;
      col.set([v * 1.0, v * 0.82, v * 0.55], i * 3);
    }
    const g = new THREE.BufferGeometry();
    g.setAttribute('position', new THREE.BufferAttribute(pos, 3));
    g.setAttribute('color', new THREE.BufferAttribute(col, 3));
    return g;
  }, [f]);
  return (
    <points geometry={geo}>
      <pointsMaterial
        map={halo}
        size={0.07}
        vertexColors
        transparent
        depthWrite={false}
        blending={THREE.AdditiveBlending}
        toneMapped={false}
      />
    </points>
  );
};

const Scene: React.FC<{f: number}> = ({f}) => {
  const [soleil, setSoleil] = useState<THREE.Mesh | null>(null);
  const cartes = useCartesNotif(6);
  const halo = useHalo('255,236,200');
  const z = camZ(f);
  const traverse = lisse(f, 90, 120);
  const intens = 1.5 + 3 * traverse;
  return (
    <>
      <color attach="background" args={['#030806']} />
      <fog attach="fog" args={['#030806', 6, 22]} />
      <Camera position={[0, 0.05 * Math.sin(f / 30), z]} cible={[0, 0, -1]} />
      {/* la porte de lumière */}
      <mesh ref={setSoleil} position={[0, 0, 0]}>
        <planeGeometry args={[PORTE.w, PORTE.h]} />
        <meshBasicMaterial color={new THREE.Color(intens, intens * 0.86, intens * 0.62)} toneMapped={false} />
      </mesh>
      {/* l'encadrement */}
      {[
        [0, PORTE.h / 2 + 0.06, PORTE.w + 0.24, 0.12],
        [0, -PORTE.h / 2 - 0.06, PORTE.w + 0.24, 0.12],
        [PORTE.w / 2 + 0.06, 0, 0.12, PORTE.h],
        [-PORTE.w / 2 - 0.06, 0, 0.12, PORTE.h],
      ].map(([x, y, w, h], i) => (
        <mesh key={i} position={[x, y, 0.02]}>
          <boxGeometry args={[w, h, 0.18]} />
          <meshBasicMaterial color="#0b0f0c" />
        </mesh>
      ))}
      {/* la lumière qui se répand sur le sol */}
      <mesh position={[0, -PORTE.h / 2 + 0.01, 2.4]} rotation={[-Math.PI / 2, 0, 0]}>
        <planeGeometry args={[4.5, 6]} />
        <meshBasicMaterial
          map={halo}
          transparent
          opacity={0.3}
          blending={THREE.AdditiveBlending}
          depthWrite={false}
          toneMapped={false}
        />
      </mesh>
      <Poussiere f={f} halo={halo} />
      {/* les notifications deviennent des étoiles */}
      {new Array(NB).fill(0).map((_, i) => {
        const p = lisse(f, 14 + i * 0.9, 40 + i * 0.9);
        const x = (r(`bx${i}`) - 0.5) * 5.5;
        const z0 = 1 + r(`bz${i}`) * 5;
        const y = -1.6 + r(`by${i}`) * 3 + f * (0.01 + 0.03 * p) * (0.6 + r(`bv${i}`));
        const scintille = 0.6 + 0.4 * Math.sin(f * 0.35 + i * 2.1);
        return (
          <group key={i} position={[x, y, z0]}>
            <mesh rotation={[0, 0, (r(`br${i}`) - 0.5) * 0.5]} scale={(1 - p) * 0.55 + 0.001}>
              <planeGeometry args={[1.1, 0.344]} />
              <meshBasicMaterial
                map={cartes[i % cartes.length]}
                transparent
                opacity={(1 - p) * 0.55}
                depthWrite={false}
                color={new THREE.Color(0.75, 0.75, 0.8)}
              />
            </mesh>
            <mesh scale={0.08 + 0.32 * p * scintille}>
              <planeGeometry args={[1, 1]} />
              <meshBasicMaterial
                map={halo}
                transparent
                opacity={p}
                blending={THREE.AdditiveBlending}
                depthWrite={false}
                toneMapped={false}
                color={new THREE.Color(2.2, 2.0, 1.7)}
              />
            </mesh>
          </group>
        );
      })}
      <EffectComposer multisampling={0} frameBufferType={THREE.UnsignedByteType}>
        {soleil ? (
          <GodRays sun={soleil} samples={70} density={0.97} decay={0.94} weight={0.3} exposure={0.18} clampMax={1} blur />
        ) : (
          <></>
        )}
        <Bloom intensity={0.45 + 1.6 * traverse} luminanceThreshold={0.86} luminanceSmoothing={0.2} mipmapBlur />
        <Noise opacity={0.06} />
        <Vignette darkness={0.7} offset={0.25} />
      </EffectComposer>
      <Avance f={f} />
    </>
  );
};

export const Portail: React.FC = () => {
  const f = useCurrentFrame();
  const {width, height} = useVideoConfig();
  const blanc = interpolate(f, [106, 120], [0, 1], {
    extrapolateLeft: 'clamp',
    extrapolateRight: 'clamp',
  });
  const entree = interpolate(f, [0, 5], [1, 0], {extrapolateRight: 'clamp'});
  return (
    <AbsoluteFill style={{background: '#030806'}}>
      <ThreeCanvas
        width={width}
        height={height}
        camera={{fov: 40, position: [0, 0, Z_DEPART], near: 0.05, far: 100}}
        gl={{antialias: false, preserveDrawingBuffer: true}}
      >
        <Scene f={f} />
      </ThreeCanvas>
      <Titre texte="Avant d'ouvrir le monde…" debut={16} fin={60} taille={86} y={16} lueur="rgba(244,199,122,0.7)" />
      <Titre texte="…ouvre ton cœur." debut={60} fin={108} taille={96} y={16} lueur="rgba(244,199,122,0.9)" />
      {/* raccord avec la carte de la barrière, puis traversée dans la lumière */}
      <AbsoluteFill style={{background: '#FFE7B8', opacity: Math.max(entree * 0.6, 0)}} />
      <AbsoluteFill style={{background: '#FFF6E6', opacity: blanc}} />
    </AbsoluteFill>
  );
};
