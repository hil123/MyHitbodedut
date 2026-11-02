import React from 'react';
import {AbsoluteFill, Img, interpolate, spring, staticFile, useCurrentFrame, useVideoConfig, random, getInputProps} from 'remotion';
/** Effets « pro » v20 : --props='{"effets":true}' */
export const FX = (getInputProps() as {effets?: boolean}).effets === true;
import {C, EASE, FPS, Z} from './theme';

export const fontFaces = `
@font-face{font-family:'Inter';src:url('${staticFile('polices/Inter-Regular.otf')}');font-weight:400}
@font-face{font-family:'Inter';src:url('${staticFile('polices/Inter-Medium.otf')}');font-weight:500}
@font-face{font-family:'Inter';src:url('${staticFile('polices/Inter-SemiBold.otf')}');font-weight:600}
@font-face{font-family:'Inter';src:url('${staticFile('polices/Inter-Bold.otf')}');font-weight:700}
@font-face{font-family:'FrankRuhl';src:url('${staticFile('polices/frank_ruhl.ttf')}')}
`;

const clamp = {extrapolateLeft: 'clamp' as const, extrapolateRight: 'clamp' as const};
export const lerp = (f: number, a: number, b: number, x: number, y: number, easing = EASE.derive) =>
  interpolate(f, [a, b], [x, y], {...clamp, easing});

/** Texte écran : apparition par ressort, mot à mot (décalage 4 images), sortie plus rapide. */
export const TexteEcran: React.FC<{texte: string; debut?: number; fin: number; y?: number; taille?: number; or?: boolean; sombre?: boolean}> = ({
  texte, debut = 12, fin, y = Z.texteHaut, taille = 64, or = false, sombre = false,
}) => {
  const f = useCurrentFrame();
  const {fps} = useVideoConfig();
  const mots = texte.split(' ');
  const sortie = lerp(f, fin - 9, fin, 1, 0, EASE.depart);
  return (
    <div style={{position: 'absolute', left: 70, right: 70, top: y, height: Z.texteBas - y, display: 'flex', alignItems: 'center', justifyContent: 'center', textAlign: 'center', opacity: sortie}}>
      <div style={{fontFamily: 'FrankRuhl, serif', fontSize: taille, lineHeight: 1.18, color: sombre ? C.petrole : or ? C.or : C.blanc,
        textShadow: sombre ? 'none' : '0 4px 30px rgba(0,0,0,.85), 0 2px 6px rgba(0,0,0,.8), 0 0 2px rgba(0,0,0,.9)', letterSpacing: 0.5}}>
        {mots.map((m, i) => {
          const s = spring({frame: f - debut - i * 4, fps, config: {damping: 200}});
          return (
            <span key={i} style={{display: 'inline-block', opacity: s, transform: `translateY(${(1 - s) * 30}px) scale(${0.96 + 0.04 * s})`, marginRight: '0.26em', filter: FX ? `blur(${(1 - s) * 10}px)` : undefined}}>
              {m}
            </span>
          );
        })}
      </div>
    </div>
  );
};

