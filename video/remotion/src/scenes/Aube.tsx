import {Bloom, EffectComposer, GodRays, Noise, Vignette} from '@react-three/postprocessing';
import {ThreeCanvas} from '@remotion/three';
import React, {useMemo, useState} from 'react';
import {AbsoluteFill, Img, interpolate, spring, staticFile, useCurrentFrame, useVideoConfig} from 'remotion';
import * as THREE from 'three';
import {Avatar} from '../fx/Avatar';
import {Avance, Camera} from '../fx/Camera';
import {lisse, r, useHalo, useTexture} from '../fx/outils';
import {Titre} from '../fx/Titre';
import {C, EASE, POLICE_TITRE, POLICE_UI, T} from '../theme';

// ACTE III — la rencontre. Deux collines, un seul ciel. L'homme et la femme,
// chacun seul, bras levés vers le soleil qui se lève.
const D = T.aube.dur;
const HORIZON = -2.2;

const ciel = {
  vertexShader: /* glsl */ `
    varying vec3 vP;
    void main() {
      vP = (modelMatrix * vec4(position, 1.0)).xyz;
      gl_Position = projectionMatrix * viewMatrix * vec4(vP, 1.0);
    }`,
  fragmentShader: /* glsl */ `
    varying vec3 vP;
    uniform vec3 uSoleil;
    uniform float uT;
    uniform float uJour;
    float hash(vec2 p) { return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453); }
    void main() {
      float h = (vP.y - ${HORIZON.toFixed(1)}) / 26.0;
      vec3 bas = mix(vec3(0.95, 0.42, 0.22), vec3(1.0, 0.62, 0.32), uJour);
      vec3 milieu = mix(vec3(0.36, 0.18, 0.36), vec3(0.62, 0.38, 0.46), uJour);
      vec3 haut = mix(vec3(0.03, 0.04, 0.13), vec3(0.12, 0.16, 0.34), uJour);
      vec3 col = mix(bas, milieu, smoothstep(0.0, 0.28, h));
      col = mix(col, haut, smoothstep(0.22, 0.85, h));
      float d = distance(vP.xy, uSoleil.xy);
      col += vec3(1.0, 0.72, 0.38) * exp(-d * 0.55) * (0.7 + 0.6 * uJour);
      col += vec3(1.0, 0.5, 0.25) * exp(-d * 0.12) * 0.25;
      vec2 q = vP.xy * 7.0;
      vec2 g = floor(q);
      float etoile = step(0.988, hash(g)) * smoothstep(0.32, 0.0, distance(fract(q), vec2(0.5)));
      float sc = 0.5 + 0.5 * sin(uT * 0.25 + hash(g + 1.7) * 40.0);
      col += vec3(etoile * sc) * smoothstep(0.35, 0.8, h) * (1.0 - uJour) * 1.4;
      gl_FragColor = vec4(col, 1.0);
    }`,
};

const colline = (fn: (x: number) => number) => {
  const s = new THREE.Shape();
  s.moveTo(-40, -30);
  for (let x = -40; x <= 40; x += 0.2) s.lineTo(x, fn(x));
  s.lineTo(40, -30);
  s.lineTo(-40, -30);
  return new THREE.ShapeGeometry(s);
};

const Collines: React.FC = () => {
  const geos = useMemo(
    () => [
      {
        g: colline((x) => HORIZON + 1.6 + 0.9 * Math.sin(x * 0.18 + 1) + 0.5 * Math.sin(x * 0.47 + 2) + 0.15 * Math.sin(x * 1.3)),
        z: -22,
        c: '#4a3550',
      },
      {
        g: colline((x) => HORIZON + 0.4 + 0.7 * Math.sin(x * 0.33 + 4) + 0.3 * Math.sin(x * 0.9 + 1) + 0.08 * Math.sin(x * 3.1)),
        z: -11,
        c: '#2a1f35',
      },
      {
        g: colline(
          (x) =>
            -3.1 +
            1.55 * Math.exp(-(((x + 1.15) / 0.95) ** 2)) +
            1.35 * Math.exp(-(((x - 1.25) / 1.0) ** 2)) +
            0.05 * Math.sin(x * 6.3) +
            0.03 * Math.sin(x * 13.7),
        ),
        z: -2.4,
        c: '#120e1a',
      },
    ],
    [],
  );
  return (
    <>
      {geos.map(({g, z, c}, i) => (
        <mesh key={i} geometry={g} position={[0, 0, z]}>
          <meshBasicMaterial color={c} fog={i < 2} />
        </mesh>
      ))}
    </>
  );
};

