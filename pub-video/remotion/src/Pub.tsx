import React from 'react';
import {AbsoluteFill, Audio, Img, Sequence, interpolate, spring, staticFile, useCurrentFrame, useVideoConfig, random} from 'remotion';
import {C, EASE, SCENES, VO_DELAY, FPS, Z} from './theme';
import {BandeTexte, Cadre, Collines, Etalonnage, FlashLumiere, LARGEUR_TEL, Provisoire, Silhouette, SousTitres, Telephone, TexteEcran, ZoneSujet, fontFaces, lerp} from './ui';

/*
 * Règle de mise en page (retour de Hillel : « chevauchement textes ou images incompatible ») :
 *  - zone texte écran  : Z.texteHaut → Z.texteBas (rien d'autre que du fond derrière) ;
 *  - zone sujet        : Z.sujetHaut → Z.sujetBas (personnages, téléphones, étiquettes : tout y est confiné) ;
 *  - zone sous-titres  : Z.stHaut → Z.stBas ;
 *  - étiquette PROVISOIRE : sous les sous-titres, seule.
 * Les zooms d'écran se font À L'INTÉRIEUR du téléphone, qui garde une taille fixe.
 */
const sc = (id: number) => SCENES[id - 1];
const H = Z.sujetBas - Z.sujetHaut; // hauteur de la zone sujet

/* ---------- Scène 1 : le monde qui court ---------- */
const Scene1: React.FC = () => {
  const f = useCurrentFrame();
  const avance = lerp(f, 0, 240, 1, 1.15); // travelling avant
  // Deux rangées séparées en hauteur (aucun recouvrement vertical) ; dans une rangée, même vitesse
  // et espacement fixe supérieur à la largeur d'une silhouette : les passants ne se croisent jamais.
  const ralenti = f < 150 ? f : 150 + (f - 150) * 0.6 - ((f - 150) ** 2) * 0.0008; // ralenti cinéma
  const RANGEES = [
    {n: 5, h: 200, bas: 340, v: -3.2, flou: 3, op: 0.8}, // arrière : pieds à 490, tête à 290
    {n: 4, h: 300, bas: 20, v: 5.0, flou: 0, op: 1}, // avant : pieds à 810, tête à 510
  ];
  const passants = RANGEES.flatMap((r, ri) =>
    Array.from({length: r.n}, (_, i) => {
      const periode = 1400;
      const x = (((i * periode) / r.n + ri * 90 + r.v * ralenti) % periode + periode) % periode - 160;
      return {ri, i, x, r, pas: f * (ri ? 0.32 : 0.24) + i};
    }),
  );
  return (
    <AbsoluteFill style={{background: '#3a2c1d'}}>
      <Cadre>
        <AbsoluteFill style={{transform: `scale(${avance})`, transformOrigin: '50% 60%'}}>
          <AbsoluteFill style={{background: 'linear-gradient(180deg,#e9d3a6 0%,#d9b57d 35%,#a98653 70%,#5a4630 100%)'}} />
          {Array.from({length: 9}, (_, i) => {
            const w = 150 + random(`w${i}`) * 120;
            const h = 380 + random(`h${i}`) * 300;
            return (
              <div key={i} style={{position: 'absolute', left: i * 130 - 60, bottom: 340, width: w, height: h - 100, filter: 'blur(2.5px)',
                background: 'linear-gradient(180deg,#d8c19a,#b99a6c)', boxShadow: 'inset -20px 0 40px rgba(80,50,20,.35)', borderTop: '6px solid #c9ad80'}}>
                {Array.from({length: 5}, (_, k) => (
                  <div key={k} style={{position: 'absolute', left: 25, top: 40 + k * 110, width: w - 50, height: 55, borderRadius: '30px 30px 4px 4px', background: 'rgba(60,40,25,.55)'}} />
                ))}
              </div>
            );
          })}
          <div style={{position: 'absolute', left: 0, right: 0, bottom: 0, height: 345, background: 'linear-gradient(180deg,#9a8260,#4a3b2a)'}} />
          {passants.map((p) => (
            <div key={`${p.ri}-${p.i}`} style={{position: 'absolute', left: p.x, bottom: p.r.bas, filter: `blur(${p.r.flou}px)`, opacity: p.r.op,
              transform: p.r.v < 0 ? 'scaleX(-1)' : undefined}}>
              <Silhouette h={p.r.h} pas={p.pas} />
            </div>
          ))}
        </AbsoluteFill>
      </Cadre>
      <BandeTexte />
      <TexteEcran texte={sc(1).ecran!} debut={20} fin={232} />
      <Provisoire />
    </AbsoluteFill>
  );
};

