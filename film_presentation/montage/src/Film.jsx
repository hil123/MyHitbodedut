import React from 'react';
import {AbsoluteFill, Img, Sequence, interpolate, interpolateColors, spring, useCurrentFrame, useVideoConfig, Easing, staticFile, random} from 'remotion';

// ---------- thème ----------
const FPS = 30;
const GOLD = '#FFD27A';
const BRAND = '#16775A';
const DARK = '#173C32';
const GROUND = 1110; // ligne de sol des personnages (zone sûre : rien d'essentiel sous ~1290)
const F = (s) => Math.round(s * FPS);
const clamp = {extrapolateLeft: 'clamp', extrapolateRight: 'clamp'};
const io = Easing.inOut(Easing.cubic);
const out = Easing.out(Easing.cubic);
const iv = (f, a, b, x, y, e = io) => interpolate(f, [a, b], [x, y], {...clamp, easing: e});

// ---------- scènes (secondes) ----------
import TIMING from './timing.json';
const DUR = TIMING.dur;
const START = DUR.reduce((acc, d, i) => [...acc, i === 0 ? 0 : acc[i - 1] + DUR[i - 1]], []);
export const TOTAL = F(DUR.reduce((a, b) => a + b, 0));
export const VOICE_OFFSET = 0.2; // la voix démarre 0,2 s après le début de chaque scène
export const SCENE_START = START;

const VOICE = TIMING.voice;

// sous-titres : morceaux affichés l'un après l'autre, mots-clés en or
const CAPS = [
  [['Le matin, à qui parlez-vous *en *premier ?']],
  [['Les messages, les nouvelles…'], ['et la journée *file sans vous.']],
  [['MyHitbodedut vous garde', 'un moment *à *l’écart,'], ['avant tout le reste.']],
  [['Vous choisissez les applis.'], ['Le matin, elles *attendent', 'votre hitbodedout.']],
  [['Un minuteur, un niggoun…'], ['et vous parlez à *Hachem,', 'simplement.']],
  [['Un mot dans votre journal.'], ['Et votre *série grandit,', 'jour après jour.']],
  [['Zmanim, calendrier,', 'prières du jour :'], ['*tout *est *là.']],
  [['*Rien ne quitte', 'votre téléphone.']],
  [],
];

const P = (g) => (g === 'm' ? 'homme_papillotes' : 'fille_foulard_blanc');
const ch = (g, n) => staticFile(`chars/${P(g)}_${n}.png`);
const scr = (g, n) => staticFile(`ecrans/${g}_${n}.png`);