const Poussiere: React.FC<{f: number; halo: THREE.Texture}> = ({f, halo}) => {
  const geo = useMemo(() => {
    const n = 380;
    const pos = new Float32Array(n * 3);
    const col = new Float32Array(n * 3);
    for (let i = 0; i < n; i++) {
      const x = (r(`ax${i}`) - 0.5) * 7 + Math.sin(f * 0.015 + i) * 0.15 + f * 0.003;
      const y = -2.2 + r(`ay${i}`) * 5 + Math.sin(f * 0.02 + i * 0.7) * 0.1 + f * 0.002;
      const z = -1 + r(`az${i}`) * 8;
      pos.set([x, y, z], i * 3);
      const v = (0.25 + 0.75 * Math.abs(Math.sin(f * 0.06 + i * 2.3))) * (0.5 + r(`aw${i}`));
      col.set([v * 1.0, v * 0.78, v * 0.45], i * 3);
    }
    const g = new THREE.BufferGeometry();
    g.setAttribute('position', new THREE.BufferAttribute(pos, 3));
    g.setAttribute('color', new THREE.BufferAttribute(col, 3));
    return g;
  }, [f]);
  return (
    <points geometry={geo}>
      <pointsMaterial map={halo} size={0.05} vertexColors transparent depthWrite={false} blending={THREE.AdditiveBlending} toneMapped={false} />
    </points>
  );
};

const Scene: React.FC<{f: number; h: THREE.Texture | null; fe: THREE.Texture | null}> = ({f, h, fe}) => {
  const [soleil, setSoleil] = useState<THREE.Mesh | null>(null);
  const halo = useHalo('255,214,150');
  const brume = useHalo('255,190,200');
  const p = f / D;
  const jour = lisse(f, 0, D);
  const soleilY = interpolate(f, [0, D], [HORIZON - 1.4, HORIZON + 2.2], {easing: EASE.derive});
  const uniforms = useMemo(
    () => ({uSoleil: {value: new THREE.Vector3()}, uT: {value: 0}, uJour: {value: 0}}),
    [],
  );
  uniforms.uSoleil.value.set(0, soleilY, -30);
  uniforms.uT.value = f;
  uniforms.uJour.value = jour;
  const camY = interpolate(f, [0, D], [-0.55, 0.35], {easing: EASE.derive});
  const camZ = interpolate(f, [0, D], [9.2, 11.2], {easing: EASE.derive});
  // contre-jour : silhouettes chaudes, qui s'éclairent un peu avec le jour
  const contre = new THREE.Color(0.3 + 0.28 * jour, 0.23 + 0.2 * jour, 0.24 + 0.12 * jour);
  const leve = (debut: number) => interpolate(f, [debut, debut + 24], [0.96, 1], {easing: EASE.arrivee, extrapolateLeft: 'clamp', extrapolateRight: 'clamp'});
  return (
    <>
      <color attach="background" args={['#120c18']} />
      <fog attach="fog" args={['#b0707a', 10, 46]} />
      <Camera position={[0.15 * Math.sin(p * 2), camY, camZ]} cible={[0, -0.6, -6]} />
      <mesh position={[0, 8, -32]}>
        <planeGeometry args={[90, 60]} />
        <shaderMaterial args={[{...ciel, uniforms}]} />
      </mesh>
      <mesh ref={setSoleil} position={[0, soleilY, -30]}>
        <circleGeometry args={[0.9, 48]} />
        <meshBasicMaterial color={new THREE.Color(3.2, 2.5, 1.6)} toneMapped={false} />
      </mesh>
      {[-6, -14].map((z, i) => (
        <mesh key={i} position={[((f * 0.012 * (i + 1)) % 4) - 2, HORIZON + 0.4 + i * 0.6, z]}>
          <planeGeometry args={[26, 3.2]} />
          <meshBasicMaterial map={brume} transparent opacity={0.22} blending={THREE.AdditiveBlending} depthWrite={false} />
        </mesh>
      ))}
      <Collines />
      {/* halos derrière les personnages : le soleil les découpe */}
      {[
        [-1.15, -0.95],
        [1.25, -1.12],
      ].map(([x, y], i) => (
        <mesh key={i} position={[x, y, -2.15]}>
          <planeGeometry args={[2.4, 2.4]} />
          <meshBasicMaterial map={halo} transparent opacity={0.45 + 0.25 * jour} blending={THREE.AdditiveBlending} depthWrite={false} toneMapped={false} />
        </mesh>
      ))}
      <Avatar tex={h} hauteur={1.32 * leve(10)} position={[-1.15, -1.0, -2.0]} teinte={contre} />
      <Avatar tex={fe} hauteur={1.28 * leve(18)} position={[1.25, -1.16, -2.0]} teinte={contre} />
      <Poussiere f={f} halo={halo} />
      <EffectComposer multisampling={0} frameBufferType={THREE.UnsignedByteType}>
        {soleil ? (
          <GodRays sun={soleil} samples={70} density={0.97} decay={0.95} weight={0.6} exposure={0.5} clampMax={1} blur />
        ) : (
          <></>
        )}
        <Bloom intensity={1.1} luminanceThreshold={0.72} luminanceSmoothing={0.25} mipmapBlur />
        <Noise opacity={0.05} />
        <Vignette darkness={0.6} offset={0.3} />
      </EffectComposer>
      <Avance f={f} />
    </>
  );
};