/* ---------- Scène 2 : une question (zoom lent sur le visage) ---------- */
const Scene2: React.FC = () => {
  const f = useCurrentFrame();
  const z = lerp(f, 0, 240, 1.0, 1.3);
  return (
    <AbsoluteFill style={{background: '#2c241a'}}>
      <Cadre>
        <AbsoluteFill style={{background: 'linear-gradient(180deg,#d6bb8c,#9d7d52 55%,#3f3324)', filter: 'blur(14px)'}} />
        <AbsoluteFill style={{transform: `scale(${z})`, transformOrigin: '52% 22%'}}>
          <Img src={staticFile('img/char_walking.webp')} style={{position: 'absolute', height: 1250, left: 330 + lerp(f, 0, 240, 0, -25), top: 40}} />
        </AbsoluteFill>
      </Cadre>
      <BandeTexte />
      <TexteEcran texte={sc(2).ecran!} debut={70} fin={232} />
      <Provisoire texte="PROVISOIRE — personnage de l'app, plan réel à tourner/générer" />
    </AbsoluteFill>
  );
};

/* ---------- Scène 3 : quitter le bruit ---------- */
const Scene3: React.FC = () => {
  const f = useCurrentFrame();
  const aerien = lerp(f, 140, 270, 0, 300);
  const marche = lerp(f, 0, 200, 0, 1);
  return (
    <AbsoluteFill style={{overflow: 'hidden'}}>
      <AbsoluteFill style={{transform: `scale(${lerp(f, 0, 270, 1.12, 1.0)})`}}>
        <Collines ciel={['#2b4b5c', '#e59a5b', '#f6c27a']} teintes={['#a26b4a', '#7d4f37', '#573626', '#33231a']} montee={aerien} soleilY={820 + aerien * 0.2} />
      </AbsoluteFill>
      <div style={{position: 'absolute', left: 520 - marche * 40, top: Z.sujetBas - 380 - marche * 220 - aerien * 0.5,
        transform: `scale(${1 - marche * 0.55 - aerien / 1000})`, transformOrigin: '50% 100%'}}>
        <Silhouette h={360} chemise="#e8e1d2" pas={f * 0.22} />
      </div>
      <Provisoire />
    </AbsoluteFill>
  );
};

/* ---------- Scène 4 : parler à Hachem ---------- */
const Scene4: React.FC = () => {
  const f = useCurrentFrame();
  const proche = f > 180;
  return (
    <AbsoluteFill style={{overflow: 'hidden'}}>
      {!proche ? (
        <AbsoluteFill style={{transform: `scale(${lerp(f, 0, 180, 1.0, 1.06)})`, transformOrigin: '50% 60%'}}>
          <Collines ciel={['#30405a', '#d98a56', '#f3b56c']} teintes={['#8d5d42', '#5f3d2b', '#3a271c']} soleilY={700} />
          <Img src={staticFile('img/char_open_arms.webp')} style={{position: 'absolute', height: 560, left: 345, top: Z.sujetBas - 600,
            filter: 'brightness(.12) contrast(1.2) drop-shadow(0 0 30px rgba(255,200,120,.6))'}} />
        </AbsoluteFill>
      ) : (
        <AbsoluteFill style={{background: 'radial-gradient(circle at 50% 30%, #f6d090, #c27a45 40%, #3b2518 85%)'}}>
          <Cadre>
            <AbsoluteFill style={{transform: `scale(${lerp(f, 180, 360, 1.0, 1.08)})`, transformOrigin: '50% 30%'}}>
              <Img src={staticFile('img/char_praise.webp')} style={{position: 'absolute', height: 1300, left: 140, top: 40,
                filter: 'brightness(.18) contrast(1.15) drop-shadow(0 0 40px rgba(255,210,140,.75))'}} />
            </AbsoluteFill>
          </Cadre>
        </AbsoluteFill>
      )}
      <BandeTexte />
      <TexteEcran texte={sc(4).ecran!} debut={200} fin={352} />
      <Provisoire texte="PROVISOIRE — silhouette en contre-jour, plan réel à générer" />
    </AbsoluteFill>
  );
};

