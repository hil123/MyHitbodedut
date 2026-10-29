import React from 'react';
import * as THREE from 'three';

// Un personnage détouré posé sur un plan. `teinte` simule la lumière reçue :
// bleu d'écran à l'acte I, contre-jour doré à l'aube.
export const Avatar: React.FC<{
  tex: THREE.Texture | null;
  hauteur: number;
  position: [number, number, number];
  teinte: THREE.ColorRepresentation;
  opacite?: number;
  echelle?: number;
}> = ({tex, hauteur, position, teinte, opacite = 1, echelle = 1}) => {
  if (!tex) return null;
  const img = tex.image as HTMLImageElement;
  const ratio = img.width / img.height;
  return (
    <mesh position={position} scale={echelle} renderOrder={2}>
      <planeGeometry args={[hauteur * ratio, hauteur]} />
      <meshBasicMaterial
        map={tex}
        color={teinte}
        transparent
        opacity={opacite}
        alphaTest={0.04}
        toneMapped={false}
      />
    </mesh>
  );
};