// ---------- décor : ciel d'aube continu (élément commun à toutes les scènes) ----------
const Sky = () => {
  const f = useCurrentFrame();
  const t = f / FPS;
  const k = [0, 4, 8.6, 11.5, 30, 46, 53];
  const top = interpolateColors(t, k, ['#0b1533', '#1a2a52', '#3c5577', '#24406e', '#2b4b7c', '#2f5486', '#355d90']);
  const mid = interpolateColors(t, k, ['#2a3566', '#5a6f93', '#9fb2c6', '#e58f62', '#f1a46c', '#f4b07a', '#f6bb86']);
  const low = interpolateColors(t, k, ['#6b5a7c', '#a9b4c2', '#dfe1dc', '#ffd59a', '#ffe0a8', '#ffe6b6', '#ffeac0']);
  const sunY = interpolate(t, [0, 8.8, 15, 53], [1500, 1450, 1000, 880], {...clamp, easing: Easing.inOut(Easing.sin)});
  const sunO = iv(f, F(8.6), F(11), 0, 1);
  const rays = iv(f, F(9.5), F(13), 0, 0.33) + (t > 21.7 && t < 27.9 ? iv(f, F(21.7), F(23.5), 0, 0.12) - iv(f, F(26.5), F(27.9), 0, 0.12) : 0);
  const stars = iv(f, 0, F(5), 1, 0);
  const hillA = interpolateColors(t, k, ['#141d3a', '#3b4a5f', '#5c6c66', '#4d6a4f', '#4e7350', '#527953', '#577f57']);
  const hillB = interpolateColors(t, k, ['#0d1430', '#2a3647', '#3e4f47', '#2f4d3a', '#2f533b', '#33593e', '#365e41']);
  const hillC = interpolateColors(t, k, ['#080d22', '#1b2433', '#26342e', '#1d3527', '#1e3a2a', '#213e2d', '#234230']);
  const drift = Math.sin(t / 6) * 10;
  return (
    <AbsoluteFill>
      <AbsoluteFill style={{background: `linear-gradient(180deg, ${top} 0%, ${mid} 52%, ${low} 68%)`}} />
      {/* étoiles */}
      <AbsoluteFill style={{opacity: stars}}>
        {Array.from({length: 70}).map((_, i) => (
          <div key={i} style={{position: 'absolute', left: random(`sx${i}`) * 1080, top: random(`sy${i}`) * 900, width: 3, height: 3, borderRadius: 2, background: '#fff', opacity: 0.4 + 0.6 * Math.abs(Math.sin(t * 2 + i))}} />
        ))}
      </AbsoluteFill>
      {/* soleil */}
      <div style={{position: 'absolute', left: 540 - 700, top: sunY - 700, width: 1400, height: 1400, borderRadius: '50%', opacity: sunO,
        background: 'radial-gradient(circle, rgba(255,246,214,1) 0%, rgba(255,226,160,.95) 7%, rgba(255,196,120,.45) 18%, rgba(255,170,100,.12) 40%, rgba(255,160,90,0) 60%)'}} />
      {/* rayons */}
      <div style={{position: 'absolute', left: 540 - 1400, top: sunY - 1400, width: 2800, height: 2800, opacity: rays, mixBlendMode: 'screen',
        transform: `rotate(${t * 1.2}deg)`,
        background: 'repeating-conic-gradient(from 0deg, rgba(255,236,190,.55) 0deg 3deg, rgba(255,236,190,0) 3deg 14deg)',
        WebkitMaskImage: 'radial-gradient(circle, #000 0%, rgba(0,0,0,.5) 25%, transparent 55%)'}} />
      {/* collines en parallaxe */}
      <svg width="1080" height="1920" style={{position: 'absolute', left: 0, top: 0}}>
        <path transform={`translate(${drift * 0.4},0)`} d="M-60 1010 C 160 930, 330 980, 520 950 S 900 900, 1140 980 L1140 1920 L-60 1920 Z" fill={hillA} />
        <path transform={`translate(${drift * 0.8},0)`} d="M-60 1080 C 200 1030, 420 1090, 640 1060 S 950 1030, 1140 1075 L1140 1920 L-60 1920 Z" fill={hillB} />
        <path transform={`translate(${drift * 1.3},0)`} d="M-80 1150 C 240 1110, 520 1160, 760 1135 S 1020 1120, 1160 1150 L1160 1920 L-80 1920 Z" fill={hillC} />
      </svg>
      {/* poussière dans la lumière */}
      <AbsoluteFill style={{opacity: 0.25 + sunO * 0.6}}>
        {Array.from({length: 45}).map((_, i) => {
          const sp = 18 + random(`p${i}`) * 30;
          const y = ((random(`py${i}`) * 1400 - t * sp) % 1400 + 1400) % 1400 + 100;
          const x = random(`px${i}`) * 1080 + Math.sin(t / 2 + i) * 20;
          const s = 3 + random(`ps${i}`) * 6;
          return <div key={i} style={{position: 'absolute', left: x, top: y, width: s, height: s, borderRadius: '50%', background: 'rgba(255,240,205,.7)', filter: 'blur(1.5px)'}} />;
        })}
      </AbsoluteFill>
    </AbsoluteFill>
  );
};

const Finish = () => {
  const f = useCurrentFrame();
  return (
    <>
      <AbsoluteFill style={{background: 'radial-gradient(ellipse at 50% 45%, rgba(0,0,0,0) 55%, rgba(0,0,0,.45) 100%)', pointerEvents: 'none'}} />
      <AbsoluteFill style={{backgroundImage: `url(${staticFile('grain.png')})`, backgroundPosition: `${(f * 37) % 512}px ${(f * 91) % 512}px`, mixBlendMode: 'overlay', opacity: 0.08}} />
    </>
  );
};

// ---------- personnage ----------
const Char = ({src, x, h, bottom = GROUND, from = 0, dur, exitLen = 9, delay = 0, rise = 40, dissolveOut = false, fade = false}) => {
  const f = useCurrentFrame() - from;
  const {fps} = useVideoConfig();
  if (f < delay || f > dur) return null;
  const e = spring({frame: f - delay, fps, config: {damping: 200}, durationInFrames: 20});
  const xo = dissolveOut ? 1 : iv(f, dur - exitLen, dur, 1, 0, Easing.in(Easing.cubic));
  const breathe = 1 + 0.007 * Math.sin((f / fps) * 2.1);
  const sway = 0.5 * Math.sin((f / fps) * 1.3);
  const op = Math.min(e, dissolveOut ? iv(f, dur - exitLen, dur, 1, 0) : xo);
  return (
    <>
      <div style={{position: 'absolute', left: x - h * 0.22, top: bottom - 18, width: h * 0.44, height: 36, borderRadius: '50%', background: 'rgba(0,0,0,.35)', filter: 'blur(14px)', opacity: op}} />
      <Img src={src} style={{position: 'absolute', left: x, top: bottom - h, height: h, opacity: op,
        transform: `translateX(-50%) translateY(${(1 - e) * rise + (1 - xo) * 30}px) scale(${(0.96 + 0.04 * e) * breathe}) rotate(${sway}deg)`, transformOrigin: '50% 100%',
        filter: 'drop-shadow(0 0 26px rgba(255,214,150,.22))', ...(fade ? {WebkitMaskImage: 'linear-gradient(180deg,#000 70%,transparent 100%)'} : {})}} />
    </>
  );
};