/* ---------- fond des scènes application ---------- */
const FondApp: React.FC = () => {
  const f = useCurrentFrame();
  return (
    <AbsoluteFill style={{background: `radial-gradient(circle at 50% ${45 + Math.sin(f / 60) * 3}%, ${C.petrole} 0%, #0a2424 35%, ${C.noir} 75%)`}}>
      <AbsoluteFill style={{background: `radial-gradient(circle at 85% 15%, ${C.or}33, rgba(0,0,0,0) 40%)`}} />
    </AbsoluteFill>
  );
};

const Logo: React.FC<{taille: number; apparition: number}> = ({taille, apparition}) => (
  <Img src={staticFile('img/logo.png')} style={{width: taille, height: taille, borderRadius: taille * 0.22, boxShadow: `0 0 60px ${C.or}55`,
    opacity: apparition, transform: `scale(${0.92 + 0.08 * apparition})`}} />
);

/** Carte « capture Android à fournir » : honnête, jamais une fausse interface. */
const AFournir: React.FC<{titre: string}> = ({titre}) => (
  <AbsoluteFill style={{background: '#10302c', alignItems: 'center', justifyContent: 'center', padding: 30, textAlign: 'center',
    fontFamily: 'Inter', color: '#ffdf8a', fontSize: 26, fontWeight: 700, lineHeight: 1.4}}>
    CAPTURE ANDROID À FOURNIR<br />{titre}
  </AbsoluteFill>
);

/* ---------- Scène 5 : présentation de l'application ---------- */
const Scene5: React.FC = () => {
  const f = useCurrentFrame();
  const {fps} = useVideoConfig();
  const apparition = spring({frame: f - 10, fps, config: {damping: 200}, durationInFrames: 40});
  const logo = spring({frame: f - 95, fps, config: {damping: 200}});
  return (
    <AbsoluteFill>
      <FondApp />
      <ZoneSujet style={{perspective: 1800}}>
        <div style={{transform: `translateY(${(1 - apparition) * 120}px) rotateY(${lerp(f, 0, 210, -16, 8)}deg) rotateX(3deg)`, opacity: apparition}}>
          <Telephone src="captures/web_accueil.png" />
        </div>
      </ZoneSujet>
      <div style={{position: 'absolute', top: Z.texteHaut, height: Z.texteBas - Z.texteHaut, left: 0, right: 0, display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 28}}>
        <Logo taille={120} apparition={logo} />
        <div style={{fontFamily: 'Inter', fontWeight: 700, fontSize: 64, letterSpacing: 6, color: C.blanc, opacity: logo, transform: `translateY(${(1 - logo) * 20}px)`,
          textShadow: `0 0 40px ${C.turquoise}`}}>MY HITBODEDUT</div>
      </div>
    </AbsoluteFill>
  );
};

