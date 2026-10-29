import {useThree} from '@react-three/fiber';
import React from 'react';
import {useDelayRender} from 'remotion';

// Place la caméra à chaque image, avant le rendu de la scène.
export const Camera: React.FC<{
  position: [number, number, number];
  cible?: [number, number, number];
}> = ({position, cible = [0, 0, 0]}) => {
  const camera = useThree((s) => s.camera);
  camera.position.set(...position);
  camera.lookAt(...cible);
  camera.updateMatrixWorld();
  return null;
};

// Le compositeur d'effets ne monte ses passes qu'après le premier rendu de
// Remotion : sans rendus supplémentaires, les premières images que voit chaque
// onglet de rendu sortent vides. On rend encore deux fois avant la capture.
export const Avance: React.FC<{f: number}> = ({f}) => {
  const advance = useThree((s) => s.advance);
  const {delayRender, continueRender} = useDelayRender();
  React.useEffect(() => {
    const h = delayRender(`effets image ${f}`);
    advance(performance.now());
    const a = requestAnimationFrame(() => {
      advance(performance.now());
      requestAnimationFrame(() => {
        advance(performance.now());
        continueRender(h);
      });
    });
    return () => {
      cancelAnimationFrame(a);
      continueRender(h);
    };
  }, [f, advance, delayRender, continueRender]);
  return null;
};
