import {useEffect, useMemo, useState} from 'react';
import {continueRender, delayRender, random, staticFile} from 'remotion';
import * as THREE from 'three';

// Texture chargée avant que Remotion ne capture l'image.
export const useTexture = (src: string) => {
  const [tex, setTex] = useState<THREE.Texture | null>(null);
  const [handle] = useState(() => delayRender(`texture ${src}`));
  useEffect(() => {
    new THREE.TextureLoader().load(
      staticFile(src),
      (t) => {
        t.colorSpace = THREE.SRGBColorSpace;
        t.anisotropy = 8;
        setTex(t);
        continueRender(handle);
      },
      undefined,
      (e) => {
        console.warn('TEXTURE ECHEC', src, String(e));
        continueRender(handle);
      },
    );
  }, [src, handle]);
  return tex;
};

// Bruit déterministe : même image, même rendu.
export const r = (seed: string | number) => random(seed);

export const lisse = (t: number, a: number, b: number) => {
  const x = Math.min(1, Math.max(0, (t - a) / (b - a)));
  return x * x * (3 - 2 * x);
};

// Halo radial doux (lueur d'écran, soleil, poussière).
export const useHalo = (couleur = '255,255,255') =>
  useMemo(() => {
    const s = 256;
    const c = document.createElement('canvas');
    c.width = c.height = s;
    const g = c.getContext('2d')!;
    const grad = g.createRadialGradient(s / 2, s / 2, 0, s / 2, s / 2, s / 2);
    grad.addColorStop(0, `rgba(${couleur},1)`);
    grad.addColorStop(0.25, `rgba(${couleur},0.55)`);
    grad.addColorStop(0.6, `rgba(${couleur},0.12)`);
    grad.addColorStop(1, `rgba(${couleur},0)`);
    g.fillStyle = grad;
    g.fillRect(0, 0, s, s);
    const t = new THREE.CanvasTexture(c);
    t.colorSpace = THREE.SRGBColorSpace;
    return t;
  }, [couleur]);

// Une notification : carte arrondie, pastille rouge, lignes de texte.
const ICONES = ['#25D366', '#1877F2', '#FF0000', '#8E44AD', '#0A84FF', '#F39C12'];
export const useCartesNotif = (n = 6) =>
  useMemo(() => {
    return new Array(n).fill(0).map((_, k) => {
      const w = 512;
      const h = 160;
      const c = document.createElement('canvas');
      c.width = w;
      c.height = h;
      const g = c.getContext('2d')!;
      const rr = 38;
      g.fillStyle = 'rgba(245,247,252,0.96)';
      g.beginPath();
      g.roundRect(8, 8, w - 16, h - 16, rr);
      g.fill();
      g.fillStyle = ICONES[k % ICONES.length];
      g.beginPath();
      g.roundRect(30, 38, 84, 84, 22);
      g.fill();
      g.fillStyle = '#2b2f38';
      g.fillRect(134, 46, 190 + (k * 37) % 120, 22);
      g.fillStyle = '#9aa1ad';
      g.fillRect(134, 86, 250 + (k * 53) % 90, 16);
      g.fillRect(134, 112, 150 + (k * 29) % 110, 14);
      g.fillStyle = '#FF3B4E';
      g.beginPath();
      g.arc(w - 40, 40, 26, 0, Math.PI * 2);
      g.fill();
      g.fillStyle = '#fff';
      g.font = 'bold 30px sans-serif';
      g.textAlign = 'center';
      g.textBaseline = 'middle';
      g.fillText(String(1 + ((k * 7) % 9)), w - 40, 41);
      const t = new THREE.CanvasTexture(c);
      t.colorSpace = THREE.SRGBColorSpace;
      t.anisotropy = 8;
      return t;
    });
  }, [n]);