/* ---------- Scène 6 : Hitbodedut guidée (5 temps, textes réels de l'app) ---------- */
const ETAPES = ['Dire ce qu’on a sur le cœur', 'Le regret du passé', 'La demande', 'Faire de l’étude une prière', 'L’action de grâce'];
const Scene6: React.FC = () => {
  const f = useCurrentFrame();
  const {fps} = useVideoConfig();
  const largeur = 300;
  return (
    <AbsoluteFill>
      <FondApp />
      <div style={{position: 'absolute', left: 50, top: Z.sujetHaut + (H - largeur * 2676 / 1236 - 36) / 2}}>
        <Telephone src="" largeur={largeur}><AFournir titre="« Hitbodedout guidée »" /></Telephone>
      </div>
      <div style={{position: 'absolute', left: 430, right: 40, top: Z.sujetHaut, height: H, display: 'flex', flexDirection: 'column', justifyContent: 'center', gap: 20}}>
        {ETAPES.map((e, i) => {
          const s = spring({frame: f - 30 - i * 18, fps, config: {damping: 200}});
          return (
            <div key={i} style={{opacity: s, transform: `translateX(${(1 - s) * 50}px)`, display: 'flex', alignItems: 'center', gap: 16,
              background: 'rgba(255,255,255,.07)', border: `1px solid ${C.turquoise}`, borderRadius: 20, padding: '16px 18px'}}>
              <div style={{minWidth: 50, height: 50, borderRadius: 25, background: C.or, color: C.noir, fontFamily: 'Inter', fontWeight: 700, fontSize: 26, display: 'flex', alignItems: 'center', justifyContent: 'center'}}>{i + 1}</div>
              <div style={{fontFamily: 'Inter', fontWeight: 600, fontSize: 29, color: C.blanc, lineHeight: 1.2}}>{e}</div>
            </div>
          );
        })}
        <div style={{fontFamily: 'Inter', fontSize: 21, color: '#b9d6d3', opacity: lerp(f, 130, 150, 0, 1)}}>
          D’après Likoutei Moharan II, 25 et le Rambam (Hilkhot Tefila 1, 2) — texte de l’application
        </div>
      </div>
      <TexteEcran texte={sc(6).ecran!} debut={10} fin={202} />
    </AbsoluteFill>
  );
};

/* ---------- Scène 7 : ton objectif quotidien (zoom dans l'écran) ---------- */
const Scene7: React.FC = () => {
  const f = useCurrentFrame();
  const premier = f < 105;
  return (
    <AbsoluteFill>
      <FondApp />
      <ZoneSujet>
        {premier ? (
          <Telephone src="captures/web_choix_duree.png" zoom={lerp(f, 10, 100, 1, 1.7)} origine="50% 82%" />
        ) : (
          <Telephone src="captures/web_seance.png" zoom={lerp(f, 105, 205, 1.6, 1.15)} origine="50% 38%" />
        )}
      </ZoneSujet>
      <TexteEcran texte={sc(7).ecran!} debut={10} fin={202} />
    </AbsoluteFill>
  );
};

/* ---------- Scène 8 : Je suis avec Hachem ---------- */
const ICONES = ['#e85d75', '#4f8cf7', '#f2b233', '#37c27a', '#9b6cf0', '#f07a3c'];
const Scene8: React.FC = () => {
  const f = useCurrentFrame();
  const choix = f < 110;
  return (
    <AbsoluteFill>
      <FondApp />
      <ZoneSujet>
        {choix ? <Telephone src=""><AFournir titre="« Je suis avec Hachem » : choix des applications" /></Telephone>
          : <Telephone src="captures/web_seance.png" zoom={lerp(f, 110, 235, 1.0, 1.25)} origine="50% 30%" />}
      </ZoneSujet>
      {ICONES.map((c, i) => {
        const gauche = i % 2 === 0;
        const rang = Math.floor(i / 2);
        const efface = lerp(f, 110 + i * 8, 140 + i * 8, 1, 0);
        return (
          <div key={i} style={{position: 'absolute', left: gauche ? 110 : 860, top: Z.sujetHaut + 150 + rang * 220 + Math.sin(f / 25 + i) * 6, width: 110, height: 110, borderRadius: 28,
            background: c, opacity: efface * 0.9, filter: `blur(${(1 - efface) * 12}px)`, transform: `scale(${0.7 + efface * 0.3})`, boxShadow: '0 10px 30px rgba(0,0,0,.4)'}} />
        );
      })}
      <TexteEcran texte={sc(8).ecran!} debut={10} fin={232} taille={58} />
    </AbsoluteFill>
  );
};