/** Sous-titres synchronisés : la phrase est découpée en segments (ponctuation), durée au prorata des caractères. */
export const SousTitres: React.FC<{texte: string; debut: number; duree: number; blocs?: {t: number; txt: string}[]}> = ({texte, debut, duree, blocs: blocsCales}) => {
  const f = useCurrentFrame();
  if (blocsCales) {
    // blocs calés sur l'horodatage réel des mots de la voix (secondes)
    const pl = blocsCales.map((b, i) => ({b: b.txt, a: debut + b.t * FPS, z: debut + (i + 1 < blocsCales.length ? blocsCales[i + 1].t : duree) * FPS}));
    if (FX) {
      // fondu croisé glissé entre blocs : le bloc sortant monte et s'efface pendant que le suivant arrive
      return (<>{pl.map((p, i) => {
        const fin = p.z + (i === pl.length - 1 ? 12 : 0);
        if (f < p.a || f > fin) return null;
        const e = spring({frame: f - p.a - 1, fps: FPS, config: {damping: 200}, durationInFrames: 9});
        const x = lerp(f, fin - 5, fin, 0, 1); // sort AVANT l'arrivée du bloc suivant : jamais deux sous-titres superposés
        return (
          <div key={i} style={{position: 'absolute', left: 80, right: 80, top: Z.stHaut, height: Z.stBas - Z.stHaut, display: 'flex', alignItems: 'flex-start', justifyContent: 'center',
            opacity: e * (1 - x), transform: `translateY(${(1 - e) * 18 - x * 14}px) scale(${0.97 + 0.03 * e})`, filter: `blur(${(1 - e) * 6 + x * 4}px)`}}>
            <div style={{fontFamily: 'Inter', fontWeight: 600, fontSize: 40, lineHeight: 1.3, color: C.blanc, textAlign: 'center', background: 'rgba(7,17,17,.62)', padding: '14px 26px', borderRadius: 18, maxWidth: 900}}>{p.b}</div>
          </div>
        );
      })}</>);
    }
    const c = pl.find((p, i) => f >= p.a && f < p.z + (i === pl.length - 1 ? 12 : 0));
    if (!c) return null;
    const o = Math.min(lerp(f, c.a, c.a + 4, 0, 1), lerp(f, c.z + 8, c.z + 12, 1, 0));
    return (
      <div style={{position: 'absolute', left: 80, right: 80, top: Z.stHaut, height: Z.stBas - Z.stHaut, display: 'flex', alignItems: 'flex-start', justifyContent: 'center', opacity: o}}>
        <div style={{fontFamily: 'Inter', fontWeight: 600, fontSize: 40, lineHeight: 1.3, color: C.blanc, textAlign: 'center', background: 'rgba(7,17,17,.62)', padding: '14px 26px', borderRadius: 18, maxWidth: 900}}>{c.b}</div>
      </div>
    );
  }
  const segs = texte.match(/[^.,?!…]+[.,?!…]*/g)!.map((s) => s.trim()).filter(Boolean);
  // regroupe les segments trop courts
  const blocs: string[] = [];
  segs.forEach((s) => {
    if (blocs.length && (blocs[blocs.length - 1].length < 18 || s.length < 12) && (blocs[blocs.length - 1] + s).length < 52) blocs[blocs.length - 1] += ' ' + s;
    else blocs.push(s);
  });
  const total = blocs.reduce((a, b) => a + b.length, 0);
  let t = debut;
  const frames = duree * FPS;
  const plages = blocs.map((b) => {
    const d = (b.length / total) * frames;
    const r = {b, a: t, z: t + d};
    t += d;
    return r;
  });
  const cur = plages.find((p) => f >= p.a && f < p.z + (p === plages[plages.length - 1] ? 12 : 0));
  if (!cur) return null;
  const o = Math.min(lerp(f, cur.a, cur.a + 4, 0, 1), lerp(f, cur.z + 8, cur.z + 12, 1, 0));
  return (
    <div style={{position: 'absolute', left: 80, right: 80, top: Z.stHaut, height: Z.stBas - Z.stHaut, display: 'flex', alignItems: 'flex-start', justifyContent: 'center', opacity: o}}>
      <div style={{fontFamily: 'Inter', fontWeight: 600, fontSize: 40, lineHeight: 1.3, color: C.blanc, textAlign: 'center',
        background: 'rgba(7,17,17,.62)', padding: '14px 26px', borderRadius: 18, maxWidth: 900}}>
        {cur.b}
      </div>
    </div>
  );
};

/** Étiquette visible tant qu'un plan est provisoire (règle montage-video §2). */
export const Provisoire: React.FC<{texte?: string}> = ({texte = 'PROVISOIRE — plan cinéma à tourner/générer'}) => (
  <div style={{position: 'absolute', top: 1660, left: 60, fontFamily: 'Inter', fontWeight: 700, fontSize: 22, letterSpacing: 2,
    color: '#ffdf8a', background: 'rgba(0,0,0,.55)', padding: '8px 14px', borderRadius: 8, border: '1px solid #ffdf8a'}}>
    {texte}
  </div>
);