// ---------- téléphone ----------
const Phone = ({w, x, y, rotY = 0, rotZ = 0, op = 1, scale = 1, children}) => {
  const h = w * (2532 / 1170) + w * 0.06;
  return (
    <div style={{position: 'absolute', left: x - w / 2, top: y - h / 2, width: w, height: h, opacity: op, perspective: 1600}}>
      <div style={{width: '100%', height: '100%', transform: `rotateY(${rotY}deg) rotateZ(${rotZ}deg) scale(${scale})`, transformStyle: 'preserve-3d'}}>
        <div style={{position: 'absolute', inset: 0, borderRadius: w * 0.15, background: 'linear-gradient(145deg,#3a3d42,#0d0e10 40%,#1c1d20)', boxShadow: '0 40px 80px rgba(0,0,0,.45), 0 0 0 2px rgba(255,255,255,.08) inset'}} />
        <div style={{position: 'absolute', left: w * 0.03, top: w * 0.03, right: w * 0.03, bottom: w * 0.03, borderRadius: w * 0.125, overflow: 'hidden', background: '#000'}}>
          {children}
          <div style={{position: 'absolute', inset: 0, background: 'linear-gradient(115deg, rgba(255,255,255,.16) 0%, rgba(255,255,255,0) 35%)'}} />
        </div>
        <div style={{position: 'absolute', left: '50%', top: w * 0.055, width: w * 0.28, height: w * 0.075, marginLeft: -w * 0.14, borderRadius: 40, background: '#000'}} />
      </div>
    </div>
  );
};
const Screen = ({src, op = 1, dx = 0, zoom = 1, top = false}) => (
  <Img src={src} style={{position: 'absolute', inset: 0, width: '100%', height: '100%', objectFit: 'cover', objectPosition: top ? '50% 0%' : '50% 50%', opacity: op, transform: `translateX(${dx}%) scale(${zoom})`, transformOrigin: '50% 30%'}} />
);

// écran de verrouillage avec notifications (scènes 1-2)
const NOTIFS = [
  ['Groupe Famille', '14 nouveaux messages'], ['Infos', 'Alerte : dernière minute'], ['Messages', 'Tu as vu ça ?'], ['Mail', 'Réunion avancée à 9 h'],
  ['Promo', '-50 % aujourd’hui seulement'], ['Groupe Boulot', '23 nouveaux messages'], ['Vidéos', 'Nouvelle vidéo pour vous'], ['Infos', 'Ce qu’il faut savoir ce matin'],
];
const LockScreen = ({f, w}) => {
  const {fps} = useVideoConfig();
  const mins = Math.floor(interpolate(f, [0, F(2.2), F(4.8)], [2, 9, 167], {...clamp, easing: Easing.in(Easing.quad)}));
  const hh = 7 + Math.floor(mins / 60);
  const mm = String(mins % 60).padStart(2, '0');
  const u = w / 400;
  return (
    <AbsoluteFill style={{background: 'linear-gradient(170deg,#1f2b4d,#3b2e55 60%,#2a2140)', fontFamily: 'Inter'}}>
      <div style={{textAlign: 'center', color: '#fff', marginTop: 70 * u, fontSize: 82 * u, fontWeight: 300, letterSpacing: -2}}>{`0${hh}`.slice(-2)}:{mm}</div>
      <div style={{textAlign: 'center', color: 'rgba(255,255,255,.75)', fontSize: 17 * u}}>mardi 3 novembre</div>
      <div style={{position: 'absolute', left: 14 * u, right: 14 * u, top: 230 * u}}>
        {NOTIFS.map(([a, b], i) => {
          const s = spring({frame: f - F(0.25) - i * 7 * (i > 2 ? 0.55 : 1), fps, config: {damping: 16, stiffness: 140}});
          return (
            <div key={i} style={{background: 'rgba(255,255,255,.88)', borderRadius: 18 * u, padding: `${10 * u}px ${14 * u}px`, marginBottom: 8 * u, opacity: s, transform: `translateY(${(1 - s) * -40}px) scale(${0.9 + 0.1 * s})`}}>
              <div style={{fontSize: 13 * u, fontWeight: 700, color: '#222'}}>{a}</div>
              <div style={{fontSize: 14 * u, color: '#444'}}>{b}</div>
            </div>
          );
        })}
      </div>
    </AbsoluteFill>
  );
};