/* ---------- Scène 9 : montage rapide des réglages ---------- */
const MONTAGE9: {src: string; leg: string; zoom: number; o: string}[] = [
  {src: 'captures/web_langue.png', leg: 'Langue', zoom: 1.5, o: '50% 92%'},
  {src: 'captures/web_onboarding_6.png', leg: 'Comment t’appeler', zoom: 1.5, o: '50% 62%'},
  {src: 'captures/web_couleur_fond.png', leg: 'Couleurs', zoom: 1.5, o: '50% 88%'},
  {src: 'captures/web_accueil_sombre.png', leg: 'Clair ou sombre', zoom: 1.2, o: '50% 30%'},
  {src: '', leg: 'Horaires', zoom: 1, o: '50% 50%'},
  {src: '', leg: 'Durée du report', zoom: 1, o: '50% 50%'},
];
const Scene9: React.FC = () => {
  const f = useCurrentFrame();
  const n = MONTAGE9.length;
  const per = 210 / n;
  const i = Math.min(n - 1, Math.floor(f / per));
  const loc = f - i * per;
  const m = MONTAGE9[i];
  return (
    <AbsoluteFill>
      <FondApp />
      <div style={{position: 'absolute', right: 70, top: Z.sujetHaut, height: H, display: 'flex', alignItems: 'center',
        transform: `translateX(${lerp(loc, 0, 7, 40, 0, EASE.arrivee)}px)`}}>
        <Telephone src={m.src} zoom={lerp(loc, 0, per, 1, m.zoom)} origine={m.o}>
          {!m.src && <AFournir titre={`« ${m.leg} »`} />}
        </Telephone>
      </div>
      <div style={{position: 'absolute', left: 60, width: 1080 - 70 - LARGEUR_TEL - 36 - 60 - 40, top: Z.sujetHaut, height: H, display: 'flex', flexDirection: 'column', justifyContent: 'center', gap: 22}}>
        {MONTAGE9.map((x, k) => (
          <div key={k} style={{fontFamily: 'Inter', fontWeight: k === i ? 700 : 500, fontSize: k === i ? 40 : 30, color: k === i ? C.or : 'rgba(255,255,255,.45)'}}>{x.leg}</div>
        ))}
      </div>
      <TexteEcran texte={sc(9).ecran!} debut={8} fin={202} taille={60} />
    </AbsoluteFill>
  );
};

/* ---------- Scène 10 : Shabbat ---------- */
const Scene10: React.FC = () => {
  const f = useCurrentFrame();
  const table = f < 105;
  const flamme = 0.85 + 0.15 * Math.sin(f / 3) * Math.sin(f / 7);
  return (
    <AbsoluteFill>
      {table ? (
        <AbsoluteFill style={{background: 'radial-gradient(circle at 60% 50%, #6b3d1d 0%, #2a160b 55%, #0d0705 100%)'}}>
          <Cadre>
            <AbsoluteFill style={{background: `radial-gradient(circle at 66% 55%, rgba(255,190,100,${0.45 * flamme}) 0, rgba(255,150,60,.12) 380px, rgba(0,0,0,0) 650px)`}} />
            <Img src={staticFile('img/char_shabbat.webp')} style={{position: 'absolute', width: 1080, left: lerp(f, 0, 105, 0, -30), top: H - 1080 * 640 / 565 + 60,
              filter: `brightness(${0.75 + 0.1 * flamme}) sepia(.25)`}} />
          </Cadre>
          <Provisoire texte="PROVISOIRE — illustration de l'app, plan réel à générer" />
        </AbsoluteFill>
      ) : (
        <AbsoluteFill>
          <FondApp />
          <ZoneSujet>
            <Telephone src="captures/web_bougies.png" zoom={lerp(f, 105, 205, 1.0, 1.45)} origine="50% 75%" />
          </ZoneSujet>
        </AbsoluteFill>
      )}
      <BandeTexte />
      <TexteEcran texte={sc(10).ecran!} debut={14} fin={202} taille={60} />
    </AbsoluteFill>
  );
};