/** Grain + vignette, couche commune d'étalonnage. */
export const Etalonnage: React.FC = () => {
  const f = useCurrentFrame();
  return (
    <AbsoluteFill style={{pointerEvents: 'none'}}>
      <AbsoluteFill style={{background: 'radial-gradient(ellipse at 50% 45%, rgba(0,0,0,0) 55%, rgba(0,0,0,.55) 100%)'}} />
      <AbsoluteFill style={{opacity: 0.06, mixBlendMode: 'overlay',
        backgroundImage: `url("data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' width='300' height='300'><filter id='n'><feTurbulence type='fractalNoise' baseFrequency='0.9' numOctaves='2' seed='${f % 8}'/></filter><rect width='300' height='300' filter='url(%23n)'/></svg>")`}} />
    </AbsoluteFill>
  );
};

/** Transition lumineuse discrète : halo doré qui culmine sur la coupe. */
export const FlashLumiere: React.FC<{coupes: number[]}> = ({coupes}) => {
  const f = useCurrentFrame();
  const v = Math.max(0, ...coupes.map((c) => interpolate(f, [c - 8, c, c + 10], [0, 1, 0], {...clamp, easing: EASE.derive})));
  if (v <= 0) return null;
  return (
    <AbsoluteFill style={{pointerEvents: 'none', opacity: v * 0.85, mixBlendMode: 'screen',
      background: `radial-gradient(ellipse at 70% 30%, rgba(255,240,200,.95) 0%, ${C.or}aa 30%, rgba(8,124,128,.25) 60%, rgba(0,0,0,0) 80%)`}} />
  );
};

/** Smartphone avec une capture réelle (rapport largeur/hauteur conservé : 1236×2676). */
export const LARGEUR_TEL = Math.floor((Z.sujetBas - Z.sujetHaut - 36) * 1236 / 2676);
export const Telephone: React.FC<{src: string; largeur?: number; style?: React.CSSProperties; children?: React.ReactNode; ecranStyle?: React.CSSProperties; zoom?: number; origine?: string}> = ({
  src, largeur = LARGEUR_TEL, style, children, ecranStyle, zoom = 1, origine = '50% 50%',
}) => {
  const h = largeur * (2676 / 1236);
  const f = useCurrentFrame();
  // le téléphone vit : pivot 3D lent et flottement (jamais figé)
  const vie = `perspective(1600px) rotateY(${Math.sin(f / 45) * 8}deg) rotateX(${Math.sin(f / 63) * 3}deg) translateY(${Math.sin(f / 33) * 8}px)`;
  return (
    <div style={{position: 'relative', width: largeur + 36, height: h + 36, transform: vie, borderRadius: largeur * 0.14, background: 'linear-gradient(145deg,#2b3535,#0b1414 40%,#1d2a2a)',
      padding: 18, boxShadow: `0 40px 120px rgba(0,0,0,.7), 0 0 0 2px #3b4848, 0 0 80px ${C.turquoise}55`, ...style}}>
      <div style={{width: largeur, height: h, borderRadius: largeur * 0.11, overflow: 'hidden', position: 'relative', background: '#000', ...ecranStyle}}>
        {src ? <Img src={staticFile(src)} style={{width: '100%', height: '100%', display: 'block', transform: `scale(${zoom})`, transformOrigin: origine}} /> : null}
        {children}
      </div>
    </div>
  );
};