// ---------- sous-titres ----------
const Caption = ({lines, from, len}) => {
  const f = useCurrentFrame() - from;
  const {fps} = useVideoConfig();
  if (f < 0 || f > len) return null;
  const outO = iv(f, len - 7, len, 1, 0);
  let wi = 0;
  return (
    <div style={{position: 'absolute', left: 50, right: 50, top: 1150, textAlign: 'center', fontFamily: 'Inter', fontWeight: 800, fontSize: 64, lineHeight: 1.18, color: '#fff', opacity: outO,
      textShadow: '0 4px 18px rgba(0,0,0,.55), 0 1px 3px rgba(0,0,0,.6)'}}>
      {lines.map((ln, li) => (
        <div key={li}>
          {ln.split(' ').map((w, i) => {
            const key = w.startsWith('*');
            const s = spring({frame: f - (wi++) * 2.5, fps, config: {damping: 200}, durationInFrames: 14});
            return (
              <span key={i} style={{display: 'inline-block', marginRight: '0.26em', color: key ? GOLD : '#fff', opacity: s, transform: `translateY(${(1 - s) * 22}px)`}}>{w.replace('*', '')}</span>
            );
          })}
        </div>
      ))}
    </div>
  );
};

const Captions = ({g}) => (
  <>
    {CAPS.map((chunks, si) => {
      if (!chunks.length) return null;
      const vd = VOICE[g][si];
      const total = chunks.reduce((a, c) => a + c.join(' ').length, 0);
      let acc = 0;
      return chunks.map((lines, ci) => {
        const l = lines.join(' ').length;
        const st = START[si] + VOICE_OFFSET + (acc / total) * vd - 0.1;
        acc += l;
        const isLast = ci === chunks.length - 1;
        const en = isLast ? START[si] + DUR[si] - 0.05 : START[si] + VOICE_OFFSET + (acc / total) * vd - 0.12;
        return <Caption key={`${si}-${ci}`} lines={lines} from={F(st)} len={F(en - st)} />;
      });
    })}
  </>
);

// ---------- éléments de marque ----------
const Logo = ({g, size}) => (
  <Img src={staticFile(`logo_${g}.png`)} style={{width: size, height: size, borderRadius: size * 0.23, boxShadow: '0 18px 40px rgba(0,0,0,.35)'}} />
);

const Bubble = ({x, y, label, sub, at}) => {
  const f = useCurrentFrame();
  const {fps} = useVideoConfig();
  const s = spring({frame: f - at, fps, config: {damping: 12, stiffness: 160}});
  const fl = Math.sin((f - at) / 14) * 6;
  return (
    <div style={{position: 'absolute', left: x, top: y + fl, transform: `translate(-50%,-50%) scale(${s})`, background: 'rgba(255,255,255,.93)', borderRadius: 22, padding: '14px 22px', fontFamily: 'Inter', boxShadow: '0 12px 30px rgba(0,0,0,.3)', minWidth: 250}}>
      <div style={{display: 'flex', alignItems: 'center', gap: 10}}>
        <div style={{width: 14, height: 14, borderRadius: 7, background: '#e5484d'}} />
        <div style={{fontSize: 24, fontWeight: 700, color: '#222'}}>{label}</div>
      </div>
      <div style={{fontSize: 22, color: '#555', marginTop: 4}}>{sub}</div>
    </div>
  );
};