/* ---------- Scène 11 : message final ---------- */
const Scene11: React.FC = () => {
  const f = useCurrentFrame();
  const {fps} = useVideoConfig();
  const lever = lerp(f, 0, 200, 0, 1);
  const versNoir = lerp(f, 185, 205, 0, 1);
  const carte = spring({frame: f - 198, fps, config: {damping: 200}});
  const slogan = spring({frame: f - 214, fps, config: {damping: 200}});
  const sec = spring({frame: f - 230, fps, config: {damping: 200}});
  return (
    <AbsoluteFill style={{background: C.noir}}>
      <AbsoluteFill style={{transform: `scale(${lerp(f, 0, 200, 1.15, 1.0)})`, opacity: 1 - versNoir}}>
        <Collines ciel={['#1d3550', '#7fa3b8', '#f4cf8e']} teintes={['#6f6a5c', '#4b473d', '#2e2b25']} soleilY={1000 - lever * 220} />
        <Img src={staticFile('img/char_open_arms.webp')} style={{position: 'absolute', height: 480, left: 395, top: Z.sujetBas - 520,
          filter: 'brightness(.1) drop-shadow(0 0 26px rgba(255,220,150,.7))'}} />
      </AbsoluteFill>
      {f < 185 && <Provisoire />}
      {f >= 185 && (
        <AbsoluteFill style={{background: `radial-gradient(circle at 50% 45%, ${C.petrole} 0%, ${C.noir} 70%)`, opacity: versNoir}}>
          <div style={{position: 'absolute', top: 520, left: 80, right: 80, display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 44}}>
            <Logo taille={260} apparition={carte} />
            <div style={{fontFamily: 'Inter', fontWeight: 700, fontSize: 70, letterSpacing: 6, color: C.blanc, opacity: carte}}>MY HITBODEDUT</div>
            <div style={{fontFamily: 'Inter', fontWeight: 600, fontSize: 32, letterSpacing: 3, color: C.or, opacity: slogan, transform: `translateY(${(1 - slogan) * 20}px)`, textAlign: 'center', lineHeight: 1.5}}>
              YOUR PERSONAL CONVERSATION<br />WITH HASHEM
            </div>
            <div style={{fontFamily: 'FrankRuhl, serif', fontSize: 40, color: '#cfe5e2', opacity: sec, transform: `translateY(${(1 - sec) * 20}px)`, textAlign: 'center', lineHeight: 1.4}}>
              MY HITBODEDUT<br />Parle à Hachem. Chaque jour.
            </div>
          </div>
        </AbsoluteFill>
      )}
    </AbsoluteFill>
  );
};

const COMPOSANTS = [Scene1, Scene2, Scene3, Scene4, Scene5, Scene6, Scene7, Scene8, Scene9, Scene10, Scene11];

/** Musique baissée (≈ −11,5 dB) sous la voix, remontée dans les respirations. */
const volumeMusique = (f: number) => {
  let duck = 0;
  for (const s of SCENES) {
    const a = s.from + VO_DELAY;
    const z = a + s.voDur * FPS;
    duck = Math.max(duck, interpolate(f, [a - 8, a, z, z + 12], [0, 1, 1, 0], {extrapolateLeft: 'clamp', extrapolateRight: 'clamp'}));
  }
  return 0.9 - duck * 0.66;
};

export const Pub: React.FC = () => {
  const coupes = SCENES.slice(1).map((s) => s.from);
  return (
    <AbsoluteFill style={{background: C.noir}}>
      <style>{fontFaces}</style>
      {SCENES.map((s, i) => {
        const Comp = COMPOSANTS[i];
        return (
          <Sequence key={s.id} from={s.from} durationInFrames={s.dur} name={`Scène ${s.id}`}>
            <Comp />
            <Sequence from={VO_DELAY} name={`Voix ${s.id}`}>
              <Audio src={staticFile(`voix/s${String(s.id).padStart(2, '0')}.wav`)} />
            </Sequence>
          </Sequence>
        );
      })}
      <Etalonnage />
      <FlashLumiere coupes={coupes} />
      {SCENES.map((s) => (
        <Sequence key={`st${s.id}`} from={s.from} durationInFrames={s.dur}>
          <SousTitres texte={s.vo} debut={VO_DELAY} duree={s.voDur} />
        </Sequence>
      ))}
      <Audio src={staticFile('musique/musique_temoin.wav')} volume={volumeMusique} />
    </AbsoluteFill>
  );
};