export const Aube: React.FC = () => {
  const f = useCurrentFrame();
  const h = useTexture('avatars/char_open_arms.png');
  const fe = useTexture('avatars/char_f_arms_up.png');
  const {width, height, fps} = useVideoConfig();
  const blanc = interpolate(f, [0, 20], [1, 0], {easing: EASE.arrivee, extrapolateRight: 'clamp'});
  const logo = spring({frame: f - 112, fps, config: {damping: 200}, durationInFrames: 24});
  const appel = spring({frame: f - 140, fps, config: {damping: 200}});
  return (
    <AbsoluteFill style={{background: '#120c18'}}>
      <ThreeCanvas
        width={width}
        height={height}
        camera={{fov: 40, position: [0, 0, 10], near: 0.1, far: 200}}
        gl={{antialias: false, preserveDrawingBuffer: true}}
      >
        <Scene f={f} h={h} fe={fe} />
      </ThreeCanvas>
      <Titre texte="Une heure." debut={26} fin={108} taille={120} y={15} lueur="rgba(255,190,120,0.9)" />
      <Titre texte="Toi et Lui." debut={44} fin={108} taille={120} y={23} lueur="rgba(255,190,120,0.9)" italique />
      {f >= 108 ? (
        <div
          style={{
            position: 'absolute',
            top: '12%',
            width: '100%',
            display: 'flex',
            flexDirection: 'column',
            alignItems: 'center',
            opacity: logo,
            transform: `translateY(${(1 - logo) * 30}px)`,
          }}
        >
          <Img
            src={staticFile('avatars/ic_launcher_face.png')}
            style={{width: 170, height: 170, borderRadius: '50%', background: C.creme, boxShadow: '0 0 60px rgba(255,214,150,0.8)'}}
          />
          <div style={{marginTop: 26, fontFamily: POLICE_UI, fontSize: 96, color: C.creme, textShadow: '0 2px 12px rgba(40,10,30,0.9), 0 0 36px rgba(255,190,120,0.7)'}}>
            MyHitbodedut
          </div>
          <div style={{marginTop: 10, fontFamily: POLICE_TITRE, fontStyle: 'italic', fontSize: 64, color: '#FFE9C4', textShadow: '0 2px 10px rgba(40,10,30,0.95), 0 0 30px rgba(40,10,30,0.8)'}}>
            Avant le monde, Lui.
          </div>
        </div>
      ) : null}
      {f >= 136 ? (
        <div
          style={{
            position: 'absolute',
            top: '58%',
            width: '100%',
            textAlign: 'center',
            opacity: appel,
            transform: `scale(${0.95 + 0.05 * appel})`,
          }}
        >
          <span
            style={{
              display: 'inline-block',
              padding: '26px 56px',
              borderRadius: 999,
              background: C.or,
              color: C.vert,
              fontFamily: POLICE_UI,
              fontSize: 50,
              boxShadow: '0 0 50px rgba(244,199,122,0.7)',
            }}
          >
            Commence demain matin
          </span>
        </div>
      ) : null}
      <AbsoluteFill style={{background: '#FFF6E6', opacity: blanc}} />
    </AbsoluteFill>
  );
};