// ---------- homographie : poser un écran dans un téléphone tenu en main ----------
const solve = (A, b) => {
  const n = b.length; const M = A.map((r, i) => [...r, b[i]]);
  for (let c = 0; c < n; c++) {
    let p = c; for (let r = c + 1; r < n; r++) if (Math.abs(M[r][c]) > Math.abs(M[p][c])) p = r;
    [M[c], M[p]] = [M[p], M[c]];
    for (let r = 0; r < n; r++) if (r !== c) { const k = M[r][c] / M[c][c]; for (let j = c; j <= n; j++) M[r][j] -= k * M[c][j]; }
  }
  return M.map((r, i) => r[n] / r[i]);
};
const matrix3d = (w, h, q) => {
  const src = [[0, 0], [w, 0], [w, h], [0, h]]; const A = []; const b = [];
  src.forEach(([x, y], i) => { const [u, v] = q[i]; A.push([x, y, 1, 0, 0, 0, -x * u, -y * u]); b.push(u); A.push([0, 0, 0, x, y, 1, -x * v, -y * v]); b.push(v); });
  const H = solve(A, b);
  return `matrix3d(${H[0]},${H[3]},0,${H[6]},${H[1]},${H[4]},0,${H[7]},0,0,1,0,${H[2]},${H[5]},0,1)`;
};
const RECT = {"X03_droit": {"x": 609.5, "y": 557, "w": 505, "h": 952, "a": 0}};
// image détourée (1024×1536) posée à (left, top) avec hauteur h ; screen = contenu 390×844 incrusté
const Shot = ({name, left, top, h, iw = 1024, screen, op = 1, fadeBottom = false}) => {
  const s = h / 1536; const w = iw * s;
  const rc = RECT[name];
  const fade = fadeBottom ? {WebkitMaskImage: 'linear-gradient(180deg,#000 82%,transparent 98%)'} : {};
  return (
    <div style={{position: 'absolute', left, top, width: w, height: h, opacity: op, ...fade}}>
      <Img src={staticFile(`plans/${name}.png`)} style={{position: 'absolute', inset: 0, width: '100%', height: '100%'}} />
      {rc && (() => {
        const W = rc.w * s, H = rc.h * s, b = W * 0.045, rad = W * 0.16;
        return (
          <div style={{position: 'absolute', left: rc.x * s - W / 2, top: rc.y * s - H / 2, width: W, height: H, transform: `rotate(${rc.a}deg)`}}>
            <div style={{position: 'absolute', inset: 0, borderRadius: rad, background: 'linear-gradient(145deg,#4a4d53,#16171a 35%,#0b0b0d 70%,#2a2c30)', boxShadow: '0 0 0 1px rgba(255,255,255,.18) inset, 0 6px 18px rgba(0,0,0,.35)'}} />
            <div style={{position: 'absolute', left: b, top: b, right: b, bottom: b, borderRadius: rad - b, overflow: 'hidden', background: '#000'}}>
              <div style={{position: 'absolute', left: 0, top: 0, width: 390, height: (H - 2 * b) / ((W - 2 * b) / 390), overflow: 'hidden', transformOrigin: '0 0', transform: `scale(${(W - 2 * b) / 390})`}}>{screen}</div>
              <div style={{position: 'absolute', inset: 0, background: 'linear-gradient(115deg, rgba(255,255,255,.14) 0%, rgba(255,255,255,0) 38%)'}} />
            </div>
            <div style={{position: 'absolute', left: '50%', top: b + W * 0.02, width: W * 0.26, height: W * 0.065, marginLeft: -W * 0.13, borderRadius: 40, background: '#000'}} />
          </div>
        );
      })()}
      {rc && <Img src={staticFile(`plans/${name}_doigts.png`)} style={{position: 'absolute', inset: 0, width: '100%', height: '100%'}} />}
    </div>
  );
};
// image plein cadre (décor compris), poussée lente motivée
const FullFrame = ({src, from, dur, z0 = 1, z1 = 1.1, ox = '50%', oy = '50%', fadeIn = 0, fadeOut = 0}) => {
  const f = useCurrentFrame() - from;
  if (f < 0 || f > dur) return null;
  const z = interpolate(f, [0, dur], [z0, z1], {...clamp, easing: Easing.inOut(Easing.sin)});
  const op = Math.min(fadeIn ? iv(f, 0, fadeIn, 0, 1) : 1, fadeOut ? iv(f, dur - fadeOut, dur, 1, 0) : 1);
  return (
    <AbsoluteFill style={{opacity: op, overflow: 'hidden'}}>
      <Img src={src} style={{position: 'absolute', left: -100, top: 0, width: 1280, height: 1920, transform: `scale(${z})`, transformOrigin: `${ox} ${oy}`}} />
    </AbsoluteFill>
  );
};