/** Silhouette d'homme juif orthodoxe (chapeau noir, péot, chemise blanche, pantalon noir), vue de dos ou lointaine. */
export const Silhouette: React.FC<{h: number; couleur?: string; chemise?: string; pas?: number}> = ({h, couleur = '#0a0f0f', chemise = '#d9d6cf', pas = 0}) => {
  const jambe = Math.sin(pas) * 9;
  return (
    <svg width={h * 0.42} height={h} viewBox="0 0 42 100" style={{overflow: 'visible'}}>
      <ellipse cx="21" cy="6.5" rx="11" ry="2.2" fill={couleur} />
      <rect x="14.5" y="0" width="13" height="7" rx="3" fill={couleur} />
      <circle cx="21" cy="11.5" r="5" fill={couleur} />
      <path d="M11 20 Q21 15 31 20 L33 52 L9 52 Z" fill={chemise} />
      <path d="M11 20 Q8 36 9 52 L12 52 Z M31 20 Q34 36 33 52 L30 52Z" fill={couleur} opacity=".9" />
      <path d={`M12 50 L20 50 L${19 + jambe * 0.4} 98 L${13 + jambe} 98 Z`} fill={couleur} />
      <path d={`M22 50 L30 50 L${29 - jambe} 98 L${23 - jambe * 0.4} 98 Z`} fill={couleur} />
      <path d="M13 50 L13 60 M29 50 L29 60" stroke={chemise} strokeWidth="0.8" />
    </svg>
  );
};

/** Collines de Judée, couches dégradées (perspective atmosphérique). */
export const Collines: React.FC<{ciel: [string, string, string]; teintes: string[]; montee?: number; soleilY?: number}> = ({ciel, teintes, montee = 0, soleilY = 900}) => {
  const f = useCurrentFrame();
  return (
    <AbsoluteFill style={{background: `linear-gradient(180deg, ${ciel[0]} 0%, ${ciel[1]} 45%, ${ciel[2]} 70%)`}}>
      <AbsoluteFill style={{background: `radial-gradient(circle at 50% ${soleilY}px, rgba(255,230,170,.95) 0, rgba(255,200,120,.55) 140px, rgba(255,170,90,.18) 420px, rgba(0,0,0,0) 700px)`}} />
      {teintes.map((t, i) => {
        const prof = (i + 1) / teintes.length;
        const y = 980 + i * 150 - montee * (0.3 + prof);
        const amp = 60 + i * 30;
        const ph = random(`c${i}`) * 6;
        let d = `M -100 ${1920 + 400} L -100 ${y}`;
        for (let x = -100; x <= 1180; x += 40) d += ` L ${x} ${y - amp * (0.5 + 0.5 * Math.sin(x / (260 - i * 30) + ph)) - 25 * Math.sin(x / 90 + ph * 2)}`;
        d += ` L 1180 ${1920 + 400} Z`;
        return (
          <svg key={i} width={1080} height={1920} style={{position: 'absolute', inset: 0, transform: `translateX(${Math.sin((f + i * 40) / 200) * 4 * prof}px)`}}>
            <path d={d} fill={t} />
          </svg>
        );
      })}
    </AbsoluteFill>
  );
};

/** Cadre du sujet : tout ce qui est dedans reste dans la zone sujet (bords fondus). */
export const Cadre: React.FC<{children: React.ReactNode; fondu?: number}> = ({children, fondu = 70}) => (
  <div style={{position: 'absolute', left: 0, right: 0, top: Z.sujetHaut, height: Z.sujetBas - Z.sujetHaut, overflow: 'hidden',
    WebkitMaskImage: `linear-gradient(180deg, transparent 0, #000 ${fondu}px, #000 calc(100% - ${fondu}px), transparent 100%)`}}>
    {children}
  </div>
);
/** Zone sujet centrée (téléphones, sans masque). */
export const ZoneSujet: React.FC<{children: React.ReactNode; style?: React.CSSProperties}> = ({children, style}) => (
  <div style={{position: 'absolute', left: 0, right: 0, top: Z.sujetHaut, height: Z.sujetBas - Z.sujetHaut, display: 'flex', alignItems: 'center', justifyContent: 'center', ...style}}>
    {children}
  </div>
);
/** Bande sombre derrière la zone texte : le fond reste lisible, sans sujet derrière. */
export const BandeTexte: React.FC = () => (
  <div style={{position: 'absolute', left: 0, right: 0, top: 0, height: Z.sujetHaut + 20, background: 'linear-gradient(180deg, rgba(7,17,17,.7), rgba(7,17,17,.45) 70%, rgba(7,17,17,0))'}} />
);
