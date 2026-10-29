import {Easing} from 'remotion';

export const FPS = 30;
export const W = 1080;
export const H = 1920;

// Vert de l'application (écran d'ouverture, README) et or du lever.
export const C = {
  vert: '#1E3A30',
  vertClair: '#2F5A49',
  or: '#F4C77A',
  orVif: '#FFD58A',
  creme: '#FFF6E6',
  bleuEcran: '#5AA8FF',
  rougeNotif: '#FF3B4E',
  nuit: '#05070D',
};

export const POLICE_TITRE = '"Frank Ruhl Libre", serif';
export const POLICE_UI = '"Varela Round", sans-serif';

export const EASE = {
  derive: Easing.inOut(Easing.sin),
  arrivee: Easing.out(Easing.cubic),
  depart: Easing.in(Easing.cubic),
};

// Minutage du teaser, en images.
export const T = {
  bruit: {from: 0, dur: 120},
  barriere: {from: 120, dur: 40},
  portail: {from: 160, dur: 120},
  aube: {from: 280, dur: 190},
};
export const TOTAL = T.aube.from + T.aube.dur;
