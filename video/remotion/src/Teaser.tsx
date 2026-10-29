import React, {useEffect, useState} from 'react';
import {AbsoluteFill, Audio, Sequence, continueRender, delayRender, interpolate, staticFile} from 'remotion';
import {Aube} from './scenes/Aube';
import {Barriere} from './scenes/Barriere';
import {Bruit} from './scenes/Bruit';
import {Portail} from './scenes/Portail';
import {T, TOTAL} from './theme';

const usePolices = () => {
  const [handle] = useState(() => delayRender('polices'));
  useEffect(() => {
    const polices = [
      new FontFace('Frank Ruhl Libre', `url(${staticFile('fonts/frank_ruhl.ttf')})`, {weight: '300 900'}),
      new FontFace('Varela Round', `url(${staticFile('fonts/varela_round.ttf')})`),
    ];
    Promise.all(polices.map((p) => p.load()))
      .then((ok) => ok.forEach((p) => (document.fonts as unknown as Set<FontFace>).add(p)))
      .finally(() => continueRender(handle));
  }, [handle]);
};

// Teaser d'effets visuels : 15,7 s, 9:16. Montage sans voix : texte + son.
export const Teaser: React.FC = () => {
  usePolices();
  return (
    <AbsoluteFill style={{background: '#000'}}>
      <Sequence from={T.bruit.from} durationInFrames={T.bruit.dur}>
        <Bruit />
      </Sequence>
      <Sequence from={T.barriere.from} durationInFrames={T.barriere.dur}>
        <Barriere />
      </Sequence>
      <Sequence from={T.portail.from} durationInFrames={T.portail.dur}>
        <Portail />
      </Sequence>
      <Sequence from={T.aube.from} durationInFrames={T.aube.dur}>
        <Aube />
      </Sequence>
      <Audio src={staticFile('audio/montee_acte1.m4a')} />
      {/* MUSIQUE TÉMOIN : licence non établie, ne pas diffuser */}
      <Sequence from={T.barriere.from + 26}>
        <Audio
          src={staticFile('audio/musique_temoin.m4a')}
          startFrom={0}
          volume={(f) =>
            interpolate(f, [0, 30, TOTAL - T.barriere.from - 26 - 30, TOTAL - T.barriere.from - 26], [0, 1, 1, 0], {
              extrapolateLeft: 'clamp',
              extrapolateRight: 'clamp',
            })
          }
        />
      </Sequence>
    </AbsoluteFill>
  );
};