// ---------- le film ----------
export const Film = ({g}) => {
  const f = useCurrentFrame();
  const {fps} = useVideoConfig();
  const S = START.map(F);
  const D = DUR.map(F);
  const t = f / fps;
  const M = g === 'm';

  // S2 : poussée vers le téléphone tenu en main
  // S6 : plan « téléphone en main, sourire » — l'écran montre la série
  const s6 = M ? {left: 123, top: 230, h: 1250, ox: 715, oy: 450, z0: 1.12, z1: 1.25} : {left: 40, top: 60, h: 1500, ox: 860, oy: 600, z0: 1.0, z1: 1.1};
  const s6at = S[5] + F(3.3);
  const s2out = iv(f, S[2], S[2] + 9, 0, 1, Easing.in(Easing.cubic));

  // S4 : gros plan des mains, tap sur « Commencer »
  const tapAt = S[3] + F(5.3);
  const tapF = f - tapAt;
  const tap = tapF >= 0 && tapF < 22 ? tapF / 22 : null;
  const swAt = tapAt + 10;
  const sw = iv(f, swAt, swAt + 12, 0, 1);
  const z4 = iv(f, S[3], S[4] + F(0.3), 1.0, 1.07, Easing.inOut(Easing.sin));
  const s4in = spring({frame: f - S[3], fps, config: {damping: 200}, durationInFrames: 14});

  // S5 : pose le téléphone, puis lumière → marche à l'aube
  const s5cut = S[4] + F(2.5);

  // téléphone flottant : scènes 6 à 8
  const phoneBack = spring({frame: f - S[5] - 4, fps, config: {damping: 20, stiffness: 90}});
  const phoneLeave = iv(f, S[7] - 4, S[7] + 8, 0, 1, Easing.in(Easing.cubic));
  let ph = null;
  if (f >= S[5] && f < S[7] + 8) {
    const keys = [[S[5], 790, 690, 400, -9], [S[5] + 30, 790, 690, 400, -6], [S[6], 790, 690, 400, -6], [S[6] + 22, 770, 680, 420, -5], [S[7] - 4, 770, 680, 420, -5]];
    const at = (idx) => interpolate(f, keys.map((k) => k[0]), keys.map((k) => k[idx]), {...clamp, easing: io});
    ph = {w: at(3), x: at(1) + (1 - phoneBack) * 600 + phoneLeave * 700, y: at(2), rotY: at(4) - (1 - phoneBack) * 25};
  }
  const appScreen = () => {
    const swp = (a) => iv(f, a, a + 12, 0, 1);
    const layers = [[S[5], 'parcours'], [S[6] + 10, 'zmanim']];
    return layers.map(([a, n], i) => {
      const inP = i === 0 ? 1 : swp(a);
      const nextA = layers[i + 1]?.[0];
      const outP = nextA ? swp(nextA) : 0;
      if (inP <= 0 || outP >= 1) return null;
      return <Screen key={n} src={scr(g, n)} dx={(1 - inP) * 100 - outP * 30} zoom={n === 'zmanim' ? iv(f, S[6], S[7], 1, 1.08, Easing.inOut(Easing.sin)) : 1} />;
    });
  };

  // série 18 → 19 (scène 6)
  const bumpAt = S[5] + F(3.9);
  const bump = spring({frame: f - bumpAt, fps, config: {damping: 9, stiffness: 180}});
  const streak = f >= bumpAt ? 19 : 18;

  const bubbleXY = M ? [[780, 1000], [700, 860], [860, 760], [640, 690]] : [[760, 900], [690, 760], [850, 660], [620, 600]];

  return (
    <AbsoluteFill style={{background: '#000', fontFamily: 'Inter'}}>
      <Sky />

      {/* S1 — ACCROCHE : le réveil, le téléphone sur la table de nuit s'allume */}
      <FullFrame src={staticFile(`plans/${M ? 'M01' : 'F01'}_reveil.png`)} from={0} dur={S[1] + 8} z0={1.0} z1={1.1} ox="62%" oy="52%" fadeOut={8} />
      {f < S[1] + 8 && bubbleXY.map(([x, y], i) => (
        <div key={i} style={{opacity: iv(f, S[1], S[1] + 8, 1, 0)}}>
          <Bubble x={x - 60 * i * 0} y={y} label={['Groupe Famille', 'Infos', 'Mail', 'Promo'][i]} sub={['14 nouveaux messages', 'Alerte : dernière minute', 'Réunion avancée à 9 h', '-50 % aujourd’hui'][i]} at={10 + i * 14} />
        </div>
      ))}

      {/* S2 — PROBLÈME : le téléphone seul, les notifications déferlent, l'heure file */}
      {f >= S[1] && f < S[2] + 10 && (() => {
        const e = spring({frame: f - S[1] - 2, fps, config: {damping: 18, stiffness: 90}});
        return (
          <Phone w={460} x={540} y={740 + (1 - e) * 900 - s2out * 1400} rotY={0} rotZ={(1 - e) * 8 + Math.sin((f - S[1]) / 20) * 0.8} scale={iv(f, S[1], S[2], 1, 1.06, Easing.inOut(Easing.sin))}>
            <LockScreen f={f - S[1]} w={470} />
          </Phone>
        );
      })()}

      {/* S3 — PROMESSE : logo + accueil */}
      {f >= S[2] && f < S[3] + 20 && (() => {
        const lf = f - S[2];
        const s = spring({frame: lf - 8, fps, config: {damping: 14, stiffness: 120}});
        const toCorner = iv(f, S[3], S[3] + 18, 0, 1);
        return (
          <div style={{position: 'absolute', left: 540, top: 280, transform: `translate(-50%,0) translate(${toCorner * -380}px, ${toCorner * -10}px) scale(${s * (1 - toCorner * 0.6)})`, textAlign: 'center', opacity: 1 - toCorner}}>
            <Logo g={g} size={190} />
            <div style={{color: '#fff', fontSize: 84, fontWeight: 800, marginTop: 18, letterSpacing: -1, textShadow: '0 6px 24px rgba(0,0,0,.35)'}}>MyHitbodedut</div>
          </div>
        );
      })()}
      <Char src={ch(g, '01_accueil_bras_ouverts')} x={540} h={480} from={S[2]} dur={D[2] + 6} delay={F(0.9)} />

      {/* S4 — MÉCANISME : les mains, l'écran de barrière, le tap */}
      {f >= S[3] && f < S[4] + 10 && (
        <AbsoluteFill style={{transform: `scale(${z4})`, transformOrigin: '659px 730px', opacity: Math.min(s4in, iv(f, S[4], S[4] + 10, 1, 0))}}>
          <Shot name="X03_droit" left={-100} top={0} h={1920} screen={
            <>
              <Screen src={scr(g, 'barriere_x')} dx={-sw * 30} top />
              {sw > 0 && <Screen src={scr(g, 'seance')} dx={(1 - sw) * 100} top />}
            </>
          } />
          {tap !== null && (
            <div style={{position: 'absolute', left: 664 - 90, top: 734 - 90, width: 180, height: 180, borderRadius: '50%', border: `${8 * (1 - tap)}px solid rgba(255,255,255,.95)`, background: `rgba(255,255,255,${0.3 * (1 - tap)})`, transform: `scale(${0.3 + tap})`}} />
          )}
        </AbsoluteFill>
      )}

      {/* S5 — SÉANCE : on pose le téléphone… */}
      {f >= S[4] && f < s5cut + 10 && (() => {
        const e = spring({frame: f - S[4], fps, config: {damping: 200}, durationInFrames: 16});
        return (
          <AbsoluteFill style={{opacity: e, transform: `translateY(${(1 - e) * 40}px) scale(${iv(f, S[4], s5cut + 10, 1, 1.05)})`, transformOrigin: '540px 900px'}}>
            <Shot name={M ? 'M04_pose_telephone' : 'F04_pose_telephone'} iw={M ? 1024 : 512} left={M ? 540 - 1150 * 1024 / 1536 / 2 : 540 - 1250 * 512 / 1536 / 2 - 20} top={M ? 30 : -60} h={M ? 1150 : 1250} fadeBottom />
          </AbsoluteFill>
        );
      })()}
      {/* … et on part parler à Hachem (raccord par la lumière) */}
      {M ? (
        <FullFrame src={staticFile('plans/M05_marche_aube.png')} from={s5cut} dur={S[5] - s5cut + 8} z0={1.02} z1={1.16} ox="50%" oy="42%" fadeIn={10} fadeOut={8} />
      ) : (
        <>
          <Char src={ch(g, '09_priere_mains_jointes')} x={540} h={720} from={s5cut} dur={F(1.9)} delay={0} exitLen={12} dissolveOut />
          <Char src={ch(g, '04_extase_yeux_fermes')} x={540} h={720} from={s5cut + F(1.6)} dur={S[5] - s5cut - F(1.6) + 4} delay={0} rise={0} />
        </>
      )}
      {f >= s5cut - 6 && f < s5cut + 14 && (
        <AbsoluteFill style={{background: 'radial-gradient(circle at 50% 40%, rgba(255,240,200,1), rgba(255,200,120,.6) 60%)', opacity: Math.sin(((f - s5cut + 6) / 20) * Math.PI) * 0.85}} />
      )}

      {/* logo permanent en haut (scènes 4 à 8) */}
      {f >= S[3] + 12 && f < S[8] && (
        <div style={{position: 'absolute', left: 56, top: 285, display: 'flex', alignItems: 'center', gap: 16, opacity: iv(f, S[3] + 12, S[3] + 24, 0, 1) * iv(f, S[8] - 10, S[8], 1, 0)}}>
          <Logo g={g} size={72} />
          <div style={{color: '#fff', fontSize: 36, fontWeight: 800, textShadow: '0 3px 12px rgba(0,0,0,.4)'}}>MyHitbodedut</div>
        </div>
      )}

      {/* S6 — JOURNAL puis SÉRIE */}
      <Char src={ch(g, '05_ecrit_au_bureau')} x={330} h={470} from={S[5]} dur={F(3.5)} delay={4} />
      <Char src={ch(g, '10_victoire_poings_leves')} x={320} h={690} from={s6at} dur={S[6] - s6at + 4} delay={0} rise={80} />
      {/* S7 — ZMANIM */}
      <Char src={ch(g, '03_lecture_tehilim')} x={300} h={690} from={S[6]} dur={D[6] + 4} delay={4} />
      {/* S8 — CONFIDENTIALITÉ */}
      <Char src={ch(g, '02_pouce_leve')} x={330} h={690} from={S[7]} dur={D[7] + 4} delay={2} />

      {ph && (
        <Phone w={ph.w} x={ph.x} y={ph.y} rotY={0}>{appScreen()}</Phone>
      )}

      {/* S6 — compteur de série */}
      {f >= S[5] + 8 && f < S[6] && (
        <div style={{position: 'absolute', left: 540, top: 395, transform: `translateX(-50%) scale(${f >= bumpAt ? 1 + 0.18 * (1 - bump) * Math.sin(bump * Math.PI) : spring({frame: f - S[5] - 8, fps, config: {damping: 200}})})`,
          background: 'rgba(23,60,50,.92)', color: '#fff', borderRadius: 60, padding: '20px 40px', fontSize: 46, fontWeight: 800, boxShadow: '0 16px 40px rgba(0,0,0,.35)', whiteSpace: 'nowrap',
          opacity: iv(f, S[6] - 8, S[6], 1, 0)}}>
          Série : <span style={{color: GOLD}}>{streak}</span> jours
          {f >= bumpAt && Array.from({length: 18}).map((_, i) => {
            const a = (i / 18) * Math.PI * 2;
            const p = iv(f, bumpAt, bumpAt + 22, 0, 1, out);
            return <div key={i} style={{position: 'absolute', left: '50%', top: '50%', width: 10, height: 10, borderRadius: 5, background: GOLD, opacity: 1 - p, transform: `translate(${Math.cos(a) * 220 * p}px, ${Math.sin(a) * 110 * p}px)`}} />;
          })}
        </div>
      )}

      {/* S8 — bouclier */}
      {f >= S[7] && f < S[8] + 6 && (() => {
        const lf = f - S[7];
        const draw = iv(lf, 4, 34, 0, 1, out);
        const chk = iv(lf, 30, 46, 0, 1, out);
        const s = spring({frame: lf, fps, config: {damping: 200}});
        return (
          <svg width="380" height="440" viewBox="0 0 380 440" style={{position: 'absolute', left: 780 - 190, top: 690 - 220, opacity: s * iv(f, S[8] - 4, S[8] + 6, 1, 0), filter: 'drop-shadow(0 16px 30px rgba(0,0,0,.35))'}}>
            <path d="M190 20 L340 80 L340 200 C340 310 270 380 190 420 C110 380 40 310 40 200 L40 80 Z" fill={`rgba(22,119,90,${0.85 * draw})`} stroke="#fff" strokeWidth="10" strokeDasharray="1300" strokeDashoffset={1300 * (1 - draw)} strokeLinejoin="round" />
            <path d="M120 220 L172 272 L265 170" fill="none" stroke={GOLD} strokeWidth="22" strokeLinecap="round" strokeLinejoin="round" strokeDasharray="260" strokeDashoffset={260 * (1 - chk)} />
          </svg>
        );
      })()}

      {/* S9 — APPEL À L'ACTION */}
      {f >= S[8] && (() => {
        const lf = f - S[8];
        const s1 = spring({frame: lf - 2, fps, config: {damping: 14, stiffness: 110}});
        const s2 = spring({frame: lf - 12, fps, config: {damping: 200}});
        const s3 = spring({frame: lf - F(2.2), fps, config: {damping: 12, stiffness: 140}});
        return (
          <>
            <div style={{position: 'absolute', left: 0, right: 0, top: 230, textAlign: 'center'}}>
              <div style={{display: 'inline-block', transform: `scale(${s1})`}}><Logo g={g} size={160} /></div>
              <div style={{color: '#fff', fontSize: 80, fontWeight: 800, marginTop: 10, letterSpacing: -1, opacity: s2, transform: `translateY(${(1 - s2) * 30}px)`, textShadow: '0 6px 24px rgba(0,0,0,.35)'}}>MyHitbodedut</div>
              <div style={{color: '#fff', fontSize: 34, fontWeight: 600, marginTop: 10, opacity: s3, textShadow: '0 3px 12px rgba(0,0,0,.4)'}}>Essayez-la dès maintenant :</div>
              <div style={{display: 'inline-block', marginTop: 10, background: '#fff', color: DARK, borderRadius: 50, padding: '14px 30px', fontSize: 36, fontWeight: 800, opacity: s3, transform: `scale(${0.8 + 0.2 * s3})`, boxShadow: '0 14px 34px rgba(0,0,0,.3)'}}>
                hil123.github.io/myhitbodedut-web
              </div>
              <div style={{color: '#fff', fontSize: 34, fontWeight: 800, marginTop: 16, opacity: spring({frame: lf - F(3.4), fps, config: {damping: 200}}), textShadow: '0 2px 4px rgba(0,0,0,.6), 0 4px 16px rgba(0,0,0,.5)'}}>Bientôt sur Google Play</div>
            </div>
            <div style={{position: 'absolute', left: 0, right: 0, bottom: 64, textAlign: 'center', color: 'rgba(255,255,255,.75)', fontSize: 23, opacity: s2}}>
              Musique : « Heartwarming », Kevin MacLeod (incompetech.com) · CC BY 4.0
            </div>
          </>
        );
      })()}
      <Char fade src={ch(g, '16_portrait')} x={540} h={330} from={S[8]} dur={D[8] + 10} delay={F(0.5)} />

      {/* plan encore provisoire */}
      {!M && f >= s5cut && f < S[5] && (
        <div style={{position: 'absolute', right: 40, top: 280, background: 'rgba(200,40,40,.85)', color: '#fff', fontSize: 22, fontWeight: 700, padding: '8px 16px', borderRadius: 10}}>PROVISOIRE · plan 05 à venir</div>
      )}

      <AbsoluteFill style={{background: 'linear-gradient(180deg, rgba(0,0,0,0) 54%, rgba(0,0,0,.38) 62%, rgba(0,0,0,.38) 68%, rgba(0,0,0,0) 74%)', opacity: f < F(START[8]) ? 1 : 0}} />
      <Captions g={g} />
      <Finish />
    </AbsoluteFill>
  );
};
